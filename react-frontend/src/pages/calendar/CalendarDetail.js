/**
 * @FileName : CalendarDetail.js
 * @Description : 캘린더 일정 상세 조회 및 수정/삭제 팝업
 * @Author : 정준하
 * @Date : 2026. 05. 01
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.01    정준하        최초 생성
 * @ 2026.05.14    김다솜        온보딩 카테고리 대응 (색상 표시 및 라벨 추가)
 */
import React, { useEffect, useRef, useState } from 'react';

import { request } from 'src/helpers/axios_helper';

// CoreUI
import { CButton, CCard, CCardBody, CCardHeader, CFormInput, CFormSelect, CFormTextarea } from '@coreui/react';
import CIcon from '@coreui/icons-react';
import { cilPencil, cilTrash, cilX } from '@coreui/icons';

// 일정 상세/수정 팝업
const CalendarDetail = ({
    visible = false,
    onClose,
    schedule,
    popupPosition,
    onDelete,
    onUpdateSuccess,
}) => {

    // 팝업 영역 참조
    // 바깥 클릭 여부를 확인하기 위해 실제 상세 팝업 DOM을 기억한다.
    const popupRef = useRef(null);

    // 상세 팝업 화면 모드
    // false면 읽기전용 요약, true면 수정 폼으로 전환
    const [editMode, setEditMode] = useState(false);

    // 종일 여부
    // 수정 팝업에서도 상세등록처럼 종일 체크 상태를 따로 관리한다.
    const [allDay, setAllDay] = useState(false);


    // 수정 폼 입력값
    // 기존 일정 데이터를 복사해서 수정 중인 값으로 따로 관리한다.
    const [formData, setFormData] = useState({
        title: '',
        type: 'PERSONAL',
        category: 'MEETING',
        startTime: '',
        endTime: '',
        location: '',
        content: '',
        repeatRule: '',
        visibility: 'PRIVATE',
    });



    // 팝업 상태 초기화
    // 다른 일정을 다시 열면 항상 읽기 전용 요약 화면부터 보여줌.
    useEffect(() => {
        if (visible && schedule) {
            setEditMode(false);

            setAllDay(Boolean(schedule.isAllDay));

            // 반복 일정 수정 기준 시간
            // 화면에 자동 표시된 반복 날짜가 아니라 DB에 저장된 원본 일정 시간을 수정 기준 사용.
            const editStartTime = schedule.isRepeatGenerated
                ? schedule.originalStartTime
                : schedule.startTime || schedule.start || '';

            const editEndTime = schedule.isRepeatGenerated
                ? schedule.originalEndTime
                : schedule.endTime || schedule.end || '';

            setFormData({
                title: schedule.title || '',
                type: schedule.type || 'PERSONAL',
                category: schedule.category || 'MEETING',
                startTime: editStartTime || '',
                endTime: editEndTime || '',
                location: schedule.location || '',
                content: schedule.content || '',
                repeatRule: schedule.repeatRule || '',
                visibility: schedule.isPublic ? 'COMPANY' : 'PRIVATE',
            });
        }
    }, [visible, schedule]);

    // 바깥 클릭 닫기
    // 상세 팝업 밖을 클릭하면 팝업을 닫는다.
    useEffect(() => {
        if (!visible) {
            return;
        }

        const handleOutsideClick = (e) => {
            if (popupRef.current && !popupRef.current.contains(e.target)) {
                onClose?.();
            }
        };

        document.addEventListener('mousedown', handleOutsideClick);

        return () => {
            document.removeEventListener('mousedown', handleOutsideClick);
        };
    }, [visible, onClose]);

    // 수정 폼 입력 변경
    // input의 name과 formData key를 맞춰두면 한 함수로 입력값을 바꿀 수 있다.
    const handleChange = (e) => {
        const { name, value } = e.target;

        setFormData((prev) => {
            // 반복 일정은 개인일정만 허용한다.
            if (name === 'type' && value !== 'PERSONAL') {
                return {
                    ...prev,
                    type: value,
                    repeatRule: '',
                };
            }

            return {
                ...prev,
                [name]: value,
            };
        });
    };

    // 일정 날짜 추출
    // 서버에서 온 datetime 값에서 날짜(YYYY-MM-DD)만 화면 표시용으로 꺼낸다.
    const getDateText = (dateTimeValue) => {
        if (!dateTimeValue) {
            return '';
        }

        return dateTimeValue.slice(0, 10);
    };

    // 일정 시간 추출
    // 서버에서 온 datetime 값에서 시간(HH:mm)만 input type="time"에 맞게 꺼낸다.
    const getTimeText = (dateTimeValue) => {
        if (!dateTimeValue) {
            return '';
        }

        return dateTimeValue.slice(11, 16);
    };

    // 일정 표시 시간 포맷
    // 서버/FullCalendar datetime 값을 사용자가 보기 쉬운 방식으로 바꿈
    const getDisplayTimeText = (dateTimeValue) => {
        if (!dateTimeValue) {
            return '';
        }

        const timeText = dateTimeValue.slice(11, 16);
        const hour = Number(timeText.slice(0, 2));
        const minute = timeText.slice(3, 5);
        const period = hour < 12 ? '오전' : '오후';
        const displayHour = hour % 12 === 0 ? 12 : hour % 12;

        return `${period} ${String(displayHour).padStart(2, '0')}:${minute}`;
    };

    const getDisplayDateTimeRange = (startValue, endValue) => {
        if (!startValue || !endValue) {
            return '-';
        }

        const startDate = getDateText(startValue);
        const endDate = getDateText(endValue);
        const startTime = getDisplayTimeText(startValue);
        const endTime = getDisplayTimeText(endValue);

        if (startDate === endDate) {
            return `${startDate} ${startTime} ~ ${endTime}`;
        }

        return `${startDate} ${startTime} ~ ${endDate} ${endTime}`;
    };

    // 일정 코드 표시명
    // DB 코드값을 상세 팝업에서 읽기 쉬운 한글 이름으로 보여준다.
    const getCategoryLabel = (category) => {
        const labels = {
            MEETING: '회의',
            WORK: '업무',
            NOTICE: '공지',
            EDUCATION: '교육',
            ONBOARDING: '온보딩',
            ETC: '기타',
        };

        return labels[category] || category || '회의';
    };

    const getTypeLabel = (type) => {
        const labels = {
            PERSONAL: '개인일정',
            DEPARTMENT: '부서일정',
            COMPANY: '전사일정',
        };

        return labels[type] || type || '개인일정';
    };

    // 백엔드 LocalDateTime 요청 형식으로 변환
    // FUllcalendar 값에 붙는 +09:00 시간대 정보는 LocalDateTime이 받을 수 없어서 제거한다.
    const toRequestDateTime = (dateTimeValue) => {
        if (!dateTimeValue) {
            return '';
        }

        return dateTimeValue.slice(0, 19);
    };

    // 종료 시간 자동 계산
    // 시작 시간을 바꾸면 종료 시간을 시작 + 1시간으로 맞춘다.
    const addOneHour = (dateTimeValue) => {
        if (!dateTimeValue) return '';

        const date = new Date(dateTimeValue);
        date.setHours(date.getHours() + 1);

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');

        return `${year}-${month}-${day}T${hour}:${minute}`;
    };

    // 수정 시간 변경
    // 날짜는 유지하고 시간만 바꿔서 startTime/endTime을 다시 만든다.
    const handleTimeChange = (field, timeValue) => {
        const dateValue = getDateText(formData.startTime || formData.endTime);

        if (!dateValue) {
            return;
        }

        const dateTimeValue = `${dateValue}T${timeValue}`;

        setFormData((prev) => ({
            ...prev,
            [field]: dateTimeValue,
            ...(field === 'startTime' ? { endTime: addOneHour(dateTimeValue) } : {}),
        }));
    };

    // 일정 수정 저장
    // 수정 팝업의 입력값을 백엔드 PUT API로 보낸다.
    const handleUpdateSubmit = async () => {
        if (!formData.title.trim()) {
            return;
        }

        const payload = {
            title: formData.title.trim(),
            startTime: toRequestDateTime(formData.startTime),
            endTime: toRequestDateTime(formData.endTime),
            location: formData.location,
            content: formData.content,
            type: formData.type,
            category: formData.category,
            isAllDay: allDay,
            isPublic: formData.visibility !== 'PRIVATE',
            repeatRule: formData.type === 'PERSONAL' && formData.repeatRule ? formData.repeatRule : null,
        };

        try {
            await request('PUT', `/calendar/${schedule.scheduleId}`, payload);

            await onUpdateSuccess?.();
            onClose?.();
        } catch (error) {
            console.error('일정 수정 실패:', error);
        }
    };

    if (!visible || !schedule) {
        return null;
    }

    // 수정 폼은 상세등록처럼 큰 팝업이라 상단 기준을 고정해 높이를 확보한다.
    const detailPopupTop = editMode ? 80 : (popupPosition?.top || 90);
    const detailPopupLeft = popupPosition?.left || '50%';

    const detailPopupStyle = {
        position: 'fixed',
        top: detailPopupTop,
        left: detailPopupLeft,
        transform: popupPosition ? 'none' : 'translateX(-50%)',
        // 읽기 전용 요약은 작게, 수정 폼은 상세등록 팝업과 같은 크기로 보여준다.
        width: editMode
            ? 'min(560px, calc(100vw - 48px))'
            : 'min(360px, calc(100vw - 48px))',
        maxHeight: `calc(100vh - ${detailPopupTop}px - 24px)`,
        overflowY: 'auto',
        borderRadius: '14px',
        boxShadow: '0 18px 42px rgba(15, 23, 42, 0.24)',
        backgroundColor: '#ffffff',
        zIndex: 1060,
        animation: 'calendarDetailPopupIn 0.18s ease-out',
        pointerEvents: 'auto',
    };

    const iconButtonStyle = {
        position: 'relative',
        width: '28px',
        height: '28px',
        border: 'none',
        borderRadius: '6px',
        background: 'transparent',
        color: '#9ca3af',
        cursor: 'pointer',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
    };

    const editInputStyle = {
        width: '100%',
        height: '40px',
        border: '1px solid #d1d5db',
        borderRadius: '6px',
        padding: '8px 10px',
        fontSize: '14px',
        color: '#111827',
        backgroundColor: '#ffffff',
    };

    const selectInputStyle = {
        ...editInputStyle,
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
    };

    const timeRowStyle = {
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        marginTop: '14px',
    };

    const timeInputStyle = {
        flex: 1,
        ...editInputStyle,
        colorScheme: 'light',
    };

    const fieldBlockStyle = {
        marginTop: '14px',
    };

    const helperTextStyle = {
        fontSize: '12px',
        color: '#6b7280',
    };

    return (
        <>
            <style>
                {`
                        .calendar-detail-action-button:hover {
                            background-color: #f3f4f6;
                            color: #4b5563;
                        }

                        .calendar-detail-action-button:hover .calendar-detail-tooltip {
                            opacity: 1;
                            visibility: visible;
                            transform: translateX(-50%) translateY(0);
                        }

                        .calendar-detail-tooltip {
                            position: absolute;
                            top: 34px;
                            left: 50%;
                            transform: translateX(-50%) translateY(-4px);
                            white-space: nowrap;
                            padding: 6px 8px;
                            border-radius: 5px;
                            background-color: #111827;
                            color: #ffffff;
                            font-size: 12px;
                            font-weight: 600;
                            opacity: 0;
                            visibility: hidden;
                            pointer-events: none;
                            transition: 0.14s ease;
                            z-index: 1080;
                        }

                        .calendar-detail-popup,
                        .calendar-detail-popup .card-header,
                        .calendar-detail-popup .card-body {
                            background-color: #ffffff;
                            color: #111827;
                        }

                        .calendar-detail-popup .form-label,
                        .calendar-detail-popup strong {
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


                        @keyframes calendarDetailPopupIn {

                        from {
                            opacity: 0;
                            margin-top: -6px;
                        }

                        to {
                            opacity: 1;
                            margin-top: 0;
                        }
                    }
                `}
            </style>

            <CCard ref={popupRef} className="calendar-detail-popup" style={detailPopupStyle}>

                <CCardHeader
                    style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                    }}
                >
                    {/* 일정 색상 표시 */}
                    {/* 수정 모드는 상세등록 팝업처럼 제목형 헤더로 보여준다. */}
                    {editMode ? (
                        <strong>일정 수정</strong>
                    ) : (
                        <span
                            style={{
                                width: '12px',
                                height: '12px',
                                borderRadius: '3px',
                                backgroundColor: schedule.category === 'ONBOARDING' ? '#2e7d32' : '#0D6EFD',
                                display: 'inline-block',
                            }}
                        />
                    )}

                    {/* 상세 팝업 액션 */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        {editMode ? (
                            <button
                                type="button"
                                aria-label="닫기"
                                onClick={onClose}
                                style={iconButtonStyle}
                                className="calendar-detail-action-button"
                            >
                                <CIcon icon={cilX} size="sm" />
                                <span className="calendar-detail-tooltip">닫기</span>
                            </button>
                        ) : (
                            <>
                                <button
                                    type="button"
                                    aria-label="수정"
                                    onClick={() => setEditMode(true)}
                                    style={iconButtonStyle}
                                    className="calendar-detail-action-button"
                                >
                                    <CIcon icon={cilPencil} size="sm" />
                                    <span className="calendar-detail-tooltip">일정 수정</span>
                                </button>

                                <button
                                    type="button"
                                    aria-label="삭제"
                                    onClick={() => onDelete?.(schedule)}
                                    style={iconButtonStyle}
                                    className="calendar-detail-action-button"
                                >
                                    <CIcon icon={cilTrash} size="sm" />
                                    <span className="calendar-detail-tooltip">일정 삭제</span>
                                </button>

                                <button
                                    type="button"
                                    aria-label="닫기"
                                    onClick={onClose}
                                    style={iconButtonStyle}
                                    className="calendar-detail-action-button"
                                >
                                    <CIcon icon={cilX} size="sm" />
                                    <span className="calendar-detail-tooltip">닫기</span>
                                </button>
                            </>
                        )}
                    </div>
                </CCardHeader>

                <CCardBody
                    className={editMode ? 'p-0 d-flex flex-column' : ''}
                    style={editMode ? { minHeight: 0 } : { padding: '12px 24px 24px 24px' }}
                >
                    {!editMode ? (
                        <>
                            {/* 일정 요약 정보 */}
                            <div style={{ fontSize: '18px', fontWeight: '800', color: '#111827' }}>
                                {schedule.title || '(제목 없음)'}
                            </div>

                            <div style={{ marginTop: '10px', fontSize: '14px', color: '#374151' }}>
                                {getDisplayDateTimeRange(
                                    schedule.startTime || schedule.start,
                                    schedule.endTime || schedule.end
                                )}
                            </div>

                            {schedule.repeatRule && (
                                <div
                                    style={{
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        marginTop: '10px',
                                        padding: '4px 8px',
                                        borderRadius: '999px',
                                        backgroundColor: '#e7f1ff',
                                        color: '#0D6EFD',
                                        fontSize: '12px',
                                        fontWeight: '700',
                                    }}
                                >
                                    반복일정
                                </div>
                            )}

                            <div style={{ marginTop: '14px', borderTop: '1px solid #e5e7eb' }} />

                            <div style={{ marginTop: '14px', fontSize: '14px', color: '#374151' }}>
                                <strong style={{ marginRight: '8px' }}>장소</strong>
                                {schedule.location || '장소 없음'}
                            </div>

                            <div style={{ marginTop: '14px', borderTop: '1px solid #e5e7eb' }} />

                            <div style={{ marginTop: '14px', fontSize: '14px', color: '#374151' }}>
                                <strong style={{ marginRight: '8px' }}>분류</strong>
                                {getCategoryLabel(schedule.category)}
                            </div>

                            <div style={{ marginTop: '14px', borderTop: '1px solid #e5e7eb' }} />

                            <div style={{ marginTop: '14px', fontSize: '14px', color: '#374151' }}>
                                <strong style={{ marginRight: '8px' }}>구분</strong>
                                {getTypeLabel(schedule.type)}
                            </div>

                            {schedule.content && (
                                <>
                                    <div style={{ marginTop: '14px', borderTop: '1px solid #e5e7eb' }} />

                                    <div style={{ marginTop: '14px', fontSize: '14px', color: '#374151', lineHeight: 1.6 }}>
                                        {schedule.content}
                                    </div>
                                </>
                            )}
                        </>
                    ) : (
                        <div style={{ padding: '16px 20px 18px 20px' }}>
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
                                <option value="PERSONAL">개인일정</option>
                                <option value="DEPARTMENT">부서일정</option>
                                <option value="COMPANY">전사일정</option>
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

                            <div style={{ marginTop: '12px', ...helperTextStyle }}>
                                {getDateText(formData.startTime)}
                            </div>

                            <div style={timeRowStyle}>
                                <CFormInput
                                    className="calendar-time-input"
                                    type="time"
                                    value={getTimeText(formData.startTime)}
                                    onChange={(e) => handleTimeChange('startTime', e.target.value)}
                                    disabled={allDay}
                                    style={timeInputStyle}
                                />

                                <span style={{ color: '#6b7280', fontWeight: '600' }}>~</span>

                                <CFormInput
                                    className="calendar-time-input"
                                    type="time"
                                    value={getTimeText(formData.endTime)}
                                    onChange={(e) => handleTimeChange('endTime', e.target.value)}
                                    disabled={allDay}
                                    style={timeInputStyle}
                                />

                            </div>

                            <div style={{ marginTop: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                <input
                                    id="editAllDay"
                                    type="checkbox"
                                    checked={allDay}
                                    onChange={(e) => setAllDay(e.target.checked)}
                                />
                                <label htmlFor="editAllDay" style={{ fontSize: '13px', color: '#374151', cursor: 'pointer' }}>
                                    종일
                                </label>
                            </div>

                            {/* 장소 */}
                            <CFormInput
                                label="장소"
                                name="location"
                                value={formData.location}
                                onChange={handleChange}
                                placeholder="장소를 입력하세요"
                            />

                            <div style={{ marginTop: '16px' }}>
                                <strong>참석자</strong>
                                <div style={{ marginTop: '8px', marginBottom: '8px', color: '#777' }}>
                                    선택된 참석자가 없습니다.
                                </div>
                                <CButton
                                    color="primary"
                                    variant="outline"
                                    size="sm"
                                    type="button"
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
                            {/* 상세 수정 확장 항목: 반복, 공개 범위 */}
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
                                style={selectInputStyle}
                            >
                                <option value="PRIVATE">비공개</option>
                                <option value="DEPARTMENT">부서 공개</option>
                                <option value="COMPANY">전사 공개</option>
                            </CFormSelect>

                            <div style={{ marginTop: '18px', display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                                <CButton
                                    color="secondary"
                                    variant="outline"
                                    size="sm"
                                    type="button"
                                    onClick={() => setEditMode(false)}
                                >
                                    취소
                                </CButton>

                                <CButton
                                    color="primary"
                                    size="sm"
                                    type="button"
                                    onClick={handleUpdateSubmit}
                                >
                                    수정 저장
                                </CButton>
                            </div>
                        </div>
                    )}
                </CCardBody>

            </CCard >
        </>
    );
};

export default CalendarDetail;
