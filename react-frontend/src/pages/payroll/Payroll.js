import React from 'react';

import { Navigate } from 'react-router-dom';

import { PATH } from 'src/constants/path';

// 급여관리 메뉴 진입 페이지
// - 사이드바의 급여관리 메뉴가 이 페이지에 연결되어 있다.
// - 실제 급여명세서 조회 화면으로 바로 이동시킨다.
const Payroll = () => {

    return (
        <Navigate
            to={PATH.PAYROLL.ISSUE}
            replace
        />
    );
};

export default Payroll;