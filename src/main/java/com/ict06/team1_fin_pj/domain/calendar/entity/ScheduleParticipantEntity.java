package com.ict06.team1_fin_pj.domain.calendar.entity;

import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "SCH_PARTICIPANT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sch_parti_id")
    private Integer schPartiId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ScheduleEntity schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_no", nullable = false)
    private EmpEntity employee;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ParticipantStatus status = ParticipantStatus.PENDING;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    // 참석자가 일정 초대에 응답하면 상태와 응답 시각을 함께 갱신한다.
    public void updateStatus(ParticipantStatus status) {
        this.status = status;
        this.respondedAt = LocalDateTime.now();
    }
}