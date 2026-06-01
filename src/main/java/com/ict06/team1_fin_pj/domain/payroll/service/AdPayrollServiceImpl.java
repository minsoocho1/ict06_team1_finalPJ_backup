package com.ict06.team1_fin_pj.domain.payroll.service;

import com.ict06.team1_fin_pj.common.dto.payroll.*;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.payroll.config.PayrollRateConstants;
import com.ict06.team1_fin_pj.domain.payroll.entity.*;
import com.ict06.team1_fin_pj.domain.payroll.repository.PayrollRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 관리자용 급여대장 서비스 구현
@Service
@RequiredArgsConstructor
public class AdPayrollServiceImpl implements AdPayrollService  {

    private final PayrollRepository payrollRepository;
    private final EntityManager entityManager;


    // 사원 검색 autocomplete
    @Override
    @Transactional(readOnly = true)
    public List<PayrollEmployeeSearchResponseDTO> searchEmployees(PayrollEmployeeSearchDTO searchDTO) {

        validateEmployeeSearch(searchDTO);

        return payrollRepository.searchEmployees(searchDTO);
    }

    // 사원 인사정보 조회
    @Override
    @Transactional(readOnly = true)
    public PayrollEmployeeInfoResponseDTO getEmployeeInfo(String empNo) {

        validateEmpNo(empNo);

        return payrollRepository.selectEmployeeInfo(empNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사원입니다."));
    }

    // 급여대장 상태 조회
    @Override
    @Transactional(readOnly = true)
    public PayrollStatusResponseDTO getPayrollStatus(PayrollMainRequestDTO requestDTO) {

        validateMainRequest(requestDTO);

        String payMonth = makePayMonth(requestDTO.getPayYear(), requestDTO.getPayMonth());

        return payrollRepository.selectPayrollStatus(requestDTO.getEmpNo(), payMonth)
                .orElseGet(() -> PayrollStatusResponseDTO.builder()
                        .payrollId(null)
                        .payrollStatus("NEW")
                        .payrollStatusName("미작성")
                        .payDate(null)
                        .editable(true)
                        .deletable(false)
                        .previewAvailable(true)
                        .confirmAvailable(false)
                        .payConfirmAvailable(false)
                        .resetAvailable(true)
                        .build());
    }

    // 기본급 자동 로딩
    @Override
    @Transactional(readOnly = true)
    public PayrollBaseSalaryResponseDTO getBaseSalary(PayrollMainRequestDTO requestDTO) {

        // 사번, 작성년도, 작성월 검증
        validateMainRequest(requestDTO);

        // PAYROLL.payMonth 형식인 YYYY-MM 생성
        String payMonth = makePayMonth(requestDTO.getPayYear(), requestDTO.getPayMonth());

        // 현재 급여대장 상태 조회
        PayrollStatusResponseDTO statusDTO = getPayrollStatus(requestDTO);

        /*
         * 현재 사원의 "현재 부서 + 현재 직급 + 현재 급여등급" 기준 기본급 정책 조회
         *
         * 중요:
         * - 저장 당시 PAYROLL.grade 기준으로 정책을 찾지 않는다.
         * - 작성중(DRAFT)이라도 현재 사원이 G1에서 G2로 바뀌었으면,
         *   G2 + 현재 부서 + 현재 직급 기준 정책을 다시 조회한다.
         */
        PayrollBaseSalaryResponseDTO currentPolicySalary =
                payrollRepository.selectCurrentSalaryPolicyBaseSalary(requestDTO.getEmpNo())
                        .orElse(null);

        /*
         * 1. 기존 급여대장이 있는 경우
         * - DRAFT / CONFIRMED / PAID
         * - 기본급은 우선 저장된 PAYROLL.baseSalary를 사용한다.
         */
        if (!"NEW".equals(statusDTO.getPayrollStatus())) {

            PayrollBaseSalaryResponseDTO savedSalary =
                    payrollRepository.selectSavedPayrollBaseSalary(requestDTO.getEmpNo(), payMonth)
                            .orElseGet(() -> PayrollBaseSalaryResponseDTO.builder()
                                    .baseSalary(statusDTO.getBaseSalary())
                                    .savedBaseSalary(statusDTO.getBaseSalary())
                                    .salarySource("SAVED")
                                    .policyExists(true)
                                    .policyChanged(false)
                                    .warningRequired(false)
                                    .policyDecisionRequired(false)
                                    .policyDecisionCompleted(false)
                                    .warningMessage(null)
                                    .build());

            savedSalary.setSalarySource("SAVED");
            savedSalary.setSavedBaseSalary(savedSalary.getBaseSalary());

            /*
             * CONFIRMED / PAID 상태에서는 정책 경고를 띄우지 않는다.
             * 이미 확정 또는 지급완료된 급여이므로 현재 정책 변경과 무관하게 당시 값을 보존한다.
             */
            if ("CONFIRMED".equals(statusDTO.getPayrollStatus())
                    || "PAID".equals(statusDTO.getPayrollStatus())) {

                savedSalary.setPolicyChanged(false);
                savedSalary.setWarningRequired(false);
                savedSalary.setPolicyDecisionRequired(false);
                savedSalary.setPolicyDecisionCompleted(true);
                savedSalary.setWarningMessage(null);

                return savedSalary;
            }

            /*
             * 2. DRAFT 상태 정책 경고 판단
             *
             * 기준:
             * - 현재 사원의 현재 부서/직급/급여등급 기준 정책이 없으면 경고
             * - 현재 기준 정책이 있고,
             *   currentPolicy.updatedAt > savedPayroll.updatedAt 이면 경고
             */
            if ("DRAFT".equals(statusDTO.getPayrollStatus())) {

                // 현재 기준 기본급 정책이 없거나 삭제/비활성화된 경우
                if (currentPolicySalary == null || currentPolicySalary.getBaseSalary() == null) {
                    savedSalary.setPolicyExists(false);
                    savedSalary.setPolicyChanged(false);
                    savedSalary.setWarningRequired(true);
                    savedSalary.setPolicyDecisionRequired(false);
                    savedSalary.setPolicyDecisionCompleted(false);
                    savedSalary.setWarningMessage("현재 사원의 기본급 정책이 설정되어 있지 않거나 삭제되었습니다. 기본급 관리에서 확인해주세요.");
                    return savedSalary;
                }

                // 현재 정책 기본급 비교 표시용
                savedSalary.setPolicyBaseSalary(currentPolicySalary.getBaseSalary());
                savedSalary.setPolicyUpdatedAt(currentPolicySalary.getPolicyUpdatedAt());

                /*
                 * 현재 정책이 새로 등록된 경우
                 *
                 * 기준:
                 * - createdAt == updatedAt
                 * - 정책 생성일이 저장된 DRAFT 이후
                 *
                 * 즉:
                 * 저장 당시에는 정책이 없었고
                 * 이후 새 정책이 등록된 상황
                 */
                if (currentPolicySalary.getPolicyCreatedAt() != null
                        && currentPolicySalary.getPolicyUpdatedAt() != null
                        && savedSalary.getPayrollUpdatedAt() != null
                        && currentPolicySalary.getPolicyCreatedAt()
                        .isEqual(currentPolicySalary.getPolicyUpdatedAt())
                        && currentPolicySalary.getPolicyCreatedAt()
                        .isAfter(savedSalary.getPayrollUpdatedAt())) {

                    savedSalary.setPolicyExists(true);
                    savedSalary.setPolicyChanged(false);
                    savedSalary.setWarningRequired(true);
                    savedSalary.setPolicyDecisionRequired(true);
                    savedSalary.setPolicyDecisionCompleted(false);

                    savedSalary.setWarningMessage(
                            "새 기본급 정책이 등록되었습니다. 등록된 기본급 정책을 적용하시겠습니까?"
                    );

                    return savedSalary;
                }

                // 현재 기준 정책이 저장된 DRAFT 이후에 수정된 경우
                if (currentPolicySalary.getPolicyUpdatedAt() != null
                        && savedSalary.getPayrollUpdatedAt() != null
                        && currentPolicySalary.getPolicyUpdatedAt().isAfter(savedSalary.getPayrollUpdatedAt())) {

                    savedSalary.setPolicyExists(true);
                    savedSalary.setPolicyChanged(true);
                    savedSalary.setWarningRequired(true);
                    savedSalary.setPolicyDecisionRequired(true);
                    savedSalary.setPolicyDecisionCompleted(false);
                    savedSalary.setWarningMessage("기본급 정책이 변경되었습니다. 변경된 기본급 정책을 적용하시겠습니까?");
                    return savedSalary;
                }

                // 현재 기준 정책도 있고, 저장 이후 정책 변경도 없는 경우
                savedSalary.setPolicyExists(true);
                savedSalary.setPolicyChanged(false);
                savedSalary.setWarningRequired(false);
                savedSalary.setPolicyDecisionRequired(false);
                savedSalary.setPolicyDecisionCompleted(true);
                savedSalary.setWarningMessage(null);

                return savedSalary;
            }

            return savedSalary;
        }

        /*
         * 3. NEW 상태
         * - 아직 저장된 급여대장이 없으므로 자동 기본급 로딩 규칙을 적용한다.
         */

        // 선택 지급월보다 과거인 CONFIRMED/PAID 중 가장 최근 기본급 조회
        PayrollBaseSalaryResponseDTO recentSalary =
                payrollRepository.selectRecentConfirmedBaseSalary(requestDTO.getEmpNo(), payMonth)
                        .orElse(null);

        /*
         * 3-1. 과거 확정/지급완료 기본급이 없는 경우
         * - 현재 정책 기본급 사용
         * - 현재 정책도 없으면 직접입력
         */
        if (recentSalary == null || recentSalary.getBaseSalary() == null) {

            if (currentPolicySalary != null && currentPolicySalary.getBaseSalary() != null) {
                currentPolicySalary.setSalarySource("POLICY");
                currentPolicySalary.setPolicyExists(true);
                currentPolicySalary.setPolicyChanged(false);
                currentPolicySalary.setWarningRequired(false);
                currentPolicySalary.setPolicyDecisionRequired(false);
                currentPolicySalary.setPolicyDecisionCompleted(true);
                currentPolicySalary.setWarningMessage(null);
                return currentPolicySalary;
            }

            return PayrollBaseSalaryResponseDTO.builder()
                    .baseSalary(null)
                    .salarySource("MANUAL")
                    .policyExists(false)
                    .policyChanged(false)
                    .warningRequired(true)
                    .policyDecisionRequired(false)
                    .policyDecisionCompleted(true)
                    .warningMessage("현재 사원의 기본급 정책이 설정되어 있지 않거나 삭제되었습니다. 기본급을 직접 입력해주세요.")
                    .build();
        }

        /*
         * 3-2. 과거 확정/지급완료 기본급이 있는 경우
         * - 현재 사원의 현재 grade와 과거 확정 급여 grade를 비교한다.
         */
        PayrollEmployeeInfoResponseDTO employeeInfo = getEmployeeInfo(requestDTO.getEmpNo());

        int recentGradeOrder = getGradeOrder(recentSalary.getGradeId());
        int currentGradeOrder = getGradeOrder(employeeInfo.getGradeId());

        /*
         * 현재 정책이 없는 경우
         * - 동일 등급 또는 승진이면 과거 확정 기본급 사용
         * - 강등이면 과거 높은 기본급을 그대로 쓰면 위험하므로 직접입력
         */
        if (currentPolicySalary == null || currentPolicySalary.getBaseSalary() == null) {

            if (currentGradeOrder >= recentGradeOrder) {
                recentSalary.setSalarySource("RECENT_CONFIRMED");
                recentSalary.setPolicyExists(false);
                recentSalary.setPolicyChanged(false);
                recentSalary.setWarningRequired(true);
                recentSalary.setPolicyDecisionRequired(false);
                recentSalary.setPolicyDecisionCompleted(true);
                recentSalary.setWarningMessage("현재 사원의 기본급 정책이 설정되어 있지 않거나 삭제되었습니다. 최근 확정 급여의 기본급을 불러왔습니다.");
                return recentSalary;
            }

            return PayrollBaseSalaryResponseDTO.builder()
                    .baseSalary(null)
                    .salarySource("MANUAL")
                    .policyExists(false)
                    .policyChanged(false)
                    .warningRequired(true)
                    .policyDecisionRequired(false)
                    .policyDecisionCompleted(true)
                    .warningMessage("현재 사원의 기본급 정책이 설정되어 있지 않거나 삭제되었습니다. 기본급을 직접 입력해주세요.")
                    .gradeId(employeeInfo.getGradeId())
                    .build();
        }

        // 현재 정책 기본급 비교 표시용
        recentSalary.setPolicyBaseSalary(currentPolicySalary.getBaseSalary());
        recentSalary.setPolicyUpdatedAt(currentPolicySalary.getPolicyUpdatedAt());

        /*
         * 등급 동일
         * - 연봉협상/수동조정 가능성이 있으므로 과거 확정 기본급 유지
         */
        if (currentGradeOrder == recentGradeOrder) {
            recentSalary.setSalarySource("RECENT_CONFIRMED");
            recentSalary.setPolicyExists(true);
            recentSalary.setPolicyChanged(false);
            recentSalary.setWarningRequired(false);
            recentSalary.setPolicyDecisionRequired(false);
            recentSalary.setPolicyDecisionCompleted(true);
            recentSalary.setWarningMessage(null);
            return recentSalary;
        }

        /*
         * 승진
         * - 현재 정책 기본급과 과거 확정 기본급 중 큰 금액 사용
         */
        if (currentGradeOrder > recentGradeOrder) {

            if (currentPolicySalary.getBaseSalary().compareTo(recentSalary.getBaseSalary()) >= 0) {
                currentPolicySalary.setSalarySource("PROMOTION_POLICY");
                currentPolicySalary.setPolicyExists(true);
                currentPolicySalary.setPolicyChanged(false);
                currentPolicySalary.setWarningRequired(false);
                currentPolicySalary.setPolicyDecisionRequired(false);
                currentPolicySalary.setPolicyDecisionCompleted(true);
                currentPolicySalary.setWarningMessage("직급/급여등급 변경으로 현재 기본급 정책을 적용했습니다.");
                return currentPolicySalary;
            }

            recentSalary.setSalarySource("PROMOTION_RECENT_HIGHER");
            recentSalary.setPolicyExists(true);
            recentSalary.setPolicyChanged(false);
            recentSalary.setWarningRequired(false);
            recentSalary.setPolicyDecisionRequired(false);
            recentSalary.setPolicyDecisionCompleted(true);
            recentSalary.setWarningMessage("승진 후 기본급 정책보다 최근 확정 기본급이 높아 기존 기본급을 유지했습니다.");
            return recentSalary;
        }

        /*
         * 강등
         * - 현재 정책 기본급 적용
         */
        currentPolicySalary.setSalarySource("DEMOTION_POLICY");
        currentPolicySalary.setPolicyExists(true);
        currentPolicySalary.setPolicyChanged(false);
        currentPolicySalary.setWarningRequired(false);
        currentPolicySalary.setPolicyDecisionRequired(false);
        currentPolicySalary.setPolicyDecisionCompleted(true);
        currentPolicySalary.setWarningMessage("직급/급여등급 변경으로 현재 기본급 정책을 적용했습니다.");

        return currentPolicySalary;
    }

    // 급여대장 저장
    @Override
    @Transactional
    public String savePayroll(PayrollSaveRequestDTO requestDTO) {

        // 저장 요청 검증
        validatePayrollSaveRequest(requestDTO);

        String payMonth = makePayMonth(requestDTO.getPayYear(), requestDTO.getPayMonth());
        BigDecimal zero = BigDecimal.ZERO;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        PayrollEntity payroll = payrollRepository
                .findByEmployee_EmpNoAndPayMonth(requestDTO.getEmpNo(), payMonth)
                .orElse(null);

        // 확정/지급완료는 수정 불가
        if (payroll != null) {
            if (PayrollStatus.CONFIRMED.equals(payroll.getStatus())
                    || PayrollStatus.PAID.equals(payroll.getStatus())) {
                throw new IllegalStateException("확정 또는 지급완료 상태의 급여대장은 수정할 수 없습니다.");
            }
        }

        PayrollEmployeeInfoResponseDTO employeeInfo = getEmployeeInfo(requestDTO.getEmpNo());

        EmpEntity employee = entityManager.getReference(EmpEntity.class, requestDTO.getEmpNo());

        GradeCodeEntity grade = null;
        if (StringUtils.hasText(employeeInfo.getGradeId())) {
            grade = entityManager.getReference(GradeCodeEntity.class, employeeInfo.getGradeId());
        }

        /*
         * 저장 snapshot 계산
         *
         * 저장 직후 근태연동 변경 경고가 다시 뜨지 않게
         * 저장 시점 계산값 snapshot 생성
         */
        PayrollPreviewResponseDTO previewForSnapshot =
                calculatePayrollPreview(
                        requestDTO,
                        employeeInfo,
                        payMonth
                );

        // NEW 저장
        if (payroll == null) {

            payroll = PayrollEntity.builder()
                    .employee(employee)
                    .grade(grade)
                    .payMonth(payMonth)

                    .familyCount(
                            requestDTO.getFamilyCount() == null
                                    ? 1
                                    : requestDTO.getFamilyCount()
                    )

                    .baseSalary(requestDTO.getBaseSalary())

                    .bonus(BigDecimal.ZERO)

                    .totalAllowance(previewForSnapshot.getTotalAllowance())
                    .totalGross(previewForSnapshot.getTotalGross())
                    .taxableIncome(previewForSnapshot.getTaxableIncome())

                    .incomeTax(previewForSnapshot.getIncomeTax())
                    .localIncomeTax(previewForSnapshot.getLocalIncomeTax())

                    .nationalPensionAmount(requestDTO.getNationalPensionAmount())
                    .healthInsuranceAmount(requestDTO.getHealthInsuranceAmount())
                    .longTermCareAmount(requestDTO.getLongTermCareAmount())
                    .employmentInsuranceAmount(requestDTO.getEmploymentInsuranceAmount())
                    .totalInsurance(requestDTO.getTotalInsurance())

                    .totalDeduction(previewForSnapshot.getTotalDeduction())
                    .netSalary(previewForSnapshot.getNetSalary())

                    .status(PayrollStatus.DRAFT)
                    .payDate(null)

                    .build();

            payrollRepository.save(payroll);
        } else {
            // 엔티티에 setter를 추가하지 않기 위해 JPQL update 사용
            entityManager.createQuery("""
                update PayrollEntity p
                   set p.grade = :grade,
                       p.familyCount = :familyCount,
                       p.baseSalary = :baseSalary,
                       p.status = :status,
                       p.bonus = :zero,
                                                
                       p.totalAllowance = :totalAllowance,
                       p.totalGross = :totalGross,
                       p.taxableIncome = :taxableIncome,
                        
                       p.incomeTax = :incomeTax,
                       p.localIncomeTax = :localIncomeTax,
                        
                       p.nationalPensionAmount = :nationalPensionAmount,
                       p.healthInsuranceAmount = :healthInsuranceAmount,
                       p.longTermCareAmount = :longTermCareAmount,
                       p.employmentInsuranceAmount = :employmentInsuranceAmount,
                       p.totalInsurance = :totalInsurance,
                       
                       p.totalDeduction = :totalDeduction,
                       p.netSalary = :netSalary,
                    
                       p.payDate = null,
                       p.updatedAt = :now
                 where p.payrollId = :payrollId
                """)
                    .setParameter("grade", grade)
                    .setParameter("nationalPensionAmount", requestDTO.getNationalPensionAmount())
                    .setParameter("healthInsuranceAmount", requestDTO.getHealthInsuranceAmount())
                    .setParameter("longTermCareAmount", requestDTO.getLongTermCareAmount())
                    .setParameter("employmentInsuranceAmount", requestDTO.getEmploymentInsuranceAmount())
                    .setParameter("totalInsurance", requestDTO.getTotalInsurance())

                    .setParameter("incomeTax", requestDTO.getIncomeTax())
                    .setParameter("localIncomeTax", requestDTO.getLocalIncomeTax())
                    .setParameter("totalDeduction", requestDTO.getTotalDeduction())
                    .setParameter("totalGross", requestDTO.getTotalGross())
                    .setParameter("netSalary", requestDTO.getNetSalary())
                    .setParameter("familyCount", requestDTO.getFamilyCount() == null ? 1 : requestDTO.getFamilyCount())
                    .setParameter("baseSalary", requestDTO.getBaseSalary())
                    .setParameter("status", PayrollStatus.DRAFT)
                    .setParameter("payrollId", payroll.getPayrollId())
                    .setParameter("zero", zero)
                    .setParameter("totalAllowance", previewForSnapshot.getTotalAllowance())
                    .setParameter("taxableIncome", previewForSnapshot.getTaxableIncome())
                    .setParameter("now", now)
                    .executeUpdate();
        }

        PayrollEntity savedPayroll = payrollRepository
                .findByEmployee_EmpNoAndPayMonth(requestDTO.getEmpNo(), payMonth)
                .orElseThrow(() -> new IllegalStateException("급여대장 저장 중 오류가 발생했습니다."));

        savePayrollItems(savedPayroll, requestDTO.getItems(), previewForSnapshot);

        return "급여대장이 저장되었습니다.";
    }

    // 지급/공제항목 조회
    @Override
    @Transactional(readOnly = true)
    public PayrollItemLoadResponseDTO getPayrollItems(PayrollMainRequestDTO requestDTO) {

        validateMainRequest(requestDTO);

        String payMonth = makePayMonth(requestDTO.getPayYear(), requestDTO.getPayMonth());

        PayrollStatusResponseDTO statusDTO = getPayrollStatus(requestDTO);

        PayrollItemLoadResponseDTO response = new PayrollItemLoadResponseDTO();

        /*
         * NEW 상태
         * - 저장된 PAYROLL_ITEM snapshot 없음
         * - 현재 활성 PAY_ITEM_SETTING 기준으로 항목 표시
         * - 현재 근태값/조정항목은 화면 표시용으로 붙인다.
         * - 저장된 계산값이 없으므로 무효화 알림은 필요 없다.
         */
        if ("NEW".equals(statusDTO.getPayrollStatus())) {

            response.setItemSettingChanged(false);
            response.setWarningMessage(null);
            response.setAttendanceInvalidationRequired(false);
            response.setAttendanceInvalidationMessage(null);

            List<PayrollItemLoadResponseDTO.Item> items =
                    payrollRepository.selectCurrentPayItemSettings();

            applyCurrentAttendanceSummaryToItems(
                    items,
                    requestDTO.getEmpNo(),
                    requestDTO.getPayYear(),
                    requestDTO.getPayMonth()
            );

            appendDerivedAdjustmentItems(
                    items,
                    requestDTO.getEmpNo(),
                    payMonth
            );

            response.setItems(items);
            return response;
        }

        /*
         * CONFIRMED / PAID 상태
         * - 확정/지급완료는 저장 당시 snapshot만 보여준다.
         * - 현재 ATTENDANCE 기준으로 다시 계산하면 안 된다.
         * - 조정수당/조정공제도 다시 붙이면 안 된다.
         */
        if ("CONFIRMED".equals(statusDTO.getPayrollStatus())
                || "PAID".equals(statusDTO.getPayrollStatus())) {

            response.setItemSettingChanged(false);
            response.setWarningMessage(null);
            response.setAttendanceInvalidationRequired(false);
            response.setAttendanceInvalidationMessage(null);

            List<PayrollItemLoadResponseDTO.Item> items =
                    payrollRepository.selectSavedPayrollItems(
                            requestDTO.getEmpNo(),
                            payMonth
                    );

            restoreAdjustmentMetaFromSnapshotName(items);

            /*
             * CONFIRMED / PAID는 현재 근태를 다시 조회하면 안 된다.
             * 대신 저장된 PAYROLL_ITEM snapshot에서
             * 확정 당시 연장분/결근일수/조정항목 표시값을 복원한다.
             */
            restoreAttendanceCountFromSavedSnapshot(items);

            response.setItems(items);
            return response;
        }

        /*
         * DRAFT 상태
         * - 저장된 PAYROLL_ITEM snapshot을 먼저 조회한다.
         * - 이 snapshot과 현재 ATTENDANCE 기준 근태값을 비교해서
         *   계산결과 무효화 여부를 판단한다.
         */
        List<PayrollItemLoadResponseDTO.Item> items =
                payrollRepository.selectSavedPayrollItems(
                        requestDTO.getEmpNo(),
                        payMonth
                );

        /*
         * 조정수당[yyyy-MM], 조정공제[yyyy-MM]
         * snapshot 이름 기준으로 근태연동 유형 복원
         */
        restoreAdjustmentMetaFromSnapshotName(items);

        /*
         * 저장 당시 snapshot 기준 분/일수 복원
         */
        restoreAttendanceCountFromSavedSnapshot(items);

        boolean attendanceInvalidationRequired =
                hasSavedCalculationValue(statusDTO)
                        && isAttendanceInvalidationRequired(
                        requestDTO.getEmpNo(),
                        payMonth,
                        items
                );

        response.setAttendanceInvalidationRequired(attendanceInvalidationRequired);

        if (attendanceInvalidationRequired) {
            response.setAttendanceInvalidationMessage(
                    "저장 이후 근태연동 값이 변경되어 계산 미리보기가 다시 필요합니다."
            );
        } else {
            response.setAttendanceInvalidationMessage(null);
        }

        /*
         * 무효화 판단 이후에만 화면 표시용 현재 근태값을 다시 세팅한다.
         * 순서 중요:
         * - 먼저 snapshot 비교
         * - 그 다음 화면 표시값 갱신
         */
        applyCurrentAttendanceSummaryToItems(
                items,
                requestDTO.getEmpNo(),
                requestDTO.getPayYear(),
                requestDTO.getPayMonth()
        );

        appendDerivedAdjustmentItems(
                items,
                requestDTO.getEmpNo(),
                payMonth
        );

        response.setItems(items);

        /*
         * DRAFT 지급/공제항목 설정 변경 경고
         * - PAY_ITEM_SETTING 최신 수정일이 저장된 DRAFT 이후면 경고
         */
        PayrollBaseSalaryResponseDTO savedPayroll =
                payrollRepository.selectSavedPayrollBaseSalary(
                        requestDTO.getEmpNo(),
                        payMonth
                ).orElse(null);

        java.time.LocalDateTime latestItemSettingUpdatedAt =
                payrollRepository.selectLatestPayItemSettingUpdatedAt();

        boolean itemSettingChanged = false;

        if (savedPayroll != null
                && savedPayroll.getPayrollUpdatedAt() != null
                && latestItemSettingUpdatedAt != null
                && latestItemSettingUpdatedAt.isAfter(savedPayroll.getPayrollUpdatedAt())) {

            List<PayrollItemLoadResponseDTO.Item> latestSettingItems =
                    payrollRepository.selectCurrentPayItemSettings();

            /*
             * updatedAt은 바뀌었더라도
             * 저장된 DRAFT snapshot과 현재 항목설정의 실제 구성이 같으면
             * 항목변경권고를 띄우지 않는다.
             */
            itemSettingChanged =
                    !isSamePayItemSettingStructure(items, latestSettingItems);
        }

        response.setItemSettingChanged(itemSettingChanged);

        if (itemSettingChanged) {
            response.setWarningMessage("지급/공제항목 설정이 변경되었습니다. 변경된 항목 설정을 적용하시겠습니까?");
        } else {
            response.setWarningMessage(null);
        }

        return response;
    }

    // 지급/공제항목 변경 경고 확인 처리
    @Override
    @Transactional
    public String decidePayItemSettingChange(PayrollMainRequestDTO requestDTO) {

        validateMainRequest(requestDTO);

        String payMonth = makePayMonth(requestDTO.getPayYear(), requestDTO.getPayMonth());

        PayrollEntity payroll = payrollRepository
                .findByEmployee_EmpNoAndPayMonth(requestDTO.getEmpNo(), payMonth)
                .orElseThrow(() -> new IllegalArgumentException("작성중 급여대장이 없습니다."));

        if (!PayrollStatus.DRAFT.equals(payroll.getStatus())) {
            throw new IllegalStateException("작성중 상태에서만 항목 설정 변경 확인 처리가 가능합니다.");
        }

        /*
         * APPLY: 최신 설정 적용
         * KEEP : 기존 항목 유지
         */
        String decision = requestDTO.getItemSettingDecision();

        if (!"APPLY".equals(decision) && !"KEEP".equals(decision)) {
            throw new IllegalArgumentException("지급/공제항목 설정 변경 처리 방식이 올바르지 않습니다.");
        }

        /*
         * 최신 설정 적용 선택 시에만
         * 기존 PAYROLL_ITEM snapshot을 삭제하고
         * 현재 PAY_ITEM_SETTING 기준으로 다시 생성한다.
         */
        if ("APPLY".equals(decision)) {

            List<PayrollItemLoadResponseDTO.Item> savedItems =
                    payrollRepository.selectSavedPayrollItems(
                            requestDTO.getEmpNo(),
                            payMonth
                    );

            List<PayrollItemLoadResponseDTO.Item> latestItems =
                    payrollRepository.selectCurrentPayItemSettings();

            applyCurrentAttendanceSummaryToItems(
                    latestItems,
                    requestDTO.getEmpNo(),
                    requestDTO.getPayYear(),
                    requestDTO.getPayMonth()
            );

            appendDerivedAdjustmentItems(
                    latestItems,
                    requestDTO.getEmpNo(),
                    payMonth
            );

            entityManager.createQuery("""
        delete from PayrollItemEntity item
         where item.payroll.payrollId = :payrollId
        """)
                    .setParameter("payrollId", payroll.getPayrollId())
                    .executeUpdate();

            for (PayrollItemLoadResponseDTO.Item item : latestItems) {

                PayItemSettingEntity itemSetting = null;

                if (item.getItemSettingId() != null) {
                    itemSetting = entityManager.getReference(
                            PayItemSettingEntity.class,
                            item.getItemSettingId()
                    );
                }

                BigDecimal saveAmount =
                        decideKeepAmountAfterApply(
                                item,
                                savedItems
                        );

                PayrollItemEntity payrollItem = PayrollItemEntity.builder()
                        .payroll(payroll)
                        .itemSetting(itemSetting)
                        .itemNameSnapshot(item.getItemNameSnapshot())
                        .itemType(item.getItemType())
                        .amount(saveAmount)
                        .taxType(item.getTaxType())
                        .nonTaxCode(item.getNonTaxCode())
                        .taxableAmount(BigDecimal.ZERO)
                        .nonTaxableAmount(BigDecimal.ZERO)
                        .isValidNonTax(true)
                        .build();

                entityManager.persist(payrollItem);
            }
        }

        /*
         * KEEP 선택 시:
         * - 기존 PAYROLL_ITEM은 그대로 둔다.
         *
         * APPLY / KEEP 공통:
         * - PAYROLL.updatedAt만 현재 시간으로 갱신해서
         *   같은 변경 건에 대한 경고가 반복되지 않게 한다.
         */
        entityManager.createQuery("""
        update PayrollEntity p
           set p.updatedAt = :now
         where p.payrollId = :payrollId
        """)
                .setParameter("now", java.time.LocalDateTime.now())
                .setParameter("payrollId", payroll.getPayrollId())
                .executeUpdate();

        return "지급/공제항목 설정 변경 확인이 완료되었습니다.";
    }

    // 지급/공제항목 설정 저장
    @Override
    @Transactional
    public List<PayrollItemLoadResponseDTO.Item> savePayItemSettings(PayItemSettingSaveRequestDTO requestDTO) {

        validatePayItemSettingSaveRequest(requestDTO);

        Set<Integer> activeIdSet = new HashSet<>();

        for (PayItemSettingSaveRequestDTO.Item itemDTO : requestDTO.getItems()) {

            /*
             * 신규 항목 처리
             *
             * 일반 항목:
             * - 새 PAY_ITEM_SETTING row insert
             *
             * 근태연동 항목:
             * - 과거에 삭제되어 isActive=false 된 OVERTIME/ABSENCE row가 있으면
             *   새로 insert하지 않고 기존 row를 다시 isActive=true로 복구한다.
             */
            if (itemDTO.getItemSettingId() == null) {

                Integer restoredId = restoreInactiveAttendanceSettingIfExists(itemDTO);

                if (restoredId != null) {
                    activeIdSet.add(restoredId);
                    continue;
                }

                PayItemSettingEntity itemSetting = PayItemSettingEntity.builder()
                        .itemName(itemDTO.getItemName().trim())
                        .itemType(itemDTO.getItemType())
                        .taxType(itemDTO.getTaxType())
                        .nonTaxCode(itemDTO.getNonTaxCode())
                        .linkedAttendanceType(itemDTO.getLinkedAttendanceType())
                        .isActive(true)
                        .build();

                entityManager.persist(itemSetting);
                entityManager.flush();

                activeIdSet.add(itemSetting.getItemSettingId());
                continue;
            }

            /*
             * 기존 항목 수정
             * - 모달에 남아있는 항목은 활성 상태로 갱신한다.
             */
            activeIdSet.add(itemDTO.getItemSettingId());

            entityManager.createQuery("""
            update PayItemSettingEntity item
               set item.itemName = :itemName,
                   item.itemType = :itemType,
                   item.taxType = :taxType,
                   item.nonTaxCode = :nonTaxCode,
                   item.linkedAttendanceType = :linkedAttendanceType,
                   item.isActive = true,
                   item.updatedAt = :now
             where item.itemSettingId = :itemSettingId
            """)
                    .setParameter("itemName", itemDTO.getItemName().trim())
                    .setParameter("itemType", itemDTO.getItemType())
                    .setParameter("taxType", itemDTO.getTaxType())
                    .setParameter("nonTaxCode", itemDTO.getNonTaxCode())
                    .setParameter("linkedAttendanceType", itemDTO.getLinkedAttendanceType())
                    .setParameter("now", java.time.LocalDateTime.now())
                    .setParameter("itemSettingId", itemDTO.getItemSettingId())
                    .executeUpdate();
        }

        /*
         * 모달에서 삭제된 항목 처리
         *
         * 일반 항목:
         * - PAYROLL_ITEM FK를 null로 끊고 PAY_ITEM_SETTING 물리삭제
         *
         * 근태연동 항목:
         * - 물리삭제하지 않는다.
         * - isActive=false 처리만 한다.
         * - 그래야 같은 급여월에서 다시 근태연동을 켰을 때 기존 row를 복구할 수 있다.
         */
        if (activeIdSet.isEmpty()) {

            // 1) 근태연동 항목은 비활성화
            entityManager.createQuery("""
            update PayItemSettingEntity item
               set item.isActive = false,
                   item.updatedAt = :now
             where item.linkedAttendanceType is not null
            """)
                    .setParameter("now", java.time.LocalDateTime.now())
                    .executeUpdate();

            // 2) 일반 항목은 FK 해제 후 물리삭제
            entityManager.createQuery("""
            update PayrollItemEntity item
               set item.itemSetting = null
             where item.itemSetting is not null
               and item.itemSetting.linkedAttendanceType is null
            """)
                    .executeUpdate();

            entityManager.createQuery("""
            delete from PayItemSettingEntity item
             where item.linkedAttendanceType is null
            """)
                    .executeUpdate();

        } else {

            // 1) 모달에서 빠진 근태연동 항목은 비활성화
            entityManager.createQuery("""
            update PayItemSettingEntity item
               set item.isActive = false,
                   item.updatedAt = :now
             where item.itemSettingId not in :activeIdSet
               and item.linkedAttendanceType is not null
            """)
                    .setParameter("activeIdSet", activeIdSet)
                    .setParameter("now", java.time.LocalDateTime.now())
                    .executeUpdate();

            // 2) 모달에서 빠진 일반 항목은 FK 해제 후 물리삭제
            entityManager.createQuery("""
            
                update PayrollItemEntity item
               set item.itemSetting = null
             where item.itemSetting is not null
               and item.itemSetting.itemSettingId not in :activeIdSet
               and item.itemSetting.linkedAttendanceType is null
            """)
                    .setParameter("activeIdSet", activeIdSet)
                    .executeUpdate();

            entityManager.createQuery("""
            delete from PayItemSettingEntity item
             where item.itemSettingId not in :activeIdSet
               and item.linkedAttendanceType is null
            """)
                    .setParameter("activeIdSet", activeIdSet)
                    .executeUpdate();
        }

        entityManager.flush();
        entityManager.clear();

        // 저장 후 최신 활성 설정 목록 반환
        return payrollRepository.selectCurrentPayItemSettings();
    }

    // 비활성화된 근태연동 항목이 있으면 새로 insert하지 않고 복구한다.
// - OVERTIME: 연장수당
// - ABSENCE : 결근공제
// 일반 항목은 여기서 처리하지 않는다.
    private Integer restoreInactiveAttendanceSettingIfExists(PayItemSettingSaveRequestDTO.Item itemDTO) {

        if (!StringUtils.hasText(itemDTO.getLinkedAttendanceType())) {
            return null;
        }

        List<PayItemSettingEntity> inactiveItems =
                entityManager.createQuery("""
                select item
                  from PayItemSettingEntity item
                 where item.linkedAttendanceType = :linkedAttendanceType
                   and item.isActive = false
                 order by item.itemSettingId desc
                """, PayItemSettingEntity.class)
                        .setParameter("linkedAttendanceType", itemDTO.getLinkedAttendanceType())
                        .setMaxResults(1)
                        .getResultList();

        if (inactiveItems.isEmpty()) {
            return null;
        }

        Integer itemSettingId = inactiveItems.get(0).getItemSettingId();

        entityManager.createQuery("""
        update PayItemSettingEntity item
           set item.itemName = :itemName,
               item.itemType = :itemType,
               item.taxType = :taxType,
               item.nonTaxCode = :nonTaxCode,
               item.linkedAttendanceType = :linkedAttendanceType,
               item.isActive = true,
               item.updatedAt = :now
         where item.itemSettingId = :itemSettingId
        """)
                .setParameter("itemName", itemDTO.getItemName().trim())
                .setParameter("itemType", itemDTO.getItemType())
                .setParameter("taxType", itemDTO.getTaxType())
                .setParameter("nonTaxCode", itemDTO.getNonTaxCode())
                .setParameter("linkedAttendanceType", itemDTO.getLinkedAttendanceType())
                .setParameter("now", java.time.LocalDateTime.now())
                .setParameter("itemSettingId", itemSettingId)
                .executeUpdate();

        return itemSettingId;
    }

    // 계산 미리보기
    @Override
    @Transactional(readOnly = true)
    public PayrollPreviewResponseDTO previewPayroll(PayrollSaveRequestDTO requestDTO) {

        validatePayrollSaveRequest(requestDTO);

        String payMonth = makePayMonth(requestDTO.getPayYear(), requestDTO.getPayMonth());
        PayrollEmployeeInfoResponseDTO employeeInfo = getEmployeeInfo(requestDTO.getEmpNo());

        return calculatePayrollPreview(requestDTO, employeeInfo, payMonth);
    }

    // 급여대장 확정
    @Override
    @Transactional
    public String confirmPayroll(PayrollSaveRequestDTO requestDTO) {

        validateEmpNo(requestDTO.getEmpNo());

        if (requestDTO.getPayYear() == null
                || requestDTO.getPayMonth() == null) {
            throw new IllegalArgumentException("급여년월 정보가 없습니다.");
        }

        String payMonth = makePayMonth(
                requestDTO.getPayYear(),
                requestDTO.getPayMonth()
        );

        PayrollEntity payroll = payrollRepository
                .findByEmployee_EmpNoAndPayMonth(requestDTO.getEmpNo(), payMonth)
                .orElseThrow(() -> new IllegalArgumentException("작성중 급여대장이 없습니다. 먼저 저장해 주세요."));

        if (PayrollStatus.PAID.equals(payroll.getStatus())) {
            throw new IllegalStateException("지급완료 상태의 급여대장은 확정할 수 없습니다.");
        }

        if (PayrollStatus.CONFIRMED.equals(payroll.getStatus())) {
            return "이미 확정된 급여대장입니다.";
        }

        if (!PayrollStatus.DRAFT.equals(payroll.getStatus())) {
            throw new IllegalStateException("작성중 상태의 급여대장만 확정할 수 있습니다.");
        }

        /**
         * 확정은 DRAFT에 저장된 PAYROLL / PAYROLL_ITEM snapshot을 보존한 채
         * 상태만 CONFIRMED로 변경한다.
         *
         * 중요:
         * - 확정 시 프론트 request.items 기준으로 PAYROLL_ITEM을 재저장하지 않는다.
         * - 기존 항목 삭제 후 재생성하면 화면 수집 누락 시 항목이 사라진다.
         */
        entityManager.createQuery("""
        update PayrollEntity p
           set p.status = :status,
               p.updatedAt = :now
         where p.payrollId = :payrollId
    """)
                .setParameter("status", PayrollStatus.CONFIRMED)
                .setParameter("now", java.time.LocalDateTime.now())
                .setParameter("payrollId", payroll.getPayrollId())
                .executeUpdate();

        return "급여대장이 확정되었습니다.";
    }

    // 급여대장 지급확정
    @Override
    @Transactional
    public String payConfirmPayroll(PayrollSaveRequestDTO requestDTO) {

        validateEmpNo(requestDTO.getEmpNo());

        if (requestDTO.getPayYear() == null
                || requestDTO.getPayMonth() == null) {
            throw new IllegalArgumentException("급여년월 정보가 없습니다.");
        }

        if (requestDTO.getPayDate() == null) {
            throw new IllegalArgumentException("지급일을 선택해 주세요.");
        }

        String payMonth = makePayMonth(
                requestDTO.getPayYear(),
                requestDTO.getPayMonth()
        );

        PayrollEntity payroll = payrollRepository
                .findByEmployee_EmpNoAndPayMonth(requestDTO.getEmpNo(), payMonth)
                .orElseThrow(() -> new IllegalArgumentException("확정된 급여대장이 없습니다. 먼저 저장/확정을 진행해 주세요."));

        if (PayrollStatus.PAID.equals(payroll.getStatus())) {
            return "이미 지급완료된 급여대장입니다.";
        }

        if (!PayrollStatus.CONFIRMED.equals(payroll.getStatus())) {
            throw new IllegalStateException("확정 상태의 급여대장만 지급확정할 수 있습니다.");
        }

        /**
         * 지급확정도 저장된 snapshot을 보존하고
         * 상태와 지급일만 변경한다.
         */
        entityManager.createQuery("""
        update PayrollEntity p
           set p.status = :status,
               p.payDate = :payDate,
               p.updatedAt = :now
         where p.payrollId = :payrollId
    """)
                .setParameter("status", PayrollStatus.PAID)
                .setParameter("payDate", requestDTO.getPayDate())
                .setParameter("now", java.time.LocalDateTime.now())
                .setParameter("payrollId", payroll.getPayrollId())
                .executeUpdate();

        return "급여대장이 지급확정되었습니다.";
    }

    // 급여대장 삭제
    @Override
    @Transactional
    public String deletePayroll(PayrollMainRequestDTO requestDTO) {

        validateMainRequest(requestDTO);

        String payMonth = makePayMonth(requestDTO.getPayYear(), requestDTO.getPayMonth());

        PayrollEntity payroll = payrollRepository
                .findByEmployee_EmpNoAndPayMonth(requestDTO.getEmpNo(), payMonth)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 급여대장이 없습니다."));

        if (!PayrollStatus.DRAFT.equals(payroll.getStatus())) {
            throw new IllegalStateException("작성중 상태의 급여대장만 삭제할 수 있습니다.");
        }

        // 먼저 상세 항목 삭제
        entityManager.createQuery("""
            delete from PayrollItemEntity item
             where item.payroll.payrollId = :payrollId
            """)
                .setParameter("payrollId", payroll.getPayrollId())
                .executeUpdate();

        // 급여대장 삭제
        entityManager.createQuery("""
            delete from PayrollEntity p
             where p.payrollId = :payrollId
            """)
                .setParameter("payrollId", payroll.getPayrollId())
                .executeUpdate();

        return "급여대장이 삭제되었습니다.";
    }

    /**
     * 항목설정 저장 직후 최신 항목 미리보기
     *
     * 중요:
     * - PAYROLL_ITEM snapshot 변경 금지
     * - 최신 PAY_ITEM_SETTING 기준
     * - 조정수당/조정공제 계산 포함
     */
    @Override
    @Transactional(readOnly = true)
    public List<PayrollItemLoadResponseDTO.Item> getLatestPreviewPayrollItems(
            String empNo,
            Integer payYear,
            Integer payMonth
    ) {

        String payMonthStr =
                payYear + "-" + String.format("%02d", payMonth);

        /**
         * 최신 활성 항목 조회
         */
        List<PayrollItemLoadResponseDTO.Item> items =
                payrollRepository.selectCurrentPayItemSettings();

        /**
         * 연장/결근 계산 붙이기
         */
        applyCurrentAttendanceSummaryToItems(
                items,
                empNo,
                payYear,
                payMonth
        );

        /**
         * 조정수당/조정공제 계산
         */
        appendDerivedAdjustmentItems(
                items,
                empNo,
                payMonthStr
        );

        return items;
    }

    /**
     * 연장/결근/조정 누적분 변경 시
     * 저장된 계산값만 초기화한다.
     *
     * 중요:
     * - PayrollEntity는 수정하지 않는다.
     * - PAYROLL_ITEM은 건드리지 않는다.
     * - 조정수당/조정공제 생성·이월·소멸 규칙도 건드리지 않는다.
     * - DRAFT 상태만 초기화한다.
     */
    @Override
    @Transactional
    public String resetAttendanceCalculation(PayrollMainRequestDTO requestDTO) {

        validateMainRequest(requestDTO);

        String payMonth = makePayMonth(
                requestDTO.getPayYear(),
                requestDTO.getPayMonth()
        );

        PayrollEntity payroll = payrollRepository
                .findByEmployee_EmpNoAndPayMonth(
                        requestDTO.getEmpNo(),
                        payMonth
                )
                .orElse(null);

        if (payroll == null) {
            return "급여대장 없음";
        }

        if (!PayrollStatus.DRAFT.equals(payroll.getStatus())) {
            return "작성중 상태가 아니므로 초기화하지 않습니다.";
        }

        entityManager.createQuery("""
        update PayrollEntity p
           set p.incomeTax = 0,
               p.localIncomeTax = 0,
               p.nationalPensionAmount = 0,
               p.healthInsuranceAmount = 0,
               p.longTermCareAmount = 0,
               p.employmentInsuranceAmount = 0,
               p.totalInsurance = 0,
               p.totalGross = 0,
               p.totalDeduction = 0,
               p.netSalary = 0
         where p.payrollId = :payrollId
           and p.status = :status
    """)
                .setParameter("payrollId", payroll.getPayrollId())
                .setParameter("status", PayrollStatus.DRAFT)
                .executeUpdate();

        return "연장/결근/조정 변경으로 계산값을 초기화했습니다.";
    }

    // 작성년월 select 옵션 조회
    @Override
    @Transactional(readOnly = true)
    public PayrollPeriodOptionDTO getPeriodOptions(String empNo) {

        validateEmpNo(empNo);

        PayrollEmployeeInfoResponseDTO employeeInfo = getEmployeeInfo(empNo);

        LocalDate now = LocalDate.now();
        LocalDate hireDate = employeeInfo.getHireDate();

        // 최대 과거 5년까지만 선택
        int minYear = now.getYear() - 4;

        // 입사년도가 더 늦으면 입사년도부터 선택
        if (hireDate != null && hireDate.getYear() > minYear) {
            minYear = hireDate.getYear();
        }

        List<Integer> yearList = new java.util.ArrayList<>();

        for (int year = minYear; year <= now.getYear(); year++) {
            yearList.add(year);
        }

        List<Integer> monthList = makeAvailableMonths(now.getYear(), hireDate);

        return PayrollPeriodOptionDTO.builder()
                .defaultYear(now.getYear())
                .defaultMonth(now.getMonthValue())
                .hireDate(hireDate)
                .availableYears(yearList)
                .availableMonths(monthList)
                .build();
    }



    // 사원 검색값 검증
    private void validateEmployeeSearch(PayrollEmployeeSearchDTO searchDTO) {

        if (searchDTO == null || !StringUtils.hasText(searchDTO.getKeyword())) {
            throw new IllegalArgumentException("검색어를 입력해 주세요.");
        }

        String keyword = searchDTO.getKeyword().trim();
        String searchType = searchDTO.getSearchType();

        if ("EMP_NO".equals(searchType) && keyword.length() < 6) {
            throw new IllegalArgumentException("사번은 6자리 이상 입력해 주세요.");
        }

        if ("NAME".equals(searchType) && keyword.length() < 2) {
            throw new IllegalArgumentException("이름은 2글자 이상 입력해 주세요.");
        }

        if (!"EMP_NO".equals(searchType) && !"NAME".equals(searchType)) {
            throw new IllegalArgumentException("검색 유형이 올바르지 않습니다.");
        }

        if (searchDTO.getLimit() == null || searchDTO.getLimit() <= 0) {
            searchDTO.setLimit(10);
        }

        if (searchDTO.getShowAll() == null) {
            searchDTO.setShowAll(false);
        }
    }

    // 사번 검증
    private void validateEmpNo(String empNo) {

        if (!StringUtils.hasText(empNo)) {
            throw new IllegalArgumentException("사번이 없습니다.");
        }

        if (empNo.trim().length() < 6) {
            throw new IllegalArgumentException("사번은 6자리 이상이어야 합니다.");
        }
    }

    // 메인 조회값 검증
    private void validateMainRequest(PayrollMainRequestDTO requestDTO) {

        if (requestDTO == null) {
            throw new IllegalArgumentException("조회 조건이 없습니다.");
        }

        validateEmpNo(requestDTO.getEmpNo());

        if (requestDTO.getPayYear() == null) {
            throw new IllegalArgumentException("작성년도를 선택해 주세요.");
        }

        if (requestDTO.getPayMonth() == null) {
            throw new IllegalArgumentException("작성월을 선택해 주세요.");
        }

        LocalDate now = LocalDate.now();

        if (requestDTO.getPayYear() > now.getYear()) {
            throw new IllegalArgumentException("미래 연도는 조회할 수 없습니다.");
        }

        if (requestDTO.getPayYear() == now.getYear()
                && requestDTO.getPayMonth() > now.getMonthValue()) {
            throw new IllegalArgumentException("미래 월은 조회할 수 없습니다.");
        }

        // 입사일 이전 급여는 조회 불가
        PayrollEmployeeInfoResponseDTO employeeInfo = getEmployeeInfo(requestDTO.getEmpNo());
        LocalDate hireDate = employeeInfo.getHireDate();

        if (hireDate != null) {
            if (requestDTO.getPayYear() < hireDate.getYear()) {
                throw new IllegalArgumentException("입사일 이전 급여는 조회할 수 없습니다.");
            }

            if (requestDTO.getPayYear() == hireDate.getYear()
                    && requestDTO.getPayMonth() < hireDate.getMonthValue()) {
                throw new IllegalArgumentException("입사일 이전 급여는 조회할 수 없습니다.");
            }
        }
    }

    // 급여 계산 미리보기 공통 로직
    private PayrollPreviewResponseDTO calculatePayrollPreview(
            PayrollSaveRequestDTO requestDTO,
            PayrollEmployeeInfoResponseDTO employeeInfo,
            String payMonth
    ) {

        BigDecimal baseSalary = nvl(requestDTO.getBaseSalary());

        BigDecimal taxableAllowance = BigDecimal.ZERO;
        BigDecimal nonTaxableAllowance = BigDecimal.ZERO;
        BigDecimal otherDeduction = BigDecimal.ZERO;

        // 지급월 기준 근태 집계 기간 계산
        LocalDate startDate = getPayMonthStartDate(
                requestDTO.getPayYear(),
                requestDTO.getPayMonth(),
                employeeInfo.getHireDate()
        );

        LocalDate endDate = getPayMonthEndDate(
                requestDTO.getPayYear(),
                requestDTO.getPayMonth()
        );

        // 전자결재 승인 후 ATTENDANCE에 반영된 근태값 조회
        PayrollAttendanceSummaryDTO attendanceSummary =
                payrollRepository.selectAttendanceSummary(
                        requestDTO.getEmpNo(),
                        startDate,
                        endDate
                );

        if (attendanceSummary == null) {
            attendanceSummary = PayrollAttendanceSummaryDTO.builder()
                    .overtimeMinutes(0)
                    .absenceDays(0)
                    .workingDays(0)
                    .build();
        }

        if (attendanceSummary.getOvertimeMinutes() == null) {
            attendanceSummary.setOvertimeMinutes(0);
        }

        if (attendanceSummary.getAbsenceDays() == null) {
            attendanceSummary.setAbsenceDays(0);
        }

        // 토/일 제외 근무예정일수 계산
        int workingDays = countWorkingDays(startDate, endDate);
        attendanceSummary.setWorkingDays(workingDays);

        // 계산 상세 row
        List<PayrollPreviewResponseDTO.ItemRow> itemRows = new java.util.ArrayList<>();

        if (requestDTO.getItems() != null) {
            for (PayrollSaveRequestDTO.Item item : requestDTO.getItems()) {

                if (item == null) {
                    continue;
                }

                BigDecimal inputAmount = nvl(item.getAmount());
                BigDecimal calculatedAmount = inputAmount;

                BigDecimal taxableAmount = BigDecimal.ZERO;
                BigDecimal nonTaxableAmount = BigDecimal.ZERO;

                String formula = null;

                /*
                 * 연장근무수당
                 * - 입력값: 60분당 연장수당 단가
                 * - 계산값: 승인 반영된 연장근무분 × 60분당 단가 / 60
                 */
                if ("OVERTIME".equals(item.getLinkedAttendanceType())) {

                    int overtimeMinutes =
                            item.getOvertimeMinutes() == null
                                    ? attendanceSummary.getOvertimeMinutes()
                                    : item.getOvertimeMinutes();

                    calculatedAmount = inputAmount
                            .multiply(BigDecimal.valueOf(overtimeMinutes))
                            .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);

                    formula = overtimeMinutes
                            + "분 × "
                            + formatMoney(inputAmount)
                            + "원 / 60분";
                }

                /*
                 * 결근공제
                 * - 계산값: 결근일수 × 1일 공제액
                 * - 1일 공제액: 기본급 / 근무예정일수
                 */
                if ("ABSENCE".equals(item.getLinkedAttendanceType())) {

                    /*
                     * 결근공제
                     * - 입력값: 하루당 공제단가
                     * - 계산값: 결근일수 × 하루당 공제단가
                     * - PAYROLL_ITEM.amount에는 최종 공제금액이 아니라 하루당 공제단가가 저장된다.
                     */
                    BigDecimal dailyDeduction = inputAmount;

                    int absenceDays =
                            item.getAbsenceDays() == null
                                    ? attendanceSummary.getAbsenceDays()
                                    : item.getAbsenceDays();

                    calculatedAmount = inputAmount
                            .multiply(BigDecimal.valueOf(absenceDays))
                            .setScale(0, RoundingMode.HALF_UP);

                    formula = absenceDays
                            + "일 × "
                            + formatMoney(inputAmount)
                            + "원/하루";

                }

                // 지급항목 합산
                if ("ALLOWANCE".equals(item.getItemType())) {

                    if ("NON_TAXABLE".equals(item.getTaxType())) {
                        nonTaxableAllowance = nonTaxableAllowance.add(calculatedAmount);
                        nonTaxableAmount = calculatedAmount;
                    } else {
                        taxableAllowance = taxableAllowance.add(calculatedAmount);
                        taxableAmount = calculatedAmount;
                    }
                }

                // 공제항목 합산
                if ("DEDUCTION".equals(item.getItemType())) {
                    otherDeduction = otherDeduction.add(calculatedAmount);
                }

                // 근태연동 항목은 조정수당/조정공제 역산을 위해
                // taxableAmount에 실제 계산 반영금액을 저장한다.
                // 컬럼명은 taxableAmount지만, ERD 변경 없이 자동계산 결과 snapshot 용도로 사용한다.
                if ("OVERTIME".equals(item.getLinkedAttendanceType())
                        || "ABSENCE".equals(item.getLinkedAttendanceType())) {
                    taxableAmount = calculatedAmount;
                }

                itemRows.add(PayrollPreviewResponseDTO.ItemRow.builder()
                        .itemNameSnapshot(item.getItemNameSnapshot())
                        .itemType(item.getItemType())
                        .inputAmount(inputAmount)
                        .calculatedAmount(calculatedAmount)
                        .taxType(item.getTaxType())
                        .nonTaxCode(item.getNonTaxCode())
                        .linkedAttendanceType(item.getLinkedAttendanceType())
                        .taxableAmount(taxableAmount)
                        .nonTaxableAmount(nonTaxableAmount)
                        .validNonTax(true)
                        .formula(formula)
                        .build());
            }
        }

        BigDecimal totalAllowance = taxableAllowance.add(nonTaxableAllowance);
        BigDecimal totalGross = baseSalary.add(totalAllowance);

        /*
         * 과세소득
         * - 기본급 + 과세 지급항목
         * - 현재는 비과세 선택 시 전액 비과세 처리
         */
        BigDecimal taxableIncome = baseSalary.add(taxableAllowance);

        /*
         * 4대보험 기준금액
         * - 3차 초기 구현에서는 기본급 기준으로 계산한다.
         * - 추후 과세 지급항목까지 포함하려면 insuranceBase = taxableIncome 으로 교체하면 된다.
         */
        BigDecimal insuranceBase = taxableIncome;


        BigDecimal nationalPension = calc(insuranceBase, PayrollRateConstants.NATIONAL_PENSION);
        BigDecimal healthInsurance = calc(insuranceBase, PayrollRateConstants.HEALTH_INSURANCE);

        // 장기요양보험은 건강보험료 기준
        BigDecimal longTermCare = calc(healthInsurance, PayrollRateConstants.LONG_TERM_CARE);
        BigDecimal employmentInsurance = calc(insuranceBase, PayrollRateConstants.EMPLOYMENT_INSURANCE);


        BigDecimal totalInsurance = nationalPension
                .add(healthInsurance)
                .add(longTermCare)
                .add(employmentInsurance);

        // 원천징수: 단일 고정세율
        BigDecimal incomeTax = calc(taxableIncome, PayrollRateConstants.INCOME_TAX);

        // 지방소득세: 소득세 기준
        BigDecimal localIncomeTax = calc(incomeTax, PayrollRateConstants.LOCAL_INCOME_TAX);


        BigDecimal totalDeduction = totalInsurance
                .add(incomeTax)
                .add(localIncomeTax)
                .add(otherDeduction);

        BigDecimal netSalary = totalGross.subtract(totalDeduction);

        List<PayrollPreviewResponseDTO.InsuranceRow> insuranceRows = List.of(
                makeInsuranceRow("국민연금", insuranceBase, PayrollRateConstants.NATIONAL_PENSION, nationalPension),
                makeInsuranceRow("건강보험", insuranceBase, PayrollRateConstants.HEALTH_INSURANCE, healthInsurance),
                makeInsuranceRow("장기요양보험", healthInsurance, PayrollRateConstants.LONG_TERM_CARE, longTermCare),
                makeInsuranceRow("고용보험", insuranceBase, PayrollRateConstants.EMPLOYMENT_INSURANCE, employmentInsurance)
        );

        return PayrollPreviewResponseDTO.builder()
                .empNo(employeeInfo.getEmpNo())
                .empName(employeeInfo.getEmpName())
                .deptName(employeeInfo.getDeptName())
                .positionName(employeeInfo.getPositionName())
                .payMonth(payMonth)
                .baseSalary(baseSalary)
                .taxableAllowance(taxableAllowance)
                .nonTaxableAllowance(nonTaxableAllowance)
                .totalAllowance(totalAllowance)
                .totalGross(totalGross)
                .taxableIncome(taxableIncome)
                .nationalPensionAmount(nationalPension)
                .healthInsuranceAmount(healthInsurance)
                .longTermCareAmount(longTermCare)
                .employmentInsuranceAmount(employmentInsurance)
                .totalInsurance(totalInsurance)
                .incomeTax(incomeTax)
                .localIncomeTax(localIncomeTax)
                .otherDeduction(otherDeduction)
                .totalDeduction(totalDeduction)
                .netSalary(netSalary)
                .insuranceRows(insuranceRows)
                .itemRows(itemRows)
                .build();
    }

    // 계산 결과를 포함하여 급여대장 저장
    private void savePayrollWithCalculation(
            PayrollSaveRequestDTO requestDTO,
            PayrollPreviewResponseDTO preview,
            PayrollStatus status,
            LocalDate payDate
    ) {

        String payMonth = makePayMonth(requestDTO.getPayYear(), requestDTO.getPayMonth());

        PayrollEntity payroll = payrollRepository
                .findByEmployee_EmpNoAndPayMonth(requestDTO.getEmpNo(), payMonth)
                .orElse(null);

        PayrollEmployeeInfoResponseDTO employeeInfo = getEmployeeInfo(requestDTO.getEmpNo());

        EmpEntity employee = entityManager.getReference(EmpEntity.class, requestDTO.getEmpNo());

        GradeCodeEntity grade = null;
        if (StringUtils.hasText(employeeInfo.getGradeId())) {
            grade = entityManager.getReference(GradeCodeEntity.class, employeeInfo.getGradeId());
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        if (payroll == null) {

            payroll = PayrollEntity.builder()
                    .employee(employee)
                    .grade(grade)
                    .payMonth(payMonth)
                    .familyCount(requestDTO.getFamilyCount() == null ? 1 : requestDTO.getFamilyCount())
                    .baseSalary(preview.getBaseSalary())
                    .bonus(BigDecimal.ZERO)
                    .totalAllowance(preview.getTotalAllowance())
                    .totalGross(preview.getTotalGross())
                    .taxableIncome(preview.getTaxableIncome())
                    .incomeTax(preview.getIncomeTax())
                    .localIncomeTax(preview.getLocalIncomeTax())
                    .nationalPensionAmount(preview.getNationalPensionAmount())
                    .healthInsuranceAmount(preview.getHealthInsuranceAmount())
                    .longTermCareAmount(preview.getLongTermCareAmount())
                    .employmentInsuranceAmount(preview.getEmploymentInsuranceAmount())
                    .totalInsurance(preview.getTotalInsurance())
                    .totalDeduction(preview.getTotalDeduction())
                    .netSalary(preview.getNetSalary())
                    .status(status)
                    .payDate(payDate)
                    .build();

            payrollRepository.save(payroll);
        } else {

            if (PayrollStatus.PAID.equals(payroll.getStatus())) {
                throw new IllegalStateException("지급완료 상태의 급여대장은 수정할 수 없습니다.");
            }

            entityManager.createQuery("""
                update PayrollEntity p
                   set p.grade = :grade,
                       p.familyCount = :familyCount,
                       p.baseSalary = :baseSalary,
                       p.status = :status,
                       p.bonus = :bonus,
                       p.totalAllowance = :totalAllowance,
                       p.totalGross = :totalGross,
                       p.taxableIncome = :taxableIncome,
                       p.incomeTax = :incomeTax,
                       p.localIncomeTax = :localIncomeTax,
                       p.nationalPensionAmount = :nationalPensionAmount,
                       p.healthInsuranceAmount = :healthInsuranceAmount,
                       p.longTermCareAmount = :longTermCareAmount,
                       p.employmentInsuranceAmount = :employmentInsuranceAmount,
                       p.totalInsurance = :totalInsurance,
                       p.totalDeduction = :totalDeduction,
                       p.netSalary = :netSalary,
                       p.payDate = :payDate,
                       p.updatedAt = :now
                 where p.payrollId = :payrollId
                """)
                    .setParameter("grade", grade)
                    .setParameter("familyCount", requestDTO.getFamilyCount() == null ? 1 : requestDTO.getFamilyCount())
                    .setParameter("baseSalary", preview.getBaseSalary())
                    .setParameter("status", status)
                    .setParameter("bonus", BigDecimal.ZERO)
                    .setParameter("totalAllowance", preview.getTotalAllowance())
                    .setParameter("totalGross", preview.getTotalGross())
                    .setParameter("taxableIncome", preview.getTaxableIncome())
                    .setParameter("incomeTax", preview.getIncomeTax())
                    .setParameter("localIncomeTax", preview.getLocalIncomeTax())
                    .setParameter("nationalPensionAmount", preview.getNationalPensionAmount())
                    .setParameter("healthInsuranceAmount", preview.getHealthInsuranceAmount())
                    .setParameter("longTermCareAmount", preview.getLongTermCareAmount())
                    .setParameter("employmentInsuranceAmount", preview.getEmploymentInsuranceAmount())
                    .setParameter("totalInsurance", preview.getTotalInsurance())
                    .setParameter("totalDeduction", preview.getTotalDeduction())
                    .setParameter("netSalary", preview.getNetSalary())
                    .setParameter("payDate", payDate)
                    .setParameter("now", now)
                    .setParameter("payrollId", payroll.getPayrollId())
                    .executeUpdate();
        }

        PayrollEntity savedPayroll = payrollRepository
                .findByEmployee_EmpNoAndPayMonth(requestDTO.getEmpNo(), payMonth)
                .orElseThrow(() -> new IllegalStateException("급여대장 저장 중 오류가 발생했습니다."));

        savePayrollItems(savedPayroll, requestDTO.getItems(), preview);
    }



    private PayrollPreviewResponseDTO.InsuranceRow makeInsuranceRow(
            String name,
            BigDecimal baseAmount,
            BigDecimal rate,
            BigDecimal amount
    ) {

        /*
         * 계산식 표시용 기준금액 콤마 처리
         * 예)
         * 4800000 → 4,800,000
         */
        String formattedBaseAmount =
                java.text.NumberFormat
                        .getNumberInstance()
                        .format(nvl(baseAmount));

        return PayrollPreviewResponseDTO.InsuranceRow.builder()
                .name(name)
                .baseAmount(baseAmount)
                .rate(rate)
                .amount(amount)

                /*
                 * 계산식 표시
                 * 예)
                 * 4,800,000 × 0.045
                 */
                .formula(formattedBaseAmount + " × " + rate)
                .build();
    }

    // 금액 계산 공통 처리
    private BigDecimal calc(BigDecimal baseAmount, BigDecimal rate) {

        if (baseAmount == null
                || rate == null
                || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return baseAmount.multiply(rate)
                .setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatMoney(BigDecimal value) {
        return nvl(value).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    // 급여대장 저장 요청 검증
    private void validatePayrollSaveRequest(PayrollSaveRequestDTO requestDTO) {

        if (requestDTO == null) {
            throw new IllegalArgumentException("저장할 급여대장 정보가 없습니다.");
        }

        validateEmpNo(requestDTO.getEmpNo());

        if (requestDTO.getPayYear() == null) {
            throw new IllegalArgumentException("작성년도를 선택해 주세요.");
        }

        if (requestDTO.getPayMonth() == null) {
            throw new IllegalArgumentException("작성월을 선택해 주세요.");
        }

        if (requestDTO.getBaseSalary() == null
                || requestDTO.getBaseSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("기본급은 0보다 큰 값으로 입력해 주세요.");
        }

        validatePayrollItems(requestDTO.getItems());
        validateAttendanceLinkedUnitAmount(requestDTO.getItems());
    }

    /**
     * 근태연동 항목 단가 검증
     *
     * 기존 규칙을 건드리지 않고,
     * 실제 반영 수량이 있는 근태연동 항목의 단가 0원 저장만 막는다.
     *
     * 규칙:
     * 1. 조정수당/조정공제는 생성됐다는 것 자체가 반영 대상이 있다는 뜻이므로 단가 필수
     * 2. 일반 연장수당은 연장분이 0분이면 단가 0 허용
     * 3. 일반 결근공제는 결근일수가 0일이면 단가 0 허용
     * 4. 실제 연장분/결근일수가 있으면 단가 0 불가
     */
    private void validateAttendanceLinkedUnitAmount(List<PayrollSaveRequestDTO.Item> items) {

        if (items == null || items.isEmpty()) {
            return;
        }

        for (PayrollSaveRequestDTO.Item item : items) {

            if (item == null) {
                continue;
            }

            String itemName =
                    item.getItemNameSnapshot() == null
                            ? ""
                            : item.getItemNameSnapshot().trim();

            BigDecimal amount =
                    item.getAmount() == null
                            ? BigDecimal.ZERO
                            : item.getAmount();

            boolean adjustmentAllowance =
                    itemName.startsWith("조정수당");

            boolean adjustmentDeduction =
                    itemName.startsWith("조정공제");

            /**
             * 조정항목은 생성 자체가 반영 대상 존재를 의미하므로
             * 단가 0원 허용 금지
             */
            if ((adjustmentAllowance || adjustmentDeduction)
                    && amount.compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "반영 대상 근태 또는 조정항목이 존재합니다.\n"
                                + "단가 입력 후 계산 미리보기를 진행해 주세요."
                );
            }

            /**
             * 일반 연장수당:
             * 연장분이 있을 때만 단가 필수
             */
            if ("OVERTIME".equals(item.getLinkedAttendanceType())
                    && !adjustmentAllowance) {

                int overtimeMinutes =
                        item.getOvertimeMinutes() == null
                                ? 0
                                : item.getOvertimeMinutes();

                if (overtimeMinutes > 0
                        && amount.compareTo(BigDecimal.ZERO) <= 0) {

                    throw new IllegalArgumentException(
                            "반영 대상 근태 또는 조정항목이 존재합니다.\n"
                                    + "단가 입력 후 계산 미리보기를 진행해 주세요."
                    );
                }
            }

            /**
             * 일반 결근공제:
             * 결근일수가 있을 때만 단가 필수
             */
            if ("ABSENCE".equals(item.getLinkedAttendanceType())
                    && !adjustmentDeduction) {

                int absenceDays =
                        item.getAbsenceDays() == null
                                ? 0
                                : item.getAbsenceDays();

                if (absenceDays > 0
                        && amount.compareTo(BigDecimal.ZERO) <= 0) {

                    throw new IllegalArgumentException(
                            "반영 대상 근태 또는 조정항목이 존재합니다.\n"
                                    + "단가 입력 후 계산 미리보기를 진행해 주세요."
                    );
                }
            }
        }
    }

    // 지급/공제항목 검증
    private void validatePayrollItems(List<PayrollSaveRequestDTO.Item> items) {

        if (items == null || items.isEmpty()) {
            return;
        }

        Set<String> itemNameSet = new HashSet<>();

        for (PayrollSaveRequestDTO.Item item : items) {

            if (item == null) {
                continue;
            }

            if (!StringUtils.hasText(item.getItemNameSnapshot())) {
                throw new IllegalArgumentException("지급/공제 항목명을 입력해 주세요.");
            }

            String itemName = item.getItemNameSnapshot().trim();

            /**
             * 조정항목은 PAY_ITEM_SETTING에 없는 자동 생성 항목이다.
             * 따라서 itemSettingId가 null이고 linkedAttendanceType도 null일 수 있다.
             *
             * 하지만 저장/검증/계산에서는 근태연동 유형이 필요하므로
             * 이름 기준으로 내부값만 복원한다.
             */
            if (itemName.startsWith("조정수당")) {
                item.setLinkedAttendanceType("OVERTIME");
            }

            if (itemName.startsWith("조정공제")) {
                item.setLinkedAttendanceType("ABSENCE");
            }

            // 기본급은 고정 항목
            if ("기본급".equals(itemName)) {
                throw new IllegalArgumentException("'기본급'은 지급/공제항목으로 등록할 수 없습니다.");
            }

            // 중복 방지
            if (itemNameSet.contains(itemName)) {
                throw new IllegalArgumentException("이미 등록된 지급/공제 항목명입니다.");
            }

            itemNameSet.add(itemName);

            // 지급/공제 구분 검증
            if (!"ALLOWANCE".equals(item.getItemType())
                    && !"DEDUCTION".equals(item.getItemType())) {
                throw new IllegalArgumentException("지급/공제 구분이 올바르지 않습니다.");
            }

            if ("ALLOWANCE".equals(item.getItemType())) {
                validateAllowanceItem(item);
            }

            if ("DEDUCTION".equals(item.getItemType())) {
                validateDeductionItem(item);
            }
        }
    }

    // 지급/공제항목 설정 저장 요청 검증
    private void validatePayItemSettingSaveRequest(PayItemSettingSaveRequestDTO requestDTO) {

        if (requestDTO == null || requestDTO.getItems() == null) {
            throw new IllegalArgumentException("저장할 지급/공제항목 설정 정보가 없습니다.");
        }

        Set<String> itemNameSet = new HashSet<>();

        for (PayItemSettingSaveRequestDTO.Item item : requestDTO.getItems()) {

            if (item == null) {
                continue;
            }

            // 항목명 입력 여부 검증
            if (!StringUtils.hasText(item.getItemName())) {
                throw new IllegalArgumentException("지급/공제 항목명을 입력해 주세요.");
            }

            // 근태연동 여부
            boolean attendanceLinked =
                    StringUtils.hasText(item.getLinkedAttendanceType());

            // 예약어/전용명 검증
            validateReservedItemName(
                    item.getItemName(),
                    attendanceLinked
            );

            String itemName = item.getItemName().trim();

            if (itemNameSet.contains(itemName)) {
                throw new IllegalArgumentException("이미 등록된 지급/공제 항목명입니다.");
            }

            itemNameSet.add(itemName);

            if (!"ALLOWANCE".equals(item.getItemType())
                    && !"DEDUCTION".equals(item.getItemType())) {
                throw new IllegalArgumentException("지급/공제 구분이 올바르지 않습니다.");
            }

            if ("ALLOWANCE".equals(item.getItemType())) {
                validatePayItemAllowanceSetting(item);
            }

            if ("DEDUCTION".equals(item.getItemType())) {
                validatePayItemDeductionSetting(item);
            }
        }
    }

    // 지급/공제 항목명 검증 - 기본급은 직접 추가 금지와 초과수당 / 결근공제는 근태연동 전용명으로 사용
    private void validateReservedItemName(String itemName, boolean attendanceLinked) {

        // 공백 제거
        String name = itemName.trim();

        // 기본급은 시스템 기본 항목이므로 직접 등록 금지
        if ("기본급".equals(name)) {
            throw new IllegalArgumentException("'기본급'은 등록할 수 없습니다.");
        }

        if ("조정수당".equals(name) || "조정공제".equals(name)) {
            throw new IllegalArgumentException("조정수당/조정공제는 시스템 자동 조정항목이므로 항목설정에서 등록할 수 없습니다.");
        }

        if (("연장수당".equals(name) || "결근공제".equals(name)) && !attendanceLinked) {
            throw new IllegalArgumentException("연장수당/결근공제는 근태연동 예 항목으로만 등록할 수 있습니다.");
        }
    }

    // 지급항목 설정 검증
    private void validatePayItemAllowanceSetting(PayItemSettingSaveRequestDTO.Item item) {

        if (!StringUtils.hasText(item.getTaxType())) {
            throw new IllegalArgumentException("지급항목의 과세/비과세 여부를 선택해 주세요.");
        }

        if (!"TAXABLE".equals(item.getTaxType())
                && !"NON_TAXABLE".equals(item.getTaxType())) {
            throw new IllegalArgumentException("과세 유형이 올바르지 않습니다.");
        }

        // 과세 지급항목에서만 근태연동 허용
        // 비과세 지급항목은 아래 NON_TAXABLE 분기에서 null 처리한다.
        if ("TAXABLE".equals(item.getTaxType())
                && StringUtils.hasText(item.getLinkedAttendanceType())) {

            if (!"OVERTIME".equals(item.getLinkedAttendanceType())) {
                throw new IllegalArgumentException("지급항목의 근태연동 유형이 올바르지 않습니다.");
            }

            // 지급 + 과세 + 근태연동 예 = 연장수당 고정
            item.setItemName("연장수당");
        }

        // 비과세 지급항목
        if ("NON_TAXABLE".equals(item.getTaxType())) {

            // 비과세 지급항목은 근태연동 불가
            item.setLinkedAttendanceType(null);

            if (!StringUtils.hasText(item.getNonTaxCode())) {
                throw new IllegalArgumentException("비과세 항목을 선택해 주세요.");
            }

            validateNonTaxCode(item.getNonTaxCode());

            return;
        }

        /*
         * 과세 지급항목
         * - 비과세 항목은 사용하지 않으므로 null 처리
         * - 항목명은 기본급 문구만 아니면 자유롭게 입력 가능
         */
        item.setNonTaxCode(null);
    }

    // 공제항목 설정 검증
    private void validatePayItemDeductionSetting(PayItemSettingSaveRequestDTO.Item item) {

        // 공제항목은 과세/비과세, 비과세 항목을 사용하지 않는다.
        item.setTaxType(null);
        item.setNonTaxCode(null);

        // 공제 + 근태연동 예 = 결근공제 고정
        if (StringUtils.hasText(item.getLinkedAttendanceType())) {

            if (!"ABSENCE".equals(item.getLinkedAttendanceType())) {
                throw new IllegalArgumentException("공제항목의 근태연동 유형이 올바르지 않습니다.");
            }

            item.setItemName("결근공제");
        }
    }

    // 지급항목 검증
    private void validateAllowanceItem(PayrollSaveRequestDTO.Item item) {

        if (!StringUtils.hasText(item.getTaxType())) {
            throw new IllegalArgumentException("지급항목의 과세/비과세 여부를 선택해 주세요.");
        }

        // 과세/비과세 값 검증
        if (!"TAXABLE".equals(item.getTaxType())
                && !"NON_TAXABLE".equals(item.getTaxType())) {
            throw new IllegalArgumentException("과세 유형이 올바르지 않습니다.");
        }

        // 비과세 선택 시
        if ("NON_TAXABLE".equals(item.getTaxType())) {

            if (!StringUtils.hasText(item.getNonTaxCode())) {
                throw new IllegalArgumentException("비과세 유형을 선택해 주세요.");
            }

            validateNonTaxCode(item.getNonTaxCode());
        }

        // 비과세 지급항목은 근태연동 불가
        if ("NON_TAXABLE".equals(item.getTaxType())
                && StringUtils.hasText(item.getLinkedAttendanceType())) {
            throw new IllegalArgumentException("비과세 지급항목은 근태연동으로 설정할 수 없습니다.");
        }

        // 과세 지급항목 중 근태연동 항목 검증
        if ("TAXABLE".equals(item.getTaxType())
                && StringUtils.hasText(item.getLinkedAttendanceType())) {

            if (!"OVERTIME".equals(item.getLinkedAttendanceType())) {
                throw new IllegalArgumentException("근태연동 지급 유형이 올바르지 않습니다.");
            }

            String itemName = item.getItemNameSnapshot().trim();

            /*
             * 근태연동 지급항목 허용
             * - 연장수당: 해당월 연장근무 수당
             * - 조정수당[yyyy-MM]: 과거 확정/지급완료월 이후 반영된 연장근무 조정항목
             */
            if (!"연장수당".equals(itemName)
                    && !itemName.startsWith("조정수당")) {

                throw new IllegalArgumentException(
                        "근태연동 지급항목은 '연장수당' 또는 조정수당이어야 합니다."
                );
            }

            // 근태연동 지급항목도 빈 값이면 0으로 처리한다.
            // 화면에서는 연장분이 없거나 단가를 입력하지 않은 경우 0원으로 계산되게 한다.
            if (item.getAmount() == null) {
                item.setAmount(BigDecimal.ZERO);
            }

            if (item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("근태연동 지급 기준값은 음수일 수 없습니다.");
            }

            return;
        }

        // 일반 지급항목은 blank면 0 처리
        if (item.getAmount() == null) {
            item.setAmount(BigDecimal.ZERO);
        }

        if (item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("지급항목 금액은 음수일 수 없습니다.");
        }
    }

    // 공제항목 검증
    private void validateDeductionItem(PayrollSaveRequestDTO.Item item) {

        // 공제항목은 과세/비과세 사용 안 함
        item.setTaxType(null);
        item.setNonTaxCode(null);

        // 근태연동 공제항목 검증
        if (StringUtils.hasText(item.getLinkedAttendanceType())) {

            if (!"ABSENCE".equals(item.getLinkedAttendanceType())) {
                throw new IllegalArgumentException("근태연동 공제 유형이 올바르지 않습니다.");
            }

            String itemName = item.getItemNameSnapshot().trim();

            /*
             * 근태연동 공제항목 허용
             * - 결근공제: 해당월 결근 공제
             * - 조정공제[yyyy-MM]: 과거 확정/지급완료월 이후 반영된 결근 조정항목
             */
            if (!"결근공제".equals(itemName)
                    && !itemName.startsWith("조정공제")) {

                throw new IllegalArgumentException(
                        "근태연동 공제항목은 '결근공제' 또는 조정공제여야 합니다."
                );
            }

            // 근태연동 공제항목도 빈 값이면 0으로 처리한다.
            // 결근일수가 없거나 단가를 입력하지 않은 경우 0원으로 계산되게 한다.
            if (item.getAmount() == null) {
                item.setAmount(BigDecimal.ZERO);
            }

            if (item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("근태연동 공제 기준값은 음수일 수 없습니다.");
            }

            return;
        }

        if (item.getAmount() == null) {
            item.setAmount(BigDecimal.ZERO);
        }

        if (item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("공제항목 금액은 음수일 수 없습니다.");
        }
    }

    // 비과세 유형 검증
    private void validateNonTaxCode(String nonTaxCode) {

        if (!"MEAL".equals(nonTaxCode)
                && !"CAR".equals(nonTaxCode)
                && !"RESEARCH".equals(nonTaxCode)
                && !"CHILDCARE".equals(nonTaxCode)
                && !"OVERSEAS".equals(nonTaxCode)) {

            throw new IllegalArgumentException("비과세 유형이 올바르지 않습니다.");
        }
    }

    // 지급/공제항목 저장
    private void savePayrollItems(
            PayrollEntity payroll,
            List<PayrollSaveRequestDTO.Item> items,
            PayrollPreviewResponseDTO preview
    ) {

        // 기존 항목 전체 삭제
        entityManager.createQuery("""
            delete from PayrollItemEntity item
             where item.payroll.payrollId = :payrollId
            """)
                .setParameter("payrollId", payroll.getPayrollId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        if (items == null || items.isEmpty()) {
            return;
        }

        PayrollEntity payrollRef =
                entityManager.find(PayrollEntity.class, payroll.getPayrollId());

        for (PayrollSaveRequestDTO.Item itemDTO : items) {

            PayItemSettingEntity itemSetting = null;

            if (itemDTO.getItemSettingId() != null) {

                itemSetting = entityManager.getReference(
                        PayItemSettingEntity.class,
                        itemDTO.getItemSettingId()
                );
            }

            PayrollPreviewResponseDTO.ItemRow previewRow = null;

            if (preview != null && preview.getItemRows() != null) {

                for (PayrollPreviewResponseDTO.ItemRow row : preview.getItemRows()) {

                    if (row == null) {
                        continue;
                    }

                    String rowName =
                            row.getItemNameSnapshot() == null
                                    ? ""
                                    : row.getItemNameSnapshot().trim();

                    String dtoName =
                            itemDTO.getItemNameSnapshot() == null
                                    ? ""
                                    : itemDTO.getItemNameSnapshot().trim();

                    String rowType =
                            row.getItemType() == null
                                    ? ""
                                    : row.getItemType();

                    String dtoType =
                            itemDTO.getItemType() == null
                                    ? ""
                                    : itemDTO.getItemType();

                    String rowLinked =
                            row.getLinkedAttendanceType() == null
                                    ? ""
                                    : row.getLinkedAttendanceType();

                    String dtoLinked =
                            itemDTO.getLinkedAttendanceType() == null
                                    ? ""
                                    : itemDTO.getLinkedAttendanceType();

                    /**
                     * previewRow 매칭 기준
                     *
                     * 기존에는 itemNameSnapshot만 비교해서
                     * 공백/중복/조정항목/근태연동 항목에서 매칭 실패 가능성이 있었다.
                     *
                     * 저장 당시 계산 snapshot이 payroll_item.taxableAmount에 들어가야
                     * DRAFT 재진입 시 근태 누적분을 감지할 수 있다.
                     */
                    boolean sameNormalItem =
                            rowName.equals(dtoName)
                                    && rowType.equals(dtoType)
                                    && rowLinked.equals(dtoLinked);

                    if (sameNormalItem) {
                        previewRow = row;
                        break;
                    }
                }
            }

            BigDecimal saveAmount = itemDTO.getAmount() == null
                    ? BigDecimal.ZERO
                    : itemDTO.getAmount();

            BigDecimal taxableAmount = BigDecimal.ZERO;
            BigDecimal nonTaxableAmount = BigDecimal.ZERO;

            if (previewRow != null) {
                /*
                 * amount는 최종 계산금액이 아니라 입력값/단가를 저장한다.
                 *
                 * 일반 지급/공제항목: 입력 금액
                 * OVERTIME: 60분당 단가
                 * ABSENCE: 하루당 공제단가
                 *
                 * 실제 계산 반영금액은 taxableAmount / nonTaxableAmount 또는
                 * PAYROLL.totalAllowance / totalDeduction 쪽에 반영된다.
                 */
                taxableAmount = previewRow.getTaxableAmount();
                nonTaxableAmount = previewRow.getNonTaxableAmount();
            }

            PayrollItemEntity item = PayrollItemEntity.builder()
                    .payroll(payrollRef)
                    .itemSetting(itemSetting)
                    .itemNameSnapshot(itemDTO.getItemNameSnapshot().trim())
                    .itemType(itemDTO.getItemType())
                    .taxType(itemDTO.getTaxType())
                    .nonTaxCode(itemDTO.getNonTaxCode())

                    // 계산 미리보기 전 단계
                    // - 3차에서 실제 계산값 저장 예정
                    .amount(saveAmount)
                    .taxableAmount(taxableAmount)
                    .nonTaxableAmount(nonTaxableAmount)
                    .isValidNonTax(true)
                    .build();

            entityManager.persist(item);
        }
    }

    // 현재 지급월의 연장분/결근일수를 메인 row에 세팅
    // - 연장수당 row에는 overtimeMinutes
    // - 결근공제 row에는 absenceDays
    // - 값이 없으면 0분 / 0일로 표시되게 한다.
    private void applyCurrentAttendanceSummaryToItems(List<PayrollItemLoadResponseDTO.Item> items, String empNo, Integer payYear, Integer payMonth) {

        if (items == null || items.isEmpty()) {
            return;
        }

        PayrollEmployeeInfoResponseDTO employeeInfo = getEmployeeInfo(empNo);

        LocalDate startDate = getPayMonthStartDate(
                payYear,
                payMonth,
                employeeInfo.getHireDate()
        );

        LocalDate endDate = getPayMonthEndDate(payYear, payMonth);

        PayrollAttendanceSummaryDTO summary =
                payrollRepository.selectAttendanceSummary(empNo, startDate, endDate);

        int overtimeMinutes =
                summary == null || summary.getOvertimeMinutes() == null
                        ? 0
                        : summary.getOvertimeMinutes();

        int absenceDays =
                summary == null || summary.getAbsenceDays() == null
                        ? 0
                        : summary.getAbsenceDays();

        for (PayrollItemLoadResponseDTO.Item item : items) {

            if (item == null) {
                continue;
            }

            /*
             * 조정수당/조정공제는 현재월 근태값을 넣으면 안 된다.
             * 조정항목은 appendDerivedAdjustmentItems()에서
             * 직전월 누락분 기준으로 따로 계산한다.
             */
            if (isAdjustmentSnapshotName(item.getItemNameSnapshot())) {
                continue;
            }

            if ("OVERTIME".equals(item.getLinkedAttendanceType())) {
                item.setOvertimeMinutes(overtimeMinutes);
                item.setDisplayItemName(item.getItemNameSnapshot());
            }

            if ("ABSENCE".equals(item.getLinkedAttendanceType())) {
                item.setAbsenceDays(absenceDays);
                item.setDisplayItemName(item.getItemNameSnapshot());
            }
        }
    }

    private void restoreAdjustmentMetaFromSnapshotName(
            List<PayrollItemLoadResponseDTO.Item> items
    ) {

        if (items == null || items.isEmpty()) {
            return;
        }

        for (PayrollItemLoadResponseDTO.Item item : items) {

            if (item == null || !StringUtils.hasText(item.getItemNameSnapshot())) {
                continue;
            }

            String itemName = item.getItemNameSnapshot();

            if (itemName.startsWith("조정수당[")) {
                item.setLinkedAttendanceType("OVERTIME");
                item.setDerivedAdjustment(true);
                item.setDisplayItemName("조정수당");
                item.setSourcePayMonth(
                        itemName.replace("조정수당[", "").replace("]", "")
                );
            }

            if (itemName.startsWith("조정공제[")) {
                item.setLinkedAttendanceType("ABSENCE");
                item.setDerivedAdjustment(true);
                item.setDisplayItemName("조정공제");
                item.setSourcePayMonth(
                        itemName.replace("조정공제[", "").replace("]", "")
                );
            }
        }
    }

    /**
     * CONFIRMED / PAID 조회 시
     * 저장된 PAYROLL_ITEM snapshot 기준으로
     * 연장분/결근일수 표시값을 복원한다.
     *
     * CONFIRMED / PAID에서는 현재 ATTENDANCE를 다시 조회하면 안 된다.
     */
    private void restoreAttendanceCountFromSavedSnapshot(
            List<PayrollItemLoadResponseDTO.Item> items
    ) {
        if (items == null || items.isEmpty()) {
            return;
        }

        System.out.println("===== snapshot 복원 시작 =====");

        for (PayrollItemLoadResponseDTO.Item item : items) {

            if (item == null) {
                continue;
            }

            System.out.println(
                    "item = "
                            + item.getItemNameSnapshot()
                            + " / linked="
                            + item.getLinkedAttendanceType()
                            + " / amount="
                            + item.getAmount()
                            + " / taxable="
                            + item.getTaxableAmount()
            );

            String linkedAttendanceType = item.getLinkedAttendanceType();

            if (!"OVERTIME".equals(linkedAttendanceType)
                    && !"ABSENCE".equals(linkedAttendanceType)) {
                continue;
            }

            BigDecimal unitAmount =
                    item.getAmount() == null
                            ? BigDecimal.ZERO
                            : item.getAmount();

            BigDecimal calculatedAmount =
                    item.getTaxableAmount() == null
                            ? BigDecimal.ZERO
                            : item.getTaxableAmount();

            int restoredCount = 0;

            if (unitAmount.compareTo(BigDecimal.ZERO) > 0
                    && calculatedAmount.compareTo(BigDecimal.ZERO) > 0) {

                BigDecimal count =
                        calculatedAmount.divide(
                                unitAmount,
                                10,
                                RoundingMode.HALF_UP
                        );

                if ("OVERTIME".equals(linkedAttendanceType)) {
                    count = count.multiply(BigDecimal.valueOf(60));
                }

                restoredCount =
                        count.setScale(0, RoundingMode.HALF_UP)
                                .intValue();
            }

            if ("OVERTIME".equals(linkedAttendanceType)) {
                item.setOvertimeMinutes(restoredCount);
                item.setAbsenceDays(0);
            }

            if ("ABSENCE".equals(linkedAttendanceType)) {
                item.setAbsenceDays(restoredCount);
                item.setOvertimeMinutes(0);
            }
        }
    }


    // 확정/지급완료 이후 뒤늦게 반영되어야 하는 전월 근태값을
    // 현재 급여월의 조정수당/조정공제로 1회만 붙인다.
    //
    // 중요 규칙
    // 1. 조정항목은 현재 급여월의 "직전월"만 본다.
    //    예) 2026-04 급여 → 2026-03 누락분만 확인
    // 2. 2026-04에서 2026-02 누락분을 다시 끌고 오면 안 된다.
    // 3. 직전월이 CONFIRMED/PAID가 아니면 조정항목을 만들지 않는다.
    // 4. 현재 급여월 안에서 근태연동 항목을 삭제했다가 다시 등록하면
    //    같은 월에서는 조정수당/조정공제가 다시 부활할 수 있어야 한다.
    // 5. 그래서 DB 전체의 조정항목 존재 여부로 막지 않고,
    //    현재 화면 currentItems 안에 이미 있는지만 확인한다.
    private void appendDerivedAdjustmentItems(
        List<PayrollItemLoadResponseDTO.Item> currentItems,
        String empNo,
        String currentPayMonth
    ) {

        if (currentItems == null || currentItems.isEmpty()) {
            return;
        }

        PayrollItemLoadResponseDTO.Item currentOvertimeItem = null;
        PayrollItemLoadResponseDTO.Item currentAbsenceItem = null;

        for (PayrollItemLoadResponseDTO.Item item : currentItems) {

            if (item == null) {
                continue;
            }

            if (isAdjustmentSnapshotName(item.getItemNameSnapshot())) {
                continue;
            }

            if ("OVERTIME".equals(item.getLinkedAttendanceType())) {
                currentOvertimeItem = item;
            }

            if ("ABSENCE".equals(item.getLinkedAttendanceType())) {
                currentAbsenceItem = item;
            }
        }

        // 현재 급여월에 근태연동 연장수당/결근공제가 없으면 조정항목도 만들지 않는다.
        if (currentOvertimeItem == null && currentAbsenceItem == null) {
            return;
        }

        // 조정항목은 현재 급여월의 직전월만 대상이다.
        String sourcePayMonth = getPreviousPayMonth(currentPayMonth);

        if (!StringUtils.hasText(sourcePayMonth)) {
            return;
        }

        // 직전월 급여가 확정 또는 지급완료 상태인지 확인한다.
        PayrollClosedMonthDTO closedPreviousMonth =
                findClosedPreviousMonth(empNo, currentPayMonth, sourcePayMonth);

        if (closedPreviousMonth == null) {
            return;
        }

        LocalDate sourceStartDate = LocalDate.parse(sourcePayMonth + "-01");
        LocalDate sourceEndDate =
                sourceStartDate.withDayOfMonth(sourceStartDate.lengthOfMonth());

        // 현재 ATTENDANCE 기준 최신 직전월 근태값
        PayrollAttendanceSummaryDTO latestSummary =
                payrollRepository.selectAttendanceSummary(
                        empNo,
                        sourceStartDate,
                        sourceEndDate
                );

        int latestOvertimeMinutes =
                latestSummary == null || latestSummary.getOvertimeMinutes() == null
                        ? 0
                        : latestSummary.getOvertimeMinutes();

        int latestAbsenceDays =
                latestSummary == null || latestSummary.getAbsenceDays() == null
                        ? 0
                        : latestSummary.getAbsenceDays();

        // 직전월 확정/지급완료 당시 PAYROLL_ITEM snapshot
        List<PayrollItemLoadResponseDTO.Item> savedSnapshotItems =
                payrollRepository.selectPayrollItemSnapshots(empNo, sourcePayMonth);

        int reflectedOvertimeMinutes =
                calculateReflectedCountFromSnapshot(
                        savedSnapshotItems,
                        "OVERTIME",
                        "연장수당"
                );

        int reflectedAbsenceDays =
                calculateReflectedCountFromSnapshot(
                        savedSnapshotItems,
                        "ABSENCE",
                        "결근공제"
                );

        int adjustmentOvertimeMinutes =
                Math.max(0, latestOvertimeMinutes - reflectedOvertimeMinutes);

        int adjustmentAbsenceDays =
                Math.max(0, latestAbsenceDays - reflectedAbsenceDays);

        // 조정수당 대상 분이 0이면 기존 조정수당 row가 남아있지 않도록 제거한다.
        if (adjustmentOvertimeMinutes <= 0) {
            currentItems.removeIf(item ->
                    item != null
                            && ("조정수당[" + sourcePayMonth + "]")
                            .equals(item.getItemNameSnapshot())
            );
        }

        // 조정수당 생성
        if (currentOvertimeItem != null
                && adjustmentOvertimeMinutes > 0) {

            String snapshotName =
                    "조정수당[" + sourcePayMonth + "]";

            PayrollItemLoadResponseDTO.Item existingAdjustment =
                    findItemBySnapshotName(
                            currentItems,
                            snapshotName
                    );

            // 이미 저장된 조정수당 row가 있으면
            // 분/조정항목 정보 다시 세팅
            if (existingAdjustment != null) {

                existingAdjustment.setDisplayItemName("조정수당");
                existingAdjustment.setItemType("ALLOWANCE");
                existingAdjustment.setTaxType("TAXABLE");
                existingAdjustment.setNonTaxCode(null);
                existingAdjustment.setLinkedAttendanceType("OVERTIME");
                existingAdjustment.setOvertimeMinutes(adjustmentOvertimeMinutes);
                existingAdjustment.setAbsenceDays(0);
                existingAdjustment.setDerivedAdjustment(true);
                existingAdjustment.setSourcePayMonth(sourcePayMonth);
                existingAdjustment.setItemType("ALLOWANCE");
                existingAdjustment.setTaxType("TAXABLE");
                existingAdjustment.setNonTaxCode(null);

                if (existingAdjustment.getAmount() == null) {

                    existingAdjustment.setAmount(
                            currentOvertimeItem.getAmount() == null
                                    ? BigDecimal.ZERO
                                    : currentOvertimeItem.getAmount()
                    );
                }

            } else {

                PayrollItemLoadResponseDTO.Item adjustment =
                        new PayrollItemLoadResponseDTO.Item();

                adjustment.setItemSettingId(null);
                adjustment.setItemNameSnapshot(snapshotName);
                adjustment.setDisplayItemName("조정수당");
                adjustment.setItemType("ALLOWANCE");
                adjustment.setTaxType("TAXABLE");
                adjustment.setNonTaxCode(null);
                adjustment.setLinkedAttendanceType("OVERTIME");
                adjustment.setOvertimeMinutes(adjustmentOvertimeMinutes);
                adjustment.setAbsenceDays(0);
                adjustment.setAmount(
                        currentOvertimeItem.getAmount() == null
                                ? BigDecimal.ZERO
                                : currentOvertimeItem.getAmount()
                );
                adjustment.setDerivedAdjustment(true);
                adjustment.setSourcePayMonth(sourcePayMonth);

                currentItems.add(adjustment);
            }
        }

        // 조정공제 대상 일수가 0이면 기존 조정공제 row가 남아있지 않도록 제거한다.
        if (adjustmentAbsenceDays <= 0) {
            currentItems.removeIf(item ->
                    item != null
                            && ("조정공제[" + sourcePayMonth + "]")
                            .equals(item.getItemNameSnapshot())
            );
        }

        // 조정공제 생성
        if (currentAbsenceItem != null
                && adjustmentAbsenceDays > 0) {

            String snapshotName =
                    "조정공제[" + sourcePayMonth + "]";

            PayrollItemLoadResponseDTO.Item existingAdjustment =
                    findItemBySnapshotName(
                            currentItems,
                            snapshotName
                    );

            // 이미 저장된 조정공제 row가 있으면
            // 결근일수 다시 세팅
            if (existingAdjustment != null) {

                existingAdjustment.setDisplayItemName("조정공제");
                existingAdjustment.setItemType("DEDUCTION");
                existingAdjustment.setTaxType(null);
                existingAdjustment.setNonTaxCode(null);
                existingAdjustment.setLinkedAttendanceType("ABSENCE");
                existingAdjustment.setOvertimeMinutes(0);
                existingAdjustment.setAbsenceDays(adjustmentAbsenceDays);
                existingAdjustment.setDerivedAdjustment(true);
                existingAdjustment.setSourcePayMonth(sourcePayMonth);
                existingAdjustment.setItemType("DEDUCTION");
                existingAdjustment.setTaxType(null);
                existingAdjustment.setNonTaxCode(null);

                if (existingAdjustment.getAmount() == null) {

                    existingAdjustment.setAmount(
                            currentAbsenceItem.getAmount() == null
                                    ? BigDecimal.ZERO
                                    : currentAbsenceItem.getAmount()
                    );
                }

            } else {

                PayrollItemLoadResponseDTO.Item adjustment =
                        new PayrollItemLoadResponseDTO.Item();

                adjustment.setItemSettingId(null);
                adjustment.setItemNameSnapshot(snapshotName);
                adjustment.setDisplayItemName("조정공제");
                adjustment.setItemType("DEDUCTION");
                adjustment.setTaxType(null);
                adjustment.setNonTaxCode(null);
                adjustment.setLinkedAttendanceType("ABSENCE");
                adjustment.setOvertimeMinutes(0);
                adjustment.setAbsenceDays(adjustmentAbsenceDays);
                adjustment.setAmount(
                        currentAbsenceItem.getAmount() == null
                                ? BigDecimal.ZERO
                                : currentAbsenceItem.getAmount()
                );
                adjustment.setDerivedAdjustment(true);
                adjustment.setSourcePayMonth(sourcePayMonth);

                currentItems.add(adjustment);
            }
        }
    }

    // 현재 급여월의 직전월을 만든다.
    // 예) 2026-03 -> 2026-02
    private String getPreviousPayMonth(String currentPayMonth) {

        if (!StringUtils.hasText(currentPayMonth)) {
            return null;
        }

        YearMonth current = YearMonth.parse(currentPayMonth);

        return current.minusMonths(1).toString();
    }

    // 기존 repository 메서드는 현재월 이전의 확정/지급완료 월 전체를 가져온다.
    // 하지만 조정항목은 직전월 1회만 허용하므로 여기서 sourcePayMonth 하나로 좁힌다.
    private PayrollClosedMonthDTO findClosedPreviousMonth(
            String empNo,
            String currentPayMonth,
            String sourcePayMonth
    ) {

        List<PayrollClosedMonthDTO> closedMonths =
                payrollRepository.selectClosedPayrollMonthsBefore(empNo, currentPayMonth);

        if (closedMonths == null || closedMonths.isEmpty()) {
            return null;
        }

        for (PayrollClosedMonthDTO closedMonth : closedMonths) {

            if (closedMonth == null) {
                continue;
            }

            if (sourcePayMonth.equals(closedMonth.getPayMonth())) {
                return closedMonth;
            }
        }

        return null;
    }

    // 현재 화면 목록 안에 같은 조정항목이 이미 있는지만 확인한다.
    // DB 전체 기준으로 막으면 같은 급여월에서 삭제 후 재등록 시 조정항목 부활이 막힌다.
    private boolean containsSnapshotName(List<PayrollItemLoadResponseDTO.Item> items, String snapshotName) {

        if (items == null || items.isEmpty()) {
            return false;
        }

        for (PayrollItemLoadResponseDTO.Item item : items) {

            if (item == null) {
                continue;
            }

            if (snapshotName.equals(item.getItemNameSnapshot())) {
                return true;
            }
        }

        return false;
    }

    // 현재 항목 목록에서 itemNameSnapshot이 같은 항목을 찾는다.
    // 저장 후 다시 조회된 조정수당/조정공제에
    // 연장분/결근일수/조정항목 여부를 다시 세팅하기 위해 사용한다.
    private PayrollItemLoadResponseDTO.Item findItemBySnapshotName(
            List<PayrollItemLoadResponseDTO.Item> items,
            String snapshotName
    ) {
        if (items == null || !StringUtils.hasText(snapshotName)) {
            return null;
        }

        for (PayrollItemLoadResponseDTO.Item item : items) {
            if (item == null || !StringUtils.hasText(item.getItemNameSnapshot())) {
                continue;
            }

            if (snapshotName.equals(item.getItemNameSnapshot())) {
                return item;
            }
        }

        return null;
    }


    // 확정/지급완료 당시 이미 반영된 연장분/결근일수를 역산한다.
    // 계산식:
    // - 반영 개수 = PAYROLL_ITEM.taxableAmount / PAYROLL_ITEM.amount
    // - amount: 단가
    // - taxableAmount: 당시 실제 계산 반영금액 snapshot
    private int calculateReflectedCountFromSnapshot(
            List<PayrollItemLoadResponseDTO.Item> snapshotItems,
            String linkedAttendanceType,
            String baseItemName
    ) {

        if (snapshotItems == null || snapshotItems.isEmpty()) {
            return 0;
        }

        BigDecimal totalCount = BigDecimal.ZERO;

        for (PayrollItemLoadResponseDTO.Item item : snapshotItems) {

            if (item == null) {
                continue;
            }

            if (!linkedAttendanceType.equals(item.getLinkedAttendanceType())) {
                continue;
            }

            if (!baseItemName.equals(item.getItemNameSnapshot())) {
                continue;
            }

            BigDecimal unitAmount =
                    item.getAmount() == null
                            ? BigDecimal.ZERO
                            : item.getAmount();

            BigDecimal calculatedAmount =
                    item.getTaxableAmount() == null
                            ? BigDecimal.ZERO
                            : item.getTaxableAmount();

            if (unitAmount.compareTo(BigDecimal.ZERO) <= 0
                    || calculatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal count =
                    calculatedAmount.divide(
                            unitAmount,
                            10,
                            RoundingMode.HALF_UP
                    );

            if ("OVERTIME".equals(linkedAttendanceType)) {
                count = count.multiply(BigDecimal.valueOf(60));
            }

            totalCount = totalCount.add(count);
        }

        return totalCount
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * DRAFT 저장 이후 근태연동 계산값이 바뀌었는지 판단한다.
     *
     * 판단 방식:
     * 1. 저장 당시 반영된 값
     *    - PAYROLL_ITEM snapshot의 amount / taxableAmount로 역산
     *
     * 2. 현재 반영되어야 하는 값
     *    - ATTENDANCE 기준으로 다시 계산
     *
     * 3. 둘이 다르면
     *    - 4대보험 / 세금 / 합계 / 실수령액은 더 이상 신뢰할 수 없으므로
     *      계산 미리보기를 다시 요구한다.
     *
     * 이 방식은 sessionStorage를 사용하지 않으므로
     * 다른 PC / 브라우저 종료 / 재로그인 상황에서도 동일하게 동작한다.
     */
    private boolean isAttendanceInvalidationRequired(
            String empNo,
            String currentPayMonth,
            List<PayrollItemLoadResponseDTO.Item> savedSnapshotItems
    ) {

        if (savedSnapshotItems == null || savedSnapshotItems.isEmpty()) {
            return false;
        }

        YearMonth currentYm = YearMonth.parse(currentPayMonth);

        PayrollEmployeeInfoResponseDTO employeeInfo = getEmployeeInfo(empNo);

        LocalDate currentStartDate = getPayMonthStartDate(
                currentYm.getYear(),
                currentYm.getMonthValue(),
                employeeInfo.getHireDate()
        );

        LocalDate currentEndDate = getPayMonthEndDate(
                currentYm.getYear(),
                currentYm.getMonthValue()
        );

        /*
         * 현재월 ATTENDANCE 기준 최신 근태값
         * - 연장: 승인 완료 후 ATTENDANCE.overtimeMins에 반영된 값
         * - 결근: ATTENDANCE.status = ABSENT 기준
         */
        PayrollAttendanceSummaryDTO currentSummary =
                payrollRepository.selectAttendanceSummary(
                        empNo,
                        currentStartDate,
                        currentEndDate
                );

        int latestCurrentOvertimeMinutes =
                currentSummary == null || currentSummary.getOvertimeMinutes() == null
                        ? 0
                        : currentSummary.getOvertimeMinutes();

        int latestCurrentAbsenceDays =
                currentSummary == null || currentSummary.getAbsenceDays() == null
                        ? 0
                        : currentSummary.getAbsenceDays();

        /*
         * DRAFT 저장 당시 PAYROLL_ITEM snapshot에 반영된 현재월 근태값
         */
        int savedCurrentOvertimeMinutes =
                calculateSavedAttendanceCountFromSnapshot(
                        savedSnapshotItems,
                        "OVERTIME",
                        false,
                        currentPayMonth
                );

        int savedCurrentAbsenceDays =
                calculateSavedAttendanceCountFromSnapshot(
                        savedSnapshotItems,
                        "ABSENCE",
                        false,
                        currentPayMonth
                );

        /*
         * 조정항목은 직전월만 본다.
         * 예)
         * - 2026-05 급여에서는 2026-04 누락분만 조정
         * - 2026-06 급여에서 2026-04 누락분을 다시 끌고 오면 안 됨
         */
        String sourcePayMonth = getPreviousPayMonth(currentPayMonth);

        int latestAdjustmentOvertimeMinutes = 0;
        int latestAdjustmentAbsenceDays = 0;
        int savedAdjustmentOvertimeMinutes = 0;
        int savedAdjustmentAbsenceDays = 0;

        /**
         * 조정항목은 직전월 확정/지급완료가 있을 때만 비교한다.
         * 단, 직전월 확정이 없다고 해서 현재월 연장/결근 누적 감지를 죽이면 안 된다.
         */
        if (StringUtils.hasText(sourcePayMonth)) {

            PayrollClosedMonthDTO closedPreviousMonth =
                    findClosedPreviousMonth(empNo, currentPayMonth, sourcePayMonth);

            if (closedPreviousMonth != null) {

                AdjustmentCount latestAdjustmentCount =
                        calculateLatestAdjustmentCount(
                                empNo,
                                sourcePayMonth
                        );

                savedAdjustmentOvertimeMinutes =
                        calculateSavedAttendanceCountFromSnapshot(
                                savedSnapshotItems,
                                "OVERTIME",
                                true,
                                sourcePayMonth
                        );

                savedAdjustmentAbsenceDays =
                        calculateSavedAttendanceCountFromSnapshot(
                                savedSnapshotItems,
                                "ABSENCE",
                                true,
                                sourcePayMonth
                        );

                latestAdjustmentOvertimeMinutes =
                        latestAdjustmentCount == null
                                ? 0
                                : latestAdjustmentCount.getOvertimeMinutes();

                latestAdjustmentAbsenceDays =
                        latestAdjustmentCount == null
                                ? 0
                                : latestAdjustmentCount.getAbsenceDays();
            }
        }
        /*
         * 현재 기준으로 다시 계산한 직전월 조정 대상 개수
         */
        AdjustmentCount latestAdjustmentCount =
                calculateLatestAdjustmentCount(
                        empNo,
                        sourcePayMonth
                );

        boolean hasOvertimeItem =
                savedSnapshotItems.stream()
                        .anyMatch(item ->
                                item != null
                                        && "OVERTIME".equals(item.getLinkedAttendanceType())
                        );

        boolean hasAbsenceItem =
                savedSnapshotItems.stream()
                        .anyMatch(item ->
                                item != null
                                        && "ABSENCE".equals(item.getLinkedAttendanceType())
                        );

        if (!hasOvertimeItem) {
            latestCurrentOvertimeMinutes = 0;
            savedCurrentOvertimeMinutes = 0;
            latestAdjustmentOvertimeMinutes = 0;
            savedAdjustmentOvertimeMinutes = 0;
        }

        if (!hasAbsenceItem) {
            latestCurrentAbsenceDays = 0;
            savedCurrentAbsenceDays = 0;
            latestAdjustmentAbsenceDays = 0;
            savedAdjustmentAbsenceDays = 0;
        }

        System.out.println("===== 근태 무효화 비교 =====");
        System.out.println("현재월 최신 연장분 = " + latestCurrentOvertimeMinutes);
        System.out.println("현재월 저장 연장분 = " + savedCurrentOvertimeMinutes);
        System.out.println("현재월 최신 결근일수 = " + latestCurrentAbsenceDays);
        System.out.println("현재월 저장 결근일수 = " + savedCurrentAbsenceDays);
        System.out.println("조정 최신 연장분 = " + latestAdjustmentOvertimeMinutes);
        System.out.println("조정 저장 연장분 = " + savedAdjustmentOvertimeMinutes);
        System.out.println("조정 최신 결근일수 = " + latestAdjustmentAbsenceDays);
        System.out.println("조정 저장 결근일수 = " + savedAdjustmentAbsenceDays);

        boolean invalidationResult =
                latestCurrentOvertimeMinutes != savedCurrentOvertimeMinutes
                        || latestCurrentAbsenceDays != savedCurrentAbsenceDays
                        || latestAdjustmentOvertimeMinutes != savedAdjustmentOvertimeMinutes
                        || latestAdjustmentAbsenceDays != savedAdjustmentAbsenceDays;

        System.out.println("근태 무효화 최종 결과 = " + invalidationResult);

        return invalidationResult;
    }

    /**
     * 저장된 계산 결과가 있는 DRAFT인지 판단한다.
     *
     * 계산값이 없는 DRAFT는 초기화할 4대보험/세금 값이 없으므로
     * 근태값이 달라도 무효화 알림을 띄우지 않는다.
     */
    private boolean hasSavedCalculationValue(PayrollStatusResponseDTO statusDTO) {

        if (statusDTO == null) {
            return false;
        }

        return statusDTO.getNationalPensionAmount() != null
                || statusDTO.getHealthInsuranceAmount() != null
                || statusDTO.getLongTermCareAmount() != null
                || statusDTO.getEmploymentInsuranceAmount() != null
                || statusDTO.getTotalInsurance() != null
                || statusDTO.getIncomeTax() != null
                || statusDTO.getLocalIncomeTax() != null
                || statusDTO.getTotalGross() != null
                || statusDTO.getNetSalary() != null;
    }

    /**
     * 현재 기준으로 직전월 조정 대상 근태값을 계산한다.
     *
     * 조정항목 규칙:
     * 1. 현재월 기준 직전월만 본다.
     * 2. 직전월 급여가 CONFIRMED / PAID 상태여야 한다.
     * 3. 직전월 확정 당시 이미 반영된 값은 제외한다.
     * 4. 남은 차이분만 현재월 조정수당/조정공제로 본다.
     * 5. 다음다음월로 재이월하지 않는다.
     */
    private AdjustmentCount calculateLatestAdjustmentCount(
            String empNo,
            String sourcePayMonth
    ) {

        LocalDate sourceStartDate = LocalDate.parse(sourcePayMonth + "-01");
        LocalDate sourceEndDate =
                sourceStartDate.withDayOfMonth(sourceStartDate.lengthOfMonth());

        /*
         * 현재 ATTENDANCE 기준 직전월 전체 근태값
         */
        PayrollAttendanceSummaryDTO latestSummary =
                payrollRepository.selectAttendanceSummary(
                        empNo,
                        sourceStartDate,
                        sourceEndDate
                );

        int latestOvertimeMinutes =
                latestSummary == null || latestSummary.getOvertimeMinutes() == null
                        ? 0
                        : latestSummary.getOvertimeMinutes();

        int latestAbsenceDays =
                latestSummary == null || latestSummary.getAbsenceDays() == null
                        ? 0
                        : latestSummary.getAbsenceDays();

        /*
         * 직전월 확정/지급완료 당시 이미 반영된 PAYROLL_ITEM snapshot
         */
        List<PayrollItemLoadResponseDTO.Item> sourceSnapshotItems =
                payrollRepository.selectPayrollItemSnapshots(
                        empNo,
                        sourcePayMonth
                );

        int reflectedOvertimeMinutes =
                calculateSavedAttendanceCountFromSnapshot(
                        sourceSnapshotItems,
                        "OVERTIME",
                        false,
                        sourcePayMonth
                );

        int reflectedAbsenceDays =
                calculateSavedAttendanceCountFromSnapshot(
                        sourceSnapshotItems,
                        "ABSENCE",
                        false,
                        sourcePayMonth
                );

        int adjustmentOvertimeMinutes =
                Math.max(0, latestOvertimeMinutes - reflectedOvertimeMinutes);

        int adjustmentAbsenceDays =
                Math.max(0, latestAbsenceDays - reflectedAbsenceDays);

        return new AdjustmentCount(
                adjustmentOvertimeMinutes,
                adjustmentAbsenceDays
        );
    }

    /**
     * PAYROLL_ITEM snapshot에서 저장 당시 반영된 근태 개수를 역산한다.
     *
     * amount 의미:
     * - OVERTIME: 60분당 단가
     * - ABSENCE: 1일당 공제단가
     *
     * taxableAmount 의미:
     * - 실제 계산에 반영된 금액
     *
     * 역산:
     * - OVERTIME 분 = taxableAmount / amount * 60
     * - ABSENCE 일 = taxableAmount / amount
     *
     * adjustmentOnly:
     * - false: 일반 연장수당/결근공제만 계산
     * - true : 조정수당[sourcePayMonth] / 조정공제[sourcePayMonth]만 계산
     */
    private int calculateSavedAttendanceCountFromSnapshot(
            List<PayrollItemLoadResponseDTO.Item> snapshotItems,
            String linkedAttendanceType,
            boolean adjustmentOnly,
            String sourcePayMonth
    ) {

        if (snapshotItems == null || snapshotItems.isEmpty()) {
            return 0;
        }

        BigDecimal totalCount = BigDecimal.ZERO;

        for (PayrollItemLoadResponseDTO.Item item : snapshotItems) {

            if (item == null) {
                continue;
            }

            if (!linkedAttendanceType.equals(item.getLinkedAttendanceType())) {
                continue;
            }

            String itemName =
                    item.getItemNameSnapshot() == null
                            ? ""
                            : item.getItemNameSnapshot();

            boolean adjustmentItem =
                    itemName.startsWith("조정수당[")
                            || itemName.startsWith("조정공제[");

            /*
             * 일반 근태연동 항목 계산
             * - 조정항목은 제외한다.
             */
            if (!adjustmentOnly && adjustmentItem) {
                continue;
            }

            /*
             * 조정항목 계산
             * - 현재월 기준 직전월 sourcePayMonth에 해당하는 조정항목만 본다.
             * - 예) 조정수당[2026-04]
             */
            if (adjustmentOnly) {

                String expectedName =
                        "OVERTIME".equals(linkedAttendanceType)
                                ? "조정수당[" + sourcePayMonth + "]"
                                : "조정공제[" + sourcePayMonth + "]";

                if (!expectedName.equals(itemName)) {
                    continue;
                }
            }

            BigDecimal unitAmount =
                    item.getAmount() == null
                            ? BigDecimal.ZERO
                            : item.getAmount();

            BigDecimal calculatedAmount =
                    item.getTaxableAmount() == null
                            ? BigDecimal.ZERO
                            : item.getTaxableAmount();

/**
 * 조정항목은 저장 직후 taxableAmount가 0으로 저장되는 경우가 있다.
 *
 * 이 경우 기존 역산 방식:
 *   taxableAmount / amount
 * 로는 조정수당 120분을 0분으로 판단해서
 * 저장 후 재진입 시 근태변경 알림이 반복된다.
 *
 * 따라서 조정항목이고 taxableAmount가 0이면
 * 화면/DTO에 복원되어 있는 overtimeMinutes / absenceDays 값을 우선 사용한다.
 */
            if (adjustmentOnly && calculatedAmount.compareTo(BigDecimal.ZERO) <= 0) {

                if ("OVERTIME".equals(linkedAttendanceType)
                        && item.getOvertimeMinutes() != null) {
                    totalCount = totalCount.add(BigDecimal.valueOf(item.getOvertimeMinutes()));
                    continue;
                }

                if ("ABSENCE".equals(linkedAttendanceType)
                        && item.getAbsenceDays() != null) {
                    totalCount = totalCount.add(BigDecimal.valueOf(item.getAbsenceDays()));
                    continue;
                }
            }

            if (unitAmount.compareTo(BigDecimal.ZERO) <= 0
                    || calculatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal count =
                    calculatedAmount.divide(
                            unitAmount,
                            10,
                            RoundingMode.HALF_UP
                    );

            if ("OVERTIME".equals(linkedAttendanceType)) {
                count = count.multiply(BigDecimal.valueOf(60));
            }

            totalCount = totalCount.add(count);
        }

        return totalCount
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    // 선택 연도 기준 사용 가능한 월 목록 생성
    private List<Integer> makeAvailableMonths(Integer selectedYear, LocalDate hireDate) {

        LocalDate now = LocalDate.now();

        int startMonth = 1;
        int endMonth = 12;

        // 입사년도면 입사월부터
        if (hireDate != null && selectedYear == hireDate.getYear()) {
            startMonth = hireDate.getMonthValue();
        }

        // 현재년도면 현재월까지만
        if (selectedYear == now.getYear()) {
            endMonth = now.getMonthValue();
        }

        List<Integer> monthList = new java.util.ArrayList<>();

        for (int month = startMonth; month <= endMonth; month++) {
            monthList.add(month);
        }

        return monthList;
    }

    // 급여등급 순서 추출
    // - G1, G2, G3 같은 gradeId에서 숫자 부분만 꺼내 비교한다.
    // - 숫자가 클수록 높은 등급으로 판단한다.
    private int getGradeOrder(String gradeId) {

        if (!StringUtils.hasText(gradeId)) {
            return 0;
        }

        try {
            return Integer.parseInt(gradeId.replace("G", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 지급월 시작일 계산
    private LocalDate getPayMonthStartDate(Integer year, Integer month, LocalDate hireDate) {

        LocalDate startDate = LocalDate.of(year, month, 1);

        // 입사월이면 입사일부터 집계
        if (hireDate != null
                && hireDate.getYear() == year
                && hireDate.getMonthValue() == month) {
            return hireDate;
        }

        return startDate;
    }

    // 지급월 종료일 계산
    private LocalDate getPayMonthEndDate(Integer year, Integer month) {

        LocalDate now = LocalDate.now();

        LocalDate endDate = LocalDate.of(year, month, 1)
                .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());

        // 현재월이면 오늘까지만 집계
        if (year == now.getYear() && month == now.getMonthValue()) {
            return now;
        }

        return endDate;
    }

    // 토/일 제외 근무예정일수 계산
    private int countWorkingDays(LocalDate startDate, LocalDate endDate) {

        int count = 0;
        LocalDate date = startDate;

        while (!date.isAfter(endDate)) {

            int dayOfWeek = date.getDayOfWeek().getValue();

            // 토요일 6, 일요일 7 제외
            if (dayOfWeek != 6 && dayOfWeek != 7) {
                count++;
            }

            date = date.plusDays(1);
        }

        return count;
    }
    // YYYY-MM 생성
    private String makePayMonth(Integer year, Integer month) {

        if (month < 10) {
            return year + "-0" + month;
        }

        return year + "-" + month;
    }

    private BigDecimal decideKeepAmountAfterApply(
            PayrollItemLoadResponseDTO.Item newItem,
            List<PayrollItemLoadResponseDTO.Item> savedItems
    ) {

        if (newItem == null || savedItems == null) {
            return BigDecimal.ZERO;
        }

        for (PayrollItemLoadResponseDTO.Item oldItem : savedItems) {

            if (oldItem == null) {
                continue;
            }

            /*
             * 조정수당/조정공제는 itemSettingId가 없으므로
             * 항목명 기준으로만 금액을 유지한다.
             */
            if (isAdjustmentSnapshotName(newItem.getItemNameSnapshot())
                    && isAdjustmentSnapshotName(oldItem.getItemNameSnapshot())
                    && newItem.getItemNameSnapshot().equals(oldItem.getItemNameSnapshot())) {

                return oldItem.getAmount() == null
                        ? BigDecimal.ZERO
                        : oldItem.getAmount();
            }

            /*
             * 일반항목:
             * itemSettingId가 같고 지급/공제 구분, 근태연동 그룹이 같으면 금액 유지.
             * 과세/비과세, 비과세코드 변경은 금액 유지 대상이다.
             */
            if (newItem.getItemSettingId() != null
                    && oldItem.getItemSettingId() != null
                    && newItem.getItemSettingId().equals(oldItem.getItemSettingId())
                    && java.util.Objects.equals(newItem.getItemType(), oldItem.getItemType())
                    && java.util.Objects.equals(
                    getAttendanceAmountGroup(newItem.getLinkedAttendanceType()),
                    getAttendanceAmountGroup(oldItem.getLinkedAttendanceType())
            )) {

                return oldItem.getAmount() == null
                        ? BigDecimal.ZERO
                        : oldItem.getAmount();
            }
        }

        return BigDecimal.ZERO;
    }

    private boolean isAdjustmentSnapshotName(String itemName) {

        if (!StringUtils.hasText(itemName)) {
            return false;
        }

        return itemName.startsWith("조정수당[")
                || itemName.startsWith("조정공제[");
    }

    /**
     * DRAFT 저장 snapshot과 현재 PAY_ITEM_SETTING 구성이
     * 실제 항목 내용 기준으로 같은지 비교한다.
     *
     * updatedAt만 비교하면
     * 다른 사원/다른 월에서 항목설정을 저장했지만
     * 내용은 같은 경우에도 항목변경권고가 뜰 수 있다.
     */
    private boolean isSamePayItemSettingStructure(
            List<PayrollItemLoadResponseDTO.Item> savedItems,
            List<PayrollItemLoadResponseDTO.Item> latestItems
    ) {
        List<String> savedKeys = makePayItemStructureKeys(savedItems);
        List<String> latestKeys = makePayItemStructureKeys(latestItems);

        return savedKeys.equals(latestKeys);
    }

    /**
     * 항목 비교용 key 생성.
     *
     * 비교 기준:
     * - 항목명
     * - 지급/공제
     * - 과세/비과세
     * - 비과세 코드
     * - 근태연동 유형
     *
     * 조정수당/조정공제는 시스템 자동항목이므로 비교 제외한다.
     */
    private List<String> makePayItemStructureKeys(List<PayrollItemLoadResponseDTO.Item> items) {

        if (items == null || items.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        return items.stream()
                .filter(item -> item != null)
                .filter(item -> !isAdjustmentSnapshotName(item.getItemNameSnapshot()))
                .map(item ->
                        nvlText(item.getItemNameSnapshot()) + "|"
                                + nvlText(item.getItemType()) + "|"
                                + nvlText(item.getTaxType()) + "|"
                                + nvlText(item.getNonTaxCode()) + "|"
                                + nvlText(item.getLinkedAttendanceType())
                )
                .sorted()
                .toList();
    }

    private String nvlText(String value) {
        return value == null ? "" : value.trim();
    }

    private String getAttendanceAmountGroup(String linkedAttendanceType) {

        if ("OVERTIME".equals(linkedAttendanceType)) {
            return "OVERTIME";
        }

        if ("ABSENCE".equals(linkedAttendanceType)) {
            return "ABSENCE";
        }

        return "NONE";
    }

    /**
     * 조정항목 계산 결과를 담는 내부 전용 클래스
     *
     * 별도 DTO 파일을 만들 수도 있지만,
     * 현재는 AdPayrollServiceImpl 내부 계산에서만 쓰므로
     * 내부 클래스로 둔다.
     */
    private static class AdjustmentCount {

        private final int overtimeMinutes;
        private final int absenceDays;

        private AdjustmentCount(int overtimeMinutes, int absenceDays) {
            this.overtimeMinutes = overtimeMinutes;
            this.absenceDays = absenceDays;
        }

        public int getOvertimeMinutes() {
            return overtimeMinutes;
        }

        public int getAbsenceDays() {
            return absenceDays;
        }
    }
}

