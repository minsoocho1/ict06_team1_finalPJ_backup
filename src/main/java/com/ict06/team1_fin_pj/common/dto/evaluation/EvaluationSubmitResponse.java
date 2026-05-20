/**
 * @FileName : QuizSubmitResponse.java
 * @Description : 온보딩 퀴즈 제출 결과 응답 DTO
 *                - 카테고리별 퀴즈 제출 후 총점, 만점, 통과 여부, 문항별 결과를 반환
 *                - 여러 문항을 한 번에 제출한 결과 응답에 사용
 * @Author : 김다솜
 * @Date : 2026. 04. 30
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.30    김다솜        최초 생성 및 퀴즈 제출 결과 응답 DTO 구현
 * @ 2026.05.01    김다솜        카테고리별 일괄 제출 결과 구조로 수정
 */

/*
 * 2026-05-19
 * 평가 제출 직후 자기평가 AI 비교 피드백을 함께 반환하도록 필드 추가
 */
package com.ict06.team1_fin_pj.common.dto.evaluation;

import com.ict06.team1_fin_pj.common.dto.onboarding.LearningSelfCheckResponseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EvaluationSubmitResponse {

    private String empNo;
    private String categoryName;

    private Integer totalScore;
    private Integer maxScore;
    private Boolean passed;
    private LearningSelfCheckResponseDto selfCheckFeedback;

    private List<EvaluationAnswerResult> results;
}
