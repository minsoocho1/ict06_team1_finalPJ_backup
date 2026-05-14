/**
 * @FileName : aiSecretaryRouteHelpers.js
 * @Description : AiSecretary.js 전용 라우팅 규칙 / 계산 유틸
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 */

import { PATH } from "../../../constants/path";

// AI 비서 문서 작성에서 사용하는 문서 유형
// REPORT/ MINUTES/ APPROVAL
export const FORM_TYPE = {
  REPORT: "REPORT",
  MINUTES: "MINUTES",
  APPROVAL: "APPROVAL",
};

// 유효한 문서 유형 목록
export const VALID_FORM_TYPES = [
  FORM_TYPE.REPORT,
  FORM_TYPE.MINUTES,
  FORM_TYPE.APPROVAL,
];

// 문서 유형 정규화 함수
// URL query, templateCards, DB 응답, 기존 더미 데이터 등에서 최종적으로 대문자 문서 유형으로 통일
export const normalizeFormType = (type) => {
  if (!type) {
    return FORM_TYPE.REPORT;
  }

  const normalized = String(type).trim().toUpperCase();

  return VALID_FORM_TYPES.includes(normalized)
    ? normalized
    : FORM_TYPE.REPORT;
};

// AI 비서 문서 작성 화면 URL 생성
// - "/ai-portal/assistant/docs/:docId"
export const buildAssistantDocPath = (docId) =>
  PATH.AI.ASSISTANT_DOC.replace(":docId", docId);

// AI 비서 문서 상세 prefix
// 현재 URL이 WriterScreen인지 판별할 때 사용
export const ASSISTANT_DOC_PREFIX =
  PATH.AI.ASSISTANT_DOC.replace("/:docId", "");
