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
 */
import React from 'react';
import { useNavigate } from 'react-router-dom';

import { PATH } from 'src/constants/path';
import { failText, passText, submitButton, summaryBox, summaryTitle } from 'src/styles/js/evaluation/QuizStyle';

const QuizResult = ({ submitResult, onRetry }) => {
  const navigate = useNavigate();

  return (
    <div style={summaryBox}>
      <div style={summaryTitle}>평가 결과</div>
      <div>총점: {submitResult.totalScore} / {submitResult.maxScore}</div>

      <div style={submitResult.passed ? passText : failText}>
        결과: {submitResult.passed ? '✅ 통과' : '❌ 미통과'}
      </div>

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
