/**
 * @FileName : CompareCard.js
 * @Description : aiSecretary 전용 문장 다듬기 화면 전용 원문/ 교정 결과 카드 컴포넌트
 *                - title, body, accent 여부만 props로 받아 재사용 가능하게 분리
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

export default function CompareCard({
  title,
  body,
  accent = false,
}) {
  return (
    <div
      style={{
        border: `1px solid ${C.border}`,
        borderRadius: 14,
        overflow: "hidden",
        background: "#fff",
      }}
    >
      <div
        style={{
          padding: "14px 16px",
          borderBottom: `1px solid ${C.border}`,
          fontWeight: 800,
          color: accent ? C.accent : C.text,
        }}
      >
        {title}
      </div>

      <div
        style={{
          padding: 18,
          minHeight: 260,
          display: "grid",
          alignContent: "start",
          gap: 10,
          lineHeight: 1.9,
          fontSize: 14,
        }}
      >
        {body.map((line) => (
          <div key={line}>{line}</div>
        ))}

        {/* 현 하드코딩 - 추후 수정:
            현재 프로토타입은 첨부 파일이 포함된 교정 화면 흐름을 보여주는 것이 목적이므로
            카드 하단에 고정된 첨부 미리보기 박스를 유지함 */}
        <div
          style={{
            marginTop: 24,
            padding: 12,
            borderRadius: 10,
            background: "#F8FAFC",
            border: `1px solid ${C.border}`,
            fontSize: 12,
            color: C.sub,
          }}
        >
          첨부: 3분기 마케팅 성과 보고서_초안.pptx (10.2MB)
        </div>
      </div>
    </div>
  );
}