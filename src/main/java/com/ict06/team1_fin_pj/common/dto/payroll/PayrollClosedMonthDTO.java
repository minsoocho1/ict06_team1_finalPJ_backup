package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayrollClosedMonthDTO {

    private String payMonth;

    private LocalDateTime payrollUpdatedAt;
}
