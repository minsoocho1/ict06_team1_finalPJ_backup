import React, { useEffect, useState } from 'react';

// DefaultLayout.js의 Outlet에서 보낸 userInfo 데이터 받기
import { useOutletContext } from 'react-router-dom';

// 공통 axios helper
import { request } from 'src/helpers/axios_helper';

// CoreUI 컴포넌트
import {
    CAlert,
    CButton,
    CCard,
    CCardBody,
    CCardHeader,
    CCol,
    CFormInput,
    CFormLabel,
    CFormSelect,
    CRow,
    CSpinner,
    CTable,
    CTableBody,
    CTableDataCell,
    CTableHead,
    CTableHeaderCell,
    CTableRow,
} from '@coreui/react';

// [급여관리] 사용자 급여명세서 조회 화면
const PayrollIssue = () => {

    // 로그인 사용자 정보
    const [userInfo] = useOutletContext();

    // 급여명세서 데이터
    const [statement, setStatement] = useState(null);

    // 조회년월 옵션
    const [periodOptions, setPeriodOptions] = useState(null);

    // 선택한 조회년도 / 조회월
    const [selectedYear, setSelectedYear] = useState('');
    const [selectedMonth, setSelectedMonth] = useState('');

    // 실제 조회 완료된 조회년도 / 조회월
    const [loadedYear, setLoadedYear] = useState('');
    const [loadedMonth, setLoadedMonth] = useState('');

    // 조회월 select option
    const [monthOptions, setMonthOptions] = useState([]);

    // 로딩 상태
    const [loading, setLoading] = useState(false);

    // 에러 메시지
    const [errorMessage, setErrorMessage] = useState('');

    // 계산 과정 펼치기/접기 상태
    const [insuranceOpen, setInsuranceOpen] = useState(false);
    const [taxOpen, setTaxOpen] = useState(false);

    // 화면 최초 진입 시 조회년월 옵션 + 기본 급여명세서 조회
    useEffect(() => {
        fetchInitialData();
    }, []);

    // 조회년도 변경 시 조회 가능한 월 목록 재구성
    useEffect(() => {
        if (!periodOptions || !selectedYear) return;

        const months = makeAvailableMonths(Number(selectedYear));

        setMonthOptions(months);

        if (months.length === 0) {
            setSelectedMonth('');
            return;
        }

        // 기존 월이 새 연도에서 유효하면 유지하고, 아니면 첫 번째 가능 월로 보정
        const currentMonth = Number(selectedMonth);

        if (!currentMonth || !months.includes(currentMonth)) {
            setSelectedMonth(months[0]);
        }
    }, [selectedYear, periodOptions]);

    // 최초 데이터 조회
    const fetchInitialData = async () => {
        setLoading(true);
        setErrorMessage('');

        try {
            // 1. 조회년월 옵션 조회
            const optionRes = await request(
                'GET',
                '/api/payroll/statements/me/period-options',
                {}
            );

            const optionData = optionRes.data;

            setPeriodOptions(optionData);

            const defaultYear = optionData.defaultYear;
            const defaultMonth = optionData.defaultMonth;

            setSelectedYear(defaultYear);
            setSelectedMonth(defaultMonth);
            setLoadedYear(defaultYear);
            setLoadedMonth(defaultMonth);

            // 2. 기본 조회년월 급여명세서 조회
            await fetchPayrollStatement(defaultYear, defaultMonth, false);

        } catch (error) {
            console.error(error);
            setErrorMessage('급여명세서 정보를 불러오는 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // 급여명세서 조회
    const fetchPayrollStatement = async (payYear, payMonth, updateLoaded) => {
        const params = {
            payYear: payYear,
            payMonth: payMonth,
        };

        const res = await request(
            'GET',
            '/api/payroll/statements/me',
            params
        );

        setStatement(res.data);

        if (updateLoaded) {
            setLoadedYear(Number(payYear));
            setLoadedMonth(Number(payMonth));
        }
    };

    // 조회 버튼 클릭
    const handleSearch = async () => {
        if (!selectedYear || !selectedMonth) {
            alert('조회년도와 조회월을 선택해 주세요.');
            return;
        }

        setLoading(true);
        setErrorMessage('');

        try {
            await fetchPayrollStatement(
                selectedYear,
                selectedMonth,
                true
            );
        } catch (error) {
            console.error(error);
            setErrorMessage('급여명세서 조회 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // 선택 연도 기준 조회 가능 월 생성
    const makeAvailableMonths = (year) => {
        if (!periodOptions || !periodOptions.hireDate) {
            return [];
        }

        const hireDate = new Date(periodOptions.hireDate);

        const hireYear = hireDate.getFullYear();
        const hireMonth = hireDate.getMonth() + 1;

        const currentYear = periodOptions.defaultYear;
        const currentMonth = periodOptions.defaultMonth;

        let startMonth = 1;
        let endMonth = 12;

        // 입사년도면 입사월부터
        if (year === hireYear) {
            startMonth = hireMonth;
        }

        // 현재년도면 현재월까지만
        if (year === currentYear) {
            endMonth = currentMonth;
        }

        const result = [];

        for (let month = startMonth; month <= endMonth; month++) {
            result.push(month);
        }

        return result;
    };

    // 조회 버튼 비활성화 여부
    const isSearchDisabled = () => {
        if (!selectedYear || !selectedMonth) {
            return true;
        }

        return Number(selectedYear) === Number(loadedYear)
            && Number(selectedMonth) === Number(loadedMonth);
    };

    // 금액 포맷
    const numberFormat = (value) => {
        if (value === null || value === undefined || value === '') {
            return '0';
        }

        return Number(value).toLocaleString();
    };

    // 지급/공제 표의 행 개수를 맞추기 위한 빈 행 개수 계산
    const getEmptyRowCount = (currentCount, targetCount) => {
        const diff = targetCount - currentCount;

        return diff > 0 ? diff : 0;
    };

    // 지급/공제 표의 빈 행 렌더링
    const renderEmptyRows = (count) => {
        return Array.from({ length: count }).map((_, index) => (
            <CTableRow key={`empty-row-${index}`}>
                <CTableDataCell className="text-center">&nbsp;</CTableDataCell>
                <CTableDataCell className="text-end">&nbsp;</CTableDataCell>
                <CTableDataCell className="text-center">&nbsp;</CTableDataCell>
            </CTableRow>
        ));
    };

    // 부서 표시
    const getDeptText = () => {
        if (!statement) return '';

        if (statement.parentDeptName) {
            return `${statement.deptName} (${statement.parentDeptName})`;
        }

        return statement.deptName || '';
    };

    // 상태 표시
    const getPayrollStatusText = () => {
        if (!statement) return '';

        if (statement.payrollStatus === 'PAID' && statement.payDate) {
            return `${statement.payrollStatusName} (지급일 : ${statement.payDate})`;
        }

        return statement.payrollStatusName || '';
    };

    // 상태 배지 색상
    const getPayrollStatusBadgeColor = () => {
        if (!statement) return 'secondary';

        if (statement.payrollStatus === 'NEW') return 'secondary';
        if (statement.payrollStatus === 'DRAFT') return 'warning';
        if (statement.payrollStatus === 'CONFIRMED') return 'info';
        if (statement.payrollStatus === 'PAID') return 'success';

        return 'secondary';
    };

    // 출력 기능은 2단계에서 관리자 급여명세서 양식과 동일하게 연결
    // 급여명세서 출력
    // - 관리자 급여명세서 출력물과 동일한 형태로 출력한다.
    // - 사용자 화면 UI는 그대로 두고, 아래 숨겨진 출력 전용 DOM만 인쇄한다.
    const handlePrint = () => {
        if (!statement?.pdfAvailable) {
            alert('급여명세서 출력은 확정 또는 지급완료 상태에서만 가능합니다.');
            return;
        }

        const statementDocument = document.getElementById('user-pay-statement-document');

        if (!statementDocument) {
            alert('출력할 급여명세서가 없습니다.');
            return;
        }

        const printWindow = window.open('', '_blank', 'width=900,height=1000');

        printWindow.document.write(`
    <!doctype html>
    <html>
    <head>
      <title>급여명세서</title>
    </head>

    <body style="
      margin:0;
      background:#ffffff;
      font-family:'Malgun Gothic', sans-serif;
    ">
      <div style="
        width:190mm;
        margin:0 auto;
        padding:7mm;
        box-sizing:border-box;
      ">
        ${statementDocument.innerHTML}
      </div>

      <script>
        document.querySelectorAll('table').forEach(function(table) {
          table.style.width = '100%';
          table.style.borderCollapse = 'collapse';
          table.style.fontSize = '10px';
          table.style.marginBottom = '6px';
        });

        document.querySelectorAll('th, td').forEach(function(cell) {
          cell.style.border = '1px solid #dee2e6';
          cell.style.padding = '4px 5px';
        });

        document.querySelectorAll('table tr').forEach(function(row) {
          const cells = row.querySelectorAll('th, td');

          cells.forEach(function(cell, index) {

        // 기본은 가운데 정렬
        cell.style.textAlign = 'center';

        // 3칸 테이블 금액 컬럼
        if (
            cells.length === 3
            && cell.tagName === 'TD'
            && index === 1
        ) {
            cell.style.textAlign = 'right';
            cell.style.paddingRight = '10px';
        }

        // 5칸 계산방법 테이블 금액 컬럼
        if (
            cells.length === 5
            && cell.tagName === 'TD'
            && index === 3
        ) {
            cell.style.textAlign = 'right';
            cell.style.paddingRight = '10px';
        }

        /**
         * 실수령액 영역 강제 오른쪽 정렬
         * - 2칸 table
         * - 두 번째 td
         */
        if (
            cells.length === 2
            && cell.tagName === 'TD'
            && index === 1
        ) {
            cell.style.textAlign = 'right';
            cell.style.paddingRight = '18px';
            cell.style.fontWeight = '700';
        }
        });
        });

        window.onload = function () {
          window.focus();
          window.print();
        };

        window.onafterprint = function () {
          window.close();
        };
      </script>
    </body>
    </html>
  `);

        printWindow.document.close();
    };

    /**
    * 지급/공제 테이블 행 수 자동 계산
    *
    * 규칙:
    * - 지급: 기본급 + 추가 지급항목
    * - 공제: 공제항목 + 4대보험 + 세금
    * - statement 없을 때는 0 처리
    */
    const allowanceRows = statement
        ? [
            {
                title: '기본급',
            },

            ...(statement.allowanceItems || [])
        ]
        : [];

    const deductionRows = statement
        ? [
            ...(statement.deductionItems || []),

            { title: '국민연금' },
            { title: '건강보험' },
            { title: '장기요양보험' },
            { title: '고용보험' },
            { title: '소득세' },
            { title: '지방소득세' },
        ]
        : [];

    const allowanceRowCount =
        allowanceRows.length;

    const deductionRowCount =
        deductionRows.length;

    // 지급/공제 중 더 많은 행 수를 기준으로 맞춘다.
    const maxPayItemRowCount = Math.max(
        allowanceRowCount,
        deductionRowCount
    );

    // 지급 내역에 추가해야 할 빈 행 수
    const allowanceEmptyRowCount = getEmptyRowCount(
        allowanceRowCount,
        maxPayItemRowCount
    );

    // 공제 내역에 추가해야 할 빈 행 수
    const deductionEmptyRowCount = getEmptyRowCount(
        deductionRowCount,
        maxPayItemRowCount
    );

    return (
        <div style={{ padding: '24px' }}>

            {/* 로딩 화면 */}
            {loading && !statement && (
                <CCard className="mb-4">
                    <CCardBody className="text-center py-5">
                        <CSpinner color="primary" />
                        <h5 className="mt-3">급여명세서를 불러오는 중입니다...</h5>
                        <div style={{ fontSize: '14px', color: '#6c757d' }}>
                            잠시만 기다려주세요.
                        </div>
                    </CCardBody>
                </CCard>
            )}

            {/* 에러 메시지 */}
            {errorMessage && (
                <CAlert color="danger">
                    {errorMessage}
                </CAlert>
            )}

            {/* 실제 화면 */}
            {!loading && (
                <>
                    {/* 상단 제목 */}
                    <div className="d-flex justify-content-between align-items-center mb-4">
                        <div>
                            <h2 className="mb-1">
                                {userInfo?.name || '사용자'}님의 급여명세서 조회
                            </h2>

                            <div style={{ fontSize: '14px', color: '#6c757d' }}>
                                월별 급여명세서와 4대보험 및 원천징수세 계산 내역을 확인합니다.
                            </div>
                        </div>

                        <CButton
                            color="primary"
                            variant="outline"
                            disabled={!statement?.pdfAvailable}
                            onClick={handlePrint}
                        >
                            급여명세서 출력
                        </CButton>
                    </div>

                    {/* 인사정보 */}
                    <CCard className="mb-4">
                        <CCardHeader>
                            <strong>인사정보</strong>
                        </CCardHeader>

                        <CCardBody>
                            <CRow className="g-3">

                                {/* 1행: 사번 / 성명 / 부서 / 직급 */}
                                <CCol md={3}>
                                    <CFormLabel>사번</CFormLabel>
                                    <CFormInput
                                        value={statement?.empNo || ''}
                                        readOnly
                                    />
                                </CCol>

                                <CCol md={3}>
                                    <CFormLabel>성명</CFormLabel>
                                    <CFormInput
                                        value={statement?.empName || ''}
                                        readOnly
                                    />
                                </CCol>

                                <CCol md={3}>
                                    <CFormLabel>부서</CFormLabel>
                                    <CFormInput
                                        value={getDeptText()}
                                        readOnly
                                    />
                                </CCol>

                                <CCol md={3}>
                                    <CFormLabel>직급</CFormLabel>
                                    <CFormInput
                                        value={statement?.positionName || ''}
                                        readOnly
                                    />
                                </CCol>

                                {/* 2행: 재직상태 / 급여등급 */}
                                <CCol md={3}>
                                    <CFormLabel>재직상태</CFormLabel>
                                    <CFormInput
                                        value={statement?.empStatusName || ''}
                                        readOnly
                                    />
                                </CCol>

                                <CCol md={3}>
                                    <CFormLabel>급여등급</CFormLabel>
                                    <CFormInput
                                        value={statement?.gradeId || ''}
                                        readOnly
                                    />

                                    {statement?.gradeDescription && (
                                        <div
                                            className="mt-1"
                                            style={{
                                                fontSize: '13px',
                                                color: '#0d6efd',
                                                fontWeight: 600,
                                            }}
                                        >
                                            {statement.gradeDescription}
                                        </div>
                                    )}
                                </CCol>
                            </CRow>
                        </CCardBody>
                    </CCard>

                    {/* 조회 영역 */}
                    <CCard className="mb-4">

                        <CCardBody
                            style={{
                                border: '1px solid #d8dbe0',
                                borderRadius: '6px',
                                padding: '20px',
                            }}
                        >

                            <CRow className="g-3 align-items-end">

                                {/* 작성년도 */}
                                <CCol md={2}>
                                    <CFormLabel
                                        style={{
                                            fontWeight: 600,
                                            marginBottom: '8px',
                                        }}
                                    >
                                        작성년도
                                    </CFormLabel>

                                    <CFormSelect
                                        value={selectedYear}
                                        onChange={(e) =>
                                            setSelectedYear(
                                                Number(e.target.value)
                                            )
                                        }
                                    >
                                        {(periodOptions?.availableYears || []).map((year) => (
                                            <option
                                                key={year}
                                                value={year}
                                            >
                                                {year}년
                                            </option>
                                        ))}
                                    </CFormSelect>
                                </CCol>

                                {/* 작성월 */}
                                <CCol md={2}>
                                    <CFormLabel
                                        style={{
                                            fontWeight: 600,
                                            marginBottom: '8px',
                                        }}
                                    >
                                        작성월
                                    </CFormLabel>

                                    <CFormSelect
                                        value={selectedMonth}
                                        onChange={(e) =>
                                            setSelectedMonth(
                                                Number(e.target.value)
                                            )
                                        }
                                    >
                                        {monthOptions.map((month) => (
                                            <option
                                                key={month}
                                                value={month}
                                            >
                                                {month}월
                                            </option>
                                        ))}
                                    </CFormSelect>
                                </CCol>

                                {/* 조회 버튼 */}
                                <CCol md={2}>
                                    <CButton
                                        color="primary"
                                        variant="outline"
                                        className="w-100"
                                        style={{
                                            height: '38px',
                                            marginTop: '2px',
                                        }}
                                        disabled={
                                            isSearchDisabled()
                                            || loading
                                        }
                                        onClick={handleSearch}
                                    >
                                        조회
                                    </CButton>
                                </CCol>

                                {/* 급여명세서 상태 */}
                                <CCol md={4}>
                                    <CFormLabel
                                        style={{
                                            fontWeight: 600,
                                            marginBottom: '8px',
                                        }}
                                    >
                                        급여명세서 상태
                                    </CFormLabel>

                                    <CFormInput
                                        value={getPayrollStatusText()}
                                        readOnly
                                        style={{
                                            backgroundColor: '#f8f9fa',
                                            fontWeight: 500,
                                        }}
                                    />
                                </CCol>

                            </CRow>

                        </CCardBody>
                    </CCard>

                    {/* 미작성 / 작성중 안내 */}
                    {statement && !statement.statementAvailable && (
                        <CCard className="mb-4">
                            <CCardBody className="text-center py-5">
                                <h5 className="fw-bold mb-2">
                                    {statement.unavailableMessage}
                                </h5>

                                <div style={{ fontSize: '14px', color: '#6c757d' }}>
                                    확정 또는 지급완료 상태의 급여명세서만 조회할 수 있습니다.
                                </div>
                            </CCardBody>
                        </CCard>
                    )}

                    {/* 확정 / 지급완료 상태일 때만 상세 표시 */}
                    {statement?.statementAvailable && (
                        <>
                            {/* 요약 카드 */}
                            <CRow className="mb-4">

                                <CCol md={3}>
                                    <CCard className="h-100">
                                        <CCardBody className="text-center">
                                            <div style={{ fontSize: '14px', color: '#343a40', fontWeight: 700 }}>
                                                총 지급액
                                            </div>

                                            <h4 className="mt-2 mb-0">
                                                {numberFormat(statement.totalGross)}원
                                            </h4>
                                        </CCardBody>
                                    </CCard>
                                </CCol>

                                <CCol md={3}>
                                    <CCard className="h-100">
                                        <CCardBody className="text-center">
                                            <div style={{ fontSize: '14px', color: '#343a40', fontWeight: 700 }}>
                                                총 공제액
                                            </div>

                                            <h4 className="mt-2 mb-0">
                                                {numberFormat(statement.totalDeduction)}원
                                            </h4>
                                        </CCardBody>
                                    </CCard>
                                </CCol>

                                <CCol md={3}>
                                    <CCard className="h-100">
                                        <CCardBody className="text-center">
                                            <div style={{ fontSize: '14px', color: '#343a40', fontWeight: 700 }}>
                                                원천징수세
                                            </div>

                                            <h4 className="mt-2 mb-0">
                                                {numberFormat(
                                                    Number(statement.incomeTax || 0)
                                                    + Number(statement.localIncomeTax || 0)
                                                )}원
                                            </h4>
                                        </CCardBody>
                                    </CCard>
                                </CCol>

                                <CCol md={3}>
                                    <CCard className="h-100">
                                        <CCardBody className="text-center">
                                            <div style={{ fontSize: '14px', color: '#343a40', fontWeight: 700 }}>
                                                실수령액
                                            </div>

                                            <h4 className="mt-2 mb-0">
                                                {numberFormat(statement.netSalary)}원
                                            </h4>
                                        </CCardBody>
                                    </CCard>
                                </CCol>

                            </CRow>

                            {/* 지급 / 공제 내역 */}
                            <CRow className="mb-4">
                                <CCol md={6}>
                                    <CCard className="h-100">
                                        <CCardHeader>
                                            <strong>지급 내역</strong>
                                        </CCardHeader>

                                        <CCardBody>
                                            <CTable bordered hover responsive align="middle">
                                                <CTableHead>
                                                    <CTableRow>
                                                        <CTableHeaderCell className="text-center">
                                                            항목
                                                        </CTableHeaderCell>

                                                        <CTableHeaderCell className="text-center">
                                                            금액(원)
                                                        </CTableHeaderCell>

                                                        <CTableHeaderCell className="text-center">
                                                            구분
                                                        </CTableHeaderCell>
                                                    </CTableRow>
                                                </CTableHead>

                                                <CTableBody>
                                                    <CTableRow>
                                                        <CTableDataCell className="text-center">
                                                            기본급
                                                        </CTableDataCell>
                                                        <CTableDataCell className="text-end">
                                                            {numberFormat(statement.baseSalary)}원
                                                        </CTableDataCell>
                                                        <CTableDataCell className="text-center">-</CTableDataCell>
                                                    </CTableRow>

                                                    {(statement.allowanceItems || []).map((item, index) => (
                                                        <CTableRow key={index}>
                                                            <CTableDataCell className="text-center">{item.itemName}</CTableDataCell>
                                                            <CTableDataCell className="text-end">
                                                                {numberFormat(item.amount)}원
                                                            </CTableDataCell>
                                                            <CTableDataCell className="text-center">
                                                                {item.taxType === 'NON_TAXABLE' ? '비과세' : '과세'}
                                                            </CTableDataCell>
                                                        </CTableRow>
                                                    ))}
                                                    {renderEmptyRows(allowanceEmptyRowCount)}
                                                </CTableBody>
                                            </CTable>
                                        </CCardBody>
                                    </CCard>
                                </CCol>

                                <CCol md={6}>
                                    <CCard className="h-100">
                                        <CCardHeader>
                                            <strong>공제 내역</strong>
                                        </CCardHeader>

                                        <CCardBody>
                                            <CTable bordered hover responsive align="middle">
                                                <CTableHead>
                                                    <CTableRow>
                                                        <CTableHeaderCell className="text-center">
                                                            항목
                                                        </CTableHeaderCell>

                                                        <CTableHeaderCell className="text-center">
                                                            금액(원)
                                                        </CTableHeaderCell>

                                                        <CTableHeaderCell className="text-center">
                                                            비고
                                                        </CTableHeaderCell>
                                                    </CTableRow>
                                                </CTableHead>

                                                <CTableBody>
                                                    {(statement.deductionItems || []).map((item, index) => (
                                                        <CTableRow key={index}>
                                                            <CTableDataCell className="text-center">{item.itemName}</CTableDataCell>
                                                            <CTableDataCell className="text-end">
                                                                {numberFormat(item.amount)}원
                                                            </CTableDataCell>
                                                            <CTableDataCell className="text-center">-</CTableDataCell>
                                                        </CTableRow>
                                                    ))}

                                                    <DeductionRow title="국민연금" amount={statement.nationalPensionAmount} note="4.5%" />
                                                    <DeductionRow title="건강보험" amount={statement.healthInsuranceAmount} note="3.545%" />
                                                    <DeductionRow title="장기요양보험" amount={statement.longTermCareAmount} note="12.81%" />
                                                    <DeductionRow title="고용보험" amount={statement.employmentInsuranceAmount} note="0.9%" />
                                                    <DeductionRow title="소득세" amount={statement.incomeTax} note="-" />
                                                    <DeductionRow title="지방소득세" amount={statement.localIncomeTax} note="-" />
                                                    {renderEmptyRows(deductionEmptyRowCount)}
                                                </CTableBody>
                                            </CTable>
                                        </CCardBody>
                                    </CCard>
                                </CCol>
                            </CRow>

                            {/* 4대보험 계산 과정 */}
                            <CCard className="mb-3">

                                <CCardHeader
                                    className="d-flex justify-content-between align-items-center"
                                    style={{
                                        cursor: 'pointer',
                                        minHeight: '32px',
                                        paddingTop: '6px',
                                        paddingBottom: '6px',
                                    }}
                                    onClick={() =>
                                        setInsuranceOpen(!insuranceOpen)
                                    }
                                >
                                    <strong>
                                        4대보험 계산 과정
                                    </strong>

                                    <span>
                                        {insuranceOpen ? '▲' : '▼'}
                                    </span>
                                </CCardHeader>

                                {insuranceOpen && (
                                    <CCardBody>
                                        <CTable
                                            bordered
                                            responsive
                                            align="middle"
                                        >
                                            <CTableHead>
                                                <CTableRow>

                                                    <CTableHeaderCell className="text-center">
                                                        구분
                                                    </CTableHeaderCell>

                                                    <CTableHeaderCell className="text-center">
                                                        계산 설명
                                                    </CTableHeaderCell>

                                                    <CTableHeaderCell className="text-center">
                                                        계산식
                                                    </CTableHeaderCell>

                                                    <CTableHeaderCell className="text-center">
                                                        금액(원)
                                                    </CTableHeaderCell>

                                                </CTableRow>
                                            </CTableHead>

                                            <CTableBody>

                                                <CalcRow
                                                    title="국민연금"
                                                    description="기준금액 × 보험요율"
                                                    formula={`${numberFormat(statement.baseSalary)} × 4.5%`}
                                                    amount={statement.nationalPensionAmount}
                                                />

                                                <CalcRow
                                                    title="건강보험"
                                                    description="기준금액 × 보험요율"
                                                    formula={`${numberFormat(statement.baseSalary)} × 3.545%`}
                                                    amount={statement.healthInsuranceAmount}
                                                />

                                                <CalcRow
                                                    title="장기요양보험"
                                                    description="건강보험료 × 장기요양보험율"
                                                    formula={`${numberFormat(statement.healthInsuranceAmount)} × 12.81%`}
                                                    amount={statement.longTermCareAmount}
                                                />

                                                <CalcRow
                                                    title="고용보험"
                                                    description="기준금액 × 보험요율"
                                                    formula={`${numberFormat(statement.baseSalary)} × 0.9%`}
                                                    amount={statement.employmentInsuranceAmount}
                                                />

                                            </CTableBody>
                                        </CTable>
                                    </CCardBody>
                                )}

                            </CCard>

                            {/* 원천징수세 계산 과정 */}
                            <CCard className="mb-4">
                                <CCardHeader
                                    className="d-flex justify-content-between align-items-center"
                                    style={{
                                        cursor: 'pointer',
                                        minHeight: '32px',
                                        paddingTop: '6px',
                                        paddingBottom: '6px',
                                    }}
                                    onClick={() =>
                                        setTaxOpen(!taxOpen)
                                    }
                                >
                                    <strong>
                                        원천징수세 계산 과정
                                    </strong>

                                    <span>
                                        {taxOpen ? '▲' : '▼'}
                                    </span>
                                </CCardHeader>

                                {taxOpen && (
                                    <CCardBody>
                                        <CTable bordered responsive align="middle">
                                            <CTableHead>
                                                <CTableRow>
                                                    <CTableHeaderCell className="text-center">
                                                        구분
                                                    </CTableHeaderCell>

                                                    <CTableHeaderCell className="text-center">
                                                        기준금액
                                                    </CTableHeaderCell>

                                                    <CTableHeaderCell className="text-center">
                                                        계산식
                                                    </CTableHeaderCell>

                                                    <CTableHeaderCell className="text-center">
                                                        금액(원)
                                                    </CTableHeaderCell>
                                                </CTableRow>
                                            </CTableHead>

                                            <CTableBody>
                                                <TaxCalcRow
                                                    title="총 지급액"
                                                    baseAmount={statement.totalGross}
                                                    formula="-"
                                                    amount={statement.totalGross}
                                                />

                                                <TaxCalcRow
                                                    title="비과세 제외금액"
                                                    baseAmount={
                                                        Number(statement.totalGross || 0)
                                                        - Number(statement.taxableIncome || 0)
                                                    }
                                                    formula="총지급액 - 과세대상금액"
                                                    amount={
                                                        Number(statement.totalGross || 0)
                                                        - Number(statement.taxableIncome || 0)
                                                    }
                                                />

                                                <TaxCalcRow
                                                    title="원천징수 기준금액"
                                                    baseAmount={statement.taxableIncome}
                                                    formula="과세대상금액 기준"
                                                    amount={statement.taxableIncome}
                                                />

                                                <TaxCalcRow
                                                    title="소득세"
                                                    baseAmount={statement.taxableIncome}
                                                    formula={`${numberFormat(statement.taxableIncome)} × 3%`}
                                                    amount={statement.incomeTax}
                                                />

                                                <TaxCalcRow
                                                    title="지방소득세"
                                                    baseAmount={statement.incomeTax}
                                                    formula={`${numberFormat(statement.incomeTax)} × 10%`}
                                                    amount={statement.localIncomeTax}
                                                />

                                                <CTableRow className="fw-bold">
                                                    <CTableDataCell className="text-center text-primary">
                                                        원천징수세 합계
                                                    </CTableDataCell>

                                                    <CTableDataCell className="text-center text-primary">
                                                        -
                                                    </CTableDataCell>

                                                    <CTableDataCell className="text-center text-primary">
                                                        {numberFormat(statement.incomeTax)} + {numberFormat(statement.localIncomeTax)}
                                                    </CTableDataCell>

                                                    <CTableDataCell className="text-end text-primary">
                                                        {numberFormat(
                                                            Number(statement.incomeTax || 0)
                                                            + Number(statement.localIncomeTax || 0)
                                                        )}원
                                                    </CTableDataCell>
                                                </CTableRow>
                                            </CTableBody>
                                        </CTable>

                                        <div
                                            className="mt-2 text-medium-emphasis"
                                            style={{
                                                fontSize: '13px',
                                            }}
                                        >
                                            ※ 현재는 프로젝트용 간이 원천징수 계산 로직 기준으로 계산합니다.
                                        </div>
                                    </CCardBody>
                                )}
                            </CCard>

                            {/* 출력 전용 급여명세서 영역 */}
                            {statement?.statementAvailable && (
                                <div
                                    id="user-pay-statement-document"
                                    style={{
                                        display: 'none',
                                    }}
                                >
                                    <div style={{ textAlign: 'center', marginBottom: '10px' }}>
                                        <h2
                                            style={{
                                                margin: 0,
                                                fontSize: '24px',
                                                letterSpacing: '6px',
                                            }}
                                        >
                                            급 여 명 세 서
                                        </h2>

                                        <div style={{ marginTop: '5px', fontSize: '13px' }}>
                                            {statement.payYear}년 {String(statement.payMonth).padStart(2, '0')}월분
                                        </div>
                                    </div>

                                    <table>
                                        <tbody>
                                            <tr>
                                                <th style={{ width: '12%' }}>사번</th>
                                                <td style={{ width: '38%' }}>{statement.empNo}</td>
                                                <th style={{ width: '12%' }}>성명</th>
                                                <td style={{ width: '38%' }}>{statement.empName}</td>
                                            </tr>

                                            <tr>
                                                <th>부서</th>
                                                <td>{getDeptText()}</td>
                                                <th>직급</th>
                                                <td>{statement.positionName}</td>
                                            </tr>
                                        </tbody>
                                    </table>

                                    <div style={{ display: 'table', width: '100%', tableLayout: 'fixed' }}>
                                        <div style={{ display: 'table-cell', width: '50%', verticalAlign: 'top', paddingRight: '4px' }}>
                                            <table>
                                                <thead>
                                                    <tr>
                                                        <th colSpan="3">지급 내역</th>
                                                    </tr>
                                                    <tr>
                                                        <th>항목</th>
                                                        <th>금액(원)</th>
                                                        <th>비고</th>
                                                    </tr>
                                                </thead>

                                                <tbody>
                                                    <tr>
                                                        <td>기본급</td>
                                                        <td>{numberFormat(statement.baseSalary)}</td>
                                                        <td>-</td>
                                                    </tr>

                                                    {(statement.allowanceItems || []).map((item, index) => (
                                                        <tr key={`print-allowance-${index}`}>
                                                            <td>{item.itemName}</td>
                                                            <td>{numberFormat(item.amount)}</td>
                                                            <td>{item.taxType === 'NON_TAXABLE' ? '비과세' : '과세'}</td>
                                                        </tr>
                                                    ))}

                                                    {Array.from({ length: allowanceEmptyRowCount }).map((_, index) => (
                                                        <tr key={`print-empty-allowance-${index}`}>
                                                            <td>&nbsp;</td>
                                                            <td></td>
                                                            <td></td>
                                                        </tr>
                                                    ))}

                                                    <tr>
                                                        <td><strong>총 지급액</strong></td>
                                                        <td><strong>{numberFormat(statement.totalGross)}</strong></td>
                                                        <td></td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>

                                        <div style={{ display: 'table-cell', width: '50%', verticalAlign: 'top', paddingLeft: '4px' }}>
                                            <table>
                                                <thead>
                                                    <tr>
                                                        <th colSpan="3">공제 내역</th>
                                                    </tr>
                                                    <tr>
                                                        <th>항목</th>
                                                        <th>금액(원)</th>
                                                        <th>비고</th>
                                                    </tr>
                                                </thead>

                                                <tbody>
                                                    {(statement.deductionItems || []).map((item, index) => (
                                                        <tr key={`print-deduction-${index}`}>
                                                            <td>{item.itemName}</td>
                                                            <td>{numberFormat(item.amount)}</td>
                                                            <td>-</td>
                                                        </tr>
                                                    ))}

                                                    <tr>
                                                        <td>국민연금</td>
                                                        <td>{numberFormat(statement.nationalPensionAmount)}</td>
                                                        <td>4.5%</td>
                                                    </tr>
                                                    <tr>
                                                        <td>건강보험</td>
                                                        <td>{numberFormat(statement.healthInsuranceAmount)}</td>
                                                        <td>3.545%</td>
                                                    </tr>
                                                    <tr>
                                                        <td>장기요양보험</td>
                                                        <td>{numberFormat(statement.longTermCareAmount)}</td>
                                                        <td>12.81%</td>
                                                    </tr>
                                                    <tr>
                                                        <td>고용보험</td>
                                                        <td>{numberFormat(statement.employmentInsuranceAmount)}</td>
                                                        <td>0.9%</td>
                                                    </tr>
                                                    <tr>
                                                        <td>소득세</td>
                                                        <td>{numberFormat(statement.incomeTax)}</td>
                                                        <td>-</td>
                                                    </tr>
                                                    <tr>
                                                        <td>지방소득세</td>
                                                        <td>{numberFormat(statement.localIncomeTax)}</td>
                                                        <td>-</td>
                                                    </tr>

                                                    {Array.from({ length: deductionEmptyRowCount }).map((_, index) => (
                                                        <tr key={`print-empty-deduction-${index}`}>
                                                            <td>&nbsp;</td>
                                                            <td></td>
                                                            <td></td>
                                                        </tr>
                                                    ))}

                                                    <tr>
                                                        <td><strong>총 공제액</strong></td>
                                                        <td><strong>{numberFormat(statement.totalDeduction)}</strong></td>
                                                        <td></td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>

                                    <table>
                                        <tbody>
                                            <tr>
                                                <th style={{ width: '30%', fontSize: '18px', letterSpacing: '4px' }}>
                                                    실 수 령 액
                                                </th>
                                                <td style={{ fontSize: '22px', fontWeight: 700, textAlign: 'right' }}>
                                                    {numberFormat(statement.netSalary)} 원
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>

                                    <table>
                                        <thead>
                                            <tr>
                                                <th colSpan="5">계산방법</th>
                                            </tr>
                                            <tr>
                                                <th>구분</th>
                                                <th>계산 설명</th>
                                                <th>계산식</th>
                                                <th>금액(원)</th>
                                                <th>비고</th>
                                            </tr>
                                        </thead>

                                        <tbody>
                                            <tr>
                                                <td>국민연금</td>
                                                <td>기준금액 × 보험요율</td>
                                                <td>{numberFormat(statement.baseSalary)} × 4.5%</td>
                                                <td>{numberFormat(statement.nationalPensionAmount)}</td>
                                                <td>4.5%</td>
                                            </tr>
                                            <tr>
                                                <td>건강보험</td>
                                                <td>기준금액 × 보험요율</td>
                                                <td>{numberFormat(statement.baseSalary)} × 3.545%</td>
                                                <td>{numberFormat(statement.healthInsuranceAmount)}</td>
                                                <td>3.545%</td>
                                            </tr>
                                            <tr>
                                                <td>장기요양보험</td>
                                                <td>건강보험료 × 장기요양보험율</td>
                                                <td>{numberFormat(statement.healthInsuranceAmount)} × 12.81%</td>
                                                <td>{numberFormat(statement.longTermCareAmount)}</td>
                                                <td>12.81%</td>
                                            </tr>
                                            <tr>
                                                <td>고용보험</td>
                                                <td>기준금액 × 보험요율</td>
                                                <td>{numberFormat(statement.baseSalary)} × 0.9%</td>
                                                <td>{numberFormat(statement.employmentInsuranceAmount)}</td>
                                                <td>0.9%</td>
                                            </tr>
                                        </tbody>
                                    </table>

                                    <p style={{ fontSize: '10px', whiteSpace: 'nowrap', marginTop: '6px' }}>
                                        ※ 상기 금액은 회사 내부 기준에 따라 계산되었으며, 실제 지급액과 차이가 있을 수 있습니다.
                                    </p>
                                </div>
                            )}
                        </>
                    )}
                </>
            )}
        </div>
    );

    // 공제 행
    function DeductionRow({ title, amount, note }) {
        return (
            <CTableRow>
                <CTableDataCell className="text-center">{title}</CTableDataCell>
                <CTableDataCell className="text-end">
                    {numberFormat(amount)}원
                </CTableDataCell>
                <CTableDataCell className="text-center">{note}</CTableDataCell>
            </CTableRow>
        );
    }

    // 계산 과정 행
    function CalcRow({ title, description, formula, amount }) {
        return (
            <CTableRow>
                <CTableDataCell className="text-center fw-semibold">
                    {title}
                </CTableDataCell>

                <CTableDataCell className="text-center">
                    {description}
                </CTableDataCell>

                <CTableDataCell className="text-center">
                    {formula}
                </CTableDataCell>

                <CTableDataCell className="text-end">
                    {numberFormat(amount)}원
                </CTableDataCell>
            </CTableRow>
        );
    }

    // 원천징수세 계산 과정 행
    function TaxCalcRow({
        title,
        baseAmount,
        formula,
        amount,
    }) {
        return (
            <CTableRow>
                <CTableDataCell className="text-center fw-semibold">
                    {title}
                </CTableDataCell>

                <CTableDataCell className="text-center">
                    {numberFormat(baseAmount)}
                </CTableDataCell>

                <CTableDataCell className="text-center">
                    {formula}
                </CTableDataCell>

                <CTableDataCell className="text-end">
                    {numberFormat(amount)}원
                </CTableDataCell>
            </CTableRow>
        );
    }
};

export default PayrollIssue;