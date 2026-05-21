package com.ict06.team1_fin_pj.common.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 근태 목록 DTO
 *
 * 역할:
 * - 관리자 근태 현황 화면 데이터 전달
 * - Controller → Thymeleaf 전달용 DTO
 * - 나중에 QueryDSL 조회 결과 매핑 예정
 */
@Getter                 // getter 자동 생성
@Setter                 // setter 자동 생성
@NoArgsConstructor      // 기본 생성자 자동 생성
@AllArgsConstructor     // 전체 필드 생성자 자동 생성
@Builder                // builder 패턴 사용 가능
public class AdAttendanceDTO {

    // 근태 ID
    // ATTENDANCE.attendance_id 값
    private Integer attendanceId;

    // 사번
    private String empNo;

    // 사원명
    private String empName;

    // 부서명
    private String deptName;

    // 근무일
    private String workDate;

    // 출근 시간
    private String checkIn;

    // 퇴근 시간
    private String checkOut;

    // 총 근무 시간
    private String workHours;

    // 근태 상태
    private String status;
}