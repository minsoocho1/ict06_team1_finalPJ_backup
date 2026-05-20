/**
 * @FileName : AppLogo.js
 * @Description : COREWORK 브랜드 로고 및 서비스명 표시 컴포넌트
 * @Author : 김다솜
 * @Date : 2026. 05. 11
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.11    김다솜        최초 생성 (COREWORK 육각형 로고 적용)
 * @ 2026.05.15    김다솜        사용자 페이지 로고 색상을 사내 AI 포털 메인색으로 변경
 */
import React from 'react';
import { Link } from 'react-router-dom';
import { PATH } from '../constants/path';

const AppLogo = ({ collapsed = false }) => {
  return (
    <Link to={PATH.AUTH.USERHOME} className="d-flex align-items-center text-decoration-none py-2 px-3">
      {/* 로고 이미지 */}
      <svg
        width="34"
        height="34"
        viewBox="0 0 40 40"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="me-2"
      >
        <path d="M20 3L35.5 11.5V28.5L20 37L4.5 28.5V11.5L20 3Z" fill="#2563EB" />
        <path d="M20 12V28M13 16L27 24M13 24L27 16" stroke="white" strokeWidth="2" strokeLinecap="round" opacity="0.8" />
        <circle cx="20" cy="20" r="3" fill="white" />
      </svg>

      {/* 사이드바가 접힌 상태(collapsed)일 때는 텍스트 숨기기 */}
      {!collapsed && (
        <div className="d-flex flex-column" style={{ userSelect: 'none' }}>
          <span style={{ fontSize: '1.15rem', fontWeight: '800', color: '#111827', lineHeight: 1 }}>
            CORE<span style={{ color: '#2563EB' }}>WORK</span>
          </span>
          <small className="text-muted" style={{ fontSize: '0.6rem', fontWeight: '700', letterSpacing: '0.5px', marginTop: '2px' }}>
            AI-BASED GROUPWARE
          </small>
        </div>
      )}
    </Link>
  );
};

export default AppLogo;
