/**
 * @FileName : AuthController.java
 * @Description : 사원 로그인 및 토큰 재발급 처리 컨트롤러
 * @Author : 김다솜
 * @Date : 2026. 04. 16
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.16    김다솜        최초 생성
 * @ 2026.04.17    김다솜        JWT 로그인 처리 로직 연결
 * @ 2026.05.07    김다솜        Refresh Token 재발급 엔드포인트 추가
 * @ 2026.05.08    김다솜        /refresh 경로 호환 및 요청 DTO 단순화
 * @ 2026.05.14    김다솜        JWT 테스트용 로그인/재발급 로그 보강
 */
package com.ict06.team1_fin_pj.domain.auth.controller;

import com.ict06.team1_fin_pj.domain.auth.service.AuthServiceImpl;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final AuthServiceImpl authService;

    /**
     * 로그인 처리
     *
     * @param loginRequest 로그인 요청 DTO
     * @return Access/Refresh Token 및 사용자 정보
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("[AuthController] /api/auth/login 호출 - 사번: " + loginRequest.getEmpNo());
            Map<String, Object> result = authService.login(
                    loginRequest.getEmpNo(),
                    loginRequest.getPassword()
            );
            System.out.println("[AuthController] /api/auth/login 응답 성공 - 사번: " + loginRequest.getEmpNo());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            System.out.println("[AuthController] /api/auth/login 응답 실패 - 사번: "
                    + loginRequest.getEmpNo() + ", 메시지: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Refresh Token 기반 Access Token 재발급
     *
     * @param reissueRequest 재발급 요청 DTO
     * @return 신규 Access Token
     */
    @PostMapping({"/reissue", "/refresh"})
    public ResponseEntity<?> reissue(@RequestBody ReissueRequest reissueRequest) {
        try {
            System.out.println("[AuthController] /api/auth/refresh 호출");
            Map<String, String> result = authService.reissue(reissueRequest.getRefreshToken());
            System.out.println("[AuthController] /api/auth/refresh 응답 성공");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            System.out.println("[AuthController] /api/auth/refresh 응답 실패 - 메시지: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Data
    static class LoginRequest {
        private String empNo;
        private String password;
    }

    @Data
    static class ReissueRequest {
        private String refreshToken;
    }
}
