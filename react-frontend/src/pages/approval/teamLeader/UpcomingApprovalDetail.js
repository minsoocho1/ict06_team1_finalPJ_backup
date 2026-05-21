import React from 'react';

import ApprovalsDetail from '../ApprovalsDetail';

// [전자결재] 결재 예정 문서 상세 페이지
// 예정 문서는 아직 내 승인 차례가 아니므로 문서 내용 확인 전용으로 사용합니다.
const UpcomingApprovalDetail = () => (
  <ApprovalsDetail
    pageTitle="결재 예정 문서 상세"
    allowCancel={false}
    showPrint
  />
);

export default UpcomingApprovalDetail;
