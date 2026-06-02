/**
 * @FileName : SecurityConfig.java
 * @Description : Spring Security 설정 클래스
 *              - React 사용자 API → JWT 기반 인증 (Stateless)
 *              - 관리자 페이지(Thymeleaf) → 세션 기반 인증 (Form Login)
 *              🔹 보안 구조 분리
 *              1. API (/api/**)
 *                 → JWT 인증
 *                 → 세션 사용 안함
 *              2. 관리자 (/admin/**)
 *                 → 세션 인증
 *                 → Form Login 사용
 *              🔹 목적
 *              - 사용자 API와 관리자 페이지 인증 방식 분리
 *              - API 요청 시 관리자 로그인 페이지로 리다이렉트되는 문제 방지
 * @Author : 김다솜
 * @Date : 2026. 04. 18
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.18    김다솜        최초 생성/SSE 구독, 알림 조회, 읽음 처리 API 구현
 * @ 2026.04.27    송영은        Thymeleaf 기반 관리자 페이지 전용 보안 설정(세션 기반 인증-Form Login 사용)
 * @ 2026.05.01    김다솜        API용 JWT 필터 체인과 관리자용 세션 필터 체인 분리
 * @ 2026.05.08    김다솜        SSE 구독 인증 검증 및 DaoAuthenticationProvider 생성자 수정
 * @ 2026.05.08    김다솜        PasswordEncoder 빈 분리, 인증 매니저 주입 방식 변경, 세션 고정 보호 및 로그아웃 인증정보 제거 설정 추가
 * @ 2026.05.08    김다솜        관리자 세션 만료 및 로그아웃 시 공용 로그인 페이지로 이동하도록 수정
 */

package com.ict06.team1_fin_pj.common.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtToken;

    @Value("${app.frontend.login-url:http://localhost:3000/auth/login}")
    private String commonLoginUrl;

    /**
     * 1. [React API용] JWT 기반 인증 필터 체인
     * - Stateless 세션 관리
     * - JWT 토큰 검증
     */
    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http)
        throws Exception {
        
        return http
                //1. 기본 보안 설정
                .securityMatcher("/api/**")
    
                //CORS 설정(corsConfigurationSource를 filterChain에 등록)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    
                //REST API이므로 CSRF 보안 및 formLogin, basic 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
    
                //Stateless: 세션 사용 X(JWT 기반)
                .sessionManagement(session ->
                        session.sessionCreationPolicy((SessionCreationPolicy.STATELESS)))
    
                // URL별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 공개 API(인증 불필요)
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/admin-signup").permitAll()
                        .requestMatchers("/api/organization/**").permitAll()

                        // 인증 필요한 API
                        .requestMatchers("/api/noti/subscribe").authenticated()
                        .requestMatchers("/api/noti/**").authenticated()
    
                        // 나머지 API는 JWT 인증 필요
                        .anyRequest().authenticated()
                )
    
                // 예외 처리(API 인증 에러 발생 시 리다이렉트 대신 401, 403 에러 응답)
                .exceptionHandling(e -> e
                        // 401 에러
                        .authenticationEntryPoint((request, response, authException) ->
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        // 403 에러
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                            response.sendError(HttpServletResponse.SC_FORBIDDEN))
                )
    
                // JWT 인증 필터 등록
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtToken),
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

//                // 2. 세션 정책: 타임리프 관리자 페이지를 위해 '필요 시 생성'으로 설정
//                // 👉JWT 적용하지만, 타임리프 전용으로 세션 사용 가능하도록 변경
//                .sessionManagement(session ->
//                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
//                )

//                // 3. URL별 권한 제어
//                // 👉요청 권한 설정
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/auth/**").permitAll()
//
//                        // [관리자] 관리자 로그인 및 관련 리소스는 허용하되 나머지는 ADMIN 권한 필수
//                        .requestMatchers("/admin/login", "/admin/login-process").permitAll()
//                        .requestMatchers("/admin/**").hasRole("ADMIN")
//
//                        // 👉기타
//                        .anyRequest().permitAll()
//                )
//
//                // 4. 타임리프 관리자용 폼 로그인 설정 (세션 기반)
//                .formLogin(form -> form
//                        .loginPage("/admin/login")                 // 로그인 페이지 URL
//                        .loginProcessingUrl("/admin/login-process")// 로그인 처리 URL
//                        .defaultSuccessUrl("/admin/main", true)    // 로그인 성공 후 이동
//                        .failureUrl("/admin/login?error=true")
//                        .permitAll()
//                )
//
//                // 5. 로그아웃 설정
//                .logout(logout -> logout
//                        .logoutUrl("/admin/logout")
//                        .logoutSuccessUrl("/admin/login")
//                        .invalidateHttpSession(true)
//                        .deleteCookies("JSESSIONID")
//                )


//                // 6. 필터 순서 정의
//                // API 요청 시 헤더에 토큰이 있다면 JWT 필터가 인증 수행
//                //UsernamePasswordAuthenticationFilter 앞에 JWT 인증 필터 추가
//                .addFilterBefore(new JwtAuthenticationFilter(jwtToken),
//                        UsernamePasswordAuthenticationFilter.class);

//        return http.build();

    /**
     * 2. [관리자 페이지용] 세션 기반 폼 로그인 필터 체인
     * - Form Login (username/password)
     * - 세션 유지
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminFilterChain(HttpSecurity http)
        throws Exception {
        http
                // 관리자 페이지 경로에만 적용
                .securityMatcher("/admin/**")

                // CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // URL별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 관리자 로그인 페이지 허용
                        .requestMatchers("/admin/login", "/admin/login-process").permitAll()
                        // 관리자 페이지는 ADMIN 권한 필요
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                )

                // Form Login 설정
                .formLogin(form -> form
                        .loginPage("/admin/login")      // 로그인 페이지
                        .loginProcessingUrl("/admin/login-process")         // 로그인 처리 URL
                        .defaultSuccessUrl("/admin/home", true)     // 로그인 성공 시
                        .failureUrl(commonLoginUrl)           // 로그인 실패 시
                        .permitAll()
                )

                // 관리자 세션 만료/미인증 접근 시 공용 로그인 페이지로 이동
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendRedirect(commonLoginUrl))
                )

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl(commonLoginUrl)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                )

                // 세션 정책
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                );

        return http.build();
    }

    // 3. 인증 매니저 설정
    // 관리자 로그인 시 DB 유저 정보와 비밀번호 암호화 방식을 연결
    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);
        return builder.build();
    }

    // 4. 프론트(React) -> 백엔드로 CORS(교차 출처 요청) 허용하는 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        //React 포트 허용(3000)
        corsConfig.setAllowedOrigins(List.of("http://localhost:3000"));

        //HTTP 메서드
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        //모든 헤더 허용(Authorization 누락 시 JWT 전송 불가)
        //corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control"));
        corsConfig.setAllowedHeaders(List.of("*"));

        //자격증명 허용(쿠키, 인증헤더 허용하려면 필수)
        corsConfig.setAllowCredentials(true);

        //Authorization 헤더 응답에 노출
        corsConfig.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        //모든 API 경로(/**)에 대해 위 설정 적용
        source.registerCorsConfiguration("/**", corsConfig);

        return source;
    }
}
