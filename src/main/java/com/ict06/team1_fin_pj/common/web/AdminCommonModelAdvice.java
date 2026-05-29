/**
 * @FileName : AdminCommonModelAdvice.java
 * @Description : 관리자 공통 화면 모델 속성 제공
 * @Author : 김다솜
 * @Date : 2026. 05. 29
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.29    김다솜        관리자 헤더 프로필 연결을 위한 로그인 관리자 정보 제공
 */
package com.ict06.team1_fin_pj.common.web;

import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AdminCommonModelAdvice {

    @ModelAttribute
    public void addAdminHeaderAttributes(
            HttpServletRequest request,
            Authentication authentication,
            Model model
    ) {
        String requestUri = request.getRequestURI();
        if (requestUri == null || !requestUri.startsWith("/admin")) {
            return;
        }

        if (authentication == null || !(authentication.getPrincipal() instanceof PrincipalDetails principal)) {
            return;
        }

        String empNo = principal.getEmpNo();
        model.addAttribute("adminEmpNo", empNo);
        model.addAttribute("adminLoginName", principal.getName());
        model.addAttribute("adminProfileUrl", "/admin/employees/" + empNo);
    }
}
