package com.ict06.team1_fin_pj.domain.approval.entity;

import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "APP_LINE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Integer lineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_id", nullable = false)
    private ApprovalEntity approval;

    /*
     * APP_LINE 테이블의 결재자 FK 컬럼명은 approver_id입니다.
     * 실제 값은 사번(emp_no)을 참조하지만, DB 설계 컬럼명과 엔티티 매핑명이 다르면
     * Hibernate가 다른 컬럼에 값을 넣어 NOT NULL 제약조건 오류가 발생할 수 있습니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private EmpEntity approver;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    private ApprovalLineStatus status;

    private LocalDateTime processedAt;

    /**
     * 결재 문서와 결재선의 양방향 연관관계를 맞추기 위한 메서드입니다.
     * 외부에서 임의로 approval을 바꾸지 않도록 Setter 대신 목적이 분명한 메서드로 제한합니다.
     */
    public void assignApproval(ApprovalEntity approval) {
        this.approval = approval;
    }

    /**
     * 현재 결재선을 승인 처리합니다.
     * 처리 시각을 함께 남겨 상세 화면에서 결재 이력을 보여줄 수 있게 합니다.
     */
    public void approve() {
        this.status = ApprovalLineStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 현재 결재선을 반려 처리합니다.
     * 반려도 결재자의 처리 행위이므로 처리 시각을 함께 저장합니다.
     */
    public void reject() {
        this.status = ApprovalLineStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
    }
}
