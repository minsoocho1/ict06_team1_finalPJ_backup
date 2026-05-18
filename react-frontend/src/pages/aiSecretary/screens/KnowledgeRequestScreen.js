/**
 * @FileName : KnowledgeRequestScreen.js
 * @Description : AiSecretary.js 전용 지식 추가 화면
 * @Author : 송혜진
 * @Date : 2026. 04. 30
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.30  송혜진       최초 생성
 */

import React, { useState } from "react";
import Chip from "../components/Chip";
import Field from "../components/Field";
import TextInput from "../components/TextInput";
import AppButton from "../components/AppButton";
import { I, Icon } from "../constants/aiSecretaryIcons";
import { C, styles } from "../styles/aiSecretaryTheme";

export default function KnowledgeRequestScreen() {
  const [form, setForm] = useState({
    title: "",
    description: "",
    topics: "",
    scope: "전사 공개",
  });

  const scopeOptions = ["전사 공개", "부서 공개", "특정 권한"];

  return (
    <div style={styles.page}>
      <div style={{ marginBottom: 20 }}>
        <div style={{ fontSize: 16, color: C.sub, fontWeight: 700 }}>
          지식 추가 요청
        </div>
        <h1
          style={{
            margin: "6px 0 0",
            fontSize: 38,
            fontWeight: 900,
            letterSpacing: -1,
          }}
        >
          챗봇 지식 추가 요청
        </h1>
        <p style={{ margin: "10px 0 0", color: C.sub, fontSize: 16 }}>
          챗봇 답변에 반영할 문서를 등록 요청하고, 관리자의 검토 후 승인되면
          지식베이스에 반영됩니다.
        </p>
      </div>

      <div
        style={{
          ...styles.card,
          padding: 18,
          marginBottom: 18,
          background: "#F7FAFF",
        }}
      >
        <div style={{ color: C.accent, fontWeight: 900, fontSize: 15 }}>
          사용자는 직접 반영하는 대신, 지식 추가 요청을 등록합니다.
        </div>
        <div style={{ marginTop: 8, color: C.sub, fontSize: 14 }}>
          문서의 목적, 관련 질문 주제, 공개 범위를 함께 제출하면 관리자가
          검토 후 승인 또는 반려를 결정합니다.
        </div>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 320px",
          gap: 18,
          alignItems: "start",
        }}
      >
        <div style={{ ...styles.card, padding: 24 }}>
          <div style={{ display: "grid", gap: 16 }}>
            <Field label="문서 제목" required>
              <TextInput
                placeholder="예) 2024 하반기 출장비 정산 가이드"
                value={form.title}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, title: e.target.value }))
                }
              />
            </Field>

            <Field label="문서 설명" required>
              <TextInput
                textarea
                placeholder="예) 챗봇이 출장비 정산 절차, 제출 서류, 한도 기준을 답변할 수 있도록 반영 요청합니다."
                value={form.description}
                onChange={(e) =>
                  setForm((prev) => ({
                    ...prev,
                    description: e.target.value,
                  }))
                }
              />
            </Field>

            <Field label="관련 질문 주제" required>
              <TextInput
                placeholder="예) 출장비 정산, 영수증 제출, 법인카드 사용 기준"
                value={form.topics}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, topics: e.target.value }))
                }
              />
            </Field>

            <Field label="공개 범위 제안" required>
              <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
                {scopeOptions.map((option) => (
                  <Chip
                    key={option}
                    active={form.scope === option}
                    onClick={() =>
                      setForm((prev) => ({ ...prev, scope: option }))
                    }
                  >
                    {option}
                  </Chip>
                ))}
              </div>
            </Field>

            <Field label="문서 첨부" required>
              <div
                style={{
                  border: `1px dashed ${C.border}`,
                  borderRadius: 12,
                  padding: 18,
                  color: C.sub,
                  background: "#FBFDFF",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 10,
                    fontWeight: 700,
                  }}
                >
                  <Icon>{I.clip}</Icon>
                  파일을 드래그하거나 클릭하여 업로드하세요 (최대 20MB)
                </div>
                <div style={{ marginTop: 10, fontSize: 13 }}>
                  PDF, DOCX, PPTX 파일 권장
                </div>
              </div>
            </Field>
          </div>

          <div
            style={{
              display: "flex",
              justifyContent: "flex-end",
              gap: 10,
              marginTop: 24,
            }}
          >
            <AppButton variant="secondary">임시 저장</AppButton>
            <AppButton>
              <Icon>{I.clip}</Icon>
              등록 요청 제출
            </AppButton>
          </div>
        </div>

        <div style={{ ...styles.card, padding: 22 }}>
          <h3 style={styles.sectionTitle}>요청 후 진행 절차</h3>
          <div style={{ marginTop: 18, display: "grid", gap: 14 }}>
            {[
              "1. 사용자가 문서와 요청 사유를 제출합니다.",
              "2. 관리자가 문서 내용과 공개 범위를 검토합니다.",
              "3. 승인되면 지식베이스 반영 절차가 진행됩니다.",
              "4. 반려되면 사유와 함께 상태가 표시됩니다.",
            ].map((text) => (
              <div
                key={text}
                style={{ fontSize: 14, lineHeight: 1.7, color: C.text }}
              >
                {text}
              </div>
            ))}
          </div>

          <div style={{ borderTop: `1px solid ${C.border}`, margin: "18px 0" }} />

          <div style={{ fontSize: 13, color: C.sub, lineHeight: 1.7 }}>
            민감한 문서나 권한 제한 문서는 관리자 승인 전까지 챗봇 답변에
            사용되지 않습니다.
          </div>
        </div>
      </div>
    </div>
  );
}