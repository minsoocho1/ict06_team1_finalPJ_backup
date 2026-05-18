import React, { useEffect, useState } from 'react';

import { request } from 'src/helpers/axios_helper';

// CoreUI 컴포넌트
import {
  CButton,
  CCard,
  CCardBody,
  CCardHeader,
  CRow,
  CCol,
  CProgress,
  CTable,
  CTableHead,
  CTableRow,
  CTableHeaderCell,
  CTableBody,
  CTableDataCell,
  CBadge,
} from '@coreui/react';

// CoreUI 아이콘 컴포넌트
import CIcon from '@coreui/icons-react';

// 사용할 아이콘 목록
import {
  cilMoodGood,
  cilChartPie,
  cilUserFollow,
} from '@coreui/icons';

import { useNavigate, useOutletContext } from 'react-router-dom';
import { PATH } from 'src/constants/path';

// 시연용 이미지 파일
import refImage from 'src/assets/images/first_demo/[Attendance]holidayStatus.png';

// 1차 시연용 스타일
import { containerStyle } from 'src/styles/js/demoPageStyle';

// SQL 코드 하이라이터
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { coy } from 'react-syntax-highlighter/dist/esm/styles/prism';

  // [근태관리] 연차 현황 페이지
  const HolidaysStatus = () => {
    // DefaultLayout.js의 Outlet에서 보낸 userInfo 데이터 받기
    const [userInfo] = useOutletContext();

    // 페이지 이동용 함수
    // 연차 현황 페이지에서 근태 메인으로 돌아갈 때 사용
    const navigate = useNavigate();

    // ==============================
    // 1. 연차 요약 데이터
    // ==============================
    // 로그인한 사용자의 사번
    // userInfo가 아직 없을 수 있으므로 테스트 사번을 기본값으로 사용
    const empNo = userInfo?.empNo || '20209999';

    // 연차 요약 데이터 상태
    const [leaveSummary, setLeaveSummary] = useState({
      totalDays: 0,
      usedDays: 0,
      remainDays: 0,
    });

    // ==============================
    // 2. 연차 사용률 계산
    // ==============================
    // 공식: 사용 연차 / 총 연차 * 100
    // 총 연차가 0이면 나누기 오류가 생기므로 0 처리
    const usedRate =
    leaveSummary.totalDays > 0
        ? Math.round((leaveSummary.usedDays / leaveSummary.totalDays) * 100)
        : 0;

    // ==============================
    // 3. 연차 사용 내역
    // ==============================
    const [leaveHistory, setLeaveHistory] = useState([]);

    // 로딩 상태
    // API 데이터를 불러오는 동안 true
    const [loading, setLoading] = useState(true);

    // 에러 메시지 상태
    const [errorMessage, setErrorMessage] = useState('');

    // 연차 데이터 조회 함수
    const fetchLeaveData = async () => {
      // 1. 요청 시작 처리
      // API 데이터를 불러오는 동안 로딩 화면을 보여주기 위해 true로 변경
      setLoading(true);

      // 이전에 에러가 있었다면 새 요청 전 에러 메시지 초기화
      setErrorMessage('');

      try {
        // 2. 연차 요약 API 호출
        // 총 연차 / 사용 연차 / 잔여 연차 조회
        const summaryParams = {
          empNo: empNo,
        };

        const summaryRes = await request('GET', '/leave/summary', summaryParams);

        // 3. 연차 사용 내역 API 호출
        // 연차/반차 사용 이력 조회
        const historyParams = {
          empNo: empNo,
        };

        const historyRes = await request('GET', '/leave/history', historyParams);

        // 4. 응답 데이터를 화면 상태에 저장
        // summaryRes.data 예시:
        // { totalDays: 15, usedDays: 5.5, remainDays: 9.5 }
        setLeaveSummary(summaryRes.data);

        // historyRes.data 예시:
        // [{ startDate, endDate, typeName, leaveDays, status }, ...]
        setLeaveHistory(historyRes.data);

        console.log('연차 요약:', summaryRes.data);
        console.log('연차 사용 내역:', historyRes.data);

      } catch (error) {
        // 5. 에러 처리
        // alert 대신 화면에 에러 메시지를 보여주기 위해 상태에 저장
        console.error(error);

        setErrorMessage('연차 데이터를 불러오는 중 오류가 발생했습니다.');

      } finally {
        // 6. 요청 종료 처리
        // 성공/실패와 관계없이 로딩 상태 종료
        setLoading(false);
      }
    };

    // 화면이 처음 열릴 때 연차 데이터 조회
    useEffect(() => {
    fetchLeaveData();
    }, [empNo]);

  // ==============================
  // 4. 발표/시연용 SQL
  // ==============================
  // 기존 vacation_info는 현재 ERD에 없는 테이블이라
  // 우리 ERD 기준인 leave_occurrence, leave_request, leave_type 기준으로 변경
  const sqlQuery = `
-- 사원의 연차 발생/사용/잔여 현황 조회
SELECT
    emp_no,
    target_year,
    SUM(occur_days) AS total_days,
    SUM(used_days) AS used_days,
    SUM(remain_days) AS remain_days
FROM leave_occurrence
WHERE emp_no = #{empNo}
  AND target_year = #{targetYear}
GROUP BY emp_no, target_year;

-- 사원의 연차 사용 내역 조회
SELECT
    lr.start_date,
    lr.end_date,
    lt.type_name,
    lr.leave_days,
    lr.status
FROM leave_request lr
JOIN leave_type lt
  ON lr.leave_type_id = lt.type_id
WHERE lr.emp_no = #{empNo}
ORDER BY lr.start_date DESC;
  `;

  // ==============================
  // 5. 상태값을 한글 배지로 변환
  // ==============================
  // DB/API에서는 APPROVED, PENDING 같은 코드값을 사용하고
  // 화면에서는 승인, 대기처럼 한글로 보여주기 위한 함수
  const getStatusBadge = (status) => {
    switch (status) {
      case 'APPROVED':
        return <CBadge color="success">승인</CBadge>;
      case 'PENDING':
        return <CBadge color="warning">대기</CBadge>;
      case 'REJECTED':
        return <CBadge color="danger">반려</CBadge>;
      case 'CANCELED':
        return <CBadge color="secondary">취소</CBadge>;
      default:
        return <CBadge color="dark">알 수 없음</CBadge>;
    }
  };

  // ==============================
  // 6. 연차 사용률에 따른 Progress 색상
  // ==============================
  // 사용률이 높아질수록 경고 색상으로 변경
  const getProgressColor = () => {
    if (usedRate >= 90) return 'danger';
    if (usedRate >= 70) return 'warning';
    return 'success';
  };

  return (
    <div style={containerStyle}>
      {/* ============================== */}
      {/* 로딩 중 화면 */}
      {/* ============================== */}
      {/* loading이 true일 때만 표시 */}
      {loading && (
        <CCard className="mb-4">
          <CCardBody className="text-center py-5">
            <h5>연차 데이터를 불러오는 중입니다...</h5>
            <div style={{ fontSize: '14px', color: '#6c757d' }}>
              잠시만 기다려주세요.
            </div>
          </CCardBody>
        </CCard>
      )}

      {/* ============================== */}
      {/* 에러 메시지 화면 */}
      {/* ============================== */}
      {/* errorMessage가 있을 때만 표시 */}
      {errorMessage && (
        <CCard className="mb-4 border-danger">
          <CCardBody className="text-danger text-center">
            {errorMessage}
          </CCardBody>
        </CCard>
      )}

      {/* ============================== */}
      {/* 실제 구현 화면 영역 */}
      {/* ============================== */}
      {!loading && !errorMessage && (
      <>

      {/* 상단 제목 + 근태 메인 이동 버튼 */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h2 className="mb-1">{userInfo?.name}님의 연차 현황</h2>
            <div style={{ fontSize: '14px', color: '#6c757d' }}>
            총 연차, 사용 연차, 잔여 연차를 한눈에 확인합니다.
            </div>
        </div>

        <CButton
            color="primary"
            variant="outline"
            onClick={() => navigate(PATH.ATTENDANCE.ROOT)}
        >
            ← 근태 메인으로
        </CButton>
      </div>

      {/* 1. 총 연차 / 사용 연차 / 잔여 연차 카드 */}
      {/* hover 효과와 아이콘을 추가해 대시보드 느낌 강화 */}
      <CRow className="mb-4">

        {/* 총 연차 카드 */}
        <CCol md={4}>
          <CCard
            className="border-0 shadow-sm h-100"
            style={{
              transition: 'all 0.2s ease',
              cursor: 'pointer',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'translateY(-5px)';
              e.currentTarget.style.boxShadow =
                '0 10px 20px rgba(0,0,0,0.12)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'translateY(0px)';
              e.currentTarget.style.boxShadow =
                '0 .125rem .25rem rgba(0,0,0,.075)';
            }}
          >
            <CCardBody className="d-flex justify-content-between align-items-center">

              {/* 왼쪽 텍스트 영역 */}
              <div>
                <div className="text-medium-emphasis mb-2">
                  총 연차
                </div>

                <h2 className="fw-bold mb-0">
                  {leaveSummary.totalDays}일
                </h2>
              </div>

              {/* 오른쪽 아이콘 */}
              <div
                style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: '50%',
                  backgroundColor: '#e9f3ff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <CIcon
                  icon={cilChartPie}
                  size="xl"
                  style={{ color: '#0d6efd' }}
                />
              </div>
            </CCardBody>
          </CCard>
        </CCol>

        {/* 사용 연차 카드 */}
        <CCol md={4}>
          <CCard
            className="border-0 shadow-sm h-100"
            style={{
              transition: 'all 0.2s ease',
              cursor: 'pointer',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'translateY(-5px)';
              e.currentTarget.style.boxShadow =
                '0 10px 20px rgba(0,0,0,0.12)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'translateY(0px)';
              e.currentTarget.style.boxShadow =
                '0 .125rem .25rem rgba(0,0,0,.075)';
            }}
          >
            <CCardBody className="d-flex justify-content-between align-items-center">

              {/* 왼쪽 텍스트 영역 */}
              <div>
                <div className="text-medium-emphasis mb-2">
                  사용 연차
                </div>

                <h2 className="fw-bold mb-0">
                  {leaveSummary.usedDays}일
                </h2>
              </div>

              {/* 오른쪽 아이콘 */}
              <div
                style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: '50%',
                  backgroundColor: '#fff3cd',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <CIcon
                  icon={cilUserFollow}
                  size="xl"
                  style={{ color: '#ff9800' }}
                />
              </div>
            </CCardBody>
          </CCard>
        </CCol>

        {/* 잔여 연차 카드 */}
        {/* 가장 중요한 정보라 색상 강조 */}
        <CCol md={4}>
          <CCard
            className="border-0 shadow-sm h-100"
            style={{
              transition: 'all 0.2s ease',
              cursor: 'pointer',
              background:
                'linear-gradient(135deg, #321fdb 0%, #4f46e5 100%)',
              color: 'white',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'translateY(-5px)';
              e.currentTarget.style.boxShadow =
                '0 10px 20px rgba(50,31,219,0.35)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'translateY(0px)';
              e.currentTarget.style.boxShadow =
                '0 .125rem .25rem rgba(0,0,0,.075)';
            }}
          >
            <CCardBody className="d-flex justify-content-between align-items-center">

              {/* 왼쪽 텍스트 영역 */}
              <div>
                <div
                  className="mb-2"
                  style={{ opacity: 0.8 }}
                >
                  잔여 연차
                </div>

                <h2 className="fw-bold mb-0">
                  {leaveSummary.remainDays}일
                </h2>
              </div>

              {/* 오른쪽 아이콘 */}
              <div
                style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: '50%',
                  backgroundColor: 'rgba(255,255,255,0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <CIcon
                  icon={cilMoodGood}
                  size="xl"
                  style={{ color: 'white' }}
                />
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* 2. 연차 사용률 Progress Bar */}
      <CCard className="mb-4">
        <CCardHeader>
          <strong>연차 사용률</strong>
        </CCardHeader>

        <CCardBody>
          {/* 사용량 텍스트와 퍼센트 표시 */}
          <div className="d-flex justify-content-between mb-2">
            <span>
              사용 {leaveSummary.usedDays}일 / 총 {leaveSummary.totalDays}일
            </span>
            <strong>{usedRate}%</strong>
          </div>

          {/* 실제 Progress Bar */}
          <CProgress
            value={usedRate}
            color={getProgressColor()}
            height={20}
          />

          {/* 잔여 연차 안내 문구 */}
          <div className="text-medium-emphasis mt-2">
            잔여 연차는 {leaveSummary.remainDays}일입니다.
          </div>
        </CCardBody>
      </CCard>

      {/* 3. 연차 사용 내역 테이블 */}
      <CCard className="mb-4">
        <CCardHeader>
          <strong>연차 사용 내역</strong>
        </CCardHeader>

        <CCardBody>
          <CTable hover responsive align="middle">
            <CTableHead>
              <CTableRow>
                <CTableHeaderCell>시작일</CTableHeaderCell>
                <CTableHeaderCell>종료일</CTableHeaderCell>
                <CTableHeaderCell>휴가 종류</CTableHeaderCell>
                <CTableHeaderCell>사용 일수</CTableHeaderCell>
                <CTableHeaderCell>상태</CTableHeaderCell>
              </CTableRow>
            </CTableHead>

            <CTableBody>
              {/* 연차 사용 내역이 없을 때 안내 문구 표시 */}
              {leaveHistory.length === 0 ? (
                <CTableRow>
                  <CTableDataCell
                    colSpan={5}
                    className="text-center text-medium-emphasis py-4"
                  >
                    연차 사용 내역이 없습니다.
                  </CTableDataCell>
                </CTableRow>
              ) : (
                leaveHistory.map((leave, index) => (
                  <CTableRow key={index}>
                    <CTableDataCell>{leave.startDate}</CTableDataCell>
                    <CTableDataCell>{leave.endDate}</CTableDataCell>
                    <CTableDataCell>{leave.typeName}</CTableDataCell>
                    <CTableDataCell>{leave.leaveDays}일</CTableDataCell>
                    <CTableDataCell>{getStatusBadge(leave.status)}</CTableDataCell>
                  </CTableRow>
                ))
              )}
            </CTableBody>
          </CTable>
        </CCardBody>
      </CCard>

      {/* ============================== */}
      {/* 기존 시연용 화면 영역 */}
      {/* 실제 UI 아래로 밀려나게 배치 */}
      {/* ============================== */}

      <CCard className="mb-4">
        <CCardHeader>
          <strong>시연 화면 및 관련 SQL쿼리</strong>
        </CCardHeader>

        <CCardBody className="p-0 d-flex flex-column">
          {/* 레퍼런스 이미지 영역 */}
          <div
            className="text-center"
            style={{
              backgroundColor: '#f4f4f4',
              borderTop: '1px solid #eee',
            }}
          >
            <img
              src={refImage}
              alt="연차 현황 시연 화면"
              style={{
                width: '100%',
                height: 'auto',
                display: 'block',
              }}
            />
          </div>

          {/* SQL 쿼리 영역 */}
          <div className="text-start mt-4">
            <h5
              className="mb-3"
              style={{ fontWeight: 'bold', color: '#4f5d73' }}
            >
              <span
                style={{
                  borderLeft: '4px solid #321fdb',
                  paddingLeft: '10px',
                }}
              >
                관련 SQL 쿼리
              </span>
            </h5>

            <SyntaxHighlighter language="sql" style={coy}>
              {sqlQuery}
            </SyntaxHighlighter>
          </div>
        </CCardBody>
      </CCard>
        </>
      )}  
    </div>
  );
};

export default HolidaysStatus;