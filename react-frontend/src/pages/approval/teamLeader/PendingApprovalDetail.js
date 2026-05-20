import React, { useEffect, useMemo, useState } from 'react';
import {
  CAlert,
  CBadge,
  CButton,
  CCard,
  CCardBody,
  CCardHeader,
  CCol,
  CRow,
  CSpinner,
  CTable,
  CTableBody,
  CTableDataCell,
  CTableHead,
  CTableHeaderCell,
  CTableRow,
} from '@coreui/react';
import { useNavigate, useOutletContext, useSearchParams } from 'react-router-dom';

import axiosInstance from 'src/api/axiosInstance';
import { PATH } from 'src/constants/path';
import { containerStyle } from 'src/styles/js/demoPageStyle';

const STATUS_BADGE = {
  IN_PROGRESS: 'primary',
  COMPLETED: 'success',
  REJECTED: 'danger',
  CANCELED: 'dark',
};

const LINE_STATUS_BADGE = {
  WAITING: 'secondary',
  APPROVED: 'success',
  REJECTED: 'danger',
};

const buildResourceUrl = (path) => {
  if (!path) {
    return '';
  }

  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }

  return `${PATH.API.BASE.replace(/\/api$/, '')}${path}`;
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

const formatFileSize = (size) => {
  if (!size) {
    return '-';
  }

  if (size >= 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(1)} MB`;
  }

  return `${Math.ceil(size / 1024)} KB`;
};

const parseContent = (content) => {
  if (!content) {
    return { title: '', fields: [] };
  }

  try {
    const parsed = typeof content === 'string' ? JSON.parse(content) : content;
    const normalized = typeof parsed === 'string' ? JSON.parse(parsed) : parsed;

    return {
      title: normalized?.title || '',
      fields: Array.isArray(normalized?.fields) ? normalized.fields : [],
    };
  } catch (error) {
    return { title: '', fields: [], invalid: true };
  }
};

const formatFieldValue = (field) => {
  if (field.type === 'amount') {
    const digits = String(field.value || '').replace(/[^\d]/g, '');
    return digits ? `${Number(digits).toLocaleString('ko-KR')} 원` : '-';
  }

  return field.value || '-';
};

const isImagePath = (path) => /\.(png|jpe?g|gif|webp|bmp)$/i.test(path || '');

// 결재자/참조자를 "이름(소속부서, 직급, 사번)" 형태로 표시해 동명이인도 구분할 수 있게 합니다.
const formatApprovalTarget = (line) => {
  const name = line?.approverName || line?.approverNo || '-';
  const meta = [line?.approverDeptName, line?.approverPositionName, line?.approverNo].filter(Boolean).join(', ');

  return meta ? `${name}(${meta})` : name;
};

const formatPerson = (name, deptName, positionName, empNo) => {
  const displayName = name || empNo || '-';
  const meta = [deptName, positionName, empNo].filter(Boolean).join(', ');

  return meta ? `${displayName}(${meta})` : displayName;
};

// [전자결재] 결재 대기 문서 상세 페이지
const PendingApprovalDetail = () => {
  const [userInfo] = useOutletContext();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const approvalId = searchParams.get('approvalId');

  const [detail, setDetail] = useState(null);
  const [signMap, setSignMap] = useState({});
  const [loading, setLoading] = useState(Boolean(approvalId));
  const [processing, setProcessing] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const content = useMemo(
    () => parseContent(detail?.content),
    [detail?.content]
  );

  const approvalSignLines = useMemo(
    () => (detail?.lines || [])
      .filter((line) => Number(line.stepOrder) > 0)
      .sort((a, b) => Number(a.stepOrder) - Number(b.stepOrder)),
    [detail?.lines]
  );

  // 결재 대기 상세에서도 결재선과 참조 대상을 분리해, 실제 승인 흐름만 결재선에 남깁니다.
  const approvalLines = useMemo(
    () => (detail?.lines || [])
      .filter((line) => !line.reference)
      .sort((a, b) => Number(a.stepOrder) - Number(b.stepOrder)),
    [detail?.lines]
  );

  const referenceLines = useMemo(
    () => (detail?.lines || []).filter((line) => line.reference),
    [detail?.lines]
  );

  const isCurrentApprover = detail
    && detail.status === 'IN_PROGRESS'
    && String(detail.currentApproverNo || '') === String(userInfo?.empNo || userInfo?.emp_no || '');

  const loadDetail = async () => {
    if (!approvalId) {
      setErrorMessage('조회할 결재 문서 정보가 없습니다.');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setErrorMessage('');
      const response = await axiosInstance.get(PATH.API.APPROVAL.DETAIL(approvalId));
      setDetail(response.data);
    } catch (error) {
      console.error('결재 대기 문서 상세 조회 실패:', error);
      setErrorMessage('결재 문서 상세 정보를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [approvalId]);

  useEffect(() => {
    const approverNos = [...new Set(approvalSignLines.map((line) => line.approverNo).filter(Boolean))];

    if (approverNos.length === 0) {
      setSignMap({});
      return;
    }

    // 개인 문서 상세와 동일하게 결재자별 인감 이미지를 모아서 상단 결재란에 표시합니다.
    const fetchSigns = async () => {
      const pairs = await Promise.all(
        approverNos.map(async (empNo) => {
          try {
            const response = await axiosInstance.get(PATH.API.APPROVAL.EMPLOYEE_SIGN(empNo));
            return [empNo, response.data?.signImg || ''];
          } catch (error) {
            return [empNo, ''];
          }
        })
      );

      setSignMap(Object.fromEntries(pairs));
    };

    fetchSigns();
  }, [approvalSignLines]);

  const processApproval = async (type) => {
    const isApprove = type === 'approve';
    const confirmed = window.confirm(isApprove ? '이 문서를 승인하시겠습니까?' : '이 문서를 반려하시겠습니까?');

    if (!confirmed) {
      return;
    }

    try {
      setProcessing(true);
      await axiosInstance.post(
        isApprove ? PATH.API.APPROVAL.APPROVE(approvalId) : PATH.API.APPROVAL.REJECT(approvalId)
      );
      alert(isApprove ? '승인 처리되었습니다.' : '반려 처리되었습니다.');
      await loadDetail();
    } catch (error) {
      console.error(isApprove ? '결재 승인 실패:' : '결재 반려 실패:', error);
      const message = error.response?.data?.message
        || error.response?.data?.error
        || '결재 처리 중 오류가 발생했습니다.';
      alert(message);
    } finally {
      setProcessing(false);
    }
  };

  if (loading) {
    return (
      <div style={containerStyle} className="py-5 text-center">
        <CSpinner size="sm" className="me-2" />
        결재 문서를 불러오는 중입니다.
      </div>
    );
  }

  return (
    <div style={containerStyle}>
      <header className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-1">결재 대기 문서 상세</h2>
          <div className="text-body-secondary">
            {detail?.title || '결재 문서'}
          </div>
        </div>
        <CButton color="secondary" variant="outline" onClick={() => navigate(PATH.APPROVAL.PENDING)}>
          목록
        </CButton>
      </header>

      {errorMessage && <CAlert color="danger">{errorMessage}</CAlert>}

      {detail && (
        <>
          {/* 결재자가 처리할 문서도 출력 양식과 비슷하게 상단에 결재란을 먼저 보여줍니다. */}
          <div className="d-flex justify-content-end mb-3">
            <CTable
              bordered
              align="middle"
              className="mb-0 text-center"
              style={{ width: 'auto', minWidth: approvalSignLines.length > 0 ? '260px' : '180px' }}
            >
              <CTableBody>
                <CTableRow>
                  <CTableHeaderCell className="bg-light" style={{ width: '64px' }}>
                    결재
                  </CTableHeaderCell>
                  {approvalSignLines.length === 0 ? (
                    <CTableHeaderCell>-</CTableHeaderCell>
                  ) : (
                    approvalSignLines.map((line) => (
                      <CTableHeaderCell
                        key={`${line.lineId}-sign-name`}
                        style={{ minWidth: '96px' }}
                      >
                        {line.approverName || line.approverNo || '-'}
                      </CTableHeaderCell>
                    ))
                  )}
                </CTableRow>
                <CTableRow>
                  <CTableHeaderCell className="bg-light">인감</CTableHeaderCell>
                  {approvalSignLines.length === 0 ? (
                    <CTableDataCell className="text-body-secondary small">
                      결재자 없음
                    </CTableDataCell>
                  ) : (
                    approvalSignLines.map((line) => {
                      const signImg = signMap[line.approverNo];

                      return (
                        <CTableDataCell key={`${line.lineId}-sign-img`}>
                          {signImg ? (
                            <img
                              src={buildResourceUrl(signImg)}
                              alt={`${line.approverName || line.approverNo} 인감`}
                              style={{ width: '64px', height: '64px', objectFit: 'contain' }}
                            />
                          ) : (
                            <span className="text-body-secondary small">인감 미등록</span>
                          )}
                        </CTableDataCell>
                      );
                    })
                  )}
                </CTableRow>
              </CTableBody>
            </CTable>
          </div>

          <CCard className="mb-4">
            <CCardHeader className="d-flex justify-content-between align-items-center">
              <strong>{detail.title}</strong>
              <CBadge color={STATUS_BADGE[detail.status] || 'secondary'}>
                {detail.statusLabel || detail.status}
              </CBadge>
            </CCardHeader>
            <CCardBody>
              <CRow className="g-3">
                <CCol md={4}>
                  <div className="text-body-secondary small">서식</div>
                  <div>{detail.formName || '-'}</div>
                </CCol>
                <CCol md={4}>
                  <div className="text-body-secondary small">작성자</div>
                  <div>{formatPerson(detail.writerName, detail.writerDeptName, detail.writerPositionName, detail.writerNo)}</div>
                </CCol>
                <CCol md={4}>
                  <div className="text-body-secondary small">현재 결재자</div>
                  <div>{formatPerson(detail.currentApproverName, detail.currentApproverDeptName, detail.currentApproverPositionName, detail.currentApproverNo)}</div>
                </CCol>
                <CCol md={4}>
                  <div className="text-body-secondary small">진행 단계</div>
                  <div>
                    {Number(detail.maxStep) > 0
                      ? `${detail.currentStep || 0} / ${detail.maxStep}`
                      : '-'}
                  </div>
                </CCol>
                <CCol md={4}>
                  <div className="text-body-secondary small">작성일</div>
                  <div>{formatDateTime(detail.createdAt)}</div>
                </CCol>
                <CCol md={4}>
                  <div className="text-body-secondary small">수정일</div>
                  <div>{formatDateTime(detail.updatedAt)}</div>
                </CCol>
              </CRow>
            </CCardBody>
          </CCard>

          <CCard className="mb-4">
            <CCardHeader>
              <strong>문서 내용</strong>
            </CCardHeader>
            <CCardBody>
              {content.invalid ? (
                <CAlert color="warning" className="mb-0">
                  문서 본문 JSON을 해석하지 못했습니다.
                </CAlert>
              ) : content.fields.length === 0 ? (
                <div className="text-body-secondary">작성된 내용이 없습니다.</div>
              ) : (
                <CTable responsive bordered align="middle">
                  <CTableBody>
                    {content.fields.map((field) => (
                      <CTableRow key={field.id}>
                        <CTableHeaderCell style={{ width: '220px' }}>
                          {field.label || '항목명 없음'}
                        </CTableHeaderCell>
                        <CTableDataCell style={{ whiteSpace: 'pre-wrap' }}>
                          {formatFieldValue(field)}
                        </CTableDataCell>
                      </CTableRow>
                    ))}
                  </CTableBody>
                </CTable>
              )}
            </CCardBody>
          </CCard>

          <CCard className="mb-4">
            <CCardHeader>
              <strong>결재선</strong>
            </CCardHeader>
            <CCardBody>
              <CTable responsive align="middle">
                <CTableHead>
                  <CTableRow>
                    <CTableHeaderCell>단계</CTableHeaderCell>
                    <CTableHeaderCell>대상자</CTableHeaderCell>
                    <CTableHeaderCell>상태</CTableHeaderCell>
                    <CTableHeaderCell>처리일시</CTableHeaderCell>
                  </CTableRow>
                </CTableHead>
                <CTableBody>
                  {approvalLines.length === 0 ? (
                    <CTableRow>
                      <CTableDataCell colSpan={4} className="text-center text-body-secondary">
                        결재선 정보가 없습니다.
                      </CTableDataCell>
                    </CTableRow>
                  ) : (
                    approvalLines.map((line) => (
                      <CTableRow key={line.lineId}>
                        <CTableDataCell>{line.stepOrder}단계</CTableDataCell>
                        <CTableDataCell>{formatApprovalTarget(line)}</CTableDataCell>
                        <CTableDataCell>
                          <CBadge color={LINE_STATUS_BADGE[line.status] || 'secondary'}>
                            {line.statusLabel || line.status}
                          </CBadge>
                        </CTableDataCell>
                        <CTableDataCell>{formatDateTime(line.processedAt)}</CTableDataCell>
                      </CTableRow>
                    ))
                  )}
                </CTableBody>
              </CTable>
            </CCardBody>
          </CCard>

          <CCard className="mb-4">
            <CCardHeader>
              <strong>참조</strong>
            </CCardHeader>
            <CCardBody>
              <CTable responsive align="middle" className="mb-0">
                <CTableHead>
                  <CTableRow>
                    <CTableHeaderCell>대상자</CTableHeaderCell>
                  </CTableRow>
                </CTableHead>
                <CTableBody>
                  {referenceLines.length === 0 ? (
                    <CTableRow>
                      <CTableDataCell className="text-center text-body-secondary">
                        참조 대상자가 없습니다.
                      </CTableDataCell>
                    </CTableRow>
                  ) : (
                    referenceLines.map((line) => (
                      <CTableRow key={line.lineId}>
                        <CTableDataCell>{formatApprovalTarget(line)}</CTableDataCell>
                      </CTableRow>
                    ))
                  )}
                </CTableBody>
              </CTable>
            </CCardBody>
          </CCard>

          <CCard className="mb-4">
            <CCardHeader>
              <strong>첨부파일</strong>
            </CCardHeader>
            <CCardBody>
              {(detail.files || []).length === 0 ? (
                <div className="text-body-secondary">첨부파일이 없습니다.</div>
              ) : (
                <div className="d-flex flex-wrap gap-3">
                  {detail.files.map((file) => (
                    <a
                      className="border rounded text-decoration-none text-body p-2"
                      style={{ width: '180px' }}
                      href={buildResourceUrl(file.filePath)}
                      target="_blank"
                      rel="noreferrer"
                      key={file.fileId}
                    >
                      {isImagePath(file.filePath) ? (
                        <img
                          src={buildResourceUrl(file.filePath)}
                          alt={file.fileName}
                          style={{ width: '100%', height: '110px', objectFit: 'cover' }}
                        />
                      ) : (
                        <div
                          className="d-flex align-items-center justify-content-center bg-light"
                          style={{ height: '110px' }}
                        >
                          <strong>FILE</strong>
                        </div>
                      )}
                      <div className="small text-truncate mt-2" title={file.fileName}>
                        {file.fileName}
                      </div>
                      <div className="small text-body-secondary">{formatFileSize(file.fileSize)}</div>
                    </a>
                  ))}
                </div>
              )}
            </CCardBody>
          </CCard>

          <div className="d-flex justify-content-center gap-2 mb-4">
            {isCurrentApprover ? (
              <>
                <CButton color="danger" onClick={() => processApproval('reject')} disabled={processing}>
                  반려
                </CButton>
                <CButton color="success" onClick={() => processApproval('approve')} disabled={processing}>
                  승인
                </CButton>
              </>
            ) : (
              <CAlert color="info" className="mb-0">
                현재 결재할 수 있는 상태가 아닙니다.
              </CAlert>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default PendingApprovalDetail;
