import React from 'react';

// CoreUI 
import { CButton, CCard, CCardBody, CCardHeader } from '@coreui/react';

// 페이지 이동
import { Link, useNavigate, useOutletContext } from 'react-router-dom';

// 시연용 이미지 파일
import refImage from 'src/assets/images/first_demo/[Alert]RealtimeAlert.png'

// 1차 시연용으로 화면과 sql 쿼리를 함께 보여주기 위한 스타일 구현
import { containerStyle, stepCardStyle } from 'src/styles/js/demoPageStyle';

// 코드 하이라이터 : sql 코드 보여주는 용
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'; 
import { coy } from 'react-syntax-highlighter/dist/esm/styles/prism';

// [알림] 페이지
const RealtimeAlert = () => {

    //DefaultLayout.js의 Outlet에서 보낸 userInfo 데이터 받기
    const [userInfo] = useOutletContext();

    //해당 화면의 SQL 쿼리 작성(백틱 `` 사용)
    const sqlQuery = `
        INSERT INTO notification (emp_id, noti_type, message, url, is_read)
        VALUES (
            #{targetEmpId}, 
            #{notiType}, 
            #{message}, 
            #{url}, 
            'N'
        ) RETURNING noti_id;

        SELECT * FROM notification 
        WHERE emp_id = #{empId} 
        AND created_at > CURRENT_DATE - INTERVAL '30 days'
        ORDER BY is_read ASC, created_at DESC;

        UPDATE notification 
        SET is_read = 'Y' 
        WHERE noti_id = #{notiId} AND emp_id = #{empId};
    `;

    return (
        <div style={containerStyle}>
            <header style={{ marginBottom: '30px', display: 'flex', justifyContent: 'space-between' }}>
                <h2 className="user-page-title">알림</h2>
            </header>

            {/* 1차 시연용 영역 */}
            <CCard className="mb-4" style={{ height: 'calc(100vh - 120px)' }}>
                <CCardHeader>
                    <strong>시연 화면 및 관련 SQL쿼리</strong>
                </CCardHeader>
                <CCardBody className="p-0 d-flex flex-column">

                    {/* 레퍼런스 이미지 영역 */}
                    <div className="text-center" style={{ backgroundColor: '#f4f4f4', borderTop: '1px solid #eee' }}>
                        <img 
                            src={refImage} 
                            alt="로드맵 및 체크리스트" 
                            style={{ width: '100%',
                            height: 'auto',
                            display: 'block' }} 
                        />
                    </div>

                    {/* SQL 쿼리 영역 */}
                    <div className='text-start mt-4'>
                        <h5 className='mb-3' style={{ fontWeight: 'bold', color: '#4f5d73' }}>
                            <span style={{ borderLeft: '4px solid #321fdb', paddingLeft: '10px' }}>
                                관련 SQL 쿼리
                            </span>
                        </h5>
                        <SyntaxHighlighter language='sql' style={coy}>
                            {sqlQuery}
                        </SyntaxHighlighter>
                    </div>
                </CCardBody>
            </CCard>
        </div>
    );
};

export default RealtimeAlert;
