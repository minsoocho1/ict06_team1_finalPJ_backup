package com.ict06.team1_fin_pj.domain.attendance.repository;

import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceSearchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 관리자 근태 검색용 Custom Repository
 *
 * 역할:
 * - 관리자 근태 현황 검색 기능 담당
 * - QueryDSL 동적 검색 처리
 * - 날짜 / 부서 / 상태 / 사원명 조건 검색
 * - Pageable 기반 페이징 처리
 */
public interface AttendanceRepositoryCustom {

    /**
     * 관리자 근태 검색
     *
     * @param searchDTO 검색 조건 DTO
     * @param pageable 페이징 정보
     * @return 검색 결과 Page 객체
     */
    Page<AdAttendanceDTO> searchAdminAttendance(
            AdAttendanceSearchDTO searchDTO,
            Pageable pageable
    );
}