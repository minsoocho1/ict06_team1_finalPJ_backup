/**
 * @author : 송영은
 * description : 모든 리액트 페이지에서 공통적으로 필요한 요소를 넣은 예시 파일입니다. 이 파일을 응용하여 페이지 구현해주세요. 
 * ========================================
 * DATE         AUTHOR      NOTE
 * 2026-04-23   송영은      최초 생성
 **/

// 수정 금지
import React from 'react';

// 경로 상수 
import { PATH } from "src/constants/path";

// CoreUI 템플릿. 
import { CButton, CCard, CCardBody, CCardHeader } from '@coreui/react';

// 페이지 이동
import { Link, useNavigate, useOutletContext } from 'react-router-dom';

// 시연용 이미지 파일
import refImage from 'src/assets/images/first_demo/[Onboarding]Roadmap.png';

// 1차 시연용으로 화면과 sql 쿼리를 함께 보여주기 위한 스타일 구현
import { containerStyle, stepCardStyle } from 'src/styles/js/demoPageStyle';

// 코드 하이라이터 : sql 코드 보여주는 용
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'; 
import { coy } from 'react-syntax-highlighter/dist/esm/styles/prism';

// [대분류] 페이지명
const PageTemplate = () => {

    // 방법1. 버튼 클릭 시 링크 이동
    const navigate = useNavigate();
    const handleButtonClick = () => {
        navigate(PATH.EVALUATION.QUIZ); // path에서 경로 상수 불러오기 
        // navigate('/evaluation/quiz')
    }
    
    //DefaultLayout.js의 Outlet에서 보낸 userInfo 데이터 받기
    const [userInfo] = useOutletContext();

    //해당 화면의 SQL 쿼리 작성(백틱 `` 사용)
    const sqlQuery = `
        -- 사용자별 로드맵 진행률 실시간 계산
        SELECT 
            r.roadmap_id,
            r.title,
            AVG(rp.rate) as overall_progress
        FROM roadmap r
        JOIN road_prog rp ON r.roadmap_id = rp.roadmap_id
        WHERE rp.emp_id = #{empId}
        GROUP BY r.roadmap_id, r.title;
    `;

    return (
        <div style={containerStyle}>
            <header style={{ marginBottom: '30px', display: 'flex', justifyContent: 'space-between' }}>
                <h2 className="user-page-title">🚀 {userInfo?.name}님의 성장 로드맵</h2>

                <button onClick={() => navigate('/welcome')} style={{ border: 'none', background: 'none', color: '#666', cursor: 'pointer' }}>뒤로가기</button>
            </header>

            <hr style={{ border: '0', height: '1px', background: '#eee', margin: '40px 0' }} />

            {/* 1차 시연용 영역 */}
            <CCard className="mb-4" style={{ height: 'calc(100vh - 120px)' }}>
                <CCardHeader>
                    <strong>시연 화면 및 관련 SQL쿼리</strong>
                </CCardHeader>
                <CCardBody className="p-0 d-flex flex-column">
                    <div className="p-2 d-flex justify-content-end">
                        {/* 시연용 화면 이동 버튼 */}
                        {/* 방법1 */}
                        <CButton
                            color='primary'
                            variant='outline'
                            onClick={handleButtonClick}
                            style={{ fontWeight: 'bold' }}
                            >
                            퀴즈 풀기
                        </CButton>

                        {/* 방법2 */}
                        {/* path에서 경로 상수 불러오기 */}
                        {/* <Link to="/attendance/stats"> */}
                        <Link to={PATH.ATTENDANCE.STATS}> 
                            <CButton
                                color='primary'
                                variant='outline'
                                style={{ fontWeight: 'bold' }}
                                >
                                근태통계
                            </CButton>
                        </Link>
                    </div>

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

export default PageTemplate;
