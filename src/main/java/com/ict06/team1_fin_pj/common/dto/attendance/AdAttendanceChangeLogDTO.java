package com.ict06.team1_fin_pj.common.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 근태 수정 이력 조회 DTO
 *
 * ATTENDANCE_CHANGE_LOG 데이터를
 * 관리자 화면에 출력하기 위한 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdAttendanceChangeLogDTO {

    // 수정 이력 ID
    private Integer changeLogId;

    // 수정 대상 근태 ID
    private Integer attendanceId;

    // 근무일
    private String workDate;

    // 수정 전 출근 시간
    private String beforeCheckIn;

    // 수정 후 출근 시간
    private String afterCheckIn;

    // 수정 전 퇴근 시간
    private String beforeCheckOut;

    // 수정 후 퇴근 시간
    private String afterCheckOut;

    // 수정 전 상태
    private String beforeStatus;

    // 수정 후 상태
    private String afterStatus;

    // 수정 사유
    private String changeReason;

    // 수정한 관리자 사번
    private String changedBy;

    // 수정한 관리자 이름
    private String changedByName;

    // 수정 일시
    private String changedAt;
}