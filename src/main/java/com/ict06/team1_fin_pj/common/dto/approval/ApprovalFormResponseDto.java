package com.ict06.team1_fin_pj.common.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * React 전자결재 작성 화면에서 사용하는 결재 서식 응답 DTO입니다.
 *
 * 관리자가 만든 APP_FORM 데이터를 직원 화면에 그대로 노출하지 않고,
 * 화면에 필요한 값만 선별해서 전달하기 위해 별도 DTO로 분리했습니다.
 * template에는 입력 필드 정의 JSON이 들어가며, select 타입 필드의 options도 이 JSON 안에 포함됩니다.
 */
@Getter
@Builder
@AllArgsConstructor
public class ApprovalFormResponseDto {

    // 결재 문서 작성/임시저장/상신 요청에서 다시 전달할 결재 서식 PK입니다.
    private Integer formId;

    // 사용자에게 노출되는 결재 서식명입니다. 예: 근무 계획 신청, 부재 일정
    private String formName;

    // 동적 입력 폼을 렌더링하기 위한 JSON 문자열입니다.
    private String template;

    // 제조사가 제공하는 기본 서식 여부입니다. 직원 화면에서는 배지나 안내 문구에 활용할 수 있습니다.
    private Boolean isDefault;

    // 해당 서식에 기본 연결된 결재선 서식 ID입니다. 연결되지 않은 경우 null입니다.
    private Integer lineTemplateId;

    // 해당 서식에 기본 연결된 결재선 서식명입니다. 연결되지 않은 경우 null입니다.
    private String lineTemplateName;
}
