/**
 * @FileName : SecurityConfig.java
 * @Description :
 * @Author : 김다솜
 * @Date : 2026. 04. 18
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.18    김다솜        최초 생성/SSE 구독, 알림 조회, 읽음 처리 API 구현
 * @ 2026.05.07    김다솜        Refresh Token 생성 로직 추가 및 만료 시간 분리
 */

package com.ict06.team1_fin_pj.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secretKey;

    // AccessToken 만료 시간
    @Value("${jwt.expiration}")
    private long tokenValidTime;

    // RefreshToken 만료 시간 추가
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTokenValidTime;

    private Key key;

    private final UserDetailsService userDetailsService;

    public JwtTokenProvider(@Lazy UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @PostConstruct
    protected void init() {
        //secretKey를 Base64로 인코딩, 키 생성
        byte[] keyBytes = Base64.getEncoder().encode(secretKey.getBytes());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    //JWT 토큰 생성
    public String createAccessToken(String empNo, String role) {
        Claims claims = Jwts.claims().setSubject(empNo);
        claims.put("role", role);
        Date now = new Date();

        System.out.println("[JWT Provider] Access Token 생성 완료 - 사번: " + empNo);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                // 1. 토큰 유효기간 설정
                .setExpiration(new Date(now.getTime() + tokenValidTime))
                // 2. 암호화 서명
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    //RefreshToken 생성 메서드
    public String createRefreshToken(String empNo) {
        Claims claims = Jwts.claims().setSubject(empNo);
        Date now = new Date();

        System.out.println("[JWT Provider] Refresh Token 생성 완료 - 사번: " + empNo);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenValidTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    //JWT 토큰에서 인증 정보 조회
    //@param token: JWT 토큰
    //@return Authentication 객체
    public Authentication getAuthentication(String token) {
        String empNo = this.getEmpNo(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getEmpNo(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    //토큰에서 사원번호(empNo) 추출
    //@param token JWT 토큰
    //@return 사번(empNo)
    public String getEmpNo(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    //Request의 Header에서 토큰 값 가져오기("Authorization" : "Bearer TOKEN")
    //@param request HTTP 요청 객체
    //@return 토큰 문자열
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if(bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    //토큰 유효성 + 만료 여부 검증
    //@param jwtToken 검증할 JWT 토큰
    //@return 유효 여부
    public boolean validateToken(String jwtToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwtToken);
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.out.println("[JWT Provider] 토큰 만료됨 (ExpiredJwtException)");
            return false;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            System.out.println("[JWT Provider] 유효하지 않은 토큰: " + e.getMessage());
            return false;
        }
    }
}