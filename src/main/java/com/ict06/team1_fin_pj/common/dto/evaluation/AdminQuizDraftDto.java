/**
 * @FileName : AdminQuizDraftDto.java
 * @Description : 관리자 AI 퀴즈 자동 생성 초안 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 13
 * @Modification_History
 * @
 * @ 수정일자        수정자         수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.11    김다솜         생성된 객관식 문제 초안 표시 DTO 추가
 * @ 2026.05.13    김다솜         주관식 문제 유형, 모범답안, 키워드답안, 루브릭 필드 확장
 */
package com.ict06.team1_fin_pj.common.dto.evaluation;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminQuizDraftDto {

    private String questionType;
    private String questionText;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private Integer answerNo;
    private String sampleAnswer;
    private List<String> keywordAnswer;
    private String rubric;
    private Integer score;
    private String explanation;
}
