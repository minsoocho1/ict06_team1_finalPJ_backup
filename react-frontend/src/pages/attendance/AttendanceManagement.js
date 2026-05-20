import React from 'react';

// 페이지 이동
import { useOutletContext } from 'react-router-dom';

// 스타일
import { containerStyle } from 'src/styles/js/demoPageStyle';

// 실제 근태 기능 컴포넌트
import Attendance from './Attendance';

// [근태관리] 근태관리 홈 화면
const AttendanceManagement = () => {

    //DefaultLayout.js의 Outlet에서 보낸 userInfo 데이터 받기
    const [userInfo] = useOutletContext();

    return (
        <div style={containerStyle}>    

            {/* 실제 출근/퇴근 기능 영역 */}
            <Attendance />    
        </div>
    );
};

export default AttendanceManagement;