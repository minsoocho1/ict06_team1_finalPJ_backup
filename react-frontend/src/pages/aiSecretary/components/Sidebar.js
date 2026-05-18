/**
 * @FileName : Sidebar.js
 * @Description : 사내 AI 포털 전용 사이드바
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진      최초 생성 / BACKEND 연계
 * @ 2026.05.07    송혜진      문서 유형 type 값을 REPORT / MINUTES / APPROVAL 기준으로 정리
 */

import React from "react";
import { docMeta } from "../constants/aiSecretaryData";
import { I, Icon } from "../constants/aiSecretaryIcons";
import { C, styles } from "../styles/aiSecretaryTheme";

const topItems = [
  {
    id: "assistant",
    label: "AI 비서",
    description: "보고서·회의록·결재 사유 초안을 작성합니다.",
    icon: I.file,
  },
  {
    id: "correction",
    label: "문장 다듬기",
    description: "문장의 길이, 표현, 톤을 더 자연스럽게 정리합니다.",
    icon: I.mail,
  },
];

const middleItems = [
  {
    id: "chatbot",
    label: "AI 챗봇",
    description: "사내 규정과 업무 지식을 빠르게 질의응답합니다.",
    icon: I.chat,
  },
  {
    id: "knowledge-request",
    label: "챗봇 지식 반영 요청",
    description: "챗봇 지식에 반영할 자료를 등록하거나 요청합니다.",
    icon: I.clip,
  },
];

const typeLabelMap = {
  REPORT: "보고서 초안",
  MINUTES: "회의록 정리",
  APPROVAL: "결재 사유",
};

function renderMenuButton(item, active, onTabChange) {
  return (
    <button
      key={item.id}
      type="button"
      onClick={() => onTabChange?.(item.id)}
      style={{
        width: "100%",
        border: "none",
        borderRadius: 14,
        padding: "12px 14px",
        background: active ? C.accentBg : "transparent",
        color: active ? C.accent : C.text,
        cursor: "pointer",
        textAlign: "left",
        display: "flex",
        gap: 12,
        alignItems: "flex-start",
      }}
    >
      <div
        style={{
          width: 34,
          height: 34,
          borderRadius: 10,
          background: active ? "#fff" : "#F8FAFC",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          color: active ? C.accent : C.sub,
          flexShrink: 0,
        }}
      >
        <Icon>{item.icon}</Icon>
      </div>

      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 900 }}>{item.label}</div>
        <div
          style={{
            marginTop: 4,
            fontSize: 12,
            color: active ? C.accent : C.sub,
            lineHeight: 1.4,
          }}
        >
          {item.description}
        </div>
      </div>
    </button>
  );
}

export default function Sidebar({
  tab = "assistant",
  onTabChange,
  recents = [],
  onRecentClick,
}) {
  const safeRecents = Array.isArray(recents) ? recents : [];
  const latestRecents = safeRecents.slice(0, 3);

  return (
    <aside style={styles.sidebar}>
      <div style={{ padding: 22, borderBottom: `1px solid ${C.border}` }}>
        <div style={{ fontSize: 20, fontWeight: 900, color: C.text }}>
          사내 AI 포털
        </div>
        <div style={{ marginTop: 6, fontSize: 12, color: C.sub }}>
          AI 비서와 챗봇을 한 곳에서 사용합니다.
        </div>
      </div>

      <div style={{ padding: 16, display: "grid", gap: 8 }}>
        {topItems.map((item) =>
          renderMenuButton(item, tab === item.id, onTabChange)
        )}
      </div>

      <div style={{ padding: "0 16px" }}>
        <div
          style={{
            paddingTop: 16,
            borderTop: `1px solid ${C.border}`,
          }}
        />

        <div style={{ display: "grid", gap: 8 }}>
          {middleItems.map((item) =>
            renderMenuButton(item, tab === item.id, onTabChange)
          )}
        </div>
      </div>

      <div style={{ padding: "16px 18px 16px" }}>
        <div
          style={{
            borderTop: `1px solid ${C.border}`,
            paddingTop: 14,
          }}
        >
          <div style={{ fontSize: 12, fontWeight: 900, color: C.muted }}>
            최근 작성
          </div>

          <div style={{ marginTop: 10, display: "grid", gap: 8 }}>
            {latestRecents.length === 0 ? (
              <div style={{ fontSize: 12, color: C.sub, lineHeight: 1.5 }}>
                아직 최근 작성 문서가 없습니다.
              </div>
            ) : (
              latestRecents.map((doc) => (
                <button
                  key={doc.id}
                  type="button"
                  onClick={() => onRecentClick?.(doc)}
                  style={{
                    border: `1px solid ${C.border}`,
                    borderRadius: 12,
                    background: "#fff",
                    padding: 10,
                    cursor: "pointer",
                    textAlign: "left",
                  }}
                >
                  <div
                    style={{
                      fontSize: 13,
                      fontWeight: 800,
                      color: C.text,
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                    }}
                    title={doc.title}
                  >
                    {doc.title || "제목 없는 AI 문서"}
                  </div>

                  <div
                    style={{
                      marginTop: 4,
                      fontSize: 11,
                      color: C.sub,
                    }}
                  >
                    {typeLabelMap[doc.type] || docMeta?.[doc.type]?.label || "문서"}
                  </div>
                </button>
              ))
            )}
          </div>
        </div>
      </div>
    </aside>
  );
}
