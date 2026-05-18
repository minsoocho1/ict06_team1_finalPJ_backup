package com.ict06.team1_fin_pj.domain.payroll.repository;

import com.ict06.team1_fin_pj.common.dto.payroll.*;
import com.ict06.team1_fin_pj.domain.payroll.entity.PayrollStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.ict06.team1_fin_pj.domain.employee.entity.QEmpEntity.empEntity;
import static com.ict06.team1_fin_pj.domain.employee.entity.QDepartmentEntity.departmentEntity;
import static com.ict06.team1_fin_pj.domain.employee.entity.QPositionEntity.positionEntity;
import static com.ict06.team1_fin_pj.domain.payroll.entity.QGradeCodeEntity.gradeCodeEntity;
import static com.ict06.team1_fin_pj.domain.payroll.entity.QPayrollEntity.payrollEntity;
import static com.ict06.team1_fin_pj.domain.payroll.entity.QSalaryPolicyEntity.salaryPolicyEntity;
import static com.ict06.team1_fin_pj.domain.payroll.entity.QPayItemSettingEntity.payItemSettingEntity;
import static com.ict06.team1_fin_pj.domain.payroll.entity.QPayrollItemEntity.payrollItemEntity;
import static com.ict06.team1_fin_pj.domain.attendance.entity.QAttendanceEntity.attendanceEntity;

// 급여대장 QueryDSL 구현
@Repository
@RequiredArgsConstructor
public class PayrollRepositoryCustomImpl implements PayrollRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // 사원 검색 autocomplete
    @Override
    public List<PayrollEmployeeSearchResponseDTO> searchEmployees(PayrollEmployeeSearchDTO searchDTO) {

        BooleanBuilder builder = new BooleanBuilder();

        String keyword = searchDTO.getKeyword().trim();

        if ("EMP_NO".equals(searchDTO.getSearchType())) {
            builder.and(empEntity.empNo.contains(keyword));
        }

        if ("NAME".equals(searchDTO.getSearchType())) {
            builder.and(empEntity.name.containsIgnoreCase(keyword));
        }

        builder.and(empEntity.isDeleted.eq("N"));

        var query = queryFactory
                .select(Projections.fields(
                        PayrollEmployeeSearchResponseDTO.class,

                        empEntity.empId.as("empId"),
                        empEntity.empNo.as("empNo"),
                        empEntity.name.as("empName"),
                        departmentEntity.deptName.as("deptName"),
                        departmentEntity.parentDept.deptName.as("parentDeptName"),
                        positionEntity.positionName.as("positionName"),
                        gradeCodeEntity.gradeId.as("gradeId")
                ))
                .from(empEntity)
                .leftJoin(empEntity.department, departmentEntity)
                .leftJoin(empEntity.position, positionEntity)
                .leftJoin(empEntity.grade, gradeCodeEntity)
                .where(builder)
                .orderBy(empEntity.empNo.asc());

        if (!Boolean.TRUE.equals(searchDTO.getShowAll())) {
            query.limit(searchDTO.getLimit());
        }

        return query.fetch();
    }

    // 사원 인사정보 조회
    @Override
    public Optional<PayrollEmployeeInfoResponseDTO> selectEmployeeInfo(String empNo) {

        PayrollEmployeeInfoResponseDTO result = queryFactory
                .select(Projections.fields(
                        PayrollEmployeeInfoResponseDTO.class,

                        empEntity.empId.as("empId"),
                        empEntity.empNo.as("empNo"),
                        empEntity.name.as("empName"),
                        departmentEntity.deptName.as("deptName"),
                        departmentEntity.parentDept.deptName.as("parentDeptName"),
                        departmentEntity.deptId.as("deptId"),
                        positionEntity.positionName.as("positionName"),
                        positionEntity.positionId.as("positionId"),
                        gradeCodeEntity.gradeId.as("gradeId"),
                        gradeCodeEntity.description.as("gradeDescription"),
                        empEntity.hireDate.as("hireDate"),
                        empEntity.status.as("status"),
                        empEntity.bank.as("bank"),
                        empEntity.accountNo.as("accountNo")
                ))
                .from(empEntity)
                .leftJoin(empEntity.department, departmentEntity)
                .leftJoin(empEntity.position, positionEntity)
                .leftJoin(empEntity.grade, gradeCodeEntity)
                .where(
                        empEntity.empNo.eq(empNo),
                        empEntity.isDeleted.eq("N")
                )
                .fetchOne();

        /**
         * 재직상태 코드 → 화면 표시명 변환
         *
         * 현재 EMPLOYEE.status 값이 숫자 코드로 저장되어 있어
         * 화면에 그대로 뿌리면 1, 2, 3처럼 표시된다.
         *
         * 화면에서는 한글 상태명으로 보여주기 위해
         * 조회 후 DTO에 statusName을 세팅한다.
         */
        if (result != null) {

            String status = result.getStatus();

            if ("1".equals(status)) {
                result.setStatusName("재직");
            } else if ("2".equals(status)) {
                result.setStatusName("휴직");
            } else if ("3".equals(status)) {
                result.setStatusName("퇴사");
            } else {
                result.setStatusName(status);
            }
        }


        return Optional.ofNullable(result);
    }

    // 급여대장 상태 조회
    @Override
    public Optional<PayrollStatusResponseDTO> selectPayrollStatus(String empNo, String payMonth) {

        PayrollStatusResponseDTO result = queryFactory
                .select(Projections.fields(
                        PayrollStatusResponseDTO.class,

                        payrollEntity.payrollId.longValue().as("payrollId"),
                        payrollEntity.status.stringValue().as("payrollStatus"),
                        payrollEntity.baseSalary.as("baseSalary"),
                        payrollEntity.payDate.stringValue().as("payDate"),
                        payrollEntity.nationalPensionAmount.as("nationalPensionAmount"),
                        payrollEntity.healthInsuranceAmount.as("healthInsuranceAmount"),
                        payrollEntity.longTermCareAmount.as("longTermCareAmount"),
                        payrollEntity.employmentInsuranceAmount.as("employmentInsuranceAmount"),
                        payrollEntity.totalInsurance.as("totalInsurance")
                ))
                .from(payrollEntity)
                .where(
                        payrollEntity.employee.empNo.eq(empNo),
                        payrollEntity.payMonth.eq(payMonth)
                )
                .fetchOne();

        if (result == null) {
            return Optional.empty();
        }

        setStatusInfo(result);

        return Optional.of(result);
    }

    // 기존 저장 급여대장의 기본급 조회
    // - DRAFT/CONFIRMED/PAID 상태에서 저장된 PAYROLL.baseSalary를 가져온다.
    // - DRAFT 상태에서는 현재 정책 수정일과 PAYROLL 수정일을 비교하기 위해 payrollUpdatedAt도 함께 가져온다.
    @Override
    public Optional<PayrollBaseSalaryResponseDTO> selectSavedPayrollBaseSalary(String empNo, String payMonth) {

        PayrollBaseSalaryResponseDTO result = queryFactory
                .select(Projections.fields(
                        PayrollBaseSalaryResponseDTO.class,

                        // 저장된 기본급
                        payrollEntity.baseSalary.as("baseSalary"),

                        // 기존 저장 기본급 비교용
                        payrollEntity.baseSalary.as("savedBaseSalary"),

                        // 저장 당시 급여등급
                        payrollEntity.grade.gradeId.as("gradeId"),

                        // DRAFT 정책 변경 감지용
                        payrollEntity.updatedAt.as("payrollUpdatedAt")
                ))
                .from(payrollEntity)
                .where(
                        payrollEntity.employee.empNo.eq(empNo),
                        payrollEntity.payMonth.eq(payMonth)
                )
                .fetchOne();

        if (result == null) {
            return Optional.empty();
        }

        result.setSalarySource("SAVED");
        result.setPolicyExists(true);
        result.setPolicyChanged(false);
        result.setWarningRequired(false);
        result.setPolicyDecisionRequired(false);
        result.setPolicyDecisionCompleted(false);
        result.setWarningMessage(null);

        return Optional.of(result);
    }

    // 이전 확정/지급완료 기본급 조회
    @Override
    public Optional<PayrollBaseSalaryResponseDTO> selectRecentConfirmedBaseSalary(String empNo, String payMonth) {

        PayrollBaseSalaryResponseDTO result = queryFactory
                .select(Projections.fields(
                        PayrollBaseSalaryResponseDTO.class,

                        payrollEntity.baseSalary.as("baseSalary"),
                        payrollEntity.grade.gradeId.as("gradeId")
                ))
                .from(payrollEntity)
                .where(
                        payrollEntity.employee.empNo.eq(empNo),
                        payrollEntity.payMonth.lt(payMonth),
                        payrollEntity.status.in(PayrollStatus.CONFIRMED, PayrollStatus.PAID)
                )
                .orderBy(payrollEntity.payMonth.desc())
                .limit(1)
                .fetchOne();

        if (result == null) {
            return Optional.empty();
        }

        result.setSalarySource("RECENT_CONFIRMED");
        result.setPolicyExists(true);
        result.setPolicyChanged(false);
        result.setWarningMessage(null);

        return Optional.of(result);
    }

    // 현재 기본급 정책 조회
    // - 저장 당시 PAYROLL 기준이 아니라 현재 EMPLOYEE의 부서/직급/급여등급 기준으로 조회한다.
    @Override
    public Optional<PayrollBaseSalaryResponseDTO> selectCurrentSalaryPolicyBaseSalary(String empNo) {

        PayrollBaseSalaryResponseDTO result = queryFactory
                .select(Projections.fields(
                        PayrollBaseSalaryResponseDTO.class,

                        // 현재 기준 정책 기본급
                        salaryPolicyEntity.basicSalary.as("baseSalary"),

                        // DRAFT 경고 비교 표시용
                        salaryPolicyEntity.basicSalary.as("policyBaseSalary"),

                        // 현재 기준 급여등급
                        salaryPolicyEntity.grade.gradeId.as("gradeId"),

                        // 정책 변경 감지용
                        salaryPolicyEntity.createdAt.as("policyCreatedAt"),
                        salaryPolicyEntity.updatedAt.as("policyUpdatedAt")
                ))
                .from(empEntity)
                .join(empEntity.department, departmentEntity)
                .join(empEntity.position, positionEntity)
                .join(empEntity.grade, gradeCodeEntity)
                .join(salaryPolicyEntity).on(
                        salaryPolicyEntity.department.deptId.eq(departmentEntity.deptId),
                        salaryPolicyEntity.position.positionId.eq(positionEntity.positionId),
                        salaryPolicyEntity.grade.gradeId.eq(gradeCodeEntity.gradeId),
                        salaryPolicyEntity.isActive.isTrue()
                )
                .where(
                        empEntity.empNo.eq(empNo),
                        empEntity.isDeleted.eq("N")
                )
                .fetchOne();

        if (result == null) {
            return Optional.empty();
        }

        result.setSalarySource("POLICY");
        result.setPolicyExists(true);
        result.setPolicyChanged(false);
        result.setWarningRequired(false);
        result.setPolicyDecisionRequired(false);
        result.setPolicyDecisionCompleted(false);
        result.setWarningMessage(null);

        return Optional.of(result);
    }

    // 현재 활성 지급/공제항목 설정 조회
// - NEW 상태와 급여대장 메인 최초 진입 시 기본 항목으로 사용한다.
    @Override
    public List<PayrollItemLoadResponseDTO.Item> selectCurrentPayItemSettings() {

        return queryFactory
                .select(Projections.fields(
                        PayrollItemLoadResponseDTO.Item.class,

                        payItemSettingEntity.itemSettingId.as("itemSettingId"),
                        payItemSettingEntity.itemName.as("itemNameSnapshot"),
                        payItemSettingEntity.itemType.as("itemType"),
                        payItemSettingEntity.taxType.as("taxType"),
                        payItemSettingEntity.nonTaxCode.as("nonTaxCode"),
                        payItemSettingEntity.linkedAttendanceType.as("linkedAttendanceType")
                ))
                .from(payItemSettingEntity)
                .where(payItemSettingEntity.isActive.isTrue())
                .orderBy(payItemSettingEntity.itemSettingId.asc())
                .fetch();
    }

    // 저장된 급여대장의 지급/공제항목 조회
    // - DRAFT/CONFIRMED/PAID는 저장 당시 PAYROLL_ITEM snapshot을 우선 보여준다.
    @Override
    public List<PayrollItemLoadResponseDTO.Item> selectSavedPayrollItems(String empNo, String payMonth) {

        return queryFactory
                .select(Projections.fields(
                        PayrollItemLoadResponseDTO.Item.class,

                        payrollItemEntity.itemSetting.itemSettingId.as("itemSettingId"),
                        payrollItemEntity.itemNameSnapshot.as("itemNameSnapshot"),
                        payrollItemEntity.itemType.as("itemType"),
                        payrollItemEntity.amount.as("amount"),
                        payrollItemEntity.taxType.as("taxType"),
                        payrollItemEntity.nonTaxCode.as("nonTaxCode"),

                        // 엔티티 수정 없이 현재 설정 기준 근태연동 유형을 참고한다.
                        payrollItemEntity.itemSetting.linkedAttendanceType.as("linkedAttendanceType")
                ))
                .from(payrollItemEntity)
                .join(payrollItemEntity.payroll, payrollEntity)
                .leftJoin(payrollItemEntity.itemSetting, payItemSettingEntity)
                .where(
                        payrollEntity.employee.empNo.eq(empNo),
                        payrollEntity.payMonth.eq(payMonth)
                )
                .orderBy(payrollItemEntity.payrollItemId.asc())
                .fetch();
    }

    // 현재 활성 지급/공제항목 설정의 최신 수정일 조회
// - DRAFT의 PAYROLL.updatedAt과 비교하여 항목 설정 변경 여부를 판단한다.
    @Override
    public java.time.LocalDateTime selectLatestPayItemSettingUpdatedAt() {

        return queryFactory
                .select(payItemSettingEntity.updatedAt.max())
                .from(payItemSettingEntity)
                .where(payItemSettingEntity.isActive.isTrue())
                .fetchOne();
    }

    @Override
    public PayrollAttendanceSummaryDTO selectAttendanceSummary(String empNo, LocalDate startDate, LocalDate endDate) {

        /*
         * 전자결재 승인 완료 후 ATTENDANCE에 반영된 값만 사용한다.
         * - ATTENDANCE.overtimeMins = 승인 반영된 연장근무 분
         * - ATTENDANCE.status = 승인 반영된 근태 상태
         */

        Integer overtimeMinutes = queryFactory
                .select(attendanceEntity.overtimeMins.sum())
                .from(attendanceEntity)
                .where(
                        attendanceEntity.employee.empNo.eq(empNo),
                        attendanceEntity.workDate.between(startDate, endDate)
                )
                .fetchOne();

        Long absenceDays = queryFactory
                .select(attendanceEntity.count())
                .from(attendanceEntity)
                .where(
                        attendanceEntity.employee.empNo.eq(empNo),
                        attendanceEntity.workDate.between(startDate, endDate),
                        attendanceEntity.status.stringValue().eq("ABSENT")
                )
                .fetchOne();

        return PayrollAttendanceSummaryDTO.builder()
                .overtimeMinutes(overtimeMinutes == null ? 0 : overtimeMinutes)
                .absenceDays(absenceDays == null ? 0 : absenceDays.intValue())
                .workingDays(0)
                .build();
    }

    // 상태명과 버튼 상태 세팅
    private void setStatusInfo(PayrollStatusResponseDTO result) {

        if ("DRAFT".equals(result.getPayrollStatus())) {
            result.setPayrollStatusName("작성중");
            result.setEditable(true);
            result.setDeletable(true);
            result.setPreviewAvailable(true);
            result.setConfirmAvailable(false);
            result.setPayConfirmAvailable(false);
            result.setResetAvailable(true);
        }

        if ("CONFIRMED".equals(result.getPayrollStatus())) {
            result.setPayrollStatusName("확정");
            result.setEditable(false);
            result.setDeletable(false);
            result.setPreviewAvailable(true);
            result.setConfirmAvailable(false);
            result.setPayConfirmAvailable(true);
            result.setResetAvailable(false);
        }

        if ("PAID".equals(result.getPayrollStatus())) {
            result.setPayrollStatusName("지급완료");
            result.setEditable(false);
            result.setDeletable(false);
            result.setPreviewAvailable(true);
            result.setConfirmAvailable(false);
            result.setPayConfirmAvailable(false);
            result.setResetAvailable(false);
        }
    }
}
