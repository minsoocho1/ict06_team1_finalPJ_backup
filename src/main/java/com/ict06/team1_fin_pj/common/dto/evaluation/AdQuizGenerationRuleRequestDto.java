/**
 * @FileName : AdQuizGenerationRuleRequestDto.java
 * @Description : 관리자 AI 퀴즈 출제 기준 등록/수정 요청 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 11
 * @Modification_History
 *
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.10    김다솜        최초 생성 및 퀴즈 출제 기준 요청값 추가
 * @ 2026.05.14    김다솜        가중치 설정 요청값 추가
 */
package com.ict06.team1_fin_pj.common.dto.evaluation;

import com.ict06.team1_fin_pj.domain.evaluation.entity.QuestionType;
import com.ict06.team1_fin_pj.domain.onboarding.entity.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdQuizGenerationRuleRequestDto {

    @NotBlank(message = "카테고리명은 필수입니다.")
    private String categoryName;

    @NotNull(message = "문항 수는 필수입니다.")
    @Min(value = 1, message = "문항 수는 최소 1개 이상이어야 합니다.")
    private Integer questionCount;

    @NotNull(message = "합격 점수는 필수입니다.")
    @Min(value = 0, message = "합격 점수는 0 이상이어야 합니다.")
    @Max(value = 100, message = "합격 점수는 100 이하여야 합니다.")
    private Integer passScore;

    @NotNull(message = "가중치는 필수입니다.")
    @Min(value = 10, message = "가중치는 10% 이상이어야 합니다.")
    @Max(value = 300, message = "가중치는 300% 이하여야 합니다.")
    private Integer weightPercent = 100;

    @NotNull(message = "난이도를 선택해주세요.")
    private Difficulty difficulty;

    @NotNull(message = "문항 유형을 선택해주세요.")
    private QuestionType questionType;

    private Boolean isActive = false;
}
