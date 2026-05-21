/**
 * @FileName : QuizDetailView.js
 * @Description : AI 온보딩 평가 상세 결과 화면
 * @Author : 김다솜
 * @Date : 2026. 05. 06
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.06    김다솜       최초 생성, Quiz.js에서 평가 상세 결과 컴포넌트 분리
 * @ 2026.05.15    김다솜       UI 조정(AI 사내 포털 기준으로 톤 맞춤)
 * @ 2026.05.19    김다솜       평가 상세 결과에서 자기평가 AI 비교 피드백 노출 및 학습 콘텐츠 이동 연결
 * @ 2026.05.19    김다솜       자기 평가와 AI 평가 비교 차트 추가
 * @ 2026.05.19    김다솜       AI 피드백 줄바꿈 렌더링 및 분석 차트 레이아웃 보완
 * @ 2026.05.19    김다솜       평가 상세 결과 화면 중앙 정렬 및 평가 화면 톤으로 스타일 조정
 */
import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import axiosInstance from 'src/api/axiosInstance';
import { useUser } from 'src/api/UserContext';
import { PATH } from 'src/constants/path';
import SelfCheckComparisonChart from 'src/pages/evaluation/SelfCheckComparisonChart';
import { evalResultStyles } from 'src/styles/js/evaluation/QuizStyle';

const QuizDetailView = () => {
  const { userInfo } = useUser();
  const navigate = useNavigate();
  const { categoryName } = useParams();
  const decodedCategory = decodeURIComponent(categoryName);
  const [detail, setDetail] = useState(null);

  useEffect(() => {
    if (!userInfo?.empNo || !categoryName) return;

    const fetchDetail = async () => {
      try {
        const apiUrl = PATH.API.EVALUATION.QUIZ_DETAIL(userInfo.empNo, decodedCategory);
        const res = await axiosInstance.get(apiUrl);
        setDetail(res.data);
      } catch (err) {
        console.error('quiz detail load failed', err);
      }
    };

    fetchDetail();
  }, [userInfo?.empNo, categoryName, decodedCategory]);

  if (!detail) {
    return (
      <div style={evalResultStyles.detailContainer}>
        <p style={{ color: '#6B7280' }}>상세 결과를 불러오는 중입니다.</p>
      </div>
    );
  }

  const selfCheckFeedback = detail.selfCheckFeedback;
  const hasSelfCheckFeedback = selfCheckFeedback?.submitted && selfCheckFeedback?.feedback;

  const getResultMeta = (item) => {
    if (item.isCorrect === true) {
      return { text: '정답입니다.', color: '#28a745' };
    }

    if (item.isCorrect === false) {
      return { text: '오답입니다.', color: '#dc3545' };
    }

    return { text: 'AI 평가 완료', color: '#2563EB' };
  };

  return (
    <div style={evalResultStyles.detailContainer}>
      <div style={evalResultStyles.detailHeader}>
        <div>
          <h4 style={evalResultStyles.detailHeaderTitle}>
            {detail.categoryName} 카테고리 평가 상세 결과
          </h4>
          <p style={evalResultStyles.detailHeaderDescription}>
            문항별 답변, AI 평가 결과, 자기 평가 기반 피드백을 확인합니다.
          </p>
        </div>
        <div style={evalResultStyles.detailScoreBadge}>
          <div style={{ fontSize: '12px', color: '#6B7280', marginBottom: '4px' }}>
            총점
          </div>
          <div style={{ fontSize: '24px', fontWeight: 800, color: '#2563EB', lineHeight: 1.1 }}>
            {detail.totalScore} / {detail.maxScore}
          </div>
          <div
            style={{
              marginTop: '7px',
              fontSize: '12px',
              fontWeight: 700,
              color: detail.passed ? '#16A34A' : '#DC2626',
            }}
          >
            {detail.passed ? '통과' : '미통과'}
          </div>
        </div>
      </div>

      {hasSelfCheckFeedback && (
        <div
          style={{
            marginBottom: '18px',
            padding: '20px',
            borderRadius: '20px',
            background: '#FFFFFF',
            border: '1px solid #DDE6FF',
            boxShadow: '0 2px 10px rgba(15, 23, 42, 0.03)',
          }}
        >
          <div style={{ fontWeight: 800, marginBottom: '8px', color: '#1D4ED8' }}>
            자기평가 기반 AI 피드백
          </div>
          <div
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              gap: '16px',
              alignItems: 'stretch',
            }}
          >
            <div style={{ flex: '1 1 320px', minWidth: 0 }}>
              <div
                style={{
                  lineHeight: 1.75,
                  color: '#1F2937',
                  whiteSpace: 'pre-line',
                  wordBreak: 'keep-all',
                }}
              >
                {selfCheckFeedback.feedback}
              </div>
              {selfCheckFeedback.contentId && (
                <button
                  type="button"
                  className="btn btn-sm mt-3"
                  style={{
                    background: '#1D4ED8',
                    color: '#fff',
                    borderRadius: '999px',
                    fontWeight: 700,
                    padding: '8px 14px',
                  }}
                  onClick={() => navigate(PATH.ONBOARDING.LEARNING(selfCheckFeedback.contentId))}
                >
                  학습 콘텐츠에서 다시 확인
                </button>
              )}
            </div>
            <SelfCheckComparisonChart
              selfScoreRate={selfCheckFeedback.selfScoreRate}
              evaluationScoreRate={selfCheckFeedback.evaluationScoreRate}
              scoreGap={selfCheckFeedback.scoreGap}
            />
          </div>
          <div className="d-flex flex-wrap gap-2 mt-3 small text-muted">
            {selfCheckFeedback.selfScoreRate != null && <span>자기 평가 {selfCheckFeedback.selfScoreRate}%</span>}
            {selfCheckFeedback.evaluationScoreRate != null && <span>AI 평가 {selfCheckFeedback.evaluationScoreRate}%</span>}
            {selfCheckFeedback.scoreGap != null && (
              <span>차이 {selfCheckFeedback.scoreGap > 0 ? '+' : ''}{selfCheckFeedback.scoreGap}%</span>
            )}
          </div>
        </div>
      )}

      {detail.questions.map((item, idx) => {
        const resultMeta = getResultMeta(item);

        return (
          <div key={idx} style={evalResultStyles.detailCard}>
            <div style={evalResultStyles.questionText}>
              Q{idx + 1}. {item.questionText}
            </div>
            <div style={evalResultStyles.answerBox}>
              <p><strong>내 답변:</strong> {item.userAnswer}</p>
              <p><strong>정답:</strong> {item.correctAnswer}</p>

              {item.aiScore !== null && item.aiScore !== undefined && (
                <p>
                  <strong>AI 점수:</strong> {item.aiScore} / 100
                  {item.score !== null && item.score !== undefined ? ` · 반영 점수 ${item.score}점` : ''}
                  {item.similarityScore !== null && item.similarityScore !== undefined
                    ? ` · 유사도 ${item.similarityScore}`
                    : ''}
                </p>
              )}

              {item.aiFeedback && (
                <div
                  style={{
                    marginTop: '10px',
                    padding: '12px',
                    backgroundColor: '#FFFFFF',
                    borderRadius: '12px',
                    border: '1px dashed #CBD5E1',
                    color: '#4B5563',
                    lineHeight: 1.6,
                  }}
                >
                  <small><strong>AI 피드백:</strong> {item.aiFeedback}</small>
                </div>
              )}

              <p
                style={{
                  color: resultMeta.color,
                  fontWeight: 'bold',
                  marginTop: '8px',
                }}
              >
                {resultMeta.text}
              </p>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default QuizDetailView;
