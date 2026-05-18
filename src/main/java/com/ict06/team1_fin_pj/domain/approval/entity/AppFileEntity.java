package com.ict06.team1_fin_pj.domain.approval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "APP_FILE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Integer fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_id")
    private ApprovalEntity approval;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 첨부파일이 어느 결재 문서에 속하는지 연결합니다.
     * 연관관계 관리는 ApprovalEntity.addFile()에서만 호출하는 흐름으로 사용합니다.
     */
    public void assignApproval(ApprovalEntity approval) {
        this.approval = approval;
    }
}
