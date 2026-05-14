/**
 * @FileName : aiSecretaryInitialState.js
 * @Description : AI 비서 전용 초기 상태 묶음
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 */

/* AI 비서 문서 작성 시작 폼 초기값 (사용 : StartFormScreen)
   - 문서 유형 자체는 formData에 저장하지 않음
   - URL query type과 AiSecretary.js의 currentFormType에서 관리 */
export const initialFormData = {
  title: "", // 문서 제목
  purpose: "", // 작성 목적
  audience: "", // 대상 독자 / 참석자 / 결재 대상
  targets: ["팀장"], // 보고 대상 / 정리 대상 / 결재 라인 chip 선택값
  detail: "", // 핵심 내용 / 회의 내용 / 결재 사유
  amount: "", // 원하는 분량 / 정리 방식 / 강조 포인트
  referenceFiles: [], // 참고 자료 첨부 파일 목록
  referenceMemo: "", // 참고 자료 메모
};

// 문장 다듬기 초기값 (사용: CorrectionScreen)
export const initialCorrectionState = {
  // 문장 톤
  tone: "공손",

  // 수정 강도
  strength: "보통",

  // 길이 조정
  length: "유지",

  // 맞춤법 검사 여부
  spellCheck: true,
};

/* AI 비서 WriterScreen 초기값 (사용 : WriterScreen/ AiSecretary)
   - 실제 데이터 주입 위치

   1. AI 초안 생성 성공 시
   : AiSecretary.js의 handleGenerateDraft()
 
   2. 최근 작성 문서 클릭 시
   : .js의 writer 화면 진입 useEffect()
 
   3. 추가 수정 요청 성공 시
   : WriterScreen.js의 addMessage() */
export const initialWriterState = {
  sessionId: null,     // ASSISTANT 세션 ID
  userMessageId: null, // 최초 USER 메시지 ID
  aiMessageId: null,   // 현재 최신 ASSISTANT 메시지 ID
  type: "REPORT",      // 문서 유형 (REPORT/ MINUTES/ APPROVAL)
  title: "",           // 문서 제목
  content: "",         // 현재 화면에 표시할 최신 문서 본문
  modelName: "",       // 응답 생성 모델명(gemini/ gemini-fallback)
  fallback: false,     // fallback 응답 여부
  prompt: "",          // 추가 수정 요청 입력값
  showHistory: false,  // 버전 기록 패널 표시 여부 (“버전 기록” 버튼 클릭 시 true로 변경)
  chat: [],     // 좌측 AI 대화 영역 메시지
  versions: [], // 문서 버전 목록
};
