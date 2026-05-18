/**
 * @FileName : WebConfig.java
 * @Description : Spring MVC 전역 설정 클래스
 *                - CORS 허용 설정 (React 개발 서버 연동)
 *                - RestTemplate Bean 등록 (AI 서버 HTTP 통신용)
 * @Author : 김다솜
 * @Date : 2026. 04. 18
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.18    김다솜        최초 생성 및 CORS 전역 설정 구현
 * @ 2026.05.04    김다솜        RestTemplate Bean 등록 추가 (AI 서버 연동용)
 * @ 2026.05.12    김다솜        AI 서버 응답 지연 시 무한 대기를 방지하고 원격 문서 처리 시간을 확보하기 위한 RestTemplate timeout 설정 추가
 */

package com.ict06.team1_fin_pj.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(120000);
        return new RestTemplate(requestFactory);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
