import React from 'react'
import { CFooter } from '@coreui/react'

const AppFooter = () => {
  return (
    <CFooter className="px-4 border-top">
      <div>
        <strong>
          Copyright &copy; 2026 <a href="/" className="text-decoration-none">ICT06 Team1</a>.
        </strong>
        <span className="ms-1 text-muted">All rights reserved.</span>
      </div>
      <div className="ms-auto d-none d-sm-inline-block">
        <span className="text-muted me-1">AI-BASED GROUPWARE</span>
        <b>Version</b> 1.0.0
      </div>
    </CFooter>
  )
}

export default React.memo(AppFooter)
