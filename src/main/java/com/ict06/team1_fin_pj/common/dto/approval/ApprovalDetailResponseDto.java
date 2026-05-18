package com.ict06.team1_fin_pj.common.dto.approval;

import com.ict06.team1_fin_pj.domain.approval.entity.ApprovalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결재 문서 상세 조회 응답 DTO입니다.
 *
 * 상세 화면은 문서 기본 정보, 본문 JSON, 결재선, 첨부파일을 한 번에 필요로 하므로
 * 여러 Entity를 그대로 노출하지 않고 화면 전용 DTO로 묶어서 반환합니다.
 */
@Getter
@Builder
public class ApprovalDetailResponseDto {

    // 결재 문서 고유 ID입니다.
    private Integer approvalId;

    // 문서가 사용한 결재 양식 ID입니다.
    private Integer formId;

    // 문서가 사용한 결재 양식명입니다.
    private String formName;

    // 결재 문서 제목입니다.
    private String title;

    // 결재 문서 본문 JSON 문자열입니다. 양식별 필드가 달라질 수 있어 JSON으로 보관합니다.
    private String content;

    // 결재 문서 상태입니다. 예: DRAFT, IN_PROGRESS, COMPLETED, REJECTED
    private ApprovalStatus status;

    // 작성자 사번입니다.
    private String writerNo;

    // 작성자 이름입니다.
    private String writerName;

    // 현재 결재 단계입니다.
    private Integer currentStep;

    // 전체 결재 단계 수입니다.
    private Integer maxStep;

    // 현재 결재자 사번입니다. 임시저장/완료 상태에서는 null일 수 있습니다.
    private String currentApproverNo;

    // 현재 결재자 이름입니다. 임시저장/완료 상태에서는 null일 수 있습니다.
    private String currentApproverName;

    // 문서 생성 시각입니다.
    private LocalDateTime createdAt;

    // 문서 마지막 수정 시각입니다.
    private LocalDateTime updatedAt;

    // 결재자와 참조자를 모두 포함한 결재선 목록입니다.
    private List<ApprovalLineResponseDto> lines;

    // 문서에 첨부된 파일 목록입니다.
    private List<ApprovalFileResponseDto> files;

    // enum 이름 대신 화면에 보여줄 한글 상태명입니다.
    public String getStatusLabel() {
        return status != null ? status.getLabel() : null;
    }
}
