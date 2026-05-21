import React, { useEffect, useMemo, useState } from 'react';

// API 호출용 axios
import axiosInstance from 'src/api/axiosInstance';

// CoreUI
import {
    CCard,
    CCardBody,
    CCardHeader,
    CCol,
    CRow,
    CSpinner,
    CModal,
    CModalHeader,
    CModalTitle,
    CModalBody,
    CBadge
} from '@coreui/react';

/*
 * 조직 통계 카드
 *
 * 역할:
 * - 전체 조직 통계 표시
 * - 총 구성원
 * - 재직 인원
 * - 휴직 인원
 * - 본부 수
 * - 팀 수
 * - 통계 클릭 시 상세 모달 표시
 */
const OrganizationSummaryCard = () => {

    /*
     * 전체 사원 목록
     */
    const [employees, setEmployees] = useState([]);

    /*
     * 전체 부서 목록
     */
    const [departments, setDepartments] = useState([]);

    /*
     * 로딩 상태
     */
    const [loading, setLoading] = useState(true);

    /*
     * 통계 상세 모달 상태
     */
    const [modalVisible, setModalVisible] = useState(false);

    /*
     * 현재 선택한 통계 타입
     */
    const [modalType, setModalType] = useState('');

    /*
     * 모달 현재 표시 개수
     *
     * 기본:
     * 5개
     */
    const [visibleCount, setVisibleCount] = useState(5);

    /*
     * 최초 렌더링 시 데이터 조회
     */
    useEffect(() => {

        fetchData();

    }, []);

    /*
     * 조직 데이터 조회
     */
    const fetchData = async () => {

        try {

            const [employeeResponse, departmentResponse] =
                await Promise.all([

                    axiosInstance.get('/organization/employees/all'),
                    axiosInstance.get('/organization/departments/tree'),

                ]);

            setEmployees(employeeResponse.data);
            setDepartments(departmentResponse.data);

        } catch (error) {

            console.error('조직 통계 조회 실패:', error);

        } finally {

            setLoading(false);

        }
    };

    /*
     * 통계 상세 모달 열기
     */
    const openModal = (type) => {

        // 모달 타입 설정
        setModalType(type);

        // 표시 개수 초기화
        setVisibleCount(5);

        // 모달 열기
        setModalVisible(true);
    };

    /*
     * 총 구성원 수
     */
    const totalEmployees = employees.length;

    /*
     * 재직 인원 수
     */
    const activeEmployees = useMemo(() => {

        return employees.filter(
            (employee) => employee.status === '재직'
        ).length;

    }, [employees]);

    /*
     * 휴직 인원 수
     */
    const leaveEmployees = useMemo(() => {

        return employees.filter(
            (employee) => employee.status === '휴직'
        ).length;

    }, [employees]);

    /*
     * 본부 수 계산
     */
    const headquartersCount = useMemo(() => {

        return departments.length;

    }, [departments]);

    /*
     * 팀 수 계산
     */
    const teamCount = useMemo(() => {

        let count = 0;

        const countTeams = (departmentList) => {

            departmentList.forEach((department) => {

                if (department.children?.length > 0) {

                    count += department.children.length;

                    countTeams(department.children);
                }
            });
        };

        countTeams(departments);

        return count;

    }, [departments]);

    /*
     * 모달에 표시할 데이터 목록
     */
    const modalData = useMemo(() => {

        // 전체 사원 목록
        if (modalType === '전체') {

            return employees;
        }

        // 재직 사원 목록
        if (modalType === '재직') {

            return employees.filter(
                (employee) => employee.status === '재직'
            );
        }

        // 휴직 사원 목록
        if (modalType === '휴직') {

            return employees.filter(
                (employee) => employee.status === '휴직'
            );
        }

        // 본부 목록
        if (modalType === '본부') {

            return departments.map(
                (department) => department.deptName
            );
        }

        // 팀 목록
        if (modalType === '팀') {

            const teams = [];

            const extractTeams = (departmentList) => {

                departmentList.forEach((department) => {

                    if (department.children?.length > 0) {

                        department.children.forEach((team) => {

                            teams.push(team.deptName);

                        });

                        extractTeams(department.children);
                    }
                });
            };

            extractTeams(departments);

            return teams;
        }

        return [];

    }, [modalType, employees, departments]);

    /*
     * 현재 화면에 표시할 모달 데이터
     */
    const visibleModalData = useMemo(() => {

        return modalData.slice(0, visibleCount);

    }, [modalData, visibleCount]);

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
        <>
            <CCard className="shadow-sm border-0 h-100">

                {/* 카드 헤더 */}
                <CCardHeader
                    className="fw-bold"
                    style={{
                        borderLeft: '4px solid #321fdb',
                    }}
                >
                    조직 통계
                </CCardHeader>

                {/* 카드 바디 */}
                <CCardBody>

                    <CRow className="g-3">

                        {/* 총 구성원 */}
                        <CCol xs={6}>

                            <div
                                onClick={() => openModal('전체')}
                                style={{
                                    cursor: 'pointer',
                                    backgroundColor: '#f8f9ff',
                                    borderRadius: '12px',
                                    padding: '18px',
                                }}
                            >

                                <div
                                    className="text-medium-emphasis mb-2"
                                    style={{
                                        fontSize: '13px',
                                    }}
                                >
                                    총 구성원
                                </div>

                                <div
                                    style={{
                                        fontSize: '28px',
                                        fontWeight: 'bold',
                                        color: '#321fdb',
                                    }}
                                >
                                    {totalEmployees}
                                </div>

                            </div>

                        </CCol>

                        {/* 재직 */}
                        <CCol xs={6}>

                            <div
                                onClick={() => openModal('재직')}
                                style={{
                                    cursor: 'pointer',
                                    backgroundColor: '#f4fff7',
                                    borderRadius: '12px',
                                    padding: '18px',
                                }}
                            >

                                <div
                                    className="text-medium-emphasis mb-2"
                                    style={{
                                        fontSize: '13px',
                                    }}
                                >
                                    재직 중
                                </div>

                                <div
                                    style={{
                                        fontSize: '28px',
                                        fontWeight: 'bold',
                                        color: '#198754',
                                    }}
                                >
                                    {activeEmployees}
                                </div>

                            </div>

                        </CCol>

                        {/* 휴직 */}
                        <CCol xs={6}>

                            <div
                                onClick={() => openModal('휴직')}
                                style={{
                                    cursor: 'pointer',
                                    backgroundColor: '#fffaf1',
                                    borderRadius: '12px',
                                    padding: '18px',
                                }}
                            >

                                <div
                                    className="text-medium-emphasis mb-2"
                                    style={{
                                        fontSize: '13px',
                                    }}
                                >
                                    휴직 중
                                </div>

                                <div
                                    style={{
                                        fontSize: '28px',
                                        fontWeight: 'bold',
                                        color: '#fd7e14',
                                    }}
                                >
                                    {leaveEmployees}
                                </div>

                            </div>

                        </CCol>

                        {/* 본부 수 */}
                        <CCol xs={6}>

                            <div
                                onClick={() => openModal('본부')}
                                style={{
                                    cursor: 'pointer',
                                    backgroundColor: '#f5f5f5',
                                    borderRadius: '12px',
                                    padding: '18px',
                                }}
                            >

                                <div
                                    className="text-medium-emphasis mb-2"
                                    style={{
                                        fontSize: '13px',
                                    }}
                                >
                                    본부 수
                                </div>

                                <div
                                    style={{
                                        fontSize: '28px',
                                        fontWeight: 'bold',
                                        color: '#495057',
                                    }}
                                >
                                    {headquartersCount}
                                </div>

                            </div>

                        </CCol>

                        {/* 팀 수 */}
                        <CCol xs={12}>

                            <div
                                onClick={() => openModal('팀')}
                                style={{
                                    cursor: 'pointer',
                                    backgroundColor: '#f8f9ff',
                                    borderRadius: '12px',
                                    padding: '18px',
                                }}
                            >

                                <div
                                    className="text-medium-emphasis mb-2"
                                    style={{
                                        fontSize: '13px',
                                    }}
                                >
                                    팀 수
                                </div>

                                <div
                                    style={{
                                        fontSize: '28px',
                                        fontWeight: 'bold',
                                        color: '#321fdb',
                                    }}
                                >
                                    {teamCount}
                                </div>

                            </div>

                        </CCol>

                    </CRow>

                </CCardBody>

            </CCard>

            {/* ============================= */}
            {/* 통계 상세 모달 */}
            {/* ============================= */}
            <CModal
                visible={modalVisible}
                onClose={() => setModalVisible(false)}
                alignment="center"
            >

                <CModalHeader>
                    <CModalTitle>

                        {
                            modalType === '전체'
                                ? '전체 구성원'

                                : modalType === '재직'
                                    ? '재직 중인 구성원'

                                    : modalType === '휴직'
                                        ? '휴직 중인 구성원'

                                        : modalType === '본부'
                                            ? '본부 목록'

                                            : '팀 목록'
                        }

                    </CModalTitle>
                </CModalHeader>

                <CModalBody>

                    {
                        modalData.length === 0 && (

                            <div className="text-center text-medium-emphasis py-4">
                                데이터가 없습니다.
                            </div>
                        )
                    }

                    {/* 사원 목록 */}
                    {
                        (
                            modalType === '전체' ||
                            modalType === '재직' ||
                            modalType === '휴직'
                        ) &&
                        visibleModalData.map((employee) => (

                            <div
                                key={employee.empNo}
                                className="d-flex justify-content-between align-items-center mb-3"
                                style={{
                                    paddingBottom: '10px',
                                    borderBottom: '1px solid #f1f1f1',
                                }}
                            >

                                <div>

                                    <div style={{ fontWeight: 'bold' }}>
                                        {employee.name}
                                    </div>

                                    <div
                                        className="text-medium-emphasis"
                                        style={{ fontSize: '13px' }}
                                    >
                                        {employee.deptName}
                                    </div>

                                </div>

                                <CBadge
                                    color={
                                        employee.status === '재직'
                                            ? 'success'
                                            : 'warning'
                                    }
                                >
                                    {employee.status}
                                </CBadge>

                            </div>

                        ))
                    }

                    {/* 본부 / 팀 목록 */}
                    {
                        (
                            modalType === '본부' ||
                            modalType === '팀'
                        ) &&
                        visibleModalData.map((name, index) => (

                            <div
                                key={index}
                                className="mb-3"
                                style={{
                                    paddingBottom: '10px',
                                    borderBottom: '1px solid #f1f1f1',
                                    fontWeight: 'bold',
                                }}
                            >
                                {name}
                            </div>

                        ))
                    }

                    {/* ============================= */}
                    {/* 더보기 / 접기 버튼 */}
                    {/* ============================= */}
                    {
                        modalData.length > 5 && (

                            <div
                                className="d-flex justify-content-center gap-3 mt-4"
                            >

                                {/* 더보기 */}
                                {
                                    visibleCount < modalData.length && (

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
                                            }}
                                        >
                                            더보기
                                        </button>

                                    )
                                }

                                {/* 접기 */}
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
                                            }}
                                        >
                                            접기
                                        </button>

                                    )
                                }

                            </div>
                        )
                    }

                </CModalBody>

            </CModal>
        </>
    );
};

export default OrganizationSummaryCard;