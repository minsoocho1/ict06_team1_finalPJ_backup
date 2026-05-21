package com.ict06.team1_fin_pj.common.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부서별 지각률 DTO
 *
 * 역할:
 * - 관리자 근태 통계 화면에서
 *   부서별 지각률 차트 데이터를 전달할 때 사용한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdDepartmentLateRateDTO {

    // 부서명
    private String deptName;

    // 부서 전체 근태 건수
    private long totalCount;

    // 부서 지각 건수
    private long lateCount;

    // 부서 지각률
    private double lateRate;
}