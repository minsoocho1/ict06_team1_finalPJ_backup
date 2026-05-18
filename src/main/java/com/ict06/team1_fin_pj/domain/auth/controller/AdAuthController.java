/**
 * @FileName : AdAuthController.java
 * @Description : 관리자 인증 진입 Controller
 * @Author : 김다솜
 * @Date : 2026. 05. 10
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.10    김다솜        관리자 로그인 진입을 공용 React 로그인 페이지로 리다이렉트 처리
 */
package com.ict06.team1_fin_pj.domain.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdAuthController {

    @Value("${app.frontend.login-url:http://localhost:3000/auth/login}")
    private String commonLoginUrl;

    // 관리자 로그인 페이지 (임시)
    // 관리자 인증 진입 화면은 공용 React 로그인 페이지를 사용합니다.
    @GetMapping("/login")
    public String loginPage() {
        return "redirect:" + commonLoginUrl;
    }
}
