/**
 * @FileName : DocumentProcessLogRepository.java
 * @Description : RAG 문서 처리 이력 Repository
 *                - DOCUMENT_PROCESS_LOG 테이블 조회 및 저장
 *                - 문서 업로드, 청킹, 임베딩, 승인 대기, 반영 완료 처리 이력 관리
 *                - 문서 처리 실패 원인 및 단계별 상태 추적에 사용
 *                - 관리자 RAG 문서 관리 화면의 처리 로그 조회에 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 22
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.22  송혜진       최초 생성
 * @ 2026.05.22  송혜진       RAG 문서 처리 단계 및 재처리 흐름 기준 반영
 * @ 2026.05.27  송혜진       관리자 RAG 문서 상세 처리 이력 조회 기준 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.DocumentProcessLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentProcessLogRepository extends JpaRepository<DocumentProcessLogEntity, Integer> {
    void deleteByDocument_DocId(Integer docId);
    Optional<DocumentProcessLogEntity> findTopByDocument_DocIdAndErrorMessageIsNotNullOrderByJobIdDesc(Integer docId);
}
