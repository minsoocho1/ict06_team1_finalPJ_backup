/**
 * @Description : 전자결재선 서식 생성&수정 시 공통 변수 & 메서드 관리
 * @Author : 송영은
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.08    송영은       최초 생성
*/

// [상태 정보]---------------------------------------------------------
// 참조/결재 대상 조회를 위한 상태 정보 저장
const state = {
    searchMode: 'ref',      // 조회 모드: ref(참조) | app(결재)
    targetType: 'user',     // 대상 유형: user(개인) | dept(부서) | position(직급)
    approvalStep: 0,        // 전체 결재 단계 수
    currentEditingStep: null // 현재 수정 중인 결재 단계
};

// 선택 상태 저장 구조
const selectedState = {
    ref: new Map(),     // 참조 대상
    approval: {}        // 단계별 결재 대상
};

// 결재 단계별 최소 직급 정보
const approvalRules = {
    minPositionByStep: {}
};

// offcanvas 열기 함수 (모드 + 단계 반영)
const panel = new bootstrap.Offcanvas(document.getElementById('targetSearchPanel'));

// [1) 패널/화면 제어]---------------------------------------------------
// 패널 제목 업데이트 함수
function openSearchPanel(mode) {
    state.searchMode = mode;

    if (mode === 'ref') {
        $('#panelTitle').text('참조 대상 선택');
    } else {
        $('#panelTitle').text(`${state.currentEditingStep}단계 결재 대상 선택`);
    }
    renderTargetUI();

    panel.show();
}

// renderTargetUI()
// 유형별 UI 렌더링: targetList 내용 다르게 렌더링
function renderTargetUI() {
    let html = '';

    // 검색버튼 구현 html
    const searchButtonHtml = `
        <div class="col-md-1">
            <button id="searchBtn" class="btn btn-outline-success w-100 h-100 d-flex justify-content-center align-items-center" type="button">
                <i class="fa-solid fa-magnifying-glass"></i>
            </button>
        </div>
    `;

    // 검색 유형 별 렌더링
    if (state.targetType === 'user') {
        html = '<div class="d-flex justify-content-between">';
        html += renderSearchWindow('사원 이름, 사번'); 
        html += searchButtonHtml; 
        html += `
            </div>
                <div class="mt-3">
                    <p>👤 사원 목록</p>
                    <div id="employeeList" class="list-group"></div>
                    <div id="pagination" class="mt-2"></div> 
                </div>`;
    }

    if (state.targetType === 'dept') {
        html = '<div class="d-flex justify-content-between">';
        html += `
            </div>
                <div class="mt-3">
                    <p>🏢 부서 목록</p>
                    <div id="deptList" class="list-group overflow-auto" style="max-height: 520px;"></div>
                </div>`;
    }

    if (state.targetType === 'position') {
        html = `
            <div class="mt-3">
                <p>📌 직급 목록</p>
                <div id="positionList" class="list-group"></div>
            </div>`;
    }

    $('#targetList').html(html);

    // ⭐ 렌더링 후 데이터 호출
    if (state.targetType === 'user') loadEmployees('');
    if (state.targetType === 'dept') loadDepartments();
    if (state.targetType === 'position') loadPositions();
}

// renderSearchWindow(placeholder)
// 검색창의 plcaeholder 내용 업데이트 
function renderSearchWindow(placeholder){
    let html = `
        <div class="col-md-11">
            <input class="form-control w-100" type="search" placeholder="${placeholder}" aria-label="Search"/>
        </div>
    `
    return html;
}
// [2) 결재 단계 관리]---------------------------------------------------------
// addApprovalStepUI(step)
// 결재 단계 UI 생성 (왼쪽) : 수정, 삭제 기능
function addApprovalStepUI(step) {
    const html = `
        <div class="card p-2 mb-2 approval-step-card"
            id="approval_${step}"
            data-step="${step}"
            onclick="editApprovalStep(${step})">

            <div class="d-flex justify-content-between">

                <span class="step-title">${step}단계</span>

                <button type="button"
                    class="btn btn-sm btn-danger top-0 end-0"
                    onclick="event.stopPropagation(); removeStep(${step})">
                    -
                </button>
            </div>

            <div class="approval-targets"></div>
        </div>
    `;

    $('#approvalList').append(html);
}

// editApprovalStep(step)
// 결재 단계 수정
function editApprovalStep(step) {
    state.currentEditingStep = step;

    openSearchPanel('app');
}

// removeStep(step)
// 결재 단계 삭제 버튼 클릭 시
function removeStep(step) {

    // 선택 결재 단계 삭제
    delete selectedState.approval[step];

    $(`#approval_${step}`).remove();

    reorderApprovalSteps(); // 결재 단계 재정렬
}

// reorderApprovalSteps()
// 결재 단계 삭제 후 나머지 결재 단계 순서 재정렬
function reorderApprovalSteps() {

    const newApproval = {};
    const newRules = {};

    $('.approval-step-card').each(function(index) {

        const newStep = index + 1;

        const oldStep = $(this).data('step');

        // 카드 id 수정
        $(this).attr('id', `approval_${newStep}`);

        // data-step 수정
        $(this).attr('data-step', newStep);

        // 제목 수정
        $(this).find('.step-title')
            .text(`${newStep}단계`);

        // onclick 수정
        $(this).attr(
            'onclick',
            `editApprovalStep(${newStep})`
        );

        // 데이터 이동
        newApproval[newStep] =
            selectedState.approval[oldStep] || new Map();

        // approvalRules도 이동
        newRules[newStep] =
            approvalRules.minPositionByStep[oldStep] || 0;
    });

    selectedState.approval = newApproval;

    approvalRules.minPositionByStep = newRules;

    state.approvalStep =
        Object.keys(newApproval).length;
}

// [3) 사원, 부서, 직급 정보 조회 API]--------------------------------------
// 사원 조회 (페이징)
function loadEmployees(keyword, page = 0) {

    $.get('/admin/approval/targets/employees', {
        keyword: keyword,
        minPositionId:
            state.searchMode === 'app'
                ? getPreviousStepMinPosition()
                : null, // 이전 결재 단계보다 높은 직급의 사원만 조회
        page: page,
        size: 10
    }, function (res) {

        renderEmployeeList(res.content);
        renderPagination(res);
    });
}

// 부서 조회
function loadDepartments() {

    $.get('/api/organization/departments/tree',
        function (data) {
            renderDeptTree(data);
        })
        .fail(function () {
            $('#deptList').html('<div class="text-danger">부서 정보를 불러오지 못했습니다.</div>');
        });
}

// 직급 조회
function loadPositions() {

    $.get('/admin/approval/targets/positions',
        function (data) {
            renderPositionList(data);
        });
}

// [4) 목록 렌더링]--------------------------------------------
// renderEmployeeList(data)
// 사원 목록 렌더링
function renderEmployeeList(data) {

    let html = '';

    if (!data || data.length === 0) {
        html = `<div class="text-muted">조회 결과 없음</div>`;
    } else {

        data.forEach(emp => {
            // 이전 결재 단계보다 높은 직급의 사원만 결재 가능하게 설정
            const minPositionId = getPreviousStepMinPosition(); // 각 결재 단계별 결재 가능 최소 직급 구하기
            
            // 렌더링할 값 가져오기
            const id = String(emp.empNo);

            const type = 'USER';

            const key = `${type}_${id}`;

            let isChecked = false;

            if (state.searchMode === 'ref') {
                isChecked = selectedState.ref.has(key);
            } else {
                const stepMap = selectedState.approval[state.currentEditingStep] || new Map();
                isChecked = stepMap.has(key);
            }

            html += `
                <label class="list-group-item d-flex align-items-center">
                    <input type="checkbox"
                        class="form-check-input me-2 target-checkbox"
                        value="${id}"

                        data-name="${emp.name}"
                        data-dept="${emp.deptName}"
                        data-position="${emp.positionName}"

                        data-position-id="${emp.positionId}"

                        data-type="${type}"

                        ${isChecked ? 'checked' : ''}>

                    ${emp.deptName} / ${emp.positionName}
                    / <strong>${emp.name}</strong> (${emp.empNo})
                </label>
            `;

            console.log(
                emp.name,
                emp.positionName,
                emp.positionId
            );
        });
    }

    $('#employeeList').html(html);
}

// renderDeptTree(data)
function renderDeptTree(data) {

    let html = '';

    if (!data || data.length === 0) {
        html = `<div class="text-muted">조회 결과 없음</div>`;
    } else {
        html = renderDeptTreeNodes(data, 0);
    }

    $('#deptList').html(html);
}

function renderDeptTreeNodes(departments, depth) {

    let html = '';

    departments.forEach(dept => {

        const id = String(dept.deptId);
        const name = dept.deptName;
        const type = 'DEPT';
        const children = dept.children || [];
        const hasChildren = children.length > 0;
        const isRoot = depth === 0;
        const key = `${type}_${id}`;

        let isChecked = false;

        if (state.searchMode === 'ref') {
            isChecked = selectedState.ref.has(key);
        } else {
            const stepMap = selectedState.approval[state.currentEditingStep] || new Map();
            isChecked = stepMap.has(key);
        }

        const paddingLeft = 16 + depth * 28;
        const rowClass = isRoot
            ? 'list-group-item d-flex align-items-center bg-light fw-semibold'
            : 'list-group-item d-flex align-items-center';
        const badgeClass = isRoot
            ? 'badge bg-secondary ms-auto'
            : 'badge bg-light text-dark ms-auto';
        const badgeText = isRoot ? '본부' : '팀';
        const treeMarker = hasChildren ? '-' : '';

        html += `
            <label class="${rowClass}" style="padding-left: ${paddingLeft}px;">
                <span class="text-muted me-2" style="width: 14px;">${treeMarker}</span>
                <input type="checkbox"
                    class="form-check-input me-2 target-checkbox"
                    value="${id}"
                    data-name="${escapeHtml(name)}"
                    data-type="${type}"
                    ${isChecked ? 'checked' : ''}>
                <span>${escapeHtml(name)}</span>
                <span class="${badgeClass}">${badgeText}</span>
            </label>
        `;

        if (hasChildren) {
            html += renderDeptTreeNodes(children, depth + 1);
        }
    });

    return html;
}

function escapeHtml(value) {
    return String(value || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

// renderPositionList(data)
// 직급 목록 렌더링
function renderPositionList(data) {

    let html = '';

    if (!data || data.length === 0) {
        html = `<div class="text-muted">조회 결과 없음</div>`;
    } else {

        data.forEach(pos => {

            const minPositionId = getPreviousStepMinPosition(); // 각 결재 단계의 결재 가능 최소 직급 구하기 

            if (state.searchMode === 'ref' || pos.id > minPositionId) { // 가장 낮은 직급은 결재자가 될 수 없음. 

                const id = String(pos.id);

                const type = 'POSITION';

                const key = `${type}_${id}`;

                let isChecked = false;

                if (state.searchMode === 'ref') {
                    isChecked = selectedState.ref.has(key);
                } else {
                    const stepMap = selectedState.approval[state.currentEditingStep] || new Map();
                    isChecked = stepMap.has(key);
                }

                html += `
                    <label class="list-group-item">
                        <input type="checkbox"
                            class="form-check-input me-2 target-checkbox"
                            value="${id}"
                            data-name="${pos.name}"
                            data-position-id="${pos.id}"
                            data-type="${type}"
                            ${isChecked ? 'checked' : ''}>
                        ${pos.name}
                    </label>
                `;
                console.log(pos.name, pos.id);  
            }
        });
    }

    $('#positionList').html(html);
}

// renderPagination(res)
// 사원 목록 페이징 처리
function renderPagination(res) {

    let html = '';

    for (let i = 0; i < res.totalPages; i++) {
        html += `
            <button class="btn btn-sm ${i === res.number ? 'btn-primary' : 'btn-outline-primary'}"
                onclick="loadEmployees($('#targetList input[type=search]').val(), ${i})">
                ${i + 1}
            </button>
        `;
    }

    $('#pagination').html(html);
}

// [5) 선택 상태 처리] ---------------------------
// getPreviousStepMinPosition()
// 결재 단계에 따라 선택 가능 직급 제한
function getPreviousStepMinPosition() {

    // 참조는 제한 없음
    if (state.searchMode === 'ref') {
        return 0;
    }

    // 1단계는 사원 제외만 적용
    if (state.currentEditingStep <= 1) {
        return 1;
    }

    return approvalRules.minPositionByStep[
        state.currentEditingStep - 1
    ] || 1;
}

// recalculateStepMinPosition(step)
// 결재 가능 최소 직급 재계산
function recalculateStepMinPosition(step) {

    const stepMap =
        selectedState.approval[step];

    if (!stepMap || stepMap.size === 0) {

        approvalRules.minPositionByStep[step] = 0;
        return;
    }

    let maxPosition = 0;

    stepMap.forEach(item => {

        if (
            (item.type === 'USER' || item.type === 'POSITION')
            && item.positionId
        ) {
            maxPosition = Math.max(
                maxPosition,
                item.positionId
            );
        }
    });

    approvalRules.minPositionByStep[step] =
        maxPosition;
}

// removeSelected(mode, itemType, id, step)
// 삭제 기능
function removeSelected(mode, itemType, id, step) {

    const key = `${itemType}_${id}`;

    if (mode === 'ref') {

        selectedState.ref.delete(key);

        $('#refList').empty();

        selectedState.ref.forEach(item => {
            $('#refList').append(renderBadge(item, 'ref'));
        });

    } else {

        selectedState.approval[step]?.delete(key);

        recalculateStepMinPosition(step); // 최소 결재 직급 재계산

        const stepDiv = $(`#approval_${step} .approval-targets`);

        stepDiv.empty();

        selectedState.approval[step]?.forEach(item => {
            stepDiv.append(renderBadge(item, 'app', step));
        });
    }
}

// [6) badge 관련] -------------------------------------------------
// renderBadge(item, type, step)
// badge 렌더링 + 삭제 버튼
function renderBadge(item, type, step = null) {
    let label = item.name;
    
    // 대상 유형= 사용자 -> 부서, 직급, 이름, 사번 모두 출력
    if (item.type === 'USER') {
        label =
            `${item.dept} / ${item.position}
            / ${item.name} (${item.id})`;
    }

    return `
        <span class="badge bg-${type === 'ref' ? 'secondary' : 'primary'} me-1">
            ${label}
            <button type="button" class="btn-close btn-close-white ms-1"
                onclick="removeSelected('${type}', '${item.type}', '${item.id}', '${step}')"></button>
        </span>
    `;
}

// [7) payload 생성] ---------------------------
function buildPayload() {

    return {
        templateName: $('#templateName').val(),
        description: $('#appLineDesc').val(),
        isDefault: $('#checkDefault').is(':checked'),

        refTargets:
            [...selectedState.ref.values()],

        approvalSteps:
            Object.entries(selectedState.approval)
                .filter(([_, map]) => map && map.size > 0)
                .map(([step, map]) => ({
                    step: Number(step),
                    targets: [...map.values()]
                }))
    };
}

// [8) 상태 preload 함수 (수정 기능 핵심)]---------------------------
// TODO: applyDetailData(detail)
// 수정 화면에서 기존 결재선 데이터를 selectedState 에 다시 넣는 역할
//function applyDetailData(detail){
//
//    // 기본 정보
//    $('#templateName').val(detail.templateName);
//
////    $('#appLineDesc').val(detail.description);
//
//    $('#checkDefault').prop(
//        'checked',
//        detail.isDefault
//    );
//
//    // 상태 초기화
//    selectedState.ref.clear();
//
//    selectedState.approval = {};
//
//    approvalRules.minPositionByStep = {};
//
//    $('#refList').empty();
//
//    $('#approvalList').empty();
//
//    state.approvalStep = 0;
//
//    // step 순회
//    detail.steps.forEach(stepData => {
//
//        // stepOrder 사용
//        const step = stepData.stepOrder;
//
//        // 참조
//        if(step === 0){
//
//            stepData.targets.forEach(target => {
//
//                // selectedState 구조로 변환
//                const item = {
//
//                    id:
//                        target.empNo || target.targetName,
//
//                    name:
//                        target.targetName,
//
//                    dept:
//                        target.departmentName,
//
//                    position:
//                        target.positionName,
//
//                    positionId: 0,
//
//                    type:
//                        target.type
//                };
//
//                const key =
//                    `${item.type}_${item.id}`;
//
//                selectedState.ref.set(key, item);
//
//                $('#refList').append(
//                    renderBadge(item, 'ref')
//                );
//            });
//        }
//
//        // 결재 단계
//        else {
//
//            state.approvalStep =
//                Math.max(state.approvalStep, step);
//
//            addApprovalStepUI(step);
//
//            if(!selectedState.approval[step]){
//
//                selectedState.approval[step] =
//                    new Map();
//            }
//
//            stepData.targets.forEach(target => {
//
//                // selectedState 구조로 변환
//                const item = {
//
//                    id:
//                        target.empNo || target.targetName,
//
//                    name:
//                        target.targetName,
//
//                    dept:
//                        target.departmentName,
//
//                    position:
//                        target.positionName,
//
//                    positionId: 0,
//
//                    type:
//                        target.type
//                };
//
//                const key =
//                    `${item.type}_${item.id}`;
//
//                selectedState.approval[step]
//                    .set(key, item);
//            });
//
//            // badge 렌더링
//            const stepDiv =
//                $(`#approval_${step} .approval-targets`);
//
//            selectedState.approval[step]
//                .forEach(item => {
//
//                    stepDiv.append(
//                        renderBadge(item, 'app', step)
//                    );
//                });
//
//            // 최소 직급 재계산
//            recalculateStepMinPosition(step);
//        }
//    });
//}
function applyDetailData(detail){

    // 기본 정보
    $('#templateName').val(detail.templateName);

//    $('#appLineDesc').val(detail.description);

    $('#checkDefault').prop(
        'checked',
        detail.isDefault
    );

    // 기존 상태 초기화
    selectedState.ref.clear();

    selectedState.approval = {};

    approvalRules.minPositionByStep = {};

    $('#refList').empty();

    $('#approvalList').empty();

    state.approvalStep = 0;

    // step 순회
    detail.steps.forEach(stepData => {

        // 참조(step=0)
//        if(stepData.step === 0){
        if(stepData.stepOrder === 0){

            stepData.targets.forEach(item => {

                const key =
                    `${item.type}_${item.id}`;

                selectedState.ref.set(key, item);

                $('#refList').append(
                    renderBadge(item, 'ref')
                );
            });
        }

        // 결재 단계
        else {

//            const step = stepData.step;
            const step = stepData.stepOrder;

            state.approvalStep =
                Math.max(state.approvalStep, step);

            addApprovalStepUI(step);

            if(!selectedState.approval[step]){
                selectedState.approval[step] =
                    new Map();
            }

            stepData.targets.forEach(item => {

                const key =
                    `${item.type}_${item.id}`;

                selectedState.approval[step]
                    .set(key, item);
            });

            // badge 렌더링
            const stepDiv =
                $(`#approval_${step} .approval-targets`);

            selectedState.approval[step]
                .forEach(item => {

                    stepDiv.append(
                        renderBadge(item, 'app', step)
                    );
                });

            // 최소 직급 재계산
            recalculateStepMinPosition(step);
        }
    });
}

// [9) 이벤트 바인드] -------------------------
function bindCommonEvents(){
    // 참조 대상 추가 버튼 클릭 시 
    $('#addRefTarget').on('click', function () {
        openSearchPanel('ref'); // 참조 대상 조회 모드로 설정
    });

    // 결재 단계 추가 버튼 클릭 시 
    $('#addAppSteps').on('click', function () {
        // TODO: max(position_id) -1 이하여야만 함. 백엔드 통신 후 이 값 수정할 것. 
        if(state.approvalStep <= 4) {
            state.approvalStep++; // 결재 단계 증가
            state.currentEditingStep = state.approvalStep; // 현재 수정 중인 결재 단계 업데이트

            // 왼쪽 UI에도 단계 추가
            addApprovalStepUI(state.approvalStep);

            openSearchPanel('app'); // 결재 대상 조회 모드로 설정
        }
        else{
            alert("더 결재 단계를 추가할 수 없습니다."); 
        }
    });

    // 대상 유형 변경 (라디오 버튼)
    $('input[name="btnradio"]').on('change', function () {
        const id = $(this).attr('id');

        if (id === 'btnradio1') state.targetType = 'user';
        if (id === 'btnradio2') state.targetType = 'dept';
        if (id === 'btnradio3') state.targetType = 'position';

        renderTargetUI();
    });

    // 사원 검색 버튼 클릭 동작
    $(document).on('click', '#searchBtn', function () {
        const keyword = $('#targetList input[type="search"]').val();

        if (state.targetType === 'user') {
            loadEmployees(keyword);
        }
        if (state.targetType === 'dept') {
            loadDepartments(); // 필요하면 keyword 전달
        }
    });

    // 사원/부서/권한 다중 선택 -> 상태 저장
    $(document).on('change', '.target-checkbox', function () {

        const id = $(this).val();

        const type = state.targetType === 'user' ? 'USER'
        : state.targetType === 'dept' ? 'DEPT'
        : 'POSITION';

        const key = `${type}_${id}`; // 유형별 키 생성 

        const data = {
            id: id,

            name: $(this).data('name'),

            dept: $(this).data('dept'),

            position: $(this).data('position'),

            positionId: Number($(this).data('position-id')),

            type: type
        };

        // 참조 대상 
        if (state.searchMode === 'ref') {

            if ($(this).is(':checked')) {
                selectedState.ref.set(key, data);
            } else {
                selectedState.ref.delete(key);
            }

        } 
        // 결재 대상
        else {

            if (!selectedState.approval[state.currentEditingStep]) {
                selectedState.approval[state.currentEditingStep] = new Map();
            }

            const stepMap = selectedState.approval[state.currentEditingStep];
            
            // 체크 시 저장
            if ($(this).is(':checked')) {

                stepMap.set(key, data);

                // 사원이나 직급 목록 선택 시 최소 직급 갱신
                if (type === 'USER' || type === 'POSITION') {

                    const currentMin =
                        approvalRules.minPositionByStep[
                            state.currentEditingStep
                        ] || 0;

                    approvalRules.minPositionByStep[
                        state.currentEditingStep
                    ] = Math.max(
                        currentMin,
                        data.positionId
                    );
                }
            }
            // 체크 해제 시 삭제
            else {

                stepMap.delete(key);
            }

            recalculateStepMinPosition(state.currentEditingStep); // 결재 가능 최소 직급 재계산
        }
    });

    // 선택 완료 버튼 클릭 시 제출
    $('#confirmSelect').on('click', function () {

        const selected = [];

        // 선택된 참조 대상 추가
        if (state.searchMode === 'ref') {
            $('#refList').empty();

            selectedState.ref.forEach(item => {
                $('#refList').append(renderBadge(item, 'ref'));
            });
        } 
        // 선택된 결재 대상 추가
        else {
            const step = state.currentEditingStep;
            const stepDiv = $(`#approval_${step} .approval-targets`);

            stepDiv.empty();

            // const stepMap = selectedState.approval[step] || new Map();
            const stepMap = selectedState.approval[state.currentEditingStep] || new Map();

            stepMap.forEach(item => {
                stepDiv.append(renderBadge(item, 'app', step));
            });
        }

        // 선택 후 패널 가리기
        panel.hide();
    });
}
