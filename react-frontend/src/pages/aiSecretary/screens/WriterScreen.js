/**
 * @FileName : WriterScreen.js
 * @Description : AiSecretary.js 전용 사용자 AI 비서 문서 작성/수정 화면
 *                - AI가 생성한 초안 문서를 화면에 표시
 *                - 사용자가 추가 수정 요청을 입력하면 /assistant/revise API 호출
 *                - 수정된 문서를 writerState.content에 반영
 *                - 각 수정 결과를 versions에 content와 함께 저장
 *                - 버전 미리보기 / 복원 / 복사 / 다운로드 기능 제공

 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.13  송혜진       템플릿 생성 조건 변경
 */

/*
  주의
  --------------------------------------------------
  - template는 문서 유형이 아니다.
  - correction은 문장 다듬기 입력 기능이다.
*/

import React, { useEffect, useMemo, useRef, useState } from "react";
import AppButton from "../components/AppButton";
import Bubble from "../components/Bubble";
import { I, Icon } from "../constants/aiSecretaryIcons";
import { C, styles } from "../styles/aiSecretaryTheme";
import { reviseAssistantDraft, unwrapApiData } from "../api/aiSecretaryApi";

/**
 * 문서 유형별 화면 메타 정보
 *
 * 기존 documentMap은 정적 목업 문서 전체를 들고 있었지만,
 * 실제 Gemini 초안 생성과 연결된 이후에는 chipLabel, fallbackTitle 정도만 필요합니다.
 */
const DOCUMENT_META_MAP = {
  REPORT: {
    chipLabel: "보고서 초안",
    fallbackTitle: "보고서 초안",
  },

  MINUTES: {
    chipLabel: "회의록 정리",
    fallbackTitle: "회의록 정리",
  },

  APPROVAL: {
    chipLabel: "결재 사유",
    fallbackTitle: "결재 사유",
  },

  TEMPLATE: {
    chipLabel: "템플릿 생성",
    fallbackTitle: "템플릿 생성",
  },
};

function isTableLine(line) {
  const trimmed = String(line || "").trim();
  return trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.includes("|");
}

function splitTableRow(line) {
  return String(line || "")
    .trim()
    .replace(/^\|/, "")
    .replace(/\|$/, "")
    .split("|")
    .map((cell) => cell.trim());
}

function isTableDivider(line) {
  const trimmed = String(line || "").trim();
  if (!trimmed.includes("-")) return false;

  return /^(\|?\s*:?-{3,}:?\s*)+\|?$/.test(trimmed);
}

function parseContentBlocks(content) {
  const lines = String(content || "").split(/\r?\n/);
  const blocks = [];

  const pushParagraph = (buffer) => {
    const text = buffer.join("\n").trim();
    if (text) {
      blocks.push({ type: "paragraph", text });
    }
  };

  let paragraphBuffer = [];

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index];
    const trimmed = line.trim();

    if (!trimmed) {
      pushParagraph(paragraphBuffer);
      paragraphBuffer = [];
      continue;
    }

    const headingMatch = trimmed.match(/^(#{1,3})\s+(.+)$/);
    if (headingMatch) {
      pushParagraph(paragraphBuffer);
      paragraphBuffer = [];

      blocks.push({
        type: "heading",
        level: headingMatch[1].length,
        text: headingMatch[2].trim(),
      });
      continue;
    }

    const orderedMatch = trimmed.match(/^(\d+)\.\s+(.+)$/);
    const unorderedMatch = trimmed.match(/^[-*]\s+(.+)$/);
    if (orderedMatch || unorderedMatch) {
      pushParagraph(paragraphBuffer);
      paragraphBuffer = [];

      const ordered = Boolean(orderedMatch);
      let cursor = index;
      let runLength = 0;

      while (cursor < lines.length) {
        const current = lines[cursor].trim();
        const currentOrdered = current.match(/^(\d+)\.\s+(.+)$/);
        const currentUnordered = current.match(/^[-*]\s+(.+)$/);

        if (ordered && !currentOrdered) break;
        if (!ordered && !currentUnordered) break;

        cursor += 1;
        runLength += 1;
      }

      if (ordered && runLength === 1) {
        blocks.push({
          type: "heading",
          level: 2,
          text: orderedMatch[2].trim(),
        });
        index = cursor - 1;
        continue;
      }

      const items = [];
      let listCursor = index;

      while (listCursor < lines.length) {
        const current = lines[listCursor].trim();
        const currentOrdered = current.match(/^(\d+)\.\s+(.+)$/);
        const currentUnordered = current.match(/^[-*]\s+(.+)$/);

        if (ordered && !currentOrdered) break;
        if (!ordered && !currentUnordered) break;

        items.push(ordered ? currentOrdered[2].trim() : currentUnordered[1].trim());
        listCursor += 1;
      }

      blocks.push({
        type: "list",
        ordered,
        items,
      });
      index = listCursor - 1;
      continue;
    }

    if (isTableLine(trimmed) && isTableLine(lines[index + 1] || "") && isTableDivider(lines[index + 1])) {
      pushParagraph(paragraphBuffer);
      paragraphBuffer = [];

      const header = splitTableRow(trimmed);
      const rows = [];
      let cursor = index + 2;

      while (cursor < lines.length && isTableLine(lines[cursor])) {
        const row = splitTableRow(lines[cursor]);
        if (row.length > 0) {
          rows.push(row);
        }
        cursor += 1;
      }

      blocks.push({
        type: "table",
        header,
        rows,
      });
      index = cursor - 1;
      continue;
    }

    paragraphBuffer.push(trimmed);
  }

  pushParagraph(paragraphBuffer);

  if (blocks.length === 0) {
    return [{ type: "paragraph", text: String(content || "") }];
  }

  return blocks;
}

function renderBlock(block, key) {
  if (block.type === "heading") {
    const HeadingTag = block.level === 3 ? "h3" : "h2";
    return (
      <HeadingTag
        key={key}
        style={{
          margin: "18px 0 10px",
          fontSize: block.level === 3 ? 18 : 22,
          fontWeight: 900,
          lineHeight: 1.35,
        }}
      >
        {block.text}
      </HeadingTag>
    );
  }

  if (block.type === "list") {
    const ListTag = block.ordered ? "ol" : "ul";
    return (
      <ListTag
        key={key}
        style={{
          margin: "12px 0",
          paddingLeft: block.ordered ? 24 : 20,
          lineHeight: 1.8,
        }}
      >
        {block.items.map((item, itemIndex) => (
          <li key={`${key}-${itemIndex}`} style={{ marginBottom: 4 }}>
            {item}
          </li>
        ))}
      </ListTag>
    );
  }

  if (block.type === "table") {
    const columnCount = Math.max(
      block.header?.length || 0,
      ...block.rows.map((row) => row.length)
    );

    return (
      <div
        key={key}
        style={{
          overflowX: "auto",
          margin: "16px 0",
          border: "1px solid #E5E7EB",
          borderRadius: 12,
        }}
      >
        <table
          style={{
            width: "100%",
            borderCollapse: "collapse",
            minWidth: Math.max(480, columnCount * 140),
            background: "#fff",
          }}
        >
          <thead>
            <tr>
              {Array.from({ length: columnCount }).map((_, index) => (
                <th
                  key={`head-${key}-${index}`}
                  style={{
                    borderBottom: "1px solid #D1D5DB",
                    borderRight:
                      index === columnCount - 1 ? "none" : "1px solid #E5E7EB",
                    padding: "12px 14px",
                    textAlign: "left",
                    background: "#F9FAFB",
                    fontWeight: 800,
                    fontSize: 14,
                    verticalAlign: "top",
                  }}
                >
                  {block.header?.[index] || ""}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {block.rows.map((row, rowIndex) => (
              <tr key={`row-${key}-${rowIndex}`}>
                {Array.from({ length: columnCount }).map((_, index) => (
                  <td
                    key={`cell-${key}-${rowIndex}-${index}`}
                    style={{
                      borderTop: "1px solid #E5E7EB",
                      borderRight:
                        index === columnCount - 1 ? "none" : "1px solid #E5E7EB",
                      padding: "12px 14px",
                      fontSize: 14,
                      lineHeight: 1.65,
                      verticalAlign: "top",
                    }}
                  >
                    {row?.[index] || ""}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <p
      key={key}
      style={{
        margin: "0 0 12px",
        whiteSpace: "pre-wrap",
        lineHeight: 1.9,
        fontSize: 15,
      }}
    >
      {block.text}
    </p>
  );
}

/**
 * WriterScreen 역할의 문서 유형 메타 정보
 *
  * 목적:
  * - 문서 저장 과정에서 URL이나 메타 값이 섞이지 않도록
  *   화면 기준에서는 REPORT / MINUTES / APPROVAL 문자열로 다룬다.
 */
const normalizeWriterType = (type) => {
  const normalized = String(type || "").trim().toUpperCase();

  if (normalized === "TEMPLATE") return "TEMPLATE";
  if (normalized === "MINUTES") return "MINUTES";
  if (normalized === "APPROVAL") return "APPROVAL";

  return "REPORT";
};

export default function WriterScreen({
  writerState = {},
  setWriterState,
  writerType = "REPORT",
}) {
  /**
   * writerType 안전 보정
   */
  const safeWriterType = normalizeWriterType(writerType);

  /**
   * writerState 안전 처리
   * --------------------------------------------------
   * writerState.chat / writerState.versions가 undefined인 경우에도
   * 화면이 깨지지 않도록 배열 보정
   */
  const chatMessages = Array.isArray(writerState?.chat)
    ? writerState.chat
    : [];

  const versions = Array.isArray(writerState?.versions)
    ? writerState.versions
    : [];

  /**
   * 현재 미리보기 중인 버전 ID
   * --------------------------------------------------
   * - 현재 버전(current)이면 해당 버전을 기본 미리보기 대상으로 설정
   * - 없으면 마지막 버전
   * - 아무 버전도 없으면 null
   */
  const [previewVersionId, setPreviewVersionId] = useState(null);

  const [actionMessage, setActionMessage] = useState("");
  const [isRevising, setIsRevising] = useState(false);

  /**
   * 안내 메시지 타이머 관리
   *
   * 기존 showActionMessage._timer 방식은 함수가 새로 생성되어
   * 타이머 관리가 불안정할 수 있으므로 useRef로 관리한다.
   */
  const actionTimerRef = useRef(null);
  const previewContentRef = useRef(null);
  const documentPrintRef = useRef(null);
  const [chatPanelHeight, setChatPanelHeight] = useState(760);

  const currentDoc =
    DOCUMENT_META_MAP[safeWriterType] || DOCUMENT_META_MAP.REPORT;

  const getVersionLabel = (version, index) =>
    version?.label || version?.title || `V${index + 1}`;

  /**
   * 결재 사유 문서 여부
   *
   * APPROVAL 문서일 때만 전자결재로 내보내기 버튼을 노출한다.
   */
  const isApprovalDocument = safeWriterType === "APPROVAL";

  /**
   * 실제 문서 표시 데이터 계산
   * --------------------------------------------------
   * draftTitle:
   * - writerState.title이 있으면 실제 문서 제목
   * - 없으면 문서 유형별 fallbackTitle
   *
   * draftContent:
   * - writerState.content가 있으면 실제 Gemini 생성/수정 문서
   * - 없으면 안내 문구
   */
  const fallbackContent =
    "초기 생성된 초안이 없습니다. 문서 작성 시작 화면에서 AI 초안을 먼저 생성해 주세요.";

  const draftTitle =
    writerState?.title || currentDoc.fallbackTitle || "AI 초안";

  const draftContent = writerState?.content || fallbackContent;

  /**
   * 버전 미리보기 계산
   * --------------------------------------------------
   * previewVersionId가 선택되어 있고 해당 버전의 content가 있으면
   * 오른쪽 문서 영역에는 해당 버전의 content를 보여줌
   *
   * 없으면 현재 writerState.content를 보여줌
   */
  const previewVersion = versions.find(
    (version) => version.id === previewVersionId
  );

  const displayContent = previewVersion?.content || draftContent;

  const displayTitle = writerState?.title || draftTitle;

  const documentBlocks = useMemo(
    () => parseContentBlocks(displayContent),
    [displayContent]
  );

  const displayStats = "글자 수 " + (displayContent || "").length.toLocaleString() + "자";

  /**
   * 상단/하단 액션 메시지 표시
   */
  useEffect(() => {
    if (!versions.length) {
      setPreviewVersionId(null);
      return;
    }

    setPreviewVersionId(
      versions.find((version) => version.current)?.id ||
        versions[versions.length - 1]?.id ||
        null
    );
  }, [versions]);

  useEffect(() => {
    const target = previewContentRef.current;

    if (!target) {
      return undefined;
    }

    const measureHeight = () => {
      const measuredHeight = Math.round(target.getBoundingClientRect().height || 0);
      const nextHeight = measuredHeight > 0 ? measuredHeight : 760;

      setChatPanelHeight((prev) =>
        prev === nextHeight ? prev : nextHeight
      );
    };

    measureHeight();

    if (typeof ResizeObserver === "undefined") {
      window.addEventListener("resize", measureHeight);

      return () => {
        window.removeEventListener("resize", measureHeight);
      };
    }

    const observer = new ResizeObserver(() => {
      measureHeight();
    });

    observer.observe(target);

    return () => {
      observer.disconnect();
    };
  }, []);

  const showActionMessage = (message) => {
    setActionMessage(message);

    if (actionTimerRef.current) {
      window.clearTimeout(actionTimerRef.current);
    }

    actionTimerRef.current = window.setTimeout(() => {
      setActionMessage("");
    }, 2000);
  };

  /**
   * 踰꾩쟾 誘몃━蹂닿린
   *
   * 실제 writerState.content를 바꾸지 않고,
   * 오른쪽 문서 표시만 해당 버전 content로 변경한다.
   */
  const handlePreviewVersion = (versionId) => {
    setPreviewVersionId(versionId);
  };

  /**
   * 踰꾩쟾 蹂듭썝
   *
   * 선택한 버전의 content를 writerState.content에 반영한다.
   * 즉, 복원은 미리보기와 별개로 실제 현재 문서 상태를 바꾼다.
   */
  const handleRestoreVersion = (versionId) => {
    setWriterState((prev) => {
      const safeVersions = Array.isArray(prev?.versions)
        ? prev.versions
        : [];

      const selectedVersion = safeVersions.find(
        (version) => version.id === versionId
      );

      return {
        ...prev,
        content: selectedVersion?.content ?? prev.content,
        type: safeWriterType,
        versions: safeVersions.map((version) => ({
          ...version,
          current: version.id === versionId,
        })),
      };
    });

    setPreviewVersionId(versionId);
    showActionMessage(versionId + " 버전으로 복원했습니다.");
  };

  /**
   * AI 추가 수정 요청
   *
   * 사용자가 "더 간결하게", "줄로 정리해줘" 같은 입력을 하면
   * 현재 화면에 표시 중인 문서(displayContent)를 기준으로 /assistant/revise API를 호출.
   *
   * 성공 시
   * - writerState.content를 수정된 문서로 교체
   * - versions에 새 버전 content 저장
   * - 채팅 영역에 사용자 요청/AI 응답 표시
   */
  const addMessage = async () => {
    const instruction = (writerState?.prompt || "").trim();

    if (!instruction) return;
    if (isRevising) return;

    if (!writerState?.sessionId) {
      showActionMessage("수정할 문서 세션 정보가 없습니다.");
      return;
    }

    if (!writerState?.content) {
      showActionMessage("먼저 AI 초안을 생성해 주세요.");
      return;
    }

    const userMessage = {
      role: "user",
      text: instruction,
      time: "방금",
    };

    setIsRevising(true);

    /**
      * 사용자의 메시지를 먼저 화면에 반영한다.
      * 실제 AI 응답은 API 성공 후 추가한다.
     */
    setWriterState((prev) => {
      const safeChat = Array.isArray(prev?.chat) ? prev.chat : [];

      return {
        ...prev,
        chat: [...safeChat, userMessage],
        prompt: "",
      };
    });

    try {
      const response = await reviseAssistantDraft({
        sessionId: writerState.sessionId,

        /**
         * API에는 REPORT / MINUTES / APPROVAL 대문자 문자열로 전달한다.
         */
        type: safeWriterType,

        title: displayTitle,
        currentContent: displayContent,
        instruction,
      });

      const data = unwrapApiData(response);

      const revisedContent = data?.content || displayContent;

      const aiMessage = {
        role: "ai",
        text: data?.fallback
          ? "AI 응답 생성에 실패하여 기본 안내 응답을 표시합니다."
          : "요청하신 내용에 맞게 문서를 업데이트했습니다.",
        time: "방금",
      };

      /**
       * 새 버전 ID는 현재 versions 기준으로 생성한다.
       * isRevising으로 중복 클릭을 막고 있으므로 일반 사용자에게서 안정적이다.
       */
      const nextVersion = {
        id: `v${versions.length + 1}`,
        label: `V${versions.length + 1}`,
        title: "추가 수정",
        summary: instruction,
        content: revisedContent,
        createdAt: new Date().toISOString(),
        seqNo: versions.length + 1,
        modelName: data?.modelName ?? writerState?.modelName ?? "",
        current: true,
      };

      setWriterState((prev) => {
        const safeChat = Array.isArray(prev?.chat) ? prev.chat : [];
        const safeVersions = Array.isArray(prev?.versions)
          ? prev.versions
          : [];

        return {
          ...prev,
          content: revisedContent,
          type: safeWriterType,
          aiMessageId: data?.aiMessageId ?? prev.aiMessageId,
          modelName: data?.modelName ?? prev.modelName,
          fallback: data?.fallback ?? prev.fallback,
          chat: [...safeChat, aiMessage],
          versions: [
            ...safeVersions.map((version) => ({
              ...version,
              current: false,
            })),
            nextVersion,
          ],
        };
      });

      setPreviewVersionId(nextVersion.id);
    } catch (error) {
      console.error("AI 문서 수정 실패", error);

      const aiMessage = {
        role: "ai",
        text: "문서 수정 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.",
        time: "방금",
      };

      setWriterState((prev) => {
        const safeChat = Array.isArray(prev?.chat) ? prev.chat : [];

        return {
          ...prev,
          chat: [...safeChat, aiMessage],
        };
      });
    } finally {
      setIsRevising(false);
    }
  };

  /**
   * 현재 표시 중인 문서 복사
   *
   * 미리보기 중인 버전이면 해당 버전 content를 복사한다.
   */
  const handleCopy = async () => {
    const textToCopy = displayTitle + "\n\n" + displayContent;

    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(textToCopy);
        showActionMessage("문서 내용을 복사했습니다.");
        return;
      }

      const textarea = document.createElement("textarea");
      textarea.value = textToCopy;
      textarea.style.position = "fixed";
      textarea.style.top = "-9999px";
      textarea.style.left = "-9999px";

      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();
      document.execCommand("copy");
      document.body.removeChild(textarea);

      showActionMessage("문서 내용을 복사했습니다.");
    } catch (error) {
      console.warn("복사 기능을 사용할 수 없습니다.", error);
      showActionMessage("복사에 실패했습니다.");
    }
  };

  /**
   * 현재 표시 중인 문서 다운로드
   */
  const handleDownload = () => {
    const blob = new Blob([displayTitle + "\n\n" + displayContent], {
      type: "text/plain;charset=utf-8",
    });

    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");

    anchor.href = url;
    anchor.download = (displayTitle || "AI_초안") + ".txt";
    anchor.click();

    URL.revokeObjectURL(url);

    showActionMessage("문서 다운로드가 시작되었습니다.");
  };

  const handlePrint = () => {
    const sourceNode = documentPrintRef.current;

    if (!sourceNode) {
      showActionMessage("출력할 문서 영역을 찾을 수 없습니다.");
      return;
    }

    const printWindow = window.open("", "_blank", "width=1100,height=900");

    if (!printWindow) {
      showActionMessage("인쇄 창을 열 수 없습니다.");
      return;
    }

    const clonedNode = sourceNode.cloneNode(true);

    printWindow.document.open();
    printWindow.document.write(`<!DOCTYPE html>
<html lang="ko">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>AI 비서 문서</title>
    <style>
      @page {
        size: auto;
        margin: 14mm;
      }

      html,
      body {
        margin: 0;
        padding: 0;
        background: #ffffff;
        color: #111827;
      }

      body {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans KR", sans-serif;
        -webkit-print-color-adjust: exact;
        print-color-adjust: exact;
      }

      .print-document {
        box-sizing: border-box;
        width: 100%;
        padding: 0;
      }

      .print-document h2,
      .print-document h3 {
        break-after: avoid;
      }

      .print-document p,
      .print-document li,
      .print-document th,
      .print-document td {
        word-break: break-word;
      }

      .print-document table {
        width: 100%;
        border-collapse: collapse;
      }
    </style>
  </head>
  <body>
    <div class="print-document"></div>
  </body>
</html>`);
    printWindow.document.close();

    const mountPoint = printWindow.document.querySelector(".print-document");

    if (!mountPoint) {
      printWindow.close();
      showActionMessage("인쇄 영역을 준비할 수 없습니다.");
      return;
    }

    mountPoint.appendChild(clonedNode);

    printWindow.onafterprint = () => {
      printWindow.close();
    };

    window.setTimeout(() => {
      printWindow.focus();
      printWindow.print();
    }, 150);
  };

  /**
   * 전자결재로 내보내기
   *
   * 아직 실제 전자결재 API와 연결하지 않았으므로
   * 현재의 버전 기록에 내보내기 기록만 남긴다.
   */
  const handleExportToApproval = () => {
    const nextVersion = {
      id: "v" + (versions.length + 1),
      label: "V" + (versions.length + 1),
      summary: "문서를 전자결재 > 임시보관함 용도로 내보냈습니다.",
      content: displayContent,
      createdAt: new Date().toISOString(),
      seqNo: versions.length + 1,
      modelName: writerState?.modelName || "",
      current: true,
    };

    setWriterState((prev) => {
      const safeVersions = Array.isArray(prev?.versions)
        ? prev.versions
        : [];

      return {
        ...prev,
        content: displayContent,
        type: safeWriterType,
        showHistory: true,
        versions: [
          ...safeVersions.map((version) => ({
            ...version,
            current: false,
          })),
          nextVersion,
        ],
      };
    });

    setPreviewVersionId(nextVersion.id);
    showActionMessage("전자결재 > 임시보관함에 문서가 저장되었습니다.");
  };

  return (
    <div
      style={{
        ...styles.page,
        paddingRight: writerState?.showHistory ? 12 : 28,
      }}
    >
      <style>{`
        @media print {
          @page {
            size: auto;
            margin: 14mm;
          }

          body.ai-print-mode {
            background: #fff !important;
          }

          body.ai-print-mode .ai-no-print {
            display: none !important;
          }

          body.ai-print-mode .ai-document-print-area {
            display: block !important;
            padding: 0 !important;
            margin: 0 !important;
            width: 100% !important;
          }

          body.ai-print-mode .ai-document-print-area table {
            width: 100% !important;
          }

          body.ai-print-mode .ai-document-print-area h2,
          body.ai-print-mode .ai-document-print-area h3,
          body.ai-print-mode .ai-document-print-area p,
          body.ai-print-mode .ai-document-print-area li,
          body.ai-print-mode .ai-document-print-area td,
          body.ai-print-mode .ai-document-print-area th {
            color: #111827 !important;
          }
        }
      `}</style>

      {/* 상단 제목 영역 */}
      <div className="ai-no-print" style={{ marginBottom: 18 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <h1
            style={{
              margin: 0,
              fontSize: 38,
              fontWeight: 900,
              letterSpacing: -1,
            }}
          >
            AI 비서 · 문서 작성
          </h1>

          <div
            style={{
              height: 34,
              padding: "0 12px",
              borderRadius: 10,
              background: C.accentBg,
              color: C.accent,
              display: "flex",
              alignItems: "center",
              fontWeight: 800,
            }}
          >
            {currentDoc.chipLabel}
          </div>
        </div>

        <p style={{ margin: "10px 0 0", color: C.sub, fontSize: 16 }}>
          문서를 구조화해 미리보기하고 수정할 수 있습니다.
        </p>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: writerState?.showHistory
            ? "300px 1fr 280px"
            : "300px 1fr",
          gap: 16,
          alignItems: "start",
        }}
      >
        {/* 좌측: AI 대화 영역 */}
        <div
          className="ai-no-print"
          style={{
            ...styles.card,
            padding: 20,
            display: "flex",
            flexDirection: "column",
            height: chatPanelHeight || 760,
            minHeight: 760,
            overflow: "hidden",
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <h3 style={styles.sectionTitle}>AI 대화</h3>
          </div>

          <div
            style={{
              marginTop: 18,
              flex: 1,
              display: "grid",
              gap: 16,
              alignContent: "start",
              minHeight: 0,
              overflowY: "auto",
              paddingRight: 6,
            }}
          >
            {chatMessages.map((message, index) => (
              <Bubble
                key={`${message.time}-${index}`}
                role={message.role}
                text={message.text}
                time={message.time}
              />
            ))}

            <div style={{ ...styles.card, padding: 14, borderRadius: 14 }}>
              <div style={{ fontSize: 14, fontWeight: 800 }}>
                {displayTitle}
              </div>

              <div style={{ marginTop: 8, fontSize: 12, color: C.sub }}>
                최근 수정 · 방금
              </div>

              <div
                style={{
                  marginTop: 8,
                  color: C.accent,
                  fontWeight: 800,
                  fontSize: 13,
                }}
              >
                미리보기
              </div>
            </div>
          </div>

          {/* 수정 요청 입력 영역 */}
          <div style={{ flexShrink: 0 }}>
            <div
              style={{
                border: `1px solid ${C.border}`,
                borderRadius: 14,
                padding: 12,
                display: "flex",
                gap: 10,
                alignItems: "center",
              }}
            >
              <input
                value={writerState?.prompt || ""}
                onChange={(event) =>
                  setWriterState((prev) => ({
                    ...prev,
                    prompt: event.target.value,
                  }))
                }
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    event.preventDefault();
                    addMessage();
                  }
                }}
                disabled={isRevising}
                placeholder={
                  isRevising
                    ? "AI가 문서를 수정하는 중입니다..."
                    : "수정 요청을 입력하세요..."
                }
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
                onClick={addMessage}
                disabled={isRevising}
                style={{
                  width: 36,
                  height: 36,
                  borderRadius: "50%",
                  border: "none",
                  background: C.accent,
                  color: "#fff",
                  cursor: isRevising ? "default" : "pointer",
                  opacity: isRevising ? 0.6 : 1,
                }}
              >
                <Icon color="#fff">{I.send}</Icon>
              </button>
            </div>

            <div style={{ marginTop: 10, fontSize: 12, color: C.muted }}>
              입력은 짧고 명확하게 적어 주면 더 안정적으로 반영됩니다.
            </div>
          </div>
        </div>

        {/* 가운데: 문서 미리보기 영역 */}
          <div
            ref={previewContentRef}
            style={{ ...styles.card, overflow: "hidden", alignSelf: "start" }}
          >
          <div
            className="ai-no-print"
            style={{ padding: 18, borderBottom: `1px solid ${C.border}` }}
          >
            <div
              style={{
                display: "flex",
                gap: 8,
                flexWrap: "wrap",
                alignItems: "center",
              }}
            >
              <AppButton
                variant="secondary"
                style={{ height: 36 }}
                onClick={() =>
                  setWriterState((prev) => ({
                    ...prev,
                    showHistory: !prev.showHistory,
                  }))
                }
              >
                <Icon>{I.history}</Icon>
                버전 기록
              </AppButton>

              {isApprovalDocument && (
                <AppButton
                  style={{ height: 36 }}
                  onClick={handleExportToApproval}
                >
                  전자결재로 내보내기
                </AppButton>
              )}

              <AppButton
                variant="secondary"
                style={{ height: 36 }}
                onClick={handleDownload}
              >
                <Icon>{I.download}</Icon>
                TXT 다운로드
              </AppButton>

              <AppButton
                variant="secondary"
                style={{ height: 36 }}
                onClick={handlePrint}
              >
                PDF 저장/인쇄
              </AppButton>

              <AppButton
                variant="secondary"
                style={{ height: 36 }}
                onClick={handleCopy}
              >
                <Icon>{I.copy}</Icon>
                복사
              </AppButton>
            </div>

            <div
              style={{
                marginTop: 14,
                background: C.softBlue,
                borderRadius: 12,
                padding: "12px 14px",
                color: C.accent,
                fontSize: 13,
                fontWeight: 700,
              }}
            >
              생성된 문서는 바로 복사하거나 다운로드할 수 있으며 추가 수정도 가능합니다.
              AI와 대화를 이어가며 문서를 계속 다듬을 수 있습니다.
            </div>

            {actionMessage && (
              <div
                style={{
                  marginTop: 10,
                  padding: "12px 14px",
                  borderRadius: 12,
                  background: C.softGreen,
                  color: C.success,
                  fontSize: 13,
                  fontWeight: 700,
                }}
              >
                {actionMessage}
              </div>
            )}
          </div>

          <div
            ref={documentPrintRef}
            data-preview-target="writer-document"
            className="ai-document-print-area"
            style={{ padding: 22 }}
          >
            <div style={{ fontSize: 18, fontWeight: 900 }}>{displayTitle}</div>

            <div
              style={{
                marginTop: 18,
                color: writerState?.content ? C.text : C.sub,
                fontSize: 15,
                lineHeight: 1.85,
              }}
            >
              {documentBlocks.map((block, index) =>
                renderBlock(block, `${block.type}-${index}`)
              )}
            </div>
          </div>

          <div
            style={{
              padding: "14px 22px",
              borderTop: `1px solid ${C.border}`,
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              color: C.sub,
              fontSize: 13,
            }}
          >
            <div>{displayStats}</div>
          </div>
        </div>

        {/* 우측: 버전 기록 영역 */}
        {writerState?.showHistory && (
          <div
            className="ai-no-print"
            style={{ ...styles.card, padding: 20, minHeight: 760, height: "100%" }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <h3 style={styles.sectionTitle}>버전 기록</h3>

                <div
                  style={{
                    height: 28,
                    padding: "0 10px",
                    borderRadius: 999,
                    background: C.softGreen,
                    color: C.success,
                    display: "inline-flex",
                    alignItems: "center",
                    fontSize: 12,
                    fontWeight: 800,
                  }}
                >
                  자동 저장됨
                </div>
              </div>

              <button
                type="button"
                onClick={() =>
                  setWriterState((prev) => ({
                    ...prev,
                    showHistory: false,
                  }))
                }
                style={{
                  border: `1px solid ${C.border}`,
                  background: "#fff",
                  borderRadius: 10,
                  minWidth: 54,
                  height: 32,
                  padding: "0 10px",
                  fontSize: 13,
                  fontWeight: 800,
                  cursor: "pointer",
                  color: C.sub,
                }}
              >
                닫기
              </button>
            </div>

            <div style={{ ...styles.sectionSub, marginTop: 10 }}>
              이전 버전을 미리보기하고, 원하는 버전으로 복원할 수 있습니다.
            </div>

            <div style={{ marginTop: 22, display: "grid", gap: 16 }}>
              {versions.length === 0 ? (
                <div
                  style={{
                    padding: 18,
                    border: `1px dashed ${C.border}`,
                    borderRadius: 14,
                    color: C.sub,
                    fontSize: 13,
                    lineHeight: 1.6,
                    background: "#fff",
                  }}
                >
                  저장된 버전이 없습니다.
                </div>
              ) : (
                versions
                  .slice()
                  .reverse()
                  .map((version, index) => (
                  <div
                    key={version.id}
                    style={{
                      ...styles.card,
                      padding: 16,
                      border:
                        previewVersionId === version.id
                          ? `1px solid ${C.accent}`
                          : `1px solid ${C.border}`,
                      background:
                        previewVersionId === version.id ? "#F8FBFF" : "#fff",
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                      }}
                    >
                      <div
                        style={{
                          width: 42,
                          height: 42,
                          borderRadius: "50%",
                          border: `2px solid ${C.accent}`,
                          color: C.accent,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          fontWeight: 900,
                        }}
                      >
                        {getVersionLabel(version, versions.length - 1 - index)}
                      </div>

                      {version.current && (
                        <div
                          style={{
                            height: 26,
                            padding: "0 10px",
                            borderRadius: 999,
                            background: C.softGreen,
                            color: C.success,
                            display: "flex",
                            alignItems: "center",
                            fontSize: 12,
                            fontWeight: 800,
                          }}
                        >
                          현재
                        </div>
                      )}
                    </div>

                    <div style={{ marginTop: 12, fontWeight: 900 }}>
                      {getVersionLabel(version, versions.length - 1 - index)}
                    </div>

                    <div
                      style={{
                        marginTop: 8,
                        fontSize: 13,
                        color: C.sub,
                        lineHeight: 1.6,
                      }}
                    >
                      {version.summary}
                    </div>

                    <div style={{ display: "flex", gap: 8, marginTop: 14 }}>
                      <AppButton
                        variant="secondary"
                        style={{ flex: 1 }}
                        onClick={() => handlePreviewVersion(version.id)}
                      >
                        미리보기
                      </AppButton>

                      <AppButton
                        style={{ flex: 1 }}
                        onClick={() => handleRestoreVersion(version.id)}
                      >
                        이 버전으로 복원
                      </AppButton>
                    </div>
                  </div>
                  ))
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
