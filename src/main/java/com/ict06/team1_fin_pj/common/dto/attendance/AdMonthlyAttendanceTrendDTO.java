package com.ict06.team1_fin_pj.common.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 월별 근태 추이 DTO
 *
 * 역할:
 * - 관리자 근태 통계 화면에서
 *   월별 정상/지각/조퇴 추이 차트 데이터를 전달할 때 사용한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdMonthlyAttendanceTrendDTO {

    // 월 정보
    private String month;

    // 정상 출근 건수
    private long onTimeCount;

    // 지각 건수
    private long lateCount;

    // 조퇴 건수
    private long earlyCount;
}