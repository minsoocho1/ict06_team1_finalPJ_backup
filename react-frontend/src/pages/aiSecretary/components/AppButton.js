/**
 * @FileName : AppButton.js
 * @Description : aiSecretary 전용 버튼 스타일 공통 컴포넌트
 * @Author : 송혜진
 * @Date : 2026. 04. 29
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    송혜진        최초 생성
 */

import React from "react";
import { C } from "../styles/aiSecretaryTheme";

export default function AppButton({
  children,
  variant = "primary",
  style,
  disabled = false,
  ...props
}) {
  const base = {
    height: 42,
    borderRadius: 10,
    padding: "0 16px",
    fontSize: 14,
    fontWeight: 700,
    cursor: disabled ? "not-allowed" : "pointer",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    transition: "0.15s ease",
    opacity: disabled ? 0.55 : 1,
  };

  const variants = {
    primary: {
      background: C.accent,
      color: "#fff",
      border: `1px solid ${C.accent}`,
    },
    secondary: {
      background: "#fff",
      color: C.text,
      border: `1px solid ${C.border}`,
    },
  };

  return (
    <button
      disabled={disabled}
      {...props}
      style={{
        ...base,
        ...variants[variant],
        ...style,
      }}
    >
      {children}
    </button>
  );
}