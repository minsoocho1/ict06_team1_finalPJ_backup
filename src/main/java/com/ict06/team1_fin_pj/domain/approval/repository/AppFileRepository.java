package com.ict06.team1_fin_pj.domain.approval.repository;

import com.ict06.team1_fin_pj.domain.approval.entity.AppFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 결재 첨부파일(APP_FILE) 기본 CRUD Repository입니다.
 *
 * 첨부파일 삭제는 파일이 속한 결재 문서의 작성자와 상태를 함께 검증해야 하므로,
 * 실제 권한 판단은 서비스 계층에서 처리합니다.
 */
public interface AppFileRepository extends JpaRepository<AppFileEntity, Integer> {
}
