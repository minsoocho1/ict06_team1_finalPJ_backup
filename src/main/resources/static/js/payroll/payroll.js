/**
 * =========================================================
 * payroll.js
 * 위치:
 * src/main/resources/static/js/admin/payroll/payroll.js
 * =========================================================
 *
 * [급여대장관리 메인 JS]
 *
 * - 사원 autocomplete
 * - 인사정보 조회
 * - 작성년월 조회
 * - 급여대장 상태 조회
 * - 지급/공제항목 렌더링
 * - 계산 미리보기
 * - 저장 / 확정 / 지급확정
 * - 버튼 상태 제어
 * - sessionStorage 임시복구
 *
 * ---------------------------------------------------------
 * [중요]
 * - 관리자 Thymeleaf 화면 전용
 * - AdminLTE + Bootstrap5 기반
 * - 공통 template 건드리지 않음
 * - CoreUI/sidebar/layout 수정 금지
 * ---------------------------------------------------------
 */

$(document).ready(function () {

    /**
     * =====================================================
     * 전역 상태값
     * =====================================================
     */

    // 현재 선택된 사번
    let currentEmpNo = null;

    // 현재 조회 년월
    let currentPayMonth = null;

    // 계산 미리보기 완료 여부
    let previewCompleted = false;

    // 현재 급여대장 상태
    let currentPayrollStatus = null;

    // 계산결과 캐시
    let previewResult = null;

    // 계산 미리보기 모달에 임시로 담아두는 결과
    let previewModalResult = null;

    // 마지막으로 메인에 실제 반영된 계산결과 snapshot
    let lastAppliedPreviewResultJson = '';

    // 지급/공제항목 설정 모달 원본 상태
    let originalPayItemSettingJson = '';
    let currentPayItemSettingRows = [];

    // 기본급 정책 선택 여부
    let baseSalaryDecisionRequired = false;
    let baseSalaryDecisionCompleted = true;

    // 현재 기본급 정책 조회 결과
    let currentBaseSalaryInfo = null;

    // 지급/공제항목 변경 선택 여부
    let itemSettingDecisionRequired = false;
    let itemSettingDecisionCompleted = true;
    let suppressItemSettingWarningOnce = false;

    // 현재 화면에 표시 중인 지급/공제항목
    let currentPayrollItems = [];

    // 마지막으로 조회/저장된 급여대장 화면 상태 - DRAFT에서 변경 없이 저장하는 것을 막기 위해 사용
    let lastSavedPayrollSnapshotJson = '';
    let skipNextSnapshotUpdate = false;

    /**
     * 작성년월 선택 제어용 상태값
     *
     * - currentHireDate: 선택 사원의 입사일
     * - currentDefaultYear/currentDefaultMonth: 현재 날짜 기준 기본 작성년월
     * - lastLoadedPayYear/lastLoadedPayMonth: 실제로 조회 완료된 작성년월
     *
     * 조회 버튼은 선택값과 마지막 조회 완료값이 다를 때만 활성화한다.
     */
    let currentHireDate = null;
    let currentDefaultYear = null;
    let currentDefaultMonth = null;
    let lastLoadedPayYear = null;
    let lastLoadedPayMonth = null;

    /**
     * =====================================================
     * 초기 진입 상태
     * =====================================================
     */

    // 최초 진입 시 버튼 비활성화
    initializeDisabledState();
    clearPayrollPageStateOnFreshEntry();
    // restorePayrollTempState();

    /**
     * =====================================================
     * 사원 검색 autocomplete
     * =====================================================
     */

    $('#employeeSearchInput').on('input', function () {

        const keyword = $(this).val().trim();

        /**
         * 이름은 2글자 이상
         * 사번은 6자리 이상일 때만 검색
         */
        if (keyword.length < 2) {

            $('#employeeAutocompleteBox')
                .addClass('d-none')
                .empty();

            return;
        }

        let searchType = '';

        if (/^\d+$/.test(keyword)) {
            if (keyword.length < 6) {
                $('#employeeAutocompleteBox').addClass('d-none').empty();
                return;
            }
            searchType = 'EMP_NO';
        } else {
            if (keyword.length < 2) {
                $('#employeeAutocompleteBox').addClass('d-none').empty();
                return;
            }
            searchType = 'NAME';
        }

        /**
         * 사원 autocomplete 조회
         */
        $.ajax({
            url: '/admin/payroll/main/employees/search',
            type: 'GET',
            data: {
                keyword: keyword,
                searchType: searchType,
                limit: 10,
                showAll: true
            },
            success: function (result) {

                renderEmployeeAutocomplete(result);
            },
            error: function () {

                console.error('사원 검색 실패');
            }
        });
    });

    /**
     * =====================================================
     * autocomplete 렌더링
     * =====================================================
     */

    function renderEmployeeAutocomplete(employeeList) {

        const box = $('#employeeAutocompleteBox');

        box.empty();

        if (!employeeList || employeeList.length === 0) {
            box.addClass('d-none');
            return;
        }

        const keyword = $('#employeeSearchInput').val().trim();
        const isEmpNoSearch = /^\d+$/.test(keyword);

        employeeList.forEach(employee => {

            const isAvailable = employee.payrollAvailable !== false;

            const itemClass = isAvailable
                ? ''
                : 'list-group-item-warning text-dark fw-semibold disabled';

            let displayText = '';

            if (isAvailable) {

                const deptText = employee.parentDeptName
                    ? `${employee.deptName}(${employee.parentDeptName})`
                    : employee.deptName;

                if (isEmpNoSearch) {
                    displayText = `${employee.empNo} (${employee.empName} / ${deptText} / ${employee.positionName})`;
                } else {
                    displayText = `${employee.empName} (${employee.empNo} / ${deptText} / ${employee.positionName})`;
                }

            } else {

                 if (isEmpNoSearch) {

                     displayText =
                         `[조회불가] ${employee.empNo}
                         (${employee.empName} / ${employee.deptName} / ${employee.positionName})`;

                 } else {

                     displayText =
                         `[조회불가] ${employee.empName}
                         (${employee.empNo} / ${employee.deptName} / ${employee.positionName})`;
                 }
             }

            const item = `
                <button type="button"
                        class="list-group-item list-group-item-action employee-autocomplete-item ${itemClass}"
                        data-emp-no="${employee.empNo}"
                        data-emp-name="${employee.empName}"
                        data-payroll-available="${isAvailable}"
                        data-unavailable-reason="${employee.payrollUnavailableReason || ''}">
                    ${displayText}
                </button>
            `;

            box.append(item);
        });

        box.removeClass('d-none');
    }

    /**
     * =====================================================
     * autocomplete 선택
     * =====================================================
     */

    $(document).on('click', '.employee-autocomplete-item', function () {

        const empNo = $(this).data('emp-no');
        const empName = $(this).data('emp-name');
        const payrollAvailable = $(this).data('payroll-available');

        if (payrollAvailable === false || payrollAvailable === 'false') {
            return;
        }

        /**
         * 실제 업무 처리에는 사번이 필요하므로
         * 전역 상태값에는 사번을 저장한다.
         */
        currentEmpNo = empNo;
        savePayrollTempState();

        /**
         * hidden input에도 실제 사번을 저장한다.
         * 화면 표시값과 실제 조회 기준값을 분리하기 위함이다.
         */
        $('#selectedEmpNo').val(empNo);

        /**
         * 검색창 표시값 처리
         *
         * - 숫자로 검색했다면 사번을 표시한다.
         * - 이름으로 검색했다면 사원명을 표시한다.
         *
         * 실제 조회는 currentEmpNo / selectedEmpNo 기준으로 진행된다.
         */
        const keyword = $('#employeeSearchInput').val().trim();

        if (/^\d+$/.test(keyword)) {
            $('#employeeSearchInput').val(empNo);
        } else {
            $('#employeeSearchInput').val(empName);
        }

        /**
         * autocomplete 목록 숨김
         */
        $('#employeeAutocompleteBox')
            .addClass('d-none')
            .empty();

        /**
         * 선택한 사원의 인사정보 조회
         */
        loadEmployeeInfo(empNo);
    });

    /**
     * =====================================================
     * 인사정보 조회
     * =====================================================
     */

    function loadEmployeeInfo(empNo) {

         $.ajax({
                url: `/admin/payroll/main/employees/${empNo}`,
                type: 'GET',

                success: function (result) {

                    $('#empNo').val(result.empNo);
                    $('#empName').val(result.empName);
                  const deptDisplayName = result.parentDeptName
                      ? `${result.deptName} (${result.parentDeptName})`
                      : result.deptName;

                  $('#deptName').val(deptDisplayName);
                    $('#positionName').val(result.positionName);
                    $('#gradeId').val(result.gradeId);
                    $('#gradeDescription').text(result.gradeDescription);
                    $('#empStatus').val(result.statusName || result.status);
                    $('#hireDate').val(result.hireDate);
                    $('#bank').val(result.bank);
                    $('#accountNo').val(result.accountNo);

                    /**
                     * 작성년월 옵션 조회
                     * - 입사일
                     * - 현재월
                     * - 최근 5년 제한
                     * 백단 getPeriodOptions() 결과 사용
                     */
                    loadPeriodOptions(empNo);
                },

                error: function (xhr) {
                    alert(xhr.responseText || '사원 정보를 불러오지 못했습니다.');
                }
            });
        }

    /**
     * =====================================================
     * 작성년월 옵션 조회
     * =====================================================
     *
     * 백단:
     * GET /admin/payroll/main/period-options/{empNo}
     *
     * 역할:
     * - 입사년도 이전 선택 불가
     * - 현재월 이후 선택 불가
     * - 최대 최근 5년 범위
     */
    function loadPeriodOptions(empNo) {

        $.ajax({
            url: `/admin/payroll/main/period-options/${empNo}`,
            type: 'GET',

            success: function (result) {

                /**
                 * 작성년월 계산에 필요한 기준값 저장
                 *
                 * - hireDate: 입사일
                 * - defaultYear/defaultMonth: 현재 날짜 기준 기본 작성년월
                 */
                currentHireDate = result.hireDate ? new Date(result.hireDate) : null;
                currentDefaultYear = result.defaultYear;
                currentDefaultMonth = result.defaultMonth;

                /**
                 * 작성년도/작성월 select 렌더링
                 *
                 * renderPeriodOptions 내부에서
                 * - 조회 가능 연도 생성
                 * - 선택 연도 기준 월 목록 생성
                 * - 기본값 현재년월 세팅
                 */
                renderPeriodOptions(result);

                /**
                 * 사원 선택 후 작성년월 select 활성화
                 *
                 * 현재년월은 자동조회할 것이므로
                 * 조회 버튼은 처음에는 비활성화한다.
                 */
                $('#payYear').prop('disabled', false);
                $('#payMonth').prop('disabled', false);
                $('#periodSearchBtn').prop('disabled', true);

                /**
                 * 사원 선택 직후 현재년월 자동조회
                 */
                searchPayroll();
            },

            error: function (xhr) {
                alert(xhr.responseText || '작성년월 옵션 조회 중 오류가 발생했습니다.');
            }
        });
    }

    /**
     * 작성년월 select 렌더링
     */
    function renderPeriodOptions(result) {

        const yearSelect = $('#payYear');

        yearSelect.empty();
        yearSelect.append('<option value="">작성년도 선택</option>');

        /**
         * 백단에서 계산한 조회 가능 연도 목록 사용
         *
         * 예:
         * 현재 2026년이고 입사일이 2023-04이면
         * 2023, 2024, 2025, 2026 표시
         */
        if (result.availableYears) {
            result.availableYears.forEach(function (year) {
                yearSelect.append(`<option value="${year}">${year}년</option>`);
            });
        }

        /**
         * 기본 작성년도 세팅
         */
        yearSelect.val(result.defaultYear);

        /**
         * 기본 작성년도 기준으로 월 목록 생성
         */
        renderMonthOptionsByYear(result.defaultYear, result.defaultMonth);
    }

    /**
     * 작성년도 변경 시 월 옵션 재구성
     *
     * 1차 프론트 기준:
     * - 기본 옵션은 백단에서 처음 내려준 범위 사용
     * - 연도 변경 후 조회 버튼을 눌러 다시 상태 조회
     */
   $('#payYear').on('change', function () {

       const selectedYear = Number($(this).val());

       /**
        * 연도 변경 시 해당 연도에 맞는 월 목록을 다시 만든다.
        *
        * 기존 선택 월이 새 연도에서 유효하면 유지하고,
        * 유효하지 않으면 "작성월 선택"으로 초기화한다.
        */
       renderMonthOptionsByYear(selectedYear);

       /**
        * 작성년월이 바뀌면 아직 해당 기간을 조회하지 않은 상태다.
        * 따라서 계산결과를 초기화하고 조회 버튼 상태를 다시 계산한다.
        */
       resetPreviewResult();

       updatePeriodSearchButtonState();

       $('#saveBtn').prop('disabled', true);
       $('#confirmBtn').prop('disabled', true);
       $('#payConfirmBtn').prop('disabled', true);
   });

    $('#payMonth').on('change', function () {

        /**
         * 작성월이 바뀌면 아직 해당 기간을 조회하지 않은 상태다.
         * 따라서 계산결과를 초기화하고 조회 버튼 상태를 다시 계산한다.
         */
        resetPreviewResult();

        updatePeriodSearchButtonState();

        $('#saveBtn').prop('disabled', true);
        $('#confirmBtn').prop('disabled', true);
        $('#payConfirmBtn').prop('disabled', true);
    });

    /**
     * 작성년월 조회 버튼 상태 제어
     *
     * 조회 버튼은 다음 경우에만 활성화한다.
     *
     * 1. 사원이 선택되어 있음
     * 2. 작성년도/작성월이 선택되어 있음
     * 3. 현재 선택한 작성년월이 마지막으로 조회 완료된 작성년월과 다름
     *
     * 즉:
     * - 사원 선택 직후 현재년월 자동조회 후에는 비활성화
     * - 과거월 조회 후 같은 과거월에서는 비활성화
     * - 년/월을 바꾼 경우에만 활성화
     */
    function updatePeriodSearchButtonState() {

        if (!currentEmpNo) {
            $('#periodSearchBtn').prop('disabled', true);
            return;
        }

        const selectedYear = Number($('#payYear').val());
        const selectedMonth = Number($('#payMonth').val());

        if (!selectedYear || !selectedMonth) {
            $('#periodSearchBtn').prop('disabled', true);
            return;
        }

        const sameAsLoaded =
            selectedYear === lastLoadedPayYear
            && selectedMonth === lastLoadedPayMonth;

        $('#periodSearchBtn').prop('disabled', sameAsLoaded);
    }

    /**
     * 선택 연도 기준 작성월 목록 렌더링
     *
     * 규칙:
     * 1. 현재 연도이면 현재월까지만 표시
     * 2. 입사 연도이면 입사월부터 표시
     * 3. 과거 중간 연도이면 1월~12월 표시
     * 4. 기존 선택 월이 새 연도에서 유효하면 유지
     * 5. 기존 선택 월이 유효하지 않으면 작성월 선택으로 초기화
     */
    function renderMonthOptionsByYear(selectedYear, preferredMonth) {

        const monthSelect = $('#payMonth');

        const previousMonth = preferredMonth || Number(monthSelect.val());

        monthSelect.empty();
        monthSelect.append('<option value="">작성월 선택</option>');

        if (!selectedYear) {
            return;
        }

        selectedYear = Number(selectedYear);

        let startMonth = 1;
        let endMonth = 12;

        /**
         * 입사 연도이면 입사월부터 조회 가능
         */
        if (currentHireDate && selectedYear === currentHireDate.getFullYear()) {
            startMonth = currentHireDate.getMonth() + 1;
        }

        /**
         * 현재 연도이면 현재월까지만 조회 가능
         */
        if (selectedYear === currentDefaultYear) {
            endMonth = currentDefaultMonth;
        }

        /**
         * 월 option 생성
         */
        for (let month = startMonth; month <= endMonth; month++) {
            monthSelect.append(`<option value="${month}">${month}월</option>`);
        }

        /**
         * 기존 선택 월 유지 여부 판단
         *
         * 예:
         * - 2024년 2월 선택 후 2023년으로 변경
         * - 입사일이 2023-04이면 2월은 유효하지 않으므로 placeholder로 초기화
         *
         * 예:
         * - 2024년 12월 선택 후 2023년으로 변경
         * - 입사일이 2023-04이면 12월은 유효하므로 유지
         */
        if (previousMonth >= startMonth && previousMonth <= endMonth) {
            monthSelect.val(previousMonth);
        } else {
            monthSelect.val('');
        }
    }

    /**
     * =====================================================
     * 조회 버튼
     * =====================================================
     */

    $('#periodSearchBtn').on('click', function () {

        searchPayroll();
    });

    /**
     * =====================================================
     * 급여대장 조회
     * =====================================================
     */

    function searchPayroll() {

        if (!currentEmpNo) {
            alert('사원을 먼저 선택해 주세요.');
            return;
        }

        const payYear = Number($('#payYear').val());
        const payMonth = Number($('#payMonth').val());

        if (!payYear || !payMonth) {
            alert('작성년도와 작성월을 선택해 주세요.');
            return;
        }

        currentPayMonth = `${payYear}-${String(payMonth).padStart(2, '0')}`;

        previewCompleted = false;

        previewResult = null;
        previewModalResult = null;

        lastAppliedPreviewResultJson = '';

        clearInsuranceFields();

        /**
         * 이전 계산 미리보기 화면 초기화
         */
        $('#previewSummaryArea').empty();
        $('#previewAllowanceArea').empty();
        $('#previewDeductionArea').empty();
        $('#previewInsuranceArea').empty();
        $('#previewWithholdingTaxArea').empty();

        $('#previewStateBadge')
            .removeClass('text-bg-success')
            .addClass('text-bg-secondary')
            .text('계산 필요');

        $.ajax({
            url: '/admin/payroll/main/status',
            type: 'GET',
            data: {
                empNo: currentEmpNo,
                payYear: payYear,
                payMonth: payMonth
            },
            success: function (statusResult) {

                currentPayrollStatus = statusResult.payrollStatus;

                /**
                 * 마지막으로 조회 완료된 작성년월 저장
                 *
                 * 이 값과 현재 select 값이 같으면
                 * 조회 버튼을 다시 누를 필요가 없으므로 비활성화한다.
                 */
                lastLoadedPayYear = payYear;
                lastLoadedPayMonth = payMonth;

                renderPayrollStatus(statusResult);

                renderSavedInsurance(statusResult);

                loadBaseSalary(payYear, payMonth);

                loadPayrollItems(payYear, payMonth);

                updatePeriodSearchButtonState();

                applyButtonState(currentPayrollStatus);
            },
            error: function (xhr) {
                alert(xhr.responseText || '급여대장 상태 조회 중 오류가 발생했습니다.');
            }
        });
    }

    /**
     * =====================================================
     * 상태 렌더링
     * =====================================================
     */

   function renderPayrollStatus(result) {

       let statusText = result.payrollStatusName || '';

       /**
        * 지급완료 상태이면
        * 지급일을 상태명 옆에 함께 표시
        */
       if (result.payrollStatus === 'PAID' && result.payDate) {

           statusText += ` (지급일 : ${result.payDate})`;
       }

       $('#payrollStatusName').val(statusText);

       /**
        * 기존 하단 지급일 영역 제거
        */
       $('#payDateText').text('');
   }

   /**
    * =====================================================
    * 기본급 자동로딩 + 정책경고
    * =====================================================
    *
    * 백단 getBaseSalary() 결과를 사용한다.
    * - NEW: 최근 확정/지급완료 기본급 또는 현재 정책 기본급
    * - DRAFT: 저장 기본급 + 정책 변경 여부 판단
    * - CONFIRMED/PAID: 저장 기본급 그대로
    */
   function loadBaseSalary(payYear, payMonth) {

       $.ajax({
           url: '/admin/payroll/main/base-salary',
           type: 'GET',
           data: {
               empNo: currentEmpNo,
               payYear: payYear,
               payMonth: payMonth
           },
           success: function (result) {

              currentBaseSalaryInfo = result;
              if (result.salarySource === 'MANUAL') {

                  $('#baseSalaryInput').val('');

              } else if (result.baseSalary
                          && Number(result.baseSalary) !== 0) {

                  $('#baseSalaryInput')
                      .val(numberFormat(result.baseSalary));

              } else {

                  $('#baseSalaryInput').val('');
              }

               /**
                * 기본급 출처 문구 표시
                *
                * - NEW 상태에서만 표시
                * - DRAFT 이상은 저장된 값이므로 출처 문구 숨김
                */
               renderBaseSalarySource(result);
               renderBaseSalaryWarning(result);
               applyButtonState(currentPayrollStatus);
           },
           error: function (xhr) {
               alert(xhr.responseText || '기본급 조회 중 오류가 발생했습니다.');
           }
       });
   }

   /**
    * 기본급 정책 경고 표시
    */
   function renderBaseSalaryWarning(result) {

       $('#baseSalaryWarningBox').addClass('d-none');
       $('#baseSalaryWarningMessage').text('');

       baseSalaryDecisionRequired = false;
       baseSalaryDecisionCompleted = true;

       if (!result || !result.warningRequired) {
           return;
       }

       $('#baseSalaryWarningBox').removeClass('d-none');
       $('#baseSalaryWarningMessage').text(result.warningMessage || '');

       baseSalaryDecisionRequired = result.policyDecisionRequired === true;
       baseSalaryDecisionCompleted = result.policyDecisionRequired !== true;

       // 정책 선택이 필요한 경우만 버튼 노출
       if (result.policyDecisionRequired) {
           $('#applyPolicySalaryBtn').removeClass('d-none');
           $('#keepSavedSalaryBtn').removeClass('d-none');
       } else {
           $('#applyPolicySalaryBtn').addClass('d-none');
           $('#keepSavedSalaryBtn').addClass('d-none');
       }
   }

   /**
    * 기본급 출처 표시
    *
    * 역할:
    * - 기본급 자동 로딩 결과가 어디서 왔는지 사용자에게 안내한다.
    *
    * 표시 조건:
    * - 미작성(NEW) 상태에서만 표시한다.
    *
    * 숨김 조건:
    * - 작성중(DRAFT)
    * - 확정(CONFIRMED)
    * - 지급완료(PAID)
    *
    * 이유:
    * - DRAFT부터는 저장된 기본급이 연봉협상, 관리자 수정, 예외 조정 등으로
    *   바뀌었을 수 있으므로 최초 출처를 표시하지 않는다.
    */
   function renderBaseSalarySource(result) {

       // 기본은 항상 숨김 상태로 초기화
       $('#salarySourceText')
           .addClass('d-none')
           .text('');

       if (!result) {
           return;
       }

       // NEW 상태가 아니면 출처 문구 표시하지 않음
       if (currentPayrollStatus !== 'NEW') {
           return;
       }

       let sourceText = '';

       /**
        * 백단에서 내려주는 salarySource 코드값을
        * 화면에 보여줄 한글 문구로 변환한다.
        */
       if (result.salarySource === 'RECENT_CONFIRMED') {

           sourceText = '최근 확정/지급완료 기준 기본급';

       } else if (result.salarySource === 'POLICY') {

           sourceText = '기본급 정책 기준 기본급';

       } else if (result.salarySource === 'MANUAL') {

           sourceText = '기본급 불러오기 실패 - 직접 입력';

       } else if (result.salarySource === 'PROMOTION_POLICY') {

           sourceText = '승진 - 기본급 정책 기준 기본급';

       } else if (result.salarySource === 'PROMOTION_RECENT_HIGHER') {

           sourceText = '승진 - 최근 확정/지급완료 기준 기본급';

       } else if (result.salarySource === 'DEMOTION_POLICY') {

           sourceText = '강등 - 기본급 정책 기준 기본급';
       }

       // 표시할 문구가 없으면 그대로 숨김
       if (!sourceText) {
           return;
       }

         $('#salarySourceText')
             .text(sourceText)
             .removeClass('d-none');
   }



   /**
    * 변경된 기본급 적용
    */
   $('#applyPolicySalaryBtn').on('click', function () {

       if (!currentBaseSalaryInfo || currentBaseSalaryInfo.policyBaseSalary == null) {
           alert('적용할 기본급 정책 금액이 없습니다.');
           return;
       }

       $('#baseSalaryInput').val(numberFormat(currentBaseSalaryInfo.policyBaseSalary));

       baseSalaryDecisionCompleted = true;

       $('#baseSalaryWarningBox').addClass('d-none');

       resetPreviewResult();
       applyButtonState(currentPayrollStatus);
   });

   /**
    * 기존 저장 기본급 유지
    */
  $('#keepSavedSalaryBtn').on('click', function () {

      if (
          currentBaseSalaryInfo
          && currentBaseSalaryInfo.savedBaseSalary != null
      ) {

          $('#baseSalaryInput')
              .val(numberFormat(currentBaseSalaryInfo.savedBaseSalary));
      }

      baseSalaryDecisionCompleted = true;
      baseSalaryDecisionRequired = false;

      $('#baseSalaryWarningBox')
          .addClass('d-none');

      /**
       * DRAFT에서 기존 저장값 유지 시:
       * - 4대보험 값 유지
       * - 계산 완료 상태 유지
       */
      if (currentPayrollStatus === 'DRAFT') {

          previewCompleted = true;

          $('#previewStateBadge')
              .removeClass('text-bg-secondary')
              .addClass('text-bg-success')
              .text('계산 완료');
      }

      applyButtonState(currentPayrollStatus);
      savePayrollTempState();
  });

   /**
    * =====================================================
    * 지급/공제항목 조회
    * =====================================================
    *
    * NEW: 최신 PAY_ITEM_SETTING
    * DRAFT: 저장 PAYROLL_ITEM 우선 + 변경 감지
    * CONFIRMED/PAID: 저장 snapshot
    */
   function loadPayrollItems(payYear, payMonth) {

       $.ajax({
           url: '/admin/payroll/main/items',
           type: 'GET',
           data: {
               empNo: currentEmpNo,
               payYear: payYear,
               payMonth: payMonth
           },
           success: function (result) {

               currentPayrollItems = result.items || [];

               renderPayrollItems(currentPayrollItems);

               renderItemSettingWarning(result);

               applyButtonState(currentPayrollStatus);

              setTimeout(function () {

                  if (skipNextSnapshotUpdate) {
                      skipNextSnapshotUpdate = false;
                      return;
                  }

                  const restoredRequestData = collectPayrollRequestDataForSnapshot();

                  if (restoredRequestData) {
                      lastSavedPayrollSnapshotJson =
                          makePayrollSnapshot(restoredRequestData);
                  }

              }, 100);
          },
           error: function (xhr) {
               alert(xhr.responseText || '지급/공제항목 조회 중 오류가 발생했습니다.');
           }
       });
   }

   /**
    * 지급/공제항목 변경 경고 표시
    */
   function renderItemSettingWarning(result) {

       $('#itemSettingWarningBox').addClass('d-none');
       $('#itemSettingWarningMessage').text('');

       itemSettingDecisionRequired = false;
       itemSettingDecisionCompleted = true;

        /**
        * 항목설정 모달에서 직접 저장한 직후에는
        * 이미 사용자가 변경을 의도한 것이므로 경고를 다시 띄우지 않는다.
        */
       if (suppressItemSettingWarningOnce) {
           suppressItemSettingWarningOnce = false;
           return;
       }

       if (!result || !result.itemSettingChanged) {
           return;
       }

       $('#itemSettingWarningBox').removeClass('d-none');
       $('#itemSettingWarningMessage').text(result.warningMessage || '지급/공제항목 설정이 변경되었습니다.');

       itemSettingDecisionRequired = true;
       itemSettingDecisionCompleted = false;
   }

   /**
    * 지급/공제항목 설정 변경 확인 처리
    *
    * 역할:
    * - 사용자가 '최신 설정 적용' 또는 '기존 항목 유지'를 눌렀을 때
    * - 백단에서 PAYROLL.updatedAt을 갱신하게 한다.
    * - 같은 항목 설정 변경 건에 대해 경고가 반복 표시되지 않게 한다.
    */
   function confirmItemSettingDecision(decision) {

       const requestData = {
           empNo: currentEmpNo,
           payYear: Number($('#payYear').val()),
           payMonth: Number($('#payMonth').val()),
           itemSettingDecision: decision
       };

       $.ajax({
           url: '/admin/payroll/main/item-settings/decision',
           type: 'POST',
           contentType: 'application/json',
           data: JSON.stringify(requestData),
           success: function () {

               itemSettingDecisionCompleted = true;
               itemSettingDecisionRequired = false;

               $('#itemSettingWarningBox').addClass('d-none');

               /**
                * 최신 설정 적용(APPLY)은 화면 항목 구조가 바뀐 상태다.
                * 하지만 아직 급여대장 저장 버튼을 누른 것은 아니므로,
                * loadPayrollItems()가 현재 화면을 "마지막 저장 상태"로 기록하면 안 된다.
                */
               if (decision === 'APPLY') {
                   resetPreviewResult();

                   skipNextSnapshotUpdate = true;
                   lastSavedPayrollSnapshotJson = '';
               }

               loadPayrollItems(
                   Number($('#payYear').val()),
                   Number($('#payMonth').val())
               );
           },
           error: function (xhr) {
               alert(xhr.responseText || '지급/공제항목 설정 변경 확인 처리 중 오류가 발생했습니다.');
           }
       });
   }

   /**
    * 최신 지급/공제항목 설정 적용
    */
   $('#applyLatestItemSettingBtn').on('click', function () {

       confirmItemSettingDecision('APPLY');
   });

  /**
   * 기존 저장 항목 유지
   */
  $('#keepSavedItemSettingBtn').on('click', function () {

      confirmItemSettingDecision('KEEP');
  });

    /**
     * 기본급 변경 시 계산결과 초기화
     *
     * 기본급은 계산에 직접 영향을 주므로
     * 사용자가 값을 수정하면 계산 미리보기 결과를 다시 받아야 한다.
     */
    $('#baseSalaryInput').on('input', function () {


    if (currentPayrollStatus === 'NEW') {

       $('#salarySourceText')
           .text('관리자 직접 입력')
           .removeClass('d-none');
    }

        resetPreviewResult();
        savePayrollTempState();
    });

   /**
    * 일반 지급/공제 금액 변경 시
    * 계산결과 초기화
    */
   $(document).on('input', '.payroll-amount-input', function () {

       resetPreviewResult();
       savePayrollTempState();
   });

   /**
    * =====================================================
    * 지급/공제 렌더링
    * =====================================================
    */

    function renderPayrollItems(items) {

        const allowanceBody = $('#allowanceTableBody');
        const deductionBody = $('#deductionTableBody');

        allowanceBody.empty();
        deductionBody.empty();

        if (!items || items.length === 0) {
            allowanceBody.empty();
            deductionBody.html(emptyDeductionRow());
            applyButtonState(currentPayrollStatus);
            return;
        }

        items.forEach(item => {

            let taxBadge = '';
            let calculationBadge = '<span class="badge text-bg-secondary">수동계산</span>';

            const linkedAttendanceType = item.linkedAttendanceType || '';

            if (linkedAttendanceType === 'OVERTIME' || linkedAttendanceType === 'ABSENCE') {
                calculationBadge = '<span class="badge text-bg-info">자동계산</span>';
            }

            if (item.itemType === 'ALLOWANCE') {
                if (item.taxType === 'TAXABLE') {
                    taxBadge = '<span class="badge text-bg-primary">과세</span>';
                } else if (item.taxType === 'NON_TAXABLE') {
                    taxBadge = '<span class="badge text-bg-success">비과세</span>';
                }
            }

            const displayItemName =
                String(item.itemNameSnapshot || '')
                    .replace(
                        '[',
                        '<br><small class="text-muted">'
                    )
                    .replace(
                        ']',
                        '</small>'
                    );

            let amountDisplay = '';

           if (linkedAttendanceType === 'OVERTIME') {
              amountDisplay = `
              <div class="d-flex align-items-center gap-2 flex-nowrap">

                     <div class="input-group input-group-sm" style="width: 90px;">
                         <input type="text"
                                class="form-control text-end attendance-count-input"
                                value="${item.overtimeMinutes != null ? numberFormat(item.overtimeMinutes) : '0'}"
                                readonly>
                         <span class="input-group-text">분</span>
                     </div>

                     <span class="fw-bold">×</span>

                     <div class="input-group input-group-sm" style="width: 150px;">
                         <input type="text"
                                class="form-control payroll-amount-input text-end"
                                value="${item.amount && Number(item.amount) !== 0 ? numberFormat(item.amount) : ''}"
                                placeholder="0">
                         <span class="input-group-text">원/60분</span>
                     </div>

                     <span class="fw-bold">=</span>

                     <input type="text"
                            class="form-control form-control-sm text-end attendance-calculated-amount"
                            value="0"
                            readonly
                            style="width: 120px;">
                 </div>
             `;
            }else if (linkedAttendanceType === 'ABSENCE') {

                 amountDisplay = `
                     <div class="d-flex align-items-center gap-2 flex-nowrap">

                            <div class="input-group input-group-sm" style="width: 80px;">
                                <input type="text"
                                       class="form-control text-end attendance-count-input"
                                       value="${item.absenceDays != null ? numberFormat(item.absenceDays) : '0'}"
                                       readonly>
                                <span class="input-group-text">일</span>
                            </div>

                            <span class="fw-bold">×</span>

                            <div class="input-group input-group-sm" style="width: 150px;">
                                <input type="text"
                                       class="form-control payroll-amount-input text-end"
                                       value="${item.amount && Number(item.amount) !== 0 ? numberFormat(item.amount) : ''}"
                                       placeholder="0">
                                <span class="input-group-text">원/하루</span>
                            </div>

                            <span class="fw-bold">=</span>

                            <input type="text"
                                   class="form-control form-control-sm text-end attendance-calculated-amount"
                                   value="0"
                                   readonly
                                   style="width: 120px;">
                        </div>
                    `;
              } else {
                amountDisplay = `
                    <input type="text"
                           class="form-control payroll-amount-input text-end"
                           value="${item.amount && Number(item.amount) !== 0 ? numberFormat(item.amount) : ''}"
                           placeholder="0">
                `;
            }

            const allowanceRow = `
                <tr class="payroll-item-row"
                   data-item-setting-id="${item.itemSettingId || ''}"
                   data-item-name="${item.itemNameSnapshot}"
                   data-item-type="${item.itemType}"
                   data-tax-type="${item.taxType || ''}"
                   data-non-tax-code="${item.nonTaxCode || ''}"
                   data-linked-attendance-type="${linkedAttendanceType}"
                   data-derived-adjustment="${item.derivedAdjustment === true}"
                   data-source-pay-month="${item.sourcePayMonth || ''}">
                  <td class="fw-semibold text-center">
                      ${displayItemName}
                  </td>
                    <td>${taxBadge}</td>
                    <td>${calculationBadge}</td>
                    <td>${amountDisplay}</td>
                </tr>
            `;

            const deductionRow = `
                <tr class="payroll-item-row"
                    data-item-setting-id="${item.itemSettingId || ''}"
                    data-item-name="${item.itemNameSnapshot}"
                    data-item-type="${item.itemType}"
                    data-tax-type="${item.taxType || ''}"
                    data-non-tax-code="${item.nonTaxCode || ''}"
                    data-linked-attendance-type="${linkedAttendanceType}"
                    data-derived-adjustment="${item.derivedAdjustment === true}"
                    data-source-pay-month="${item.sourcePayMonth || ''}">
                   <td class="fw-semibold text-center">
                       ${displayItemName}
                   </td>
                    <td>${calculationBadge}</td>
                    <td>${amountDisplay}</td>
                </tr>
            `;

            if (item.itemType === 'ALLOWANCE') {
                allowanceBody.append(allowanceRow);
            } else {
                deductionBody.append(deductionRow);
            }
        });

        recalculateAttendanceRows();
        applyButtonState(currentPayrollStatus);
    }

    // 화면상의 근태연동 행 계산금액 표시
    // - 실제 저장/확정 계산은 백단 preview가 최종 기준이다.
    // - 이 함수는 사용자가 입력 중인 단가를 즉시 확인하기 위한 화면 보조용이다.
    function recalculateAttendanceRows() {

        $('.payroll-item-row').each(function () {

            const row = $(this);
            const linkedAttendanceType = row.data('linked-attendance-type');

            if (linkedAttendanceType !== 'OVERTIME'
                    && linkedAttendanceType !== 'ABSENCE') {
                return;
            }

            const unitAmount = Number(removeComma(row.find('.payroll-amount-input').val() || '0'));
            const countText = row.find('.attendance-count-input').val() || '0';

            const count = Number(removeComma(countText.replace('분', '').replace('일', '')) || '0');

            let calculatedAmount = 0;

           if (linkedAttendanceType === 'OVERTIME') {

               calculatedAmount =
                   Math.round(
                       (count * unitAmount) / 60
                   );
           }

           if (linkedAttendanceType === 'ABSENCE') {

               calculatedAmount =
                   Math.round(
                       count * unitAmount
                   );
           }

            row.find('.attendance-calculated-amount')
                .val(numberFormat(calculatedAmount));
        });
    }

   $(document).on(
       'input',
       '.payroll-amount-input',
       function () {

           recalculateAttendanceRows();

           resetPreviewResult();

           savePayrollTempState();
       }
   );

    /**
     * =====================================================
     * 지급/공제항목설정 모달
     * =====================================================
     *
     * 규칙
     * - 기본급은 설정 모달에 표시하지 않는다.
     * - 기본급은 PAYROLL.baseSalary 전용 필드이다.
     * - 지급 + 근태연동 = 초과수당 자동입력
     * - 공제 + 근태연동 = 결근공제 자동입력
     * - 일반항목명에는 기본급/초과수당/결근공제 사용 금지
     * - 아무 변경 없이 등록하면 저장하지 않는다.
     * - 초기화는 모달을 열었을 때 상태로 되돌리는 기능이다.
     */

    // 지급공제항목설정 모달 열릴 때 현재 메인 항목을 복사
    $('#payItemSettingModal').on('show.bs.modal', function () {

        // CONFIRMED / PAID는 설정 변경 불가
        if (currentPayrollStatus === 'CONFIRMED' || currentPayrollStatus === 'PAID') {
            alert('확정 또는 지급완료 상태에서는 지급/공제항목 설정을 변경할 수 없습니다.');
            return false;
        }

        currentPayItemSettingRows = collectCurrentItemsForSettingModal();

        originalPayItemSettingJson = JSON.stringify(currentPayItemSettingRows);

        renderPayItemSettingModalRows(currentPayItemSettingRows);

        updatePayItemSettingSaveButtonState();
    });

    // 현재 메인 지급/공제항목을 모달용 데이터로 변환
    function collectCurrentItemsForSettingModal() {

        const result = [];

        $('.payroll-item-row').each(function () {

            const row = $(this);

            const itemName = row.data('item-name');
            const derivedAdjustment = row.data('derived-adjustment');

            // 기본급은 PAYROLL.baseSalary 전용이므로 설정 모달 대상이 아니다.
            if (itemName === '기본급') {
                return;
            }

            // 조정수당/조정공제는 시스템 자동 생성 항목이므로 설정 모달에 절대 반입하지 않는다.
            if (derivedAdjustment === true || derivedAdjustment === 'true') {
                return;
            }

            if (String(itemName || '').startsWith('조정수당')
                    || String(itemName || '').startsWith('조정공제')) {
                return;
            }

            // 중요:
            // 급여대장 메인의 일반항목은 전부 반입한다.
            // 여기에는 연장수당/결근공제 같은 근태연동 항목도 포함된다.
            result.push({
                itemSettingId: row.data('item-setting-id') || null,
                itemName: itemName,
                itemType: row.data('item-type'),
                taxType: row.data('tax-type') || null,
                nonTaxCode: row.data('non-tax-code') || null,
                linkedAttendanceType: row.data('linked-attendance-type') || null
            });
        });

        return result;
    }

    /**
     * 현재 메인 화면의 지급/공제항목 + 금액 보관
     *
     * 설정 모달 저장 후 최신 설정을 다시 렌더링할 때
     * 같은 항목이고 지급/공제 구분이 그대로이면 금액을 유지하기 위함이다.
     */
    function collectCurrentPayrollItemsWithAmount() {

        const result = [];

        $('.payroll-item-row').each(function () {

            const row = $(this);

            const amountValue =
                removeComma(
                    row.find('.payroll-amount-input').val() || '0'
                );

            const linkedAttendanceType =
                row.data('linked-attendance-type') || null;

            const countText =
                row.find('.attendance-count-input').val() || '0';

            const attendanceCount =
                Number(
                    removeComma(
                        countText
                            .replace('분', '')
                            .replace('일', '')
                    ) || '0'
                );

            result.push({
                itemSettingId: row.data('item-setting-id') || null,
                itemNameSnapshot: row.data('item-name'),
                itemType: row.data('item-type'),
                taxType: row.data('tax-type') || null,
                nonTaxCode: row.data('non-tax-code') || null,
                linkedAttendanceType: linkedAttendanceType,
                amount: amountValue,

                overtimeMinutes:
                    linkedAttendanceType === 'OVERTIME'
                        ? attendanceCount
                        : null,

                absenceDays:
                    linkedAttendanceType === 'ABSENCE'
                        ? attendanceCount
                        : null,

                derivedAdjustment:
                    row.data('derived-adjustment') === true
                    || row.data('derived-adjustment') === 'true',

                sourcePayMonth:
                    row.data('source-pay-month') || null
            });
        });

        return result;
    }

    /**
     * 최신 설정 목록에 기존 입력 금액을 반영한다.
     *
     * 금액 초기화 조건:
     * 1. 지급 ↔ 공제 변경
     * 2. 근태연동 예 -> 아니오/disabled 변경
     * 3. 근태연동 아니오/disabled -> 예 변경
     * 4. OVERTIME -> null, null -> OVERTIME
     * 5. ABSENCE -> null, null -> ABSENCE
     *
     * 금액 유지 조건:
     * - 항목명 변경
     * - 과세/비과세 변경
     * - 비과세 코드 변경
     * - 같은 지급/공제 구분
     * - 같은 근태연동 상태
     *
     * 중요:
     * - 급여대장 메인 일반항목에는 근태연동 연장수당/결근공제도 포함된다.
     * - 조정수당/조정공제는 설정 모달 대상이 아니므로 여기서 금액 보존 대상으로 보지 않는다.
     */
    function mergeLatestItemsWithCurrentAmount(latestItems, beforeItems) {

        if (!latestItems) {
            return [];
        }

        latestItems.forEach(function (item) {

            let matchedItem = null;

            (beforeItems || []).forEach(function (beforeItem) {

                const itemId = item.itemSettingId ? Number(item.itemSettingId) : null;
                const beforeId = beforeItem.itemSettingId ? Number(beforeItem.itemSettingId) : null;

                /**
                 * PAY_ITEM_SETTING 기준 항목은 반드시 itemSettingId로만 매칭한다.
                 * 이름 기준 매칭을 허용하면 일반공제가 결근공제/근태연동 항목과 섞일 수 있다.
                 */
                const sameId =
                    itemId !== null
                    && beforeId !== null
                    && itemId === beforeId;

                const sameType =
                    item.itemType === beforeItem.itemType;

                const sameAttendanceGroup =
                    getAttendanceAmountGroup(item.linkedAttendanceType)
                    === getAttendanceAmountGroup(beforeItem.linkedAttendanceType);

                if (sameId && sameType && sameAttendanceGroup) {
                    matchedItem = beforeItem;
                }
            });

            if (matchedItem) {
                item.amount = matchedItem.amount;
            } else {
                item.amount = 0;
            }
        });

        return latestItems;
    }

    function appendAdjustmentItemsAfterSettingSave(mergedItems, beforeItems) {

        const result = mergedItems ? [...mergedItems] : [];

        const hasOvertimeItem = result.some(item =>
            item
            && item.linkedAttendanceType === 'OVERTIME'
            && !isAdjustmentItem(item)
        );

        const hasAbsenceItem = result.some(item =>
            item
            && item.linkedAttendanceType === 'ABSENCE'
            && !isAdjustmentItem(item)
        );

        (beforeItems || []).forEach(item => {

            if (!isAdjustmentItem(item)) {
                return;
            }

            if (item.linkedAttendanceType === 'OVERTIME' && !hasOvertimeItem) {
                return;
            }

            if (item.linkedAttendanceType === 'ABSENCE' && !hasAbsenceItem) {
                return;
            }

            const alreadyExists = result.some(row =>
                row.itemNameSnapshot === item.itemNameSnapshot
            );

            if (!alreadyExists) {
                result.push(item);
            }
        });

        return result;
    }

    function isAdjustmentItem(item) {

        if (!item || !item.itemNameSnapshot) {
            return false;
        }

        return item.derivedAdjustment === true
            || item.itemNameSnapshot.startsWith('조정수당[')
            || item.itemNameSnapshot.startsWith('조정공제[');
    }

    function keepAdjustmentItemAmountAfterSettingSave(previewItems, beforeItems) {

        if (!previewItems) {
            return [];
        }

        previewItems.forEach(function (item) {

            if (!isAdjustmentItem(item)) {
                return;
            }

            const beforeItem = (beforeItems || []).find(function (oldItem) {
                return isAdjustmentItem(oldItem)
                    && oldItem.itemNameSnapshot === item.itemNameSnapshot;
            });

            if (!beforeItem) {
                return;
            }

            /**
             * 조정항목은 항목설정 대상이 아니므로
             * 항목설정 추가/삭제/변경과 무관하게 기존 단가를 유지한다.
             */
            item.amount = beforeItem.amount || 0;
        });

        return previewItems;
    }

    // 근태연동 금액 유지/초기화 판단용 그룹
    function getAttendanceAmountGroup(linkedAttendanceType) {

        if (linkedAttendanceType === 'OVERTIME') {
            return 'OVERTIME';
        }

        if (linkedAttendanceType === 'ABSENCE') {
            return 'ABSENCE';
        }

        // null, '', undefined, disabled, N 등은 전부 근태연동 아님으로 본다.
        return 'NONE';
    }

    // 모달 row 렌더링
    function renderPayItemSettingModalRows(items) {

        const tbody = $('#payItemSettingTableBody');
        tbody.empty();

        if (!items || items.length === 0) {
            tbody.append(makePayItemSettingRow(null));
            return;
        }

        items.forEach(function (item) {
            tbody.append(makePayItemSettingRow(item));
        });

        refreshAllPayItemSettingRows();
    }

    // 항목 추가
    $('#addPayItemSettingRowBtn').on('click', function () {
        $('#payItemSettingTableBody').append(makePayItemSettingRow(null));
        refreshAllPayItemSettingRows();
        updatePayItemSettingSaveButtonState();
    });

    // row 삭제
    $(document).on('click', '.setting-row-delete-btn', function () {
        $(this).closest('tr').remove();
        updatePayItemSettingSaveButtonState();
    });

    // 초기화 버튼: 모달 열었을 때 상태로 되돌림
    $('#resetPayItemSettingBtn').on('click', function () {

        const originalRows =
            JSON.parse(originalPayItemSettingJson || '[]');

        renderPayItemSettingModalRows(originalRows);

        $('#savePayItemSettingBtn').prop('disabled', true);
    });

   // 설정 저장
   $('#savePayItemSettingBtn').on('click', function () {

        if ($(this).prop('disabled')) {
            return;
        }

       /**
        * 설정 저장 전 현재 화면 금액을 보관한다.
        */
       const beforeItems = collectCurrentPayrollItemsWithAmount();

       /**
        * 모달 입력값 수집 및 검증
        */
       const items = collectPayItemSettingRequestItems();

       if (!items) {
           return;
       }

       const currentJson =
           makePayItemSettingCompareJson(
               collectPayItemSettingRowsForCompare()
           );

       const originalJson =
           makePayItemSettingCompareJson(
               JSON.parse(originalPayItemSettingJson || '[]')
           );

       if (currentJson === originalJson) {
           alert('변경된 항목 설정이 없습니다.');
           return;
       }

        $.ajax({
            url: '/admin/payroll/main/item-settings/save',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ items: items }),
            success: function (latestItems) {

                alert('지급/공제항목 설정이 저장되었습니다.');

                $('#payItemSettingModal').modal('hide');

                suppressItemSettingWarningOnce = true;
                itemSettingDecisionRequired = false;
                itemSettingDecisionCompleted = true;

                $('#itemSettingWarningBox').addClass('d-none');

                resetPreviewResult();

                skipNextSnapshotUpdate = true;
                lastSavedPayrollSnapshotJson = '';

                /**
                 * 항목설정 등록 직후에는 PAYROLL_ITEM snapshot을 바꾸지 않는다.
                 *
                 * NEW:
                 * - 저장된 급여대장이 없으므로 최신 항목 설정 기준으로 다시 조회한다.
                 *
                 * DRAFT:
                 * - 저장된 DRAFT snapshot은 유지해야 한다.
                 * - 현재 화면만 latestItems 기준으로 바꾼다.
                 * - 실제 반영은 급여대장 저장/확정/지급확정 시점에 이루어진다.
                 */
                if (currentPayrollStatus === 'NEW') {

                    loadPayrollItems(
                        Number($('#payYear').val()),
                        Number($('#payMonth').val())
                    );

                } else if (currentPayrollStatus === 'DRAFT') {

                    /**
                     * DRAFT에서는 PAYROLL_ITEM snapshot을 바꾸지 않고
                     * 최신 설정 + 근태연동 + 조정항목이 포함된 미리보기 목록만 다시 조회한다.
                     */
                    $.ajax({
                        url: '/admin/payroll/main/items/latest-preview',
                        type: 'GET',
                        data: {
                            empNo: currentEmpNo,
                            payYear: Number($('#payYear').val()),
                            payMonth: Number($('#payMonth').val())
                        },
                        success: function (previewItems) {

                           let mergedItems =
                               mergeLatestItemsWithCurrentAmount(
                                   previewItems || [],
                                   beforeItems
                               );

                           mergedItems =
                               keepAdjustmentItemAmountAfterSettingSave(
                                   mergedItems,
                                   beforeItems
                               );

                           currentPayrollItems = mergedItems;

                            renderPayrollItems(currentPayrollItems);

                            applyButtonState(currentPayrollStatus);
                            savePayrollTempState();
                        },
                        error: function (xhr) {
                            alert(xhr.responseText || '최신 항목 미리보기 조회 중 오류가 발생했습니다.');
                        }
                    });

                    return;

                } else {

                    loadPayrollItems(
                        Number($('#payYear').val()),
                        Number($('#payMonth').val())
                    );
                }

                applyButtonState(currentPayrollStatus);
                savePayrollTempState();
            },
            error: function (xhr) {
                alert(xhr.responseText || '지급/공제항목 설정 저장 중 오류가 발생했습니다.');
            }
        });
    });

    // 모달 row HTML 생성
    function makePayItemSettingRow(item) {

        const itemSettingId = item ? item.itemSettingId : '';
        const itemName = item ? item.itemName : '';
        const itemType = item ? item.itemType : '';
        const taxType = item ? item.taxType : '';
        const nonTaxCode = item ? item.nonTaxCode : '';
        const linkedAttendanceType = item ? (item.linkedAttendanceType || '') : '';

        const attendanceYn =
            linkedAttendanceType === 'OVERTIME' || linkedAttendanceType === 'ABSENCE'
                ? 'Y'
                : itemType
                       ? 'N'
                       : '';

        return `
            <tr class="pay-item-setting-row"
                data-item-setting-id="${itemSettingId}">

                <td>
                    <select class="form-select setting-item-type">
                        <option value="">선택</option>
                        <option value="ALLOWANCE" ${itemType === 'ALLOWANCE' ? 'selected' : ''}>지급</option>
                        <option value="DEDUCTION" ${itemType === 'DEDUCTION' ? 'selected' : ''}>공제</option>
                    </select>
                </td>

                <td>
                    <select class="form-select setting-tax-type">
                        <option value="">선택</option>
                        <option value="TAXABLE" ${taxType === 'TAXABLE' ? 'selected' : ''}>과세</option>
                        <option value="NON_TAXABLE" ${taxType === 'NON_TAXABLE' ? 'selected' : ''}>비과세</option>
                    </select>
                </td>

                <td>
                    <select class="form-select setting-non-tax-code">
                        <option value="">선택</option>
                        <option value="MEAL" ${nonTaxCode === 'MEAL' ? 'selected' : ''}>식대</option>
                        <option value="CAR" ${nonTaxCode === 'CAR' ? 'selected' : ''}>차량유지비</option>
                        <option value="RESEARCH" ${nonTaxCode === 'RESEARCH' ? 'selected' : ''}>연구활동비</option>
                        <option value="CHILDCARE" ${nonTaxCode === 'CHILDCARE' ? 'selected' : ''}>보육수당</option>
                        <option value="OVERSEAS" ${nonTaxCode === 'OVERSEAS' ? 'selected' : ''}>국외근로</option>
                    </select>
                </td>

                <td>
                    <select class="form-select setting-attendance-yn">
                        <option value="">선택</option>
                        <option value="Y" ${attendanceYn === 'Y' ? 'selected' : ''}>예</option>
                        <option value="N" ${attendanceYn === 'N' ? 'selected' : ''}>아니오</option>
                    </select>
                </td>

                <td>
                    <input type="text"
                           class="form-control setting-item-name"
                           value="${itemName}"
                           placeholder="항목명 입력">
                </td>

                <td>
                    <button type="button"
                            class="btn btn-sm btn-outline-danger setting-row-delete-btn">
                        삭제
                    </button>
                </td>
            </tr>
        `;
    }

    // 전체 row 상태 갱신
    function refreshAllPayItemSettingRows() {
        $('.pay-item-setting-row').each(function () {
            refreshPayItemSettingRow($(this));
        });
    }

    // row별 enable/disable 처리
    function refreshPayItemSettingRow(row) {

        const itemType = row.find('.setting-item-type').val();
        const taxType = row.find('.setting-tax-type').val();
        const attendanceYn = row.find('.setting-attendance-yn').val();

        const taxSelect = row.find('.setting-tax-type');
        const nonTaxSelect = row.find('.setting-non-tax-code');
        const attendanceSelect = row.find('.setting-attendance-yn');
        const nameInput = row.find('.setting-item-name');

        nameInput.prop('disabled', false).prop('readonly', false);

        if (!itemType) {
            taxSelect.val('').prop('disabled', true);
            nonTaxSelect.val('').prop('disabled', true);
            attendanceSelect.val('').prop('disabled', true);
            return;
        }

        if (itemType === 'DEDUCTION') {
            taxSelect.val('').prop('disabled', true);
            nonTaxSelect.val('').prop('disabled', true);
            attendanceSelect.prop('disabled', false);

            if (attendanceYn === 'Y') {
                nameInput.val('결근공제').prop('readonly', true);
            } else if (nameInput.val().trim() === '결근공제') {
                nameInput.val('');
            }

            return;
        }

        // 지급
        taxSelect.prop('disabled', false);

        if (!taxType) {
            nonTaxSelect.val('').prop('disabled', true);
            attendanceSelect.val('').prop('disabled', true);
            return;
        }

        if (taxType === 'NON_TAXABLE') {
            nonTaxSelect.prop('disabled', false);
            attendanceSelect.val('N').prop('disabled', true);

            if (nameInput.val().trim() === '연장수당') {
                nameInput.val('');
            }

            return;
        }

        // 지급 + 과세
        nonTaxSelect.val('').prop('disabled', true);
        attendanceSelect.prop('disabled', false);

        if (attendanceYn === 'Y') {
            nameInput.val('연장수당').prop('readonly', true);
        } else if (nameInput.val().trim() === '연장수당') {
            nameInput.val('');
        }
    }

    /**
     * 지급/공제 변경 이벤트
     *
     * 규칙:
     * - 지급/공제 구분이 바뀌면 기존 과세/비과세 값은 더 이상 맞지 않을 수 있다.
     * - 따라서 과세/비과세, 비과세 항목은 "선택" 상태로 돌린다.
     * - 단, 항목명은 사용자가 입력한 값이므로 유지한다.
     *
     * 예:
     * 지급 / 비과세 / 식대 / 복리후생비
     * → 공제로 변경하면
     * 공제 / 선택 / 선택 / 복리후생비
     */
    $(document).on('change', '.setting-item-type', function () {
        const row = $(this).closest('tr');

        const nameInput = row.find('.setting-item-name');
        const currentName = nameInput.val().trim();

        /**
         * 지급 ↔ 공제 변경 시
         * 기존 근태연동 전용 항목명은 더 이상 맞지 않으므로 제거한다.
         *
         * 예:
         * - 지급/근태연동/연장수당 → 공제로 변경 시 연장수당 제거
         * - 공제/근태연동/결근공제 → 지급으로 변경 시 결근공제 제거
         */
        if (currentName === '연장수당' || currentName === '결근공제') {
            nameInput.val('');
        }

        nameInput.prop('readonly', false);

        row.find('.setting-tax-type').val('');
        row.find('.setting-non-tax-code').val('');
        row.find('.setting-attendance-yn').val('');

        refreshPayItemSettingRow(row);
        updatePayItemSettingSaveButtonState();
    });

    /**
     * 과세/비과세 변경 이벤트
     *
     * 규칙:
     * - 지급 상태에서 과세/비과세가 바뀌면 비과세 항목은 다시 선택하게 한다.
     * - 항목명은 유지한다.
     *
     * 예:
     * 지급 / 비과세 / 식대 / 식대보조
     * → 과세로 변경하면
     * 지급 / 과세 / 선택 / 식대보조
     */
    $(document).on('change', '.setting-tax-type', function () {
        const row = $(this).closest('tr');

        row.find('.setting-non-tax-code').val('');
        row.find('.setting-attendance-yn').val('');

        refreshPayItemSettingRow(row);
    });

    // 근태연동 여부 변경 이벤트
    $(document).on('change', '.setting-attendance-yn', function () {
        const row = $(this).closest('tr');

        refreshPayItemSettingRow(row);
        updatePayItemSettingSaveButtonState();
    });

    // 항목명 입력
    $(document).on('input', '.setting-item-name', function () {
        updatePayItemSettingSaveButtonState();
    });

    // 저장 요청 데이터 수집 및 프론트 검증
    function collectPayItemSettingRequestItems() {

        const items = [];
        const nameSet = new Set();

        let valid = true;
        let message = '';

        $('.pay-item-setting-row').each(function () {

            const row = $(this);

            const itemSettingId = row.data('item-setting-id') || null;
            const itemType = row.find('.setting-item-type').val();
            const taxType = row.find('.setting-tax-type').val();
            const nonTaxCode = row.find('.setting-non-tax-code').val();
            let itemName = row.find('.setting-item-name').val().trim();
            const attendanceYn = row.find('.setting-attendance-yn').val();

            /**
             * 지급/공제 선택 검증
             */
            if (!itemType) {
                valid = false;
                message = '지급/공제 구분을 선택해 주세요.';
                return false;
            }

            /**
             * 항목명 검증
             */
            if (!itemName) {
                valid = false;
                message = '항목명을 입력해 주세요.';
                return false;
            }

            /**
             * 기본급 명칭 금지
             *
             * 기본급은 시스템 고정 항목이므로
             * "기본급", "기본급수당", "기본급공제"처럼
             * 기본급이라는 문구가 포함된 항목명은 등록하지 못하게 막는다.
             */
            if (itemName.includes('기본급')) {
                valid = false;
                message = '기본급이라는 명칭은 지급/공제항목 설정에 사용할 수 없습니다.';
                return false;
            }

            /**
             * 항목명 중복 방지
             */
            if (nameSet.has(itemName)) {
                valid = false;
                message = '중복된 항목명이 있습니다.';
                return false;
            }

            nameSet.add(itemName);

            let sendTaxType = null;
            let sendNonTaxCode = null;
            let sendLinkedAttendanceType = null;

            /**
             * 지급항목 검증
             *
             * - 지급은 과세/비과세 필수
             * - 비과세인 경우 비과세 항목 필수
             */
            if (itemType === 'ALLOWANCE') {

                if (!taxType) {
                    valid = false;
                    message = '지급항목은 과세/비과세를 선택해야 합니다.';
                    return false;
                }

                sendTaxType = taxType;

                if (taxType === 'NON_TAXABLE') {

                    if (!nonTaxCode) {
                        valid = false;
                        message = '비과세 항목을 선택해 주세요.';
                        return false;
                    }

                    sendNonTaxCode = nonTaxCode;
                    sendLinkedAttendanceType = null;
                }
                 if (taxType === 'TAXABLE') {
                        sendNonTaxCode = null;

                        if (!attendanceYn) {
                            valid = false;
                            message = '과세 지급항목은 근태연동 여부를 선택해 주세요.';
                            return false;
                        }

                        if (attendanceYn === 'Y') {
                            sendLinkedAttendanceType = 'OVERTIME';
                            itemName = '연장수당';
                        }
                 }
            }

            /**
             * 공제항목
             *
             * - 공제는 과세/비과세, 비과세 항목을 사용하지 않는다.
             */
            if (itemType === 'DEDUCTION') {
                sendTaxType = null;
                sendNonTaxCode = null;
                if (!attendanceYn) {
                        valid = false;
                        message = '공제항목은 근태연동 여부를 선택해 주세요.';
                        return false;
                    }

                    if (attendanceYn === 'Y') {
                        sendLinkedAttendanceType = 'ABSENCE';
                        itemName = '결근공제';
                    }
                }

            items.push({
                itemSettingId: itemSettingId,
                itemName: itemName,
                itemType: itemType,
                taxType: sendTaxType,
                nonTaxCode: sendNonTaxCode,
                linkedAttendanceType: sendLinkedAttendanceType,
            });
        });

        if (!valid) {
            alert(message);
            return null;
        }

        return items;
    }

    function normalizePayItemSettingRows(items) {

        if (!items) {
            return [];
        }

        return items.map(function (item) {

            return {

                /**
                 * 숫자 / 문자열 / null 차이 제거
                 */
                itemSettingId:
                    item.itemSettingId === null
                    || item.itemSettingId === undefined
                    || item.itemSettingId === ''
                        ? null
                        : Number(item.itemSettingId),

                /**
                 * itemName / itemNameSnapshot 차이 제거
                 * trim()으로 공백 차이 제거
                 */
                itemName:
                    (item.itemName
                        || item.itemNameSnapshot
                        || '')
                        .trim(),

                itemType:
                    item.itemType || '',

                /**
                 * '' 와 null 차이 제거
                 */
                taxType:
                    item.taxType || null,

                nonTaxCode:
                    item.nonTaxCode || null,

                linkedAttendanceType:
                    item.linkedAttendanceType || null
            };
        });
    }

    function makePayItemSettingCompareJson(items) {
        return JSON.stringify(
            normalizePayItemSettingRows(items)
        );
    }

    // 항목설정 모달의 현재 상태를 비교용으로 수집한다.
    // 저장 검증용 collectPayItemSettingRequestItems()를 쓰면
    // 입력 중인 상태에서 alert가 뜰 수 있어서 비교용은 따로 둔다.
    function collectPayItemSettingRowsForCompare() {

        const items = [];

        $('.pay-item-setting-row').each(function () {

            const row = $(this);

            const itemType = row.find('.setting-item-type').val();
            const taxType = row.find('.setting-tax-type').val();
            const nonTaxCode = row.find('.setting-non-tax-code').val();
            const attendanceYn = row.find('.setting-attendance-yn').val();

            let itemName = row.find('.setting-item-name').val().trim();

            let compareTaxType = null;
            let compareNonTaxCode = null;
            let linkedAttendanceType = null;

            if (itemType === 'ALLOWANCE') {

                compareTaxType = taxType || null;

                if (taxType === 'NON_TAXABLE') {
                    compareNonTaxCode = nonTaxCode || null;
                    linkedAttendanceType = null;
                }

                if (taxType === 'TAXABLE') {
                    compareNonTaxCode = null;

                    if (attendanceYn === 'Y') {
                        linkedAttendanceType = 'OVERTIME';
                        itemName = '연장수당';
                    }
                }
            }

            if (itemType === 'DEDUCTION') {

                compareTaxType = null;
                compareNonTaxCode = null;

                if (attendanceYn === 'Y') {
                    linkedAttendanceType = 'ABSENCE';
                    itemName = '결근공제';
                }
            }

            items.push({
                itemSettingId: row.data('item-setting-id') || null,
                itemName: itemName,
                itemType: itemType || '',
                taxType: compareTaxType,
                nonTaxCode: compareNonTaxCode,
                linkedAttendanceType: linkedAttendanceType
            });
        });

        return items;
    }

    // 최초 모달 진입 상태와 현재 상태를 비교해서 등록 버튼을 제어한다.
    // 삭제 후 다시 같은 항목을 추가해서 결과가 같으면 disabled 처리된다.
    function updatePayItemSettingSaveButtonState() {

        const originalRows =
            normalizePayItemSettingRows(
                JSON.parse(originalPayItemSettingJson || '[]')
            );

        const currentRows =
            normalizePayItemSettingRows(
                collectPayItemSettingRowsForCompare()
            );

        const originalJson = JSON.stringify(originalRows);
        const currentJson = JSON.stringify(currentRows);

        $('#savePayItemSettingBtn').prop('disabled', originalJson === currentJson);
    }

    /**
     * =====================================================
     * 급여대장 삭제
     * =====================================================
     *
     * - DRAFT 상태만 삭제 가능
     * - NEW는 DB row가 없으므로 삭제 대상 없음
     * - CONFIRMED / PAID는 삭제 불가
     */
    $('#deleteBtn').on('click', function () {

        if (currentPayrollStatus !== 'DRAFT') {
            alert('작성중 상태의 급여대장만 삭제할 수 있습니다.');
            return;
        }

        if (!confirm('작성중 급여대장을 삭제하시겠습니까?')) {
            return;
        }

        const requestData = {
            empNo: currentEmpNo,
            payYear: Number($('#payYear').val()),
            payMonth: Number($('#payMonth').val())
        };

        $.ajax({
            url: '/admin/payroll/main/delete',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function (message) {
                alert(message || '급여대장이 삭제되었습니다.');

                resetPreviewResult();

                // 삭제 후 같은 사원/년월 기준으로 다시 조회하면 NEW 상태로 돌아감
                searchPayroll();
            },
            error: function (xhr) {
                alert(xhr.responseText || '급여대장 삭제 중 오류가 발생했습니다.');
            }
        });
    });

    /**
     * =====================================================
     * 초기화
     * =====================================================
     *
     * - NEW / DRAFT에서만 사용
     * - 선택 사원과 작성년월은 유지
     * - 입력 중인 값과 계산결과는 초기화
     * - 1차 구현에서는 현재 사원/년월을 다시 조회하여 화면을 복구한다.
     */
    $('#resetBtn').on('click', function () {

        if (currentPayrollStatus === 'CONFIRMED' || currentPayrollStatus === 'PAID') {
            alert('확정 또는 지급완료 상태에서는 초기화할 수 없습니다.');
            return;
        }

        if (!confirm('현재 입력 중인 내용을 초기화하시겠습니까?')) {
            return;
        }

       resetPreviewResult();

       /**
        * 초기화는 현재 화면을 저장된 DB 상태로 다시 불러오는 기능이다.
        * 따라서 항목설정 직후에 걸어둔 snapshot 갱신 스킵을 해제해야 한다.
        */
       skipNextSnapshotUpdate = false;
       lastSavedPayrollSnapshotJson = '';

       // 현재 사원/작성년월 유지 후 다시 조회
       searchPayroll();
    });

    /**
     * =====================================================
     * 계산결과 초기화
     * =====================================================
     *
     * - 지급/공제항목 설정 변경
     * - 금액/단가 변경
     * - 기본급 변경
     * - 초기화/삭제 후 재조회
     *
     * 위 상황에서 4대보험 결과를 비우고 계산 미리보기를 다시 요구한다.
     */
    function resetPreviewResult() {

        lastAppliedPreviewResultJson = '';
        previewCompleted = false;
        previewResult = null;
        previewModalResult = null;

        $('#nationalPensionAmount').val('');
        $('#healthInsuranceAmount').val('');
        $('#longTermCareAmount').val('');
        $('#employmentInsuranceAmount').val('');
        $('#totalInsurance').val('');

        $('#previewStateBadge')
            .removeClass('text-bg-success')
            .addClass('text-bg-secondary')
            .text('계산 필요');

        applyButtonState(currentPayrollStatus);
    }

    /**
     * 계산 미리보기 모달 종료 시
     * 반영 버튼 상태 초기화
     */
    $('#payrollPreviewModal').on('hidden.bs.modal', function () {

        /**
         * 확정/지급완료 상태에서는
         * 계산 미리보기는 조회용으로만 사용한다.
         * 따라서 모달을 닫아도 반영 버튼을 다시 활성화하면 안 된다.
         */
        if (currentPayrollStatus === 'CONFIRMED'
                || currentPayrollStatus === 'PAID') {

            $('#applyPreviewBtn')
                .prop('disabled', true)
                .removeClass('btn-primary')
                .addClass('btn-secondary')
                .text('반영 불가');

            return;
        }

        $('#applyPreviewBtn')
            .prop('disabled', false)
            .removeClass('btn-secondary')
            .addClass('btn-primary')
            .text('반영');
    });

    function clearInsuranceFields() {

        $('#nationalPensionAmount').val('');
        $('#healthInsuranceAmount').val('');
        $('#longTermCareAmount').val('');
        $('#employmentInsuranceAmount').val('');
        $('#totalInsurance').val('');
    }

    /**
     * 지급/공제 설정 변경 경고 표시
     * - DRAFT 상태에서 최신 설정이 저장되면 바로 덮어쓰지 않고 사용자 선택을 받는다.
     */
    function showItemSettingChangedWarning() {

        $('#itemSettingWarningBox').removeClass('d-none');
        $('#itemSettingWarningMessage')
            .text('지급/공제항목 설정이 변경되었습니다. 변경된 항목 설정을 적용하시겠습니까?');

        $('#saveBtn').prop('disabled', true);
        $('#confirmBtn').prop('disabled', true);
        $('#payConfirmBtn').prop('disabled', true);
    }



    /**
     * =====================================================
     * 보험 렌더링
     * =====================================================
     */

    function renderInsurance(result) {

        $('#nationalPensionAmount')
            .val(numberFormat(result.nationalPensionAmount));

        $('#healthInsuranceAmount')
            .val(numberFormat(result.healthInsuranceAmount));

        $('#longTermCareAmount')
            .val(numberFormat(result.longTermCareAmount));

        $('#employmentInsuranceAmount')
            .val(numberFormat(result.employmentInsuranceAmount));

        $('#totalInsurance')
            .val(numberFormat(result.totalInsurance));
    }

    /**
     * 계산 미리보기 결과 동일 여부 비교
     *
     * 역할:
     * - 모달에서 새로 계산한 결과와
     * - 이미 메인 화면에 반영된 결과가 같은지 확인한다.
     *
     * 같으면 "반영" 버튼을 눌러도 실제 변경이 아니므로 막는다.
     */
    function isSamePreviewResult(newResult, oldResult) {

        /**
         * 둘 다 없으면 동일한 상태로 본다.
         */
        if (!newResult && !oldResult) {
            return true;
        }

        /**
         * 하나만 없으면 다른 상태
         */
        if (!newResult || !oldResult) {
            return false;
        }

        /**
         * 비교 대상 계산 필드
         */
        const compareFields = [
            'nationalPensionAmount',
            'healthInsuranceAmount',
            'longTermCareAmount',
            'employmentInsuranceAmount',
            'totalInsurance',

            'incomeTax',
            'localIncomeTax',

            'totalDeduction',
            'totalGross',
            'netSalary'
        ];

        /**
         * 숫자 정규화 비교
         */
        for (const field of compareFields) {

            const newValue =
                Number(removeComma(newResult[field] || 0));

            const oldValue =
                Number(removeComma(oldResult[field] || 0));

            if (newValue !== oldValue) {

                console.log('계산결과 다름:', field, newValue, oldValue);

                return false;
            }
        }

        return true;
    }

    function makePreviewResultSnapshot(result) {

        if (!result) {
            return '';
        }

        return JSON.stringify({

            nationalPensionAmount:
                Number(removeComma(result.nationalPensionAmount || 0)),

            healthInsuranceAmount:
                Number(removeComma(result.healthInsuranceAmount || 0)),

            longTermCareAmount:
                Number(removeComma(result.longTermCareAmount || 0)),

            employmentInsuranceAmount:
                Number(removeComma(result.employmentInsuranceAmount || 0)),

            totalInsurance:
                Number(removeComma(result.totalInsurance || 0)),

            incomeTax:
                Number(removeComma(result.incomeTax || 0)),

            localIncomeTax:
                Number(removeComma(result.localIncomeTax || 0)),

            totalDeduction:
                Number(removeComma(result.totalDeduction || 0)),

            totalGross:
                Number(removeComma(result.totalGross || 0)),

            netSalary:
                Number(removeComma(result.netSalary || 0))
        });
    }

   function renderSavedInsurance(result) {

      if (!result) {

          clearInsuranceFields();

          previewCompleted = false;

          previewResult = null;
          previewModalResult = null;
          lastAppliedPreviewResultJson = '';

          $('#previewStateBadge')
              .removeClass('text-bg-success')
              .addClass('text-bg-secondary')
              .text('계산 필요');

          return;
      }

       if (result.payrollStatus === 'NEW') {

           clearInsuranceFields();

           previewCompleted = false;
           previewResult = null;
           lastAppliedPreviewResultJson = '';

           $('#previewStateBadge')
               .removeClass('text-bg-success')
               .addClass('text-bg-secondary')
               .text('계산 필요');

           return;
       }

       const hasInsurance =
           result.nationalPensionAmount != null
           || result.healthInsuranceAmount != null
           || result.longTermCareAmount != null
           || result.employmentInsuranceAmount != null
           || result.totalInsurance != null;

       if (!hasInsurance) {

           clearInsuranceFields();

           previewCompleted = false;
           previewResult = null;
           lastAppliedPreviewResultJson = '';

           $('#previewStateBadge')
               .removeClass('text-bg-success')
               .addClass('text-bg-secondary')
               .text('계산 필요');

           return;
       }

       $('#nationalPensionAmount').val(numberFormat(result.nationalPensionAmount));
       $('#healthInsuranceAmount').val(numberFormat(result.healthInsuranceAmount));
       $('#longTermCareAmount').val(numberFormat(result.longTermCareAmount));
       $('#employmentInsuranceAmount').val(numberFormat(result.employmentInsuranceAmount));
       $('#totalInsurance').val(numberFormat(result.totalInsurance));

       /**
        * 저장된 계산결과 복원
        * - 화면에만 보험값을 보여주면 다시 저장 시 null로 덮어써질 수 있음
        * - JS 내부 계산 상태도 같이 복원한다.
        */
       previewCompleted = true;

       previewResult = {
            nationalPensionAmount: result.nationalPensionAmount || 0,
               healthInsuranceAmount: result.healthInsuranceAmount || 0,
               longTermCareAmount: result.longTermCareAmount || 0,
               employmentInsuranceAmount: result.employmentInsuranceAmount || 0,
               totalInsurance: result.totalInsurance || 0,

               incomeTax: result.incomeTax || 0,
               localIncomeTax: result.localIncomeTax || 0,

               totalDeduction: result.totalDeduction || 0,
               totalGross: result.totalGross || 0,
               netSalary: result.netSalary || 0,

               taxableAllowance: result.taxableAllowance || 0,
               nonTaxableAllowance: result.nonTaxableAllowance || 0,
               otherDeduction: result.otherDeduction || 0
       };

       $('#previewStateBadge')
           .removeClass('text-bg-secondary')
           .addClass('text-bg-success')
           .text('계산 완료');
       lastAppliedPreviewResultJson =
           makePreviewResultSnapshot(previewResult);
   }

    /**
     * =====================================================
     * 계산 미리보기 모달 렌더링
     * =====================================================
     *
     * 구성:
     * 1. 사원/지급년월 요약
     * 2. 지급 총액
     * 3. 공제 총액
     * 4. 최종 결과
     * 5. 4대보험 계산과정 접기/펼치기
     */
    function renderPreviewModal(result) {

        $('#insuranceDetailCollapse').collapse('hide');
        $('#withholdingTaxDetailCollapse').collapse('hide');

        const empName = result.empName || $('#empName').val();
        const empNo = result.empNo || currentEmpNo;
        const deptName = $('#deptName').val() || result.deptName;
        const positionName = result.positionName || $('#positionName').val();
        const payMonth = result.payMonth || currentPayMonth;

        /**
         * 상단 요약 + 3분할 결과
         */
        const summaryHtml = `
            <div class="alert alert-light border mb-3">
                <div class="row">
                    <div class="col-md-6 mb-2">
                        <span class="text-muted me-3">사원명</span>
                        <strong>${empName}(${empNo})</strong>
                    </div>
                    <div class="col-md-6 mb-2">
                        <span class="text-muted me-3">소속</span>
                        <strong>${deptName}</strong>
                    </div>
                    <div class="col-md-6">
                        <span class="text-muted me-3">작성년월</span>
                        <strong>${payMonth}</strong>
                    </div>
                    <div class="col-md-6">
                        <span class="text-muted me-3">직급</span>
                        <strong>${positionName}</strong>
                    </div>
                </div>
            </div>

            <div class="row g-3 mb-3">

                <div class="col-md-4">
                    <h6 class="fw-bold text-primary">1. 지급 총액</h6>
                    <table class="table table-bordered align-middle text-center">
                        <tbody>
                        <tr>
                            <th class="text-center">기본급</th>
                            <td class="text-end">${numberFormat(result.baseSalary)}</td>
                        </tr>
                        <tr>
                            <th class="text-center">과세 수당 합계<br><span class="small text-muted">(기본급 제외)</span></th>
                            <td class="text-end">${numberFormat(result.taxableAllowance)}</td>
                        </tr>
                        <tr>
                            <th class="text-center">비과세 수당 합계</th>
                            <td class="text-end">${numberFormat(result.nonTaxableAllowance)}</td>
                        </tr>
                        <tr class="table-light">
                            <th class="text-center text-primary">총 지급액</th>
                            <td class="text-end fw-bold text-primary">${numberFormat(result.totalGross)}</td>
                        </tr>
                        </tbody>
                    </table>
                </div>

                <div class="col-md-4">
                    <h6 class="fw-bold text-primary">2. 공제 총액</h6>
                    <table class="table table-bordered align-middle text-center">
                        <tbody>
                        <tr>
                            <th>4대보험 합계</th>
                            <td class="text-end">${numberFormat(result.totalInsurance)}</td>
                        </tr>
                        <tr>
                            <th>소득세</th>
                            <td class="text-end">${numberFormat(result.incomeTax)}</td>
                        </tr>
                        <tr>
                            <th>지방소득세</th>
                            <td class="text-end">${numberFormat(result.localIncomeTax)}</td>
                        </tr>
                        <tr>
                            <th>기타 공제</th>
                            <td class="text-end">${numberFormat(result.otherDeduction)}</td>
                        </tr>
                        <tr class="table-light">
                            <th class="text-primary">총 공제액</th>
                            <td class="text-end fw-bold text-primary">${numberFormat(result.totalDeduction)}</td>
                        </tr>
                        </tbody>
                    </table>
                </div>

                <div class="col-md-4">
                    <h6 class="fw-bold text-primary">3. 최종 결과</h6>
                    <div class="border rounded p-4 text-center bg-light">
                        <div class="text-muted mb-2">실수령액</div>
                        <div class="display-6 fw-bold text-primary">
                            ${numberFormat(result.netSalary)} 원
                        </div>
                    </div>

                    <div class="border rounded p-3 mt-2 text-center">
                        총 지급액 ${numberFormat(result.totalGross)}
                        -
                        총 공제액 ${numberFormat(result.totalDeduction)}
                        =
                        <br>
                        <strong>실수령액 ${numberFormat(result.netSalary)}</strong>
                    </div>
                </div>
            </div>
        `;

        $('#previewSummaryArea').html(summaryHtml);

        renderPreviewAllowanceArea(result);
        renderPreviewDeductionArea(result);
        renderPreviewInsuranceArea(result);
        renderPreviewWithholdingTaxArea(result);
    }

    /**
     * 지급 상세
     *
     * 기존 기능 유지:
     * - 계산 미리보기는 현재 메인 화면에 입력된 값을 기준으로 표시한다.
     * - 계산결과 반영 버튼을 누르기 전까지 메인 4대보험/세금 값은 변경하지 않는다.
     *
     * 근태연동 추가:
     * - 일반 지급항목: 수동계산
     * - 연장수당: 자동계산, "연장분 × 60분당 단가 / 60" 구조로 표시
     */
    function renderPreviewAllowanceArea(result) {

        let html = `
            <table class="table table-bordered align-middle text-center">
                <thead class="table-light">
                <tr>
                    <th>항목명</th>
                    <th>유형</th>
                    <th>계산유형</th>
                    <th>반영금액(원)</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td class="text-center">기본급</td>
                    <td class="text-center">과세</td>
                    <td class="text-center">수동계산</td>
                    <td class="text-end">${numberFormat(result.baseSalary)}</td>
                </tr>
        `;

        $('.payroll-item-row').each(function () {

            const row = $(this);

            if (row.data('item-type') !== 'ALLOWANCE') {
                return;
            }

            const itemName = row.data('item-name');
            const taxType = row.data('tax-type') === 'NON_TAXABLE' ? '비과세' : '과세';
            const linkedAttendanceType = row.data('linked-attendance-type') || '';

            const unitAmount = removeComma(row.find('.payroll-amount-input').val() || '0');

            let calculationType = '수동계산';
            let inputText = numberFormat(unitAmount);
            let calculatedAmount = Number(unitAmount || 0);
            let formulaHtml = '';

            if (linkedAttendanceType === 'OVERTIME') {

                calculationType = '자동계산';

                const minuteText = row.find('.attendance-count-input').val() || '0분';
                const overtimeMinutes = Number(removeComma(minuteText.replace('분', '')) || '0');

                calculatedAmount = Math.round(overtimeMinutes * Number(unitAmount || 0) / 60);

                inputText = `${numberFormat(overtimeMinutes)}분 / ${numberFormat(unitAmount)}원`;

                formulaHtml = `
                    <tr class="table-light">
                        <td colspan="4" class="text-center small text-primary fw-semibold">
                            계산식: ${numberFormat(overtimeMinutes)}분 × ${numberFormat(unitAmount)}원 / 60분
                            = ${numberFormat(calculatedAmount)}원
                        </td>
                    </tr>
                `;
            }

            html += `
                <tr>
                    <td class="text-center">${itemName}</td>
                    <td class="text-center">${taxType}</td>
                    <td class="text-center">${calculationType}</td>
                    <td class="text-end">${numberFormat(calculatedAmount)}</td>
                </tr>
                ${formulaHtml}
            `;
        });

        html += `
                </tbody>
            </table>
        `;

        $('#previewAllowanceArea').html(html);
    }

    /**
     * 공제 상세
     *
     * 기존 기능 유지:
     * - 4대보험, 소득세, 지방소득세는 기존 result 값을 그대로 사용한다.
     * - 일반 공제항목은 기존처럼 입력 금액을 표시한다.
     *
     * 근태연동 추가:
     * - 결근공제는 자동계산으로 표시한다.
     * - "결근일수 × 1일 공제단가" 구조로 보여준다.
     */
    function renderPreviewDeductionArea(result) {

        let html = `
            <table class="table table-bordered align-middle text-center">
                <thead class="table-light">
                <tr>
                    <th>항목명</th>
                    <th>계산유형</th>
                    <th>반영금액(원)</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td class="text-center">4대보험 합계</td>
                    <td class="text-center">자동계산</td>
                    <td class="text-end">${numberFormat(result.totalInsurance)}</td>
                </tr>
        `;

        $('.payroll-item-row').each(function () {

            const row = $(this);

            if (row.data('item-type') !== 'DEDUCTION') {
                return;
            }

            const itemName = row.data('item-name');
            const linkedAttendanceType = row.data('linked-attendance-type') || '';
            const unitAmount = removeComma(row.find('.payroll-amount-input').val() || '0');

            let calculationType = '수동계산';
            let inputText = numberFormat(unitAmount);
            let calculatedAmount = Number(unitAmount || 0);
            let formulaHtml = '';

            if (linkedAttendanceType === 'ABSENCE') {

                calculationType = '자동계산';

                const dayText = row.find('.attendance-count-input').val() || '0일';
                const absenceDays = Number(removeComma(dayText.replace('일', '')) || '0');

                calculatedAmount = Math.round(absenceDays * Number(unitAmount || 0));

                inputText = `${numberFormat(absenceDays)}일 / ${numberFormat(unitAmount)}원`;

                formulaHtml = `
                    <tr class="table-light">
                        <td colspan="3" class="text-center small text-primary fw-semibold">
                            계산식: ${numberFormat(absenceDays)}일 × ${numberFormat(unitAmount)}원
                            = ${numberFormat(calculatedAmount)}원
                        </td>
                    </tr>
                `;
            }

            html += `
                <tr>
                    <td class="text-center">${itemName}</td>
                    <td class="text-center">${calculationType}</td>
                    <td class="text-end">${numberFormat(calculatedAmount)}</td>
                </tr>
                ${formulaHtml}
            `;
        });

        html += `
                <tr>
                    <td class="text-center">소득세</td>
                    <td class="text-center">자동계산</td>
                    <td class="text-end">${numberFormat(result.incomeTax)}</td>
                </tr>
                <tr>
                    <td class="text-center">지방소득세</td>
                    <td class="text-center">자동계산</td>
                    <td class="text-end">${numberFormat(result.localIncomeTax)}</td>
                </tr>
                </tbody>
            </table>
        `;

        $('#previewDeductionArea').html(html);
    }

    /**
     * 계산 미리보기 상세 테이블 실제 row 수 조회
     *
     * 하드코딩 금지
     * - 현재 tbody tr 기준으로 자동 계산
     */
    function getPreviewRowCount(targetSelector) {

        return $(targetSelector)
            .find('tbody tr')
            .length;
    }

    /**
     * 4대보험 계산과정
     *
     * 기본은 접힌 상태로 보여주고,
     * 사용자가 필요할 때 펼쳐서 확인한다.
     */
    function renderPreviewInsuranceArea(result) {

        let rowsHtml = '';

        if (result.insuranceRows && result.insuranceRows.length > 0) {

            result.insuranceRows.forEach(function (row) {

                rowsHtml += `
                    <tr>
                        <td class="text-center">${row.name}</td>

                        <td class="text-center">
                            ${numberFormat(row.baseAmount)}
                        </td>

                        <td class="text-center">
                            ${rateFormat(row.rate)}
                        </td>

                        <td class="text-center">
                            ${row.formula || ''}
                        </td>

                        <td class="text-end">
                            ${numberFormat(row.amount)}
                        </td>
                    </tr>
                `;
            });
        }

        const html = `
            <div class="border rounded">

                <button type="button"
                        class="btn btn-light w-100 text-start fw-bold"
                        data-bs-toggle="collapse"
                        data-bs-target="#insuranceDetailCollapse">
                    4대보험 계산과정 보기
                </button>

                <div id="insuranceDetailCollapse" class="collapse">
                    <table class="table table-bordered align-middle mb-0">
                       <thead class="table-light">
                       <tr>
                           <th class="text-center">구분</th>

                           <th class="text-center">
                               기준금액
                           </th>

                           <th class="text-center">
                               요율
                           </th>

                           <th class="text-center">
                               계산식
                           </th>

                           <th class="text-center">
                               금액(원)
                           </th>
                       </tr>
                        </thead>
                        <tbody>
                        ${rowsHtml}
                        <tr class="table-light">

                            <!-- 구분 -->
                            <th class="text-center text-primary fw-bold">
                                4대보험 합계
                            </th>

                            <!-- 기준금액 -->
                            <td class="text-center">
                                -
                            </td>

                            <!-- 요율 -->
                            <td class="text-center">
                                -
                            </td>

                            <!-- 계산식 -->
                            <td class="text-center text-primary fw-semibold">

                                ${result.insuranceRows
                                    .map(row => numberFormat(row.amount))
                                    .join(' + ')}

                            </td>

                            <!-- 최종 합계 -->
                            <td class="text-center text-primary fw-bold">
                                <div class="text-end">
                                    ${numberFormat(result.totalInsurance)}
                                </div>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="form-text mt-2">
                ※ 계산 기준은 현재 설정된 보험요율을 기준으로 하며, 실제 처리 시 변경될 수 있습니다.
            </div>
        `;

        $('#previewInsuranceArea').html(html);
    }

    /**
     * =====================================================
     * 원천징수세 계산 상세 렌더링
     * =====================================================
     */
    function renderPreviewWithholdingTaxArea(result) {

        const area = $('#previewWithholdingTaxArea');

        area.empty();

        const totalGross =
            Number(result.totalGross || 0);

        const nonTaxableAmount =
            Number(result.nonTaxableAllowance || 0);

        const taxableAmount =
            totalGross - nonTaxableAmount;

        const incomeTax =
            Number(result.incomeTax || 0);

        const localIncomeTax =
            Number(result.localIncomeTax || 0);

        const totalWithholdingTax =
            incomeTax + localIncomeTax;

        const html = `

            <div class="border rounded">

                <button type="button"
                        class="btn btn-light w-100 text-start fw-bold"
                        data-bs-toggle="collapse"
                        data-bs-target="#withholdingTaxDetailCollapse">

                    원천징수세 계산과정 보기

                </button>

                <div id="withholdingTaxDetailCollapse"
                     class="collapse">

                    <table class="table table-bordered table-sm text-center align-middle mb-0">

                        <thead class="table-light">

                            <tr>
                                <th style="width: 25%">구분</th>
                                <th style="width: 25%">기준금액</th>
                                <th style="width: 30%">계산식</th>
                                <th style="width: 20%">금액(원)</th>
                            </tr>

                        </thead>

                        <tbody>

                            <tr>
                                <td>총 지급액</td>
                                <td>${numberFormat(totalGross)}</td>
                                <td>-</td>
                                <td class="text-end">${numberFormat(totalGross)}</td>
                            </tr>

                            <tr>
                                <td>비과세 제외금액</td>
                                <td>${numberFormat(nonTaxableAmount)}</td>
                                <td>총지급액 - 비과세수당</td>
                                <td class="text-end">${numberFormat(taxableAmount)}</td>
                            </tr>

                            <tr>
                                <td>원천징수 기준금액</td>
                                <td>${numberFormat(taxableAmount)}</td>
                                <td>과세대상금액 기준</td>
                                <td class="text-end">${numberFormat(taxableAmount)}</td>
                            </tr>

                            <tr>
                                <td>소득세</td>
                                <td>${numberFormat(taxableAmount)}</td>
                                <td>${numberFormat(taxableAmount)} × 0.03</td>
                                <td class="text-end">${numberFormat(incomeTax)}</td>
                            </tr>

                            <tr>
                                <td>지방소득세</td>
                                <td>${numberFormat(incomeTax)}</td>
                                <td>${numberFormat(incomeTax)} × 0.1</td>
                                <td class="text-end">${numberFormat(localIncomeTax)}</td>
                            </tr>

                            <tr class="table-light fw-bold">

                                <td class="text-primary">
                                    원천징수세 합계
                                </td>

                                <!--
                                  합계 row는 기준금액 개념이 아니므로
                                  "-" 처리
                                -->
                                <td class="text-primary">
                                    -
                                </td>

                                <!--
                                  실제 계산 흐름 표시
                                  예:
                                  45,000 + 4,500
                                -->
                                <td class="text-primary">

                                    ${numberFormat(incomeTax)}
                                    +
                                    ${numberFormat(localIncomeTax)}

                                </td>

                                <td class="text-end text-primary fw-bold">
                                    ${numberFormat(totalWithholdingTax)}
                                </td>

                            </tr>

                        </tbody>

                    </table>

                </div>

            </div>
        `;

        area.html(html);
    }

    /**
     * =====================================================
     * 계산 미리보기
     * =====================================================
     */

    $('#previewBtn').on('click', function () {

        const requestData = collectPayrollRequestData();

         /**
             * 요청 데이터 수집 실패 시 중단
             * - 필수값 누락
             * - 금액 오류
             * - 사원/년월 미선택
             * 같은 상황에서 Ajax 요청을 보내지 않기 위함
             */
            if (!requestData) {
                return;
            }

        $.ajax({
            url: '/admin/payroll/main/preview',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),

          success: function (result) {

              /**
               * 계산 미리보기 결과는
               * 모달 전용 임시 객체에만 저장한다.
               */
              previewModalResult = result;

              renderPreviewModal(result);

              /**
               * 현재 메인 화면에 이미 반영된 계산결과와
               * 새 계산결과가 동일한지 확인한다.
               *
               * 동일하면:
               * - 반영 버튼 비활성화
               * - 버튼 문구 변경
               */
             const previewAppliedRequestData = collectPayrollRequestData();

             if (previewAppliedRequestData) {

                 previewAppliedRequestData.nationalPensionAmount = previewModalResult.nationalPensionAmount;
                 previewAppliedRequestData.healthInsuranceAmount = previewModalResult.healthInsuranceAmount;
                 previewAppliedRequestData.longTermCareAmount = previewModalResult.longTermCareAmount;
                 previewAppliedRequestData.employmentInsuranceAmount = previewModalResult.employmentInsuranceAmount;
                 previewAppliedRequestData.totalInsurance = previewModalResult.totalInsurance;

                 previewAppliedRequestData.incomeTax = previewModalResult.incomeTax;
                 previewAppliedRequestData.localIncomeTax = previewModalResult.localIncomeTax;
                 previewAppliedRequestData.totalDeduction = previewModalResult.totalDeduction;
                 previewAppliedRequestData.totalGross = previewModalResult.totalGross;
                 previewAppliedRequestData.netSalary = previewModalResult.netSalary;
             }

             const sameResult =
                 isSamePreviewResult(previewModalResult, previewResult);

             const fixedStatus =
                 currentPayrollStatus === 'CONFIRMED'
                 || currentPayrollStatus === 'PAID';

             console.log('previewModalResult', previewModalResult);
             console.log('previewResult', previewResult);
             console.log('sameResult', sameResult);
             console.log('fixedStatus', fixedStatus);

             /**
              * 확정/지급완료 상태에서는
              * 계산 미리보기는 조회 전용이다.
              * 따라서 반영 버튼은 항상 비활성화한다.
              */
             if (fixedStatus) {

                 $('#applyPreviewBtn')
                     .prop('disabled', true)
                     .removeClass('btn-primary')
                     .addClass('btn-secondary')
                     .text('반영 불가');

             } else if (sameResult) {

                 $('#applyPreviewBtn')
                     .prop('disabled', true)
                     .removeClass('btn-primary')
                     .addClass('btn-secondary')
                     .text('이미 반영됨');

             } else {

                 $('#applyPreviewBtn')
                     .prop('disabled', false)
                     .removeClass('btn-secondary')
                     .addClass('btn-primary')
                     .text('반영');
             }

              $('#payrollPreviewModal').modal('show');
          },

            error: function (xhr) {
                alert(xhr.responseText || '계산 미리보기 중 오류가 발생했습니다.');
            }
        });
    });

    /**
     * =====================================================
     * 계산결과 반영
     * =====================================================
     *
     * - 모달에서 확인한 계산결과를 메인 4대보험 영역에 반영
     * - 이후 확정/지급확정 가능
     */
    $('#applyPreviewBtn').on('click', function () {

            /**
             * 모달에서 계산된 결과가 없으면 반영 불가
             */
            if (!previewModalResult) {
                alert('반영할 계산 결과가 없습니다.');
                return;
            }

            const previewAppliedRequestData = collectPayrollRequestData();

            if (previewAppliedRequestData) {

                previewAppliedRequestData.nationalPensionAmount = previewModalResult.nationalPensionAmount;
                previewAppliedRequestData.healthInsuranceAmount = previewModalResult.healthInsuranceAmount;
                previewAppliedRequestData.longTermCareAmount = previewModalResult.longTermCareAmount;
                previewAppliedRequestData.employmentInsuranceAmount = previewModalResult.employmentInsuranceAmount;
                previewAppliedRequestData.totalInsurance = previewModalResult.totalInsurance;

                previewAppliedRequestData.incomeTax = previewModalResult.incomeTax;
                previewAppliedRequestData.localIncomeTax = previewModalResult.localIncomeTax;
                previewAppliedRequestData.totalDeduction = previewModalResult.totalDeduction;
                previewAppliedRequestData.totalGross = previewModalResult.totalGross;
                previewAppliedRequestData.netSalary = previewModalResult.netSalary;
            }

            if ($(this).prop('disabled')
                    || isSamePreviewResult(previewModalResult, previewResult)) {
                return;
            }
            /**
             * 여기서부터가 실제 메인 반영이다.
             * previewResult는 "저장/확정/지급확정에 사용할 계산결과"이다.
             */
              previewResult = JSON.parse(
                  JSON.stringify(previewModalResult)
              );

              /**
               * 실제 메인 화면에 반영된 계산결과 snapshot 저장
               */
              lastAppliedPreviewResultJson =
                  makePreviewResultSnapshot(previewResult);

            renderInsurance(previewResult);

            previewCompleted = true;

            $('#previewStateBadge')
                .removeClass('text-bg-secondary')
                .addClass('text-bg-success')
                .text('계산 완료');

            $('#payrollPreviewModal').modal('hide');

            applyButtonState(currentPayrollStatus);

    });

    /**
     * =====================================================
     * 저장
     * =====================================================
     */

    $('#saveBtn').on('click', function () {

        const requestData = collectPayrollRequestData();

         /**
         * 저장 요청 데이터가 유효하지 않으면 저장 중단
         */
        if (!requestData) {
            return;
        }

        const currentSnapshotJson = makePayrollSnapshot(requestData);

        /**
         * DRAFT 상태에서 마지막 저장 상태와 현재 화면 상태가 같으면 저장하지 않는다.
         *
         * NEW는 아직 DB row가 없을 수 있으므로 저장 허용.
         * DRAFT만 무변경 저장 차단.
         */
        if (currentPayrollStatus === 'DRAFT'
                && lastSavedPayrollSnapshotJson
                && currentSnapshotJson === lastSavedPayrollSnapshotJson) {

            alert('변경된 내용이 없습니다.');
            return;
        }

        $.ajax({
            url: '/admin/payroll/main/save',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function () {

                // 저장 성공 시 현재 화면 상태를 마지막 저장 snapshot으로 보관
                lastSavedPayrollSnapshotJson = makePayrollSnapshot(requestData);

                alert('저장되었습니다.');

                searchPayroll();
            },
            error: function () {

                alert('저장 실패');
            }
        });
    });

    /**
     * =====================================================
     * 확정
     * =====================================================
     */

    $('#confirmBtn').on('click', function () {

        if (!confirm('급여대장을 확정하시겠습니까?')) {

            return;
        }

        const requestData = collectPayrollRequestData();

            /**
             * 확정 요청 데이터가 유효하지 않으면 확정 중단
             */
            if (!requestData) {
                return;
            }

        $.ajax({
            url: '/admin/payroll/main/confirm',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function () {

                alert('확정 처리되었습니다.');

                searchPayroll();
            },
            error: function (xhr) {

                alert(xhr.responseText || '확정 실패');
            }
        });
    });

    /**
     * 지급확정 버튼 클릭
     *
     * - 실제 지급완료 처리는 바로 하지 않는다.
     * - 먼저 지급일을 입력받기 위해 지급확정 모달을 연다.
     * - CONFIRMED 상태와 NEW/DRAFT 상태의 안내문구를 다르게 보여준다.
     */
    $('#payConfirmBtn').on('click', function () {

        /**
         * 계산 미리보기 결과가 메인 화면에 반영되어야 지급확정 가능
         */
        if (!previewCompleted) {
            alert('계산 미리보기를 먼저 완료해 주세요.');
            return;
        }

        /**
         * 지급일 기본값은 오늘 날짜로 세팅한다.
         */
        const today = new Date();

        const yyyy = today.getFullYear();
        const mm = String(today.getMonth() + 1).padStart(2, '0');
        const dd = String(today.getDate()).padStart(2, '0');

        $('#payDate').val(`${yyyy}-${mm}-${dd}`);

        /**
         * 상태별 지급확정 안내문구
         *
         * - CONFIRMED:
         *   이미 확정된 급여대장을 지급완료 처리
         *
         * - NEW / DRAFT:
         *   확정 단계를 건너뛰고 바로 지급완료 처리
         */
        if (currentPayrollStatus === 'CONFIRMED') {

            $('#payConfirmWarning').text(
                '지급완료 처리 후에는 수정, 삭제, 초기화가 불가능합니다.'
            );

        } else {

            $('#payConfirmWarning').text(
                '현재 상태에서 바로 지급완료 처리됩니다. 지급완료 후에는 수정, 삭제, 초기화가 불가능합니다.'
            );
        }

        /**
         * 지급확정 모달 열기
         */
        $('#payConfirmModal').modal('show');
    });

    /**
     * =====================================================
     * 지급확정
     * =====================================================
     */

    $('#executePayConfirmBtn').on('click', function () {

        const requestData = collectPayrollRequestData();
         /**
          * 지급확정 요청 데이터가 유효하지 않으면 중단
          */
            if (!requestData) {
                return;
            }

         /**
           * 지급일은 지급확정 필수값
           */
            if (!$('#payDate').val()) {
                alert('지급일을 선택해 주세요.');
                return;
            }

        requestData.payDate = $('#payDate').val();

        $.ajax({
            url: '/admin/payroll/main/pay-confirm',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function () {

                alert('지급완료 처리되었습니다.');

                searchPayroll();

                $('#payConfirmModal').modal('hide');
            },
            error: function () {

                alert('지급확정 실패');
            }
        });
    });

    /**
     * =====================================================
     * 요청 데이터 수집
     * =====================================================
     */

    function collectPayrollRequestData() {

        const itemList = [];

        /**
         * 사원 선택 여부 검증
         */
        if (!currentEmpNo) {
            alert('사원을 먼저 선택해 주세요.');
            return null;
        }

        /**
         * 작성년도/작성월 검증
         */
        const payYear = Number($('#payYear').val());
        const payMonth = Number($('#payMonth').val());

        if (!payYear || !payMonth) {
            alert('작성년도와 작성월을 선택해 주세요.');
            return null;
        }

        /**
         * 기본급 검증
         * - 기본급은 필수
         * - 0보다 커야 계산/저장 가능
         */
        const baseSalary = removeComma($('#baseSalaryInput').val());

        if (!baseSalary || Number(baseSalary) <= 0) {
            alert('기본급을 입력해 주세요.');
            return null;
        }

        $('.payroll-item-row').each(function () {

            const row = $(this);

            const amountInput = row.find('.payroll-amount-input');

            /**
             * 지급/공제항목 금액 처리
             *
             * - 일반항목:
             *   사용자가 입력한 input 값을 사용한다.
             *
             * - 근태연동항목:
             *   화면에는 단가만 표시하고 hidden input에 값을 넣어둔다.
             *
             * - 혹시 input이 없는 경우:
             *   JS 오류를 막기 위해 0으로 처리한다.
             */
            const amount = amountInput.length > 0
                ? removeComma(amountInput.val())
                : '0';

            const linkedAttendanceType =
                row.data('linked-attendance-type') || null;

            const countText =
                row.find('.attendance-count-input').val() || '0';

            const attendanceCount =
                Number(
                    removeComma(
                        countText
                            .replace('분', '')
                            .replace('일', '')
                    ) || '0'
                );

            itemList.push({
                itemSettingId: row.data('item-setting-id') || null,
                itemNameSnapshot: row.data('item-name'),
                itemType: row.data('item-type'),

                // 일반항목: 금액
                // 연장수당: 60분당 단가
                // 결근공제: 하루당 단가
                amount: amount,

                taxType: row.data('tax-type') || null,
                nonTaxCode: row.data('non-tax-code') || null,
                linkedAttendanceType: linkedAttendanceType,

                // 근태연동 계산용
                overtimeMinutes:
                    linkedAttendanceType === 'OVERTIME'
                        ? attendanceCount
                        : null,

                absenceDays:
                    linkedAttendanceType === 'ABSENCE'
                        ? attendanceCount
                        : null,

                // 조정항목 저장/확정/지급확정 연결용
                derivedAdjustment:
                    row.data('derived-adjustment') === true
                    || row.data('derived-adjustment') === 'true',

                sourcePayMonth:
                    row.data('source-pay-month') || null
            });
        });

        return {
            empNo: currentEmpNo,
            payYear: payYear,
            payMonth: payMonth,
            baseSalary: baseSalary,
            familyCount: 1,
            items: itemList,
            nationalPensionAmount: previewCompleted && previewResult ? previewResult.nationalPensionAmount : null,
            healthInsuranceAmount: previewCompleted && previewResult ? previewResult.healthInsuranceAmount : null,
            longTermCareAmount: previewCompleted && previewResult ? previewResult.longTermCareAmount : null,
            employmentInsuranceAmount: previewCompleted && previewResult ? previewResult.employmentInsuranceAmount : null,
            totalInsurance: previewCompleted && previewResult ? previewResult.totalInsurance : null,

            incomeTax: previewCompleted && previewResult ? previewResult.incomeTax : null,
            localIncomeTax: previewCompleted && previewResult ? previewResult.localIncomeTax : null,
            totalDeduction: previewCompleted && previewResult ? previewResult.totalDeduction : null,
            totalGross: previewCompleted && previewResult ? previewResult.totalGross : null,
            netSalary: previewCompleted && previewResult ? previewResult.netSalary : null
        };
    }

    function collectPayrollRequestDataForSnapshot() {

        if (!currentEmpNo) {
            return null;
        }

        const payYear = Number($('#payYear').val());
        const payMonth = Number($('#payMonth').val());

        if (!payYear || !payMonth) {
            return null;
        }

        const baseSalary = removeComma($('#baseSalaryInput').val());

        if (!baseSalary || Number(baseSalary) <= 0) {
            return null;
        }

        const itemList = [];

        $('.payroll-item-row').each(function () {

            const row = $(this);

            itemList.push({
                itemSettingId: row.data('item-setting-id') || null,
                itemNameSnapshot: row.data('item-name'),
                itemType: row.data('item-type'),
                amount: removeComma(row.find('.payroll-amount-input').val() || '0'),
                taxType: row.data('tax-type') || null,
                nonTaxCode: row.data('non-tax-code') || null,
                linkedAttendanceType: row.data('linked-attendance-type') || null
            });
        });

        return {
            empNo: currentEmpNo,
            payYear: payYear,
            payMonth: payMonth,
            baseSalary: baseSalary,
            familyCount: 1,
            items: itemList,

            nationalPensionAmount: previewResult ? previewResult.nationalPensionAmount : null,
            healthInsuranceAmount: previewResult ? previewResult.healthInsuranceAmount : null,
            longTermCareAmount: previewResult ? previewResult.longTermCareAmount : null,
            employmentInsuranceAmount: previewResult ? previewResult.employmentInsuranceAmount : null,
            totalInsurance: previewResult ? previewResult.totalInsurance : null,

            incomeTax: previewResult ? previewResult.incomeTax : null,
            localIncomeTax: previewResult ? previewResult.localIncomeTax : null,
            totalDeduction: previewResult ? previewResult.totalDeduction : null,
            totalGross: previewResult ? previewResult.totalGross : null,
            netSalary: previewResult ? previewResult.netSalary : null
        };
    }

    /**
     * 현재 화면 상태를 비교용 snapshot으로 만든다.
     *
     * 목적:
     * - DRAFT 상태에서 아무 값도 바꾸지 않고 저장하는 것을 막기 위함
     * - 무의미한 저장으로 PAYROLL.updatedAt이 바뀌면
     *   정책 변경 감지 / 지급공제항목 변경 감지 로직이 꼬일 수 있음
     *
     * 비교 대상:
     * - 기본급
     * - 지급/공제 항목명
     * - 지급/공제 구분
     * - 과세유형
     * - 비과세코드
     * - 금액
     * - 4대보험 / 세금 / 총액 / 실수령액
     */
    function makePayrollSnapshot(requestData) {

        if (!requestData) {
            return '';
        }

        const snapshot = {
            baseSalary: String(requestData.baseSalary || '0'),

            items: (requestData.items || []).map(function (item) {
                return {
                    itemSettingId: item.itemSettingId ? String(item.itemSettingId) : '',
                    itemNameSnapshot: item.itemNameSnapshot || '',
                    itemType: item.itemType || '',
                    amount: String(item.amount || '0'),
                    taxType: item.taxType || '',
                    nonTaxCode: item.nonTaxCode || '',
                    linkedAttendanceType: item.linkedAttendanceType || ''
                };
            }),

            nationalPensionAmount: String(requestData.nationalPensionAmount || '0'),
            healthInsuranceAmount: String(requestData.healthInsuranceAmount || '0'),
            longTermCareAmount: String(requestData.longTermCareAmount || '0'),
            employmentInsuranceAmount: String(requestData.employmentInsuranceAmount || '0'),
            totalInsurance: String(requestData.totalInsurance || '0'),

            incomeTax: String(requestData.incomeTax || '0'),
            localIncomeTax: String(requestData.localIncomeTax || '0'),
            totalDeduction: String(requestData.totalDeduction || '0'),
            totalGross: String(requestData.totalGross || '0'),
            netSalary: String(requestData.netSalary || '0')
        };

        return JSON.stringify(snapshot);
    }

    /**
     * =====================================================
     * 버튼 상태 제어
     * =====================================================
     */
   function applyButtonState(status) {

       /**
        * 전체 기본 비활성화
        */
       $('#saveBtn').prop('disabled', true);
       $('#confirmBtn').prop('disabled', true);
       $('#payConfirmBtn').prop('disabled', true);
       $('#deleteBtn').prop('disabled', true);
       $('#resetBtn').prop('disabled', true);
       $('#previewBtn').prop('disabled', true);
       $('#payItemSettingBtn').prop('disabled', true);

       /**
        * 사원 미선택
        */
       if (!currentEmpNo || !status) {
           return;
       }

       /**
        * 정책/항목 경고 미처리 여부
        */
       const decisionBlocked =
           (baseSalaryDecisionRequired && !baseSalaryDecisionCompleted)
           || (itemSettingDecisionRequired && !itemSettingDecisionCompleted);

       /**
        * =================================================
        * NEW
        * =================================================
        */
       if (status === 'NEW') {

           $('#baseSalaryInput').prop('disabled', false);

           $('.payroll-amount-input').prop('disabled', false);

           $('#payItemSettingBtn').prop('disabled', false);

           $('#previewBtn').prop('disabled', decisionBlocked);

           $('#saveBtn').prop('disabled', decisionBlocked);

           $('#resetBtn').prop('disabled', false);

           /**
            * 계산 완료 후만 확정 가능
            */
           if (previewCompleted && !decisionBlocked) {

               $('#confirmBtn').prop('disabled', false);

               $('#payConfirmBtn').prop('disabled', false);
           }

           return;
       }

       /**
        * =================================================
        * DRAFT
        * =================================================
        */
       if (status === 'DRAFT') {

           $('#baseSalaryInput').prop('disabled', false);

           $('.payroll-amount-input').prop('disabled', false);

           $('#payItemSettingBtn').prop('disabled', false);

           $('#previewBtn').prop('disabled', decisionBlocked);

           $('#saveBtn').prop('disabled', decisionBlocked);

           $('#deleteBtn').prop('disabled', false);

           $('#resetBtn').prop('disabled', false);

           /**
            * 계산 완료 후만 확정 가능
            */
           if (previewCompleted && !decisionBlocked) {

               $('#confirmBtn').prop('disabled', false);

               $('#payConfirmBtn').prop('disabled', false);
           }

           return;
       }

       /**
        * =================================================
        * CONFIRMED
        * =================================================
        */
       if (status === 'CONFIRMED') {

           $('#baseSalaryInput').prop('disabled', true);

           $('.payroll-amount-input').prop('disabled', true);

           $('#payItemSettingBtn').prop('disabled', true);

           $('#previewBtn').prop('disabled', false);

           $('#payConfirmBtn').prop('disabled', false);

           return;
       }

       /**
        * =================================================
        * PAID
        * =================================================
        */
       if (status === 'PAID') {

           $('#baseSalaryInput').prop('disabled', true);

           $('.payroll-amount-input').prop('disabled', true);

           $('#payItemSettingBtn').prop('disabled', true);

           $('#previewBtn').prop('disabled', false);

           return;
       }
   }

   function clearPayrollPageStateOnFreshEntry() {

       const navigationEntry = performance.getEntriesByType('navigation')[0];
       const navigationType = navigationEntry ? navigationEntry.type : '';

       /**
        * 새로고침은 나중에 sessionStorage 복구 대상으로 둘 예정이라
        * 여기서는 지우지 않는다.
        */
       if (navigationType === 'reload') {
           return;
       }

       /**
        * 다른 메뉴에서 급여대장으로 새로 들어온 경우는
        * 임시 저장값을 제거하고 빈 화면으로 시작한다.
        */
       sessionStorage.removeItem('payrollMainTempState');

       $('#employeeSearchInput').val('');
       $('#selectedEmpNo').val('');
       $('#employeeAutocompleteBox').addClass('d-none').empty();

       currentEmpNo = null;
       currentPayMonth = null;
       currentPayrollStatus = null;
       previewCompleted = false;
       previewResult = null;
   }

   function restorePayrollTempState() {

       const navigationEntry = performance.getEntriesByType('navigation')[0];
       const navigationType = navigationEntry ? navigationEntry.type : '';

       if (navigationType !== 'reload') {
           return;
       }

       const savedStateText = sessionStorage.getItem('payrollMainTempState');

       if (!savedStateText) {
           return;
       }

       const savedState = JSON.parse(savedStateText);

       if (!savedState.empNo) {
           return;
       }

       currentEmpNo = savedState.empNo;
       currentPayrollStatus = savedState.status || null;
       previewCompleted = savedState.previewCompleted === true;
       previewResult = savedState.previewResult || null;

       $('#employeeSearchInput').val(savedState.searchText || '');
       $('#selectedEmpNo').val(savedState.empNo);

       loadEmployeeInfo(savedState.empNo);
       setTimeout(function () {

           /**
            * 저장된 작성년월 복원
            */
           if (savedState.payYear) {
               $('#payYear').val(savedState.payYear);
           }

           renderMonthOptionsByYear(
               Number(savedState.payYear),
               Number(savedState.payMonth)
           );

           if (savedState.payMonth) {
               $('#payMonth').val(savedState.payMonth);
           }

           /**
            * 저장된 기본급 복원
            */
           if (savedState.baseSalary) {
               $('#baseSalaryInput').val(savedState.baseSalary);
           }

           /**
            * 저장된 지급/공제 금액 복원
            */
           if (savedState.items && savedState.items.length > 0) {

               savedState.items.forEach(function (savedItem) {

                   $('.payroll-item-row').each(function () {

                       const row = $(this);

                       const itemId = row.data('item-setting-id');

                       if (Number(itemId) === Number(savedItem.itemSettingId)) {

                           row.find('.payroll-amount-input')
                               .val(numberFormat(savedItem.amount || 0));
                       }
                   });
               });
           }

           /**
            * 계산결과 복원
            */
           if (savedState.previewCompleted && savedState.previewResult) {

               previewCompleted = true;
               previewResult = savedState.previewResult;

               renderInsurance(savedState.previewResult);

               $('#previewStateBadge')
                   .removeClass('text-bg-secondary')
                   .addClass('text-bg-success')
                   .text('계산 완료');
           }

           /**
            * 버튼 상태 재적용
            */
           applyButtonState(currentPayrollStatus);

           /**
            * 복원 완료 후 최신 상태 다시 저장
            *
            * - refresh 직후 일부 값 수정 시
            *   sessionStorage 상태 꼬임 방지
            */
           savePayrollTempState();

           }, 500);
   }

   function savePayrollTempState() {

       if (!currentEmpNo) {
           return;
       }

       const state = {
           empNo: currentEmpNo,
           searchText: $('#employeeSearchInput').val(),
           payYear: $('#payYear').val(),
           payMonth: $('#payMonth').val(),
           baseSalary: $('#baseSalaryInput').val(),
           status: currentPayrollStatus,
           previewCompleted: previewCompleted,
           previewResult: previewResult,
           items: collectCurrentPayrollItemsWithAmount()
       };

       sessionStorage.setItem('payrollMainTempState', JSON.stringify(state));
   }

    /**
     * =====================================================
     * 최초 비활성화
     * =====================================================
     */

    function initializeDisabledState() {

        $('#payYear').prop('disabled', true);
        $('#payMonth').prop('disabled', true);
        $('#periodSearchBtn').prop('disabled', true);

        $('#previewBtn').prop('disabled', true);
        $('#saveBtn').prop('disabled', true);
        $('#confirmBtn').prop('disabled', true);
        $('#payConfirmBtn').prop('disabled', true);
        $('#deleteBtn').prop('disabled', true);
        $('#resetBtn').prop('disabled', true);

        $('#payItemSettingBtn').prop('disabled', true);
    }

    /**
     * =====================================================
     * 빈 row
     * =====================================================
     */

    function emptyRow() {

        return `
            <tr>
                <td colspan="3" class="text-muted py-4">
                </td>
            </tr>
        `;
    }

    function emptyDeductionRow() {

        return `
            <tr>
                <td colspan="3" class="text-muted py-4">
                    공제항목이 없습니다.
                </td>
            </tr>
        `;
    }

    /**
     * =====================================================
     * 숫자 포맷
     * =====================================================
     */

    function numberFormat(value) {

        if (!value) {

            return '0';
        }

        return Number(value).toLocaleString();
    }

    /**
     * 요율 표시
     *
     * 0.045 -> 4.5%
     */
    function rateFormat(value) {

        if (value == null || value === '') {
            return '-';
        }

        return (Number(value) * 100).toFixed(3).replace(/\.?0+$/, '') + '%';
    }

    /**
     * =====================================================
     * 콤마 제거
     * =====================================================
     */

    function removeComma(value) {

        return String(value).replaceAll(',', '');
    }

});