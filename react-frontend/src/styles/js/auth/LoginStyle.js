/**
 * @FileName : LoginStyle.js
 * @Description : 로그인/웰컴 화면 공통 스타일 정의
 *                - 사용자 홈 톤과 맞춘 블루 그라데이션 배경
 *                - 브랜드 소개 패널 및 액션 카드 스타일
 *                - 입력 필드, 버튼, 정보 박스 스타일
 * @Author : 김다솜
 * @Date : 2026. 04. 30
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.30    김다솜        최초 생성 및 LoginPage 스타일 분리
 * @ 2026.05.15    김다솜        사용자 홈 톤에 맞춘 로그인/웰컴 화면 스타일 전면 개편
 */

export const containerStyle = {
  minHeight: '100vh',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  padding: '32px 20px',
  background:
    'radial-gradient(circle at top left, rgba(255,255,255,0.18) 0, rgba(255,255,255,0) 24%), linear-gradient(135deg, #1f2a56 0%, #321fdb 58%, #4f6bff 100%)',
  fontFamily: '"Segoe UI", Tahoma, Geneva, Verdana, sans-serif',
}

export const pageShell = {
  width: '100%',
  maxWidth: '1120px',
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
  gap: '28px',
  alignItems: 'stretch',
}

export const brandPanel = {
  color: '#ffffff',
  padding: '40px 28px',
  borderRadius: '28px',
  background: 'linear-gradient(180deg, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0.06) 100%)',
  border: '1px solid rgba(255,255,255,0.18)',
  boxShadow: '0 24px 60px rgba(16, 24, 40, 0.24)',
  backdropFilter: 'blur(8px)',
}

export const brandEyebrow = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: '8px',
  padding: '8px 14px',
  borderRadius: '999px',
  background: 'rgba(255,255,255,0.14)',
  border: '1px solid rgba(255,255,255,0.14)',
  fontSize: '0.82rem',
  fontWeight: 700,
  letterSpacing: '0.03em',
  marginBottom: '18px',
}

export const brandTitle = {
  margin: 0,
  fontSize: 'clamp(2rem, 3.5vw, 3rem)',
  lineHeight: 1.08,
  fontWeight: 800,
  letterSpacing: '-0.03em',
}

export const brandSubtitle = {
  marginTop: '18px',
  maxWidth: '540px',
  color: 'rgba(255,255,255,0.86)',
  fontSize: '1rem',
  lineHeight: 1.7,
}

export const brandFeatureGrid = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))',
  gap: '14px',
  marginTop: '28px',
}

export const brandFeatureCard = {
  padding: '16px 18px',
  borderRadius: '18px',
  background: 'rgba(255,255,255,0.12)',
  border: '1px solid rgba(255,255,255,0.12)',
}

export const brandFeatureLabel = {
  fontSize: '0.78rem',
  color: 'rgba(255,255,255,0.72)',
  marginBottom: '8px',
}

export const brandFeatureValue = {
  fontSize: '1rem',
  fontWeight: 700,
  color: '#ffffff',
  lineHeight: 1.5,
}

export const cardStyle = {
  width: '100%',
  alignSelf: 'center',
  padding: '38px 34px',
  background: '#ffffff',
  borderRadius: '28px',
  boxShadow: '0 24px 60px rgba(16, 24, 40, 0.18)',
  border: '1px solid rgba(227, 232, 246, 0.9)',
}

export const cardBadge = {
  display: 'inline-flex',
  alignItems: 'center',
  padding: '8px 14px',
  borderRadius: '999px',
  background: '#eef2ff',
  color: '#321fdb',
  fontSize: '0.8rem',
  fontWeight: 700,
  marginBottom: '16px',
}

export const cardTitle = {
  margin: 0,
  color: '#18213a',
  fontSize: '1.9rem',
  fontWeight: 800,
  letterSpacing: '-0.02em',
}

export const cardDescription = {
  marginTop: '10px',
  marginBottom: '26px',
  color: '#63708a',
  fontSize: '0.96rem',
  lineHeight: 1.7,
}

export const inputStyle = {
  width: '100%',
  padding: '14px 16px',
  marginBottom: '14px',
  border: '1px solid #d7ddec',
  borderRadius: '14px',
  fontSize: '15px',
  color: '#1f2a44',
  background: '#fbfcff',
  boxSizing: 'border-box',
  outline: 'none',
}

export const loginButton = (isLoading) => ({
  width: '100%',
  padding: '15px 16px',
  marginTop: '4px',
  background: isLoading
    ? 'linear-gradient(135deg, #a7b5ef 0%, #8ea2f0 100%)'
    : 'linear-gradient(135deg, #321fdb 0%, #4a68ff 100%)',
  color: '#ffffff',
  border: 'none',
  borderRadius: '14px',
  fontSize: '15px',
  fontWeight: 700,
  cursor: isLoading ? 'not-allowed' : 'pointer',
  boxShadow: isLoading ? 'none' : '0 14px 28px rgba(50, 31, 219, 0.22)',
})

export const helperLink = {
  marginTop: '22px',
  fontSize: '14px',
  color: '#6b7280',
  cursor: 'pointer',
  display: 'inline-block',
}

export const welcomeCard = {
  ...cardStyle,
  maxWidth: '520px',
  margin: '0 auto',
  textAlign: 'left',
}

export const welcomeTitle = {
  margin: 0,
  color: '#18213a',
  fontWeight: 800,
  fontSize: '1.9rem',
  letterSpacing: '-0.02em',
}

export const userInfoBox = {
  marginTop: '24px',
  marginBottom: '24px',
  padding: '18px',
  background: '#f6f8ff',
  borderRadius: '18px',
  border: '1px solid #e4e8f5',
}

export const userInfoRow = {
  margin: '0 0 10px',
  color: '#42506a',
  fontSize: '0.95rem',
}

export const primaryButton = {
  width: '100%',
  padding: '14px 16px',
  border: 'none',
  borderRadius: '14px',
  background: 'linear-gradient(135deg, #321fdb 0%, #4a68ff 100%)',
  color: '#ffffff',
  fontWeight: 700,
  cursor: 'pointer',
  boxShadow: '0 14px 28px rgba(50, 31, 219, 0.18)',
}

export const secondaryButton = {
  width: '100%',
  padding: '14px 16px',
  borderRadius: '14px',
  background: '#f3f5fb',
  color: '#42506a',
  fontWeight: 700,
  cursor: 'pointer',
  border: '1px solid #dce3f2',
  marginTop: '12px',
}
