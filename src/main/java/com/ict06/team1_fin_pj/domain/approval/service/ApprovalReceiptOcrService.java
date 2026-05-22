package com.ict06.team1_fin_pj.domain.approval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict06.team1_fin_pj.common.dto.approval.ReceiptOcrResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 비용 정산 신청 서식의 영수증 OCR 자동입력을 담당하는 서비스입니다.
 *
 * <p>네이버 CLOVA OCR Secret Key는 브라우저에 노출되면 안 되므로 React에서 직접 호출하지 않고
 * 백엔드가 중계합니다. 이 서비스는 CLOVA 응답을 현재 비용 정산 신청 template의 필드 id로 변환합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class ApprovalReceiptOcrService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");
    private static final DateTimeFormatter BASIC_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestTemplate restTemplate;

    /*
     * 현재 프로젝트에서는 ObjectMapper를 Spring Bean으로 별도 등록하지 않습니다.
     * 그래서 생성자 주입 대신 서비스 내부에서 직접 생성해 애플리케이션 기동 실패를 방지합니다.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ncloud.ocr.receipt-url:}")
    private String receiptOcrUrl;

    @Value("${ncloud.ocr.secret-key:}")
    private String ocrSecretKey;

    /**
     * 업로드된 영수증 파일을 CLOVA OCR에 전달하고, 비용 정산 신청 필드에 들어갈 값을 반환합니다.
     */
    public ReceiptOcrResponseDto recognizeReceipt(MultipartFile file) {
        validateOcrProperties();
        validateReceiptFile(file);

        try {
            JsonNode root = callClovaReceiptOcr(file);
            Map<String, String> fieldValues = mapReceiptFields(root);

            return ReceiptOcrResponseDto.builder()
                    .success(!fieldValues.isEmpty())
                    .message(fieldValues.isEmpty()
                            ? "영수증에서 자동 입력할 항목을 찾지 못했습니다."
                            : "영수증 OCR 인식이 완료되었습니다.")
                    .fieldValues(fieldValues)
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException("영수증 파일을 읽는 중 오류가 발생했습니다.", e);
        } catch (RestClientException e) {
            throw new IllegalStateException("네이버 CLOVA OCR 호출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * CLOVA OCR Invoke URL과 Secret Key가 서버 설정에 등록되어 있는지 확인합니다.
     */
    private void validateOcrProperties() {
        if (receiptOcrUrl == null || receiptOcrUrl.isBlank()) {
            throw new IllegalStateException("ncloud.ocr.receipt-url 설정이 필요합니다.");
        }

        if (ocrSecretKey == null || ocrSecretKey.isBlank()) {
            throw new IllegalStateException("ncloud.ocr.secret-key 설정이 필요합니다.");
        }
    }

    /**
     * 비용 정산 증빙은 이미지 또는 PDF만 허용합니다.
     */
    private void validateReceiptFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("OCR 인식할 영수증 파일이 필요합니다.");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("영수증 OCR은 이미지(jpg, jpeg, png) 또는 PDF 파일만 지원합니다.");
        }
    }

    /**
     * CLOVA OCR 영수증 특화 API는 multipart/form-data에서 message JSON과 file 파트를 함께 받습니다.
     */
    private JsonNode callClovaReceiptOcr(MultipartFile file) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        String messageJson = objectMapper.writeValueAsString(Map.of(
                "version", "V2",
                "requestId", UUID.randomUUID().toString(),
                "timestamp", System.currentTimeMillis(),
                "images", List.of(Map.of(
                        "format", extension,
                        "name", createImageName(file.getOriginalFilename())
                ))
        ));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("message", messageJson);
        body.add("file", new MultipartFileResource(file));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-OCR-SECRET", ocrSecretKey);

        String response = restTemplate.postForObject(
                receiptOcrUrl,
                new HttpEntity<>(body, headers),
                String.class
        );

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("네이버 CLOVA OCR 응답이 비어 있습니다.");
        }

        return objectMapper.readTree(response);
    }

    /**
     * CLOVA OCR 원본 응답에서 현재 비용 정산 신청 template의 필드 id로 값을 변환합니다.
     */
    private Map<String, String> mapReceiptFields(JsonNode root) {
        JsonNode result = root.path("images").path(0).path("receipt").path("result");

        Map<String, String> fieldValues = new LinkedHashMap<>();

        putIfPresent(fieldValues, "payment_date", normalizeDate(readFirstText(
                result.path("paymentInfo").path("date"),
                result.path("paymentInfo").path("confirmedDate"),
                result.path("paymentInfo").path("approvalDate")
        )));

        putIfPresent(fieldValues, "expense_amount", normalizeAmount(readFirstText(
                result.path("totalPrice").path("price"),
                result.path("paymentInfo").path("price"),
                result.path("totalPrice")
        )));

        putIfPresent(fieldValues, "receipt_items_summary", summarizeItems(result));

        return fieldValues;
    }

    /**
     * 영수증 품목 목록을 "품목1, 품목2 ..." 형태로 줄여서 구매 내역 요약 필드에 채웁니다.
     */
    private String summarizeItems(JsonNode result) {
        List<String> itemNames = new ArrayList<>();

        JsonNode subResults = result.path("subResults");
        if (subResults.isArray()) {
            for (JsonNode subResult : subResults) {
                JsonNode items = subResult.path("items");
                if (!items.isArray()) {
                    continue;
                }

                for (JsonNode item : items) {
                    String itemName = readFirstText(
                            item.path("name"),
                            item.path("item"),
                            item.path("text")
                    );

                    if (!itemName.isBlank()) {
                        itemNames.add(itemName);
                    }

                    if (itemNames.size() >= 5) {
                        break;
                    }
                }

                if (itemNames.size() >= 5) {
                    break;
                }
            }
        }

        if (itemNames.isEmpty()) {
            return "";
        }

        return String.join(", ", itemNames);
    }

    /**
     * CLOVA OCR의 값 노드는 formatted.value, text, inferText 등 다양한 이름으로 내려올 수 있어
     * 가장 자주 쓰이는 후보를 순서대로 확인합니다.
     */
    private String readFirstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            String value = readText(node);
            if (!value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private String readText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }

        if (node.isTextual() || node.isNumber()) {
            return node.asText("").trim();
        }

        String formattedValue = node.path("formatted").path("value").asText("").trim();
        if (!formattedValue.isBlank()) {
            return formattedValue;
        }

        for (String fieldName : List.of("text", "inferText", "value", "name", "price")) {
            String value = node.path(fieldName).asText("").trim();
            if (!value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    /**
     * input type=date에 바로 넣을 수 있도록 yyyy-MM-dd 형태로 변환합니다.
     */
    private String normalizeDate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() == 8) {
            try {
                LocalDate parsedDate = LocalDate.parse(digits, BASIC_DATE_FORMATTER);
                return parsedDate.format(ISO_DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
                return "";
            }
        }

        return "";
    }

    /**
     * amount 타입은 React에서 쉼표를 표시하므로 서버에는 숫자 문자열만 전달합니다.
     */
    private String normalizeAmount(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.replaceAll("[^0-9]", "");
    }

    private void putIfPresent(Map<String, String> fieldValues, String fieldId, String value) {
        if (value != null && !value.isBlank()) {
            fieldValues.put(fieldId, value);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }

        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String createImageName(String filename) {
        String baseName = filename == null || filename.isBlank() ? "receipt" : filename;
        int dotIndex = baseName.lastIndexOf('.');
        return dotIndex > 0 ? baseName.substring(0, dotIndex) : baseName;
    }

    /**
     * MultipartFile을 RestTemplate multipart 요청의 file 파트로 보내기 위한 Resource입니다.
     */
    private static class MultipartFileResource extends ByteArrayResource {

        private final String filename;

        MultipartFileResource(MultipartFile file) throws IOException {
            super(file.getBytes());
            this.filename = file.getOriginalFilename();
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
