package com.ict06.team1_fin_pj.domain.attendance.service;

import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceSummaryDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceSearchDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceStatisticsDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdDepartmentWorkHourDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdMonthlyAttendanceTrendDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdDepartmentLateRateDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdTodayAttendanceStatusDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceUpdateRequestDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceChangeLogDTO;

import java.util.List;

import org.springframework.data.domain.Page;

// 관리자 근태 관리 Service 인터페이스
public interface AdAttendanceService {


    // 관리자 근태 목록 조회
    // searchDTO: 날짜/부서/상태/사원명 검색 조건
    Page<AdAttendanceDTO> getAttendanceList(
            AdAttendanceSearchDTO searchDTO,
            int page,
            int size
    );

    // 관리자 근태 상단 요약 카드 조회
    AdAttendanceSummaryDTO getAttendanceSummary();

    // 관리자 근태 통계 조회
    AdAttendanceStatisticsDTO getAttendanceStatistics();

    // 부서별 평균 근무시간 조회
    List<AdDepartmentWorkHourDTO> getDepartmentAverageWorkHours();

    // 월별 근태 추이 조회
    List<AdMonthlyAttendanceTrendDTO> getMonthlyAttendanceTrend();

    // 부서별 지각률 조회
    List<AdDepartmentLateRateDTO> getDepartmentLateRates();

    // 오늘 출근 현황 조회
    AdTodayAttendanceStatusDTO getTodayAttendanceStatus();

    // 관리자 근태 수정
    void updateAttendanceByAdmin(AdAttendanceUpdateRequestDTO request, String adminEmpNo);

    // 관리자 근태 수정 이력 조회(페이징)
    Page<AdAttendanceChangeLogDTO> getAttendanceChangeLogs(int page, int size);

    // 관리자 근태 현황 Excel 다운로드
    byte[] downloadAttendanceExcel(AdAttendanceSearchDTO searchDTO);
}

