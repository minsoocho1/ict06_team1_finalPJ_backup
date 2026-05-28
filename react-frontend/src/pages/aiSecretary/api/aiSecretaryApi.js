/**
 * @FileName : aiSecretaryApi.js
 * @Description : AI ??쑴苑?/ AI 筌?ロ겦 ?紐꾨?獄?筌롫뗄?놅쭪? API
 * @Author : ??レ굺筌?
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ ??륁젟??        ??륁젟??       ??륁젟??곸뒠
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    ??レ굺筌?       筌ㅼ뮇????밴쉐 / BACKEND ?怨뚭퍙
 * @ 2026.05.05    ??レ굺筌?       ?얜챷????삳쾳疫?API ?곕떽?
 * @ 2026.05.06    ??レ굺筌?       AI ??쑴苑??귐딅뮞???곕떽? API ?곕떽?
 * @ 2026.05.07    ??レ굺筌?       ?얜챷苑??醫륁굨 type 揶쏅???REPORT / MINUTES / APPROVAL 疫꿸퀣???곗쨮 癰귣똻??
 * @ 2026.05.07    ??レ굺筌?       ??쀫탣???遺욧퍕 API + 鈺곌퀣彛?鈺곌퀬??API ?곕떽?
 */

import axios from "axios";
import { PATH } from 'src/constants/path';

// AI ??쑴苑??袁⑹뒠 axios instance
const api = axios.create({
  baseURL: PATH.API.BASE,
  withCredentials: true, // CORS ?怨뱀넺?癒?퐣 ?묒쥚沅??紐꾩쵄 ?類ｋ궖????됱뒠
});

// JWT ?醫뤾쿃 ?癒?짗 筌ｂ뫀? ()
// 嚥≪뮄????源껊궗 ??localStorage?????貫留?token???곗눖沅?筌뤴뫀諭?AI ??쑴苑?API ?遺욧퍕??Authorization Bearer ??삳쐭嚥??븐늿??
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


// 獄쏄퉮肉??ApiResponse ?닌듼?unwrap
// response.data.data -> unwrapApiData(response)嚥???쇱젫 data筌??????띾┛ ?袁る퉸
export const unwrapApiData = (response) => {
  if (response?.data?.data !== undefined) return response.data.data;
  if (response?.data !== undefined) return response.data;
  return response;
};

/* 獄쏄퉮肉???臾먮뼗 ?닌듼?
 * {
 *   success: true,
 *   message: "...",
 *   data: ...
 * }
 */

// ?얜챷苑??醫륁굨??獄쏄퉮肉??DB 疫꿸퀣? ???얜챷?꾣에?癰귣똻??
export const toApiDocumentType = (type) => {
  const normalized = String(type || "").trim().toUpperCase();

  if (normalized === "MINUTES") return "MINUTES";
  if (normalized === "APPROVAL") return "APPROVAL";
  return "REPORT";
};

// ?얜챷苑??醫륁굨 type????쇰선揶쎛??payload??API 疫꿸퀣???곗쨮 癰귣똻??
const normalizeDocumentPayload = (payload = {}) => ({
  ...payload,
  type: toApiDocumentType(payload?.type),
});

// -----------------------------------------------------
// ?紐꾨??온??API
// -----------------------------------------------------

/**
 * ?⑤벏???紐꾨???밴쉐
 *
 * ?袁⑹삺 雅뚯눘???癒?カ:
 * - CHATBOT ?紐꾨?? getOrCreateChatbotSession ????
 * - ASSISTANT ?紐꾨?? /assistant/draft ????癒?퐣 獄쏄퉮肉??? ??밴쉐
 *
 * ?怨뺤뵬??????λ땾????ㅻ즴 ???뮞???癒?뮉 ??덊돩??밸퓠 揶쎛繹먯빖??
 */
export const createSession = (payload) =>
  api.post("/ai-secretary/sessions", payload);


// CHATBOT 筌ㅼ뮄???紐꾨?鈺곌퀬???癒?뮉 ??밴쉐
export const getOrCreateChatbotSession = (empNo) =>
  api.post("/ai-secretary/chatbot/session", null, {
    params: {
      empNo: String(empNo),
    },
  });

// AI ??쑴苑?筌ㅼ뮄???臾믨쉐 筌뤴뫖以?鈺곌퀬??
export const getAssistantSessionList = (empNo) =>
  api.get("/ai-secretary/sessions", {
    params: {
      empNo: String(empNo),
      sessionType: "ASSISTANT",
    },
  });

// ?⑤벏???紐꾨?筌뤴뫖以?鈺곌퀬??
export const getSessionList = (empNo, sessionType) =>
  api.get("/ai-secretary/sessions", {
    params: {
      empNo: String(empNo),
      sessionType,
    },
  });

// ?紐꾨???筌롫뗄?놅쭪? 筌뤴뫖以?鈺곌퀬??(筌?ロ겦 筌롫뗄?놅쭪? ????? 筌ㅼ뮄???臾믨쉐 ?얜챷苑???????ASSISTANT ?紐꾨?筌롫뗄?놅쭪? 嚥≪뮆逾?
export const getMessages = (sessionId) =>
  api.get(`/ai-secretary/sessions/${sessionId}/messages`);

// ?紐꾨???筌롫뗄?놅쭪? ????
export const sendMessage = (sessionId, payload) =>
  api.post(`/ai-secretary/sessions/${sessionId}/messages`, payload);

// -----------------------------------------------------
// 筌?ロ겦 API
// -----------------------------------------------------

// Gemini 疫꿸퀡而?筌?ロ겦 筌욌뜄揆
// USER 筌롫뗄?놅쭪? ??????Gemini ?紐꾪뀱 ??ASSISTANT 筌롫뗄?놅쭪? ??????AI_LOG ????
export const askChatbot = (payload) =>
  api.post("/ai-secretary/chatbot/ask", payload);

// -----------------------------------------------------
// ?얜챷????삳쾳疫?API
// -----------------------------------------------------

// ?얜챷????삳쾳疫??袁⑨세?袁る뱜 ??쎈뻬
// ??낆젾 ?얜챷?ｆ?mode ?袁⑤뼎 ??Gemini ?얜챷????삳쾳疫???fallback 筌ｌ꼶????AI_LOG ????
export const correctText = (payload) =>
  api.post("/ai-secretary/correction", payload);

// -----------------------------------------------------
// AI ??쑴苑??얜챷苑??臾믨쉐 API
// -----------------------------------------------------

// AI ??쑴苑??얜챷苑??λ뜆釉???밴쉐
export const createAssistantDraft = (payload) =>
  api.post(
    "/ai-secretary/assistant/draft",
    normalizeDocumentPayload(payload)
  );

// 참고 자료 첨부 파일을 서버에 보내 본문 텍스트만 추출한다.
export const extractReferenceText = (file) => {
  const formData = new FormData();
  formData.append("file", file);

  return api.post("/ai-secretary/assistant/reference/extract", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
};

export const reviseAssistantDraft = (payload) =>
  api.post(
    "/ai-secretary/assistant/revise",
    normalizeDocumentPayload(payload)
  );

// -----------------------------------------------------
// AI ??쑴苑?> ??쀫탣???遺욧퍕 API
// -----------------------------------------------------

// ?곕뗄荑???쀫탣??筌뤴뫖以??곕떽? ?遺욧퍕 ????
export const createTemplateRequest = (payload) =>
  api.post(
    "/ai-secretary/template-request",
    normalizeDocumentPayload(payload)
  );

// ???곕뗄荑???쀫탣???곕떽? ?遺욧퍕 筌뤴뫖以?鈺곌퀬??
export const getMyTemplateRequests = (empNo) =>
  api.get("/ai-secretary/template-request/my", {
    params: {
      empNo: String(empNo),
    },
  });

// AI ??쀫탣????밴쉐
export const createAssistantTemplate = (payload) =>
  api.post("/ai-secretary/assistant/template", {
    ...payload,
    type: payload?.type ? toApiDocumentType(payload.type) : undefined,
  });

// -----------------------------------------------------
// 鈺곌퀣彛?鈺곌퀬??API
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
// ??????癒?┷ ?源낆쨯 ?遺욧퍕 API
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
