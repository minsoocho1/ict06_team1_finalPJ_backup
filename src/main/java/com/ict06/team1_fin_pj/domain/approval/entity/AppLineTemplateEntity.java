package com.ict06.team1_fin_pj.domain.approval.entity;

import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "APP_LINE_TEMPLATE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppLineTemplateEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Integer templateId;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private EmpEntity createdBy;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AppLineTemplateDetailEntity> details = new ArrayList<>();

    /**
     * 결재선 서식에 상세 결재 조건을 추가합니다.
     * 양방향 연관관계를 한 곳에서 맞춰 템플릿과 상세 항목의 연결이 어긋나지 않게 합니다.
     */
    public void addDetail(AppLineTemplateDetailEntity detail) {
        this.details.add(detail);
        detail.assignTemplate(this);
    }

    /**
     * 결재선 서식의 이름과 기본 결재선 여부를 수정합니다.
     */
    public void updateNameIsDefault(String templateName, Boolean isDefault) {
        this.templateName = templateName;
        this.isDefault = isDefault;
    }
}