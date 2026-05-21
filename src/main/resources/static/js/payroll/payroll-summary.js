/**
 * =========================================================
 * payroll-summary.js
 * 위치:
 * src/main/resources/static/js/payroll/payroll-summary.js
 * =========================================================
 *
 * [급여요약 전체조회 JS]
 *
 * 역할:
 * - 조회년도 / 조회월 select 구성
 * - 현재년도 미래월 선택 방지
 * - 조회년 변경 시 월 유지/보정
 * - 행 클릭 상세조회 진입 준비
 *
 * 중요:
 * - 필터, 정렬, 조회년월 변경만으로는 조회하지 않는다.
 * - 실제 조회는 form submit, 즉 [조회] 버튼으로만 실행한다.
 * - 초기화는 /admin/payroll/summary 로 이동하여 현재월 기본조회로 돌아간다.
 */

document.addEventListener('DOMContentLoaded', function () {

    const searchForm = document.getElementById('payrollSummarySearchForm');

    if (!searchForm) {
        return;
    }

    const payYearSelect = document.getElementById('payYear');
    const payMonthSelect = document.getElementById('payMonth');

    if (!payYearSelect || !payMonthSelect) {
        return;
    }

    /**
     * 백단에서 내려준 기준값
     *
     * currentYear/currentMonth:
     * - 현재년월
     * - 미래월 차단 기준
     *
     * startYear:
     * - 급여요약 조회 시작년도
     */
    const currentYear = Number(searchForm.dataset.currentYear);
    const currentMonth = Number(searchForm.dataset.currentMonth);
    const startYear = Number(searchForm.dataset.startYear);

    /**
     * 현재 조회조건으로 선택된 월
     * - 새로고침/페이징/검색 후에도 GET 파라미터 값이 유지된다.
     */
    const selectedMonthFromServer = Number(payMonthSelect.dataset.selectedMonth);

    /**
     * 최초 진입 시 월 select 구성
     *
     * 예:
     * - 현재 2026년 5월이고 searchDTO가 2026-05이면 1~5월 생성 후 5월 선택
     * - 사용자가 2025-10으로 조회했다면 1~12월 생성 후 10월 선택
     */
    renderMonthOptionsByYear(
        Number(payYearSelect.value),
        selectedMonthFromServer
    );

    /**
     * 조회년도 변경 시 월 select 재구성
     *
     * 규칙:
     * 1. 현재년도면 현재월까지만 표시
     * 2. 과거년도면 1~12월 표시
     * 3. 기존 월이 새 연도에서도 유효하면 유지
     * 4. 기존 월이 미래월이면 마지막 유효월로 보정
     *
     * 예:
     * 2025년 3월 → 2026년 변경 → 2026년 3월 유지
     * 2025년 10월 → 2026년 변경 → 2026년 5월로 보정
     */
    payYearSelect.addEventListener('change', function () {

        const selectedYear = Number(this.value);
        const previousMonth = Number(payMonthSelect.value);

        renderMonthOptionsByYear(selectedYear, previousMonth);
    });

    /**
     * =====================================================
     * 급여요약 row 클릭 → 급여명세서 이동
     * =====================================================
     *
     * 역할:
     * - 급여요약 전체조회 목록에서 row 클릭 시
     *   해당 사원의 급여명세서 화면으로 이동한다.
     *
     * 중요:
     * - 명세서 화면에서 [목록으로] 클릭 시
     *   현재 급여요약 검색조건 / 정렬 / 페이지 상태를 유지해야 한다.
     * - 그래서 return 파라미터를 함께 전달한다.
     */
    document.querySelectorAll('.payroll-summary-row').forEach(function (row) {

        row.addEventListener('click', function () {

            const empNo = this.dataset.empNo;
            const payMonth = this.dataset.payMonth;

            if (!empNo || !payMonth) {
                alert('급여명세서 정보를 찾을 수 없습니다.');
                return;
            }

            /**
             * YYYY-MM → year/month 분리
             *
             * 예:
             * 2026-05
             */
            const splitPayMonth = payMonth.split('-');

            const payYear = splitPayMonth[0];
            const payMonthValue = splitPayMonth[1];

            /**
             * 현재 급여요약 검색조건 유지
             */
            const currentParams =
                new URLSearchParams(window.location.search);

            /**
             * 급여명세서 URL 생성
             */
            const statementUrl =
                '/admin/payroll/summary/statement'
                + '?empNo=' + encodeURIComponent(empNo)
                + '&payYear=' + encodeURIComponent(payYear)
                + '&payMonth=' + encodeURIComponent(payMonthValue)

                // 목록 복귀용 파라미터
                + '&returnPayYear='
                + encodeURIComponent(currentParams.get('payYear') || '')

                + '&returnPayMonth='
                + encodeURIComponent(currentParams.get('payMonth') || '')

                + '&returnKeyword='
                + encodeURIComponent(currentParams.get('keyword') || '')

                + '&returnDeptId='
                + encodeURIComponent(currentParams.get('deptId') || '')

                + '&returnPositionId='
                + encodeURIComponent(currentParams.get('positionId') || '')

                + '&returnStatus='
                + encodeURIComponent(currentParams.get('status') || '')

                + '&returnSortType='
                + encodeURIComponent(currentParams.get('sortType') || '')

                + '&returnPage='
                + encodeURIComponent(currentParams.get('page') || '1');

            /**
             * 급여명세서 화면 이동
             */
            window.location.href = statementUrl;
        });
    });

    /**
     * 조회년도 기준 조회월 option 생성
     */
    function renderMonthOptionsByYear(selectedYear, preferredMonth) {

        payMonthSelect.innerHTML = '';

        if (!selectedYear) {
            return;
        }

        let endMonth = 12;

        /**
         * 현재년도이면 현재월까지만 선택 가능
         * 미래월은 표시하지 않는다.
         */
        if (selectedYear === currentYear) {
            endMonth = currentMonth;
        }

        for (let month = 1; month <= endMonth; month++) {

            const option = document.createElement('option');

            option.value = String(month);
            option.textContent = month + '월';

            payMonthSelect.appendChild(option);
        }

        /**
         * 기존 선택 월이 새 연도에서도 유효하면 유지한다.
         */
        if (preferredMonth >= 1 && preferredMonth <= endMonth) {
            payMonthSelect.value = String(preferredMonth);
            return;
        }

        /**
         * 기존 선택 월이 유효하지 않으면 마지막 유효월로 보정한다.
         *
         * 예:
         * 현재 2026년 5월
         * 2025년 10월 선택 후 2026년으로 변경하면 5월로 보정
         */
        payMonthSelect.value = String(endMonth);
    }
});