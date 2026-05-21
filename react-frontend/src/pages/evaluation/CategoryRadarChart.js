/**
 * @FileName : CategoryRadarChart.js
 * @Description : 카테고리별 학습 진행률과 평가 점수율 레이더 차트
 * @Author : 김다솜
 * @Date : 2026. 05. 19
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.19    김다솜       전체 학습/평가 데이터 기반 카테고리별 방사형 차트 생성
 */
import React from 'react';
import {
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
  ResponsiveContainer,
  Tooltip,
} from 'recharts';

const roundOneDecimal = (value) => Math.round(value * 10) / 10;

const buildChartRows = (roadmapGroups = [], results = []) => {
  const resultMap = new Map(results.map((result) => [result.categoryName, result]));
  const categoryNames = Array.from(
    new Set([
      ...roadmapGroups.map((group) => group.category_name),
      ...results.map((result) => result.categoryName),
    ].filter(Boolean))
  );

  return categoryNames.map((categoryName) => {
    const group = roadmapGroups.find((item) => item.category_name === categoryName);
    const result = resultMap.get(categoryName);
    const items = group?.items || [];
    const completedCount = items.filter((item) => item.status === 'COMPLETED').length;
    const learningRate = items.length > 0 ? roundOneDecimal((completedCount / items.length) * 100) : 0;
    const evaluationRate = result?.maxScore > 0
      ? roundOneDecimal((result.totalScore / result.maxScore) * 100)
      : 0;

    return {
      categoryName,
      learningRate,
      evaluationRate,
      gap: roundOneDecimal(evaluationRate - learningRate),
      passed: Boolean(result?.passed),
      submitted: Boolean(result?.submitted),
    };
  });
};

const buildSummary = (rows) => {
  if (rows.length === 0) {
    return {
      learningAverage: 0,
      evaluationAverage: 0,
      passedCount: 0,
      biggestGapCategory: '-',
      biggestGap: 0,
    };
  }

  const learningAverage = roundOneDecimal(
    rows.reduce((sum, row) => sum + row.learningRate, 0) / rows.length
  );
  const evaluationAverage = roundOneDecimal(
    rows.reduce((sum, row) => sum + row.evaluationRate, 0) / rows.length
  );
  const biggestGapRow = rows.reduce((selected, row) => (
    Math.abs(row.gap) > Math.abs(selected.gap) ? row : selected
  ), rows[0]);

  return {
    learningAverage,
    evaluationAverage,
    passedCount: rows.filter((row) => row.passed).length,
    biggestGapCategory: biggestGapRow.categoryName,
    biggestGap: biggestGapRow.gap,
  };
};

const CustomTooltip = ({ active, payload }) => {
  if (!active || !payload?.length) return null;

  const row = payload[0].payload;

  return (
    <div
      style={{
        padding: '10px 12px',
        borderRadius: '12px',
        background: '#FFFFFF',
        border: '1px solid #DDE3EA',
        boxShadow: '0 8px 18px rgba(15, 23, 42, 0.08)',
        fontSize: '12px',
        lineHeight: 1.7,
      }}
    >
      <strong style={{ display: 'block', marginBottom: '4px' }}>{row.categoryName}</strong>
      <div>학습 진행률: {row.learningRate}%</div>
      <div>평가 점수율: {row.evaluationRate}%</div>
      <div>차이: {row.gap > 0 ? '+' : ''}{row.gap}%</div>
      <div>상태: {row.passed ? '통과' : row.submitted ? '재응시 필요' : '평가 대기'}</div>
    </div>
  );
};

const CategoryRadarChart = ({ roadmapGroups, results }) => {
  const rows = buildChartRows(roadmapGroups, results);
  const summary = buildSummary(rows);

  if (rows.length === 0) {
    return null;
  }

  const summaryCards = [
    { label: '평균 학습 진행률', value: `${summary.learningAverage}%`, color: '#2563EB' },
    { label: '평균 평가 점수율', value: `${summary.evaluationAverage}%`, color: '#16A34A' },
    { label: '통과 카테고리', value: `${summary.passedCount} / ${rows.length}`, color: '#7C3AED' },
    {
      label: '가장 큰 차이',
      value: `${summary.biggestGapCategory} ${summary.biggestGap > 0 ? '+' : ''}${summary.biggestGap}%`,
      color: summary.biggestGap >= 0 ? '#2563EB' : '#DC2626',
    },
  ];

  return (
    <div
      style={{
        marginTop: '24px',
        padding: '20px',
        borderRadius: '20px',
        background: '#FFFFFF',
        border: '1px solid #DDE3EA',
        boxShadow: '0 2px 10px rgba(15, 23, 42, 0.03)',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          gap: '16px',
          flexWrap: 'wrap',
          marginBottom: '14px',
        }}
      >
        <div>
          <h5 style={{ margin: 0, fontWeight: 800 }}>카테고리별 학습·평가 균형</h5>
          <p style={{ margin: '6px 0 0', color: '#6B7280', fontSize: '13px' }}>
            전체 로드맵 학습 진행률과 평가 점수율을 같은 축에서 비교합니다.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center', fontSize: '12px' }}>
          <span><span style={{ color: '#2563EB', fontWeight: 900 }}>●</span> 학습 진행률</span>
          <span><span style={{ color: '#16A34A', fontWeight: 900 }}>●</span> 평가 점수율</span>
        </div>
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '18px', alignItems: 'stretch' }}>
        <div style={{ flex: '1 1 520px', minHeight: '360px' }}>
          <ResponsiveContainer width="100%" height={360}>
            <RadarChart data={rows} outerRadius="72%">
              <PolarGrid stroke="#DDE3EA" />
              <PolarAngleAxis dataKey="categoryName" tick={{ fill: '#374151', fontSize: 12 }} />
              <PolarRadiusAxis angle={90} domain={[0, 100]} tick={{ fill: '#6B7280', fontSize: 11 }} />
              <Tooltip content={<CustomTooltip />} />
              <Radar
                name="학습 진행률"
                dataKey="learningRate"
                stroke="#2563EB"
                fill="#2563EB"
                fillOpacity={0.22}
              />
              <Radar
                name="평가 점수율"
                dataKey="evaluationRate"
                stroke="#16A34A"
                fill="#16A34A"
                fillOpacity={0.18}
              />
            </RadarChart>
          </ResponsiveContainer>
        </div>

        <div
          style={{
            flex: '0 1 300px',
            display: 'grid',
            gridTemplateColumns: '1fr',
            gap: '10px',
          }}
        >
          {summaryCards.map((card) => (
            <div
              key={card.label}
              style={{
                padding: '14px',
                borderRadius: '14px',
                background: '#F9FAFB',
                border: '1px solid #E5E7EB',
              }}
            >
              <div style={{ fontSize: '12px', color: '#6B7280', marginBottom: '5px' }}>
                {card.label}
              </div>
              <strong style={{ color: card.color, fontSize: '18px' }}>{card.value}</strong>
            </div>
          ))}
          <div
            style={{
              padding: '14px',
              borderRadius: '14px',
              background: '#EFF6FF',
              color: '#1D4ED8',
              fontSize: '13px',
              lineHeight: 1.6,
              fontWeight: 600,
            }}
          >
            학습 진행률은 높은데 평가 점수율이 낮은 카테고리는 문제 풀이 기준으로 다시 확인하는 것이 좋습니다.
          </div>
        </div>
      </div>
    </div>
  );
};

export default CategoryRadarChart;
