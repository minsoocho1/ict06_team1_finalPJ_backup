/**
 * @FileName : JwtAuthenticationFilter.java
 * @Description : 모든 요청에서 JWT 토큰 유효성 검사하는 필터
 * @Author : 김다솜
 * @Date : 2026. 04. 18
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.18    김다솜        최초 생성/SSE 구독, 알림 조회, 읽음 처리 API 구현
 * @ 2026.04.23    김다솜        SSE 지원 위한 쿼리 파라미터 토큰 추출 로직 추가
 * @ 2026.05.08    김다솜        관리자 페이지(/admin) 경로는 JWT 검사 제외
 *                              쿼리 파라미터 토큰 인증을 SSE 구독 API로 제한
 */

package com.ict06.team1_fin_pj.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

/**
 * 모든 요청에서 JWT 토큰 유효성 검사하는 필터
 * UsernamePasswordAuthenticationFilter 이전에 실행되도록 설정함
 */

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // [필수 로그] 모든 요청의 경로와 메서드 출력하여 필터 작동 여부 확인
        System.out.println("[JWT Filter] 요청 수신 -> [" + method + "] " + uri);

        // 1. 관리자 페이지는 JWT 검사 제외(세션/폼로그인에서 처리)
        if (uri.startsWith("/admin")) {
            System.out.println("[JWT Filter] 관리자 경로 스킵 (Session 사용): " + uri);
            chain.doFilter(request, response);
            return;
        }

        // 2. API 요청은 JWT 검증
        String token = null;

        // 2-1. Authorization 헤더에서 토큰 추출
        token = jwtTokenProvider.resolveToken(httpRequest);

        // 2-2. 헤더에 없으면 쿼리 파라미터에서 추출
        if(!StringUtils.hasText(token) && "/api/noti/subscribe".equals(uri)) {
            token = httpRequest.getParameter("token");
            System.out.println("[JWT Filter] SSE 구독: 쿼리 파라미터에서 토큰 추출 시도");
        }

        // 2-3. 토큰 유효성 검증
        if(token != null) {
            boolean isValid = jwtTokenProvider.validateToken(token);
            System.out.println("[JWT Filter] 토큰 유효성 검증 결과: " + isValid + " (토큰 앞부분: " + token.substring(0, Math.min(token.length(), 10)) + "...)");
            
            if(isValid) {
                // 유효하면 Authentication 객체 생성 -> SecurityContext에 저장
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                System.out.println("[JWT Filter] 인증 성공 - 사번: " + authentication.getName());
            }
        } else {
            // [로그 추가] 토큰이 없는 경우 (로그인 전이거나 비인가 경로 요청)
            System.out.println("[JWT Filter] 전달된 토큰 없음 (Anonymous User)");
        }

        // 원본: 관리자 페이지 세션 처리 안 한 경우
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//
//        //1. Request Header에서 JWT 토큰 추출
//        String token = jwtTokenProvider.resolveToken((HttpServletRequest) request);
//
//        //2. 헤더에 토큰 없는 경우 URL 파라미터("token")에서 추출(SSE 대응)
//        if(!StringUtils.hasText(token)) {
//            token = httpRequest.getParameter("token");
//        }
//
//        //3. 토큰 유효성 검증
//        if(token != null && jwtTokenProvider.validateToken(token)) {
//            //토큰 유효하면 인증 객체(Authentication) 생성, SecurityContext에 저장
//            Authentication authentication = jwtTokenProvider.getAuthentication(token);
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//        }

        //4. 다음 필터로 이동
        chain.doFilter(request, response);
    }
}
