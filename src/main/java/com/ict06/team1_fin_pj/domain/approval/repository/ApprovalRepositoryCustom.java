package com.ict06.team1_fin_pj.domain.approval.repository;

import com.ict06.team1_fin_pj.common.dto.approval.ApprovalListResponseDto;
import com.ict06.team1_fin_pj.domain.approval.entity.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 결재 문서 목록처럼 조건이 복잡해질 수 있는 조회를 위한 QueryDSL 전용 Repository 인터페이스입니다.
 *
 * Spring Data JPA 메서드명 쿼리로도 단순 조회는 가능하지만,
 * 상태 필터, 작성자/결재자/참조자 조건, 삭제 여부, 페이징, DTO 직접 조회를 함께 다루기 위해 QueryDSL로 분리했습니다.
 */
public interface ApprovalRepositoryCustom {

    /**
     * 개인 문서함 목록 조회
     *
     * @param writerNo 로그인 사용자 사번
     * @param status 조회할 결재 상태. null이면 DRAFT를 제외한 전체 문서를 조회합니다.
     * @param pageable 페이지 번호, 크기 정보
     */
    Page<ApprovalListResponseDto> findMyDocuments(
            String writerNo,
            ApprovalStatus status,
            Pageable pageable
    );

    /**
     * 임시저장 문서함 목록 조회
     *
     * @param writerNo 로그인 사용자 사번
     * @param pageable 페이지 번호, 크기 정보
     */
    Page<ApprovalListResponseDto> findMyDrafts(
            String writerNo,
            Pageable pageable
    );

    /**
     * 참조 문서함 목록 조회
     *
     * 참조자는 결재자는 아니지만 문서 열람 권한이 있는 대상입니다.
     * APP_LINE에서 approverNo가 로그인 사용자이고 stepOrder가 0인 문서를 조회합니다.
     *
     * @param referenceNo 로그인 사용자 사번
     * @param status 조회할 결재 상태. null이면 DRAFT를 제외한 전체 참조 문서를 조회합니다.
     * @param pageable 페이지 번호, 크기 정보
     */
    Page<ApprovalListResponseDto> findMyReferencedDocuments(
            String referenceNo,
            ApprovalStatus status,
            Pageable pageable
    );

    /**
     * 결재 대기 문서함 목록 조회
     *
     * 현재 로그인 사용자가 지금 승인/반려해야 하는 문서만 조회합니다.
     *
     * @param approverNo 로그인 사용자 사번
     * @param pageable 페이지 번호, 크기 정보
     */
    Page<ApprovalListResponseDto> findPendingApprovals(
            String approverNo,
            Pageable pageable
    );

    /**
     * 결재 예정 문서함 목록 조회
     *
     * 로그인 사용자가 결재선에는 포함되어 있지만 아직 본인 차례가 아닌 문서를 조회합니다.
     * stepOrder=0인 참조자는 결재자가 아니므로 예정 문서함에서 제외합니다.
     *
     * @param approverNo 로그인 사용자 사번
     * @param pageable 페이지 번호, 크기 정보
     */
    Page<ApprovalListResponseDto> findUpcomingApprovals(
            String approverNo,
            Pageable pageable
    );
}
