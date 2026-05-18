/**
 * @FileName : AssistantHome.js
 * @Description : AiSecretary.js 전용 AI비서 홈 화면
 *                - AI 비서 진입 화면
 *                - 보고서 초안 / 회의록 정리 / 결재 사유 / 템플릿 생성으로 이동하는 카드 제공
 *                - DB에서 조회한 최근 작성 ASSISTANT 문서 목록 표시
 *                - 최근 작성 문서 클릭 시 WriterScreen으로 이동
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.12    송혜진        로직 변경(lastMessage 기준 48시간 이후 삭제)
 */

/*
  주의
  --------------------------------------------------
  - template은 문서 유형이 아니라 템플릿 생성 화면이다.
  - correction은 문장 다듬기 독립 기능이며, 현재 AssistantHome quick card에는 포함하지 않는다.
    문장 다듬기는 Sidebar의 별도 메뉴에서 진입한다.
*/

import React, { useEffect, useState } from "react";
import AppButton from "../components/AppButton";
import { docMeta } from "../constants/aiSecretaryData";
import { I, Icon } from "../constants/aiSecretaryIcons";
import { C, styles } from "../styles/aiSecretaryTheme";

export default function AssistantHome({
  onOpenForm,
  onOpenTemplate,
  recents = [],
  onRecentClick,
  loadingRecents = false,
  recentError = "",
}) {
  // AI 비서 주요 기능 카드
  // REPORT / MINUTES / APPROVAL: StartFormScreen으로 이동
  // template: TemplateScreen으로 이동
  const quicks = [
    {
      id: "REPORT",
      label: "보고서 초안",
      icon: I.file,
      color: "#EEF2FF",
      iconColor: "#4F46E5",
    },
    {
      id: "MINUTES",
      label: "회의록 정리",
      icon: I.users,
      color: "#ECFDF5",
      iconColor: "#059669",
    },
    {
      id: "APPROVAL",
      label: "결재 사유",
      icon: I.check,
      color: "#F5F3FF",
      iconColor: "#7C3AED",
    },
    {
      id: "template",
      label: "템플릿 생성",
      icon: I.spark,
      color: "#EFF6FF",
      iconColor: "#2563EB",
    },
  ];

  /**
   * recents 안전 배열
   *
   * DB 조회 전 또는 오류 시 recents가 undefined가 되어도
   * 화면이 깨지지 않게 보정한다.
   */
  const safeRecents = Array.isArray(recents) ? recents : [];

  const PAGE_SIZE = 5;
  const [recentPage, setRecentPage] = useState(1);

  const totalRecentPages = Math.ceil(safeRecents.length / PAGE_SIZE) || 1;

  /**
   * 최근 작성 목록이 줄어들었을 때 현재 페이지가 범위를 벗어나지 않도록 보정한다.
   */
  useEffect(() => {
    if (recentPage > totalRecentPages) {
      setRecentPage(totalRecentPages);
    }
  }, [recentPage, totalRecentPages]);

  const pagedRecents = safeRecents.slice(
    (recentPage - 1) * PAGE_SIZE,
    recentPage * PAGE_SIZE
  );

  /**
   * 최근 작성 목록의 문서 유형 라벨
   *
   * 문서 유형:
   * - REPORT
   * - MINUTES
   * - APPROVAL
   *
   * template은 문서 유형은 아니지만 fallback label로 유지한다.
   */
  const typeLabelMap = {
    REPORT: "보고서 초안",
    MINUTES: "회의록 정리",
    APPROVAL: "결재 사유",
    TEMPLATE: "템플릿 생성",
    template: "템플릿 생성",
  };

  /**
   * 최근 작성 날짜 표시
   *
   * DB 기반 최근 작성 데이터는 updatedAt 또는 lastMessageAt을 가질 수 있고,
   * 예전 seed 데이터는 date를 가질 수 있다.
   */
  const formatRecentDate = (doc) => {
    const value = doc?.date || doc?.updatedAt || doc?.lastMessageAt;

    if (!value) {
      return "-";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return String(value);
    }

    return date.toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  };

  return (
    <div style={styles.page}>
      {/* AI 비서 홈 상단 카드 */}
      <div
        style={{
          ...styles.card,
          color: C.text,
          background: "#fff",
          padding: 28,
        }}
      >
        <div
          style={{
            fontSize: 38,
            fontWeight: 900,
            letterSpacing: -1,
            color: C.text,
          }}
        >
          AI 비서
        </div>

        <div style={{ ...styles.sectionSub, fontSize: 16 }}>
          보고서 초안, 회의록 정리, 결재 사유, 템플릿 생성까지 문서 작성
          흐름을 한 화면에서 시작할 수 있습니다.
        </div>

        {/* 주요 기능 카드 */}
        <div
          style={{
            marginTop: 24,
            display: "grid",
            gridTemplateColumns: "repeat(4, minmax(0, 1fr))",
            gap: 14,
          }}
        >
          {quicks.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => {
                if (item.id === "template") {
                  return onOpenTemplate();
                }

                return onOpenForm(item.id);
              }}
              style={{
                ...styles.card,
                color: C.text,
                background: "#fff",
                padding: 18,
                textAlign: "left",
                cursor: "pointer",
                minHeight: 132,
              }}
            >
              <div
                style={{
                  width: 42,
                  height: 42,
                  borderRadius: 12,
                  background: item.color,
                  color: item.iconColor,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <Icon>{item.icon}</Icon>
              </div>

              <div
                style={{
                  marginTop: 14,
                  fontWeight: 800,
                  fontSize: 15,
                  color: C.text,
                }}
              >
                {item.label}
              </div>

              <div
                style={{
                  marginTop: 8,
                  fontSize: 13,
                  color: C.sub,
                  lineHeight: 1.5,
                }}
              >
                {docMeta?.[item.id]?.description || ""}
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* 최근 작성 전체 보기 */}
      <div
        style={{
          ...styles.card,
          color: C.text,
          background: "#fff",
          marginTop: 18,
          padding: 22,
        }}
      >
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 16,
          }}
        >
          <div>
            <h3 style={styles.sectionTitle}>최근 작성 전체 보기</h3>
            <div style={{ ...styles.sectionSub, marginTop: 6 }}>
              최근 작성한 문서를 한 번에 확인하고 다시 이어서 작업할 수
              있습니다.
            </div>
          </div>

          <div style={{ fontSize: 13, color: C.sub, fontWeight: 700 }}>
            총 {safeRecents.length}건 · {recentPage} / {totalRecentPages} 페이지
          </div>
        </div>

        {recentError && (
          <div
            style={{
              marginBottom: 12,
              padding: "12px 14px",
              borderRadius: 12,
              background: "#FEF2F2",
              color: "#DC2626",
              fontSize: 13,
              fontWeight: 700,
            }}
          >
            {recentError}
          </div>
        )}

        <div
          style={{
            border: `1px solid ${C.border}`,
            borderRadius: 14,
            overflow: "hidden",
          }}
        >
          {/* 테이블 헤더 */}
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1.8fr 1fr 140px 120px",
              padding: "14px 18px",
              background: "#F8FAFC",
              borderBottom: `1px solid ${C.border}`,
              fontSize: 13,
              fontWeight: 800,
              color: C.sub,
            }}
          >
            <div>문서명</div>
            <div>유형</div>
            <div>최근 수정일</div>
            <div style={{ textAlign: "center" }}>바로가기</div>
          </div>

          {/* 로딩 상태 */}
          {loadingRecents && (
            <div
              style={{
                padding: 24,
                color: C.sub,
                fontSize: 14,
                textAlign: "center",
              }}
            >
              최근 작성 목록을 불러오는 중입니다...
            </div>
          )}

          {/* 빈 상태 */}
          {!loadingRecents && pagedRecents.length === 0 && (
            <div
              style={{
                padding: 24,
                color: C.sub,
                fontSize: 14,
                textAlign: "center",
                background: "#fff",
              }}
            >
              아직 최근 작성 문서가 없습니다.
              <br />
              보고서 초안, 회의록 정리, 결재 사유 작성을 시작해 보세요.
            </div>
          )}

          {/* 최근 작성 목록 */}
          {!loadingRecents &&
            pagedRecents.map((doc, index) => (
              <div
                key={doc.id}
                style={{
                  display: "grid",
                  gridTemplateColumns: "1.8fr 1fr 140px 120px",
                  padding: "14px 18px",
                  borderBottom:
                    index === pagedRecents.length - 1
                      ? "none"
                      : `1px solid ${C.border}`,
                  alignItems: "center",
                  gap: 12,
                  fontSize: 14,
                  background: "#fff",
                }}
              >
                <div
                  style={{
                    fontWeight: 700,
                    color: C.text,
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                    whiteSpace: "nowrap",
                  }}
                  title={doc.title}
                >
                  {doc.title}
                </div>

                <div style={{ color: C.sub }}>
                  {typeLabelMap[doc.type] || "문서"}
                </div>

                <div style={{ color: C.sub }}>{formatRecentDate(doc)}</div>

                <div style={{ display: "flex", justifyContent: "center" }}>
                  <AppButton
                    variant="secondary"
                    style={{ height: 34 }}
                    onClick={() => onRecentClick(doc)}
                  >
                    열기
                  </AppButton>
                </div>
              </div>
            ))}
        </div>

        {/* 페이지네이션 */}
        {safeRecents.length > PAGE_SIZE && (
          <div
            style={{
              display: "flex",
              justifyContent: "center",
              gap: 8,
              marginTop: 18,
            }}
          >
            <AppButton
              variant="secondary"
              style={{ height: 36 }}
              onClick={() => setRecentPage((prev) => Math.max(prev - 1, 1))}
              disabled={recentPage === 1}
            >
              이전
            </AppButton>

            {Array.from({ length: totalRecentPages }, (_, index) => index + 1).map(
              (page) => (
                <AppButton
                  key={page}
                  variant={recentPage === page ? "primary" : "secondary"}
                  style={{ height: 36, minWidth: 36, padding: "0 12px" }}
                  onClick={() => setRecentPage(page)}
                >
                  {page}
                </AppButton>
              )
            )}

            <AppButton
              variant="secondary"
              style={{ height: 36 }}
              onClick={() =>
                setRecentPage((prev) => Math.min(prev + 1, totalRecentPages))
              }
              disabled={recentPage === totalRecentPages}
            >
              다음
            </AppButton>
          </div>
        )}
      </div>
    </div>
  );
}
