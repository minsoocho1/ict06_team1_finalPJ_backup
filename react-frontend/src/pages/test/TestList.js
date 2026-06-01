/**
 * @FileName : TestList.js
 * @Description : 학습을 위한 Test 예제. 최종 사이트엔 미포함되는 기능입니다.
 *          전자결재 서식 목록을 조회하는 예제 페이지.  
 *          axios로 REST API 사용하여 백엔드와 통신
 * @Author : 송영은
 * @Date : 2026. 04. 29
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    송영은       최초 생성/화면 구성
 */

import React, { useEffect, useState } from 'react';

// 백엔드와 통신하기 위해 axios 사용
import { request } from 'src/helpers/axios_helper';

// CoreUI 템플릿.
import { CButton, CCard, CCardBody, CCardHeader } from '@coreui/react';

// 1차 시연용으로 화면과 sql 쿼리를 함께 보여주기 위한 스타일 구현
import { containerStyle, stepCardStyle } from 'src/styles/js/demoPageStyle';

// 페이지 이동
import { Link, useNavigate, useOutletContext } from 'react-router-dom';

// 경로 상수 
import { PATH } from 'src/constants/path';

const TestList = () => {
    //DefaultLayout.js의 Outlet에서 보낸 userInfo 데이터 받기
    const [userInfo] = useOutletContext();

    // 전자결재 서식 목록 조회 
    const [appForms, setAppForms] = useState([]);

    // 최초 렌더링 시 데이터 조회
    useEffect(() => {
        fetchApprovalList();
    }, []);

    // ✅ Axios API 호출 (JWT 자동 포함): 백엔드 데이터 가져오기 
    const fetchApprovalList = async () => {
        try {
            const response = await request('GET', PATH.TEST.LIST, null);

            console.log('응답 데이터:', response.data);

            setAppForms(response.data);

        } catch (error) {
            console.error('데이터 조회 실패:', error);

            // JWT 만료 대응 (선택)
            if (error.response && error.response.status === 401) {
                alert('로그인이 필요합니다.');
            }
        }
    };


    return (
        <div style={containerStyle}>
            <header style={{ marginBottom: '30px', display: 'flex', justifyContent: 'space-between' }}>
                <h2 className="user-page-title">{userInfo?.name}님 안녕하세요. 여기에 페이지 제목을 써주세요</h2>
            </header>

            {/* ✅ 리스트 출력 */}
            {appForms.length === 0 ? (
                <p> 데이터가 없습니다 </p>
            ) : (
                appForms.map((item) => (
                    <CCard key={item.formId} style={{ marginBottom: '15px' }}>
                        <CCardHeader>
                            📄 {item.formName}
                        </CCardHeader>

                        <CCardBody>
                            <p><strong>작성일:</strong> {item.createdAt}</p>
                            <p><strong>수정일:</strong> {item.updatedAt}</p>
                        </CCardBody>
                    </CCard>
                ))
            )} 
        </div>
    );
};

export default TestList;
