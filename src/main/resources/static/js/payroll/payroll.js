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

    // 현재 화면에 표시 중인 지급/공제항목
    let currentPayrollItems = [];

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

        employeeList.forEach(employee => {

            /**
             * 표시 형식
             * 홍길동 (20209999 / 개발팀 / 사원)
             */
            const item = `
                <button type="button"
                        class="list-group-item list-group-item-action employee-autocomplete-item"
                        data-emp-no="${employee.empNo}"
                        data-emp-name="${employee.empName}">

                    ${employee.empName}
                    (${employee.empNo} / ${employee.deptName} / ${employee.positionName})

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

        resetPreviewResult();

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

               $('#baseSalaryInput').val(numberFormat(result.baseSalary));

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

       if (currentBaseSalaryInfo && currentBaseSalaryInfo.savedBaseSalary != null) {
           $('#baseSalaryInput').val(numberFormat(currentBaseSalaryInfo.savedBaseSalary));
       }

       baseSalaryDecisionCompleted = true;

       $('#baseSalaryWarningBox').addClass('d-none');

       resetPreviewResult();
       applyButtonState(currentPayrollStatus);
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

              loadPayrollItems(
                  Number($('#payYear').val()),
                  Number($('#payMonth').val())
              );

              /**
               * 최신 설정 적용(APPLY)은 항목 구조가 바뀌므로 계산결과 초기화.
               * 기존 항목 유지(KEEP)는 snapshot이 그대로라 계산결과를 굳이 초기화하지 않는다.
               */
              if (decision === 'APPLY') {
                  resetPreviewResult();
              }
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

            /**
             * 과세 / 비과세 표시
             */
            let typeBadge = '';

            if (item.itemType === 'ALLOWANCE') {

                if (item.taxType === 'TAXABLE') {
                    typeBadge = '<span class="badge text-bg-primary">과세</span>';
                } else if (item.taxType === 'NON_TAXABLE') {
                    typeBadge = '<span class="badge text-bg-success">비과세</span>';
                }

            } else if (item.itemType === 'DEDUCTION') {

                typeBadge = '<span class="badge text-bg-secondary">수동</span>';
            }

           /**
            * 금액 입력칸
            *
            * 근태연동은 후순위 구현으로 제외했으므로
            * 현재는 모든 지급/공제항목을 일반 금액 입력칸으로 표시한다.
            */
          let amountDisplay = `
              <input type="text"
                     class="form-control payroll-amount-input text-end"

                     /*
                      * 화면 표시용
                      * - 실제 금액이 0이거나 없으면 input value는 비워둔다.
                      * - 대신 placeholder="0"으로 흐리게 0을 보여준다.
                      * - 이렇게 해야 사용자가 입력한 0인지, 아직 미입력인지 구분하기 쉽다.
                      */
                     value="${item.amount && Number(item.amount) !== 0 ? numberFormat(item.amount) : ''}"
                     placeholder="0"

                     data-item-id="${item.itemSettingId}">
          `;

            const row = `
                <tr class="payroll-item-row"
                    data-item-setting-id="${item.itemSettingId || ''}"
                    data-item-name="${item.itemNameSnapshot}"
                    data-item-type="${item.itemType}"
                    data-tax-type="${item.taxType || ''}"
                    data-non-tax-code="${item.nonTaxCode || ''}"
                    data-linked-attendance-type="${item.linkedAttendanceType || ''}">

                    <td class="fw-semibold">
                        ${item.itemNameSnapshot}
                    </td>

                    <td>
                        ${typeBadge}
                    </td>

                    <td>
                        ${amountDisplay}
                    </td>
                </tr>
            `;

            /**
             * 지급 / 공제 구분
             */
            if (item.itemType === 'ALLOWANCE') {

                allowanceBody.append(row);

            } else {

                deductionBody.append(row);
            }
        });

         /**
          * 상태별 readonly 재적용
          *
          * - CONFIRMED
          * - PAID
          *
          * 상태에서는 input 수정 불가 처리
          */
        applyButtonState(currentPayrollStatus);
    }

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
    });

    // 현재 메인 지급/공제항목을 모달용 데이터로 변환
    function collectCurrentItemsForSettingModal() {

        const result = [];

        $('.payroll-item-row').each(function () {

            const itemName = $(this).data('item-name');

            // 기본급은 설정 대상이 아니므로 제외
            if (itemName === '기본급') {
                return;
            }

            result.push({
                itemSettingId: $(this).data('item-setting-id') || null,
                itemName: itemName,
                itemType: $(this).data('item-type'),
                taxType: $(this).data('tax-type') || null,
                nonTaxCode: $(this).data('non-tax-code') || null
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

            /**
             * placeholder="0"은 화면 표시용이다.
             * 사용자가 아무것도 입력하지 않으면 실제 value는 '' 이다.
             *
             * 저장/계산 시에는 빈 값을 0으로 처리한다.
             */
            const amountValue =
                removeComma(
                    row.find('.payroll-amount-input').val() || '0'
                );

            result.push({
                itemSettingId: row.data('item-setting-id') || null,
                itemNameSnapshot: row.data('item-name'),
                itemType: row.data('item-type'),
                amount: amountValue
            });
        });

        return result;
    }

    /**
     * 최신 설정 목록에 기존 입력 금액을 반영한다.
     *
     * 유지 조건:
     * - 같은 itemSettingId
     * - 지급/공제 구분(itemType)이 그대로
     *
     * 초기화 조건:
     * - 신규 항목
     * - 지급 -> 공제 변경
     * - 공제 -> 지급 변경
     */
    function mergeLatestItemsWithCurrentAmount(latestItems, beforeItems) {

        if (!latestItems) {
            return [];
        }

        latestItems.forEach(function (item) {

            let matchedItem = null;

            beforeItems.forEach(function (beforeItem) {

                if (item.itemSettingId
                        && beforeItem.itemSettingId
                        && Number(item.itemSettingId) === Number(beforeItem.itemSettingId)
                        && item.itemType === beforeItem.itemType) {

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
    });

    // row 삭제
    $(document).on('click', '.setting-row-delete-btn', function () {
        $(this).closest('tr').remove();
    });

    // 초기화 버튼: 모달 열었을 때 상태로 되돌림
    $('#resetPayItemSettingBtn').on('click', function () {
        const originalRows = JSON.parse(originalPayItemSettingJson || '[]');
        renderPayItemSettingModalRows(originalRows);
    });

   // 설정 저장
   $('#savePayItemSettingBtn').on('click', function () {

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

       const currentJson = JSON.stringify(
           normalizePayItemSettingRows(items)
       );

       const originalJson = JSON.stringify(
           normalizePayItemSettingRows(
               JSON.parse(originalPayItemSettingJson || '[]')
           )
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

                // NEW는 최신 설정 바로 반영
               if (currentPayrollStatus === 'NEW') {

                   /**
                    * 최신 설정 저장 후에도
                    * 현재 입력 중인 금액을 최대한 유지한다.
                    */
                   const mergedItems =
                       mergeLatestItemsWithCurrentAmount(latestItems, beforeItems);

                   renderPayrollItems(mergedItems);

                   resetPreviewResult();

                   return;
               }

               // DRAFT도 설정 저장 직후 현재 화면에 바로 반영한다.
               // 단, 계산 결과는 무효화하고 계산 미리보기를 다시 요구한다.
               if (currentPayrollStatus === 'DRAFT') {

                   const mergedItems = mergeLatestItemsWithCurrentAmount(latestItems, beforeItems);

                   renderPayrollItems(mergedItems);

                   itemSettingDecisionRequired = false;
                   itemSettingDecisionCompleted = true;

                   $('#itemSettingWarningBox').addClass('d-none');

                   resetPreviewResult();
                   applyButtonState(currentPayrollStatus);

                   return;
               }
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

        return `
            <tr class="pay-item-setting-row"
                data-item-setting-id="${itemSettingId}">

                <!-- 지급 / 공제 -->
                <td>
                    <select class="form-select setting-item-type">
                        <option value="">선택</option>

                        <option value="ALLOWANCE"
                            ${itemType === 'ALLOWANCE' ? 'selected' : ''}>
                            지급
                        </option>

                        <option value="DEDUCTION"
                            ${itemType === 'DEDUCTION' ? 'selected' : ''}>
                            공제
                        </option>
                    </select>
                </td>

                <!-- 과세 / 비과세 -->
                <td>
                    <select class="form-select setting-tax-type">
                        <option value="">선택</option>

                        <option value="TAXABLE"
                            ${taxType === 'TAXABLE' ? 'selected' : ''}>
                            과세
                        </option>

                        <option value="NON_TAXABLE"
                            ${taxType === 'NON_TAXABLE' ? 'selected' : ''}>
                            비과세
                        </option>
                    </select>
                </td>

                <!-- 비과세 항목 -->
                <td>
                    <select class="form-select setting-non-tax-code">

                        <option value="">선택</option>

                        <option value="MEAL"
                            ${nonTaxCode === 'MEAL' ? 'selected' : ''}>
                            식대
                        </option>

                        <option value="CAR"
                            ${nonTaxCode === 'CAR' ? 'selected' : ''}>
                            차량유지비
                        </option>

                        <option value="RESEARCH"
                            ${nonTaxCode === 'RESEARCH' ? 'selected' : ''}>
                            연구활동비
                        </option>

                        <option value="CHILDCARE"
                            ${nonTaxCode === 'CHILDCARE' ? 'selected' : ''}>
                            보육수당
                        </option>

                        <option value="OVERSEAS"
                            ${nonTaxCode === 'OVERSEAS' ? 'selected' : ''}>
                            국외근로
                        </option>

                    </select>
                </td>

                <!-- 항목명 -->
                <td>
                    <input type="text"
                           class="form-control setting-item-name"
                           value="${itemName}"
                           placeholder="항목명 입력">
                </td>

                <!-- 삭제 -->
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

    const taxSelect = row.find('.setting-tax-type');
    const nonTaxSelect = row.find('.setting-non-tax-code');
    const nameInput = row.find('.setting-item-name');

    /**
     * 항목명은 항상 입력 가능
     */
    nameInput.prop('disabled', false);
    nameInput.prop('readonly', false);

    /**
     * 지급/공제 미선택
     */
    if (!itemType) {

        taxSelect.val('').prop('disabled', true);

        nonTaxSelect
            .val('')
            .prop('disabled', true);

        return;
    }

    /**
     * 공제
     *
     * - 과세/비과세 사용 안 함
     * - 비과세 항목 사용 안 함
     */
    if (itemType === 'DEDUCTION') {

        taxSelect
            .val('')
            .prop('disabled', true);

        nonTaxSelect
            .val('')
            .prop('disabled', true);

        return;
    }

    /**
     * 지급
     */
    taxSelect.prop('disabled', false);

    /**
     * 지급 + 비과세
     */
    if (taxType === 'NON_TAXABLE') {

        nonTaxSelect.prop('disabled', false);

        return;
    }

    /**
     * 지급 + 과세
     */
    nonTaxSelect
        .val('')
        .prop('disabled', true);
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

        // 과세/비과세 select를 "선택"으로 초기화
        row.find('.setting-tax-type').val('');

        // 비과세 항목 select도 "선택"으로 초기화
        row.find('.setting-non-tax-code').val('');

        // 현재 row의 지급/공제 상태에 맞게 disabled 처리 재적용
        refreshPayItemSettingRow(row);
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

        // 비과세 항목 select를 "선택"으로 초기화
        row.find('.setting-non-tax-code').val('');

        // 과세면 비과세 항목 disabled, 비과세면 enabled 처리
        refreshPayItemSettingRow(row);
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
            const itemName = row.find('.setting-item-name').val().trim();

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
            }

            items.push({
                itemSettingId: itemSettingId,
                itemName: itemName,
                itemType: itemType,
                taxType: sendTaxType,
                nonTaxCode: sendNonTaxCode,

                /**
                 * 근태연동은 후순위 구현으로 제외한다.
                 * 현재 설정 모달에서는 항상 null로 저장한다.
                 */
                linkedAttendanceType: null
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
                itemSettingId: item.itemSettingId ? Number(item.itemSettingId) : null,
                itemName: item.itemName || '',
                itemType: item.itemType || '',
                taxType: item.taxType || null,
                nonTaxCode: item.nonTaxCode || null
            };
        });
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

        previewCompleted = false;
        previewResult = null;

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

    function renderSavedInsurance(result) {

       if (!result
               || result.payrollStatus === 'NEW'
               || result.payrollStatus === 'DRAFT') {
           return;
       }

        const hasInsurance =
            result.nationalPensionAmount != null
            || result.healthInsuranceAmount != null
            || result.longTermCareAmount != null
            || result.employmentInsuranceAmount != null
            || result.totalInsurance != null;

        if (!hasInsurance) {
            return;
        }

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

        previewCompleted = true;

        $('#previewStateBadge')
            .removeClass('text-bg-secondary')
            .addClass('text-bg-success')
            .text('계산 완료');
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

        const empName = result.empName || $('#empName').val();
        const empNo = result.empNo || currentEmpNo;
        const deptName = result.deptName || $('#deptName').val();
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
                    <table class="table table-bordered align-middle">
                        <tbody>
                        <tr>
                            <th>기본급</th>
                            <td class="text-end">${numberFormat(result.baseSalary)}</td>
                        </tr>
                        <tr>
                            <th>과세 수당 합계</th>
                            <td class="text-end">${numberFormat(result.taxableAllowance)}</td>
                        </tr>
                        <tr>
                            <th>비과세 수당 합계</th>
                            <td class="text-end">${numberFormat(result.nonTaxableAllowance)}</td>
                        </tr>
                        <tr class="table-light">
                            <th class="text-primary">총 지급액</th>
                            <td class="text-end fw-bold text-primary">${numberFormat(result.totalGross)}</td>
                        </tr>
                        </tbody>
                    </table>
                </div>

                <div class="col-md-4">
                    <h6 class="fw-bold text-primary">2. 공제 총액</h6>
                    <table class="table table-bordered align-middle">
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
    }

    /**
     * 지급 상세
     *
     * 현재 화면에 입력된 지급항목을 기준으로 보여준다.
     */
    function renderPreviewAllowanceArea(result) {

        let html = `
            <table class="table table-bordered align-middle">
                <thead class="table-light">
                <tr>
                    <th>항목명</th>
                    <th>유형</th>
                    <th class="text-end">금액</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>기본급</td>
                    <td>과세</td>
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
            const amount = row.find('.payroll-amount-input').val() || '0';

            html += `
                <tr>
                    <td>${itemName}</td>
                    <td>${taxType}</td>
                    <td class="text-end">${amount}</td>
                </tr>
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
     */
    function renderPreviewDeductionArea(result) {

        let html = `
            <table class="table table-bordered align-middle">
                <thead class="table-light">
                <tr>
                    <th>항목명</th>
                    <th class="text-end">금액</th>
                </tr>
                </thead>
                <tbody>
        `;

        $('.payroll-item-row').each(function () {

            const row = $(this);

            if (row.data('item-type') !== 'DEDUCTION') {
                return;
            }

            const itemName = row.data('item-name');
            const amount = row.find('.payroll-amount-input').val() || '0';

            html += `
                <tr>
                    <td>${itemName}</td>
                    <td class="text-end">${amount}</td>
                </tr>
            `;
        });

        html += `
                <tr>
                    <td>소득세</td>
                    <td class="text-end">${numberFormat(result.incomeTax)}</td>
                </tr>
                <tr>
                    <td>지방소득세</td>
                    <td class="text-end">${numberFormat(result.localIncomeTax)}</td>
                </tr>
                </tbody>
            </table>
        `;

        $('#previewDeductionArea').html(html);
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
                        <td>${row.name}</td>
                        <td class="text-end">${numberFormat(row.baseAmount)}</td>
                        <td class="text-end">${rateFormat(row.rate)}</td>
                        <td>${row.formula || ''}</td>
                        <td class="text-end">${numberFormat(row.amount)}</td>
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
                            <th>구분</th>
                            <th class="text-end">기준금액</th>
                            <th class="text-end">요율</th>
                            <th>계산식</th>
                            <th class="text-end">금액</th>
                        </tr>
                        </thead>
                        <tbody>
                        ${rowsHtml}
                        <tr class="table-light">
                            <th colspan="4" class="text-primary">4대보험 합계</th>
                            <th class="text-end text-primary">${numberFormat(result.totalInsurance)}</th>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="form-text mt-2">
                계산 기준은 현재 설정된 보험요율을 기준으로 하며, 실제 처리 시 변경될 수 있습니다.
            </div>
        `;

        $('#previewInsuranceArea').html(html);
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

                previewResult = result;

                renderPreviewModal(result);

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

        if (!previewResult) {
            alert('반영할 계산 결과가 없습니다.');
            return;
        }

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

        $.ajax({
            url: '/admin/payroll/main/save',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function () {

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

            itemList.push({
                itemSettingId: row.data('item-setting-id') || null,
                itemNameSnapshot: row.data('item-name'),
                itemType: row.data('item-type'),
                amount: amount,
                taxType: row.data('tax-type') || null,
                nonTaxCode: row.data('non-tax-code') || null,
                linkedAttendanceType: null
            });
        });

        return {
            empNo: currentEmpNo,
            payYear: payYear,
            payMonth: payMonth,
            baseSalary: baseSalary,
            familyCount: 1,
            items: itemList,
            nationalPensionAmount: previewResult?.nationalPensionAmount || 0,
            healthInsuranceAmount: previewResult?.healthInsuranceAmount || 0,
            longTermCareAmount: previewResult?.longTermCareAmount || 0,
            employmentInsuranceAmount: previewResult?.employmentInsuranceAmount || 0,
            totalInsurance: previewResult?.totalInsurance || 0,

            incomeTax: previewResult?.incomeTax || 0,
            localIncomeTax: previewResult?.localIncomeTax || 0,
            totalDeduction: previewResult?.totalDeduction || 0,
            totalGross: previewResult?.totalGross || 0,
            netSalary: previewResult?.netSalary || 0
        };
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