/**
 * @FileName : CrawlingController.java
 * @Description : 사용자 홈 피드용 날씨/뉴스 API 컨트롤러
 * @Author : 김다솜
 * @Date : 2026. 05. 15
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.15    김다솜       위치 좌표 기반 구 단위 날씨 조회 파라미터 지원
 */
package com.ict06.team1_fin_pj.external.crawling;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class CrawlingController {

    private final CrawlingServiceImpl crawlingService;

    //날씨 엔드포인트
    @GetMapping("/weather")
    public ResponseEntity<String> getWeather(
            @RequestParam(defaultValue = "Seoul") String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        System.out.println("[external > crawling > CrawlingController - getWeather()]");

        String data = crawlingService.getWeatherData(city, lat, lon);

        return ResponseEntity.ok(data);
    }

    //뉴스 엔드포인트
    @GetMapping("/news")
    public ResponseEntity<List<Map<String, String>>> getNews() {
        System.out.println("[external > crawling > CrawlingController - getNews()]");

        //뉴스 리스트 가져오기
        List<Map<String, String>> newsList = crawlingService.getNewsData();

        return ResponseEntity.ok(newsList);
    }
}
