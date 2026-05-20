package com.ict06.team1_fin_pj.common.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부서별 평균 근무시간 DTO
 *
 * 역할:
 * - 관리자 근태 통계 화면에서
 *   부서별 평균 근무시간 차트 데이터를 전달할 때 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdDepartmentWorkHourDTO {

    // 부서명
    private String deptName;

    // 평균 근무시간
    private double averageWorkHours;
}
