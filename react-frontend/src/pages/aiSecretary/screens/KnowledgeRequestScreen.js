/**
 * @FileName : KnowledgeRequestScreen.js
 * @Description : AiSecretary.js 전용 지식 등록 요청 화면
 */

import React, { useCallback, useEffect, useMemo, useState } from "react";
import AppButton from "../components/AppButton";
import AutocompleteInput from "../components/AutocompleteInput";
import Field from "../components/Field";
import OrganizationSelector from "../components/OrganizationSelector";
import TextInput from "../components/TextInput";
import {
  createKnowledgeRequest,
  getKnowledgeRequestSuggestions,
  getMyKnowledgeRequests,
  unwrapApiData,
} from "../api/aiSecretaryApi";
import { I, Icon } from "../constants/aiSecretaryIcons";
import { C, styles } from "../styles/aiSecretaryTheme";

const STATUS_LABEL_MAP = {
  PENDING: "대기중",
  APPROVED: "승인",
  REJECTED: "반려",
  PUBLISHED: "반영 완료",
};

const STATUS_TONE_MAP = {
  PENDING: {
    background: "#EFF6FF",
    color: "#1D4ED8",
    border: "#BFDBFE",
  },
  APPROVED: {
    background: "#ECFDF5",
    color: "#047857",
    border: "#A7F3D0",
  },
  REJECTED: {
    background: "#F3F4F6",
    color: "#374151",
    border: "#E5E7EB",
  },
  PUBLISHED: {
    background: "#EEF2FF",
    color: "#4338CA",
    border: "#C7D2FE",
  },
};

const INFO_BOX_STYLE = {
  borderRadius: 16,
  padding: 16,
  border: `1px solid ${C.border}`,
  background: "#fff",
};

const fieldErrorStyle = {
  marginTop: 8,
  fontSize: 12,
  color: "#DC2626",
  fontWeight: 700,
  lineHeight: 1.5,
};


const DEFAULT_REQUEST_TYPES = [
  "FAQ",
  "업무 규정",
  "절차 안내",
  "신청 방법",
  "기타",
];

const DEFAULT_CATEGORIES = [
  "인사",
  "총무",
  "IT",
  "회계",
  "교육",
  "복지",
  "보안",
  "기타",
];

function normalizeText(value) {
  if (value === null || value === undefined) {
    return "";
  }

  return String(value).trim();
}

function normalizeSuggestionList(defaults, values) {
  const merged = new Set();

  [...(Array.isArray(defaults) ? defaults : []), ...(Array.isArray(values) ? values : [])].forEach((value) => {
    const normalized = normalizeText(value);
    if (normalized) {
      merged.add(normalized);
    }
  });

  return Array.from(merged);
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function formatToday() {
  return formatDateTime(new Date());
}

function getRequesterLabel(userInfo, empNo) {
  const name = normalizeText(userInfo?.name || userInfo?.empName || userInfo?.userName);
  if (name && empNo) {
    return `${name} (${empNo})`;
  }
  if (name) {
    return name;
  }
  if (empNo) {
    return empNo;
  }
  return "-";
}

function getStatusLabel(status, fallback) {
  const key = normalizeText(status).toUpperCase();
  return STATUS_LABEL_MAP[key] || normalizeText(fallback) || key || "-";
}

function getStatusTone(status) {
  const key = normalizeText(status).toUpperCase();
  return STATUS_TONE_MAP[key] || {
    background: "#F3F4F6",
    color: "#374151",
    border: "#E5E7EB",
  };
}

function buildTargetDeptSummary(form) {
  const audience = normalizeText(form?.audience);
  const targets = Array.isArray(form?.targets)
    ? form.targets.map((item) => normalizeText(item)).filter(Boolean)
    : [];

  if (targets.length > 0) {
    return targets.join(" / ");
  }

  return audience;
}

function KnowledgeRequestFieldError({ error }) {
  if (!error) {
    return null;
  }

  return <div style={fieldErrorStyle}>{error}</div>;
}

export default function KnowledgeRequestScreen({ userInfo }) {
  const empNo = useMemo(
    () => normalizeText(userInfo?.empNo ?? userInfo?.emp_no ?? ""),
    [userInfo]
  );
  const requesterName = useMemo(
    () => normalizeText(userInfo?.name || userInfo?.empName || userInfo?.userName),
    [userInfo]
  );
  const requesterLabel = useMemo(
    () => getRequesterLabel(userInfo, empNo),
    [userInfo, empNo]
  );

  const [form, setForm] = useState({
    title: "",
    requestType: "",
    category: "",
    reason: "",
    sampleQuestion: "",
    referenceUrl: "",
    audience: "",
    targets: [],
    accessLevel: "CUSTOM",
  });
  const [errors, setErrors] = useState({});
  const [feedback, setFeedback] = useState(null);
  const [saving, setSaving] = useState(false);
  const [loadingSuggestions, setLoadingSuggestions] = useState(false);
  const [loadingRequests, setLoadingRequests] = useState(false);
  const [requests, setRequests] = useState([]);
  const [suggestions, setSuggestions] = useState({
    requestTypes: normalizeSuggestionList(DEFAULT_REQUEST_TYPES, []),
    categories: normalizeSuggestionList(DEFAULT_CATEGORIES, []),
  });

  const updateField = useCallback((field, value) => {
    setForm((prev) => ({
      ...prev,
      [field]: value,
    }));
    setErrors((prev) => ({
      ...prev,
      [field]: undefined,
    }));
  }, []);

  const resetForm = useCallback(
    ({ preserveFeedback = false } = {}) => {
      setForm({
        title: "",
        requestType: "",
        category: "",
        reason: "",
        sampleQuestion: "",
        referenceUrl: "",
        audience: "",
        targets: [],
        accessLevel: "CUSTOM",
      });
      setErrors({});
      if (!preserveFeedback) {
        setFeedback(null);
      }
    },
    []
  );

  const validateForm = useCallback(
    (nextForm) => {
      const nextErrors = {};
      const title = normalizeText(nextForm.title);
      const requestType = normalizeText(nextForm.requestType);
      const category = normalizeText(nextForm.category);
      const reason = normalizeText(nextForm.reason);
      const sampleQuestion = normalizeText(nextForm.sampleQuestion);
      const targetDept = buildTargetDeptSummary(nextForm);

      if (!empNo) {
        nextErrors.requester = "??? ??? ??? ?? ? ????.";
      }
      if (!title) {
        nextErrors.title = "??? ?? ?? ??? ?????.";
      }
      if (!requestType) {
        nextErrors.requestType = "?? ??? ?????.";
      }
      if (!category) {
        nextErrors.category = "????? ?????.";
      }
      if (!reason) {
        nextErrors.reason = "?? ??? ?????.";
      }
      if (!sampleQuestion) {
        nextErrors.sampleQuestion = "?? ?? ??? ?????.";
      }
      if (!targetDept) {
        nextErrors.targets = "?? ?? ??? ??? ???.";
      }

      return nextErrors;
    },
    [empNo]
  );

  const fetchSuggestions = useCallback(async () => {
    setLoadingSuggestions(true);
    try {
      const response = await getKnowledgeRequestSuggestions();
      const data = unwrapApiData(response);
      setSuggestions({
        requestTypes: normalizeSuggestionList(
          DEFAULT_REQUEST_TYPES,
          data?.requestTypes
        ),
        categories: normalizeSuggestionList(
          DEFAULT_CATEGORIES,
          data?.categories
        ),
      });
    } catch (error) {
      setSuggestions({
        requestTypes: normalizeSuggestionList(DEFAULT_REQUEST_TYPES, []),
        categories: normalizeSuggestionList(DEFAULT_CATEGORIES, []),
      });
    } finally {
      setLoadingSuggestions(false);
    }
  }, []);

  const fetchRequests = useCallback(async () => {
    if (!empNo) {
      setRequests([]);
      return;
    }

    setLoadingRequests(true);
    try {
      const response = await getMyKnowledgeRequests(empNo);
      const data = unwrapApiData(response);
      setRequests(Array.isArray(data) ? data : []);
    } catch (error) {
      setRequests([]);
    } finally {
      setLoadingRequests(false);
    }
  }, [empNo]);

  useEffect(() => {
    fetchSuggestions();
    fetchRequests();
  }, [fetchSuggestions, fetchRequests]);

  const handleSubmit = useCallback(
    async (event) => {
      event.preventDefault();

      if (saving) {
        return;
      }

      const nextErrors = validateForm(form);
      if (Object.keys(nextErrors).length > 0) {
        setErrors(nextErrors);
        setFeedback({
          type: "error",
          text: "입력값을 다시 확인해 주세요.",
        });
        return;
      }

      const targetDept = buildTargetDeptSummary(form);
      const payload = {
        requesterNo: empNo,
        title: normalizeText(form.title),
        requestType: normalizeText(form.requestType),
        category: normalizeText(form.category),
        targetDept: targetDept || null,
        reason: normalizeText(form.reason),
        sampleQuestion: normalizeText(form.sampleQuestion),
        referenceUrl: normalizeText(form.referenceUrl) || null,
        accessLevel: normalizeText(form.accessLevel) || "CUSTOM",
      };

      setSaving(true);
      try {
        await createKnowledgeRequest(payload);
        resetForm({ preserveFeedback: true });
        setFeedback({
          type: "success",
          text: "자료 등록 요청이 정상적으로 접수되었습니다.",
        });
        await fetchRequests();
      } catch (error) {
        setFeedback({
          type: "error",
          text: "?? ?? ?? ??? ??????.",
        });
      } finally {
        setSaving(false);
      }
    },
    [empNo, fetchRequests, form, resetForm, saving, validateForm]
  );

  const renderAutocompleteInput = (
    fieldName,
    placeholder,
    suggestionList,
    helperText
  ) => (
    <div style={{ display: "grid", gap: 8 }}>
      <AutocompleteInput
        value={form[fieldName]}
        onChange={(nextValue) => updateField(fieldName, nextValue)}
        suggestions={suggestionList}
        placeholder={placeholder}
        helperText={helperText}
        emptyText="직접 입력해 주세요."
      />
      {helperText ? <div style={{ fontSize: 12, color: C.sub }}>{helperText}</div> : null}
    </div>
  );

  const renderSelect = (fieldName, options, placeholder) => (
    <select
      value={form[fieldName]}
      onChange={(event) => updateField(fieldName, event.target.value)}
      style={{
        width: "100%",
        minHeight: 46,
        border: `1px solid ${C.border}`,
        borderRadius: 12,
        outline: "none",
        background: "#fff",
        color: C.text,
        fontSize: 14,
        padding: "0 14px",
        boxSizing: "border-box",
      }}
    >
      <option value="">{placeholder}</option>
      {(Array.isArray(options) ? options : []).map((option) => {
        const value =
          typeof option === "string" ? option : normalizeText(option?.value);
        const label =
          typeof option === "string" ? option : normalizeText(option?.label || option?.value);

        if (!value) {
          return null;
        }

        return (
          <option key={value} value={value}>
            {label || value}
          </option>
        );
      })}
    </select>
  );

  return (
    <div style={styles.page}>
      <div style={{ marginBottom: 20 }}>
        <div style={{ fontSize: 16, color: C.sub, fontWeight: 700 }}>
          자료 등록 요청
        </div>
        <h1
          style={{
            margin: "6px 0 0",
            fontSize: 38,
            fontWeight: 900,
            letterSpacing: -1,
          }}
        >
          챗봇 자료 등록 요청
        </h1>
        <p style={{ margin: "10px 0 0", color: C.sub, fontSize: 16 }}>
          관리자가 검토할 자료를 입력하면, 문서 처리 흐름으로 이어질 수 있도록 요청을 접수합니다.
        </p>
      </div>

      <div
        style={{
          ...styles.card,
          padding: 18,
          marginBottom: 18,
          background: "#F7FAFF",
        }}
      >
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
            gap: 16,
          }}
        >
          <div style={{ ...INFO_BOX_STYLE, display: "grid", gap: 10 }}>
            <div style={{ color: C.accent, fontWeight: 900, fontSize: 15 }}>
              상세 요청 정보를 입력해 주세요
            </div>
            <div style={{ color: C.sub, fontSize: 14, lineHeight: 1.7 }}>
              입력한 정보는 관리자 RAG 검토의 기준이 됩니다. 문서 유형이나 카테고리는 추천 목록에서 선택하거나 직접 입력할 수 있습니다.
            </div>
            <div style={{ fontSize: 12, color: C.muted, lineHeight: 1.6 }}>
              현재 추천 목록은 {loadingSuggestions ? "불러오는 중입니다." : "즉시 사용 가능합니다."}
            </div>
          </div>

          <div style={{ ...INFO_BOX_STYLE, display: "grid", gap: 10 }}>
            <div style={{ color: C.accent, fontWeight: 900, fontSize: 15 }}>
              요청자 정보
            </div>
            <div style={{ display: "grid", gap: 8, fontSize: 14, color: C.text }}>
              <div>
                <span style={{ color: C.sub, fontWeight: 700 }}>요청자명: </span>
                {requesterName || "-"}
              </div>
              <div>
                <span style={{ color: C.sub, fontWeight: 700 }}>사번 / empNo: </span>
                {empNo || "-"}
              </div>
              <div>
                <span style={{ color: C.sub, fontWeight: 700 }}>요청일: </span>
                {formatToday()}
              </div>
            </div>
            <div style={{ fontSize: 12, color: C.muted, lineHeight: 1.6 }}>
              요청자 정보는 목록과 상세에서 함께 활용됩니다.
            </div>
          </div>
        </div>
      </div>

      {feedback ? (
        <div
          style={{
            ...styles.card,
            padding: "14px 16px",
            marginBottom: 18,
            borderColor: feedback.type === "success" ? "#BBF7D0" : "#FECACA",
            background: feedback.type === "success" ? "#F0FDF4" : "#FEF2F2",
            color: feedback.type === "success" ? "#166534" : "#B91C1C",
            fontWeight: 700,
          }}
        >
          {feedback.text}
        </div>
      ) : null}

      <form onSubmit={handleSubmit} style={{ display: "grid", gap: 18 }}>
        <div style={{ ...styles.card, padding: 24 }}>
          <div style={{ display: "grid", gap: 16 }}>
            <Field label="문서명" required>
              <div>
                <TextInput
                  placeholder="예: 2026 연차 규정"
                  value={form.title}
                  onChange={(e) => updateField("title", e.target.value)}
                />
                <KnowledgeRequestFieldError error={errors.title} />
              </div>
            </Field>

            <Field label="문서 유형" required>
              <div>
                {renderAutocompleteInput(
                  "requestType",
                  "예: 업무 매뉴얼, FAQ, 제도 안내",
                  suggestions.requestTypes,
                  "추천 목록에서 선택하거나 직접 입력할 수 있습니다."
                )}
                <KnowledgeRequestFieldError error={errors.requestType} />
              </div>
            </Field>

            <Field label="카테고리" required>
              <div>
                {renderAutocompleteInput(
                  "category",
                  "예: 인사, 총무, IT, 복지",
                  suggestions.categories,
                  "추천 목록에서 선택하거나 직접 입력할 수 있습니다."
                )}
                <KnowledgeRequestFieldError error={errors.category} />
              </div>
            </Field>

            <Field label="요청 사유" required>
              <div>
                <TextInput
                  textarea
                  placeholder="어떤 업무에서 필요하고, 어떤 내용을 알고 싶은지 구체적으로 적어 주세요."
                  value={form.reason}
                  onChange={(e) => updateField("reason", e.target.value)}
                />
                <KnowledgeRequestFieldError error={errors.reason} />
              </div>
            </Field>

            <Field label="챗봇 질문 예시" required>
              <div>
                <TextInput
                  textarea
                  placeholder="예: 연차 신청은 어디서 하나요?"
                  value={form.sampleQuestion}
                  onChange={(e) => updateField("sampleQuestion", e.target.value)}
                />
                <KnowledgeRequestFieldError error={errors.sampleQuestion} />
              </div>
            </Field>

            <Field label="참고 URL 또는 콘텐츠 경로">
              <div style={{ display: "grid", gap: 10 }}>
                <TextInput
                  placeholder="예: https://company... 또는 /docs/hr/연차규정.pdf"
                  value={form.referenceUrl}
                  onChange={(e) => updateField("referenceUrl", e.target.value)}
                />
                <div style={{ fontSize: 12, color: C.sub, lineHeight: 1.6 }}>
                  참고할 문서가 있다면 공개 URL 또는 내부 콘텐츠 경로를 입력해 주세요.
                </div>
              </div>
            </Field>

            <Field label="권한 희망 조건" required>
              <div style={{ display: "grid", gap: 10 }}>
                <OrganizationSelector
                  formType="REPORT"
                  audience={form.audience}
                  targets={form.targets}
                  onChangeFormData={updateField}
                  showReferenceNote={false}
                />
                <KnowledgeRequestFieldError error={errors.targets} />
                <div className="form-text">
                  대상 본부, 팀, 직책 기준, 사원 선택을 순서대로 지정해 주세요.
                </div>
              </div>
            </Field>

            <KnowledgeRequestFieldError error={errors.requester} />
          </div>

          <div
            style={{
              display: "flex",
              justifyContent: "flex-end",
              gap: 10,
              marginTop: 24,
              flexWrap: "wrap",
            }}
          >
            <AppButton
              type="button"
              variant="secondary"
              onClick={() => resetForm()}
            >
              초기화
            </AppButton>
            <AppButton type="submit" disabled={saving || !empNo}>
              <Icon>{I.send}</Icon>
              {saving ? "전송 중..." : "전송"}
            </AppButton>
          </div>
        </div>
      </form>

      <div style={{ ...styles.card, padding: 22, marginTop: 18 }}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
          <div>
            <h3 style={styles.sectionTitle}>내 요청 목록</h3>
            <div style={styles.sectionSub}>
              최근 제출한 요청과 현재 처리 상태를 확인할 수 있습니다.
            </div>
          </div>

          <div
            style={{
              color: C.sub,
              fontSize: 13,
              fontWeight: 700,
              display: "flex",
              alignItems: "center",
              gap: 8,
            }}
          >
            <Icon>{I.history}</Icon>
            {loadingRequests ? "불러오는 중..." : `${requests.length}건`}
          </div>
        </div>

        <div style={{ marginTop: 18, display: "grid", gap: 12 }}>
          {requests.length === 0 && !loadingRequests ? (
            <div
              style={{
                border: `1px dashed ${C.border}`,
                borderRadius: 14,
                padding: 20,
                color: C.sub,
                textAlign: "center",
              }}
            >
              아직 제출한 요청이 없습니다.
            </div>
          ) : null}

          {requests.map((request) => {
            const tone = getStatusTone(request?.status);
            const title = normalizeText(request?.title) || "-";
            const requester = normalizeText(request?.requesterName) || requesterLabel;

            return (
              <div
                key={request?.knowledgeRequestId || `${title}-${request?.createdAt || request?.requestDate || Math.random()}`}
                style={{
                  border: `1px solid ${C.border}`,
                  borderRadius: 16,
                  padding: 18,
                  background: "#fff",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    gap: 12,
                    alignItems: "flex-start",
                    flexWrap: "wrap",
                  }}
                >
                  <div style={{ minWidth: 0 }}>
                    <div style={{ fontSize: 18, fontWeight: 900, color: C.text, marginBottom: 6 }}>
                      {title}
                    </div>
                    <div style={{ fontSize: 13, color: C.sub, lineHeight: 1.6 }}>
                      {formatDateTime(request?.createdAt || request?.requestDate)} · {requester}
                    </div>
                  </div>

                  <span
                    style={{
                      background: tone.background,
                      color: tone.color,
                      border: `1px solid ${tone.border}`,
                      borderRadius: 999,
                      padding: "6px 12px",
                      fontSize: 12,
                      fontWeight: 900,
                      flexShrink: 0,
                    }}
                  >
                    {getStatusLabel(request?.status, request?.statusLabel)}
                  </span>
                </div>

                <div
                  style={{
                    marginTop: 16,
                    display: "grid",
                    gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
                    gap: 14,
                  }}
                >
                  <div>
                    <div style={{ fontSize: 12, color: C.muted, fontWeight: 700 }}>문서 유형</div>
                    <div style={{ marginTop: 4, fontSize: 14, color: C.text }}>
                      {normalizeText(request?.requestType) || "-"}
                    </div>
                  </div>

                  <div>
                    <div style={{ fontSize: 12, color: C.muted, fontWeight: 700 }}>카테고리</div>
                    <div style={{ marginTop: 4, fontSize: 14, color: C.text }}>
                      {normalizeText(request?.category) || "-"}
                    </div>
                  </div>

                  <div>
                    <div style={{ fontSize: 12, color: C.muted, fontWeight: 700 }}>권한 희망 조건</div>
                    <div style={{ marginTop: 4, fontSize: 14, color: C.text }}>
                      {normalizeText(request?.targetDept) || "-"}
                    </div>
                  </div>

                  <div>
                    <div style={{ fontSize: 12, color: C.muted, fontWeight: 700 }}>
                      참고 URL 또는 콘텐츠 경로
                    </div>
                    <div style={{ marginTop: 4, fontSize: 14, color: C.text, wordBreak: "break-all" }}>
                      {normalizeText(request?.referenceUrl) || "-"}
                    </div>
                  </div>
                </div>

                <div style={{ marginTop: 14 }}>
                  <div style={{ fontSize: 12, color: C.muted, fontWeight: 700 }}>요청 사유</div>
                  <div
                    style={{
                      marginTop: 4,
                      fontSize: 14,
                      color: C.text,
                      lineHeight: 1.7,
                      whiteSpace: "pre-wrap",
                    }}
                  >
                    {normalizeText(request?.reason) || "-"}
                  </div>
                </div>

                <div style={{ marginTop: 14 }}>
                  <div style={{ fontSize: 12, color: C.muted, fontWeight: 700 }}>챗봇 질문 예시</div>
                  <div
                    style={{
                      marginTop: 4,
                      fontSize: 14,
                      color: C.text,
                      lineHeight: 1.7,
                      whiteSpace: "pre-wrap",
                    }}
                  >
                    {normalizeText(request?.sampleQuestion) || "-"}
                  </div>
                </div>

                {normalizeText(request?.adminComment) ? (
                  <div
                    style={{
                      marginTop: 14,
                      borderRadius: 12,
                      background: "#F8FAFC",
                      border: `1px solid ${C.border}`,
                      padding: 14,
                    }}
                  >
                    <div style={{ fontSize: 12, color: C.muted, fontWeight: 700 }}>관리자 검토 메모</div>
                    <div
                      style={{
                        marginTop: 4,
                        fontSize: 14,
                        color: C.text,
                        lineHeight: 1.7,
                        whiteSpace: "pre-wrap",
                      }}
                    >
                      {request.adminComment}
                    </div>
                  </div>
                ) : null}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
