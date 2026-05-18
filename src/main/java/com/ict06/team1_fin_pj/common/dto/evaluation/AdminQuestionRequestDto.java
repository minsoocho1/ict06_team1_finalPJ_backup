/**
 * @FileName : AdminQuestionRequestDto.java
 * @Description : 관리자 평가 문제 수정 요청 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 13
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.13    김다솜        평가 문제 수정/삭제 화면 연동용 요청 DTO 추가
 */
package com.ict06.team1_fin_pj.common.dto.evaluation;

import com.ict06.team1_fin_pj.domain.evaluation.entity.QuestionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminQuestionRequestDto {

    private Integer contentId;
    private String categoryName;
    private QuestionType questionType;
    private String questionText;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private Integer answerNo;
    private String sampleAnswer;
    private String keywordAnswerText;
    private String rubric;
    private Integer score;
    private String explanation;
}
