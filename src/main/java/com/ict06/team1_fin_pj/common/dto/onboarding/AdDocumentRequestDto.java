/**
 * @FileName : AdDocumentRequestDto.java
 * @Description : Admin onboarding document/RAG create and update request DTO.
 * @Author : 김다솜
 * @Date : 2026. 05. 10
 * @Modification_History
 * @
 * @ 수정일자       수정자        수정내용
 * @ ----------    ---------    -----------------------------------------------
 * @ 2026.05.10    김다솜        최초 생성
 * @ 2026.05.18                 관련 콘텐츠 선택 필드 추가
 * @ 2026.05.18                 다중 관련 콘텐츠 선택 필드 추가
 */
package com.ict06.team1_fin_pj.common.dto.onboarding;

import com.ict06.team1_fin_pj.domain.onboarding.entity.AccessLevel;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentStage;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AdDocumentRequestDto {

    @NotBlank(message = "문서 제목은 필수 입력 항목입니다.")
    private String title;

    @NotBlank(message = "파일 경로는 필수 입력 항목입니다.")
    private String filePath;

    private Integer contentId;

    private List<Integer> contentIds = new ArrayList<>();

    private Integer deptId;

    private AccessLevel accessLevel;

    private DocumentStage currentStage;
}
