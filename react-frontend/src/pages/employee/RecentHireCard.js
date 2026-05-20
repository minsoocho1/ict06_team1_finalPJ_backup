import React, { useEffect, useMemo, useState } from 'react';

// API 호출용 axios
import axiosInstance from 'src/api/axiosInstance';

// CoreUI
import {
    CBadge,
    CCard,
    CCardBody,
    CCardHeader,
    CSpinner
} from '@coreui/react';

/*
 * 최근 입사자 카드 컴포넌트
 *
 * 역할:
 * - 전체 사원 조회
 * - 최근 30일 이내 입사자 조회
 * - 최근 입사자 목록 출력
 */
const RecentHireCard = () => {

    /*
     * 전체 사원 목록
     */
    const [employees, setEmployees] = useState([]);

    /*
     * 로딩 상태
     */
    const [loading, setLoading] = useState(true);

    /*
    * 현재 표시 개수
    *
    * 기본:
    * 5명
    */
    const [visibleCount, setVisibleCount] = useState(5);

    /*
     * 컴포넌트 최초 렌더링 시
     * 전체 사원 조회
     */
    useEffect(() => {
        fetchEmployees();
    }, []);

    /*
     * 전체 사원 조회 API
     */
    const fetchEmployees = async () => {

        try {

            const response = await axiosInstance.get(
                '/organization/employees/all'
            );

            console.log('전체 사원:', response.data);

            setEmployees(response.data);

        } catch (error) {

            console.error('전체 사원 조회 실패:', error);

        } finally {

            setLoading(false);

        }
    };

    /*
     * 최근 입사자 필터링
     *
     * 기준:
     * 현재 날짜 기준 30일 이내 입사자
     */
    const recentEmployees = useMemo(() => {

        const today = new Date();

        return employees
            .filter((employee) => {

                if (!employee.hireDate) {
                    return false;
                }

                const hireDate = new Date(employee.hireDate);

                // 날짜 차이 계산
                const diffDays =
                    (today - hireDate) / (1000 * 60 * 60 * 24);

                return diffDays >= 0 && diffDays <= 30;

            })
            .sort((a, b) => {
                return new Date(b.hireDate) - new Date(a.hireDate);
            });

    }, [employees]);

    /*
    * 현재 화면에 표시할 최근 입사자
    */
    const visibleEmployees = useMemo(() => {

        return recentEmployees.slice(0, visibleCount);

    }, [recentEmployees, visibleCount]);

    /*
     * 날짜 포맷
     */
    const formatDate = (dateString) => {

        if (!dateString) {
            return '-';
        }

        return dateString.replaceAll('-', '.');
    };

    /*
     * 로딩 중
     */
    if (loading) {

        return (
            <CCard className="shadow-sm border-0">
                <CCardBody className="text-center py-5">
                    <CSpinner />
                </CCardBody>
            </CCard>
        );
    }

    return (
        <CCard className="shadow-sm border-0 h-100">

            {/* 카드 헤더 */}
            <CCardHeader
                className="fw-bold"
                style={{
                    borderLeft: '4px solid #0D6EFD',
                }}
            >
                최근 입사자
            </CCardHeader>

            {/* 카드 바디 */}
            <CCardBody>

                {
                    recentEmployees.length === 0 && (

                        <div
                            className="text-center text-medium-emphasis py-4"
                        >
                            최근 입사자가 없습니다.
                        </div>
                    )
                }

                {
                    visibleEmployees.map((employee) => (

                        <div
                            key={employee.empNo}
                            className="d-flex align-items-center justify-content-between mb-3"
                            style={{
                                paddingBottom: '12px',
                                borderBottom: '1px solid #f1f1f1',
                            }}
                        >

                            {/* 왼쪽 영역 */}
                            <div className="d-flex align-items-center">

                                {/* 프로필 이니셜 */}
                                <div
                                    className="d-flex justify-content-center align-items-center"
                                    style={{
                                        width: '42px',
                                        height: '42px',
                                        borderRadius: '50%',
                                        backgroundColor: '#e7f1ff',
                                        color: '#0D6EFD',
                                        fontWeight: 'bold',
                                        marginRight: '12px',
                                        flexShrink: 0,
                                    }}
                                >
                                    {employee.name?.charAt(0)}
                                </div>

                                {/* 이름 / 부서 */}
                                <div>

                                    <div
                                        style={{
                                            fontWeight: 'bold',
                                            fontSize: '14px',
                                        }}
                                    >
                                        {employee.name}
                                    </div>

                                    <div
                                        className="text-medium-emphasis"
                                        style={{
                                            fontSize: '12px',
                                        }}
                                    >
                                        {employee.deptName}
                                    </div>

                                </div>

                            </div>

                            {/* 오른쪽 영역 */}
                            <div className="text-end">

                                <CBadge color="primary">
                                    신규
                                </CBadge>

                                <div
                                    className="text-medium-emphasis mt-1"
                                    style={{
                                        fontSize: '12px',
                                    }}
                                >
                                    {formatDate(employee.hireDate)}
                                </div>

                            </div>

                        </div>

                    ))
                }

                {/* ============================= */}
                {/* 더보기 / 접기 버튼 */}
                {/* ============================= */}
                {
                    recentEmployees.length > 5 && (

                        <div
                            className="text-center mt-3 d-flex justify-content-center gap-3"
                        >

                            {/* 더보기 버튼 */}
                            {
                                visibleCount < recentEmployees.length && (

                                    <button
                                        onClick={() => {

                                            setVisibleCount((prev) => prev + 5);

                                        }}
                                        style={{
                                            border: 'none',
                                            background: 'transparent',
                                            color: '#0D6EFD',
                                            fontWeight: 'bold',
                                            cursor: 'pointer',
                                            fontSize: '14px',
                                        }}
                                    >
                                        더보기
                                    </button>

                                )
                            }

                            {/* 접기 버튼 */}
                            {
                                visibleCount > 5 && (

                                    <button
                                        onClick={() => {

                                            setVisibleCount(5);

                                        }}
                                        style={{
                                            border: 'none',
                                            background: 'transparent',
                                            color: '#6c757d',
                                            fontWeight: 'bold',
                                            cursor: 'pointer',
                                            fontSize: '14px',
                                        }}
                                    >
                                        접기
                                    </button>

                                )
                            }

                        </div>
                    )
                }
            </CCardBody>

        </CCard>
    );
};

export default RecentHireCard;