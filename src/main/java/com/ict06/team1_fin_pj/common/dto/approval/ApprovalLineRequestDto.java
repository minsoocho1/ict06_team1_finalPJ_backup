package com.ict06.team1_fin_pj.common.dto.approval;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApprovalLineRequestDto {

    // 결재자로 지정할 사원의 사번입니다. EmpEntity.empNo와 매칭됩니다.
    private String approverNo;

    // 결재 순서입니다. 1부터 시작하며 가장 작은 순서의 결재자가 현재 결재자가 됩니다.
    private Integer stepOrder;
}
