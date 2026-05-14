/**
 * @FileName : Chip.js
 * @Description : aiSecretary 전용 선택형 버튼 공통 컴포넌트
 *                - 문서 작성 시작 화면, 템플릿 생성 화면, 문장 다듬기 옵션 화면 등
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

export default function Chip({ active, children, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        height: 38,
        padding: "0 14px",
        borderRadius: 10,
        border: `1px solid ${active ? C.accent : C.border}`,
        background: active ? C.accentBg : "#fff",
        color: active ? C.accent : C.text,
        fontSize: 14,
        fontWeight: active ? 700 : 500,
        cursor: onClick ? "pointer" : "default",
        display: "inline-flex",
        alignItems: "center",
        gap: 8,
      }}
    >
      {children}
    </button>
  );
}