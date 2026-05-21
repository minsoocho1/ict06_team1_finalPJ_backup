/**
 * @FileName : CrawlingServiceImpl.java
 * @Description : 사용자 홈 피드용 외부 날씨/뉴스 데이터 조회 서비스
 * @Author : 김다솜
 * @Date : 2026. 05. 15
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.15    김다솜       OpenWeather 좌표 기반 조회와 역지오코딩 위치명 보강
 * @ 2026.05.18    김다솜       현재 위치 구/동 단위 표기를 위한 역지오코딩 위치명 보강
 */
package com.ict06.team1_fin_pj.external.crawling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CrawlingServiceImpl {

    //application.properties에서 OpenWeather API Key 가져오기
    @Value("${weather.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    //날씨 데이터 가져오기
    public String getWeatherData(String city, Double lat, Double lon) {
        System.out.println("[external > crawling > CrawlingServiceImpl - getWeatherData()]");

        UriComponentsBuilder weatherUrlBuilder = UriComponentsBuilder
                .fromUriString("https://api.openweathermap.org/data/2.5/weather")
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .queryParam("lang", "kr")
                .queryParam(lat != null && lon != null ? "lat" : "q", lat != null && lon != null ? lat : city);

        if (lat != null && lon != null) {
            weatherUrlBuilder.queryParam("lon", lon);
        }

        String url = weatherUrlBuilder.build().toUriString();

        RestTemplate restTemplate = new RestTemplate();
        String weatherJson = restTemplate.getForObject(url, String.class);

        if (lat == null || lon == null || weatherJson == null) {
            return weatherJson;
        }

        try {
            ObjectNode weatherNode = (ObjectNode) objectMapper.readTree(weatherJson);
            String reverseUrl = UriComponentsBuilder
                    .fromUriString("https://api.openweathermap.org/geo/1.0/reverse")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParam("limit", 1)
                    .queryParam("appid", apiKey)
                    .build()
                    .toUriString();

            String reverseJson = restTemplate.getForObject(reverseUrl, String.class);
            JsonNode locationNode = objectMapper.readTree(reverseJson);

            if (locationNode.isArray() && !locationNode.isEmpty()) {
                JsonNode firstLocation = locationNode.get(0);
                String localName = firstLocation.path("local_names").path("ko").asText("");
                String defaultName = firstLocation.path("name").asText("");
                String displayLocation = localName.isBlank() ? defaultName : localName;

                if (!displayLocation.isBlank()) {
                    weatherNode.put("display_location", displayLocation);
                }
            }

            String detailLocation = getDetailedLocationName(restTemplate, lat, lon);
            if (!detailLocation.isBlank()) {
                weatherNode.put("display_location_detail", detailLocation);
                weatherNode.put("display_location", detailLocation);
            }

            weatherNode.put("location_source", "GPS");
            return objectMapper.writeValueAsString(weatherNode);
        } catch (Exception e) {
            System.err.println("위치명 보강 실패: " + e.getMessage());
            return weatherJson;
        }
    }

    //뉴스 데이터 가져오기(크롤링)
    public List<Map<String, String>> getNewsData() {
        System.out.println("[external > crawling > CrawlingServiceImpl - getWeatherData()]");

        String url = "https://news.naver.com/section/105";
        List<Map<String, String>> newsList = new ArrayList<>();

        try {
            //Jsoup으로 위 url의 HTML 긁어오기
            //.userAgent: 봇으로 인식해 차단 방지 역할
            Document doc = Jsoup.connect(url)
                                .userAgent("Mozilla/5.0")
                                .get();

            //뉴스 제목/링크 추출
            Elements elements = doc.select(".sa_text_strong");

            for(int i = 0; i < Math.min(elements.size(), 5); i++) {
                Map<String, String> news = new HashMap<>();
                news.put("title", elements.get(i).text());

                //부모/조상 링크 탐색
                String link = "";
                org.jsoup.nodes.Element el = elements.get(i);

                //위로 올라가면서 a태그 찾기
                while(el != null) {
                    if(el.tagName().equals("a")) {
                        link = el.attr("abs:href");
                        break;
                    }
                    el = el.parent();
                }

                //a태그 없으면 근처 a태그에서 링크 추출
                if(link.isEmpty()) {
                    org.jsoup.nodes.Element nearestA = elements.get(i).selectFirst("a");
                    if(nearestA != null) {
                        link = nearestA.attr("abs:href");
                    }
                }
                System.out.println("뉴스: " + elements.get(i).text() + "/링크: " + link);
                news.put("link", link);
                newsList.add(news);
            }
        } catch(IOException e) {
            System.err.println("뉴스 크롤링 오류: " + e.getMessage());
        }
        return newsList;

    }

    private String getDetailedLocationName(RestTemplate restTemplate, Double lat, Double lon) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString("https://nominatim.openstreetmap.org/reverse")
                    .queryParam("format", "jsonv2")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParam("zoom", 18)
                    .queryParam("addressdetails", 1)
                    .queryParam("accept-language", "ko")
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "ict06-team1-finalPJ/1.0");

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            JsonNode address = objectMapper.readTree(response.getBody()).path("address");
            if (address.isMissingNode()) {
                return "";
            }

            String city = firstText(address, "city", "province", "state");
            String district = firstText(address, "city_district", "borough", "county");
            String town = firstText(address, "suburb", "quarter", "neighbourhood", "village", "town");

            return joinLocation(city, district, town);
        } catch (Exception e) {
            System.err.println("상세 위치명 조회 실패: " + e.getMessage());
            return "";
        }
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = node.path(fieldName).asText("");
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String joinLocation(String... parts) {
        List<String> locationParts = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank() && !locationParts.contains(part)) {
                locationParts.add(part);
            }
        }
        return String.join(" ", locationParts);
    }
}
