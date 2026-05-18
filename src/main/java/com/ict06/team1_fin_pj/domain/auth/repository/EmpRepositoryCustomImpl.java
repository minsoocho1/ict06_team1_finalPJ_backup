/**
 * @FileName : EmpRepositoryCustomImpl.java
 * @Description : QueryDSL 커스텀 레포지토리 구현체(Auth 도메인)
 *                (JPA 기본쿼리로 처리 어려운 동적쿼리 및 수정 처리)
 * @Author : 김다솜
 * @Date : 2026. 04. 22
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.22    김다솜        최초 생성/fetchOne()을 이용한 상세 조회 구현
 * @ 2026.04.23    김다솜        QueryDSL 기반 사원 정보 조회 구현
 * @ 2026.05.12    김다솜        마이페이지 부서 표시를 위해 로그인 사용자 조회 시 부서 정보를 fetch join 처리
 */

package com.ict06.team1_fin_pj.domain.auth.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.ict06.team1_fin_pj.domain.employee.entity.QEmpEntity.empEntity;

//QueryDSL 전용 Custom Repository
@RequiredArgsConstructor
public class EmpRepositoryCustomImpl implements EmpRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<EmpEntity> findLoginEmpInfByEmpNo(String empNo) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(empEntity)
                        .leftJoin(empEntity.department).fetchJoin()
                        .where(empEntity.empNo.eq(empNo))
                        .fetchOne()
        );
    }

    //마이페이지 > 정보 수정
    @Override
    @Transactional
    public void updateEmpInfo(String empNo, String name, String email, String phone) {
        queryFactory
                .update(empEntity)
                .set(empEntity.name, name)
                .set(empEntity.email, email)
                .set(empEntity.phone, phone)
                .where(empEntity.empNo.eq(empNo))
                .execute();
    }
}
