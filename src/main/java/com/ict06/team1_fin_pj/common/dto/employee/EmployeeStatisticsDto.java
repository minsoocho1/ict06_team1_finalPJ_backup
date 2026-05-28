package com.ict06.team1_fin_pj.common.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/*
 * 인사 통계 DTO
 *
 * 관리자 인사 통계 탭에서 사용하는:
 * - 재직 상태 통계
 * - 권한 상태 통계
 * - 부서별 인원 통계
 * - 직급별 인원 통계
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeStatisticsDto {

    /*
     * 재직 상태 통계
     */
    private long activeCount;
    private long leaveCount;
    private long resignCount;

    /*
     * 권한 상태 통계
     */
    private long adminCount;
    private long leaderCount;
    private long userCount;

    /*
     * 부서별 인원 통계
     *
     * key   = 부서명
     * value = 인원 수
     */
    private Map<String, Long> departmentStatistics;

    /*
     * 직급별 인원 통계
     *
     * key   = 직급명
     * value = 인원 수
     */
    private Map<String, Long> positionStatistics;
}