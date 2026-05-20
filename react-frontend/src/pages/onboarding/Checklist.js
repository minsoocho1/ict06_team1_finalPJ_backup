/**
 * @FileName : Checklist.js
 * @Description : AI 온보딩 > 체크리스트 화면
 *                - 체크리스트 목록 조회
 *                - 체크리스트 완료 처리
 *                - 개인별 진행 상태 표시
 * @Author : 김다솜
 * @Date : 2026. 04. 29
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    김다솜        최초 생성 및 체크리스트 조회/완료 기능 구현
 * @ 2026.05.08    김다솜        체크리스트 완료 변경 시 홈/요약 카드 갱신 이벤트 추가
 * @ 2026.05.15    김다솜        스타일 수정
 */

import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import axiosInstance from 'src/api/axiosInstance';
import { useUser } from 'src/api/UserContext';
import { PATH } from 'src/constants/path';
import {
    actionButtonPending,
    actionButtonPrimary,
    actionButtonSuccess,
    checkboxBox,
    checklistContainer,
    checklistEyebrow,
    checklistList,
    checklistPageDesc,
    checklistPageHeader,
    checklistPageTitle,
    checklistRow,
    checklistRowAction,
    checklistRowMeta,
    checklistRowTitle,
    checklistSectionCount,
    checklistSectionHeader,
    checklistSectionTitle,
    emptyChecklistCard,
    mandatoryBadge,
    optionalBadge,
    progressFill,
    progressMetaGrid,
    progressMetaItem,
    progressMetaLabel,
    progressMetaValue,
    progressSummaryCard,
    progressSummaryLabel,
    progressSummaryTop,
    progressSummaryValue,
    progressTrack,
} from 'src/styles/js/onboarding/ChecklistStyle';

const Checklist = () => {

    const navigate = useNavigate();
    const location = useLocation();

    const { userInfo } = useUser();
    const [checklist, setChecklist] = useState([]);
    const [loading, setLoading] = useState(true);

    //체크리스트 조회
    useEffect(() => {
        const fetchChecklist = async () => {
            const empNo = userInfo?.empNo;

            if (!empNo) {
                console.warn("[Checklist] empNo 없음:", userInfo);
                setLoading(false);
                return;
            }

            try {
                const res = await axiosInstance.get(
                    PATH.API.ONBOARDING.CHECKLIST_LIST(empNo)
                );
                setChecklist(res.data);
            } catch (err) {
                console.error("체크리스트 조회 실패", err);
            } finally {
                setLoading(false);
            }
        };

        fetchChecklist();
    }, [userInfo]);

    //학습 상세페이지에서 체크리스트 완료 후 돌아왔을 때
    //전달받은 updatedChecklistId에 해당하는 항목을 즉시 완료 상태로 변경
    useEffect(() => {
        const updatedChecklistId = location.state?.updatedChecklistId;

        if (!updatedChecklistId) return;

        setChecklist((prev) =>
            prev.map((item) =>
                Number(item.checklistId) === Number(updatedChecklistId)
                    ? { ...item, status: 'COMPLETED' }
                    : item
            )
        );

        //state 반복 적용 방지
        navigate(location.pathname, { replace: true, state: {} });
    }, [location.state, location.pathname, navigate]);

    //완료 처리
    const handleComplete = async (checklistId) => {
        try {
            await axiosInstance.post(PATH.API.ONBOARDING.CHECKLIST_COMPLETE, {
                empNo: userInfo.empNo,
                checklistId
            });

            //즉시 UI 반영
            setChecklist(prev =>
                prev.map(item =>
                    item.checklistId === checklistId
                        ? { ...item, status: 'COMPLETED' }
                        : item
                )
            );
            window.dispatchEvent(new Event('onboardingProgressUpdated'));
        } catch (err) {
            console.error("완료 처리 실패", err);
            alert(err.response?.data || "완료 처리 중 오류가 발생했습니다.");
        }
    };

    //미완료 처리
    const handleUncomplete = async (item) => {
        if (item.evaluationSubmitted) {
            alert("평가를 마친 항목입니다.");
            return;
        }

        try {
            await axiosInstance.post(PATH.API.ONBOARDING.CHECKLIST_UNCOMPLETE,
                {
                    empNo: userInfo.empNo,
                    checklistId: item.checklistId
            });

            setChecklist(prev =>
                prev.map(prevItem =>
                    prevItem.checklistId === item.checklistId
                        ? { ...prevItem, status: 'NOT_STARTED' }
                        : prevItem
                )
            );
            window.dispatchEvent(new Event('onboardingProgressUpdated'));
        } catch (err) {
            console.error("완료 취소 실패", err);
            alert(err.response?.data || "완료 취소 중 오류가 발생했습니다.");
        }
    };

    if (loading)
        return <div>Loading...</div>;

    /**
     * 체크리스트 전체 진행률 계산
     * 
     * :param {Array} items - 체크리스트 목록
     * :returns {{ completed: number, total: number, percent: number }}
     */
    const calculateProgress = (items = []) => {
        const total = items.length;
        const completed = items.filter((item) => item.status === 'COMPLETED').length;
        const percent = total === 0 ? 0 : Math.round((completed / total) * 100);

        return { completed, total, percent };
    };

    const progress = calculateProgress(checklist);
    const pendingItems = checklist.filter((item) => item.status !== 'COMPLETED');
    const completedItems = checklist.filter((item) => item.status === 'COMPLETED');
    const mandatoryTotal = checklist.filter((item) => item.isMandatory).length;
    const mandatoryCompleted = checklist.filter((item) => item.isMandatory && item.status === 'COMPLETED').length;

    const renderChecklistRow = (item) => {
        const isCompleted = item.status === 'COMPLETED';
        const hasRelatedContent = Boolean(item.relatedContentId);

        const moveToLearning = () => {
            if (!hasRelatedContent) return;

            const confirmed = window.confirm("학습 콘텐츠로 이동하시겠습니까?");
            if (!confirmed) return;

            navigate(PATH.ONBOARDING.LEARNING(item.relatedContentId), {
                state: {
                    title: item.relatedContentTitle || item.title,
                    checklistId: item.checklistId,
                    userInfo,
                    isCompleted,
                }
            });
        };

        const handleRowClick = () => {
            if (hasRelatedContent) {
                moveToLearning();
            }
        };

        const handleCheckboxClick = (event) => {
            event.stopPropagation();

            if (hasRelatedContent) {
                moveToLearning();
                return;
            }

            if (isCompleted) {
                handleUncomplete(item);
            } else {
                handleComplete(item.checklistId);
            }
        };

        return (
            <div
                key={item.checklistId}
                className={`checklist-card ${item.isMandatory ? 'mandatory' : ''}`}
                style={checklistRow(isCompleted)}
                onClick={handleRowClick}
            >
                <button
                    type="button"
                    style={{
                        ...checkboxBox(isCompleted),
                        cursor: 'pointer',
                    }}
                    onClick={handleCheckboxClick}
                    title={isCompleted ? '완료 취소' : '완료 처리'}
                >
                    {isCompleted ? '✓' : ''}
                </button>

                <div style={{ minWidth: 0 }}>
                    <div style={checklistRowTitle(isCompleted)}>{item.title}</div>
                    <div style={checklistRowMeta}>
                        <span>{item.category}</span>
                        <span style={item.isMandatory ? mandatoryBadge : optionalBadge}>
                            {item.isMandatory ? '필수' : '선택'}
                        </span>
                    </div>
                </div>

                <div style={checklistRowAction}>
                    {isCompleted ? (
                        <button
                            className='checklist-action-btn'
                            style={actionButtonPending}
                            onClick={(event) => {
                                event.stopPropagation();
                                handleUncomplete(item);
                            }}
                        >
                            완료 취소
                        </button>
                    ) : item.relatedContentId ? (
                        <button
                            className='checklist-action-btn'
                            style={actionButtonPrimary}
                            onClick={(event) => {
                                event.stopPropagation();
                                moveToLearning();
                            }}
                        >
                            학습 보기
                        </button>
                    ) : (
                        <button
                            className='checklist-action-btn success'
                            style={actionButtonSuccess}
                            onClick={(event) => {
                                event.stopPropagation();
                                handleComplete(item.checklistId);
                            }}
                        >
                            완료
                        </button>
                    )}
                </div>
            </div>
        );
    };

    return (
        <div style={checklistContainer}>
            <div style={checklistPageHeader}>
                <div>
                    <p style={checklistEyebrow}>AI ONBOARDING</p>
                    <h2 style={checklistPageTitle}>온보딩 체크리스트</h2>
                    <p style={checklistPageDesc}>
                        입사 초기에 확인해야 할 항목을 한곳에서 관리합니다. 관련 학습이 있는 항목은 학습 상세 화면으로 바로 이동할 수 있습니다.
                    </p>
                </div>
            </div>

            <div style={progressSummaryCard}>
                <div style={progressSummaryTop}>
                    <div>
                        <div style={progressSummaryLabel}>전체 진행률</div>
                        <div style={checklistPageDesc}>
                            {progress.completed}/{progress.total}개 완료
                        </div>
                    </div>
                    <div style={progressSummaryValue}>{progress.percent}%</div>
                </div>

                <div style={progressTrack}>
                    <div style={progressFill(progress.percent)} />
                </div>

                <div style={progressMetaGrid}>
                    <div style={progressMetaItem}>
                        <span style={progressMetaLabel}>남은 항목</span>
                        <span style={progressMetaValue}> {pendingItems.length}개</span>
                    </div>
                    <div style={progressMetaItem}>
                        <span style={progressMetaLabel}>필수 완료</span>
                        <span style={progressMetaValue}> {mandatoryCompleted}/{mandatoryTotal}</span>
                    </div>
                </div>
            </div>

            <div style={checklistSectionHeader}>
                <h3 style={checklistSectionTitle}>진행할 항목</h3>
                <span style={checklistSectionCount}>{pendingItems.length}개</span>
            </div>

            {pendingItems.length === 0 ? (
                <div style={emptyChecklistCard}>진행할 체크리스트가 없습니다. 모든 항목을 완료했습니다.</div>
            ) : (
                <div style={checklistList}>
                    {pendingItems.map(renderChecklistRow)}
                </div>
            )}

            <div style={checklistSectionHeader}>
                <h3 style={checklistSectionTitle}>완료한 항목</h3>
                <span style={checklistSectionCount}>{completedItems.length}개</span>
            </div>

            {completedItems.length > 0 && (
                <div style={checklistList}>
                    {completedItems.map(renderChecklistRow)}
                </div>
            )}
        </div>
    );
};
export default Checklist;
