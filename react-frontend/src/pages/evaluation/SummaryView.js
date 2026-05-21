/**
 * @FileName : SummaryView.js
 * @Description : AI 온보딩 평가 결과 요약 화면
 * @Author : 김다솜
 * @Date : 2026. 05. 15
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.06    김다솜       최초 생성, Quiz.js에서 평가 진행 현황/결과 요약 컴포넌트 분리
 * @ 2026.05.15    김다솜       UI 조정(AI 사내 포털 기준으로 톤 맞춤)
 * @ 2026.05.19    김다솜       카테고리별 학습·평가 방사형 통계 차트 추가
 */
import { CBadge, CCard, CCardBody } from '@coreui/react';
import React from 'react';
import { useNavigate } from 'react-router-dom';

import { useUser } from 'src/api/UserContext';
import { PATH } from 'src/constants/path';
import CategoryRadarChart from './CategoryRadarChart';

const SummaryView = ({
  results,
  roadmapGroups,
  userName,
  styles,
  headerTitle,
  headerDescription,
}) => {
  const { userInfo } = useUser();
  const navigate = useNavigate();
  if (!styles) return null;

  const roadmapCategories = roadmapGroups.map((group) => group.category_name).filter(Boolean);
  const completedCategories = roadmapGroups
    .filter((group) => group.items.every((item) => item.status === 'COMPLETED'))
    .map((group) => group.category_name);

  const passedCount = results.filter((result) => result.passed).length;

  const pendingEvaluations = roadmapCategories
    .map((category) => {
      const result = results.find((item) => item.categoryName === category);
      const isLearningCompleted = completedCategories.includes(category);

      return {
        categoryName: category,
        isLearningCompleted,
        submitted: result?.submitted || false,
        passed: result?.passed || false,
        totalScore: result?.totalScore || 0,
        maxScore: result?.maxScore || 0,
      };
    })
    .filter((item) => !item.passed);

  const completedEvaluations = results.filter((result) => result.passed);

  const getPendingDescription = (item) => {
    if (!item.isLearningCompleted) {
      return '학습 미완료 · 로드맵 학습을 먼저 완료해 주세요';
    }

    if (item.submitted) {
      return `재응시 필요 · ${item.totalScore} / ${item.maxScore}`;
    }

    return '학습 완료 · 평가 미응시';
  };

  const getPendingBadge = (item) => {
    if (!item.isLearningCompleted) {
      return { color: 'secondary', text: '학습 미완료' };
    }

    if (item.submitted) {
      return { color: 'warning', text: '재응시 필요' };
    }

    return { color: 'info', text: '평가 대기' };
  };

  const getPendingButtonLabel = (item) => {
    if (!item.isLearningCompleted) {
      return '로드맵에서 학습하기';
    }

    return item.submitted ? '다시 응시하기' : '평가 응시하기';
  };

  const moveToRoadmapCategory = (categoryName) => {
    navigate(PATH.ONBOARDING.ROADMAP, {
      state: { focusCategory: categoryName },
    });
  };

  return (
    <div>
      <div style={styles.summaryHeader}>
        <div>
          <h4 style={{ margin: 0, fontWeight: 700 }}>{headerTitle}</h4>
          <p style={{ margin: '6px 0 0', color: '#6c757d' }}>
            {headerDescription || `${userName}님의 카테고리별 평가 진행 현황입니다.`}
          </p>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '28px', fontWeight: 700, color: '#2563EB' }}>
            {passedCount} / {completedCategories.length}
          </div>
          <div style={{ fontSize: '13px', color: '#6c757d' }}>
            학습 완료 카테고리 중 통과
          </div>
        </div>
      </div>

      <CategoryRadarChart roadmapGroups={roadmapGroups} results={results} />

      <div style={{ marginTop: '24px' }}>
        <h5 style={{ fontWeight: 700 }}>진행할 평가</h5>

        {pendingEvaluations.length === 0 ? (
          <div style={{ textAlign: 'center', color: '#999', padding: '24px 0' }}>
            진행할 평가가 없습니다.
          </div>
        ) : (
          <div style={styles.grid}>
            {pendingEvaluations.map((item) => {
              const badge = getPendingBadge(item);

              return (
                <CCard
                  key={item.categoryName}
                  style={{
                    borderRadius: '18px',
                    border: item.isLearningCompleted
                      ? item.submitted
                        ? '1px solid #ffe8a1'
                        : '1px solid #DDE3EA'
                      : '1px solid #d6d8db',
                    boxShadow: '0 2px 10px rgba(15, 23, 42, 0.03)',
                    background: !item.isLearningCompleted
                      ? '#F4F7FB'
                      : item.submitted
                        ? '#fff3f3'
                        : '#FFFFFF',
                    cursor: !item.isLearningCompleted ? 'pointer' : 'default',
                  }}
                  onClick={() => {
                    if (!item.isLearningCompleted) {
                      moveToRoadmapCategory(item.categoryName);
                    }
                  }}
                >
                  <CCardBody>
                    <div style={{ fontWeight: 600, fontSize: '15px', marginBottom: '8px' }}>
                      {item.categoryName}
                    </div>

                    <div style={{ fontSize: '13px', color: '#6c757d', marginBottom: '12px' }}>
                      {getPendingDescription(item)}
                    </div>

                    <CBadge
                      color={badge.color}
                      shape="rounded-pill"
                      style={{ padding: '6px 14px', fontSize: '13px' }}
                    >
                      {badge.text}
                    </CBadge>

                    <button
                      onClick={() => {
                        if (!item.isLearningCompleted) {
                          moveToRoadmapCategory(item.categoryName);
                          return;
                        }

                        navigate(PATH.EVALUATION.QUIZ(item.categoryName));
                      }}
                      className="btn btn-sm"
                      style={{
                        marginTop: '12px',
                        width: '100%',
                        borderRadius: '999px',
                        border: item.isLearningCompleted ? '1px solid #2563EB' : '1px solid #DDE3EA',
                        background: item.isLearningCompleted ? '#2563EB' : '#FFFFFF',
                        color: item.isLearningCompleted ? '#FFFFFF' : '#6B7280',
                        fontWeight: 700,
                      }}
                    >
                      {getPendingButtonLabel(item)}
                    </button>
                  </CCardBody>
                </CCard>
              );
            })}
          </div>
        )}
      </div>

      <div style={{ marginTop: '28px' }}>
        <h5 style={{ fontWeight: 700 }}>완료된 평가</h5>

        {completedEvaluations.length === 0 ? (
          <div style={{ textAlign: 'center', color: '#999', padding: '24px 0' }}>
            완료된 평가가 없습니다.
          </div>
        ) : (
          <div style={styles.grid}>
            {completedEvaluations.map((result, idx) => {
              const scoreRate = result.maxScore > 0
                ? (result.totalScore / result.maxScore) * 100
                : 0;

              return (
                <CCard
                  key={`${result.categoryName}-${idx}`}
                  style={{
                    borderRadius: '18px',
                    border: '1px solid #c3e6cb',
                    boxShadow: '0 2px 10px rgba(15, 23, 42, 0.03)',
                  }}
                >
                  <CCardBody>
                    <div style={{ fontWeight: 600, fontSize: '15px', marginBottom: '12px' }}>
                      {result.categoryName}
                    </div>

                    <div style={{ marginBottom: '12px' }}>
                      <div
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          fontSize: '13px',
                          color: '#6c757d',
                          marginBottom: '4px',
                        }}
                      >
                        <span>점수</span>
                        <span>{result.totalScore} / {result.maxScore}</span>
                      </div>

                      <div
                        style={{
                          height: '8px',
                          background: '#e9ecef',
                          borderRadius: '4px',
                          overflow: 'hidden',
                        }}
                      >
                        <div
                          style={{
                            width: `${scoreRate}%`,
                            height: '100%',
                            background: '#16A34A',
                            borderRadius: '4px',
                            transition: 'width 0.4s ease',
                          }}
                        />
                      </div>
                    </div>

                    <CBadge
                      color="success"
                      shape="rounded-pill"
                      style={{ padding: '6px 14px', fontSize: '13px' }}
                    >
                      통과
                    </CBadge>

                    <button
                      onClick={() => navigate(PATH.EVALUATION.QUIZ_DETAIL(userInfo.empNo, result.categoryName))}
                      className="btn btn-sm"
                      style={{
                        marginTop: '12px',
                        width: '100%',
                        borderRadius: '999px',
                        border: '1px solid #2563EB',
                        color: '#2563EB',
                        background: '#FFFFFF',
                        fontWeight: 700,
                      }}
                    >
                      상세 결과 보기
                    </button>
                  </CCardBody>
                </CCard>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default SummaryView;
