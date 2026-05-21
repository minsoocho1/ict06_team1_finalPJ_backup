package com.ict06.team1_fin_pj.common.dto.approval;

import lombok.Builder;
import lombok.Getter;

/**
 * 전자결재 결재자 인감 이미지 응답 DTO입니다.
 *
 * 결재선 설정 화면에서 결재자를 선택하는 즉시 인감 이미지 존재 여부를 확인하고,
 * 향후 PDF 출력 시에도 같은 signImg 경로를 결재자 도장 이미지로 사용할 수 있게 합니다.
 */
@Getter
@Builder
public class ApprovalEmployeeSignResponseDto {

    // 결재자로 선택된 사원의 사번입니다.
    private String empNo;

    // 결재자로 선택된 사원의 이름입니다.
    private String name;

    // EmployeeImageWebConfig가 브라우저에 노출하는 인감 이미지 경로입니다.
    private String signImg;
}
