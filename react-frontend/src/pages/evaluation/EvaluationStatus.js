/**
 * @FileName : EvaluationStatus.js
 * @Description : AI 온보딩 평가 응시 현황 화면
 * @Author : 김다솜
 * @Date : 2026. 05. 06
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.06    김다솜       최초 생성, Quiz.js에서 평가 응시 현황 컴포넌트 분리
 * @ 2026.05.15    김다솜       UI 조정(AI 사내 포털 기준으로 톤 맞춤)
 */
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import axiosInstance from 'src/api/axiosInstance';
import { useUser } from 'src/api/UserContext';
import { PATH } from 'src/constants/path';
import {
  categoryText,
  evaluationStatusBar,
  getEvaluationButtonClass,
  getEvaluationStatusSubText,
  getEvaluationStatusText,
  quizContainer,
  quizHeader,
  statusButton,
  statusCard,
  statusContentWrap,
  statusGrid,
  statusTitle,
} from 'src/styles/js/evaluation/QuizStyle';

const EvaluationStatus = () => {
  const { userInfo } = useUser();
  const navigate = useNavigate();

  const [evaluationResults, setEvaluationResults] = useState([]);
  const [roadmapGroups, setRoadmapGroups] = useState([]);
  const [completedCategories, setCompletedCategories] = useState([]);

  useEffect(() => {
    if (!userInfo?.empNo) return;

    const fetchEvaluationResults = async () => {
      try {
        const res = await axiosInstance.get(PATH.API.EVALUATION.QUIZ_RESULT(userInfo.empNo));
        setEvaluationResults(Array.isArray(res.data) ? res.data : []);
      } catch (err) {
        console.error('evaluation result load failed', err);
      }
    };

    fetchEvaluationResults();
  }, [userInfo?.empNo]);

  useEffect(() => {
    if (!userInfo?.empNo) return;

    const fetchRoadmap = async () => {
      try {
        const res = await axiosInstance.get(PATH.API.ONBOARDING.ROADMAP(userInfo.empNo));
        const fetchedRoadmapGroups = res.data?.recommended_roadmap || [];
        const completed = fetchedRoadmapGroups
          .filter((group) => group.items.every((item) => item.status === 'COMPLETED'))
          .map((group) => group.category_name);

        setRoadmapGroups(fetchedRoadmapGroups);
        setCompletedCategories(completed);
      } catch (err) {
        console.error('roadmap load failed', err);
      }
    };

    fetchRoadmap();
  }, [userInfo?.empNo]);

  const categories = Array.from(
    new Set([
      ...roadmapGroups.map((group) => group.category_name),
      ...evaluationResults.map((result) => result.categoryName),
    ].filter(Boolean))
  );

  return (
    <div style={quizContainer}>
      <div style={quizHeader}>
        <h2>AI 퀴즈 및 평가</h2>
        <p style={categoryText}>
          로드맵에서 학습을 완료한 카테고리만 평가를 응시할 수 있습니다.
        </p>
      </div>

      <div style={statusGrid}>
        {categories.map((category) => {
          const result = evaluationResults.find((item) => item.categoryName === category);
          const isLearningCompleted = completedCategories.includes(category);
          const hasPassed = isLearningCompleted && result?.passed;
          const hasSubmitted = isLearningCompleted && result?.submitted;

          return (
            <div key={category} style={statusCard}>
              <div style={statusContentWrap}>
                <div
                  style={evaluationStatusBar({
                    isLearningCompleted,
                    isPassed: hasPassed,
                    isSubmitted: hasSubmitted,
                  })}
                />

                <div>
                  <div style={statusTitle}>{category}</div>
                  <div
                    style={getEvaluationStatusSubText({
                      isLearningCompleted,
                      isPassed: hasPassed,
                      isSubmitted: hasSubmitted,
                    })}
                  >
                    {getEvaluationStatusText({
                      isLearningCompleted,
                      result: isLearningCompleted ? result : null,
                    })}
                  </div>
                </div>
              </div>

              <button
                className={`btn btn-sm ${getEvaluationButtonClass({
                  isLearningCompleted,
                  isPassed: hasPassed,
                  isSubmitted: hasSubmitted,
                })}`}
                style={{
                  ...statusButton,
                  border: !isLearningCompleted
                    ? '1px solid #DDE3EA'
                    : hasPassed
                      ? '1px solid #16A34A'
                      : hasSubmitted
                        ? '1px solid #F59E0B'
                        : '1px solid #2563EB',
                  background: !isLearningCompleted
                    ? '#FFFFFF'
                    : hasPassed
                      ? '#16A34A'
                      : hasSubmitted
                        ? '#F59E0B'
                        : '#2563EB',
                  color: !isLearningCompleted ? '#6B7280' : '#FFFFFF',
                }}
                disabled={!isLearningCompleted}
                onClick={() => {
                  if (isLearningCompleted && hasPassed) {
                    navigate(PATH.EVALUATION.RESULT);
                  } else if (isLearningCompleted) {
                    navigate(PATH.EVALUATION.QUIZ(category));
                  }
                }}
              >
                {!isLearningCompleted ? '학습 미완료' : hasPassed ? '결과 조회' : '응시하기'}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default EvaluationStatus;
