import React, { useEffect, useState } from 'react';
import {
  CAlert,
  CBadge,
  CCard,
  CCardBody,
  CCardHeader,
  CFormInput,
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

// [전자결재] 임시저장함 페이지
const TmpApprovals = () => {
  const [userInfo] = useOutletContext();
  const navigate = useNavigate();

  const [page, setPage] = useState(0);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [documents, setDocuments] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const fetchDrafts = async () => {
      try {
        setLoading(true);
        setErrorMessage('');

        const response = await axiosInstance.get(PATH.API.APPROVAL.DRAFTS, {
          params: {
            page,
            size: PAGE_SIZE,
            startDate: startDate || undefined,
            endDate: endDate || undefined,
          },
        });

        setDocuments(response.data?.content || []);
        setTotalPages(Math.max(1, response.data?.totalPages || 1));
      } catch (error) {
        console.error('임시저장 문서함 조회 실패:', error);
        setErrorMessage('임시저장 문서 목록을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchDrafts();
  }, [page, startDate, endDate]);

  // 상신일 기간 필터가 바뀌면 첫 페이지부터 다시 조회합니다.
  useEffect(() => {
    setPage(0);
  }, [startDate, endDate]);

  const openDraft = (approvalId) => {
    navigate(PATH.APPROVAL.TMP_DETAIL_WITH_ID(approvalId));
  };

  return (
    <div style={containerStyle}>
      <header className="mb-4">
        <div>
          <h2 className="mb-1">임시저장함</h2>
          <div className="text-body-secondary">
            {userInfo?.name ? `${userInfo.name}님이 아직 상신하지 않은 문서입니다.` : '아직 상신하지 않은 결재 문서입니다.'}
          </div>
        </div>
      </header>

      <CCard className="mb-4">
        <CCardHeader className="d-flex flex-wrap justify-content-between align-items-center gap-3">
          <strong>임시저장 문서</strong>
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
              임시저장 문서를 불러오는 중입니다.
            </div>
          ) : (
            <CTable hover responsive align="middle">
              <CTableHead>
                <CTableRow>
                  <CTableHeaderCell style={{ width: '90px' }}>번호</CTableHeaderCell>
                  <CTableHeaderCell style={{ width: '100px' }}>상태</CTableHeaderCell>
                  <CTableHeaderCell>문서 제목</CTableHeaderCell>
                  <CTableHeaderCell>서식</CTableHeaderCell>
                  <CTableHeaderCell>작성자</CTableHeaderCell>
                  <CTableHeaderCell>최종 수정일</CTableHeaderCell>
                </CTableRow>
              </CTableHead>
              <CTableBody>
                {documents.length === 0 ? (
                  <CTableRow>
                    <CTableDataCell colSpan={6} className="text-center text-body-secondary py-5">
                      임시저장된 문서가 없습니다.
                    </CTableDataCell>
                  </CTableRow>
                ) : (
                  documents.map((document) => (
                    <CTableRow
                      key={document.approvalId}
                      role="button"
                      onClick={() => openDraft(document.approvalId)}
                    >
                      <CTableDataCell>{document.approvalId}</CTableDataCell>
                      <CTableDataCell>
                        <CBadge color="secondary">
                          {document.statusLabel || '임시저장'}
                        </CBadge>
                      </CTableDataCell>
                      <CTableDataCell>
                        <strong>{document.title || '-'}</strong>
                      </CTableDataCell>
                      <CTableDataCell>{document.formName || '-'}</CTableDataCell>
                      <CTableDataCell>{document.writerName || document.writerNo || '-'}</CTableDataCell>
                      <CTableDataCell>{formatDateTime(document.updatedAt || document.createdAt)}</CTableDataCell>
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

export default TmpApprovals;
