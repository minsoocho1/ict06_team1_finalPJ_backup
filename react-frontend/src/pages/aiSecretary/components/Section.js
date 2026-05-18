/**
 * @FileName : Section.js
 * @Description : aiSecretary 전용 WriterScreen 화면 전용 문서 본문 섹션 컴포넌트
 *                - 문서 본문 섹션 : 1. 개요 / 2. 주요 내용 / 4. 후속 계획
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    송혜진        최초 생성
 */

import React from "react";

export default function Section({ title, children }) {
  return (
    <div style={{ marginBottom: 24 }}>
      <div
        style={{
          fontSize: 28,
          fontWeight: 900,
          marginBottom: 12,
        }}
      >
        {title}
      </div>

      <div>{children}</div>
    </div>
  );
}