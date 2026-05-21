package com.ict06.team1_fin_pj.common.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 관리자 연차/휴가 현황 화면에서 사용할 DTO
 *
 * 화면 경로:
 * /admin/attendance/leave
 *
 * 사용 목적:
 * - 사원별 총 연차
 * - 사용 연차
 * - 잔여 연차
 * 를 관리자 화면 테이블에 출력하기 위한 데이터 전달 객체
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdLeaveStatusDTO {

    // 사번
    private String empNo;

    // 사원명
    private String empName;

    // 부서명
    private String deptName;

    // 총 발생 연차
    private BigDecimal totalDays;

    // 사용 연차
    private BigDecimal usedDays;

    // 잔여 연차
    private BigDecimal remainDays;
}