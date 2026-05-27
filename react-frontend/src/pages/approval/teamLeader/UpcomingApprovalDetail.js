import React from 'react';
import { useSearchParams } from 'react-router-dom';

import ApprovalsDetail from '../ApprovalsDetail';

// [전자결재] 결재 예정 문서 상세 페이지
// 결재 대기 문서함의 '처리 완료' 탭에서도 같은 읽기 전용 상세 화면을 재사용합니다.
const UpcomingApprovalDetail = () => {
  const [searchParams] = useSearchParams();
  const isProcessedDocument = searchParams.get('source') === 'processed';

  return (
    <ApprovalsDetail
      pageTitle={isProcessedDocument ? '처리 완료 문서 상세' : '결재 예정 문서 상세'}
      allowCancel={false}
      showPrint
    />
  );
};

export default UpcomingApprovalDetail;
