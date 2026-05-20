/**
 * @FileName : path.js
 * @Description : 프론트엔드 라우팅 및 API 경로 상수 정의
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.19    김다솜        알림 SSE 구독 경로 상수 추가
 */

// constants/path.js
// 1) 단순 URL 주소 문자열을 상수로 정의하는 단계 (데이터)
// path.js(여기!) -> routes/대분류 별 파일 -> routes/index.js -> App.js 

export const PATH = {
  ROOT: "/",

  // 학습용 예제
  TEST: {
    LIST: "/test/list"
  },

  // 대분류 : 인증/인가   
  AUTH: {
    LOGIN: "/auth/login",
    WELCOME: "/auth/welcome",
    USERHOME: "/auth/userhome",
    MYPAGE: "/auth/mypage"
  },

  // 대분류 : 근태 관리
  ATTENDANCE: {
    ROOT: "/attendance",                // 근태 관리 페이지
    COMMUTE: "/attendance/commute",     // 출퇴근 처리 페이지
    STATS: "/attendance/stats",         // 근태 통계 페이지    
    HOLIDAYS: "/attendance/holidays",   // 연차 현황 페이지
  },

  // 대분류 : 캘린더
  CALENDAR: {
    ROOT: "/calendar",                  // 캘린더 메인 페이지
    SIMPLE_ADD: "/calendar/simple-add", // 일정 간단 등록 페이지
    DETAIL_ADD: "/calendar/detail-add", // 상세 등록 / 반복 / 참석자 일정 페이지
    DETAIL: "/calendar/detail",         // 일정 상세/삭제 페이지
  },

  // 대분류 : 전자결재
  APPROVAL: {
    ROOT: "/approval",                                      // 전자결재 메인 페이지
    NEW_SELECT: "/approval/new/select-form",                //  새 결재 진행 - 결재 서식 선택 페이지
    NEW_WRITE: "/approval/new/write",                       // 새 결재 진행 - 결재 내용 작성 페이지
    NEW_SETLINE: "/approval/new/set-line",                  // 새 결재 진행 - 결재선 설정 페이지
    TMP: "/approval/tmpApprovals",                          // 임시저장함 페이지
    TMP_DETAIL: "/approval/tmpApprovals/detail",             // 임시저장 문서 상세 페이지
    TMP_DETAIL_WITH_ID: (approvalId) => `/approval/tmpApprovals/detail?approvalId=${approvalId}`,
    PERSONAL: "/approval/personalApprovals",                // 개인 문서함 페이지
    PERSONAL_DETAIL: "/approval/personalApprovals/detail",  // 개인 문서 상세 페이지
    PERSONAL_DETAIL_WITH_ID: (approvalId) => `/approval/personalApprovals/detail?approvalId=${approvalId}`,
    PENDING: "/approval/pendingApprovals",                  // 결재 대기 문서함 페이지
    PENDING_DETAIL: "/approval/pendingApprovals/detail",    // 결재 대기 문서 상세 페이지
    PENDING_DETAIL_WITH_ID: (approvalId) => `/approval/pendingApprovals/detail?approvalId=${approvalId}`,
    UPCOMING: "/approval/upcomingApprovals",                // 결재 예정 문서함 페이지
    UPCOMING_DETAIL: "/approval/upcomingApprovals/detail",  // 결재 예정 문서 상세 페이지
    UPCOMING_DETAIL_WITH_ID: (approvalId) => `/approval/upcomingApprovals/detail?approvalId=${approvalId}`,
  },

  // 대분류 : 인사평가 - 온보딩
  ONBOARDING: {
    ROOT: "/onboarding",
    ROADMAP: "/onboarding/myroadmap",         // 로드맵
    DASHBOARD: "/onboarding/dashboard",           // 대시보드
    LEARNING: (contentId) => `/onboarding/learning/${contentId}`,
    LEARNING_DETAIL: "/onboarding/learning/:contentId",           // 학습 상세 페이지
    PROGRESS_COMPLETE: "/onboarding/progress/complete",
    CHECKLIST: "/onboarding/checklist",
  }, 

  // 대분류 : 인사평가 - AI 퀴즈 및 평가
  EVALUATION: {
    // 사이드바 진입(평가 현황)
    ROOT: "/evaluation",
    // 퀴즈 응시(로드맵에서 연결)
    QUIZ: (categoryName) => `/evaluation/quiz?categoryName=${encodeURIComponent(categoryName)}`,

    // 평가 결과 조회(평가 현황에서 연결)
    RESULT: "/evaluation/result",
    
    // 평가 결과 상세 조회(함수형)
    QUIZ_DETAIL: (empNo, categoryName) => `/evaluation/result/detail/${empNo}/${encodeURIComponent(categoryName)}`,
    // 평가 결과 상세 조회(라우트 등록용)
    QUIZ_DETAIL_PATTERN: "/evaluation/result/detail/:empNo/:categoryName",
  }, 
  
  // 인사평가 - 외부 AI 서버 통신용
  AI_API: {
    BASE: process.env.REACT_APP_AI_SERVER_URL || 'http://localhost:8000/api',
    ROADMAP: (empNo) => `/ai/roadmap/${empNo}`,
    CONTENT_DETAIL: (contentId) => `/ai/content/${contentId}`,     // 학습자료 상세 조회
  },

  // 대분류 : 인사관리
  EMPLOYEE: {
    ROOT: "/employee",          // 인사관리 메인 페이지
    DETAIL: "/employee/detail", // 인사관리 상세 페이지
  },

  // 대분류 : 급여관리
  PAYROLL: {
    ROOT: "/payroll",           // 사원별 급여 확인 페이지
    ISSUE: "/payroll/issue",    // 사원별 급여명세서 발급 페이지
  },

  // 사내 AI 포털
  AI: {
    ROOT: "/ai-portal",

    // AI 비서
    ASSISTANT: "/ai-portal/assistant",                    // AI 비서 홈
    ASSISTANT_NEW: "/ai-portal/assistant/new",            // 새 문서 작성 시작
    ASSISTANT_TEMPLATE: "/ai-portal/assistant/template",  // 템플릿 생성
    ASSISTANT_DOC: "/ai-portal/assistant/docs/:docId",    // 기존 문서/이전 대화 진입

    // 추가 기능
    CORRECTION: "/ai-portal/correction",                  // 문장 다듬기
    KNOWLEDGE_REQUEST: "/ai-portal/knowledge-request",    // 지식 추가 요청

    // AI 챗봇
    CHATBOT: "/ai-portal/chatbot",                        // AI 챗봇 메인
  },

  ETC: {
    // 대분류 : 실시간 알림
    ALERT: "/alert",
  },

  // SpringBoot REST API
  API: {
    BASE: process.env.REACT_APP_SERVER_URL || 'http://localhost:8081/api',
    USER_ME: '/user/me',
    USER_UPDATE: '/user/update',

    NOTIFICATION: {
      SUBSCRIBE: (token) => `${PATH.API.BASE}/noti/subscribe?token=${encodeURIComponent(token)}`,
    },

    APPROVAL: {
      FORMS: '/approval/forms',
      FORM_DETAIL: (formId) => `/approval/forms/${formId}`,
      LINE_TEMPLATE_DETAIL: (templateId) => `/approval/line-templates/${templateId}`,
      EMPLOYEES: '/approval/employees',
      EMPLOYEE_SIGN: (empNo) => `/approval/employees/${empNo}/sign`,
      MY_DOCUMENTS: '/approval/my-documents',
      REFERENCED_DOCUMENTS: '/approval/referenced-documents',
      PENDING_DOCUMENTS: '/approval/pending-documents',
      PROCESSED_DOCUMENTS: '/approval/processed-documents',
      UPCOMING_DOCUMENTS: '/approval/upcoming-documents',
      DETAIL: (approvalId) => `/approval/${approvalId}`,
      APPROVE: (approvalId) => `/approval/${approvalId}/approve`,
      REJECT: (approvalId) => `/approval/${approvalId}/reject`,
      CANCEL: (approvalId) => `/approval/${approvalId}/cancel`,
      DRAFTS: '/approval/drafts',
      UPDATE_DRAFT: (approvalId) => `/approval/drafts/${approvalId}`,
      DELETE_DRAFT: (approvalId) => `/approval/drafts/${approvalId}`,
      SUBMIT_DRAFT: (approvalId) => `/approval/drafts/${approvalId}/submit`,
      DELETE_FILE: (fileId) => `/approval/files/${fileId}`,
      SUBMIT: '/approval/submit',
    },

    ONBOARDING: {
      ROADMAP: (empNo) => `/onboarding/roadmap/${empNo}`,
      DASHBOARD: (empNo) => `/onboarding/dashboard/${empNo}`,
      CONTENT_DETAIL: (contentId) => `/onboarding/dashboard/content/${contentId}`,
      CONTENT_SELF_CHECK: (contentId) => `/onboarding/dashboard/content/${contentId}/self-check`,
      CHECKLIST_LIST: (empNo) => `/onboarding/checklist/${empNo}`,
      CHECKLIST_COMPLETE: "/onboarding/checklist/complete",
      CHECKLIST_UNCOMPLETE: "/onboarding/checklist/uncomplete",
      PROGRESS_COMPLETE: "/onboarding/progress/complete",
      PROGRESS_UNCOMPLETE: "/onboarding/progress/uncomplete",
    },
    EVALUATION: {
      QUIZ_QUESTIONS: (categoryName) => `/evaluation/quiz/category/${encodeURIComponent(categoryName)}`,
      QUIZ_SUBMIT: "/evaluation/quiz/submit",
      QUIZ_RESULT: (empNo) => `/evaluation/result/${empNo}`,
      QUIZ_DETAIL: (empNo, categoryName) => `/evaluation/result/detail/${empNo}/${encodeURIComponent(categoryName)}`,
    }
  }
};
