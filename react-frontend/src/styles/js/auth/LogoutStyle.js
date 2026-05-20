/**
 * @FileName : LogoutStyle.js
 * @Description : 로그아웃 확인 모달 및 로그아웃 토스트 스타일 정의
 *                - 사용자 홈/웰컴/사내 AI 포털 화면 톤에 맞춘 블루 계열 UI
 *                - 헤더 컴포넌트의 인라인 스타일 최소화를 위한 스타일 분리
 * @Author : 김다솜
 * @Date : 2026. 05. 18
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.18    김다솜        로그아웃 모달/토스트 스타일 신규 분리
 * @ 2026.05.19    김다솜        포털 기반 로그아웃 커스텀 모달 오버레이 스타일 추가
 */

const palette = {
  primary: '#2563EB',
  primaryDark: '#1D4ED8',
  navy: '#0F172A',
  bg: '#F4F7FB',
  card: '#FFFFFF',
  border: '#DDE3EA',
  text: '#111827',
  sub: '#6B7280',
  softBlue: '#EEF2FF',
}

export const dropdownLogoutItemStyle = {
  cursor: 'pointer',
  userSelect: 'none',
  width: '100%',
  textAlign: 'left',
}

export const logoutModalDialogStyle = {
  fontFamily: 'Pretendard, Apple SD Gothic Neo, Noto Sans KR, sans-serif',
}

export const logoutBackdropStyle = {
  position: 'fixed',
  inset: 0,
  zIndex: 2000,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: '20px',
  background: 'rgba(15, 23, 42, 0.42)',
  backdropFilter: 'blur(8px)',
}

export const logoutDialogShellStyle = {
  width: 'min(480px, 100%)',
  outline: 'none',
}

export const logoutModalContentStyle = {
  borderRadius: '24px',
  overflow: 'hidden',
  border: `1px solid ${palette.border}`,
  boxShadow: '0 24px 60px rgba(15, 23, 42, 0.18)',
}

export const logoutModalHeaderStyle = {
  borderBottom: 'none',
  padding: '24px 26px 0',
  background:
    'radial-gradient(circle at top right, rgba(37, 99, 235, 0.12), rgba(255, 255, 255, 0) 36%), #FFFFFF',
}

export const logoutModalTitleWrapStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: '12px',
  width: '100%',
}

export const logoutModalIconStyle = {
  width: '42px',
  height: '42px',
  borderRadius: '16px',
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  color: palette.primary,
  background: palette.softBlue,
  border: '1px solid rgba(37, 99, 235, 0.16)',
  flexShrink: 0,
}

export const logoutModalTitleStyle = {
  margin: 0,
  fontSize: '1.15rem',
  fontWeight: 800,
  color: palette.text,
  letterSpacing: '-0.02em',
}

export const logoutModalEyebrowStyle = {
  margin: '0 0 3px',
  fontSize: '0.76rem',
  fontWeight: 800,
  color: palette.primary,
  letterSpacing: '0.04em',
}

export const logoutModalBodyStyle = {
  padding: '20px 26px 8px',
  background: palette.card,
}

export const logoutMessageCardStyle = {
  padding: '18px',
  borderRadius: '18px',
  background: palette.bg,
  border: `1px solid ${palette.border}`,
}

export const logoutMessageStyle = {
  margin: 0,
  fontSize: '1rem',
  fontWeight: 800,
  color: palette.text,
  letterSpacing: '-0.01em',
}

export const logoutSubTextStyle = {
  display: 'block',
  marginTop: '8px',
  color: palette.sub,
  fontSize: '0.9rem',
  lineHeight: 1.6,
  wordBreak: 'keep-all',
  overflowWrap: 'normal',
}

export const logoutModalFooterStyle = {
  borderTop: 'none',
  padding: '18px 26px 26px',
  gap: '10px',
  background: palette.card,
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
}

export const logoutCancelButtonStyle = {
  minWidth: '98px',
  padding: '10px 16px',
  borderRadius: '12px',
  border: `1px solid ${palette.border}`,
  background: '#FFFFFF',
  color: palette.sub,
  fontWeight: 800,
  boxShadow: 'none',
}

export const logoutConfirmButtonStyle = {
  minWidth: '112px',
  padding: '10px 16px',
  borderRadius: '12px',
  border: 'none',
  background: `linear-gradient(135deg, ${palette.primary} 0%, ${palette.primaryDark} 64%, ${palette.navy} 100%)`,
  color: '#FFFFFF',
  fontWeight: 800,
  boxShadow: '0 14px 28px rgba(37, 99, 235, 0.24)',
}

export const logoutToastStyle = {
  borderRadius: '16px',
  border: '1px solid rgba(37, 99, 235, 0.14)',
  boxShadow: '0 18px 40px rgba(15, 23, 42, 0.16)',
  background: '#FFFFFF',
  color: palette.text,
}

export const logoutToastBodyStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: '10px',
  fontSize: '14px',
  fontWeight: 700,
}

export const logoutToastIconStyle = {
  width: '24px',
  height: '24px',
  borderRadius: '999px',
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: palette.softBlue,
  color: palette.primary,
  fontSize: '14px',
  fontWeight: 900,
}
