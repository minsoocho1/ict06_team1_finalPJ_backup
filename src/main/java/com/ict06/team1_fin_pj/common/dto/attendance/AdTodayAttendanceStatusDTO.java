package com.ict06.team1_fin_pj.common.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 오늘 출근 현황 DTO
 *
 * 역할:
 * - 관리자 근태 통계 화면에서
 *   오늘 기준 출근 현황 카드를 표시할 때 사용한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdTodayAttendanceStatusDTO {

    // 오늘 출근 완료 건수
    private long checkedInCount;

    // 오늘 지각 건수
    private long lateCount;

    // 오늘 미출근 건수
    private long notCheckedInCount;
}