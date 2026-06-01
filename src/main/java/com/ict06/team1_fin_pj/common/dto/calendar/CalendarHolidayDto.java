package com.ict06.team1_fin_pj.common.dto.calendar;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// 캘린더에 공휴일 라벨을 표시하기 위한 DTO
@Getter
@Builder
public class CalendarHolidayDto {

    private LocalDate holidayDate;
    private String holidayName;
}
