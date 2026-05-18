package com.ict06.team1_fin_pj.test.entity;

import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
import com.ict06.team1_fin_pj.domain.approval.entity.AppLineTemplateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
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

/**
 * 전자결재 샘플 화면용 엔티티입니다.
 * 실제 운영 코드는 AppFormEntity를 사용하고, 이 클래스는 MVC/QueryDSL 예제 확인용으로만 사용합니다.
 */
@Entity
@Table(name = "APP_FORM")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "form_id")
    private Integer formId;

    @Column(name = "form_name", nullable = false, length = 100)
    private String formName;

    @Column(columnDefinition = "TEXT")
    private String template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "line_template_id",
            foreignKey = @ForeignKey(name = "fk_app_form_line_template")
    )
    private AppLineTemplateEntity lineTemplate;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
}
