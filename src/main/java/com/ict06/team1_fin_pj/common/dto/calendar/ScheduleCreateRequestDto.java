package com.ict06.team1_fin_pj.common.dto.calendar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 일정 간단 등록 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleCreateRequestDto {

    private String title;

    private String content;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String type;

    private String creatorNo;

    private Integer deptId;

    private String category;

    private String location;

    private Boolean isAllDay;

    private Boolean isPublic;

    private String repeatRule;

    // 참석자로 선택된 사원 번호 목록
    private List<String> participantNos;
}