/**
 * @FileName : OnboardingStyle.js
 * @Description : AI 온보딩 공통 스타일 정의
 * @Author : 김다솜
 * @Date : 2026. 05. 12
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.12    김다솜       최초 생성
 * @ 2026.05.15    김다솜       사내 AI 포털 기준 온보딩 공통 색상, 카드, 배지 톤 정리
 */

export const COLORS = {
  primary: '#2563EB',
  success: '#16A34A',
  warning: '#F59E0B',
  info: '#0EA5E9',
  dark: '#111827',
  sub: '#6B7280',
  muted: '#94A3B8',
  gray: '#F4F7FB',
  softBlue: '#EEF2FF',
  border: '#DDE3EA',
  font: 'Pretendard, Apple SD Gothic Neo, Noto Sans KR, sans-serif',
};

export const cardCore = {
  background: '#FFFFFF',
  border: `1px solid ${COLORS.border}`,
  boxShadow: '0 2px 10px rgba(15, 23, 42, 0.03)',
  borderRadius: '18px',
};

export const badgeAiTag = {
  backgroundColor: COLORS.softBlue,
  color: COLORS.primary,
  fontSize: '0.7rem',
  fontWeight: '800',
  padding: '0.2rem 0.5rem',
  borderRadius: '999px',
  verticalAlign: 'middle',
  border: '1px solid rgba(37, 99, 235, 0.16)',
  display: 'inline-block',
  lineHeight: '1.2',
};

export const badgeAiSidebar = {
  backgroundColor: COLORS.softBlue,
  color: COLORS.primary,
  fontSize: '0.68rem',
  fontWeight: '800',
  padding: '0.2rem 0.45rem',
  borderRadius: '999px',
  border: '1px solid rgba(37, 99, 235, 0.16)',
  lineHeight: '1.1',
};

export const badgeStageBase = {
  fontWeight: '600',
  padding: '0.4em 0.8em',
  borderRadius: '10rem',
  color: 'white',
};
