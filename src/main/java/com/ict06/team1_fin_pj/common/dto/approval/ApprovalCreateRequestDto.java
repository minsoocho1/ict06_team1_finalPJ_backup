package com.ict06.team1_fin_pj.common.dto.approval;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 결재 문서 임시저장/상신 요청 DTO입니다.
 *
 * React 작성 화면에서 선택한 결재 서식, 입력한 본문 JSON, 결재선을 서버로 전달할 때 사용합니다.
 * 파일 첨부가 있는 경우에도 multipart의 request 파트에 이 DTO가 JSON 형태로 들어갑니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ApprovalCreateRequestDto {

    // 사용자가 선택한 결재 서식 ID입니다. 예: 근무 계획 신청, 부재 일정, 물품구입확인서 등
    private Integer formId;

    // 결재 문서 제목입니다. 목록 화면과 상세 화면에서 대표 제목으로 사용합니다.
    private String title;

    // 결재 문서 본문입니다. 서식별 입력 항목이 다르므로 JSON 문자열로 저장합니다.
    private String content;

    // 사용자가 직접 지정한 결재선입니다. 상신 시에는 참조자를 제외한 실제 결재자가 최소 1명 필요합니다.
    private List<ApprovalLineRequestDto> approvalLines;
}
