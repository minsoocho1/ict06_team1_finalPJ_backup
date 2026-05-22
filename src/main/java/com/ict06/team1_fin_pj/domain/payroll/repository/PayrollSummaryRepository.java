package com.ict06.team1_fin_pj.domain.payroll.repository;

import com.ict06.team1_fin_pj.domain.payroll.entity.PayrollEntity;
import org.springframework.data.jpa.repository.JpaRepository;

// 급여요약 JPA Repository
// - 기본 CRUD는 JpaRepository가 담당한다.
// - 복잡한 조회는 PayrollSummaryRepositoryCustom(QueryDSL)이 담당한다.
public interface PayrollSummaryRepository extends JpaRepository<PayrollEntity, Integer>, PayrollSummaryRepositoryCustom  {
}
