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
import { CButton, CCard, CCardBody, CCardHeader, CFormInput, CFormSelect, CFormTextarea, CModal, CModalHeader, CModalTitle, CModalBody, CModalFooter } from '@coreui/react';
import CIcon from '@coreui/icons-react';
import { cilClock, cilLayers, cilLocationPin, cilPencil, cilTag, cilTrash, cilUser, cilX } from '@coreui/icons';

// 일정 상세/수정 팝업
// 수정 모드에서도 상세등록과 같은 권한 정책을 적용하기위해 userInfo를 함께 받음.
const CalendarDetail = ({
    visible = false,
    onClose,
    schedule,
    userInfo,
    popupPosition,
    onDelete,
    onUpdateSuccess,
    getMemberScheduleColor,
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

    // 일정 수정 권한은 상세등록과 같은 기준 사용.
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

    // 구분 select에는 현재 사용자 권한으로 수정 가능한 일정 범위만 보여준다.
    const scheduleTypeOptions = [
        { value: 'PERSONAL', label: '개인일정' },
        ...(isTeamLeaderLevel ? [{ value: 'DEPARTMENT', label: '부서일정' }] : []),
        ...(isAdmin ? [{ value: 'COMPANY', label: '전사일정' }] : []),
    ];

    // 공개된 다른 사람 일정은 조회만 가능해야 하므로 수정/삭제 버튼은 작성자 또는 관리자에게만 보여줌.
    const isScheduleOwner = String(schedule?.creatorNo || '') === String(userInfo?.empNo || '');
    const canManageSchedule = isAdmin || isScheduleOwner;

    const scheduleCreatorNo = String(schedule?.creatorNo || '');
    const loginEmpNo = String(userInfo?.empNo || '');

    const participants = schedule?.participants || [];
    const myParticipant = participants.find(
        (participant) => String(participant.empId) === loginEmpNo
    );

    // 내가 작성자가 아니라 참석자로 초대받은 일정이면 응답 버튼을 보여준다.
    const isInvitedParticipant = Boolean(myParticipant) && !isScheduleOwner;
    // 서버에서 내려온 내 참석 상태를 기준값으로 사용한다.
    const scheduleParticipantStatus = myParticipant?.status || 'PENDING';

    // 참석자 응답 상태를 간단확인 팝업에서 읽기 좋은 라벨로 변환한다.
    const getParticipantStatusLabel = (status) => {
        switch (status) {
            case 'ACCEPTED':
                return '참석';
            case 'REJECTED':
                return '불참';
            case 'PENDING':
            default:
                return '미정';
        }
    };

    // 참석자 응답 상태별로 작은 상태 뱃지 색상을 다르게 보여준다.
    const getParticipantStatusBadgeStyle = (status) => {
        switch (status) {
            case 'ACCEPTED':
                return { backgroundColor: '#22c55e', color: '#ffffff' };
            case 'REJECTED':
                return { backgroundColor: '#ef4444', color: '#ffffff' };
            case 'PENDING':
            default:
                return { backgroundColor: '#e5e7eb', color: '#6b7280' };
        }
    };

    const getParticipantStatusMark = (status) => {
        switch (status) {
            case 'ACCEPTED':
                return '✓';
            case 'REJECTED':
                return '×';
            case 'PENDING':
            default:
                return '…';
        }
    };

    // 캘린더에 표시된 일정 dot 색상과 상세 팝업 상단 색상을 맞춘다.
    const scheduleColor =
        schedule?.category === 'ONBOARDING'
            ? '#2e7d32'
            : schedule?.type === 'PERSONAL' && scheduleCreatorNo && scheduleCreatorNo !== loginEmpNo && getMemberScheduleColor
                ? getMemberScheduleColor(scheduleCreatorNo)
                : '#0D6EFD';

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
        participants: [],
        repeatRule: '',
        visibility: 'PRIVATE',
    });

    // 참석여부 select는 서버 저장 성공 전후에도 즉시 화면에 반영되도록 로컬 상태로 관리한다.
    const [participantStatusValue, setParticipantStatusValue] = useState(scheduleParticipantStatus);
    const myParticipantStatus = participantStatusValue;

    // 현재 로그인 사용자의 참석 상태는 select 로컬 상태를 우선 사용해서 팝업 안에서 즉시 반영한다.
    const getDisplayedParticipantStatus = (participant) => {
        if (String(participant.empId) === loginEmpNo) {
            return participantStatusValue;
        }

        return participant.status || 'PENDING';
    };

    // 수정 팝업 참석자 선택 모달 상태
    const [participantModalVisible, setParticipantModalVisible] = useState(false);
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

    // 팝업 상태 초기화
    // 다른 일정을 다시 열면 항상 읽기 전용 요약 화면부터 보여줌.
    useEffect(() => {
        if (visible && schedule) {
            setEditMode(false);
            setParticipantStatusValue(scheduleParticipantStatus);

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
                // 백엔드 응답에 참석자 목록을 추가하면 이 값으로 기존 참석자를 복원한다.
                participants: schedule.participants || [],
                repeatRule: schedule.repeatRule || '',
                visibility: schedule.isPublic ? 'COMPANY' : 'PRIVATE',
            });

            setParticipantModalVisible(false);
        }
    }, [visible, schedule, scheduleParticipantStatus]);

    // 바깥 클릭 닫기
    // 상세 팝업 밖을 클릭하면 팝업을 닫는다.
    useEffect(() => {
        // 참석자 모달이 떠 있을 때는 모달 클릭을 상세 팝업 바깥 클릭으로 오인하지 않게 막는다.
        if (!visible || participantModalVisible) {
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
    }, [visible, participantModalVisible, onClose]);

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
                // 참석자가 있으면 초대받은 사람이 볼 수 있어야 하므로 화면의 공개 범위도 공개로 맞춘다.
                visibility:
                    prev.type === 'PERSONAL' && nextParticipants.length > 0
                        ? 'COMPANY'
                        : prev.visibility,
            };
        });
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

    // 수정 폼 입력 변경
    // input의 name과 formData key를 맞춰두면 한 함수로 입력값을 바꿀 수 있다.
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
                // 개인일정만 비공개/공개를 선택할 수 있다.
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

    const getDisplayDateTimeRange = (startValue, endValue, isAllDay = false) => {
        // 종일 일정은 시간 범위를 보여주지 않고 날짜 + 종일로 간단하게 표시한다.
        if (isAllDay) {
            const allDayDate = getDateText(startValue || endValue);

            return (
                <>
                    <span>{allDayDate || '날짜 정보 없음'}</span>
                    <span style={{ marginLeft: '10px' }}>종일</span>
                </>
            );
        }

        if (!startValue || !endValue) {
            return '시간 정보 없음';
        }

        const startDate = getDateText(startValue);
        const endDate = getDateText(endValue);
        const startTime = getDisplayTimeText(startValue);
        const endTime = getDisplayTimeText(endValue);

        if (startDate === endDate) {
            return (
                <>
                    <span>{startDate}</span>
                    <span style={{ marginLeft: '10px' }}>{startTime} ~ {endTime}</span>
                </>
            );
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

    // 일정 작성자 표시명
    // 개인 일정은 내 일정/작성자 일정으로 보여주가, 부서/전사 일정은 조직 일정 성격을 우선 표시.
    const getScheduleOwnerLabel = () => {
        const type = schedule?.type || 'PERSONAL';

        if (type === 'DEPARTMENT') {
            return '부서 일정';
        }

        if (type === 'COMPANY') {
            return '전사 일정';
        }

        const isMine = String(schedule?.creatorNo || '') === String(userInfo?.empNo || '');

        if (isMine) {
            return '내 일정';
        }

        return schedule?.creatorName
            ? `${schedule.creatorName} 일정`
            : '팀원 일정';
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
            // 참석자가 있으면 초대받은 사람이 볼 수 있어야 하므로 공개로 저장한다.
            isPublic: formData.participants.length > 0 || formData.visibility !== 'PRIVATE',
            repeatRule: formData.type === 'PERSONAL' && formData.repeatRule ? formData.repeatRule : null,
            participantNos: formData.participants.map((participant) => participant.empId),
        };

        try {
            await request(
                'PUT',
                `/calendar/${schedule.scheduleId}?requesterNo=${encodeURIComponent(userInfo?.empNo || '')}`,
                payload
            );

            await onUpdateSuccess?.();
            onClose?.();
        } catch (error) {
            console.error('일정 수정 실패:', error);
        }
    };

    // 초대받은 참석자가 자신의 참석 상태를 변경한다.
    const handleParticipantStatusChange = async (status) => {
        if (!schedule?.scheduleId || !loginEmpNo) {
            return;
        }

        const previousStatus = participantStatusValue;
        setParticipantStatusValue(status);

        try {
            await request(
                'PATCH',
                `/calendar/${schedule.scheduleId}/participants/status?empNo=${encodeURIComponent(loginEmpNo)}&status=${encodeURIComponent(status)}`
            );

            await onUpdateSuccess?.();
        } catch (error) {
            setParticipantStatusValue(previousStatus);
            console.error('참석 응답 변경 실패:', error);
            alert('참석 응답 변경에 실패했습니다.');
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
        zIndex: participantModalVisible ? 1040 : 1060,
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

    const detailInfoListStyle = {
        marginTop: '14px',
        borderTop: '1px solid #e5e7eb',
    };

    const detailInfoRowStyle = {
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        minHeight: '38px',
        borderBottom: '1px solid #eef2f7',
        fontSize: '13px',
        color: '#374151',
    };

    const detailInfoIconStyle = {
        width: '14px',
        height: '14px',
        color: '#6b7280',
        flexShrink: 0,
    };

    const detailInfoValueStyle = {
        minWidth: 0,
        flex: 1,
        color: '#111827',
        fontWeight: '600',
    };

    const participantResponseBoxStyle = {
        marginTop: '14px',
        paddingTop: '14px',
        borderTop: '1px solid #e5e7eb',
    };

    const participantResponseTitleStyle = {
        marginBottom: '8px',
        fontSize: '12px',
        fontWeight: '700',
        color: '#6b7280',
    };

    const participantResponseRowStyle = {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '12px',
    };

    const participantResponseSelectStyle = {
        minWidth: '96px',
        height: '34px',
        border: '1px solid #d1d5db',
        borderRadius: '7px',
        padding: '0 10px',
        backgroundColor: '#ffffff',
        color: '#111827',
        fontSize: '13px',
        fontWeight: '700',
        cursor: 'pointer',
    };

    const participantSummaryStyle = {
        marginTop: '14px',
        paddingTop: '14px',
        borderTop: '1px solid #e5e7eb',
    };

    const participantSummaryHeaderStyle = {
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        marginBottom: '10px',
        fontSize: '13px',
        fontWeight: '800',
        color: '#111827',
    };

    const participantListStyle = {
        display: 'flex',
        flexDirection: 'column',
        gap: '8px',
    };

    const participantItemStyle = {
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
    };

    const participantAvatarStyle = {
        position: 'relative',
        width: '30px',
        height: '30px',
        borderRadius: '50%',
        backgroundColor: '#e5e7eb',
        color: '#9ca3af',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
    };

    const participantStatusMarkStyle = {
        position: 'absolute',
        right: '-2px',
        bottom: '-2px',
        width: '14px',
        height: '14px',
        borderRadius: '50%',
        border: '2px solid #ffffff',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '9px',
        fontWeight: '900',
        lineHeight: 1,
    };

    const participantNameStyle = {
        fontSize: '13px',
        fontWeight: '700',
        color: '#111827',
    };

    const participantStatusTextStyle = {
        marginTop: '2px',
        fontSize: '11px',
        color: '#6b7280',
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
                                backgroundColor: scheduleColor,
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
                                {/* 작성자 또는 관리자만 일정을 수정/삭제할 수 있다. */}
                                {canManageSchedule && (
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
                                    </>
                                )}

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

                            <div
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '8px',
                                    marginTop: '10px',
                                    fontSize: '14px',
                                    color: '#374151',
                                }}
                            >
                                <CIcon icon={cilClock} style={detailInfoIconStyle} />
                                <span>
                                    {getDisplayDateTimeRange(
                                        schedule.startTime || schedule.start,
                                        schedule.endTime || schedule.end,
                                        Boolean(schedule.isAllDay)
                                    )}
                                </span>
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

                            {/* 일정 부가 정보: 아이콘을 붙여 빠르게 스캔할 수 있게 정리한다. */}
                            <div style={detailInfoListStyle}>
                                <div style={detailInfoRowStyle}>
                                    <CIcon icon={cilUser} style={detailInfoIconStyle} />
                                    <span style={detailInfoValueStyle}>{getScheduleOwnerLabel()}</span>
                                </div>

                                <div style={detailInfoRowStyle}>
                                    <CIcon icon={cilLocationPin} style={detailInfoIconStyle} />
                                    <span style={detailInfoValueStyle}>{schedule.location || '장소 없음'}</span>
                                </div>

                                <div style={detailInfoRowStyle}>
                                    <CIcon icon={cilTag} style={detailInfoIconStyle} />
                                    <span style={detailInfoValueStyle}>{getCategoryLabel(schedule.category)}</span>
                                </div>

                                <div style={detailInfoRowStyle}>
                                    <CIcon icon={cilLayers} style={detailInfoIconStyle} />
                                    <span style={detailInfoValueStyle}>{getTypeLabel(schedule.type)}</span>
                                </div>
                            </div>

                            {participants.length > 0 && (
                                <div style={participantSummaryStyle}>
                                    <div style={participantSummaryHeaderStyle}>
                                        <CIcon icon={cilUser} style={detailInfoIconStyle} />
                                        <span>참석자 {participants.length}명</span>
                                    </div>

                                    <div style={participantListStyle}>
                                        {participants.map((participant) => {
                                            const participantStatus = getDisplayedParticipantStatus(participant);

                                            return (
                                                <div key={participant.empId} style={participantItemStyle}>
                                                    <span style={participantAvatarStyle}>
                                                        <CIcon icon={cilUser} size="sm" />
                                                        <span
                                                            style={{
                                                                ...participantStatusMarkStyle,
                                                                ...getParticipantStatusBadgeStyle(participantStatus),
                                                            }}
                                                        >
                                                            {getParticipantStatusMark(participantStatus)}
                                                        </span>
                                                    </span>

                                                    <span>
                                                        <div style={participantNameStyle}>{participant.name}</div>
                                                        <div style={participantStatusTextStyle}>
                                                            {getParticipantStatusLabel(participantStatus)}
                                                        </div>
                                                    </span>
                                                </div>
                                            );
                                        })}
                                    </div>
                                </div>
                            )}

                            {schedule.content && (
                                <>
                                    <div style={{ marginTop: '14px', borderTop: '1px solid #e5e7eb' }} />

                                    <div style={{ marginTop: '14px', fontSize: '13px', color: '#374151', lineHeight: 1.6 }}>
                                        {schedule.content}
                                    </div>
                                </>
                            )}

                            {isInvitedParticipant && (
                                <div style={participantResponseBoxStyle}>
                                    <div style={participantResponseRowStyle}>
                                        <div style={participantResponseTitleStyle}>참석여부</div>

                                        <select
                                            value={myParticipantStatus}
                                            onChange={(e) => handleParticipantStatusChange(e.target.value)}
                                            style={participantResponseSelectStyle}
                                        >
                                            <option value="ACCEPTED">예</option>
                                            <option value="REJECTED">아니오</option>
                                            <option value="PENDING">미정</option>
                                        </select>
                                    </div>
                                </div>
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

                            {/* 일정 구분: 현재 사용자 권한으로 수정 가능한 일정 범위만 보여준다. */}
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
                                <strong style={{ fontSize: '13px', fontWeight: '700' }}>참석자</strong>
                                <div style={{ marginTop: '8px', marginBottom: '8px', color: '#777', fontSize: '13px' }}>
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
                    <CButton color="secondary" variant="outline" size="sm" type="button" onClick={closeParticipantModal}>
                        닫기
                    </CButton>

                    {/* 선택은 체크박스 클릭 시 바로 반영되고, 선택완료는 모달만 닫는다. */}
                    <CButton color="primary" size="sm" type="button" onClick={closeParticipantModal}>
                        선택완료
                    </CButton>
                </CModalFooter>
            </CModal>
        </>
    );
};

export default CalendarDetail;
