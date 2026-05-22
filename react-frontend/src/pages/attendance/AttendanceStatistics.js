import React from 'react';

// 페이지 이동
import {useOutletContext } from 'react-router-dom';

// 스타일 구현
import { containerStyle } from 'src/styles/js/demoPageStyle';

// [근태관리] 근태 대시보드 페이지
const AttendanceStatistics = () => {

    //DefaultLayout.js의 Outlet에서 보낸 userInfo 데이터 받기
    const [userInfo] = useOutletContext();

    return (
        <div style={containerStyle}>

            <header style={{ marginBottom: '30px', display: 'flex', justifyContent: 'space-between' }}>
                <h2>{userInfo?.name}님의 근태 통계</h2>
            </header>

            <hr style={{ border: '0', height: '1px', background: '#eee', margin: '40px 0' }} />
        </div>
    );
};

export default AttendanceStatistics;