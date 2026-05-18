/**
 * @FileName : UserHomeStyle.js
 * @Description : 사용자 홈 피드 화면 스타일 정의
 * @Author : 김다솜
 * @Date : 2026. 05. 12
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.12    김다솜        사용자 홈 피드 카드 중앙 정렬 및 최대 폭 제한 스타일 추가
 * @ 2026.05.15    김다솜        UI 조정(AI 사내 포털 기준으로 톤 맞춤)
 */

export const userHomePageStyle = {
  width: '100%',
  maxWidth: '1180px',
  margin: '0 auto',
  padding: '24px',
  background: '#F4F7FB',
  color: '#111827',
  fontFamily: 'Pretendard, Apple SD Gothic Neo, Noto Sans KR, sans-serif',
};

export const userWelcomeSection = {
  background: 'linear-gradient(135deg, #2563EB 0%, #1D4ED8 55%, #0F172A 100%)',
  color: '#FFFFFF',
  padding: '2rem',
  borderRadius: '18px',
  marginBottom: '1.5rem',
  border: '1px solid rgba(255, 255, 255, 0.14)',
  boxShadow: '0 14px 30px rgba(37, 99, 235, 0.2)',
};

export const progressLabel = {
  fontSize: '0.85rem',
  fontWeight: '700',
  color: '#6B7280',
};
