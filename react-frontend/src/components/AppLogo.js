/**
 * @FileName : AppLogo.js
 * @Description : COREWORK 브랜드 로고 및 서비스명 표시 컴포넌트
 * @Author : 김다솜
 * @Date : 2026. 05. 11
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.11    김다솜        최초 생성(COREWORK 육각형 로고 적용)
 * @ 2026.05.15    김다솜        사용자 페이지 로고 색상을 사내 AI 포털 메인색으로 변경
 * @ 2026.05.29    김다솜        서비스 로고 디자인 수정
 */
import React from 'react';
import { Link } from 'react-router-dom';
import { PATH } from '../constants/path';

const AppLogo = ({ collapsed = false }) => {
  return (
    <Link
      to={PATH.AUTH.USERHOME}
      className="d-flex align-items-center"
      style={{
        alignSelf: 'center',
        background: 'linear-gradient(135deg, rgba(37, 99, 235, 0.12) 0%, rgba(255, 255, 255, 0.86) 100%)',
        border: '0',
        borderBottom: '0',
        borderRadius: '16px',
        boxShadow: 'none',
        gap: '10px',
        justifyContent: collapsed ? 'center' : 'flex-start',
        margin: '12px auto 10px',
        outline: 'none',
        padding: collapsed ? '10px' : '11px 14px',
        textDecoration: 'none',
        textDecorationLine: 'none',
        width: collapsed ? '54px' : 'fit-content',
      }}
    >
      <svg
        width="34"
        height="34"
        viewBox="0 0 44 44"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-label="COREWORK logo"
      >
        <defs>
          <linearGradient id="coreworkLogoGradient" x1="6" y1="5" x2="39" y2="40" gradientUnits="userSpaceOnUse">
            <stop stopColor="#2563EB" />
            <stop offset="0.56" stopColor="#1D4ED8" />
            <stop offset="1" stopColor="#0F172A" />
          </linearGradient>
        </defs>
        <rect x="4" y="4" width="36" height="36" rx="12" fill="url(#coreworkLogoGradient)" />
        <path
          d="M28.6 15.7C27.25 14.08 25.16 13 22.54 13C17.84 13 14.2 16.63 14.2 22C14.2 27.36 17.84 31 22.54 31C25.22 31 27.34 29.88 28.7 28.18"
          stroke="#FFFFFF"
          strokeLinecap="round"
          strokeWidth="3"
        />
        <path
          d="M21.5 14.2L25.15 29.8L29.7 17.2L34.15 29.8L37.8 14.2"
          stroke="#BFDBFE"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="2.4"
        />
      </svg>

      {!collapsed && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '3px', userSelect: 'none', minWidth: 0 }}>
          <span
            style={{
              color: '#0F172A',
              fontSize: '1.2rem',
              fontWeight: 900,
              letterSpacing: '-0.06em',
              lineHeight: 1,
              textDecoration: 'none',
            }}
          >
            CORE<span style={{ color: '#2563EB' }}>WORK</span>
          </span>
          <span
            style={{
              color: '#64748B',
              fontSize: '0.58rem',
              fontWeight: 800,
              letterSpacing: '0.1em',
              lineHeight: 1,
              textDecoration: 'none',
            }}
          >
            AI-BASED GROUPWARE
          </span>
        </div>
      )}
    </Link>
  );
};

export default AppLogo;
