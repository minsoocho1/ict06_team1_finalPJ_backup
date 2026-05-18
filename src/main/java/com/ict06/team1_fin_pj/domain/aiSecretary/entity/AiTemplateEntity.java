package com.ict06.team1_fin_pj.domain.aiSecretary.entity;

import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "AI_TEMPLATE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiTemplateEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Integer templateId; // 추천 템플릿 식별자

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_request_id")
    private AiTemplateRequestEntity sourceRequest; // 원본 요청

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType type; // 문서 유형(REPORT / MINUTES / APPROVAL)

    @Column(length = 100)
    private String category; // 문서 카테고리

    @Column(length = 100)
    private String dept; // 관련 부서 또는 업무 영역

    @Column(length = 255)
    private String situation; // 사용 상황

    @Column(length = 50)
    private String tone; // 톤앤매너

    @Column(nullable = false, length = 255)
    private String title; // 템플릿 제목

    @Column(columnDefinition = "TEXT")
    private String description; // 템플릿 설명

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 템플릿 본문

    @JdbcTypeCode(SqlTypes.JSON) // *** 추가
    @Column(name = "preview_json", columnDefinition = "jsonb")
    private List<String> previewJson; // 미리보기 목록

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", columnDefinition = "jsonb")
    private Map<String, Object> optionsJson; // 템플릿 생성 옵션

    @Builder.Default
    @Column(name = "is_active", nullable = false) // *** name = "is_active" 추가
    private Boolean isActive = true; // 노출 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private EmpEntity createdBy; // 등록자 또는 승인 관리자

    // *** 노출 상태로 변경
    public void activate() {
        this.isActive = true;
    }

    // *** 미노출 상태로 변경
    public void deactivate() {
        this.isActive = false;
    }
}