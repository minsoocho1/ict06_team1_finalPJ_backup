/**
 * @FileName : Bubble.js
 * @Description : aiSecretary 전용 채팅 말풍선 공통 컴포넌트
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 */

import React from "react";
import { C } from "../styles/aiSecretaryTheme";

const isLinkableReference = (url) =>
  typeof url === "string" &&
  (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/"));

export default function Bubble({ role, text, time, references = [] }) {
  const isUser = role === "user";
  const isAi = role === "ai" || role === "ASSISTANT";
  const hasReferences = isAi && Array.isArray(references) && references.length > 0;

  return (
    <div
      style={{
        display: "flex",
        justifyContent: isUser ? "flex-end" : "flex-start",
      }}
    >
      <div style={{ maxWidth: "88%" }}>
        <div
          style={{
            background: isUser ? C.accentBg : "#fff",
            border: `1px solid ${isUser ? "#C7DBFF" : C.border}`,
            borderRadius: isUser
              ? "16px 16px 6px 16px"
              : "16px 16px 16px 6px",
            padding: "14px 14px",
            whiteSpace: "pre-line",
            lineHeight: 1.6,
            fontSize: 14,
          }}
        >
          {text}
        </div>

        <div
          style={{
            marginTop: 6,
            fontSize: 12,
            color: C.muted,
          }}
        >
          {time}
        </div>

        {hasReferences && (
          <div
            style={{
              marginTop: 10,
              padding: "10px 12px",
              borderRadius: 12,
              border: `1px solid ${C.border}`,
              background: "#f8fafc",
            }}
          >
            <div
              style={{
                fontSize: 12,
                fontWeight: 700,
                color: "#374151",
                marginBottom: 8,
              }}
            >
              참고한 문서
            </div>

            <div style={{ display: "grid", gap: 10 }}>
              {references.map((reference, index) => (
                <div key={`${reference?.docId ?? "ref"}-${index}`}>
                  <div style={{ fontSize: 13, color: "#111827", lineHeight: 1.5 }}>
                    • {reference?.title || "참고 문서"}
                  </div>
                  <div style={{ marginTop: 4, marginLeft: 12, fontSize: 12 }}>
                    {isLinkableReference(reference?.url) ? (
                      <a
                        href={reference.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{ color: C.accent, textDecoration: "none", fontWeight: 600 }}
                      >
                        문서 열기
                      </a>
                    ) : (
                      <span style={{ color: C.muted }}>문서 링크 없음</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
