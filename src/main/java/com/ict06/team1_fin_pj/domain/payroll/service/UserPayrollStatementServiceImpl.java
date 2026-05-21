package com.ict06.team1_fin_pj.domain.payroll.service;

import com.ict06.team1_fin_pj.common.dto.payroll.PayrollPeriodOptionDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollStatementResponseDTO;
import com.ict06.team1_fin_pj.domain.payroll.repository.PayrollSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

// 사용자 급여명세서 서비스 구현
@Service
@RequiredArgsConstructor
public class UserPayrollStatementServiceImpl implements UserPayrollStatementService  {

    private final PayrollSummaryRepository payrollSummaryRepository;

    // 내 급여명세서 조회
    @Override
    @Transactional(readOnly = true)
    public PayrollStatementResponseDTO getMyPayrollStatement(
            String empNo,
            Integer payYear,
            Integer payMonth
    ) {

        validateStatementRequest(empNo, payYear, payMonth);

        String payMonthText = makePayMonth(payYear, payMonth);

        PayrollStatementResponseDTO statement =
                payrollSummaryRepository.selectPayrollStatement(empNo, payMonthText)
                        .orElseThrow(() ->
                                new IllegalArgumentException("사원 정보를 찾을 수 없습니다.")
                        );

        statement.setPayYear(payYear);
        statement.setPayMonth(payMonth);
        statement.setPayMonthText(payMonthText);

        setEmployeeStatusName(statement);
        setPayrollStatusInfo(statement);

        // 급여대장이 있는 경우에만 지급/공제 snapshot을 조회한다.
        if (statement.getPayrollId() != null) {
            List<PayrollStatementResponseDTO.Item> items =
                    payrollSummaryRepository.selectPayrollStatementItems(
                            statement.getPayrollId()
                    );

            splitStatementItems(statement, items);
        }

        setUserStatementAvailableInfo(statement);

        // 출력 양식에서 지급/공제 표 높이를 맞추기 위한 행 수 보정
        if (statement.isStatementAvailable()) {
            setStatementRowBalance(statement);
        }

        return statement;
    }

    // 내 급여명세서 조회년월 옵션
    @Override
    @Transactional(readOnly = true)
    public PayrollPeriodOptionDTO getMyStatementPeriodOptions(String empNo) {

        if (!StringUtils.hasText(empNo)) {
            throw new IllegalArgumentException("로그인 정보가 없습니다.");
        }

        YearMonth now = YearMonth.now();

        PayrollStatementResponseDTO employeeInfo =
                payrollSummaryRepository
                        .selectPayrollStatement(
                                empNo,
                                makePayMonth(now.getYear(), now.getMonthValue())
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException("사원 정보를 찾을 수 없습니다.")
                        );

        LocalDate hireDate = employeeInfo.getHireDate();

        if (hireDate == null) {
            throw new IllegalArgumentException("입사일 정보가 없습니다.");
        }

        int currentYear = now.getYear();
        int hireYear = hireDate.getYear();

        /**
         * 조회 가능 연도
         * - 기본은 입사년도부터 현재년도까지
         * - 너무 오래된 사원은 최근 5년만 노출
         */
        int startYear = Math.max(hireYear, currentYear - 4);

        List<Integer> availableYears = new ArrayList<>();

        for (int year = startYear; year <= currentYear; year++) {
            availableYears.add(year);
        }

        PayrollPeriodOptionDTO result = new PayrollPeriodOptionDTO();

        result.setHireDate(hireDate);
        result.setDefaultYear(currentYear);
        result.setDefaultMonth(now.getMonthValue());
        result.setAvailableYears(availableYears);

        return result;
    }

    // PAYROLL.pay_month 형식 생성
    private String makePayMonth(Integer payYear, Integer payMonth) {
        return payYear + "-" + String.format("%02d", payMonth);
    }

    // 급여명세서 요청 검증
    private void validateStatementRequest(
            String empNo,
            Integer payYear,
            Integer payMonth
    ) {

        if (!StringUtils.hasText(empNo)) {
            throw new IllegalArgumentException("로그인 정보가 없습니다.");
        }

        if (payYear == null) {
            throw new IllegalArgumentException("조회년도를 선택해 주세요.");
        }

        if (payMonth == null) {
            throw new IllegalArgumentException("조회월을 선택해 주세요.");
        }

        if (payMonth < 1 || payMonth > 12) {
            throw new IllegalArgumentException("조회월 정보가 올바르지 않습니다.");
        }

        YearMonth now = YearMonth.now();
        YearMonth requestedMonth = YearMonth.of(payYear, payMonth);

        if (requestedMonth.isAfter(now)) {
            throw new IllegalArgumentException("미래월 급여명세서는 조회할 수 없습니다.");
        }

        PayrollStatementResponseDTO employeeInfo =
                payrollSummaryRepository
                        .selectPayrollStatement(
                                empNo,
                                makePayMonth(now.getYear(), now.getMonthValue())
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException("사원 정보를 찾을 수 없습니다.")
                        );

        LocalDate hireDate = employeeInfo.getHireDate();

        if (hireDate == null) {
            throw new IllegalArgumentException("입사일 정보가 없습니다.");
        }

        YearMonth hireMonth = YearMonth.from(hireDate);

        if (requestedMonth.isBefore(hireMonth)) {
            throw new IllegalArgumentException("입사 이전 급여명세서는 조회할 수 없습니다.");
        }
    }

    // 사원 재직상태 표시명
    private void setEmployeeStatusName(PayrollStatementResponseDTO statement) {

        String status = statement.getEmpStatus();

        if ("1".equals(status)) {
            statement.setEmpStatusName("재직");
        } else if ("2".equals(status)) {
            statement.setEmpStatusName("휴직");
        } else if ("3".equals(status)) {
            statement.setEmpStatusName("퇴사");
        } else {
            statement.setEmpStatusName(status);
        }
    }

    // 급여대장 상태 표시명
    private void setPayrollStatusInfo(PayrollStatementResponseDTO statement) {

        String payrollStatus = statement.getPayrollStatus();

        if (!StringUtils.hasText(payrollStatus)) {
            statement.setPayrollStatus("NEW");
            statement.setPayrollStatusName("미작성");
            return;
        }

        if ("DRAFT".equals(payrollStatus)) {
            statement.setPayrollStatusName("작성중");
        } else if ("CONFIRMED".equals(payrollStatus)) {
            statement.setPayrollStatusName("확정");
        } else if ("PAID".equals(payrollStatus)) {
            statement.setPayrollStatusName("지급완료");
        } else {
            statement.setPayrollStatusName(payrollStatus);
        }
    }

    // 지급/공제 항목 분리
    private void splitStatementItems(
            PayrollStatementResponseDTO statement,
            List<PayrollStatementResponseDTO.Item> items
    ) {

        if (items == null || items.isEmpty()) {
            return;
        }

        for (PayrollStatementResponseDTO.Item item : items) {

            if ("ALLOWANCE".equals(item.getItemType())) {
                statement.getAllowanceItems().add(item);
            }

            if ("DEDUCTION".equals(item.getItemType())) {
                statement.getDeductionItems().add(item);
            }
        }
    }

    // 사용자 급여명세서 표시 가능 여부
    private void setUserStatementAvailableInfo(PayrollStatementResponseDTO statement) {

        String status = statement.getPayrollStatus();

        /**
         * 사용자 화면 규칙
         * - NEW: 조회 불가
         * - DRAFT: 계산 반영 여부와 관계없이 조회 불가
         * - CONFIRMED / PAID: 조회 및 출력 가능
         */
        if ("NEW".equals(status)) {
            statement.setStatementAvailable(false);
            statement.setPdfAvailable(false);
            statement.setUnavailableMessage(
                    "해당 월의 급여명세서가 아직 작성되지 않았습니다."
            );
            return;
        }

        if ("DRAFT".equals(status)) {
            statement.setStatementAvailable(false);
            statement.setPdfAvailable(false);
            statement.setUnavailableMessage(
                    "해당 월의 급여명세서는 현재 작성 중입니다. 급여 확정 후 조회 및 출력이 가능합니다."
            );
            return;
        }

        if ("CONFIRMED".equals(status) || "PAID".equals(status)) {
            statement.setStatementAvailable(true);
            statement.setPdfAvailable(true);
            statement.setUnavailableMessage(null);
            return;
        }

        statement.setStatementAvailable(false);
        statement.setPdfAvailable(false);
        statement.setUnavailableMessage("급여명세서를 조회할 수 없는 상태입니다.");
    }

    // 출력 양식 지급/공제 행 수 보정
    private void setStatementRowBalance(PayrollStatementResponseDTO statement) {

        int allowanceItemCount = statement.getAllowanceItems() == null
                ? 0
                : statement.getAllowanceItems().size();

        int deductionItemCount = statement.getDeductionItems() == null
                ? 0
                : statement.getDeductionItems().size();

        // 기본급 1행은 지급 내역에 항상 포함된다.
        int allowanceRows = allowanceItemCount + 1;

        // 공제 내역은 사용자 설정 공제 + 4대보험 4행 + 세금 2행
        int deductionRows = deductionItemCount + 6;

        if (allowanceRows > deductionRows) {
            statement.setEmptyDeductionRowCount(allowanceRows - deductionRows);
            statement.setEmptyAllowanceRowCount(0);
            return;
        }

        if (deductionRows > allowanceRows) {
            statement.setEmptyAllowanceRowCount(deductionRows - allowanceRows);
            statement.setEmptyDeductionRowCount(0);
            return;
        }

        statement.setEmptyAllowanceRowCount(0);
        statement.setEmptyDeductionRowCount(0);
    }


}
