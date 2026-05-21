package com.ict06.team1_fin_pj.domain.payroll.repository;

import com.ict06.team1_fin_pj.common.dto.payroll.PayrollStatementResponseDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollSummaryPageResponseDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollSummaryResponseDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollSummarySearchDTO;
import com.ict06.team1_fin_pj.domain.payroll.entity.PayrollStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ict06.team1_fin_pj.domain.employee.entity.QDepartmentEntity.departmentEntity;
import static com.ict06.team1_fin_pj.domain.employee.entity.QEmpEntity.empEntity;
import static com.ict06.team1_fin_pj.domain.employee.entity.QPositionEntity.positionEntity;
import static com.ict06.team1_fin_pj.domain.payroll.entity.QPayrollEntity.payrollEntity;
import static com.ict06.team1_fin_pj.domain.payroll.entity.QGradeCodeEntity.gradeCodeEntity;
import static com.ict06.team1_fin_pj.domain.payroll.entity.QPayrollItemEntity.payrollItemEntity;

// 급여요약 QueryDSL 구현체
@Repository
@RequiredArgsConstructor
public class PayrollSummaryRepositoryCustomImpl implements PayrollSummaryRepositoryCustom  {

    private final JPAQueryFactory queryFactory;

    // 급여요약 전체조회
    @Override
    public PayrollSummaryPageResponseDTO selectPayrollSummaryList(PayrollSummarySearchDTO searchDTO, String payMonth) {

        BooleanBuilder builder = new BooleanBuilder();

        // 삭제되지 않은 팀 소속 사원만 조회한다.
        builder.and(empEntity.isDeleted.eq("N"));
        builder.and(departmentEntity.parentDept.deptId.isNotNull());

        /**
         * 입사월 이전 조회 제외
         *
         * 급여요약은 입사월부터 조회 대상이다.
         * 예) 입사일 2026-03-15
         * - 2026-01 조회: 제외
         * - 2026-02 조회: 제외
         * - 2026-03 조회: 포함, 급여대장 없으면 미작성
         */
        Integer searchPayMonth = Integer.valueOf(payMonth.replace("-", ""));

        builder.and(
                empEntity.hireDate.isNotNull()
                        .and(
                                empEntity.hireDate.year()
                                        .multiply(100)
                                        .add(empEntity.hireDate.month())
                                        .loe(searchPayMonth)
                        )
        );

        // 사번 또는 이름 검색
        if (StringUtils.hasText(searchDTO.getKeyword())) {

            String keyword = searchDTO.getKeyword().trim();

            builder.and(
                    empEntity.empNo.containsIgnoreCase(keyword)
                            .or(empEntity.name.containsIgnoreCase(keyword))
            );
        }

        // 부서 필터
        if (StringUtils.hasText(searchDTO.getDeptId())) {
            builder.and(departmentEntity.deptId.eq(Integer.valueOf(searchDTO.getDeptId())));
        }

        // 직급 필터
        if (StringUtils.hasText(searchDTO.getPositionId())) {
            builder.and(positionEntity.positionId.eq(Integer.valueOf(searchDTO.getPositionId())));
        }

        // 상태 필터
        if (StringUtils.hasText(searchDTO.getStatus())) {

            if ("NEW".equals(searchDTO.getStatus())) {
                builder.and(payrollEntity.payrollId.isNull());
            }

            if ("DRAFT".equals(searchDTO.getStatus())) {
                builder.and(payrollEntity.status.eq(PayrollStatus.DRAFT));
            }

            if ("CONFIRMED".equals(searchDTO.getStatus())) {
                builder.and(payrollEntity.status.eq(PayrollStatus.CONFIRMED));
            }

            if ("PAID".equals(searchDTO.getStatus())) {
                builder.and(payrollEntity.status.eq(PayrollStatus.PAID));
            }
        }

        int page = searchDTO.getPage() < 1 ? 1 : searchDTO.getPage();
        int size = searchDTO.getSize() < 1 ? 10 : searchDTO.getSize();

        long offset = (long) (page - 1) * size;

        Long countResult = queryFactory
                .select(empEntity.count())
                .from(empEntity)
                .leftJoin(empEntity.department, departmentEntity)
                .leftJoin(empEntity.position, positionEntity)
                .leftJoin(payrollEntity).on(
                        payrollEntity.employee.empNo.eq(empEntity.empNo),
                        payrollEntity.payMonth.eq(payMonth)
                )
                .where(builder)
                .fetchOne();

        long totalCount = countResult == null ? 0 : countResult;
        int totalPages = (int) Math.ceil((double) totalCount / size);

        List<PayrollSummaryResponseDTO> content = queryFactory
                .select(Projections.fields(
                        PayrollSummaryResponseDTO.class,

                        payrollEntity.payrollId.longValue().as("payrollId"),

                        empEntity.empNo.as("empNo"),
                        empEntity.name.as("empName"),

                        departmentEntity.deptId.as("deptId"),
                        departmentEntity.deptName.as("deptName"),
                        departmentEntity.parentDept.deptName.as("parentDeptName"),

                        positionEntity.positionId.as("positionId"),
                        positionEntity.positionName.as("positionName"),

                        payrollEntity.totalGross.as("totalGross"),
                        payrollEntity.totalDeduction.as("totalDeduction"),
                        payrollEntity.netSalary.as("netSalary"),

                        // 조회월
                        payrollEntity.payMonth.as("payMonth"),
                        payrollEntity.status.stringValue().as("payrollStatus")
                ))
                .from(empEntity)
                .leftJoin(empEntity.department, departmentEntity)
                .leftJoin(empEntity.position, positionEntity)
                .leftJoin(payrollEntity).on(
                        payrollEntity.employee.empNo.eq(empEntity.empNo),
                        payrollEntity.payMonth.eq(payMonth)
                )
                .where(builder)
                .orderBy(getPayrollSummaryOrder(searchDTO.getSortType()))
                .offset(offset)
                .limit(size)
                .fetch();

        for (PayrollSummaryResponseDTO item : content) {

            item.setPayMonth(payMonth);

            if (item.getPayrollStatus() == null) {
                item.setPayrollStatus("NEW");
                item.setPayrollStatusName("미작성");
            } else if ("DRAFT".equals(item.getPayrollStatus())) {
                item.setPayrollStatusName("작성중");
            } else if ("CONFIRMED".equals(item.getPayrollStatus())) {
                item.setPayrollStatusName("확정");
            } else if ("PAID".equals(item.getPayrollStatus())) {
                item.setPayrollStatusName("지급완료");
            }
        }

        return new PayrollSummaryPageResponseDTO(
                content,
                totalCount,
                page,
                size,
                totalPages
        );
    }

    // 급여명세서 기본정보 조회
    @Override
    public Optional<PayrollStatementResponseDTO> selectPayrollStatement(
            String empNo,
            String payMonth
    ) {

        PayrollStatementResponseDTO result = queryFactory
                .select(Projections.fields(
                        PayrollStatementResponseDTO.class,

                        empEntity.empNo.as("empNo"),
                        empEntity.name.as("empName"),

                        departmentEntity.deptName.as("deptName"),
                        departmentEntity.parentDept.deptName.as("parentDeptName"),

                        positionEntity.positionName.as("positionName"),

                        gradeCodeEntity.gradeId.as("gradeId"),
                        gradeCodeEntity.description.as("gradeDescription"),

                        empEntity.status.as("empStatus"),
                        empEntity.hireDate.as("hireDate"),
                        empEntity.bank.as("bank"),
                        empEntity.accountNo.as("accountNo"),

                        payrollEntity.payrollId.as("payrollId"),
                        payrollEntity.status.stringValue().as("payrollStatus"),
                        payrollEntity.payDate.as("payDate"),

                        payrollEntity.baseSalary.as("baseSalary"),
                        payrollEntity.totalGross.as("totalGross"),
                        payrollEntity.totalDeduction.as("totalDeduction"),
                        payrollEntity.netSalary.as("netSalary"),

                        payrollEntity.taxableIncome.as("taxableIncome"),

                        payrollEntity.nationalPensionAmount.as("nationalPensionAmount"),
                        payrollEntity.healthInsuranceAmount.as("healthInsuranceAmount"),
                        payrollEntity.longTermCareAmount.as("longTermCareAmount"),
                        payrollEntity.employmentInsuranceAmount.as("employmentInsuranceAmount"),
                        payrollEntity.totalInsurance.as("totalInsurance"),

                        payrollEntity.incomeTax.as("incomeTax"),
                        payrollEntity.localIncomeTax.as("localIncomeTax")
                ))
                .from(empEntity)
                .leftJoin(empEntity.department, departmentEntity)
                .leftJoin(empEntity.position, positionEntity)
                .leftJoin(empEntity.grade, gradeCodeEntity)
                .leftJoin(payrollEntity).on(
                        payrollEntity.employee.empNo.eq(empEntity.empNo),
                        payrollEntity.payMonth.eq(payMonth)
                )
                .where(
                        empEntity.empNo.eq(empNo),
                        empEntity.isDeleted.eq("N")
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    // 급여명세서 지급/공제항목 조회
    @Override
    public List<PayrollStatementResponseDTO.Item> selectPayrollStatementItems(
            Integer payrollId
    ) {

        return queryFactory
                .select(Projections.fields(
                        PayrollStatementResponseDTO.Item.class,

                        payrollItemEntity.itemNameSnapshot.as("itemName"),
                        payrollItemEntity.itemType.as("itemType"),
                        payrollItemEntity.amount.as("amount"),
                        payrollItemEntity.taxType.as("taxType"),
                        payrollItemEntity.nonTaxCode.as("nonTaxCode"),

                        // Entity 추가 없이 설정 테이블에 남아있는 근태연동 유형만 참고한다.
                        payrollItemEntity.itemSetting.linkedAttendanceType.as("linkedAttendanceType")
                ))
                .from(payrollItemEntity)
                .leftJoin(payrollItemEntity.itemSetting)
                .where(payrollItemEntity.payroll.payrollId.eq(payrollId))
                .orderBy(payrollItemEntity.payrollItemId.asc())
                .fetch();
    }

    // 급여요약 정렬 조건
    private OrderSpecifier<?>[] getPayrollSummaryOrder(String sortType) {

        List<OrderSpecifier<?>> orders = new ArrayList<>();

        // 기본순(상태 우선): NEW → DRAFT → CONFIRMED → PAID
        NumberExpression<Integer> statusOrder = new CaseBuilder()
                .when(payrollEntity.payrollId.isNull()).then(1)
                .when(payrollEntity.status.eq(PayrollStatus.DRAFT)).then(2)
                .when(payrollEntity.status.eq(PayrollStatus.CONFIRMED)).then(3)
                .when(payrollEntity.status.eq(PayrollStatus.PAID)).then(4)
                .otherwise(5);

        if (!StringUtils.hasText(sortType) || "DEFAULT".equals(sortType)) {
            orders.add(statusOrder.asc());
            orders.add(empEntity.empNo.asc());

            return orders.toArray(new OrderSpecifier[0]);
        }

        if ("EMP_NO".equals(sortType)) {
            orders.add(empEntity.empNo.asc());

            return orders.toArray(new OrderSpecifier[0]);
        }

        if ("NAME".equals(sortType)) {
            orders.add(empEntity.name.asc());
            orders.add(empEntity.empNo.asc());

            return orders.toArray(new OrderSpecifier[0]);
        }

        if ("NET_DESC".equals(sortType)) {
            orders.add(payrollEntity.netSalary.desc().nullsLast());
            orders.add(empEntity.empNo.asc());

            return orders.toArray(new OrderSpecifier[0]);
        }

        if ("NET_ASC".equals(sortType)) {
            orders.add(payrollEntity.netSalary.asc().nullsLast());
            orders.add(empEntity.empNo.asc());

            return orders.toArray(new OrderSpecifier[0]);
        }

        orders.add(statusOrder.asc());
        orders.add(empEntity.empNo.asc());

        return orders.toArray(new OrderSpecifier[0]);
    }
}

