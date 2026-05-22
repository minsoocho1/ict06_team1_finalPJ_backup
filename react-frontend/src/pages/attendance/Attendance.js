import React, { useEffect, useState } from 'react';

// react-router-dom
// 사용자 정보 + 페이지 이동
import { useNavigate, useOutletContext } from 'react-router-dom';

// CoreUI 컴포넌트
import {
  CButton,
  CCard,
  CCardBody,
  CCardHeader,
  CBadge,
  CProgress,
  CRow,
  CCol,
} from '@coreui/react';

// Recharts 차트 라이브러리
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,

  // PieChart 관련
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';

// PATH 상수
import { PATH } from 'src/constants/path';

// 공통 axios helper
import { request } from 'src/helpers/axios_helper';
import axiosInstance from 'src/api/axiosInstance';

// [결재-근태 연동용]: 근태 화면에서 바로 열어야 하는 전자결재 기본 서식명입니다.
const ABSENCE_FORM_NAME = '부재 일정';
const WORK_RESULT_FORM_NAME = '근무 결과 신청';
const STANDARD_END_HOUR = 18;

const toDateInputValue = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const toTimeInputValue = (date) =>
  `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;

const isBeforeStandardEndTime = (date) => date.getHours() < STANDARD_END_HOUR;

const extractErrorMessage = (error) => {
  const data = error?.response?.data;
  if (typeof data === 'string') {
    return data;
  }

  return data?.message || error?.message || '';
};

const isCompanyLocationError = (error) =>
  extractErrorMessage(error).includes('회사 근처에서만');

// 일반 사원용 근태 메인 화면
const Attendance = () => {
  // DefaultLayout에서 전달받은 로그인 사용자 정보
  const [userInfo] = useOutletContext();

  // 로그인 사용자 사번
  const empNo = userInfo?.empNo;

  // 오늘 출근 상태
  const [todayStatus, setTodayStatus] = useState('출근 전');

  // 출근 시간
  const [checkInTime, setCheckInTime] = useState(null);

  // 퇴근 시간
  const [checkOutTime, setCheckOutTime] = useState(null);

  // 페이지 이동용
  const navigate = useNavigate();

  /**
   * [결재-근태 연동용]: 근태 화면에서 전자결재 작성 화면으로 이동하며 기본값과 잠금 필드를 전달합니다.
   *
   * 근태 화면에서 전자결재 작성 화면으로 바로 이동할 때 사용하는 공통 함수입니다.
   *
   * 전자결재 작성 화면은 selectedForm을 라우터 state로 받아 시작하므로,
   * 먼저 결재 서식 목록에서 필요한 서식을 찾은 뒤 기본 입력값과 잠금 필드를 함께 넘깁니다.
   */
  const openApprovalWriteForm = async (formName, presetFieldValues, lockedFieldIds = []) => {
    const response = await axiosInstance.get(PATH.API.APPROVAL.FORMS);
    const form = (response.data || []).find((item) => item.formName === formName);

    if (!form) {
      alert(`${formName} 서식을 찾을 수 없습니다. 관리자에게 서식 등록 상태를 확인해주세요.`);
      return;
    }

    navigate(PATH.APPROVAL.NEW_WRITE, {
      state: {
        selectedForm: form,
        presetFieldValues,
        lockedFieldIds,
      },
    });
  };

  /**
   * [결재-근태 연동용]: 18시 이전 퇴근이 발생하면 조퇴 처리를 위한 "부재 일정" 서식으로 연결합니다.
   *
   * 18시 이전 퇴근은 조퇴 사유가 필요한 상황이므로 부재 일정 서식으로 이어줍니다.
   * 조퇴 계산 기준은 "퇴근 시각부터 18시까지"이므로 absence_start_time에는 퇴근 시각을 넣습니다.
   */
  const openEarlyLeaveApproval = async (checkoutDate) => {
    const todayValue = toDateInputValue(checkoutDate);
    await openApprovalWriteForm(
      ABSENCE_FORM_NAME,
      {
        absence_type: '조퇴',
        absence_start_date: todayValue,
        absence_end_date: todayValue,
        absence_start_time: toTimeInputValue(checkoutDate),
        absence_end_time: '18:00',
      },
      ['absence_type', 'absence_start_date', 'absence_end_date', 'absence_start_time']
    );
  };

  /**
   * [결재-근태 연동용]: 회사 위치 밖에서 퇴근에 실패하면 외근 소명을 위한 "근무 결과 신청" 서식으로 연결합니다.
   *
   * 회사 위치 밖에서 퇴근 처리에 실패한 경우, 실제 근무 결과를 결재로 소명할 수 있게 연결합니다.
   */
  const openOutsideWorkResultApproval = async (attemptedCheckoutDate) => {
    await openApprovalWriteForm(
      WORK_RESULT_FORM_NAME,
      {
        work_result_date: toDateInputValue(attemptedCheckoutDate),
        actual_start_time: checkInTime || '',
        actual_end_time: toTimeInputValue(attemptedCheckoutDate),
        break_minutes: '60',
        work_plan_type: '외근',
        work_result_reason: '회사 위치 외부에서 퇴근 처리 시도',
      },
      ['work_result_date', 'actual_end_time']
    );
  };

  // 휴가 요약 데이터
  // 근태 메인 상단의 "내 휴가" 카드에 표시할 데이터
  const [leaveSummary, setLeaveSummary] = useState({
    usedDays: 0,
    totalDays: 0,
    remainDays: 0,
  });

  // 근무시간 프로그래스바, 백엔드에서 계산된 workHours 값을 저장한다.
  const [todayWorkHours, setTodayWorkHours] = useState(0);

  // 실시간 근무시간
  // 퇴근 전에는 출근 시간부터 현재 시간까지 계산한 값을 저장
  const [liveWorkHours, setLiveWorkHours] = useState(0);

  // GPS 처리 상태 메시지
  // 출근/퇴근 버튼을 눌렀을 때 위치 확인 상태를 화면에 보여주기 위함
  const [gpsMessage, setGpsMessage] = useState('위치 확인 전');

  // 내 근태 목록 저장 (DB에서 가져올 데이터)
  const [attendanceList, setAttendanceList] = useState([]);

  // 조회 보기 모드: day / week / month
  const [viewMode, setViewMode] = useState('month');

  // 현재 캘린더에서 기준이 되는 날짜
  // 이전/다음 버튼을 누르면 이 날짜가 바뀐다.
  const [currentDate, setCurrentDate] = useState(new Date());

  // 근태 상태 필터
  // ALL / ON_TIME / LATE / EARLY
  const [statusFilter, setStatusFilter] = useState('ALL');

  // 출근/퇴근 처리 결과 메시지
  // 성공/실패 내용을 화면에 보여주기 위한 상태값
  const [attendanceMessage, setAttendanceMessage] = useState('');

  // 출근/퇴근 처리 중 여부 (로딩 상태)
  // 버튼 중복 클릭 방지 + UX 개선
  const [loading, setLoading] = useState(false);

  // 실제 오늘 날짜
  const today = new Date();

  // 화면에서 보고 있는 기준 날짜
  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  // ==============================
  // Date 기반 캘린더 데이터 생성
  // ==============================

  // 현재 월의 1일
  // 예: currentDate가 2026-05-15라면 startOfMonth는 2026-05-01
  const startOfMonth = new Date(year, month, 1);

  // 현재 월 1일의 요일
  // 0: 일요일, 1: 월요일, ..., 6: 토요일
  const startDay = startOfMonth.getDay();

  // 달력에 표시할 첫 번째 날짜
  // 월 달력은 1일이 무슨 요일인지에 따라 앞쪽에 이전 달 날짜가 보일 수 있음
  // 예: 5월 1일이 금요일이면, 달력 시작은 그 주 일요일인 4월 26일
  const calendarStart = new Date(startOfMonth);
  calendarStart.setDate(startOfMonth.getDate() - startDay);

  // 월 보기용 날짜 배열
  // 6주 x 7일 = 총 42칸을 만든다.
  // 각 칸은 단순 숫자가 아니라 날짜 정보를 가진 객체로 만든다.
  const monthDays = Array.from({ length: 42 }, (_, i) => {
    const date = new Date(calendarStart);
    date.setDate(calendarStart.getDate() + i);

    return {
      // 실제 Date 객체
      date: date,

      // 화면에 보여줄 날짜 숫자
      day: date.getDate(),

      // DB의 workDate와 비교할 문자열
      // 예: 2026-05-01
      dateString: date.toLocaleDateString('en-CA'),
    };
  });

  // 현재 보고 있는 주의 시작일 계산
  // 일요일 시작 기준
  const weekStart = new Date(currentDate);
  weekStart.setDate(currentDate.getDate() - currentDate.getDay());

  // 주 보기용 날짜 배열
  // 현재 주의 일요일부터 토요일까지 7개 날짜를 만든다.
  const weekDays = Array.from({ length: 7 }, (_, i) => {
    const date = new Date(weekStart);
    date.setDate(weekStart.getDate() + i);

    return {
      date: date,
      day: date.getDate(),
      dateString: date.toLocaleDateString('en-CA'),
    };
  });

  // 일 보기용 날짜 배열
  // 하루만 보여주지만, 구조를 월/주와 똑같이 맞춘다.
  const dayDays = [
    {
      date: currentDate,
      day: currentDate.getDate(),
      dateString: currentDate.toLocaleDateString('en-CA'),
    },
  ];

  // 보기 모드에 따라 화면에 보여줄 날짜 배열 결정
  // month: 42칸 달력
  // week: 7일
  // day: 1일
  const displayDays =
    viewMode === 'month'
      ? monthDays
      : viewMode === 'week'
        ? weekDays
        : dayDays;

  // 이전 기간으로 이동
  const handlePrev = () => {
    const newDate = new Date(currentDate);

    if (viewMode === 'month') {
      // 이전 달
      newDate.setMonth(newDate.getMonth() - 1);
    } else if (viewMode === 'week') {
      // 이전 주
      newDate.setDate(newDate.getDate() - 7);
    } else {
      // 이전 일
      newDate.setDate(newDate.getDate() - 1);
    }

    setCurrentDate(newDate);
  };

  // 다음 기간으로 이동
  const handleNext = () => {
    const newDate = new Date(currentDate);

    if (viewMode === 'month') {
      // 다음 달
      newDate.setMonth(newDate.getMonth() + 1);
    } else if (viewMode === 'week') {
      // 다음 주
      newDate.setDate(newDate.getDate() + 7);
    } else {
      // 다음 일
      newDate.setDate(newDate.getDate() + 1);
    }

    setCurrentDate(newDate);
  };

  // 오늘로 이동
  const handleToday = () => {
    setCurrentDate(new Date());
  };      

  // 근태 목록을 다시 조회하는 함수
  // 처음 화면 들어왔을 때도 사용하고,
  // 출근/퇴근 성공 후 화면 갱신할 때도 다시 사용할 함수
  const fetchAttendance = async () => {
    // 로그인 사용자 사번이 없으면 조회하지 않음
    if (!empNo) return;

    try {
      const params = {
        empNo: empNo,
      }; 
      const res = await request('GET', '/attendance/my', params);

      // 1. 캘린더에 보여줄 근태 목록 저장
      setAttendanceList(res.data);

      // 2. 오늘 날짜 만들기
      const todayDate = new Date().toLocaleDateString('en-CA');

      // 3. 근태 목록 중 오늘 날짜 데이터 찾기
      const todayData = res.data.find((item) => item.workDate === todayDate);

      // 4. 오늘 데이터가 있으면 오늘 카드에 반영
      if (todayData) {
        // 상태 반영
        setTodayStatus(todayData.status);

        // 출근 시간이 있으면 HH:mm 형태로 잘라서 저장
        setCheckInTime(
          todayData.checkInAt ? todayData.checkInAt.substring(11, 16) : null
        );

        // 퇴근 시간이 있으면 HH:mm 형태로 잘라서 저장
        setCheckOutTime(
          todayData.checkOutAt ? todayData.checkOutAt.substring(11, 16) : null
        );

        // 오늘 근무시간 저장
        // DB에서 workHours가 null이면 0으로 처리
        setTodayWorkHours(todayData.workHours || 0);

      } else {
        // 오늘 데이터가 없으면 출근 전 상태로 초기화
        setTodayStatus('출근 전');

        // 출근 시간이 없으므로 null
        setCheckInTime(null);

        // 퇴근 시간이 없으므로 null
        setCheckOutTime(null);

        // 오늘 근무시간도 0으로 초기화
        setTodayWorkHours(0);
      }
    } catch (err) {
      console.error(err);
      alert('근태 조회 중 오류가 발생했습니다.');
    }
  };

  // 연차 요약 조회 함수
  // 근태 메인에서 "내 연차" 카드에 보여줄 데이터 조회
  const fetchLeaveSummary = async () => {
    if (!empNo) return;
    
    try {
      // 공통 axios helper 사용
      // GET 요청이므로 세 번째 인자는 자동으로 params로 전달됨
      const params = {
        empNo: empNo,
      };

      const res = await request('GET', '/leave/summary', params);

      setLeaveSummary(res.data);
    } catch (error) {

      // 휴가 데이터 조회 실패 시 화면이 깨지지 않도록 0으로 유지
      setLeaveSummary({
        usedDays: 0,
        totalDays: 0,
        remainDays: 0,
      });
    }
  };

  // 화면이 처음 열리거나 로그인 사용자 사번이 준비되면 데이터 조회
  useEffect(() => {
    // userInfo가 아직 준비되지 않았으면 API 호출하지 않음
    if (!empNo) return;

    fetchAttendance();

    // 근태 메인 진입 시 휴가 요약도 함께 조회
    fetchLeaveSummary();
  }, [empNo]);

  // 출근 후 퇴근 전일 때 실시간 근무시간 계산
  useEffect(() => {

    // 출근 안 했으면 계산 안 함
    if (!checkInTime) return;

    // 퇴근했으면 DB 값 사용
    if (checkOutTime) {
      setLiveWorkHours(todayWorkHours);
      return;
    }
    
    // 1초마다 현재 시간 기준으로 근무시간 다시 계산
    const timer = setInterval(() => {
      // 오늘 날짜 문자열 생성
      const todayDate = new Date().toLocaleDateString('en-CA');

      // 출근 시간 → Date 변환
      const checkInDateTime = new Date(`${todayDate}T${checkInTime}:00`);

      // 현재 시간
      const now = new Date();

      // 밀리초 차이 계산
      const diffMs = now - checkInDateTime;

      // 시간 단위 변환
      const hours = diffMs / (1000 * 60 * 60);

      // 소수점 2자리까지 반올림
      setLiveWorkHours(Number(hours.toFixed(2)));

    }, 1000);

    // 화면이 바뀌거나 값이 바뀌면 타이머 정리
    return () => clearInterval(timer);

  }, [checkInTime, checkOutTime, todayWorkHours]);

  // 출근 버튼 클릭
  const handleCheckIn = () => {

    // 이미 처리 중이면 실행 막기
    if (loading) return;

    // 1. 사용자 확인
    const isConfirm = window.confirm('출근 처리하시겠습니까?');
    if (!isConfirm) return;

    // 로딩 시작
    setLoading(true);

    // 2. 브라우저 GPS 요청
    if (!navigator.geolocation) {
      alert('GPS를 지원하지 않는 브라우저입니다.');

      setLoading(false);
      return;
    }

    // GPS 요청 시작 상태 표시
    setGpsMessage('위치 확인 중입니다...');

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          // 3. 현재 위치 가져오기
          const lat = position.coords.latitude;
          const lng = position.coords.longitude;

          // GPS 성공 메시지
          setGpsMessage('GPS 확인 완료');

          // 4. 백엔드 출근 API 호출
          const params = {
            empNo: empNo,
            lat: lat,
            lng: lng,
          }
          await request('POST', '/attendance/check-in', params);


          // 성공 메시지
          setAttendanceMessage('출근 처리가 완료되었습니다.');

          // 데이터 다시 조회
          await fetchAttendance();

        } catch (error) {

          // 실패 메시지
          setAttendanceMessage(
            error.response?.data?.message || '출근 처리에 실패했습니다.'
          );

        } finally {
          // 무조건 실행 (성공/실패 둘 다)
          setLoading(false);
        }
      },

      // GPS 실패 시
      (error) => {
        console.error(error);

        setGpsMessage('위치 정보를 가져올 수 없습니다.');
        setAttendanceMessage('GPS 위치를 가져올 수 없습니다.');

        setLoading(false); 
      }
    );
  };

  // 퇴근 버튼 클릭
  const handleCheckOut = () => {

    // 이미 처리 중이면 실행 막기
    if (loading) return;

    const isConfirm = window.confirm('퇴근 처리하시겠습니까?');
    if (!isConfirm) return;

    // 로딩 시작
    setLoading(true);

    if (!navigator.geolocation) {
      alert('GPS를 지원하지 않는 브라우저입니다.');

      setLoading(false); 
      return;
    }

    setGpsMessage('위치 확인 중입니다...');

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          const lat = position.coords.latitude;
          const lng = position.coords.longitude;

          // GPS 성공
          setGpsMessage('GPS 확인 완료');

          const params = {
              empNo: empNo,
              lat: lat,
              lng: lng,
          }
          await request('POST', '/attendance/check-out', params);
          const checkoutDate = new Date();

          // 성공 메시지
          setAttendanceMessage('퇴근 처리가 완료되었습니다.');

          await fetchAttendance();

          // [결재-근태 연동용]: 정상 퇴근은 완료하되, 18시 이전이면 조퇴 결재 작성 여부를 안내합니다.
          if (isBeforeStandardEndTime(checkoutDate)) {
            const shouldWriteEarlyLeave = window.confirm(
              '18시 이전 퇴근으로 조퇴 신청이 필요합니다. 부재 일정 결재 문서를 작성하시겠습니까?'
            );

            if (shouldWriteEarlyLeave) {
              await openEarlyLeaveApproval(checkoutDate);
            }
          }

        } catch (error) {

          // [결재-근태 연동용]: 회사 위치 밖에서 퇴근에 실패한 경우 외근 근무 결과 신청으로 이어줍니다.
          if (isCompanyLocationError(error)) {
            const shouldWriteWorkResult = window.confirm(
              '회사 위치 밖에서 퇴근 처리에 실패했습니다. 근무 결과 신청 결재 문서를 작성하시겠습니까?'
            );

            if (shouldWriteWorkResult) {
              await openOutsideWorkResultApproval(new Date());
            }
          }

          setAttendanceMessage(
            error.response?.data?.message || '퇴근 처리에 실패했습니다.'
          );

        } finally {
          // 반드시 필요
          setLoading(false);
        }
      },

      // GPS 실패
      (error) => {
        console.error(error);

        setGpsMessage('위치 정보를 가져올 수 없습니다.');
        setAttendanceMessage('GPS 위치를 가져올 수 없습니다.');

        setLoading(false); 
      }
    );
  };

  // 상태 배지 색상
  const getBadgeColor = (status) => {
    if (status === 'ON_TIME' || status === '정상출근') return 'success';
    if (status === 'LEAVE' || status === '휴가') return 'info';
    if (status === 'HALF_LEAVE' || status === '반차') return 'info';
    if (status === 'LATE' || status === '지각') return 'warning';
    if (status === 'EARLY' || status === '조퇴') return 'danger';
    if (status === 'LEFT' || status === '퇴근') return 'primary';
    return 'secondary';
  };

  // 상태 한글 표시
  const getStatusText = (status) => {
    // DB에서 영어 Enum 코드가 올 경우
    if (status === 'ON_TIME') return '정상출근';
    if (status === 'LEAVE') return '휴가';
    if (status === 'HALF_LEAVE') return '반차';
    if (status === 'LATE') return '지각';
    if (status === 'EARLY') return '조퇴';
    if (status === 'LEFT') return '퇴근완료';

    // DB에서 이미 한글 label이 올 경우
    if (status === '정상출근') return '정상출근';
    if (status === '휴가') return '휴가';
    if (status === '반차') return '반차';
    if (status === '지각') return '지각';
    if (status === '조퇴') return '조퇴';
    if (status === '퇴근') return '퇴근완료';

    // 오늘 기록이 없을 때만 출근 전
    return '출근 전';
  };

  // 하루 기준 근무시간
  // 우리 프로젝트는 고정근무제 기준 1일 8시간으로 계산
  const STANDARD_WORK_HOURS = 8;

  // 화면에 표시할 근무시간
  // 8시간을 넘으면 Progress Bar가 100%를 초과하지 않게 제한
  // 퇴근 전이면 liveWorkHours, 퇴근 후면 todayWorkHours 사용
  const displayWorkHours = checkOutTime ? todayWorkHours : liveWorkHours;

  // 진행률 계산
  const workProgress = Math.min(
    Math.round((displayWorkHours / STANDARD_WORK_HOURS) * 100),
    100
  );

  // ==============================
  // 이번 달 근태 요약 계산
  // ==============================

  // 이번 달 문자열 만들기
  // 예: 2026-05
  const currentMonth = `${year}-${String(month + 1).padStart(2, '0')}`;

  // 이번 달 데이터만 필터링
  const monthlyAttendanceList = attendanceList.filter((item) =>
    item.workDate?.startsWith(currentMonth)
  );

  // 정상출근 횟수
  const onTimeCount = monthlyAttendanceList.filter(
    (item) => item.status === 'ON_TIME' || item.status === '정상출근'
  ).length;

  // 지각 횟수
  const lateCount = monthlyAttendanceList.filter(
    (item) => item.status === 'LATE' || item.status === '지각'
  ).length;

  // 조퇴 횟수
  const earlyCount = monthlyAttendanceList.filter(
    (item) => item.status === 'EARLY' || item.status === '조퇴'
  ).length;

  // 이번 달 총 근무시간
  const totalWorkHours = monthlyAttendanceList
    .reduce((sum, item) => sum + Number(item.workHours || 0), 0)
    .toFixed(2);

  // ==============================
  // 이번 주 근무시간 차트 데이터
  // ==============================

  // 이번 주 시작일 계산
  const chartWeekStart = new Date(currentDate);
  chartWeekStart.setDate(currentDate.getDate() - currentDate.getDay());

  // 요일 이름
  const weekLabels = ['일', '월', '화', '수', '목', '금', '토'];

  // 차트 데이터 생성
  const weeklyChartData = Array.from({ length: 7 }, (_, i) => {

    // 현재 요일 날짜
    const date = new Date(chartWeekStart);
    date.setDate(chartWeekStart.getDate() + i);

    // YYYY-MM-DD 형식
    const dateString = date.toLocaleDateString('en-CA');

    // 해당 날짜 근태 찾기
    const attendance = attendanceList.find(
      (item) => item.workDate === dateString
    );

    return {
      day: weekLabels[i],

      // 근무시간
      hours: Number(attendance?.workHours || 0),
    };
  });

  // 월간 근태 상태 비율 차트 데이터
  const monthlyPieData = [
    {
      name: '정상출근',
      value: onTimeCount,
    },
    {
      name: '지각',
      value: lateCount,
    },
    {
      name: '조퇴',
      value: earlyCount,
    },
  ];

  // PieChart 색상
  const pieColors = ['#198754', '#f9b115', '#e55353'];

  // 로그인 사용자 정보가 아직 준비되지 않았을 때 화면
  // userInfo가 늦게 들어오는 경우 API 호출 전에 안내 화면을 보여준다.
  if (!empNo) {
    return (
      <div style={{ padding: '24px' }}>
        <CCard>
          <CCardBody className="text-center py-5">
            사용자 정보를 불러오는 중입니다...
          </CCardBody>
        </CCard>
      </div>
    );
  }

  return (
    <div style={{ padding: '24px' }}>
      <h2 className="mb-4">
        {userInfo?.name ? `${userInfo.name}님의 근태 현황` : '근태 현황'}
      </h2>
      
      {/* 이번 달 근태 요약 카드 */}
      <CRow className="mb-4">

        {/* 정상출근 카드 */}
        <CCol md={2}>
          <CCard
            onClick={() => setStatusFilter('ON_TIME')} // 정상출근 필터 적용
            style={{
              cursor: 'pointer',
              border: statusFilter === 'ON_TIME' ? '2px solid #198754' : '1px solid #ddd',
              minHeight: '120px',
            }}
          >
            <CCardBody>
              <div style={{ fontSize: '13px', color: '#6c757d' }}>
                {month + 1}월 정상출근
              </div>
              <h4 className="mt-2 mb-0">{onTimeCount}회</h4>
            </CCardBody>
          </CCard>
        </CCol>

        {/* 지각 카드 */}
        <CCol md={2}>
          <CCard
            onClick={() => setStatusFilter('LATE')} // 지각 필터 적용
            style={{
              cursor: 'pointer',
              border: statusFilter === 'LATE' ? '2px solid #f9b115' : '1px solid #ddd',
              minHeight: '120px',
            }}
          >
            <CCardBody>
              <div style={{ fontSize: '13px', color: '#6c757d' }}>
                {month + 1}월 지각
              </div>
              <h4 className="mt-2 mb-0">{lateCount}회</h4>
            </CCardBody>
          </CCard>
        </CCol>

        {/* 조퇴 카드 */}
        <CCol md={2}>
          <CCard
            onClick={() => setStatusFilter('EARLY')} // 조퇴 필터 적용
            style={{
              cursor: 'pointer',
              border: statusFilter === 'EARLY' ? '2px solid #e55353' : '1px solid #ddd',
              minHeight: '120px',
            }}
          >
            <CCardBody>
              <div style={{ fontSize: '13px', color: '#6c757d' }}>
                {month + 1}월 조퇴
              </div>
              <h4 className="mt-2 mb-0">{earlyCount}회</h4>
            </CCardBody>
          </CCard>
        </CCol>

        {/* 총 근무시간 카드 */}
        <CCol md={3}>
          <CCard
            onClick={() => setStatusFilter('ALL')} // 전체 보기로 초기화
            style={{
              cursor: 'pointer',
              border: statusFilter === 'ALL' ? '2px solid #321fdb' : '1px solid #ddd',
              minHeight: '120px',
            }}
          >
            <CCardBody>
              <div style={{ fontSize: '13px', color: '#6c757d' }}>
                {month + 1}월 총 근무시간
              </div>
              <h4 className="mt-2 mb-0">{totalWorkHours}시간</h4>
            </CCardBody>
          </CCard>
        </CCol>

        {/* 내 휴가 카드 */}
        <CCol md={3}>
          <CCard
            onClick={() => navigate(PATH.ATTENDANCE.HOLIDAYS)} // 휴가 현황 페이지로 이동
            style={{
              cursor: 'pointer',
              border: '1px solid #ddd',
              minHeight: '120px',
              backgroundColor: '#f8f9ff',
            }}
          >
            <CCardBody>
              <div style={{ fontSize: '13px', color: '#6c757d' }}>
                내 휴가
              </div>

              {/* 남은 휴가 / 총 휴가 */}
              <h4 className="mt-2 mb-0">
                {leaveSummary.remainDays} / {leaveSummary.totalDays}일
              </h4>

              <div className="mt-2" style={{ fontSize: '13px', color: '#6c757d' }}>
                클릭하면 휴가 현황으로 이동
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* 대시보드 메인 영역: 오늘 근무 상태 + 월간 비율 차트 */}
      <CRow className="mb-4">

        {/* 왼쪽: 오늘 출퇴근 카드 */}
        <CCol md={7}>
          <CCard className="h-100">
            <CCardHeader>
              <strong>오늘의 근무 상태</strong>
            </CCardHeader>

            <CCardBody>
              <h5>
                상태:{' '}
                <CBadge color={getBadgeColor(todayStatus)}>
                  {getStatusText(todayStatus)}
                </CBadge>
              </h5>

              <p className="mt-3">출근 시간: {checkInTime || '-'}</p>
              <p>퇴근 시간: {checkOutTime || '-'}</p>

              {/* 오늘 근무시간 */}
              <div className="mt-3">
                <div className="d-flex justify-content-between mb-1">
                  <strong>오늘 근무시간</strong>
                  <span>{displayWorkHours} / 8시간</span>
                </div>

                <CProgress value={workProgress} height={16} />

                <div className="mt-1" style={{ fontSize: '13px', color: '#6c757d' }}>
                  진행률: {workProgress}%
                </div>
              </div>

              <p className="mt-2" style={{ fontSize: '13px', color: '#6c757d' }}>
                GPS 상태: {gpsMessage}
              </p>

              {attendanceMessage && (
                <div
                  className="mt-2"
                  style={{
                    fontSize: '13px',
                    color: attendanceMessage.includes('완료') ? '#198754' : '#dc3545',
                  }}
                >
                  {attendanceMessage}
                </div>
              )}

              {/* 출근/퇴근 버튼 */}
              <div className="mt-3">
                <CButton
                  color="primary"
                  variant="outline"
                  onClick={handleCheckIn}
                  disabled={checkInTime !== null || loading}
                >
                  {loading ? '처리중...' : '출근하기'}
                </CButton>

                <CButton
                  color="success"
                  variant="outline"
                  className="ms-2"
                  onClick={handleCheckOut}
                  disabled={checkInTime === null || checkOutTime !== null || loading}
                >
                  {loading ? '처리중...' : '퇴근하기'}
                </CButton>
              </div>

              <div className="mt-2" style={{ fontSize: '13px', color: '#6c757d' }}>
                {checkInTime === null && '아직 출근 전입니다. 출근 버튼을 눌러 출근을 등록하세요.'}
                {checkInTime !== null && checkOutTime === null && '출근이 등록되었습니다. 퇴근 시 퇴근 버튼을 눌러주세요.'}
                {checkInTime !== null && checkOutTime !== null && '오늘의 출퇴근 처리가 완료되었습니다.'}
              </div>
            </CCardBody>
          </CCard>
        </CCol>

        {/* 오른쪽: 월간 PieChart */}
        <CCol md={5}>
          <CCard className="h-100">
            <CCardHeader>
              <strong>{month + 1}월 근태 상태 비율</strong>
            </CCardHeader>

            <CCardBody>
              <div style={{ width: '100%', height: '260px' }}>
                <ResponsiveContainer>
                  <PieChart>
                    <Pie
                      data={monthlyPieData}
                      dataKey="value"
                      nameKey="name"
                      outerRadius={75}
                      label
                    >
                      {monthlyPieData.map((entry, index) => (
                        <Cell
                          key={`cell-${index}`}
                          fill={pieColors[index]}
                        />
                      ))}
                    </Pie>

                    <Tooltip />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* 주간 근무시간 차트 */}
      <CCard className="mb-4">
        <CCardHeader>
          <strong>
            {`${weekStart.getFullYear()}-${String(
              weekStart.getMonth() + 1
            ).padStart(2, '0')}-${String(
              weekStart.getDate()
            ).padStart(2, '0')} 기준 주간 근무시간`}
          </strong>
        </CCardHeader>

        <CCardBody>
          <div style={{ width: '100%', height: '220px' }}>
            <ResponsiveContainer>
              <BarChart data={weeklyChartData}>
                <XAxis dataKey="day" />
                <YAxis domain={[0, 8]} />
                <Tooltip />

                <Bar
                  dataKey="hours"
                  name="근무시간"
                  fill="#321fdb"
                  barSize={36}
                  radius={[6, 6, 0, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </CCardBody>
      </CCard>      

      {/* 월간 캘린더 */}
      <CCard>
        <CCardHeader>

          {/* 상단 헤더 영역 */}
          <div className="d-flex justify-content-between align-items-start">

            {/* 왼쪽: 캘린더 제목 */}
            <div>

              <strong>
                {/* 일 보기 제목 */}
                {viewMode === 'day' &&
                  `${year}년 ${month + 1}월 ${currentDate.getDate()}일 근태`
                }

                {/* 주 보기 제목 */}
                {viewMode === 'week' && (() => {

                  const start = new Date(weekStart);

                  const end = new Date(weekStart);
                  end.setDate(start.getDate() + 6);

                  return `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, '0')}-${String(start.getDate()).padStart(2, '0')} ~ ${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, '0')}-${String(end.getDate()).padStart(2, '0')} 근태`;

                })()}

                {/* 월 보기 제목 */}
                {viewMode === 'month' &&
                  `${year}년 ${month + 1}월 근태 캘린더`
                }
              </strong>

              {/* 현재 필터 표시 */}
              <span
                style={{
                  fontSize: '13px',
                  color: '#6c757d',
                  marginLeft: '10px',
                }}
              >
                {statusFilter === 'ALL' && '전체 보기'}
                {statusFilter === 'ON_TIME' && '정상출근만 보기'}
                {statusFilter === 'LATE' && '지각만 보기'}
                {statusFilter === 'EARLY' && '조퇴만 보기'}
              </span>

            </div>

            {/* 오른쪽 버튼 영역 */}
            <div className="d-flex flex-column align-items-end">

              {/* 위쪽: 일 / 주 / 월 버튼 */}
              <div className="mb-2">

                <CButton
                  size="sm"
                  shape="rounded-pill"
                  color={viewMode === 'day' ? 'primary' : 'secondary'}
                  variant={viewMode === 'day' ? undefined : 'outline'}
                  onClick={() => setViewMode('day')}
                >
                  일
                </CButton>

                <CButton
                  size="sm"
                  shape="rounded-pill"
                  className="ms-2"
                  color={viewMode === 'week' ? 'primary' : 'secondary'}
                  variant={viewMode === 'week' ? undefined : 'outline'}
                  onClick={() => setViewMode('week')}
                >
                  주
                </CButton>

                <CButton
                  size="sm"
                  shape="rounded-pill"
                  className="ms-2"
                  color={viewMode === 'month' ? 'primary' : 'secondary'}
                  variant={viewMode === 'month' ? undefined : 'outline'}
                  onClick={() => setViewMode('month')}
                >
                  월
                </CButton>

              </div>

              {/* 아래쪽: 이전 / 오늘 / 다음 버튼 */}
              <div>

                <CButton
                  size="sm"
                  color="info"
                  variant="outline"
                  onClick={handlePrev}
                >
                  ◀ 이전
                </CButton>

                <CButton
                  size="sm"
                  color="dark"
                  className="ms-2"
                  onClick={handleToday}
                >
                  오늘
                </CButton>

                <CButton
                  size="sm"
                  color="info"
                  variant="outline"
                  className="ms-2"
                  onClick={handleNext}
                >
                  다음 ▶
                </CButton>

              </div>

            </div>

          </div>

        </CCardHeader>
        <CCardBody>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(7, 1fr)',
              gap: '10px',
            }}
          >
            {/* 월/주 모드에서만 요일 표시 */}
            {viewMode !== 'day' &&
              ['일','월','화','수','목','금','토'].map((dayName) => (
              <div
                key={dayName}
                style={{
                  textAlign: 'center',
                  fontWeight: 'bold',
                  color: '#6c757d',
                  padding: '6px 0',
                }}
              >
                {dayName}
              </div>
            ))}

            {displayDays.map((item) => {

                // displayDays 안의 날짜 객체에서 필요한 값 꺼내기
                const { date, day, dateString } = item;

                // 오늘 날짜인지 확인
                // 연/월/일을 각각 비교해서 브라우저 문자열 포맷 영향 없이 정확하게 판단
                const isToday =
                  date.getFullYear() === today.getFullYear() &&
                  date.getMonth() === today.getMonth() &&
                  date.getDate() === today.getDate();

                // 월 보기에서 현재 달이 아닌 날짜인지 확인
                // 예: 5월 달력에 보이는 4월 28일, 6월 1일 같은 날짜
                const isOtherMonth = viewMode === 'month' && date.getMonth() !== month;

                const attendance = attendanceList.find((item) => {
                  // 날짜가 다르면 제외
                  // dateString은 YYYY-MM-DD 형식이라 DB의 workDate와 비교하기 좋음
                  if (item.workDate !== dateString) return false;

                  // 전체 보기일 때는 날짜만 맞으면 표시
                  if (statusFilter === 'ALL') return true;

                  // DB에서 영어 Enum 코드로 올 경우
                  if (item.status === statusFilter) return true;

                  // DB에서 한글 상태값으로 올 경우도 대비
                  if (statusFilter === 'ON_TIME' && item.status === '정상출근') return true;
                  if (statusFilter === 'LATE' && item.status === '지각') return true;
                  if (statusFilter === 'EARLY' && item.status === '조퇴') return true;

                  // 위 조건에 해당하지 않으면 표시하지 않음
                  return false;
                });                

              return (
                <div
                  key={dateString}
                  
                  style={{
                    height: '120px',
                    border: isToday ? '2px solid #321fdb' : '1px solid #ddd',
                    borderRadius: '8px',
                    padding: '10px',

                    // 오늘 날짜는 연한 배경으로 강조
                    backgroundColor: isToday ? '#f3f5ff' : '#fff',

                    // 월 보기에서 현재 달이 아닌 날짜는 흐리게 표시
                    opacity: isOtherMonth ? 0.35 : 1,
                    
                    // 다른달 날짜 클릭 막기
                    pointerEvents: isOtherMonth ? 'none' : 'auto',
                    
                    // 클릭 가능한 느낌
                    cursor: isOtherMonth ? 'default' : 'pointer',
                  }}
                >
                  <strong>{day}일</strong>

                  {/* 해당 날짜에 근태 데이터가 있을 때만 화면에 출력 */}
                  {attendance && (
                    <div className="mt-2">
                      
                       {/* 상태 표시 (지각 / 정상출근 등) */}
                      <CBadge color={getBadgeColor(attendance.status)}>
                        {getStatusText(attendance.status)}
                      </CBadge>

                       {/* 출근 시간 표시 
                           시:분만 잘라오기
                           index 형태를 문자열.substring(시작, 끝)(포함, 미포함) */}
                      <div style={{ fontSize: '13px', marginTop: '8px' }}>
                        출근: {attendance.checkInAt ? attendance.checkInAt.substring(11, 16) : '-'}
                      </div>
                      {/* 퇴근 시간 표시 */}
                      <div style={{ fontSize: '13px' }}>
                        퇴근: {attendance.checkOutAt ? attendance.checkOutAt.substring(11, 16) : '-'}
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </CCardBody>
      </CCard>
    </div>
  );
};

export default Attendance;
