package com.ict06.team1_fin_pj.common.dto.approval;

import lombok.Builder;
import lombok.Getter;

/**
 * 결재 문서 상세 화면에서 첨부파일 목록을 보여주기 위한 응답 DTO입니다.
 *
 * 파일 다운로드/미리보기는 filePath를 기준으로 처리하고,
 * 화면 표시용 이름은 사용자가 업로드했던 원본 fileName을 사용합니다.
 */
@Getter
@Builder
public class ApprovalFileResponseDto {

    // 첨부파일 고유 ID입니다.
    private Integer fileId;

    // 사용자가 업로드한 원본 파일명입니다.
    private String fileName;

    // 브라우저에서 접근할 수 있는 정적 리소스 경로입니다.
    private String filePath;

    // 파일 크기(byte)입니다. 화면에서 KB/MB 단위로 변환해 표시할 수 있습니다.
    private Long fileSize;
}
