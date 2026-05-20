/**
 * @FileName : AuthServiceImpl.java
 * @Description : 계정 인증 및 JWT 토큰 관리 서비스
 * @Author : 김다솜
 * @Date : 2026. 04. 17
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.17    김다솜        최초 생성 및 로그인/JWT 발급 로직 구현
 * @ 2026.04.23    김다솜        사번 기반 권한 매핑 로직 보완
 * @ 2026.05.07    김다솜        Refresh Token DB 저장 및 재발급 로직 추가
 * @ 2026.05.08    김다솜        Spring Security UserDetailsService 구현 및 subject 기반 재발급 검증 적용
 * @ 2026.05.14    김다솜        JWT 만료/재발급 테스트용 상세 로그 및 로그인 사용자 소속 응답 보강
 * @ 2026.05.18    김다솜        로그인 응답 부서/직급 포함 및 role_name 기반 관리자 권한 판별 보강
 */
package com.ict06.team1_fin_pj.domain.auth.service;

import com.ict06.team1_fin_pj.common.security.JwtTokenProvider;
import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Primary
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements UserDetailsService {

    private final EmpRepository empRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Spring Security 사용자 인증 객체 조회
     *
     * @param empNo 사번
     * @return UserDetails 인증 객체
     */
    @Override
    public UserDetails loadUserByUsername(String empNo) throws UsernameNotFoundException {
        EmpEntity emp = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사번입니다."));

        return new PrincipalDetails(emp);
    }

    /**
     * 사용자 로그인 및 Access/Refresh Token 발급
     *
     * @param empNo 사번
     * @param password 사용자 비밀번호
     * @return 토큰 및 사용자 기본 정보
     */
    @Transactional
    public Map<String, Object> login(String empNo, String password) {
        System.out.println("[AuthService] 로그인 요청 수신 - 사번: " + empNo);

        EmpEntity emp = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        boolean match = passwordEncoder.matches(password, emp.getPassword());
        if (!match) {
            System.out.println("[AuthService] 로그인 실패 - 비밀번호 불일치, 사번: " + empNo);
            throw new RuntimeException("사번 또는 비밀번호가 일치하지 않습니다.");
        }

        String roleName = getRoleName(emp);
        String accessToken = jwtTokenProvider.createAccessToken(emp.getEmpNo(), roleName);
        String refreshToken = jwtTokenProvider.createRefreshToken(emp.getEmpNo());
        emp.updateRefreshToken(refreshToken);

        System.out.println("[AuthService] 로그인 성공 및 Refresh Token 저장 완료 - 사번: " + emp.getEmpNo());

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("empNo", emp.getEmpNo());
        response.put("userName", emp.getName());
        response.put("role", roleName);
        response.put("deptName", emp.getDeptName());
        response.put("positionName", emp.getPosition() != null ? emp.getPosition().getPositionName() : null);

        return response;
    }

    /**
     * Refresh Token 기반 Access Token 재발급
     *
     * @param refreshToken Refresh Token
     * @return 신규 Access Token
     */
    @Transactional
    public Map<String, String> reissue(String refreshToken) {
        System.out.println("[AuthService] Access Token 재발급 요청 수신");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            System.out.println("[AuthService] Refresh Token 검증 실패 - 만료 또는 형식 오류");
            throw new RuntimeException("만료되거나 유효하지 않은 Refresh Token입니다.");
        }

        String empNo = jwtTokenProvider.getEmpNo(refreshToken);
        System.out.println("[AuthService] Refresh Token subject 확인 - 사번: " + empNo);

        EmpEntity emp = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (emp.getRefreshToken() == null || !emp.getRefreshToken().equals(refreshToken)) {
            System.out.println("[AuthService] Refresh Token 불일치 - 사번: " + empNo);
            throw new RuntimeException("저장된 Refresh Token과 일치하지 않습니다.");
        }

        String roleName = getRoleName(emp);
        String newAccessToken = jwtTokenProvider.createAccessToken(emp.getEmpNo(), roleName);
        System.out.println("[AuthService] Access Token 재발급 성공 - 사번: " + emp.getEmpNo());

        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", newAccessToken);
        return tokenMap;
    }

    @Transactional
    public Map<String, String> reissue(String empNo, String refreshToken) {
        return reissue(refreshToken);
    }

    /**
     * role_id 기반 권한명 변환
     *
     * @param roleId 역할 ID
     * @return Spring Security 권한명
     */
    private String getRoleName(EmpEntity emp) {
        if (emp == null || emp.getRole() == null) {
            return "ROLE_USER";
        }

        String roleName = normalizeRoleName(emp.getRole().getRoleName());
        if (roleName != null) {
            return roleName;
        }

        Integer roleId = emp.getRole().getRoleId();
        if (roleId == null) {
            return "ROLE_USER";
        }

        return switch (roleId) {
            case 1 -> "ROLE_ADMIN";
            case 2 -> "ROLE_TEAM_LEADER";
            default -> "ROLE_USER";
        };
    }

    private String normalizeRoleName(String rawRoleName) {
        if (rawRoleName == null || rawRoleName.isBlank()) {
            return null;
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
        return normalized.startsWith("ROLE_") ? normalized : null;
    }
}
