package com.ict06.team1_fin_pj.domain.attendance.repository;

import com.ict06.team1_fin_pj.domain.attendance.entity.AttChangeLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 근태 수정 이력 Repository
 *
 * ATTENDANCE_CHANGE_LOG 테이블에 저장된
 * 관리자 수정 이력을 조회/저장한다.
 */
public interface AttChangeLogRepository extends JpaRepository<AttChangeLogEntity, Integer> {

    // 근태 수정 이력 페이징 조회
    Page<AttChangeLogEntity> findAllByOrderByChangedAtDesc(Pageable pageable);
}