package com.ict06.team1_fin_pj.domain.approval.repository;

import com.ict06.team1_fin_pj.domain.approval.entity.AppLineTemplateEntity;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author : 송영은$
 * description : 결재선 서식(APP_LINE_TEMPLATE) Repository입니다.
 * ========================================
 * DATE         AUTHOR      NOTE
 * 2026-05-06   송영은       최초 생성
 */
@Repository
public interface AppLineTemplateRepository extends JpaRepository<AppLineTemplateEntity, Integer> {

    @NonNull
    Page<AppLineTemplateEntity> findAll(@NonNull Pageable pageable);

    /**
     * 결재선 서식 상세 화면에서 상세 결재 조건을 함께 조회합니다.
     * detail 하위의 결재자/부서/직급을 fetch join해 화면 변환 중 N+1 쿼리를 줄입니다.
     */
    @Query("""
        SELECT DISTINCT t
        FROM AppLineTemplateEntity t
        LEFT JOIN FETCH t.details d
        LEFT JOIN FETCH d.approver
        LEFT JOIN FETCH d.department
        LEFT JOIN FETCH d.minPosition
        LEFT JOIN FETCH t.createdBy
        WHERE t.templateId = :id
    """)
    Optional<AppLineTemplateEntity> findDetailById(@Param("id") Integer id);
}
