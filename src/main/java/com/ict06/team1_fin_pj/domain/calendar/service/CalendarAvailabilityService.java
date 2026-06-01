package com.ict06.team1_fin_pj.domain.calendar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict06.team1_fin_pj.common.dto.calendar.CalendarUnavailableEmployeeDto;
import com.ict06.team1_fin_pj.domain.attendance.entity.LeaveRequestEntity;
import com.ict06.team1_fin_pj.domain.attendance.entity.LeaveStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class CalendarAvailabilityService {

    private static final String LEAVE_ANNUAL = "연차";
    private static final String LEAVE_AM_HALF = "오전반차";
    private static final String LEAVE_PM_HALF = "오후반차";
    private static final String LEAVE_EARLY = "조퇴";
    private static final String LEAVE_SICK = "병가";
    private static final String LEAVE_FAMILY_EVENT = "경조사";

    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime AM_HALF_END_TIME = LocalTime.of(13, 0);
    private static final LocalTime PM_HALF_START_TIME = LocalTime.of(14, 0);
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);

    private static final String FIELD_ABSENCE_START_TIME = "absence_start_time";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    // 일정 기간과 실제 시간이 겹치는 승인 부재자를 조회한다.
    public List<CalendarUnavailableEmployeeDto> findUnavailableEmployees(
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<String> empNos
    ) {
        LocalDate startDate = toDate(startTime);
        LocalDate endDate = toDate(endTime);

        if (startDate == null || endDate == null) {
            return List.of();
        }

        if (endDate.isBefore(startDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        List<String> normalizedEmpNos = normalizeEmpNos(empNos);
        boolean hasEmpFilter = !normalizedEmpNos.isEmpty();

        String jpql = """
                SELECT lr
                  FROM LeaveRequestEntity lr
                  JOIN FETCH lr.employee e
                  LEFT JOIN FETCH lr.leaveType lt
                  LEFT JOIN FETCH lr.approval a
                 WHERE lr.status = :status
                   AND lr.startDate <= :endDate
                   AND lr.endDate >= :startDate
                """;

        if (hasEmpFilter) {
            jpql += " AND e.empNo IN :empNos";
        }

        var query = entityManager.createQuery(jpql, LeaveRequestEntity.class)
                .setParameter("status", LeaveStatus.APPROVED)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate);

        if (hasEmpFilter) {
            query.setParameter("empNos", normalizedEmpNos);
        }

        return query.getResultList().stream()
                .map(this::toUnavailableDto)
                // 반차/조퇴는 실제 시간까지 겹칠 때만 제한한다.
                .filter(dto -> isTimeOverlapped(startTime, endTime, dto))
                .toList();
    }

    // 휴가/반차/조퇴/병가/경조사 기간에는 본인 일정 등록/수정이 불가하다.
    public void validateCreatorAvailable(String creatorNo, LocalDateTime startTime, LocalDateTime endTime) {
        if (creatorNo == null || creatorNo.trim().isEmpty()) {
            return;
        }

        List<CalendarUnavailableEmployeeDto> unavailable =
                findUnavailableEmployees(startTime, endTime, List.of(creatorNo.trim()));

        if (!unavailable.isEmpty()) {
            CalendarUnavailableEmployeeDto first = unavailable.get(0);
            throw new IllegalArgumentException(first.getReasonName() + " 시간에는 일정을 등록할 수 없습니다.");
        }
    }

    // 휴가/반차/조퇴/병가/경조사 시간에는 참석자로 선택할 수 없다.
    public void validateParticipantsAvailable(List<String> participantNos, LocalDateTime startTime, LocalDateTime endTime) {
        if (participantNos == null || participantNos.isEmpty()) {
            return;
        }

        List<String> normalizedNos = normalizeEmpNos(participantNos);

        if (normalizedNos.isEmpty()) {
            return;
        }

        List<CalendarUnavailableEmployeeDto> unavailable =
                findUnavailableEmployees(startTime, endTime, normalizedNos);

        if (!unavailable.isEmpty()) {
            String names = unavailable.stream()
                    .map(CalendarUnavailableEmployeeDto::getName)
                    .distinct()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("선택한 참석자");

            throw new IllegalArgumentException(names + "님은 해당 시간에 부재 상태라\n참석자로 선택할 수 없습니다.");
        }
    }

    private CalendarUnavailableEmployeeDto toUnavailableDto(LeaveRequestEntity leaveRequest) {
        String typeName = leaveRequest.getLeaveType() != null
                ? leaveRequest.getLeaveType().getTypeName()
                : LEAVE_ANNUAL;

        LocalDateTime unavailableStartTime = resolveUnavailableStartTime(leaveRequest, typeName);
        LocalDateTime unavailableEndTime = resolveUnavailableEndTime(leaveRequest, typeName);

        return CalendarUnavailableEmployeeDto.builder()
                .empNo(leaveRequest.getEmployee().getEmpNo())
                .name(leaveRequest.getEmployee().getName())
                .reasonType(resolveReasonType(typeName))
                .reasonName(typeName)
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .unavailableStartTime(unavailableStartTime)
                .unavailableEndTime(unavailableEndTime)
                .isAllDay(isAllDayAbsence(typeName))
                .build();
    }

    private LocalDateTime resolveUnavailableStartTime(LeaveRequestEntity leaveRequest, String typeName) {
        LocalDate startDate = leaveRequest.getStartDate();

        if (LEAVE_AM_HALF.equals(typeName)) {
            return startDate.atTime(WORK_START_TIME);
        }

        if (LEAVE_PM_HALF.equals(typeName)) {
            return startDate.atTime(PM_HALF_START_TIME);
        }

        if (LEAVE_EARLY.equals(typeName)) {
            return startDate.atTime(resolveEarlyLeaveStartTime(leaveRequest));
        }

        return startDate.atStartOfDay();
    }

    private LocalDateTime resolveUnavailableEndTime(LeaveRequestEntity leaveRequest, String typeName) {
        LocalDate endDate = leaveRequest.getEndDate();

        if (LEAVE_AM_HALF.equals(typeName)) {
            return endDate.atTime(AM_HALF_END_TIME);
        }

        if (LEAVE_PM_HALF.equals(typeName) || LEAVE_EARLY.equals(typeName)) {
            return endDate.atTime(WORK_END_TIME);
        }

        return endDate.plusDays(1).atStartOfDay();
    }

    private LocalTime resolveEarlyLeaveStartTime(LeaveRequestEntity leaveRequest) {
        String content = leaveRequest.getApproval() != null
                ? leaveRequest.getApproval().getContent()
                : null;

        String startTimeText = findApprovalFieldValue(content, FIELD_ABSENCE_START_TIME);

        if (startTimeText == null || startTimeText.isBlank()) {
            // 조퇴 시작 시간이 없으면 오후 전체를 제한한다.
            return PM_HALF_START_TIME;
        }

        return LocalTime.parse(startTimeText);
    }

    private String findApprovalFieldValue(String content, String fieldId) {
        if (content == null || content.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode fields = root.path("fields");

            if (!fields.isArray()) {
                return null;
            }

            for (JsonNode field : fields) {
                if (fieldId.equals(field.path("id").asText())) {
                    return field.path("value").asText(null);
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private boolean isTimeOverlapped(
            LocalDateTime scheduleStartTime,
            LocalDateTime scheduleEndTime,
            CalendarUnavailableEmployeeDto unavailable
    ) {
        if (scheduleStartTime == null || scheduleEndTime == null) {
            return false;
        }

        LocalDateTime unavailableStartTime = unavailable.getUnavailableStartTime();
        LocalDateTime unavailableEndTime = unavailable.getUnavailableEndTime();

        return scheduleStartTime.isBefore(unavailableEndTime)
                && scheduleEndTime.isAfter(unavailableStartTime);
    }

    private String resolveReasonType(String typeName) {
        if (LEAVE_AM_HALF.equals(typeName) || LEAVE_PM_HALF.equals(typeName)) {
            return "HALF_LEAVE";
        }

        if (LEAVE_EARLY.equals(typeName)) {
            return "EARLY";
        }

        if (typeName != null && (LEAVE_SICK.equals(typeName) || typeName.contains("병"))) {
            return "SICK";
        }

        if (LEAVE_FAMILY_EVENT.equals(typeName)) {
            return "FAMILY_EVENT";
        }

        return "LEAVE";
    }

    private boolean isAllDayAbsence(String typeName) {
        return LEAVE_ANNUAL.equals(typeName)
                || LEAVE_SICK.equals(typeName)
                || LEAVE_FAMILY_EVENT.equals(typeName)
                || (!LEAVE_AM_HALF.equals(typeName)
                && !LEAVE_PM_HALF.equals(typeName)
                && !LEAVE_EARLY.equals(typeName));
    }

    private List<String> normalizeEmpNos(List<String> empNos) {
        if (empNos == null) {
            return List.of();
        }

        return empNos.stream()
                .filter(empNo -> empNo != null && !empNo.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private LocalDate toDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }
}