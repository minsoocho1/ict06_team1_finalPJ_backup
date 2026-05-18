package com.ict06.team1_fin_pj.domain.payroll.repository;

import com.ict06.team1_fin_pj.domain.payroll.entity.PayrollEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 급여대장 JPA Repository
public interface PayrollRepository extends JpaRepository<PayrollEntity, Integer>, PayrollRepositoryCustom{

    // 사번 + 지급월 기준 급여대장 조회
    Optional<PayrollEntity> findByEmployee_EmpNoAndPayMonth(String empNo, String payMonth);
}
