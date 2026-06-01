package com.ict06.team1_fin_pj.domain.attendance.service;

import com.ict06.team1_fin_pj.common.dto.attendance.AttendanceDTO;

import java.util.List;

public interface AttendanceService {

    // 출근 처리
    void checkIn(String empNo, Double lat, Double lng);

    // 내 근태 목록 조회 (DTO 반환)
    List<AttendanceDTO> getMyAttendance(String empNo);

    // 퇴근 처리
    void checkOut(String empNo, Double lat, Double lng);

}