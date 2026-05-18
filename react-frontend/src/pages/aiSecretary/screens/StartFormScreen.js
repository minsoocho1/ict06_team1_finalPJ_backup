/**
 * @FileName : StartFormScreen.js
 * @Description : AiSecretary.js 전용 문서 작성 시작 화면
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 */

import React, { useEffect, useRef, useState } from "react";
import AppButton from "../components/AppButton";
import Field from "../components/Field";
import TextInput from "../components/TextInput";
import OrganizationSelector from "../components/OrganizationSelector";
import { docMeta } from "../constants/aiSecretaryData";
import { I, Icon } from "../constants/aiSecretaryIcons";
import { C, styles } from "../styles/aiSecretaryTheme";

export default function StartFormScreen({
  formType,
  formData,
  templateSeed = null,
  initialOrganizationSeed = null,
  onChangeFormType,
  onChangeFormData,
  onGenerateDraft,
  onOpenTemplate,
  generating = false,
  error = "",
}) {
  const tabs = ["REPORT", "MINUTES", "APPROVAL", "template"];
  const amountOptions = [
    { value: "short", label: "짧게" },
    { value: "normal", label: "보통" },
    { value: "detail", label: "자세히" },
    { value: "custom", label: "직접 입력" },
  ];

  const [amountMode, setAmountMode] = useState("normal");
  const [customAmount, setCustomAmount] = useState("");
  const referenceInputRef = useRef(null);

  const maxReferenceFiles = 3;
  const maxReferenceFileSize = 10 * 1024 * 1024;
  const referenceInputId = "ai-secretary-reference-files";

  const safeFormType =
    formType === "REPORT" || formType === "MINUTES" || formType === "APPROVAL"
      ? formType
      : "REPORT";

  const safeFormData = {
    title: formData?.title || "",
    purpose: formData?.purpose || "",
    audience: formData?.audience || "",
    targets: Array.isArray(formData?.targets)
      ? formData.targets
      : [],
    detail: formData?.detail || "",
    amount: formData?.amount || "",
    referenceFiles: Array.isArray(formData?.referenceFiles)
      ? formData.referenceFiles
      : [],
    referenceMemo: formData?.referenceMemo || "",
  };

  const organizationSeed =
    templateSeed?.organizationSeed ||
    (templateSeed?.deptText ? { deptText: templateSeed.deptText } : null) ||
    initialOrganizationSeed ||
    null;

  const normalizeAmountMode = (amountValue) => {
    const normalized = String(amountValue || "").trim();
    if (normalized === "짧게") return "short";
    if (normalized === "보통" || normalized === "") return "normal";
    if (normalized === "자세히") return "detail";
    return "custom";
  };

  const formatReferenceFileSize = (size) => {
    const bytes = Number(size || 0);

    if (!bytes) return "0 KB";
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const handleReferenceFilesChange = (event) => {
    const selectedFiles = Array.from(event.target.files || [])
      .filter((file) => Number(file?.size || 0) <= maxReferenceFileSize)
      .slice(0, maxReferenceFiles);

    const mergedFiles = [...safeFormData.referenceFiles, ...selectedFiles];
    const dedupedFiles = mergedFiles.filter(
      (file, index, list) =>
        index ===
        list.findIndex(
          (item) =>
            item?.name === file?.name &&
            item?.size === file?.size &&
            item?.lastModified === file?.lastModified
        )
    );

    onChangeFormData("referenceFiles", dedupedFiles.slice(0, maxReferenceFiles));
    event.target.value = "";
  };

  const handleRemoveReferenceFile = (removeIndex) => {
    const nextFiles = safeFormData.referenceFiles.filter(
      (_, index) => index !== removeIndex
    );

    onChangeFormData("referenceFiles", nextFiles);
  };

  const baseFields = {
    REPORT: {
      title: "보고서 제목",
      titlePlaceholder: "예: 3분기 마케팅 성과 보고 초안",
      purpose: "작성 목적",
      purposePlaceholder:
        "예: 부서 성과를 정리하고 다음 분기 계획을 공유하기 위함",
      detail: "상세 내용",
      detailPlaceholder:
        "주요 내용, 배경, 현황, 분석, 제안 등을 작성하세요.",
      amount: "작성 분량",
      amountPlaceholder: "예: A4 3~5페이지 분량",
    },
    MINUTES: {
      title: "회의 제목",
      titlePlaceholder: "예: 4월 2주차 영업회의 회의록",
      purpose: "회의 목적",
      purposePlaceholder: "예: 진행 상황 공유 및 의사결정 정리",
      detail: "회의 내용",
      detailPlaceholder:
        "회의 발언 내용, 안건, 의사결정 사항, 액션아이템을 입력하세요.",
      amount: "작성 분량",
      amountPlaceholder: "예: 결정사항 중심 / 액션아이템 중심",
    },
    APPROVAL: {
      title: "결재 제목",
      titlePlaceholder: "예: 외부 전시회 참가 결재 요청",
      purpose: "요청 배경",
      purposePlaceholder:
        "예: 프로젝트 수행을 위해 필요한 승인 사항을 설명",
      detail: "결재 사유",
      detailPlaceholder:
        "요청의 필요성, 기대 효과, 비용을 포함해 작성하세요.",
      amount: "작성 분량",
      amountPlaceholder: "예: 비용 상세 / 간결한 요약 / 업무 경과",
    },
  };

  const f = baseFields[safeFormType] || baseFields.REPORT;
  const organizationLabel =
    safeFormType === "MINUTES"
      ? "정리 대상 선택"
      : safeFormType === "APPROVAL"
      ? "결재 라인 참고 대상"
      : "보고 대상 선택";

  useEffect(() => {
    if (templateSeed) {
      const seededAmount = String(templateSeed.amount || "보통").trim() || "보통";

      onChangeFormData("title", templateSeed.title || "");
      onChangeFormData("purpose", templateSeed.purpose || "");
      onChangeFormData("detail", templateSeed.detail || "");
      onChangeFormData("amount", seededAmount);
      onChangeFormData("referenceFiles", []);
      onChangeFormData("referenceMemo", "");

      if (referenceInputRef.current) {
        referenceInputRef.current.value = "";
      }

      const nextMode = normalizeAmountMode(seededAmount);
      setAmountMode(nextMode);
      setCustomAmount(
        nextMode === "custom" ? seededAmount : ""
      );
      return;
    }

    onChangeFormData("title", "");
    onChangeFormData("purpose", "");
    onChangeFormData("audience", "");
    onChangeFormData("targets", []);
    onChangeFormData("detail", "");
    onChangeFormData("amount", "보통");
    onChangeFormData("referenceFiles", []);
    onChangeFormData("referenceMemo", "");
    if (referenceInputRef.current) {
      referenceInputRef.current.value = "";
    }
    setAmountMode("normal");
    setCustomAmount("");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [safeFormType, templateSeed]);

  const handleAmountModeChange = (nextMode) => {
    setAmountMode(nextMode);

    if (nextMode === "short") {
      onChangeFormData("amount", "짧게");
      return;
    }

    if (nextMode === "normal") {
      onChangeFormData("amount", "보통");
      return;
    }

    if (nextMode === "detail") {
      onChangeFormData("amount", "자세히");
      return;
    }

    onChangeFormData("amount", customAmount || "");
  };

  const handleTopTabClick = (tab) => {
    if (tab === "template") {
      onOpenTemplate();
      return;
    }

    onChangeFormType(tab);
  };

  return (
    <div style={styles.page}>
      <div style={{ marginBottom: 18 }}>
        <div style={{ fontSize: 16, color: C.sub, fontWeight: 700 }}>
          AI 비서
        </div>

        <h1
          style={{
            margin: "6px 0 0",
            fontSize: 38,
            fontWeight: 900,
            letterSpacing: -1,
          }}
        >
          문서 작성 시작
        </h1>

        <p style={{ margin: "10px 0 0", color: C.sub, fontSize: 16 }}>
          문서 유형에 맞는 입력 항목을 작성하면 AI가 자연스럽고 정확한 초안을 생성합니다.
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
        {tabs.map((tab) => {
          const active = tab === "template" ? false : safeFormType === tab;

          const icon =
            tab === "REPORT"
              ? I.file
              : tab === "MINUTES"
              ? I.users
              : tab === "APPROVAL"
              ? I.check
              : I.spark;

          return (
            <button
              key={tab}
              type="button"
              onClick={() => handleTopTabClick(tab)}
              style={{
                ...styles.card,
                border: `1px solid ${active ? C.accent : C.border}`,
                background: active ? C.accentBg : "#fff",
                minHeight: 92,
                textAlign: "left",
                padding: 18,
                cursor: "pointer",
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
                <Icon>{icon}</Icon>
                <span style={{ fontSize: 15, fontWeight: 800 }}>
                  {docMeta?.[tab]?.label || tab}
                </span>
              </div>

              {docMeta?.[tab]?.description && (
                <div
                  style={{
                    marginTop: 8,
                    fontSize: 12,
                    color: C.sub,
                    lineHeight: 1.5,
                  }}
                >
                  {docMeta[tab].description}
                </div>
              )}
            </button>
          );
        })}
      </div>

      <div
        style={{
          ...styles.card,
          padding: 18,
          marginBottom: 18,
          background: "#F7FAFF",
        }}
      >
        <div style={{ color: C.accent, fontWeight: 900, fontSize: 15 }}>
          입력값이 구체적일수록 초안이 더 정확해집니다.
        </div>

        <div style={{ marginTop: 8, color: C.sub, fontSize: 14 }}>
          문서 목적, 상세 내용, 참고 사항을 명확히 입력하면 AI가 더 정확하고
          업무적인 결과를 생성합니다. 초안 생성 전에는 항목별로 한 번 더
          점검해 주세요.
        </div>
      </div>

      <div style={{ ...styles.card, padding: 24 }}>
        <div style={{ display: "grid", gap: 16 }}>
          <Field label={f.title} required>
            <TextInput
              value={safeFormData.title}
              onChange={(e) => onChangeFormData("title", e.target.value)}
              placeholder={f.titlePlaceholder}
            />
          </Field>

          <Field label={f.purpose} required>
            <TextInput
              value={safeFormData.purpose}
              onChange={(e) => onChangeFormData("purpose", e.target.value)}
              placeholder={f.purposePlaceholder}
            />
          </Field>

          <Field label={f.detail} required>
            <div>
              <TextInput
                textarea
                value={safeFormData.detail}
                onChange={(e) => onChangeFormData("detail", e.target.value)}
                placeholder={f.detailPlaceholder}
              />

              <div
                style={{
                  textAlign: "right",
                  fontSize: 12,
                  color: C.muted,
                  marginTop: 8,
                }}
              >
                {safeFormData.detail.length} / 1000
              </div>
            </div>
          </Field>

          <Field label={f.amount}>
            <div style={{ display: "grid", gap: 10 }}>
              <div style={{ position: "relative" }}>
                <select
                  value={amountMode}
                  onChange={(e) => handleAmountModeChange(e.target.value)}
                  style={{
                    width: "100%",
                    height: 46,
                    border: `1px solid ${C.border}`,
                    borderRadius: 10,
                    background: "#fff",
                    color: C.text,
                    fontSize: 14,
                    padding: "0 40px 0 14px",
                    boxSizing: "border-box",
                    appearance: "none",
                    WebkitAppearance: "none",
                    MozAppearance: "none",
                  }}
                >
                  {amountOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>

                <div
                  style={{
                    position: "absolute",
                    right: 12,
                    top: "50%",
                    transform: "translateY(-50%)",
                    pointerEvents: "none",
                    color: C.sub,
                  }}
                >
                  <Icon>{I.down}</Icon>
                </div>
              </div>

              {amountMode === "custom" && (
                <TextInput
                  value={customAmount}
                  onChange={(e) => {
                    const value = e.target.value;
                    setCustomAmount(value);
                    onChangeFormData("amount", value);
                  }}
                  placeholder={f.amountPlaceholder}
                />
              )}
            </div>
          </Field>

          <Field label="참고 자료 첨부">
            <div style={{ display: "grid", gap: 10 }}>
              <label
                htmlFor={referenceInputId}
                style={{
                  border: `1px dashed ${C.border}`,
                  borderRadius: 12,
                  padding: 16,
                  color: C.sub,
                  background: "#FBFDFF",
                  cursor: "pointer",
                  display: "block",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 10,
                    fontWeight: 700,
                    color: C.text,
                  }}
                >
                  <Icon>{I.clip}</Icon>
                  파일을 선택해 첨부해 주세요. 파일당 10MB 이하, 최대 3개
                </div>

                <div style={{ marginTop: 10, fontSize: 13, lineHeight: 1.6 }}>
                  PDF, DOCX, TXT 파일만 지원합니다.
                  <br />
                  선택한 파일명과 참고 메모가 문서 작성 참고 정보로 반영됩니다.
                  현재 단계에서는 파일 본문 자동 분석은 지원하지 않습니다.
                </div>

                <input
                  ref={referenceInputRef}
                  id={referenceInputId}
                  type="file"
                  multiple
                  accept=".pdf,.docx,.txt,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain"
                  onChange={handleReferenceFilesChange}
                  style={{ display: "none" }}
                />
              </label>

              {safeFormData.referenceFiles.length > 0 && (
                <div
                  style={{
                    border: `1px solid ${C.border}`,
                    borderRadius: 12,
                    padding: 12,
                    background: "#fff",
                    display: "grid",
                    gap: 8,
                  }}
                >
                  {safeFormData.referenceFiles.map((file, index) => (
                    <div
                      key={`${file.name}-${index}`}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        gap: 10,
                        fontSize: 13,
                      }}
                    >
                      <div style={{ minWidth: 0 }}>
                        <div
                          style={{
                            fontWeight: 700,
                            color: C.text,
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                          }}
                        >
                          {file.name}
                        </div>
                        <div style={{ marginTop: 3, color: C.sub }}>
                          {formatReferenceFileSize(file.size)}
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleRemoveReferenceFile(index)}
                        style={{
                          border: `1px solid ${C.border}`,
                          background: "#fff",
                          color: C.sub,
                          borderRadius: 8,
                          height: 30,
                          padding: "0 10px",
                          cursor: "pointer",
                          fontSize: 12,
                          flexShrink: 0,
                        }}
                      >
                        제거
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <TextInput
                textarea
                value={safeFormData.referenceMemo}
                onChange={(e) =>
                  onChangeFormData("referenceMemo", e.target.value)
                }
                placeholder="참고 자료에서 특히 반영할 내용이나 요약 메모를 입력하세요."
              />
            </div>
          </Field>

          <Field label={organizationLabel}>
            <OrganizationSelector
              key={safeFormType}
              formType={safeFormType}
              audience={safeFormData.audience}
              targets={safeFormData.targets}
              initialOrganizationSeed={organizationSeed}
              onChangeFormData={onChangeFormData}
            />
          </Field>
        </div>

        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            gap: 10,
            marginTop: 24,
            alignItems: "center",
            flexWrap: "wrap",
          }}
        >
          <AppButton
            variant="secondary"
            onClick={() => {
              onChangeFormData("title", "");
              onChangeFormData("purpose", "");
              onChangeFormData("audience", "");
              onChangeFormData("targets", []);
              onChangeFormData("detail", "");
              onChangeFormData("amount", "보통");
              onChangeFormData("referenceFiles", []);
              onChangeFormData("referenceMemo", "");
              if (referenceInputRef.current) {
                referenceInputRef.current.value = "";
              }
              setAmountMode("normal");
              setCustomAmount("");
            }}
          >
            초기화          </AppButton>

          <AppButton onClick={onGenerateDraft} disabled={generating}>
            <Icon>{I.spark}</Icon>
            {generating ? "AI 초안 생성 중..." : "AI 초안 생성"}
          </AppButton>
        </div>

        {error && (
          <div
            style={{
              marginTop: 12,
              color: "#d32f2f",
              fontSize: 13,
            }}
          >
            {error}
          </div>
        )}
      </div>
    </div>
  );
}

