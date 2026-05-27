// 4) path.js에서 만든 상수를 불러와서 실제 React 컴포넌트와 매핑 (로직)
// path.js -> routes/대분류 별 파일 -> routes/index.js -> App.js(여기!)

import { Routes, Route, Navigate, useRoutes } from 'react-router-dom';
import React, { Suspense } from 'react';

//1. 기존 페이지 컴포넌트
//[그룹 A] 독립 페이지(초기 진입 시 빠른 로딩을 위해 일반 import)
import LoginPage from './pages/auth/LoginPage';
import WelcomePage from './pages/auth/WelcomePage';
import { UserProvider, useUser, } from './api/UserContext';
import NotificationListener from './pages/auth/NotificationListener';
import { PATH } from './constants/path';
import { getAppRoutes } from './routes/index';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

// 레이아웃 및 하위 서비스 페이지(Lazy Loading 적용)
const UserAppLayout = React.lazy(() => import("./layout/UserAppLayout"));

function AppContent() {

  const { userInfo, setUserInfo, userLoading } = useUser();
  const appRoutes = getAppRoutes(userInfo, setUserInfo);

  if(userLoading) {
    return <div className='pt-3 text-center'>Loading...</div>
  }

  return (
    <NotificationListener>
      {/* Lazy Laoding 적용: 페이지 컴포넌트가 로드될 때까지 로딩 메시지 표시 */}
      <Suspense fallback={<div className="pt-3 text-center">Loading...</div>}>
        <Routes>
          {/* 1. 인증 필요 없는 공용 경로 */}
          <Route path={PATH.ROOT} element={<Navigate to={PATH.AUTH.LOGIN} />} />
          {/* 인증 */}
          <Route path={PATH.AUTH.LOGIN} element={<LoginPage />} />
          <Route path={PATH.AUTH.WELCOME} element={<WelcomePage />} />
          {/* 3. [그룹 B] 사이드바/헤더 있는 메인 서비스 레이아웃 */}
          {/* 메인 : routes/index.js에 정리된 모든 경로 지원*/}
          {/* 앞으로 생성할 페이지는 routes/의 각 대분류 별 파일에 Route만 추가하면 사이드바가 자동으로 적용됨 */}
          <Route path={PATH.ROOT} element={<UserAppLayout userInfo={userInfo} />}>
            {appRoutes.map((route, idx) => (
              <Route
                key={idx}
                path={route.path?.startsWith('/') ? route.path.slice(1) : route.path}
                element={route.element}
              />
            ))}
          </Route>
          {/* 3. 예외 처리: 없는 페이지 접근 시 로그인 화면으로(404 예외 처리) */}
          <Route path="*" element={<Navigate to={PATH.AUTH.LOGIN} />} />
        </Routes>

        {/* 알림 설정용 토스트 */}
        <ToastContainer
          position='top-center'
          autoClose={3000}
          hideProgressBar
          closeOnClick
          pauseOnHover={false}
        />
      </Suspense>
    </NotificationListener>
  );
}

function App() {
  return (
    <UserProvider>
      <AppContent />
    </UserProvider>
  );
}

export default App;


// 경로 리팩토링 전 --------------------------------------------------------------------
// import { Routes, Route, Navigate } from 'react-router-dom';

// import React, { Suspense, useState } from 'react';
// import AttendanceManagement from './pages/attendance/AttendanceManagement';
// import AttendanceStatistics from './pages/attendance/AttendanceStatistics';
// import CommuteProcessing from './pages/attendance/CommuteProcessing';


// //1. 기존 페이지 컴포넌트
// //[그룹 A] 독립 페이지(초기 진입 시 빠른 로딩을 위해 일반 import)
// import LoginPage from './pages/auth/LoginPage';
// import WelcomePage from './pages/auth/WelcomePage';
// import Evaluation2 from './pages/evaluation/Evaluation2';
// import HolidaysStatus from './pages/attendance/HolidaysStatus';
// import Calendar from './pages/calendar/Calendar';
// import Approval from './pages/approval/Approval';
// import ApprovalSelectForm from './pages/approval/newApproval/ApprovalSelectForm';
// import ApprovalSetLine from './pages/approval/newApproval/ApprovalSetLine';
// import ApprovalWriteNew from './pages/approval/newApproval/ApprovalWriteNew';
// import TmpApprovals from './pages/approval/TmpApprovals';
// import PersonalApprovals from './pages/approval/PersonalApprovals';
// import ApprovalsDetail from './pages/approval/ApprovalsDetail';
// import PendingApprovals from './pages/approval/teamLeader/PendingApprovals';
// import UpcomingApprovals from './pages/approval/teamLeader/UpcomingApprovals';
// import PendingApprovalDetail from './pages/approval/teamLeader/PendingApprovalDetail';
// import Employee from './pages/employee/Employee';
// import EmployeeDetail from './pages/employee/EmployeeDetail';
// import Payroll from './pages/payroll/Payroll';
// import PayrollIssue from './pages/payroll/PayrollIssue';
// import AIPortalMain from './pages/AIPortalMain';
// import AiSecretary from './pages/aiSecretary/AiSecretary';
// import Chatbot from './pages/chatbot/Chatbot';
// import AiSecretaryQuickStart from './pages/aiSecretary/AiSecretaryQuickStart';
// import AiSecretaryAnswerToChat from './pages/aiSecretary/AiSecretaryAnswerToChat';
// import ChatbotSelectMenu from './pages/chatbot/ChatbotSelectMenu';
// import ChatbotWriteMessage from './pages/chatbot/ChatbotWriteMessage';
// import ChatbotSelectMenuAction from './pages/chatbot/ChatbotSelectMenuAction';
// import RealtimeAlert from './components/RealtimeAlert';
// import ChatbotMain from './pages/chatbot/ChatbotMain';
// import ChatbotMainSelectMenu from './pages/chatbot/ChatbotMainSelectMenu';
// import ChatbotMainMessage from './pages/chatbot/ChatbotMainMessage';
// import CalendarSimpleAdd from './pages/calendar/CalendarSimpleAdd';
// import CalendarDetailAdd from './pages/calendar/CalendarDetailAdd';
// import CalendarDetail from './pages/calendar/CalendarDetail';

// //[그룹 B] 레이아웃 및 하위 서비스 페이지(Lazy Loading 적용)
// const DefaultLayout = React.lazy(() => import('./layout/DefaultLayout'));
// const UserHome = React.lazy(() => import('./pages/auth/UserHome'));
// const MyRoadmap = React.lazy(() => import('./pages/onboarding/MyRoadmap'));
// const Evaluation = React.lazy(() => import('./pages/evaluation/Evaluation'));

// // 프로젝트 메인 라우터(경로 연결)
// function App() {
//   //로그인한 사용자 정보 전역으로 관리(예시로 useState 사용, 추후 Context API나 Redux로 확장 가능)
//   //const [userInfo, setUserInfo] = useState(null);
//   //시연용 계정 데이터 임시 설정
//   const [userInfo, setUserInfo] = useState({
//     emp_no: '20209999',
//     name: '홍길동'
//   });

//   return (
//     //Lazy Laoding 적용: 페이지 컴포넌트가 로드될 때까지 로딩 메시지 표시
//     <Suspense fallback={<div className="pt-3 text-center">Loading...</div>}>
//       <Routes>
//         {/* 1. 초기 접속 시 로그인 페이지로 리다이렉트 */}
//         <Route path="/" element={<Navigate to="/auth/login" replace />} />

//         {/* 2. [그룹 A] 레이아웃 없는 독립 페이지(로그인, welcome) */}
//         <Route path="/auth/login" element={<LoginPage setUserInfo={setUserInfo} />} />
//         <Route path="/auth/welcome" element={<WelcomePage userInfo={userInfo} setUserInfo={setUserInfo} />} />

//         {/* 3. [그룹 B] 사이드바/헤더 있는 메인 서비스 레이아웃 */}
//         {/* 앞으로 생성할 페이지도 여기 Route만 추가하면 사이드바가 자동으로 적용됨 */}
//         {/* userInfo가 있을 때만 접근 가능하도록 추후 보호 로직 추가 예정 */}
//         <Route element={<DefaultLayout />}>
//           {/* DefaultLayout 내부의 AppContent 자리에 아래 페이지들이 렌더링됨 */}
//           <Route path="/auth/userhome" element={<UserHome userInfo={userInfo} />} />

//           {/* -------------------------------------------------------------- */}
//           {/* [대분류 : 인사평가 - 온보딩] */}
//           <Route path="/onboarding/myroadmap" element={<MyRoadmap userInfo={userInfo} />} />
//           <Route path="/evaluation/quiz" element={<Evaluation userInfo={userInfo} />} />
//           <Route path="/evaluation/evaluation" element={<Evaluation2 userInfo={userInfo} />} />

//           {/* -------------------------------------------------------------- */}
//           {/* [대분류 : 근태 관리] */}
//           {/* 근태 관리 페이지 */}
//           <Route path="/attendance" element={<AttendanceManagement userInfo={userInfo} />} />

//           {/* 출퇴근 처리 */}
//           <Route path="/attendance/commute" element={<CommuteProcessing userInfo={userInfo} />} />

//           {/* 근태 통계 페이지 */}
//           <Route path="/attendance/stats" element={<AttendanceStatistics userInfo={userInfo} />} />

//           {/* 연차 현황 페이지 */}
//           <Route path="/attendance/holidays" element={<HolidaysStatus userInfo={userInfo} />} />

//           {/* -------------------------------------------------------------- */}
//           {/* [대분류 : 캘린더/}*/}
//           {/* 캘린더 메인 페이지 */}
//           <Route path="/calendar" element={<Calendar userInfo={userInfo} />} />

//           {/* 일정 간단 등록 페이지 */}
//           <Route path="/calendar/simple-add" element={<CalendarSimpleAdd userInfo={userInfo} />} />

//           {/* 상세 등록 / 반복 / 참석자 일정 페이지 */}
//           <Route path="/calendar/detail-add" element={<CalendarDetailAdd userInfo={userInfo} />} />

//           {/* 일정 상세/삭제 */}
//           <Route path="/calendar/detail" element={<CalendarDetail userInfo={userInfo} />} />

//           {/* -------------------------------------------------------------- */}
//           {/* [대분류 : 전자결재/}*/}
//           {/* 전자결재 메인 페이지 */}
//           <Route path="/approval" element={<Approval userInfo={userInfo} />} />

//           {/* 새 결재 진행 - 결재 서식 선택 페이지 */}
//           <Route path="/approval/new/select-form" element={<ApprovalSelectForm userInfo={userInfo} />} />

//           {/* 새 결재 진행 - 결재 내용 작성 페이지 */}
//           <Route path="/approval/new/write" element={<ApprovalWriteNew userInfo={userInfo} />} />

//           {/* 새 결재 진행 - 결재선 설정 페이지 */}
//           <Route path="/approval/new/set-line" element={<ApprovalSetLine userInfo={userInfo} />} />

//           {/* 결재 내용 수정 */}

//           {/* 임시저장함 페이지 */}
//           <Route path="/approval/tmpApprovals" element={<TmpApprovals userInfo={userInfo} />} />

//           {/* 개인 문서함 페이지 */}
//           <Route path="/approval/personalApprovals" element={<PersonalApprovals userInfo={userInfo} />} />

//           {/* 개인 문서 상세 페이지 */}
//           <Route path="/approval/personalApprovals/detail" element={<ApprovalsDetail userInfo={userInfo} />} />

//           {/* 결재 대기 문서함 페이지*/}
//           <Route path="/approval/pendingApprovals" element={<PendingApprovals userInfo={userInfo} />} />

//           {/* 결재 대기 문서 상세 페이지 */}
//           <Route path="/approval/pendingApprovals/detail" element={<PendingApprovalDetail userInfo={userInfo} />} />

//           {/* 결재 예정 문서함 페이지*/}
//           <Route path="/approval/upcomingApprovals" element={<UpcomingApprovals userInfo={userInfo} />} />

//           {/* -------------------------------------------------------------- */}
//           {/* [대분류 : 인사관리]*/}
//           {/* 인사관리 메인 페이지*/}
//           <Route path="/employee" element={<Employee userInfo={userInfo} />} />

//           {/* 인사관리 상세 페이지*/}
//           <Route path="/employee/detail" element={<EmployeeDetail userInfo={userInfo} />} />

//           {/* -------------------------------------------------------------- */}
//           {/* [대분류 : 급여관리]*/}
//           {/* 사원별 급여 확인 페이지*/}
//           <Route path="/payroll" element={<Payroll userInfo={userInfo} />} />

//           {/* 사원별 급여명세서 발급 페이지*/}
//           <Route path="/payroll/issue" element={<PayrollIssue userInfo={userInfo} />} />

//           {/* -------------------------------------------------------------- */}
//           {/* [사내 AI 포털]*/}
//           <Route path="/ai-portal" element={<AIPortalMain userInfo={userInfo} />} />

//           {/* -------------------------------------------------------------- */}
//           {/* [대분류 : AI비서]*/}
//           {/* AI 비서 메인 */}
//           <Route path="/ai-portal/secretary" element={<AiSecretary userInfo={userInfo} />} />

//           {/* AI 비서 빠른 시작 응답 */}
//           <Route path="/ai-portal/secretary/quick-start" element={<AiSecretaryQuickStart userInfo={userInfo} />} />

//           {/* AI 비서 채팅에 응답 */}
//           <Route path="/ai-portal/secretary/answer-to-chat" element={<AiSecretaryAnswerToChat userInfo={userInfo} />} />

//           {/* -------------------------------------------------------------- */}
//           {/* [대분류 : AI챗봇]*/}
//           {/* AI 챗봇 열기 */}
//           <Route path="/ai-portal/chatbot" element={<Chatbot userInfo={userInfo} />} />

//           {/* 화면 하단에서 AI 챗봇 버튼 클릭 후 메인 페이지 */}
//           <Route path="/ai-portal/chatbot/main" element={<ChatbotMain userInfo={userInfo} />} />

//           {/* 챗봇 메인 - 메시지 선택 페이지 */}
//           <Route path="/ai-portal/chatbot/main/select-menu" element={<ChatbotMainSelectMenu userInfo={userInfo} />} />

//           {/* 챗봇 메인 - 메시지 작성 페이지 */}
//           <Route path="/ai-portal/chatbot/main/message" element={<ChatbotMainMessage userInfo={userInfo} />} />

//           {/* AI 챗봇에서 메뉴 선택 */}
//           <Route path="/ai-portal/chatbot/select-menu" element={<ChatbotSelectMenu userInfo={userInfo} />} />

//           {/* AI 챗봇에서 메뉴 선택 결과*/}
//           <Route path="/ai-portal/chatbot/select-menu/result" element={<ChatbotSelectMenuAction userInfo={userInfo} />} />

//           {/* AI 챗봇에서 메시지 작성 페이지 */}
//           <Route path="/ai-portal/chatbot/message" element={<ChatbotWriteMessage userInfo={userInfo} />} />

//           {/* -------------------------------------------------------------- */}
//           {/* [대분류 : 실시간 알림]*/}
//           <Route path="/alert" element={<RealtimeAlert userInfo={userInfo} />} />

//         </Route>

//         {/* 4. 잘못된 경로 접근 시 처리(404 예외 처리) */}
//         <Route path="*" element={<Navigate to="/auth/login" replace />} />
//       </Routes>
//     </Suspense>
//   );
// }

// export default App;
