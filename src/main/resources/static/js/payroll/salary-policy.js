/**
 * salary-policy.js
 * 기본급 관리 - 기본급 등록 모달 AJAX 처리
 *
 * 백단 연결 API
 * 1) GET /admin/payroll/salary-policy/register-check
 *    - 부서 + 직급 선택 시 급여등급 자동 조회
 *    - 기본급 정책 중복 여부 확인
 *
 * 2) GET /admin/payroll/salary-policy/check-grade-order
 *    - 기본급 입력 시 G1 < G2 < G3 < G4 < G5 서열 검증
 *
 * 최종 등록은 AJAX가 아니라 기존 form submit 사용
 * POST /admin/payroll/salary-policy/register
 */

// DOM이 완전히 로드된 후 실행되는 이벤트
// - HTML 요소들이 모두 생성된 이후에 JS가 실행되도록 보장
// 이유:
// - JS가 HTML보다 먼저 실행되면 getElementById 등이 null이 될 수 있음
// - 즉, 요소를 찾기 전에 DOM이 준비되어 있어야 함
// 특징:
// - 이미지, CSS 등은 로딩 안 끝나도 실행됨 (HTML 구조만 완성되면 실행)
// - window.onload보다 빠르게 실행됨 (성능 유리)
// 결론:
// - "HTML 요소를 안전하게 접근하기 위한 필수 이벤트"
document.addEventListener('DOMContentLoaded', function () {

    // 등록 모달 요소
    const deptSelect = document.getElementById('registerDeptId');
    const positionSelect = document.getElementById('registerPositionId');
    const gradeDisplay = document.getElementById('registerGradeDisplay');
    const gradeIdInput = document.getElementById('registerGradeId');
    const gradeDesc = document.getElementById('registerGradeDesc');
    const basicSalaryInput = document.getElementById('registerBasicSalary');
    const submitBtn = document.getElementById('registerSubmitBtn');

    // 메시지 영역
    const registerCheckMessage = document.getElementById('registerCheckMessage');
    const salaryOrderMessage = document.getElementById('salaryOrderMessage');

    // 해당 페이지가 아니면 JS 실행 중단
    if (!deptSelect || !positionSelect || !gradeDisplay || !basicSalaryInput || !submitBtn) {
        return;
    }

    // 등록 상태 초기화
    function resetRegisterState() {

        // 급여등급 초기화
        gradeDisplay.value = '';
        gradeIdInput.value = '';
        gradeDesc.textContent = '';

        // 기본급 초기화
        basicSalaryInput.value = '';
        basicSalaryInput.disabled = true;

        // 등록 버튼 비활성화
        submitBtn.disabled = true;

        // 중복 체크 메시지 초기화
        registerCheckMessage.className = 'alert d-none mb-3';
        registerCheckMessage.textContent = '';

        // 서열 검증 메시지 초기화
        salaryOrderMessage.className = 'form-text';
        salaryOrderMessage.textContent = '';
    }

    // 등록 모달 전체 초기화
    function resetRegisterModal() {
        deptSelect.value = '';
        positionSelect.value = '';
        resetRegisterState();
    }

    const registerModal = document.getElementById('salaryPolicyRegisterModal');

    if (registerModal) {
        registerModal.addEventListener('show.bs.modal', resetRegisterModal);
        registerModal.addEventListener('hidden.bs.modal', resetRegisterModal);
    }

    // 중복 체크 메시지 출력
    function showRegisterMessage(type, message) {
        registerCheckMessage.className = 'alert alert-' + type + ' mb-3';
        registerCheckMessage.textContent = message;
    }

    // 부서 + 직급 선택 시 급여등급 자동 조회 및 중복 확인
    function checkRegisterAvailable() {
        const deptId = deptSelect.value;
        const positionId = positionSelect.value;

        resetRegisterState();

        // 부서와 직급이 둘 다 선택되어야 AJAX 실행
        if (!deptId || !positionId) {
            return;
        }

        const url = `/admin/payroll/salary-policy/register-check?deptId=${encodeURIComponent(deptId)}&positionId=${encodeURIComponent(positionId)}`;

        fetch(url)
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('register-check failed');
                }
                return response.json();
            })
            .then(function (data) {

                // 등급 코드만 표시 (G1, G2 등)
                gradeDisplay.value = data.gradeId;

                // hidden 값 세팅 (서버 전송용)
                gradeIdInput.value = data.gradeId;

                // description 표시 (DB grade_code.description)
                gradeDesc.textContent = data.description;

                if (data.duplicate) {
                    showRegisterMessage('danger', data.message);
                    basicSalaryInput.disabled = true;
                    submitBtn.disabled = true;
                    return;
                }

                showRegisterMessage('success', data.message);
                basicSalaryInput.disabled = false;
            })
            .catch(function () {
                showRegisterMessage('danger', '기본급 등록 가능 여부를 확인하는 중 오류가 발생했습니다.');
                basicSalaryInput.disabled = true;
                submitBtn.disabled = true;
            });
    }

    // 기본급 입력 시 G1 < G2 < G3 < G4 < G5 서열 검증
    function checkGradeOrder() {
        const deptId = deptSelect.value;
        const positionId = positionSelect.value;
        const gradeId = gradeIdInput.value;
        const basicSalary = basicSalaryInput.value;

        // 검증 시작 시 등록 버튼은 잠시 비활성화
        submitBtn.disabled = true;

        // 메시지 영역은 비우지 않는다.
        // 입력 중마다 메시지를 지우면 모달 높이가 바뀌어서 화면이 떨릴 수 있다.
        salaryOrderMessage.className = 'form-text text-muted';

       if (!deptId || !positionId || !gradeId || !basicSalary) {
           salaryOrderMessage.className = 'form-text';
           salaryOrderMessage.textContent = '';
           return;
       }

        if (Number(basicSalary) <= 0) {
            salaryOrderMessage.className = 'form-text text-danger';
            salaryOrderMessage.textContent = '기본급은 0보다 커야 합니다.';
            return;
        }

        const url =
            `/admin/payroll/salary-policy/check-grade-order` +
            `?deptId=${encodeURIComponent(deptId)}` +
            `&positionId=${encodeURIComponent(positionId)}` +
            `&gradeId=${encodeURIComponent(gradeId)}` +
            `&basicSalary=${encodeURIComponent(basicSalary)}`;

        fetch(url)
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('check-grade-order failed');
                }
                return response.json();
            })
            .then(function (valid) {
                if (valid) {
                    salaryOrderMessage.className = 'form-text text-success';
                    salaryOrderMessage.textContent = '기본급 서열 조건을 만족합니다.';
                    submitBtn.disabled = false;
                    return;
                }

                salaryOrderMessage.className = 'form-text text-danger';
                salaryOrderMessage.textContent = '기본급은 G1 < G2 < G3 < G4 < G5 순서로 입력해야 합니다.';
                submitBtn.disabled = true;
            })
            .catch(function () {
                salaryOrderMessage.className = 'form-text text-danger';
                salaryOrderMessage.textContent = '기본급 서열 검증 중 오류가 발생했습니다.';
                submitBtn.disabled = true;
            });
    }

   // 기본급 입력 AJAX 떨림 방지용 타이머
   let salaryDebounceTimer;

   // 모달 입력값 변경 이벤트
   deptSelect.addEventListener('change', checkRegisterAvailable);
   positionSelect.addEventListener('change', checkRegisterAvailable);

   // 기본급 입력 이벤트
   // 입력할 때마다 바로 AJAX 호출하지 않고,
   // 사용자가 입력을 잠깐 멈춘 뒤 400ms 후 서열 검증 실행
   basicSalaryInput.addEventListener('input', function () {

       // 이전 예약 검증 취소
       clearTimeout(salaryDebounceTimer);

       // 400ms 동안 추가 입력이 없으면 검증 실행
       salaryDebounceTimer = setTimeout(function () {
           checkGradeOrder();
       }, 400);
   });

   // 기본급 입력칸에서 포커스가 빠질 때 최종 검증
   // debounce 대기 중이어도 마지막 값 기준으로 한 번 더 확인
   basicSalaryInput.addEventListener('blur', function () {
       clearTimeout(salaryDebounceTimer);
       checkGradeOrder();
   });

   // 수정 모달 요소
   const updateButtons = document.querySelectorAll('.update-btn');

   const updateModalElement = document.getElementById('salaryPolicyUpdateModal');
   const updateModal = updateModalElement ? new bootstrap.Modal(updateModalElement) : null;

   const updatePolicyId = document.getElementById('updatePolicyId');
   const updateDeptName = document.getElementById('updateDeptName');
   const updateDeptId = document.getElementById('updateDeptId');
   const updatePositionName = document.getElementById('updatePositionName');
   const updatePositionId = document.getElementById('updatePositionId');
   const updateGradeDisplay = document.getElementById('updateGradeDisplay');
   const updateGradeDesc = document.getElementById('updateGradeDesc');
   const updateGradeId = document.getElementById('updateGradeId');
   const updateBasicSalary = document.getElementById('updateBasicSalary');
   const updateSubmitBtn = document.getElementById('updateSubmitBtn');
   const originalBasicSalary = document.getElementById('originalBasicSalary');
   const updateCheckMessage = document.getElementById('updateCheckMessage');
   const updateSalaryOrderMessage = document.getElementById('updateSalaryOrderMessage');

   let updateDebounceTimer;

   // 수정 모달 상태 초기화
   function resetUpdateModal() {
       updatePolicyId.value = '';
       updateDeptName.value = '';
       updateDeptId.value = '';
       updatePositionName.value = '';
       updatePositionId.value = '';
       updateGradeDisplay.value = '';
       updateGradeDesc.textContent = '';
       updateGradeId.value = '';
       updateBasicSalary.value = '';
       originalBasicSalary.value = '';

       updateSubmitBtn.disabled = false;

       updateCheckMessage.className = 'alert d-none mb-3';
       updateCheckMessage.textContent = '';

       updateSalaryOrderMessage.className = 'form-text';
       updateSalaryOrderMessage.textContent = '';
   }

   // 수정 모달 메시지 출력
   function showUpdateMessage(type, message) {
       updateCheckMessage.className = 'alert alert-' + type + ' mb-3';
       updateCheckMessage.textContent = message;
   }

   // 수정 버튼 클릭 시 상세 조회 후 모달 표시
   updateButtons.forEach(function (button) {
       button.addEventListener('click', function () {
           const policyId = button.dataset.policyId;

           resetUpdateModal();

           fetch(`/admin/payroll/salary-policy/${policyId}`)
               .then(function (response) {
                   if (!response.ok) {
                       throw new Error('detail fetch failed');
                   }
                   return response.json();
               })
               .then(function (data) {
                   updatePolicyId.value = data.policyId;

                   updateDeptName.value = data.parentDeptName
                       ? data.deptName + " (" + data.parentDeptName + ")"
                       : data.deptName;
                   updateDeptId.value = data.deptId;

                   updatePositionName.value = data.positionName;
                   updatePositionId.value = data.positionId;

                   updateGradeDisplay.value = data.gradeId;
                   updateGradeId.value = data.gradeId;

                   // gradeDescription을 백단에서 내려주면 이 값 사용
                   // 없으면 일단 gradeName으로 대체 표시
                   updateGradeDesc.textContent = data.gradeDescription || data.gradeName || '';

                   updateBasicSalary.value = data.basicSalary;
                   originalBasicSalary.value = data.basicSalary;

                   // 처음 열 때는 기존 값이 유효한 상태이므로 수정 버튼 활성화
                   updateSubmitBtn.disabled = false;

                   updateModal.show();
               })
               .catch(function () {
                   showUpdateMessage('danger', '기본급 정책 정보를 불러오는 중 오류가 발생했습니다.');
               });
       });
   });

   // 수정 기본급 서열 검증
   function checkUpdateGradeOrder() {
       const policyId = updatePolicyId.value;
       const deptId = updateDeptId.value;
       const positionId = updatePositionId.value;
       const gradeId = updateGradeId.value;
       const basicSalary = updateBasicSalary.value;

       updateSubmitBtn.disabled = true;

       // 메시지 영역은 비우지 않는다.
       // 입력 중마다 메시지를 지우면 모달 높이가 바뀌어서 떨림이 생길 수 있다.
       updateSalaryOrderMessage.className = 'form-text text-muted';

       if (!policyId || !deptId || !positionId || !gradeId || !basicSalary) {
           updateSalaryOrderMessage.className = 'form-text';
           updateSalaryOrderMessage.textContent = '';
           return;
       }

       if (Number(basicSalary) <= 0) {
           updateSalaryOrderMessage.className = 'form-text text-danger';
           updateSalaryOrderMessage.textContent = '기본급은 0보다 커야 합니다.';
           updateSubmitBtn.disabled = true;
           return;
       }

       const url =
           `/admin/payroll/salary-policy/check-grade-order-update` +
           `?policyId=${encodeURIComponent(policyId)}` +
           `&deptId=${encodeURIComponent(deptId)}` +
           `&positionId=${encodeURIComponent(positionId)}` +
           `&gradeId=${encodeURIComponent(gradeId)}` +
           `&basicSalary=${encodeURIComponent(basicSalary)}`;

       fetch(url)
           .then(function (response) {
               if (!response.ok) {
                   throw new Error('update grade order check failed');
               }
               return response.json();
           })
           .then(function (valid) {
               if (valid) {
                   updateSalaryOrderMessage.className = 'form-text text-success';
                   updateSalaryOrderMessage.textContent = '기본급 서열 조건을 만족합니다.';
                   updateSubmitBtn.disabled = false;
                   return;
               }

               updateSalaryOrderMessage.className = 'form-text text-danger';
               updateSalaryOrderMessage.textContent = '기본급은 G1 < G2 < G3 < G4 < G5 순서로 입력해야 합니다.';
               updateSubmitBtn.disabled = true;
           })
           .catch(function () {
               updateSalaryOrderMessage.className = 'form-text text-danger';
               updateSalaryOrderMessage.textContent = '기본급 서열 검증 중 오류가 발생했습니다.';
               updateSubmitBtn.disabled = true;
           });
   }

   // 수정 기본급 입력 시 debounce 적용
   if (updateBasicSalary) {
       updateBasicSalary.addEventListener('input', function () {
           clearTimeout(updateDebounceTimer);

           updateDebounceTimer = setTimeout(function () {
               checkUpdateGradeOrder();
           }, 400);
       });

       updateBasicSalary.addEventListener('blur', function () {
           clearTimeout(updateDebounceTimer);
           checkUpdateGradeOrder();
       });
   }

   // 취소/X로 닫으면 수정 시도 값이 남지 않도록 초기화
   if (updateModalElement) {
       updateModalElement.addEventListener('hidden.bs.modal', resetUpdateModal);
   }

   const updateForm = document.getElementById('salaryPolicyUpdateForm');

   if (updateForm) {
       updateForm.addEventListener('submit', function (e) {

           const originalSalary = originalBasicSalary.value;
           const currentSalary = updateBasicSalary.value;

           if (String(originalSalary) === String(currentSalary)) {
               e.preventDefault();

               showUpdateMessage('warning', '변경된 내용이 없습니다.');
               return false;
           }
       });
   }
});