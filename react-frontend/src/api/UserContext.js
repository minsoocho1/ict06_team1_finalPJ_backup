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
 */

import React, { createContext, useContext, useEffect, useState } from 'react';
import { PATH } from 'src/constants/path';
import axiosInstance from './axiosInstance';

const UserContext = createContext(null);

export const UserProvider = ({ children }) => {
    const [userInfo, setUserInfo] = useState(null);
    const [userLoading, setUserLoading] = useState(true);

    useEffect(() => {
        const restoreUserInfo = async () => {
            // 새로고침 시 토큰이 존재하면 AccessToken으로 사용자 정보 우선 복구 시도
            const token = localStorage.getItem('accessToken');

            if (!token) {
                setUserLoading(false);
                return;
            }

            try {
                const response = await axiosInstance.get(PATH.API.USER_ME);

                setUserInfo(response.data);
            } catch (err) {
                console.error('사용자 정보 복구 실패:', err);
                setUserInfo(null);
            } finally {
                setUserLoading(false);
            };
        };
        restoreUserInfo();
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
        <UserContext.Provider value={{ userInfo, setUserInfo, userLoading, login, updateUserInfo, logout }}>
            {children}
        </UserContext.Provider>
    );
};

//커스텀 hook
export const useUser = () => useContext(UserContext);
