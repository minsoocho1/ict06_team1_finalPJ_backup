/**
 * @FileName : TemplateScreen.js
 * @Description : AiSecretary.js 전용 템플릿 생성 화면
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.13  송혜진       템플릿 생성 조건 변경
 */

import React, { useEffect, useMemo, useState } from "react";
import AppButton from "../components/AppButton";
import Chip from "../components/Chip";
import DepartmentTeamSelector from "../components/DepartmentTeamSelector";
import Field from "../components/Field";
import TextInput from "../components/TextInput";
import { docMeta, templateCards } from "../constants/aiSecretaryData";
import { I, Icon } from "../constants/aiSecretaryIcons";
import { C, styles } from "../styles/aiSecretaryTheme";
import {
  createAssistantTemplate,
  createTemplateRequest,
  getMyTemplateRequests,
  unwrapApiData,
} from "../api/aiSecretaryApi";

const PAGE_SIZE = 2;
const TABS = [
  { key: "REPORT", icon: I.file },
  { key: "MINUTES", icon: I.users },
  { key: "APPROVAL", icon: I.check },
  { key: "template", icon: I.spark },
];
const TYPE_OPTIONS = ["REPORT", "MINUTES", "APPROVAL"];
const toneOptions = ["공식적", "친근함", "간결함"];
const hiddenIncludeOptions = {
  includeTitle: true,
  includeParagraphs: true,
  includeSignature: false,
};
const highlightedSectionTitle = {
  ...styles.sectionTitle,
  color: C.accent,
};
const categoryByType = {
  REPORT: "보고",
  MINUTES: "회의록",
  APPROVAL: "결재",
};

const safeTemplateCards = Array.isArray(templateCards) ? templateCards : [];

function createEmptyDeptSelection() {
  return {
    headquarterId: "",
    headquarterName: "",
    teamIds: [],
    teamNames: [],
    displayName: "",
  };
}

function normalizeText(value) {
  return String(value || "").trim().toLowerCase();
}

function normalizeDeptLabel(value) {
  return String(value || "")
    .split(",")
    .map((part) => part.trim())
    .map((part) => part.split(">").pop().trim())
    .filter(Boolean)
    .join(", ");
}

function normalizeOrganizationSeed(seed) {
  if (!seed) {
    return null;
  }

  if (typeof seed === "string") {
    const deptText = normalizeDeptLabel(seed);
    return deptText ? { deptText } : null;
  }

  if (typeof seed !== "object") {
    return null;
  }

  const headquarterId = String(
    seed.headquarterId ||
      seed.headquarter?.deptId ||
      seed.headquarter?.id ||
      seed.headquarter?.dept_id ||
      ""
  ).trim();
  const headquarterName = normalizeDeptLabel(
    seed.headquarterName ||
      seed.headquarter?.deptName ||
      seed.headquarter?.name ||
      seed.headquarter?.displayName ||
      ""
  );
  const teamIds = Array.isArray(seed.teamIds)
    ? seed.teamIds.map((teamId) => String(teamId || "").trim()).filter(Boolean)
    : [];
  const teamNames = Array.isArray(seed.teamNames)
    ? seed.teamNames.map((name) => normalizeDeptLabel(name)).filter(Boolean)
    : [];
  const displayName = normalizeDeptLabel(
    seed.displayName || teamNames.join(", ") || seed.deptText || ""
  );
  const deptText = normalizeDeptLabel(seed.deptText || displayName);

  if (
    !headquarterId &&
    !headquarterName &&
    teamIds.length === 0 &&
    teamNames.length === 0 &&
    !displayName &&
    !deptText
  ) {
    return null;
  }

  return {
    headquarterId,
    headquarterName,
    teamIds,
    teamNames,
    displayName,
    deptText,
  };
}

function splitDeptNames(value) {
  return normalizeDeptLabel(value)
    .split(",")
    .map((part) => part.trim())
    .filter(Boolean);
}

function getDeptDisplayName(selection) {
  if (Array.isArray(selection?.teamNames) && selection.teamNames.length > 0) {
    return normalizeDeptLabel(selection.teamNames.join(", "));
  }

  return normalizeDeptLabel(selection?.displayName);
}

function matchesDeptSelection(cardDept, selection) {
  const selectedNames = splitDeptNames(getDeptDisplayName(selection));
  if (selectedNames.length === 0) {
    return true;
  }

  const cardNames = splitDeptNames(cardDept);
  if (cardNames.length === 0) {
    return false;
  }

  const selectedSet = new Set(selectedNames);
  const cardSet = new Set(cardNames);

  return (
    selectedNames.some((name) => cardSet.has(name)) ||
    cardNames.some((name) => selectedSet.has(name))
  );
}

function includesAnyText(targets, keyword) {
  const trimmed = normalizeText(keyword);
  if (!trimmed) {
    return true;
  }

  return targets
    .filter(Boolean)
    .join(" ")
    .toLowerCase()
    .includes(trimmed);
}

function toDbType(type) {
  const normalized = String(type || "").trim().toUpperCase();
  if (normalized === "MINUTES") return "MINUTES";
  if (normalized === "APPROVAL") return "APPROVAL";
  return "REPORT";
}

function getCategoryByType(type) {
  return categoryByType[String(type || "").toUpperCase()] || "보고";
}

function normalizeRecentRequest(request) {
  const dept = normalizeDeptLabel(request.dept);
  return {
    requestId: request.requestId,
    type: toDbType(request.type),
    category: request.category || "",
    dept,
    situation: request.situation || "",
    tone: request.tone || "공식적",
    title: request.title || "제목 없는 템플릿",
    tag: request.statusLabel || "요청",
    desc:
      request.description ||
      `${dept || "공통"} 영역의 ${
        request.situation || "업무 문서"
      } 템플릿입니다.`,
    preview: Array.isArray(request.preview) ? request.preview : [],
    generatedContent: request.content || "",
    status: request.status,
    statusLabel: request.statusLabel,
    reflected: request.reflected,
    adminComment: request.adminComment,
    createdAt: request.createdAt,
    templateFilters: {
      category: request.category,
      dept,
      situation: request.situation,
      tone: request.tone,
    },
  };
}

function sameTemplateRequest(request, card) {
  const sameTitle =
    normalizeText(request.title) === normalizeText(card.title);
  const sameCategory =
    normalizeText(request.category) === normalizeText(card.category);
  const sameDept =
    normalizeText(normalizeDeptLabel(request.dept)) ===
    normalizeText(normalizeDeptLabel(card.dept));
  const sameSituation =
    normalizeText(request.situation) === normalizeText(card.situation);
  const activeStatus =
    request.status === "PENDING" || request.status === "APPROVED";

  return sameTitle && sameCategory && sameDept && sameSituation && activeStatus;
}

function buildTemplateSeed(card) {
  const title = String(card?.title || "").trim();
  const purpose = String(card?.situation || "").trim();
  const rawDescription = String(card?.description || card?.desc || "").trim();
  const description =
    rawDescription && rawDescription !== "설명이 없습니다."
      ? rawDescription
      : "";
  const content = String(card?.content || card?.generatedContent || "").trim();
  const previewList = Array.isArray(card?.preview)
    ? card.preview.map((item) => String(item || "").trim()).filter(Boolean)
    : [];

  const detailParts = [];

  if (description) {
    detailParts.push(`설명:\n${description}`);
  }

  if (content && content !== description) {
    detailParts.push(content);
  }

  if (previewList.length > 0) {
    detailParts.push(`템플릿 구성:\n${previewList.join("\n")}`);
  }

  const organizationSeed = normalizeOrganizationSeed(
    card?.templateFilters?.generation?.relatedDept ||
      card?.relatedDept ||
      card?.deptMeta
  );
  const deptText = normalizeDeptLabel(
    organizationSeed?.displayName || card?.dept || ""
  );

  return {
    type: toDbType(card?.type || "REPORT"),
    title,
    purpose,
    detail: detailParts.join("\n\n").trim() || description || content || "",
    amount: "보통",
    audience: "",
    targets: [],
    referenceFiles: [],
    referenceMemo: "",
    organizationSeed,
    deptText,
  };
}

function getRequestStatusLabel(status) {
  switch (status) {
    case "APPROVED":
      return "반영 완료";
    case "REJECTED":
      return "반려";
    case "CANCELLED":
      return "취소됨";
    default:
      return "대기";
  }
}

function getRequestStatusStyle(status) {
  switch (status) {
    case "APPROVED":
      return { background: C.softGreen, color: C.success };
    case "REJECTED":
      return { background: "#FEF2F2", color: "#DC2626" };
    case "CANCELLED":
      return { background: "#F3F4F6", color: C.sub };
    default:
      return { background: C.accentBg, color: C.accent };
  }
}

export default function TemplateScreen({ empNo, onStartTemplate, onOpenForm }) {
  const [viewMode, setViewMode] = useState("recommended");
  const [templateError, setTemplateError] = useState("");
  const [requestModalOpen, setRequestModalOpen] = useState(false);
  const [generatingTemplate, setGeneratingTemplate] = useState(false);
  const [templateRequests, setTemplateRequests] = useState([]);
  const [generatedTemplates, setGeneratedTemplates] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [generatedPage, setGeneratedPage] = useState(1);

  const [searchFilters, setSearchFilters] = useState({
    templateName: "",
    relatedDept: createEmptyDeptSelection(),
    situation: "",
  });

  const [generationFilters, setGenerationFilters] = useState({
    type: "REPORT",
    templateName: "",
    relatedDept: createEmptyDeptSelection(),
    purpose: "",
    detail: "",
    tone: "공식적",
  });

  const filteredTemplateCards = useMemo(() => {
    return safeTemplateCards.filter((card) => {
      const templateNameMatched = includesAnyText(
        [card.title],
        searchFilters.templateName
      );

      const deptMatched = matchesDeptSelection(
        card.dept,
        searchFilters.relatedDept
      );

      const situationMatched = includesAnyText(
        [card.situation, card.desc, card.description, ...(card.preview || [])],
        searchFilters.situation
      );

      return templateNameMatched && deptMatched && situationMatched;
    });
  }, [searchFilters]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchFilters.templateName, searchFilters.relatedDept, searchFilters.situation]);

  const totalPages = Math.max(
    1,
    Math.ceil((filteredTemplateCards?.length || 0) / PAGE_SIZE)
  );

  const pagedTemplateCards = (filteredTemplateCards || []).slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE
  );

  const generatedTotalPages = Math.max(
    1,
    Math.ceil((generatedTemplates?.length || 0) / PAGE_SIZE)
  );

  const pagedGeneratedTemplates = (generatedTemplates || []).slice(
    (generatedPage - 1) * PAGE_SIZE,
    generatedPage * PAGE_SIZE
  );

  useEffect(() => {
    if (generatedPage > generatedTotalPages) {
      setGeneratedPage(generatedTotalPages);
    }
  }, [generatedPage, generatedTotalPages]);

  const loadMyTemplateRequests = async () => {
    if (!empNo) {
      return;
    }

    try {
      const response = await getMyTemplateRequests(empNo);
      const data = unwrapApiData(response) ?? [];
      const requestList = Array.isArray(data) ? data : [];

      setTemplateRequests(requestList);
      setGeneratedTemplates(requestList.map(normalizeRecentRequest));
    } catch (error) {
      console.error("추천 템플릿 요청 목록 조회 실패", error);
      setTemplateError("추천 템플릿 요청 목록을 불러오지 못했습니다.");
      setTemplateRequests([]);
      setGeneratedTemplates([]);
    }
  };

  useEffect(() => {
    loadMyTemplateRequests();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [empNo]);

  const handleTopTabClick = (tab) => {
    if (tab !== "template") {
      onOpenForm(tab);
    }
  };

  const handleGenerateTemplate = async () => {
    if (generatingTemplate) {
      return;
    }

    if (!empNo) {
      setTemplateError("사용자 정보를 찾을 수 없습니다. 다시 로그인해 주세요.");
      return;
    }

    setGeneratingTemplate(true);
    setTemplateError("");

    try {
      const type = generationFilters.type || "REPORT";
      const category = getCategoryByType(type);
      const title = generationFilters.templateName || "템플릿명 없음";
      const dept = getDeptDisplayName(generationFilters.relatedDept) || "공통";
      const situation = generationFilters.purpose || "작성 목적 없음";
      const description = generationFilters.detail || "";
      const tone = generationFilters.tone || "공식적";

      const response = await createAssistantTemplate({
        empNo: String(empNo),
        type,
        category,
        title,
        dept,
        situation,
        description,
        tone,
        ...hiddenIncludeOptions,
      });

      const data = unwrapApiData(response);

      const generatedCard = {
        id: `generated-${Date.now()}`,
        type: toDbType(data?.type || type),
        category: data?.category || category,
        dept: normalizeDeptLabel(data?.dept || dept),
        situation: data?.situation || situation,
        tone: data?.tone || tone,
        title: data?.title || title,
        desc: data?.description || description || "설명이 없습니다.",
        preview: Array.isArray(data?.preview) ? data.preview : [],
        generatedContent: data?.content || "",
        fallback: Boolean(data?.fallback),
        modelName: data?.modelName || "gemini",
        templateFilters: {
          search: searchFilters,
          generation: generationFilters,
          type,
          category,
          title,
          dept,
          situation,
          description,
          tone,
        },
      };

      setGeneratedTemplates((prev) => [generatedCard, ...prev]);
      setGeneratedPage(1);
      setViewMode("generated");
    } catch (error) {
      console.error("AI 템플릿 생성 실패", error);
      const message =
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        "AI 템플릿 생성 중 문제가 발생했습니다.";
      setTemplateError(message);
    } finally {
      setGeneratingTemplate(false);
    }
  };

  const handleRequestAddTemplate = async (card) => {
    if (!card) {
      return;
    }

    if (!empNo) {
      setTemplateError("사용자 정보를 찾을 수 없습니다. 다시 로그인해 주세요.");
      return;
    }

    const alreadyRequested = templateRequests.some((request) =>
      sameTemplateRequest(request, card)
    );

    if (alreadyRequested) {
      setTemplateError("이미 요청 중이거나 반영된 추천 템플릿입니다.");
      setRequestModalOpen(true);
      return;
    }

    try {
      const payload = {
        empNo: String(empNo),
        type: toDbType(card.type),
        category: card.category || "",
        dept: normalizeDeptLabel(card.dept || ""),
        situation: card.situation || "",
        tone: card.tone || generationFilters.tone || "공식적",
        title: card.title || "제목 없는 템플릿",
        description:
          card?.templateFilters?.description ||
          card.description ||
          card.desc ||
          "",
        content:
          card.generatedContent ||
          (Array.isArray(card.preview) ? card.preview.join("\n") : ""),
        preview: Array.isArray(card.preview) ? card.preview : [],
        ...hiddenIncludeOptions,
      };

      await createTemplateRequest(payload);
      await loadMyTemplateRequests();
      setRequestModalOpen(true);
    } catch (error) {
      console.error("추천 템플릿 요청 실패", error);
      const message =
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        "추천 템플릿 요청 중 문제가 발생했습니다.";
      setTemplateError(message);
      setRequestModalOpen(true);
    }
  };

  const handleStartTemplate = (card) => {
    if (!card) {
      return;
    }

    const templateSeed = buildTemplateSeed(card);

    onStartTemplate({
      ...card,
      templateSeed,
      templateFilters: {
        search: searchFilters,
        generation: generationFilters,
      },
    });
  };

  const isRecommendedTab = viewMode === "recommended";
  const isMineTab = viewMode === "generated";

  const renderTemplateCard = (card, isGenerated = false) => (
    <div key={card.id} style={{ ...styles.card, padding: 18 }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          gap: 12,
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
          <div
            style={{
              fontSize: 18,
              fontWeight: 900,
              color: C.text,
              wordBreak: "keep-all",
            }}
          >
            {card.title}
          </div>

          {!isGenerated && (
            <div
              style={{
                minHeight: 28,
                padding: "0 10px",
                borderRadius: 999,
                border: `1px solid ${C.border}`,
                background: "#fff",
                color: C.accent,
                display: "inline-flex",
                alignItems: "center",
                fontSize: 12,
                fontWeight: 800,
                flexShrink: 0,
                maxWidth: 220,
                whiteSpace: "nowrap",
                overflow: "hidden",
                textOverflow: "ellipsis",
              }}
            >
              {normalizeDeptLabel(card.dept) || "공통"}
            </div>
          )}
        </div>

        {isGenerated && (
          <div
            style={{
              height: 28,
              padding: "0 10px",
              borderRadius: 999,
              background: C.accentBg,
              color: C.accent,
              display: "flex",
              alignItems: "center",
              fontSize: 12,
              fontWeight: 800,
              flexShrink: 0,
              maxWidth: 180,
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis",
            }}
          >
            {card.statusLabel || "AI 생성"}
          </div>
        )}
      </div>

      <div
        style={{
          marginTop: 10,
          fontSize: 14,
          color: C.sub,
          lineHeight: 1.6,
        }}
      >
        {isGenerated && (
          <div>
            <strong style={{ color: C.text }}>상황:</strong> {card.situation || "-"}
          </div>
        )}
        <div style={{ marginTop: 10, color: C.text }}>{card.desc}</div>
      </div>

      <div
        style={{
          marginTop: 14,
          padding: 14,
          borderRadius: 10,
          background: "#F8FAFC",
          border: `1px solid ${C.border}`,
          fontSize: 13,
          color: C.sub,
          lineHeight: 1.6,
        }}
      >
        {(card.preview || []).map((line) => (
          <div key={line}>{line}</div>
        ))}
      </div>

      <div style={{ display: "grid", gap: 8, marginTop: 14 }}>
        <AppButton
          variant="secondary"
          style={{ width: "100%" }}
          onClick={() => handleStartTemplate(card)}
        >
          이 템플릿으로 시작
        </AppButton>

        {isGenerated && (
          <AppButton
            variant="secondary"
            style={{ width: "100%" }}
            onClick={() => handleRequestAddTemplate(card)}
          >
            추천 템플릿 목록에 추가 요청
          </AppButton>
        )}
      </div>
    </div>
  );

  return (
    <div style={styles.page}>
      <div style={{ marginBottom: 18 }}>
        <div style={{ fontSize: 16, color: C.sub, fontWeight: 700 }}>AI 비서</div>
        <h1
          style={{
            margin: "6px 0 0",
            fontSize: 38,
            fontWeight: 900,
            letterSpacing: -1,
          }}
        >
          템플릿 생성
        </h1>
        <p style={{ margin: "10px 0 0", color: C.sub, fontSize: 16 }}>
          작성할 템플릿의 이름, 연관 부서, 상황, 설명을 입력하면 AI가
          자연스럽고 일관된 템플릿 초안을 생성합니다.
        </p>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(4, minmax(0, 1fr))",
          gap: 12,
          marginBottom: 18,
        }}
      >
        {TABS.map((tab) => {
          const active = tab.key === "template";
          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => handleTopTabClick(tab.key)}
              style={{
                ...styles.card,
                border: `1px solid ${active ? C.accent : C.border}`,
                background: active ? C.accentBg : "#fff",
                minHeight: 92,
                textAlign: "left",
                padding: 18,
                cursor: active ? "default" : "pointer",
                color: C.text,
              }}
            >
              <div
                style={{
                  color: active ? C.accent : C.sub,
                  display: "flex",
                  alignItems: "center",
                  gap: 10,
                }}
              >
                <Icon>{tab.icon}</Icon>
                <span style={{ fontSize: 15, fontWeight: 800 }}>
                  {docMeta?.[tab.key]?.label || tab.key}
                </span>
              </div>
              <div
                style={{
                  marginTop: 8,
                  fontSize: 12,
                  color: C.sub,
                  lineHeight: 1.5,
                }}
              >
                {docMeta?.[tab.key]?.description || ""}
              </div>
            </button>
          );
        })}
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "440px 1fr",
          gap: 18,
          alignItems: "start",
        }}
      >
        <div style={{ ...styles.card, padding: 22 }}>
          {isRecommendedTab ? (
            <>
              <h3 style={highlightedSectionTitle}>추천 템플릿 검색</h3>
              <div style={{ marginTop: 16, display: "grid", gap: 14 }}>
                <Field label="템플릿명">
                  <TextInput
                    value={searchFilters.templateName}
                    onChange={(e) =>
                      setSearchFilters((prev) => ({
                        ...prev,
                        templateName: e.target.value,
                      }))
                    }
                  />
                </Field>

                <Field label="연관 부서">
                  <DepartmentTeamSelector
                    value={searchFilters.relatedDept}
                    onChange={(value) =>
                      setSearchFilters((prev) => ({
                        ...prev,
                        relatedDept: value,
                      }))
                    }
                  />
                </Field>

                <Field label="상황 검색">
                  <TextInput
                    value={searchFilters.situation}
                    onChange={(e) =>
                      setSearchFilters((prev) => ({
                        ...prev,
                        situation: e.target.value,
                      }))
                    }
                  />
                </Field>
              </div>
            </>
          ) : (
            <>
              <h3 style={highlightedSectionTitle}>AI 템플릿 생성</h3>
              <div style={styles.sectionSub}>
                선택한 조건을 바탕으로 AI 템플릿 초안을 생성합니다.
              </div>

              <div style={{ marginTop: 16, display: "grid", gap: 14 }}>
                <Field label="문서 유형">
                  <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                    {TYPE_OPTIONS.map((type) => (
                      <Chip
                        key={type}
                        active={generationFilters.type === type}
                        onClick={() =>
                          setGenerationFilters((prev) => ({
                            ...prev,
                            type,
                          }))
                        }
                      >
                        {docMeta?.[type]?.label || type}
                      </Chip>
                    ))}
                  </div>
                </Field>

                <Field label="템플릿명">
                  <TextInput
                    value={generationFilters.templateName}
                    onChange={(e) =>
                      setGenerationFilters((prev) => ({
                        ...prev,
                        templateName: e.target.value,
                      }))
                    }
                  />
                </Field>

                <Field label="연관 부서">
                  <DepartmentTeamSelector
                    value={generationFilters.relatedDept}
                    onChange={(value) =>
                      setGenerationFilters((prev) => ({
                        ...prev,
                        relatedDept: value,
                      }))
                    }
                  />
                </Field>

                <Field label="작성 목적">
                  <TextInput
                    value={generationFilters.purpose}
                    onChange={(e) =>
                      setGenerationFilters((prev) => ({
                        ...prev,
                        purpose: e.target.value,
                      }))
                    }
                  />
                </Field>

                <Field label="상세 내용">
                  <TextInput
                    textarea
                    value={generationFilters.detail}
                    onChange={(e) =>
                      setGenerationFilters((prev) => ({
                        ...prev,
                        detail: e.target.value,
                      }))
                    }
                  />
                </Field>

                <Field label="톤앤매너">
                  <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                    {toneOptions.map((tone) => (
                      <Chip
                        key={tone}
                        active={generationFilters.tone === tone}
                        onClick={() =>
                          setGenerationFilters((prev) => ({
                            ...prev,
                            tone,
                          }))
                        }
                      >
                        {tone}
                      </Chip>
                    ))}
                  </div>
                </Field>
              </div>

              <AppButton
                style={{
                  width: "100%",
                  marginTop: 20,
                  opacity: generatingTemplate ? 0.7 : 1,
                  cursor: generatingTemplate ? "default" : "pointer",
                }}
                onClick={generatingTemplate ? undefined : handleGenerateTemplate}
              >
                <Icon>{I.spark}</Icon>
                {generatingTemplate ? "AI 템플릿 생성 중..." : "AI 템플릿 생성"}
              </AppButton>

              {templateError && (
                <div
                  style={{
                    marginTop: 10,
                    color: "#d32f2f",
                    fontSize: 13,
                    fontWeight: 700,
                  }}
                >
                  {templateError}
                </div>
              )}
            </>
          )}

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: 8,
              marginTop: 12,
            }}
          >
            <button
              type="button"
              onClick={() => setViewMode("recommended")}
              style={{
                height: 38,
                borderRadius: 10,
                border: `1px solid ${
                  isRecommendedTab ? C.accent : C.border
                }`,
                background: isRecommendedTab ? C.accentBg : "#fff",
                color: isRecommendedTab ? C.accent : C.text,
                fontWeight: 800,
                cursor: "pointer",
              }}
            >
              추천 템플릿 검색            </button>

            <button
              type="button"
              onClick={() => setViewMode("generated")}
              style={{
                height: 38,
                borderRadius: 10,
                border: `1px solid ${isMineTab ? C.accent : C.border}`,
                background: isMineTab ? C.accentBg : "#fff",
                color: isMineTab ? C.accent : C.text,
                fontWeight: 800,
                cursor: "pointer",
              }}
            >
              내 AI 템플릿            </button>
          </div>
        </div>

        <div style={{ ...styles.card, padding: 22 }}>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: 16,
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <h3 style={highlightedSectionTitle}>
                {isMineTab ? "내 AI 템플릿" : "추천 템플릿"}
              </h3>
              <div
                style={{
                  width: 26,
                  height: 26,
                  borderRadius: "50%",
                  background: C.accentBg,
                  color: C.accent,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: 13,
                  fontWeight: 800,
                }}
              >
                {isMineTab ? generatedTemplates.length : filteredTemplateCards.length}
              </div>
              {isMineTab && (
                <button
                  type="button"
                  onClick={() => setRequestModalOpen(true)}
                  style={{
                    height: 30,
                    padding: "0 12px",
                    borderRadius: 999,
                    border: `1px solid ${C.border}`,
                    background: "#fff",
                    color: C.accent,
                    fontSize: 12,
                    fontWeight: 800,
                    cursor: "pointer",
                    display: "inline-flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  요청 현황 보기
                </button>
              )}
            </div>

            <div style={{ fontSize: 14, color: C.sub, fontWeight: 700 }}>
              {isMineTab
                ? `${generatedPage} / ${generatedTotalPages} 페이지`
                : `${currentPage} / ${totalPages} 페이지`}
            </div>
          </div>

          <div
            style={{
              display: "grid",
              gridTemplateColumns:
                isMineTab
                  ? (pagedGeneratedTemplates?.length || 0) === 0
                    ? "1fr"
                    : "1fr 1fr"
                  : (pagedTemplateCards?.length || 0) === 0
                  ? "1fr"
                  : "1fr 1fr",
              gap: 14,
            }}
          >
            {isMineTab ? (
              <>
                {generatedTemplates.length === 0 && (
                  <div
                    style={{
                      padding: 24,
                      border: `1px solid ${C.border}`,
                      borderRadius: 14,
                      color: C.sub,
                      textAlign: "center",
                      background: "#F8FAFC",
                    }}
                  >
                    아직 생성된 AI 템플릿이 없습니다.
                    <br />
                    왼쪽 조건을 입력하고 AI 템플릿 생성을 눌러 주세요.
                  </div>
                )}

                {generatedTemplates.length > 0 &&
                  pagedGeneratedTemplates.map((card) =>
                    renderTemplateCard(card, true)
                  )}
              </>
            ) : (
              <>
                {pagedTemplateCards.length === 0 && (
                  <div
                    style={{
                      padding: 24,
                      border: `1px solid ${C.border}`,
                      borderRadius: 14,
                      color: C.sub,
                      textAlign: "center",
                      background: "#F8FAFC",
                    }}
                  >
                    조건에 맞는 추천 템플릿이 없습니다.
                    <br />
                    검색 조건을 조금 바꿔서 다시 확인해 보세요.
                  </div>
                )}
                {pagedTemplateCards.map((card) =>
                  renderTemplateCard(card, false)
                )}
              </>
            )}
          </div>

          {(isRecommendedTab
            ? filteredTemplateCards.length > PAGE_SIZE
            : generatedTemplates.length > PAGE_SIZE) && (
            <div
              style={{
                display: "flex",
                justifyContent: "center",
                gap: 8,
                marginTop: 18,
              }}
            >
              <AppButton
                variant="secondary"
                style={{ height: 36 }}
                onClick={() => {
                  if (isMineTab) {
                    setGeneratedPage((prev) => Math.max(prev - 1, 1));
                  } else {
                    setCurrentPage((prev) => Math.max(prev - 1, 1));
                  }
                }}
                disabled={isMineTab ? generatedPage === 1 : currentPage === 1}
              >
                이전
              </AppButton>

              {Array.from(
                {
                  length: isMineTab ? generatedTotalPages : totalPages,
                },
                (_, idx) => idx + 1
              ).map((page) => {
                const active = isMineTab ? generatedPage === page : currentPage === page;

                return (
                  <AppButton
                    key={page}
                    variant={active ? "primary" : "secondary"}
                    style={{
                      height: 36,
                      minWidth: 36,
                      padding: "0 12px",
                    }}
                    onClick={() => {
                      if (isMineTab) {
                        setGeneratedPage(page);
                      } else {
                        setCurrentPage(page);
                      }
                    }}
                  >
                    {page}
                  </AppButton>
                );
              })}

              <AppButton
                variant="secondary"
                style={{ height: 36 }}
                onClick={() => {
                  if (isMineTab) {
                    setGeneratedPage((prev) =>
                      Math.min(prev + 1, generatedTotalPages)
                    );
                  } else {
                    setCurrentPage((prev) => Math.min(prev + 1, totalPages));
                  }
                }}
                disabled={isMineTab ? generatedPage === generatedTotalPages : currentPage === totalPages}
              >
                다음
              </AppButton>
            </div>
          )}
        </div>
      </div>

      {requestModalOpen && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(15, 23, 42, 0.45)",
            zIndex: 9999,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: 24,
          }}
        >
          <div
            style={{
              width: 620,
              maxHeight: "80vh",
              overflowY: "auto",
              background: "#fff",
              borderRadius: 18,
              padding: 24,
              boxShadow: "0 24px 80px rgba(15, 23, 42, 0.25)",
            }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginBottom: 18,
              }}
            >
              <div>
                <h3
                  style={{
                    margin: 0,
                    fontSize: 20,
                    fontWeight: 900,
                    color: C.text,
                  }}
                >
                  추천 템플릿 추가 요청 현황
                </h3>
                <div
                  style={{
                    marginTop: 6,
                    fontSize: 13,
                    color: C.sub,
                  }}
                >
                  AI가 생성한 템플릿을 추천 목록에 반영해 달라는 요청 내역입니다.
                </div>
              </div>

            </div>

            {templateRequests.length === 0 ? (
              <div
                style={{
                  padding: 28,
                  border: `1px solid ${C.border}`,
                  borderRadius: 14,
                  background: "#F8FAFC",
                  color: C.sub,
                  textAlign: "center",
                  fontSize: 14,
                  lineHeight: 1.6,
                }}
              >
                아직 추천 템플릿 추가 요청 내역이 없습니다.
                <br />
                AI 생성 템플릿 카드에서 요청을 진행해 주세요.
              </div>
            ) : (
              <div style={{ display: "grid", gap: 12 }}>
                {templateRequests.map((request) => {
                  const statusStyle = getRequestStatusStyle(request.status);
                  const requestDate = request.createdAt || request.requestedAt;

                  return (
                    <div
                      key={request.requestId}
                      style={{
                        border: `1px solid ${C.border}`,
                        borderRadius: 14,
                        padding: 16,
                        background: "#fff",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          gap: 12,
                          alignItems: "flex-start",
                        }}
                      >
                        <div style={{ minWidth: 0 }}>
                          <div
                            style={{
                              fontSize: 16,
                              fontWeight: 900,
                              color: C.text,
                              marginBottom: 6,
                            }}
                          >
                            {request.title}
                          </div>

                          <div style={{ fontSize: 13, color: C.sub, lineHeight: 1.6 }}>
                            <div>
                              연관 부서: {normalizeDeptLabel(request.dept) || "공통"}
                            </div>
                            <div>상황: {request.situation || "-"}</div>
                            <div>유형: {request.category || "-"}</div>
                          </div>
                        </div>

                        <div
                          style={{
                            ...statusStyle,
                            height: 28,
                            padding: "0 10px",
                            borderRadius: 999,
                            display: "inline-flex",
                            alignItems: "center",
                            fontSize: 12,
                            fontWeight: 800,
                            flexShrink: 0,
                          }}
                        >
                          {getRequestStatusLabel(request.status)}
                        </div>
                      </div>

                      {request.desc && (
                        <div
                          style={{
                            marginTop: 12,
                            fontSize: 14,
                            color: C.text,
                            lineHeight: 1.6,
                          }}
                        >
                          {request.desc}
                        </div>
                      )}

                      {Array.isArray(request.preview) && request.preview.length > 0 && (
                        <div
                          style={{
                            marginTop: 12,
                            padding: 14,
                            borderRadius: 10,
                            background: "#F8FAFC",
                            border: `1px solid ${C.border}`,
                            fontSize: 13,
                            color: C.sub,
                            lineHeight: 1.6,
                          }}
                        >
                          {request.preview.map((line) => (
                            <div key={line}>{line}</div>
                          ))}
                        </div>
                      )}

                      <div
                        style={{
                          marginTop: 12,
                          display: "flex",
                          justifyContent: "space-between",
                          gap: 12,
                          alignItems: "center",
                        }}
                      >
                        <div style={{ fontSize: 12, color: C.sub }}>
                          요청일: {requestDate || "-"}
                        </div>
                        {request.adminComment && (
                          <div style={{ fontSize: 12, color: C.sub, textAlign: "right" }}>
                            관리자 의견: {request.adminComment}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            <div style={{ marginTop: 18, display: "flex", justifyContent: "flex-end" }}>
              <AppButton onClick={() => setRequestModalOpen(false)}>닫기</AppButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

