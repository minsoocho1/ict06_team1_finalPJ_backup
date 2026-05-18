package com.ict06.team1_fin_pj.domain.approval.repository;

import com.ict06.team1_fin_pj.domain.approval.entity.AppFormEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author : 송영은
 * description : App_Form와 연결하는 Repository. Jpa와 QueryDSL 활용
 * ========================================
 * DATE         AUTHOR      NOTE
 * 2026-04-29   송영은       최초 생성
 **/
@Repository
public interface AppFormRepository extends JpaRepository<AppFormEntity, Integer> {

    // @NonNull : 부모의 "null 안전 보장" 규칙을 자식이 이어받기
    @NonNull
    Page<AppFormEntity> findAll(@NonNull Pageable pageable);

    /**
     * 특정 결재선 서식을 사용하는 결재 서식 목록을 조회합니다.
     * 하나의 결재선 서식을 여러 결재 서식이 공유할 수 있으므로 목록으로 반환합니다.
     */
    List<AppFormEntity> findByLineTemplate_TemplateId(Integer templateId);
}
