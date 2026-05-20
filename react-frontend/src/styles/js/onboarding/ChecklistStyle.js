/**
 * @FileName : checklistStyle.js
 * @Description : 온보딩 체크리스트 관련 스타일 정의
 *                - 체크리스트 목록 UI 스타일
 *                - 체크리스트 미리보기 카드 스타일
 *                - 로드맵 화면 위젯 스타일
 * @Author : 김다솜
 * @Date : 2026. 04. 29
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    김다솜        최초 생성 및 체크리스트 스타일 분리
 * @ 2026.05.15    김다솜        UI 조정(AI 사내 포털 기준으로 톤 맞춤)
 */

// ===== 공통 =====
export const checklistContainer = {
    padding: '24px',
    maxWidth: '1180px',
    margin: '0 auto',
    background: '#F4F7FB',
    color: '#111827',
    fontFamily: 'Pretendard, Apple SD Gothic Neo, Noto Sans KR, sans-serif'
};

export const checklistPageHeader = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    gap: '16px',
    marginBottom: '18px'
};

export const checklistEyebrow = {
    margin: '0 0 6px',
    fontSize: '13px',
    fontWeight: 800,
    color: '#2563EB',
    letterSpacing: '0.02em'
};

export const checklistPageTitle = {
    margin: 0,
    fontSize: '28px',
    fontWeight: 800,
    color: '#111827'
};

export const checklistPageDesc = {
    margin: '8px 0 0',
    color: '#6B7280',
    fontSize: '14px',
    lineHeight: 1.6
};

export const progressSummaryCard = {
    padding: '18px',
    background: '#FFFFFF',
    borderRadius: '18px',
    border: '1px solid #DDE3EA',
    boxShadow: '0 2px 10px rgba(15, 23, 42, 0.03)',
    marginBottom: '18px'
};

export const progressSummaryTop = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: '16px',
    marginBottom: '18px'
};

export const progressSummaryLabel = {
    fontSize: '13px',
    color: '#6B7280',
    fontWeight: 700
};

export const progressSummaryValue = {
    fontSize: '28px',
    lineHeight: 1,
    fontWeight: 900,
    color: '#2563EB'
};

export const progressTrack = {
    height: '10px',
    backgroundColor: '#E5EAF1',
    borderRadius: '999px',
    overflow: 'hidden'
};

export const progressFill = (percent) => ({
    width: `${percent}%`,
    height: '100%',
    backgroundColor: '#2563EB',
    borderRadius: '999px',
    transition: 'width 0.3s ease'
});

export const progressMetaGrid = {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
    gap: '10px',
    marginTop: '16px'
};

export const checklistList = {
    display: 'grid',
    gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
    gap: '12px'
};

export const checklistRow = (isCompleted) => ({
    display: 'grid',
    gridTemplateColumns: 'auto minmax(0, 1fr)',
    gridTemplateRows: 'auto auto',
    alignItems: 'flex-start',
    gap: '12px',
    padding: '16px',
    background: '#FFFFFF',
    border: `1px solid ${isCompleted ? '#DDE3EA' : '#DDE3EA'}`,
    borderRadius: '16px',
    boxShadow: '0 2px 10px rgba(15, 23, 42, 0.03)',
    opacity: isCompleted ? 0.72 : 1,
    minHeight: '126px'
});

export const checkboxBox = (isCompleted) => ({
    width: '22px',
    height: '22px',
    borderRadius: '7px',
    border: `2px solid ${isCompleted ? '#16A34A' : '#CBD5E1'}`,
    background: isCompleted ? '#16A34A' : '#FFFFFF',
    color: '#FFFFFF',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '14px',
    fontWeight: 900,
    flexShrink: 0
});

export const checklistRowTitle = (isCompleted) => ({
    fontWeight: 700,
    fontSize: '14px',
    color: isCompleted ? '#6B7280' : '#111827',
    textDecoration: isCompleted ? 'line-through' : 'none',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap'
});

export const checklistRowMeta = {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    marginTop: '4px',
    fontSize: '12px',
    color: '#6B7280'
};

export const checklistRowAction = {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    justifyContent: 'flex-end',
    gridColumn: '1 / -1',
    marginTop: '2px'
};

export const progressMetaItem = {
    padding: '12px',
    background: 'rgba(255, 255, 255, 0.74)',
    border: '1px solid rgba(221, 227, 234, 0.8)',
    borderRadius: '16px'
};

export const progressMetaLabel = {
    marginBottom: '4px',
    fontSize: '12px',
    color: '#6B7280'
};

export const progressMetaValue = {
    fontSize: '18px',
    fontWeight: 800,
    color: '#111827'
};

export const checklistSectionHeader = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: '12px',
    margin: '22px 0 12px'
};

export const checklistSectionTitle = {
    margin: 0,
    fontSize: '17px',
    fontWeight: 800,
    color: '#111827'
};

export const checklistSectionCount = {
    padding: '4px 10px',
    borderRadius: '999px',
    background: '#FFFFFF',
    border: '1px solid #DDE3EA',
    color: '#6B7280',
    fontSize: '12px',
    fontWeight: 700
};

// ===== 버튼(공통) =====
export const actionButton = {
    padding: '7px 12px',
    fontSize: '12px',
    borderRadius: '999px',
    border: '1px solid #DDE3EA',
    background: '#FFFFFF',
    color: '#6B7280',
    cursor: 'pointer',
    transition: 'all 0.2s ease'
};

export const actionButtonPrimary = {
    ...actionButton,
    border: '1px solid #2563EB',
    color: '#2563EB',
    background: '#EEF2FF'
};

export const actionButtonSuccess = {
    ...actionButton,
    border: '1px solid #16A34A',
    color: '#ffffff',
    background: '#16A34A'
};

export const actionButtonCompleted = {
    ...actionButton,
    border: '1px solid #198754',
    color: '#198754',
    background: '#e9f7ef'
};

export const actionButtonPending = {
    ...actionButton,
    border: '1px solid #DDE3EA',
    color: '#6B7280',
    background: '#FFFFFF'
};

// ===== Preview (MyRoadmap에서 쓰는 카드) =====
export const previewWrapper = {
    display: 'flex',
    justifyContent: 'flex-end',
    marginBottom: '20px'
};

export const previewCard = {
    width: '420px',
    padding: '16px',
    background: '#ffffff',
    borderRadius: '18px',
    border: '1px solid #DDE3EA',
    boxShadow: '0 2px 10px rgba(15, 23, 42, 0.03)',
    cursor: 'pointer'
};

export const previewHeader = {
    fontWeight: 700,
    color: '#111827'
};

export const previewHeaderRow = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: '12px'
};

export const previewTextArea = {
    minWidth: 0,
    flex: 1
};

export const previewSubText = {
    fontSize: '13px',
    color: '#6B7280',
    marginTop: '4px',
    whiteSpace: 'normal'
};

export const previewLink = {
    fontSize: '13px',
    whiteSpace: 'nowrap',
    flexShrink: 0
};

export const previewList = {
    fontSize: '13px',
    color: '#6B7280',
    marginTop: '12px',
    lineHeight: '1.6'
};

// ===== Checklist (전체 페이지) =====
export const checklistTitle = {
    fontWeight: 600,
    fontSize: '14px',
    color: '#111827'
};

export const checklistCategory = {
    fontSize: '12px',
    color: '#6B7280',
    marginTop: '3px'
};

export const checklistGrid = {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
    gap: '14px'
};

export const checklistItem = {
    padding: '18px',
    background: '#ffffff',
    borderRadius: '18px',
    border: '1px solid #DDE3EA',
    boxShadow: '0 2px 10px rgba(15, 23, 42, 0.03)',
    minHeight: '148px',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between'
};

export const checklistItemCompleted = {
    ...checklistItem,
    opacity: 0.82,
    background: '#FFFFFF',
    cursor: 'default'
};

export const checklistItemMandatory = {
    ...checklistItem,
    background: '#FFFBEB',
    border: '1px solid #FDE68A',
    boxShadow: '0 4px 12px rgba(245, 158, 11, 0.08)'
};

export const mandatoryBadge = {
    fontSize: '11px',
    padding: '4px 8px',
    borderRadius: '999px',
    background: '#FEF3C7',
    color: '#b45309',
    fontWeight: 800
};

export const optionalBadge = {
    ...mandatoryBadge,
    background: '#F4F7FB',
    color: '#6B7280'
};

export const checkTodoIcon = {
    width: '10px',
    height: '10px',
    borderRadius: '50%',
    background: '#adb5bd',
    display: 'inline-block',
    marginRight: '10px',
    flexShrink: 0
};

export const checkDoneIcon = {
    ...checkTodoIcon,
    background: '#16A34A'
};

export const checklistActionRow = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: '10px',
    marginTop: '18px'
};

export const checklistItemTop = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: '12px',
    marginBottom: '12px'
};

export const checklistTitleWrap = {
    display: 'flex',
    alignItems: 'flex-start',
    minWidth: 0
};

export const statusPill = (isCompleted) => ({
    padding: '5px 9px',
    borderRadius: '999px',
    background: isCompleted ? '#ECFDF5' : '#F4F7FB',
    border: `1px solid ${isCompleted ? '#BBF7D0' : '#DDE3EA'}`,
    color: isCompleted ? '#16A34A' : '#6B7280',
    fontSize: '11px',
    fontWeight: 800,
    whiteSpace: 'nowrap'
});

export const emptyChecklistCard = {
    padding: '40px 20px',
    textAlign: 'center',
    background: '#FFFFFF',
    border: '1px solid #DDE3EA',
    borderRadius: '18px',
    color: '#6B7280',
    boxShadow: '0 2px 10px rgba(15, 23, 42, 0.03)'
};
