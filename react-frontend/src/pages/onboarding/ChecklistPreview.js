/**
 * @FileName : ChecklistPreview.js
 * @Description : 온보딩 체크리스트 미리보기 위젯
 *                - 체크리스트 일부 조회(미완료 기준 3개)
 *                - 로드맵 화면에서 사용
 * @Author : 김다솜
 * @Date : 2026. 04. 29
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    김다솜        최초 생성 및 체크리스트 미리보기 기능 구현
 * @ 2026.05.15    김다솜        스타일 수정
 */

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axiosInstance from 'src/api/axiosInstance';
import { PATH } from 'src/constants/path';
import { previewCard, previewHeader, previewHeaderRow, previewLink, previewList, previewSubText, previewTextArea } from 'src/styles/js/onboarding/ChecklistStyle';

const ChecklistPreview = ({ userInfo }) => {
    const navigate = useNavigate();

    const [preview, setPreview] = useState([]);
    const [total, setTotal] = useState(0);
    const [completed, setCompleted] = useState(0);

    useEffect(() => {
        const fetchChecklist = async() => {
            const empNo = userInfo?.empNo;
            if(!empNo) return;

            try {
                const res = await axiosInstance.get(PATH.API.ONBOARDING.CHECKLIST_LIST(empNo));

                const data = res.data || [];

                //전체 개수
                setTotal(data.length);

                //완료 개수
                const done = data.filter(item => item.status === 'COMPLETED').length;
                setCompleted(done);

                //미완료 3개만 노출
                const previewItems = data
                    .filter(item => item.status !== 'COMPLETED')
                    .slice(0, 3);

                setPreview(previewItems);
            } catch(err) {
                console.error("체크리스트 미리보기 실패", err);
            }
        };

        fetchChecklist();
    }, [userInfo]);

    return (
        // 체크리스트 미리보기
        <div
            style={{
                ...previewCard,
                width: '360px',
            }}
            onClick={() => navigate(PATH.ONBOARDING.CHECKLIST)}
        >
            <div style={previewHeaderRow}>
                {/* 왼쪽 영역 */}
                <div style={previewTextArea}>
                    <div style={previewHeader}>✅ 온보딩 체크리스트</div>
                    <div style={previewSubText}>
                        입사 후 필수 확인 항목과 완료 상태를 점검하세요.
                    </div>
                </div>

                {/* 오른쪽 버튼 */}
                <span style={{ ...previewLink, color: '#2563EB', fontWeight: 700 }}>
                    전체 보기 →
                </span>
            </div>

            {/* 리스트 */}
            <div style={previewList}>
                {preview.length === 0 ? (
                    <div>🎉 모든 체크리스트 완료!</div>
                ) : (
                    preview.map(item => (
                        <div key={item.checklistId}>
                            • {item.title}
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

export default ChecklistPreview;
