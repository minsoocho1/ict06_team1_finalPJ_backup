package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.ReferenceExtractResponseDto;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

@Service
public class ReferenceFileExtractService {

    // 1회성 참고 자료 분석이 과도해지지 않도록 파일 크기와 본문 길이를 제한한다.
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_EXTRACTED_TEXT_LENGTH = 10_000;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt", "pdf", "docx");

    private final Tika tika = new Tika();

    public ReferenceExtractResponseDto extract(MultipartFile file) {
        // 파일 유효성 검증을 통과한 경우에만 본문 추출을 진행한다.
        validateFile(file);

        String fileName = safeFileName(file.getOriginalFilename());
        String extension = extractExtension(fileName);
        String extractedText = extractText(file);
        String normalizedText = normalizeExtractedText(extractedText);

        if (normalizedText.isBlank()) {
            // 스캔 PDF처럼 텍스트가 비는 경우에는 실패가 아니라 제한 안내로 응답한다.
            String message = "pdf".equals(extension)
                    ? "스캔본/이미지형 PDF는 본문 추출이 제한될 수 있습니다."
                    : "파일 본문을 추출하지 못했습니다.";

            return ReferenceExtractResponseDto.builder()
                    .fileName(fileName)
                    .contentType(file.getContentType())
                    .extractedText("")
                    .textLength(0)
                    .truncated(false)
                    .message(message)
                    .build();
        }

        // 너무 긴 본문은 앞부분만 잘라 AI 참고 자료로 사용한다.
        boolean truncated = normalizedText.length() > MAX_EXTRACTED_TEXT_LENGTH;
        String promptText = truncated
                ? normalizedText.substring(0, MAX_EXTRACTED_TEXT_LENGTH)
                : normalizedText;

        return ReferenceExtractResponseDto.builder()
                .fileName(fileName)
                .contentType(file.getContentType())
                .extractedText(promptText)
                .textLength(promptText.length())
                .truncated(truncated)
                .message(truncated
                        ? "본문이 길어 앞부분만 AI 참고 자료로 사용됩니다."
                        : "본문 추출 완료")
                .build();
    }

    private void validateFile(MultipartFile file) {
        // 영구 저장이나 OCR 없이 처리하므로 지원 범위를 명확히 제한한다.
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("첨부 파일이 없습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("첨부 파일은 5MB 이하만 지원합니다.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. TXT, PDF, DOCX만 업로드해 주세요.");
        }
    }

    private String extractText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            // Tika로 TXT/PDF/DOCX를 공통 방식으로 텍스트 추출한다.
            return tika.parseToString(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("첨부 파일을 읽는 중 오류가 발생했습니다.", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("파일 본문을 추출하지 못했습니다.", exception);
        }
    }

    private String normalizeExtractedText(String text) {
        if (text == null) {
            return "";
        }

        // 제어문자와 줄바꿈을 정리해 프롬프트에 바로 넣을 수 있게 만든다.
        return text
                .replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
    }

    private String safeFileName(String originalFilename) {
        String fileName = String.valueOf(originalFilename == null ? "" : originalFilename).trim();
        return fileName.isEmpty() ? "reference-file" : fileName;
    }

    private String extractExtension(String fileName) {
        int separatorIndex = fileName.lastIndexOf('.');
        if (separatorIndex < 0 || separatorIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(separatorIndex + 1).toLowerCase(Locale.ROOT);
    }
}
