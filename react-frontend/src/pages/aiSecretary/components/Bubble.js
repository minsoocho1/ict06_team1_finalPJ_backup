/**
 * @FileName : Bubble.js
 * @Description : aiSecretary 전용 채팅 말풍선 공통 컴포넌트
 *                - 역할(role)에 따라 정렬, 배경색, borderRadius만 변경
 *                - 활용: WriterScreen, CorrectionScreen, ChatbotScreen
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    송혜진        최초 생성
 */

import React from "react";
import { C } from "../styles/aiSecretaryTheme";

export default function Bubble({ role, text, time }) {
  const isUser = role === "user";

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
      </div>
    </div>
  );
}