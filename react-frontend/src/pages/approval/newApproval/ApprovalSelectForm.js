import React, { useEffect, useMemo, useState } from 'react';
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

// 결재 서식 template JSON을 안전하게 파싱합니다.
// 관리자가 잘못된 JSON을 저장했더라도 목록 화면 전체가 깨지지 않도록 빈 구조로 대체합니다.
const parseTemplate = (template) => {
  if (!template) {
    return { fields: [], fileRequired: false };
  }

  try {
    const parsed = JSON.parse(template);
    return {
      fields: Array.isArray(parsed.fields) ? parsed.fields : [],
      fileRequired: Boolean(parsed.fileRequired),
    };
  } catch (error) {
    return { fields: [], fileRequired: false, invalid: true };
  }
};

// [전자결재] 새 결재 진행 - 결재 서식 선택 페이지
const ApprovalSelectForm = () => {
  const [userInfo] = useOutletContext();
  const navigate = useNavigate();

  const [forms, setForms] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const fetchForms = async () => {
      try {
        setLoading(true);
        setErrorMessage('');
        const response = await axiosInstance.get(PATH.API.APPROVAL.FORMS);
        setForms(Array.isArray(response.data) ? response.data : []);
      } catch (error) {
        console.error('결재 서식 목록 조회 실패:', error);
        setErrorMessage('결재 서식 목록을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchForms();
  }, []);

  const filteredForms = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    if (!normalizedKeyword) {
      return forms;
    }

    return forms.filter((form) =>
      (form.formName || '').toLowerCase().includes(normalizedKeyword)
      || (form.lineTemplateName || '').toLowerCase().includes(normalizedKeyword)
    );
  }, [forms, keyword]);

  const totalPages = Math.max(1, Math.ceil(filteredForms.length / PAGE_SIZE));
  const pageForms = filteredForms.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  useEffect(() => {
    setPage(1);
  }, [keyword]);

  const moveToWritePage = (form) => {
    const template = parseTemplate(form.template);
    if (template.invalid) {
      return;
    }

    navigate(PATH.APPROVAL.NEW_WRITE, {
      state: {
        selectedForm: form,
      },
    });
  };

  return (
    <div style={containerStyle}>
      <header className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-1">새 결재 문서 작성</h2>
          <div className="text-body-secondary">
            {userInfo?.name ? `${userInfo.name}님, 사용할 결재 서식을 선택하세요.` : '사용할 결재 서식을 선택하세요.'}
          </div>
        </div>
      </header>

      <CCard className="mb-4">
        <CCardHeader className="d-flex justify-content-between align-items-center gap-3">
          <strong>결재 서식 선택</strong>
          <CFormInput
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="서식 이름 또는 결재선 검색"
            style={{ maxWidth: '320px' }}
          />
        </CCardHeader>
        <CCardBody>
          {errorMessage && <CAlert color="danger">{errorMessage}</CAlert>}

          {loading ? (
            <div className="py-5 text-center">
              <CSpinner size="sm" className="me-2" />
              결재 서식을 불러오는 중입니다.
            </div>
          ) : filteredForms.length === 0 ? (
            <CAlert color="info" className="mb-0">
              선택할 수 있는 결재 서식이 없습니다.
            </CAlert>
          ) : (
            <>
              <CTable hover responsive align="middle">
                <CTableHead>
                  <CTableRow>
                    <CTableHeaderCell>서식 이름</CTableHeaderCell>
                    <CTableHeaderCell>결재선</CTableHeaderCell>
                  </CTableRow>
                </CTableHead>
                <CTableBody>
                  {pageForms.map((form) => {
                    const template = parseTemplate(form.template);

                    return (
                      <CTableRow
                        key={form.formId}
                        role="button"
                        onClick={() => moveToWritePage(form)}
                        className={template.invalid ? 'table-danger' : ''}
                      >
                        <CTableDataCell>
                          <div className="d-flex align-items-center gap-2">
                            <strong>{form.formName}</strong>
                            {form.isDefault && <CBadge color="primary">기본</CBadge>}
                            {template.invalid && <CBadge color="danger">JSON 확인 필요</CBadge>}
                          </div>
                          <div className="small text-body-secondary">
                            입력 항목 {template.fields.length}개
                            {template.fileRequired ? ' · 첨부파일 필수' : ''}
                          </div>
                        </CTableDataCell>
                        <CTableDataCell>
                          {form.lineTemplateName || '연결 안 됨'}
                        </CTableDataCell>
                      </CTableRow>
                    );
                  })}
                </CTableBody>
              </CTable>

              <div className="d-flex justify-content-between align-items-center">
                <div className="text-body-secondary small">
                  총 {filteredForms.length}개
                </div>
                <CPagination className="mb-0">
                  <CPaginationItem
                    disabled={page === 1}
                    onClick={() => setPage((prev) => Math.max(1, prev - 1))}
                  >
                    이전
                  </CPaginationItem>
                  {Array.from({ length: totalPages }, (_, index) => index + 1).map((pageNumber) => (
                    <CPaginationItem
                      key={pageNumber}
                      active={pageNumber === page}
                      onClick={() => setPage(pageNumber)}
                    >
                      {pageNumber}
                    </CPaginationItem>
                  ))}
                  <CPaginationItem
                    disabled={page === totalPages}
                    onClick={() => setPage((prev) => Math.min(totalPages, prev + 1))}
                  >
                    다음
                  </CPaginationItem>
                </CPagination>
              </div>
            </>
          )}
        </CCardBody>
      </CCard>
    </div>
  );
};

export default ApprovalSelectForm;
