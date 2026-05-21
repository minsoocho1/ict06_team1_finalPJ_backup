import React from 'react';
import { PATH } from "../constants/path";
import ApprovalLayout from "../pages/approval/components/ApprovalLayout";

// lazy loading 적용
const Approval = React.lazy(() => import( "../pages/approval/Approval"));
const ApprovalSelectForm = React.lazy(() => import( "../pages/approval/newApproval/ApprovalSelectForm"));
const ApprovalWriteNew = React.lazy(() => import( "../pages/approval/newApproval/ApprovalWriteNew"));
const ApprovalSetLine = React.lazy(() => import( "../pages/approval/newApproval/ApprovalSetLine"));
const TmpApprovals = React.lazy(() => import( "../pages/approval/TmpApprovals"));
const TmpApprovalDetail = React.lazy(() => import( "../pages/approval/TmpApprovalDetail"));
const PersonalApprovals = React.lazy(() => import( "../pages/approval/PersonalApprovals"));
const ApprovalsDetail = React.lazy(() => import( "../pages/approval/ApprovalsDetail"));
const PendingApprovals = React.lazy(() => import( "../pages/approval/teamLeader/PendingApprovals"));
const PendingApprovalDetail = React.lazy(() => import( "../pages/approval/teamLeader/PendingApprovalDetail"));
const UpcomingApprovals = React.lazy(() => import( "../pages/approval/teamLeader/UpcomingApprovals"));
const UpcomingApprovalDetail = React.lazy(() => import( "../pages/approval/teamLeader/UpcomingApprovalDetail"));

// 전자결재 전용 보조 사이드바를 모든 전자결재 화면에 공통 적용합니다.
const withApprovalLayout = (userInfo, element) => (
  <ApprovalLayout userInfo={userInfo}>{element}</ApprovalLayout>
);

// DefaultLayout 내부의 AppContent 자리에 렌더링될 페이지만 명시
export const approvalRoutes = (userInfo) => [
  { path: PATH.APPROVAL.ROOT, element: withApprovalLayout(userInfo, <Approval userInfo={userInfo} />) }, // 전자결재 메인 페이지
  { path: PATH.APPROVAL.NEW_SELECT, element: withApprovalLayout(userInfo, <ApprovalSelectForm userInfo={userInfo} />) }, //  새 결재 진행 - 결재 서식 선택 페이지
  { path: PATH.APPROVAL.NEW_WRITE, element: withApprovalLayout(userInfo, <ApprovalWriteNew userInfo={userInfo} />) }, // 새 결재 진행 - 결재 내용 작성 페이지
  { path: PATH.APPROVAL.NEW_SETLINE, element: withApprovalLayout(userInfo, <ApprovalSetLine userInfo={userInfo} />) }, // 새 결재 진행 - 결재선 설정 페이지

  { path: PATH.APPROVAL.TMP, element: withApprovalLayout(userInfo, <TmpApprovals userInfo={userInfo} />) }, // 임시저장함 페이지
  { path: PATH.APPROVAL.TMP_DETAIL, element: withApprovalLayout(userInfo, <TmpApprovalDetail userInfo={userInfo} />) }, // 임시저장 문서 상세 페이지
  { path: PATH.APPROVAL.PERSONAL, element: withApprovalLayout(userInfo, <PersonalApprovals userInfo={userInfo} />) }, // 개인 문서함 페이지
  { path: PATH.APPROVAL.PERSONAL_DETAIL, element: withApprovalLayout(userInfo, <ApprovalsDetail userInfo={userInfo} />) }, // 개인 문서 상세 페이지

  { path: PATH.APPROVAL.PENDING, element: withApprovalLayout(userInfo, <PendingApprovals userInfo={userInfo} />) }, // 결재 대기 문서함 페이지
  { path: PATH.APPROVAL.PENDING_DETAIL, element: withApprovalLayout(userInfo, <PendingApprovalDetail userInfo={userInfo} />) }, // 결재 대기 문서 상세 페이지
  { path: PATH.APPROVAL.UPCOMING, element: withApprovalLayout(userInfo, <UpcomingApprovals userInfo={userInfo} />) }, // 결재 예정 문서함 페이지
  { path: PATH.APPROVAL.UPCOMING_DETAIL, element: withApprovalLayout(userInfo, <UpcomingApprovalDetail userInfo={userInfo} />) }, // 결재 예정 문서 상세 페이지
];
