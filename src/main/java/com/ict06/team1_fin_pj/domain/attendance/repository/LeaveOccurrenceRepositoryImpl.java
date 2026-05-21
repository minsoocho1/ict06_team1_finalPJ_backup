package com.ict06.team1_fin_pj.domain.attendance.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.core.types.Projections;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;

import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;

import java.util.List;

import com.ict06.team1_fin_pj.common.dto.attendance.AdLeaveStatusDTO;

import static com.ict06.team1_fin_pj.domain.attendance.entity.QLeaveOccurrenceEntity.leaveOccurrenceEntity;
import static com.ict06.team1_fin_pj.domain.employee.entity.QDepartmentEntity.departmentEntity;
import static com.ict06.team1_fin_pj.domain.employee.entity.QEmpEntity.empEntity;

/**
 * 관리자 연차/휴가 현황 QueryDSL 구현체
 *
 * 역할:
 * - LEAVE_OCCURRENCE 테이블 기준으로
 * - 사원별 총 연차 / 사용 연차 / 잔여 연차를 집계한다.
 */
@RequiredArgsConstructor
public class LeaveOccurrenceRepositoryImpl implements LeaveOccurrenceRepositoryCustom {

    /**
     * QueryDSL 쿼리 작성 객체
     */
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<AdLeaveStatusDTO> findAdminLeaveStatusList(
            String keyword,
            Integer deptId,
            String sortType,
            Pageable pageable
    ) {
        // 검색 조건을 담을 QueryDSL BooleanBuilder
        // 조건이 없으면 전체 조회, 조건이 있으면 where에 추가된다.
        BooleanBuilder builder = new BooleanBuilder();

        // 사원명 검색 조건
        // keyword가 비어있지 않을 때만 name like 조건을 추가한다.
        if (StringUtils.hasText(keyword)) {
            builder.and(empEntity.name.contains(keyword));
        }

        // 부서 검색 조건
        // deptId가 null이 아닐 때만 해당 부서 조건을 추가한다.
        if (deptId != null) {
            builder.and(departmentEntity.deptId.eq(deptId));
        }

        // 정렬 조건 생성
        // 화면에서 전달한 sortType 값에 따라 orderBy 기준을 다르게 적용한다.
        OrderSpecifier<?> orderSpecifier;

        // sortType이 null이면 기본값은 사번순으로 처리한다.
        if (sortType == null) {
            sortType = "empNo";
        }

        switch (sortType) {

            // 이름순 정렬
            case "name":
                orderSpecifier = empEntity.name.asc();
                break;

            // 잔여연차 많은순
            case "remainDesc":
                orderSpecifier = leaveOccurrenceEntity.remain_days.sum().desc();
                break;

            // 잔여연차 적은순
            case "remainAsc":
                orderSpecifier = leaveOccurrenceEntity.remain_days.sum().asc();
                break;

            // 기본값: 사번순 정렬
            case "empNo":
            default:
                orderSpecifier = empEntity.empNo.asc();
                break;
        }

        // 현재 페이지에 출력할 데이터 목록 조회
        List<AdLeaveStatusDTO> content = queryFactory
                .select(
                        Projections.bean(
                                AdLeaveStatusDTO.class,

                                // 사번
                                empEntity.empNo.as("empNo"),

                                // 사원명
                                empEntity.name.as("empName"),

                                // 부서명
                                departmentEntity.deptName.as("deptName"),

                                // 총 발생 연차 합계
                                leaveOccurrenceEntity.occurDays.sum().as("totalDays"),

                                // 사용 연차 합계
                                leaveOccurrenceEntity.used_days.sum().as("usedDays"),

                                // 잔여 연차 합계
                                leaveOccurrenceEntity.remain_days.sum().as("remainDays")
                        )
                )

                // 기준 테이블: LEAVE_OCCURRENCE
                .from(leaveOccurrenceEntity)

                // 연차 발생 내역과 사원 정보 조인
                .join(leaveOccurrenceEntity.employee, empEntity)

                // 사원의 부서 정보 조인
                .leftJoin(empEntity.department, departmentEntity)

                // 검색 조건 적용
                .where(builder)

                // 사원별로 합산하기 위한 그룹 기준
                .groupBy(
                        empEntity.empNo,
                        empEntity.name,
                        departmentEntity.deptName
                )

                // 정렬 조건 적용
                .orderBy(orderSpecifier)

                // 현재 페이지 시작 위치
                .offset(pageable.getOffset())

                // 한 페이지에 보여줄 데이터 개수
                .limit(pageable.getPageSize())

                .fetch();


        // 전체 데이터 개수 조회
        Long total = queryFactory
                .select(empEntity.empNo.countDistinct())

                // 기준 테이블: LEAVE_OCCURRENCE
                .from(leaveOccurrenceEntity)

                // 연차 발생 내역과 사원 정보 조인
                .join(leaveOccurrenceEntity.employee, empEntity)

                // 사원의 부서 정보 조인
                .leftJoin(empEntity.department, departmentEntity)

                // 검색 조건 적용
                .where(builder)

                .fetchOne();


        // Page 객체로 감싸서 Controller/Thymeleaf에서 페이징 정보를 사용할 수 있게 한다.
        return new PageImpl<>(
                content,
                pageable,
                total == null ? 0 : total
        );
    }
}