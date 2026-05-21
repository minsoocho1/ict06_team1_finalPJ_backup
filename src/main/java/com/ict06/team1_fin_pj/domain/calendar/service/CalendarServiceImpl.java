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
import com.ict06.team1_fin_pj.domain.calendar.entity.ParticipantStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 일정 Service 구현체
 */
@Service
public class CalendarServiceImpl implements CalendarService {

    @Autowired
    private CalendarRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    // 일정 등록
    @Override
    @Transactional
    public Integer createSchedule(ScheduleCreateRequestDto dto) {
        System.out.println("[CalendarServiceImpl] - createSchedule()");

        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("일정 제목은 필수입니다.");
        }

        if (dto.getCreatorNo() == null || dto.getCreatorNo().trim().isEmpty()) {
            throw new IllegalArgumentException("작성자 사번은 필수입니다.");
        }

        if (dto.getStartTime() == null) {
            throw new IllegalArgumentException("시작 시간은 필수입니다.");
        }

        if (dto.getEndTime() == null) {
            throw new IllegalArgumentException("종료 시간은 필수입니다.");
        }

        // 작성자 사번으로 Employee 참조
        EmpEntity creator = entityManager.getReference(EmpEntity.class, dto.getCreatorNo());

        // 일정 유형이 없으면 PERSONAL 기본값 사용
        String typeValue = (dto.getType() == null || dto.getType().trim().isEmpty())
                ? "PERSONAL"
                : dto.getType().trim();

        // 부서 ID가 있는 경우에는 해당 부서를 사용한다.
        // 부서일정인데 부서 ID가 없으면 작성자의 소속 부서를 자동으로 사용한다.
        DepartmentEntity department = null;
        if (dto.getDeptId() != null) {
            department = entityManager.getReference(DepartmentEntity.class, dto.getDeptId());
        } else if ("DEPARTMENT".equals(typeValue)
                && creator.getDepartment() != null) {
            department = creator.getDepartment();
        }

        ScheduleType scheduleType;
        try {
            scheduleType = ScheduleType.valueOf(typeValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 일정 유형입니다: " + typeValue);
        }

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

        ScheduleEntity saved = repository.save(entity);

        return saved.getScheduleId();
    }

    // 일정 목록 조회
    // 1. 로그인 사용자가 기본적으로 볼 수 있는 일정을 조회한다.
    // 2. 조직도에서 선택한 구성원이 있으면 해당 구성원의 공개 개인일정만 추가한다.
    // 3. 같은 일정이 중복으로 들어올 수 있으므로 scheduleId 기준으로 합쳐서 반환한다.
    @Override
    @Transactional(readOnly = true)
    public List<ScheduleListResponseDto> getScheduleList(String empNo, List<String> selectedMemberNos) {
        EmpEntity loginUser = entityManager.find(EmpEntity.class, empNo);

        if (loginUser == null) {
            throw new IllegalArgumentException("로그인 사용자 정보를 찾을 수 없습니다.");
        }

        Integer deptId = loginUser.getDepartment() != null
                ? loginUser.getDepartment().getDeptId()
                : null;

        // 기본 조회 범위:
        // 내 일정, 같은 부서 공개 개인일정, 내 부서일정, 전사일정
        List<ScheduleEntity> visibleSchedules = repository.findVisibleSchedules(
                empNo,
                deptId,
                ScheduleType.PERSONAL,
                ScheduleType.DEPARTMENT,
                ScheduleType.COMPANY
        );

        // 기본 조회 일정과 선택 구성원 일정을 하나의 목록으로 합친다.
        // LinkedHashMap을 사용해 조회 순서를 유지하면서 scheduleId 중복을 제거한다.
        Map<Integer, ScheduleEntity> scheduleMap = new LinkedHashMap<>();

        visibleSchedules.forEach(schedule ->
                scheduleMap.put(schedule.getScheduleId(), schedule)
        );

        // 프론트에서 넘어온 선택 구성원 사번 목록을 정리한다.
        // null, 공백, 본인 사번, 중복 사번은 추가 조회 대상에서 제외한다.
        List<String> normalizedSelectedMemberNos = selectedMemberNos == null
                ? List.of()
                : selectedMemberNos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(selectedEmpNo -> !selectedEmpNo.isEmpty())
                .filter(selectedEmpNo -> !selectedEmpNo.equals(empNo))
                .distinct()
                .toList();

        // 조직도에서 선택한 구성원은 부서와 상관없이 공개 개인일정만 추가 조회한다.
        // 다른 부서의 부서일정은 권한 범위가 달라질 수 있으므로 포함하지 않는다.
        if (!normalizedSelectedMemberNos.isEmpty()) {
            repository.findPublicPersonalSchedulesByCreators(
                            normalizedSelectedMemberNos,
                            ScheduleType.PERSONAL
                    )
                    .forEach(schedule ->
                            scheduleMap.put(schedule.getScheduleId(), schedule)
                    );
        }

        // 최종 일정 목록을 화면 응답 DTO로 변환한다.
        // 참석자 정보도 수정 팝업 복원을 위해 함께 내려준다.
        return scheduleMap.values()
                .stream()
                .map(schedule -> ScheduleListResponseDto.builder()
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
                        .build())
                .toList();
    }

    // 일정 참석자 엔티티 목록을 프론트에서 사용하는 참석자 DTO 목록으로 변환한다.
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
                            // 프론트에서 참석/미정/불참 상태를 표시할 수 있도록 enum 이름을 그대로 내려준다.
                            .status(participant.getStatus() != null ? participant.getStatus().name() : null)
                            .respondedAt(participant.getRespondedAt())
                            .build();
                })
                .toList();
    }

    // 참석자 목록을 일정 엔티티 SCH_PARTICIPANT 목록과 동기화한다.
    // 생성 시에는 새 참석자를 추가하고, 수정 시에는 기존 참석자를 비운 뒤 새 선택값으로 다시 채운다.
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
                // 작성자 본인은 참석자로 중복 저장 x
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

    // 일정 수정/삭제는 공개 일정이라도 작성자 본인만 가능하게 제한됨.
    // 프론트에서 버튼을 숨기더라도 API를 직접 호출할 수 있으므로 서비스에서 한 번 더 검증한다.
    private void validateScheduleOwner(ScheduleEntity schedule, String requesterNo) {
        if (requesterNo == null || requesterNo.trim().isEmpty()) {
            throw new IllegalArgumentException("요청자 정보가 없습니다.");
        }

        String creatorNo = schedule.getCreator() != null ? schedule.getCreator().getEmpNo() : null;

        if (creatorNo == null || !creatorNo.equals(requesterNo)) {
            throw new IllegalArgumentException("일정 작성자만 수정/삭제할 수 있습니다.");
        }
    }

    // 일정 수정
    @Override
    @Transactional
    public Integer updateSchedule(Integer scheduleId, ScheduleUpdateRequestDto dto, String requesterNo) {
        ScheduleEntity schedule = repository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        if (Boolean.TRUE.equals(schedule.getIsDeleted())) {
            throw new IllegalArgumentException("삭제된 일정은 수정할 수 없습니다.");
        }

        validateScheduleOwner(schedule, requesterNo);

        String typeValue = (dto.getType() == null || dto.getType().trim().isEmpty())
                ? "PERSONAL"
                : dto.getType().trim();

        // 부서 ID가 있으면 요청값의 부서를 사용한다.
        // 부서 일정인데 부서 ID가 없으면 기존 일정 작성자의 소속 부서를 자동으로 사용한다.
        DepartmentEntity department = null;
        if (dto.getDeptId() != null) {
            department = entityManager.getReference(DepartmentEntity.class, dto.getDeptId());
        } else if ("DEPARTMENT".equals(typeValue)
                && schedule.getCreator() != null
                && schedule.getCreator().getDepartment() != null) {
            department = schedule.getCreator().getDepartment();
        }

        ScheduleType scheduleType;
        try {
            scheduleType = ScheduleType.valueOf(typeValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 일정 유형입니다. " + typeValue);
        }

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

    // 일정 삭제
    @Override
    @Transactional
    public void deleteSchedule(Integer scheduleId, String requesterNo) {
        ScheduleEntity schedule = repository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        if (Boolean.TRUE.equals(schedule.getIsDeleted())) {
            throw new IllegalArgumentException("이미 삭제된 일정입니다.");
        }

        validateScheduleOwner(schedule, requesterNo);

        // 일정은 소프트 삭제하지만 참석자 연결 정보는 더 이상 필요 없으므로 같이 정리.
        schedule.getParticipants().clear();

        schedule.deleteSchedule();
    }

    // 참석자가 초대받은 일정에 참석/미정/불참으로 응답한다.
    @Override
    @Transactional
    public void updateParticipantStatus(Integer scheduleId, String empNo, String status) {
        if (empNo == null || empNo.trim().isEmpty()) {
            throw new IllegalArgumentException("응답 사용자 정보가 없습니다.");
        }

        ParticipantStatus participantStatus;
        try {
            participantStatus = ParticipantStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("올바르지 않은 참석 응답 상태입니다.");
        }

        List<ScheduleParticipantEntity> participants = entityManager.createQuery("""
                        SELECT sp
                          FROM ScheduleParticipantEntity sp
                         WHERE sp.schedule.scheduleId = :scheduleId
                           AND sp.employee.empNo = :empNo
                        """, ScheduleParticipantEntity.class)
                .setParameter("scheduleId", scheduleId)
                .setParameter("empNo", empNo.trim())
                .setMaxResults(1)
                .getResultList();

        if (participants.isEmpty()) {
            throw new IllegalArgumentException("해당 일정의 참석자가 아닙니다.");
        }

        participants.get(0).updateStatus(participantStatus);
    }

}