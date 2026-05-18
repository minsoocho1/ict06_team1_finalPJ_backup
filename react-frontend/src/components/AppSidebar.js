import React from 'react';
import { useSelector, useDispatch } from 'react-redux';

import {
  CSidebar,
  CSidebarBrand,
  CSidebarNav,
  CSidebarToggler,
} from '@coreui/react';

import { AppSidebarNav } from './AppSidebarNav'; 
import { setSidebarState } from '../store/store';

import AppLogo from './AppLogo'; // 새로 생성한 AppLogo 컴포넌트 임포트

import SimpleBar from 'simplebar-react';
import 'simplebar-react/dist/simplebar.min.css';

// 사이드바 네비게이션 설정 (예: _nav.js에서 임포트)
import navigation from '../_nav'; 

const AppSidebar = () => {
  const dispatch = useDispatch();
  const unfoldable = useSelector((state) => state.sidebarUnfoldable);
  const sidebarShow = useSelector((state) => state.sidebarShow);

  return (
    <CSidebar
      position="fixed"
      unfoldable={unfoldable}
      visible={sidebarShow}
      onVisibleChange={(visible) => {
        dispatch(setSidebarState({ sidebarShow: visible }));
      }}
    >
      <div className="sidebar-header border-bottom">
        <CSidebarBrand as="div" className="d-none d-md-flex">
          {/* AppLogo 컴포넌트를 사용하여 로고를 표시합니다. */}
          {/* 사이드바가 접히거나(sidebarShow가 false) unfoldable 상태일 때 collapsed prop을 true로 전달하여 텍스트를 숨깁니다. */}
          <AppLogo collapsed={!sidebarShow || unfoldable} />
        </CSidebarBrand>
        <CSidebarToggler className="d-none d-lg-flex" onClick={() => dispatch(setSidebarState({ sidebarUnfoldable: !unfoldable }))} />
      </div>
      <CSidebarNav>
        <SimpleBar><AppSidebarNav items={navigation} /></SimpleBar>
      </CSidebarNav>
    </CSidebar>
  );
};

export default AppSidebar;
