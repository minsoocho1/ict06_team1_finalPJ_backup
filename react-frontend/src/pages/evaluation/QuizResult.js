/**
 * @FileName : QuizResult.js
 * @Description : AI 온보딩 평가 응시 결과 컴포넌트
 * @Author : 김다솜
 * @Date : 2026. 05. 06
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.06    김다솜       최초 생성, Quiz.js에서 평가 응시 결과 컴포넌트 분리 
 * @ 2026.05.15    김다솜       UI 조정(AI 사내 포털 기준으로 톤 맞춤)
 * @ 2026.05.19    김다솜       평가 완료 직후 자기평가 AI 비교 피드백 노출 및 학습 콘텐츠 이동 연결
 * @ 2026.05.19    김다솜       AI 피드백 줄바꿈 렌더링 보완
 */
import React from 'react';
import { useNavigate } from 'react-router-dom';

import { PATH } from 'src/constants/path';
import { failText, passText, submitButton, summaryBox, summaryTitle } from 'src/styles/js/evaluation/QuizStyle';

const QuizResult = ({ submitResult, onRetry }) => {
  const navigate = useNavigate();
  const selfCheckFeedback = submitResult.selfCheckFeedback;
  const hasSelfCheckFeedback = selfCheckFeedback?.submitted && selfCheckFeedback?.feedback;

  return (
    <div style={summaryBox}>
      <div style={summaryTitle}>평가 결과</div>
      <div>총점: {submitResult.totalScore} / {submitResult.maxScore}</div>

      <div style={submitResult.passed ? passText : failText}>
        결과: {submitResult.passed ? '✅ 통과' : '❌ 미통과'}
      </div>

      {hasSelfCheckFeedback && (
        <div
          style={{
            marginTop: '18px',
            padding: '16px',
            borderRadius: '18px',
            background: '#F5F7FF',
            border: '1px solid #DDE6FF',
            textAlign: 'left',
            color: '#1F2937',
          }}
        >
          <div style={{ fontWeight: 800, marginBottom: '8px', color: '#1D4ED8' }}>
            자기평가 기반 AI 피드백
          </div>
          <div
            style={{
              lineHeight: 1.7,
              fontSize: '0.95rem',
              whiteSpace: 'pre-line',
              wordBreak: 'keep-all',
            }}
          >
            {selfCheckFeedback.feedback}
          </div>
          <div className="d-flex flex-wrap gap-2 mt-3 small text-muted">
            {selfCheckFeedback.selfScoreRate != null && <span>자기평가 {selfCheckFeedback.selfScoreRate}%</span>}
            {selfCheckFeedback.evaluationScoreRate != null && <span>AI 평가 {selfCheckFeedback.evaluationScoreRate}%</span>}
            {selfCheckFeedback.scoreGap != null && (
              <span>차이 {selfCheckFeedback.scoreGap > 0 ? '+' : ''}{selfCheckFeedback.scoreGap}%</span>
            )}
          </div>
          {selfCheckFeedback.contentId && (
            <button
              type="button"
              style={{
                ...submitButton,
                marginTop: '12px',
                padding: '9px 14px',
                background: '#1D4ED8',
              }}
              onClick={() => navigate(PATH.ONBOARDING.LEARNING(selfCheckFeedback.contentId))}
            >
              학습 콘텐츠에서 다시 확인
            </button>
          )}
        </div>
      )}

      <div className="d-flex flex-wrap gap-2 justify-content-center mt-3">
        {!submitResult.passed && (
          <button
            style={submitButton}
            onClick={onRetry}
          >
            다시 응시하기
          </button>
        )}

        {submitResult.passed && (
          <button
            style={submitButton}
            onClick={() => navigate(PATH.ONBOARDING.ROADMAP)}
          >
            온보딩 로드맵으로
          </button>
        )}

        <button
          style={{
            ...submitButton,
            background: '#fff',
            color: '#2563EB',
            border: '1px solid #2563EB',
          }}
          onClick={() => navigate(PATH.EVALUATION.RESULT)}
        >
          평가 결과 보기
        </button>
      </div>
    </div>
  );
};

export default QuizResult;
