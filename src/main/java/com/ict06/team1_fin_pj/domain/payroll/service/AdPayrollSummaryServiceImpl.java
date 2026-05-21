package com.ict06.team1_fin_pj.domain.payroll.service;

import com.ict06.team1_fin_pj.common.dto.payroll.PayrollPeriodOptionDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollStatementResponseDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollSummaryPageResponseDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollSummarySearchDTO;
import com.ict06.team1_fin_pj.domain.payroll.repository.PayrollSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

// 관리자 급여요약 서비스 구현
@Service
@RequiredArgsConstructor
public class AdPayrollSummaryServiceImpl implements AdPayrollSummaryService {

    private final PayrollSummaryRepository payrollSummaryRepository;

    // 급여요약 전체조회
    @Override
    @Transactional(readOnly = true)
    public PayrollSummaryPageResponseDTO getPayrollSummaryList(
            PayrollSummarySearchDTO searchDTO
    ) {

        if (searchDTO == null) {
            searchDTO = new PayrollSummarySearchDTO();
        }

        // 초기 진입 또는 초기화 시 현재년월 기준으로 조회한다.
        YearMonth now = YearMonth.now();

        if (searchDTO.getPayYear() == null) {
            searchDTO.setPayYear(now.getYear());
        }

        if (searchDTO.getPayMonth() == null) {
            searchDTO.setPayMonth(now.getMonthValue());
        }

        // 미래년월은 백단에서도 현재년월로 보정한다.
        YearMonth requestedMonth = YearMonth.of(
                searchDTO.getPayYear(),
                searchDTO.getPayMonth()
        );

        if (requestedMonth.isAfter(now)) {
            searchDTO.setPayYear(now.getYear());
            searchDTO.setPayMonth(now.getMonthValue());
        }

        if (!StringUtils.hasText(searchDTO.getSortType())) {
            searchDTO.setSortType("DEFAULT");
        }

        if (searchDTO.getPage() < 1) {
            searchDTO.setPage(1);
        }

        // 급여요약은 페이지당 10개 고정
        searchDTO.setSize(10);

        String payMonth = makePayMonth(
                searchDTO.getPayYear(),
                searchDTO.getPayMonth()
        );

        return payrollSummaryRepository.selectPayrollSummaryList(
                searchDTO,
                payMonth
        );
    }

    // 급여명세서 조회
    @Override
    @Transactional(readOnly = true)
    public PayrollStatementResponseDTO getPayrollStatement(
            String empNo,
            Integer payYear,
            Integer payMonth
    ) {

        validateStatementRequest(empNo, payYear, payMonth);

        String payMonthText = makePayMonth(payYear, payMonth);

        PayrollStatementResponseDTO statement =
                payrollSummaryRepository.selectPayrollStatement(empNo, payMonthText)
                        .orElseThrow(() -> new IllegalArgumentException("사원 정보를 찾을 수 없습니다."));

        statement.setPayYear(payYear);
        statement.setPayMonth(payMonth);
        statement.setPayMonthText(payMonthText);

        setEmployeeStatusName(statement);
        setPayrollStatusInfo(statement);

        // 급여대장이 있는 경우에만 지급/공제항목 조회
        if (statement.getPayrollId() != null) {

            List<PayrollStatementResponseDTO.Item> items =
                    payrollSummaryRepository.selectPayrollStatementItems(statement.getPayrollId());

            splitStatementItems(statement, items);
        }

        setStatementAvailableInfo(statement);

        // 명세서 표시가 가능한 경우에만 지급/공제 표 행 수를 맞춘다.
        if (statement.isStatementAvailable()) {
            setStatementRowBalance(statement);
        }

        return statement;
    }

    // 급여명세서 작성년월 옵션 조회
    @Override
    @Transactional(readOnly = true)
    public PayrollPeriodOptionDTO getStatementPeriodOptions(String empNo) {

        if (!StringUtils.hasText(empNo)) {
            throw new IllegalArgumentException("사번 정보가 없습니다.");
        }

        YearMonth now = YearMonth.now();

        PayrollStatementResponseDTO employeeInfo =
                payrollSummaryRepository
                        .selectPayrollStatement(empNo, makePayMonth(now.getYear(), now.getMonthValue()))
                        .orElseThrow(() -> new IllegalArgumentException("사원 정보를 찾을 수 없습니다."));

        LocalDate hireDate = employeeInfo.getHireDate();

        if (hireDate == null) {
            throw new IllegalArgumentException("입사일 정보가 없습니다.");
        }

        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        int hireYear = hireDate.getYear();

        // 최근 5년 또는 입사년도 중 더 늦은 연도부터 조회 가능
        int startYear = hireYear;

        List<Integer> availableYears = new ArrayList<>();

        for (int year = startYear; year <= currentYear; year++) {
            availableYears.add(year);
        }

        PayrollPeriodOptionDTO result = new PayrollPeriodOptionDTO();

        result.setHireDate(hireDate);
        result.setDefaultYear(currentYear);
        result.setDefaultMonth(currentMonth);
        result.setAvailableYears(availableYears);

        return result;
    }

    // PAYROLL.payMonth 형식 생성
    private String makePayMonth(Integer payYear, Integer payMonth) {
        return payYear + "-" + String.format("%02d", payMonth);
    }

    // 급여명세서 조회 요청 검증
    private void validateStatementRequest(
            String empNo,
            Integer payYear,
            Integer payMonth
    ) {

        if (!StringUtils.hasText(empNo)) {
            throw new IllegalArgumentException("사번 정보가 없습니다.");
        }

        if (payYear == null) {
            throw new IllegalArgumentException("작성년도를 선택해 주세요.");
        }

        if (payMonth == null) {
            throw new IllegalArgumentException("작성월을 선택해 주세요.");
        }

        if (payMonth < 1 || payMonth > 12) {
            throw new IllegalArgumentException("작성월 정보가 올바르지 않습니다.");
        }

        YearMonth now = YearMonth.now();
        YearMonth requestedMonth = YearMonth.of(payYear, payMonth);

        if (requestedMonth.isAfter(now)) {
            throw new IllegalArgumentException("미래월 급여명세서는 조회할 수 없습니다.");
        }

        /**
         * 입사월 이전 조회 방지
         *
         * 예:
         * 입사일 2026-03
         * → 2026-01 조회 불가
         * → 2026-02 조회 불가
         * → 2026-03부터 조회 가능
         */
        PayrollStatementResponseDTO employeeInfo =
                payrollSummaryRepository
                        .selectPayrollStatement(
                                empNo,
                                makePayMonth(
                                        now.getYear(),
                                        now.getMonthValue()
                                )
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException("사원 정보를 찾을 수 없습니다.")
                        );

        /**
         * 급여대장이 없어도 입사일은 조회되어야 한다.
         *
         * selectPayrollStatement()는 employee 기준 left join 구조라
         * payroll row가 없어도 emp 정보는 조회 가능하다.
         */
        LocalDate hireDate = employeeInfo.getHireDate();

        if (hireDate == null) {
            throw new IllegalArgumentException("입사일 정보가 없습니다.");
        }
        YearMonth hireMonth =
                YearMonth.from(hireDate);

        if (requestedMonth.isBefore(hireMonth)) {

            throw new IllegalArgumentException(
                    "입사 이전 급여명세서는 조회할 수 없습니다."
            );
        }
    }

    // 사원 재직상태 표시명 세팅
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

    // 급여대장 상태 표시명 세팅
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

    // 명세서 표시/PDF 가능 여부 세팅
    private void setStatementAvailableInfo(PayrollStatementResponseDTO statement) {

        String status = statement.getPayrollStatus();

        // 미작성은 급여대장 자체가 없으므로 명세서 표시 불가
        if ("NEW".equals(status)) {
            statement.setStatementAvailable(false);
            statement.setPdfAvailable(false);
            statement.setUnavailableMessage("선택한 년월의 급여대장이 아직 작성되지 않았습니다.");
            return;
        }

        // 작성중이지만 계산 결과가 없으면 명세서 표시 불가
        if ("DRAFT".equals(status) && statement.getNetSalary() == null) {
            statement.setStatementAvailable(false);
            statement.setPdfAvailable(false);
            statement.setUnavailableMessage("작성중 상태이며 계산 미리보기가 아직 반영되지 않았습니다.");
            return;
        }

        // 작성중 계산 반영 상태는 화면 조회만 허용
        if ("DRAFT".equals(status)) {
            statement.setStatementAvailable(true);
            statement.setPdfAvailable(false);
            statement.setUnavailableMessage(null);
            return;
        }

        // 확정/지급완료는 명세서 조회와 PDF 발급 허용
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

    // 지급/공제 내역 줄수 맞춤
    private void setStatementRowBalance(PayrollStatementResponseDTO statement) {

        int allowanceItemCount = statement.getAllowanceItems() == null
                ? 0
                : statement.getAllowanceItems().size();

        int deductionItemCount = statement.getDeductionItems() == null
                ? 0
                : statement.getDeductionItems().size();

        int allowanceRowCount =
                1 + allowanceItemCount + 1;

        int deductionRowCount =
                deductionItemCount + 4 + 2 + 1;

        int maxRowCount = Math.max(allowanceRowCount, deductionRowCount);

        statement.setEmptyAllowanceRowCount(maxRowCount - allowanceRowCount);
        statement.setEmptyDeductionRowCount(maxRowCount - deductionRowCount);
    }
}