package com.ict06.team1_fin_pj.common.dto.attendance;

import com.ict06.team1_fin_pj.domain.attendance.entity.AttendanceStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 관리자 근태 수정 요청 DTO
 *
 * 관리자 화면에서 근태 수정 Modal submit 시
 * Controller로 전달받는 요청 데이터
 */
@Getter
@Setter
public class AdAttendanceUpdateRequestDTO {

    // 수정 대상 근태 ID
    private Integer attendanceId;

    // 수정할 출근 시간
    private LocalDateTime checkInAt;

    // 수정할 퇴근 시간
    private LocalDateTime checkOutAt;

    // 수정할 근태 상태
    private AttendanceStatus status;

    // 관리자 수정 사유
    private String changeReason;
}