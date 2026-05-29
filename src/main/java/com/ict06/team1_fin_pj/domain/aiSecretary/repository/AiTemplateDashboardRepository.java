/**
 * @FileName : AiTemplateDashboardRepository.java
 * @Description : 관리자 AI 템플릿 대시보드 Repository
 *                - AI 추천 템플릿 요청 및 승인 현황 조회
 *                - 템플릿 상태별 집계 데이터 조회
 *                - 관리자 AI 템플릿 승인 관리 화면의 통계/목록 데이터 조회에 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 05. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.05.28  송혜진       최초 생성
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AiTemplateDashboardRepository extends JpaRepository<AiTemplateEntity, Integer> {

    long countByCreatedAtBetween(LocalDateTime startAt, LocalDateTime endAt);
}
