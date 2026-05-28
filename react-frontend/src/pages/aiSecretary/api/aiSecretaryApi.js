/**
 * @FileName : aiSecretaryApi.js
 * @Description : AI 비서 / AI 챗봇 세션 및 메시지 관련 API
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진       세션 생성/조회 / 백엔드 연계
 * @ 2026.05.05    송혜진       문장 다듬기 API 추가
 * @ 2026.05.06    송혜진       AI 비서 문서 생성 관련 API 추가
 * @ 2026.05.07    송혜진       문서 유형 type 값을 REPORT / MINUTES / APPROVAL 기준으로 정리
 * @ 2026.05.07    송혜진       템플릿 생성 요청 API + 자료 등록 요청 API 추가
 */

import axios from "axios";
import { PATH } from 'src/constants/path';

// AI 비서 전용 axios instance
const api = axios.create({
  baseURL: PATH.API.BASE,
  withCredentials: true, // CORS 환경에서 인증 쿠키를 함께 전송
});

// JWT access token 자동 주입
// 로그인 후 localStorage에 저장된 token이 있으면 AI 비서 API 요청 헤더에
// Authorization: Bearer ... 형태로 자동 추가합니다.
api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem("accessToken");

  if (accessToken) {
    config.headers = config.headers || {};
    config.headers.Authorization = accessToken.startsWith("Bearer ")
      ? accessToken
      : `Bearer ${accessToken}`;
  }

  return config;
}, (error) => {
  return Promise.reject(error);
});


// 공통 ApiResponse wrapper unwrap
// response.data.data -> unwrapApiData(response) 형태로 실제 data만 꺼냅니다.
export const unwrapApiData = (response) => {
  if (response?.data?.data !== undefined) return response.data.data;
  if (response?.data !== undefined) return response.data;
  return response;
};

/* 공통 응답 wrapper 예시
 * {
 *   success: true,
 *   message: "...",
 *   data: ...
 * }
 */

// 문서 유형 값을 프론트/DB 공통 문서 유형 기준으로 정리
export const toApiDocumentType = (type) => {
  const normalized = String(type || "").trim().toUpperCase();

  if (normalized === "MINUTES") return "MINUTES";
  if (normalized === "APPROVAL") return "APPROVAL";
  return "REPORT";
};

// 문서 유형 type 값을 payload 전송 전에 API 기준으로 정리
const normalizeDocumentPayload = (payload = {}) => ({
  ...payload,
  type: toApiDocumentType(payload?.type),
});

// -----------------------------------------------------
// 세션 관리 API
// -----------------------------------------------------

/**
 * 공용 세션 생성
 *
 * 실제 사용 예시
 * - CHATBOT 세션은 getOrCreateChatbotSession 사용
 * - ASSISTANT 세션은 /assistant/draft 내부에서 자동 생성되므로 직접 호출 빈도는 낮음
 *
 * 현재는 범용 예비 API 성격으로 유지합니다.
 */
export const createSession = (payload) =>
  api.post("/ai-secretary/sessions", payload);


// CHATBOT 단일 최근 세션 조회 또는 생성
export const getOrCreateChatbotSession = (empNo) =>
  api.post("/ai-secretary/chatbot/session", null, {
    params: {
      empNo: String(empNo),
    },
  });

// AI 비서 최근 작성용 ASSISTANT 세션 목록 조회
export const getAssistantSessionList = (empNo) =>
  api.get("/ai-secretary/sessions", {
    params: {
      empNo: String(empNo),
      sessionType: "ASSISTANT",
    },
  });

// 공용 세션 목록 조회
export const getSessionList = (empNo, sessionType) =>
  api.get("/ai-secretary/sessions", {
    params: {
      empNo: String(empNo),
      sessionType,
    },
  });

// 세션별 메시지 목록 조회
// 챗봇 메시지, 문서 작성 이력, ASSISTANT 세션 이력을 같은 구조로 조회합니다.
export const getMessages = (sessionId) =>
  api.get(`/ai-secretary/sessions/${sessionId}/messages`);

// 세션별 메시지 저장
export const sendMessage = (sessionId, payload) =>
  api.post(`/ai-secretary/sessions/${sessionId}/messages`, payload);

// -----------------------------------------------------
// 챗봇 API
// -----------------------------------------------------

// Gemini 기반 챗봇 질문/답변
// USER 메시지 저장, Gemini 응답 생성, ASSISTANT 메시지 저장, AI_LOG 기록 흐름과 연결됩니다.
export const askChatbot = (payload) =>
  api.post("/ai-secretary/chatbot/ask", payload);

// -----------------------------------------------------
// 문장 다듬기 API
// -----------------------------------------------------

// 문장 다듬기 요청
// 입력 문장과 mode를 기준으로 Gemini가 문장을 자연스럽게 정리하고 AI_LOG에 기록합니다.
export const correctText = (payload) =>
  api.post("/ai-secretary/correction", payload);

// -----------------------------------------------------
// AI 비서 문서 생성 API
// -----------------------------------------------------

// AI 비서 문서 초안 생성
export const createAssistantDraft = (payload) =>
  api.post(
    "/ai-secretary/assistant/draft",
    normalizeDocumentPayload(payload)
  );

// 참고 자료 첨부 파일을 서버로 보내 본문 텍스트만 추출합니다.
export const extractReferenceText = (file) => {
  const formData = new FormData();
  formData.append("file", file);

  return api.post("/ai-secretary/assistant/reference/extract", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
};

// AI 비서 문서 수정
export const reviseAssistantDraft = (payload) =>
  api.post(
    "/ai-secretary/assistant/revise",
    normalizeDocumentPayload(payload)
  );

// -----------------------------------------------------
// AI 비서 > 템플릿 생성 요청 API
// -----------------------------------------------------

// 템플릿 생성 요청 등록
export const createTemplateRequest = (payload) =>
  api.post(
    "/ai-secretary/template-request",
    normalizeDocumentPayload(payload)
  );

// 내 템플릿 생성 요청 목록 조회
export const getMyTemplateRequests = (empNo) =>
  api.get("/ai-secretary/template-request/my", {
    params: {
      empNo: String(empNo),
    },
  });

// AI 템플릿 생성
export const createAssistantTemplate = (payload) =>
  api.post("/ai-secretary/assistant/template", {
    ...payload,
    type: payload?.type ? toApiDocumentType(payload.type) : undefined,
  });

// -----------------------------------------------------
// 조직도 조회 API
// -----------------------------------------------------

export const getDepartmentTree = () =>
  api.get("/organization/departments/tree");

export const getEmployeesByDepartment = (deptId) =>
  api.get("/organization/employees", {
    params: {
      deptId: String(deptId),
    },
  });

// -----------------------------------------------------
// 지식 반영 요청 API
// -----------------------------------------------------

export const createKnowledgeRequest = (payload) =>
  api.post("/ai-secretary/knowledge-request", payload);

export const getKnowledgeRequestSuggestions = () =>
  api.get("/ai-secretary/knowledge-request/suggestions");

export const getMyKnowledgeRequests = (empNo) =>
  api.get("/ai-secretary/knowledge-request/my", {
    params: {
      empNo: String(empNo),
    },
  });
