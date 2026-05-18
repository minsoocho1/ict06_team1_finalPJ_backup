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
        PayrollPreviewResponseDTO preview =
                calculatePayrollPreview(requestDTO, employeeInfo, payMonth);

        EmpEntity employee = entityManager.getReference(EmpEntity.class, requestDTO.getEmpNo());

        GradeCodeEntity grade = null;
        if (StringUtils.hasText(employeeInfo.getGradeId())) {
            grade = entityManager.getReference(GradeCodeEntity.class, employeeInfo.getGradeId());
        }

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
                    .setParameter("totalAllowance", preview.getTotalAllowance())
                    .setParameter("taxableIncome", preview.getTaxableIncome())
                    .setParameter("now", now)
                    .executeUpdate();
        }

        // 기존 지급/공제항목 삭제 후 현재 화면 기준으로 다시 저장
        savePayrollItems(payroll, requestDTO.getItems(), preview);

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
         * - 저장된 급여대장이 없으므로 현재 활성 지급/공제항목 설정을 기본값으로 내려준다.
         */
        if ("NEW".equals(statusDTO.getPayrollStatus())) {
            response.setItemSettingChanged(false);
            response.setWarningMessage(null);
            response.setItems(payrollRepository.selectCurrentPayItemSettings());
            return response;
        }

        /*
         * CONFIRMED / PAID 상태
         * - 최신 항목 설정 변경 영향을 받으면 안 된다.
         * - 저장 당시 PAYROLL_ITEM snapshot만 보여준다.
         */
        if ("CONFIRMED".equals(statusDTO.getPayrollStatus())
                || "PAID".equals(statusDTO.getPayrollStatus())) {

            response.setItemSettingChanged(false);
            response.setWarningMessage(null);
            response.setItems(payrollRepository.selectSavedPayrollItems(requestDTO.getEmpNo(), payMonth));
            return response;
        }

        /*
         * DRAFT 상태
         * - 저장된 PAYROLL_ITEM을 우선 보여준다.
         * - PAY_ITEM_SETTING이 수정/삭제되었으면 변경 알림을 내려준다.
         */
        response.setItems(payrollRepository.selectSavedPayrollItems(requestDTO.getEmpNo(), payMonth));

        PayrollBaseSalaryResponseDTO savedPayroll =
                payrollRepository.selectSavedPayrollBaseSalary(requestDTO.getEmpNo(), payMonth)
                        .orElse(null);

        java.time.LocalDateTime latestItemSettingUpdatedAt =
                payrollRepository.selectLatestPayItemSettingUpdatedAt();

        boolean itemSettingChanged = false;

        if (savedPayroll != null
                && savedPayroll.getPayrollUpdatedAt() != null
                && latestItemSettingUpdatedAt != null
                && latestItemSettingUpdatedAt.isAfter(savedPayroll.getPayrollUpdatedAt())) {
            itemSettingChanged = true;
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

            /*
             * 기존 저장 항목 조회
             *
             * 같은 itemSettingId이고,
             * 지급/공제 구분이 그대로이면 기존 입력 금액을 유지한다.
             */
            List<PayrollItemLoadResponseDTO.Item> savedItems =
                    payrollRepository.selectSavedPayrollItems(requestDTO.getEmpNo(), payMonth);

            java.util.Map<Integer, PayrollItemLoadResponseDTO.Item> savedItemMap =
                    new java.util.HashMap<>();

            if (savedItems != null) {
                for (PayrollItemLoadResponseDTO.Item savedItem : savedItems) {

                    if (savedItem.getItemSettingId() != null) {
                        savedItemMap.put(savedItem.getItemSettingId(), savedItem);
                    }
                }
            }

            /*
             * 현재 최신 지급/공제항목 설정 조회
             */
            List<PayrollItemLoadResponseDTO.Item> latestItems =
                    payrollRepository.selectCurrentPayItemSettings();

            /*
             * 기존 snapshot 삭제
             */
            entityManager.createQuery("""
        delete from PayrollItemEntity item
         where item.payroll.payrollId = :payrollId
        """)
                    .setParameter("payrollId", payroll.getPayrollId())
                    .executeUpdate();

            /*
             * 최신 설정 기준 snapshot 재생성
             */
            for (PayrollItemLoadResponseDTO.Item item : latestItems) {

                PayItemSettingEntity itemSetting = null;

                if (item.getItemSettingId() != null) {
                    itemSetting = entityManager.getReference(
                            PayItemSettingEntity.class,
                            item.getItemSettingId()
                    );
                }

                /*
                 * 기본값은 0
                 * - 신규 항목
                 * - 지급 -> 공제 변경
                 * - 공제 -> 지급 변경
                 * 위 경우에는 금액을 유지하지 않는다.
                 */
                BigDecimal saveAmount = BigDecimal.ZERO;

                /*
                 * 기존 항목이 있고 지급/공제 구분이 그대로이면 금액 유지
                 */
                if (item.getItemSettingId() != null) {

                    PayrollItemLoadResponseDTO.Item savedItem =
                            savedItemMap.get(item.getItemSettingId());

                    if (savedItem != null
                            && savedItem.getItemType() != null
                            && savedItem.getItemType().equals(item.getItemType())) {

                        saveAmount = savedItem.getAmount() == null
                                ? BigDecimal.ZERO
                                : savedItem.getAmount();
                    }

                    /*
                     * itemSettingId가 달라졌더라도
                     * 항목명이 같고 지급/공제 구분이 같으면
                     * 기존 금액 유지
                     */
                    if (saveAmount.compareTo(BigDecimal.ZERO) == 0
                            && savedItems != null) {

                        for (PayrollItemLoadResponseDTO.Item oldItem : savedItems) {

                            if (oldItem.getItemNameSnapshot() != null
                                    && item.getItemNameSnapshot() != null
                                    && oldItem.getItemNameSnapshot().trim()
                                    .equals(item.getItemNameSnapshot().trim())
                                    && oldItem.getItemType() != null
                                    && oldItem.getItemType().equals(item.getItemType())) {

                                saveAmount = oldItem.getAmount() == null
                                        ? BigDecimal.ZERO
                                        : oldItem.getAmount();

                                break;
                            }
                        }
                    }
                }

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

            // 신규 항목 등록
            if (itemDTO.getItemSettingId() == null) {

                PayItemSettingEntity itemSetting = PayItemSettingEntity.builder()
                        .itemName(itemDTO.getItemName().trim())
                        .itemType(itemDTO.getItemType())
                        .taxType(itemDTO.getTaxType())
                        .nonTaxCode(itemDTO.getNonTaxCode())

                        /*
                         * 근태연동은 후순위 구현이므로
                         * 현재 지급/공제항목 설정에서는 항상 null로 저장한다.
                         */
                        .linkedAttendanceType(null)

                        .isActive(true)
                        .build();

                entityManager.persist(itemSetting);
                entityManager.flush();

                activeIdSet.add(itemSetting.getItemSettingId());
                continue;
            }

            // 기존 항목 수정
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
                    .setParameter("linkedAttendanceType", null)
                    .setParameter("now", java.time.LocalDateTime.now())
                    .setParameter("itemSettingId", itemDTO.getItemSettingId())
                    .executeUpdate();
        }

        /*
         * 모달에서 삭제된 항목 처리
         *
         * - 사용자가 설정 모달에서 제거한 PAY_ITEM_SETTING은 물리 삭제한다.
         * - 기존 PAYROLL_ITEM이 item_setting_id FK로 참조하고 있을 수 있으므로
         *   먼저 PAYROLL_ITEM.itemSetting 연결을 null로 끊은 뒤 삭제한다.
         */
        if (activeIdSet.isEmpty()) {

            // 모든 기존 항목을 삭제하는 경우: 먼저 참조 해제
            entityManager.createQuery("""
                update PayrollItemEntity item
                   set item.itemSetting = null
                 where item.itemSetting is not null
                """)
                    .executeUpdate();

            // 그 다음 설정 항목 전체 물리 삭제
            entityManager.createQuery("""
                delete from PayItemSettingEntity item
                """)
                    .executeUpdate();

        } else {

            // 삭제 대상 항목을 참조 중인 PAYROLL_ITEM의 FK 연결 해제
            entityManager.createQuery("""
                update PayrollItemEntity item
                   set item.itemSetting = null
                 where item.itemSetting.itemSettingId not in :activeIdSet
                """)
                    .setParameter("activeIdSet", activeIdSet)
                    .executeUpdate();

            // 모달에서 빠진 설정 항목 물리 삭제
            entityManager.createQuery("""
                delete from PayItemSettingEntity item
                 where item.itemSettingId not in :activeIdSet
                """)
                    .setParameter("activeIdSet", activeIdSet)
                    .executeUpdate();
        }

        entityManager.flush();
        entityManager.clear();

        // 저장 후 최신 활성 설정 목록 반환
        return payrollRepository.selectCurrentPayItemSettings();
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

        validatePayrollSaveRequest(requestDTO);

        String payMonth = makePayMonth(requestDTO.getPayYear(), requestDTO.getPayMonth());

        PayrollEntity payroll = payrollRepository
                .findByEmployee_EmpNoAndPayMonth(requestDTO.getEmpNo(), payMonth)
                .orElse(null);

        if (payroll != null && PayrollStatus.PAID.equals(payroll.getStatus())) {
            throw new IllegalStateException("지급완료 상태의 급여대장은 확정할 수 없습니다.");
        }

        PayrollEmployeeInfoResponseDTO employeeInfo = getEmployeeInfo(requestDTO.getEmpNo());
        PayrollPreviewResponseDTO preview = calculatePayrollPreview(requestDTO, employeeInfo, payMonth);

        savePayrollWithCalculation(requestDTO, preview, PayrollStatus.CONFIRMED, null);

        return "급여대장이 확정되었습니다.";
    }

    // 급여대장 지급확정
    @Override
    @Transactional
    public String payConfirmPayroll(PayrollSaveRequestDTO requestDTO) {

        validatePayrollSaveRequest(requestDTO);

        if (requestDTO.getPayDate() == null) {
            throw new IllegalArgumentException("지급일을 선택해 주세요.");
        }

        String payMonth = makePayMonth(requestDTO.getPayYear(), requestDTO.getPayMonth());

        PayrollEntity payroll = payrollRepository
                .findByEmployee_EmpNoAndPayMonth(requestDTO.getEmpNo(), payMonth)
                .orElse(null);

        if (payroll != null && PayrollStatus.PAID.equals(payroll.getStatus())) {
            throw new IllegalStateException("이미 지급완료된 급여대장입니다.");
        }

        PayrollEmployeeInfoResponseDTO employeeInfo = getEmployeeInfo(requestDTO.getEmpNo());
        PayrollPreviewResponseDTO preview = calculatePayrollPreview(requestDTO, employeeInfo, payMonth);

        savePayrollWithCalculation(requestDTO, preview, PayrollStatus.PAID, requestDTO.getPayDate());

        return "급여대장이 지급완료 처리되었습니다.";
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

                    calculatedAmount = inputAmount
                            .multiply(BigDecimal.valueOf(attendanceSummary.getOvertimeMinutes()))
                            .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);

                    formula = attendanceSummary.getOvertimeMinutes()
                            + "분 × "
                            + formatMoney(inputAmount)
                            + " / 60분";
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

                    calculatedAmount = dailyDeduction
                            .multiply(BigDecimal.valueOf(attendanceSummary.getAbsenceDays()));

                    formula = attendanceSummary.getAbsenceDays()
                            + "일 × "
                            + formatMoney(dailyDeduction)
                            + " / 하루";
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
        BigDecimal insuranceBase = baseSalary;


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
        return PayrollPreviewResponseDTO.InsuranceRow.builder()
                .name(name)
                .baseAmount(baseAmount)
                .rate(rate)
                .amount(amount)
                .formula(formatMoney(baseAmount) + " × " + rate)
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

        /*
         * 근태연동은 후순위 구현으로 제외한다.
         * 설정 저장 시 linkedAttendanceType은 항상 null로 정리한다.
         */
        item.setLinkedAttendanceType(null);

        // 비과세 지급항목
        if ("NON_TAXABLE".equals(item.getTaxType())) {

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

        /*
         * 공제항목은 과세/비과세, 비과세 항목, 근태연동을 사용하지 않는다.
         */
        item.setTaxType(null);
        item.setNonTaxCode(null);
        item.setLinkedAttendanceType(null);
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

            if (!"초과수당".equals(item.getItemNameSnapshot().trim())) {
                throw new IllegalArgumentException("근태연동 지급항목명은 '초과수당'이어야 합니다.");
            }

            // 근태연동 항목은 0이어도 입력값이 있어야 한다.
            if (item.getAmount() == null) {
                throw new IllegalArgumentException("근태연동 지급항목 기준값을 입력해 주세요.");
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

            if (!"결근공제".equals(item.getItemNameSnapshot().trim())) {
                throw new IllegalArgumentException("근태연동 공제항목명은 '결근공제'이어야 합니다.");
            }

            // 근태연동 공제는 0이어도 입력값이 있어야 한다.
            if (item.getAmount() == null) {
                throw new IllegalArgumentException("근태연동 공제 기준값을 입력해 주세요.");
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

                    if (row.getItemNameSnapshot() != null
                            && row.getItemNameSnapshot().equals(itemDTO.getItemNameSnapshot())) {
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
}

