/**
 * @FileName : AdDocumentRequestDto.java
 * @Description : 관리자 온보딩 문서/RAG 데이터 등록 및 수정 요청 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 10
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.10    김다솜        최초 생성 및 문서 메타데이터/처리 단계 요청값 추가
 */
package com.ict06.team1_fin_pj.common.dto.onboarding;

import com.ict06.team1_fin_pj.domain.onboarding.entity.AccessLevel;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentStage;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class AdDocumentRequestDto {

    @NotBlank(message = "문서 제목은 필수 입력 항목입니다.")
    private String title;

    @NotBlank(message = "파일 경로는 필수 입력 항목입니다.")
    private String filePath;

    @NotNull(message = "부서 ID는 필수입니다.")
    private Integer deptId;

    @NotNull(message = "접근 권한 설정이 필요합니다.")
    private AccessLevel accessLevel;

    private DocumentStage currentStage;
}
