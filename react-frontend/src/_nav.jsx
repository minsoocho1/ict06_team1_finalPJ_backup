/**
 * @FileName : _nav.jsx
 * @Description : 사이드바 메뉴 구성
 *                - 주요 기능별 화면 이동 경로 정의
 *                - 인사 평가 영역 (로드맵 / 평가 / 결과 조회 / 관리) 포함
 *                - 일부 메뉴는 역할 기반 접근 제어(roles) 적용
 * @Author : 김다솜
 * @Date : 2026. 04. 21
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.21    김다솜        화면 설계 기반 사이드바 메뉴 초기 구성
 * @ 2026.04.22    김다솜        메뉴별 접근 권한(roles) 설정 예시 추가
 * @ 2026.05.01    김다솜        평가 메뉴 구조 개선 (로드맵/평가/결과 분리)
 */

import React from 'react'
import CIcon from '@coreui/icons-react'
import { cilCalculator, cilCalendar, cilChartLine, cilChatBubble, cilClock, cilDescription, cilHome, cilSettings, cilTask, cilUser } from '@coreui/icons'
import { CNavItem, CNavTitle } from '@coreui/react'
import { PATH } from './constants/path'

const _nav = [
  {
    component: CNavItem,
    name: '사내 AI 포털',
    to: '/ai-portal',
    icon: <CIcon icon={cilChatBubble} customClassName="nav-icon" />,
    badge: {
      color: 'light',
      text: 'AI',
      attributes: {
        className: 'badge badge-ai-sidebar ms-auto',
      },
    },
  },
  {
    component: CNavItem,
    name: '홈 피드',
    to: PATH.AUTH.USERHOME,
    icon: <CIcon icon={cilHome} customClassName="nav-icon" />,
  },
  {
    component: CNavItem,
    name: '일정 관리',
    to: '/calendar',
    icon: <CIcon icon={cilCalendar} customClassName="nav-icon" />,
  },
  {
    component: CNavItem,
    name: '전자결재',
    to: '/approval',
    icon: <CIcon icon={cilDescription} customClassName="nav-icon" />,
  },
  {
    component: CNavTitle,
    name: '인사 관리',
  },
  {
    component: CNavItem,
    name: '내 정보 및 조직도',
    to: '/employee',
    icon: <CIcon icon={cilUser} customClassName="nav-icon" />,
  },
  {
    component: CNavTitle,
    name: '인사 평가',
  },
  {
    component: CNavItem,
    name: 'AI 온보딩 로드맵',
    to: PATH.ONBOARDING.DASHBOARD,
    icon: <CIcon icon={cilTask} customClassName="nav-icon" />,
    badge: {
      color: 'light',
      text: 'AI',
      attributes: {
        className: 'badge badge-ai-sidebar ms-auto',
      },
    },
  },
  {
    component: CNavItem,
    name: 'AI 온보딩 평가',
    to: PATH.EVALUATION.ROOT,
    icon: <CIcon icon={cilChartLine} customClassName="nav-icon" />,
    badge: {
      color: 'light',
      text: 'AI',
      attributes: {
        className: 'badge badge-ai-sidebar ms-auto',
      },
    },
    end: true,
  },
  {
    component: CNavTitle,
    name: '업무 지원',
  },
  {
    component: CNavItem,
    name: '근태관리',
    to: '/attendance',
    icon: <CIcon icon={cilClock} customClassName="nav-icon" />,
  },
  {
    component: CNavItem,
    name: '연차 현황',
    to: PATH.ATTENDANCE.HOLIDAYS, // /attendance/holidays 로 이동
    icon: <CIcon icon={cilCalendar} customClassName="nav-icon" />,
  },
  {
    component: CNavItem,
    name: '급여관리',
    to: '/payroll',
    icon: <CIcon icon={cilCalculator} customClassName="nav-icon" />,
  },
]

export default _nav
