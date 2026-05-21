package com.ict06.team1_fin_pj.domain.attendance.entity;

import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "LEAVE_OCCURRENCE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveOccurrenceEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "occurrence_id")
    private Integer occurrenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_no", nullable = false)
    private EmpEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private LeaveTypeEntity leaveType;

    @Column(name = "target_year", nullable = false)
    private Integer targetYear;

    @Column(name = "occur_date", nullable = false)
    private LocalDate occurDate;

    @Column(name = "occur_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal occurDays;

    @Column(name = "used_days", precision = 4, scale = 1)
    private BigDecimal used_days;

    @Column(name = "remain_days", precision = 4, scale = 1)
    private BigDecimal remain_days;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * [결재-근태 연동용]: 승인 완료된 부재 일정이 연차 차감 대상일 때 사용 일수와 잔여 일수를 갱신합니다.
     *
     * 승인 완료된 부재 일정 결재에 따라 휴가 사용 일수를 차감합니다.
     *
     * LeaveOccurrence는 연차 발생/사용/잔여일을 함께 들고 있으므로, 외부에서 필드를 직접 바꾸지 않고
     * 이 메서드를 통해 잔여일 부족 여부를 검증한 뒤 used_days와 remain_days를 한 번에 갱신합니다.
     */
    public void useDays(BigDecimal days) {
        BigDecimal currentUsedDays = this.used_days == null ? BigDecimal.ZERO : this.used_days;
        BigDecimal currentRemainDays = this.remain_days == null ? BigDecimal.ZERO : this.remain_days;

        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("차감할 휴가 일수가 올바르지 않습니다.");
        }

        if (currentRemainDays.compareTo(days) < 0) {
            throw new IllegalStateException("잔여 휴가 일수가 부족합니다.");
        }

        this.used_days = currentUsedDays.add(days);
        this.remain_days = currentRemainDays.subtract(days);
    }
}
