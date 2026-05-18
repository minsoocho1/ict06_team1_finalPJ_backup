/**
 * @FileName : QuizForm.js
 * @Description : 퀴즈 응시 화면
 *                - 카테고리별 퀴즈 문항 조회 및 렌더링
 *                - 객관식/주관식 답변 입력 처리
 *                - SpringBoot-FastAPI 연동을 통한 AI 자동 채점 및 피드백 제공
 * @Author : 김다솜
 * @Date : 2026. 05. 06
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.06    김다솜        Quiz.js에서 퀴즈 응시 컴포넌트 분리,
 *                              AI 채점 결과(유사도, 피드백) 연동 및 시각화 로직 추가,
 *                              AI 평가 결과 UI 개선(점수와 유사도 강조, 피드백 구분)
 * @ 2026.05.07    김다솜        AI 유사도 스케일(0~1 vs 0~100) 정규화하여 % 표시 수정, aiFeedbackBox 참조 오류(import 누락) 수정
 * @ 2026.05.08    김다솜        중복 제출 차단 응답 메시지 표시 추가
 */

import React, { useEffect, useState } from 'react';
import axiosInstance from 'src/api/axiosInstance';
import { useUser } from 'src/api/UserContext';
import { PATH } from 'src/constants/path';
import QuizResult from './QuizResult';
import {
    quizContainer, quizHeader, categoryText,
    questionGrid, questionCard, questionTitle,
    optionLabel, optionInput, resultBox, resultText,
    explanationText, submitArea, submitButton,
    disabledButton, answerCountText,
    aiResultContainer,
    aiResultHeader,
    aiScoreText,
    similarityBadge,
    aiFeedbackBox
} from 'src/styles/js/evaluation/QuizStyle';

// AI similarity 정규화: (0~100)% 스케일
const toSimilarityPercent = (raw) => {
    const value = typeof raw === 'string' ? parseFloat(raw) : Number(raw);
    if (Number.isNaN(value)) return 0;
    return value <= 1 ? value * 100 : value;
};


const QuizForm = ({ categoryName }) => {
    const { userInfo } = useUser();

    const [questions, setQuestions] = useState([]);
    const [answers, setAnswers] = useState({});
    const [results, setResults] = useState({});
    const [loading, setLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitResult, setSubmitResult] = useState(null);
    const [submitError, setSubmitError] = useState(null);

    // 퀴즈 문항 조회
    useEffect(() => {
        const fetchQuiz = async () => {
            try {
                console.log("[Quiz] categoryName:", categoryName);
                const res = await axiosInstance.get(
                    PATH.API.EVALUATION.QUIZ_QUESTIONS(categoryName)
                );
                console.log("[Quiz] fetched questions:", res.data);

                if (Array.isArray(res.data)) {
                    setQuestions(res.data);
                } else if (Array.isArray(res.data.questions)) {
                    setQuestions(res.data.questions);
                } else {
                    console.warn("[Quiz] 퀴즈 응답이 배열이 아님:", res.data);
                    setQuestions([]);
                }
            } catch (err) {
                console.error("퀴즈 조회 실패", err);
            } finally {
                setLoading(false);
            }
        };

        fetchQuiz();
    }, [categoryName]);

    // 객관식 답변 선택
    const handleSelect = (questionId, selectedNo) => {
        setAnswers(prev => ({
            ...prev,
            [questionId]: { questionId, selectedNo, answerText: null }
        }));
    };

    // 주관식(단답형/서술형) 답변 입력
    const handleTextAnswer = (questionId, text) => {
        setAnswers((prev) => ({
            ...prev,
            [questionId]: { questionId, selectedNo: null, answerText: text }
        }));
    };

    // 전체 응답 완료 여부
    const isAllAnswered = () => {
        return questions.every((q) => {
            const answer = answers[q.questionId];

            if (!answer) return false;
            if (q.questionType === 'MULTIPLE_CHOICE') {
                return answer.selectedNo !== null && answer.selectedNo !== undefined;
            }

            return answer.answerText && answer.answerText.trim() !== '';
        });
    };

    // 응답 전체 제출
    const handleSubmit = async () => {
        setIsSubmitting(true);
        setSubmitError(null);
        const payload = {
            empNo: userInfo.empNo,
            categoryName,
            answers: Object.values(answers)
        };

        console.log("[Quiz] submit payload:", payload);

        try {
            const res = await axiosInstance.post(PATH.API.EVALUATION.QUIZ_SUBMIT, payload);
            console.log("[Quiz] submit response:", res.data);

            setSubmitResult(res.data);

            const resultMap = {};
            res.data.results.forEach(r => {
                resultMap[r.questionId] = r;
            });

            setResults(resultMap);
        } catch (err) {
            console.error("퀴즈 제출 실패", err);
            setSubmitError(err.response?.data || '퀴즈 제출에 실패했습니다.');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (loading)
        return <div>Loading...</div>;

    // 로드맵 > 퀴즈 응시 화면
    return (
        <div style={quizContainer}>
            <div style={quizHeader}>
                <h2>📝 AI 온보딩 평가</h2>
                <div style={categoryText}>카테고리: {categoryName}</div>
            </div>

            {!Array.isArray(questions) || questions.length === 0 ? (
                <div>등록된 퀴즈가 없습니다.</div>
            ) : (
                <>
                    <div style={questionGrid}>
                        {questions.map((q, index) => {
                            const questionResult = results[q.questionId];

                            return (

                                <div key={q.questionId} style={questionCard}>
                                    <div style={questionTitle}>
                                        {index + 1}. {q.questionText}
                                    </div>

                                    {/* 문항 타입 분기(주관식-단답형/서술형) */}
                                    {q.questionType === 'SHORT_ANSWER' || q.questionType === 'ESSAY' ? (
                                        <textarea
                                            value={answers[q.questionId]?.answerText || ''}
                                            onChange={(e) =>
                                                handleTextAnswer(q.questionId, e.target.value)
                                            }
                                            style={{
                                                width: '100%',
                                                height: '80px',
                                                marginTop: '10px'
                                            }}
                                            placeholder='답변을 입력하세요'
                                        />
                                    ) : (
                                        [1, 2, 3, 4].map((no) => {
                                            const option = q[`option${no}`];

                                            if (!option) return null;

                                            return (
                                                <label key={no} style={optionLabel}>
                                                    <input
                                                        type='radio'
                                                        name={`question-${q.questionId}`}
                                                        value={no}
                                                        checked={answers[q.questionId]?.selectedNo === no}
                                                        onChange={() => handleSelect(q.questionId, no)}
                                                        style={optionInput}
                                                    />
                                                    {option}
                                                </label>
                                            );
                                        })
                                    )}

                                    {/* 문항별 평가 결과 */}
                                    {results[q.questionId] && (
                                        <div style={resultBox}>
                                            {results[q.questionId].isCorrect === true ? (
                                                <span className='badge bg-success'>정답</span>
                                            ) : questionResult.isCorrect === false ? (
                                                <span className='badge bg-danger'>오답</span>
                                            ) : (
                                                <span className='badge bg-primary'>AI 평가</span>
                                            )}

                                            <div style={resultText}>
                                                점수: {results[q.questionId].score}점
                                            </div>

                                            {results[q.questionId].explanation && (
                                                <div style={explanationText}>
                                                    해설: {results[q.questionId].explanation}
                                                </div>
                                            )}

                                            {/* AI 평가 결과 */}
                                            {(results[q.questionId].aiScore !== null &&
                                                results[q.questionId].aiScore !== undefined) && (
                                                    <div style={aiResultContainer}>
                                                        <div style={aiResultHeader}>
                                                            <span>🤖</span> AI 평가 결과
                                                        </div>

                                                        <div style={aiScoreText}>
                                                            AI 점수: {results[q.questionId].aiScore}점
                                                        </div>
                                                        <div style={aiScoreText}>
                                                            답변 유사도:
                                                            <span
                                                                style={similarityBadge(
                                                                    toSimilarityPercent(results[q.questionId].similarityScore)
                                                                )}
                                                            >
                                                                {Math.round(
                                                                    toSimilarityPercent(results[q.questionId].similarityScore)
                                                                )}
                                                                %
                                                            </span>
                                                        </div>

                                                        {results[q.questionId].aiFeedback && (
                                                            <div style={aiFeedbackBox}>
                                                                <strong>Feedback:</strong> {results[q.questionId].aiFeedback}
                                                            </div>
                                                        )}
                                                    </div>
                                                )}
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>

                    {/* 응답 제출 버튼 */}
                    <div style={submitArea}>
                        <button
                            style={
                                !isAllAnswered() || isSubmitting || submitResult !== null
                                    ? disabledButton
                                    : submitButton
                            }
                            disabled={!isAllAnswered() || isSubmitting || submitResult !== null}
                            onClick={handleSubmit}
                        >
                            {isSubmitting ? '🤖 AI가 답변을 평가 중입니다...' : '전체 제출'}
                        </button>

                        <div style={answerCountText}>
                            {Object.keys(answers).length} / {questions.length} 문항 응답 완료
                        </div>

                        {submitError && (
                            <div className='text-danger mt-2'>
                                {submitError}
                            </div>
                        )}
                    </div>

                    {/* 평가 결과 화면 연동 */}
                    {submitResult && (
                        <QuizResult
                            submitResult={submitResult}
                            onRetry={() => {
                                setAnswers({});
                                setResults({});
                                setSubmitResult(null);
                            }}
                        />
                    )}
                </>
            )}
        </div>
    );
};

export default QuizForm;
