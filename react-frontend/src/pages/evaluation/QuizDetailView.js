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
 */
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';

import axiosInstance from 'src/api/axiosInstance';
import { useUser } from 'src/api/UserContext';
import { PATH } from 'src/constants/path';
import { evalResultStyles } from 'src/styles/js/evaluation/QuizStyle';

const QuizDetailView = () => {
  const { userInfo } = useUser();
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

  if (!detail) return <p>상세 결과를 불러오는 중입니다.</p>;

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
    <div>
      <div style={{ marginBottom: '20px' }}>
        <h4 style={{ marginBottom: '8px' }}>{detail.categoryName} 카테고리 평가 상세 결과</h4>
        <p style={{ margin: 0, color: '#6c757d' }}>
          총점 {detail.totalScore} / {detail.maxScore} · {detail.passed ? '통과' : '미통과'}
        </p>
      </div>

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
                    padding: '10px',
                    backgroundColor: '#F4F7FB',
                    borderRadius: '12px',
                    border: '1px solid #DDE3EA',
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
