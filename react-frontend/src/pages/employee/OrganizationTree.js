import React, { useEffect, useState } from 'react';

// API 호출
import axiosInstance from 'src/api/axiosInstance';

// CoreUI
import {
    CButton,
    CSpinner
} from '@coreui/react';

// 아이콘
import CIcon from '@coreui/icons-react';

import {
    cilCaretRight,
    cilCaretBottom,
    cilBuilding,
    cilPeople,
    cilExpandDown,
    cilChevronDoubleUp,
    cilUser
} from '@coreui/icons';

/*
 * 조직도 트리 컴포넌트
 *
 * 역할:
 * - 전체 조직도 트리 표시
 * - 본부 / 팀 구조 표시
 * - 부서 선택 기능
 * - 전체 펼치기 / 접기
 * - 내 부서 바로가기
 */
const OrganizationTree = ({
    selectedDepartment,
    setSelectedDepartment,
    setSelectedEmployee,
    userInfo,
}) => {

    /*
     * 조직도 데이터
     */
    const [departments, setDepartments] = useState([]);

    /*
     * 로딩 상태
     */
    const [loading, setLoading] = useState(true);

    /*
     * 펼쳐진 본부 목록
     */
    const [expandedDepartments, setExpandedDepartments] = useState([]);

    /*
     * 최초 렌더링 시 조직도 조회
     */
    useEffect(() => {

        fetchDepartments();

    }, []);

    /*
     * 조직도 데이터 조회
     */
    const fetchDepartments = async () => {

        try {

            const response = await axiosInstance.get(
                '/organization/departments/tree'
            );

            console.log('조직도 데이터:', response.data);

            setDepartments(response.data);

            /*
             * 기본:
             * 첫 번째 본부 펼치기
             */
            if (response.data.length > 0) {

                setExpandedDepartments([
                    response.data[0].deptId
                ]);
            }

        } catch (error) {

            console.error('조직도 조회 실패:', error);

        } finally {

            setLoading(false);

        }
    };

    /*
     * 본부 펼침/접기 처리
     */
    const toggleDepartment = (deptId) => {

        setExpandedDepartments((prev) => {

            // 이미 열려있으면 닫기
            if (prev.includes(deptId)) {

                return prev.filter((id) => id !== deptId);
            }

            // 닫혀있으면 열기
            return [...prev, deptId];
        });
    };

    /*
     * 전체 펼치기
     */
    const handleExpandAll = () => {

        const allDeptIds = departments.map(
            (department) => department.deptId
        );

        setExpandedDepartments(allDeptIds);
    };

    /*
     * 전체 접기
     */
    const handleCollapseAll = () => {

        setExpandedDepartments([]);
    };

    /*
    * 내 부서 바로가기
    */
    const handleMoveMyDepartment = () => {

        console.log('userInfo:', userInfo);

        /*
        * 부서 정보 없는 경우
        */
        if (!userInfo?.deptId) {

            console.warn('부서 정보 없음');

            return;
        }

        /*
        * 숫자 타입 변환
        */
        const myDeptId = Number(
            userInfo.deptId
        );

        console.log('내 부서 ID:', myDeptId);

        /*
        * 내 부서가 포함된 본부 펼치기
        */
        departments.forEach((department) => {

            const hasMyDepartment =
                department.children?.some(
                    (child) =>
                        child.deptId === myDeptId
                );

            console.log(
                department.deptName,
                hasMyDepartment
            );

            if (hasMyDepartment) {

                setExpandedDepartments((prev) => {

                    if (prev.includes(department.deptId)) {

                        return prev;
                    }

                    return [...prev, department.deptId];
                });
            }
        });

        /*
        * 내 부서 선택
        */
        setSelectedDepartment({

            deptId: myDeptId,
            deptName: userInfo.deptName,
        });

        /*
        * 선택된 사원 초기화
        */
        setSelectedEmployee(null);
    };

    /*
     * 본부 클릭
     */
    const handleParentClick = (department) => {

        setSelectedDepartment({
            deptId: department.deptId,
            deptName: department.deptName,
        });

        setSelectedEmployee(null);

        toggleDepartment(department.deptId);
    };

    /*
     * 팀 클릭
     */
    const handleChildClick = (department) => {

        setSelectedDepartment({
            deptId: department.deptId,
            deptName: department.deptName,
        });

        setSelectedEmployee(null);
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
            {/* 상단 액션 버튼 */}
            {/* ============================= */}
            <div
                className="d-flex gap-2 mb-3"
                style={{
                    flexWrap: 'wrap',
                }}
            >

                {/* 전체 펼치기 */}
                <CButton
                    size="sm"
                    color="light"
                    onClick={handleExpandAll}
                    style={{
                        borderRadius: '10px',
                        fontWeight: '600',
                    }}
                >
                    <CIcon
                        icon={cilExpandDown}
                        className="me-1"
                    />

                    전체 펼치기
                </CButton>

                {/* 전체 접기 */}
                <CButton
                    size="sm"
                    color="light"
                    onClick={handleCollapseAll}
                    style={{
                        borderRadius: '10px',
                        fontWeight: '600',
                    }}
                >
                    <CIcon
                        icon={cilChevronDoubleUp}
                        className="me-1"
                    />

                    전체 접기
                </CButton>

                {/* 내 부서 */}
                <CButton
                    size="sm"
                    color="primary"
                    variant="outline"
                    onClick={handleMoveMyDepartment}
                    style={{
                        borderRadius: '10px',
                        fontWeight: '600',
                    }}
                >
                    <CIcon
                        icon={cilUser}
                        className="me-1"
                    />

                    내 부서
                </CButton>

            </div>

            {
                departments.map((department) => {

                    const isExpanded =
                        expandedDepartments.includes(
                            department.deptId
                        );

                    const isSelectedParent =
                        selectedDepartment?.deptId === department.deptId;

                    return (

                        <div
                            key={department.deptId}
                            className="mb-2"
                        >

                            {/* ============================= */}
                            {/* 본부 */}
                            {/* ============================= */}
                            <div
                                onClick={() => handleParentClick(department)}
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between',
                                    padding: '12px 14px',
                                    borderRadius: '14px',
                                    cursor: 'pointer',
                                    transition: 'all 0.2s ease',
                                    backgroundColor: isSelectedParent
                                        ? '#EEF2FF'
                                        : '#FFFFFF',
                                    border: isSelectedParent
                                        ? '1px solid #C7D2FE'
                                        : '1px solid transparent',
                                    marginBottom: '6px',
                                }}
                                onMouseEnter={(e) => {

                                    if (!isSelectedParent) {

                                        e.currentTarget.style.backgroundColor =
                                            '#F8FAFC';
                                    }
                                }}
                                onMouseLeave={(e) => {

                                    if (!isSelectedParent) {

                                        e.currentTarget.style.backgroundColor =
                                            '#FFFFFF';
                                    }
                                }}
                            >

                                {/* 왼쪽 */}
                                <div
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '10px',
                                    }}
                                >

                                    {/* 펼침 아이콘 */}
                                    <div
                                        style={{
                                            color: '#64748B',
                                            display: 'flex',
                                            alignItems: 'center',
                                        }}
                                    >

                                        <CIcon
                                            icon={
                                                isExpanded
                                                    ? cilCaretBottom
                                                    : cilCaretRight
                                            }
                                            size="sm"
                                        />

                                    </div>

                                    {/* 본부 아이콘 */}
                                    <div
                                        style={{
                                            width: '28px',
                                            height: '28px',
                                            borderRadius: '10px',
                                            backgroundColor: '#E0E7FF',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                        }}
                                    >

                                        <CIcon
                                            icon={cilBuilding}
                                            size="sm"
                                            style={{
                                                color: '#4338CA',
                                            }}
                                        />

                                    </div>

                                    {/* 본부명 */}
                                    <div
                                        style={{
                                            fontWeight: '600',
                                            fontSize: '14px',
                                            color: '#1E293B',
                                        }}
                                    >
                                        {department.deptName}
                                    </div>

                                </div>

                                {/* 팀 개수 */}
                                <div
                                    style={{
                                        fontSize: '12px',
                                        color: '#94A3B8',
                                        fontWeight: '500',
                                    }}
                                >
                                    {department.children?.length || 0}팀
                                </div>

                            </div>

                            {/* ============================= */}
                            {/* 하위 팀 */}
                            {/* ============================= */}
                            {
                                isExpanded &&
                                department.children?.length > 0 && (

                                    <div
                                        style={{
                                            marginLeft: '18px',
                                            borderLeft: '1px solid #E2E8F0',
                                            paddingLeft: '12px',
                                            marginTop: '6px',
                                        }}
                                    >

                                        {
                                            department.children.map((child) => {

                                                const isSelectedChild =
                                                    selectedDepartment?.deptId === child.deptId;

                                                return (

                                                    <div
                                                        key={child.deptId}
                                                        onClick={() => handleChildClick(child)}
                                                        style={{
                                                            display: 'flex',
                                                            alignItems: 'center',
                                                            justifyContent: 'space-between',
                                                            padding: '10px 14px',
                                                            borderRadius: '12px',
                                                            cursor: 'pointer',
                                                            transition: 'all 0.2s ease',
                                                            backgroundColor: isSelectedChild
                                                                ? '#EFF6FF'
                                                                : '#FFFFFF',
                                                            border: isSelectedChild
                                                                ? '1px solid #BFDBFE'
                                                                : '1px solid transparent',
                                                            marginBottom: '6px',
                                                        }}
                                                        onMouseEnter={(e) => {

                                                            if (!isSelectedChild) {

                                                                e.currentTarget.style.backgroundColor =
                                                                    '#F8FAFC';
                                                            }
                                                        }}
                                                        onMouseLeave={(e) => {

                                                            if (!isSelectedChild) {

                                                                e.currentTarget.style.backgroundColor =
                                                                    '#FFFFFF';
                                                            }
                                                        }}
                                                    >

                                                        {/* 왼쪽 */}
                                                        <div
                                                            style={{
                                                                display: 'flex',
                                                                alignItems: 'center',
                                                                gap: '10px',
                                                            }}
                                                        >

                                                            {/* 점 */}
                                                            <div
                                                                style={{
                                                                    width: '8px',
                                                                    height: '8px',
                                                                    borderRadius: '999px',
                                                                    backgroundColor: isSelectedChild
                                                                        ? '#2563EB'
                                                                        : '#CBD5E1',
                                                                }}
                                                            />

                                                            {/* 팀 아이콘 */}
                                                            <CIcon
                                                                icon={cilPeople}
                                                                size="sm"
                                                                style={{
                                                                    color: '#64748B',
                                                                }}
                                                            />

                                                            {/* 팀명 */}
                                                            <div
                                                                style={{
                                                                    fontSize: '14px',
                                                                    fontWeight: '500',
                                                                    color: isSelectedChild
                                                                        ? '#1D4ED8'
                                                                        : '#334155',
                                                                }}
                                                            >
                                                                {child.deptName}
                                                            </div>

                                                        </div>

                                                    </div>
                                                );
                                            })
                                        }

                                    </div>
                                )
                            }

                        </div>
                    );
                })
            }

        </div>
    );
};

export default OrganizationTree;