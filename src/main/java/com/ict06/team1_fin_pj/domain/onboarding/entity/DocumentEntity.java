/**
 * @FileName : DocumentEntity.java
 * @Description : 온보딩 문서/RAG 원천 문서 Entity
 * @Author : 김다솜
 * @Date : 2026. 05. 10
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.10    김다솜        관리자 문서/RAG 데이터 수정 처리를 위한 update 메서드 추가
 * @ 2026.05.18    김다솜        문서와 온보딩 콘텐츠 여러 건의 연결 매핑 추가
 */
package com.ict06.team1_fin_pj.domain.onboarding.entity;

import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.DepartmentEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "DOCUMENT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Integer docId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "summary_preview", columnDefinition = "TEXT")
    private String summaryPreview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_content_id")
    private OnContentEntity relatedContent;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "DOCUMENT_ON_CONTENT",
            joinColumns = @JoinColumn(name = "doc_id"),
            inverseJoinColumns = @JoinColumn(name = "content_id")
    )
    @Builder.Default
    private Set<OnContentEntity> relatedContents = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    private DepartmentEntity department;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 30)
    private AccessLevel accessLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", length = 20)
    @Builder.Default
    private DocumentStage currentStage = DocumentStage.UPLOADED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private EmpEntity createdBy;

    // 문서 → 청크들
    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocChunkEntity> chunks = new ArrayList<>();

    public void addChunk(DocChunkEntity chunk) {
        this.chunks.add(chunk);
        chunk.setDocument(this);
    }

    public void clearChunks() {
        this.chunks.clear();
    }

    public void updateStage(DocumentStage currentStage) {
        this.currentStage = currentStage;
    }

    public void updateSummaryPreview(String summaryPreview) {
        this.summaryPreview = summaryPreview;
    }

    public void updateDocument(
            String title,
            String filePath,
            Collection<OnContentEntity> relatedContents,
            DepartmentEntity department,
            AccessLevel accessLevel,
            DocumentStage currentStage
    ) {
        this.title = title;
        this.filePath = filePath;
        this.relatedContents.clear();
        if (relatedContents != null) {
            this.relatedContents.addAll(relatedContents);
        }
        this.relatedContent = this.relatedContents.stream().findFirst().orElse(null);
        this.department = department;
        this.accessLevel = accessLevel;
        this.currentStage = currentStage;
    }
}
