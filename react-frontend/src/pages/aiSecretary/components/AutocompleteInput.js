/**
 * @FileName : AutocompleteInput.js
 * @Description : KnowledgeRequestScreen.js 전용 문서유형/ 카테고리 input 박스
 *                - input value 표시
 *                - 사용자가 입력하면 후보 목록 필터링 
 *                -  input focus 시 후보 목록 표시
 *                - 후보 클릭 시 값 선택 VS 사용자가 직접 입력한 값 유지
 *                - 후보가 없으면 “새 값으로 입력됩니다.” 또는 후보 없음 문구 표시
 * @Author : 송혜진
 * @Date : 2026. 05. 15
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.05.15  송혜진       최초 생성
 */


import React, { useEffect, useMemo, useRef, useState } from "react";
import { C } from "../styles/aiSecretaryTheme";

// ====================================================================
// 1. 인라인 스타일 가이드 (CSS-IN-JS 패턴)
// ====================================================================
// 컴포넌트의 가장 바깥쪽 감싸는 레이아웃 스타일
const rootStyle = {
  position: "relative",
  display: "grid",
  gap: 8,
};

// 텍스트를 입력하는 input 필드 스타일
const inputStyle = {
  width: "100%",
  minHeight: 46,
  border: `1px solid ${C.border}`,
  borderRadius: 12,
  outline: "none",
  background: "#fff",
  color: C.text,
  fontSize: 14,
  padding: "0 14px",
  boxSizing: "border-box",
  transition: "border-color 0.15s ease, box-shadow 0.15s ease",
};

// 상단 라벨(제목) 스타일
const labelStyle = {
  fontSize: 14,
  fontWeight: 700,
  color: C.text,
  lineHeight: 1.4,
};

// 하단 가이드 문구(helpter text) 스타일
const helperStyle = {
  fontSize: 12,
  color: C.sub,
  lineHeight: 1.55,
};

// 자동완성 추천 목록 팝업(드롭다운) 박스 스타일
const dropdownStyle = {
  position: "absolute",
  top: "calc(100% + 8px)", // input창 바로 아래에 배치
  left: 0,
  right: 0,
  zIndex: 60, // 모달이나 다른 요소에 가려지지 않도록 높게 설정
  background: "#fff",
  border: `1px solid ${C.border}`,
  borderRadius: 12,
  boxShadow: "0 12px 24px rgba(15, 23, 42, 0.12)", // 부드러운 그림자 효과
  maxHeight: 220, // 추천 항목이 많아질 경우 최대 높이 제한 후 스크롤 생성
  overflowY: "auto",
  overflowX: "hidden",
};

// 드롭다운 내부의 개별 추천 아이템 버튼 스타일 
const itemStyle = {
  width: "100%",
  padding: "11px 14px",
  border: 0,
  background: "transparent",
  textAlign: "left",
  cursor: "pointer",
  fontSize: 14,
  color: C.text,
  lineHeight: 1.4,
};

// 검색 결과가 없을 때 보여주는 박스 스타일
const emptyStyle = {
  padding: "13px 14px",
  fontSize: 13,
  color: C.sub,
};

// 텍스트가 너무 길면 잘라내고 말줄임표(...) 처리하는 스타일
const truncateStyle = {
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
};

// 문자열 전처리 헬퍼 함수
// null 이나 undefined가 들어오면 빈 문자열로 바꾸고, 문자열 양 끝의 공백을 제거
function normalizeText(value) {
  return value == null ? "" : String(value).trim();
}

// ====================================================================
// 2. AutocompleteInput 메인 컴포넌트
// ====================================================================
export default function AutocompleteInput({
  label,               // 입력창 라벨 이름 (예: 문서 카테고리)
  showLabel = false,   // 라벨을 화면에 표시할지 여부
  value,               // 상위 컴포넌트로부터 제어받는 input의 현재값(State)
  onChange,            // 값이 변경 될 때 부모 컴포넌트로 전달해주는 핸들러 함수
  suggestions = [],    // 자동완성 추천 단어 리스트 배열
  placeholder,         // 플레이스홀더 문구
  helperText,          // 하단 가이드 문구
  required,            // 필수 입력값(Required Value) 여부
  emptyText = "새 값으로 입력됩니다.", // 매칭되는 추천어가 없을 때 띄월 텍스트
}) {
  const rootRef = useRef(null); // 컴포넌트 최외곽 div를 참조 (바깥 클릭 감지용)
  const inputRef = useRef(null); // input 엘리먼트 참조 (포커스 아웃 제어용)
  
  // WAI-ARIA 접근성 가이드를 위해 드롭다운 박스에 부여할 고유한 ID 값 생성 (컴포넌트가 리렌더링되어도 유지됨)
  const listboxIdRef = useRef(
    `autocomplete-listbox-${Math.random().toString(36).slice(2, 10)}`
  );

  // 드롭다운이 열려있는지 닫혀있는지 관리하는 상태
  const [isOpen, setIsOpen] = useState(false);

  // 현재 입력된 값을 전처리하여 보관
  const normalizedValue = normalizeText(value);

  // [useMemo] 입력값에 따라 추천 단어 목록을 필터링하는 함수
  // 성능 최적화를 위해 normalizedValue와 suggestions 목록이 바뀔 때만 재연산함
  const filteredSuggestions = useMemo(() => {
    const source = Array.isArray(suggestions) ? suggestions : [];
    const query = normalizedValue.toLowerCase();

    // 배열 내 각 아이템들을 텍스트 전처리(공백 제거 등)하고 빈 값은 걸러냄
    const normalizedSource = source
      .map((item) => normalizeText(item))
      .filter(Boolean);

    // 검색어(query)가 없으면 전체 추천 목록을 그대로 반환
    if (!query) {
      return normalizedSource;
    }

    // 사용자가 입력한 단어가 포함(includes)되어 있는 아이템들만 매칭하여 필터링
    const matched = normalizedSource.filter((item) =>
      item.toLowerCase().includes(query)
    );

    // 매칭된 결과가 있으면 결과 배열을, 없으면 빈 배열 반환
    return matched.length > 0 ? matched : [];
  }, [normalizedValue, suggestions]);

  // [useEffect] 드롭다운이 열렸을 대, 화면 전역에 이벤트 리스너를 달아주는 이펙트
  // - 창 바깥을 클릭하면 드롭다운을 자동으로 닫아줌
  // - ESC 키를 누르면 드롭다운을 닫아줌
  useEffect(() => {
    // 드롭다운이 닫혀 있으면 이벤트를 등록할 필요가 없으므로 얼리 리턴
    if (!isOpen) {
      return undefined;
    }

    // 마우스 클릭 및 터치 이벤트 핸들러
    const handlePointerDown = (event) => {
      if (!rootRef.current) {
        return;
      }

      // 클릭한 타겟(event.target)이 컴포넌트 영역(!rootRef.current.contains) 바깥이면 드롭다운을 닫음
      if (!rootRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };

    // 키보드 키 다운 이벤트 핸들러
    const handleKeyDown = (event) => {
      // ESC 키 입력 시 드롭다운을 닫고 input 창에서 포커스를 아웃(blur)시킴
      if (event.key === "Escape") {
        setIsOpen(false);
        inputRef.current?.blur();
      }
    };

    // 전역 document에 이벤트 리스너 등록 (데스크톱 마우스 클릭, 모바일 터치, 키보드 대응)
    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("touchstart", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    // [Clean-up 함수] 컴포넌트가 언마운트되거나 isOpen 상태가 바뀌기 직전에 전역 리스너를 청소하여 메모리 누수 방지
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("touchstart", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  // 사용자가 키보드로 값을 입력할 때 실행되는 핸들러
  const handleInputChange = (event) => {
    onChange?.(event.target.value); // 부모 컴포넌트의 State를 변경 요청
    if (!isOpen) {
      setIsOpen(true); // 글자를 타이핑하기 시작하면 추천 리스트 팝업을 염
    }
  };

  // 추천 항목 중 하나를 마우스로 클릭했을 때 실행되는 핸들러
  const handleSelect = (nextValue) => {
    onChange?.(nextValue); // 선택한 단어로 값을 변경
    setIsOpen(false);      // 선택이 완료되었으므로 드롭다운을 닫음 
  };

  return (
    <div ref={rootRef} style={rootStyle}>
      {/* 1. 라벨 영역 : showLabel이 true이고 label 문구가 존재할 때만 렌더링  */}
      {showLabel && label ? (
        <div style={labelStyle}>
          {label}
          {required && <span style={{ color: "#DC2626", marginLeft: 4 }}>*</span>}
        </div>
      ) : null}

      {/* 2. 텍스트 입력 필드 영역 */}
      <input
        ref={inputRef}
        type="text"
        value={value}
        onFocus={() => setIsOpen(true)} // 인풋창 클릭(포커스) 시 드롭다운 열기
        onChange={handleInputChange}    // 타이핑 이벤트 핸들러 연동
        onKeyDown={(event) => {
          if (event.key === "Escape") {
            setIsOpen(false);
          }
        }}
        // 한글, 한자 등 한 글자가 완성되는 시점(Composition)의 데이터 누락 방지를 위한 이벤트 보정 코드
        onCompositionEnd={(event) => {
          onChange?.(event.currentTarget.value);
          setIsOpen(true);
        }}
        placeholder={placeholder}
        autoComplete="off" // 브라우저 자체의 기본 자동완성 기능 끔

        // 스크린 리더(웹 접근성)을 위한 표준 ARIA 속성 정의
        role="combobox"
        aria-autocomplete="list"
        aria-expanded={isOpen}
        aria-controls={listboxIdRef.current}
        aria-label={label}

        // 포커스 여부(isOpen)에 따라 테두리 색상 및 부드러운 하이라이트 그림자(boxShadow) 동적 변경
        style={{
          ...inputStyle,
          borderColor: isOpen ? C.accent : C.border,
          boxShadow: isOpen ? "0 0 0 3px rgba(37, 99, 235, 0.08)" : "none",
        }}
      />

      {/* 3. 하단 안내 문구 영역 */}
      {helperText ? <div style={helperStyle}>{helperText}</div> : null}

      {/* 4. 자동완성 팝업 리스트 영역(isOpen이 true일 때만 화면에 그림) */}
      {isOpen ? (
        <div id={listboxIdRef.current} role="listbox" style={dropdownStyle}>
          {filteredSuggestions.length > 0 ? (
            // 필터링된 추천어가 존재할 경우 리스트 반복 출력
            filteredSuggestions.map((item) => {
              const current = normalizeText(value);
              const isSelected = current && current === item; // 현재 적힌 글자와 완전히 똑같은 항목인지 판별

              return (
                <button
                  key={item}
                  type="button"
                  role="option"
                  aria-selected={isSelected}
                  // 마우스 다운 시 input창의 포커스가 풀려서 팝업이 의도치 않게 먼저 닫히는 현상 방지
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => handleSelect(item)}
                  style={{
                    ...itemStyle,
                    background: isSelected ? "#F3F4F6" : "transparent",
                    fontWeight: isSelected ? 700 : 500,
                  }}
                  
                  onMouseEnter={(event) => {
                    event.currentTarget.style.background = "#F8FAFC";
                  }}
                  onMouseLeave={(event) => {
                    event.currentTarget.style.background = isSelected ? "#F3F4F6" : "transparent";
                  }}
                >
                  <span style={truncateStyle}>{item}</span>
                </button>
              );
            })
          ) : (
            <div style={emptyStyle}>{emptyText}</div>
          )}
        </div>
      ) : null}
    </div>
  );
}

