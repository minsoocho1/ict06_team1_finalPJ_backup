package com.ict06.team1_fin_pj.domain.onboarding.entity;

import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CHECKLIST")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer checklistId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_content_id")
    private OnContentEntity relatedContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "checklist_type", length = 20, nullable = false)
    private ChecklistType checklistType;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    public void updateRelatedContent(OnContentEntity relatedContent) {
        this.relatedContent = relatedContent;
    }
}
