/**
 * @FileName : ChecklistRepository.java
 * @Description : AI 온보딩 체크리스트 Repository
 *                CHECKLIST 테이블 조회 담당
 * @Author : 김다솜
 * @Date : 2026. 04. 29
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    김다솜        최초 생성 및 체크리스트 조회 Repository 구현
 * @ 2026.05.14    김다솜        관련 콘텐츠 ID 기반 조회 및 재연결 조회 메서드 추가
 */

package com.ict06.team1_fin_pj.domain.onboarding.repository;

import com.ict06.team1_fin_pj.domain.onboarding.entity.ChecklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChecklistRepository extends JpaRepository<ChecklistEntity, Integer> {

    List<ChecklistEntity> findAllByOrderByOrderNoAsc();

    Optional<ChecklistEntity> findByRelatedContent_ContentId(Integer contentId);

    List<ChecklistEntity> findByRelatedContentIsNotNull();
}
