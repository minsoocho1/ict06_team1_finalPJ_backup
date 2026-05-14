/**
 * @FileName : AiSecretary.js
 * @Description : 사내 AI 비서 화면을 묶는 최상위 컨테이너 컴포넌트
 *                - 전체 페이지 구성
 *                - URL 기반 화면 분기
 *                - AI 비서/챗봇/문장 다듬기/지식 요청 화면 제어
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일        수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.27    송혜진       최초 생성
 * @ 2026.05.06    송혜진       문서 데이터 제거 / DB 기반 최근 작성 목록 연결
 * @ 2026.05.07    송혜진       문서 유형 REPORT / MINUTES / APPROVAL 정리
 * @ 2026.05.07    송혜진       mail 명목 correction 화면으로 정리
 */

import React, { useEffect, useMemo, useState } from "react";
import {
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";

import { PATH } from "../../constants/path";

import {
  normalizeFormType,
  buildAssistantDocPath,
  ASSISTANT_DOC_PREFIX,
} from "./utils/aiSecretaryRouteHelpers";

import {
  initialFormData,
  initialCorrectionState,
  initialWriterState,
} from "./constants/aiSecretaryInitialState";

import AssistantHome from "./screens/AssistantHome";
import StartFormScreen from "./screens/StartFormScreen";
import TemplateScreen from "./screens/TemplateScreen";
import WriterScreen from "./screens/WriterScreen";
import ChatbotScreen from "./screens/ChatbotScreen";
import CorrectionScreen from "./screens/CorrectionScreen";
import KnowledgeRequestScreen from "./screens/KnowledgeRequestScreen";

import Sidebar from "./components/Sidebar";

import {
  createAssistantDraft,
  getAssistantSessionList,
  getMessages,
  unwrapApiData,
} from "./api/aiSecretaryApi";

import { styles } from "./styles/aiSecretaryTheme";

/**
 * ASSISTANT 세션 응답을 Sidebar / AssistantHome에서 사용하는 recent 문서 형식으로 변환한다.
 *
 * 현재 상태:
 * - AI_CHAT_SESSION에는 documentType 컬럼이 아직 없음
 * - 상황에 따라 REPORT를 기본값으로 사용
 *
 * 향후 개선:
 * - AI_CHAT_SESSION.document_type 컬럼 추가
 * - 백엔드 AiChatSessionResponseDto에 documentType 추가
 * - 이 함수에서 session.documentType을 type으로 매핑
 */
function normalizeRecentDocumentType(type) {
  const normalized = String(type || "").trim().toUpperCase();

  if (normalized === "MINUTES") return "MINUTES";
  if (normalized === "APPROVAL") return "APPROVAL";
  if (normalized === "TEMPLATE") return "TEMPLATE";
  return "REPORT";
}

const mapSessionToRecentDoc = (session) => ({
  id: String(session.sessionId),
  sessionId: session.sessionId,
  title: session.title || "제목 없는 AI 문서",
  type: normalizeRecentDocumentType(
    session.documentType || session.type || "REPORT"
  ),
  screen: "writer",
  updatedAt: session.lastMessageAt || session.updatedAt || session.createdAt,
});

const buildVersionsFromMessages = (messages) => {
  const assistantMessages = (Array.isArray(messages) ? messages : [])
    .filter((message) =>
      String(message?.role || "").trim().toUpperCase() === "ASSISTANT"
    )
    .sort((left, right) => (left?.seqNo || 0) - (right?.seqNo || 0));

  return assistantMessages.map((message, index) => {
    const versionNumber = index + 1;

    return {
      id: `v${versionNumber}-${message?.messageId ?? versionNumber}`,
      messageId: message?.messageId ?? null,
      label: `V${versionNumber}`,
      title: `V${versionNumber}`,
      summary: message?.modelName
        ? `${message.modelName} 응답`
        : "DB에서 불러온 버전입니다.",
      content: message?.content || "",
      createdAt: message?.createdAt || null,
      seqNo: message?.seqNo ?? versionNumber,
      modelName: message?.modelName || "",
      current: index === assistantMessages.length - 1,
    };
  });
};

const buildReferenceTargets = (referenceFiles, referenceMemo) => {
  const targets = [];

  const fileNames = Array.isArray(referenceFiles)
    ? referenceFiles
        .map((file) => String(file?.name || "").trim())
        .filter(Boolean)
    : [];

  if (fileNames.length > 0) {
    targets.push(`참고 자료: ${fileNames.join(", ")}`);
  }

  const memo = String(referenceMemo || "").trim();
  if (memo) {
    targets.push(`참고 자료 메모: ${memo}`);
  }

  return targets;
};

function normalizeTemplateDeptLabel(value) {
  return String(value || "")
    .split(",")
    .map((part) => part.trim())
    .map((part) => part.split(">").pop().trim())
    .filter(Boolean)
    .join(", ");
}

function normalizeTemplateOrganizationSeed(seed) {
  if (!seed) {
    return null;
  }

  if (typeof seed === "string") {
    const deptText = normalizeTemplateDeptLabel(seed);
    return deptText ? { deptText } : null;
  }

  if (typeof seed !== "object") {
    return null;
  }

  const headquarterId = String(
    seed.headquarterId ||
      seed.headquarter?.deptId ||
      seed.headquarter?.id ||
      seed.headquarter?.dept_id ||
      ""
  ).trim();
  const headquarterName = normalizeTemplateDeptLabel(
    seed.headquarterName ||
      seed.headquarter?.deptName ||
      seed.headquarter?.name ||
      seed.headquarter?.displayName ||
      ""
  );
  const teamIds = Array.isArray(seed.teamIds)
    ? seed.teamIds.map((teamId) => String(teamId || "").trim()).filter(Boolean)
    : [];
  const teamNames = Array.isArray(seed.teamNames)
    ? seed.teamNames.map((name) => normalizeTemplateDeptLabel(name)).filter(Boolean)
    : [];
  const displayName = normalizeTemplateDeptLabel(
    seed.displayName || teamNames.join(", ") || seed.deptText || ""
  );
  const deptText = normalizeTemplateDeptLabel(seed.deptText || displayName);

  if (
    !headquarterId &&
    !headquarterName &&
    teamIds.length === 0 &&
    teamNames.length === 0 &&
    !displayName &&
    !deptText
  ) {
    return null;
  }

  return {
    headquarterId,
    headquarterName,
    teamIds,
    teamNames,
    displayName,
    deptText,
  };
}

const buildTemplateSeedFromCard = (card) => {
  const title = String(card?.title || "").trim();
  const purpose = String(card?.situation || "").trim();
  const rawDescription = String(
    card?.templateSeed?.description || card?.description || card?.desc || ""
  ).trim();
  const description =
    rawDescription && rawDescription !== "설명이 없습니다."
      ? rawDescription
      : "";
  const content = String(
    card?.templateSeed?.content || card?.content || card?.generatedContent || ""
  ).trim();
  const previewList = Array.isArray(card?.templateSeed?.preview)
    ? card.templateSeed.preview
    : Array.isArray(card?.preview)
    ? card.preview
    : [];
  const previewLines = previewList
    .map((item) => String(item || "").trim())
    .filter(Boolean);

  const detailParts = [];
  if (description) {
    detailParts.push(`설명:\n${description}`);
  }
  if (content && content !== description) {
    detailParts.push(content);
  }
  if (previewLines.length > 0) {
    detailParts.push(`템플릿 구성:\n${previewLines.join("\n")}`);
  }

  const organizationSeed = normalizeTemplateOrganizationSeed(
    card?.templateFilters?.generation?.relatedDept ||
      card?.relatedDept ||
      card?.deptMeta
  );
  const deptText = normalizeTemplateDeptLabel(
    organizationSeed?.displayName || card?.dept || ""
  );

  return {
    type: normalizeFormType(card?.type),
    title,
    purpose,
    detail: detailParts.join("\n\n").trim() || description || content || "",
    amount: card?.templateSeed?.amount || "보통",
    audience: card?.templateSeed?.audience || "",
    targets: Array.isArray(card?.templateSeed?.targets)
      ? card.templateSeed.targets
      : [],
    referenceFiles: Array.isArray(card?.templateSeed?.referenceFiles)
      ? card.templateSeed.referenceFiles
      : [],
    referenceMemo: card?.templateSeed?.referenceMemo || "",
    organizationSeed: card?.templateSeed?.organizationSeed || organizationSeed,
    deptText:
      card?.templateSeed?.deptText ||
      deptText ||
      normalizeTemplateDeptLabel(card?.dept || ""),
  };
};
export default function AiSecretary({ userInfo }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { docId } = useParams();
  const [searchParams] = useSearchParams();

  // ------------------------------------------------
  // 0) 화면 공통 상태
  // ------------------------------------------------

  /**
   * StartFormScreen 입력값
   */
  const [formData, setFormData] = useState(initialFormData);
  const [templateSeed, setTemplateSeed] = useState(null);

  /**
   * CorrectionScreen 상태
   */
  const [correction, setCorrection] = useState(initialCorrectionState);

  /**
   * WriterScreen 상태
   */
  const [writerState, setWriterState] = useState(initialWriterState);

  /**
   * 최근 생성 목록
   *
    * 기본값:
    * - recentDocsSeed 기반 정적 목록
    *
    * 변경 사항
    * - DB 기반 AI_CHAT_SESSION 중 ASSISTANT 세션만 조회
   */
  const [recents, setRecents] = useState([]);
  const [loadingRecents, setLoadingRecents] = useState(false);
  const [recentError, setRecentError] = useState("");

  /**
   * AI 초안 생성 상태
   *
   * generatingDraft:
   * - AI 초안 생성 중 중복 클릭 방지
   *
   * draftError:
   * - 입력값 검증 실패 또는 API 실패 메시지
   */
  const [generatingDraft, setGeneratingDraft] = useState(false);
  const [draftError, setDraftError] = useState("");

  /**
   * 로그인 사용자 사번
   *
   * 주의:
   * - userInfo가 null이면 최근 목록 조회/초안 생성이 중단됨
   * - 필요하면 이후 /api/user/welcome 방식 추가 가능
   */
  const empNo = userInfo?.empNo ?? userInfo?.emp_no ?? null;

  // ------------------------------------------------
  // 1) URL 기반 현재 페이지 상태 확인
  // ------------------------------------------------

  /**
   * query string의 type 값을 문서 유형으로 정규화한다.
   *
   * 경로 예시:
   * /ai-portal/assistant/new?type=REPORT
   * /ai-portal/assistant/new?type=MINUTES
   * /ai-portal/assistant/new?type=APPROVAL
   *
   * normalizeFormType은 대문자로 정규화한다.
   */
  const queryType = normalizeFormType(searchParams.get("type"));

  /**
 * 현재 URL의 docId가 최근 작성 목록에 있으면 해당 문서를 찾는다.
   *
 * recent.id는 sessionId 문자열이다.
   */
  const matchedRecentDoc = useMemo(() => {
    return recents.find((doc) => String(doc.id) === String(docId));
  }, [recents, docId]);

  /**
 * 현재 탭 구분
   *
   * assistant:
 * - AI 비서 문서 작성/미리보기 Writer
   *
   * chatbot:
 * - 사내 지식 챗봇
   *
   * correction:
   * - 臾몄옣 ?ㅻ벉湲?
   *
   * knowledge-request:
 * - 문장 다듬기
   */
  const currentTab = useMemo(() => {
    if (location.pathname.startsWith(PATH.AI.CHATBOT)) return "chatbot";

    /**
     * 기존 polish 명칭은 내부 호환용으로만 유지
     * 기능명과 API/route 기준이 맞지 않더라도 correction으로 정리한다.
     */
    if (location.pathname.startsWith(PATH.AI.CORRECTION)) return "correction";

    if (location.pathname.startsWith(PATH.AI.KNOWLEDGE_REQUEST)) {
      return "knowledge-request";
    }

    return "assistant";
  }, [location.pathname]);

  /**
   * assistant ???대? ?붾㈃ 援щ텇
   */
  const currentScreen = useMemo(() => {
    if (location.pathname === PATH.AI.ASSISTANT) return "assistant-home";
    if (location.pathname === PATH.AI.ASSISTANT_NEW) return "form";
    if (location.pathname === PATH.AI.ASSISTANT_TEMPLATE) return "template";

    if (location.pathname.startsWith(`${ASSISTANT_DOC_PREFIX}/`)) {
      return "writer";
    }

    return "assistant-home";
  }, [location.pathname]);

  // 현재 문서 유형
  // 최근 작성 목록에서 찾은 문서 type + query string type + normalizeFormType 중 우선값을 사용한다.
  const currentFormType = useMemo(() => {
    if (currentScreen === "writer") {
      if (matchedRecentDoc?.type) {
        return normalizeRecentDocumentType(matchedRecentDoc.type);
      }

      return normalizeRecentDocumentType(searchParams.get("type"));
    }

    if (matchedRecentDoc?.type) {
      return normalizeFormType(matchedRecentDoc.type);
    }

    return queryType;
  }, [currentScreen, matchedRecentDoc, queryType, searchParams]);

  // ------------------------------------------------
  // 2) URL 이동 helper
  // ------------------------------------------------

  const goAssistantHome = () => navigate(PATH.AI.ASSISTANT);

  /**
   * AI 비서 문서 작성 시작 화면으로 이동
   *
   * 문서 유형:
   * - REPORT
   * - MINUTES
   * - APPROVAL
   */
  const goAssistantForm = (type = "REPORT", options = {}) => {
    const normalized = normalizeFormType(type);
    if (!options.keepTemplateSeed) {
      setTemplateSeed(null);
    }
    navigate(`${PATH.AI.ASSISTANT_NEW}?type=${normalized}`);
  };

  const goAssistantTemplate = () => navigate(PATH.AI.ASSISTANT_TEMPLATE);

  /**
   * AI 비서 문서 작성/수정 화면으로 이동
   */
  const goAssistantDoc = (targetDocId, type = "REPORT") => {
    const normalized = normalizeRecentDocumentType(type);
    navigate(`${buildAssistantDocPath(targetDocId)}?type=${normalized}`);
  };

  const goChatbot = () => navigate(PATH.AI.CHATBOT);
  const goCorrection = () => navigate(PATH.AI.CORRECTION);
  const goKnowledgeRequest = () => navigate(PATH.AI.KNOWLEDGE_REQUEST);

  // ------------------------------------------------
  // 3) 최근 작성 목록 DB 조회
  // ------------------------------------------------

  /**
   * ASSISTANT 세션만 최근 작성 목록으로 조회한다.
   *
   * 정책:
   * - ASSISTANT 세션은 기존 그대로 유지
   * - 최근 작성 목록만 불러옴
   * - CHATBOT 세션은 최근 작성 목록에 포함하지 않음
   */
  useEffect(() => {
    if (!empNo) return;

    const loadAssistantRecents = async () => {
      setLoadingRecents(true);
      setRecentError("");

      try {
        const response = await getAssistantSessionList(empNo);
        const data = unwrapApiData(response) ?? [];

        const mappedRecents = Array.isArray(data)
          ? data.map(mapSessionToRecentDoc)
          : [];

        setRecents(mappedRecents);
      } catch (error) {
        console.error("최근 작성 목록 조회 실패", error);

        setRecentError("최근 작성 목록을 불러오지 못했습니다.");
        setRecents([]);
      } finally {
        setLoadingRecents(false);
      }
    };

    loadAssistantRecents();
  }, [empNo]);

  // ------------------------------------------------
  // 4) WriterScreen 진입 시 DB 메시지 로딩
  // ------------------------------------------------

  /**
   * 최근 작성 문서를 클릭하면 URL로 직접 진입한다.
   *
   * /ai-portal/assistant/docs/{sessionId}?type=REPORT
   *
   * 같은 writer 화면으로 들어가면,
   * sessionId 기준으로 메시지 목록을 불러온다.
   *
   * 현재 동작:
   * - 가장 마지막 ASSISTANT 메시지를 최신 문서 본문으로 사용
   * - 모든 메시지를 왼쪽 AI 대화 내용으로 표시
   */
  useEffect(() => {
    if (currentScreen !== "writer") return;
    if (!docId) return;

    const sessionId = Number(docId);

    if (Number.isNaN(sessionId)) return;

    const loadWriterDocument = async () => {
      try {
        const response = await getMessages(sessionId);
        const messages = unwrapApiData(response) ?? [];

        if (!Array.isArray(messages) || messages.length === 0) {
          return;
        }

        const lastAssistantMessage = [...messages]
          .reverse()
          .find((message) => message.role === "ASSISTANT");

        const firstUserMessage = messages.find(
          (message) => message.role === "USER"
        );

        if (!lastAssistantMessage) {
          return;
        }

        const matchedDoc = recents.find(
          (doc) => Number(doc.sessionId ?? doc.id) === sessionId
        );

        setWriterState((prev) => ({
          ...prev,
          sessionId,
          title: matchedDoc?.title || prev.title || "AI 문서",
          content: lastAssistantMessage.content,
          aiMessageId: lastAssistantMessage.messageId,
          userMessageId: firstUserMessage?.messageId ?? prev.userMessageId,
          modelName: lastAssistantMessage.modelName,
          fallback: lastAssistantMessage.modelName === "gemini-fallback",

          /**
 * 왼쪽 AI 대화 영역의 DB 메시지를 그대로 복원한다.
           */
          chat: messages.map((message) => ({
            role: message.role === "USER" ? "user" : "ai",
            text: message.content,
            time: "방금",
          })),

          /**
 * 현재는 document_version 테이블이 없으므로
 * 마지막 ASSISTANT 메시지를 v1로 구성한다.
           *
 * 향후 개선:
 * - 메시지 seq_no 또는 버전 테이블 기준으로 v1/v2/v3 복원 가능
           */
          versions: buildVersionsFromMessages(messages),

          prompt: "",
          showHistory: false,
        }));
      } catch (error) {
      console.error("AI 문서 메시지 조회 실패", error);
      }
    };

    loadWriterDocument();
  }, [currentScreen, docId, recents]);

  // ------------------------------------------------
  // 5) AI 초안 생성
  // ------------------------------------------------

  // StartFormScreen의 "AI 초안 생성" 버튼 클릭 시 실행
  // /assistant/draft API 호출
  const handleGenerateDraft = async () => {
    if (generatingDraft) {
      return;
    }

    if (!empNo) {
      setDraftError("사용자 정보를 찾을 수 없습니다. 다시 로그인해 주세요.");
      return;
    }

    if (!formData.title?.trim()) {
      setDraftError("문서 제목을 입력해 주세요.");
      return;
    }

    if (!formData.detail?.trim()) {
      setDraftError("답변 내용을 입력해 주세요.");
      return;
    }

    setGeneratingDraft(true);
    setDraftError("");

    try {
      const response = await createAssistantDraft({
        empNo: String(empNo),
        type: currentFormType,
        title: formData.title,
        purpose: formData.purpose,
        audience: formData.audience,
        targets: [
          ...(Array.isArray(formData.targets) ? formData.targets : []),
          ...buildReferenceTargets(
            formData.referenceFiles,
            formData.referenceMemo
          ),
        ],
        detail: formData.detail,
        amount: formData.amount,
        tone: "BUSINESS",
      });

      const data = unwrapApiData(response);

      setWriterState((prev) => ({
        ...prev,
        sessionId: data.sessionId,
        userMessageId: data.userMessageId,
        aiMessageId: data.aiMessageId,
        type: normalizeFormType(data.type || currentFormType),
        title: data.title,
        content: data.content,
        modelName: data.modelName,
        fallback: data.fallback,

        // 왼쪽 AI 대화 영역의 초기 메시지
        chat: [
          {
            role: "user",
            text: "초안 생성을 요청했습니다.",
            time: "방금",
          },
          {
            role: "ai",
            text: data.fallback
              ? "AI 응답 생성이 아직 완료되지 않아 기본 안내를 반영했습니다."
              : "요청하신 내용을 바탕으로 초안을 생성했습니다.",
            time: "방금",
          },
        ],

        // 최초 생성 버전
        // content를 바탕으로 v1 미리보기/복원 정상 동작
        versions: buildVersionsFromMessages([
          {
            messageId: data.aiMessageId,
            role: "ASSISTANT",
            content: data.content,
            seqNo: 1,
            createdAt: new Date().toISOString(),
            modelName: data.modelName,
          },
        ]),

        prompt: "",
        showHistory: false,
      }));

      /**
 * 초안 생성 후 최근 작성 목록도 즉시 반영한다.
 * DB 기준이라도 Sidebar/AssistantHome에 바로 보이게 함.
       */
      setRecents((prev) => [
        {
          id: String(data.sessionId),
          sessionId: data.sessionId,
          title: data.title || formData.title || "제목 없는 AI 문서",
          type: normalizeFormType(data.type || currentFormType),
          screen: "writer",
          updatedAt: new Date().toISOString(),
        },
        ...prev.filter(
          (doc) => String(doc.sessionId) !== String(data.sessionId)
        ),
      ]);

      goAssistantDoc(data.sessionId, data.type || currentFormType);
    } catch (err) {
      console.error("AI 초안 생성 실패", err);
      setDraftError(
        "AI 초안 생성 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요."
      );
    } finally {
      setGeneratingDraft(false);
    }
  };

  // ------------------------------------------------
  // 6) 사이드바 / 최근 작성 클릭 처리
  // ------------------------------------------------

  const handleSidebarChange = (next) => {
    if (next === "assistant") {
      goAssistantHome();
      return;
    }

    if (next === "chatbot") {
      goChatbot();
      return;
    }

    // correction: 현재 정리된 문장 다듬기 key
    if (next === "correction") {
      goCorrection();
      return;
    }

    if (next === "knowledge-request") {
      goKnowledgeRequest();
    }
  };

  /**
   * 최근 작성 클릭 처리
   *
   * DB 기반 recent 문서의 screen이 "writer"이면
   * sessionId 기준으로 writer URL로 이동한다.
   */
  const handleRecentClick = (doc) => {
    if (doc.screen === "writer") {
      goAssistantDoc(doc.sessionId ?? doc.id, doc.type ?? "REPORT");
      return;
    }

    setFormData((prev) => ({
      ...prev,
      title: doc.title,
    }));

    goAssistantForm(doc.type ?? "REPORT");
  };

  // ------------------------------------------------
  // 7) 라우팅 함수 모음
  // ------------------------------------------------

  const renderStandalonePage = () => {
    if (currentTab === "chatbot") {
      return <ChatbotScreen userInfo={userInfo} />;
    }

    if (currentTab === "correction") {
      return (
        <CorrectionScreen
          correction={correction}
          setCorrection={setCorrection}
          onBackHome={goAssistantHome}
        />
      );
    }

    if (currentTab === "knowledge-request") {
      return <KnowledgeRequestScreen userInfo={userInfo} />;
    }

    return null;
  };

  // ------------------------------------------------
  // 8) AI 비서 각 화면 렌더링
  // ------------------------------------------------

  const renderAssistantPage = () => {
    if (currentScreen === "assistant-home") {
      return (
        <AssistantHome
          onOpenForm={goAssistantForm}
          onOpenTemplate={goAssistantTemplate}
          recents={recents}
          onRecentClick={handleRecentClick}
          loadingRecents={loadingRecents}
          recentError={recentError}
        />
      );
    }

    if (currentScreen === "form") {
      return (
        <StartFormScreen
          formType={currentFormType}
          formData={formData}
          templateSeed={templateSeed}
          initialOrganizationSeed={
            templateSeed?.organizationSeed ||
            (templateSeed?.deptText ? { deptText: templateSeed.deptText } : null)
          }
          onChangeFormType={goAssistantForm}
          onChangeFormData={(key, value) =>
            setFormData((prev) => ({ ...prev, [key]: value }))
          }
          onGenerateDraft={handleGenerateDraft}
          onOpenTemplate={goAssistantTemplate}
          generating={generatingDraft}
          error={draftError}
        />
      );
    }

    if (currentScreen === "template") {
      return (
        <TemplateScreen
          empNo={empNo}
          onOpenForm={goAssistantForm}
          onStartTemplate={(card) => {
            /**
             * 선택한 템플릿의 generatedContent / preview를 기준으로
             * AI 초안 생성에 필요한 입력값을 미리 구성한다.
             *
             * 선택 순서:
             * 1. AI 생성 템플릿 card.generatedContent
             * 2. 정적 추천 템플릿 card.preview.join("\n")
             */
            const inferredType = normalizeFormType(card.type);
            const nextTemplateSeed =
              card?.templateSeed || buildTemplateSeedFromCard(card);

            setTemplateSeed(nextTemplateSeed);
            setFormData((prev) => ({
              ...prev,
              title: nextTemplateSeed.title || card.title || "",
              purpose: nextTemplateSeed.purpose || "",
              audience: nextTemplateSeed.audience || "",
              targets: Array.isArray(nextTemplateSeed.targets)
                ? nextTemplateSeed.targets
                : [],
              detail: nextTemplateSeed.detail || "",
              amount: nextTemplateSeed.amount || "보통",
              referenceFiles: Array.isArray(nextTemplateSeed.referenceFiles)
                ? nextTemplateSeed.referenceFiles
                : [],
              referenceMemo: nextTemplateSeed.referenceMemo || "",
            }));

            goAssistantForm(inferredType, { keepTemplateSeed: true });
          }}
        />
      );
    }

    if (currentScreen === "writer") {
      return (
        <WriterScreen
          writerState={writerState}
          setWriterState={setWriterState}
          writerType={currentFormType}
        />
      );
    }

    return null;
  };

  // ------------------------------------------------
  // 9) 최종 페이지 선택
  // ------------------------------------------------

  const page = useMemo(() => {
    const standalonePage = renderStandalonePage();
    if (standalonePage) return standalonePage;

    const assistantPage = renderAssistantPage();
    if (assistantPage) return assistantPage;

    return (
      <AssistantHome
        onOpenForm={goAssistantForm}
        onOpenTemplate={goAssistantTemplate}
        recents={recents}
        onRecentClick={handleRecentClick}
        loadingRecents={loadingRecents}
        recentError={recentError}
      />
    );
  }, [
    currentTab,
    currentScreen,
    currentFormType,
    correction,
    writerState,
    recents,
    formData,
    userInfo,
    generatingDraft,
    draftError,
    loadingRecents,
    recentError,
  ]);

  return (
    <div style={styles.app}>
      <Sidebar
        tab={currentTab}
        onTabChange={handleSidebarChange}
        recents={recents}
        onRecentClick={handleRecentClick}
      />

      <main style={styles.main}>{page}</main>
    </div>
  );
}
