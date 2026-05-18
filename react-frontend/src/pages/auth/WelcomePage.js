/**
 * @FileName : WelcomePage.js
 * @Description : 로그인 성공 후 진입하는 웰컴 페이지
 *                - 사용자 기본 정보 표시
 *                - 대시보드 이동 및 로그아웃 기능 제공
 * @Author : 김다솜
 * @Date : 2026. 04. 17
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.17    김다솜        최초 생성
 * @ 2026.04.30    김다솜        스타일 코드 분리(LoginStyle.js) 및 UI 정리
 * @ 2026.05.07    김다솜        관리자도 동일 로그인 페이지 사용하도록, 대시보드 입장 시 백엔드 세션 로그인(/admin/login-process) 브릿지 처리 추가
 * @ 2026.05.08    김다솜        관리자 세션 브릿지 실패 시 공용 로그인 페이지로 이동하도록 수정
 * @ 2026.05.15    김다솜        사용자 홈 톤에 맞춘 웰컴 화면 스타일 개편
 */

import React from 'react'
import { useNavigate } from 'react-router-dom'
import { useUser } from 'src/api/UserContext'
import { PATH } from 'src/constants/path'
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
  containerStyle,
  pageShell,
  primaryButton,
  secondaryButton,
  userInfoBox,
  userInfoRow,
  welcomeCard,
  welcomeTitle,
} from 'src/styles/js/auth/LoginStyle'

const normalizeRole = (roleValue) => {
  if (typeof roleValue === 'string') return roleValue.toUpperCase()
  if (Array.isArray(roleValue) && roleValue.length > 0) return normalizeRole(roleValue[0])
  if (roleValue && typeof roleValue === 'object') {
    const candidate = roleValue.roleName || roleValue.authority || roleValue.name
    return typeof candidate === 'string' ? candidate.toUpperCase() : ''
  }
  return ''
}

function WelcomePage() {
  const navigate = useNavigate()
  const { userInfo, logout } = useUser()

  const handleLogout = () => {
    logout()
    navigate(PATH.AUTH.LOGIN)
  }

  const handleEntry = () => {
    if (!userInfo || !userInfo.role) {
      alert('사용자 권한을 확인할 수 없습니다. 다시 로그인해 주세요.')
      return
    }

    const userRole = normalizeRole(userInfo.role)

    if (userRole === 'ROLE_ADMIN') {
      const serverOrigin = PATH.API.BASE.replace('/api', '')
      const bridgeRaw = sessionStorage.getItem('adminLoginBridge')

      if (bridgeRaw) {
        try {
          const bridge = JSON.parse(bridgeRaw)
          const isBridgeValid =
            bridge?.username &&
            bridge?.password &&
            Date.now() - (bridge.createdAt || 0) < 3 * 60 * 1000

          if (isBridgeValid) {
            const form = document.createElement('form')
            form.method = 'POST'
            form.action = `${serverOrigin}/admin/login-process`

            const usernameInput = document.createElement('input')
            usernameInput.type = 'hidden'
            usernameInput.name = 'username'
            usernameInput.value = bridge.username

            const passwordInput = document.createElement('input')
            passwordInput.type = 'hidden'
            passwordInput.name = 'password'
            passwordInput.value = bridge.password

            form.appendChild(usernameInput)
            form.appendChild(passwordInput)
            document.body.appendChild(form)

            sessionStorage.removeItem('adminLoginBridge')
            form.submit()
            return
          }
        } catch (parseError) {
          console.error('adminLoginBridge 파싱 실패:', parseError)
        }
      }

      navigate(PATH.AUTH.LOGIN)
    } else {
      navigate(PATH.AUTH.USERHOME)
    }
  }

  if (!userInfo) {
    return (
      <div style={containerStyle}>
        <div style={welcomeCard}>
          <h3 style={welcomeTitle}>정보를 불러오는 중입니다...</h3>
        </div>
      </div>
    )
  }

  const employeeNo = userInfo.empNo || userInfo.emp_no
  const departmentName = userInfo?.department?.deptName || userInfo?.dept_name || '소속 정보 없음'
  const positionName = userInfo?.position?.positionName || userInfo?.position_name || '직급 정보 없음'

  return (
    <div style={containerStyle}>
      <div style={pageShell}>
        <div style={brandPanel}>
          <div style={brandEyebrow}>WELCOME BACK</div>
          <h1 style={brandTitle}>
            오늘의 업무 흐름이
            <br />
            다시 준비되었습니다
          </h1>
          <p style={brandSubtitle}>
            사용자 홈의 톤과 같은 흐름으로, 지금 로그인한 계정에 맞는 홈 화면과 온보딩
            진행 상태를 이어서 확인할 수 있습니다.
          </p>

          <div style={brandFeatureGrid}>
            <div style={brandFeatureCard}>
              <div style={brandFeatureLabel}>ACCOUNT</div>
              <div style={brandFeatureValue}>{userInfo.name}님 계정 연결 완료</div>
            </div>
            <div style={brandFeatureCard}>
              <div style={brandFeatureLabel}>NEXT STEP</div>
              <div style={brandFeatureValue}>대시보드에서 오늘의 업무와 학습 진행 확인</div>
            </div>
            <div style={brandFeatureCard}>
              <div style={brandFeatureLabel}>AI FLOW</div>
              <div style={brandFeatureValue}>문서 기반 학습 도우미와 평가 흐름 연계</div>
            </div>
          </div>
        </div>

        <div style={welcomeCard}>
          <div style={cardBadge}>WELCOME</div>
          <h2 style={welcomeTitle}>{userInfo.name}님, 환영합니다!</h2>
          <p style={cardDescription}>
            로그인 정보가 정상적으로 확인되었습니다. 아래 정보를 확인한 뒤 바로 대시보드로
            이동할 수 있습니다.
          </p>

          <div style={userInfoBox}>
            <p style={userInfoRow}>
              <strong>아이디 :</strong> {employeeNo}
            </p>
            <p style={userInfoRow}>
              <strong>이름 :</strong> {userInfo.name}
            </p>
            <p style={userInfoRow}>
              <strong>부서/팀 :</strong> {departmentName}
            </p>
            <p style={{ ...userInfoRow, marginBottom: 0 }}>
              <strong>직급 :</strong> {positionName}
            </p>
          </div>

          <button onClick={handleEntry} style={primaryButton}>
            대시보드로 입장
          </button>

          <button onClick={handleLogout} style={secondaryButton}>
            로그아웃
          </button>
        </div>
      </div>
    </div>
  )
}

export default WelcomePage
