/**
 * @FileName : CorrectionScreen.js
 * @Description : AiSecretary.js 전용 문장 다듬기 화면
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 */

import React, { useCallback, useEffect, useMemo, useState } from "react";
import axios from "axios";

import { useUser } from "src/api/UserContext";
import { correctText, unwrapApiData } from "../api/aiSecretaryApi";
import { C, styles } from "../styles/aiSecretaryTheme";
import { PATH } from 'src/constants/path';

/**
 * 문장 다듬기 모드 목록
 *
 * 백엔드 CorrectionRequestDto.mode 로 전달되는 값:
 * - BASIC
 * - BUSINESS_POLITE
 * - CONCISE
 * - LOGICAL
 * - FRIENDLY
 */
const CORRECTION_MODES = [
  {
    value: "BASIC",
    label: "기본 교정",
    description: "맞춤법, 띄어쓰기, 어색한 표현을 자연스럽게 다듬습니다.",
  },
  {
    value: "BUSINESS_POLITE",
    label: "업무용 공손체",
    description: "메일, 메신저, 보고에 적합한 공손한 문장으로 바꿉니다.",
  },
  {
    value: "CONCISE",
    label: "간결하게",
    description: "의미는 유지하면서 불필요한 표현을 줄입니다.",
  },
  {
    value: "LOGICAL",
    label: "논리적으로",
    description: "문장을 더 명확하고 구조적으로 정리합니다.",
  },
  {
    value: "FRIENDLY",
    label: "부드럽게",
    description: "딱딱한 표현을 조금 더 부드럽고 자연스럽게 바꿉니다.",
  },
];

const SAMPLE_TEXT =
  "오늘 회의 언제인지 알려줄게. 2시에 회의실 A에서 모여.";

export default function CorrectionScreen() {
  const { userInfo, updateUserInfo } = useUser();

  /**
   * userInfo가 최초 렌더링 시 null인 경우가 있어
   * ChatbotScreen과 동일하게 자체 복구용 상태를 둔다.
   */
  const [resolvedUserInfo, setResolvedUserInfo] = useState(userInfo);

  const [input, setInput] = useState("");
  const [result, setResult] = useState("");
  const [mode, setMode] = useState("BUSINESS_POLITE");

  const [loadingUser, setLoadingUser] = useState(false);
  const [loadingCorrection, setLoadingCorrection] = useState(false);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);

  const empNo =
    resolvedUserInfo?.empNo ??
    resolvedUserInfo?.emp_no ??
    null;

  const selectedMode = useMemo(() => {
    return (
      CORRECTION_MODES.find((item) => item.value === mode) ??
      CORRECTION_MODES[0]
    );
  }, [mode]);

  const isSubmitDisabled =
    loadingUser || loadingCorrection || !input.trim();

  useEffect(() => {
    if (userInfo) {
      setResolvedUserInfo(userInfo);
    }
  }, [userInfo]);

  /**
   * token은 있는데 UserContext가 비어 있는 경우
   * /api/user/welcome 으로 사용자 정보를 복구한다.
   */
  const ensureUserInfo = useCallback(async () => {
    if (resolvedUserInfo) return resolvedUserInfo;

    const token = localStorage.getItem("token");

    if (!token) {
      setError("로그인 정보가 없습니다. 다시 로그인해 주세요.");
      return null;
    }

    setLoadingUser(true);
    setError("");

    try {
      const response = await axios.get(PATH.AUTH.WELCOME, {
        headers: {
          Authorization: `Bearer ${token}`,
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
        err?.response?.data?.message ||
          "사용자 정보를 불러오지 못했습니다. 다시 로그인해 주세요."
      );

      return null;
    } finally {
      setLoadingUser(false);
    }
  }, [resolvedUserInfo, updateUserInfo]);

  /**
   * 문장 다듬기 실행
   *
   * 프론트 역할:
   * - 원문, 모드, empNo를 백엔드로 전달
   *
   * 백엔드 역할:
   * - Gemini 호출
   * - fallback 처리
   * - AI_LOG 저장
   */
  const submitCorrection = useCallback(async () => {
    const trimmed = input.trim();

    if (!trimmed) {
      setError("다듬을 문장을 입력해 주세요.");
      return;
    }

    if (loadingCorrection) {
      return;
    }

    setLoadingCorrection(true);
    setError("");
    setCopied(false);

    try {
      const ensuredUser = await ensureUserInfo();

      const effectiveEmpNo =
        ensuredUser?.empNo ??
        ensuredUser?.emp_no ??
        empNo;

      if (!effectiveEmpNo) {
        setError("사원번호를 찾을 수 없습니다. 다시 로그인해 주세요.");
        return;
      }

      const response = await correctText({
        empNo: String(effectiveEmpNo),
        text: trimmed,
        mode,
      });

      const data = unwrapApiData(response);

      setResult(data?.correctedText ?? "");
    } catch (err) {
      console.error("문장 다듬기 실패", err);

      setError("문장 다듬기 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setLoadingCorrection(false);
    }
  }, [input, mode, empNo, loadingCorrection, ensureUserInfo]);

  const handleCopyResult = async () => {
    if (!result) return;

    try {
      await navigator.clipboard.writeText(result);
      setCopied(true);

      setTimeout(() => {
        setCopied(false);
      }, 1500);
    } catch (err) {
      console.error("결과 복사 실패", err);
      setError("결과를 복사하지 못했습니다.");
    }
  };

  const handleUseSample = () => {
    setInput(SAMPLE_TEXT);
    setResult("");
    setError("");
    setCopied(false);
  };

  const handleReset = () => {
    setInput("");
    setResult("");
    setError("");
    setCopied(false);
    setMode("BUSINESS_POLITE");
  };

  return (
    <div style={styles.page}>
      {/* 상단 타이틀 */}
      <div style={{ marginBottom: 20 }}>
        <div style={{ fontSize: 16, color: C.sub, fontWeight: 700 }}>
          AI 문장 다듬기
        </div>

        <h1
          style={{
            margin: "6px 0 0",
            fontSize: 38,
            fontWeight: 900,
            letterSpacing: -1,
          }}
        >
          문장을 더 자연스럽게 정리해 보세요
        </h1>

        <p
          style={{
            margin: "10px 0 0",
            color: C.sub,
            fontSize: 14,
            lineHeight: 1.6,
          }}
        >
          메일, 보고 문장, 메신저 문구를 업무 상황에 맞게 다듬을 수 있습니다.
        </p>
      </div>

      {/* 모드 선택 */}
      <div
        style={{
          ...styles.card,
          padding: 18,
          marginBottom: 18,
        }}
      >
        <div
          style={{
            fontSize: 15,
            fontWeight: 800,
            marginBottom: 12,
          }}
        >
          다듬기 방식 선택
        </div>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(5, minmax(0, 1fr))",
            gap: 10,
          }}
        >
          {CORRECTION_MODES.map((item) => {
            const active = item.value === mode;

            return (
              <button
                key={item.value}
                type="button"
                onClick={() => {
                  setMode(item.value);
                  setError("");
                  setCopied(false);
                }}
                style={{
                  border: `1px solid ${active ? C.accent : C.border}`,
                  background: active ? "#EEF2FF" : "#fff",
                  color: active ? C.accent : "#111827",
                  borderRadius: 14,
                  padding: "14px 12px",
                  cursor: "pointer",
                  textAlign: "left",
                  minHeight: 92,
                }}
              >
                <div
                  style={{
                    fontSize: 14,
                    fontWeight: 800,
                    marginBottom: 6,
                  }}
                >
                  {item.label}
                </div>

                <div
                  style={{
                    fontSize: 12,
                    color: active ? C.accent : C.sub,
                    lineHeight: 1.45,
                  }}
                >
                  {item.description}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* 입력 / 결과 영역 */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: 18,
          alignItems: "stretch",
        }}
      >
        {/* 원문 입력 */}
        <div
          style={{
            ...styles.card,
            padding: 20,
            minHeight: 520,
            display: "flex",
            flexDirection: "column",
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: 12,
            }}
          >
            <div>
              <div
                style={{
                  fontSize: 16,
                  fontWeight: 900,
                  marginBottom: 4,
                }}
              >
                원문
              </div>

              <div
                style={{
                  fontSize: 13,
                  color: C.sub,
                }}
              >
                현재 선택: {selectedMode.label}
              </div>
            </div>

            <div style={{ display: "flex", gap: 8 }}>
              <button
                type="button"
                onClick={handleUseSample}
                disabled={loadingCorrection}
                style={{
                  border: `1px solid ${C.border}`,
                  background: "#fff",
                  color: "#111827",
                  borderRadius: 10,
                  padding: "8px 10px",
                  fontSize: 13,
                  cursor: loadingCorrection ? "default" : "pointer",
                  opacity: loadingCorrection ? 0.6 : 1,
                }}
              >
                예시 넣기
              </button>

              <button
                type="button"
                onClick={handleReset}
                disabled={loadingCorrection}
                style={{
                  border: `1px solid ${C.border}`,
                  background: "#fff",
                  color: C.sub,
                  borderRadius: 10,
                  padding: "8px 10px",
                  fontSize: 13,
                  cursor: loadingCorrection ? "default" : "pointer",
                  opacity: loadingCorrection ? 0.6 : 1,
                }}
              >
                초기화
              </button>
            </div>
          </div>

          <textarea
            value={input}
            onChange={(e) => {
              setInput(e.target.value);
              setError("");
              setCopied(false);
            }}
            placeholder="다듬고 싶은 문장을 입력하세요."
            disabled={loadingCorrection}
            style={{
              flex: 1,
              width: "100%",
              resize: "none",
              border: `1px solid ${C.border}`,
              borderRadius: 14,
              padding: 16,
              outline: "none",
              fontSize: 14,
              lineHeight: 1.7,
              color: "#111827",
              background: loadingCorrection ? "#F9FAFB" : "#fff",
              boxSizing: "border-box",
            }}
          />

          <div
            style={{
              marginTop: 12,
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              gap: 12,
            }}
          >
            <div
              style={{
                fontSize: 12,
                color: C.sub,
              }}
            >
              {input.trim().length}자 입력됨
            </div>

            <button
              type="button"
              onClick={submitCorrection}
              disabled={isSubmitDisabled}
              style={{
                border: "none",
                borderRadius: 12,
                background: C.accent,
                color: "#fff",
                padding: "11px 18px",
                fontSize: 14,
                fontWeight: 800,
                cursor: isSubmitDisabled ? "default" : "pointer",
                opacity: isSubmitDisabled ? 0.6 : 1,
              }}
            >
              {loadingCorrection ? "다듬는 중..." : "문장 다듬기"}
            </button>
          </div>

          {error && (
            <div
              style={{
                marginTop: 12,
                color: "#d32f2f",
                fontSize: 13,
                lineHeight: 1.5,
              }}
            >
              {error}
            </div>
          )}
        </div>

        {/* 결과 출력 */}
        <div
          style={{
            ...styles.card,
            padding: 20,
            minHeight: 520,
            display: "flex",
            flexDirection: "column",
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: 12,
            }}
          >
            <div>
              <div
                style={{
                  fontSize: 16,
                  fontWeight: 900,
                  marginBottom: 4,
                }}
              >
                다듬은 결과
              </div>

              <div
                style={{
                  fontSize: 13,
                  color: C.sub,
                }}
              >
                Gemini 기반으로 문장을 자연스럽게 정리합니다.
              </div>
            </div>

            <button
              type="button"
              onClick={handleCopyResult}
              disabled={!result}
              style={{
                border: `1px solid ${C.border}`,
                background: "#fff",
                color: result ? "#111827" : C.sub,
                borderRadius: 10,
                padding: "8px 10px",
                fontSize: 13,
                cursor: result ? "pointer" : "default",
                opacity: result ? 1 : 0.6,
              }}
            >
              {copied ? "복사 완료" : "결과 복사"}
            </button>
          </div>

          <div
            style={{
              flex: 1,
              border: `1px solid ${C.border}`,
              borderRadius: 14,
              padding: 16,
              background: "#F9FAFB",
              color: result ? "#111827" : C.sub,
              fontSize: 14,
              lineHeight: 1.8,
              whiteSpace: "pre-wrap",
              overflowY: "auto",
            }}
          >
            {loadingCorrection ? (
              <div style={{ color: C.sub }}>
                AI가 문장을 다듬고 있습니다...
              </div>
            ) : result ? (
              result
            ) : (
              <div>
                다듬은 결과가 이 영역에 표시됩니다.
                <br />
                왼쪽에 문장을 입력하고 원하는 다듬기 방식을 선택해 주세요.
              </div>
            )}
          </div>

          <div
            style={{
              marginTop: 12,
              fontSize: 12,
              color: C.sub,
              lineHeight: 1.5,
            }}
          >
            AI 결과는 참고용입니다. 중요한 공지, 인사, 결재 문구는 전송 전
            직접 한 번 더 확인해 주세요.
          </div>
        </div>
      </div>
    </div>
  );
}