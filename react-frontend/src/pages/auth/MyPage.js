/**
 * @FileName : MyPage.js
 * @Description : 마이페이지(내 정보 조회/수정)
 * @Author : 김다솜
 * @Date : 2026. 04. 22
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.22    김다솜        최초 생성 및 화면 구성
 * @ 2026.04.23    김다솜        내 정보 조회/수정 구현
 * @ 2026.04.30    김다솜        스타일 코드 분리(MyPageStyle.js) 및 UI 구조 개선
 * @ 2026.05.19    김다솜        마이페이지 기본 프로필 이미지 제거
 * @ 2026.05.28    김다솜        사이트 공통 톤 반영, 정보/급여 탭 구조 정리 및 급여 기본급 데이터 연결
 * @ 2026.05.29    김다솜        급여 탭-기본급 데이터 연동, 프로필 사진 추가·수정 기능 및 관리자 기준 이미지 형식/최신 정보 재조회 적용
 */

import {
    CAvatar,
    CBadge,
    CButton,
    CCard,
    CCardBody,
    CCol,
    CFormInput,
    CNav,
    CNavItem,
    CNavLink,
    CRow,
} from '@coreui/react';
import React, { useEffect, useRef, useState } from 'react';

import CIcon from '@coreui/icons-react';
import { cilCamera, cilCheckAlt, cilPencil, cilUser, cilX } from '@coreui/icons';
import axiosInstance from 'src/api/axiosInstance';
import { PATH } from 'src/constants/path';
import { useUser } from 'src/api/UserContext';
import {
    accountInfoGroup,
    activeStatusBadge,
    editButtonStyle,
    fieldCardStyle,
    fieldLabelStyle,
    fieldRowStyle,
    fieldValueStyle,
    pageWrapStyle,
    profileAvatar,
    profileAvatarWrap,
    profileCover,
    profileHeader,
    profileInfoBlock,
    profileMetaStyle,
    profileNameStyle,
    saveButtonStyle,
    tabLinkStyle,
    uploadProfileButtonStyle,
    valueGroup,
} from 'src/styles/js/auth/MyPageStyle';

const TABS = [
    { id: 'info', label: '정보' },
    { id: 'payroll', label: '급여' },
];

const MyPage = () => {
    const { userInfo, updateUserInfo, refreshUserInfo } = useUser();
    const [isEditing, setIsEditing] = useState(false);
    const [activeTab, setActiveTab] = useState('info');
    const [payrollInfo, setPayrollInfo] = useState(null);
    const [payrollLoading, setPayrollLoading] = useState(false);
    const [payrollError, setPayrollError] = useState('');
    const [profileUploading, setProfileUploading] = useState(false);
    const profileFileInputRef = useRef(null);
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        phone: '',
    });

    useEffect(() => {
        if (userInfo) {
            setFormData({
                name: userInfo.name || '',
                email: userInfo.email || '',
                phone: userInfo.phone || '',
            });
        }
    }, [userInfo]);

    useEffect(() => {
        if (activeTab !== 'payroll' || payrollInfo || payrollLoading) {
            return;
        }

        const fetchPayrollInfo = async () => {
            setPayrollLoading(true);
            setPayrollError('');

            try {
                const response = await axiosInstance.get('/payroll/statements/me');
                setPayrollInfo(response.data);
            } catch (error) {
                console.error('마이페이지 급여 정보 조회 실패: ', error);
                setPayrollError('급여관리 데이터를 불러오지 못했습니다.');
            } finally {
                setPayrollLoading(false);
            }
        };

        fetchPayrollInfo();
    }, [activeTab, payrollInfo, payrollLoading]);

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleUpdate = async () => {
        try {
            const payload = {
                ...formData,
                empNo: userInfo.empNo || userInfo.emp_no,
            };

            const response = await axiosInstance.put('/user/update', payload);

            if (response.status === 200) {
                const refreshedUserInfo = await refreshUserInfo?.();

                if (!refreshedUserInfo) {
                    updateUserInfo({ ...userInfo, ...formData });
                }

                setIsEditing(false);
            }
        } catch (error) {
            console.error('마이페이지 정보 수정 실패: ', error);
            alert('정보 수정 중 오류가 발생했습니다.');
        }
    };

    const handleProfileImageChange = async (event) => {
        const file = event.target.files?.[0];

        if (!file) {
            return;
        }

        const uploadData = new FormData();
        uploadData.append('file', file);
        setProfileUploading(true);

        try {
            const response = await axiosInstance.post('/user/profile-image', uploadData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });

            updateUserInfo({ ...userInfo, ...response.data });
            await refreshUserInfo?.();
        } catch (error) {
            console.error('프로필 사진 업로드 실패: ', error);
            alert(error.response?.data?.message || '프로필 사진 업로드 중 오류가 발생했습니다.');
        } finally {
            setProfileUploading(false);
            event.target.value = '';
        }
    };

    if (!userInfo) {
        return <div className="p-4 text-center">Loading...</div>;
    }

    const profileImage = userInfo?.profileImg || userInfo?.profile_img;
    const backendOrigin = PATH.API.BASE.replace(/\/api$/, '');
    const profileImageUrl = profileImage?.startsWith('/employee/uploads/')
        ? `${backendOrigin}${profileImage}`
        : profileImage;
    const empNo = userInfo?.empNo || userInfo?.emp_no || '-';
    const departmentName =
        userInfo?.department?.deptName ||
        userInfo?.dept?.deptName ||
        userInfo?.department?.name ||
        userInfo?.deptName ||
        userInfo?.dept_name ||
        '부서 없음';
    const positionName =
        userInfo?.position?.positionName ||
        userInfo?.posName ||
        userInfo?.positionName ||
        userInfo?.position_name ||
        '직급 없음';
    const roleName = userInfo?.role?.roleName || userInfo?.role || 'USER';
    const gradeName = userInfo?.grade?.gradeName || userInfo?.grade_id || '정보 없음';
    const bankName = userInfo?.bank || '-';
    const accountNo = userInfo?.accountNo || userInfo?.account_no || '-';
    const numberFormat = (value) => {
        if (value === null || value === undefined || value === '') {
            return '정보 없음';
        }

        return `${Number(value).toLocaleString()}원`;
    };

    const renderInfoTab = () => (
        <CCard className="border-0" style={fieldCardStyle}>
            <CCardBody>
                <div className="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <p className="mb-1 text-muted small fw-semibold">Profile</p>
                        <h5 className="mb-0 fw-bold text-dark">기본 정보</h5>
                    </div>
                    <div>
                        {isEditing ? (
                            <>
                                <CButton size="sm" className="me-2" style={saveButtonStyle} onClick={handleUpdate}>
                                    <CIcon icon={cilCheckAlt} className="me-1" />
                                    저장
                                </CButton>
                                <CButton color="secondary" variant="outline" size="sm" onClick={() => setIsEditing(false)}>
                                    <CIcon icon={cilX} className="me-1" />
                                    취소
                                </CButton>
                            </>
                        ) : (
                            <CButton size="sm" style={editButtonStyle} onClick={() => setIsEditing(true)}>
                                <CIcon icon={cilPencil} className="me-1" />
                                변경
                            </CButton>
                        )}
                    </div>
                </div>

                <CRow className={fieldRowStyle}>
                    <CCol sm={3} style={fieldLabelStyle}>이름</CCol>
                    <CCol sm={9}>
                        {isEditing ? (
                            <CFormInput name="name" value={formData.name} onChange={handleChange} size="sm" className="w-50" />
                        ) : (
                            <span style={fieldValueStyle}>{userInfo?.name || '정보 없음'}</span>
                        )}
                    </CCol>
                </CRow>

                <CRow className={fieldRowStyle}>
                    <CCol sm={3} style={fieldLabelStyle}>이메일 · 사번</CCol>
                    <CCol sm={9}>
                        <div style={valueGroup}>
                            {isEditing ? (
                                <CFormInput name="email" value={formData.email} onChange={handleChange} size="sm" className="w-50" />
                            ) : (
                                <span style={fieldValueStyle}>{userInfo?.email || '정보 없음'}</span>
                            )}
                            <span className="ms-5 me-3 text-muted small">사번</span>
                            <span style={fieldValueStyle}>{empNo}</span>
                        </div>
                    </CCol>
                </CRow>

                <CRow className={fieldRowStyle}>
                    <CCol sm={3} style={fieldLabelStyle}>휴대전화번호</CCol>
                    <CCol sm={9}>
                        {isEditing ? (
                            <CFormInput name="phone" value={formData.phone} onChange={handleChange} size="sm" className="w-50" placeholder="010-0000-0000" />
                        ) : (
                            <span style={fieldValueStyle}>{userInfo?.phone || '정보 없음'}</span>
                        )}
                    </CCol>
                </CRow>

                <CRow className={fieldRowStyle}>
                    <CCol sm={3} style={fieldLabelStyle}>입사 정보</CCol>
                    <CCol sm={9}>
                        <span style={fieldValueStyle}>{userInfo?.hireDate || '정보 없음'}</span>
                        {userInfo?.hireDate && (
                            <CBadge color="success" className="ms-3 px-2 py-1" shape="rounded-pill" style={activeStatusBadge}>
                                재직중
                            </CBadge>
                        )}
                    </CCol>
                </CRow>

                <CRow className={fieldRowStyle}>
                    <CCol sm={3} style={fieldLabelStyle}>소속</CCol>
                    <CCol sm={9}>
                        <span className="me-4 text-muted small">부서</span>
                        <span style={fieldValueStyle}>{departmentName}</span>
                        <span className="ms-5 me-4 text-muted small">직급</span>
                        <span style={fieldValueStyle}>{positionName}</span>
                    </CCol>
                </CRow>

                <CRow className={fieldRowStyle}>
                    <CCol sm={3} style={fieldLabelStyle}>계정 권한</CCol>
                    <CCol sm={9}>
                        <CBadge color="info" shape="rounded-pill">{roleName}</CBadge>
                    </CCol>
                </CRow>

                <CRow className={fieldRowStyle}>
                    <CCol sm={3} style={fieldLabelStyle}>계좌 정보</CCol>
                    <CCol sm={9}>
                        <div style={accountInfoGroup}>
                            <div>
                                <span className="me-4 text-muted small">은행</span>
                                <span style={fieldValueStyle}>{bankName}</span>
                            </div>
                            <div>
                                <span className="me-2 text-muted small">계좌</span>
                                <span style={fieldValueStyle}>{accountNo}</span>
                            </div>
                        </div>
                    </CCol>
                </CRow>
            </CCardBody>
        </CCard>
    );

    const renderPayrollTab = () => (
        <CCard className="border-0" style={fieldCardStyle}>
            <CCardBody>
                <p className="mb-1 text-muted small fw-semibold">Payroll</p>
                <h5 className="mb-4 fw-bold text-dark">급여 정보</h5>

                <CRow className={fieldRowStyle}>
                    <CCol sm={3} style={fieldLabelStyle}>급여 등급</CCol>
                    <CCol sm={9}>
                        {payrollLoading ? (
                            <span className="text-muted small">급여관리 데이터를 불러오는 중입니다.</span>
                        ) : payrollError ? (
                            <span className="text-danger small">{payrollError}</span>
                        ) : (
                            <span style={fieldValueStyle}>{payrollInfo?.gradeId || gradeName}</span>
                        )}
                    </CCol>
                </CRow>

                <CRow className={fieldRowStyle}>
                    <CCol sm={3} style={fieldLabelStyle}>기본급</CCol>
                    <CCol sm={9}>
                        {payrollLoading ? (
                            <span className="text-muted small">급여관리 데이터를 불러오는 중입니다.</span>
                        ) : payrollError ? (
                            <span className="text-danger small">{payrollError}</span>
                        ) : (
                            <>
                                <span style={fieldValueStyle}>{numberFormat(payrollInfo?.baseSalary)}</span>
                                <span className="ms-3 text-muted small">
                                    {payrollInfo?.payMonthText ? `${payrollInfo.payMonthText} 기준` : '급여 기준'}
                                </span>
                            </>
                        )}
                    </CCol>
                </CRow>
            </CCardBody>
        </CCard>
    );

    const renderActiveTab = () => {
        if (activeTab === 'payroll') {
            return renderPayrollTab();
        }

        return renderInfoTab();
    };

    return (
        <div style={pageWrapStyle}>
            <CRow>
                <CCol xs={12}>
                    <CCard className="mb-4 border-0" style={fieldCardStyle}>
                        <div style={profileCover}></div>
                        <CCardBody className="pt-0">
                            <div className="d-flex align-items-end" style={profileHeader}>
                                <div style={profileAvatarWrap}>
                                    <CAvatar
                                        src={profileImageUrl || undefined}
                                        size="xl"
                                        className="border border-4 border-white shadow"
                                        style={profileAvatar}
                                    >
                                        {!profileImage && <CIcon icon={cilUser} />}
                                    </CAvatar>
                                    <input
                                        ref={profileFileInputRef}
                                        type="file"
                                        accept="image/jpeg,image/png"
                                        className="d-none"
                                        onChange={handleProfileImageChange}
                                    />
                                    <button
                                        type="button"
                                        style={uploadProfileButtonStyle}
                                        disabled={profileUploading}
                                        onClick={() => profileFileInputRef.current?.click()}
                                        title="프로필 사진 추가/수정"
                                        aria-label="프로필 사진 추가/수정"
                                    >
                                        <CIcon icon={cilCamera} />
                                    </button>
                                </div>
                            </div>
                            <div style={profileInfoBlock}>
                                <h4 className="mb-0" style={profileNameStyle}>
                                    {userInfo?.name || '사용자'}
                                </h4>
                                <div style={profileMetaStyle}>
                                    {departmentName} · {positionName}
                                </div>
                                <div className="text-muted small mt-1">
                                    {userInfo?.email || '-'} · {empNo}
                                </div>
                                {profileUploading && <div className="text-muted small mt-2">업로드 중...</div>}
                            </div>

                            <CNav variant="tabs" className="mt-4 border-bottom-0">
                                {TABS.map((tab) => (
                                    <CNavItem key={tab.id}>
                                        <CNavLink
                                            active={activeTab === tab.id}
                                            href="#"
                                            style={tabLinkStyle(activeTab === tab.id)}
                                            onClick={(event) => {
                                                event.preventDefault();
                                                setActiveTab(tab.id);
                                            }}
                                        >
                                            {tab.label}
                                        </CNavLink>
                                    </CNavItem>
                                ))}
                            </CNav>
                        </CCardBody>
                    </CCard>

                    {renderActiveTab()}
                </CCol>
            </CRow>
        </div>
    );
};

export default MyPage;
