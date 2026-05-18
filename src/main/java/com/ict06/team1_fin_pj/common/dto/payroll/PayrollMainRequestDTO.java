package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.Data;

// 급여대장 메인 조회 요청 DTO
@Data
public class PayrollMainRequestDTO {

    // 사원 PK
    private String empNo;

    // 작성년도
    private Integer payYear;

    // 작성월
    private Integer payMonth;

    // 지급/공제항목 설정 변경 선택값
    private String itemSettingDecision;
}
