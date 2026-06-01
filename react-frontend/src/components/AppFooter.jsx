/**
 * @FileName : AppFooter.jsx
 * @Description : 사용자 페이지 공통 푸터 컴포넌트
 * @Author : 김다솜
 * @Date : 2026. 05. 29
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.29    김다솜        카피라이트 영역 링크 제거 및 관리자 푸터 기준 폰트 톤 적용
 */

import React from 'react'
import { CFooter } from '@coreui/react'

const AppFooter = () => {
  return (
    <CFooter
      className="px-4 border-top"
      style={{
        color: '#4b5563',
        fontSize: '0.875rem',
        fontWeight: 400,
        lineHeight: 1.5,
      }}
    >
      <div>
        <strong>
          Copyright &copy; 2026 <span>ICT06 Team1</span>.
        </strong>
        <span className="ms-1">All rights reserved.</span>
      </div>
      <div className="ms-auto d-none d-sm-inline-block">
        <span className="me-1">AI-BASED GROUPWARE</span>
        <strong>Version</strong> 1.0.0
      </div>
    </CFooter>
  )
}

export default React.memo(AppFooter)
