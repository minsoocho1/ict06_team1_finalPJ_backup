/**
 * @FileName : OptionGroup.js
 * @Description : 문장 다듬기 화면 전용 섹션 컴포넌트
 *                - 활용: 문장 다듬기 화면 => 톤 / 수정 강도 / 길이 조절
 *                - label + options + selected + onChange 패턴
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    송혜진        최초 생성
 */

import React from "react";
import Chip from "./Chip";

export default function OptionGroup({
  label,
  options,
  selected,
  onChange,
}) {
  return (
    <div>
      <div
        style={{
          fontSize: 14,
          fontWeight: 800,
          marginBottom: 10,
        }}
      >
        {label}
      </div>

      <div
        style={{
          display: "flex",
          gap: 8,
          flexWrap: "wrap",
        }}
      >
        {options.map((option) => (
          <Chip
            key={option}
            active={selected === option}
            onClick={() => onChange(option)}
          >
            {option}
          </Chip>
        ))}
      </div>
    </div>
  );
}