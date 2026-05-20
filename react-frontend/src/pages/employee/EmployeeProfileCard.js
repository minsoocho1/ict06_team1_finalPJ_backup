import React from 'react';

// CoreUI
import {
    CBadge
} from '@coreui/react';

/*
 * 사원 상세 카드 컴포넌트
 *
 * 역할:
 * - 선택한 사원 상세 정보 출력
 */
const EmployeeProfileCard = ({
    selectedEmployee,
}) => {

    /*
     * 선택된 사원이 없는 경우
     */
    if (!selectedEmployee) {

        return (
            <div
                className="text-center text-medium-emphasis py-5"
            >
                구성원을 선택해주세요.
            </div>
        );
    }

    return (
        <div>

            {/* 상단 프로필 영역 */}
            <div
                className="d-flex align-items-center mb-4"
            >

                {/* 프로필 이미지 */}
                <div
                    style={{
                        width: '80px',
                        height: '80px',
                        borderRadius: '50%',
                        overflow: 'hidden',
                        backgroundColor: '#f3f4f7',
                        marginRight: '20px',
                        flexShrink: 0,
                    }}
                >

                    {
                        selectedEmployee.profileImg ? (
                            <img
                                src={selectedEmployee.profileImg}
                                alt="프로필"
                                style={{
                                    width: '100%',
                                    height: '100%',
                                    objectFit: 'cover',
                                }}
                            />
                        ) : (
                            <div
                                className="d-flex justify-content-center align-items-center h-100"
                                style={{
                                    fontSize: '28px',
                                    fontWeight: 'bold',
                                    color: '#999',
                                }}
                            >
                                {selectedEmployee.name?.charAt(0)}
                            </div>
                        )
                    }

                </div>

                {/* 기본 정보 */}
                <div>

                    <div
                        style={{
                            fontSize: '22px',
                            fontWeight: 'bold',
                        }}
                    >
                        {selectedEmployee.name}
                    </div>

                    <div
                        className="text-medium-emphasis mb-2"
                    >
                        {selectedEmployee.deptName}
                        {' · '}
                        {selectedEmployee.positionName}
                    </div>

                    <CBadge
                        color={
                            selectedEmployee.status === '재직'
                                ? 'success'
                                : selectedEmployee.status === '휴직'
                                    ? 'warning'
                                    : 'secondary'
                        }
                    >
                        {selectedEmployee.status}
                    </CBadge>

                </div>

            </div>

            {/* 상세 정보 */}
            <div
                className="row"
            >

                {/* 이메일 */}
                <div className="col-md-6 mb-3">

                    <div
                        className="text-medium-emphasis mb-1"
                    >
                        이메일
                    </div>

                    <div>
                        {selectedEmployee.email || '-'}
                    </div>

                </div>

                {/* 연락처 */}
                <div className="col-md-6 mb-3">

                    <div
                        className="text-medium-emphasis mb-1"
                    >
                        연락처
                    </div>

                    <div>
                        {selectedEmployee.phone || '-'}
                    </div>

                </div>

                {/* 사번 */}
                <div className="col-md-6 mb-3">

                    <div
                        className="text-medium-emphasis mb-1"
                    >
                        사번
                    </div>

                    <div>
                        {selectedEmployee.empNo || '-'}
                    </div>

                </div>

                {/* 로그인 아이디 */}
                <div className="col-md-6 mb-3">

                    <div
                        className="text-medium-emphasis mb-1"
                    >
                        아이디
                    </div>

                    <div>
                        {selectedEmployee.empId || '-'}
                    </div>

                </div>

            </div>

        </div>
    );
};

export default EmployeeProfileCard;