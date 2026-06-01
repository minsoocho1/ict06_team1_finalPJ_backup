package com.ict06.team1_fin_pj.domain.attendance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict06.team1_fin_pj.domain.attendance.entity.HolidayEntity;
import com.ict06.team1_fin_pj.domain.attendance.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

/*
 * 공공데이터포털 공휴일 API 연동 Service
 *
 * 역할:
 * - 한국천문연구원 특일 정보 API(getRestDeInfo)를 호출한다.
 * - 응답 JSON에서 공휴일 정보를 추출한다.
 * - HOLIDAY 테이블에 저장한다.
 */
@Service
@RequiredArgsConstructor
public class HolidayApiService {

    // HOLIDAY 테이블 저장/조회 Repository
    private final HolidayRepository holidayRepository;

    /*
     * application-local.properties
     * application-prod.properties
     *
     * 에 등록한 공공데이터포털 API Key 주입
     */
    @Value("${holiday.api.service-key}")
    private String serviceKey;

    /*
     * 한국천문연구원 특일 정보 API URL
     *
     * getRestDeInfo:
     * - 공휴일 정보 조회 API
     */
    private static final String API_URL =
            "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo";

    /*
     * 특정 연도의 공휴일 정보를 API에서 조회해
     * HOLIDAY 테이블에 저장한다.
     *
     * 예:
     * saveHolidaysByYear(2026)
     *
     * 처리 흐름:
     * - 1월 ~ 12월 반복
     * - API 호출
     * - JSON 응답 받기
     * - 공휴일 데이터 저장
     */
    public int saveHolidaysByYear(int year) {

        // 최종 저장된 공휴일 개수
        int savedCount = 0;

        // 외부 API 호출용 객체
        RestTemplate restTemplate = new RestTemplate();

        // 1월 ~ 12월 반복 조회
        for (int month = 1; month <= 12; month++) {

            /*
             * API는 월을 2자리 문자열로 받는다.
             *
             * 예:
             * 1 -> 01
             * 5 -> 05
             * 12 -> 12
             */
            String solMonth = String.format("%02d", month);

            /*
             * 공공데이터 API 호출 URI 생성
             *
             * query parameter:
             * - solYear
             * - solMonth
             * - ServiceKey
             * - numOfRows
             */
            URI uri = UriComponentsBuilder
                    .fromUriString(API_URL)
                    .queryParam("solYear", year)
                    .queryParam("solMonth", solMonth)
                    .queryParam("ServiceKey", serviceKey)
                    .queryParam("numOfRows", 20)
                    .build(false)
                    .toUri();

            /*
             * API 호출
             *
             * 반환값:
             * JSON 문자열
             */
            String json = restTemplate.getForObject(uri, String.class);

            /*
             * JSON 응답을 분석해서
             * 공휴일 데이터를 HOLIDAY 테이블에 저장
             */
            savedCount += saveHolidaysFromJson(json);
        }

        // 전체 저장 개수 반환
        return savedCount;
    }

    /*
     * API 응답 JSON에서
     * 공휴일 정보를 추출해 DB에 저장한다.
     */
    private int saveHolidaysFromJson(String json) {

        // 응답이 비어있으면 저장할 데이터 없음
        if (json == null || json.isBlank()) {
            return 0;
        }

        // 저장된 공휴일 개수
        int savedCount = 0;

        try {

            /*
             * JSON 파싱 객체
             *
             * Jackson ObjectMapper 사용
             */
            ObjectMapper objectMapper = new ObjectMapper();

            /*
             * JSON 문자열 -> JsonNode 변환
             */
            JsonNode root = objectMapper.readTree(json);

            /*
             * API 응답 코드 조회
             *
             * 정상:
             * resultCode = "00"
             */
            JsonNode header =
                    root.path("response")
                            .path("header");

            String resultCode =
                    header.path("resultCode").asText();

            /*
             * 정상 응답이 아니면 예외 발생
             */
            if (!"00".equals(resultCode)) {

                String resultMsg =
                        header.path("resultMsg").asText();

                throw new RuntimeException(
                        "공휴일 API 호출 실패: " + resultMsg
                );
            }

            /*
             * 실제 공휴일 데이터 영역 접근
             *
             * response
             *  -> body
             *      -> items
             *          -> item
             */
            JsonNode itemNode =
                    root.path("response")
                            .path("body")
                            .path("items")
                            .path("item");

            /*
             * item 데이터가 없으면 종료
             */
            if (itemNode.isMissingNode() || itemNode.isNull()) {
                return 0;
            }

            /*
             * 공휴일 데이터가 여러 개일 경우
             *
             * 배열 형태:
             * [
             *   {...},
             *   {...}
             * ]
             */
            if (itemNode.isArray()) {

                for (JsonNode item : itemNode) {

                    /*
                     * item 하나씩 저장
                     */
                    savedCount += saveOneHolidayItem(item);
                }

            } else {

                /*
                 * 공휴일 데이터가 1개일 경우
                 *
                 * 단일 객체 형태:
                 * { ... }
                 */
                savedCount += saveOneHolidayItem(itemNode);
            }

        } catch (Exception e) {

            /*
             * JSON 파싱/저장 중 오류 발생 시 예외 처리
             */
            throw new RuntimeException(
                    "공휴일 API JSON 처리 중 오류 발생: "
                            + e.getMessage(),
                    e
            );
        }

        return savedCount;
    }

    /*
     * 공휴일 item 1개를
     * HOLIDAY 테이블에 저장한다.
     */
    private int saveOneHolidayItem(JsonNode item) {

        /*
         * 공공기관 휴일 여부
         *
         * Y -> 공휴일
         * N -> 일반일
         */
        String isHoliday =
                item.path("isHoliday").asText();

        /*
         * 공휴일이 아니면 저장하지 않는다.
         */
        if (!"Y".equals(isHoliday)) {
            return 0;
        }

        /*
         * 공휴일명
         *
         * 예:
         * 설날
         * 어린이날
         * 광복절
         */
        String dateName =
                item.path("dateName").asText();

        /*
         * 날짜 문자열
         *
         * 예:
         * 20260101
         */
        String locdate =
                item.path("locdate").asText();

        /*
         * 날짜 문자열이 이상하면 저장하지 않는다.
         */
        if (locdate.length() != 8) {
            return 0;
        }

        /*
         * 문자열 날짜 -> LocalDate 변환
         */
        LocalDate holidayDate = LocalDate.of(
                Integer.parseInt(locdate.substring(0, 4)),
                Integer.parseInt(locdate.substring(4, 6)),
                Integer.parseInt(locdate.substring(6, 8))
        );

        /*
         * 이미 활성 공휴일로 등록되어 있으면
         * 중복 저장하지 않는다.
         */
        if (holidayRepository.existsByHolidayDateAndIsActiveTrue(holidayDate)) {
            return 0;
        }

        /*
         * HOLIDAY Entity 생성
         */
        HolidayEntity holiday = HolidayEntity.builder()
                .holidayDate(holidayDate)
                .holidayName(dateName)
                .isActive(true)
                .build();

        /*
         * DB 저장
         */
        holidayRepository.save(holiday);

        // 저장 성공
        return 1;
    }
}