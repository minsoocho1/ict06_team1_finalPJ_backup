import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import CIcon from '@coreui/icons-react';
import {
  cilClock,
  cilDescription,
  cilInbox,
  cilPencil,
  cilTask,
  cilArrowThickFromRight,
  cilArrowThickFromLeft,
} from '@coreui/icons';

import { PATH } from 'src/constants/path';
import { canViewApproverMenus } from './approvalAuth';

const C = {
  bg: '#F4F7FB',
  sidebar: '#FFFFFF',
  border: '#DDE3EA',
  text: '#111827',
  sub: '#6B7280',
  muted: '#94A3B8',
  accent: '#2563EB',
  accentBg: '#EEF2FF',
  hoverBg: '#F8FAFC',
};

const baseMenuItems = [
  {
    label: '새 결재 작성',
    description: '결재 서식을 선택해 문서를 작성합니다.',
    path: PATH.APPROVAL.NEW_SELECT,
    matchPaths: [PATH.APPROVAL.NEW_SELECT, PATH.APPROVAL.NEW_WRITE, PATH.APPROVAL.NEW_SETLINE],
    icon: cilPencil,
  },
  {
    label: '개인문서함',
    description: '상신 문서와 참조 문서를 확인합니다.',
    path: PATH.APPROVAL.PERSONAL,
    matchPaths: [PATH.APPROVAL.PERSONAL, PATH.APPROVAL.PERSONAL_DETAIL],
    icon: cilDescription,
  },
  {
    label: '임시저장함',
    description: '상신 전 문서를 이어서 작성합니다.',
    path: PATH.APPROVAL.TMP,
    matchPaths: [PATH.APPROVAL.TMP, PATH.APPROVAL.TMP_DETAIL],
    icon: cilInbox,
  },
];

const approverMenuItems = [
  {
    label: '결재 대기 문서함',
    description: '지금 내가 결재해야 하는/결재 완료한 문서입니다.',
    path: PATH.APPROVAL.PENDING,
    matchPaths: [PATH.APPROVAL.PENDING, PATH.APPROVAL.PENDING_DETAIL],
    icon: cilTask,
  },
  {
    label: '결재 예정 문서함',
    description: '이후 단계에서 내 차례가 올 문서입니다.',
    path: PATH.APPROVAL.UPCOMING,
    matchPaths: [PATH.APPROVAL.UPCOMING, PATH.APPROVAL.UPCOMING_DETAIL],
    icon: cilClock,
  },
];

const styles = {
  app: {
    display: 'flex',
    minHeight: 'calc(100vh - 92px)',
    background: C.bg,
    color: C.text,
  },
  sidebar: {
    background: C.sidebar,
    borderRight: `1px solid ${C.border}`,
    display: 'flex',
    flexDirection: 'column',
    flexShrink: 0,
    overflow: 'hidden',
    transition: 'width 150ms ease',
    willChange: 'width',
  },
  main: {
    flex: 1,
    minWidth: 0,
  },
  menuList: {
    padding: 16,
    display: 'grid',
    gap: 8,
  },
};

/*
 * AI 포털의 보조 사이드바 패턴을 전자결재에 맞춰 적용한 레이아웃입니다.
 * 라우트 전체를 감싸므로 전자결재 어느 화면에서든 같은 위치에서 문서함을 이동할 수 있습니다.
 */
const ApprovalLayout = ({ userInfo, children }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [hoveredPath, setHoveredPath] = useState('');
  const showApproverMenus = canViewApproverMenus(userInfo);
  const menuItems = showApproverMenus ? [...baseMenuItems, ...approverMenuItems] : baseMenuItems;

  const isActive = (item) => item.matchPaths.some((path) => location.pathname === path);

  return (
    <div style={styles.app}>
      <aside
          style={{
            ...styles.sidebar,
            width: sidebarOpen ? 272 : 58,
            padding: sidebarOpen ? 0 : '14px 10px',
          }}
      >
        <div
          style={{
            padding: sidebarOpen ? 22 : 0,
            borderBottom: sidebarOpen ? `1px solid ${C.border}` : 'none',
            display: 'flex',
            alignItems: sidebarOpen ? 'flex-start' : 'center',
            justifyContent: sidebarOpen ? 'space-between' : 'center',
            gap: 12,
            transition: 'padding 150ms ease, border-color 150ms ease',
          }}
        >
          <div
            style={{
              opacity: sidebarOpen ? 1 : 0,
              width: sidebarOpen ? 188 : 0,
              overflow: 'hidden',
              whiteSpace: 'nowrap',
              transition: 'opacity 120ms ease, width 150ms ease',
            }}
          >
              <div style={{ fontSize: 20, fontWeight: 900, color: C.text }}>전자결재</div>
          </div>

          <button
            type="button"
            onClick={() => setSidebarOpen((prev) => !prev)}
            title={sidebarOpen ? '전자결재 사이드바 접기' : '전자결재 사이드바 펼치기'}
            aria-label={sidebarOpen ? '전자결재 사이드바 접기' : '전자결재 사이드바 펼치기'}
            style={{
              width: 36,
              height: 36,
              border: `0px solid ${C.border}`,
              borderRadius: 10,
              background: '#fff',
              color: C.accent,
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              flexShrink: 0,
              transition: 'border-color 120ms ease, color 120ms ease, background-color 120ms ease',
            }}
          >
            <CIcon icon={sidebarOpen ? cilArrowThickFromRight : cilArrowThickFromLeft} />
          </button>
        </div>

        <nav
          style={{
            ...styles.menuList,
            opacity: sidebarOpen ? 1 : 0,
            pointerEvents: sidebarOpen ? 'auto' : 'none',
            transform: sidebarOpen ? 'translateX(0)' : 'translateX(-8px)',
            transition: 'opacity 120ms ease, transform 150ms ease',
          }}
          aria-label="전자결재 메뉴"
        >
            {menuItems.map((item) => {
              const active = isActive(item);
              const hovered = hoveredPath === item.path;

              return (
                <button
                  key={item.path}
                  type="button"
                  onClick={() => navigate(item.path)}
                  onMouseEnter={() => setHoveredPath(item.path)}
                  onMouseLeave={() => setHoveredPath('')}
                  style={{
                    width: '100%',
                    border: 'none',
                    borderRadius: 14,
                    padding: '12px 14px',
                    background: active ? C.accentBg : hovered ? C.hoverBg : 'transparent',
                    color: active ? C.accent : C.text,
                    cursor: 'pointer',
                    textAlign: 'left',
                    display: 'flex',
                    gap: 12,
                    alignItems: 'flex-start',
                    transition: 'background-color 120ms ease, color 120ms ease',
                  }}
                >
                  <span
                    style={{
                      width: 34,
                      height: 34,
                      borderRadius: 10,
                      background: active ? '#fff' : hovered ? '#fff' : '#F8FAFC',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: active || hovered ? C.accent : C.sub,
                      flexShrink: 0,
                      transition: 'background-color 120ms ease, color 120ms ease',
                    }}
                  >
                    <CIcon icon={item.icon} />
                  </span>

                  <span style={{ minWidth: 0 }}>
                    <span style={{ display: 'block', fontSize: 14, fontWeight: 900 }}>{item.label}</span>
                    <span
                      style={{
                        display: 'block',
                        marginTop: 4,
                        fontSize: 12,
                        color: active || hovered ? C.accent : C.sub,
                        lineHeight: 1.4,
                        transition: 'color 120ms ease',
                      }}
                    >
                      {item.description}
                    </span>
                  </span>
                </button>
              );
            })}
        </nav>
      </aside>

      <main style={styles.main}>{children}</main>
    </div>
  );
};

export default ApprovalLayout;
