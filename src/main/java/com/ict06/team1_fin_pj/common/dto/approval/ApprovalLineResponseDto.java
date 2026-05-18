package com.ict06.team1_fin_pj.common.dto.approval;

import com.ict06.team1_fin_pj.domain.approval.entity.ApprovalLineStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 결재 문서 상세 화면에서 결재선 한 줄을 표현하는 응답 DTO입니다.
 *
 * stepOrder가 0이면 결재 순서에 참여하지 않는 참조자이고,
 * 1 이상이면 실제 승인/반려 처리를 담당하는 결재자입니다.
 */
@Getter
@Builder
public class ApprovalLineResponseDto {

    // 결재선 고유 ID입니다.
    private Integer lineId;

    // 결재자 또는 참조자의 사번입니다.
    private String approverNo;

    // 결재자 또는 참조자의 이름입니다.
    private String approverName;

    // 결재 순서입니다. 0은 참조자, 1 이상은 실제 결재 단계입니다.
    private Integer stepOrder;

    // 결재선 처리 상태입니다. 참조자는 별도 처리 없이 WAITING 상태로 남을 수 있습니다.
    private ApprovalLineStatus status;

    // 승인/반려가 처리된 시각입니다. 아직 처리 전이면 null입니다.
    private LocalDateTime processedAt;

    // 화면에서 참조자 여부를 쉽게 판단할 수 있도록 제공하는 파생 값입니다.
    public boolean isReference() {
        return Integer.valueOf(0).equals(stepOrder);
    }

    // enum 이름 대신 화면에 보여줄 한글 상태명입니다.
    public String getStatusLabel() {
        return status != null ? status.getLabel() : null;
    }
}
