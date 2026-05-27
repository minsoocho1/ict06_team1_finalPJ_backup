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
 */

package com.ict06.team1_fin_pj.common.security;

import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 로그인 요청 시 DB에서 User 정보 가져오는 클래스
 */
@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {

    private final EmpRepository usersRepository;

    /**
     * Spring Security가 내부적으로 사용하는 메서드
     * * @param username: 로그인 폼에서 전달된 ID (우리 프로젝트에서는 userId)
     * * @return UserDetails (PrincipalDetails 클래스)
     * * @throws UsernameNotFoundException: 사용자를 찾을 수 없을 때 던지는 예외
     */

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        EmpEntity user = usersRepository
                .findByEmpNoOrEmpId(username, username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "사용자를 찾을 수 없습니다: " + username));

        return new PrincipalDetails(user);
    }
}
