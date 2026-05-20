/**
 * @FileName : KnowledgeRequestScreen.js
 * @Description : AiSecretary.js 전용 지식 추가 화면
 * @Author : 송혜진
 * @Date : 2026. 04. 30
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.30  송혜진       최초 생성
 */

import React, { useState } from "react";
import Chip from "../components/Chip";
import Field from "../components/Field";
import TextInput from "../components/TextInput";
import {
  createKnowledgeRequest,
  getKnowledgeRequestSuggestions,
  getMyKnowledgeRequests,
  unwrapApiData,
} from "../api/aiSecretaryApi";
import { C, styles } from "../styles/aiSecretaryTheme";

export default function KnowledgeRequestScreen() {
  const [form, setForm] = useState({
    title: "",
    requestType: "",
    category: "",
    reason: "",
    sampleQuestion: "",
    referenceUrl: "",
    accessLevel: "",
    customDept: "",
    customPosition: "",
  };
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

function getAccessLevelLabel(value) {
  const key = normalizeText(value).toUpperCase();
  if (key === "PUBLIC") {
    return "전체 공개";
  }
  if (key === "CUSTOM") {
    return "조건 조합";
  }
  if (key === "ADMIN_ONLY") {
    return "관리자 전용";
  }
  return normalizeText(value) || "-";
}

function getAccessLevelGuide(value) {
  if (value === "PUBLIC") {
    return "전체 공개 문서는 모든 직원이 챗봇/RAG 검색 결과로 접근할 수 있습니다.";
  }
  if (value === "CUSTOM") {
    return "선택한 조건은 관리자 검토 시 기본 접근 권한 참고 정보로 활용됩니다. 최종 접근 권한은 관리자 검토 후 확정됩니다.";
  }
  if (value === "ADMIN_ONLY") {
    return "관리자 전용 문서는 관리자 권한 사용자만 챗봇/RAG 검색 결과로 접근할 수 있습니다.";
  }
  return "기본 열람 권한을 선택하면 챗봇/RAG 검색 결과 접근 범위를 확인할 수 있습니다.";
}

function buildCustomTargetDept(form) {
  const team = normalizeText(form.customDept);
  const position = normalizeText(form.customPosition);
  const parts = [];

  if (team) {
    parts.push(`대상 팀: ${team}`);
  }
  if (position) {
    parts.push(`직책 기준: ${position}`);
  }

  return parts.join(" / ");
}

function normalizeSuggestionList(defaults, values) {
  const merged = new Set();

  [...(Array.isArray(defaults) ? defaults : []), ...(Array.isArray(values) ? values : [])].forEach((value) => {
    const normalized = normalizeText(value);
    if (normalized) {
      merged.add(normalized);
    }
  });

  const scopeOptions = ["전사 공개", "부서 공개", "특정 권한"];

  return (
    <div style={styles.page}>
      <div style={{ marginBottom: 20 }}>
        <div style={{ fontSize: 16, color: C.sub, fontWeight: 700 }}>
          지식 추가 요청
        </div>
        <h1
          style={{
            margin: "6px 0 0",
            fontSize: 38,
            fontWeight: 900,
            letterSpacing: -1,
          }}
        >
          챗봇 지식 추가 요청
        </h1>
        <p style={{ margin: "10px 0 0", color: C.sub, fontSize: 16 }}>
          관리자 검토 후 등록이 완료되면, 챗봇이 해당 데이터를 학습하여 스마트한 답변을 제공합니다.
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
          <div
            style={{
              ...INFO_BOX_STYLE,
              display: "grid",
              gap: 10,
            }}
          >
            <div style={{ color: C.accent, fontWeight: 900, fontSize: 15 }}>
              상세 항목을 입력해 주세요.
            </div>
            <div style={{ color: C.sub, fontSize: 14, lineHeight: 1.7 }}>
              입력하신 정보는 관리자 RAG 시스템의 상세 구조로 저장되어, 더욱 정확하고 풍부한 챗봇 답변의 밑바탕이 됩니다.
            </div>
            <div style={{ fontSize: 12, color: C.muted, lineHeight: 1.6 }}>
              현재 단계에서는 파일 본문 자동 분석 대신 참고 URL 또는 파일명을 기반으로 검토가 진행됩니다.
            </div>
          </div>

          <div
            style={{
              ...INFO_BOX_STYLE,
              display: "grid",
              gap: 10,
            }}
          >
            <div style={{ color: C.accent, fontWeight: 900, fontSize: 15 }}>
              요청자 정보
            </div>
            <div style={{ display: "grid", gap: 8, fontSize: 14, color: C.text }}>
              <div>
                <span style={{ color: C.sub, fontWeight: 700 }}>사용자명: </span>
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
              요청자 정보는 로그인 사용자 기준으로 자동 표시됩니다.
            </div>
          </div>
        </div>
      </div>

      {feedback && (
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
      )}

      <form onSubmit={handleSubmit} style={{ display: "grid", gap: 18 }}>
        <div style={{ ...styles.card, padding: 24 }}>
          <div style={{ display: "grid", gap: 16 }}>
            <Field label="문서명" required>
              <div>
                <TextInput
                  placeholder="예: 2026 근태 지침"
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
                  "예: 업무 매뉴얼, FAQ, 서비스 이용 안내",
                  suggestions.requestTypes,
                  "기존 유형을 선택하거나 새 유형을 직접 입력할 수 있습니다."
                )}
                <KnowledgeRequestFieldError error={errors.requestType} />
              </div>
            </Field>

            <Field label="카테고리" required>
              <div>
                {renderAutocompleteInput(
                  "category",
                  "예: 근태, 인사, 전자결재, 시스템",
                  suggestions.categories,
                  "기존 카테고리를 선택하거나 새 카테고리를 직접 입력할 수 있습니다."
                )}
                <KnowledgeRequestFieldError error={errors.category} />
              </div>
            </Field>

            <Field label="요청 사유" required>
              <div>
                <TextInput
                  textarea
                  placeholder="어떤 업무에서 필요하고, 어떤 질문에 도움이 되는지 자세히 적어 주세요."
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
                  placeholder="예: 조퇴 신청은 어떻게 하나요?"
                  value={form.sampleQuestion}
                  onChange={(e) => updateField("sampleQuestion", e.target.value)}
                />
                <KnowledgeRequestFieldError error={errors.sampleQuestion} />
              </div>
            </Field>

            <Field label="참고 URL 또는 콘텐츠 경로">
              <div style={{ display: "grid", gap: 10 }}>
                <TextInput
                  placeholder="예: https://company.notion.site/leave-policy 또는 /docs/hr/근태관리_조퇴신청_기준.pdf"
                  value={form.referenceUrl}
                  onChange={(e) => updateField("referenceUrl", e.target.value)}
                />
                <div style={{ fontSize: 12, color: C.sub, lineHeight: 1.6 }}>
                  현재 단계에서는 파일 본문 자동 분석이나 실제 업로드는 지원하지 않으며, 입력한 URL 또는 콘텐츠 경로를 기준으로 관리자가 검토합니다.
                </div>
              </div>
            </Field>

            <Field label="기본 열람 권한" required>
              <div style={{ display: "grid", gap: 10 }}>
                {renderSelect(
                  "accessLevel",
                  ACCESS_LEVEL_OPTIONS,
                  "기본 열람 권한을 선택해 주세요."
                )}
                <KnowledgeRequestFieldError error={errors.accessLevel} />
                <div
                  style={{
                    borderRadius: 12,
                    padding: "12px 14px",
                    background:
                      form.accessLevel === "PUBLIC"
                        ? "#ECFDF5"
                        : form.accessLevel === "ADMIN_ONLY"
                          ? "#EFF6FF"
                          : form.accessLevel === "CUSTOM"
                            ? "#F8FAFC"
                            : "#F8FAFC",
                    color:
                      form.accessLevel === "PUBLIC"
                        ? "#047857"
                        : form.accessLevel === "ADMIN_ONLY"
                          ? "#1D4ED8"
                          : C.sub,
                    fontSize: 13,
                    lineHeight: 1.6,
                    border: `1px solid ${
                      form.accessLevel === "PUBLIC"
                        ? "#A7F3D0"
                        : form.accessLevel === "ADMIN_ONLY"
                          ? "#BFDBFE"
                          : C.border
                    }`,
                  }}
                >
                  {accessLevelGuide}
                </div>

                {showCustomSection && (
                  <div
                    style={{
                      marginTop: 4,
                      borderRadius: 12,
                      padding: 14,
                      background: "#F8FAFC",
                      border: `1px solid ${C.border}`,
                      display: "grid",
                      gap: 12,
                    }}
                  >
                    <div>
                      <div style={{ fontSize: 14, fontWeight: 900, color: C.text }}>
                        조건 조합
                      </div>
                      <div style={{ marginTop: 4, fontSize: 12, color: C.sub, lineHeight: 1.6 }}>
                        선택한 조건은 관리자 검토 시 기본 접근 권한 참고 정보로 활용됩니다. 최종 접근 권한은 관리자 검토 후 확정됩니다.
                      </div>
                    </div>

                    <div style={{ display: "grid", gap: 12 }}>
                      <Field label="대상 팀">
                        <div>
                          {renderSelect("customDept", TEAM_OPTIONS, "대상 팀을 선택해 주세요.")}
                        </div>
                      </Field>

                      <Field label="직책 기준">
                        <div>
                          {renderSelect("customPosition", POSITION_OPTIONS, "직책 기준을 선택해 주세요.")}
                        </div>
                      </Field>
                    </div>

                    <KnowledgeRequestFieldError error={errors.customDept || errors.customPosition} />
                  </div>
                )}
              </div>
            </Field>

            {errors.requester && (
              <div style={{ ...fieldErrorStyle, marginTop: 0 }}>{errors.requester}</div>
            )}
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
              onClick={() => {
                resetForm();
                setFeedback(null);
              }}
            >
              초기화
            </AppButton>
            <AppButton type="submit" disabled={saving || !empNo}>
              <Icon>{I.send}</Icon>
              {saving ? "제출 중..." : "제출"}
            </AppButton>
          </div>
        </div>
      </form>

      <div style={{ ...styles.card, padding: 22, marginTop: 18 }}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
          <div>
            <h3 style={styles.sectionTitle}>내 요청 목록</h3>
            <div style={styles.sectionSub}>
              최근 제출한 요청을 확인하고, 현재 처리 상태와 관리자 메모를 볼 수 있습니다.
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
            const tone = getStatusTone(request.status);
            return (
              <div
                key={request.knowledgeRequestId || `${request.title}-${request.createdAt}`}
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
                      {normalizeText(request.title) || "-"}
                    </div>
                    <div style={{ fontSize: 13, color: C.sub, lineHeight: 1.6 }}>
                      {formatDateTime(request.createdAt)} · {normalizeText(request.requesterName) || requesterLabel}
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
                    {getStatusLabel(request.status, request.statusLabel)}
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
                      {normalizeText(request.requestType) || "-"}
                    </div>
                  </div>

                  <div>
                    <div style={{ fontSize: 12, color: C.muted, fontWeight: 700 }}>카테고리</div>
                    <div style={{ marginTop: 4, fontSize: 14, color: C.text }}>
                      {normalizeText(request.category) || "-"}
                    </div>
                  </div>

                  <div>
                    <div style={{ fontSize: 12, color: C.muted, fontWeight: 700 }}>기본 열람 권한</div>
                    <div style={{ marginTop: 4, fontSize: 14, color: C.text }}>
                      {getAccessLevelLabel(request.accessLevel)}
                    </div>
                  </div>

                  <div>
                    <div style={{ fontSize: 12, color: C.muted, fontWeight: 700 }}>
                      참고 URL 또는 콘텐츠 경로
                    </div>
                    <div style={{ marginTop: 4, fontSize: 14, color: C.text, wordBreak: "break-all" }}>
                      {normalizeText(request.referenceUrl) || "-"}
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
                    {normalizeText(request.reason) || "-"}
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
                    {normalizeText(request.sampleQuestion) || "-"}
                  </div>
                </div>

                {normalizeText(request.adminComment) && (
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
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}