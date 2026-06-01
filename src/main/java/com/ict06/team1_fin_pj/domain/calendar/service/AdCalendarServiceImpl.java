package com.ict06.team1_fin_pj.domain.calendar.service;

import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleListResponseDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleUpdateRequestDto;
import com.ict06.team1_fin_pj.domain.calendar.entity.ScheduleEntity;
import com.ict06.team1_fin_pj.domain.calendar.entity.ScheduleParticipantEntity;
import com.ict06.team1_fin_pj.domain.calendar.entity.ScheduleType;
import com.ict06.team1_fin_pj.domain.calendar.repository.CalendarRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.DepartmentEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AdCalendarServiceImpl implements AdCalendarService {

    private final CalendarRepository repository;
    private final CalendarAvailabilityService calendarAvailabilityService;
    private final CalendarHolidayService calendarHolidayService;

    @PersistenceContext
    private EntityManager entityManager;

    public AdCalendarServiceImpl(
            CalendarRepository repository,
            CalendarAvailabilityService calendarAvailabilityService,
            CalendarHolidayService calendarHolidayService
    ) {
        this.repository = repository;
        this.calendarAvailabilityService = calendarAvailabilityService;
        this.calendarHolidayService = calendarHolidayService;
    }

    // 관리자 캘린더 목록 조회
    // 개인 비공개 일정은 제외하고, 공개 개인일정/부서일정/전사일정만 관리 대상으로 가져온다.
    @Override
    @Transactional(readOnly = true)
    public List<ScheduleListResponseDto> getAdminScheduleList(List<String> selectedMemberNos, String adminEmpNo) {
        Map<Integer, ScheduleEntity> scheduleMap = new LinkedHashMap<>();

        // 기본 관리자 목록: 개인 비공개를 제외한 공개 개인일정, 부서일정, 전사일정.
        repository.findAdminManageableSchedules(ScheduleType.PERSONAL, adminEmpNo)
                .forEach(schedule -> scheduleMap.put(schedule.getScheduleId(), schedule));

        // 일정 범위에서 조직도 구성원을 선택하면, 부서와 상관없이 해당 구성원의 공개 개인일정을 추가한다.
        // 프론트에서 선택 구성원 사번이 "20201111,20202222"처럼 한 문자열로 오거나
        // selectedMemberNos=20201111&selectedMemberNos=20202222처럼 반복 파라미터로 와도 같은 방식으로 처리한다.
        List<String> normalizedSelectedMemberNos = selectedMemberNos == null
                ? List.of()
                : selectedMemberNos.stream()
                        .filter(Objects::nonNull)
                        .flatMap(value -> Arrays.stream(value.split(",")))
                        .map(String::trim)
                        .filter(empNo -> !empNo.isEmpty())
                        .distinct()
                        .toList();

        if (!normalizedSelectedMemberNos.isEmpty()) {
            repository.findPublicPersonalSchedulesByCreators(
                            normalizedSelectedMemberNos,
                            ScheduleType.PERSONAL
                    )
                    .forEach(schedule -> scheduleMap.put(schedule.getScheduleId(), schedule));
        }

        return scheduleMap.values()
                .stream()
                .map(this::toScheduleListResponseDto)
                .toList();
    }

    // 관리자 일정 등록
    // 사용자 일정 등록 로직과 같은 DTO를 사용하되, 관리자 영역에서는 별도 소유자 검증 없이 생성한다.
    @Override
    @Transactional
    public Integer createAdminSchedule(ScheduleCreateRequestDto dto, String adminEmpNo) {
        validateScheduleRequiredFields(dto.getTitle(), dto.getStartTime(), dto.getEndTime());

        // 관리자 화면에서는 작성자 사번 입력칸을 노출하지 않는다.
        // 요청에 작성자 사번이 없으면 로그인한 관리자 사번을 기본 작성자로 사용한다.
        String creatorNo = dto.getCreatorNo();

        if (creatorNo == null || creatorNo.trim().isEmpty()) {
            creatorNo = adminEmpNo;
        }

        if (creatorNo == null || creatorNo.trim().isEmpty()) {
            throw new IllegalArgumentException("작성자 사번은 필수입니다.");
        }

        // 공휴일에는 관리자도 일정을 등록할 수 없다.
        calendarHolidayService.validateNotHoliday(
                dto.getStartTime(),
                dto.getEndTime()
        );
        // 휴가/병결 기간에는 해당 작성자의 일정 등록과 참석자 초대를 막는다.
        calendarAvailabilityService.validateCreatorAvailable(
                creatorNo.trim(),
                dto.getStartTime(),
                dto.getEndTime()
        );
        calendarAvailabilityService.validateParticipantsAvailable(
                dto.getParticipantNos(),
                dto.getStartTime(),
                dto.getEndTime()
        );

        EmpEntity creator = entityManager.getReference(EmpEntity.class, creatorNo.trim());
        ScheduleType scheduleType = resolveScheduleType(dto.getType());
        DepartmentEntity department = resolveDepartment(dto.getDeptId(), scheduleType, creator);

        ScheduleEntity entity = ScheduleEntity.builder()
                .title(dto.getTitle().trim())
                .content(dto.getContent())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .type(scheduleType)
                .creator(creator)
                .department(department)
                .category(dto.getCategory())
                .location(dto.getLocation())
                .isAllDay(Boolean.TRUE.equals(dto.getIsAllDay()))
                .isPublic(dto.getIsPublic() == null ? true : dto.getIsPublic())
                .repeatRule(dto.getRepeatRule())
                .isDeleted(false)
                .build();

        syncParticipants(entity, dto.getParticipantNos());

        return repository.save(entity).getScheduleId();
    }

    // 관리자 일정 수정
    // 개인 비공개 일정은 관리자 화면에서 관리 대상이 아니므로 수정도 차단한다.
    @Override
    @Transactional
    public Integer updateAdminSchedule(Integer scheduleId, ScheduleUpdateRequestDto dto, String adminEmpNo) {
        ScheduleEntity schedule = repository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        validateAdminManageableSchedule(schedule, adminEmpNo);
        validateScheduleRequiredFields(dto.getTitle(), dto.getStartTime(), dto.getEndTime());

        String creatorNo = schedule.getCreator() != null
                ? schedule.getCreator().getEmpNo()
                : adminEmpNo;

        // 휴가/병결 기간에는 해당 작성자의 일정 수정과 참석자 초대를 막는다.
        calendarHolidayService.validateNotHoliday(
                dto.getStartTime(),
                dto.getEndTime()
        );
        // 공휴일에는 관리자도 일정을 수정할 수 없다.
        calendarAvailabilityService.validateCreatorAvailable(
                creatorNo,
                dto.getStartTime(),
                dto.getEndTime()
        );
        calendarAvailabilityService.validateParticipantsAvailable(
                dto.getParticipantNos(),
                dto.getStartTime(),
                dto.getEndTime()
        );

        ScheduleType scheduleType = resolveScheduleType(dto.getType());
        DepartmentEntity department = resolveDepartment(dto.getDeptId(), scheduleType, schedule.getCreator());

        schedule.updateSchedule(
                dto.getTitle(),
                dto.getContent(),
                dto.getStartTime(),
                dto.getEndTime(),
                scheduleType,
                department,
                dto.getCategory(),
                dto.getLocation(),
                dto.getIsAllDay(),
                dto.getIsPublic(),
                dto.getRepeatRule()
        );

        syncParticipants(schedule, dto.getParticipantNos());

        return schedule.getScheduleId();
    }

    // 관리자 일정 삭제
    // soft delete 전에 참석자 연결을 비워서 sch_participant 데이터가 남지 않도록 정리한다.
    @Override
    @Transactional
    public void deleteAdminSchedule(Integer scheduleId, String adminEmpNo) {
        ScheduleEntity schedule = repository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        validateAdminManageableSchedule(schedule, adminEmpNo);

        schedule.getParticipants().clear();
        schedule.deleteSchedule();
    }

    // 관리자 관리 가능 여부 검증
    // 삭제된 일정과 개인 비공개 일정은 관리자 수정/삭제 대상에서 제외한다.
    // 관리자는 공개 일정은 관리할 수 있고, 개인 비공개 일정은 본인이 작성한 경우만 관리할 수 있다.
    private void validateAdminManageableSchedule(ScheduleEntity schedule, String adminEmpNo) {
        if (Boolean.TRUE.equals(schedule.getIsDeleted())) {
            throw new IllegalArgumentException("이미 삭제된 일정입니다.");
        }

        boolean privatePersonalSchedule = schedule.getType() == ScheduleType.PERSONAL
                && !Boolean.TRUE.equals(schedule.getIsPublic());

        if (!privatePersonalSchedule) {
            return;
        }

        String creatorNo = schedule.getCreator() != null ? schedule.getCreator().getEmpNo() : null;

        if (adminEmpNo == null || !adminEmpNo.equals(creatorNo)) {
            throw new IllegalArgumentException("다른 사용자의 개인 비공개 일정은 관리할 수 없습니다.");
        }
    }

    // 등록/수정 공통 필수값 검증
    // 화면 검증을 우회해도 서버에서 제목/시간 필수 조건을 한 번 더 막는다.
    private void validateScheduleRequiredFields(String title, LocalDateTime startTime, LocalDateTime endTime) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("일정 제목은 필수입니다.");
        }

        if (startTime == null) {
            throw new IllegalArgumentException("시작 시간은 필수입니다.");
        }

        if (endTime == null) {
            throw new IllegalArgumentException("종료 시간은 필수입니다.");
        }

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 빠를 수 없습니다.");
        }
    }

    // 문자열 일정 구분 값을 enum으로 변환한다.
    // 값이 없으면 사용자 캘린더와 동일하게 개인일정을 기본값으로 둔다.
    private ScheduleType resolveScheduleType(String type) {
        String typeValue = (type == null || type.trim().isEmpty())
                ? "PERSONAL"
                : type.trim();

        try {
            return ScheduleType.valueOf(typeValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 일정 유형입니다. " + typeValue);
        }
    }

    // 부서일정일 경우 부서 정보를 결정한다.
    // 요청에 deptId가 있으면 우선 사용하고, 없으면 작성자의 소속 부서를 사용한다.
    private DepartmentEntity resolveDepartment(Integer deptId, ScheduleType scheduleType, EmpEntity creator) {
        if (deptId != null) {
            return entityManager.getReference(DepartmentEntity.class, deptId);
        }

        if (scheduleType == ScheduleType.DEPARTMENT
                && creator != null
                && creator.getDepartment() != null) {
            return creator.getDepartment();
        }

        return null;
    }

    // 참석자 목록을 일정 엔티티의 SCH_PARTICIPANT 연결과 동기화한다.
    // null이면 기존 참석자를 유지하고, 빈 배열이면 참석자를 모두 제거한다.
    private void syncParticipants(ScheduleEntity schedule, List<String> participantNos) {
        if (participantNos == null) {
            return;
        }

        schedule.getParticipants().clear();

        String creatorNo = schedule.getCreator() != null
                ? schedule.getCreator().getEmpNo()
                : null;

        participantNos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(empNo -> !empNo.isEmpty())
                .distinct()
                .filter(empNo -> creatorNo == null || !creatorNo.equals(empNo))
                .forEach(empNo -> {
                    EmpEntity employee = entityManager.getReference(EmpEntity.class, empNo);

                    schedule.getParticipants().add(
                            ScheduleParticipantEntity.builder()
                                    .schedule(schedule)
                                    .employee(employee)
                                    .build()
                    );
                });
    }

    // 일정 엔티티를 관리자 캘린더 화면 DTO로 변환한다.
    // 화면 표시, 상세 팝업, 참석자 확인에 필요한 값을 한 번에 내려준다.
    private ScheduleListResponseDto toScheduleListResponseDto(ScheduleEntity schedule) {
        return ScheduleListResponseDto.builder()
                .scheduleId(schedule.getScheduleId())
                .title(schedule.getTitle())
                .content(schedule.getContent())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .type(schedule.getType() != null ? schedule.getType().name() : null)
                .category(schedule.getCategory())
                .location(schedule.getLocation())
                .isAllDay(schedule.getIsAllDay())
                .isPublic(schedule.getIsPublic())
                .repeatRule(schedule.getRepeatRule())
                .creatorNo(schedule.getCreator() != null ? schedule.getCreator().getEmpNo() : null)
                .creatorName(schedule.getCreator() != null ? schedule.getCreator().getName() : null)
                .participants(toParticipantDtos(schedule))
                .build();
    }

    // 참석자 엔티티 목록을 화면 DTO로 변환한다.
    // 관리자 상세 팝업에서 이름, 부서, 직급, 참석 상태를 표시할 수 있게 한다.
    private List<ScheduleListResponseDto.ParticipantDto> toParticipantDtos(ScheduleEntity schedule) {
        return schedule.getParticipants()
                .stream()
                .map(participant -> {
                    EmpEntity employee = participant.getEmployee();
                    DepartmentEntity department = employee.getDepartment();

                    return ScheduleListResponseDto.ParticipantDto.builder()
                            .empId(employee.getEmpNo())
                            .name(employee.getName())
                            .deptId(department != null ? department.getDeptId() : null)
                            .deptName(department != null ? department.getDeptName() : null)
                            .positionName(employee.getPosition() != null ? employee.getPosition().getPositionName() : null)
                            .status(participant.getStatus() != null ? participant.getStatus().name() : null)
                            .respondedAt(participant.getRespondedAt())
                            .build();
                })
                .toList();
    }
}
