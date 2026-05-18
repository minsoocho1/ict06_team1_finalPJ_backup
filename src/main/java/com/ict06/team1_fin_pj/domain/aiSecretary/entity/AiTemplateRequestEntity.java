package com.ict06.team1_fin_pj.domain.aiSecretary.entity;

import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
import com.ict06.team1_fin_pj.domain.approval.entity.RequestStatus;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "AI_TEMPLATE_REQUEST")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiTemplateRequestEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId; // 템플릿 추가 요청 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_no", nullable = false)
    private EmpEntity employee; // 요청한 사원

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
    private String title; // 요청 템플릿 제목

    @Column(columnDefinition = "TEXT")
    private String description; // 템플릿 설명

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 템플릿 본문

    @JdbcTypeCode(SqlTypes.JSON) // *** 추가
    @Column(name = "preview_json", columnDefinition = "jsonb")
    private List<String> previewJson; // 템플릿 미리보기 목록

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", columnDefinition = "jsonb")
    private Map<String, Object> optionsJson; // 템플릿 생성 옵션

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20)
    private RequestStatus status = RequestStatus.PENDING; // 요청 상태

    @Column(name = "admin_comment", columnDefinition = "TEXT") // *** 수정
    private String adminComment; // 관리자 검토 메모

    @Column(name = "reviewed_at") // *** 추가
    private LocalDateTime reviewedAt; // 관리자 검토 일시

    // 관리자 승인/반려/취소 처리
    public void updateStatus(RequestStatus status, String comment) {
        this.status = status;
        this.adminComment = comment;
        this.reviewedAt = LocalDateTime.now();
    }
}