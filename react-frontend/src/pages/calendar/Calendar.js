/**
 * @FileName : Calendar.js
 * @Description : 사원용 캘린더 화면
 *                - 개인/부서/전사 일정 조회 및 관리
 * @Author : 정준하
 * @Date : 2026. 05. 01
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.01    정준하        최초 생성 및 FullCalendar 기본 기능 구현
 * @ 2026.05.14    김다솜        온보딩 카테고리 일정 전용 스타일(onboardingCalendarStyle) 적용
 */

import React, { useCallback, useEffect, useRef, useState } from 'react';

// CoreUI 
import { CCard, CCardBody } from '@coreui/react';

// 간편등록 퀵 팝업
import CalendarSimpleAdd from './CalendarSimpleAdd';

// 상세등록 큰 팝업
import CalendarDetailAdd from './CalendarDetailAdd';

// 기존 일정 상세/수정/삭제 팝업
import CalendarDetail from './CalendarDetail';

import { request } from 'src/helpers/axios_helper';

// 온보딩 일정 전용 스타일 임포트
import { onboardingCalendarStyle } from 'src/styles/js/onboarding/onboardingCalendarStyle';

// 풀캘린더
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import koLocale from '@fullcalendar/core/locales/ko';

const Calendar = () => {

    // FullCalendar를 직접 제어하기 위한 ref
    const calendarRef = useRef(null);

    // 선택한 날짜
    const [selectedDate, setSelectedDate] = useState(null);

    // 선택한 날짜 + 시간
    // 주/일 보기에서 시간 슬롯을 클릭했을 때 기본 시작시간으로 사용.
    const [selectedDateTime, setSelectedDateTime] = useState(null);

    // 간편등록 퀵 팝업 열림 여부
    const [simpleAddVisible, setSimpleAddVisible] = useState(false);

    // 상세등록 큰 팝업 열림 여부    
    const [detailAddVisible, setDetailAddVisible] = useState(false);

    // 기존 일정 상세 팝업 열림 여부
    const [detailVisible, setDetailVisible] = useState(false);

    // 선택한 기존 일정 정보
    const [selectedSchedule, setSelectedSchedule] = useState(null);

    // 간편등록 퀵 팝업 위치
    const [popupPosition, setPopupPosition] = useState({
        top: 120,
        left: 420,
    });

    // 상세등록 큰 팝업 위치
    const [detailPopupPosition, setDetailPopupPosition] = useState({
        top: 100,
        left: 760,
    });

    // 현재 캘린더 제목
    const [calendarTitle, setCalendarTitle] = useState('');

    // 현재 캘린더 보기 방식
    const [currentView, setCurrentView] = useState('dayGridMonth');

    // 현재 열려있는 필터 드롭다운
    const [openFilter, setOpenFilter] = useState(null);

    // 일정 범위 필터
    const [scopeFilters, setScopeFilters] = useState({
        my: true,
        department: true,
        company: true,
    });

    // 카테고리 필터
    const [categoryFilters, setCategoryFilters] = useState({
        meeting: true,
        work: true,
        notice: true,
        etc: true,
    });

    // 멤버 일정 필터 - 백엔드 연결 전 더미 데이터
    const [memberFilters, setMemberFilters] = useState([
        { id: 'me', name: '내 일정', checked: true, color: '#0D6EFD' },
        { id: 'song', name: '송창범', checked: true, color: '#20C997' },
        { id: 'cho', name: '조민수', checked: true, color: '#FD7E14' },
        { id: 'kim', name: '김다솜', checked: true, color: '#6F42C1' },
    ]);

    // 캘린더 일정 목록
    // 백엔드 조회 결과를 FullCalendar 형식으로 관리한다.
    const [calendarEvents, setCalendarEvents] = useState([]);

    // 원본 일정 목록
    // DB 에서 받은 일정은 원본으로 보관, 화면 푯기용 일정은 별도로 만듬.
    const [scheduleList, setScheduleList] = useState([]);

    // 현재 캘린더 표시 범위
    // 월/주/일 화면이 바뀔 때마다 FullCalendar가 보고있는 시작일과 종료일 저장.
    const [calendarRange, setCalendarRange] = useState({
        start: null,
        end: null,
    });

    // 등록 완료 알림
    const [successMessage, setSuccessMessage] = useState('');

    // 일정 입력 실시간 반영 노출
    // 등록 전 입력 중인 제목/시간을 캘린더에 임시로 보여줌
    const [draftEvent, setDraftEvent] = useState(null);

    // 온보딩 전용 스타일(onboardingCalendarStyle.js)을 document head에 동적으로 주입
    useEffect(() => {
        const styleTag = document.createElement('style');
        styleTag.innerHTML = onboardingCalendarStyle.css;
        document.head.appendChild(styleTag);

        // 컴포넌트 언마운트 시 스타일 제거
        return () => {
            document.head.removeChild(styleTag);
        };
    }, []);

    // 캘린더 일정 클릭 처리
    // 기존 일정은 페이지 이동 대신 읽기 전용 상세 팝업으로 열림.
    const handleEventClick = (info) => {
        const eventId = info.event.id;
        const scheduleId = info.event.extendedProps.scheduleId || eventId;

        // 저장 전 미리보기 일정은 실제 DB 일정이 아니므로 상세 팝업을 열지 않는다.
        if (eventId === 'calendar-draft-event') {
            return;
        }

        // 기존 일정 클릭 위치 기준으로 상세 팝업 위치 계산
        // info.el은 FullCalendar에서 실제로 클릭한 일정 DOM 영역이다.
        const rect = info.el.getBoundingClientRect();
        const popupGap = 12;
        const popupWidth = 560;
        const popupHeight = 520;
        const viewportPadding = 20;

        let left = rect.right + popupGap;
        let top = rect.top;

        // 오른쪽 공간이 부족하면 일정 왼쪽으로 팝업을 띄운다.
        if (left + popupWidth > window.innerWidth) {
            left = rect.left - popupWidth - popupGap;
        }

        // 화면 왼쪽 밖으로 나가지 않게 보정한다.
        if (left < viewportPadding) {
            left = viewportPadding;
        }

        // 화면 아래쪽으로 잘리지 않게 위로 올린다.
        if (top + popupHeight > window.innerHeight) {
            top = window.innerHeight - popupHeight - viewportPadding;
        }

        // 상단 메뉴 영역과 너무 붙지 않게 최소 위치를 잡는다.
        if (top < 80) {
            top = 80;
        }

        setDetailPopupPosition({
            top,
            left,
        });

        setDraftEvent(null);
        setSimpleAddVisible(false);
        setDetailAddVisible(false);

        setSelectedSchedule({
            scheduleId,
            title: info.event.title,
            startTime: info.event.startStr,
            endTime: info.event.endStr,
            isAllDay: info.event.allDay,
            ...info.event.extendedProps,
        });

        setDetailVisible(true);
    };

    // 반복 간격 계산
    // 원본 날짜에서 다음 반복 날짜(매일/매주/매월)를 만든다.
    const addRepeatInterval = (date, repeatRule) => {
        const nextDate = new Date(date);

        if (repeatRule === 'DAILY') {
            nextDate.setDate(nextDate.getDate() + 1);
        } else if (repeatRule === 'WEEKLY') {
            nextDate.setDate(nextDate.getDate() + 7);
        } else if (repeatRule === 'MONTHLY') {
            nextDate.setMonth(nextDate.getMonth() + 1);
        } else {
            return null;
        }

        return nextDate;
    };

    // FullCalendar 날짜 형식 변환
    // Date 객체를 캘린더가 읽을 수 있는 yyyy-MM-ddTHH:mm:ss 문자열로 바꾼다.
    const formatCalendarDateTime = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');
        const second = String(date.getSeconds()).padStart(2, '0');

        return `${year}-${month}-${day}T${hour}:${minute}:${second}`;
    };

    // 캘린더 표시용 일정 생성
    // DB 일정 데이터를 FullCalendar event 형식으로 바꾼다.
    const createCalendarEvent = (schedule, startTime, endTime, repeatIndex = 0) => ({
        id: repeatIndex === 0
            ? String(schedule.scheduleId)
            : `${schedule.scheduleId}-repeat-${repeatIndex}`,
        title: schedule.title,
        start: startTime,
        end: endTime,
        allDay: schedule.isAllDay,
        className: schedule.category === 'ONBOARDING' ? onboardingCalendarStyle.eventClass : '',
        extendedProps: {
            scheduleId: schedule.scheduleId,
            type: schedule.type,
            category: schedule.category,
            location: schedule.location,
            content: schedule.content,
            isPublic: schedule.isPublic,
            repeatRule: schedule.repeatRule,
            creatorNo: schedule.creatorNo,

            // 반복 일정 원본 정보
            // 화면에 표시된 반복 날짜와 DB에 저장된 원본 날짜를 구분하기 위해 보관한다.
            originalStartTime: schedule.startTime,
            originalEndTime: schedule.endTime,
            isRepeatGenerated: repeatIndex > 0,
        },
    });

    // 캘린더 표시 범위 포함 여부
    // 일정 시간이 현재 월/주/일 화면 범위와 겹치는지 확인한다.
    const isEventInCalendarRange = (startTime, endTime, rangeStart, rangeEnd) => {
        const eventStart = new Date(startTime);
        const eventEnd = new Date(endTime || startTime);

        if (Number.isNaN(eventStart.getTime()) || Number.isNaN(eventEnd.getTime())) {
            return false;
        }

        return eventStart < rangeEnd && eventEnd > rangeStart;
    };

    // 반복 일정 펼치기
    // DB에는 원본 1개만 저장하고, 현재 화면 범위 안에서만 반복 일정을 만들어 보여준다.
    const expandRepeatedScheduleEvents = (schedule, rangeStart, rangeEnd) => {
        if (!rangeStart || !rangeEnd) {
            return [];
        }

        const canRepeat = schedule.type === 'PERSONAL' && schedule.repeatRule;

        if (!canRepeat) {
            return isEventInCalendarRange(schedule.startTime, schedule.endTime, rangeStart, rangeEnd)
                ? [createCalendarEvent(schedule, schedule.startTime, schedule.endTime)]
                : [];
        }

        const events = [];
        const startDate = new Date(schedule.startTime);
        const endDate = new Date(schedule.endTime || schedule.startTime);

        if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) {
            return [];
        }

        const duration = endDate.getTime() - startDate.getTime();
        let currentStart = startDate;
        let repeatIndex = 0;

        // 화면 시작일 전 반복 일정은 건너뛴다.
        while (currentStart && currentStart < rangeStart && repeatIndex < 5000) {
            const currentEnd = new Date(currentStart.getTime() + duration);

            if (currentEnd > rangeStart) {
                break;
            }

            currentStart = addRepeatInterval(currentStart, schedule.repeatRule);
            repeatIndex += 1;
        }

        // 현재 화면 범위 안에 들어오는 반복 일정만 만든다.
        while (currentStart && currentStart < rangeEnd && repeatIndex < 5000) {
            const currentEnd = new Date(currentStart.getTime() + duration);

            if (currentEnd > rangeStart) {
                events.push(
                    createCalendarEvent(
                        schedule,
                        formatCalendarDateTime(currentStart),
                        formatCalendarDateTime(currentEnd),
                        repeatIndex
                    )
                );
            }

            currentStart = addRepeatInterval(currentStart, schedule.repeatRule);
            repeatIndex += 1;
        }

        return events;
    };

    // 일정 목록 조회
    // 서버 데이터를 FullCalendar 형식으로 변환한다.
    const fetchScheduleList = async () => {
        try {
            // GET 요청으로 일정 목록 API 호출
            // response.data 에 백엔드가 준 일정 배열이 들어있다.
            const response = await request('GET', '/calendar/list', null);

            setScheduleList(response.data);

        } catch (error) {
            console.error('일정 목록 조회 실패:', error);
        }
    };

    // 첫 화면 조회
    // 페이지가 열릴 때 일정 목록을 한 번 불러온다.
    useEffect(() => {
        fetchScheduleList();
    }, []);

    // 화면 표시용 일정 생성
    // 원본 일정 목록과 현재 캘린더 범위를 기준으로 FullCalendar용 이벤트를 만든다.
    useEffect(() => {
        if (!calendarRange.start || !calendarRange.end) {
            return;
        }

        const events = scheduleList.flatMap((schedule) =>
            expandRepeatedScheduleEvents(schedule, calendarRange.start, calendarRange.end)
        );

        setCalendarEvents(events);
    }, [scheduleList, calendarRange]);

    // 등록 성공 처리
    // 목록을 다시 불러오고 성공 문구 띄움
    const handleCreateSuccess = async () => {
        await fetchScheduleList();
        setDraftEvent(null);
        setSimpleAddVisible(false);
        setDetailAddVisible(false);
        setSuccessMessage('일정이 등록되었습니다.');

        setTimeout(() => {
            setSuccessMessage('');
        }, 2000);
    };

    // 일정 삭제 처리
    // 기존 일정 팝업에서 삭제 버튼을 누르면 백엔드 DELETE API를 호출.
    const handleDeleteSchedule = async (schedule) => {
        if (!schedule?.scheduleId) {
            return;
        }

        // 반복 일정은 원본 일정 삭제로 처리되므로 전체 삭제 문구를 보여준다.
        const deleteMessage = schedule.repeatRule
            ? '반복 일정 전체가 삭제됩니다. 삭제하시겠습니까?'
            : '일정을 삭제하시겠습니까?';

        if (!window.confirm(deleteMessage)) {
            return;
        }

        try {
            await request('DELETE', `/calendar/${schedule.scheduleId}`, null);

            await fetchScheduleList();
            setSelectedSchedule(null);
            setDetailVisible(false);
            setSuccessMessage('일정이 삭제되었습니다.');

            setTimeout(() => {
                setSuccessMessage('');
            }, 2000);
        } catch (error) {
            console.error('일정 삭제 실패:', error);
        }
    };

    // 일정 입력 미리보기 반영
    // 자식 팝업에서 넘어온 입력값을 FullCalendar용 임시 일정으로 만듬
    // useCallback으로 함수를 고정해서 자식 useEffect가 무한 반복되지 않게 함
    const handleDraftChange = useCallback((draft) => {
        if (!draft) {
            setDraftEvent(null);
            return;
        }

        setDraftEvent({
            id: 'calendar-draft-event',
            title: draft.title?.trim() ? draft.title.trim() : '(제목 없음)',
            start: draft.start,
            end: draft.end,
            allDay: draft.allDay,
            className: 'calendar-draft-event',
        });
    }, []);

    // 상세등록 팝업 열기
    // 간편등록 팝업은 닫고, 새 일정 등록 모드로 큰 팝업을 띄움
    const handleOpenDetailAdd = () => {
        setDraftEvent(null);
        setSelectedSchedule(null);
        setDetailVisible(false);
        setSimpleAddVisible(false);
        setDetailAddVisible(true);
    };

    // 날짜 클릭 시 선택 날짜 옆에 간편등록 퀵 팝업 열기
    const handleDateClick = (info) => {

        // 클릭한 날짜 저장
        // 주/일 보기에서는 dateStr에 시간대(+09:00)까지 붙어서 날짜만 잘라서 사용
        const clickedDate = info.dateStr.slice(0, 10);
        const clickedDateTime = info.dateStr.length >= 16 ? info.dateStr.slice(0, 16) : null;

        setSelectedDate(clickedDate);
        setSelectedDateTime(clickedDateTime);
        setDraftEvent(null);
        setSelectedSchedule(null);
        setDetailVisible(false);
        setDetailAddVisible(false);

        // 클릭 위치 기준 잡기
        // 월 보기는 날짜 셀 기준, 주/일 보기는 실제 마우스 클릭 위치 기준 팝업창 띄움.
        const rect = info.dayEl.getBoundingClientRect();
        const isTimeGridView = info.view.type === 'timeGridWeek' || info.view.type === 'timeGridDay';

        // 간편등록 팝업 크기
        const popupWidth = 390;
        const popupHeight = 520;

        // 상세등록 팝업 크기
        const detailPopupWidth = 560;

        // 기본위치 : 월 보기는 날짜 셀 근처, 주/일 보기는 클릭한 시간 슬롯 근처
        let left = isTimeGridView
            ? info.jsEvent.clientX + 12
            : rect.right + 10;

        let top = isTimeGridView
            ? info.jsEvent.clientY + 12
            : rect.top + 10;

        // 화면 오른쪽 벗어나면 날짜 셀 왼쪽 표시
        if (left + popupWidth > window.innerWidth) {
            left = rect.left - popupWidth - 10;
        }

        // 화면 아래쪽 벗어나면 위로 보정
        if (top + popupHeight > window.innerHeight) {
            top = window.innerHeight - popupHeight - 30;
        }

        // 아래로 밀리지 않도록 최종 보정
        top = Math.min(top, window.innerHeight - popupHeight - 30);

        // 너무 왼쪽/위쪽으로 붙지 않게 최소값 보정
        if (left < 20) {
            left = 20;
        }

        if (top < 20) {
            top = 20;
        }

        // 팝업 위치 저장
        setPopupPosition({
            top,
            left,
        });

        // 상세등록 팝업 위치 계산
        let detailLeft = rect.right + 10;
        let detailTop = 80;

        if (detailLeft + detailPopupWidth > window.innerWidth) {
            detailLeft = rect.left - detailPopupWidth - 10;
        }

        if (detailLeft < 20) {
            detailLeft = 20;
        }

        if (detailTop < 20) {
            detailTop = 20;
        }

        setDetailPopupPosition({
            top: detailTop,
            left: detailLeft,
        });

        // 퀵 팝업 열기
        setSimpleAddVisible(true);
    };

    // 일정 범위 필터 체크 변경
    const handleScopeFilterChange = (key) => {
        setScopeFilters((prev) => ({
            ...prev,
            [key]: !prev[key],
        }));
    };

    // 카테고리 필터 체크 변경
    const handleCategoryFilterChange = (key) => {
        setCategoryFilters((prev) => ({
            ...prev,
            [key]: !prev[key],
        }));
    };

    // 멤버 일정 필터 체크 변경
    const handleMemberFilterChange = (id) => {
        setMemberFilters((prev) =>
            prev.map((member) =>
                member.id === id
                    ? { ...member, checked: !member.checked }
                    : member
            )
        );
    };

    // 필터 버튼 클릭 시 드롭다운 열고 닫기
    const handleFilterButtonClick = (filterName) => {
        setOpenFilter((prev) => (prev === filterName ? null : filterName));
    };

    // FullCalendar API 가져오기
    const getCalendarApi = () => {
        return calendarRef.current?.getApi();
    };

    // 이전 달/주/일 이동
    const handlePrevClick = () => {
        const calendarApi = getCalendarApi();

        if (!calendarApi) {
            return;
        }

        calendarApi.prev();
    };

    // 다음 달/주/일 이동
    const handleNextClick = () => {
        const calendarApi = getCalendarApi();

        if (!calendarApi) {
            return;
        }

        calendarApi.next();
    };

    // 오늘로 이동
    const handleTodayClick = () => {
        const calendarApi = getCalendarApi();

        if (!calendarApi) {
            return;
        }

        calendarApi.today();
    };

    // 월/주/일 보기 전환
    const handleViewChange = (viewName) => {
        const calendarApi = getCalendarApi();

        if (!calendarApi) {
            return;
        }

        calendarApi.changeView(viewName);
        setCurrentView(viewName);
    };

    // 캘린더 메인 화면 스타일
    const calendarLayoutStyle = {
        padding: '0',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
    };

    const calendarToolbarStyle = {
        display: 'grid',
        gridTemplateColumns: '1fr auto 1fr',
        alignItems: 'center',
        gap: '12px',
        marginBottom: '0',
        flexShrink: 0,
        minHeight: '56px',
        padding: '16px 12px 8px',
    };

    const calendarToolbarLeftStyle = {
        display: 'flex',
        alignItems: 'center',
        gap: '6px',
        justifyContent: 'flex-start',
    };

    const calendarToolbarRightStyle = {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        gap: '2px',
    };

    const calendarTitleStyle = {
        minWidth: '120px',
        fontSize: '16px',
        fontWeight: '800',
        color: '#0D6EFD',
        whiteSpace: 'nowrap',
    };

    const filterBarStyle = {
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        position: 'relative',
    };

    const toolbarButtonStyle = {
        border: '1px solid #cfe2ff',
        borderRadius: '8px',
        backgroundColor: '#f8fbff',
        color: '#0D6EFD',
        padding: '0 10px',
        fontSize: '12px',
        fontWeight: '700',
        cursor: 'pointer',
        height: '30px',
        lineHeight: '28px',
    };

    const viewButtonStyle = {
        border: '1px solid #cfe2ff',
        backgroundColor: '#fff',
        color: '#0D6EFD',
        padding: '0 10px',
        fontSize: '12px',
        fontWeight: '700',
        cursor: 'pointer',
        height: '30px',
        lineHeight: '28px',
    };

    const activeViewButtonStyle = {
        ...viewButtonStyle,
        backgroundColor: '#0D6EFD',
        color: '#fff',
        border: '1px solid #0D6EFD',
    };

    const filterButtonStyle = {
        border: '1px solid #cfe2ff',
        borderRadius: '999px',
        backgroundColor: '#f8fbff',
        color: '#0D6EFD',
        padding: '0 13px',
        fontSize: '13px',
        fontWeight: '700',
        cursor: 'pointer',
        height: '30px',
        lineHeight: '28px',
    };

    const activeFilterButtonStyle = {
        ...filterButtonStyle,
        border: '2px solid #0D6EFD',
        backgroundColor: '#ffffff',
        boxShadow: '0 0 0 3px rgba(13, 110, 253, 0.12)',
        lineHeight: '26px',
    };

    const filterDropdownStyle = {
        position: 'absolute',
        top: '42px',
        width: '230px',
        padding: '14px',
        border: '1px solid #e5e7eb',
        borderRadius: '12px',
        backgroundColor: '#fff',
        boxShadow: '0 8px 20px rgba(0, 0, 0, 0.12)',
        zIndex: 100,
        color: '#212529',
    };

    const filterOptionStyle = {
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        marginBottom: '8px',
        fontSize: '14px',
    };

    const memberColorDotStyle = {
        width: '9px',
        height: '9px',
        borderRadius: '50%',
        display: 'inline-block',
    };

    const calendarMainStyle = {
        border: 'none',
        borderRadius: '0',
        padding: '0',
        backgroundColor: '#fff',
        flex: 1,
        minHeight: 0,
        overflow: 'hidden',
    };

    const pageStyle = {
        width: 'calc(100% + 32px)',
        margin: '-32px -16px 0 -16px',
        padding: '0',
    };

    return (
        <div style={pageStyle}>
            <style>
                {`
                .calendar-main-area .fc-daygrid-day-top {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    padding-top: 6px;
                }

                .calendar-main-area .fc-daygrid-day-number {
                    width: 100%;
                    text-align: center;
                    padding: 0;
                    text-decoration: none;
                }

                .calendar-main-area .calendar-day-number {
                    display: inline-block;
                    width: 100%;
                    text-align: center;
                    font-size: 15px;
                    font-weight: 600;
                }

                .calendar-main-area .fc-col-header-cell-cushion {
                    display: block;
                    width: 100%;
                    text-align: center;
                    padding: 6px 0;
                    text-decoration: none;
                }

                /* 월간 캘린더 행 높이 고정 */
                .calendar-main-area .fc-daygrid-body,
                .calendar-main-area .fc-daygrid-body table {
                    height: 100% !important;
                }

                .calendar-main-area .fc-daygrid-body tbody {
                    height: 100%;
                }

                .calendar-main-area .fc-daygrid-body tr {
                    height: calc(100% / 6);
                }

                .calendar-main-area .fc-daygrid-day-frame {
                    height: 100%;
                    min-height: 0;
                }

                .calendar-main-area .fc-daygrid-day-events {
                    min-height: 0;
                }

                .calendar-main-area .fc-daygrid-day-bottom {
                    padding: 0 6px 6px;
                }

                /* 더보기 링크 스타일 */
                .calendar-main-area .fc-daygrid-more-link {
                    display: block;
                    margin-top: 2px;
                    padding: 2px 6px;
                    border-radius: 4px;
                    background-color: #f3f4f6;
                    color: #6b7280 !important;
                    font-size: 12px;
                    font-weight: 600;
                    text-decoration: none;
                }
            `}
            </style>

            {successMessage && (
                <div
                    style={{
                        position: 'fixed',
                        top: '90px',
                        left: '50%',
                        transform: 'translateX(-50%)',
                        backgroundColor: '#22c55e',
                        color: '#ffffff',
                        padding: '12px 18px',
                        borderRadius: '10px',
                        fontSize: '14px',
                        fontWeight: '700',
                        boxShadow: '0 10px 24px rgba(34, 197, 94, 0.28)',
                        zIndex: 2000,
                    }}
                >
                    {successMessage}
                </div>
            )}

            <CCard
                className="mb-0"
                style={{
                    width: '100%',
                    height: 'calc(100vh - 115px)',
                    overflow: 'hidden',
                    display: 'flex',
                    flexDirection: 'column',
                    borderRadius: '0',
                    border: 'none',
                    boxShadow: 'none',
                }}
            >
                <CCardBody
                    className="p-0 d-flex flex-column"
                    style={{
                        flex: 1,
                        minHeight: 0,
                        overflow: 'hidden',
                    }}
                >

                    {/* 캘린더 영역 */}
                    <div style={calendarLayoutStyle}>

                        {/* 캘린더 상단 툴바 영역 */}
                        <div style={calendarToolbarStyle}>
                            {/* 왼쪽: 이동 버튼 + 현재 년월 */}
                            <div style={calendarToolbarLeftStyle}>
                                <button
                                    type="button"
                                    style={toolbarButtonStyle}
                                    onClick={handleTodayClick}
                                >
                                    오늘
                                </button>

                                <button
                                    type="button"
                                    style={toolbarButtonStyle}
                                    onClick={handlePrevClick}
                                >
                                    ‹
                                </button>

                                <button
                                    type="button"
                                    style={toolbarButtonStyle}
                                    onClick={handleNextClick}
                                >
                                    ›
                                </button>

                                <span style={calendarTitleStyle}>
                                    {calendarTitle}
                                </span>
                            </div>

                            {/* 가운데: 필터 버튼 */}
                            <div style={filterBarStyle}>
                                {/* 일정 범위 필터 */}
                                <div style={{ position: 'relative' }}>
                                    <button
                                        type="button"
                                        style={openFilter === 'scope' ? activeFilterButtonStyle : filterButtonStyle}
                                        onClick={() => handleFilterButtonClick('scope')}
                                    >
                                        일정 범위 ▾
                                    </button>

                                    {openFilter === 'scope' && (
                                        <div style={{ ...filterDropdownStyle, left: 0 }}>
                                            <strong style={{ display: 'block', marginBottom: '10px' }}>
                                                일정 범위
                                            </strong>

                                            <label style={filterOptionStyle}>
                                                <input
                                                    type="checkbox"
                                                    checked={scopeFilters.my}
                                                    onChange={() => handleScopeFilterChange('my')}
                                                />
                                                내 일정
                                            </label>

                                            <label style={filterOptionStyle}>
                                                <input
                                                    type="checkbox"
                                                    checked={scopeFilters.department}
                                                    onChange={() => handleScopeFilterChange('department')}
                                                />
                                                같은 부서 일정
                                            </label>

                                            <label style={filterOptionStyle}>
                                                <input
                                                    type="checkbox"
                                                    checked={scopeFilters.company}
                                                    onChange={() => handleScopeFilterChange('company')}
                                                />
                                                전사 일정
                                            </label>
                                        </div>
                                    )}
                                </div>

                                {/* 카테고리 필터 */}
                                <div style={{ position: 'relative' }}>
                                    <button
                                        type="button"
                                        style={openFilter === 'category' ? activeFilterButtonStyle : filterButtonStyle}
                                        onClick={() => handleFilterButtonClick('category')}
                                    >
                                        카테고리 ▾
                                    </button>

                                    {openFilter === 'category' && (
                                        <div style={{ ...filterDropdownStyle, left: 0 }}>
                                            <strong style={{ display: 'block', marginBottom: '10px' }}>
                                                카테고리
                                            </strong>

                                            <label style={filterOptionStyle}>
                                                <input
                                                    type="checkbox"
                                                    checked={categoryFilters.meeting}
                                                    onChange={() => handleCategoryFilterChange('meeting')}
                                                />
                                                회의
                                            </label>

                                            <label style={filterOptionStyle}>
                                                <input
                                                    type="checkbox"
                                                    checked={categoryFilters.work}
                                                    onChange={() => handleCategoryFilterChange('work')}
                                                />
                                                업무
                                            </label>

                                            <label style={filterOptionStyle}>
                                                <input
                                                    type="checkbox"
                                                    checked={categoryFilters.notice}
                                                    onChange={() => handleCategoryFilterChange('notice')}
                                                />
                                                공지
                                            </label>

                                            <label style={filterOptionStyle}>
                                                <input
                                                    type="checkbox"
                                                    checked={categoryFilters.etc}
                                                    onChange={() => handleCategoryFilterChange('etc')}
                                                />
                                                기타
                                            </label>
                                        </div>
                                    )}
                                </div>

                                {/* 멤버 일정 필터 */}
                                <div style={{ position: 'relative' }}>
                                    <button
                                        type="button"
                                        style={openFilter === 'member' ? activeFilterButtonStyle : filterButtonStyle}
                                        onClick={() => handleFilterButtonClick('member')}
                                    >
                                        멤버 일정 ▾
                                    </button>

                                    {openFilter === 'member' && (
                                        <div style={{ ...filterDropdownStyle, left: 0, width: '260px' }}>
                                            <strong style={{ display: 'block', marginBottom: '10px' }}>
                                                같은 부서 멤버
                                            </strong>

                                            {memberFilters.map((member) => (
                                                <label key={member.id} style={filterOptionStyle}>
                                                    <input
                                                        type="checkbox"
                                                        checked={member.checked}
                                                        onChange={() => handleMemberFilterChange(member.id)}
                                                    />

                                                    <span
                                                        style={{
                                                            ...memberColorDotStyle,
                                                            backgroundColor: member.color,
                                                        }}
                                                    />

                                                    {member.name}
                                                </label>
                                            ))}

                                            <button
                                                type="button"
                                                style={{
                                                    width: '100%',
                                                    marginTop: '10px',
                                                    padding: '8px 10px',
                                                    border: '1px solid #0D6EFD',
                                                    borderRadius: '8px',
                                                    backgroundColor: '#fff',
                                                    color: '#0D6EFD',
                                                    fontWeight: '700',
                                                    cursor: 'pointer',
                                                }}
                                                onClick={() => alert('조직도 선택 모달 연결 예정')}
                                            >
                                                조직도에서 멤버 추가
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* 오른쪽: 월/주/일 보기 전환 */}
                            <div style={calendarToolbarRightStyle}>
                                <button
                                    type="button"
                                    style={currentView === 'dayGridMonth' ? activeViewButtonStyle : viewButtonStyle}
                                    onClick={() => handleViewChange('dayGridMonth')}
                                >
                                    월
                                </button>

                                <button
                                    type="button"
                                    style={currentView === 'timeGridWeek' ? activeViewButtonStyle : viewButtonStyle}
                                    onClick={() => handleViewChange('timeGridWeek')}
                                >
                                    주
                                </button>

                                <button
                                    type="button"
                                    style={currentView === 'timeGridDay' ? activeViewButtonStyle : viewButtonStyle}
                                    onClick={() => handleViewChange('timeGridDay')}
                                >
                                    일
                                </button>
                            </div>
                        </div>

                        {/* 캘린더 본문 */}
                        <main className="calendar-main-area" style={calendarMainStyle}>
                            <FullCalendar
                                ref={calendarRef}
                                plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
                                initialView="dayGridMonth"
                                locale={koLocale}
                                height="100%"
                                expandRows={true}
                                dayMaxEvents={2}
                                moreLinkContent={(arg) => `${arg.num}개 더보기`}
                                events={draftEvent ? [...calendarEvents, draftEvent] : calendarEvents}
                                eventClick={handleEventClick}
                                dateClick={handleDateClick}
                                headerToolbar={false}
                                datesSet={(info) => {
                                    setCalendarTitle(info.view.title);
                                    setCurrentView(info.view.type);
                                    setCalendarRange({
                                        start: info.start,
                                        end: info.end,
                                    });
                                }}
                                dayCellContent={(arg) => {
                                    return (
                                        <span className="calendar-day-number">
                                            {arg.date.getDate()}
                                        </span>
                                    );
                                }}
                            />
                        </main>
                    </div>
                </CCardBody>
            </CCard>

            {/* 기존 일정 상세/수정/삭제 팝업 */}
            <CalendarDetail
                visible={detailVisible}
                onClose={() => {
                    setSelectedSchedule(null);
                    setDetailVisible(false);
                }}
                schedule={selectedSchedule}
                popupPosition={detailPopupPosition}
                onEdit={(schedule) => {
                    setSelectedSchedule(schedule);
                    setDetailVisible(false);
                    setDetailAddVisible(true);
                }}
                onUpdateSuccess={async () => {
                    await fetchScheduleList();
                    setSuccessMessage('일정이 수정되었습니다.');

                    setTimeout(() => {
                        setSuccessMessage('');
                    }, 2000);
                }}
                onDelete={handleDeleteSchedule}
            />

            {/* 간편등록 퀵 팝업 */}
            <CalendarSimpleAdd
                visible={simpleAddVisible}
                onClose={() => {
                    setDraftEvent(null);
                    setSelectedDateTime(null);
                    setSimpleAddVisible(false);
                }}
                selectedDateProp={selectedDate}
                selectedDateTimeProp={selectedDateTime}
                popupPosition={popupPosition}
                onCreateSuccess={handleCreateSuccess}
                onOpenDetailAdd={handleOpenDetailAdd}
                onDraftChange={handleDraftChange}
            />

            {/* 상세등록 팝업 */}
            <CalendarDetailAdd
                visible={detailAddVisible}
                onClose={() => {
                    setDraftEvent(null);
                    setSelectedDateTime(null);
                    setDetailAddVisible(false);
                }}
                selectedDateProp={selectedDate}
                popupPosition={detailPopupPosition}
                onCreateSuccess={handleCreateSuccess}
                onDraftChange={handleDraftChange}
                popupMode={true}
            />
        </div>
    );
};

export default Calendar;