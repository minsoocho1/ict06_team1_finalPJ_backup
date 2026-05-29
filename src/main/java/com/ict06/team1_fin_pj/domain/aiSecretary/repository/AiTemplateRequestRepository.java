/**
 * @FileName : AiTemplateRequestRepository.java
 * @Description : AI 추천 템플릿 요청 Repository
 *                - 사용자가 요청한 추천 템플릿 목록 조회 및 저장
 *                - 관리자 승인/반려 대상 템플릿 요청 조회
 *                - AI 추천 템플릿 승인 관리 화면에서 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.28  송혜진       관리자 추천 템플릿 승인 관리 기준 반영
 */

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