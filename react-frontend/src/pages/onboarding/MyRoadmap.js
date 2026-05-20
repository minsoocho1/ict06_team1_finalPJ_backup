/**
 * @FileName : MyRoadmap.js
 * @Description : 사용자 AI 온보딩 로드맵 화면
 * @Author : 김다솜
 * @Date : 2026. 05. 12
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.12    김다솜        사용자 정보 Context 연동, 홈 피드와 통일된 카드 디자인 및 로드맵 상세 구성 적용
 * @ 2026.05.15    김다솜        UI 조정(AI 사내 포털 기준으로 톤 맞춤)
 */
import { CBadge, CCard, CCardBody, CCardHeader, CCol, CProgress, CProgressBar, CRow, CSpinner } from '@coreui/react';
import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import axiosInstance from 'src/api/axiosInstance';
import { useUser } from 'src/api/UserContext';
import { PATH } from 'src/constants/path';
import { userHomePageStyle, progressLabel } from 'src/styles/js/common/UserHomeStyle';
import { cardCore, COLORS } from 'src/styles/js/onboarding/OnboardingStyle';

import ChecklistPreview from './ChecklistPreview';

function MyRoadmap({ userInfo: propUserInfo }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { userInfo: contextUserInfo, userLoading } = useUser();
  const userInfo = propUserInfo || contextUserInfo;

  const [roadmapGroups, setRoadmapGroups] = useState([]);
  const [openGroup, setOpenGroup] = useState(null);
  const [loading, setLoading] = useState(true);
  const [evaluationResults, setEvaluationResults] = useState([]);

  const handleLearningClick = (contentId, title, itemId, isCompleted = false) => {
    navigate(PATH.ONBOARDING.LEARNING(contentId), {
      state: { title, itemId, userInfo, isCompleted },
    });
  };

  useEffect(() => {
    const loadRoadmap = async () => {
      const empNo = userInfo?.empNo || userInfo?.emp_no;

      if (!empNo) {
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const response = await axiosInstance.get(PATH.API.ONBOARDING.ROADMAP(empNo));
        setRoadmapGroups(response.data?.recommended_roadmap || []);
      } catch (err) {
        console.error('[MyRoadmap] roadmap load failed:', err);
        setRoadmapGroups([]);
      } finally {
        setLoading(false);
      }
    };

    if (!userLoading) {
      loadRoadmap();
    }
  }, [userInfo?.empNo, userInfo?.emp_no, userLoading]);

  useEffect(() => {
    const empNo = userInfo?.empNo || userInfo?.emp_no;
    if (!empNo) return;

    const loadEvaluationResults = async () => {
      try {
        const res = await axiosInstance.get(PATH.API.EVALUATION.QUIZ_RESULT(empNo));
        setEvaluationResults(Array.isArray(res.data) ? res.data : []);
      } catch (err) {
        console.error('[MyRoadmap] evaluation result load failed:', err);
      }
    };

    loadEvaluationResults();
  }, [userInfo?.empNo, userInfo?.emp_no]);

  useEffect(() => {
    const updatedItemId = location.state?.updatedItemId;
    if (!updatedItemId) return;

    setRoadmapGroups((prevGroups) =>
      prevGroups.map((group) => ({
        ...group,
        items: group.items.map((item) =>
          Number(item.item_id || item.itemId) === Number(updatedItemId)
            ? { ...item, status: 'COMPLETED', rate: 100 }
            : item
        ),
      }))
    );

    navigate(location.pathname, { replace: true, state: {} });
  }, [location.state, location.pathname, navigate]);

  useEffect(() => {
    const focusCategory = location.state?.focusCategory;
    if (!focusCategory || roadmapGroups.length === 0) return;

    const targetIndex = roadmapGroups.findIndex((group) => group.category_name === focusCategory);
    if (targetIndex >= 0) {
      setOpenGroup(targetIndex);
      setTimeout(() => {
        const targetElement = document.getElementById(`roadmap-category-${targetIndex}`);
        targetElement?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 80);
    }
  }, [location.state, roadmapGroups]);

  const getStatusBadge = (status) => {
    switch (status) {
      case 'COMPLETED':
        return { color: 'success', text: '완료' };
      case 'IN_PROGRESS':
        return { color: 'warning', text: '진행중' };
      case 'NOT_STARTED':
      default:
        return { color: 'secondary', text: '미시작' };
    }
  };

  const calculateProgress = (items = []) => {
    const total = items.length;
    const completed = items.filter((item) => item.status === 'COMPLETED').length;
    const percent = total === 0 ? 0 : Math.round((completed / total) * 100);
    return { completed, total, percent };
  };

  const calculateTotalProgress = (roadmap = []) => {
    let total = 0;
    let completed = 0;

    roadmap.forEach((group) => {
      total += group.items.length;
      completed += group.items.filter((item) => item.status === 'COMPLETED').length;
    });

    const percent = total === 0 ? 0 : Math.round((completed / total) * 100);
    return { total, completed, percent };
  };

  const totalProgress = calculateTotalProgress(roadmapGroups);

  if (userLoading || !userInfo) {
    return (
      <div className="text-center py-5">
        <CSpinner color="primary" />
        <p>사용자 정보를 불러오는 중입니다.</p>
      </div>
    );
  }

  return (
    <div style={userHomePageStyle}>
      <CRow className="justify-content-center">
        <CCol lg={8} className="mb-4">
          <CCard className="h-100" style={cardCore}>
            <CCardHeader className="bg-white border-0 py-3">
              <h4 className="mb-1 fw-bold text-dark">{`${userInfo?.name || '사용자'}님의 AI 온보딩 로드맵`}</h4>
              <div className="small text-muted">카테고리별 학습을 진행하고, 완료 후 평가에 응시할 수 있습니다.</div>
            </CCardHeader>
            <CCardBody className="py-4">
              <div className="d-flex justify-content-between align-items-end mb-2">
                <span style={progressLabel}>전체 진행률</span>
                <span className="h4 mb-0 fw-bold" style={{ color: COLORS.primary }}>
                  {totalProgress.completed}/{totalProgress.total} · {totalProgress.percent}%
                </span>
              </div>
              <CProgress height={12} className="bg-light">
                <CProgressBar style={{ backgroundColor: COLORS.primary }} value={totalProgress.percent} animated={totalProgress.percent < 100} />
              </CProgress>
            </CCardBody>
          </CCard>
        </CCol>

        <CCol lg={4} className="mb-4">
          <ChecklistPreview userInfo={userInfo} />
        </CCol>
      </CRow>

      {loading ? (
        <div className="text-center py-5">
          <CSpinner color="primary" />
          <p className="mt-3" style={{ color: '#666' }}>
            {`${userInfo?.name || '사용자'}님의 데이터를 분석해 로드맵을 불러오는 중입니다.`}
          </p>
        </div>
      ) : roadmapGroups.length === 0 ? (
        <CCard style={cardCore}>
          <CCardBody className="text-center text-muted py-5">
            추천 로드맵이 없습니다. 잠시 후 다시 시도해 주세요.
          </CCardBody>
        </CCard>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
          {roadmapGroups.map((group, idx) => {
            const progress = calculateProgress(group.items);
            const isCategoryCompleted = progress.completed === progress.total;
            const evaluationResult = evaluationResults.find(
              (result) => result.categoryName === group.category_name
            );
            const isSubmitted = evaluationResult?.submitted;
            const isPassed = evaluationResult?.passed;

            return (
              <CCard key={idx} id={`roadmap-category-${idx}`} style={cardCore}>
                <CCardBody>
                  <button
                    type="button"
                    className="w-100 border-0 bg-transparent p-0 text-start"
                    onClick={() => setOpenGroup(openGroup === idx ? null : idx)}
                  >
                    <div className="d-flex justify-content-between align-items-start gap-3">
                      <div>
                        <div className="fw-bold text-dark">
                          {group.category_name}
                          <span className="small text-muted ms-2">
                            {progress.completed}/{progress.total} 완료 · {progress.percent}%
                          </span>
                        </div>
                        <div className="small text-muted mt-1">
                          {!isCategoryCompleted
                            ? `학습 진행 중 (${progress.completed}/${progress.total})`
                            : isPassed
                              ? '평가 통과 완료'
                              : isSubmitted
                                ? '평가 재응시 필요'
                                : '학습 완료 / 평가 미응시'}
                        </div>
                      </div>
                      <span className="small text-muted">{openGroup === idx ? '접기 ▲' : '열기 ▼'}</span>
                    </div>
                  </button>

                  {openGroup === idx && (
                    <div className="mt-3">
                      {group.items?.map((content, i) => {
                        const contentId = content.content_id || content.contentId;
                        const itemId = content.item_id || content.itemId;
                        const title = content.item_title || content.title;

                        return (
                          <button
                            key={itemId || i}
                            type="button"
                            className="list-group-item list-group-item-action w-100 px-3 py-3 border-0 rounded-3 mb-2"
                            style={{
                              background: '#F4F7FB',
                              border: '1px solid #DDE3EA',
                              textAlign: 'left',
                            }}
                            onClick={() => {
                              if (!contentId) {
                                alert('콘텐츠 정보가 없습니다.');
                                return;
                              }

                              handleLearningClick(contentId, title, itemId, content.status === 'COMPLETED');
                            }}
                          >
                            <div className="d-flex justify-content-between align-items-start gap-3">
                              <div>
                                <div className="fw-semibold text-dark">{content.item_title}</div>
                                {content.recommendation_reason && (
                                  <div className="small text-muted mt-1" style={{ maxWidth: '680px' }}>
                                    추천 이유: {content.recommendation_reason}
                                  </div>
                                )}
                                <div className="small text-muted mt-1">CONTENT {i + 1} · 진행률 {content.rate || 0}%</div>
                              </div>
                              <div className="d-flex align-items-center gap-2">
                                <CBadge color={getStatusBadge(content.status).color} shape="rounded-pill">
                                  {getStatusBadge(content.status).text}
                                </CBadge>
                                <CBadge color="primary" shape="rounded-pill">학습하기</CBadge>
                              </div>
                            </div>
                          </button>
                        );
                      })}

                      <div className="text-end mt-3">
                        <button
                          className={`btn btn-sm ${
                            !isCategoryCompleted
                              ? 'btn-secondary'
                              : isPassed
                                ? 'btn-success'
                                : isSubmitted
                                  ? 'btn-warning'
                                  : 'btn-primary'
                          }`}
                          style={{
                            borderRadius: '999px',
                            border: !isCategoryCompleted
                              ? '1px solid #DDE3EA'
                              : isPassed
                                ? '1px solid #16A34A'
                                : isSubmitted
                                  ? '1px solid #F59E0B'
                                  : `1px solid ${COLORS.primary}`,
                            background: !isCategoryCompleted
                              ? '#FFFFFF'
                              : isPassed
                                ? '#16A34A'
                                : isSubmitted
                                  ? '#F59E0B'
                                  : COLORS.primary,
                            color: !isCategoryCompleted ? '#6B7280' : '#FFFFFF',
                            fontWeight: 700,
                          }}
                          disabled={!isCategoryCompleted || isPassed}
                          onClick={() => navigate(PATH.EVALUATION.QUIZ(group.category_name))}
                        >
                          {!isCategoryCompleted
                            ? '학습 완료 후 응시 가능'
                            : isPassed
                              ? '평가 통과'
                              : isSubmitted
                                ? '재응시 필요'
                                : '퀴즈 보기'}
                        </button>
                      </div>
                    </div>
                  )}
                </CCardBody>
              </CCard>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default MyRoadmap;
