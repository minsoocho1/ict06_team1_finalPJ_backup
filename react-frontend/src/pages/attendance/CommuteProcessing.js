import React from 'react';

// 페이지 링크 이동
import { Link } from 'react-router-dom';

// 시연용 이미지 파일
import demo_image from '../../assets/images/first_demo/attendance_checkin_checkout.png';

// 출퇴근 처리 화면
const CommuteProcessing = () => {

    // 핵심 SQL 
    const sql = `
    (필요한 sql 문 작성해주세요)
    `; 

    return (
        <div>
            {/* 버튼 클릭하여 페이지 이동 */}
            <Link to="/attendance/stats">
                <button> 근태통계 </button>
            </Link>

            {/* 화면 설계 이미지 */}
            <img src={demo_image} alt="attendance"/>
            <br />
            

            {/* 핵심 SQL */}
            {/* <SyntaxHighlighter language='sql' style={atomDark}>
                {sql}
            </SyntaxHighlighter> */}
            
        </div>
    );
};

export default CommuteProcessing;