/**
 * =========================================================
 * payroll-statement.js
 * 위치:
 * src/main/resources/static/js/payroll/payroll-statement.js
 * =========================================================
 *
 * [급여명세서 JS]
 *
 * - 조회년도 / 조회월 select 구성
 * - 입사월 이전 조회 방지
 * - 현재월 이후 조회 방지
 * - 조회 버튼 상태 제어
 * - 급여요약 목록 복귀 파라미터 유지
 *
 * ---------------------------------------------------------
 * [중요]
 * - 급여요약 전체조회와 유사한 조회년월 UX를 사용한다.
 * - 유효하지 않은 월은 비우지 않고 가까운 유효월로 보정한다.
 * - 명세서 안에서 조회년월을 바꿔도 목록 복귀용 return 파라미터는 유지한다.
 * =========================================================
 */

$(document).ready(function () {

    const yearSelect = $('#statementPayYear');
    const monthSelect = $('#statementPayMonth');
    const searchBtn = $('#statementSearchBtn');

    // 급여명세서 페이지가 아니면 실행하지 않는다.
    if (yearSelect.length === 0 || monthSelect.length === 0) {
        return;
    }

    // 현재 명세서 대상 사번
    const empNo = yearSelect.data('emp-no');

    // 현재 화면에 실제 조회 완료된 년월
    const loadedYear = Number(yearSelect.data('current-year'));
    const loadedMonth = Number(yearSelect.data('current-month'));

    // 작성년월 계산 기준값
    let currentHireDate = null;
    let currentDefaultYear = null;
    let currentDefaultMonth = null;

    // 마지막 조회 완료 년월
    let lastLoadedPayYear = loadedYear;
    let lastLoadedPayMonth = loadedMonth;

    /**
     * =====================================================
     * 조회년월 옵션 조회
     * =====================================================
     *
     * 백단:
     * GET /admin/payroll/summary/statement/period-options
     *
     * 역할:
     * - 입사일 기준 조회 시작월 확인
     * - 현재년월 기준 미래월 차단
     * - 최근 5년 범위 연도 목록 구성
     */
    $.ajax({
        url: '/admin/payroll/summary/statement/period-options',
        type: 'GET',
        data: {
            empNo: empNo
        },
        success: function (result) {

            currentHireDate = result.hireDate ? new Date(result.hireDate) : null;
            currentDefaultYear = result.defaultYear;
            currentDefaultMonth = result.defaultMonth;

            renderYearOptions(result.availableYears || []);

            // 현재 조회 중인 년월로 select 초기값 세팅
            yearSelect.val(loadedYear);
            renderMonthOptionsByYear(loadedYear, loadedMonth);

            updateSearchButtonState();
        },
        error: function (xhr) {
            alert(xhr.responseText || '조회년월 옵션을 불러오지 못했습니다.');
        }
    });

    /**
     * =====================================================
     * 조회년도 변경
     * =====================================================
     *
     * 규칙:
     * - 기존 월이 새 연도에서 유효하면 유지
     * - 입사월보다 빠르면 입사월로 보정
     * - 현재월보다 늦으면 현재월로 보정
     */
    yearSelect.on('change', function () {

        const selectedYear = Number($(this).val());
        const previousMonth = Number(monthSelect.val());

        renderMonthOptionsByYear(selectedYear, previousMonth);
        updateSearchButtonState();
    });

    /**
     * =====================================================
     * 조회월 변경
     * =====================================================
     */
    monthSelect.on('change', function () {
        updateSearchButtonState();
    });

    /**
     * =====================================================
     * 조회 버튼
     * =====================================================
     *
     * 현재 명세서 조회 기준 payYear/payMonth만 변경한다.
     * returnPayYear, returnPayMonth, returnPage 등 목록 복귀용 값은 그대로 둔다.
     */
    searchBtn.on('click', function () {

        const payYear = Number(yearSelect.val());
        const payMonth = Number(monthSelect.val());

        if (!payYear || !payMonth) {
            alert('조회년도와 조회월을 선택해 주세요.');
            return;
        }

        const url = new URL(window.location.href);

        url.searchParams.set('empNo', empNo);
        url.searchParams.set('payYear', payYear);
        url.searchParams.set('payMonth', payMonth);

        window.location.href = url.toString();
    });

    /**
     * =====================================================
     * 급여명세서 출력
     * =====================================================
     *
     * 출력 가능 상태
     * - CONFIRMED (확정)
     * - PAID (지급완료)
     *
     * 출력 불가 상태
     * - NEW
     * - DRAFT (계산 반영 포함)
     */
    $('#statementPrintBtn').on('click', function () {

        const payrollStatus =
            $(this).data('payroll-status');

        if (payrollStatus !== 'CONFIRMED'
            && payrollStatus !== 'PAID') {

            alert('급여명세서 출력은 확정 또는 지급완료 상태에서만 가능합니다.');
            return;
        }

        const statementPeriod =
          $('.pay-statement-document .fs-5')
              .first()
              .text()
              .trim();

        const statementDocument =
           $('.pay-statement-document').html();

        if (!statementDocument) {
           alert('출력할 급여명세서가 없습니다.');
           return;
        }

       /**
        * 인쇄 창 생성
        */
       const printWindow =
           window.open('', '_blank',
               'width=900,height=1000');

       printWindow.document.write(`
       <!doctype html>
       <html>
       <head>
           <title></title>
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

           <!-- ===================================================== -->
           <!-- 제목 -->
           <!-- ===================================================== -->
           <div style="
               text-align:center;
               margin-bottom:10px;
           ">

               <!-- 제목 -->
               <h2 style="
                   margin:0;
                   font-size:24px;
                   letter-spacing:6px;
               ">
                   급 여 명 세 서
               </h2>

               <!-- 지급년월 -->
               <div style="
                   margin-top:5px;
                   font-size:13px;
               ">
                   ${statementPeriod}
               </div>

           </div>

           <!-- ===================================================== -->
           <!-- 실제 급여명세서 -->
           <!-- - 기존 HTML 그대로 사용
                - 지급/공제 테이블만 좌우 고정 -->
           <!-- ===================================================== -->
           <div id="printDocument">
               ${statementDocument}
           </div>

       </div>

       <script>

            /**
             * =====================================================
             * 기존 화면용 제목 제거
             * =====================================================
             *
             * 이유:
             * - 출력창 상단에 새 제목 생성
             * - 기존 화면 제목 중복 제거
             */
            const originTitleArea =
                document.querySelector(
                    '#printDocument .text-center.mb-4'
                );

            if (originTitleArea) {
                originTitleArea.remove();
            }

            /**
             * 계산방법 제목 제거
             */
            document.querySelectorAll('th')
                .forEach(function(th) {

                const text =
                    th.textContent.trim();

                if (text === '계산방법') {
                    th.remove();
                }
            });

           /**
            * =====================================================
            * 지급/공제 영역 좌우 배치 강제
            * =====================================================
            *
            * 목적:
            * - Bootstrap row 깨짐 방지
            * - 지급내역 좌측
            * - 공제내역 우측
            */
           const row =
               document.querySelector('#printDocument .row.g-3.mb-3');

           if (row) {

               row.style.display = 'table';
               row.style.width = '100%';
               row.style.boxSizing =  'border-box';
               row.style.tableLayout = 'fixed';

               const columns =
                   row.querySelectorAll('.col-md-6');

               columns.forEach(function(col) {

                   col.style.display = 'table-cell';
                   col.style.width = '50%';
                   col.style.verticalAlign = 'top';
                   col.style.padding = '0';
               });
           }

           /**
            * =====================================================
            * 한 장 출력 최적화
            * =====================================================
            */
           document.querySelectorAll('table')
               .forEach(function(table) {

               table.style.width = '100%';
               table.style.borderCollapse = 'collapse';
               table.style.fontSize = '10px';
               table.style.marginBottom = '6px';
           });

           /**
            * =====================================================
            * 인사정보 테이블 보정
            * =====================================================
            *
            * 지급/공제와 높이 자연스럽게 맞춤
            * (강제 height 제거)
            */
           const infoTable =
               document.querySelector(
                   '#printDocument table'
               );

           if (infoTable) {

               infoTable.style.marginBottom =
                   '6px';

               infoTable.style.tableLayout =
                   'fixed';
           }



           document.querySelectorAll('th, td')
               .forEach(function(cell) {

               cell.style.border =
                   '1px solid #dee2e6';

               cell.style.padding =
                   '4px 5px';
           });

       /**
        * =====================================================
        * 표 정렬 정리
        * =====================================================
        *
        * 규칙
        * - 항목 / 비고 / 구분 / 계산설명 / 계산식: 가운데
        * - 금액: 오른쪽
        */
      document.querySelectorAll('table tr')
          .forEach(function(row) {

          const cells =
              row.querySelectorAll('th, td');

          cells.forEach(function(cell, index) {

              /**
               * 기본 가운데 정렬
               */
              cell.style.textAlign =
                  'center';

              /**
               * 지급/공제 표
               * 항목 / 금액 / 비고
               */
              if (cells.length === 3) {

                  // 실제 금액만 오른쪽
                  if (
                      cell.tagName === 'TD'
                      && index === 1
                  ) {

                      cell.style.textAlign =
                          'right';

                      cell.style.paddingRight =
                          '10px';
                  }
              }

              /**
               * 계산방법 표
               * 구분 / 계산설명 / 계산식 / 금액 / 비고
               */
              if (cells.length === 5) {

                  // 실제 금액만 오른쪽
                  if (
                      cell.tagName === 'TD'
                      && index === 3
                  ) {

                      cell.style.textAlign =
                          'right';

                      cell.style.paddingRight =
                          '10px';
                  }
              }
          });
      });

      /**
       * =====================================================
       * 실수령액 정렬
       * =====================================================
       */
      document.querySelectorAll('th, td')
          .forEach(function(cell) {

          const text =
              cell.textContent.trim();

          // 실수령액 글자
          if (text === '실 수 령 액') {

              cell.style.fontSize =
                  '18px';

              cell.style.fontWeight =
                  '700';

              cell.style.letterSpacing =
                  '4px';

              cell.style.textAlign =
                  'center';
          }

          // 실수령액 숫자
          if (
              cell.classList.contains('fs-3')
          ) {

              cell.style.fontSize =
                  '22px';

              cell.style.fontWeight =
                  '700';

              cell.style.textAlign =
                  'right';

              cell.style.paddingRight =
                  '14px';
          }
      });

      /**
       * =====================================================
       * 하단 안내문 한 줄 처리
       * =====================================================
       */
      const notice =
          document.querySelector(
              '#printDocument p.text-muted'
          );

      if (notice) {

          notice.style.fontSize =
              '10px';

          notice.style.whiteSpace =
              'nowrap';

          notice.style.marginTop =
              '6px';
      }

      /**
       * 인쇄 실행
       */
      window.onload = function () {

          window.focus();
          window.print();
      };

      /**
       * 인쇄 종료 시 닫기
       */
      window.onafterprint = function () {

          window.close();
      };

       </script>

       </body>
       </html>
       `);

        printWindow.document.close();
    });

    /**
     * 조회년도 option 생성
     */
    function renderYearOptions(years) {

        yearSelect.empty();

        if (!years || years.length === 0) {
            return;
        }

        years.forEach(function (year) {
            yearSelect.append(`<option value="${year}">${year}년</option>`);
        });
    }

    /**
     * 조회년도 기준 조회월 option 생성
     *
     * 규칙:
     * 1. 입사년도면 입사월부터 조회 가능
     * 2. 현재년도면 현재월까지만 조회 가능
     * 3. 과거 중간 연도면 1월~12월 조회 가능
     * 4. 기존 월이 가능 범위 안이면 유지
     * 5. 기존 월이 너무 이르면 시작월로 보정
     * 6. 기존 월이 너무 늦으면 마지막 월로 보정
     */
    function renderMonthOptionsByYear(selectedYear, preferredMonth) {

        monthSelect.empty();

        if (!selectedYear) {
            return;
        }

        selectedYear = Number(selectedYear);

        let startMonth = 1;
        let endMonth = 12;

        // 입사 연도이면 입사월부터 조회 가능
        if (currentHireDate && selectedYear === currentHireDate.getFullYear()) {
            startMonth = currentHireDate.getMonth() + 1;
        }

        // 현재 연도이면 현재월까지만 조회 가능
        if (selectedYear === currentDefaultYear) {
            endMonth = currentDefaultMonth;
        }

        for (let month = startMonth; month <= endMonth; month++) {
            monthSelect.append(`<option value="${month}">${month}월</option>`);
        }

        let selectedMonth = Number(preferredMonth);

        // 기존 월이 없으면 시작월로 세팅
        if (!selectedMonth) {
            selectedMonth = startMonth;
        }

        // 입사월보다 빠르면 입사월로 보정
        if (selectedMonth < startMonth) {
            selectedMonth = startMonth;
        }

        // 현재월 또는 해당 연도 마지막 가능월보다 늦으면 마지막 가능월로 보정
        if (selectedMonth > endMonth) {
            selectedMonth = endMonth;
        }

        monthSelect.val(selectedMonth);
    }

    /**
     * 조회 버튼 상태 제어
     *
     * - 현재 화면에 이미 조회된 년월이면 disabled
     * - 다른 년월을 선택하면 enabled
     */
    function updateSearchButtonState() {

        const selectedYear = Number(yearSelect.val());
        const selectedMonth = Number(monthSelect.val());

        if (!selectedYear || !selectedMonth) {
            searchBtn.prop('disabled', true);
            return;
        }

        const sameAsLoaded =
            selectedYear === lastLoadedPayYear
            && selectedMonth === lastLoadedPayMonth;

        searchBtn.prop('disabled', sameAsLoaded);
    }
});