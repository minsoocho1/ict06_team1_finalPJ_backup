/**
 * @FileName : LearningSelfCheckEntity.java
 * @Description : 학습자별 콘텐츠 이해도 자기 평가 엔티티
 * @Date : 2026. 05. 18
 * @Modification_History
 * @
 * @ 수정일자        수정내용
 * @ ----------    -----------------------------------------------
 * @ 2026.05.18    학습 이해도 자기 평가 저장 기능 추가
 */
package com.ict06.team1_fin_pj.domain.onboarding.entity;

import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "LEARNING_SELF_CHECK",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_learning_self_check_emp_content",
                        columnNames = {"emp_no", "content_id"}
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningSelfCheckEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "self_check_id")
    private Integer selfCheckId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_no", nullable = false)
    private EmpEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private OnContentEntity content;

    @Column(name = "understanding_score", nullable = false)
    private Integer understandingScore;

    @Column(name = "confidence_score", nullable = false)
    private Integer confidenceScore;

    @Column(name = "need_more_explanation")
    private Boolean needMoreExplanation;

    @Column(name = "memo", length = 1000)
    private String memo;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    public void update(
            Integer understandingScore,
            Integer confidenceScore,
            Boolean needMoreExplanation,
            String memo
    ) {
        this.understandingScore = understandingScore;
        this.confidenceScore = confidenceScore;
        this.needMoreExplanation = needMoreExplanation;
        this.memo = memo;
        this.checkedAt = LocalDateTime.now();
    }
}
