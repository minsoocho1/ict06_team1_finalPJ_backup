package com.ict06.team1_fin_pj.common.dto.calendar;

import lombok.*;

import java.time.LocalDateTime;

// 캘린더 목록 응답 DTO
// 화면에 보여줄 일정 정보만 담는다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleListResponseDto {

    private Integer scheduleId;

    private String title;

    private String content;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String type;

    private String category;

    private String location;

    private Boolean isAllDay;

    private Boolean isPublic;

    private String repeatRule;

    private String creatorNo;

    private String creatorName;

}
