package com.ict06.team1_fin_pj.domain.attendance.service;

import com.ict06.team1_fin_pj.common.dto.attendance.LeaveHistoryDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.LeaveSummaryDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdLeaveStatusDTO;

import org.springframework.data.domain.Page;

import java.util.List;

// 연차 현황 Service 인터페이스
public interface LeaveService {

    // 1. 총 연차 / 사용 연차 / 잔여 연차 조회
    LeaveSummaryDTO getLeaveSummary(String empNo);

    // 2. 연차 사용 내역 조회
    List<LeaveHistoryDTO> getLeaveHistory(String empNo);

    // ==============================
    // 3. 연차 자동 부여 API
    // ==============================
    // 입사일 기준으로 올해 부여할 연차를 계산하고
    // LEAVE_OCCURRENCE 테이블에 저장한다.
    LeaveSummaryDTO grantAnnualLeave(String empNo);

    /**
     * 관리자용 전체 사원 연차 자동 부여
     *
     * 역할:
     * - 재직 중인 전체 사원을 대상으로 연차를 자동 부여한다.
     * - 이미 해당 연도 연차가 부여된 사원은 중복 생성하지 않는다.
     *
     * 반환값:
     * - 새로 연차가 부여된 사원 수
     */
    int grantAnnualLeaveForAllEmployees();

    /**
     * 1년 미만 신입사원 월차 자동 부여
     *
     * 역할:
     * - 입사 1년 미만 재직자를 대상으로 한다.
     * - 매월 1일 실행되는 Scheduler에서 호출한다.
     * - 이번 달 이미 월차가 부여된 사원은 중복 생성하지 않는다.
     *
     * 반환값:
     * - 새로 월차가 부여된 사원 수
     */
    int grantMonthlyLeaveForNewEmployees();

    // 관리자 연차/휴가 현황 목록 조회
    // keyword  : 사원명 검색어
    // deptId   : 부서 ID
    // sortType : 정렬 조건
    // page     : 현재 페이지 번호
    // size     : 한 페이지당 데이터 개수
    Page<AdLeaveStatusDTO> findAdminLeaveStatusList(
            String keyword,
            Integer deptId,
            String sortType,
            int page,
            int size
    );

    /**
     * 관리자 연차/휴가 현황 Excel 다운로드
     *
     * 역할:
     * - 현재 검색 조건에 맞는 연차/휴가 현황 목록을 조회한다.
     * - 조회 결과를 Excel(.xlsx) 파일 byte 배열로 생성한다.
     *
     * @param keyword 사원명 검색어
     * @param deptId 부서 ID
     * @param sortType 정렬 조건
     * @return Excel 파일 byte 배열
     */
    byte[] downloadLeaveExcel(
            String keyword,
            Integer deptId,
            String sortType
    );
}