import React, { useEffect, useRef, useState } from 'react';

// CoreUI 
import { CButton, CCard, CCardBody, CCardHeader, CForm, CFormInput, CFormSelect, CFormTextarea, CModal, CModalHeader, CModalTitle, CModalBody, CModalFooter } from '@coreui/react';

// 페이지 이동
import { Link, useLocation, useNavigate, useOutletContext } from 'react-router-dom';

// 페이지 전체 레이아웃 스타일
import { containerStyle } from 'src/styles/js/demoPageStyle';

// 경로 상수
import { PATH } from 'src/constants/path';
import { request } from 'src/helpers/axios_helper';

// [캘린더] 상세 등록 / 반복 / 참석자 일정 페이지
const CalendarDetailAdd = ({
    visible = true,
    onClose,
    selectedDateProp,
    popupPosition,
    onCreateSuccess,
    onDraftChange,
    popupMode = false,
    mode = 'create',
    initialSchedule = null,
}) => {

    // DefaultLayout.js의 Outlet에서 보낸 userInfo 데이터 받기
    const [userInfo] = useOutletContext();

    // js 코드로 페이지 이동할 때 사용하는 함수
    const navigate = useNavigate();

    // 캘린더에서 선택한 날짜를 URL query string으로 받음
    const location = useLocation();
    const searchParams = new URLSearchParams(location.search);
    const selectedDate = selectedDateProp || searchParams.get('date');

    // 팝업 영역 참조
    // 바깥 클릭 여부 확인하기 위해 실제 상세등록 팝업 DOM을 기억한다.
    const popupRef = useRef(null);

    // 상세/수정 모드 확인
    // edit 모드면 기존 일정 데이터를 채우고, 이후 수정 로직을 호출.
    const isEditMode = mode === 'edit';

    // 일정 등록 권한은 현재 프로젝트의 직급 체계 기준으로 판단한다.
    const TEAM_LEADER_POSITION_NAMES = ['주임', '선임', '책임', '수석'];

    // 로그인 정보의 role 값이 문자열/배열/객체 형태로 올 수 있어서 문자열로 정규화한다.
    const normalizeRole = (roleValue) => {
        if (typeof roleValue === 'string') return roleValue.toUpperCase();

        if (Array.isArray(roleValue) && roleValue.length > 0) {
            return normalizeRole(roleValue[0]);
        }

        if (roleValue && typeof roleValue === 'object') {
            return String(roleValue.roleName || roleValue.authority || roleValue.name || '').toUpperCase();
        }

        return '';
    };

    // userInfo 구조가 화면마다 조금 다를 수 있어서 가능한 직급명 필드를 모두 확인한다.
    const getPositionName = (user) => {
        return user?.position?.positionName || user?.positionName || user?.position_name || '';
    };

    const userRole = normalizeRole(userInfo?.role);
    const positionName = getPositionName(userInfo);

    const isAdmin = userRole.includes('ADMIN');

    // 부서일정은 관리자이거나, 팀장 권한 role이 있거나, 주임 이상 직급이면 선택 가능하다.
    const isTeamLeaderLevel =
        isAdmin ||
        userRole.includes('TEAM_LEADER') ||
        TEAM_LEADER_POSITION_NAMES.some((name) => positionName.includes(name));

    // 구분 select에는 현재 사용자 권한으로 등록 가능한 일정 범위만 보여준다.
    const scheduleTypeOptions = [
        { value: 'PERSONAL', label: '개인일정' },
        ...(isTeamLeaderLevel ? [{ value: 'DEPARTMENT', label: '부서일정' }] : []),
        ...(isAdmin ? [{ value: 'COMPANY', label: '전사일정' }] : []),
    ];

    // 선택 날짜가 있으면 현재 시각 기준 다음 정각으로 기본 시간 설정
    const getDefaultDateTime = (dateStr, plusHour = 0) => {
        if (!dateStr) return '';

        const now = new Date();
        const hour = now.getMinutes() === 0 ? now.getHours() : now.getHours() + 1;
        const targetHour = hour + plusHour;

        const formattedHour = String(targetHour).padStart(2, '0');

        return `${dateStr}T${formattedHour}:00`;
    };

    const defaultStart = getDefaultDateTime(selectedDate, 0);
    const defaultEnd = getDefaultDateTime(selectedDate, 1);

    // 상세등록 닫기
    // 팝업이면 닫기 콜백 쓰고, 페이지면 캘린더로 이동.
    const handleClose = () => {
        if (onClose) {
            onClose();
        } else {
            navigate(PATH.CALENDAR.ROOT);
        }
    };

    // 상세 등록 폼 입력값 관리
    const [formData, setFormData] = useState({
        title: '',
        type: 'PERSONAL',
        category: 'MEETING',
        start: defaultStart,
        end: defaultEnd,
        location: '',
        content: '',
        participants: [],
        repeatRule: '',
        visibility: 'PRIVATE',
    });

    // 참석자 선택 모달 열림/닫힘 상태 관리
    const [participantModalVisible, setParticipantModalVisible] = useState(false);

    // 종일 여부
    const [allDay, setAllDay] = useState(false);

    // 등록 오류 메세지
    // 저장 실패 시 화면에 짧게 보여줌
    const [errorMessage, setErrorMessage] = useState('');

    // 상세등록 폼 초기화
    // create 모드는 새 일정 기본값, edit 모드는 기존 일정값을 폼에 채움.
    useEffect(() => {
        if (!visible) {
            return;
        }

        if (isEditMode && initialSchedule) {
            setFormData({
                title: initialSchedule.title || '',
                type: initialSchedule.type || 'PERSONAL',
                category: initialSchedule.category || 'MEETING',
                start: initialSchedule.startTime || '',
                end: initialSchedule.endTime || '',
                location: initialSchedule.location || '',
                content: initialSchedule.content || '',
                participants: [],
                repeatRule: initialSchedule.repeatRule || '',
                visibility: initialSchedule.isPublic ? 'COMPANY' : 'PRIVATE',
            });

            setAllDay(Boolean(initialSchedule.isAllDay));
            setErrorMessage('');
            setParticipantModalVisible(false);

            if (onDraftChange) {
                onDraftChange(null);
            }

            return;
        }

        if (!selectedDate) {
            return;
        }

        setFormData({
            title: '',
            type: 'PERSONAL',
            category: 'MEETING',
            start: getDefaultDateTime(selectedDate, 0),
            end: getDefaultDateTime(selectedDate, 1),
            location: '',
            content: '',
            participants: [],
            repeatRule: '',
            visibility: 'PRIVATE',
        });

        setAllDay(false);
        setErrorMessage('');
        setParticipantModalVisible(false);

        if (onDraftChange) {
            onDraftChange(null);
        }

    }, [visible, selectedDate, isEditMode, initialSchedule, onDraftChange]);

    // 종일 시간 반영
    // 새 일정 등록 중 종일 체크 시 해당 날짜의 처음부터 끝까지로 시간을 맞춤
    useEffect(() => {
        if (!visible || !selectedDate || isEditMode) {
            return;
        }

        setFormData((prev) => ({
            ...prev,
            start: allDay
                ? `${selectedDate}T00:00`
                : getDefaultDateTime(selectedDate, 0),
            end: allDay
                ? `${selectedDate}T23:59`
                : getDefaultDateTime(selectedDate, 1),
        }));
    }, [visible, selectedDate, allDay, isEditMode]);

    // 일정 입력 미리보기
    // 새 일정 등록 중인 제목/시간만 부모 캘린더에 임시 일정으로 전달한다.
    useEffect(() => {
        if (!visible || !selectedDate || !onDraftChange || isEditMode) {
            return;
        }

        onDraftChange({
            title: formData.title,
            start: formData.start,
            end: formData.end,
            allDay,
        });
    }, [visible, selectedDate, formData.title, formData.start, formData.end, allDay, onDraftChange, isEditMode]);

    // 바깥 클릭 닫기
    // document 전체 클릭을 감지하고, 상세등록 팝업 밖이면 닫는다
    useEffect(() => {
        if (!visible || participantModalVisible) {
            return;
        }

        const handleOutsideClick = (e) => {
            // popupRef.current => 실제 상세등록 팝업 div
            // contains(e.target)이 false면 팝업 바깥을 클릭한 것
            if (popupRef.current && !popupRef.current.contains(e.target)) {
                handleClose();
            }
        };

        document.addEventListener('mousedown', handleOutsideClick);

        return () => {
            document.removeEventListener('mousedown', handleOutsideClick);
        };
    }, [visible, participantModalVisible]);

    // 상세등록 팝업 배경
    const detailOverlayStyle = {
        position: 'fixed',
        inset: 0,
        backgroundColor: 'transparent',
        zIndex: 1040,
        pointerEvents: 'none',
    };

    // 상세등록 팝업 위치
    // top 값에 따라 화면 아래쪽에 남은 높이 계산
    const detailPopupTop = popupPosition?.top || 90;
    const detailPopupLeft = popupPosition?.left || '50%';

    // 상세등록 큰 팝업
    const detailPopupStyle = {
        position: 'fixed',
        top: detailPopupTop,
        left: detailPopupLeft,
        transform: popupPosition ? 'none' : 'translateX(-50%)',
        width: 'min(560px, calc(100vw - 48px))',
        maxHeight: `calc(100vh - ${detailPopupTop}px - 24px)`,
        overflowY: 'auto',
        borderRadius: '14px',
        boxShadow: '0 18px 42px rgba(15, 23, 42, 0.24)',
        backgroundColor: '#ffffff',
        animation: 'calendarDetailPopupIn 0.18s ease-out',
        pointerEvents: 'auto',
    };

    const fieldBlockStyle = {
        marginTop: '14px',
    };

    const compactInputStyle = {
        height: '40px',
        fontSize: '14px',
        backgroundColor: '#ffffff',
        color: '#111827',
        border: '1px solid #d1d5db',
    };

    const selectInputStyle = {
        ...compactInputStyle,
        appearance: 'none',
        backgroundImage:
            'linear-gradient(45deg, transparent 50%, #6b7280 50%), linear-gradient(135deg, #6b7280 50%, transparent 50%)',
        backgroundPosition: 'calc(100% - 18px) 50%, calc(100% - 13px) 50%',
        backgroundSize: '5px 5px, 5px 5px',
        backgroundRepeat: 'no-repeat',
        paddingRight: '34px',
    };


    const titleInputStyle = {
        width: '100%',
        border: 'none',
        borderBottom: '1px solid #d1d5db',
        outline: 'none',
        padding: '10px 0 12px 0',
        fontSize: '16px',
        fontWeight: '700',
        backgroundColor: 'transparent',
        color: '#111827',
        marginBottom: '2px',
    };

    const timeRowStyle = {
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        marginTop: '14px',
    };

    const timeInputStyle = {
        flex: 1,
        ...compactInputStyle,
        colorScheme: 'light',
    };

    const helperTextStyle = {
        fontSize: '12px',
        color: '#6b7280',
    };

    // 참석자 선택 테스트용 같은 부서 구성원 더미 데이터
    const deptMembers = [
        { empId: '20240001', name: '송창범' },
        { empId: '20240002', name: '조민수' },
        { empId: '20240003', name: '김재원' },
    ];

    // 입력값이 변경될 때마다 formData의 해당 항목만 갱신
    const handleChange = (e) => {
        const { name, value } = e.target;

        setFormData((prev) => {
            if (name === 'type') {
                // 화면에 노출되지 않은 구분 값이 임의로 들어오는 경우를 막는다.
                const canSelectType = scheduleTypeOptions.some((option) => option.value === value);

                if (!canSelectType) {
                    return prev;
                }

                // 부서/전사일정은 공개 범위를 사용자가 따로 고르지 않고 정책에 맞게 자동 고정한다.
                // 개인일정만 비공개/공개 선택 가능.
                const nextVisibility =
                    value === 'DEPARTMENT'
                        ? 'DEPARTMENT'
                        : value === 'COMPANY'
                            ? 'COMPANY'
                            : prev.visibility === 'PRIVATE'
                                ? 'PRIVATE'
                                : 'COMPANY';

                return {
                    ...prev,
                    type: value,
                    visibility: nextVisibility,

                    // 반복 일정은 1차 구현에서 개인일정만 허용한다.
                    repeatRule: value === 'PERSONAL' ? prev.repeatRule : '',
                };
            }

            if (name === 'visibility') {
                // 공개 범위 직접 선택은 개인일정에서만 허용한다.
                if (prev.type !== 'PERSONAL') {
                    return prev;
                }

                return {
                    ...prev,
                    // 현재 백엔드는 공개 여부를 isPublic boolean으로 저장하므로,
                    // 개인일정의 "공개"는 내부적으로 COMPANY 값을 사용해 공개 상태로만 구분한다.
                    visibility: value === 'PRIVATE' ? 'PRIVATE' : 'COMPANY',
                };
            }

            return {
                ...prev,
                [name]: value,
            };
        });
    };

    // 종료 시간 자동 계산
    // 문자열 시간을 Date로 바꾼 뒤 1시간을 더해서 다시 input용 문자열로 만든다.
    const addOneHour = (dateTimeValue) => {
        if (!dateTimeValue) return '';

        // ex) 2026-05-20T09:00 -> Date 객체 -> 1시간 추가
        const date = new Date(dateTimeValue);
        date.setHours(date.getHours() + 1);

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');

        return `${year}-${month}-${day}T${hour}:${minute}`;
    };


    // 시작/종료 시간 변경
    // field가 start면 시작/종료를 같이 바꾸고, end면 종료 시간만 바꾼다.
    const handleTimeChange = (field, timeValue) => {
        // 우선 캘린더에서 클릭한 날짜를 사용하고,
        // 없으면 기존 시작/종료값에서 날짜 부분만 가져옴
        const dateValue =
            selectedDate ||
            formData.start?.slice(0, 10) ||
            formData.end?.slice(0, 10);

        if (!dateValue) {
            return;
        }

        // field에는 'start' 또는 'end'가 들어옴
        // ex) start + 09:00 -> 2026-05-20T09:00
        const dateTimeValue = `${dateValue}T${timeValue}`;

        setFormData((prev) => ({
            ...prev,
            [field]: dateTimeValue,

            // 시작 시간을 바꿀 때만 end 값을 추가로 덮어쓴다.
            ...(field === 'start' ? { end: addOneHour(dateTimeValue) } : {}),
        }));
    };


    // 참석자 선택 토글 처리: 선택된 사람은 제거, 선택 안 된 사람은 추가
    const handleParticipantChange = (member) => {
        const isSelected = formData.participants.some(
            (participant) => participant.empId === member.empId
        );

        setFormData({
            ...formData,
            participants: isSelected
                ? formData.participants.filter((participant) => participant.empId !== member.empId)
                : [...formData.participants, member],
        });
    };

    // 상세 일정 등록
    // 입력값 백엔드 DTO 형식으로 바꿔 저장 API 호출
    const handleSubmit = async (e) => {
        e.preventDefault();
        setErrorMessage('');

        if (!formData.title.trim()) {
            setErrorMessage('제목을 입력해 주세요.');
            return;
        }

        if (!userInfo?.empNo) {
            setErrorMessage('로그인 사용자 정보를 확인할 수 없습니다.');
            return;
        }

        // 저장 요청 데이터
        // 화면 state 이름을 백엔드 DTO 필드명에 맞춰 변환한다.
        const payload = {
            title: formData.title.trim(),
            content: formData.content,
            startTime: formData.start,
            endTime: formData.end,
            type: formData.type,
            creatorNo: userInfo.empNo,
            deptId: null,
            category: formData.category,
            location: formData.location,
            isAllDay: allDay,
            isPublic: formData.visibility !== 'PRIVATE',
            repeatRule: formData.type === 'PERSONAL' && formData.repeatRule ? formData.repeatRule : null,
        };

        try {
            // 상세 일정 저장 요청
            // 성공하면 캘린더 메인으로 이동한다.
            await request('POST', '/calendar/create', payload);

            if (onCreateSuccess) {
                await onCreateSuccess();
            } else {
                navigate(PATH.CALENDAR.ROOT);
            }
        } catch (error) {
            console.error('상세 일정 등록 실패:', error);

            // 실패 메시지 표시
            // 서버에서 내려준 메시지가 있으면 우선 사용한다.
            const message = error.response?.data;
            setErrorMessage(
                typeof message === 'string' ? message : '상세 일정 등록에 실패했습니다.'
            );
        }
    };

    if (!visible) {
        return null;
    }

    const wrapperStyle = popupMode ? detailOverlayStyle : containerStyle;
    const cardStyle = popupMode
        ? { ...detailPopupStyle }
        : { height: 'calc(100vh - 120px)' };

    return (
        <div style={wrapperStyle}>
            {/* 상세등록 팝업 입력창 색상 보정 */}
            <style>
                {`
                @keyframes calendarDetailPopupIn {
                    from {
                        opacity: 0;
                        margin-top: -8px;
                    }

                    to {
                        opacity: 1;
                        margin-top: 0;
                    }
                }

                .calendar-detail-popup,

                .calendar-detail-popup .card-header,
                .calendar-detail-popup .card-body {
                    background-color: #ffffff;
                    color: #111827;
                }

                .calendar-detail-popup .form-label,
                .calendar-detail-popup strong,
                .calendar-detail-popup h4 {
                    color: #111827;
                }

                .calendar-detail-popup {
                    border: 1px solid #e5e7eb;
                }

                .calendar-detail-popup .card-header {
                    border-bottom: none;
                    padding: 20px 22px 8px 22px;
                }

                .calendar-detail-popup .card-body {
                    padding: 0;
                }

                .calendar-detail-popup .form-label {
                    margin-top: 12px;
                    margin-bottom: 6px;
                    font-size: 12px;
                    color: #6b7280;
                }

                .calendar-detail-popup .form-control,
                .calendar-detail-popup .form-select {
                    height: 40px;
                    background-color: #ffffff !important;
                    color: #111827 !important;
                    border: 1px solid #d1d5db !important;
                    border-radius: 6px;
                    font-size: 14px;
                    color-scheme: light;
                }

                .calendar-detail-popup textarea.form-control {
                    height: auto;
                    min-height: 96px;
                }


                .calendar-detail-popup .form-control::placeholder {
                    color: #9ca3af;
                }
            `}
            </style>

            {/* 일정 상세 등록 영역 */}
            <CCard
                ref={popupRef}
                className={`calendar-detail-popup ${popupMode ? 'mb-0' : 'mb-4'}`}
                style={cardStyle}
            >

                <CCardHeader style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <strong>일정 상세 등록</strong>

                    <button
                        type="button"
                        onClick={handleClose}
                        style={{
                            border: 'none',
                            background: 'transparent',
                            color: '#9ca3af',
                            fontSize: '20px',
                            cursor: 'pointer',
                        }}
                    >
                        ×
                    </button>
                </CCardHeader>

                <CCardBody className="p-0 d-flex flex-column" style={{ minHeight: 0 }}>
                    {/* 일정 상세 등록 폼 영역 */}
                    <div style={{ padding: '16px 20px 18px 20px' }}>
                        <CForm onSubmit={handleSubmit}>
                            <input
                                name="title"
                                value={formData.title}
                                onChange={handleChange}
                                placeholder="제목을 입력하세요"
                                style={titleInputStyle}
                            />

                            {/* 일정 구분 */}
                            <CFormSelect
                                name="type"
                                value={formData.type}
                                onChange={handleChange}
                                style={{ ...fieldBlockStyle, ...selectInputStyle }}
                            >
                                {scheduleTypeOptions.map((option) => (
                                    <option key={option.value} value={option.value}>
                                        {option.label}
                                    </option>
                                ))}
                            </CFormSelect>

                            {/* 카테고리 */}
                            <CFormSelect
                                name="category"
                                value={formData.category}
                                onChange={handleChange}
                                style={{ ...fieldBlockStyle, ...selectInputStyle }}
                            >
                                <option value="MEETING">회의</option>
                                <option value="WORK">업무</option>
                                <option value="NOTICE">공지</option>
                                <option value="EDUCATION">교육</option>
                                <option value="ETC">기타</option>
                            </CFormSelect>

                            {/* 선택 날짜 */}
                            <div style={{ marginTop: '12px', ...helperTextStyle }}>
                                {selectedDate}
                            </div>

                            {/* 시작/종료 시간 */}
                            <div style={timeRowStyle}>
                                <CFormInput
                                    className="calendar-time-input"
                                    type="time"
                                    value={formData.start ? formData.start.slice(11, 16) : ''}
                                    onChange={(e) => handleTimeChange('start', e.target.value)}
                                    disabled={allDay}
                                    style={timeInputStyle}
                                />

                                <span style={{ color: '#6b7280', fontWeight: '600' }}>~</span>

                                <CFormInput
                                    className="calendar-time-input"
                                    type="time"
                                    value={formData.end ? formData.end.slice(11, 16) : ''}
                                    onChange={(e) => handleTimeChange('end', e.target.value)}
                                    disabled={allDay}
                                    style={timeInputStyle}
                                />
                            </div>

                            {/* 종일 */}
                            <div style={{ marginTop: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                <input
                                    id="detailAllDay"
                                    type="checkbox"
                                    checked={allDay}
                                    onChange={(e) => setAllDay(e.target.checked)}
                                />
                                <label htmlFor="detailAllDay" style={{ fontSize: '13px', color: '#374151', cursor: 'pointer' }}>
                                    종일
                                </label>
                            </div>


                            {/* 장소와 내용은 상세등록에서도 기본 일정 정보로 입력 */}
                            <CFormInput
                                label="장소"
                                name="location"
                                value={formData.location}
                                onChange={handleChange}
                                placeholder="장소를 입력하세요"
                            />

                            {/* 참석자는 직접 입력하지 않고 모달에서 선택 */}
                            <div style={{ marginTop: '16px' }}>
                                <strong>참석자</strong>

                                {/* 선택된 참석자가 있으면 이름을 표시하고, 없으면 안내문구 표시 */}
                                <div style={{ marginTop: '8px', marginBottom: '8px' }}>
                                    {formData.participants.length === 0 ? (
                                        <span style={{ color: '#777' }}>선택된 참석자가 없습니다.</span>
                                    ) : (
                                        formData.participants.map((participant) => (
                                            <span key={participant.empId} style={{ marginRight: '8px' }}>
                                                {participant.name}
                                            </span>
                                        ))
                                    )}
                                </div>

                                {/* 버튼 클릭시 참석자 선택 모달 열기 */}
                                <CButton
                                    color="primary"
                                    variant="outline"
                                    size="sm"
                                    type="button"
                                    onClick={() => setParticipantModalVisible(true)}
                                >
                                    참석자 선택
                                </CButton>
                            </div>

                            <CFormTextarea
                                label="내용"
                                name="content"
                                value={formData.content}
                                onChange={handleChange}
                                placeholder="일정 내용을 입력하세요"
                                rows={4}
                            />

                            {/* 상세등록 확장 항목: 반복, 공개 범위 */}
                            <CFormSelect
                                label="반복"
                                name="repeatRule"
                                value={formData.repeatRule}
                                onChange={handleChange}
                                disabled={formData.type !== 'PERSONAL'}
                                style={selectInputStyle}
                            >
                                <option value="">반복 없음</option>
                                <option value="DAILY">매일</option>
                                <option value="WEEKLY">매주</option>
                                <option value="MONTHLY">매월</option>
                            </CFormSelect>

                            <CFormSelect
                                label="공개 범위"
                                name="visibility"
                                value={formData.visibility}
                                onChange={handleChange}
                                disabled={formData.type !== 'PERSONAL'}
                                style={selectInputStyle}
                            >
                                {/* 개인일정만 비공개/공개를 직접 선택할 수 있다. */}
                                {formData.type === 'PERSONAL' && (
                                    <>
                                        <option value="PRIVATE">비공개</option>
                                        <option value="COMPANY">공개</option>
                                    </>
                                )}

                                {/* 부서일정은 부서 공개로 자동 고정한다. */}
                                {formData.type === 'DEPARTMENT' && (
                                    <option value="DEPARTMENT">부서 공개</option>
                                )}

                                {/* 전사일정은 전사 공개로 자동 고정한다. */}
                                {formData.type === 'COMPANY' && (
                                    <option value="COMPANY">전사 공개</option>
                                )}
                            </CFormSelect>

                            {errorMessage && (
                                <div style={{ marginTop: '16px', color: '#dc3545', fontSize: '14px' }}>
                                    {errorMessage}
                                </div>
                            )}

                            {/* 취소/등록 버튼 영역 */}
                            <div style={{ marginTop: '24px', display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                                <CButton
                                    color="secondary"
                                    variant="outline"
                                    size="sm"
                                    type="button"
                                    onClick={handleClose}
                                >
                                    취소
                                </CButton>

                                <CButton color="primary" size="sm" type="submit">
                                    등록
                                </CButton>
                            </div>
                        </CForm>
                    </div>
                </CCardBody>
            </CCard>

            {/* 참석자 선택 모달 */}
            <CModal
                visible={participantModalVisible}
                onClose={() => setParticipantModalVisible(false)}
                size="lg"
            >
                <CModalHeader>
                    <CModalTitle>멤버 초대하기</CModalTitle>
                </CModalHeader>

                <CModalBody>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: '24px' }}>
                        {/* 왼쪽 : 검색/필터 영역, 현재는 화면 확인용 */}
                        <div>
                            <h6>검색 조건</h6>

                            <CFormInput
                                placeholder="멤버 검색"
                                style={{ marginTop: '12px' }}
                            />

                            <CFormSelect style={{ marginTop: '12px' }}>
                                <option>부서 전체</option>
                                <option>개발팀</option>
                                <option>인사팀</option>
                            </CFormSelect>
                        </div>

                        {/* 오른쪽 : 같은 부서 구성원 목록 */}
                        <div>
                            <h6>팀 멤버</h6>

                            <div style={{ marginTop: '12px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                {deptMembers.map((member) => (
                                    <label key={member.empId}>
                                        <input
                                            type="checkbox"
                                            checked={formData.participants.some(
                                                (participant) => participant.empId === member.empId
                                            )}
                                            onChange={() => handleParticipantChange(member)}
                                        />
                                        {' '}
                                        {member.name} ({member.empId})
                                    </label>
                                ))}
                            </div>

                            {/* 선택된 멤버는 아래에 태그 형태로 표시 */}
                            <div style={{ marginTop: '16px' }}>
                                <strong>선택된 멤버 {formData.participants.length}</strong>

                                <div style={{ marginTop: '8px', display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                                    {formData.participants.length === 0 ? (
                                        <span style={{ color: '#777' }}>선택된 멤버가 없습니다.</span>
                                    ) : (
                                        formData.participants.map((participant) => (
                                            <span
                                                key={participant.empId}
                                                style={{
                                                    display: 'inline-flex',
                                                    alignItems: 'center',
                                                    gap: '6px',
                                                    padding: '6px 10px',
                                                    borderRadius: '6px',
                                                    backgroundColor: '#0D6EFD',
                                                    color: '#fff',
                                                    fontWeight: 'bold',
                                                }}
                                            >
                                                {participant.name}
                                                <button
                                                    type="button"
                                                    onClick={() => handleParticipantChange(participant)}
                                                    style={{
                                                        border: 'none',
                                                        background: 'transparent',
                                                        color: '#fff',
                                                        fontWeight: 'bold',
                                                        cursor: 'pointer',
                                                    }}
                                                >
                                                    ×
                                                </button>
                                            </span>
                                        ))
                                    )}
                                </div>
                            </div>

                        </div>
                    </div>
                </CModalBody>

                <CModalFooter>
                    <CButton
                        color="secondary"
                        variant="outline"
                        type="button"
                        onClick={() => setParticipantModalVisible(false)}
                    >
                        닫기
                    </CButton>

                    {/* 선택은 체크박스 클릭 시 바로 반영되고, 선택완료는 모달만 닫음 */}
                    <CButton
                        color="primary"
                        type="button"
                        onClick={() => setParticipantModalVisible(false)}
                    >
                        선택완료
                    </CButton>
                </CModalFooter>
            </CModal>
        </div>
    );
};

export default CalendarDetailAdd;