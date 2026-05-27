/**
 * @author : 송영은
 * description : 여기에서 'Run' 버튼을 눌러서 먼저 프로젝트 실행해주세요.
 * 그다음 비주얼 스튜디오코드에서 리액트 프로젝트를 실행해주세요.
 * ========================================
 * DATE         AUTHOR      NOTE
 * 2026-04-20   송영은       최초 생성
 **/

package com.ict06.team1_fin_pj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// http://localhost:8081/admin/login
@EnableJpaAuditing
@EnableScheduling	// 스케줄러 기능 사용 설정
@SpringBootApplication // Spring Boot 시작 클래스
@EnableAsync
public class Team1FinPjApplication {

	public static void main(String[] args) {
		SpringApplication.run(Team1FinPjApplication.class, args);
	}
}
