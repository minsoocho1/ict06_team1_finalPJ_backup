/**
 * @FileName : Dashboard.js
 * @Description : 온보딩 상세 대시보드 화면
 *                - 학습 진행률, 완료 교육 수, 평가 응시/통과 현황 표시
 * @Author : 김다솜
 * @Date : 2026. 05. 06
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.06    김다솜        최초 생성 및 온보딩 대시보드 기본 구조 작성
 * @ 2026.05.08    김다솜        체크리스트 진행률 요약 표시 추가
 * @ 2026.05.12    김다솜        전체화면에서도 카드가 과도하게 확장되지 않도록 중앙 정렬 처리
 * @ 2026.05.15    김다솜        스타일 수정
 */

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axiosInstance from 'src/api/axiosInstance';
import { useUser } from 'src/api/UserContext';
import { PATH } from 'src/constants/path';
import { summaryTitle } from 'src/styles/js/evaluation/QuizStyle';
import {
  dashboardPageStyle,
  progressFill,
  progressTrack,
  summaryCard,
  summaryDesc,
  summaryGrid,
  summaryItemLabel,
  summaryPercent,
} from 'src/styles/js/onboarding/DashboardStyle';

const Dashboard = () => {
  const { userInfo } = useUser();
  const [dashboard, setDashboard] = useState(null);

  const navigate = useNavigate();

  useEffect(() => {
    const empNo = userInfo?.empNo || userInfo?.emp_no;
    if (!empNo) return;

    const fetchDashboard = async () => {
      try {
        const res = await axiosInstance.get(PATH.API.ONBOARDING.DASHBOARD(empNo));
        setDashboard(res.data);
      } catch (err) {
        console.error('[Dashboard] 대시보드 조회 실패', err);
      }
    };
    fetchDashboard();
  }, [userInfo?.empNo, userInfo?.emp_no]);

  if (!dashboard) {
    return <div style={dashboardPageStyle}>온보딩 대시보드를 불러오는 중입니다...</div>;
  }

  return (
    <div style={dashboardPageStyle}>
      <h3 style={{ marginBottom: '8px' }}>온보딩 대시보드</h3>
      <p style={summaryDesc}>
        학습 진행률과 평가 결과를 종합적으로 확인할 수 있습니다.
      </p>

      <div style={summaryCard}>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <div>
            <h5 style={summaryTitle}>전체 학습 진행률</h5>
            <div style={summaryDesc}>
              완료 카테고리 {dashboard.completedCategoryCount} / {dashboard.totalCategoryCount}
              {' · '}
              완료 콘텐츠 {dashboard.completedLearningCount} / {dashboard.totalLearningCount}
              {' · '}
              체크리스트 {dashboard.completedChecklistCount ?? 0} / {dashboard.totalChecklistCount ?? 0}
            </div>
          </div>

          <div style={summaryPercent}>
            {dashboard.learningProgressPercent}%
          </div>
        </div>

        <div style={progressTrack}>
          <div style={progressFill(dashboard.learningProgressPercent)} />
        </div>
      </div>

      <div style={{ marginBottom: '20px', textAlign: 'right' }}>
        <button
          className="btn btn-sm"
          style={{
            borderRadius: '999px',
            border: '1px solid #2563EB',
            background: '#2563EB',
            color: '#FFFFFF',
            fontWeight: 700,
          }}
          onClick={() => navigate(PATH.ONBOARDING.ROADMAP)}
        >
          상세 로드맵 보기
        </button>
      </div>

      <div style={summaryGrid}>
        <div style={summaryCard}>
          <div style={summaryItemLabel}>완료한 카테고리</div>
          <h4>{dashboard.completedCategoryCount} / {dashboard.totalCategoryCount}</h4>
        </div>

        <div style={summaryCard}>
          <div style={summaryItemLabel}>완료한 콘텐츠</div>
          <h4>{dashboard.completedLearningCount} / {dashboard.totalLearningCount}</h4>
        </div>

        <div style={summaryCard}>
          <div style={summaryItemLabel}>체크리스트 진행률</div>
          <h4>{dashboard.checklistProgressPercent ?? 0}%</h4>
          <small style={summaryDesc}>
            {dashboard.completedChecklistCount ?? 0} / {dashboard.totalChecklistCount ?? 0} 완료
          </small>
        </div>

        <div style={summaryCard}>
          <div style={summaryItemLabel}>응시한 평가</div>
          <h4>{dashboard.submittedEvaluationCount}개</h4>
        </div>

        <div style={summaryCard}>
          <div style={summaryItemLabel}>통과 평가</div>
          <h4>{dashboard.passedEvaluationCount}개</h4>
        </div>

        <div style={summaryCard}>
          <div style={summaryItemLabel}>평가 통과율</div>
          <h4>{dashboard.evaluationPassRatePercent}%</h4>
        </div>
      </div>

      <div style={summaryCard}>
        <h5 style={{ ...summaryTitle, marginBottom: '12px' }}>카테고리별 진행 현황</h5>
        {(dashboard.categoryProgresses || []).map((category) => (
          <div key={category.categoryName} style={{ marginBottom: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
              <span style={{ fontWeight: 600 }}>{category.categoryName}</span>
              <span style={summaryDesc}>
                {category.completedLearningCount} / {category.totalLearningCount} ({category.progressPercent}%)
              </span>
            </div>
            <div style={{ ...progressTrack, marginTop: 0 }}>
              <div style={progressFill(category.progressPercent)} />
            </div>
          </div>
        ))}
      </div>

      <div style={summaryCard}>
        <h5 style={{ ...summaryTitle, marginBottom: '12px' }}>미완료 카테고리 Top3</h5>
        {(dashboard.categoryProgresses || [])
          .filter((category) => category.progressPercent < 100)
          .sort((a, b) => a.progressPercent - b.progressPercent)
          .slice(0, 3)
          .map((category, idx) => (
            <div
              key={category.categoryName}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                padding: '8px 0',
                borderBottom: idx === 2 ? 'none' : '1px solid #DDE3EA',
              }}
            >
              <span>{category.categoryName}</span>
              <strong>{category.progressPercent}%</strong>
            </div>
          ))}
        {(dashboard.categoryProgresses || []).every((category) => category.progressPercent === 100) && (
          <div style={summaryDesc}>모든 카테고리를 완료했습니다.</div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;
