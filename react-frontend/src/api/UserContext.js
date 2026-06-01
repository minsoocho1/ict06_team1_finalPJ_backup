/**
 * @FileName : UserContext.js
 * @Description : 로그인 사용자 정보 전역 관리 Context
 * @Author : 김다솜
 * @Date : 2026. 04. 22
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.22    김다솜        최초 생성/화면 구성/로그인 계정 정보 저장 Context 구현
 * @ 2026.04.29    김다솜        새로고침 시 토큰 기반 사용자 정보 복구 로직 추가
 * @ 2026.05.07    김다솜        RefreshToken 도입에 따른 이중 토큰 저장 및 로그아웃 로직 수정
 * @ 2026.05.19    김다솜        로그아웃 시 SSE/알림 구독 정리를 위한 전역 로그아웃 이벤트 발행
 * @ 2026.05.29    김다솜        관리자/사용자 정보 연동을 위한 사용자 정보 최신 재조회 로직 추가
 */

import React, { createContext, useContext, useEffect, useState } from 'react';
import { PATH } from 'src/constants/path';
import axiosInstance from './axiosInstance';

const UserContext = createContext(null);

export const UserProvider = ({ children }) => {
    const [userInfo, setUserInfo] = useState(null);
    const [userLoading, setUserLoading] = useState(true);

    const refreshUserInfo = async () => {
        // 새로고침 시 토큰이 존재하면 AccessToken으로 사용자 정보 우선 복구 시도
        const token = localStorage.getItem('accessToken');

        if (!token) {
            setUserLoading(false);
            setUserInfo(null);
            return null;
        }

        try {
            const response = await axiosInstance.get(PATH.API.USER_ME);

            setUserInfo(response.data);
            return response.data;
        } catch (err) {
            console.error('사용자 정보 복구 실패:', err);
            setUserInfo(null);
            return null;
        } finally {
            setUserLoading(false);
        }
    };

    useEffect(() => {
        refreshUserInfo();
    }, []);

    useEffect(() => {
        const handleFocusRefresh = () => {
            if (document.visibilityState === 'visible' && localStorage.getItem('accessToken')) {
                refreshUserInfo();
            }
        };

        window.addEventListener('focus', handleFocusRefresh);
        document.addEventListener('visibilitychange', handleFocusRefresh);

        return () => {
            window.removeEventListener('focus', handleFocusRefresh);
            document.removeEventListener('visibilitychange', handleFocusRefresh);
        };
    }, []);

    //로그인 시 AccessToken, RefreshToken 저장
    const login = (basicInfo, accessToken, refreshToken) => {
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        setUserInfo(basicInfo);
    };

    //로그아웃 시 토큰 삭제 및 사용자 정보 초기화
    const logout = () => {
        window.dispatchEvent(new CustomEvent('appLogout'));
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('token');
        setUserInfo(null);
    };

    //상세 정보 업데이트
    const updateUserInfo = (detailInfo) => {
        setUserInfo(prev => ({ ...prev, ...detailInfo }));
    };

    return (
        <UserContext.Provider value={{ userInfo, setUserInfo, userLoading, login, updateUserInfo, refreshUserInfo, logout }}>
            {children}
        </UserContext.Provider>
    );
};

//커스텀 hook
export const useUser = () => useContext(UserContext);
