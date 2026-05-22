package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.Data;

//  급여대장 사원 검색 요청 DTO
@Data
public class PayrollEmployeeSearchDTO {

    // 검색어
    private String keyword;

    // 검색 타입 (NAME / EMP_NO)
    private String searchType;

    // 조회 개수 제한
    private Integer limit;

    // 전체보기 여부
    private Boolean showAll;

}
