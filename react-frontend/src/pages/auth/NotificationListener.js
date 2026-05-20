/**
 * @FileName : NotificationListener.js
 * @Description : 사용자 실시간 알림 SSE 구독 및 토스트 표시 컴포넌트
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.19    김다솜        로그아웃 시 SSE 연결을 정상 종료로 처리하고 SSE 구독 경로를 상수화
 */

import { CToast, CToastBody, CToaster, CToastHeader } from '@coreui/react';
import React, { useEffect, useRef, useState } from 'react';
import { useUser } from 'src/api/UserContext';
import { PATH } from 'src/constants/path';

const NotificationListener = ({ children }) => {
    const { userInfo } = useUser();
    const [toasts, setToasts] = useState([]);
    const eventSourceRef = useRef(null);
    const intentionalCloseRef = useRef(false);

    useEffect(() => {
        const handleAppLogout = () => {
            intentionalCloseRef.current = true;
            eventSourceRef.current?.close();
            eventSourceRef.current = null;
            setToasts([]);
        };

        window.addEventListener('appLogout', handleAppLogout);
        return () => window.removeEventListener('appLogout', handleAppLogout);
    }, []);

    useEffect(() => {
        const token = localStorage.getItem('accessToken');
        console.log('[SSE] userInfo:', userInfo);
        console.log('[SSE] empNo:', userInfo?.empNo);

        if (userInfo && userInfo.empNo && token) {
            intentionalCloseRef.current = false;
            console.log('알림 구독 시작: ', userInfo.empNo);

            const eventSource = new EventSource(PATH.API.NOTIFICATION.SUBSCRIBE(token));
            eventSourceRef.current = eventSource;

            eventSource.onopen = () => console.log('SSE 연결 성공');

            eventSource.onmessage = (event) => {
                console.log('[SSE] onmessage:', event);
            };

            eventSource.addEventListener('notification', (event) => {
                console.log('[SSE] notification 이벤트 수신:', event.data);
                const newNoti = JSON.parse(event.data);
                console.log('[SSE] 파싱된 알림:', newNoti);

                const pushToast = (
                    <CToast
                        key={Date.now()}
                        autohide
                        visible
                        className="shadow-sm"
                        style={{
                            borderRadius: '12px',
                            overflow: 'hidden',
                        }}
                    >
                        <CToastHeader
                            closeButton
                            style={{
                                backgroundColor: '#321fdb',
                                color: '#ffffff',
                                borderBottom: 'none',
                            }}
                        >
                            <div className="fw-bold me-auto">
                                {newNoti.title || '새 알림'}
                            </div>
                            <small style={{ color: 'rgba(255,255,255,0.75)' }}>방금 전</small>
                        </CToastHeader>

                        <CToastBody
                            style={{
                                backgroundColor: '#f7f5ff',
                                color: '#212529',
                                fontWeight: 500,
                            }}
                        >
                            {newNoti.content}
                        </CToastBody>
                    </CToast>
                );

                window.dispatchEvent(new CustomEvent('newNotification'));
                setToasts((prev) => [...prev, pushToast]);
            });

            eventSource.onerror = (error) => {
                if (intentionalCloseRef.current || !localStorage.getItem('accessToken')) {
                    eventSource.close();
                    return;
                }

                console.error('[SSE] 에러:', error);
                eventSource.close();
            };

            return () => {
                intentionalCloseRef.current = true;
                eventSource.close();
                if (eventSourceRef.current === eventSource) {
                    eventSourceRef.current = null;
                }
                console.log('SSE 연결 종료');
            };
        }

        console.log('[SSE] 구독 조건 미충족 - empNo 또는 token 없음');
        return undefined;
    }, [userInfo]);

    return (
        <>
            <CToaster placement="top-center">
                {toasts}
            </CToaster>
            {children}
        </>
    );
};

export default NotificationListener;
