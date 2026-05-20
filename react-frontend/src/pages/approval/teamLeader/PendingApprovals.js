import React, { useEffect, useState } from 'react';
import {
  CAlert,
  CBadge,
  CButton,
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

const BOX_TYPES = [
  { key: 'pending', label: '결재 대기', apiPath: PATH.API.APPROVAL.PENDING_DOCUMENTS },
  { key: 'processed', label: '처리 완료', apiPath: PATH.API.APPROVAL.PROCESSED_DOCUMENTS },
];

const STATUS_OPTIONS = [
  { value: '', label: '전체' },
  // { value: 'IN_PROGRESS', label: '진행중' },
  { value: 'COMPLETED', label: '완료' },
  { value: 'REJECTED', label: '반려' },
  { value: 'CANCELED', label: '취소' },
];

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

// [전자결재] 결재대기문서함
const PendingApprovals = () => {
  const [userInfo] = useOutletContext();
  const navigate = useNavigate();

  const [boxType, setBoxType] = useState('pending');
  const [status, setStatus] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [documents, setDocuments] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const fetchPendingDocuments = async () => {
      try {
        setLoading(true);
        setErrorMessage('');

        const currentBox = BOX_TYPES.find((item) => item.key === boxType) || BOX_TYPES[0];
        const response = await axiosInstance.get(currentBox.apiPath, {
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
        console.error('결재 대기 문서함 조회 실패:', error);
        setErrorMessage('결재 대기 문서 목록을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchPendingDocuments();
  }, [boxType, page, status, startDate, endDate]);

  // 문서함 종류나 검색 필터가 바뀌면 첫 페이지부터 다시 조회합니다.
  useEffect(() => {
    setPage(0);
  }, [boxType, status, startDate, endDate]);

  // 결재 대기 상세 화면에서는 승인/반려 처리를 해야 하므로 approvalId를 query string으로 넘깁니다.
  const openDetail = (approvalId) => {
    if (boxType === 'processed') {
      navigate(PATH.APPROVAL.UPCOMING_DETAIL_WITH_ID(approvalId));
      return;
    }

    navigate(PATH.APPROVAL.PENDING_DETAIL_WITH_ID(approvalId));
  };

  return (
    <div style={containerStyle}>
      <header className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-1">결재 대기 문서함</h2>
          <div className="text-body-secondary">
            {userInfo?.name ? `${userInfo.name}님의 결재 대기 및 처리 완료 문서입니다.` : '결재 대기 및 처리 완료 문서입니다.'}
          </div>
        </div>
      </header>

      <CCard className="mb-4">
        <CCardHeader className="d-flex flex-wrap justify-content-between align-items-center gap-3">
          <div className="d-flex gap-2">
            {BOX_TYPES.map((item) => (
              <CButton
                color={boxType === item.key ? 'primary' : 'secondary'}
                variant={boxType === item.key ? undefined : 'outline'}
                key={item.key}
                onClick={() => setBoxType(item.key)}
              >
                {item.label}
              </CButton>
            ))}
          </div>
          <div className="d-flex flex-wrap gap-2 align-items-center">
            {/* '처리 완료' 탭에서만 상태 필터 적용 */}
            {
              boxType === 'processed' ?
                <CFormSelect
                value={status}
                onChange={(event) => setStatus(event.target.value)}
                style={{ width: '160px' }}
              >
                {STATUS_OPTIONS.map((option) => (
                  <option value={option.value} key={option.value}>
                    {option.label}
                  </option>
                ))}
              </CFormSelect>
              :null
            }
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
              결재 대기 문서를 불러오는 중입니다.
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
                  <CTableHeaderCell>진행</CTableHeaderCell>
                  <CTableHeaderCell>상신일</CTableHeaderCell>
                </CTableRow>
              </CTableHead>
              <CTableBody>
                {documents.length === 0 ? (
                  <CTableRow>
                    <CTableDataCell colSpan={7} className="text-center text-body-secondary py-5">
                      조회된 문서가 없습니다.
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

export default PendingApprovals;
