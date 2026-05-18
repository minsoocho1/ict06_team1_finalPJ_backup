/**
 * @FileName : aiSecretaryTheme.js
 * @Description : AiSecretary.js 전용 CSS
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 */

import React from "react";

export const C = {
  bg: "#F4F7FB",
  sidebar: "#FFFFFF",
  card: "#FFFFFF",
  border: "#DDE3EA",
  text: "#111827",
  sub: "#6B7280",
  muted: "#94A3B8",
  accent: "#2563EB",
  accentBg: "#EEF2FF",
  success: "#059669",
  softBlue: "#EFF6FF",
  softGreen: "#ECFDF5",
};

export const styles = {
  app: {
    display: "flex",
    minHeight: "100vh",
    background: C.bg,
    fontFamily: "Pretendard, Apple SD Gothic Neo, Noto Sans KR, sans-serif",
    color: C.text,
  },

  sidebar: {
    width: 272,
    background: C.sidebar,
    borderRight: `1px solid ${C.border}`,
    display: "flex",
    flexDirection: "column",
    flexShrink: 0,
  },

  main: {
    flex: 1,
    minWidth: 0,
    display: "flex",
    flexDirection: "column",
  },

  page: {
    padding: 28,
  },

  card: {
    background: C.card,
    border: `1px solid ${C.border}`,
    borderRadius: 18,
    boxShadow: "0 2px 10px rgba(15, 23, 42, 0.03)",
  },

  sectionTitle: {
    fontSize: 15,
    fontWeight: 800,
    margin: 0,
  },

  sectionSub: {
    fontSize: 13,
    color: C.sub,
    marginTop: 6,
    lineHeight: 1.5,
  },
};