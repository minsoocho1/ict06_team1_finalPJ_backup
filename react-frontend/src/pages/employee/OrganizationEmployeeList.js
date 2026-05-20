import React, { useEffect, useMemo, useState } from 'react';

// API 호출용 axios
import axiosInstance from 'src/api/axiosInstance';

// CoreUI
import {
    CBadge,
    CFormInput,
    CSpinner
} from '@coreui/react';

/*
 * 조직도 사원 목록 컴포넌트
 *
 * 역할:
 * - 선택한 부서의 사원 목록 조회
 * - 사원 검색
 * - 직급 그룹핑
 * - 사원 클릭 처리
 * - 현재 선택한 사원 active 상태 표시
 */
const OrganizationEmployeeList = ({
    selectedDepartment,
    selectedEmployee,
    setSelectedEmployee,
}) => {

    /*
     * 사원 목록 데이터
     */
    const [employees, setEmployees] = useState([]);

    /*
     * 검색어 상태
     */
    const [keyword, setKeyword] = useState('');

    /*
     * 로딩 상태
     */
    const [loading, setLoading] = useState(false);

    /*
     * 직급 정렬 우선순위
     *
     * 숫자가 낮을수록 상위 직급
     */
    const positionOrder = {
        '수석': 1,
        '주임': 2,
        '선임': 3,
        '책임': 4,
        '사원': 5,
    };

    /*
     * 선택 부서 변경 시
     * 사원 목록 다시 조회
     */
    useEffect(() => {

        // 선택 부서 없으면 초기화
        if (!selectedDepartment?.deptId) {
            setEmployees([]);
            setKeyword('');
            return;
        }

        // 부서가 변경되면 검색어 초기화
        setKeyword('');

        fetchEmployees();

    }, [selectedDepartment]);

    /*
     * 부서별 사원 목록 조회
     */
    const fetchEmployees = async () => {

        try {

            setLoading(true);

            const response = await axiosInstance.get(
                `/organization/employees?deptId=${selectedDepartment.deptId}`
            );

            console.log('사원 목록:', response.data);

            setEmployees(response.data);

        } catch (error) {

            console.error('사원 목록 조회 실패:', error);

        } finally {

            setLoading(false);

        }
    };

    /*
     * 검색 필터링된 사원 목록
     */
    const filteredEmployees = useMemo(() => {

        // 검색어 없으면 전체 반환
        if (!keyword.trim()) {
            return employees;
        }

        const lowerKeyword = keyword.toLowerCase();

        return employees.filter((employee) => {

            return (
                employee.name?.toLowerCase().includes(lowerKeyword) ||
                employee.positionName?.toLowerCase().includes(lowerKeyword) ||
                employee.deptName?.toLowerCase().includes(lowerKeyword) ||
                employee.empNo?.toLowerCase().includes(lowerKeyword)
            );
        });

    }, [employees, keyword]);

    /*
     * 직급별 그룹핑 처리
     */
    const groupedEmployees = useMemo(() => {

        const grouped = {};

        // 직급별 그룹 생성
        filteredEmployees.forEach((employee) => {

            const positionName = employee.positionName || '기타';

            if (!grouped[positionName]) {
                grouped[positionName] = [];
            }

            grouped[positionName].push(employee);
        });

        // 직급 정렬
        return Object.entries(grouped).sort((a, b) => {

            const orderA = positionOrder[a[0]] || 999;
            const orderB = positionOrder[b[0]] || 999;

            return orderA - orderB;
        });

    }, [filteredEmployees]);

    /*
     * 사원 클릭 처리
     */
    const handleEmployeeClick = (employee) => {

        setSelectedEmployee(employee);
    };

    /*
     * 로딩 중
     */
    if (loading) {
        return (
            <div className="text-center py-5">
                <CSpinner />
            </div>
        );
    }

    return (
        <div>

            {/* ============================= */}
            {/* 검색 영역 */}
            {/* ============================= */}
            <div className="mb-3">

                <CFormInput
                    placeholder="이름, 사번, 부서 또는 직급 검색"
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                />

            </div>

            {/* ============================= */}
            {/* 검색 결과 없음 */}
            {/* ============================= */}
            {
                groupedEmployees.length === 0 && (
                    <div
                        className="text-center text-medium-emphasis py-5"
                    >
                        검색 결과가 없습니다.
                    </div>
                )
            }

            {/* ============================= */}
            {/* 직급 그룹 */}
            {/* ============================= */}
            {
                groupedEmployees.map(([positionName, employeeList]) => (

                    <div
                        key={positionName}
                        className="mb-4"
                    >

                        {/* 직급 헤더 */}
                        <div
                            style={{
                                fontSize: '14px',
                                fontWeight: 'bold',
                                color: '#321fdb',
                                marginBottom: '10px',
                                paddingBottom: '6px',
                                borderBottom: '2px solid #e9ecef',
                            }}
                        >
                            {positionName}
                            <span
                                style={{
                                    marginLeft: '8px',
                                    color: '#6c757d',
                                    fontSize: '12px',
                                    fontWeight: 'normal',
                                }}
                            >
                                {employeeList.length}명
                            </span>
                        </div>

                        {/* 사원 목록 */}
                        {
                            employeeList.map((employee) => {

                                /*
                                 * 현재 선택된 사원인지 확인
                                 */
                                const isSelected = selectedEmployee?.empNo === employee.empNo;

                                return (
                                    <div
                                        key={employee.empNo}
                                        onClick={() => handleEmployeeClick(employee)}
                                        style={{
                                            padding: '14px 16px',
                                            borderBottom: '1px solid #f1f1f1',
                                            cursor: 'pointer',
                                            transition: '0.2s',
                                            borderRadius: '8px',
                                            backgroundColor: isSelected
                                                ? '#eef1ff'
                                                : 'transparent',
                                            borderLeft: isSelected
                                                ? '4px solid #321fdb'
                                                : '4px solid transparent',
                                            boxShadow: isSelected
                                                ? '0 2px 8px rgba(50, 31, 219, 0.12)'
                                                : 'none',
                                        }}
                                        onMouseEnter={(e) => {
                                            if (!isSelected) {
                                                e.currentTarget.style.backgroundColor = '#e7ebff';
                                            }
                                        }}
                                        onMouseLeave={(e) => {
                                            if (!isSelected) {
                                                e.currentTarget.style.backgroundColor = 'transparent';
                                            }
                                        }}
                                    >

                                        {/* 상단 */}
                                        <div
                                            className="d-flex justify-content-between align-items-center"
                                        >

                                            {/* 이름 */}
                                            <div>

                                                <div
                                                    style={{
                                                        fontWeight: 'bold',
                                                        fontSize: '15px',
                                                    }}
                                                >
                                                    {employee.name}
                                                </div>

                                                <div
                                                    className="text-medium-emphasis"
                                                    style={{
                                                        fontSize: '13px',
                                                    }}
                                                >
                                                    {employee.deptName}
                                                </div>

                                            </div>

                                            {/* 상태 */}
                                            <CBadge
                                                color={
                                                    employee.status === '재직'
                                                        ? 'success'
                                                        : employee.status === '휴직'
                                                            ? 'warning'
                                                            : 'secondary'
                                                }
                                            >
                                                {employee.status}
                                            </CBadge>

                                        </div>

                                    </div>
                                );
                            })
                        }

                    </div>

                ))
            }

        </div>
    );
};

export default OrganizationEmployeeList;