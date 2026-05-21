package com.ict06.team1_fin_pj.domain.approval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict06.team1_fin_pj.domain.approval.entity.ApprovalEntity;
import com.ict06.team1_fin_pj.domain.attendance.entity.AttendanceEntity;
import com.ict06.team1_fin_pj.domain.attendance.entity.AttendanceStatus;
import com.ict06.team1_fin_pj.domain.attendance.entity.LeaveOccurrenceEntity;
import com.ict06.team1_fin_pj.domain.attendance.entity.LeaveRequestEntity;
import com.ict06.team1_fin_pj.domain.attendance.entity.LeaveStatus;
import com.ict06.team1_fin_pj.domain.attendance.entity.LeaveTypeEntity;
import com.ict06.team1_fin_pj.domain.attendance.repository.AttendanceRepository;
import com.ict06.team1_fin_pj.domain.attendance.repository.LeaveOccurrenceRepository;
import com.ict06.team1_fin_pj.domain.attendance.repository.LeaveRequestRepository;
import com.ict06.team1_fin_pj.domain.attendance.repository.LeaveTypeRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [결재-근태 연동용]: 전자결재 승인 완료 문서를 근태 도메인 데이터로 반영하는 전용 서비스입니다.
 *
 * 전자결재 완료 문서를 근태 도메인 데이터로 반영하는 동기화 서비스입니다.
 *
 * 결재가 진행 중이거나 반려/취소된 문서는 근태 데이터에 영향을 주면 안 되므로,
 * ApprovalServiceImpl에서 마지막 승인으로 문서가 COMPLETED 되는 순간에만 이 서비스를 호출합니다.
 */
@Service
@RequiredArgsConstructor
public class ApprovalAttendanceSyncService {

    private static final String FIELD_WORK_RESULT_DATE = "work_result_date";
    private static final String FIELD_ACTUAL_START_TIME = "actual_start_time";
    private static final String FIELD_ACTUAL_END_TIME = "actual_end_time";
    private static final String FIELD_BREAK_MINUTES = "break_minutes";
    private static final String FIELD_WORK_PLAN_TYPE = "work_plan_type";
    private static final String FIELD_WORK_RESULT_REASON = "work_result_reason";

    private static final String FIELD_ABSENCE_TYPE = "absence_type";
    private static final String FIELD_ABSENCE_START_DATE = "absence_start_date";
    private static final String FIELD_ABSENCE_END_DATE = "absence_end_date";
    private static final String FIELD_ABSENCE_START_TIME = "absence_start_time";
    private static final String FIELD_ABSENCE_REASON = "absence_reason";

    private static final String WORK_TYPE_REMOTE = "재택근무";
    private static final String WORK_TYPE_OUTSIDE = "외근";
    private static final String WORK_TYPE_OVERTIME = "연장근무";

    private static final String LEAVE_ANNUAL = "연차";
    private static final String LEAVE_AM_HALF = "오전반차";
    private static final String LEAVE_PM_HALF = "오후반차";
    private static final String LEAVE_EARLY = "조퇴";
    private static final String LEAVE_SICK = "병가";
    private static final String LEAVE_FAMILY_EVENT = "경조사";

    private static final LocalTime STANDARD_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime STANDARD_END_TIME = LocalTime.of(18, 0);
    private static final BigDecimal STANDARD_WORK_HOURS = BigDecimal.valueOf(8);

    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveOccurrenceRepository leaveOccurrenceRepository;

    /*
     * [결재-근태 연동용]: 승인된 결재 문서의 JSON 본문에서 서식 필드 id와 입력값을 꺼내기 위한 mapper입니다.
     *
     * 결재 content는 이미 문자열 JSON으로 저장되어 있으므로, 이 서비스 내부에서 field id/value만 읽으면 됩니다.
     * 프로젝트에 Jackson 2/3 계열 의존성이 함께 보이는 상태라 빈 타입 충돌을 피하기 위해 내부 전용 mapper를 사용합니다.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * [결재-근태 연동용]: 승인 완료된 결재 문서가 근태 연동 대상 서식인지 판단하고 실제 반영 로직으로 분기합니다.
     *
     * 완료된 결재 문서의 content 필드 구성을 보고 근태 연동 대상인지 판단한 뒤 반영합니다.
     *
     * 서식명은 관리자가 수정할 수 있으므로, 실무 데이터 연동은 template field id를 기준으로 식별합니다.
     */
    public void syncIfAttendanceForm(ApprovalEntity approval) {
        Map<String, String> fieldValues = parseFieldValues(approval.getContent());

        if (fieldValues.containsKey(FIELD_WORK_RESULT_DATE)) {
            applyWorkResult(approval, fieldValues);
            return;
        }

        if (fieldValues.containsKey(FIELD_ABSENCE_TYPE)) {
            applyAbsence(approval, fieldValues);
        }
    }

    /**
     * [결재-근태 연동용]: "근무 결과 신청" 승인 내용을 Attendance 테이블의 출퇴근 기록으로 반영합니다.
     */
    private void applyWorkResult(ApprovalEntity approval, Map<String, String> fieldValues) {
        EmpEntity writer = approval.getWriter();
        LocalDate workDate = parseDate(fieldValues.get(FIELD_WORK_RESULT_DATE), FIELD_WORK_RESULT_DATE);
        LocalTime startTime = parseTime(fieldValues.get(FIELD_ACTUAL_START_TIME), FIELD_ACTUAL_START_TIME);
        LocalTime endTime = parseTime(fieldValues.get(FIELD_ACTUAL_END_TIME), FIELD_ACTUAL_END_TIME);
        int breakMinutes = parseInt(fieldValues.get(FIELD_BREAK_MINUTES), 0);
        String workType = normalizeWorkType(fieldValues.get(FIELD_WORK_PLAN_TYPE));
        String reason = fieldValues.getOrDefault(FIELD_WORK_RESULT_REASON, "");

        LocalDateTime requestedCheckIn = LocalDateTime.of(workDate, startTime);
        LocalDateTime requestedCheckOut = LocalDateTime.of(workDate, endTime);
        if (!requestedCheckOut.isAfter(requestedCheckIn)) {
            throw new IllegalArgumentException("근무 종료 시간은 근무 시작 시간보다 이후여야 합니다.");
        }

        AttendanceEntity attendance = attendanceRepository
                .findByEmployee_EmpNoAndWorkDate(writer.getEmpNo(), workDate)
                .orElseGet(() -> AttendanceEntity.builder()
                        .employee(writer)
                        .workDate(workDate)
                        .status(AttendanceStatus.ON_TIME)
                        .build());

        LocalDateTime appliedCheckIn = requestedCheckIn;
        LocalDateTime appliedCheckOut = requestedCheckOut;
        AttendanceStatus status;

        if (WORK_TYPE_OVERTIME.equals(workType)) {
            /*
             * [결재-근태 연동용]: 연장근무는 기존 출근 기록을 유지하면서 퇴근 시간과 연장근무 분만 보정합니다.
             *
             * 연장근무는 지각/조퇴 판정보다 퇴근 시간과 연장근무 분 보정이 핵심입니다.
             * 기존 출근 기록이 있으면 출근 시각은 보존하고, 없으면 신청서의 시작 시각을 사용합니다.
             */
            if (attendance.getCheckInAt() != null) {
                appliedCheckIn = attendance.getCheckInAt();
            }
            status = AttendanceStatus.ON_TIME;
        } else if (WORK_TYPE_OUTSIDE.equals(workType)) {
            BigDecimal workHours = calculateWorkHours(appliedCheckIn, appliedCheckOut, breakMinutes);
            status = workHours.compareTo(STANDARD_WORK_HOURS) >= 0
                    ? AttendanceStatus.ON_TIME
                    : AttendanceStatus.EARLY;
        } else {
            status = calculateDefaultStatus(startTime, endTime);
        }

        BigDecimal workHours = calculateWorkHours(appliedCheckIn, appliedCheckOut, breakMinutes);
        int overtimeMins = calculateOvertimeMins(appliedCheckOut.toLocalTime());
        String note = buildApprovalNote(approval, "근무 결과 신청", "유형=" + workType, reason);

        attendance.applyApprovedWorkResult(appliedCheckIn, appliedCheckOut, workHours, overtimeMins, status, note);
        attendanceRepository.save(attendance);
    }

    /**
     * [결재-근태 연동용]: "부재 일정" 승인 내용을 LeaveRequest로 기록하고 Attendance 요약 상태도 함께 반영합니다.
     */
    private void applyAbsence(ApprovalEntity approval, Map<String, String> fieldValues) {
        if (leaveRequestRepository.existsByApproval_ApprovalId(approval.getApprovalId())) {
            return;
        }

        EmpEntity writer = approval.getWriter();
        String absenceTypeName = normalizeAbsenceType(fieldValues.get(FIELD_ABSENCE_TYPE));
        LocalDate startDate = parseDate(fieldValues.get(FIELD_ABSENCE_START_DATE), FIELD_ABSENCE_START_DATE);
        LocalDate endDate = parseDate(fieldValues.get(FIELD_ABSENCE_END_DATE), FIELD_ABSENCE_END_DATE);
        String reason = fieldValues.getOrDefault(FIELD_ABSENCE_REASON, "");

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("부재 시작일은 종료일보다 이후일 수 없습니다.");
        }

        validateSingleDayAbsence(absenceTypeName, startDate, endDate);

        LeaveTypeEntity leaveType = leaveTypeRepository.findByTypeName(absenceTypeName)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 부재 유형입니다. typeName=" + absenceTypeName));
        BigDecimal leaveDays = calculateLeaveDays(absenceTypeName, startDate, endDate, fieldValues);

        if (Boolean.TRUE.equals(leaveType.getIsAnnualDeduct())) {
            deductLeaveOccurrence(writer.getEmpNo(), startDate.getYear(), leaveDays);
        }

        LeaveRequestEntity leaveRequest = LeaveRequestEntity.builder()
                .employee(writer)
                .leaveType(leaveType)
                .startDate(startDate)
                .endDate(endDate)
                .leaveDays(leaveDays)
                .status(LeaveStatus.APPROVED)
                .approval(approval)
                .reason(reason)
                .approvedAt(LocalDateTime.now())
                .build();
        leaveRequestRepository.save(leaveRequest);

        applyAbsenceAttendance(approval, writer, absenceTypeName, startDate, endDate, reason);
    }

    /**
     * [결재-근태 연동용]: 승인된 부재 기간의 각 날짜에 Attendance 요약 상태와 결재 메모를 남깁니다.
     */
    private void applyAbsenceAttendance(
            ApprovalEntity approval,
            EmpEntity writer,
            String absenceTypeName,
            LocalDate startDate,
            LocalDate endDate,
            String reason
    ) {
        AttendanceStatus status = resolveAbsenceAttendanceStatus(absenceTypeName);
        String note = buildApprovalNote(approval, "부재 일정", "유형=" + absenceTypeName, reason);

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            LocalDate workDate = cursor;
            AttendanceEntity attendance = attendanceRepository
                    .findByEmployee_EmpNoAndWorkDate(writer.getEmpNo(), workDate)
                    .orElseGet(() -> AttendanceEntity.builder()
                            .employee(writer)
                            .workDate(workDate)
                            .status(status)
                            .build());

            attendance.applyApprovedAbsence(status, note);
            attendanceRepository.save(attendance);
            cursor = cursor.plusDays(1);
        }
    }

    /**
     * [결재-근태 연동용]: 근태 담당자와 합의한 부재 유형별 AttendanceStatus 매핑을 적용합니다.
     *
     * 연차/병가/경조사는 하루 단위 휴가 성격이므로 LEAVE,
     * 오전반차/오후반차는 HALF_LEAVE, 조퇴는 기존 EARLY 상태를 사용합니다.
     */
    private AttendanceStatus resolveAbsenceAttendanceStatus(String absenceTypeName) {
        if (LEAVE_EARLY.equals(absenceTypeName)) {
            return AttendanceStatus.EARLY;
        }

        if (LEAVE_AM_HALF.equals(absenceTypeName) || LEAVE_PM_HALF.equals(absenceTypeName)) {
            return AttendanceStatus.HALF_LEAVE;
        }

        return AttendanceStatus.LEAVE;
    }

    /**
     * [결재-근태 연동용]: 결재 본문 JSON의 fields 배열을 id-value Map으로 변환합니다.
     */
    private Map<String, String> parseFieldValues(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode fields = root.path("fields");
            Map<String, String> values = new HashMap<>();

            if (!fields.isArray()) {
                return values;
            }

            for (JsonNode field : fields) {
                String id = field.path("id").asText("");
                if (!id.isBlank()) {
                    values.put(id, field.path("value").asText(""));
                }
            }

            return values;
        } catch (Exception e) {
            throw new IllegalArgumentException("결재 문서 본문 JSON을 해석할 수 없습니다.", e);
        }
    }

    /**
     * [결재-근태 연동용]: 재택근무 등 일반 근무 결과 신청에서 9시/18시 기준으로 근태 상태를 계산합니다.
     */
    private AttendanceStatus calculateDefaultStatus(LocalTime startTime, LocalTime endTime) {
        if (endTime.isBefore(STANDARD_END_TIME)) {
            return AttendanceStatus.EARLY;
        }

        if (startTime.isAfter(STANDARD_START_TIME)) {
            return AttendanceStatus.LATE;
        }

        return AttendanceStatus.ON_TIME;
    }

    /**
     * [결재-근태 연동용]: 승인된 근무 결과 신청의 시작/종료/휴게 시간을 이용해 실제 근무 시간을 계산합니다.
     */
    private BigDecimal calculateWorkHours(LocalDateTime checkInAt, LocalDateTime checkOutAt, int breakMinutes) {
        long workMinutes = Duration.between(checkInAt, checkOutAt).toMinutes() - breakMinutes;
        if (workMinutes <= 0) {
            throw new IllegalArgumentException("휴게 시간을 제외한 근무 시간이 0분 이하입니다.");
        }

        return BigDecimal.valueOf(workMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /**
     * [결재-근태 연동용]: 18시 이후 근무 시간을 연장근무 분 단위로 계산합니다.
     */
    private int calculateOvertimeMins(LocalTime endTime) {
        if (!endTime.isAfter(STANDARD_END_TIME)) {
            return 0;
        }

        return (int) Duration.between(STANDARD_END_TIME, endTime).toMinutes();
    }

    /**
     * [결재-근태 연동용]: 부재 유형별 휴가 사용 일수를 계산합니다.
     */
    private BigDecimal calculateLeaveDays(
            String absenceTypeName,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, String> fieldValues
    ) {
        if (LEAVE_AM_HALF.equals(absenceTypeName) || LEAVE_PM_HALF.equals(absenceTypeName)) {
            return BigDecimal.valueOf(0.5);
        }

        if (LEAVE_EARLY.equals(absenceTypeName)) {
            LocalTime absenceStartTime = parseTime(fieldValues.get(FIELD_ABSENCE_START_TIME), FIELD_ABSENCE_START_TIME);
            if (!absenceStartTime.isBefore(STANDARD_END_TIME)) {
                throw new IllegalArgumentException("조퇴 시작 시간은 18시 이전이어야 합니다.");
            }

            long leaveMinutes = Duration.between(absenceStartTime, STANDARD_END_TIME).toMinutes();
            return BigDecimal.valueOf(leaveMinutes)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                    .divide(STANDARD_WORK_HOURS, 1, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(ChronoUnit.DAYS.between(startDate, endDate) + 1);
    }

    /**
     * [결재-근태 연동용]: 연차 차감 대상 부재라면 만료일이 빠른 발생분부터 잔여 휴가를 차감합니다.
     */
    private void deductLeaveOccurrence(String empNo, Integer targetYear, BigDecimal leaveDays) {
        BigDecimal remainingDaysToDeduct = leaveDays;
        List<LeaveOccurrenceEntity> occurrences = leaveOccurrenceRepository
                .findByEmployee_EmpNoAndTargetYear(empNo, targetYear)
                .stream()
                .sorted(Comparator.comparing(LeaveOccurrenceEntity::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        for (LeaveOccurrenceEntity occurrence : occurrences) {
            if (remainingDaysToDeduct.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal remainDays = occurrence.getRemain_days() == null
                    ? BigDecimal.ZERO
                    : occurrence.getRemain_days();
            if (remainDays.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal deductDays = remainDays.min(remainingDaysToDeduct);
            occurrence.useDays(deductDays);
            remainingDaysToDeduct = remainingDaysToDeduct.subtract(deductDays);
        }

        if (remainingDaysToDeduct.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("잔여 휴가 일수가 부족합니다.");
        }
    }

    /**
     * [결재-근태 연동용]: 반차/조퇴처럼 하루 안에서만 처리해야 하는 부재 유형의 날짜 범위를 검증합니다.
     */
    private void validateSingleDayAbsence(String absenceTypeName, LocalDate startDate, LocalDate endDate) {
        if ((LEAVE_AM_HALF.equals(absenceTypeName)
                || LEAVE_PM_HALF.equals(absenceTypeName)
                || LEAVE_EARLY.equals(absenceTypeName))
                && !startDate.equals(endDate)) {
            throw new IllegalArgumentException(absenceTypeName + "는 시작일과 종료일이 같아야 합니다.");
        }
    }

    private LocalDate parseDate(String value, String fieldId) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldId + " 값이 필요합니다.");
        }

        return LocalDate.parse(value);
    }

    private LocalTime parseTime(String value, String fieldId) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldId + " 값이 필요합니다.");
        }

        return LocalTime.parse(value);
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

    private String normalizeWorkType(String value) {
        if (value == null || value.isBlank()) {
            return WORK_TYPE_REMOTE;
        }

        return value.trim();
    }

    private String normalizeAbsenceType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("부재 유형 값이 필요합니다.");
        }

        return switch (value.trim()) {
            case "0" -> LEAVE_ANNUAL;
            case "1" -> LEAVE_AM_HALF;
            case "2" -> LEAVE_PM_HALF;
            case "3" -> LEAVE_EARLY;
            case "4" -> LEAVE_SICK;
            case "5" -> LEAVE_FAMILY_EVENT;
            default -> value.trim();
        };
    }

    private String buildApprovalNote(ApprovalEntity approval, String formLabel, String typeText, String reason) {
        StringBuilder note = new StringBuilder();
        note.append("전자결재 반영: ")
                .append(formLabel)
                .append(" / ")
                .append(typeText)
                .append(" / approvalId=")
                .append(approval.getApprovalId());

        if (reason != null && !reason.isBlank()) {
            note.append(" / 사유=").append(reason.trim());
        }

        return note.toString();
    }
}
