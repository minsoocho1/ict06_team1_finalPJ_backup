/**
 * @FileName : SelfCheckComparisonChart.js
 * @Description : 자기 평가와 AI 평가 점수 비교 미니 차트
 * @Author : 김다솜
 * @Date : 2026. 05. 19
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.19    김다솜       자기 평가와 AI 평가 비교 시각화 컴포넌트 생성
 * @ 2026.05.19    김다솜       점수 차이, 일치도, 평균 성취도 기반 분석 차트로 확장
 */
import React from 'react';

const clampRate = (value) => {
  const numberValue = Number(value);
  if (Number.isNaN(numberValue)) return 0;
  return Math.max(0, Math.min(100, numberValue));
};

const SelfCheckComparisonChart = ({ selfScoreRate, evaluationScoreRate, scoreGap }) => {
  const selfRate = clampRate(selfScoreRate);
  const aiRate = clampRate(evaluationScoreRate);
  const gap = scoreGap != null ? Number(scoreGap) : aiRate - selfRate;
  const absGap = Math.abs(gap);
  const averageRate = Math.round(((selfRate + aiRate) / 2) * 10) / 10;
  const alignmentRate = Math.round(Math.max(0, 100 - absGap) * 10) / 10;
  const gapText = `${gap > 0 ? '+' : ''}${Math.round(gap * 10) / 10}%`;

  const getStatus = () => {
    if (absGap <= 10) {
      return {
        label: '균형적',
        color: '#059669',
        background: '#D1FAE5',
        message: '자기 평가와 AI 평가가 비슷해 현재 이해 수준을 비교적 정확히 보고 있습니다.',
      };
    }
    if (gap >= 20) {
      return {
        label: '자신감 낮음',
        color: '#2563EB',
        background: '#DBEAFE',
        message: 'AI 평가보다 자기 평가가 낮습니다. 실제 성과에 비해 자신감을 낮게 잡은 상태입니다.',
      };
    }
    if (gap <= -20) {
      return {
        label: '재점검 필요',
        color: '#DC2626',
        background: '#FEE2E2',
        message: '자기 평가보다 AI 평가가 낮습니다. 이해했다고 느낀 부분을 문제 기준으로 다시 확인하세요.',
      };
    }
    return {
      label: '가벼운 차이',
      color: '#D97706',
      background: '#FEF3C7',
      message: '두 점수 사이에 차이가 조금 있습니다. 틀렸거나 덜 맞은 문제 중심으로 보완하면 좋습니다.',
    };
  };

  const status = getStatus();

  const rows = [
    {
      label: '자기 평가',
      value: selfRate,
      color: '#60A5FA',
      background: '#DBEAFE',
    },
    {
      label: 'AI 평가',
      value: aiRate,
      color: '#2563EB',
      background: '#C7D2FE',
    },
  ];

  return (
    <div
      style={{
        flex: '0 1 340px',
        minWidth: '280px',
        padding: '16px',
        borderRadius: '16px',
        background: '#FFFFFF',
        border: '1px solid #DDE6FF',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', marginBottom: '14px' }}>
        <div>
          <div style={{ fontWeight: 800, color: '#1D4ED8' }}>평가 비교 분석</div>
          <div style={{ marginTop: '3px', fontSize: '0.78rem', color: '#6B7280' }}>
            자기 인식과 AI 평가의 간격을 함께 봅니다.
          </div>
        </div>
        <div
          style={{
            flex: '0 0 72px',
            height: '72px',
            borderRadius: '50%',
            background: `conic-gradient(${status.color} ${alignmentRate * 3.6}deg, #E5E7EB 0deg)`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <div
            style={{
              width: '54px',
              height: '54px',
              borderRadius: '50%',
              background: '#fff',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              lineHeight: 1.1,
            }}
          >
            <strong style={{ fontSize: '0.95rem', color: '#111827' }}>{alignmentRate}</strong>
            <span style={{ fontSize: '0.68rem', color: '#6B7280' }}>일치도</span>
          </div>
        </div>
      </div>

      {rows.map((row) => (
        <div key={row.label} style={{ marginBottom: '12px' }}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              gap: '10px',
              fontSize: '0.82rem',
              color: '#4B5563',
              marginBottom: '5px',
            }}
          >
            <span>{row.label}</span>
            <strong style={{ color: '#111827' }}>{row.value}%</strong>
          </div>
          <div
            style={{
              height: '10px',
              borderRadius: '999px',
              background: row.background,
              overflow: 'hidden',
            }}
          >
            <div
              style={{
                width: `${row.value}%`,
                height: '100%',
                borderRadius: '999px',
                background: row.color,
              }}
            />
          </div>
        </div>
      ))}

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
          gap: '8px',
          marginTop: '14px',
        }}
      >
        {[
          { label: '평균 성취도', value: `${averageRate}%`, color: '#111827' },
          { label: '차이', value: gapText, color: gap >= 0 ? '#1D4ED8' : '#DC2626' },
          { label: '상태', value: status.label, color: status.color },
        ].map((item) => (
          <div
            key={item.label}
            style={{
              padding: '10px 8px',
              borderRadius: '12px',
              background: '#F9FAFB',
              border: '1px solid #E5E7EB',
              minWidth: 0,
            }}
          >
            <div style={{ fontSize: '0.72rem', color: '#6B7280', marginBottom: '4px' }}>
              {item.label}
            </div>
            <strong style={{ display: 'block', color: item.color, fontSize: '0.9rem' }}>
              {item.value}
            </strong>
          </div>
        ))}
      </div>

      <div
        style={{
          marginTop: '12px',
          padding: '11px 12px',
          borderRadius: '12px',
          background: status.background,
          color: status.color,
          fontSize: '0.82rem',
          lineHeight: 1.5,
          fontWeight: 600,
        }}
      >
        {status.message}
      </div>
    </div>
  );
};

export default SelfCheckComparisonChart;
