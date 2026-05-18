/**
 * @FileName : ChatbotScreen.js
 * @Description : AI 챗봇 화면(사내 지식 질의응답)
 * @Author : 송혜진
 * @Date : 2026. 04. 29
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.28    송혜진        최초 생성/ BACKEND 연결
 * @ 2026.05.12    송혜진        로직 변경(lastMessage 기준 48시간 이후 삭제)
 */

import React, { useCallback, useEffect, useRef, useState } from "react";
import axios from "axios";

import Bubble from "../components/Bubble";
import { I, Icon } from "../constants/aiSecretaryIcons";
import { C, styles } from "../styles/aiSecretaryTheme";
import {
  getOrCreateChatbotSession,
  getMessages,
  askChatbot,
  sendMessage,
  unwrapApiData,
} from "../api/aiSecretaryApi";
import { useUser } from "src/api/UserContext";
import { PATH } from 'src/constants/path';

// 시간 포맷
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

// 백엔드 메시지 -> Bubble props
const mapMessageToBubble = (message) => ({
  id: message.messageId,
  role: message.role === "USER" ? "user" : "ai",
  text: message.content,
  time: formatMessageTime(message.createdAt),
});

export default function ChatbotScreen() {
  const { userInfo, updateUserInfo } = useUser();
 
  // Context가 비어 있어도 화면에서 자체 복구 가능하도록 로컬 상태 사용
  const [resolvedUserInfo, setResolvedUserInfo] = useState(userInfo);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [currentSessionId, setCurrentSessionId] = useState(null);
  
  const [loadingUser, setLoadingUser] = useState(false);
  const [loadingInit, setLoadingInit] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  
  const bottomRef = useRef(null);

  const empNo =
    resolvedUserInfo?.empNo ??
    resolvedUserInfo?.emp_no ??
    null;

  // Context userInfo가 바뀌면 로컬에도 반영
  useEffect(() => {
    if (userInfo) {
      setResolvedUserInfo(userInfo);
    }
  }, [userInfo]);

  // 메시지 변경 시 자동 스크롤
  useEffect(() => {
    bottomRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "end",
    });
  }, [messages, sending]);

  // 로그인 페이지처럼 token 기반 welcome 재조회
  const ensureUserInfo = useCallback(async () => {
    if (resolvedUserInfo) return resolvedUserInfo;

    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
      setError("로그인 정보가 없습니다. 다시 로그인해 주세요.");
      return null;
    }

    setLoadingUser(true);
    setError("");

    try {
      const authorization = accessToken.startsWith("Bearer ")
        ? accessToken
        : `Bearer ${accessToken}`;

      const response = await axios.get(PATH.AUTH.WELCOME, {
        headers: {
          Authorization: authorization,
        },
      });

      const detail = response.data;

      const normalizedUser = {
        ...detail,
        empNo: detail?.empNo ?? detail?.emp_no,
      };

      setResolvedUserInfo(normalizedUser);

      if (updateUserInfo) {
        updateUserInfo(normalizedUser);
      }

      return normalizedUser;
    } catch (err) {
      console.error("사용자 정보 복구 실패", err);
      setError(
        err?.response?.data?.message || "사용자 정보를 불러오지 못했습니다."
      );
      return null;
    } finally {
      setLoadingUser(false);
    }
  }, [resolvedUserInfo, updateUserInfo]);

  // 챗봇 세션 초기화 - 최근 48시간 내 세션 있으면 재사용, 없으면 생성
  const initializeChatbotSession = useCallback(async () => {
    setLoadingInit(true);
    setError("");

    try {
      const ensuredUser = await ensureUserInfo();

      const effectiveEmpNo =
        ensuredUser?.empNo ??
        ensuredUser?.emp_no ??
        resolvedUserInfo?.empNo ??
        resolvedUserInfo?.emp_no;

      if (!effectiveEmpNo) {
        setError("사원번호를 찾을 수 없습니다.");
        return;
      }

      const response = await getOrCreateChatbotSession(effectiveEmpNo);
      const session = unwrapApiData(response);

      if (!session?.sessionId) {
        throw new Error("챗봇 세션 응답에 sessionId가 없습니다.");
      }

      setCurrentSessionId(session.sessionId);

      // 개발 확인용
      console.log("[CHATBOT] sessionId =", session.sessionId);
    } catch (err) {
        console.error("챗봇 응답 생성 실패", err);
        setError(
          "AI 응답 생성 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요."
        );
    } finally {
      setLoadingInit(false);
    }
  }, [ensureUserInfo, resolvedUserInfo]);

  // 메시지 목록 조회
  const loadMessages = useCallback(async (sessionId) => {
    if (!sessionId) return;

    setLoadingMessages(true);
    setError("");

    try {
      const response = await getMessages(sessionId);
      const messageItems = unwrapApiData(response) ?? [];

      setMessages(
        Array.isArray(messageItems)
          ? messageItems.map(mapMessageToBubble)
          : []
      );
    } catch (err) {
      console.error("메시지 목록 조회 실패", err);
      setError(
        err?.response?.data?.message || "메시지 목록을 불러오지 못했습니다."
      );
    } finally {
      setLoadingMessages(false);
    }
  }, []);

  // 메시지 저장
  // 현재 단계:
  // - USER 메시지 저장까지만 담당
  // - Gemini/GPT 응답 생성은 다음 단계에서 별도 API로 붙이는 것이 좋음
  const send = useCallback(async () => {
    const trimmed = input.trim();

    // [1] 빈 질문 방지
    if (!trimmed) {
      setError("질문을 입력해 주세요.");
      return;
    }

    // [2] 세션 준비 전 전송 방지
    if (!currentSessionId) {
      setError("채팅방을 불러오는 중입니다.");
      return;
    }

    // [3] 중복 전송 방지
    if (sending) return;

    setSending(true);
    setError("");

    // askChatbot API = USER 메시지 저장 + Gemini 호출 + ASSISTANT 메시지 저장 처리
    try {
      await askChatbot({
        sessionId: currentSessionId,
        content: trimmed,
      });

      // 입력창 초기화
      setInput("");

      // DB 기준 최신 메시지 재조회
      await loadMessages(currentSessionId);
    } catch (err) {
      console.error("챗봇 응답 생성 실패", err);
      setError("AI 응답 생성 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setSending(false);
    }
  }, [input, currentSessionId, sending, loadMessages]);

  // 최초 진입 시 챗봇 세션 초기화
  useEffect(() => {
    initializeChatbotSession();
  }, [initializeChatbotSession]);

  // 세션 바뀌면 메시지 재조회
  useEffect(() => {
    if (!currentSessionId) return;
    loadMessages(currentSessionId);
  }, [currentSessionId, loadMessages]);

  const isLoading = loadingUser || loadingInit || loadingMessages;
  const isInputDisabled = loadingUser || loadingInit || sending || !currentSessionId;

  return (
    <div
      style={{
        ...styles.page,
        height: "calc(100vh - 56px)",
        boxSizing: "border-box",
        display: "flex",
        flexDirection: "column",
        gap: 20,
        minHeight: 0,
        overflow: "hidden",
      }}
    >
      <div>
        <div style={{ fontSize: 16, color: C.sub, fontWeight: 700 }}>
          AI 챗봇
        </div>

        <h1
          style={{
            margin: "6px 0 0",
            fontSize: 38,
            fontWeight: 900,
            letterSpacing: -1,
          }}
        >
          사내 지식 검색
        </h1>
      </div>

      <div
        style={{
          ...styles.card,
          padding: 20,
          flex: 1,
          minHeight: 0,
          display: "flex",
          flexDirection: "column",
          overflow: "hidden",
        }}
      >
        <div
          style={{
            flex: 1,
            minHeight: 0,
            display: "grid",
            gap: 16,
            alignContent: "start",
            overflowY: "auto",
            paddingRight: 4,
          }}
        >
          {loadingInit || loadingMessages ? (
            <div style={{ color: C.sub, fontSize: 14 }}>
              대화를 불러오는 중입니다...
            </div>
          ) : messages.length > 0 ? (
            messages.map((msg, idx) => (
              <Bubble
                key={msg.id ?? `${msg.role}-${idx}`}
                role={msg.role}
                text={msg.text}
                time={msg.time}
              />
            ))
          ) : (
            <Bubble
              role="ai"
              text="사내 규정이나 업무 절차에 대해 무엇이든 질문해 주세요."
              time=""
            />
          )}

          {sending && (
            <div
              style={{
                alignSelf: "flex-start",
                maxWidth: "70%",
                padding: "12px 14px",
                border: `1px solid ${C.border}`,
                borderRadius: 14,
                color: C.sub,
                fontSize: 14,
                background: "#fff",
              }}
            >
              AI가 답변을 생성하는 중입니다...
            </div>
          )}

          {error && (
            <div style={{ color: "#d32f2f", fontSize: 13 }}>
              {error}
            </div>
          )}

          <div ref={bottomRef} />
        </div>

        <div
          style={{
            marginTop: 16,
            border: `1px solid ${C.border}`,
            borderRadius: 14,
            padding: 12,
            display: "flex",
            gap: 12,
            alignItems: "center",
            background: "#fff",
            flexShrink: 0,
          }}
        >
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();

                if (!isInputDisabled) {
                  send();
                }
              }
            }}
            placeholder={
              sending
                ? "AI가 답변을 생성하는 중입니다..."
                : "질문을 입력하세요..."
            }
            disabled={isInputDisabled}
            style={{
              flex: 1,
              border: "none",
              outline: "none",
              fontSize: 14,
              background: "transparent",
              color: "#111827",
              caretColor: "#111827",
              WebkitTextFillColor: "#111827",
            }}
          />

          <button
            type="button"
            onClick={send}
            disabled={isInputDisabled}
            style={{
              width: 38,
              height: 38,
              borderRadius: "50%",
              border: "none",
              background: C.accent,
              color: "#fff",
              cursor: isInputDisabled ? "default" : "pointer",
              opacity: isInputDisabled ? 0.6 : 1,
            }}
          >
            <Icon color="#fff">{I.send}</Icon>
          </button>
        </div>
      </div>
    </div>
  );
}
