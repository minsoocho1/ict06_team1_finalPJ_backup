package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiTemplateRequestEntity;
import com.ict06.team1_fin_pj.domain.approval.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AiTemplateRequestRepository extends JpaRepository<AiTemplateRequestEntity, Integer> {

    // 내 요청 목록 조회
    List<AiTemplateRequestEntity> findByEmployee_EmpNoOrderByCreatedAtDesc(String empNo);

    // 관리자 화면에서 상태별 요청 조회 예정
    List<AiTemplateRequestEntity> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    // 중복 요청 방지
    boolean existsByEmployee_EmpNoAndTitleAndCategoryAndDeptAndSituationAndStatusIn(
            String empNo,
            String title,
            String category,
            String dept,
            String situation,
            Collection<RequestStatus> statuses
    );
}