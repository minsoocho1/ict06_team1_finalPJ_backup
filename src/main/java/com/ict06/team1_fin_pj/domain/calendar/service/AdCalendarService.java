package com.ict06.team1_fin_pj.domain.calendar.service;

import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleListResponseDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleUpdateRequestDto;

import java.util.List;

public interface AdCalendarService {

    // 관리자 화면에서 개인 비공개 일정을 제외한 관리 가능한 일정을 조회한다.
    List<ScheduleListResponseDto> getAdminScheduleList(List<String> selectedMemberNos, String adminEmpNo);

    // 관리자는 개인 비공개 일정을 제외한 범위에서 일정을 등록할 수 있다.
    // 화면에서 작성자 사번을 직접 받지 않으면 로그인한 관리자 사번을 작성자로 사용한다.
    Integer createAdminSchedule(ScheduleCreateRequestDto dto, String adminEmpNo);

    // 관리자는 공개 일정과 본인 비공개 개인일정만 수정할 수 있다.
    Integer updateAdminSchedule(Integer scheduleId, ScheduleUpdateRequestDto dto, String adminEmpNo);

    // 관리자는 공개 일정과 본인 비공개 개인일정만 삭제할 수 있다.
    void deleteAdminSchedule(Integer scheduleId, String adminEmpNo);
}
