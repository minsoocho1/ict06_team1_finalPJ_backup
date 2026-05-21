package com.ict06.team1_fin_pj.domain.calendar.service;

import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleListResponseDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleUpdateRequestDto;

import java.util.List;

public interface CalendarService {

    // 일정 등록
    Integer createSchedule(ScheduleCreateRequestDto dto);

    // 일정 목록 조회
    // 로그인 사용자 기준 기본 일정과 조직도에서 선택한 구성원의 공개 개인일정을 반환
    List<ScheduleListResponseDto> getScheduleList(String empNo, List<String> selectedMemberNos);

    // 일정 수정
    Integer updateSchedule(Integer scheduleId, ScheduleUpdateRequestDto dto, String requesterNo);

    // 일정 삭제
    void deleteSchedule(Integer scheduleId, String requesterNo);

    // 참석자 응답 상태 변경
    void updateParticipantStatus(Integer scheduleId, String empNo, String status);
}
