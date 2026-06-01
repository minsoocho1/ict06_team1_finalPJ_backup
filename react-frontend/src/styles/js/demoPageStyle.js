// 1차 시연용으로 화면과 sql 쿼리를 함께 보여주기 위한 스타일 구현용 코드입니다. 
// src/styles/js/demoPageStyle.js

export const containerStyle = {
  padding: '24px',
  maxWidth: '1600px',
  margin: '0 auto',
  fontFamily: 'var(--cw-user-font-sans)'
};

export const stepCardStyle = (status) => ({
  padding: '20px',
  marginBottom: '15px',
  borderRadius: '10px',
  borderLeft: `8px solid ${
    status === 'completed'
      ? '#27ae60'
      : status === 'current'
      ? '#1877f2'
      : '#ddd'
  }`,
  backgroundColor: '#fff',
  boxShadow: '0 4px 6px rgba(0,0,0,0.05)',
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center'
});
