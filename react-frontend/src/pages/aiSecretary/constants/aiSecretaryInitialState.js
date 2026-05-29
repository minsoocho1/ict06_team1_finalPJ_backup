/**
 * @FileName : aiSecretaryInitialState.js
 * @Description : AI 비서 화면에서 공통으로 사용하는 초기 상태 모음
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진        최초 생성
 */

/* AI 비서 문서 작성 시작 화면의 기본 입력값이다.
   - StartFormScreen에서 문서 유형별 formData 초기화에 사용한다.
   - URL query type과 AiSecretary.js의 currentFormType 흐름과 함께 동작한다. */
export const initialFormData = {
  title: "", // 문서 제목
  purpose: "", // 작성 목적
  audience: "", // 대상 독자 / 수신자 / 보고 대상
  targets: ["대상"], // 정리 대상 / 보고 대상 / 결재 라인 chip 문자열
  detail: "", // 핵심 내용 / 회의 안건 / 결재 사유
  amount: "", // 원하는 분량 / 정리 방식 / 출력 스타일
  referenceFiles: [], // 첨부한 참고 자료 파일 목록
  referenceMemo: "", // 참고 자료 메모
  // 파일 본문 자동 추출 결과는 이번 문서 생성 요청에만 사용한다.
  referenceText: "",
  referenceExtractStatus: "idle",
  referenceExtractMessage: "",
  referenceExtractTruncated: false,
};

// AI 문장 교정 화면에서 사용하는 기본값이다.
export const initialCorrectionState = {
  // 교정 톤
  tone: "공손히",

  // 수정 강도
  strength: "보통",

  // 길이 조절
  length: "유지",

  // 맞춤법 교정 여부
  spellCheck: true,
};

/* AI 비서 WriterScreen에서 사용하는 기본 상태다.
   - 최초 생성된 초안
   - 문서 작성 세션 정보
   - 대화형 수정 메시지 기록
   를 함께 관리한다. */
export const initialWriterState = {
  sessionId: null, // ASSISTANT 세션 ID
  userMessageId: null, // 문서 생성 요청 USER 메시지 ID
  aiMessageId: null, // 문서 생성 결과 ASSISTANT 메시지 ID
  type: "REPORT", // 문서 유형 (REPORT / MINUTES / APPROVAL)
  title: "", // 문서 제목
  content: "", // AI가 생성한 초안 본문
  modelName: "", // 응답 생성에 사용된 모델 이름
  fallback: false, // fallback 응답 여부
  prompt: "", // 화면에서 관리하는 추가 프롬프트 정보
  showHistory: false, // 수정 이력 패널 표시 여부
  chat: [], // 사용자와 AI 간 수정 대화
  versions: [], // 문서 버전 목록
};
