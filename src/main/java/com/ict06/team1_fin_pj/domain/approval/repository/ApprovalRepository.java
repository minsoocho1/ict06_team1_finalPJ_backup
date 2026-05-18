package com.ict06.team1_fin_pj.domain.approval.repository;

import com.ict06.team1_fin_pj.domain.approval.entity.ApprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 결재 문서 기본 Repository입니다.
 *
 * - 단순 저장/조회는 JpaRepository 기능을 사용합니다.
 * - 조건이 붙는 개인 문서함/임시저장함 목록 조회는 ApprovalRepositoryCustom의 QueryDSL 구현을 사용합니다.
 */
public interface ApprovalRepository extends JpaRepository<ApprovalEntity, Integer>, ApprovalRepositoryCustom {
}
