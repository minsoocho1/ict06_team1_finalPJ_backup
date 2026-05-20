/**
 * @FileName : EvaluationDetailResponse.java
 * @Description : 온보딩 퀴즈 결과 상세 조회 응답 DTO
 *                - 특정 평가의 문항별 내 답변, 정답, 채점 결과 정보를 포함
 * @Author : 김다솜
 * @Date : 2026. 05. 06
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.06    김다솜        최초 생성
 */

/*
 * 2026-05-19
 * 평가 상세 화면에서 자기평가 AI 비교 피드백을 함께 반환하도록 필드 추가
 */
package com.ict06.team1_fin_pj.common.dto.evaluation;

import com.ict06.team1_fin_pj.common.dto.onboarding.LearningSelfCheckResponseDto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class EvaluationDetailResponse {
    private String categoryName;
    private String empNo;

    private Integer totalScore;
    private Integer maxScore;
    private Boolean passed;
    private LearningSelfCheckResponseDto selfCheckFeedback;

    private List<QuestionDetail> questions;

    @Getter
    @Builder
    public static class QuestionDetail {
        private Integer questionId;
        private String questionText;
        private String userAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        private Integer score;
        private BigDecimal aiScore;
        private String aiFeedback;
        private BigDecimal similarityScore;
    }
}
