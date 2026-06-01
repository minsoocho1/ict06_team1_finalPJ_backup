import React from 'react'
import { AppSidebar, AppFooter, AppHeader } from '../components/index'
import { Outlet, useLocation } from 'react-router-dom'

const DefaultLayout = ({userInfo}) => {
  const { pathname } = useLocation()
  const hasSecondarySidebar =
    pathname.startsWith('/ai-portal') || pathname.startsWith('/approval')

  return (
    <div className="wrapper d-flex flex-column min-vh-100">
      {/* 사이드바에 userInfo 전달 */}
      <AppSidebar userInfo={userInfo} />
      <div className="wrapper d-flex flex-column min-vh-100 bg-light">
        <AppHeader userInfo={userInfo} />
        <div className={`body flex-grow-1 ${hasSecondarySidebar ? '' : 'px-3'}`}>
          {/* 하위 페이지 컴포넌트에서 userInfo 사용할 수 있도록 context로 전달 */}
          <main
            className={`user-main-content ${hasSecondarySidebar ? 'user-main-content-secondary-sidebar' : ''}`}
          >
            <Outlet context={[userInfo]} />
          </main>
        </div>
        <AppFooter />
      </div>
    </div>
  )
}

export default DefaultLayout
