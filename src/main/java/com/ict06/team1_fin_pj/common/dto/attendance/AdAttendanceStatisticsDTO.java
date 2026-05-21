package com.ict06.team1_fin_pj.common.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 근태 통계 DTO
 *
 * 역할:
 * - 관리자 근태 통계 화면에 표시할 요약 통계 데이터를 담는다.
 * - Controller에서 Thymeleaf로 전달된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdAttendanceStatisticsDTO {

    // 전체 평균 근무시간
    private double averageWorkHours;

    // 전체 지각률
    private double lateRate;

    // 전체 조퇴율
    private double earlyRate;

    // 전체 정상 출근률
    private double onTimeRate;

    // 전체 근태 건수
    private int totalCount;

    // 정상 출근 건수
    private int onTimeCount;

    // 지각 건수
    private int lateCount;

    // 조퇴 건수
    private int earlyCount;

}