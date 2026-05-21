package com.ict06.team1_fin_pj.domain.attendance.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ict06.team1_fin_pj.common.dto.attendance.AdLeaveStatusDTO;

import java.util.List;

/**
 * 관리자 연차/휴가 현황 조회용 Custom Repository
 *
 * 역할:
 * - QueryDSL로 사원별 연차 현황을 집계한다.
 * - LEAVE_OCCURRENCE의 발생/사용/잔여 연차를 사원별로 합산한다.
 */
public interface LeaveOccurrenceRepositoryCustom {

    /**
     * 관리자 연차 현황 목록 조회
     *
     * keyword  : 사원명 검색어
     * deptId   : 부서 ID
     * sortType : 정렬 조건
     * pageable : 현재 페이지 번호와 한 페이지당 개수
     */
    Page<AdLeaveStatusDTO> findAdminLeaveStatusList(
            String keyword,
            Integer deptId,
            String sortType,
            Pageable pageable
    );
}