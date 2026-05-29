package com.ict06.team1_fin_pj.common.dto.calendar;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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

    // 일정 참석자 목록.
    private List<ParticipantDto> participants;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParticipantDto {

        private String empId;

        private String name;

        private Integer deptId;

        private String deptName;

        private String positionName;

        // 참석자의 현재 응답 상태. ACCEPTED, PENDING, REJECTED 중 하나로 내려준다.
        private String status;

        // 참석자가 마지막으로 응답한 시각. 아직 응답하지 않은 경우 null 이다.
        private LocalDateTime respondedAt;
    }



}
