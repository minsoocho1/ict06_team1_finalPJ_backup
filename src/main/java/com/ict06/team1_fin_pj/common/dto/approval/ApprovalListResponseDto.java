package com.ict06.team1_fin_pj.common.dto.approval;

import com.ict06.team1_fin_pj.domain.approval.entity.ApprovalStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ApprovalListResponseDto {

    // 결재 문서 고유 ID입니다. 상세 페이지 이동 시 사용합니다.
    private Integer approvalId;

    // 문서가 사용한 결재 양식 ID입니다.
    private Integer formId;

    // 문서가 사용한 결재 양식명입니다. 예: 연차 신청서, 연장근무 신청서
    private String formName;

    // 문서 작성자 사번입니다. 참조 문서함에서 누가 올린 문서인지 보여줄 때 사용합니다.
    private String writerNo;

    // 문서 작성자 이름입니다. 참조 문서함 목록에서 작성자를 표시할 수 있습니다.
    private String writerName;

    // 결재 문서 제목입니다.
    private String title;

    // 결재 문서 상태 enum입니다. 화면에서는 statusLabel을 함께 사용할 수 있습니다.
    private ApprovalStatus status;

    // 현재 결재 단계입니다.
    private Integer currentStep;

    // 전체 결재 단계 수입니다.
    private Integer maxStep;

    // 현재 결재자 사번입니다. 완료/반려/임시저장 상태에서는 null일 수 있습니다.
    private String currentApproverNo;

    // 현재 결재자 이름입니다. 목록 화면에서 "현재 결재자"로 보여줄 수 있습니다.
    private String currentApproverName;

    // 문서 최초 생성 시각입니다.
    private LocalDateTime createdAt;

    // 문서 마지막 수정 시각입니다. 목록 정렬 기준으로 사용합니다.
    private LocalDateTime updatedAt;

    /**
     * enum 이름(DRAFT, IN_PROGRESS 등) 대신 화면에 보여줄 한글 상태명입니다.
     * DTO 필드로 따로 저장하지 않고 getter로 제공해, enum label 변경 시 자동 반영되게 했습니다.
     */
    public String getStatusLabel() {
        return status != null ? status.getLabel() : null;
    }
}
