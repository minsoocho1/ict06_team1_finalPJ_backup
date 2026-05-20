/**
 * @FileName : AppSidebar.jsx
 * @Description : 사용자 권한별 사이드바 메뉴 및 프로필 요약 영역
 * @Author : 김다솜
 * @Date : 2026. 05. 19
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.19    김다솜        사이드바 기본 프로필 이미지 제거
 */

import React from 'react'
import { useDispatch, useSelector } from 'react-redux'

import {
  CAvatar,
  CFormInput,
  CInputGroup,
  CInputGroupText,
  CSidebar,
  CSidebarBrand,
  CSidebarNav,
} from '@coreui/react'
import { CIcon } from '@coreui/icons-react'
import { cilSearch, cilUser } from '@coreui/icons'

import { AppSidebarNav } from './AppSidebarNav'
import AppLogo from './AppLogo'
import navigation from '../_nav'
import { setSidebarState } from 'src/store/store'

const normalizeRole = (roleValue) => {
  if (typeof roleValue === 'string') return roleValue.toUpperCase()
  if (Array.isArray(roleValue) && roleValue.length > 0) return normalizeRole(roleValue[0])
  if (roleValue && typeof roleValue === 'object') {
    const candidate = roleValue.roleName || roleValue.authority || roleValue.name
    return typeof candidate === 'string' ? candidate.toUpperCase() : ''
  }
  return ''
}

const AppSidebar = ({ userInfo }) => {
  const dispatch = useDispatch()
  const sidebarShow = useSelector((state) => state.sidebarShow)

  const userRole = normalizeRole(userInfo?.role) || 'ROLE_USER'
  const employeeName = userInfo?.name || '사용자'
  const employeeNo = userInfo?.empNo || userInfo?.emp_no || '-'
  const departmentName =
    userInfo?.department?.deptName ||
    userInfo?.dept?.deptName ||
    userInfo?.department?.name ||
    userInfo?.deptName ||
    userInfo?.dept_name ||
    '소속 정보 없음'
  const positionName =
    userInfo?.position?.positionName ||
    userInfo?.posName ||
    userInfo?.positionName ||
    userInfo?.position_name ||
    '직급 정보 없음'

  const filteredNav = navigation.filter((item) => {
    if (!item.roles) return true
    return item.roles.includes(userRole)
  })

  return (
    <CSidebar
      position="fixed"
      visible={sidebarShow}
      onVisibleChange={(visible) => {
        dispatch(setSidebarState({ sidebarShow: visible }))
      }}
    >
      <CSidebarBrand className="d-flex align-items-center border-bottom">
        <AppLogo />
      </CSidebarBrand>

      <div className="p-3 border-bottom sidebar-profile-area">
        <div
          className="mb-3"
          style={{
            background: '#f4f6fb',
            border: '1px solid #e4e8f3',
            borderRadius: '16px',
            padding: '14px',
          }}
        >
          <div className="d-flex align-items-center gap-3">
            <CAvatar color="secondary" textColor="white" size="lg">
              <CIcon icon={cilUser} />
            </CAvatar>
            <div style={{ minWidth: 0 }}>
              <div className="fw-semibold text-dark text-truncate">{employeeName}</div>
              <div className="small text-muted">사번: {employeeNo}</div>
              <div className="small text-secondary mt-1 text-truncate">{departmentName}</div>
              <div className="small text-secondary text-truncate">{positionName}</div>
            </div>
          </div>
        </div>

        <CInputGroup>
          <CInputGroupText className="bg-white border-secondary text-secondary">
            <CIcon icon={cilSearch} />
          </CInputGroupText>
          <CFormInput
            placeholder="Search"
            className="bg-white border-secondary text-dark"
            style={{ fontSize: '0.85rem' }}
          />
        </CInputGroup>
      </div>

      <CSidebarNav>
        <AppSidebarNav items={filteredNav} />
      </CSidebarNav>
    </CSidebar>
  )
}

export default React.memo(AppSidebar)
