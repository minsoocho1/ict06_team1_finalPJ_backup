import React, { useEffect, useMemo, useState } from 'react';
import {
  CAlert,
  CBadge,
  CButton,
  CCard,
  CCardBody,
  CCardHeader,
  CSpinner,
  CTable,
  CTableBody,
  CTableDataCell,
  CTableHead,
  CTableHeaderCell,
  CTableRow,
} from '@coreui/react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import axiosInstance from 'src/api/axiosInstance';
import { PATH } from 'src/constants/path';
import { containerStyle } from 'src/styles/js/demoPageStyle';

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

const parseContent = (content) => {
  if (!content) {
    return { fields: [] };
  }

  try {
    const parsed = JSON.parse(content);
    return {
      title: parsed.title || '',
      fields: Array.isArray(parsed.fields) ? parsed.fields : [],
    };
  } catch (error) {
    return { fields: [], invalid: true };
  }
};

const formatFieldValue = (field) => {
  const value = field?.value;
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  if (field.type === 'amount') {
    const digits = String(value).replace(/[^\d]/g, '');
    return digits ? `${Number(digits).toLocaleString('ko-KR')}원` : '-';
  }

  return String(value);
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

// [전자결재] 임시저장 문서 상세 페이지
const TmpApprovalDetail = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const approvalId = searchParams.get('approvalId');

  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const content = useMemo(
    () => parseContent(detail?.content),
    [detail?.content]
  );

  useEffect(() => {
    if (!approvalId) {
      setErrorMessage('임시저장 문서 번호가 필요합니다.');
      return;
    }

    const fetchDraftDetail = async () => {
      try {
        setLoading(true);
        setErrorMessage('');

        const response = await axiosInstance.get(PATH.API.APPROVAL.DETAIL(approvalId));
        const data = response.data;

        if (data?.status !== 'DRAFT') {
          setErrorMessage('임시저장 상태의 문서만 이 화면에서 확인할 수 있습니다.');
          return;
        }

        setDetail(data);
      } catch (error) {
        console.error('임시저장 문서 상세 조회 실패:', error);
        setErrorMessage(error.response?.data?.message || '임시저장 문서를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchDraftDetail();
  }, [approvalId]);

  const moveToEdit = () => {
    navigate(PATH.APPROVAL.NEW_WRITE, {
      state: {
        draftId: Number(approvalId),
      },
    });
  };

  const deleteDraft = async () => {
    if (!window.confirm('임시저장 문서를 삭제하시겠습니까? 삭제 후에는 복구할 수 없습니다.')) {
      return;
    }

    try {
      setDeleting(true);
      setErrorMessage('');

      await axiosInstance.delete(PATH.API.APPROVAL.DELETE_DRAFT(approvalId));
      alert('임시저장 문서를 삭제했습니다.');
      navigate(PATH.APPROVAL.TMP);
    } catch (error) {
      console.error('임시저장 문서 삭제 실패:', error);
      setErrorMessage(error.response?.data?.message || '임시저장 문서 삭제 중 오류가 발생했습니다.');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div style={containerStyle}>
      <header className="mb-4 d-flex justify-content-between align-items-start gap-3">
        <div>
          <h2 className="mb-1">임시저장 문서 상세</h2>
          <div className="text-body-secondary">
            아직 상신하지 않은 문서의 내용을 확인한 뒤 수정하거나 삭제할 수 있습니다.
          </div>
        </div>
        <CButton color="secondary" variant="outline" onClick={() => navigate(PATH.APPROVAL.TMP)}>
          목록
        </CButton>
      </header>

      {errorMessage && <CAlert color="danger">{errorMessage}</CAlert>}

      {loading ? (
        <div className="py-5 text-center">
          <CSpinner size="sm" className="me-2" />
          임시저장 문서를 불러오는 중입니다.
        </div>
      ) : detail ? (
        <>
          <CCard className="mb-4">
            <CCardHeader className="d-flex justify-content-between align-items-center">
              <strong>{detail.title || '-'}</strong>
              <CBadge color="secondary">{detail.statusLabel || '임시저장'}</CBadge>
            </CCardHeader>
            <CCardBody>
              <CTable responsive align="middle">
                <CTableBody>
                  <CTableRow>
                    <CTableHeaderCell style={{ width: 160 }}>문서 번호</CTableHeaderCell>
                    <CTableDataCell>{detail.approvalId}</CTableDataCell>
                  </CTableRow>
                  <CTableRow>
                    <CTableHeaderCell>서식명</CTableHeaderCell>
                    <CTableDataCell>{detail.formName || '-'}</CTableDataCell>
                  </CTableRow>
                  <CTableRow>
                    <CTableHeaderCell>작성자</CTableHeaderCell>
                    <CTableDataCell>
                      {detail.writerName || '-'} {detail.writerNo ? `(${detail.writerNo})` : ''}
                    </CTableDataCell>
                  </CTableRow>
                  <CTableRow>
                    <CTableHeaderCell>작성일</CTableHeaderCell>
                    <CTableDataCell>{formatDateTime(detail.createdAt)}</CTableDataCell>
                  </CTableRow>
                  <CTableRow>
                    <CTableHeaderCell>최종 수정일</CTableHeaderCell>
                    <CTableDataCell>{formatDateTime(detail.updatedAt)}</CTableDataCell>
                  </CTableRow>
                </CTableBody>
              </CTable>
            </CCardBody>
          </CCard>

          <CCard className="mb-4">
            <CCardHeader>
              <strong>작성 내용</strong>
            </CCardHeader>
            <CCardBody>
              {content.invalid ? (
                <CAlert color="warning" className="mb-0">
                  문서 본문 JSON을 해석할 수 없습니다.
                </CAlert>
              ) : content.fields.length === 0 ? (
                <div className="text-body-secondary py-3">작성된 상세 항목이 없습니다.</div>
              ) : (
                <CTable responsive align="middle">
                  <CTableHead>
                    <CTableRow>
                      <CTableHeaderCell style={{ width: 220 }}>항목</CTableHeaderCell>
                      <CTableHeaderCell>입력값</CTableHeaderCell>
                    </CTableRow>
                  </CTableHead>
                  <CTableBody>
                    {content.fields.map((field) => (
                      <CTableRow key={field.id}>
                        <CTableDataCell>
                          <strong>{field.label || field.id}</strong>
                        </CTableDataCell>
                        <CTableDataCell>{formatFieldValue(field)}</CTableDataCell>
                      </CTableRow>
                    ))}
                  </CTableBody>
                </CTable>
              )}
            </CCardBody>
          </CCard>

          <CCard className="mb-4">
            <CCardHeader>
              <strong>첨부파일</strong>
            </CCardHeader>
            <CCardBody>
              {detail.files?.length > 0 ? (
                <div className="d-grid gap-2">
                  {detail.files.map((file) => (
                    <a
                      key={file.fileId}
                      href={buildResourceUrl(file.filePath)}
                      target="_blank"
                      rel="noreferrer"
                    >
                      {file.fileName || '첨부파일'}
                    </a>
                  ))}
                </div>
              ) : (
                <div className="text-body-secondary">첨부파일이 없습니다.</div>
              )}
            </CCardBody>
          </CCard>

          <div className="d-flex justify-content-center gap-2">
            <CButton color="primary" onClick={moveToEdit}>
              수정
            </CButton>
            <CButton color="danger" variant="outline" onClick={deleteDraft} disabled={deleting}>
              {deleting ? '삭제 중...' : '삭제'}
            </CButton>
          </div>
        </>
      ) : null}
    </div>
  );
};

export default TmpApprovalDetail;
