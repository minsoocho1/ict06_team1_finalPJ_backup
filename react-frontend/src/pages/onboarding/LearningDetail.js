/**
 * @FileName : LearningDetail.js
 * @Description : AI 온보딩 학습 콘텐츠 상세 페이지
 *                - 콘텐츠 상세 정보 조회
 *                - 콘텐츠 유형별 렌더링 처리(VIDEO/PDF/LINK/QUIZ)
 *                - 학습 완료 처리 및 진행 상태 저장
 *                - 학습 완료 후 로드맵 화면 상태 즉시 반영
 * @Author : 김다솜
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.28    김다솜        최초 생성 및 로드맵 콘텐츠 상세 조회/타입별 렌더링 구현
 * @ 2026.04.29    김다솜        학습 완료 처리 API 연동 및 로드맵 상태 반영 로직 추가
 * @ 2026.05.06    김다솜        콘텐츠 상세 요청 URL 수정 (/content → /ai/content)
 * @ 2026.05.15    김다솜        AI 학습 도우미 및 완료 학습 재수강 안내 로직 추가, UI 개선
 * @ 2026.05.18    김다솜        AI 학습 도우미 전송 버튼과 학습 완료 버튼 겹침 방지 레이아웃 조정
 */
import {
  CButton,
  CCard,
  CCardBody,
  CCardHeader,
  CCol,
  CFormTextarea,
  CRow,
  CSpinner,
} from '@coreui/react'
import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'

import axiosInstance from 'src/api/axiosInstance'
import { useUser } from 'src/api/UserContext'
import { PATH } from 'src/constants/path'
import { userHomePageStyle } from 'src/styles/js/common/UserHomeStyle'
import { cardCore, COLORS } from 'src/styles/js/onboarding/OnboardingStyle'

const YOUTUBE_PATTERNS = [
  /(?:youtube\.com\/watch\?v=)([^&]+)/,
  /(?:youtube\.com\/embed\/)([^?&/]+)/,
  /(?:youtu\.be\/)([^?&/]+)/,
  /(?:youtube\.com\/shorts\/)([^?&/]+)/,
]

const getYoutubeEmbedUrl = (url) => {
  if (!url) return null

  for (const pattern of YOUTUBE_PATTERNS) {
    const match = url.match(pattern)
    if (match?.[1]) {
      return `https://www.youtube.com/embed/${match[1]}`
    }
  }

  return null
}

const getGoogleDrivePreviewUrl = (url) => {
  if (!url || !url.includes('drive.google.com')) return null

  const match = url.match(/\/file\/d\/([^/]+)\//)
  if (!match?.[1]) return null

  return `https://drive.google.com/file/d/${match[1]}/preview`
}

const isExternalUrl = (url) => /^https?:\/\//i.test(url || '')
const isPdfPath = (url) => /\.pdf($|\?)/i.test(url || '')
const isVideoFilePath = (url) => /\.(mp4|webm|ogg)($|\?)/i.test(url || '')

const getInlineSource = (type, path) => {
  if (!path) return { mode: 'none', src: null }

  if (type === 'VIDEO') {
    const youtubeEmbedUrl = getYoutubeEmbedUrl(path)
    if (youtubeEmbedUrl) {
      return { mode: 'iframe', src: youtubeEmbedUrl }
    }

    if (isVideoFilePath(path)) {
      return { mode: 'video', src: path }
    }

    if (path.includes('youtube.com/embed')) {
      return { mode: 'iframe', src: path }
    }
  }

  if (type === 'PDF') {
    const googleDrivePreviewUrl = getGoogleDrivePreviewUrl(path)
    if (googleDrivePreviewUrl) {
      return { mode: 'iframe', src: googleDrivePreviewUrl }
    }

    if (isPdfPath(path) || !isExternalUrl(path)) {
      return { mode: 'iframe', src: path }
    }
  }

  return { mode: 'external', src: path }
}

const formatAiText = (text) => {
  if (!text) return ''

  return text
    .replace(/\r/g, '')
    .replace(/([.!?])\s+(?=[A-Z가-힣0-9])/g, '$1\n\n')
    .replace(/(:)\s+/g, '$1\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

const createChatEntry = (type, text, meta = {}) => ({
  id: `${type}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  type,
  text,
  meta,
})

const LearningDetail = () => {
  const { contentId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { userInfo, userLoading } = useUser()

  const [content, setContent] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [assistLoading, setAssistLoading] = useState(false)
  const [assistError, setAssistError] = useState(null)
  const [questionInput, setQuestionInput] = useState('')
  const [chatHistory, setChatHistory] = useState([])
  const [selfCheck, setSelfCheck] = useState(null)
  const [selfCheckLoading, setSelfCheckLoading] = useState(false)
  const [selfCheckSaving, setSelfCheckSaving] = useState(false)
  const [selfCheckForm, setSelfCheckForm] = useState({
    understandingScore: 3,
    confidenceScore: 3,
    needMoreExplanation: false,
    memo: '',
  })
  const [alreadyCompleted, setAlreadyCompleted] = useState(Boolean(location.state?.isCompleted))
  const chatEndRef = useRef(null)

  const pageTitle = location.state?.title || '학습 상세 정보'
  const displayTitle = content?.title || pageTitle
  const itemId = location.state?.itemId
  const checklistId = location.state?.checklistId

  const inlineSource = useMemo(
    () => getInlineSource(content?.type, content?.path),
    [content?.type, content?.path],
  )

  const canUseAiAssist = ['PDF', 'LINK'].includes(content?.type)

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [chatHistory, assistLoading])

  useEffect(() => {
    setAlreadyCompleted(Boolean(location.state?.isCompleted))
  }, [location.state?.isCompleted])

  useEffect(() => {
    const fetchDetail = async () => {
      try {
        setLoading(true)
        setError(null)

        const response = await axiosInstance.get(PATH.API.ONBOARDING.CONTENT_DETAIL(contentId))

        if (!response.data || response.data.error) {
          setContent(null)
          setError(response.data?.error || '콘텐츠를 찾을 수 없습니다.')
          return
        }

        setContent(response.data)
      } catch (err) {
        console.error('[LearningDetail] detail load failed:', err)
        setContent(null)
        setError('학습 콘텐츠를 불러오는 중 오류가 발생했습니다.')
      } finally {
        setLoading(false)
      }
    }

    fetchDetail()
  }, [contentId])

  useEffect(() => {
    const fetchSelfCheck = async () => {
      const empNo = userInfo?.empNo
      if (!empNo || !contentId) {
        return
      }

      try {
        setSelfCheckLoading(true)
        const response = await axiosInstance.get(PATH.API.ONBOARDING.CONTENT_SELF_CHECK(contentId), {
          params: { empNo },
        })
        const data = response.data
        setSelfCheck(data)
        if (data?.submitted) {
          setSelfCheckForm({
            understandingScore: data.understandingScore || 3,
            confidenceScore: data.confidenceScore || 3,
            needMoreExplanation: Boolean(data.needMoreExplanation),
            memo: data.memo || '',
          })
        }
      } catch (err) {
        console.error('[LearningDetail] self-check load failed:', err)
      } finally {
        setSelfCheckLoading(false)
      }
    }

    fetchSelfCheck()
  }, [contentId, userInfo?.empNo])

  const handleCompleteLearning = async () => {
    try {
      const empNo = userInfo?.empNo

      if (!empNo) {
        alert('사용자 정보가 없습니다.')
        return
      }

      if (itemId) {
        await axiosInstance.post(PATH.API.ONBOARDING.PROGRESS_COMPLETE, {
          empNo,
          itemId,
        })
      }

      if (checklistId) {
        await axiosInstance.post(PATH.API.ONBOARDING.CHECKLIST_COMPLETE, {
          empNo,
          checklistId,
        })

        toast.success('학습 및 체크리스트 완료!', { icon: '✅' })
        setAlreadyCompleted(true)
        navigate(PATH.ONBOARDING.CHECKLIST, {
          state: { updatedChecklistId: checklistId },
        })
        return
      }

      toast.success('학습 완료!', { icon: '✅' })
      setAlreadyCompleted(true)
      navigate(PATH.ONBOARDING.ROADMAP, {
        state: { updatedItemId: itemId },
      })
    } catch (err) {
      console.error('[LearningDetail] complete failed:', err)
      alert('학습 완료 처리 중 오류가 발생했습니다.')
    }
  }

  const pushAiAnswer = (label, data) => {
    setChatHistory((prev) => [
      ...prev,
      createChatEntry('assistant', data.answer, {
        label,
        sourceTitle: data.sourceTitle,
        usedChunkCount: data.usedChunkCount,
      }),
    ])
  }

  const requestAssist = async (mode) => {
    const label = mode === 'summary' ? '핵심 요약' : '용어 정리'

    try {
      setAssistLoading(true)
      setAssistError(null)
      setChatHistory((prev) => [
        ...prev,
        createChatEntry('system', `${label} 요청`, { label }),
      ])

      const response = await axiosInstance.get(
        `${PATH.API.ONBOARDING.CONTENT_DETAIL(contentId)}/explain?mode=${mode}`,
      )
      pushAiAnswer(label, response.data)
    } catch (err) {
      console.error('[LearningDetail] ai assist failed:', err)
      setAssistError(
        typeof err?.response?.data === 'string'
          ? err.response.data
          : 'AI 학습 도우미 응답 생성 중 오류가 발생했습니다.',
      )
    } finally {
      setAssistLoading(false)
    }
  }

  const handleAskQuestion = async () => {
    const trimmedQuestion = questionInput.trim()

    if (!trimmedQuestion) {
      setAssistError('질문 내용을 입력해 주세요.')
      return
    }

    try {
      setAssistLoading(true)
      setAssistError(null)
      setChatHistory((prev) => [
        ...prev,
        createChatEntry('user', trimmedQuestion),
      ])

      const response = await axiosInstance.post(
        `${PATH.API.ONBOARDING.CONTENT_DETAIL(contentId)}/question`,
        { question: trimmedQuestion },
      )

      setQuestionInput('')
      pushAiAnswer('직접 질문 답변', response.data)
    } catch (err) {
      console.error('[LearningDetail] ask question failed:', err)
      setAssistError(
        typeof err?.response?.data === 'string'
          ? err.response.data
          : '직접 질문 답변 생성 중 오류가 발생했습니다.',
      )
    } finally {
      setAssistLoading(false)
    }
  }

  const handleQuestionKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      handleAskQuestion()
    }
  }

  const handleRetryLearning = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleSelfCheckChange = (field, value) => {
    setSelfCheckForm((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  const handleSaveSelfCheck = async () => {
    const empNo = userInfo?.empNo
    if (!empNo) {
      alert('사용자 정보가 없습니다.')
      return
    }

    try {
      setSelfCheckSaving(true)
      const response = await axiosInstance.post(PATH.API.ONBOARDING.CONTENT_SELF_CHECK(contentId), {
        empNo,
        understandingScore: Number(selfCheckForm.understandingScore),
        confidenceScore: Number(selfCheckForm.confidenceScore),
        needMoreExplanation: Boolean(selfCheckForm.needMoreExplanation),
        memo: selfCheckForm.memo,
      })
      setSelfCheck(response.data)
      toast.success('학습 이해도 자기 평가를 저장했습니다.')
    } catch (err) {
      console.error('[LearningDetail] self-check save failed:', err)
      alert('자기 평가 저장 중 오류가 발생했습니다.')
    } finally {
      setSelfCheckSaving(false)
    }
  }

  const renderLearningCompletionActions = () => (
    <div
      className="mt-4 d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3"
      style={{
        borderTop: `1px solid ${COLORS.border}`,
        paddingTop: '18px',
      }}
    >
      {alreadyCompleted ? (
        <>
          <p className="text-muted small mb-0">
            이미 학습한 콘텐츠입니다. 필요하면 다시 학습할 수 있습니다.
          </p>
          <CButton color="primary" onClick={handleRetryLearning}>
            다시 학습하기
          </CButton>
        </>
      ) : (
        <>
          <p className="text-muted small mb-0">
            학습이 끝나면 완료 버튼을 눌러 진행 상태를 반영해 주세요.
          </p>

          <CButton color="success" onClick={handleCompleteLearning} disabled={!itemId && !checklistId}>
            학습 완료
          </CButton>
        </>
      )}
    </div>
  )

  const renderScoreOptions = () => [1, 2, 3, 4, 5].map((score) => (
    <option key={score} value={score}>
      {score}점
    </option>
  ))

  const renderSelfCheckCard = () => (
    <CCard className="mt-4" style={cardCore}>
      <CCardHeader className="bg-white py-3 d-flex justify-content-between align-items-center">
        <strong>학습 이해도 자기 평가</strong>
        {selfCheckLoading && <CSpinner size="sm" color="primary" />}
      </CCardHeader>
      <CCardBody>
        <p className="text-muted small mb-3">
          학습 직후 느낀 이해도와 자신감을 남기면, 제출한 AI 평가 결과와 비교해 피드백을 제공합니다.
        </p>

        <div className="row g-3">
          <div className="col-md-6">
            <label className="form-label small fw-semibold">이해도</label>
            <select
              className="form-select"
              value={selfCheckForm.understandingScore}
              onChange={(event) => handleSelfCheckChange('understandingScore', event.target.value)}
            >
              {renderScoreOptions()}
            </select>
          </div>
          <div className="col-md-6">
            <label className="form-label small fw-semibold">자신감</label>
            <select
              className="form-select"
              value={selfCheckForm.confidenceScore}
              onChange={(event) => handleSelfCheckChange('confidenceScore', event.target.value)}
            >
              {renderScoreOptions()}
            </select>
          </div>
        </div>

        <label className="d-flex align-items-center gap-2 mt-3 small">
          <input
            type="checkbox"
            checked={selfCheckForm.needMoreExplanation}
            onChange={(event) => handleSelfCheckChange('needMoreExplanation', event.target.checked)}
          />
          추가 설명이 필요해요
        </label>

        <CFormTextarea
          className="mt-3"
          rows={3}
          placeholder="헷갈렸던 개념이나 다시 보고 싶은 부분을 적어 주세요."
          value={selfCheckForm.memo}
          onChange={(event) => handleSelfCheckChange('memo', event.target.value)}
        />

        <div className="d-flex justify-content-end mt-3">
          <CButton color="primary" onClick={handleSaveSelfCheck} disabled={selfCheckSaving}>
            {selfCheckSaving ? '저장 중...' : '자기 평가 저장'}
          </CButton>
        </div>

        {selfCheck?.feedback && (
          <div
            className="mt-3 p-3"
            style={{
              background: COLORS.softBlue,
              border: `1px solid ${COLORS.border}`,
              borderRadius: '8px',
            }}
          >
            <div className="fw-semibold mb-2">AI 평가 비교 피드백</div>
            <div className="small" style={{ lineHeight: 1.7 }}>{selfCheck.feedback}</div>
            <div className="d-flex flex-wrap gap-2 mt-2 small text-muted">
              {selfCheck.selfScoreRate != null && <span>자기 평가 {selfCheck.selfScoreRate}%</span>}
              {selfCheck.evaluationScoreRate != null && <span>AI 평가 {selfCheck.evaluationScoreRate}%</span>}
              {selfCheck.scoreGap != null && <span>차이 {selfCheck.scoreGap > 0 ? '+' : ''}{selfCheck.scoreGap}%</span>}
            </div>
          </div>
        )}
      </CCardBody>
    </CCard>
  )

  if (userLoading) {
    return (
      <div className="text-center py-5">
        <CSpinner color="primary" />
        <p>사용자 인증 확인 중입니다.</p>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="text-center py-5">
        <CSpinner color="primary" />
        <p>학습 콘텐츠를 불러오는 중입니다...</p>
      </div>
    )
  }

  if (!userLoading && !userInfo) {
    alert('로그인이 필요한 서비스입니다.')
    navigate(PATH.AUTH.LOGIN)
    return null
  }

  if (!content) {
    return (
      <div className="text-center py-5">
        <h4>{error || '콘텐츠를 찾을 수 없습니다.'}</h4>
        <CButton color="primary" onClick={() => navigate(-1)}>
          뒤로가기
        </CButton>
      </div>
    )
  }

  const openExternal = () => {
    window.open(content.path, '_blank', 'noopener,noreferrer')
  }

  const renderQuickActions = () => (
    <div>
      <div className="small text-muted mb-2">빠른 질문</div>
      <div className="d-flex flex-wrap gap-2">
        <CButton
          color="primary"
          size="sm"
          variant="outline"
          onClick={() => requestAssist('summary')}
          disabled={assistLoading}
          style={{ borderRadius: '999px', borderColor: COLORS.primary, color: COLORS.primary, fontWeight: 700 }}
        >
          핵심 요약
        </CButton>
        <CButton
          color="primary"
          size="sm"
          variant="outline"
          onClick={() => requestAssist('simple')}
          disabled={assistLoading}
          style={{ borderRadius: '999px', borderColor: COLORS.border, color: COLORS.primary, fontWeight: 700 }}
        >
          용어 정리
        </CButton>
      </div>
      {!chatHistory.length && !assistLoading && !assistError && (
        <div className="text-muted small mt-2" style={{ lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>
          {formatAiText(
            '핵심 요약이나 용어 정리를 먼저 눌러 보고, 바로 아래 입력창에서 문서 기반 질문을 이어서 해보세요.',
          )}
        </div>
      )}
    </div>
  )

  const renderChatHistory = () => (
    <div
      style={{
        maxHeight: '460px',
        overflowY: 'auto',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
        paddingRight: '4px',
      }}
    >
      {chatHistory.map((entry) => {
        const isUser = entry.type === 'user'
        const isAssistant = entry.type === 'assistant'
        const isSystem = entry.type === 'system'

        return (
          <div
            key={entry.id}
            style={{
              alignSelf: isUser ? 'flex-end' : 'stretch',
              maxWidth: isUser ? '88%' : '100%',
            }}
          >
            <div
              style={{
                background: isUser ? COLORS.primary : isAssistant ? '#F4F7FB' : COLORS.softBlue,
                color: isUser ? '#fff' : '#2f3640',
                borderRadius: isUser ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
                padding: isSystem ? '8px 12px' : '12px 14px',
                border: isAssistant ? `1px solid ${COLORS.border}` : 'none',
                whiteSpace: 'pre-wrap',
                lineHeight: 1.75,
                fontSize: '0.94rem',
              }}
            >
              {isAssistant && (
                <div className="small text-muted mb-2">
                  {entry.meta?.label || 'AI 답변'}
                  {entry.meta?.sourceTitle ? ` · ${entry.meta.sourceTitle}` : ''}
                  {entry.meta?.usedChunkCount ? ` · 참고 청크 ${entry.meta.usedChunkCount}개` : ''}
                </div>
              )}
              {isSystem ? entry.text : formatAiText(entry.text)}
            </div>
          </div>
        )
      })}

      {assistLoading && (
        <div
          style={{
            background: '#F4F7FB',
            borderRadius: '16px 16px 16px 4px',
            padding: '12px 14px',
            border: `1px solid ${COLORS.border}`,
          }}
        >
          <CSpinner size="sm" color="primary" />
          <div className="small text-muted mt-2">문서 내용을 바탕으로 답변을 생성하는 중입니다.</div>
        </div>
      )}

      <div ref={chatEndRef} />
    </div>
  )

  const renderAiAssistCard = () => {
    if (!canUseAiAssist) return null

    return (
      <CCard className="h-100" style={cardCore}>
        <CCardHeader className="bg-white py-3">
          <div className="d-flex justify-content-between align-items-center gap-2">
            <strong>AI 학습 도우미</strong>
            <span className="small text-muted">문서 기반 대화</span>
          </div>
        </CCardHeader>
        <CCardBody
          className="d-flex flex-column"
          style={{
            minHeight: '560px',
            paddingBottom: '12px',
          }}
        >
          {assistError && !assistLoading && (
            <div className="text-danger small mb-3">{assistError}</div>
          )}

          <div className="flex-grow-1 mb-3">
            {renderChatHistory()}
          </div>

          <div
            style={{
              borderTop: `1px solid ${COLORS.border}`,
              paddingTop: '12px',
              marginTop: 'auto',
            }}
          >
            <div className="small fw-semibold mb-2">직접 질문하기</div>
            <div className="mb-3">
              {renderQuickActions()}
            </div>
            <CFormTextarea
              rows={3}
              placeholder="이 문서에서 중요한 절차는 무엇인가요?처럼 궁금한 점을 입력해 보세요."
              value={questionInput}
              onChange={(event) => setQuestionInput(event.target.value)}
              onKeyDown={handleQuestionKeyDown}
              disabled={assistLoading}
              style={{ resize: 'none' }}
            />
            <div className="d-flex justify-content-end mt-2" style={{ position: 'relative', zIndex: 1 }}>
              <CButton color="dark" size="sm" onClick={handleAskQuestion} disabled={assistLoading}>
                전송
              </CButton>
            </div>
          </div>
        </CCardBody>
      </CCard>
    )
  }

  return (
    <div style={userHomePageStyle}>
      <header
        style={{
          marginBottom: '20px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <div>
          <small className="text-muted">Onboarding Step {contentId}</small>
          <h2 style={{ fontWeight: 'bold' }}>{displayTitle}</h2>
        </div>
        <CButton color="secondary" variant="outline" onClick={() => navigate(PATH.ONBOARDING.ROADMAP)}>
          목록으로
        </CButton>
      </header>

      <CRow className="g-4">
        {canUseAiAssist && (
          <CCol lg={4} className="order-1 order-lg-2">
            <div style={{ position: 'sticky', top: '90px' }}>
              {renderAiAssistCard()}
            </div>
          </CCol>
        )}

        <CCol lg={canUseAiAssist ? 8 : 12} className="order-2 order-lg-1">
          <CCard style={cardCore}>
            <CCardHeader className="bg-white py-3 d-flex justify-content-between align-items-center">
              <strong>{content.type} 학습 모드</strong>
              {content.path && inlineSource.mode === 'external' && (
                <CButton color="primary" size="sm" style={{ borderRadius: '999px', background: COLORS.primary, borderColor: COLORS.primary, fontWeight: 700 }} onClick={openExternal}>
                  새 창에서 열기
                </CButton>
              )}
            </CCardHeader>
            <CCardBody>
              {!content.path ? (
                <div className="py-5 text-center">
                  <p>준비된 학습 자료가 없습니다. 관리자에게 문의해 주세요.</p>
                </div>
              ) : (
                <div>
                  {content.type === 'VIDEO' && (
                    <>
                      {inlineSource.mode === 'iframe' && (
                        <div className="ratio ratio-16x9">
                          <iframe
                            src={inlineSource.src}
                            title="Video content"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                            allowFullScreen
                            style={{ border: 0, borderRadius: '8px' }}
                          />
                        </div>
                      )}

                      {inlineSource.mode === 'video' && (
                        <video controls style={{ width: '100%', borderRadius: '8px', background: '#000' }}>
                          <source src={inlineSource.src} />
                        </video>
                      )}

                      {inlineSource.mode === 'external' && (
                        <div className="text-center py-5">
                          <h5>영상 콘텐츠를 새 창에서 확인해 주세요.</h5>
                          <p className="text-muted small">{content.path}</p>
                          <CButton color="primary" style={{ borderRadius: '999px', background: COLORS.primary, borderColor: COLORS.primary, fontWeight: 700 }} onClick={openExternal}>
                            영상 열기
                          </CButton>
                        </div>
                      )}
                    </>
                  )}

                  {content.type === 'PDF' && (
                    <>
                      {inlineSource.mode === 'iframe' && (
                        <div style={{ height: '820px' }}>
                          <iframe
                            src={inlineSource.src}
                            title="PDF content"
                            width="100%"
                            height="100%"
                            style={{ border: 0, borderRadius: '8px' }}
                          />
                        </div>
                      )}

                      {inlineSource.mode === 'external' && (
                        <div className="text-center py-5">
                          <h5>PDF 자료를 새 창에서 확인해 주세요.</h5>
                          <p className="text-muted small">{content.path}</p>
                          <CButton color="primary" style={{ borderRadius: '999px', background: COLORS.primary, borderColor: COLORS.primary, fontWeight: 700 }} onClick={openExternal}>
                            PDF 열기
                          </CButton>
                        </div>
                      )}
                    </>
                  )}

                  {content.type === 'LINK' && (
                    <div className="text-center py-5">
                      <div className="mb-4">
                        <i className="cil-external-link" style={{ fontSize: '3rem', color: COLORS.primary }} />
                      </div>
                      <h5>이 콘텐츠는 외부 사이트에서 제공됩니다.</h5>
                      <p className="text-muted">아래 버튼을 눌러 학습을 계속해 주세요.</p>
                      <CButton color="primary" size="lg" style={{ borderRadius: '999px', background: COLORS.primary, borderColor: COLORS.primary, fontWeight: 700 }} onClick={openExternal}>
                        외부 사이트로 이동
                      </CButton>
                    </div>
                  )}

                  {content.type === 'QUIZ' && (
                    <div className="text-center py-5">
                      <h5>퀴즈 콘텐츠입니다.</h5>
                      <p className="text-muted">아래 버튼을 눌러 퀴즈를 시작해 주세요.</p>
                      <CButton color="primary" size="lg" style={{ borderRadius: '999px', background: COLORS.primary, borderColor: COLORS.primary, fontWeight: 700 }} onClick={() => navigate(PATH.EVALUATION.QUIZ)}>
                        퀴즈 시작하기
                      </CButton>
                    </div>
                  )}

                  {!['VIDEO', 'PDF', 'LINK', 'QUIZ'].includes(content.type) && (
                    <div className="py-5 text-center">
                      <p>지원하지 않는 콘텐츠 유형입니다.</p>
                    </div>
                  )}
                </div>
              )}
            </CCardBody>
          </CCard>

          {renderSelfCheckCard()}

          {renderLearningCompletionActions()}
        </CCol>
      </CRow>
    </div>
  )
}

export default LearningDetail
