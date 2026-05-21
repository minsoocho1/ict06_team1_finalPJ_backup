import React, { useEffect, useState } from 'react';
import {
  CAlert,
  CBadge,
  CCard,
  CCardBody,
  CCardHeader,
  CFormInput,
  CFormSelect,
  CPagination,
  CPaginationItem,
  CSpinner,
  CTable,
  CTableBody,
  CTableDataCell,
  CTableHead,
  CTableHeaderCell,
  CTableRow,
} from '@coreui/react';
import { useNavigate, useOutletContext } from 'react-router-dom';

import axiosInstance from 'src/api/axiosInstance';
import { PATH } from 'src/constants/path';
import { containerStyle } from 'src/styles/js/demoPageStyle';

const PAGE_SIZE = 10;

const STATUS_BADGE = {
  IN_PROGRESS: 'primary',
  COMPLETED: 'success',
  REJECTED: 'danger',
  CANCELED: 'dark',
};

const formatDateTime = (value) => {
  if (!value) {
    return '-';
  }

  return new Date(value).toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// [전자결재] 결재 예정 문서함
// 아직 내 결재 차례는 아니지만 결재선에 포함된 문서를 미리 확인하는 화면입니다.
const UpcomingApprovals = () => {
  const [userInfo] = useOutletContext();
  const navigate = useNavigate();

  const [documents, setDocuments] = useState([]);
  const [status, setStatus] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const fetchUpcomingDocuments = async () => {
      try {
        setLoading(true);
        setErrorMessage('');

        const response = await axiosInstance.get(PATH.API.APPROVAL.UPCOMING_DOCUMENTS, {
          params: {
            page,
            size: PAGE_SIZE,
            status: status || undefined,
            startDate: startDate || undefined,
            endDate: endDate || undefined,
          },
        });

        setDocuments(response.data?.content || []);
        setTotalPages(Math.max(1, response.data?.totalPages || 1));
      } catch (error) {
        console.error('결재 예정 문서함 조회 실패:', error);
        setErrorMessage('결재 예정 문서 목록을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchUpcomingDocuments();
  }, [page, status, startDate, endDate]);

  // 검색 필터가 바뀌면 첫 페이지부터 다시 조회합니다.
  useEffect(() => {
    setPage(0);
  }, [status, startDate, endDate]);

  // 예정 문서는 승인/반려 대상이 아니므로 읽기 전용 상세 화면으로 이동합니다.
  const openDetail = (approvalId) => {
    navigate(PATH.APPROVAL.UPCOMING_DETAIL_WITH_ID(approvalId));
  };

  return (
    <div style={containerStyle}>
      <header className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-1">결재 예정 문서함</h2>
          <div className="text-body-secondary">
            {userInfo?.name
              ? `${userInfo.name}님이 이후 단계에서 결재할 문서입니다.`
              : '아직 내 차례는 아니지만 결재선에 포함된 문서입니다.'}
          </div>
        </div>
      </header>

      <CCard className="mb-4">
        <CCardHeader className="d-flex flex-wrap justify-content-between align-items-center gap-3">
          <strong>결재 예정 문서</strong>
          <div className="d-flex flex-wrap gap-2 align-items-center">
            <CFormInput
              type="date"
              value={startDate}
              onChange={(event) => setStartDate(event.target.value)}
              style={{ width: '160px' }}
              aria-label="상신일 시작일"
            />
            <span className="text-body-secondary">~</span>
            <CFormInput
              type="date"
              value={endDate}
              onChange={(event) => setEndDate(event.target.value)}
              style={{ width: '160px' }}
              aria-label="상신일 종료일"
            />
          </div>
        </CCardHeader>
        <CCardBody>
          {errorMessage && <CAlert color="danger">{errorMessage}</CAlert>}

          {loading ? (
            <div className="py-5 text-center">
              <CSpinner size="sm" className="me-2" />
              결재 예정 문서를 불러오는 중입니다.
            </div>
          ) : (
            <CTable hover responsive align="middle">
              <CTableHead>
                <CTableRow>
                  <CTableHeaderCell style={{ width: '90px' }}>번호</CTableHeaderCell>
                  <CTableHeaderCell style={{ width: '90px' }}>상태</CTableHeaderCell>
                  <CTableHeaderCell>문서 제목</CTableHeaderCell>
                  <CTableHeaderCell>서식</CTableHeaderCell>
                  <CTableHeaderCell>작성자</CTableHeaderCell>
                  <CTableHeaderCell>현재 진행</CTableHeaderCell>
                  <CTableHeaderCell>현재 결재자</CTableHeaderCell>
                  <CTableHeaderCell>상신일</CTableHeaderCell>
                </CTableRow>
              </CTableHead>
              <CTableBody>
                {documents.length === 0 ? (
                  <CTableRow>
                    <CTableDataCell colSpan={8} className="text-center text-body-secondary py-5">
                      결재 예정 문서가 없습니다.
                    </CTableDataCell>
                  </CTableRow>
                ) : (
                  documents.map((document) => (
                    <CTableRow
                      key={document.approvalId}
                      role="button"
                      onClick={() => openDetail(document.approvalId)}
                    >
                      <CTableDataCell>{document.approvalId}</CTableDataCell>
                      <CTableDataCell>
                        <CBadge color={STATUS_BADGE[document.status] || 'secondary'}>
                          {document.statusLabel || document.status}
                        </CBadge>
                      </CTableDataCell>
                      <CTableDataCell>
                        <strong>{document.title}</strong>
                      </CTableDataCell>
                      <CTableDataCell>{document.formName || '-'}</CTableDataCell>
                      <CTableDataCell>{document.writerName || document.writerNo || '-'}</CTableDataCell>
                      <CTableDataCell>
                        {Number(document.maxStep) > 0
                          ? `${document.currentStep || 0} / ${document.maxStep}`
                          : '-'}
                      </CTableDataCell>
                      <CTableDataCell>
                        {document.currentApproverName || document.currentApproverNo || '-'}
                      </CTableDataCell>
                      <CTableDataCell>{formatDateTime(document.createdAt)}</CTableDataCell>
                    </CTableRow>
                  ))
                )}
              </CTableBody>
            </CTable>
          )}

          <div className="d-flex justify-content-end">
            <CPagination className="mb-0">
              <CPaginationItem
                disabled={page === 0}
                onClick={() => setPage((prev) => Math.max(0, prev - 1))}
              >
                이전
              </CPaginationItem>
              {Array.from({ length: totalPages }, (_, index) => index).map((pageNumber) => (
                <CPaginationItem
                  active={pageNumber === page}
                  key={pageNumber}
                  onClick={() => setPage(pageNumber)}
                >
                  {pageNumber + 1}
                </CPaginationItem>
              ))}
              <CPaginationItem
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((prev) => Math.min(totalPages - 1, prev + 1))}
              >
                다음
              </CPaginationItem>
            </CPagination>
          </div>
        </CCardBody>
      </CCard>
    </div>
  );
};

export default UpcomingApprovals;
