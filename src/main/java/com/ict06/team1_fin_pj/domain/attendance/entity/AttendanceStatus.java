package com.ict06.team1_fin_pj.domain.attendance.entity;

import lombok.Getter;

@Getter
public enum AttendanceStatus {
    ON_TIME("정상출근"),
    ABSENT("결근"),
    LATE("지각"),
    EARLY("조퇴"),
    LEFT("퇴근"),
    OVERTIME("연장근무"),

    // 전자결재/휴가 연동용 상태
    LEAVE("휴가"),
    HALF_LEAVE("반차");

    private final String label;

    AttendanceStatus(String label) {
        this.label = label;
    }

}