package com.ict06.team1_fin_pj.domain.aiSecretary.entity;

import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "AI_KNOWLEDGE_REQUEST")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiKnowledgeRequestEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "knowledge_request_id")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_no", referencedColumnName = "emp_no", nullable = false)
    private EmpEntity requester;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "request_type", nullable = false, length = 50)
    private String requestType;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "target_dept", length = 100)
    private String targetDept;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "sample_question", nullable = false, columnDefinition = "TEXT")
    private String sampleQuestion;

    @Column(name = "reference_url", length = 500)
    private String referenceUrl;

    @Column(name = "access_level", length = 50)
    private String accessLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AiKnowledgeStatus status = AiKnowledgeStatus.PENDING;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_no", referencedColumnName = "emp_no")
    private EmpEntity reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_doc_id")
    private DocumentEntity targetDoc;

    public void updateReviewStatus(AiKnowledgeStatus status, String adminComment, EmpEntity reviewer) {
        this.status = status;
        this.adminComment = adminComment;
        this.reviewer = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    public void publishToDocument(DocumentEntity targetDoc) {
        this.status = AiKnowledgeStatus.PUBLISHED;
        this.targetDoc = targetDoc;
    }
}