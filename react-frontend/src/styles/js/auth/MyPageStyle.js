/**
 * @FileName : MyPageStyle.js
 * @Description : 마이페이지 화면 스타일 정의
 *                - 프로필 상단 커버 영역
 *                - 탭 및 상세 정보 카드 영역
 *                - 사이트 공통 블루/그레이 톤 적용
 * @Author : 김다솜
 * @Date : 2026. 04. 30
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.30    김다솜        최초 생성 및 마이페이지 스타일 분리
 * @ 2026.05.28    김다솜        페이지 디자인 및 프로필 탭 구성 수정
 * @ 2026.05.29    김다솜        프로필 사진 변경 버튼 수정
 */

export const pageWrapStyle = {
    width: '100%',
    maxWidth: '1180px',
    margin: '0 auto',
    padding: '24px',
    background: '#F4F7FB',
    color: '#111827',
    fontFamily: 'var(--cw-user-font-sans)',
};

export const profileCover = {
    height: '150px',
    background: 'linear-gradient(135deg, #2563EB 0%, #1D4ED8 58%, #0F172A 100%)',
    borderRadius: '18px 18px 0 0',
    border: '1px solid rgba(255, 255, 255, 0.14)',
};

export const profileHeader = {
    marginTop: '-50px',
    paddingLeft: '4px',
};

export const profileAvatarWrap = {
    position: 'relative',
    width: '100px',
    height: '100px',
};

export const profileAvatar = {
    width: '100px',
    height: '100px',
    background: '#EEF2FF',
    color: '#2563EB',
};

export const profileNameStyle = {
    color: '#111827',
    fontWeight: 800,
    letterSpacing: '-0.02em',
};

export const profileMetaStyle = {
    color: '#475569',
    fontSize: '0.88rem',
    fontWeight: 700,
    marginTop: '4px',
};

export const profileInfoBlock = {
    marginTop: '14px',
    paddingLeft: '4px',
};

export const activeStatusBadge = {
    fontSize: '0.7rem',
};

export const valueGroup = {
    display: 'flex',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: '0.35rem',
};

export const accountInfoGroup = {
    display: 'flex',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: '1.5rem',
};

export const fieldCardStyle = {
    background: '#FFFFFF',
    border: '1px solid #DDE3EA',
    borderRadius: '18px',
    boxShadow: '0 14px 30px rgba(15, 23, 42, 0.06)',
    overflow: 'hidden',
};

export const fieldRowStyle = 'mb-3 py-3 border-bottom border-light align-items-center';

export const fieldLabelStyle = {
    color: '#64748B',
    fontSize: '0.86rem',
    fontWeight: 800,
};

export const fieldValueStyle = {
    color: '#111827',
    fontWeight: 700,
};

export const tabLinkStyle = (active) => ({
    border: 0,
    borderRadius: '999px',
    marginRight: '8px',
    padding: '9px 16px',
    color: active ? '#FFFFFF' : '#475569',
    background: active ? '#2563EB' : '#EEF2FF',
    fontWeight: 800,
    boxShadow: active ? '0 8px 18px rgba(37, 99, 235, 0.22)' : 'none',
});

export const editButtonStyle = {
    color: '#2563EB',
    background: '#EEF2FF',
    border: '1px solid #BFDBFE',
    borderRadius: '999px',
    fontWeight: 800,
};

export const saveButtonStyle = {
    color: '#FFFFFF',
    background: '#2563EB',
    border: '1px solid #2563EB',
    borderRadius: '999px',
    fontWeight: 800,
};

export const uploadProfileButtonStyle = {
    position: 'absolute',
    right: '-2px',
    bottom: '-2px',
    width: '34px',
    height: '34px',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#FFFFFF',
    background: '#2563EB',
    border: '3px solid #FFFFFF',
    borderRadius: '50%',
    boxShadow: '0 8px 18px rgba(15, 23, 42, 0.18)',
    cursor: 'pointer',
};
