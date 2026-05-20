/**
 * @FileName : AppHeaderDropdown.jsx
 * @Description : 헤더 사용자 드롭다운 및 로그아웃 확인 모달 컴포넌트
 * @Author : 김다솜
 * @Date : 2026. 05. 18
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.18    김다솜        로그아웃 모달을 홈/웰컴/사내 AI 포털 톤으로 개선하고 스타일 파일 분리
 * @ 2026.05.19    김다솜        헤더 기본 프로필 이미지 제거 및 로그아웃 확인창을 포털 기반 커스텀 모달로 전환
 */

import React, { useState } from 'react'
import { createPortal } from 'react-dom'
import {
  CAvatar,
  CDropdown,
  CDropdownDivider,
  CDropdownHeader,
  CDropdownItem,
  CDropdownMenu,
  CDropdownToggle,
  CToast,
  CToastBody,
  CToaster,
} from '@coreui/react'
import { cilLockLocked, cilUser } from '@coreui/icons'
import CIcon from '@coreui/icons-react'

import { NavLink, useNavigate } from 'react-router-dom'
import { useUser } from 'src/api/UserContext'
import { PATH } from 'src/constants/path'
import {
  dropdownLogoutItemStyle,
  logoutBackdropStyle,
  logoutCancelButtonStyle,
  logoutConfirmButtonStyle,
  logoutDialogShellStyle,
  logoutMessageCardStyle,
  logoutMessageStyle,
  logoutModalBodyStyle,
  logoutModalContentStyle,
  logoutModalDialogStyle,
  logoutModalEyebrowStyle,
  logoutModalFooterStyle,
  logoutModalHeaderStyle,
  logoutModalIconStyle,
  logoutModalTitleStyle,
  logoutModalTitleWrapStyle,
  logoutSubTextStyle,
  logoutToastBodyStyle,
  logoutToastIconStyle,
  logoutToastStyle,
} from 'src/styles/js/auth/LogoutStyle'

const AppHeaderDropdown = () => {
  const navigate = useNavigate()
  const { logout } = useUser()
  const [logoutModal, setLogoutModal] = useState(false)
  const [toast, addToast] = useState(null)

  const openLogoutModal = (event) => {
    event.preventDefault()
    event.stopPropagation()
    setLogoutModal(true)
  }

  const handleLogout = () => {
    logout()
    setLogoutModal(false)

    addToast(
      <CToast autohide={true} delay={1500} style={logoutToastStyle}>
        <CToastBody style={logoutToastBodyStyle}>
          <span style={logoutToastIconStyle}>✓</span>
          <span>로그아웃되었습니다.</span>
        </CToastBody>
      </CToast>,
    )

    setTimeout(() => {
      navigate(PATH.AUTH.LOGIN)
    }, 1500)
  }

  const logoutConfirmModal = logoutModal
    ? createPortal(
        <div style={logoutBackdropStyle} role="presentation" onMouseDown={() => setLogoutModal(false)}>
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="logout-modal-title"
            style={{ ...logoutModalDialogStyle, ...logoutDialogShellStyle }}
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div style={logoutModalContentStyle}>
              <div style={logoutModalHeaderStyle}>
                <div style={logoutModalTitleWrapStyle}>
                  <span style={logoutModalIconStyle}>
                    <CIcon icon={cilLockLocked} />
                  </span>
                  <div>
                    <p style={logoutModalEyebrowStyle}>SESSION</p>
                    <h5 id="logout-modal-title" style={logoutModalTitleStyle}>
                      로그아웃할까요?
                    </h5>
                  </div>
                </div>
              </div>

              <div style={logoutModalBodyStyle}>
                <div style={logoutMessageCardStyle}>
                  <p style={logoutMessageStyle}>현재 계정에서 로그아웃합니다.</p>
                  <small style={logoutSubTextStyle}>
                    진행 중인 작업이 있다면 저장 여부를 한 번 더 확인해 주세요.
                    <br />
                    로그아웃 후 로그인 페이지로 이동합니다.
                  </small>
                </div>
              </div>

              <div style={logoutModalFooterStyle}>
                <button type="button" style={logoutCancelButtonStyle} onClick={() => setLogoutModal(false)}>
                  취소
                </button>
                <button type="button" style={logoutConfirmButtonStyle} onClick={handleLogout}>
                  로그아웃
                </button>
              </div>
            </div>
          </div>
        </div>,
        document.body,
      )
    : null

  return (
    <>
      <CDropdown variant="nav-item">
        <CDropdownToggle placement="bottom-end" className="py-0 pe-0" caret={false}>
          <CAvatar color="secondary" textColor="white" size="md">
            <CIcon icon={cilUser} />
          </CAvatar>
        </CDropdownToggle>
        <CDropdownMenu className="pt-0" placement="bottom-end">
          <CDropdownHeader className="bg-body-secondary fw-semibold my-2">내 계정</CDropdownHeader>
          <CDropdownItem as={NavLink} to={PATH.AUTH.MYPAGE}>
            <CIcon icon={cilUser} className="me-2" />
            마이페이지
          </CDropdownItem>
          <CDropdownDivider />
          <button
            type="button"
            className="dropdown-item"
            onMouseDown={(event) => event.stopPropagation()}
            onClick={openLogoutModal}
            style={dropdownLogoutItemStyle}
          >
            <CIcon icon={cilLockLocked} className="me-2" />
            로그아웃
          </button>
        </CDropdownMenu>
      </CDropdown>

      {logoutConfirmModal}
      <CToaster push={toast} placement="top-center" />
    </>
  )
}

export default AppHeaderDropdown
