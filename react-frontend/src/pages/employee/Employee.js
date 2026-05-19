import React, { useState } from 'react';
import RecentHireCard from './RecentHireCard';
import OrganizationSummaryCard from './OrganizationSummaryCard';

// CoreUI
import {
    CCard,
    CCardBody,
    CCardHeader,
    CCol,
    CRow,
    CModal,
    CModalHeader,
    CModalTitle,
    CModalBody
} from '@coreui/react';

// 조직도 컴포넌트
import OrganizationTree from './OrganizationTree';
import OrganizationEmployeeList from './OrganizationEmployeeList';
import EmployeeProfileCard from './EmployeeProfileCard';

/*
 * [사용자] 인사관리 메인 페이지
 *
 * 역할:
 * - 조직도 화면 레이아웃 구성
 * - 선택한 부서 상태 관리
 * - 선택한 사원 상태 관리
 * - 선택한 사원의 상세 정보를 모달로 표시
 */
const Employee = ({ userInfo }) => {

    /*
     * 현재 선택한 부서 정보
     */
    const [selectedDepartment, setSelectedDepartment] = useState({
        deptId: null,
        deptName: '',
    });

    /*
     * 현재 선택한 사원 정보
     *
     * 값이 있으면 상세 모달이 열린다.
     * null이면 상세 모달이 닫힌다.
     */
    const [selectedEmployee, setSelectedEmployee] = useState(null);

    /*
     * 사원 상세 모달 닫기
     */
    const closeEmployeeModal = () => {
        setSelectedEmployee(null);
    };

    return (
        <div className="container-fluid">

            {/* 페이지 제목 */}
            <div className="mb-4">
                <h2 className="fw-bold">내 정보 및 조직도</h2>

                <div className="text-medium-emphasis">
                    조직도 및 구성원 정보를 조회할 수 있습니다.
                </div>
            </div>

            {/* 전체 레이아웃 */}
            <CRow>

                {/* ============================= */}
                {/* 왼쪽 조직도 영역 */}
                {/* ============================= */}
                <CCol lg={3}>

                    <CCard className="shadow-sm border-0 h-100">

                        <CCardHeader className="fw-bold">
                            조직도
                        </CCardHeader>

                        <CCardBody
                            style={{
                                height: '75vh',
                                overflowY: 'auto',
                            }}
                        >

                            {/* 조직도 트리 */}
                            <OrganizationTree
                                selectedDepartment={selectedDepartment}
                                setSelectedDepartment={setSelectedDepartment}
                                setSelectedEmployee={setSelectedEmployee}
                                userInfo={userInfo}
                            />

                        </CCardBody>

                    </CCard>

                </CCol>

                {/* ============================= */}
                {/* 오른쪽 영역 */}
                {/* ============================= */}
                <CCol lg={9}>

                    <CRow>

                        {/* ============================= */}
                        {/* 사원 목록 영역 */}
                        {/* ============================= */}
                        <CCol lg={12} className="mb-3">

                            <CCard className="shadow-sm border-0">

                                <CCardHeader className="fw-bold">

                                    {
                                        selectedDepartment?.deptName
                                            ? `${selectedDepartment.deptName} 구성원`
                                            : '구성원 목록'
                                    }

                                </CCardHeader>

                                <CCardBody
                                    style={{
                                        height: '50vh',
                                        overflowY: 'auto',
                                    }}
                                >

                                    <OrganizationEmployeeList
                                        selectedDepartment={selectedDepartment}
                                        selectedEmployee={selectedEmployee}
                                        setSelectedEmployee={setSelectedEmployee}
                                    />

                                </CCardBody>

                            </CCard>

                        </CCol>

                        {/* ============================= */}
                        {/* 하단 정보 영역 */}
                        {/* ============================= */}
                        <CCol lg={12}>

                            <CRow>

                                {/* ============================= */}
                                {/* 조직 통계 */}
                                {/* ============================= */}
                                <CCol lg={5} className="mb-3">

                                    <OrganizationSummaryCard />

                                </CCol>

                                {/* ============================= */}
                                {/* 최근 입사자 */}
                                {/* ============================= */}
                                <CCol lg={7} className="mb-3">

                                    <RecentHireCard />

                                </CCol>

                            </CRow>

                        </CCol>

                    </CRow>

                </CCol>

            </CRow>

            {/* ============================= */}
            {/* 사원 상세 모달 */}
            {/* ============================= */}
            <CModal
                visible={selectedEmployee !== null}
                onClose={closeEmployeeModal}
                alignment="center"
                size="lg"
            >
                <CModalHeader>
                    <CModalTitle>
                        구성원 상세 정보
                    </CModalTitle>
                </CModalHeader>

                <CModalBody>
                    <EmployeeProfileCard
                        selectedEmployee={selectedEmployee}
                    />
                </CModalBody>
            </CModal>

        </div>
    );
};

export default Employee;