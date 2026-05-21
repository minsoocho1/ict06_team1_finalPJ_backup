package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.Data;
import lombok.NoArgsConstructor;

// 급여요약 전체조회 검색 조건 DTO
@Data
@NoArgsConstructor
public class PayrollSummarySearchDTO {

    // 조회년도
    private Integer payYear;

    // 조회월
    private Integer payMonth;

    // 사번 또는 이름 검색어
    private String keyword;

    // 부서 필터
    private String deptId;

    // 직급 필터
    private String positionId;

    // 상태 필터: NEW / DRAFT / CONFIRMED / PAID
    private String status;

    // 정렬: DEFAULT / EMP_NO / NAME / NET_DESC / NET_ASC
    private String sortType = "DEFAULT";

    // 현재 페이지
    private int page = 1;

    // 한 페이지 개수
    private int size = 10;
}
