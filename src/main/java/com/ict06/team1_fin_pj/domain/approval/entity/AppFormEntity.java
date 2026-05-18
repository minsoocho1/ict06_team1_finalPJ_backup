package com.ict06.team1_fin_pj.domain.approval.entity;

import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
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

@Entity
@Table(name = "APP_FORM")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppFormEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "form_id")
    private Integer formId;

    @Column(name = "form_name", nullable = false, length = 100)
    private String formName;

    @Column(columnDefinition = "TEXT")
    private String template;

    /**
     * 결재 서식에서 기본으로 사용할 결재선 서식입니다.
     * 하나의 결재선 서식을 여러 결재 서식에서 공유할 수 있도록 APP_FORM이 FK를 가집니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "line_template_id",
            foreignKey = @ForeignKey(name = "fk_app_form_line_template")
    )
    private AppLineTemplateEntity lineTemplate;

    /**
     * 제조사가 제공하는 기본 결재 서식 여부입니다.
     * 관리자 화면에서 생성되는 고객사 서식은 항상 false이며, true 값은 초기 SQL로만 지정합니다.
     */
    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    /**
     * 고객사 관리자가 만든 결재 서식의 이름과 본문을 수정합니다.
     * 기본 결재 서식은 제조사 제공 서식이므로 수정할 수 없습니다.
     */
    public void updateForm(String formName, String template) {
        if (isDefaultForm()) {
            throw new IllegalStateException("기본 결재 서식은 수정할 수 없습니다.");
        }

        this.formName = formName;
        this.template = template;
    }

    /**
     * 결재 서식에 연결할 결재선 서식을 변경합니다.
     * null을 전달하면 결재선 서식이 연결되지 않은 상태로 되돌립니다.
     */
    public void updateLineTemplate(AppLineTemplateEntity lineTemplate) {
        this.lineTemplate = lineTemplate;
    }

    /**
     * Boolean null 값과 무관하게 기본 서식 여부를 안전하게 판단합니다.
     */
    public boolean isDefaultForm() {
        return Boolean.TRUE.equals(this.isDefault);
    }
}
