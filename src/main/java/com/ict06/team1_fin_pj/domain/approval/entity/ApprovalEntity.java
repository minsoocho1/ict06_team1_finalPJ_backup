package com.ict06.team1_fin_pj.domain.approval.entity;

import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "APPROVAL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Integer approvalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id")
    private AppFormEntity form;

    @Column(length = 200)
    private String title;

    /*
     * DB의 approval.content 컬럼은 jsonb 타입입니다.
     * String 필드만 선언하면 Hibernate가 일반 varchar로 바인딩하므로,
     * PostgreSQL jsonb 컬럼에 그대로 저장될 수 있도록 JSON 타입 코드를 명시합니다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_no", nullable = false)
    private EmpEntity writer;

    @Column(name = "current_step")
    private Integer currentStep;

    @Column(name = "max_step")
    private Integer maxStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_approver_no")
    private EmpEntity currentApprover;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AppLineEntity> lines = new ArrayList<>();

    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AppFileEntity> files = new ArrayList<>();

    /**
     * 결재 문서에 실제 결재자 또는 참조자 한 명을 추가합니다.
     * ApprovalEntity가 연관관계의 주인은 아니지만, 자식 엔티티에 부모를 지정해
     * 양방향 연관관계를 일관되게 유지합니다.
     */
    public void addLine(AppLineEntity line) {
        this.lines.add(line);
        line.assignApproval(this);
    }

    /**
     * 임시저장 문서의 결재선을 새 결재선으로 교체합니다.
     * 기존 결재선은 orphanRemoval=true 설정에 따라 삭제 대상이 됩니다.
     */
    public void replaceLines(List<AppLineEntity> newLines) {
        this.lines.clear();
        newLines.forEach(this::addLine);
        this.maxStep = newLines.stream()
                .map(AppLineEntity::getStepOrder)
                .filter(step -> step != null && step > 0)
                .max(Integer::compareTo)
                .orElse(0);
    }

    /**
     * 결재 문서에 첨부 파일을 추가합니다.
     * 파일 엔티티가 approval_id를 가진 주인 쪽이므로, 부모 문서 참조도 함께 세팅합니다.
     */
    public void addFile(AppFileEntity file) {
        this.files.add(file);
        file.assignApproval(this);
    }

    /**
     * 문서를 아직 상신하지 않은 임시저장 상태로 전환합니다.
     * 현재 결재자는 존재하지 않으므로 currentApprover를 비워 둡니다.
     */
    public void saveAsDraft() {
        this.status = ApprovalStatus.DRAFT;
        this.currentStep = 0;
        this.currentApprover = null;
    }

    /**
     * 임시저장 문서의 기본 내용을 수정합니다.
     * 작성자와 상태값은 서비스 계층에서 권한/상태 검증을 거친 뒤 변경합니다.
     */
    public void updateDraftContent(AppFormEntity form, String title, String content) {
        this.form = form;
        this.title = title;
        this.content = content;
        saveAsDraft();
    }

    /**
     * 문서를 상신 상태로 전환하고 첫 번째 결재자를 현재 결재자로 지정합니다.
     * 이후 승인/반려 처리에서 currentStep과 currentApprover를 다음 단계로 이동시킵니다.
     */
    public void submit(EmpEntity firstApprover, Integer maxStep) {
        this.status = ApprovalStatus.IN_PROGRESS;
        this.currentStep = 1;
        this.currentApprover = firstApprover;
        this.maxStep = maxStep;
    }

    /**
     * 다음 결재 단계로 문서를 이동합니다.
     * 현재 결재자의 승인 처리가 끝난 뒤 아직 남은 결재자가 있을 때 호출합니다.
     */
    public void moveToNextApprover(EmpEntity nextApprover, Integer nextStep) {
        this.currentApprover = nextApprover;
        this.currentStep = nextStep;
    }

    /**
     * 모든 결재 단계가 승인되면 문서를 완료 상태로 전환합니다.
     * 완료 문서에는 더 이상 현재 결재자가 없으므로 currentApprover를 비웁니다.
     */
    public void complete() {
        this.status = ApprovalStatus.COMPLETED;
        this.currentApprover = null;
        this.currentStep = this.maxStep;
    }

    /**
     * 결재자가 반려한 문서를 반려 상태로 전환합니다.
     * 반려 이후에는 결재 흐름이 종료되므로 currentApprover를 비웁니다.
     */
    public void reject() {
        this.status = ApprovalStatus.REJECTED;
        this.currentApprover = null;
    }

    /**
     * 상신자가 완료 전 진행 중 문서를 취소 상태로 전환합니다.
     * 취소 문서는 이력으로 남기지만 더 이상 현재 결재자가 없도록 currentApprover를 비웁니다.
     */
    public void cancel() {
        this.status = ApprovalStatus.CANCELED;
        this.currentApprover = null;
    }
}
