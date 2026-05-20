/*
 * 전자결재 화면에서 결재자 전용 메뉴를 보여줄지 판단하는 공통 함수입니다.
 * 실제 승인/반려 권한은 백엔드에서 다시 검증하므로, 이 함수는 화면 노출 제어에만 사용합니다.
 */
export const canViewApproverMenus = (userInfo) => {
  const rawPositionId = userInfo?.position?.positionId
    ?? userInfo?.positionId
    ?? userInfo?.position_id;
  const positionId = Number(rawPositionId);

  return !Number.isNaN(positionId) && positionId > 1;
};
