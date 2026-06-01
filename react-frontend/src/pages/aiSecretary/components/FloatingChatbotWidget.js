import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";

import Bubble from "./Bubble";
import {
  askChatbot,
  getMessages,
  getOrCreateChatbotSession,
  unwrapApiData,
} from "../api/aiSecretaryApi";

const formatMessageTime = (value) => {
  if (!value) return "";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";

  return date.toLocaleTimeString("ko-KR", {
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  });
};

const mapMessageToBubble = (message) => ({
  id: message?.messageId,
  role: message?.role === "USER" ? "user" : "ai",
  text: message?.content || "",
  time: formatMessageTime(message?.createdAt),
  references: Array.isArray(message?.references) ? message.references : [],
});

const mapAskResponseMessageToBubble = (message, references = []) => ({
  id: message?.messageId,
  role: message?.role === "USER" ? "user" : "ai",
  text: message?.content || "",
  time: formatMessageTime(message?.createdAt),
  references: Array.isArray(references) ? references : [],
});

const floatingButtonStyle = {
  position: "fixed",
  right: 24,
  bottom: 24,
  width: 60,
  height: 60,
  borderRadius: "50%",
  border: "none",
  background: "linear-gradient(135deg, #2563eb, #1d4ed8)",
  color: "#fff",
  boxShadow: "0 18px 40px rgba(37, 99, 235, 0.28)",
  fontSize: 15,
  fontWeight: 800,
  cursor: "pointer",
  zIndex: 1400,
};

const panelStyle = {
  position: "fixed",
  right: 24,
  bottom: 96,
  width: "min(392px, calc(100vw - 24px))",
  height: "min(640px, calc(100vh - 120px))",
  borderRadius: 20,
  background: "#f8fafc",
  boxShadow: "0 24px 64px rgba(15, 23, 42, 0.24)",
  border: "1px solid rgba(148, 163, 184, 0.22)",
  display: "flex",
  flexDirection: "column",
  overflow: "hidden",
  zIndex: 1399,
};

export default function FloatingChatbotWidget({ userInfo }) {
  const [isOpen, setIsOpen] = useState(false);
  const [sessionId, setSessionId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loadingSession, setLoadingSession] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const [hasLoadedMessages, setHasLoadedMessages] = useState(false);
  const bottomRef = useRef(null);

  const empNo = userInfo?.empNo ?? userInfo?.emp_no ?? null;
  const isBusy = loadingSession || loadingMessages;
  const isInputDisabled = !sessionId || sending || loadingSession;
  const isMobileViewport =
    typeof window !== "undefined" && window.innerWidth <= 768;

  const canShowWidget = useMemo(() => Boolean(empNo), [empNo]);

  const scrollToBottom = useCallback(() => {
    bottomRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "end",
    });
  }, []);

  useEffect(() => {
    if (!isOpen) return;
    scrollToBottom();
  }, [isOpen, messages, sending, scrollToBottom]);

  const loadSessionMessages = useCallback(async (nextSessionId) => {
    if (!nextSessionId) return;

    setLoadingMessages(true);
    setError("");

    try {
      const response = await getMessages(nextSessionId);
      const payload = unwrapApiData(response) ?? [];
      setMessages(Array.isArray(payload) ? payload.map(mapMessageToBubble) : []);
      setHasLoadedMessages(true);
    } catch (err) {
      console.error("Floating chatbot message load failed", err);
      setError("메시지 목록을 불러오지 못했습니다.");
    } finally {
      setLoadingMessages(false);
    }
  }, []);

  const ensureChatbotSession = useCallback(async (forceReload = false) => {
    if (!empNo) {
      setError("사용자 정보를 확인할 수 없습니다.");
      return null;
    }

    let nextSessionId = sessionId;

    if (!nextSessionId) {
      setLoadingSession(true);
      setError("");

      try {
        const response = await getOrCreateChatbotSession(empNo);
        const payload = unwrapApiData(response);

        if (!payload?.sessionId) {
          throw new Error("sessionId missing");
        }

        nextSessionId = payload.sessionId;
        setSessionId(payload.sessionId);
      } catch (err) {
        console.error("Floating chatbot session init failed", err);
        setError("챗봇 세션을 준비하지 못했습니다.");
        return null;
      } finally {
        setLoadingSession(false);
      }
    }

    if (forceReload || !hasLoadedMessages) {
      await loadSessionMessages(nextSessionId);
    }

    return nextSessionId;
  }, [empNo, hasLoadedMessages, loadSessionMessages, sessionId]);

  useEffect(() => {
    if (!isOpen) return;
    ensureChatbotSession(false);
  }, [ensureChatbotSession, isOpen]);

  const handleSend = useCallback(async () => {
    const trimmed = input.trim();

    if (!trimmed) {
      setError("질문을 입력해 주세요.");
      return;
    }

    if (sending) return;

    const nextSessionId = await ensureChatbotSession(false);
    if (!nextSessionId) {
      return;
    }

    setSending(true);
    setError("");

    try {
      const result = await askChatbot({
        sessionId: nextSessionId,
        content: trimmed,
      });

      const payload = result?.data?.data || unwrapApiData(result) || result?.data || result;
      const references = Array.isArray(payload?.references) ? payload.references : [];

      setInput("");

      if (payload?.userMessage && payload?.aiMessage) {
        setMessages((prev) => [
          ...prev,
          mapAskResponseMessageToBubble(payload.userMessage),
          mapAskResponseMessageToBubble(payload.aiMessage, references),
        ]);
      } else {
        await loadSessionMessages(nextSessionId);
      }
    } catch (err) {
      console.error("Floating chatbot ask failed", err);
      setError("답변을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setSending(false);
    }
  }, [ensureChatbotSession, input, loadSessionMessages, sending]);

  if (!canShowWidget) {
    return null;
  }

  return (
    <>
      {isOpen && (
        <div
          style={{
            ...panelStyle,
            right: isMobileViewport ? 12 : 24,
            bottom: isMobileViewport ? 80 : 96,
            width: isMobileViewport ? "calc(100vw - 24px)" : "392px",
            height: isMobileViewport ? "min(640px, calc(100vh - 104px))" : "min(640px, calc(100vh - 120px))",
          }}
        >
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: 12,
              padding: "16px 18px",
              background: "#ffffff",
              borderBottom: "1px solid rgba(148, 163, 184, 0.2)",
              flexShrink: 0,
            }}
          >
            <div>
              <div style={{ fontSize: 15, fontWeight: 800, color: "#0f172a" }}>
                사내 AI 챗봇
              </div>
              <div style={{ fontSize: 12, color: "#64748b", marginTop: 4 }}>
                업무 규정과 사내 가이드를 빠르게 찾아드려요.
              </div>
            </div>

            <button
              type="button"
              onClick={() => setIsOpen(false)}
              style={{
                width: 34,
                height: 34,
                borderRadius: "50%",
                border: "1px solid rgba(148, 163, 184, 0.28)",
                background: "#fff",
                color: "#334155",
                cursor: "pointer",
                fontSize: 18,
                lineHeight: 1,
              }}
              aria-label="챗봇 닫기"
            >
              ×
            </button>
          </div>

          <div
            style={{
              flex: 1,
              minHeight: 0,
              overflowY: "auto",
              display: "grid",
              gap: 14,
              alignContent: "start",
              padding: 16,
              background: "#f8fafc",
            }}
          >
            {isBusy ? (
              <div style={{ color: "#64748b", fontSize: 13 }}>
                대화를 불러오는 중입니다...
              </div>
            ) : messages.length > 0 ? (
              messages.map((message, index) => (
                <Bubble
                  key={message.id ?? `${message.role}-${index}`}
                  role={message.role}
                  text={message.text}
                  time={message.time}
                  references={message.references}
                />
              ))
            ) : (
              <Bubble
                role="ai"
                text={"안녕하세요. 사내 규정, 업무 표준, 운영 가이드에 대해 물어보세요."}
                time=""
                references={[]}
              />
            )}

            {sending && (
              <div
                style={{
                  alignSelf: "flex-start",
                  maxWidth: "80%",
                  padding: "12px 14px",
                  borderRadius: 14,
                  border: "1px solid rgba(148, 163, 184, 0.22)",
                  background: "#fff",
                  color: "#64748b",
                  fontSize: 13,
                }}
              >
                답변을 준비하고 있어요...
              </div>
            )}

            {error && (
              <div style={{ fontSize: 13, color: "#b91c1c" }}>
                {error}
              </div>
            )}

            <div ref={bottomRef} />
          </div>

          <div
            style={{
              display: "flex",
              gap: 10,
              alignItems: "center",
              padding: 14,
              borderTop: "1px solid rgba(148, 163, 184, 0.2)",
              background: "#ffffff",
              flexShrink: 0,
            }}
          >
            <input
              value={input}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                  event.preventDefault();
                  if (!isInputDisabled) {
                    handleSend();
                  }
                }
              }}
              placeholder={
                sending ? "답변을 준비하고 있어요..." : "질문을 입력해 주세요."
              }
              disabled={isInputDisabled}
              style={{
                flex: 1,
                minWidth: 0,
                border: "1px solid rgba(148, 163, 184, 0.28)",
                borderRadius: 12,
                padding: "11px 14px",
                outline: "none",
                fontSize: 14,
                background: isInputDisabled ? "#f8fafc" : "#fff",
              }}
            />

            <button
              type="button"
              onClick={handleSend}
              disabled={isInputDisabled}
              style={{
                border: "none",
                borderRadius: 12,
                padding: "11px 14px",
                background: "#2563eb",
                color: "#fff",
                fontWeight: 700,
                cursor: isInputDisabled ? "default" : "pointer",
                opacity: isInputDisabled ? 0.65 : 1,
                flexShrink: 0,
              }}
            >
              전송
            </button>
          </div>
        </div>
      )}

      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        style={{
          ...floatingButtonStyle,
          right: isMobileViewport ? 12 : 24,
          bottom: isMobileViewport ? 12 : 24,
          width: isMobileViewport ? 56 : 60,
          height: isMobileViewport ? 56 : 60,
        }}
        aria-label={isOpen ? "챗봇 닫기" : "챗봇 열기"}
      >
        AI
      </button>
    </>
  );
}
