package com.ict06.team1_fin_pj.common.dto.calendar;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 캘린더가 휴가/병가/반차/조퇴/경조사자를 표시하거나 선택 제한할 때 사용하는 조회 전용 DTO
@Getter
@Builder
public class CalendarUnavailableEmployeeDto {

    private String empNo;
    private String name;

    // LEAVE, SICK, HALF_LEAVE, EARLY, FAMILY_EVENT 등 캘린더 구분용 타입
    private String reasonType;

    // 연차, 오전반차, 오후반차, 조퇴, 병가, 경조사 등 실제 부재 유형명
    private String reasonName;

    private LocalDate startDate;
    private LocalDate endDate;

    // 실제 일정 등록/참석 제한에 사용할 시간 범위
    private LocalDateTime unavailableStartTime;
    private LocalDateTime unavailableEndTime;

    // 캘린더에서 종일 라벨로 보여줄지 판단하기 위한 값
    private Boolean isAllDay;
}