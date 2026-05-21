/**
 * @FileName : AdEvaluationService.java
 * @Description : 관리자 온보딩 평가 관리 서비스 인터페이스
 * @Author : 김다솜
 * @Date : 2026. 05. 13
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.13    김다솜        AI 퀴즈 초안 생성 및 문서 연계 자동 생성 인터페이스 정리
 * @ 2026.05.14    김다솜        평가 점수 통계 및 이해도 시각화용 집계 인터페이스 추가
 * @ 2026.05.15    김다솜        카테고리별 평가 기준 설정 인터페이스 추가
 * @ 2026.05.18    김다솜        AI 리포트용 LLM 이탈 위험 분석 인터페이스 추가, 깨진 주석 복구
 */
package com.ict06.team1_fin_pj.domain.evaluation.service;

import com.ict06.team1_fin_pj.common.dto.evaluation.AdminEvaluationAnalyticsDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminQuizGenerationRequestDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminQuizSaveRequestDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminRetentionRiskDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AiQuizGenerationResponseDto;

import java.util.List;

public interface AdEvaluationService {

    // AI 퀴즈 초안 생성
    AiQuizGenerationResponseDto generateQuizDrafts(AdminQuizGenerationRequestDto requestDto);

    // 생성된 퀴즈 초안 저장
    int saveGeneratedQuestions(AdminQuizSaveRequestDto requestDto);

    // 콘텐츠와 연결된 문서를 기준으로 평가 문제 자동 생성 및 저장
    int generateAndSaveQuestionsForContent(Integer contentId);

    // 관리자 평가 통계 조회
    AdminEvaluationAnalyticsDto getEvaluationAnalytics();

    // 카테고리별 평가 기준 수정
    int updateCategoryCriteria(String categoryName, Integer passScore, Double weight);

    // 평가 통계 기반 AI 분석 코멘트 생성
    String getAiEvaluationAnalysis(Object stats);

    // 이탈 위험 직원 목록 기반 LLM 분석 코멘트 생성
    String getAiRetentionRiskAnalysis(List<AdminRetentionRiskDto> retentionRiskStats);
}
