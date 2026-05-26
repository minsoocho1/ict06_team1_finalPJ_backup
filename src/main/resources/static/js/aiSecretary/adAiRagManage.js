/**
 * @FileName : adAiRagManage.js
 * @Description : 관리자 지식 베이스 및 RAG 관리 화면 전용 스크립트
 * @Author : 송혜진
 * @Date : 2026. 05. 20
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.05.20    송혜진        최초 생성 (RAG 관리 화면 모달, 필터, 상세 보기 스크립트 분리)
 * @ 2026.05.21    송혜진        자료 등록 요청 상세 모달 및 문서/RAG 데이터 상세 모달 처리 정리
 * @ 2026.05.22    송혜진        본부/팀/직책 기준 권한 선택 UI 및 targetDept 요약값 처리 반영
 * @ 2026.05.22    송혜진        새 문서 업로드, 문서 활성화 버튼, 상태 라벨 표시 흐름 정리
 */

document.addEventListener("DOMContentLoaded", function () {
  // ------------------------------------------------------------
  // Bootstrap tooltip
  // ------------------------------------------------------------
  if (window.bootstrap) {
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (tooltipEl) {
      bootstrap.Tooltip.getOrCreateInstance(tooltipEl);
    });
  }

  // ------------------------------------------------------------
  // 기간 필터 라벨
  // ------------------------------------------------------------
  document.querySelectorAll(".dropdown").forEach(function (dropdown) {
    const labelButton = dropdown.querySelector(".js-date-range-label");
    const startInput = dropdown.querySelector(".js-date-start");
    const endInput = dropdown.querySelector(".js-date-end");
    if (!labelButton || !startInput || !endInput) return;

    const labelSpan = labelButton.querySelector("span");

    function formatDate(value) {
      if (!value) return "";
      return value.replaceAll("-", ".");
    }

    function updateDateRangeLabel() {
      if (!labelSpan) return;

      const hasStart = !!startInput.value;
      const hasEnd = !!endInput.value;

      if (!hasStart && !hasEnd) {
        labelSpan.textContent = "전체 기간";
        return;
      }

      if (hasStart && hasEnd) {
        labelSpan.textContent = formatDate(startInput.value) + " - " + formatDate(endInput.value);
        return;
      }

      labelSpan.textContent =
        (hasStart ? formatDate(startInput.value) : "전체 기간") +
        " - " +
        (hasEnd ? formatDate(endInput.value) : "전체 기간");
    }

    startInput.addEventListener("change", updateDateRangeLabel);
    endInput.addEventListener("change", updateDateRangeLabel);
    updateDateRangeLabel();
  });

  // ------------------------------------------------------------
  // Modal roots
  // ------------------------------------------------------------
  const requestDetailModal = document.getElementById("knowledgeRequestDetailModal");
  const documentManagementModal = document.getElementById("documentManagementDetailModal");
  const uploadDocumentModal = document.getElementById("uploadDocumentModal");

  if (!requestDetailModal) return;

  // ------------------------------------------------------------
  // 자료 등록 요청 상세 모달 요소
  // ------------------------------------------------------------
  const titleInput = requestDetailModal.querySelector("#knowledgeRequestTitle");
  const requestTypeInput = requestDetailModal.querySelector("#knowledgeRequestType");
  const categoryInput = requestDetailModal.querySelector("#knowledgeRequestCategory");
  const requesterInput = requestDetailModal.querySelector("#knowledgeRequestRequesterName");
  const createdAtInput = requestDetailModal.querySelector("#knowledgeRequestCreatedAt");
  const reasonInput = requestDetailModal.querySelector("#knowledgeRequestReason");
  const sampleQuestionInput = requestDetailModal.querySelector("#knowledgeRequestSampleQuestion");
  const adminNameInput = requestDetailModal.querySelector("#knowledgeRequestAdminName");
  const userHopeAccessLevelInput = requestDetailModal.querySelector("#knowledgeRequestUserHopeAccessLevel");
  const userHopeConditionInput = requestDetailModal.querySelector("#knowledgeRequestUserHopeCondition");
  const referenceLink = requestDetailModal.querySelector("#knowledgeRequestReferenceLink");
  const referenceText = requestDetailModal.querySelector("#knowledgeRequestReferenceText");

  const requestFinalTargetSelector = requestDetailModal.querySelector("#requestFinalTargetSelector");
  const requestFinalTargetSummary = requestDetailModal.querySelector("#requestFinalTargetSummary");
  const requestFinalHeadquarterId = requestDetailModal.querySelector("#requestFinalHeadquarterId");
  const requestFinalTeamDeptIds = requestDetailModal.querySelector("#requestFinalTeamDeptIds");
  const requestFinalPositionId = requestDetailModal.querySelector("#requestFinalPositionId");
  const requestFinalEmpNo = requestDetailModal.querySelector("#requestFinalEmpNo");

  const reviewForm = requestDetailModal.querySelector("#knowledgeRequestReviewForm");
  const reviewStatusInput = requestDetailModal.querySelector("#knowledgeRequestReviewStatus");
  const adminReviewMemo = requestDetailModal.querySelector('[name="adminComment"]');
  const approveBtn = requestDetailModal.querySelector(".js-knowledge-request-approve");
  const rejectBtn = requestDetailModal.querySelector(".js-knowledge-request-reject");
  const autocompleteWrappers = Array.from(requestDetailModal.querySelectorAll(".admin-autocomplete"));
  const reviewModalDismissButtons = Array.from(requestDetailModal.querySelectorAll('[data-bs-dismiss="modal"]'));

  // ------------------------------------------------------------
  // 문서 및 RAG 데이터 상세 모달 요소
  // ------------------------------------------------------------
  const documentManagementDocumentId = documentManagementModal?.querySelector("#documentManagementDocumentId");
  const documentManagementRegisteredAt = documentManagementModal?.querySelector("#documentManagementRegisteredAt");
  const documentManagementTopStatusBadge = documentManagementModal?.querySelector("#documentManagementTopStatusBadge");
  const documentManagementStatusBadge = documentManagementModal?.querySelector("#documentManagementStatusBadge");
  const documentManagementTitle = documentManagementModal?.querySelector("#documentManagementTitle");
  const documentManagementType = documentManagementModal?.querySelector("#documentManagementType");
  const documentManagementCategory = documentManagementModal?.querySelector("#documentManagementCategory");
  const documentManagementApproverName = documentManagementModal?.querySelector("#documentManagementApproverName");
  const documentManagementChunkCount = documentManagementModal?.querySelector("#documentManagementChunkCount");
  const documentManagementVectorCount = documentManagementModal?.querySelector("#documentManagementVectorCount");
  const documentManagementChunkDetailWrap = documentManagementModal?.querySelector("#documentManagementChunkDetailWrap");
  const documentManagementChunkDetailBody = documentManagementModal?.querySelector("#documentManagementChunkDetailBody");
  const documentManagementVectorDetailWrap = documentManagementModal?.querySelector("#documentManagementVectorDetailWrap");
  const documentManagementVectorDetailBody = documentManagementModal?.querySelector("#documentManagementVectorDetailBody");
  const documentManagementRequesterName = documentManagementModal?.querySelector("#documentManagementRequesterName");
  const documentManagementRequestDate = documentManagementModal?.querySelector("#documentManagementRequestDate");
  const documentManagementReason = documentManagementModal?.querySelector("#documentManagementReason");
  const documentManagementSampleQuestion = documentManagementModal?.querySelector("#documentManagementSampleQuestion");
  const documentManagementReferenceLink = documentManagementModal?.querySelector("#documentManagementReferenceLink");
  const documentManagementReferenceText = documentManagementModal?.querySelector("#documentManagementReferenceText");
  const documentManagementSummary = documentManagementModal?.querySelector("#documentManagementSummary");
  const documentManagementAdminComment = documentManagementModal?.querySelector("#documentManagementAdminComment");
  const documentManagementHopeCondition = documentManagementModal?.querySelector("#documentManagementHopeCondition");
  const documentManagementAppliedTargetDept = documentManagementModal?.querySelector("#documentManagementAppliedTargetDept");
  const documentManagementAppliedAdminComment = documentManagementModal?.querySelector("#documentManagementAppliedAdminComment");
  const documentManagementAppliedUpdatedAt = documentManagementModal?.querySelector("#documentManagementAppliedUpdatedAt");
  const documentManagementAdminName = documentManagementModal?.querySelector("#documentManagementAdminName");
  const documentManagementStageWrap = documentManagementModal?.querySelector("#documentManagementStageWrap");
  const documentManagementFailureReason = documentManagementModal?.querySelector("#documentManagementFailureReason");
  const documentManagementFailureSection = documentManagementModal?.querySelector("#documentManagementFailureSection");
  const documentManagementStatusLabel = documentManagementModal?.querySelector("#documentManagementStatusLabel");
  const documentManagementFailureStatusLabel = documentManagementModal?.querySelector("#documentManagementFailureStatusLabel");
  const documentManagementFailureAt = documentManagementModal?.querySelector("#documentManagementFailureAt");
  const documentManagementToggleActiveButton = documentManagementModal?.querySelector("#documentManagementToggleActiveButton");
  const documentManagementSaveButton = documentManagementModal?.querySelector("#documentManagementSaveButton");

  const documentManagementFinalTargetSelector = documentManagementModal?.querySelector("#documentManagementFinalTargetSelector");
  const documentManagementFinalTargetSummary = documentManagementModal?.querySelector("#documentManagementFinalTargetSummary");
  const documentManagementFinalHeadquarterId = documentManagementModal?.querySelector("#documentManagementFinalHeadquarterId");
  const documentManagementFinalTeamDeptIds = documentManagementModal?.querySelector("#documentManagementFinalTeamDeptIds");
  const documentManagementFinalPositionId = documentManagementModal?.querySelector("#documentManagementFinalPositionId");
  const documentManagementFinalEmpNo = documentManagementModal?.querySelector("#documentManagementFinalEmpNo");

  // ------------------------------------------------------------
  // 새 문서 업로드 모달 요소
  // ------------------------------------------------------------
  const uploadDocumentForm = uploadDocumentModal?.querySelector("#uploadDocumentForm");
  const uploadDocumentTitle = uploadDocumentModal?.querySelector("#uploadDocumentTitle");
  const uploadDocumentType = uploadDocumentModal?.querySelector("#uploadDocumentType");
  const uploadDocumentCategory = uploadDocumentModal?.querySelector("#uploadDocumentCategory");
  const uploadDocumentReason = uploadDocumentModal?.querySelector("#uploadDocumentReason");
  const uploadDocumentSampleQuestion = uploadDocumentModal?.querySelector("#uploadDocumentSampleQuestion");
  const uploadDocumentReferenceUrl = uploadDocumentModal?.querySelector("#uploadDocumentReferenceUrl");
  const uploadDocumentTargetSelector = uploadDocumentModal?.querySelector("#uploadDocumentTargetSelector");
  const uploadDocumentTargetSummary = uploadDocumentModal?.querySelector("#uploadDocumentTargetSummary");
  const uploadDocumentHeadquarterId = uploadDocumentModal?.querySelector("#uploadDocumentHeadquarterId");
  const uploadDocumentTeamDeptIds = uploadDocumentModal?.querySelector("#uploadDocumentTeamDeptIds");
  const uploadDocumentPositionId = uploadDocumentModal?.querySelector("#uploadDocumentPositionId");
  const uploadDocumentEmpNo = uploadDocumentModal?.querySelector("#uploadDocumentEmpNo");
  const uploadDocumentSubmitButton = uploadDocumentModal?.querySelector("#uploadDocumentSubmitButton");
  const uploadDocumentDismissButtons = Array.from(uploadDocumentModal?.querySelectorAll('[data-bs-dismiss="modal"]') || []);

  // ------------------------------------------------------------
  // 공통 상태
  // ------------------------------------------------------------
  const accessLevelLabels = {
    PUBLIC: "전체 공개",
    CUSTOM: "조건 조합",
    ADMIN_ONLY: "관리자 전용",
    DEPT: "부서 공개",
    ROLE: "역할 공개",
    PRIVATE: "비공개"
  };

  const baseRequestTypeSuggestions = ["사내 규정", "업무 매뉴얼", "FAQ", "서비스 이용 안내", "기타"];
  const baseCategorySuggestions = ["근태", "인사", "전자결재", "교육", "복지", "서비스", "보안", "기타"];

  let requestTypeSuggestions = buildSuggestionList(baseRequestTypeSuggestions, window.adAiRagManageSuggestions?.requestTypes);
  let categorySuggestions = buildSuggestionList(baseCategorySuggestions, window.adAiRagManageSuggestions?.categories);
  let isSubmittingReview = false;
  let documentManagementInitialIsActive = false;
  let currentDocumentChunkDetails = [];
  let currentDocumentVectorDetails = [];
  let documentManagementEditState = null;

  const orgCache = {
    departmentTree: null,
    employeesByTeam: new Map(),
    departmentLoadingPromise: null
  };

  // ------------------------------------------------------------
  // 기본 유틸
  // ------------------------------------------------------------
  function safeDatasetValue(value, fallback = "") {
    const normalized = value == null ? "" : String(value).trim();
    return normalized || fallback;
  }

  function setEditableInputValue(element, value) {
    if (!element) return;
    element.value = safeDatasetValue(value);
  }

  function setReadonlyValue(element, value, fallback = "-") {
    if (!element) return;
    element.value = safeDatasetValue(value, fallback);
  }

  function setDisplayValue(element, value, fallback = "-") {
    if (!element) return;
    element.textContent = safeDatasetValue(value, fallback);
  }

  function setFieldInvalid(element, invalid) {
    if (!element) return;
    element.classList.toggle("is-invalid", !!invalid);
  }

  function escapeHtml(value) {
    return String(value ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");
  }

  function parseJsonArray(value) {
    const normalized = safeDatasetValue(value);
    if (!normalized) return [];

    try {
      const parsed = JSON.parse(normalized);
      return Array.isArray(parsed) ? parsed : [];
    } catch (error) {
      return [];
    }
  }

  function buildSuggestionList(baseList, extraList) {
    const values = [];
    const seen = new Set();

    [...(baseList || []), ...(extraList || [])].forEach(function (item) {
      const value = safeDatasetValue(item);
      if (!value || seen.has(value)) return;
      seen.add(value);
      values.push(value);
    });

    return values;
  }

  function addSuggestionValue(list, value) {
    const normalized = safeDatasetValue(value);
    if (!normalized || list.includes(normalized)) return;
    list.push(normalized);
  }

  function filterSuggestions(list, query) {
    const normalizedQuery = safeDatasetValue(query).toLowerCase();
    if (!normalizedQuery) return list.slice();

    return list.filter(function (item) {
      return String(item).toLowerCase().includes(normalizedQuery);
    });
  }

  function buildHopeConditionText(targetDept, accessLevel) {
    const normalizedTargetDept = safeDatasetValue(targetDept);
    const normalizedAccessLevel = safeDatasetValue(accessLevel);
    const accessLabel = accessLevelLabels[normalizedAccessLevel] || normalizedAccessLevel;

    if (normalizedTargetDept && accessLabel) return accessLabel + " / " + normalizedTargetDept;
    if (normalizedTargetDept) return normalizedTargetDept;
    if (accessLabel) return accessLabel;
    return "-";
  }

  function getCurrentAdminName() {
    const candidates = [
      document.querySelector(".user-menu .nav-link .d-none.d-md-inline"),
      document.querySelector(".user-menu .user-header p span"),
      document.querySelector(".user-panel .info a"),
      document.querySelector("[data-admin-name]")
    ];

    for (const element of candidates) {
      const value = safeDatasetValue(element?.dataset?.adminName || element?.textContent);
      if (value) return value;
    }

    return "-";
  }

  // ------------------------------------------------------------
  // 참고 URL / 콘텐츠 경로 표시
  // ------------------------------------------------------------
  function showReferenceValue(value) {
    if (!referenceLink || !referenceText) return;

    const textValue = safeDatasetValue(value);

    if (!textValue) {
      referenceLink.classList.add("d-none");
      referenceLink.removeAttribute("href");
      referenceLink.removeAttribute("title");
      referenceLink.textContent = "";
      referenceText.classList.remove("d-none");
      referenceText.textContent = "등록된 파일 URL 또는 콘텐츠 경로가 없습니다.";
      referenceText.removeAttribute("title");
      return;
    }

    if (/^https?:\/\//i.test(textValue)) {
      referenceLink.href = textValue;
      referenceLink.textContent = textValue;
      referenceLink.title = textValue;
      referenceLink.classList.remove("d-none");
      referenceText.classList.add("d-none");
      referenceText.textContent = "";
      referenceText.removeAttribute("title");
      return;
    }

    referenceLink.classList.add("d-none");
    referenceLink.removeAttribute("href");
    referenceLink.removeAttribute("title");
    referenceLink.textContent = "";
    referenceText.classList.remove("d-none");
    referenceText.textContent = textValue;
    referenceText.title = textValue;
  }

  function showDocumentReferenceValue(value) {
    if (!documentManagementReferenceLink || !documentManagementReferenceText) return;

    const textValue = safeDatasetValue(value);

    if (!textValue) {
      documentManagementReferenceLink.classList.add("d-none");
      documentManagementReferenceLink.removeAttribute("href");
      documentManagementReferenceLink.removeAttribute("title");
      documentManagementReferenceLink.textContent = "";
      documentManagementReferenceText.classList.remove("d-none");
      documentManagementReferenceText.textContent = "등록된 본문 URL이 없습니다.";
      documentManagementReferenceText.removeAttribute("title");
      return;
    }

    documentManagementReferenceLink.href = textValue;
    documentManagementReferenceLink.textContent = textValue;
    documentManagementReferenceLink.title = textValue;
    documentManagementReferenceLink.classList.remove("d-none");
    documentManagementReferenceText.classList.add("d-none");
    documentManagementReferenceText.textContent = "";
    documentManagementReferenceText.removeAttribute("title");
  }

  function normalizeDocumentManagementEditValue(value) {
    return safeDatasetValue(value).trim();
  }

  function collectDocumentManagementEditableState() {
    return {
      title: normalizeDocumentManagementEditValue(documentManagementTitle?.value),
      requestType: normalizeDocumentManagementEditValue(documentManagementType?.value),
      category: normalizeDocumentManagementEditValue(documentManagementCategory?.value),
      targetDept: normalizeDocumentManagementEditValue(documentOrgSelector?.getSummary()),
      adminComment: normalizeDocumentManagementEditValue(documentManagementAdminComment?.value)
    };
  }

  function buildDocumentManagementInitialState(data) {
    return {
      title: normalizeDocumentManagementEditValue(data.title),
      requestType: normalizeDocumentManagementEditValue(data.requestType),
      category: normalizeDocumentManagementEditValue(data.category),
      targetDept: normalizeDocumentManagementEditValue(data.targetDept),
      adminComment: normalizeDocumentManagementEditValue(data.adminComment)
    };
  }

  function updateDocumentManagementSaveButtonState() {
    if (!documentManagementSaveButton) return;
    if (!documentManagementEditState || !documentManagementEditState.documentId) {
      documentManagementSaveButton.disabled = true;
      return;
    }

    const current = collectDocumentManagementEditableState();
    const initial = documentManagementEditState.initial || {};
    const changed = Object.keys(initial).some(function (key) {
      return normalizeDocumentManagementEditValue(current[key]) !== normalizeDocumentManagementEditValue(initial[key]);
    });

    documentManagementSaveButton.disabled = !changed;
  }

  // ------------------------------------------------------------
  // Autocomplete
  // ------------------------------------------------------------
  function closeAutocomplete(wrapper) {
    if (!wrapper) return;
    const dropdown = wrapper.querySelector(".js-admin-autocomplete-dropdown");
    if (!dropdown) return;
    dropdown.classList.remove("show");
    dropdown.setAttribute("aria-hidden", "true");
    dropdown.innerHTML = "";
  }

  function closeAllAutocomplete() {
    autocompleteWrappers.forEach(closeAutocomplete);
  }

  function renderAutocomplete(wrapper, items, input) {
    const dropdown = wrapper?.querySelector(".js-admin-autocomplete-dropdown");
    if (!dropdown || !input) return;

    dropdown.innerHTML = "";
    const currentValue = safeDatasetValue(input.value);

    if (!items.length) {
      const empty = document.createElement("div");
      empty.className = "admin-autocomplete-empty";
      empty.textContent = "입력한 값으로 사용합니다.";
      dropdown.appendChild(empty);
      dropdown.classList.add("show");
      dropdown.setAttribute("aria-hidden", "false");
      return;
    }

    items.forEach(function (item) {
      const option = document.createElement("button");
      option.className = "admin-autocomplete-item";
      option.type = "button";
      option.textContent = item;
      option.setAttribute("role", "option");
      option.setAttribute("aria-selected", String(currentValue === item));
      option.tabIndex = -1;
      option.dataset.value = item;

      if (currentValue === item) {
        option.classList.add("active");
      }

      option.addEventListener("mousedown", function (event) {
        event.preventDefault();
        input.value = item;
        input.dispatchEvent(new Event("input", { bubbles: true }));
        closeAutocomplete(wrapper);
      });

      dropdown.appendChild(option);
    });

    dropdown.classList.add("show");
    dropdown.setAttribute("aria-hidden", "false");
  }

  function openAutocomplete(wrapper) {
    if (!wrapper) return;

    const input = wrapper.querySelector(".js-admin-autocomplete-input");
    if (!input) return;

    const suggestionList = wrapper.dataset.autocomplete === "category" ? categorySuggestions : requestTypeSuggestions;
    const filtered = filterSuggestions(suggestionList, input.value);
    renderAutocomplete(wrapper, filtered, input);
  }

  function initAutocomplete(wrapper) {
    const input = wrapper.querySelector(".js-admin-autocomplete-input");
    if (!input) return;

    input.addEventListener("focus", function () {
      openAutocomplete(wrapper);
    });

    input.addEventListener("input", function () {
      openAutocomplete(wrapper);
    });

    input.addEventListener("keydown", function (event) {
      if (event.key === "Escape") {
        closeAutocomplete(wrapper);
      }
    });

    document.addEventListener("mousedown", function (event) {
      if (!wrapper.contains(event.target)) {
        closeAutocomplete(wrapper);
      }
    });
  }

  autocompleteWrappers.forEach(initAutocomplete);

  // ------------------------------------------------------------
  // 조직 선택기: 대상 본부 → 팀 → 직책 기준
  // 사원 선택 UI는 사용하지 않음
  // 직원 API는 직책 목록 추출용으로만 사용
  // ------------------------------------------------------------
  function getDeptId(dept) {
    return safeDatasetValue(dept?.deptId ?? dept?.id ?? dept?.departmentId);
  }

  function getDeptName(dept) {
    return safeDatasetValue(dept?.deptName ?? dept?.name ?? dept?.departmentName);
  }

  function getDeptChildren(dept) {
    if (Array.isArray(dept?.children)) return dept.children;
    if (Array.isArray(dept?.childDepartments)) return dept.childDepartments;
    if (Array.isArray(dept?.departments)) return dept.departments;
    return [];
  }

  function normalizeEmployee(raw) {
    return {
      empNo: safeDatasetValue(raw?.empNo ?? raw?.employeeNo ?? raw?.id),
      empName: safeDatasetValue(raw?.empName ?? raw?.name ?? raw?.employeeName),
      positionId: safeDatasetValue(raw?.positionId ?? raw?.positionNo ?? raw?.positionCode),
      positionName: safeDatasetValue(raw?.positionName ?? raw?.position),
      deptId: safeDatasetValue(raw?.deptId ?? raw?.departmentId),
      deptName: safeDatasetValue(raw?.deptName ?? raw?.departmentName ?? raw?.teamName)
    };
  }

  function parseTargetSummary(summary) {
    const text = safeDatasetValue(summary);

    const extract = function (label) {
      const match = text.match(new RegExp(label + "\\s*:\\s*([^/]+)"));
      return match ? safeDatasetValue(match[1]) : "";
    };

    return {
      headquarterName: extract("본부") || extract("대상 본부"),
      teamName: extract("팀") || extract("대상 팀"),
      positionName: extract("직책") || extract("직책 기준")
    };
  }

  function findDepartmentByName(list, name) {
    const targetName = safeDatasetValue(name);
    if (!targetName) return null;

    return (Array.isArray(list) ? list : []).find(function (dept) {
      return getDeptName(dept) === targetName;
    }) || null;
  }

  function findHeadquarterByTeamName(tree, teamName) {
    const targetTeamName = safeDatasetValue(teamName);
    if (!targetTeamName) return null;

    return (Array.isArray(tree) ? tree : []).find(function (head) {
      return getDeptChildren(head).some(function (team) {
        return getDeptName(team) === targetTeamName;
      });
    }) || null;
  }

  function uniquePositions(employees) {
    const map = new Map();

    (Array.isArray(employees) ? employees : []).forEach(function (employee) {
      if (!employee.positionId && !employee.positionName) return;

      const key = employee.positionId || employee.positionName;

      if (!map.has(key)) {
        map.set(key, {
          positionId: employee.positionId || employee.positionName,
          positionName: employee.positionName || employee.positionId
        });
      }
    });

    return Array.from(map.values()).sort(function (a, b) {
      const aNum = Number(a.positionId);
      const bNum = Number(b.positionId);

      if (!Number.isNaN(aNum) && !Number.isNaN(bNum)) {
        return aNum - bNum;
      }

      return String(a.positionName).localeCompare(String(b.positionName), "ko");
    });
  }

  function loadDepartmentTree() {
    if (Array.isArray(orgCache.departmentTree)) {
      return Promise.resolve(orgCache.departmentTree);
    }

    if (orgCache.departmentLoadingPromise) {
      return orgCache.departmentLoadingPromise;
    }

    orgCache.departmentLoadingPromise = fetch("/api/organization/departments/tree", {
      credentials: "same-origin"
    })
      .then(function (response) {
        if (!response.ok) {
          throw new Error("조직도 조회 실패");
        }

        return response.json();
      })
      .then(function (payload) {
        orgCache.departmentTree = Array.isArray(payload)
          ? payload
          : Array.isArray(payload?.data)
            ? payload.data
            : [];

        return orgCache.departmentTree;
      })
      .catch(function (error) {
        orgCache.departmentTree = [];
        throw error;
      })
      .finally(function () {
        orgCache.departmentLoadingPromise = null;
      });

    return orgCache.departmentLoadingPromise;
  }

  function loadEmployees(teamDeptId) {
    const teamId = safeDatasetValue(teamDeptId);

    if (!teamId) {
      return Promise.resolve([]);
    }

    if (orgCache.employeesByTeam.has(teamId)) {
      return Promise.resolve(orgCache.employeesByTeam.get(teamId));
    }

    return fetch("/api/organization/employees?deptId=" + encodeURIComponent(teamId), {
      credentials: "same-origin"
    })
      .then(function (response) {
        if (!response.ok) {
          throw new Error("직원 목록 조회 실패");
        }

        return response.json();
      })
      .then(function (payload) {
        const rows = Array.isArray(payload)
          ? payload
          : Array.isArray(payload?.data)
            ? payload.data
            : Array.isArray(payload?.content)
              ? payload.content
              : [];

        const employees = rows.map(normalizeEmployee).filter(function (employee) {
          return employee.empNo || employee.positionId || employee.positionName;
        });

        orgCache.employeesByTeam.set(teamId, employees);
        return employees;
      });
  }

  function clearSelect(select, placeholder, disabled) {
    if (!select) return;

    select.innerHTML = "";

    const option = document.createElement("option");
    option.value = "";
    option.textContent = placeholder;
    select.appendChild(option);
    select.disabled = !!disabled;
  }

  function appendOption(select, value, text, selected) {
    if (!select) return;

    const option = document.createElement("option");
    option.value = safeDatasetValue(value);
    option.textContent = safeDatasetValue(text, "-");
    option.selected = !!selected;
    select.appendChild(option);
  }

  function selectedText(select) {
    if (!select || !select.value) return "";
    return safeDatasetValue(select.options[select.selectedIndex]?.textContent);
  }

  function createOrgTargetSelector(config) {
    const container = document.getElementById(config.containerId);
    const summaryInput = document.getElementById(config.summaryInputId);
    const headInput = document.getElementById(config.headInputId);
    const teamInput = document.getElementById(config.teamInputId);
    const positionInput = document.getElementById(config.positionInputId);
    const empInput = config.empInputId ? document.getElementById(config.empInputId) : null;

    if (!container) return null;

    let state = {
      headquarterId: "",
      teamDeptId: "",
      positionId: "",
      employees: [],
      seedSummary: ""
    };

    function renderBase() {
      container.innerHTML = [
        '<div class="row g-3">',
        '<div class="col-md-4">',
        '<label class="form-label small fw-bold text-muted">대상 본부 선택</label>',
        '<select class="form-select form-select-sm" id="' + config.headSelectId + '"></select>',
        '</div>',
        '<div class="col-md-4">',
        '<label class="form-label small fw-bold text-muted">팀 선택</label>',
        '<select class="form-select form-select-sm" id="' + config.teamSelectId + '" disabled></select>',
        '</div>',
        '<div class="col-md-4">',
        '<label class="form-label small fw-bold text-muted">직책 기준 선택</label>',
        '<select class="form-select form-select-sm" id="' + config.positionSelectId + '" disabled></select>',
        '</div>',
        '</div>'
      ].join("");
    }

    function getHeadSelect() {
      return container.querySelector("#" + config.headSelectId);
    }

    function getTeamSelect() {
      return container.querySelector("#" + config.teamSelectId);
    }

    function getPositionSelect() {
      return container.querySelector("#" + config.positionSelectId);
    }

    function getSelectedHeadquarter() {
      const tree = Array.isArray(orgCache.departmentTree) ? orgCache.departmentTree : [];

      return tree.find(function (dept) {
        return getDeptId(dept) === state.headquarterId;
      }) || null;
    }

    function getTeamOptions() {
      const headquarter = getSelectedHeadquarter();
      if (!headquarter) return [];

      const children = getDeptChildren(headquarter);
      return children.length ? children : [headquarter];
    }

    function renderHeadquarters() {
      const select = getHeadSelect();
      const tree = Array.isArray(orgCache.departmentTree) ? orgCache.departmentTree : [];

      clearSelect(select, "대상 본부를 선택하세요", false);

      if (!tree.length) {
        clearSelect(select, "선택 가능한 본부가 없습니다", true);
        return;
      }

      tree.forEach(function (dept) {
        appendOption(select, getDeptId(dept), getDeptName(dept), getDeptId(dept) === state.headquarterId);
      });
    }

    function renderTeams() {
      const select = getTeamSelect();
      const teams = getTeamOptions();

      clearSelect(select, "팀을 선택하세요", !state.headquarterId);

      if (!state.headquarterId) return;

      if (!teams.length) {
        clearSelect(select, "선택 가능한 팀이 없습니다", true);
        return;
      }

      teams.forEach(function (dept) {
        appendOption(select, getDeptId(dept), getDeptName(dept), getDeptId(dept) === state.teamDeptId);
      });
    }

    function renderPositions() {
      const select = getPositionSelect();
      const positions = uniquePositions(state.employees);

      clearSelect(select, "직책을 선택하세요", !state.teamDeptId || !positions.length);

      if (!state.teamDeptId) return;

      if (!positions.length) {
        clearSelect(select, "선택 가능한 직책이 없습니다", true);
        return;
      }

      positions.forEach(function (position) {
        appendOption(select, position.positionId, position.positionName, position.positionId === state.positionId);
      });
    }

    function updateHidden() {
      const headName = selectedText(getHeadSelect());
      const teamName = selectedText(getTeamSelect());
      const positionName = selectedText(getPositionSelect());
      const parts = [];

      if (headName) parts.push("본부: " + headName);
      if (teamName) parts.push("팀: " + teamName);
      if (positionName) parts.push("직책: " + positionName);

      if (summaryInput) summaryInput.value = parts.join(" / ");
      if (headInput) headInput.value = state.headquarterId;
      if (teamInput) teamInput.value = state.teamDeptId;
      if (positionInput) positionInput.value = state.positionId;
      if (empInput) empInput.value = "";
    }

    function bindEvents() {
      if (container.dataset.orgTargetBound === "true") return;

      container.addEventListener("change", function (event) {
        const target = event.target;
        if (!(target instanceof HTMLSelectElement)) return;

        if (target.id === config.headSelectId) {
          state.headquarterId = safeDatasetValue(target.value);
          state.teamDeptId = "";
          state.positionId = "";
          state.employees = [];

          renderTeams();
          renderPositions();
          updateHidden();
          setFieldInvalid(target, false);
          return;
        }

        if (target.id === config.teamSelectId) {
          state.teamDeptId = safeDatasetValue(target.value);
          state.positionId = "";
          state.employees = [];

          renderPositions();
          updateHidden();
          setFieldInvalid(target, false);

          if (!state.teamDeptId) return;

          loadEmployees(state.teamDeptId)
            .then(function (employees) {
              state.employees = employees;
              renderPositions();
              updateHidden();
            })
            .catch(function () {
              state.employees = [];
              renderPositions();
              updateHidden();
            });
          return;
        }

        if (target.id === config.positionSelectId) {
          state.positionId = safeDatasetValue(target.value);
          updateHidden();
          setFieldInvalid(target, false);
        }
      });

      container.dataset.orgTargetBound = "true";
    }

    function applySeed() {
      const seed = parseTargetSummary(state.seedSummary);
      const tree = Array.isArray(orgCache.departmentTree) ? orgCache.departmentTree : [];
      let headquarter = findDepartmentByName(tree, seed.headquarterName) || findHeadquarterByTeamName(tree, seed.teamName);

      if (headquarter) {
        state.headquarterId = getDeptId(headquarter);
      }

      renderHeadquarters();
      renderTeams();

      const team = findDepartmentByName(getTeamOptions(), seed.teamName);

      if (!team) {
        updateHidden();
        return Promise.resolve();
      }

      state.teamDeptId = getDeptId(team);
      renderTeams();

      return loadEmployees(state.teamDeptId).then(function (employees) {
        state.employees = employees;

        const matchedPosition = uniquePositions(employees).find(function (position) {
          return position.positionName === seed.positionName;
        });

        if (matchedPosition) {
          state.positionId = matchedPosition.positionId;
        }

        renderPositions();
        updateHidden();
      });
    }

    function init(seedSummary) {
      state = {
        headquarterId: "",
        teamDeptId: "",
        positionId: "",
        employees: [],
        seedSummary: safeDatasetValue(seedSummary)
      };

      renderBase();
      bindEvents();

      clearSelect(getHeadSelect(), "본부를 불러오는 중입니다", true);
      clearSelect(getTeamSelect(), "팀을 선택하세요", true);
      clearSelect(getPositionSelect(), "직책을 선택하세요", true);
      updateHidden();

      return loadDepartmentTree()
        .then(function () {
          renderHeadquarters();
          renderTeams();
          renderPositions();
          return applySeed();
        })
        .catch(function () {
          container.innerHTML = '<div class="text-danger small">조직 데이터를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.</div>';

          if (summaryInput) summaryInput.value = "";
          if (headInput) headInput.value = "";
          if (teamInput) teamInput.value = "";
          if (positionInput) positionInput.value = "";
          if (empInput) empInput.value = "";
        });
    }

    function reset() {
      container.innerHTML = "";
      if (summaryInput) summaryInput.value = "";
      if (headInput) headInput.value = "";
      if (teamInput) teamInput.value = "";
      if (positionInput) positionInput.value = "";
      if (empInput) empInput.value = "";
    }

    function getSummary() {
      return safeDatasetValue(summaryInput?.value);
    }

    function validate() {
      const headSelect = getHeadSelect();
      const teamSelect = getTeamSelect();
      const positionSelect = getPositionSelect();

      if (!safeDatasetValue(headSelect?.value)) {
        setFieldInvalid(headSelect, true);
        headSelect?.focus();
        return { valid: false, message: "대상 본부를 선택해 주세요." };
      }

      if (!safeDatasetValue(teamSelect?.value)) {
        setFieldInvalid(teamSelect, true);
        teamSelect?.focus();
        return { valid: false, message: "팀을 선택해 주세요." };
      }

      if (!safeDatasetValue(positionSelect?.value)) {
        setFieldInvalid(positionSelect, true);
        positionSelect?.focus();
        return { valid: false, message: "직책 기준을 선택해 주세요." };
      }

      return { valid: true, message: "" };
    }

    return {
      init: init,
      reset: reset,
      validate: validate,
      getSummary: getSummary
    };
  }

  const requestOrgSelector = createOrgTargetSelector({
    containerId: "requestFinalTargetSelector",
    headSelectId: "knowledgeRequestHeadquarterSelect",
    teamSelectId: "knowledgeRequestTeamSelect",
    positionSelectId: "knowledgeRequestPositionSelect",
    summaryInputId: "requestFinalTargetSummary",
    headInputId: "requestFinalHeadquarterId",
    teamInputId: "requestFinalTeamDeptIds",
    positionInputId: "requestFinalPositionId",
    empInputId: "requestFinalEmpNo"
  });

  const documentOrgSelector = createOrgTargetSelector({
    containerId: "documentManagementFinalTargetSelector",
    headSelectId: "documentManagementFinalHeadquarterSelect",
    teamSelectId: "documentManagementFinalTeamSelect",
    positionSelectId: "documentManagementFinalPositionSelect",
    summaryInputId: "documentManagementFinalTargetSummary",
    headInputId: "documentManagementFinalHeadquarterId",
    teamInputId: "documentManagementFinalTeamDeptIds",
    positionInputId: "documentManagementFinalPositionId",
    empInputId: "documentManagementFinalEmpNo"
  });

  const uploadOrgSelector = createOrgTargetSelector({
    containerId: "uploadDocumentTargetSelector",
    headSelectId: "uploadDocumentHeadquarterSelect",
    teamSelectId: "uploadDocumentTeamSelect",
    positionSelectId: "uploadDocumentPositionSelect",
    summaryInputId: "uploadDocumentTargetSummary",
    headInputId: "uploadDocumentHeadquarterId",
    teamInputId: "uploadDocumentTeamDeptIds",
    positionInputId: "uploadDocumentPositionId",
    empInputId: "uploadDocumentEmpNo"
  });

  // ------------------------------------------------------------
  // 새 문서 업로드
  // ------------------------------------------------------------
  function clearUploadValidationState() {
    [
      uploadDocumentTitle,
      uploadDocumentType,
      uploadDocumentCategory,
      uploadDocumentReason,
      uploadDocumentSampleQuestion,
      uploadDocumentReferenceUrl
    ].forEach(function (element) {
      setFieldInvalid(element, false);
    });

    uploadDocumentTargetSelector?.querySelectorAll("select").forEach(function (element) {
      setFieldInvalid(element, false);
    });
  }

  function resetUploadDocumentForm() {
    if (uploadDocumentForm) uploadDocumentForm.reset();
    clearUploadValidationState();
    uploadOrgSelector?.reset();
    if (uploadDocumentTargetSummary) uploadDocumentTargetSummary.value = "";
    if (uploadDocumentHeadquarterId) uploadDocumentHeadquarterId.value = "";
    if (uploadDocumentTeamDeptIds) uploadDocumentTeamDeptIds.value = "";
    if (uploadDocumentPositionId) uploadDocumentPositionId.value = "";
    if (uploadDocumentEmpNo) uploadDocumentEmpNo.value = "";
    if (uploadDocumentSubmitButton) {
      uploadDocumentSubmitButton.disabled = false;
      uploadDocumentSubmitButton.textContent = "등록";
    }
    uploadDocumentDismissButtons.forEach(function (button) {
      button.disabled = false;
      if (!button.classList.contains("btn-close")) {
        button.textContent = "닫기";
      }
    });
  }

  function setUploadSubmittingState() {
    if (uploadDocumentSubmitButton) {
      uploadDocumentSubmitButton.disabled = true;
      uploadDocumentSubmitButton.textContent = "등록 처리 중...";
    }

    uploadDocumentDismissButtons.forEach(function (button) {
      button.disabled = true;
    });
  }

  function validateUploadDocumentForm() {
    clearUploadValidationState();

    const titleValue = safeDatasetValue(uploadDocumentTitle?.value);
    const typeValue = safeDatasetValue(uploadDocumentType?.value);
    const categoryValue = safeDatasetValue(uploadDocumentCategory?.value);
    const reasonValue = safeDatasetValue(uploadDocumentReason?.value);
    const sampleQuestionValue = safeDatasetValue(uploadDocumentSampleQuestion?.value);
    const referenceUrlValue = safeDatasetValue(uploadDocumentReferenceUrl?.value);

    if (!titleValue) {
      alert("문서명을 입력해 주세요.");
      setFieldInvalid(uploadDocumentTitle, true);
      uploadDocumentTitle?.focus();
      return false;
    }

    if (!typeValue) {
      alert("문서 유형을 선택해 주세요.");
      setFieldInvalid(uploadDocumentType, true);
      uploadDocumentType?.focus();
      return false;
    }

    if (!categoryValue) {
      alert("카테고리를 선택해 주세요.");
      setFieldInvalid(uploadDocumentCategory, true);
      uploadDocumentCategory?.focus();
      return false;
    }

    if (!reasonValue) {
      alert("요청 사유를 입력해 주세요.");
      setFieldInvalid(uploadDocumentReason, true);
      uploadDocumentReason?.focus();
      return false;
    }

    if (!sampleQuestionValue) {
      alert("챗봇 질문 예시를 입력해 주세요.");
      setFieldInvalid(uploadDocumentSampleQuestion, true);
      uploadDocumentSampleQuestion?.focus();
      return false;
    }

    if (!referenceUrlValue) {
      alert("참고 URL 또는 콘텐츠 경로를 입력해 주세요.");
      setFieldInvalid(uploadDocumentReferenceUrl, true);
      uploadDocumentReferenceUrl?.focus();
      return false;
    }

    const orgValidation = uploadOrgSelector?.validate();
    if (orgValidation && !orgValidation.valid) {
      alert(orgValidation.message);
      return false;
    }

    return true;
  }

  // ------------------------------------------------------------
  // 자료 등록 요청 상세: 승인/반려 버튼 상태
  // ------------------------------------------------------------
  function resetReviewButtonLabels() {
    if (approveBtn) {
      approveBtn.disabled = false;
      approveBtn.textContent = "승인";
    }

    if (rejectBtn) {
      rejectBtn.disabled = false;
      rejectBtn.textContent = "반려";
    }

    reviewModalDismissButtons.forEach(function (button) {
      button.disabled = false;
      if (!button.classList.contains("btn-close")) {
        button.textContent = "닫기";
      }
    });
  }

  function setReviewSubmittingState(action) {
    isSubmittingReview = true;

    [approveBtn, rejectBtn, ...reviewModalDismissButtons].forEach(function (button) {
      if (button) button.disabled = true;
    });

    if (action === "APPROVED" && approveBtn) {
      approveBtn.textContent = "승인 처리 중...";
    } else if (action === "REJECTED" && rejectBtn) {
      rejectBtn.textContent = "반려 처리 중...";
    }
  }

  function resetReviewSubmittingState() {
    isSubmittingReview = false;
    resetReviewButtonLabels();
  }

  function clearValidationState() {
    [titleInput, requestTypeInput, categoryInput, adminReviewMemo].forEach(function (element) {
      setFieldInvalid(element, false);
    });

    requestFinalTargetSelector?.querySelectorAll("select").forEach(function (element) {
      setFieldInvalid(element, false);
    });
  }

  function validateAdminAction(action) {
    if (isSubmittingReview) return false;

    clearValidationState();

    const typeValue = safeDatasetValue(requestTypeInput?.value);
    const categoryValue = safeDatasetValue(categoryInput?.value);
    const memoValue = safeDatasetValue(adminReviewMemo?.value);
    const currentRequestId = safeDatasetValue(reviewForm?.dataset?.currentRequestId);

    if (!typeValue) {
      alert("자료 유형을 입력해 주세요.");
      setFieldInvalid(requestTypeInput, true);
      requestTypeInput?.focus();
      return false;
    }

    if (!categoryValue) {
      alert("카테고리를 입력해 주세요.");
      setFieldInvalid(categoryInput, true);
      categoryInput?.focus();
      return false;
    }

    const orgValidation = requestOrgSelector?.validate();
    if (orgValidation && !orgValidation.valid) {
      alert(orgValidation.message);
      return false;
    }

    if (!memoValue) {
      alert("관리자 검토 메모를 입력해 주세요.");
      setFieldInvalid(adminReviewMemo, true);
      adminReviewMemo?.focus();
      return false;
    }

    if (!reviewForm || !reviewStatusInput) {
      alert("승인 또는 반려를 처리할 수 없습니다.");
      return false;
    }

    if (!currentRequestId) {
      alert("요청 ID가 없어 승인 또는 반려를 처리할 수 없습니다.");
      return false;
    }

    reviewStatusInput.value = action;
    setReviewSubmittingState(action);
    reviewForm.action = reviewForm.dataset.reviewBaseUrl.replace(/\/$/, "") + "/" + encodeURIComponent(currentRequestId) + "/review";

    try {
      if (typeof reviewForm.requestSubmit === "function") {
        reviewForm.requestSubmit();
      } else {
        reviewForm.submit();
      }
    } catch (error) {
      resetReviewSubmittingState();
      throw error;
    }

    return true;
  }

  // ------------------------------------------------------------
  // 문서/RAG 상세 상태 표시
  // ------------------------------------------------------------
  function applyDocumentStatusBadge(status, label) {
    const badges = [documentManagementTopStatusBadge, documentManagementStatusBadge].filter(Boolean);
    const normalizedStatus = safeDatasetValue(status).toUpperCase();

    badges.forEach(function (badge) {
      badge.className = "badge";

      if (normalizedStatus === "CHUNK_FAILED" || normalizedStatus === "EMBED_FAILED" || normalizedStatus === "REJECTED") {
        badge.classList.add("text-bg-danger");
      } else if (normalizedStatus === "PUBLISHED") {
        badge.classList.add("text-bg-success");
      } else if (normalizedStatus === "APPROVAL_PENDING") {
        badge.classList.add("text-bg-warning");
      } else if (normalizedStatus === "CHUNKING" || normalizedStatus === "EMBEDDING") {
        badge.classList.add("text-bg-primary");
      } else if (normalizedStatus === "UPLOADED" || normalizedStatus === "APPROVED") {
        badge.classList.add("text-bg-success");
      } else {
        badge.classList.add("text-bg-secondary");
      }

      badge.textContent = safeDatasetValue(label, "-");
    });
  }

  function getDocumentStageItems(status) {
    const normalized = safeDatasetValue(status).toUpperCase();
    const stageItems = [
      { key: "UPLOADED", label: "업로드 완료" },
      { key: "CHUNKING", label: "청크 분할" },
      { key: "EMBEDDING", label: "임베딩 진행" },
      { key: "APPROVAL_PENDING", label: "임베딩 완료" }
    ];

    return stageItems.map(function (item) {
      let active = item.key === normalized;

      if (!active && normalized === "PUBLISHED" && item.key === "APPROVAL_PENDING") {
        active = true;
      }

      if (normalized === "CHUNK_FAILED" || normalized === "EMBED_FAILED") {
        active = false;
      }

      return {
        label: item.label,
        active: active
      };
    });
  }

  function renderDocumentStages(status) {
    if (!documentManagementStageWrap) return;

    documentManagementStageWrap.innerHTML = "";
    const normalized = safeDatasetValue(status).toUpperCase();
    const items = getDocumentStageItems(status);

    items.forEach(function (item) {
      const badge = document.createElement("span");
      badge.className = "document-management-stage-badge";
      badge.classList.add(item.active ? "is-active" : "is-idle");
      badge.textContent = item.label;
      documentManagementStageWrap.appendChild(badge);
    });

    if (normalized === "PUBLISHED") {
      const published = document.createElement("span");
      published.className = "document-management-stage-badge is-active";
      published.textContent = "반영 완료";
      documentManagementStageWrap.appendChild(published);
    } else if (normalized === "CHUNK_FAILED" || normalized === "EMBED_FAILED") {
      const failure = document.createElement("span");
      failure.className = "document-management-stage-badge is-failure";
      failure.textContent = "처리 실패";
      documentManagementStageWrap.appendChild(failure);
    }
  }

  function getDocumentActivationButtonState(status) {
    const normalized = safeDatasetValue(status).toUpperCase();

    switch (normalized) {
      case "UPLOADED":
        return { text: "처리 시작", disabled: false };
      case "CHUNKING":
      case "EMBEDDING":
        return { text: "처리 중", disabled: true };
      case "CHUNK_FAILED":
      case "EMBED_FAILED":
        return { text: "재처리", disabled: false };
      case "APPROVAL_PENDING":
        return { text: "활성화", disabled: false };
      case "PUBLISHED":
        return { text: "비활성화", disabled: false };
      default:
        return { text: "상태 변경", disabled: true };
    }
  }

  function applyDocumentActivationButtonState(status, documentId) {
    if (!documentManagementToggleActiveButton) return;

    const state = getDocumentActivationButtonState(status);
    documentManagementToggleActiveButton.textContent = state.text;
    documentManagementToggleActiveButton.disabled = state.disabled;
    documentManagementToggleActiveButton.dataset.documentId = safeDatasetValue(documentId);
    documentManagementToggleActiveButton.dataset.status = safeDatasetValue(status).toUpperCase();
  }

  function buildCsrfHeaders() {
    const headers = {};
    const csrfMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    const csrfInput = document.querySelector('input[name="_csrf"]');
    const token = csrfMeta?.getAttribute("content") || csrfInput?.value || "";
    const headerName = csrfHeaderMeta?.getAttribute("content") || "X-CSRF-TOKEN";

    if (token) {
      headers[headerName] = token;
    }

    return headers;
  }

  function renderChunkDetailCards(items) {
    if (!documentManagementChunkDetailBody) return;

    documentManagementChunkDetailBody.innerHTML = "";

    if (!items.length) {
      documentManagementChunkDetailBody.innerHTML = '<div class="text-muted small">등록된 청크 상세 데이터가 없습니다.</div>';
      return;
    }

    items.forEach(function (item) {
      const card = document.createElement("div");
      card.className = "document-management-detail-card";
      card.innerHTML = [
        '<div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">',
        '<div class="fw-bold">청크 ' + escapeHtml(item.chunkNo ?? "-") + '</div>',
        '<div class="text-muted small">' + escapeHtml(item.hasVector ? "벡터 있음" : "벡터 없음") + '</div>',
        '</div>',
        '<div class="small text-muted mt-1">섹션 제목: ' + escapeHtml(item.sectionTitle || "-") + '</div>',
        '<div class="small text-muted">토큰 수: ' + escapeHtml(item.tokenCount ?? 0) + '</div>',
        '<div class="small text-muted">임베딩 모델: ' + escapeHtml(item.modelName || "-") + '</div>',
        '<div class="small text-muted">차원: ' + escapeHtml(item.dimension ?? "-") + '</div>',
        '<div class="mt-2 text-break" style="white-space: pre-wrap; max-height: 140px; overflow: auto;">' + escapeHtml(item.contentPreview || "-") + '</div>'
      ].join("");
      documentManagementChunkDetailBody.appendChild(card);
    });
  }

  function renderVectorDetailCards(items) {
    if (!documentManagementVectorDetailBody) return;

    documentManagementVectorDetailBody.innerHTML = "";

    if (!items.length) {
      documentManagementVectorDetailBody.innerHTML = '<div class="text-muted small">등록된 벡터 상세 데이터가 없습니다.</div>';
      return;
    }

    items.forEach(function (item) {
      const card = document.createElement("div");
      card.className = "document-management-detail-card";
      card.innerHTML = [
        '<div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">',
        '<div class="fw-bold">청크 ' + escapeHtml(item.chunkNo ?? "-") + ' 벡터</div>',
        '<div class="text-muted small">' + escapeHtml(item.dimension ?? "-") + '차원</div>',
        '</div>',
        '<div class="small text-muted mt-1">벡터 ID: ' + escapeHtml(item.vectorId || "-") + '</div>',
        '<div class="small text-muted">청크 ID: ' + escapeHtml(item.chunkId || "-") + '</div>',
        '<div class="small text-muted">섹션 제목: ' + escapeHtml(item.sectionTitle || "-") + '</div>',
        '<div class="small text-muted">임베딩 모델: ' + escapeHtml(item.modelName || "-") + '</div>'
      ].join("");
      documentManagementVectorDetailBody.appendChild(card);
    });
  }

  // ------------------------------------------------------------
  // 모달 데이터 주입
  // ------------------------------------------------------------
  function populateDetailModal(button) {
    const data = button?.dataset || {};

    setEditableInputValue(titleInput, data.title);
    setEditableInputValue(requestTypeInput, data.requestType);
    setEditableInputValue(categoryInput, data.category);
    setReadonlyValue(requesterInput, data.requesterName);
    setReadonlyValue(createdAtInput, data.createdAt);
    setReadonlyValue(reasonInput, data.reason);
    setReadonlyValue(sampleQuestionInput, data.sampleQuestion);

    const accessLevelText = accessLevelLabels[safeDatasetValue(data.accessLevel)] || safeDatasetValue(data.accessLevel, "-");
    setReadonlyValue(userHopeAccessLevelInput, accessLevelText);
    setDisplayValue(userHopeConditionInput, buildHopeConditionText(data.targetDept, data.accessLevel));
    showReferenceValue(data.referenceUrl);

    if (adminNameInput) {
      adminNameInput.value = getCurrentAdminName();
    }

    if (adminReviewMemo) {
      adminReviewMemo.value = safeDatasetValue(data.adminComment);
    }

    if (reviewForm && reviewForm.dataset.reviewBaseUrl && data.requestId) {
      reviewForm.dataset.currentRequestId = safeDatasetValue(data.requestId);
      reviewForm.action = reviewForm.dataset.reviewBaseUrl.replace(/\/$/, "") + "/" + encodeURIComponent(safeDatasetValue(data.requestId)) + "/review";
    } else if (reviewForm) {
      reviewForm.dataset.currentRequestId = "";
      reviewForm.removeAttribute("action");
    }

    if (reviewStatusInput) {
      reviewStatusInput.value = "";
    }

    resetReviewButtonLabels();
    addSuggestionValue(requestTypeSuggestions, data.requestType);
    addSuggestionValue(categorySuggestions, data.category);
    requestOrgSelector?.init(data.targetDept);
  }

  function populateDocumentManagementModal(button) {
    const data = button?.dataset || {};

    currentDocumentChunkDetails = parseJsonArray(data.chunks);
    currentDocumentVectorDetails = parseJsonArray(data.vectors);
    documentManagementInitialIsActive = safeDatasetValue(data.status).toUpperCase() === "PUBLISHED";

    setReadonlyValue(documentManagementDocumentId, data.documentId);
    setReadonlyValue(documentManagementRegisteredAt, data.registeredAt);
    applyDocumentStatusBadge(data.status, data.statusLabel);
    setEditableInputValue(documentManagementTitle, data.title);
    setEditableInputValue(documentManagementType, data.requestType);
    setEditableInputValue(documentManagementCategory, data.category);
    setReadonlyValue(documentManagementApproverName, data.approverName);
    setReadonlyValue(documentManagementChunkCount, data.chunkCount, "0");
    setReadonlyValue(documentManagementVectorCount, data.vectorCount, "0");
    setReadonlyValue(documentManagementRequesterName, data.requesterName);
    setReadonlyValue(documentManagementRequestDate, data.requestDate);
    setReadonlyValue(documentManagementReason, data.reason);
    setReadonlyValue(documentManagementSampleQuestion, data.sampleQuestion);
    showDocumentReferenceValue(data.referenceUrl);
    setDisplayValue(documentManagementSummary, data.summary, "본문 미리보기가 없습니다.");
    setEditableInputValue(documentManagementAdminComment, data.adminComment);
    setDisplayValue(documentManagementHopeCondition, buildHopeConditionText(data.targetDept, data.accessLevel));
    setDisplayValue(documentManagementAppliedTargetDept, data.targetDept);
    setDisplayValue(documentManagementAppliedAdminComment, data.adminComment);
    setDisplayValue(documentManagementAppliedUpdatedAt, data.permissionUpdatedAt || data.updatedAt || data.registeredAt);
    setReadonlyValue(documentManagementAdminName, data.approverName);
    setDisplayValue(documentManagementFailureReason, data.failureReason, "실패 사유가 기록되지 않았습니다.");
    setReadonlyValue(documentManagementStatusLabel, data.statusLabel);
    setReadonlyValue(documentManagementFailureStatusLabel, data.statusLabel);
    setReadonlyValue(documentManagementFailureAt, data.failureAt || data.updatedAt || data.registeredAt);

    renderChunkDetailCards(currentDocumentChunkDetails);
    renderVectorDetailCards(currentDocumentVectorDetails);
    renderDocumentStages(data.status);
    documentOrgSelector?.init(data.targetDept);

    const normalizedStatus = safeDatasetValue(data.status).toUpperCase();
    const isFailedStage = normalizedStatus === "CHUNK_FAILED" || normalizedStatus === "EMBED_FAILED";

    documentManagementFailureSection?.classList.toggle("d-none", !isFailedStage);

    if (!isFailedStage) {
      setDisplayValue(documentManagementFailureReason, "-");
      setReadonlyValue(documentManagementFailureStatusLabel, "-");
      setReadonlyValue(documentManagementFailureAt, "-");
    }

    if (documentManagementChunkDetailWrap) documentManagementChunkDetailWrap.classList.add("d-none");
    if (documentManagementVectorDetailWrap) documentManagementVectorDetailWrap.classList.add("d-none");

    const chunkToggle = documentManagementModal?.querySelector('[data-detail-target="chunk"]');
    const vectorToggle = documentManagementModal?.querySelector('[data-detail-target="vector"]');

    if (chunkToggle) chunkToggle.disabled = safeDatasetValue(data.chunkCount, "0") === "0";
    if (vectorToggle) vectorToggle.disabled = safeDatasetValue(data.vectorCount, "0") === "0";

    applyDocumentActivationButtonState(data.status, data.documentId);

    documentManagementEditState = {
      documentId: safeDatasetValue(data.documentId),
      initial: buildDocumentManagementInitialState(data)
    };

    updateDocumentManagementSaveButtonState();
  }

  // ------------------------------------------------------------
  // 이벤트 바인딩
  // ------------------------------------------------------------
  requestDetailModal.addEventListener("show.bs.modal", function (event) {
    const trigger = event.relatedTarget;
    if (!trigger || !trigger.classList.contains("js-knowledge-request-detail")) return;
    populateDetailModal(trigger);
  });

  requestDetailModal.addEventListener("hidden.bs.modal", function () {
    closeAllAutocomplete();
    clearValidationState();
    resetReviewSubmittingState();

    if (reviewStatusInput) reviewStatusInput.value = "";
    if (adminNameInput) adminNameInput.value = "-";
    requestOrgSelector?.reset();

    if (reviewForm) {
      reviewForm.dataset.currentRequestId = "";
      reviewForm.removeAttribute("action");
    }
  });

  uploadDocumentModal?.addEventListener("show.bs.modal", function () {
    resetUploadDocumentForm();
    uploadOrgSelector?.init("");
  });

  uploadDocumentModal?.addEventListener("hidden.bs.modal", function () {
    resetUploadDocumentForm();
  });

  uploadDocumentForm?.addEventListener("submit", function (event) {
    if (!validateUploadDocumentForm()) {
      event.preventDefault();
      return;
    }

    setUploadSubmittingState();
  });

  [
    uploadDocumentTitle,
    uploadDocumentType,
    uploadDocumentCategory,
    uploadDocumentReason,
    uploadDocumentSampleQuestion,
    uploadDocumentReferenceUrl
  ].forEach(function (element) {
    element?.addEventListener("input", function () {
      setFieldInvalid(element, false);
    });
    element?.addEventListener("change", function () {
      setFieldInvalid(element, false);
    });
  });

  documentManagementModal?.addEventListener("show.bs.modal", function (event) {
    const trigger = event.relatedTarget;
    if (!trigger || !trigger.classList.contains("js-document-management-detail")) return;
    populateDocumentManagementModal(trigger);
  });

  documentManagementModal?.addEventListener("hidden.bs.modal", function () {
    [
      documentManagementDocumentId,
      documentManagementRegisteredAt,
      documentManagementApproverName,
      documentManagementChunkCount,
      documentManagementVectorCount,
      documentManagementRequesterName,
      documentManagementRequestDate,
      documentManagementReason,
      documentManagementSampleQuestion
    ].forEach(function (element) {
      setReadonlyValue(element, "-");
    });

    [
      documentManagementTitle,
      documentManagementType,
      documentManagementCategory,
      documentManagementAdminComment
    ].forEach(function (element) {
      setEditableInputValue(element, "-");
    });

    if (documentManagementTopStatusBadge) {
      documentManagementTopStatusBadge.className = "badge text-bg-secondary";
      documentManagementTopStatusBadge.textContent = "-";
    }

    if (documentManagementStatusBadge) {
      documentManagementStatusBadge.className = "badge text-bg-secondary";
      documentManagementStatusBadge.textContent = "-";
    }

    setDisplayValue(documentManagementSummary, "-");
    showDocumentReferenceValue("");
    setDisplayValue(documentManagementHopeCondition, "-");
    setDisplayValue(documentManagementAppliedTargetDept, "-");
    setDisplayValue(documentManagementAppliedAdminComment, "-");
    setDisplayValue(documentManagementAppliedUpdatedAt, "-");
    setReadonlyValue(documentManagementAdminName, "-");
    setDisplayValue(documentManagementFailureReason, "-");
    setReadonlyValue(documentManagementStatusLabel, "-");
    setReadonlyValue(documentManagementFailureStatusLabel, "-");
    setReadonlyValue(documentManagementFailureAt, "-");

    if (documentManagementFailureSection) documentManagementFailureSection.classList.add("d-none");
    if (documentManagementStageWrap) documentManagementStageWrap.innerHTML = "";
    if (documentManagementChunkDetailWrap) documentManagementChunkDetailWrap.classList.add("d-none");
    if (documentManagementVectorDetailWrap) documentManagementVectorDetailWrap.classList.add("d-none");
    if (documentManagementChunkDetailBody) documentManagementChunkDetailBody.innerHTML = "";
    if (documentManagementVectorDetailBody) documentManagementVectorDetailBody.innerHTML = "";

    currentDocumentChunkDetails = [];
    currentDocumentVectorDetails = [];
    documentManagementInitialIsActive = false;
    documentManagementEditState = null;
    documentOrgSelector?.reset();

    if (documentManagementToggleActiveButton) {
      documentManagementToggleActiveButton.textContent = "활성화 / 비활성화";
      documentManagementToggleActiveButton.disabled = false;
      documentManagementToggleActiveButton.dataset.documentId = "";
      documentManagementToggleActiveButton.dataset.status = "";
    }

    updateDocumentManagementSaveButtonState();
  });

  [titleInput, requestTypeInput, categoryInput, adminReviewMemo].forEach(function (element) {
    element?.addEventListener("input", function () {
      setFieldInvalid(element, false);
    });
  });

  reviewForm?.addEventListener("submit", function (event) {
    if (isSubmittingReview) return;

    const currentAction = safeDatasetValue(reviewStatusInput?.value);
    if (!currentAction) {
      event.preventDefault();
      return;
    }

    setReviewSubmittingState(currentAction);
  });

  approveBtn?.addEventListener("click", function () {
    validateAdminAction("APPROVED");
  });

  rejectBtn?.addEventListener("click", function () {
    validateAdminAction("REJECTED");
  });

  documentManagementToggleActiveButton?.addEventListener("click", async function () {
    const documentId = safeDatasetValue(documentManagementToggleActiveButton.dataset.documentId);
    const baseUrl = safeDatasetValue(documentManagementToggleActiveButton.dataset.activationBaseUrl);

    if (!documentId || !baseUrl) {
      alert("문서 상태 변경 대상 정보를 찾을 수 없습니다.");
      return;
    }

    const previousText = documentManagementToggleActiveButton.textContent;
    documentManagementToggleActiveButton.disabled = true;
    documentManagementToggleActiveButton.textContent = "처리 중...";

    try {
      const response = await fetch(baseUrl.replace(/\/$/, "") + "/" + encodeURIComponent(documentId) + "/activation", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...buildCsrfHeaders()
        },
        credentials: "same-origin"
      });

      let payload = null;
      try {
        payload = await response.json();
      } catch (error) {
        payload = null;
      }

      if (!response.ok || !payload?.success) {
        throw new Error(payload?.message || "문서 상태 변경에 실패했습니다.");
      }

      alert(payload.message || "문서 상태가 변경되었습니다.");
      window.location.reload();
    } catch (error) {
      alert(error?.message || "문서 상태 변경 중 오류가 발생했습니다.");
      documentManagementToggleActiveButton.textContent = previousText;
      documentManagementToggleActiveButton.disabled = getDocumentActivationButtonState(
        documentManagementToggleActiveButton.dataset.status
      ).disabled;
    }
  });

  [
    documentManagementTitle,
    documentManagementType,
    documentManagementCategory,
    documentManagementAdminComment
  ].forEach(function (element) {
    element?.addEventListener("input", updateDocumentManagementSaveButtonState);
    element?.addEventListener("change", updateDocumentManagementSaveButtonState);
  });

  documentManagementFinalTargetSelector?.addEventListener("change", updateDocumentManagementSaveButtonState);
  documentManagementFinalTargetSelector?.addEventListener("input", updateDocumentManagementSaveButtonState);

  documentManagementSaveButton?.addEventListener("click", async function () {
    if (documentManagementSaveButton.disabled) return;
    if (!documentManagementEditState?.documentId) {
      alert("문서 정보를 확인할 수 없습니다.");
      return;
    }

    const payload = collectDocumentManagementEditableState();
    const baseUrl = safeDatasetValue(documentManagementSaveButton.dataset.detailBaseUrl);

    if (!payload.title) {
      alert("문서명을 입력해 주세요.");
      documentManagementTitle?.focus();
      return;
    }

    if (!payload.requestType) {
      alert("자료 유형을 입력해 주세요.");
      documentManagementType?.focus();
      return;
    }

    if (!payload.category) {
      alert("카테고리를 입력해 주세요.");
      documentManagementCategory?.focus();
      return;
    }

    if (!payload.targetDept) {
      alert("최종 권한 조건을 선택해 주세요.");
      return;
    }

    if (!baseUrl) {
      alert("저장 요청 경로를 확인할 수 없습니다.");
      return;
    }

    const previousDisabled = documentManagementSaveButton.disabled;
    documentManagementSaveButton.disabled = true;

    try {
      const params = new URLSearchParams();
      params.set("title", payload.title);
      params.set("requestType", payload.requestType);
      params.set("category", payload.category);
      params.set("targetDept", payload.targetDept);
      params.set("adminComment", payload.adminComment);

      const response = await fetch(baseUrl + "/" + encodeURIComponent(documentManagementEditState.documentId) + "/detail", {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
          ...buildCsrfHeaders()
        },
        credentials: "same-origin",
        body: params.toString()
      });

      let result = null;
      try {
        result = await response.json();
      } catch (error) {
        result = null;
      }

      if (!response.ok || !result?.success) {
        throw new Error(result?.message || "문서 정보를 저장하지 못했습니다.");
      }

      alert(result.message || "저장되었습니다.");
      window.location.reload();
    } catch (error) {
      alert(error?.message || "문서 정보를 저장하지 못했습니다.");
      documentManagementSaveButton.disabled = previousDisabled;
      updateDocumentManagementSaveButtonState();
    }
  });

  documentManagementModal?.addEventListener("click", function (event) {
    const toggleButton = event.target?.closest(".js-document-detail-toggle");
    if (!toggleButton || !documentManagementModal.contains(toggleButton)) return;

    const target = safeDatasetValue(toggleButton.dataset.detailTarget);

    if (target === "chunk" && documentManagementChunkDetailWrap) {
      documentManagementChunkDetailWrap.classList.toggle("d-none");
      if (!documentManagementChunkDetailWrap.classList.contains("d-none")) {
        renderChunkDetailCards(currentDocumentChunkDetails);
      }
    }

    if (target === "vector" && documentManagementVectorDetailWrap) {
      documentManagementVectorDetailWrap.classList.toggle("d-none");
      if (!documentManagementVectorDetailWrap.classList.contains("d-none")) {
        renderVectorDetailCards(currentDocumentVectorDetails);
      }
    }
  });
});
