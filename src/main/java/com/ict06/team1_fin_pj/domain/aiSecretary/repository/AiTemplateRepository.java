/**
 * @FileName : AiTemplateRepository.java
 * @Description : AI 추천 템플릿 Repository
 *                - 승인된 추천 템플릿 조회 및 저장
 *                - 사용자 AI 비서 템플릿 목록 제공
 *                - 관리자 승인 처리 후 추천 템플릿 반영에 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.28  송혜진       템플릿 요청 승인 후 추천 템플릿 등록 기준 반영
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiTemplateEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiTemplateRepository extends JpaRepository<AiTemplateEntity, Integer> {

    List<AiTemplateEntity> findByIsActiveTrueOrderByCreatedAtDesc();

    List<AiTemplateEntity> findByTypeAndIsActiveTrueOrderByCreatedAtDesc(DocumentType type);
}