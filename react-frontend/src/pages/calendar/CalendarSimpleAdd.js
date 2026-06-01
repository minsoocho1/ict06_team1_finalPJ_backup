import React, { useEffect, useRef, useState } from 'react';

// CoreUI 
import { CButton, CForm, CFormInput, CFormSelect, CModal, CModalHeader, CModalTitle, CModalBody, CModalFooter } from '@coreui/react';

// 페이지 이동
import { useLocation, useNavigate, useOutletContext } from 'react-router-dom';

import { PATH } from 'src/constants/path';
import { request } from 'src/helpers/axios_helper';

// [캘린더] 일정 간단 등록 페이지
const CalendarSimpleAdd = ({
    visible,
    onClose,
    selectedDateProp,
    selectedDateTimeProp,
    popupPosition,
    onCreateSuccess,
    onOpenDetailAdd,
    onDraftChange,
    onError,
}) => {

    // js 코드로 페이지 이동할 때 사용하는 함수
    const navigate = useNavigate();
    const [userInfo] = useOutletContext();

    // 캘린더에서 선택된 날짜를 URL query string으로 받음
    const location = useLocation();
    const searchParams = new URLSearchParams(location.search);

    // 캘린더에서 날짜를 넘겨주면 그 날짜 사용, 없으면 URL의 ?date= 값 사용
    const selectedDate = selectedDateProp || searchParams.get('date');

    // 팝업 영역 참조
    // 바깥 클릭 여부를 확인하기 위해 실제 팝업 DOM을 기억한다.
    const popupRef = useRef(null);

    // 퀵 팝업으로 열렸으면 팝업만 닫고,
    // 단독 페이지로 접근한 경우에는 캘린더 메인으로 이동
    const handleClose = () => {
        resetSimpleForm();

        if (onClose) {
            onClose();
        } else {
            navigate(PATH.CALENDAR.ROOT);
        }
    };

    // 상세등록 팝업 전환
    // 간편등록 입력값을 비우고 부모 캘린더에 큰 팝업 열기를 요청.
    const handleDetailAddClick = () => {
        resetSimpleForm();

        if (onOpenDetailAdd) {
            onOpenDetailAdd();
        } else {
            navigate(`${PATH.CALENDAR.DETAIL_ADD}?date=${selectedDate || ''}`);
        }
    };

    // 선택 날짜가 있으면 현재 시각 기준 다음 정각으로 기본 시간 설정
    const getDefaultDateTime = (dateStr, plusHour = 0) => {
        if (!dateStr) return '';

        const now = new Date();
        const hour = now.getMinutes() === 0 ? now.getHours() : now.getHours() + 1;
        const targetHour = hour + plusHour;

        const formattedHour = String(targetHour).padStart(2, '0');

        return `${dateStr}T${formattedHour}:00`;
    };

    // 종료 시간 자동 계산
    // 시작 시간을 바꾸면 종료 시간을 시작 시간 +1시간 배정
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

    // 기본 시작/종료 시간
    // 주/일 보기에서 클릭한 시간이 있으면 그 시간을 우선 사용.
    const getInitialDateTime = (plusHour = 0) => {
        if (selectedDateTimeProp) {
            return plusHour === 0
                ? selectedDateTimeProp
                : addOneHour(selectedDateTimeProp);
        }

        return getDefaultDateTime(selectedDate, plusHour);
    };

    const defaultStart = getInitialDateTime(0);
    const defaultEnd = getInitialDateTime(1);

    // userInfo 구조가 화면마다 달라질 수 있어서 가능한 부서 ID 필드를 모두 확인한다.
    const getUserDeptId = (user) => {
        return user?.department?.deptId || user?.dept?.deptId || user?.deptId || user?.dept_id || null;
    };

    // 퀵 팝업 초기화
    // 닫을 때 이전 입력값을 비운다
    const resetSimpleForm = () => {
        setFormData({
            title: '',
            type: 'PERSONAL',
            category: 'MEETING',
            start: getInitialDateTime(0),
            end: getInitialDateTime(1),
            location: '',
            content: '',
            participants: [],
        });
        setAllDay(false);
        setErrorMessage('');
        setParticipantModalVisible(false);

        if (onDraftChange) {
            onDraftChange(null);
        }
    };

    // 간단 등록 폼 입력값 관리
    const [formData, setFormData] = useState({
        title: '',
        type: 'PERSONAL',
        category: 'MEETING',
        start: defaultStart,
        end: defaultEnd,
        location: '',
        content: '',
        participants: [],
    });

    // 참석자 선택 모달 상태
    const [participantModalVisible, setParticipantModalVisible] = useState(false);

    // 참석자 모달 검색/필터 상태
    const [participantSearchKeyword, setParticipantSearchKeyword] = useState('');

    // 참석자 목록 -> 기본은 같은 부서 구성원, 조직도 모드에서는 선택한 부서 구성원을 보여준다.
    const [deptMembers, setDeptMembers] = useState([]);
    const [participantLoading, setParticipantLoading] = useState(false);
    const [participantLoadError, setParticipantLoadError] = useState('');

    // 참석자 모달 내부에서 팀 멤버 보기와 조직도 선택 보기를 전환한다.
    const [participantViewMode, setParticipantViewMode] = useState('TEAM');
    const [orgDepartments, setOrgDepartments] = useState([]);
    const [orgDeptId, setOrgDeptId] = useState('');
    const [orgMembers, setOrgMembers] = useState([]);
    const [orgLoading, setOrgLoading] = useState(false);
    const [orgLoadError, setOrgLoadError] = useState('');

    // 종일 여부
    const [allDay, setAllDay] = useState(false);

    // 등록 오류 메세지
    const [errorMessage, setErrorMessage] = useState('');

    // 날짜/종일 상태에 따른 시간 세팅
    useEffect(() => {
        if (!visible || !selectedDate) {
            return;
        }

        setFormData((prev) => ({
            ...prev,
            type: 'PERSONAL',
            start: allDay
                ? `${selectedDate}T00:00`
                : getInitialDateTime(0),
            end: allDay
                ? `${selectedDate}T23:59`
                : getInitialDateTime(1),
        }));
    }, [visible, selectedDate, selectedDateTimeProp, allDay]);

    // 바깥 클릭 닫기
    // document 전체 클릭을 감지한 뒤, 클릭 위치가 팝업 밖이면 팝업을 닫는다.
    useEffect(() => {
        // 참석자 모달이 떠 있을 때는 모달 클릭을 간단등록 팝업 바깥 클릭으로 오인하지 않게 막는다.
        if (!visible || participantModalVisible) {
            return;
        }

        const handleOutsideClick = (e) => {
            // popupRef.current => 실제 팝업 div
            // contains(e.target)이 false면 팝업 바깥 클릭한 것
            if (popupRef.current && !popupRef.current.contains(e.target)) {
                handleClose();
            }
        };

        document.addEventListener('mousedown', handleOutsideClick);

        return () => {
            document.removeEventListener('mousedown', handleOutsideClick);
        };
    }, [visible, participantModalVisible]);

    // 일정 입력 미리보기
    // 입력 중인 제목/시간을 부모 캘린더에 임시 일정으로 전달.
    useEffect(() => {
        if (!visible || !selectedDate || !onDraftChange) {
            return;
        }

        onDraftChange({
            title: formData.title,
            start: formData.start,
            end: formData.end,
            allDay,
        });
    }, [visible, selectedDate, formData.title, formData.start, formData.end, allDay, onDraftChange]);

    // 참석자 선택 모달이 열릴 때 같은 부서 구성원을 실제 조직도 API에서 불러온다.
    useEffect(() => {
        if (!participantModalVisible) {
            return;
        }

        const deptId = getUserDeptId(userInfo);

        if (!deptId) {
            setDeptMembers([]);
            setParticipantLoadError('소속 부서 정보를 확인할 수 없습니다.');
            return;
        }

        const fetchDeptMembers = async () => {
            setParticipantLoading(true);
            setParticipantLoadError('');

            try {
                const response = await request('GET', '/api/organization/employees', { deptId });

                const members = (response.data || [])
                    .filter((employee) => String(employee.empNo) !== String(userInfo?.empNo))
                    .map((employee) => ({
                        empId: employee.empNo,
                        name: employee.name,
                        deptId: employee.deptId,
                        deptName: employee.deptName,
                        positionName: employee.positionName,
                        profileImg: employee.profileImg,
                    }));

                setDeptMembers(members);
            } catch (error) {
                console.error('참석자 목록 조회 실패:', error);
                setDeptMembers([]);
                setParticipantLoadError('참석자 목록을 불러오지 못했습니다.');
            } finally {
                setParticipantLoading(false);
            }
        };

        fetchDeptMembers();
    }, [participantModalVisible, userInfo]);

    // 참석자 선택은 같은 부서 멤버를 기본 범위로 두고, 현재 단계에서는 이름/사번 검색만 제공한다.
    const filteredDeptMembers = deptMembers.filter((member) => {
        const keyword = participantSearchKeyword.trim().toLowerCase();

        return (
            !keyword ||
            member.name.toLowerCase().includes(keyword) ||
            String(member.empId).toLowerCase().includes(keyword)
        );
    });

    // 참석자 선택값은 유지하고, 모달을 닫을 때 검색/조직도 화면 상태만 초기화한다.
    const closeParticipantModal = () => {
        setParticipantModalVisible(false);
        setParticipantSearchKeyword('');
        setParticipantViewMode('TEAM');
        setOrgDeptId('');
        setOrgMembers([]);
        setOrgLoadError('');
    };

    // 조직도 선택 모드로 전환한다. 같은 모달 안에서 화면만 바꿔 이중 모달을 피한다.
    const openParticipantOrgMode = () => {
        setParticipantViewMode('ORG');
        setParticipantSearchKeyword('');
    };

    // 팀 멤버 모드로 돌아간다.
    const openParticipantTeamMode = () => {
        setParticipantViewMode('TEAM');
        setOrgLoadError('');
    };

    // 조직도 모드가 열리면 부서 트리를 불러온다.
    useEffect(() => {
        if (!participantModalVisible || participantViewMode !== 'ORG') {
            return;
        }

        const fetchOrgDepartments = async () => {
            setOrgLoading(true);
            setOrgLoadError('');

            try {
                const response = await request('GET', '/api/organization/departments/tree');
                setOrgDepartments(response.data || []);
            } catch (error) {
                console.error('조직도 부서 목록 조회 실패:', error);
                setOrgDepartments([]);
                setOrgLoadError('조직도 부서 목록을 불러오지 못했습니다.');
            } finally {
                setOrgLoading(false);
            }
        };

        fetchOrgDepartments();
    }, [participantModalVisible, participantViewMode]);

    // 조직도에서 부서를 선택하면 해당 부서 구성원을 불러온다.
    useEffect(() => {
        if (!participantModalVisible || participantViewMode !== 'ORG' || !orgDeptId) {
            return;
        }

        const fetchOrgMembers = async () => {
            setOrgLoading(true);
            setOrgLoadError('');

            try {
                const response = await request('GET', '/api/organization/employees', {
                    deptId: orgDeptId,
                });

                const members = (response.data || [])
                    .filter((employee) => String(employee.empNo) !== String(userInfo?.empNo))
                    .map((employee) => ({
                        empId: employee.empNo,
                        name: employee.name,
                        deptId: employee.deptId,
                        deptName: employee.deptName,
                        positionName: employee.positionName,
                        profileImg: employee.profileImg,
                    }));

                setOrgMembers(members);
            } catch (error) {
                console.error('조직도 구성원 목록 조회 실패:', error);
                setOrgMembers([]);
                setOrgLoadError('조직도 구성원 목록을 불러오지 못했습니다.');
            } finally {
                setOrgLoading(false);
            }
        };

        fetchOrgMembers();
    }, [participantModalVisible, participantViewMode, orgDeptId, userInfo?.empNo]);

    // 조직도 부서 트리를 같은 모달 안에 렌더링한다.
    // 부서 버튼 안에 하위 부서 버튼이 들어가지 않도록 div로 감싸서 클릭 꼬임을 막는다.
    const renderParticipantOrgDepartments = (departments, depth = 0) => {
        return departments.map((department) => (
            <div key={department.deptId}>
                <button
                    type="button"
                    onClick={() => setOrgDeptId(String(department.deptId))}
                    style={{
                        width: '100%',
                        padding: '7px 8px',
                        paddingLeft: `${8 + depth * 14}px`,
                        border: 'none',
                        borderRadius: '7px',
                        backgroundColor: String(orgDeptId) === String(department.deptId) ? '#eef2ff' : '#fff',
                        color: String(orgDeptId) === String(department.deptId) ? '#4f46e5' : '#374151',
                        fontSize: '12px',
                        fontWeight: String(orgDeptId) === String(department.deptId) ? '800' : '600',
                        textAlign: 'left',
                        cursor: 'pointer',
                    }}
                >
                    {department.children?.length > 0 ? '▾ ' : '• '}
                    {department.deptName}
                </button>

                {department.children?.length > 0 && renderParticipantOrgDepartments(department.children, depth + 1)}
            </div>
        ));
    };

    // 조직도 모드에서도 멤버 검색어를 적용한다.
    const filteredOrgMembers = orgMembers.filter((member) => {
        const keyword = participantSearchKeyword.trim().toLowerCase();

        return (
            !keyword ||
            member.name.toLowerCase().includes(keyword) ||
            String(member.empId).toLowerCase().includes(keyword)
        );
    });

    // 현재 모드에 따라 오른쪽 목록에 보여줄 참석자 후보를 정한다.
    const visibleParticipantMembers =
        participantViewMode === 'ORG'
            ? filteredOrgMembers
            : filteredDeptMembers;

    const participantListLoading =
        participantViewMode === 'ORG'
            ? orgLoading
            : participantLoading;

    const participantListError =
        participantViewMode === 'ORG'
            ? orgLoadError
            : participantLoadError;

    // 팀 멤버/조직도 멤버 모두 같은 방식으로 현재 목록 전체 선택을 처리한다.
    const handleSelectAllVisibleParticipants = (checked) => {
        setFormData((prev) => {
            const visibleIds = new Set(visibleParticipantMembers.map((member) => member.empId));

            if (!checked) {
                return {
                    ...prev,
                    participants: prev.participants.filter(
                        (participant) => !visibleIds.has(participant.empId)
                    ),
                };
            }

            const selectedIds = new Set(prev.participants.map((participant) => participant.empId));
            const nextParticipants = [...prev.participants];

            visibleParticipantMembers.forEach((member) => {
                if (!selectedIds.has(member.empId)) {
                    nextParticipants.push(member);
                }
            });

            return {
                ...prev,
                participants: nextParticipants,
            };
        });
    };

    // 입력값이 변경될 때마다 formData의 해당 항목만 갱신
    const handleChange = (e) => {
        const { name, value } = e.target;

        setFormData({
            ...formData,
            [name]: value,
        });
    };

    // 시작/종료 시간 변경
    // field가 start면 시작/종료를 같이 바꿈, end면 종료 시간만 바꿈
    const handleTimeChange = (field, timeValue) => {
        const dateValue =
            selectedDate ||
            formData.start?.slice(0, 10) ||
            formData.end?.slice(0, 10);

        if (!dateValue) {
            return;
        }

        const dateTimeValue = `${dateValue}T${timeValue}`;

        setFormData((prev) => ({
            ...prev,
            [field]: dateTimeValue,

            // 시작 시간을 바꿀 때만 end 값을 추가로 덮어쓴다.
            ...(field === 'start' ? { end: addOneHour(dateTimeValue) } : {}),
        }));
    };

    // 참석자 체크박스 선택/해제 시 실행되는 함수
    // 이미 선택된 참석자면 participants 배열에서 제거,
    // 선택되지 않은 참석자면 participants 배열에 추가.
    const handleParticipantChange = (member) => {
        // 현재 클릭한 참석자가 이미 선택된 상태인지 확인
        const isSelected = formData.participants.some(
            (participant) => participant.empId === member.empId
        );

        setFormData((prev) => ({
            ...prev,
            // isSelected가 true면 제거, false면 추가
            participants: isSelected
                ? prev.participants.filter((participant) => participant.empId !== member.empId)
                : [...prev.participants, member],
        }));
    };

    // 등록 버튼 클릭 시 현재 입력값을 확인하고 팝업을 닫음
    const handleSubmit = async (e) => {
        e.preventDefault();
        setErrorMessage('');

        if (!formData.title.trim()) {
            // 입력 검증 메시지도 관리자처럼 상단 toast로 통일한다.
            onError?.('제목을 입력해 주세요.');
            return;
        }

        if (!userInfo?.empNo) {
            onError?.('로그인 사용자 정보를 확인할 수 없습니다.');
            return;
        }

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
            // 간단등록은 개인일정 기준이다.
            // 참석자가 있으면 초대받은 사람이 볼 수 있어야 하므로 공개로 저장하고, 없으면 비공개로 저장한다.
            isPublic: formData.participants.length > 0,
            repeatRule: null,
            participantNos: formData.participants.map((participant) => participant.empId),
        };

        try {
            await request('POST', '/calendar/create', payload);

            // 등록 성공 처리
            // 입력값을 초기화한 뒤 부모 캘린더에 성공을 알린다.
            resetSimpleForm();

            if (onCreateSuccess) {
                await onCreateSuccess();
            } else {
                handleClose();
            }
        } catch (error) {
            console.error('일정 등록 실패:', error);

            const message = error.response?.data;

            // 공휴일/부재 차단 메시지는 팝업 내부가 아니라 상단 toast로 보여준다.
            onError?.(
                typeof message === 'string' ? message : '일정 등록에 실패했습니다.'
            );
        }


    };


    // 간편등록 팝업 스타일
    // Calendar.js에서 클릭 위치를 넘겨주면 해당 위치 근처에 띄우고,
    // 위치값이 없으면 화면 중앙 상단 쪽에 임시로 표시한다.
    const simplePopupStyle = {
        position: 'fixed',
        top: popupPosition?.top || 90,
        left: popupPosition?.left || '50%',
        transform: popupPosition ? 'none' : 'translateX(-50%)',
        width: '390px',
        maxHeight: 'calc(100vh - 40px)',
        overflowY: 'auto',
        backgroundColor: '#ffffff',
        color: '#111827',
        border: '1px solid #e5e7eb',
        borderRadius: '14px',
        boxShadow: '0 12px 30px rgba(0, 0, 0, 0.18)',
        padding: '22px 18px',
        zIndex: 1050,
        animation: 'calendarQuickPopupIn 0.18s ease-out',
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
        colorScheme: 'light',   // 시간 선택 아이콘이 흰색으로 묻히는 현상 방지
    };
    const helperTextStyle = {
        fontSize: '12px',
        color: '#6b7280',
    };

    return (
        <>
            {/* 퀵 팝업 등장 애니메이션 */}
            <style>
                {`
        @keyframes calendarQuickPopupIn {
            from {
                opacity: 0;
                margin-top: -6px;
            }

            to {
                opacity: 1;
                margin-top: 0;
            }
        }

        /* 시간 선택 아이콘 표시 보정 */
        .calendar-time-input::-webkit-calendar-picker-indicator {
            opacity: 1;
            cursor: pointer;
            filter: invert(35%);
        }
    `}
            </style>

            {/* 일정 간단 등록 퀵 팝업 */}
            {visible && (
                <div ref={popupRef} style={simplePopupStyle}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                        <strong>일정 간단 등록</strong>

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
                    </div>
                    <CForm onSubmit={handleSubmit}>
                        <input
                            name="title"
                            value={formData.title}
                            onChange={handleChange}
                            placeholder="제목을 입력하세요"
                            style={titleInputStyle}
                        />

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
                                id="allDay"
                                type="checkbox"
                                checked={allDay}
                                onChange={(e) => setAllDay(e.target.checked)}
                            />
                            <label htmlFor="allDay" style={{ fontSize: '13px', color: '#374151', cursor: 'pointer' }}>
                                종일
                            </label>
                        </div>

                        {/* 장소 */}
                        <CFormInput
                            name="location"
                            value={formData.location}
                            onChange={handleChange}
                            placeholder="장소를 입력하세요"
                            style={{ ...fieldBlockStyle, ...compactInputStyle }}
                        />

                        {/* 참석자 */}
                        <div style={{ marginTop: '14px' }}>
                            <div style={{ fontSize: '12px', color: '#6b7280', marginBottom: '6px' }}>
                                참석자
                            </div>

                            <div
                                style={{
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'center',
                                    gap: '8px',
                                }}
                            >
                                <div style={{ flex: 1, fontSize: '13px', color: '#6b7280' }}>
                                    {formData.participants.length === 0 ? (
                                        <span>선택된 참석자가 없습니다.</span>
                                    ) : (
                                        formData.participants.map((participant) => (
                                            <span key={participant.empId} style={{ marginRight: '8px' }}>
                                                {participant.name}
                                            </span>
                                        ))
                                    )}
                                </div>

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
                        </div>
                        {/* 등록/상세등록/취소 버튼 영역 */}
                        <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                            <CButton
                                color="secondary"
                                variant="outline"
                                size="sm"
                                type="button"
                                onClick={handleClose}
                            >
                                취소
                            </CButton>

                            <CButton
                                color="primary"
                                variant="outline"
                                size="sm"
                                type="button"
                                onClick={handleDetailAddClick}
                            >
                                상세등록
                            </CButton>

                            <CButton color="primary" size="sm" type="submit">
                                등록
                            </CButton>
                        </div>
                    </CForm>
                </div>
            )}

            {/* 참석자 선택 모달 */}
            <CModal
                alignment="center"
                visible={participantModalVisible}
                onClose={closeParticipantModal}
                size="lg"
            >
                <CModalHeader>
                    <CModalTitle style={{ fontSize: '16px', fontWeight: '800' }}>
                        멤버 초대하기
                    </CModalTitle>
                </CModalHeader>

                <CModalBody>
                    <div style={{ display: 'grid', gridTemplateColumns: 'minmax(180px, 0.85fr) minmax(280px, 1.4fr)', gap: '22px', fontSize: '13px' }}>
                        <div>
                            <div style={{ marginBottom: '10px', fontSize: '13px', fontWeight: '800', color: '#111827' }}>
                                검색 조건
                            </div>

                            <CFormInput
                                value={participantSearchKeyword}
                                onChange={(e) => setParticipantSearchKeyword(e.target.value)}
                                placeholder="멤버 검색"
                                style={{ marginTop: '10px', fontSize: '13px' }}
                            />

                            {/* 같은 모달 안에서 팀 멤버 보기와 조직도 선택 보기를 전환한다. */}
                            {participantViewMode === 'TEAM' ? (
                                <button
                                    type="button"
                                    onClick={openParticipantOrgMode}
                                    style={{
                                        marginTop: '10px',
                                        width: '100%',
                                        height: '32px',
                                        border: '1px solid #c7d2fe',
                                        borderRadius: '6px',
                                        backgroundColor: '#eef2ff',
                                        color: '#4f46e5',
                                        fontSize: '13px',
                                        fontWeight: '700',
                                        cursor: 'pointer',
                                    }}
                                >
                                    조직도에서 선택
                                </button>
                            ) : (
                                <button
                                    type="button"
                                    onClick={openParticipantTeamMode}
                                    style={{
                                        marginTop: '10px',
                                        width: '100%',
                                        height: '32px',
                                        border: '1px solid #d1d5db',
                                        borderRadius: '6px',
                                        backgroundColor: '#ffffff',
                                        color: '#374151',
                                        fontSize: '13px',
                                        fontWeight: '700',
                                        cursor: 'pointer',
                                    }}
                                >
                                    팀 멤버로 돌아가기
                                </button>
                            )}

                            {participantViewMode === 'ORG' && (
                                <div style={{ marginTop: '12px' }}>
                                    <div style={{ marginBottom: '8px', fontSize: '12px', fontWeight: '800', color: '#374151' }}>
                                        부서 선택
                                    </div>

                                    <div style={{ height: '210px', overflowY: 'auto', paddingRight: '4px' }}>
                                        {orgLoading && orgDepartments.length === 0 ? (
                                            <div style={{ fontSize: '12px', color: '#6b7280' }}>
                                                조직도를 불러오는 중입니다.
                                            </div>
                                        ) : orgDepartments.length === 0 ? (
                                            <div style={{ fontSize: '12px', color: '#6b7280' }}>
                                                표시할 부서가 없습니다.
                                            </div>
                                        ) : (
                                            renderParticipantOrgDepartments(orgDepartments)
                                        )}
                                    </div>
                                </div>
                            )}
                        </div>

                        <div>
                            <div style={{ marginBottom: '10px', fontSize: '13px', fontWeight: '800', color: '#111827' }}>
                                {participantViewMode === 'ORG' ? '조직도 멤버' : '팀 멤버'}
                            </div>

                            {participantViewMode === 'ORG' && !orgDeptId ? (
                                <div style={{ color: '#777', fontSize: '13px' }}>
                                    부서를 선택해주세요.
                                </div>
                            ) : (
                                <>
                                    <label
                                        style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '8px',
                                            fontSize: '13px',
                                            color: '#374151',
                                            cursor: 'pointer',
                                        }}
                                    >
                                        <input
                                            type="checkbox"
                                            checked={
                                                visibleParticipantMembers.length > 0 &&
                                                visibleParticipantMembers.every((member) =>
                                                    formData.participants.some((participant) => participant.empId === member.empId)
                                                )
                                            }
                                            onChange={(e) => handleSelectAllVisibleParticipants(e.target.checked)}
                                        />
                                        현재 목록 멤버 전체 선택
                                    </label>

                                    <div style={{ marginTop: '10px', display: 'flex', flexDirection: 'column', gap: '8px', height: '240px', overflowY: 'auto', paddingRight: '4px' }}>
                                        {participantListLoading ? (
                                            <div style={{ color: '#777', fontSize: '13px' }}>
                                                멤버 목록을 불러오는 중입니다.
                                            </div>
                                        ) : participantListError ? (
                                            <div style={{ color: '#dc3545', fontSize: '13px' }}>
                                                {participantListError}
                                            </div>
                                        ) : visibleParticipantMembers.length === 0 ? (
                                            <div style={{ color: '#777', fontSize: '13px' }}>
                                                조건에 맞는 멤버가 없습니다.
                                            </div>
                                        ) : (
                                            visibleParticipantMembers.map((member) => {
                                                const isSelected = formData.participants.some(
                                                    (participant) => participant.empId === member.empId
                                                );

                                                return (
                                                    <label
                                                        key={member.empId}
                                                        style={{
                                                            display: 'flex',
                                                            alignItems: 'center',
                                                            gap: '10px',
                                                            padding: '9px 10px',
                                                            borderRadius: '8px',
                                                            border: isSelected ? '1px solid #5b8def' : '1px solid #e5e7eb',
                                                            backgroundColor: isSelected ? '#5b8def' : '#ffffff',
                                                            color: isSelected ? '#ffffff' : '#111827',
                                                            cursor: 'pointer',
                                                            transition: 'all 0.12s ease',
                                                        }}
                                                    >
                                                        <input
                                                            type="checkbox"
                                                            checked={isSelected}
                                                            onChange={() => handleParticipantChange(member)}
                                                        />

                                                        <div>
                                                            <div style={{ fontWeight: '800' }}>
                                                                {member.name}
                                                            </div>
                                                            <div style={{ marginTop: '2px', fontSize: '12px', color: isSelected ? '#dbeafe' : '#6b7280' }}>
                                                                {member.deptName ? `${member.deptName} · ` : ''}
                                                                {member.positionName || '직급 없음'} · {member.empId}
                                                            </div>
                                                        </div>
                                                    </label>
                                                );
                                            })
                                        )}
                                    </div>
                                </>
                            )}

                            <div style={{ marginTop: '14px' }}>
                                <strong style={{ fontSize: '13px' }}>
                                    선택된 멤버 {formData.participants.length}
                                </strong>

                                <div style={{ marginTop: '8px', display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                                    {formData.participants.length === 0 ? (
                                        <span style={{ color: '#777', fontSize: '13px' }}>
                                            선택된 멤버가 없습니다.
                                        </span>
                                    ) : (
                                        formData.participants.map((participant) => (
                                            <span
                                                key={participant.empId}
                                                style={{
                                                    display: 'inline-flex',
                                                    alignItems: 'center',
                                                    gap: '6px',
                                                    padding: '5px 9px',
                                                    borderRadius: '999px',
                                                    backgroundColor: '#5b8def',
                                                    color: '#ffffff',
                                                    fontSize: '12px',
                                                    fontWeight: '700',
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
                                                        padding: 0,
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
                        size="sm"
                        type="button"
                        onClick={closeParticipantModal}
                    >
                        닫기
                    </CButton>

                    {/* 선택은 체크박스 클릭 시 바로 반영되고, 선택완료는 모달만 닫는다. */}
                    <CButton
                        color="primary"
                        size="sm"
                        type="button"
                        onClick={closeParticipantModal}
                    >
                        선택완료
                    </CButton>
                </CModalFooter>
            </CModal>
        </>
    );
};

export default CalendarSimpleAdd;