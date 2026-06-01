package com.ict06.team1_fin_pj.common.dto.calendar;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// 일정 수정 요청
@Getter
@Setter
public class ScheduleUpdateRequestDto {
    private String title;

    private String content;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String type;

    private Integer deptId;

    private String category;

    private String location;

    private Boolean isAllDay;

    private Boolean isPublic;

    private String repeatRule;

    // 수정 시 새로 선택된 참석자 사번 목록.
    private List<String> participantNos;
}
