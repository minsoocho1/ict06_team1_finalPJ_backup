/**
 * @FileName : LoginPage.js
 * @Description : 로그인 페이지
 *                - 사번/비밀번호 입력 및 인증 처리
 *                - 로그인 성공 시 사용자 정보 Context 저장 후 웰컴 페이지 이동
 * @Author : 김다솜
 * @Date : 2026. 04. 16
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.16    김다솜        최초 생성
 * @ 2026.04.30    김다솜        스타일 코드 분리(LoginStyle.js) 및 버튼 상태 처리 개선
 * @ 2026.05.07    김다솜        role 타입/형식 정규화 및 관리자 로그인 브릿지용 sessionStorage(adminLoginBridge) 저장 추가
 * @ 2026.05.15    김다솜        사용자 홈 톤에 맞춘 로그인 화면 레이아웃 및 스타일 개편
 */

import { useEffect, useState } from 'react'
import axios from 'axios'
import { useNavigate } from 'react-router-dom'
import { useUser } from '../../api/UserContext'
import {
  brandEyebrow,
  brandFeatureCard,
  brandFeatureGrid,
  brandFeatureLabel,
  brandFeatureValue,
  brandPanel,
  brandSubtitle,
  brandTitle,
  cardBadge,
  cardDescription,
  cardStyle,
  cardTitle,
  containerStyle,
  helperLink,
  inputStyle,
  loginButton,
  pageShell,
} from 'src/styles/js/auth/LoginStyle'
import { PATH } from 'src/constants/path'
import axiosInstance from 'src/api/axiosInstance'

const normalizeRole = (roleValue) => {
  if (typeof roleValue === 'string') return roleValue.toUpperCase()
  if (Array.isArray(roleValue) && roleValue.length > 0) return normalizeRole(roleValue[0])
  if (roleValue && typeof roleValue === 'object') {
    const candidate = roleValue.roleName || roleValue.authority || roleValue.name
    return typeof candidate === 'string' ? candidate.toUpperCase() : ''
  }
  return ''
}

function LoginPage() {
  const navigate = useNavigate()
  const { login, updateUserInfo } = useUser()

  const [loginData, setLoginData] = useState({
    empNo: '',
    password: '',
  })
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    setLoginData({
      empNo: '',
      password: '',
    })
  }, [])

  const handleChange = (e) => {
    setLoginData({
      ...loginData,
      [e.target.name]: e.target.value,
    })
  }

  const handleLogin = async (e) => {
    e.preventDefault()
    setIsLoading(true)
    setError('')

    try {
      const response = await axios.post(`${PATH.API.BASE}/auth/login`, {
        empNo: loginData.empNo,
        password: loginData.password,
      })

      const { accessToken, refreshToken, empNo, userName, role } = response.data
      const normalizedRole = normalizeRole(role)

      login({ empNo, name: userName, role: normalizedRole || role }, accessToken, refreshToken)

      if (normalizedRole !== 'ROLE_ADMIN') {
        const empResponse = await axiosInstance.get(PATH.API.USER_ME)
        updateUserInfo(empResponse.data)
      } else {
        updateUserInfo({ empNo, name: userName, role: normalizedRole || role })
        sessionStorage.setItem(
          'adminLoginBridge',
          JSON.stringify({
            username: loginData.empNo,
            password: loginData.password,
            createdAt: Date.now(),
          }),
        )
      }

      navigate(PATH.AUTH.WELCOME)
    } catch (err) {
      if (!err.response) {
        setError('서버에 연결할 수 없습니다.')
      } else {
        const message = err.response.data
        setError(typeof message === 'string' ? message : '사번 또는 비밀번호를 확인해 주세요.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  const handleFindCredentials = () => {
    alert('임시메시지: 사번과 비밀번호 찾기 기능은 관리자에게 문의해 주세요.')
  }

  return (
    <div style={containerStyle}>
      <div style={pageShell}>
        <div style={brandPanel}>
          <div style={brandEyebrow}>AI-BASED GROUPWARE</div>
          <h1 style={brandTitle}>
            COREWORK에
            <br />
            오신 것을 환영합니다!
          </h1>
          <p style={brandSubtitle}>
            온보딩, 학습, 평가, 협업 흐름을 하나의 공간에서 이어가는 그룹웨어
          </p>

          <div style={brandFeatureGrid}>
            <div style={brandFeatureCard}>
              <div style={brandFeatureLabel}>ONBOARDING</div>
              <div style={brandFeatureValue}>학습, 체크리스트, 평가 흐름 연결</div>
            </div>
            <div style={brandFeatureCard}>
              <div style={brandFeatureLabel}>AI SUPPORT</div>
              <div style={brandFeatureValue}>문서 요약, 재설명, 질문 응답 지원</div>
            </div>
            <div style={brandFeatureCard}>
              <div style={brandFeatureLabel}>HOME FEED</div>
              <div style={brandFeatureValue}>오늘의 업무와 진행 상태 한 번에 확인</div>
            </div>
          </div>
        </div>

        <div style={cardStyle}>
          <div style={cardBadge}>LOGIN</div>
          <h2 style={cardTitle}>업무 시작하기</h2>
          <p style={cardDescription}>
            사번과 비밀번호를 입력해주세요.
          </p>

          <form onSubmit={handleLogin}>
            <input
              type="text"
              name="empNo"
              placeholder="사번"
              value={loginData.empNo}
              onChange={handleChange}
              style={inputStyle}
              required
              autoComplete="off"
            />
            <input
              type="password"
              name="password"
              placeholder="비밀번호"
              value={loginData.password}
              onChange={handleChange}
              style={inputStyle}
              required
              autoComplete="new-password"
            />
            {error && <p style={{ color: '#d93025', fontSize: '14px', marginBottom: '16px' }}>{error}</p>}
            <button type="submit" style={loginButton(isLoading)} disabled={isLoading}>
              {isLoading ? '로그인 중...' : '로그인'}
            </button>
          </form>

          <div style={{ marginTop: '24px', textAlign: 'center' }}>
            <span
              onClick={handleFindCredentials}
              style={helperLink}
              onMouseOver={(e) => (e.target.style.color = '#321fdb')}
              onMouseOut={(e) => (e.target.style.color = '#6b7280')}
            >
              사번/비밀번호 찾기
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoginPage
