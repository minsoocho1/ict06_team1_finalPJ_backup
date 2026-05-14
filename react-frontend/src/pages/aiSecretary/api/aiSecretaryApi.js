/**
 * @FileName : aiSecretaryApi.js
 * @Description : AI 비서 / AI 챗봇 세션 및 메시지 API
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진        최초 생성 / BACKEND 연결
 * @ 2026.05.05    송혜진        문장 다듬기 API 추가
 * @ 2026.05.06    송혜진        AI 비서 리스트 추가 API 추가
 * @ 2026.05.07    송혜진        문서 유형 type 값을 REPORT / MINUTES / APPROVAL 기준으로 보정
 * @ 2026.05.07    송혜진        템플릿 요청 API + 조직 조회 API 추가
 */

import axios from "axios";
import { PATH } from 'src/constants/path';

// AI 비서 전용 axios instance
const api = axios.create({
  baseURL: PATH.API.BASE,
  withCredentials: true, // CORS 상황에서 쿠키/인증 정보를 허용
});

// JWT 토큰 자동 첨부 ()
// 로그인 성공 후 localStorage에 저장된 token을 꺼내 모든 AI 비서 API 요청에 Authorization Bearer 헤더로 붙임
api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem("accessToken");

  if (accessToken) {
    config.headers.Authorization = accessToken.startsWith("Bearer ")
      ? accessToken
      : `Bearer ${accessToken}`;
  }

  return config;
}, (error) => {
  return Promise.reject(error);
});


// 백엔드 ApiResponse 구조 unwrap
// response.data.data -> unwrapApiData(response)로 실제 data만 사용하기 위해
export const unwrapApiData = (response) => {
  if (response?.data?.data !== undefined) return response.data.data;
  if (response?.data !== undefined) return response.data;
  return response;
};

/* 백엔드 응답 구조:
 * {
 *   success: true,
 *   message: "...",
 *   data: ...
 * }
 */

// 문서 유형을 백엔드/DB 기준 대문자로 보정
export const toApiDocumentType = (type) => {
  const normalized = String(type || "").trim().toUpperCase();

  if (normalized === "MINUTES") return "MINUTES";
  if (normalized === "APPROVAL") return "APPROVAL";
  return "REPORT";
};

// 문서 유형 type이 들어가는 payload를 API 기준으로 보정
const normalizeDocumentPayload = (payload = {}) => ({
  ...payload,
  type: toApiDocumentType(payload?.type),
});

// -----------------------------------------------------
// 세션 관련 API
// -----------------------------------------------------

/**
 * 공통 세션 생성
 *
 * 현재 주요 흐름:
 * - CHATBOT 세션은 getOrCreateChatbotSession 사용
 * - ASSISTANT 세션은 /assistant/draft 내부에서 백엔드가 생성
 *
 * 따라서 이 함수는 단독 테스트 또는 예비용에 가깝다.
 */
export const createSession = (payload) =>
  api.post("/ai-secretary/sessions", payload);


// CHATBOT 최근 세션 조회 또는 생성
export const getOrCreateChatbotSession = (empNo) =>
  api.post("/ai-secretary/chatbot/session", null, {
    params: {
      empNo: String(empNo),
    },
  });

// AI 비서 최근 작성 목록 조회
export const getAssistantSessionList = (empNo) =>
  api.get("/ai-secretary/sessions", {
    params: {
      empNo: String(empNo),
      sessionType: "ASSISTANT",
    },
  });

// 공통 세션 목록 조회
export const getSessionList = (empNo, sessionType) =>
  api.get("/ai-secretary/sessions", {
    params: {
      empNo: String(empNo),
      sessionType,
    },
  });

// 세션 내 메시지 목록 조회 (챗봇 메시지 재조회/ 최근 작성 문서 클릭 시 ASSISTANT 세션 메시지 로딩)
export const getMessages = (sessionId) =>
  api.get(`/ai-secretary/sessions/${sessionId}/messages`);

// 세션 내 메시지 저장
export const sendMessage = (sessionId, payload) =>
  api.post(`/ai-secretary/sessions/${sessionId}/messages`, payload);

// -----------------------------------------------------
// 챗봇 API
// -----------------------------------------------------

// Gemini 기반 챗봇 질문
// USER 메시지 저장 → Gemini 호출 → ASSISTANT 메시지 저장 → AI_LOG 저장
export const askChatbot = (payload) =>
  api.post("/ai-secretary/chatbot/ask", payload);

// -----------------------------------------------------
// 문장 다듬기 API
// -----------------------------------------------------

// 문장 다듬기 프롬프트 실행
// 입력 문장과 mode 전달 → Gemini 문장 다듬기 → fallback 처리 → AI_LOG 저장
export const correctText = (payload) =>
  api.post("/ai-secretary/correction", payload);

// -----------------------------------------------------
// AI 비서 문서 작성 API
// -----------------------------------------------------

// AI 비서 문서 초안 생성
export const createAssistantDraft = (payload) =>
  api.post(
    "/ai-secretary/assistant/draft",
    normalizeDocumentPayload(payload)
  );

// AI 비서 문서 추가 수정 (WriterScreen에서 “더 간결하게”, “표로 정리해줘” 등 추가 수정 요청)
export const reviseAssistantDraft = (payload) =>
  api.post(
    "/ai-secretary/assistant/revise",
    normalizeDocumentPayload(payload)
  );

// -----------------------------------------------------
// AI 비서 > 템플릿 요청 API
// -----------------------------------------------------

// 추천 템플릿 목록 추가 요청 저장
export const createTemplateRequest = (payload) =>
  api.post(
    "/ai-secretary/template-request",
    normalizeDocumentPayload(payload)
  );

// 내 추천 템플릿 추가 요청 목록 조회
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
// 조직 조회 API
// -----------------------------------------------------

export const getDepartmentTree = () =>
  api.get("/organization/departments/tree");

export const getEmployeesByDepartment = (deptId) =>
  api.get("/organization/employees", {
    params: {
      deptId: String(deptId),
    },
  });
