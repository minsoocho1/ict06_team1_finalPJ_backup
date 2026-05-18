/**
 * @FileName : Field.js
 * @Description : aiSecretary 전용 작은 조각들 (Field)
 *                - 라벨 / 필수표시 / children 입력영역 공통 컴포넌트 분리
 *                - 활용: 입력 폼의 라벨 + 입력영역 배치는 StartFormScreen, KnowledgeRequestScreen 등
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    송혜진        최초 생성
 */

import React from "react";

export default function Field({ label, required, children }) {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "136px 1fr",
        gap: 14,
        alignItems: "start",
      }}
    >
      <div style={{ paddingTop: 12, fontSize: 14, fontWeight: 700 }}>
        {label}
        {required && (
          <span style={{ color: "#DC2626", marginLeft: 4 }}>*</span>
        )}
      </div>

      {/* children: 실제 input, textarea, chip 영역 */}
      <div>{children}</div>
    </div>
  );
}