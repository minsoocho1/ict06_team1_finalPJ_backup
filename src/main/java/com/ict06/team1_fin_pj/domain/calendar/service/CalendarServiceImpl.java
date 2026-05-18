package com.ict06.team1_fin_pj.domain.calendar.service;

import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleListResponseDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleUpdateRequestDto;
import com.ict06.team1_fin_pj.domain.calendar.entity.ScheduleEntity;
import com.ict06.team1_fin_pj.domain.calendar.entity.ScheduleType;
import com.ict06.team1_fin_pj.domain.calendar.repository.CalendarRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.DepartmentEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        ScheduleEntity saved = repository.save(entity);

        return saved.getScheduleId();
    }

    // 일정 목록 조회
    // 로그인 사용자 기준으로 조회 가능한 일정만 캘린더 데이터로 변환한다.
    @Override
    @Transactional(readOnly = true)
    public List<ScheduleListResponseDto> getScheduleList(String empNo) {
        EmpEntity loginUser = entityManager.find(EmpEntity.class, empNo);

        if (loginUser == null) {
            throw new IllegalArgumentException("로그인 사용자 정보를 찾을 수 없습니다.");
        }

        Integer deptId = loginUser.getDepartment() != null
                ? loginUser.getDepartment().getDeptId()
                : null;

        return repository.findVisibleSchedules(
                        empNo,
                        deptId,
                        ScheduleType.PERSONAL,
                        ScheduleType.DEPARTMENT,
                        ScheduleType.COMPANY
                )
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
                        .build())
                .toList();
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

        schedule.deleteSchedule();
    }

}