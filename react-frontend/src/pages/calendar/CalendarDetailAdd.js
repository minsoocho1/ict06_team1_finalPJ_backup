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
    visible,
    onClose,
    selectedDateProp,
    popupPosition,
    onCreateSuccess,
    onDraftChange,
    popupMode = false,
    onError,
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

    // 상세등록 팝업은 새 일정 등록만 담당한다.
    // 기존 일정 수정은 CalendarDetail.js에서 처리한다.
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

    // userInfo 구조가 화면마다 달라질 수 있어서 가능한 부서 ID 필드를 모두 확인한다.
    const getUserDeptId = (user) => {
        return user?.department?.deptId || user?.dept?.deptId || user?.deptId || user?.dept_id || null;
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
    // 저장 실패 시 화면에 짧게 보여줌
    const [errorMessage, setErrorMessage] = useState('');

    // 상세등록 폼 초기화
    // create 모드는 새 일정 기본값, edit 모드는 기존 일정값을 폼에 채움.
    useEffect(() => {
        if (!visible) {
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

    }, [visible, selectedDate, onDraftChange]);

    // 종일 시간 반영
    // 새 일정 등록 중 종일 체크 시 해당 날짜의 처음부터 끝까지로 시간을 맞춤
    useEffect(() => {
        if (!visible || !selectedDate) {
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
    }, [visible, selectedDate, allDay]);

    // 일정 입력 미리보기
    // 새 일정 등록 중인 제목/시간만 부모 캘린더에 임시 일정으로 전달한다.
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

    // 상세등록 하단 버튼을 관리자 캘린더처럼 팝업 하단에 고정한다.
    const detailFooterStyle = {
        position: 'sticky',
        bottom: 0,
        margin: '18px -20px 0',
        padding: '12px 20px 14px',
        display: 'flex',
        justifyContent: 'flex-end',
        gap: '8px',
        borderTop: '1px solid #e5e7eb',
        backgroundColor: '#ffffff',
        zIndex: 2,
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

    const participantModalGridStyle = {
        display: 'grid',
        gridTemplateColumns: 'minmax(180px, 0.85fr) minmax(280px, 1.4fr)',
        gap: '22px',
        fontSize: '13px',
    };

    const participantSectionTitleStyle = {
        marginBottom: '10px',
        fontSize: '13px',
        fontWeight: '800',
        color: '#111827',
    };

    const participantFieldStyle = {
        marginTop: '10px',
        fontSize: '13px',
    };

    const participantResetButtonStyle = {
        marginTop: '10px',
        width: '100%',
        height: '32px',
        border: '1px solid #e5e7eb',
        borderRadius: '6px',
        backgroundColor: '#f9fafb',
        color: '#374151',
        fontSize: '13px',
        fontWeight: '700',
    };

    const participantListStyle = {
        marginTop: '10px',
        display: 'flex',
        flexDirection: 'column',
        gap: '8px',
        height: '240px',
        overflowY: 'auto',
        paddingRight: '4px',
    };

    const getParticipantCardStyle = (isSelected) => ({
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
    });

    const getParticipantSubTextStyle = (isSelected) => ({
        marginTop: '2px',
        fontSize: '12px',
        color: isSelected ? '#dbeafe' : '#6b7280',
    });

    const selectedParticipantTagStyle = {
        display: 'inline-flex',
        alignItems: 'center',
        gap: '6px',
        padding: '5px 9px',
        borderRadius: '999px',
        backgroundColor: '#5b8def',
        color: '#ffffff',
        fontSize: '12px',
        fontWeight: '700',
    };

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
                visibility:
                    prev.type === 'PERSONAL' && nextParticipants.length > 0
                        ? 'COMPANY'
                        : prev.visibility,
            };
        });
    };

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


    const handleParticipantChange = (member) => {
        setFormData((prev) => {
            const isSelected = prev.participants.some(
                (participant) => participant.empId === member.empId
            );

            const nextParticipants = isSelected
                ? prev.participants.filter((participant) => participant.empId !== member.empId)
                : [...prev.participants, member];

            return {
                ...prev,
                participants: nextParticipants,
                // 참석자가 있으면 초대받은 사람이 볼 수 있어야 하므로 화면의 공개 범위도 공개로 맞춘다.
                visibility:
                    prev.type === 'PERSONAL' && nextParticipants.length > 0
                        ? 'COMPANY'
                        : prev.visibility,
            };
        });
    };

    // 상세 일정 등록
    // 입력값 백엔드 DTO 형식으로 바꿔 저장 API 호출
    const handleSubmit = async (e) => {
        e.preventDefault();
        setErrorMessage('');

        if (!formData.title.trim()) {
            onError?.('제목을 입력해 주세요.');
            return;
        }

        if (!userInfo?.empNo) {
            onError?.('로그인 사용자 정보를 확인할 수 없습니다.');
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
            // 참석자가 있으면 초대받은 사람이 볼 수 있어야 하므로 공개로 저장한다.
            isPublic: formData.participants.length > 0 || formData.visibility !== 'PRIVATE',
            repeatRule: formData.type === 'PERSONAL' && formData.repeatRule ? formData.repeatRule : null,
            participantNos: formData.participants.map((participant) => participant.empId),
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

            onError?.(
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
                                <strong style={{ fontSize: '13px', fontWeight: '700' }}>참석자</strong>

                                {/* 선택된 참석자가 있으면 이름을 표시하고, 없으면 안내문구 표시 */}
                                <div style={{ marginTop: '8px', marginBottom: '8px', fontSize: '13px' }}>
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

                            {/* 취소/등록 버튼 영역 */}
                            {/* 관리자 캘린더와 동일하게 버튼 영역을 팝업 하단에 고정한다. */}
                            <div style={detailFooterStyle}>
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
                    <div style={participantModalGridStyle}>
                        <div>
                            <div style={participantSectionTitleStyle}>
                                검색 조건
                            </div>

                            <CFormInput
                                value={participantSearchKeyword}
                                onChange={(e) => setParticipantSearchKeyword(e.target.value)}
                                placeholder="멤버 검색"
                                style={participantFieldStyle}
                            />

                            {/* 같은 모달 안에서 팀 멤버 보기와 조직도 선택 보기를 전환한다. */}
                            {participantViewMode === 'TEAM' ? (
                                <button
                                    type="button"
                                    onClick={openParticipantOrgMode}
                                    style={{
                                        ...participantResetButtonStyle,
                                        borderColor: '#c7d2fe',
                                        backgroundColor: '#eef2ff',
                                        color: '#4f46e5',
                                    }}
                                >
                                    조직도에서 선택
                                </button>
                            ) : (
                                <button
                                    type="button"
                                    onClick={openParticipantTeamMode}
                                    style={{
                                        ...participantResetButtonStyle,
                                        borderColor: '#d1d5db',
                                        backgroundColor: '#ffffff',
                                        color: '#374151',
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
                            <div style={participantSectionTitleStyle}>
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

                                    <div style={participantListStyle}>
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
                                                        style={getParticipantCardStyle(isSelected)}
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
                                                            <div style={getParticipantSubTextStyle(isSelected)}>
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
                                            <span key={participant.empId} style={selectedParticipantTagStyle}>
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
        </div>
    );
};

export default CalendarDetailAdd;