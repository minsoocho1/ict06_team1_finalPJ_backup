/**
 * @FileName : JwtTokenProvider.java
 * @Description : JWT Access/Refresh Token 생성, 검증 및 Authentication 복원 처리
 * @Author : 김다솜
 * @Date : 2026. 04. 18
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.18    김다솜        최초 생성/SSE 구독, 알림 조회, 읽음 처리 API 구현
 * @ 2026.05.07    김다솜        Refresh Token 생성 로직 추가 및 만료 시간 분리
 * @ 2026.05.19    김다솜        JWT role claim 기반 Authentication 생성으로 Lazy Role 프록시 오류 방지
 */

package com.ict06.team1_fin_pj.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long tokenValidTime;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTokenValidTime;

    private Key key;

    private final UserDetailsService userDetailsService;

    public JwtTokenProvider(@Lazy UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @PostConstruct
    protected void init() {
        byte[] keyBytes = Base64.getEncoder().encode(secretKey.getBytes());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String empNo, String role) {
        Claims claims = Jwts.claims().setSubject(empNo);
        claims.put("role", normalizeRoleName(role));
        Date now = new Date();

        System.out.println("[JWT Provider] Access Token 생성 완료 - 사번: " + empNo);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + tokenValidTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

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

    public Authentication getAuthentication(String token) {
        String empNo = getEmpNo(token);
        String roleName = normalizeRoleName(getRole(token));
        UserDetails userDetails = userDetailsService.loadUserByUsername(empNo);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                "",
                List.of(new SimpleGrantedAuthority(roleName))
        );
    }

    public String getEmpNo(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String getRole(String token) {
        Object role = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("role");
        return role != null ? role.toString() : "ROLE_USER";
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String jwtToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwtToken);
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.out.println("[JWT Provider] 토큰 만료됨(ExpiredJwtException)");
            return false;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            System.out.println("[JWT Provider] 유효하지 않은 토큰: " + e.getMessage());
            return false;
        }
    }

    private String normalizeRoleName(String rawRoleName) {
        if (rawRoleName == null || rawRoleName.isBlank()) {
            return "ROLE_USER";
        }

        String normalized = rawRoleName.trim().toUpperCase();
        if (normalized.contains("ADMIN") || normalized.contains("관리자")) {
            return "ROLE_ADMIN";
        }
        if (normalized.contains("TEAM_LEADER") || normalized.contains("TEAM LEADER") || normalized.contains("팀장")) {
            return "ROLE_TEAM_LEADER";
        }
        if (normalized.contains("USER") || normalized.contains("사원")) {
            return "ROLE_USER";
        }
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_USER";
    }
}
