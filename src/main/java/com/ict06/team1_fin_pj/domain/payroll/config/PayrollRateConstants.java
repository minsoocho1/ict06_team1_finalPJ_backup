package com.ict06.team1_fin_pj.domain.payroll.config;

import java.math.BigDecimal;

// 급여 계산에 사용하는 고정 요율 상수
// - properties 파일 없이 고정 요율로 계산한다.
// - 서비스 내부에 숫자를 직접 쓰지 않기 위해 상수 클래스로 분리한다.
public final class PayrollRateConstants {

    private PayrollRateConstants() {
    }

    // 4대보험 근로자 부담분
    public static final BigDecimal NATIONAL_PENSION = new BigDecimal("0.045");
    public static final BigDecimal HEALTH_INSURANCE = new BigDecimal("0.03545");

    // 장기요양보험은 건강보험료 기준 요율
    public static final BigDecimal LONG_TERM_CARE = new BigDecimal("0.1281");

    public static final BigDecimal EMPLOYMENT_INSURANCE = new BigDecimal("0.009");

    // 임시 원천징수 고정세율
    public static final BigDecimal INCOME_TAX = new BigDecimal("0.03");

    // 지방소득세는 소득세 기준
    public static final BigDecimal LOCAL_INCOME_TAX = new BigDecimal("0.10");
}
