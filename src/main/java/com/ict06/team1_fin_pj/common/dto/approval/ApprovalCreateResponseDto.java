package com.ict06.team1_fin_pj.common.dto.approval;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApprovalCreateResponseDto {

    // 저장 또는 상신 후 생성된 결재 문서 ID입니다.
    private Integer approvalId;

    // 저장된 결재 문서 상태입니다. 예: DRAFT, IN_PROGRESS
    private String status;

    // 현재 진행 중인 결재 단계입니다. 임시저장은 0, 상신 직후는 1입니다.
    private Integer currentStep;

    // 결재선에서 가장 큰 단계 번호입니다. 진행률 표시나 완료 판단에 사용할 수 있습니다.
    private Integer maxStep;

    // 현재 결재자 사번입니다. 임시저장 상태에서는 null입니다.
    private String currentApproverNo;

    // 현재 결재자 이름입니다. 임시저장 상태에서는 null입니다.
    private String currentApproverName;
}
