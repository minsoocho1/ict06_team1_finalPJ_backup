package com.ict06.team1_fin_pj.domain.attendance.entity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ATTENDANCE",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_emp_work_date",
                        columnNames = {"emp_no", "work_date"}
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Integer attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_no", nullable = false)
    private EmpEntity employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "check_in_at")
    private LocalDateTime checkInAt;

    @Column(name = "check_out_at")
    private LocalDateTime checkOutAt;

    @Column(name = "check_in_lat", precision = 10, scale = 7)
    private BigDecimal checkInLat;

    @Column(name = "check_in_long", precision = 10, scale = 7)
    private BigDecimal checkInLong;

    @Column(name = "check_out_lat", precision = 10, scale = 7)
    private BigDecimal checkOutLat;

    @Column(name = "check_out_long", precision = 10, scale = 7)
    private BigDecimal checkOutLong;

    @Column(name = "work_hours", precision = 5, scale = 2)
    private BigDecimal workHours;

    @Column(name = "overtime_mins")
    private Integer overtimeMins;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // 퇴근 처리 시 필요한 값들을 한 번에 변경하는 메서드
    public void checkOut(
            LocalDateTime checkOutAt,
            BigDecimal checkOutLat,
            BigDecimal checkOutLong,
            BigDecimal workHours,
            Integer overtimeMins,
            AttendanceStatus status
    ) {
        // 이미 퇴근 시간이 있으면 중복 퇴근 방지
        if (this.checkOutAt != null) {
            throw new IllegalStateException("이미 퇴근 처리된 기록입니다.");
        }

        this.checkOutAt = checkOutAt;       // 퇴근 시각
        this.checkOutLat = checkOutLat;     // 퇴근 위치 위도
        this.checkOutLong = checkOutLong;   // 퇴근 위치 경도
        this.workHours = workHours;         // 총 근무 시간
        this.overtimeMins = overtimeMins;   // 연장근무 분
        this.status = status;               // 퇴근 상태
    }

    /**
     * 관리자 근태 수정 메서드
     * <p>
     * 관리자 화면에서 출근시간, 퇴근시간, 상태를 수정할 때 사용한다.
     * Entity의 필드를 직접 set하지 않고,
     * 의미 있는 메서드를 통해 변경하기 위해 만든다.
     */
    public void updateByAdmin(
            LocalDateTime checkInAt,
            LocalDateTime checkOutAt,
            AttendanceStatus status
    ) {
        this.checkInAt = checkInAt;     // 관리자 수정 출근 시간
        this.checkOutAt = checkOutAt;   // 관리자 수정 퇴근 시간
        this.status = status;           // 관리자 수정 근태 상태
    }


    /**
     * [결재-근태 연동용]: 승인 완료된 "근무 결과 신청" 문서의 값을 실제 근태 기록에 덮어씁니다.
     * <p>
     * 승인 완료된 전자결재 "근무 결과 신청" 내용을 근태 기록에 반영합니다.
     * <p>
     * 일반 출퇴근 버튼으로 생성된 기록과 달리, 결재 기반 정정은 이미 퇴근 시간이 있는 기록도
     * 승인된 값으로 보정할 수 있어야 하므로 checkOut()의 중복 퇴근 방지 로직과 분리했습니다.
     */
    public void applyApprovedWorkResult(
            LocalDateTime checkInAt,
            LocalDateTime checkOutAt,
            BigDecimal workHours,
            Integer overtimeMins,
            AttendanceStatus status,
            String note
    ) {
        this.checkInAt = checkInAt;
        this.checkOutAt = checkOutAt;
        this.workHours = workHours;
        this.overtimeMins = overtimeMins;
        this.status = status;
        this.note = note;
    }

    /**
     * [결재-근태 연동용]: 승인 완료된 "부재 일정" 문서의 요약 상태와 사유를 근태 기록에 남깁니다.
     * <p>
     * 승인 완료된 전자결재 "부재 일정" 내용을 근태 요약 상태에 반영합니다.
     * <p>
     * 현재 AttendanceStatus enum에 휴가/병가/경조사 전용 값이 없으므로, 상태는 기존 enum 안에서
     * 표현하고 실제 부재 유형과 사유는 note에 남겨 관리자 화면에서 맥락을 확인할 수 있게 합니다.
     */
    public void applyApprovedAbsence(
            AttendanceStatus status,
            String note
    ) {
        this.status = status;
        this.note = note;
    }
}
