// 관리자 상세 팝업과 수정 폼이 공유하는 현재 선택 일정 상태
let adminCalendarSelectedSchedule = null;

// 관리자 등록/수정 폼에서 선택된 참석자 목록
let adminCalendarFormParticipants = [];

// 간단등록 중 캘린더 셀에 보여줄 임시 일정이다.
let adminCalendarDraftEvent = null;

// 상세등록/수정 팝업을 마지막으로 클릭한 날짜/일정 옆에 띄우기 위한 기준 요소다.
let adminCalendarFormAnchorEl = null;

// 참석자 모달에서 임시로 편집 중인 선택 목록.
// 닫기/취소 시 원래 폼 참석자를 건드리지 않고, 선택완료 시에만 반영한다.
let adminCalendarParticipantDraft = [];

// 참석자 모달의 현재 보기 모드.
// TEAM은 전체 구성원 목록, ORG는 조직도 부서 선택 후 구성원 목록이다.
let adminCalendarParticipantMode = 'TEAM';

// 참석자 모달에서 렌더링할 구성원/부서 데이터 캐시.
let adminCalendarParticipantMembers = [];
let adminCalendarOrgDepartments = [];
let adminCalendarOrgDeptId = '';

// 일정 범위 드롭다운에서 선택한 구성원 일정 상태.
// 사용자 캘린더와 같은 기준으로 구성원별 색상을 유지하고, 선택된 구성원의 공개 개인일정을 함께 조회한다.
let adminCalendarMemberScheduleMembers = [];
let adminCalendarMemberOrgDepartments = [];
let adminCalendarMemberOrgMembers = [];
let adminCalendarMemberOrgDeptId = '';
let adminCalendarSelectedMemberScheduleNos = new Set();

// 저장/삭제 요청 중 중복 클릭으로 같은 요청이 여러 번 나가는 것을 막는다.
let adminCalendarSaving = false;
let adminCalendarDeleting = false;

// 구성원 수가 늘어도 색이 바로 겹치지 않도록 넉넉한 팔레트를 둔다.
const adminCalendarMemberColorPalette = [
    '#3b82f6', '#10b981', '#f59e0b', '#ef4444',
    '#8b5cf6', '#06b6d4', '#ec4899', '#84cc16',
    '#f97316', '#14b8a6', '#6366f1', '#a855f7',
    '#22c55e', '#eab308', '#0ea5e9', '#d946ef'
];

document.addEventListener('DOMContentLoaded', function () {
    const calendarEl = document.getElementById('adminCalendar');

    if (!calendarEl) {
        return;
    }

    // 내 일정은 개인일정 필터와 분리해서 제어한다.
    const filterState = {
        scopes: new Set(['MINE', 'PERSONAL', 'DEPARTMENT', 'COMPANY']),
        categories: new Set(['MEETING', 'WORK', 'NOTICE', 'ETC'])
    };

    const calendar = new FullCalendar.Calendar(calendarEl, {
        locale: 'ko',
        initialView: 'dayGridMonth',
        height: '100%',
        expandRows: true,
        headerToolbar: false,
        dayMaxEvents: 2,
        moreLinkContent: function (arg) {
            return arg.num + '개 더보기';
        },
        eventTimeFormat: {
            hour: 'numeric',
            minute: '2-digit',
            meridiem: 'short',
            hour12: true
        },

        // 관리자 일정 목록을 가져온 뒤 현재 필터 상태에 맞는 일정만 FullCalendar 이벤트로 변환한다.
        events: async function (fetchInfo, successCallback, failureCallback) {
            try {
                const selectedMemberNos = Array.from(adminCalendarSelectedMemberScheduleNos);
                const query = selectedMemberNos.length
                    ? '?selectedMemberNos=' + encodeURIComponent(selectedMemberNos.join(','))
                    : '';
                const response = await fetch('/admin/calendar/schedules' + query);

                if (!response.ok) {
                    throw new Error('관리자 일정 목록 조회 실패');
                }

                const schedules = normalizeAdminArrayResponse(await response.json());
                const events = schedules
                    .map(normalizeAdminSchedule)
                    .filter(function (schedule) {
                        return isVisibleByAdminFilter(schedule, filterState);
                    })
                    .flatMap(function (schedule) {
                        return expandAdminRepeatedScheduleEvents(schedule, fetchInfo.start, fetchInfo.end);
                    });

                // 관리자도 본인 + 선택 구성원 부재 라벨을 일정과 함께 표시한다.
                // 부재 라벨 조회가 실패해도 기존 일정 목록은 유지한다.
                let absenceEvents = [];
                let holidayEvents = [];

                try {
                    absenceEvents = await fetchAdminAbsenceEvents(fetchInfo);
                } catch (absenceError) {
                    console.error(absenceError);
                }

                try {
                    holidayEvents = await fetchAdminHolidayEvents(fetchInfo);
                } catch (holidayError) {
                    console.error(holidayError);
                }

                successCallback(holidayEvents.concat(events, absenceEvents));
            } catch (error) {
                console.error(error);
                failureCallback(error);
            }
        },

        eventContent: renderAdminCalendarEvent,

        // 사용자 캘린더처럼 일정 위에 마우스를 올리면 기본 툴팁으로 제목을 확인할 수 있게 한다.
        eventDidMount: function (info) {
            info.el.title = info.event.title || '';

            // 종일 개인일정 라벨 색상은 FullCalendar 바깥 이벤트 요소에 CSS 변수로 심어준다.
            const schedule = info.event.extendedProps || {};

            if (info.event.allDay && (schedule.type || 'PERSONAL') === 'PERSONAL') {
                const color = getAdminScheduleColor(schedule);

                info.el.style.setProperty('--admin-calendar-event-color', color);
                info.el.style.setProperty('--admin-calendar-all-day-bg', getAdminAllDayBackgroundColor(color));
            }
        },

        // 클릭한 일정 데이터를 사용자 캘린더와 같은 간단 상세 팝업으로 표시한다.
        eventClick: function (info) {
            // 부재 라벨은 일정이 아니므로 상세 팝업을 열지 않는다.
            if (
                info.event.extendedProps?.source === 'ABSENCE' ||
                info.event.extendedProps?.source === 'HOLIDAY'
            ) {
                return;
            }

            openAdminCalendarDetailPopup(info);
        },

        // 날짜 빈 칸을 클릭하면 사용자 캘린더처럼 클릭한 셀 근처에 간단 등록 팝업을 연다.
        dateClick: function (info) {
            openAdminCalendarForm(calendar, {
                mode: 'create',
                dateStr: info.dateStr,
                dayEl: info.dayEl
            });
        },

        datesSet: function (info) {
            const titleEl = document.getElementById('adminCalendarTitle');

            if (titleEl) {
                titleEl.textContent = info.view.title;
            }

            setActiveViewButton(info.view.type);
        },

        dayCellContent: function (arg) {
            return {
                html: '<span class="admin-calendar-day-number">' + arg.date.getDate() + '</span>'
            };
        }
    });

    calendar.render();

    bindAdminCalendarToolbar(calendar);
    bindAdminCalendarFilters(calendar, filterState);
    bindAdminCalendarDetailPopup(calendar);
    bindAdminCalendarForm(calendar);
    bindAdminCalendarResize(calendar);
});

// 서버에서 받은 일정 DTO를 FullCalendar가 이해할 수 있는 이벤트 구조로 바꾼다.
// API 응답이 배열 또는 {data: []} 형태로 와도 같은 방식으로 처리한다.
function normalizeAdminArrayResponse(response) {
    if (Array.isArray(response)) {
        return response;
    }

    if (Array.isArray(response?.data)) {
        return response.data;
    }

    if (Array.isArray(response?.content)) {
        return response.content;
    }

    return [];
}

// 일정 DTO의 필드명이 API별로 조금씩 달라도 관리자 캘린더 내부에서는 같은 이름으로 사용한다.
function normalizeAdminSchedule(schedule) {
    const participants = Array.isArray(schedule?.participants)
        ? schedule.participants.map(normalizeAdminParticipant)
        : [];

    return {
        ...schedule,
        scheduleId: schedule?.scheduleId ?? schedule?.id ?? schedule?.schedule_id,
        creatorNo: getAdminMemberEmpNo({
            empNo: schedule?.creatorNo,
            empId: schedule?.creatorId,
            emp_no: schedule?.creator_no
        }),
        creatorName: schedule?.creatorName || schedule?.creator?.name || schedule?.name || '',
        participants: participants
    };
}

// 참석자/구성원 DTO를 같은 구조로 맞춘다.
function normalizeAdminParticipant(participant) {
    const empId = getAdminMemberEmpNo(participant);

    return {
        ...participant,
        empId: empId,
        empNo: empId,
        name: getAdminMemberName(participant),
        deptId: participant?.deptId ?? participant?.departmentId ?? participant?.department?.deptId,
        deptName: participant?.deptName || participant?.departmentName || participant?.department?.deptName || '',
        positionName: participant?.positionName || participant?.position?.positionName || '',
        profileImg: participant?.profileImg || ''
    };
}

function getAdminMemberEmpNo(member) {
    return String(
        member?.empNo ??
        member?.empId ??
        member?.employeeNo ??
        member?.emp_no ??
        ''
    ).trim();
}

function getAdminMemberName(member) {
    const empNo = getAdminMemberEmpNo(member);

    return member?.name ||
        member?.empName ||
        member?.employeeName ||
        member?.memberName ||
        empNo;
}

function formatAdminCalendarEvent(schedule, startTime, endTime, repeatIndex = 0) {
    schedule = normalizeAdminSchedule(schedule);
    const type = schedule.type || 'PERSONAL';
    const scheduleId = schedule.scheduleId ?? schedule.id ?? schedule.schedule_id;
    const creatorNo = getAdminMemberEmpNo({
        empNo: schedule.creatorNo,
        empId: schedule.creatorId,
        emp_no: schedule.creator_no
    });
    const isSelectedMemberSchedule = type === 'PERSONAL'
        && creatorNo
        && adminCalendarSelectedMemberScheduleNos.has(creatorNo);

    const normalizedSchedule = {
        ...schedule,
        scheduleId: scheduleId,
        creatorNo: creatorNo,
        // 반복 이벤트를 클릭해도 상세/수정/삭제 기준은 DB 원본 일정으로 유지한다.
        startTime: schedule.startTime,
        endTime: schedule.endTime,
        // 화면에 표시된 반복 발생 시각은 필요할 때만 참고용으로 따로 보관한다.
        occurrenceStartTime: startTime || schedule.startTime,
        occurrenceEndTime: endTime || schedule.endTime,
        isRepeatGenerated: repeatIndex > 0,
        originalStartTime: schedule.startTime,
        originalEndTime: schedule.endTime,
        isSelectedMemberSchedule: isSelectedMemberSchedule
    };

    const isAllDay = Boolean(schedule.isAllDay);
    const normalizedType = String(type || 'PERSONAL').toUpperCase();

    return {
        id: repeatIndex === 0 ? String(scheduleId) : scheduleId + '-repeat-' + repeatIndex,
        title: schedule.title || '(제목 없음)',

        // FullCalendar 종일 일정은 날짜 문자열로 넘겨야 월간보기에서 바 라벨로 안정적으로 표시된다.
        start: isAllDay
            ? toAdminDateOnlyValue(startTime || schedule.startTime)
            : (startTime || schedule.startTime),
        end: isAllDay
            ? toAdminAllDayExclusiveEnd(endTime || schedule.endTime || startTime || schedule.startTime)
            : (endTime || schedule.endTime),

        allDay: isAllDay,
        classNames: [
            isAllDay && normalizedType === 'PERSONAL'
                ? 'admin-calendar-event-all-day-personal'
                : '',
            isSelectedMemberSchedule
                ? 'admin-calendar-event-selected-member'
                : ''
        ].filter(Boolean),
        extendedProps: normalizedSchedule
    };
}

// 부재 사유별 관리자 캘린더 라벨 색상
function getAdminAbsenceEventColor(reasonType) {
    const colors = {
        LEAVE: { bg: '#ccfbf1', border: '#5eead4', text: '#0f766e' },
        HALF_LEAVE: { bg: '#fef3c7', border: '#fbbf24', text: '#92400e' },
        EARLY: { bg: '#ffedd5', border: '#fb923c', text: '#9a3412' },
        SICK: { bg: '#fee2e2', border: '#f87171', text: '#991b1b' },
        FAMILY_EVENT: { bg: '#f1f5f9', border: '#94a3b8', text: '#334155' },
    };

    return colors[reasonType] || colors.LEAVE;
}

// 관리자 캘린더에 표시할 부재 라벨 이벤트를 만든다.
function formatAdminAbsenceEvent(absence) {
    const color = getAdminAbsenceEventColor(absence.reasonType);
    const isAllDay = absence.isAllDay === true || absence.allDay === true;

    return {
        id: 'absence-' + absence.empNo + '-' + absence.reasonType + '-' + absence.unavailableStartTime,
        title: '[' + absence.reasonName + '] ' + absence.name,
        start: absence.unavailableStartTime,
        end: absence.unavailableEndTime,
        allDay: isAllDay,
        backgroundColor: color.bg,
        borderColor: color.border,
        textColor: color.text,
        classNames: ['admin-calendar-absence-event'],
        extendedProps: {
            source: 'ABSENCE',
            absence: absence,
            absenceColor: color
        }
    };
}

async function fetchAdminAbsenceEvents(fetchInfo) {
    const empNos = [
        getAdminLoginEmpNo(),
        ...Array.from(adminCalendarSelectedMemberScheduleNos)
    ].filter(Boolean);

    if (empNos.length === 0) {
        return [];
    }

    const query =
        '?start=' + encodeURIComponent(formatAdminCalendarDateTime(fetchInfo.start)) +
        '&end=' + encodeURIComponent(formatAdminCalendarDateTime(fetchInfo.end)) +
        '&empNos=' + encodeURIComponent(empNos.join(','));

    const response = await fetch('/admin/calendar/availability/unavailable-employees' + query);

    if (!response.ok) {
        throw new Error('부재 라벨 조회 실패');
    }

    return normalizeAdminArrayResponse(await response.json()).map(formatAdminAbsenceEvent);
}

// 관리자 캘린더에 표시할 공휴일 라벨을 조회한다.
async function fetchAdminHolidayEvents(fetchInfo) {
    const query =
        '?start=' + encodeURIComponent(formatAdminCalendarDateTime(fetchInfo.start)) +
        '&end=' + encodeURIComponent(formatAdminCalendarDateTime(fetchInfo.end));

    const response = await fetch('/admin/calendar/holidays' + query);

    if (!response.ok) {
        throw new Error('공휴일 조회 실패');
    }

    return normalizeAdminArrayResponse(await response.json()).map(formatAdminHolidayEvent);
}

// 공휴일 DTO를 FullCalendar 이벤트로 변환한다.
function formatAdminHolidayEvent(holiday) {
    const holidayDate = holiday.holidayDate;

    return {
        id: 'holiday-' + holidayDate,
        title: holiday.holidayName || '공휴일',
        start: holidayDate,
        end: getAdminNextDateString(holidayDate),
        allDay: true,
        classNames: ['admin-calendar-holiday-event'],
        extendedProps: {
            source: 'HOLIDAY',
            holiday: holiday
        }
    };
}

// yyyy-MM-dd 기준 다음 날짜 문자열을 만든다.
function getAdminNextDateString(dateString) {
    const parts = dateString.split('-').map(Number);
    const date = new Date(parts[0], parts[1] - 1, parts[2]);

    date.setDate(date.getDate() + 1);

    return toAdminDateOnlyValue(date);
}

// FullCalendar 종일 일정용 날짜 값으로 변환한다.
function toAdminDateOnlyValue(value) {
    if (!value) {
        return '';
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return '';
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return year + '-' + month + '-' + day;
}

// FullCalendar 종일 일정의 end는 exclusive라서 다음 날짜로 넘긴다.
function toAdminAllDayExclusiveEnd(value) {
    if (!value) {
        return '';
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return '';
    }

    date.setDate(date.getDate() + 1);

    return toAdminDateOnlyValue(date);
}

// 반복 간격을 계산한다.
// 사용자 캘린더와 동일하게 매일/매주/매월까지만 지원한다.
function addAdminRepeatInterval(date, repeatRule) {
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
}

// Date 객체를 FullCalendar가 읽을 수 있는 yyyy-MM-ddTHH:mm:ss 문자열로 바꾼다.
function formatAdminCalendarDateTime(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    const second = String(date.getSeconds()).padStart(2, '0');

    return year + '-' + month + '-' + day + 'T' + hour + ':' + minute + ':' + second;
}

// 일정 시간이 현재 캘린더 화면 범위와 겹치는지 확인한다.
function isAdminEventInCalendarRange(startTime, endTime, rangeStart, rangeEnd) {
    const eventStart = new Date(startTime);
    const eventEnd = new Date(endTime || startTime);

    if (Number.isNaN(eventStart.getTime()) || Number.isNaN(eventEnd.getTime())) {
        return false;
    }

    return eventStart < rangeEnd && eventEnd > rangeStart;
}

// DB에는 반복 원본 1개만 저장하고, 화면 범위 안에서만 반복 일정을 만들어 보여준다.
function expandAdminRepeatedScheduleEvents(schedule, rangeStart, rangeEnd) {
    if (!rangeStart || !rangeEnd) {
        return [];
    }

    const canRepeat = schedule.type === 'PERSONAL' && schedule.repeatRule;

    if (!canRepeat) {
        return isAdminEventInCalendarRange(schedule.startTime, schedule.endTime, rangeStart, rangeEnd)
            ? [formatAdminCalendarEvent(schedule)]
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

        currentStart = addAdminRepeatInterval(currentStart, schedule.repeatRule);
        repeatIndex += 1;
    }

    // 현재 화면 범위 안에 들어오는 반복 일정만 만든다.
    while (currentStart && currentStart < rangeEnd && repeatIndex < 5000) {
        const currentEnd = new Date(currentStart.getTime() + duration);

        if (currentEnd > rangeStart) {
            events.push(
                formatAdminCalendarEvent(
                    schedule,
                    formatAdminCalendarDateTime(currentStart),
                    formatAdminCalendarDateTime(currentEnd),
                    repeatIndex
                )
            );
        }

        currentStart = addAdminRepeatInterval(currentStart, schedule.repeatRule);
        repeatIndex += 1;
    }

    return events;
}

function isVisibleByAdminFilter(schedule, filterState) {
    const type = schedule.type || 'PERSONAL';
    const category = normalizeCategory(schedule.category);
    const creatorNo = String(schedule.creatorNo || '');
    const loginEmpNo = String(getAdminLoginEmpNo() || '');

    if (!filterState.categories.has(category)) {
        return false;
    }

    // 본인 개인일정은 "내 일정" 필터로 따로 제어한다.
    if (type === 'PERSONAL' && (!creatorNo || creatorNo === loginEmpNo)) {
        return filterState.scopes.has('MINE');
    }

    return filterState.scopes.has(type);
}

// DB에 저장된 카테고리 값이 비어있거나 예상 밖이면 기타로 처리한다.
function normalizeCategory(category) {
    if (!category) {
        return 'ETC';
    }

    const normalized = String(category).toUpperCase();

    if (['MEETING', 'WORK', 'NOTICE', 'ETC'].includes(normalized)) {
        return normalized;
    }

    return 'ETC';
}

function bindAdminCalendarToolbar(calendar) {
    document.getElementById('adminCalendarPrev')?.addEventListener('click', function () {
        closeAdminCalendarFloatingLayers();
        calendar.prev();
    });

    document.getElementById('adminCalendarNext')?.addEventListener('click', function () {
        closeAdminCalendarFloatingLayers();
        calendar.next();
    });

    document.getElementById('adminCalendarToday')?.addEventListener('click', function () {
        closeAdminCalendarFloatingLayers();
        calendar.today();
    });

    document.querySelectorAll('[data-admin-calendar-view]').forEach(function (button) {
        button.addEventListener('click', function () {
            closeAdminCalendarFloatingLayers();
            calendar.changeView(button.dataset.adminCalendarView);
        });
    });
}

// 관리자 상세 팝업의 닫기/수정/삭제 동작을 연결한다.
function bindAdminCalendarDetailPopup(calendar) {
    const popup = document.getElementById('adminCalendarDetailPopup');

    document.getElementById('adminCalendarDetailClose')?.addEventListener('click', closeAdminCalendarDetailPopup);

    // 참석여부 변경은 사용자 캘린더의 participants/status API를 그대로 사용한다.
    document.getElementById('adminCalendarParticipantStatus')?.addEventListener('change', function (event) {
        updateAdminParticipantStatus(calendar, event.target.value);
    });

    document.getElementById('adminCalendarDetailEdit')?.addEventListener('click', function () {
        if (!adminCalendarSelectedSchedule) {
            return;
        }

        openAdminCalendarForm(calendar, {
            mode: 'edit',
            schedule: adminCalendarSelectedSchedule,
            eventEl: adminCalendarFormAnchorEl
        });
    });

    document.getElementById('adminCalendarDetailDelete')?.addEventListener('click', async function () {
        if (!adminCalendarSelectedSchedule?.scheduleId) {
            return;
        }

        if (adminCalendarDeleting) {
            return;
        }

        // 반복 일정은 원본 일정 삭제로 처리되므로 전체 삭제 안내를 보여준다.
        const deleteMessage = adminCalendarSelectedSchedule.repeatRule
            ? '반복 일정 전체가 삭제됩니다. 삭제할까요?'
            : '선택한 일정을 삭제할까요?';

        if (!confirm(deleteMessage)) {
            return;
        }

        try {
            adminCalendarDeleting = true;
            const response = await fetch('/admin/calendar/schedules/' + adminCalendarSelectedSchedule.scheduleId, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error('관리자 일정 삭제 실패');
            }

            closeAdminCalendarDetailPopup();
            calendar.refetchEvents();
            showAdminCalendarToast('일정이 삭제되었습니다.');
        } catch (error) {
            console.error(error);
            showAdminCalendarToast('일정 삭제 중 오류가 발생했습니다.', 'error');
        } finally {
            adminCalendarDeleting = false;
        }
    });

    document.addEventListener('mousedown', function (event) {
        const formOverlay = document.getElementById('adminCalendarDetailFormLayer');

        if (!popup || popup.hidden) {
            return;
        }

        if (formOverlay && !formOverlay.hidden) {
            return;
        }

        if (!event.target.closest('#adminCalendarDetailPopup')) {
            closeAdminCalendarDetailPopup();
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            closeAdminCalendarForm();
            closeAdminCalendarDetailPopup();
        }
    });
}

// 관리자 사이드바 접힘/펼침 후 FullCalendar 내부 폭을 다시 계산한다.
// 사용자 캘린더와 동일하게 오른쪽 빈 영역이 남지 않도록 레이아웃 전환 직후와 애니메이션 종료 시점에 updateSize를 호출한다.
function bindAdminCalendarResize(calendar) {
    const updateCalendarSize = function () {
        calendar.updateSize();
    };

    const updateAfterLayoutChange = function () {
        requestAnimationFrame(updateCalendarSize);
        setTimeout(updateCalendarSize, 200);
        setTimeout(updateCalendarSize, 350);
    };

    window.addEventListener('resize', updateAfterLayoutChange);

    document.querySelector('.app-sidebar')?.addEventListener('open.lte.push-menu', updateAfterLayoutChange);
    document.querySelector('.app-sidebar')?.addEventListener('collapse.lte.push-menu', updateAfterLayoutChange);

    document.querySelectorAll('[data-lte-toggle="sidebar"]').forEach(function (button) {
        button.addEventListener('click', updateAfterLayoutChange);
    });

    updateAfterLayoutChange();
}

function bindAdminCalendarFilters(calendar, filterState) {
    bindDropdownToggle('adminScopeFilterButton', 'adminScopeFilterDropdown');
    bindDropdownToggle('adminCategoryFilterButton', 'adminCategoryFilterDropdown');
    bindAdminMemberScheduleFilters(calendar);

    document.querySelectorAll('[data-admin-scope-filter]').forEach(function (checkbox) {
        checkbox.addEventListener('change', function () {
            closeAdminCalendarFloatingLayers();
            updateFilterSet(filterState.scopes, checkbox.dataset.adminScopeFilter, checkbox.checked);
            calendar.refetchEvents();
        });
    });

    document.querySelectorAll('[data-admin-category-filter]').forEach(function (checkbox) {
        checkbox.addEventListener('change', function () {
            closeAdminCalendarFloatingLayers();
            updateFilterSet(filterState.categories, checkbox.dataset.adminCategoryFilter, checkbox.checked);
            calendar.refetchEvents();
        });
    });

    document.addEventListener('click', function (event) {
        if (!event.target.closest('.admin-calendar-filter-wrap')) {
            closeAdminCalendarDropdowns();
        }
    });
}

function bindDropdownToggle(buttonId, dropdownId) {
    const button = document.getElementById(buttonId);
    const dropdown = document.getElementById(dropdownId);

    if (!button || !dropdown) {
        return;
    }

    button.addEventListener('click', function (event) {
        event.stopPropagation();

        document.querySelectorAll('.admin-calendar-filter-dropdown').forEach(function (target) {
            if (target !== dropdown) {
                target.classList.remove('open');
            }
        });

        document.querySelectorAll('.admin-calendar-filter-btn').forEach(function (target) {
            if (target !== button) {
                target.classList.remove('active');
            }
        });

        dropdown.classList.toggle('open');
        button.classList.toggle('active', dropdown.classList.contains('open'));
    });

    dropdown.addEventListener('click', function (event) {
        event.stopPropagation();
    });
}

function closeAdminCalendarDropdowns() {
    document.querySelectorAll('.admin-calendar-filter-dropdown').forEach(function (dropdown) {
        dropdown.classList.remove('open');
    });

    document.querySelectorAll('.admin-calendar-filter-btn').forEach(function (button) {
        button.classList.remove('active');
    });
}

function updateFilterSet(targetSet, value, checked) {
    if (checked) {
        targetSet.add(value);
        return;
    }

    targetSet.delete(value);
}

function setActiveViewButton(viewType) {
    document.querySelectorAll('[data-admin-calendar-view]').forEach(function (button) {
        button.classList.toggle('active', button.dataset.adminCalendarView === viewType);
    });
}

// 관리자 캘린더의 모든 플로팅 UI를 한 번에 정리한다.
// 사용자 캘린더처럼 상세팝업, 간단등록, 상세등록/수정, 참석자 모달이 동시에 남지 않도록 한다.
function closeAdminCalendarFloatingLayers(options = {}) {
    const keepParticipantModal = options.keepParticipantModal === true;

    closeAdminCalendarDetailPopup();
    closeAdminCalendarSimpleForm();
    closeAdminCalendarDetailForm();

    if (!keepParticipantModal) {
        closeAdminParticipantModal();
    }
}

// 관리자 일정 상세 팝업을 연다.
// 일정 클릭 시 다른 등록/수정 팝업을 먼저 닫아서 팝업이 두 개 이상 겹치지 않게 한다.
function openAdminCalendarDetailPopup(info) {
    const popup = document.getElementById('adminCalendarDetailPopup');
    const schedule = info.event.extendedProps || {};
    const type = schedule.type || 'PERSONAL';

    if (!popup) {
        return;
    }

    closeAdminCalendarFloatingLayers();

    adminCalendarSelectedSchedule = schedule;
    adminCalendarFormAnchorEl = info.el;
    popup.dataset.scheduleId = schedule.scheduleId || '';

    setTextContent('adminCalendarDetailTitle', schedule.title || info.event.title || '(제목 없음)');
    setHtmlContent('adminCalendarDetailTime', getAdminCalendarDetailTimeHtml(schedule));
    // 반복 일정이면 상세 팝업에서 사용자 캘린더와 같은 반복 뱃지를 보여준다.
    renderAdminRepeatBadge(schedule);
    setTextContent('adminCalendarDetailOwner', getAdminScheduleOwnerLabel(schedule));
    setTextContent('adminCalendarDetailLocation', schedule.location || '장소 없음');
    setTextContent('adminCalendarDetailCategory', getAdminCategoryLabel(schedule.category));
    setTextContent('adminCalendarDetailType', getAdminTypeLabel(type));
    setTextContent('adminCalendarDetailContent', schedule.content || '');

    renderAdminCalendarDetailParticipants(schedule);
    // 내가 초대받은 일정이면 참석여부 select를 보여준다.
    renderAdminParticipantResponse(schedule);

    const colorEl = document.getElementById('adminCalendarDetailColor');

    if (colorEl) {
        colorEl.style.backgroundColor = getAdminScheduleColor(schedule);
    }

    positionAdminCalendarDetailPopup(popup, info.el);

    popup.hidden = false;
}

// 상세 팝업의 참석자 목록을 사용자 캘린더와 같은 정보 구조로 표시한다.
function renderAdminCalendarDetailParticipants(schedule) {
    const wrapper = document.getElementById('adminCalendarDetailParticipants');
    const countEl = document.getElementById('adminCalendarDetailParticipantCount');
    const listEl = document.getElementById('adminCalendarDetailParticipantList');
    const participants = Array.isArray(schedule.participants) ? schedule.participants : [];

    if (!wrapper || !countEl || !listEl) {
        return;
    }

    if (!participants.length) {
        wrapper.hidden = true;
        listEl.innerHTML = '';
        countEl.textContent = '';
        return;
    }

    wrapper.hidden = false;
    countEl.textContent = '참석자 ' + participants.length + '명';

    listEl.innerHTML = participants.map(function (participant) {
        const status = participant.status || 'PENDING';
        const statusClass = status === 'ACCEPTED'
            ? 'accepted'
            : status === 'REJECTED'
                ? 'rejected'
                : 'pending';
        const statusMark = status === 'ACCEPTED'
            ? '✓'
            : status === 'REJECTED'
                ? '×'
                : '';

        return ''
            + '<div class="admin-calendar-detail-participant-item">'
            + '  <span class="admin-calendar-detail-participant-avatar">'
            + '    <i class="fa-solid fa-user"></i>'
            + '    <span class="admin-calendar-detail-participant-status-mark admin-calendar-detail-participant-status-' + statusClass + '">' + statusMark + '</span>'
            + '  </span>'
            + '  <span>'
            + '    <span class="admin-calendar-detail-participant-name">' + escapeHtml(participant.name || participant.empId || '') + '</span>'
            + '    <span class="admin-calendar-detail-participant-status-text">' + getAdminParticipantStatusLabel(status) + '</span>'
            + '  </span>'
            + '</div>';
    }).join('');
}

// 사용자 캘린더와 같은 참석 상태 라벨로 표시한다.
function getAdminParticipantStatusLabel(status) {
    if (status === 'ACCEPTED') {
        return '참석';
    }

    if (status === 'REJECTED') {
        return '불참';
    }

    return '미정';
}

// 현재 로그인 관리자 사번을 화면 hidden 값에서 읽는다.
function getAdminLoginEmpNo() {
    return getFormValue('adminCalendarLoginEmpNo');
}

// 현재 로그인 관리자가 참석자인지 찾는다.
function getAdminMyParticipant(schedule) {
    const loginEmpNo = getAdminLoginEmpNo();
    const participants = Array.isArray(schedule?.participants) ? schedule.participants : [];

    if (!loginEmpNo) {
        return null;
    }

    return participants.find(function (participant) {
        return String(participant.empId) === String(loginEmpNo);
    }) || null;
}

// 내가 작성자가 아니고 참석자로 초대된 일정이면 참석여부 select를 보여준다.
function renderAdminParticipantResponse(schedule) {
    const wrapper = document.getElementById('adminCalendarParticipantResponse');
    const statusEl = document.getElementById('adminCalendarParticipantStatus');
    const loginEmpNo = getAdminLoginEmpNo();
    const creatorNo = getAdminMemberEmpNo({ empNo: schedule?.creatorNo });
    const myParticipant = getAdminMyParticipant(schedule);

    if (!wrapper || !statusEl) {
        return;
    }

    if (!myParticipant || String(creatorNo) === String(loginEmpNo)) {
        wrapper.hidden = true;
        return;
    }

    statusEl.value = myParticipant.status || 'PENDING';
    wrapper.hidden = false;
}

// 참석여부 변경값을 서버에 저장하고 캘린더를 다시 불러온다.
async function updateAdminParticipantStatus(calendar, status) {
    const scheduleId = adminCalendarSelectedSchedule?.scheduleId;
    const loginEmpNo = getAdminLoginEmpNo();

    if (!scheduleId || !loginEmpNo) {
        return;
    }

    try {
        const response = await fetch(
            '/calendar/' + scheduleId + '/participants/status?empNo=' + encodeURIComponent(loginEmpNo) +
            '&status=' + encodeURIComponent(status),
            { method: 'PATCH' }
        );

        if (!response.ok) {
            throw new Error('관리자 참석여부 변경 실패');
        }

        calendar.refetchEvents();
    } catch (error) {
        console.error(error);
        showAdminCalendarToast('참석여부 변경 중 오류가 발생했습니다.', 'error');
    }
}

// 일정 상세 팝업 위치를 사용자 캘린더처럼 클릭한 일정 옆에 배치한다.
function positionAdminCalendarDetailPopup(popup, eventEl) {
    const rect = eventEl?.getBoundingClientRect();
    const popupGap = 12;
    const popupWidth = 360;
    const popupHeight = 520;
    const viewportPadding = 20;

    let left = rect ? rect.right + popupGap : Math.round((window.innerWidth - popupWidth) / 2);
    let top = rect ? rect.top : 90;

    if (rect && left + popupWidth > window.innerWidth) {
        left = rect.left - popupWidth - popupGap;
    }

    if (left < viewportPadding) {
        left = viewportPadding;
    }

    if (top + popupHeight > window.innerHeight) {
        top = window.innerHeight - popupHeight - viewportPadding;
    }

    if (top < 80) {
        top = 80;
    }

    popup.style.left = left + 'px';
    popup.style.top = top + 'px';
    popup.style.maxHeight = 'calc(100vh - ' + top + 'px - 24px)';
}

// 상세 팝업을 닫고 현재 선택된 일정 상태를 초기화한다.
function closeAdminCalendarDetailPopup() {
    const popup = document.getElementById('adminCalendarDetailPopup');

    if (popup) {
        popup.hidden = true;
    }

    adminCalendarSelectedSchedule = null;
}

// 특정 id를 가진 요소에 텍스트를 안전하게 넣는다.
function setTextContent(id, value) {
    const target = document.getElementById(id);

    if (target) {
        target.textContent = value;
    }
}

// 서버가 내려준 검증 실패 메시지를 우선 사용한다.
async function getAdminCalendarErrorMessage(response, fallbackMessage) {
    const message = await response.text();

    return message && message.trim()
        ? message.trim()
        : fallbackMessage;
}

// 사용자 캘린더처럼 저장/수정/삭제 결과를 상단 토스트로 보여준다.
function showAdminCalendarToast(message, type = 'success') {
    let toast = document.getElementById('adminCalendarToast');

    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'adminCalendarToast';
        toast.className = 'admin-calendar-toast';
        document.body.appendChild(toast);
    }

    toast.textContent = message;
    toast.className = 'admin-calendar-toast admin-calendar-toast-' + type + ' is-visible';

    window.clearTimeout(showAdminCalendarToast.timer);
    showAdminCalendarToast.timer = window.setTimeout(function () {
        toast.classList.remove('is-visible');
    }, 2200);
}

// 아이콘이 포함된 시간 영역처럼 HTML 조각이 필요한 요소에만 사용한다.
function setHtmlContent(id, value) {
    const target = document.getElementById(id);

    if (target) {
        target.innerHTML = value;
    }
}

// 일정의 시작/종료 시간을 사용자 캘린더 상세 팝업과 같은 톤으로 보여준다.
function getAdminCalendarDetailTimeHtml(schedule) {
    const startText = formatAdminDateTime(schedule.startTime);
    const endText = formatAdminDateTime(schedule.endTime);

    // 종일 일정은 사용자 캘린더처럼 "날짜 + 종일"로 표시한다.
    if (Boolean(schedule.isAllDay)) {
        const dateText = formatAdminDateOnly(schedule.startTime || schedule.endTime);

        return '<i class="fa-regular fa-clock admin-calendar-detail-icon"></i><span>' + dateText + ' 종일</span>';
    }

    return '<i class="fa-regular fa-clock admin-calendar-detail-icon"></i><span>' + startText + ' ~ ' + endText + '</span>';
}

// 반복 일정 여부에 따라 상세 팝업의 반복 뱃지를 표시한다.
function renderAdminRepeatBadge(schedule) {
    const badge = document.getElementById('adminCalendarDetailRepeatBadge');

    if (!badge) {
        return;
    }

    badge.hidden = !schedule?.repeatRule;
}

// 서버에서 받은 날짜 문자열을 한국어 날짜/시간 표기로 변환한다.
// 사용자 캘린더처럼 날짜와 오전/오후 사이에는 공백을 넉넉히 둔다.
function formatAdminDateTime(value) {
    if (!value) {
        return '';
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return '';
    }

    const dateText = date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    }).replace(/\. /g, '-').replace('.', '');

    const timeText = date.toLocaleTimeString('ko-KR', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
    });

    return dateText + '  ' + timeText;
}

// 종일 일정 상세팝업에서 시간 없이 날짜만 표시한다.
function formatAdminDateOnly(value) {
    if (!value) {
        return '';
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return '';
    }

    return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    }).replace(/\. /g, '-').replace('.', '');
}

// 작성자 이름과 일정 구분을 함께 보여준다.
function getAdminScheduleOwnerLabel(schedule) {
    const type = schedule.type || 'PERSONAL';

    // 사용자 캘린더처럼 조직 일정은 작성자보다 일정 범위를 우선 보여준다.
    if (type === 'DEPARTMENT') {
        return '부서 일정';
    }

    if (type === 'COMPANY') {
        return '전사 일정';
    }

    const creatorNo = getAdminMemberEmpNo({
        empNo: schedule?.creatorNo,
        empId: schedule?.creatorId
    });
    const loginEmpNo = String(getAdminLoginEmpNo() || '');

    // 로그인한 관리자의 개인일정은 작성자명이 아니라 "내 일정"으로 보여준다.
    if (!creatorNo || creatorNo === loginEmpNo) {
        return '내 일정';
    }

    return schedule.creatorName
        ? schedule.creatorName + ' 일정'
        : '작성자 일정';
}

// DB 카테고리 코드를 화면용 한글 라벨로 바꾼다.
function getAdminCategoryLabel(category) {
    const labels = {
        MEETING: '회의',
        WORK: '업무',
        NOTICE: '공지',
        ETC: '기타'
    };

    return labels[normalizeCategory(category)] || '기타';
}

// 일정 구분 코드를 화면용 한글 라벨로 바꾼다.
function getAdminTypeLabel(type) {
    const labels = {
        PERSONAL: '개인일정',
        DEPARTMENT: '부서일정',
        COMPANY: '전사일정'
    };

    return labels[type] || '개인일정';
}

// 상세 팝업 상단 색상칩은 캘린더 셀 색상과 같은 기준으로 맞춘다.
function getAdminScheduleColor(scheduleOrType) {
    const type = typeof scheduleOrType === 'string'
        ? scheduleOrType
        : scheduleOrType?.type;

    if (type === 'DEPARTMENT') {
        return '#a78bfa';
    }

    if (type === 'COMPANY') {
        return '#f472b6';
    }

    if (typeof scheduleOrType !== 'string') {
        const creatorNo = getAdminMemberEmpNo({
            empNo: scheduleOrType?.creatorNo,
            empId: scheduleOrType?.creatorId
        });
        const loginEmpNo = getAdminLoginEmpNo();

        // 구성원 일정에서 체크한 멤버는 배정색으로 강조한다.
        if (creatorNo && adminCalendarSelectedMemberScheduleNos.has(creatorNo)) {
            return getAdminMemberScheduleColor(creatorNo);
        }

        // 관리자 본인 개인일정은 사용자 캘린더 기준 기본 파란색을 유지한다.
        if (!creatorNo || creatorNo === loginEmpNo) {
            return '#3b82f6';
        }

        // 체크하지 않은 다른 멤버의 공개 개인일정은 내 일정과 구분되는 기본 타인 색으로 표시한다.
        return '#64748b';
    }

    return '#3b82f6';
}

// 종일 라벨은 원색보다 옅은 배경을 써서 사용자 캘린더 바 라벨처럼 보이게 한다.
function getAdminAllDayBackgroundColor(color) {
    const backgrounds = {
        '#3b82f6': '#dbeafe', // 내 일정
        '#64748b': '#e5e7eb', // 다른 멤버 기본
        '#10b981': '#d1fae5',
        '#f59e0b': '#fef3c7',
        '#ef4444': '#fee2e2',
        '#8b5cf6': '#ede9fe',
        '#06b6d4': '#cffafe',
        '#ec4899': '#fce7f3',
        '#84cc16': '#ecfccb',
        '#f97316': '#ffedd5',
        '#14b8a6': '#ccfbf1',
        '#6366f1': '#e0e7ff',
        '#a855f7': '#f3e8ff',
        '#22c55e': '#dcfce7',
        '#eab308': '#fef9c3',
        '#0ea5e9': '#e0f2fe',
        '#d946ef': '#fae8ff'
    };

    return backgrounds[color] || '#e5e7eb';
}

// 구성원 사번을 기준으로 항상 같은 색을 반환한다.
// 선택 목록 순서를 우선 사용하고, 목록에 없으면 해시로 팔레트 안에서 안정적으로 배정한다.
function getAdminMemberScheduleColor(empNo) {
    const normalizedEmpNo = String(empNo || '').trim();
    const visibleMemberNos = [
        ...adminCalendarMemberScheduleMembers,
        ...adminCalendarMemberOrgMembers
    ]
        .map(getAdminMemberEmpNo)
        .filter(Boolean)
        .filter(function (value, index, array) {
            return array.indexOf(value) === index;
        })
        .sort();
    const visibleIndex = visibleMemberNos.indexOf(normalizedEmpNo);

    if (visibleIndex >= 0) {
        return adminCalendarMemberColorPalette[visibleIndex % adminCalendarMemberColorPalette.length];
    }

    let hash = 0;

    for (let index = 0; index < normalizedEmpNo.length; index += 1) {
        hash = (hash * 31 + normalizedEmpNo.charCodeAt(index)) % adminCalendarMemberColorPalette.length;
    }

    return adminCalendarMemberColorPalette[Math.abs(hash) % adminCalendarMemberColorPalette.length];
}

function renderAdminCalendarEvent(eventInfo) {

    if (eventInfo.event.extendedProps?.source === 'HOLIDAY') {
        return {
            html:
                '<div class="admin-calendar-holiday-label">' +
                    escapeHtml(eventInfo.event.title || '공휴일') +
                '</div>'
        };
    }

    if (eventInfo.event.extendedProps?.source === 'ABSENCE') {
        const color = eventInfo.event.extendedProps.absenceColor || getAdminAbsenceEventColor('LEAVE');

        return {
            html:
                '<div class="admin-calendar-absence-label" ' +
                    'style="background:' + color.bg + '; border-color:' + color.border + '; color:' + color.text + ';">' +
                    escapeHtml(eventInfo.event.title || '') +
                '</div>'
        };
    }

    const schedule = eventInfo.event.extendedProps || {};
    const type = schedule.type || 'PERSONAL';
    const isAllDay = eventInfo.event.allDay;

    // FullCalendar 이벤트 값을 화면 출력용 문자열로 정리한다.
    const title = escapeHtml(eventInfo.event.title || '(제목 없음)');
    const timeText = eventInfo.timeText || '';

    // 구성원 일정 필터로 추가된 개인일정이면 해당 구성원 색상을 우선 사용한다.
    const creatorNo = getAdminMemberEmpNo({
        empNo: schedule.creatorNo,
        empId: schedule.creatorId,
        emp_no: schedule.creator_no
    });
    const memberColor = schedule.isSelectedMemberSchedule
        ? getAdminMemberScheduleColor(creatorNo)
        : null;

    // 종일 개인일정도 내 일정/다른 멤버/선택 멤버 색상 정책을 그대로 따른다.
    const eventColor = memberColor || getAdminScheduleColor(schedule);

    // 내가 초대받은 일정이면 참석 상태에 따라 캘린더 셀 표시를 다르게 보여준다.
    const myParticipant = getAdminMyParticipant(schedule);
    const participantStatusClass = myParticipant?.status === 'REJECTED'
        ? ' admin-calendar-event-participant-rejected'
        : myParticipant?.status === 'PENDING'
            ? ' admin-calendar-event-participant-pending'
            : '';

    if (type === 'DEPARTMENT' || type === 'COMPANY') {
        const scopeLabel = type === 'DEPARTMENT' ? '부서' : '전사';
        const companyClass = type === 'COMPANY' ? ' admin-calendar-event-bar-company' : '';

        return {
            html:
                '<div class="admin-calendar-event-bar' + companyClass + participantStatusClass + '">' +
                    '<span class="admin-calendar-event-scope">' + scopeLabel + '</span>' +
                    (!isAllDay && timeText ? '<span class="admin-calendar-event-time">' + escapeHtml(timeText) + '</span>' : '') +
                    '<span class="admin-calendar-event-title">' + title + '</span>' +
                '</div>'
        };
    }

    return {
        html:
            '<div class="admin-calendar-event-line' + participantStatusClass + '" ' +
                'style="--admin-calendar-event-color:' + eventColor + ';">' +
                '<span class="admin-calendar-event-dot" style="background:' + eventColor + '"></span>' +
                (!isAllDay && timeText ? '<span class="admin-calendar-event-time">' + escapeHtml(timeText) + '</span>' : '') +
                '<span class="admin-calendar-event-text">' + title + '</span>' +
            '</div>'
    };
}

// 관리자 등록/수정 팝업의 이벤트를 새 HTML 구조에 맞게 연결한다.
function bindAdminCalendarForm(calendar) {
    document.getElementById('adminCalendarSimpleClose')?.addEventListener('click', closeAdminCalendarForm);
    document.getElementById('adminCalendarSimpleCancel')?.addEventListener('click', closeAdminCalendarForm);
    document.getElementById('adminCalendarDetailFormClose')?.addEventListener('click', closeAdminCalendarForm);
    document.getElementById('adminCalendarDetailFormCancel')?.addEventListener('click', closeAdminCalendarForm);

    document.getElementById('adminCalendarSimpleAllDay')?.addEventListener('change', function () {
        syncAdminCalendarSimpleAllDayInputs();
        syncAdminCalendarDraftEvent(calendar);
    });
    document.getElementById('adminCalendarDetailFormAllDay')?.addEventListener('change', syncAdminCalendarDetailAllDayInputs);
    document.getElementById('adminCalendarDetailFormType')?.addEventListener('change', syncAdminCalendarVisibilityByType);

    // 간단등록 입력값은 사용자 캘린더처럼 캘린더 셀의 임시 일정과 실시간으로 맞춘다.
    [
        'adminCalendarSimpleTitle',
        'adminCalendarSimpleStartTime',
        'adminCalendarSimpleEndTime',
        'adminCalendarSimpleCategory'
    ].forEach(function (id) {
        const target = document.getElementById(id);

        if (!target) {
            return;
        }

        target.addEventListener('input', function () {
            syncAdminCalendarDraftEvent(calendar);
        });

        target.addEventListener('change', function () {
            syncAdminCalendarDraftEvent(calendar);
        });
    });

    document.getElementById('adminCalendarSimpleParticipantButton')?.addEventListener('click', openAdminParticipantModal);
    document.getElementById('adminCalendarDetailParticipantButton')?.addEventListener('click', openAdminParticipantModal);

    bindAdminParticipantModalEvents();

    // 간단등록 팝업 바깥을 누르면 사용자 캘린더처럼 팝업을 닫는다.
    document.addEventListener('mousedown', function (event) {
        if (event.target.closest('#adminCalendarParticipantModal')) {
            return;
        }

        const detailLayer = document.getElementById('adminCalendarDetailFormLayer');
        const detailPopup = document.getElementById('adminCalendarDetailFormPopup');

        if (detailLayer && !detailLayer.hidden) {
            if (detailPopup && detailPopup.contains(event.target)) {
                return;
            }

            closeAdminCalendarDetailForm();
            return;
        }

        const simplePopup = document.getElementById('adminCalendarSimpleAddPopup');

        if (!simplePopup || simplePopup.hidden) {
            return;
        }

        if (event.target.closest('#adminCalendarSimpleAddPopup')) {
            return;
        }

        closeAdminCalendarSimpleForm();
    });

    document.getElementById('adminCalendarSimpleDetailButton')?.addEventListener('click', function () {
        openAdminCalendarForm(calendar, {
            mode: 'create-detail',
            dateStr: getFormValue('adminCalendarSimpleDate'),
            dayEl: adminCalendarFormAnchorEl,
            fromSimple: true
        });
    });

    document.getElementById('adminCalendarSimpleForm')?.addEventListener('submit', async function (event) {
        event.preventDefault();

        if (adminCalendarSaving) {
            return;
        }

        try {
            // 간단등록은 상세등록으로 넘기지 않고 바로 저장한다.
            const payload = buildAdminCalendarSimplePayload();

            if (!validateAdminCalendarPayload(payload)) {
                return;
            }

            adminCalendarSaving = true;
            const response = await fetch('/admin/calendar/schedules', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                throw new Error(await getAdminCalendarErrorMessage(response, '관리자 간단 일정 등록 실패'));
            }

            showAdminCalendarToast('일정이 등록되었습니다.');
            closeAdminCalendarSimpleForm();
            calendar.refetchEvents();
        } catch (error) {
            console.error(error);
            showAdminCalendarToast(error.message || '일정 등록 중 오류가 발생했습니다.', 'error');
        } finally {
            adminCalendarSaving = false;
        }
    });

    document.getElementById('adminCalendarDetailForm')?.addEventListener('submit', async function (event) {
        event.preventDefault();

        if (adminCalendarSaving) {
            return;
        }

        try {
            const scheduleId = document.getElementById('adminCalendarDetailFormScheduleId').value;
            const payload = buildAdminCalendarPayload();

            if (!validateAdminCalendarPayload(payload)) {
                return;
            }

            const isEdit = Boolean(scheduleId);

            adminCalendarSaving = true;
            const response = await fetch(
                isEdit ? '/admin/calendar/schedules/' + scheduleId : '/admin/calendar/schedules',
                {
                    method: isEdit ? 'PUT' : 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(payload)
                }
            );

            if (!response.ok) {
                throw new Error(await getAdminCalendarErrorMessage(
                    response,
                    isEdit ? '관리자 일정 수정 실패' : '관리자 일정 등록 실패'
                ));
            }

            closeAdminCalendarForm();
            closeAdminCalendarDetailPopup();
            calendar.refetchEvents();
            showAdminCalendarToast(isEdit ? '일정이 수정되었습니다.' : '일정이 등록되었습니다.');
        } catch (error) {
            console.error(error);
            showAdminCalendarToast(error.message || '일정 저장 중 오류가 발생했습니다.', 'error');
        } finally {
            adminCalendarSaving = false;
        }
    });
}

// 관리자 등록/수정 팝업을 연다.
// create는 간단등록, create-detail/edit은 상세등록/수정으로 분기한다.
function openAdminCalendarForm(calendar, options) {
    const isEdit = options?.mode === 'edit';
    const isDetailMode = isEdit || options?.mode === 'create-detail';

    if (isDetailMode) {
        closeAdminCalendarSimpleForm();
        closeAdminParticipantModal();
        openAdminCalendarDetailForm(options);
        return;
    }

    closeAdminCalendarFloatingLayers();
    openAdminCalendarSimpleForm({
        ...options,
        calendar: calendar
    });
}

// 간단등록 팝업을 연다.
// 날짜 셀 클릭 시 기존 상세/수정/참석자 모달을 모두 닫고 하나의 간단등록만 유지한다.
function openAdminCalendarSimpleForm(options) {
    const popup = document.getElementById('adminCalendarSimpleAddPopup');

    if (!popup) {
        return;
    }

    closeAdminCalendarDetailPopup();
    closeAdminCalendarDetailForm();
    closeAdminParticipantModal();

    adminCalendarFormParticipants = [];

    setFormValue('adminCalendarSimpleDate', options?.dateStr || '');
    setTextContent('adminCalendarSimpleDateText', options?.dateStr || '');
    setFormValue('adminCalendarSimpleTitle', '');
    setFormValue('adminCalendarSimpleCategory', 'MEETING');
    const defaultTimeRange = getAdminDefaultTimeRange();

    setFormValue('adminCalendarSimpleStartTime', defaultTimeRange.startTime);
    setFormValue('adminCalendarSimpleEndTime', defaultTimeRange.endTime);
    setFormValue('adminCalendarSimpleLocation', '');

    syncAdminCalendarSimpleAllDayInputs();
    renderAdminCalendarFormParticipants();
    // 상세등록으로 넘어갈 때도 같은 날짜 셀 옆에 뜨도록 기준 요소를 보관한다.
    adminCalendarFormAnchorEl = options?.dayEl || null;

    positionAdminCalendarPopup(popup, options?.dayEl, 390, 520);
    syncAdminCalendarDraftEvent(options?.calendar);
    popup.hidden = false;
}

// 상세등록/수정 팝업을 연다.
// 사용자 캘린더의 상세등록/수정 팝업처럼 화면 상단 기준 fixed 카드로 띄운다.
function openAdminCalendarDetailForm(options) {
    const layer = document.getElementById('adminCalendarDetailFormLayer');
    const popup = document.getElementById('adminCalendarDetailFormPopup');
    const titleEl = document.getElementById('adminCalendarDetailFormTitle');
    const submitEl = document.getElementById('adminCalendarDetailFormSubmit');
    const schedule = options?.schedule || null;
    const isEdit = options?.mode === 'edit';
    const defaultTimeRange = getAdminDefaultTimeRange();

    if (!layer || !popup) {
        return;
    }

    closeAdminCalendarSimpleForm();
    closeAdminCalendarDetailPopup();
    closeAdminParticipantModal();

    adminCalendarFormParticipants = Array.isArray(schedule?.participants)
        ? schedule.participants
            .map(normalizeAdminParticipant)
            .filter(function (participant) {
                return participant.empId;
            })
        : adminCalendarFormParticipants;

    if (titleEl) {
        titleEl.textContent = isEdit ? '일정 수정' : '일정 상세 등록';
    }

    if (submitEl) {
        submitEl.textContent = isEdit ? '저장' : '등록';
    }

    setFormValue('adminCalendarDetailFormScheduleId', isEdit ? schedule.scheduleId : '');
    setFormValue('adminCalendarDetailFormCreatorNo', isEdit ? schedule.creatorNo : '');
    setFormValue(
        'adminCalendarDetailFormTitleInput',
        isEdit ? schedule.title : getFormValue('adminCalendarSimpleTitle')
    );
    setFormValue('adminCalendarDetailFormType', isEdit ? (schedule.type || 'PERSONAL') : 'PERSONAL');
    setFormValue(
        'adminCalendarDetailFormCategory',
        isEdit ? normalizeCategory(schedule.category) : getFormValue('adminCalendarSimpleCategory')
    );
    setFormValue(
        'adminCalendarDetailFormDate',
        isEdit ? toAdminDateValue(schedule.startTime) : (options?.dateStr || getFormValue('adminCalendarSimpleDate'))
    );
    setFormValue(
        'adminCalendarDetailFormStartTime',
        isEdit ? toAdminTimeValue(schedule.startTime) : (getFormValue('adminCalendarSimpleStartTime') || defaultTimeRange.startTime)
    );
    setFormValue(
        'adminCalendarDetailFormEndTime',
        isEdit ? toAdminTimeValue(schedule.endTime) : (getFormValue('adminCalendarSimpleEndTime') || defaultTimeRange.endTime)
    );
    setFormValue(
        'adminCalendarDetailFormLocation',
        isEdit ? (schedule.location || '') : getFormValue('adminCalendarSimpleLocation')
    );
    setFormValue('adminCalendarDetailFormContent', isEdit ? (schedule.content || '') : '');
    // 수정 팝업에서는 기존 반복 설정을 복원하고, 신규 등록은 반복 없음으로 시작한다.
    setFormValue('adminCalendarDetailFormRepeatRule', isEdit ? (schedule.repeatRule || '') : '');

    const allDayInput = document.getElementById('adminCalendarDetailFormAllDay');

    if (allDayInput) {
        allDayInput.checked = isEdit
            ? Boolean(schedule?.isAllDay)
            : Boolean(document.getElementById('adminCalendarSimpleAllDay')?.checked);
    }

    if (isEdit) {
        setFormValue('adminCalendarDetailFormVisibility', getAdminVisibilityValue(schedule));
    } else {
        // 신규 개인일정은 기본 비공개로 시작한다. 참석자 선택 시 payload 생성 단계에서 공개로 전환된다.
        setFormValue('adminCalendarDetailFormVisibility', 'PRIVATE');
    }

    syncAdminCalendarDetailAllDayInputs();
    syncAdminCalendarVisibilityByType();
    renderAdminCalendarFormParticipants();

    layer.hidden = false;

    // 선택한 날짜 셀 또는 일정 요소 옆에 상세등록/수정 팝업을 배치한다.
    positionAdminCalendarDetailFormPopup(popup, options?.dayEl || options?.eventEl || adminCalendarFormAnchorEl);
}

// 관리자 등록/수정 팝업을 모두 닫는다.
function closeAdminCalendarForm() {
    closeAdminCalendarSimpleForm();
    closeAdminCalendarDetailForm();
}

function closeAdminCalendarSimpleForm() {
    const popup = document.getElementById('adminCalendarSimpleAddPopup');

    if (popup) {
        popup.hidden = true;
    }

    clearAdminCalendarDraftEvent();
}

function closeAdminCalendarDetailForm() {
    const layer = document.getElementById('adminCalendarDetailFormLayer');

    if (layer) {
        layer.hidden = true;
    }
}

// 간단등록 폼 값을 서버 DTO 형식으로 변환한다.
function buildAdminCalendarSimplePayload() {
    const isAllDay = document.getElementById('adminCalendarSimpleAllDay')?.checked;
    const participantNos = adminCalendarFormParticipants
        .map(getAdminMemberEmpNo)
        .filter(Boolean);

    return {
        title: getFormValue('adminCalendarSimpleTitle'),
        content: '',
        startTime: buildAdminDateTime('adminCalendarSimpleDate', 'adminCalendarSimpleStartTime', isAllDay, true),
        endTime: buildAdminDateTime('adminCalendarSimpleDate', 'adminCalendarSimpleEndTime', isAllDay, false),
        type: 'PERSONAL',
        creatorNo: '',
        category: getFormValue('adminCalendarSimpleCategory'),
        location: getFormValue('adminCalendarSimpleLocation'),
        isAllDay: isAllDay,
        // 개인일정은 기본 비공개로 저장하고, 참석자가 있으면 초대 대상에게 보여야 하므로 공개로 저장한다.
        isPublic: participantNos.length > 0,
        repeatRule: null,
        participantNos: participantNos
    };
}

// 일정 범위 드롭다운 안에서 구성원 공개 일정을 선택/해제한다.
// 선택값은 서버 조회 파라미터와 화면 색상 계산에 함께 사용한다.
function bindAdminMemberScheduleFilters(calendar) {
    document.getElementById('adminCalendarMemberOrgButton')?.addEventListener('click', function (event) {
        event.stopPropagation();
        toggleAdminMemberOrgPanel();
    });

    document.getElementById('adminCalendarMemberOrgDeptSelect')?.addEventListener('change', function (event) {
        adminCalendarMemberOrgDeptId = event.target.value;
        loadAdminMemberOrgMembers(adminCalendarMemberOrgDeptId, calendar);
    });

    loadAdminMemberScheduleMembers(calendar);
}

async function loadAdminMemberScheduleMembers(calendar) {
    const listEl = document.getElementById('adminCalendarMemberScheduleList');

    if (!listEl) {
        return;
    }

    try {
        listEl.innerHTML = '<div class="admin-calendar-member-empty">구성원 목록을 불러오는 중입니다.</div>';
        const employees = await fetchAdminEmployeesWithFallback();

        // 구성원 일정 목록에는 로그인한 본인은 표시하지 않는다.
        // 본인 일정은 별도 "내 일정" 필터에서 제어한다.
        const loginEmpNo = getAdminLoginEmpNo();

        adminCalendarSelectedMemberScheduleNos.delete(String(loginEmpNo));

        adminCalendarMemberScheduleMembers = normalizeAdminEmployees(employees)
            .filter(function (employee) {
                return String(getAdminMemberEmpNo(employee)) !== String(loginEmpNo);
            });

        renderAdminMemberScheduleList(calendar);
    } catch (error) {
        console.error(error);
        adminCalendarMemberScheduleMembers = [];
        listEl.innerHTML = '<div class="admin-calendar-member-empty">구성원 목록을 불러오지 못했습니다.</div>';
    }
}

function renderAdminMemberScheduleList(calendar) {
    const listEl = document.getElementById('adminCalendarMemberScheduleList');
    const countEl = document.getElementById('adminCalendarMemberScheduleCount');

    if (!listEl) {
        return;
    }

    const members = adminCalendarMemberScheduleMembers;

    if (!members.length) {
        listEl.innerHTML = '<div class="admin-calendar-member-empty">구성원이 없습니다.</div>';
    } else {
        listEl.innerHTML = members.map(function (member) {
            const empNo = getAdminMemberEmpNo(member);
            const checked = adminCalendarSelectedMemberScheduleNos.has(empNo);
            const color = getAdminMemberScheduleColor(empNo);

            return ''
                + '<label class="admin-calendar-member-row">'
                + '  <input type="checkbox" data-admin-member-schedule-no="' + escapeHtml(empNo) + '" ' + (checked ? 'checked' : '') + '>'
                + '  <span class="admin-calendar-member-color-dot" style="background:' + color + '"></span>'
                + '  <span class="admin-calendar-member-name">' + escapeHtml(member.name || empNo) + '</span>'
                + '  <span class="admin-calendar-member-position">' + escapeHtml(member.positionName || '') + '</span>'
                + '</label>';
        }).join('');
    }

    listEl.querySelectorAll('[data-admin-member-schedule-no]').forEach(function (checkbox) {
        checkbox.addEventListener('change', function () {
            if (checkbox.checked) {
                adminCalendarSelectedMemberScheduleNos.add(checkbox.dataset.adminMemberScheduleNo);
            } else {
                adminCalendarSelectedMemberScheduleNos.delete(checkbox.dataset.adminMemberScheduleNo);
            }

            renderAdminMemberScheduleList(calendar);
            calendar.refetchEvents();
        });
    });

    if (countEl) {
        const count = adminCalendarSelectedMemberScheduleNos.size;
        countEl.textContent = count ? '선택된 구성원 ' + count + '명' : '';
    }
}

async function toggleAdminMemberOrgPanel() {
    const panel = document.getElementById('adminCalendarMemberOrgPanel');

    if (!panel) {
        return;
    }

    const willOpen = panel.hasAttribute('hidden');

    if (!willOpen) {
        panel.setAttribute('hidden', 'hidden');
        return;
    }

    panel.removeAttribute('hidden');
    await loadAdminMemberOrgDepartments();
}

async function loadAdminMemberOrgDepartments() {
    const deptSelect = document.getElementById('adminCalendarMemberOrgDeptSelect');

    if (!deptSelect) {
        return;
    }

    try {
        const response = await fetch('/api/organization/departments/tree');

        if (!response.ok) {
            throw new Error('조직도 부서 조회 실패');
        }

        adminCalendarMemberOrgDepartments = normalizeAdminArrayResponse(await response.json());
        deptSelect.innerHTML = '<option value="">부서를 선택해주세요.</option>' +
            flattenAdminDepartments(adminCalendarMemberOrgDepartments)
                .map(function (department) {
                    return '<option value="' + escapeHtml(String(department.deptId)) + '">' +
                        escapeHtml(department.label) +
                        '</option>';
                })
                .join('');
    } catch (error) {
        console.error(error);
        deptSelect.innerHTML = '<option value="">부서를 불러오지 못했습니다.</option>';
    }
}

async function loadAdminMemberOrgMembers(deptId, calendar) {
    const orgListEl = document.getElementById('adminCalendarMemberOrgList');

    if (!orgListEl) {
        return;
    }

    if (!deptId) {
        orgListEl.innerHTML = '<div class="admin-calendar-member-empty">부서를 선택해주세요.</div>';
        return;
    }

    try {
        orgListEl.innerHTML = '<div class="admin-calendar-member-empty">구성원 목록을 불러오는 중입니다.</div>';
        const response = await fetch('/api/organization/employees?deptId=' + encodeURIComponent(deptId));

        if (!response.ok) {
            throw new Error('조직도 구성원 조회 실패');
        }

        // 조직도에서 선택한 구성원 목록에도 로그인한 본인은 표시하지 않는다.
        const loginEmpNo = getAdminLoginEmpNo();

        adminCalendarSelectedMemberScheduleNos.delete(String(loginEmpNo));

        adminCalendarMemberOrgMembers = normalizeAdminEmployees(await response.json())
            .filter(function (employee) {
                return String(getAdminMemberEmpNo(employee)) !== String(loginEmpNo);
            });

        renderAdminMemberOrgList(calendar);
    } catch (error) {
        console.error(error);
        adminCalendarMemberOrgMembers = [];
        orgListEl.innerHTML = '<div class="admin-calendar-member-empty">구성원을 불러오지 못했습니다.</div>';
    }
}

function renderAdminMemberOrgList(calendar) {
    const orgListEl = document.getElementById('adminCalendarMemberOrgList');

    if (!orgListEl) {
        return;
    }

    if (!adminCalendarMemberOrgMembers.length) {
        orgListEl.innerHTML = '<div class="admin-calendar-member-empty">구성원이 없습니다.</div>';
        return;
    }

    orgListEl.innerHTML = adminCalendarMemberOrgMembers.map(function (member) {
        const empNo = getAdminMemberEmpNo(member);
        const checked = adminCalendarSelectedMemberScheduleNos.has(empNo);
        const color = getAdminMemberScheduleColor(empNo);

        return ''
            + '<label class="admin-calendar-member-row">'
            + '  <input type="checkbox" data-admin-org-member-no="' + escapeHtml(empNo) + '" ' + (checked ? 'checked' : '') + '>'
            + '  <span class="admin-calendar-member-color-dot" style="background:' + color + '"></span>'
            + '  <span class="admin-calendar-member-name">' + escapeHtml(member.name || empNo) + '</span>'
            + '  <span class="admin-calendar-member-position">' + escapeHtml(member.positionName || '') + '</span>'
            + '</label>';
    }).join('');

    orgListEl.querySelectorAll('[data-admin-org-member-no]').forEach(function (checkbox) {
        checkbox.addEventListener('change', function () {
            if (checkbox.checked) {
                adminCalendarSelectedMemberScheduleNos.add(checkbox.dataset.adminOrgMemberNo);
            } else {
                adminCalendarSelectedMemberScheduleNos.delete(checkbox.dataset.adminOrgMemberNo);
            }

            renderAdminMemberScheduleList(calendar);
            renderAdminMemberOrgList(calendar);
            calendar.refetchEvents();
        });
    });
}

// 등록/수정 폼 값을 서버 DTO 형식으로 변환한다.
function buildAdminCalendarPayload() {
    const type = getFormValue('adminCalendarDetailFormType');
    const visibility = getFormValue('adminCalendarDetailFormVisibility');
    const repeatRule = getFormValue('adminCalendarDetailFormRepeatRule');
    const isAllDay = document.getElementById('adminCalendarDetailFormAllDay')?.checked;
    const participantNos = adminCalendarFormParticipants
        .map(getAdminMemberEmpNo)
        .filter(Boolean);

    return {
        title: getFormValue('adminCalendarDetailFormTitleInput'),
        content: getFormValue('adminCalendarDetailFormContent'),
        startTime: buildAdminDateTime('adminCalendarDetailFormDate', 'adminCalendarDetailFormStartTime', isAllDay, true),
        endTime: buildAdminDateTime('adminCalendarDetailFormDate', 'adminCalendarDetailFormEndTime', isAllDay, false),
        type: type,
        creatorNo: getFormValue('adminCalendarDetailFormCreatorNo'),
        category: getFormValue('adminCalendarDetailFormCategory'),
        location: getFormValue('adminCalendarDetailFormLocation'),
        isAllDay: isAllDay,
        isPublic: type === 'PERSONAL' ? visibility !== 'PRIVATE' || participantNos.length > 0 : true,
        // 사용자 캘린더 기준으로 반복 일정은 개인일정에서만 저장한다.
        repeatRule: type === 'PERSONAL' && repeatRule ? repeatRule : null,
        participantNos: participantNos
    };
}

// 저장 전에 화면에서 먼저 기본 입력값과 시간 순서를 검증한다.
function validateAdminCalendarPayload(payload) {
    const title = String(payload?.title || '').trim();
    const startDate = new Date(payload?.startTime || '');
    const endDate = new Date(payload?.endTime || '');

    if (!title) {
        showAdminCalendarToast('제목을 입력해 주세요.', 'error');
        return false;
    }

    if (!payload?.startTime || Number.isNaN(startDate.getTime())) {
        showAdminCalendarToast('시작 시간을 확인해 주세요.', 'error');
        return false;
    }

    if (!payload?.endTime || Number.isNaN(endDate.getTime())) {
        showAdminCalendarToast('종료 시간을 확인해 주세요.', 'error');
        return false;
    }

    if (endDate < startDate) {
        showAdminCalendarToast('종료 시간은 시작 시간보다 빠를 수 없습니다.', 'error');
        return false;
    }

    return true;
}

// 참석자 선택 모달의 버튼/검색/전체 선택 이벤트를 한 번만 연결한다.
function bindAdminParticipantModalEvents() {
    document.getElementById('adminCalendarParticipantClose')?.addEventListener('click', closeAdminParticipantModal);
    document.getElementById('adminCalendarParticipantCancel')?.addEventListener('click', closeAdminParticipantModal);
    document.getElementById('adminCalendarParticipantApply')?.addEventListener('click', applyAdminParticipantSelection);

    document.getElementById('adminCalendarParticipantKeyword')?.addEventListener('input', renderAdminParticipantList);

    document.getElementById('adminCalendarParticipantSelectAll')?.addEventListener('change', function (event) {
        toggleAdminParticipantSelectAll(event.target.checked);
    });

    document.getElementById('adminCalendarParticipantTeamMode')?.addEventListener('click', function () {
        adminCalendarParticipantMode = 'TEAM';
        adminCalendarOrgDeptId = '';
        hideAdminParticipantDepartmentPicker();
        syncAdminParticipantModeControls();
        loadAdminParticipantMembers();
    });

    document.getElementById('adminCalendarParticipantOrgMode')?.addEventListener('click', function () {
        adminCalendarParticipantMode = 'ORG';
        syncAdminParticipantModeControls();
        loadAdminParticipantDepartments();
    });

    document.getElementById('adminCalendarParticipantDeptSelect')?.addEventListener('change', function (event) {
        adminCalendarOrgDeptId = event.target.value;
        loadAdminParticipantMembersByDept(adminCalendarOrgDeptId);
    });
}

// 참석자 선택 모달을 연다.
// 등록/수정 팝업은 유지하고 참석자 모달만 가장 위에 띄운다.
function openAdminParticipantModal() {
    const modal = document.getElementById('adminCalendarParticipantModal');

    if (!modal) {
        return;
    }

    adminCalendarParticipantDraft = adminCalendarFormParticipants.map(function (participant) {
        return { ...participant };
    });

    adminCalendarParticipantMode = 'TEAM';
    adminCalendarOrgDeptId = '';

    setFormValue('adminCalendarParticipantKeyword', '');
    hideAdminParticipantDepartmentPicker();
    syncAdminParticipantModeControls();

    modal.hidden = false;
    loadAdminParticipantMembers();
}

// 참석자 모달을 닫고 검색/조직도 임시 상태를 초기화한다.
function closeAdminParticipantModal() {
    const modal = document.getElementById('adminCalendarParticipantModal');

    if (modal) {
        modal.hidden = true;
    }

    adminCalendarParticipantMode = 'TEAM';
    adminCalendarOrgDeptId = '';
    adminCalendarParticipantMembers = [];
    setFormValue('adminCalendarParticipantKeyword', '');
    hideAdminParticipantDepartmentPicker();
    syncAdminParticipantModeControls();
}

// 현재 선택된 참석자를 실제 폼 참석자 목록에 반영한다.
function applyAdminParticipantSelection() {
    adminCalendarFormParticipants = adminCalendarParticipantDraft.map(function (participant) {
        return { ...participant };
    });

    // 참석자가 있으면 개인일정 비공개 상태를 공개로 자동 전환한다.
    if (adminCalendarFormParticipants.length > 0 && getFormValue('adminCalendarDetailFormVisibility') === 'PRIVATE') {
        setFormValue('adminCalendarDetailFormVisibility', 'COMPANY');
    }

    renderAdminCalendarFormParticipants();
    closeAdminParticipantModal();
}

// 관리자 참석자 기본 목록은 부서 제한 없이 전체 구성원을 보여준다.
async function loadAdminParticipantMembers() {
    try {
        setAdminParticipantListMessage('구성원 목록을 불러오는 중입니다.');

        // 참석자 선택도 일정 범위 구성원 필터와 같은 API fallback/정규화 흐름을 사용한다.
        const employees = await fetchAdminEmployeesWithFallback();
        adminCalendarParticipantMembers = normalizeAdminEmployees(employees);

        setAdminParticipantModeTitle('팀 멤버');
        renderAdminParticipantList();
    } catch (error) {
        console.error(error);
        adminCalendarParticipantMembers = [];
        setAdminParticipantListMessage('구성원 목록을 불러오지 못했습니다.');
    }
}

// 관리자 참석자/구성원 일정 목록에서 사용할 전체 구성원을 불러온다.
// 전체 구성원 API가 실패하거나 비어 있으면 조직도 부서별 구성원 조회로 보완한다.
async function fetchAdminEmployeesWithFallback() {
    // 1차: 전체 구성원 API를 먼저 사용한다.
    // 관리자 캘린더의 참석자 목록과 구성원 일정 필터는 전체 멤버를 기준으로 보여준다.
    try {
        const response = await fetch('/api/organization/employees/all');

        if (response.ok) {
            const employees = normalizeAdminEmployees(await response.json());

            if (employees.length > 0) {
                return employees;
            }
        }
    } catch (error) {
        console.warn('관리자 캘린더 전체 구성원 조회 실패:', error);
    }

    // 2차: 전체 구성원 API가 실패하거나 빈 배열이면 조직도 부서 트리를 순회해서 구성원을 모은다.
    // 조직도 API가 살아 있으면 참석자/구성원 목록을 최대한 복구할 수 있다.
    return fetchAdminEmployeesFromDepartmentTree();
}

async function fetchAdminEmployeesFromDepartmentTree() {
    // 전체 구성원 API가 비어 있을 때 사용하는 fallback이다.
    // 부서 트리를 펼친 뒤 각 부서별 구성원 API를 호출해서 중복 없이 합친다.
    try {
        const deptResponse = await fetch('/api/organization/departments/tree');

        if (!deptResponse.ok) {
            return [];
        }

        const departments = normalizeAdminArrayResponse(await deptResponse.json());
        const flatDepartments = flattenAdminDepartments(departments);
        const employeeMap = new Map();

        for (const department of flatDepartments) {
            if (!department.deptId) {
                continue;
            }

            try {
                const employeeResponse = await fetch('/api/organization/employees?deptId=' + encodeURIComponent(department.deptId));

                if (!employeeResponse.ok) {
                    continue;
                }

                normalizeAdminEmployees(await employeeResponse.json()).forEach(function (employee) {
                    if (employee.empId) {
                        employeeMap.set(employee.empId, employee);
                    }
                });
            } catch (error) {
                console.warn('관리자 캘린더 부서 구성원 조회 실패:', department.deptId, error);
            }
        }

        return Array.from(employeeMap.values());
    } catch (error) {
        console.warn('관리자 캘린더 조직도 기반 구성원 조회 실패:', error);
        return [];
    }
}

async function loadAdminParticipantDepartments() {
    const deptSelect = document.getElementById('adminCalendarParticipantDeptSelect');
    const deptTree = document.getElementById('adminCalendarParticipantDeptTree');

    if (!deptSelect || !deptTree) {
        return;
    }

    try {
        setAdminParticipantModeTitle('조직도 멤버');
        setAdminParticipantListMessage('부서를 선택해주세요.');

        deptSelect.setAttribute('hidden', 'hidden');
        deptTree.hidden = false;
        deptTree.innerHTML = '<div class="admin-calendar-participant-empty">조직도를 불러오는 중입니다.</div>';

        const response = await fetch('/api/organization/departments/tree');

        if (!response.ok) {
            throw new Error('조직도 부서 조회 실패');
        }

        adminCalendarOrgDepartments = await response.json();

        deptSelect.innerHTML = '<option value="">부서를 선택해주세요.</option>' +
            flattenAdminDepartments(adminCalendarOrgDepartments)
                .map(function (department) {
                    return '<option value="' + escapeHtml(String(department.deptId)) + '">' +
                        escapeHtml(department.label) +
                        '</option>';
                })
                .join('');

        renderAdminParticipantDepartmentTree();
        adminCalendarParticipantMembers = [];
        renderAdminParticipantList();
    } catch (error) {
        console.error(error);
        adminCalendarOrgDepartments = [];
        adminCalendarParticipantMembers = [];
        deptTree.innerHTML = '<div class="admin-calendar-participant-empty">부서를 불러오지 못했습니다.</div>';
        setAdminParticipantListMessage('조직도 부서 목록을 불러오지 못했습니다.');
    }
}


function hideAdminParticipantDepartmentPicker() {
    const deptSelect = document.getElementById('adminCalendarParticipantDeptSelect');
    const deptTree = document.getElementById('adminCalendarParticipantDeptTree');

    if (deptSelect) {
        deptSelect.setAttribute('hidden', 'hidden');
    }

    if (deptTree) {
        deptTree.hidden = true;
        deptTree.innerHTML = '';
    }
}

function syncAdminParticipantModeControls() {
    const teamModeButton = document.getElementById('adminCalendarParticipantTeamMode');
    const orgModeButton = document.getElementById('adminCalendarParticipantOrgMode');

    // The user calendar shows only the button that switches to the other mode.
    if (teamModeButton) {
        teamModeButton.hidden = adminCalendarParticipantMode === 'TEAM';
    }

    if (orgModeButton) {
        orgModeButton.hidden = adminCalendarParticipantMode === 'ORG';
    }
}

function renderAdminParticipantDepartmentTree() {
    const deptTree = document.getElementById('adminCalendarParticipantDeptTree');

    if (!deptTree) {
        return;
    }

    const departments = normalizeAdminArrayResponse(adminCalendarOrgDepartments);

    if (!departments.length) {
        deptTree.innerHTML = '<div class="admin-calendar-participant-empty">표시할 부서가 없습니다.</div>';
        return;
    }

    deptTree.innerHTML = buildAdminParticipantDepartmentButtons(departments, 0);
    deptTree.querySelectorAll('[data-admin-participant-dept-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            adminCalendarOrgDeptId = button.dataset.adminParticipantDeptId || '';
            renderAdminParticipantDepartmentTree();
            loadAdminParticipantMembersByDept(adminCalendarOrgDeptId);
        });
    });
}

function buildAdminParticipantDepartmentButtons(departments, depth) {
    return normalizeAdminArrayResponse(departments).map(function (department) {
        const deptId = String(department.deptId || '');
        const selected = deptId && String(adminCalendarOrgDeptId) === deptId;
        const children = normalizeAdminArrayResponse(department.children);
        const marker = children.length ? '▸' : '•';

        return '' +
            '<div class="admin-calendar-participant-dept-node">' +
                '<button type="button" class="admin-calendar-participant-dept-button ' + (selected ? 'is-selected' : '') + '" ' +
                    'style="padding-left: ' + (8 + depth * 14) + 'px" ' +
                    'data-admin-participant-dept-id="' + escapeHtml(deptId) + '">' +
                    '<span class="admin-calendar-participant-dept-marker">' + marker + '</span>' +
                    '<span>' + escapeHtml(department.deptName || department.name || department.label || '부서') + '</span>' +
                '</button>' +
                (children.length ? buildAdminParticipantDepartmentButtons(children, depth + 1) : '') +
            '</div>';
    }).join('');
}



// 조직도에서 선택한 부서의 구성원을 불러온다.
async function loadAdminParticipantMembersByDept(deptId) {
    if (!deptId) {
        adminCalendarParticipantMembers = [];
        setAdminParticipantListMessage('부서를 선택해주세요.');
        return;
    }

    try {
        setAdminParticipantListMessage('구성원 목록을 불러오는 중입니다.');

        const response = await fetch('/api/organization/employees?deptId=' + encodeURIComponent(deptId));

        if (!response.ok) {
            throw new Error('부서 구성원 조회 실패');
        }

        const employees = await response.json();
        adminCalendarParticipantMembers = normalizeAdminEmployees(employees);

        renderAdminParticipantList();
    } catch (error) {
        console.error(error);
        adminCalendarParticipantMembers = [];
        setAdminParticipantListMessage('구성원 목록을 불러오지 못했습니다.');
    }
}

// 서버 사원 DTO를 참석자 선택 UI에서 쓰는 형태로 통일한다.
function normalizeAdminEmployees(employees) {
    return normalizeAdminArrayResponse(employees)
        .filter(function (employee) {
            return employee && getAdminMemberEmpNo(employee);
        })
        .map(function (employee) {
            const empNo = getAdminMemberEmpNo(employee);

            return {
                empId: empNo,
                empNo: empNo,
                name: getAdminMemberName(employee),
                deptId: employee.deptId ?? employee.departmentId ?? employee.department?.deptId,
                deptName: employee.deptName || employee.departmentName || employee.department?.deptName || '',
                positionName: employee.positionName || employee.position?.positionName || '',
                profileImg: employee.profileImg || ''
            };
        });
}

// 부서 트리를 select에 넣기 좋게 평탄화한다.
function flattenAdminDepartments(departments, depth = 0) {
    return (departments || []).flatMap(function (department) {
        const prefix = depth > 0 ? '　'.repeat(depth) + 'ㄴ ' : '';
        const current = {
            deptId: department.deptId,
            label: prefix + department.deptName
        };

        return [
            current,
            ...flattenAdminDepartments(department.children || [], depth + 1)
        ];
    });
}

// 참석자 후보 목록을 검색어/선택상태에 맞춰 다시 그린다.
function renderAdminParticipantList() {
    const listEl = document.getElementById('adminCalendarParticipantList');
    const selectAllEl = document.getElementById('adminCalendarParticipantSelectAll');

    if (!listEl) {
        return;
    }

    const keyword = getFormValue('adminCalendarParticipantKeyword').toLowerCase();
    const visibleMembers = adminCalendarParticipantMembers.filter(function (member) {
        return !keyword ||
            member.name.toLowerCase().includes(keyword) ||
            String(member.empId).toLowerCase().includes(keyword);
    });

    if (!visibleMembers.length) {
        listEl.innerHTML = '<div class="admin-calendar-participant-empty">표시할 멤버가 없습니다.</div>';
        renderAdminParticipantSelectedList();

        if (selectAllEl) {
            selectAllEl.checked = false;
        }

        return;
    }

    listEl.innerHTML = visibleMembers.map(function (member) {
        const selected = isAdminParticipantSelected(member.empId);

        return '' +
            '<label class="admin-calendar-participant-card ' + (selected ? 'is-selected' : '') + '">' +
                '<input type="checkbox" data-admin-participant-id="' + escapeHtml(member.empId) + '" ' + (selected ? 'checked' : '') + '>' +
                '<span>' +
                    '<strong>' + escapeHtml(member.name) + '</strong>' +
                    '<small>' + escapeHtml(buildAdminParticipantSubText(member)) + '</small>' +
                '</span>' +
            '</label>';
    }).join('');

    listEl.querySelectorAll('[data-admin-participant-id]').forEach(function (checkbox) {
        checkbox.addEventListener('change', function () {
            const member = adminCalendarParticipantMembers.find(function (candidate) {
                return candidate.empId === checkbox.dataset.adminParticipantId;
            });

            if (member) {
                toggleAdminParticipant(member);
            }
        });
    });

    if (selectAllEl) {
        selectAllEl.checked = visibleMembers.length > 0 &&
            visibleMembers.every(function (member) {
                return isAdminParticipantSelected(member.empId);
            });
    }

    renderAdminParticipantSelectedList();
}

// 참석자 카드 하단에 부서/직급/사번을 표시한다.
function buildAdminParticipantSubText(member) {
    return [
        member.deptName,
        member.positionName,
        member.empId
    ].filter(Boolean).join(' · ');
}

// 특정 멤버 선택/해제.
function toggleAdminParticipant(member) {
    if (isAdminParticipantSelected(member.empId)) {
        adminCalendarParticipantDraft = adminCalendarParticipantDraft.filter(function (participant) {
            return participant.empId !== member.empId;
        });
    } else {
        adminCalendarParticipantDraft.push(member);
    }

    renderAdminParticipantList();
}

// 현재 목록 전체 선택/해제.
function toggleAdminParticipantSelectAll(checked) {
    const keyword = getFormValue('adminCalendarParticipantKeyword').toLowerCase();
    const visibleMembers = adminCalendarParticipantMembers.filter(function (member) {
        return !keyword ||
            member.name.toLowerCase().includes(keyword) ||
            String(member.empId).toLowerCase().includes(keyword);
    });

    const visibleIds = new Set(visibleMembers.map(function (member) {
        return member.empId;
    }));

    if (!checked) {
        adminCalendarParticipantDraft = adminCalendarParticipantDraft.filter(function (participant) {
            return !visibleIds.has(participant.empId);
        });

        renderAdminParticipantList();
        return;
    }

    const selectedIds = new Set(adminCalendarParticipantDraft.map(function (participant) {
        return participant.empId;
    }));

    visibleMembers.forEach(function (member) {
        if (!selectedIds.has(member.empId)) {
            adminCalendarParticipantDraft.push(member);
        }
    });

    renderAdminParticipantList();
}

// 선택된 참석자 태그 영역을 렌더링한다.
function renderAdminParticipantSelectedList() {
    const countEl = document.getElementById('adminCalendarParticipantSelectedCount');
    const selectedEl = document.getElementById('adminCalendarParticipantSelectedList');

    if (countEl) {
        countEl.textContent = '선택된 멤버 ' + adminCalendarParticipantDraft.length;
    }

    if (!selectedEl) {
        return;
    }

    if (!adminCalendarParticipantDraft.length) {
        selectedEl.textContent = '선택된 멤버가 없습니다.';
        return;
    }

    selectedEl.innerHTML = adminCalendarParticipantDraft.map(function (participant) {
        return '' +
            '<span class="admin-calendar-participant-tag">' +
                escapeHtml(participant.name || participant.empId) +
                '<button type="button" data-admin-selected-participant-id="' + escapeHtml(participant.empId) + '">×</button>' +
            '</span>';
    }).join('');

    selectedEl.querySelectorAll('[data-admin-selected-participant-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            adminCalendarParticipantDraft = adminCalendarParticipantDraft.filter(function (participant) {
                return participant.empId !== button.dataset.adminSelectedParticipantId;
            });

            renderAdminParticipantList();
        });
    });
}

function isAdminParticipantSelected(empId) {
    return adminCalendarParticipantDraft.some(function (participant) {
        return participant.empId === empId;
    });
}

function setAdminParticipantModeTitle(title) {
    const titleEl = document.getElementById('adminCalendarParticipantListTitle');

    if (titleEl) {
        titleEl.textContent = title;
    }
}

function setAdminParticipantListMessage(message) {
    const listEl = document.getElementById('adminCalendarParticipantList');

    if (listEl) {
        listEl.innerHTML = '<div class="admin-calendar-participant-empty">' + escapeHtml(message) + '</div>';
    }

    renderAdminParticipantSelectedList();
}

// 선택된 참석자 이름 영역을 간단 등록과 상세 등록/수정 팝업에 동시에 반영한다.
function renderAdminCalendarFormParticipants() {
    const targets = [
        document.getElementById('adminCalendarSimpleParticipantNames'),
        document.getElementById('adminCalendarDetailFormParticipantNames')
    ].filter(Boolean);

    if (!targets.length) {
        return;
    }

    const text = adminCalendarFormParticipants.length
        ? adminCalendarFormParticipants
                .map(function (participant) {
                    return participant.name || participant.empId;
                })
                .join(' ')
        : '선택된 참석자가 없습니다.';

    targets.forEach(function (target) {
        target.textContent = text;
    });
}

// 간단 등록 종일 체크 상태에 따라 시간 입력을 고정/비활성화한다.
function syncAdminCalendarSimpleAllDayInputs() {
    const allDay = document.getElementById('adminCalendarSimpleAllDay')?.checked;
    const startInput = document.getElementById('adminCalendarSimpleStartTime');
    const endInput = document.getElementById('adminCalendarSimpleEndTime');

    syncAdminCalendarTimeInputs(startInput, endInput, allDay);
}

// 상세 등록/수정 종일 체크 상태에 따라 시간 입력을 고정/비활성화한다.
function syncAdminCalendarDetailAllDayInputs() {
    const allDay = document.getElementById('adminCalendarDetailFormAllDay')?.checked;
    const startInput = document.getElementById('adminCalendarDetailFormStartTime');
    const endInput = document.getElementById('adminCalendarDetailFormEndTime');

    syncAdminCalendarTimeInputs(startInput, endInput, allDay);
}

function syncAdminCalendarTimeInputs(startInput, endInput, allDay) {
    if (!startInput || !endInput) {
        return;
    }

    startInput.disabled = allDay;
    endInput.disabled = allDay;

    if (allDay) {
        startInput.value = '00:00';
        endInput.value = '23:59';
    }
}

// 구분 선택에 따라 공개 범위를 자동 고정한다.
function syncAdminCalendarVisibilityByType() {
    const type = getFormValue('adminCalendarDetailFormType');
    const visibilityEl = document.getElementById('adminCalendarDetailFormVisibility');
    const repeatEl = document.getElementById('adminCalendarDetailFormRepeatRule');
    const participantButton = document.getElementById('adminCalendarDetailParticipantButton');

    if (!visibilityEl) {
        return;
    }

    if (type === 'DEPARTMENT') {
        visibilityEl.value = 'COMPANY';
        visibilityEl.disabled = true;

        // 반복 일정은 사용자 캘린더 기준으로 개인일정에서만 허용한다.
        if (repeatEl) {
            repeatEl.value = '';
            repeatEl.disabled = true;
        }

        // 부서일정은 참석자 초대 대상이 아니므로 참석자 선택 버튼도 막는다.
        if (participantButton) {
            participantButton.disabled = true;
        }

        // 부서일정은 참석자 초대 대상이 아니므로 기존 선택값을 정리한다.
        adminCalendarFormParticipants = [];
        renderAdminCalendarFormParticipants();

        return;
    }

    if (type === 'COMPANY') {
        visibilityEl.value = 'COMPANY';
        visibilityEl.disabled = true;

        // 전사일정도 반복 저장 대상에서 제외한다.
        if (repeatEl) {
            repeatEl.value = '';
            repeatEl.disabled = true;
        }

        // 전사일정도 참석자 초대 대상이 아니므로 참석자 선택 버튼도 막는다.
        if (participantButton) {
            participantButton.disabled = true;
        }

        // 전사일정도 참석자 초대 대상이 아니므로 기존 선택값을 정리한다.
        adminCalendarFormParticipants = [];
        renderAdminCalendarFormParticipants();

        return;
    }

    visibilityEl.disabled = false;

    if (repeatEl) {
        repeatEl.disabled = false;
    }

    if (participantButton) {
        participantButton.disabled = false;
    }
}

// 간단 등록 팝업을 사용자 캘린더처럼 클릭한 날짜 셀 오른쪽에 배치한다.
function positionAdminCalendarPopup(popup, anchorEl, popupWidth, popupHeight) {
    const gap = 18;
    const rect = anchorEl?.getBoundingClientRect();

    let top = rect ? rect.top - 8 : Math.round((window.innerHeight - popupHeight) / 2);
    let left = rect ? rect.right + 14 : Math.round((window.innerWidth - popupWidth) / 2);

    // 오른쪽 공간이 부족하면 날짜 셀 왼쪽으로 띄운다.
    if (rect && left + popupWidth > window.innerWidth - gap) {
        left = rect.left - popupWidth - 14;
    }

    // 아래로 넘치면 화면 안쪽으로 끌어올린다.
    if (top + popupHeight > window.innerHeight - gap) {
        top = window.innerHeight - popupHeight - gap;
    }

    popup.style.top = Math.max(gap, top) + 'px';
    popup.style.left = Math.max(gap, left) + 'px';
}

// 상세 등록/수정 팝업은 사용자 상세등록처럼 화면 상단 기준으로 안정적으로 배치한다.
function positionAdminCalendarDetailFormPopup(popup, anchorEl) {
    const popupWidth = Math.min(520, window.innerWidth - 40);
    const gap = 16;
    const topPadding = window.innerWidth <= 720 ? 12 : 56;
    const bottomPadding = 24;
    const rect = anchorEl?.getBoundingClientRect();

    let left = rect ? rect.right + 14 : Math.round((window.innerWidth - popupWidth) / 2);

    // 상세등록/수정은 내용이 길어서 사용자 캘린더처럼 화면 상단 쪽으로 끌어올린다.
    let top = topPadding;

    if (rect && left + popupWidth > window.innerWidth - gap) {
        left = rect.left - popupWidth - 14;
    }

    if (left < gap) {
        left = gap;
    }

    if (left + popupWidth > window.innerWidth - gap) {
        left = window.innerWidth - popupWidth - gap;
    }

    popup.style.top = top + 'px';
    popup.style.left = left + 'px';
    popup.style.width = popupWidth + 'px';
    popup.style.maxHeight = 'calc(100vh - ' + top + 'px - ' + bottomPadding + 'px)';
}

// 폼 date/time 값을 LocalDateTime 문자열로 만든다.
function buildAdminDateTime(dateInputId, timeInputId, isAllDay, isStart) {
    const date = getFormValue(dateInputId);
    const time = isAllDay
        ? (isStart ? '00:00' : '23:59')
        : getFormValue(timeInputId);

    return date + 'T' + time + ':00';
}

// 간단등록 팝업 입력값을 캘린더 셀의 임시 일정에 반영한다.
function syncAdminCalendarDraftEvent(calendar) {
    const date = getFormValue('adminCalendarSimpleDate');
    const title = getFormValue('adminCalendarSimpleTitle') || '(제목 없음)';
    const isAllDay = Boolean(document.getElementById('adminCalendarSimpleAllDay')?.checked);
    const startTime = getFormValue('adminCalendarSimpleStartTime');
    const endTime = getFormValue('adminCalendarSimpleEndTime');

    if (!calendar || !date) {
        return;
    }

    if (adminCalendarDraftEvent) {
        adminCalendarDraftEvent.remove();
        adminCalendarDraftEvent = null;
    }

    adminCalendarDraftEvent = calendar.addEvent({
        id: 'admin-calendar-draft-event',
        title: title,

        // 종일 미리보기는 등록된 종일 일정과 같은 바 라벨 스타일로 보여준다.
        start: isAllDay ? date : date + 'T' + startTime + ':00',
        end: isAllDay ? toAdminAllDayExclusiveEnd(date) : date + 'T' + endTime + ':00',
        allDay: isAllDay,
        classNames: [
            'admin-calendar-draft-event',
            isAllDay ? 'admin-calendar-event-all-day-personal' : ''
        ].filter(Boolean),
        extendedProps: {
            type: 'PERSONAL',
            category: getFormValue('adminCalendarSimpleCategory'),
            isDraft: true,
            isAllDay: isAllDay
        }
    });
}

// 간단등록을 닫거나 저장할 때 임시 일정을 제거한다.
function clearAdminCalendarDraftEvent() {
    if (adminCalendarDraftEvent) {
        adminCalendarDraftEvent.remove();
        adminCalendarDraftEvent = null;
    }
}

function getAdminVisibilityValue(schedule) {
    if (schedule.type === 'DEPARTMENT') {
        return 'COMPANY';
    }

    if (schedule.type === 'COMPANY') {
        return 'COMPANY';
    }

    return schedule.isPublic ? 'COMPANY' : 'PRIVATE';
}

function toAdminDateValue(value) {
    if (!value) {
        return '';
    }

    return String(value).slice(0, 10);
}

function toAdminTimeValue(value) {
    if (!value) {
        return '';
    }

    return String(value).slice(11, 16);
}

// 현재 시각 기준 다음 정시를 기본 일정 시간으로 잡는다.
// 예: 18:21에 열면 19:00 ~ 20:00
function getAdminDefaultTimeRange() {
    const now = new Date();
    const start = new Date(now);

    start.setMinutes(0, 0, 0);
    start.setHours(start.getHours() + 1);

    const end = new Date(start);
    end.setHours(end.getHours() + 1);

    return {
        startTime: toAdminTimeInputValue(start),
        endTime: toAdminTimeInputValue(end)
    };
}

function toAdminTimeInputValue(date) {
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');

    return hour + ':' + minute;
}

function getFormValue(id) {
    return document.getElementById(id)?.value?.trim() || '';
}

function setFormValue(id, value) {
    const target = document.getElementById(id);

    if (target) {
        target.value = value ?? '';
    }
}

// HTML 문자열을 직접 조립하는 영역에서 사용자/부서 이름이 태그로 해석되지 않도록 이스케이프한다.
// 구성원 목록, 조직도 옵션, 참석자 태그 렌더링에서 공통으로 사용한다.
function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}
