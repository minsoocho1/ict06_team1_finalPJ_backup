/**
 * @FileName : PrincipalDetails.java
 * @Description : Spring Security Authentication 객체에서 사용하는 사용자 상세 정보
 * @Author : 김다솜
 * @Date : 2026. 04. 18
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.18    김다솜        최초 생성
 * @ 2026.05.18    김다솜        role_name 기반 관리자 권한 판별 보강
 * @ 2026.05.19    김다솜        인증 필터에서 Lazy Role 프록시 초기화 오류가 나지 않도록 권한 판별 로직 보완
 */

package com.ict06.team1_fin_pj.common.security;

import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security 인증에 필요한 사용자 정보를 감싸는 UserDetails 구현체입니다.
 */
@Getter
@RequiredArgsConstructor
public class PrincipalDetails implements UserDetails {

    private final EmpEntity empEntity;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(resolveRoleName()));
    }

    private String resolveRoleName() {
        if (empEntity == null || empEntity.getRole() == null) {
            return "ROLE_USER";
        }

        Integer roleId = empEntity.getRole().getRoleId();
        if (roleId == null) {
            return "ROLE_USER";
        }

        return switch (roleId) {
            case 1 -> "ROLE_ADMIN";
            case 2 -> "ROLE_TEAM_LEADER";
            default -> "ROLE_USER";
        };
    }

    @Override
    public String getPassword() {
        return empEntity.getPassword();
    }

    @Override
    public String getUsername() {
        return empEntity.getEmpNo();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "N".equals(empEntity.getIsDeleted());
    }

    public String getEmpNo() {
        return empEntity.getEmpNo();
    }

    public String getName() {
        return empEntity.getName();
    }

    public EmpEntity getEmp() {
        return empEntity;
    }
}
