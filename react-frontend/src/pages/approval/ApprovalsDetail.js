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
  DRAFT: 'secondary',
  PENDING: 'warning',
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
    return { title: '', fields: [], invalid: true, raw: content };
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

// 결재선/참조 영역에서 대상자를 "이름(소속부서, 직급, 사번)" 형태로 통일해 보여줍니다.
// 부서나 직급 정보가 없는 과거 데이터도 깨지지 않도록 존재하는 값만 괄호 안에 조합합니다.
const formatApprovalTarget = (line) => {
  const name = line?.approverName || line?.approverNo || '-';
  const meta = [line?.approverDeptName, line?.approverPositionName, line?.approverNo].filter(Boolean).join(', ');

  return meta ? `${name}(${meta})` : name;
};

// 작성자/현재 결재자처럼 상세 응답의 최상위 필드로 내려오는 사람 정보도 같은 형식으로 표시합니다.
const formatPerson = (name, deptName, positionName, empNo) => {
  const displayName = name || empNo || '-';
  const meta = [deptName, positionName, empNo].filter(Boolean).join(', ');

  return meta ? `${displayName}(${meta})` : displayName;
};

// 인쇄용 HTML을 문자열로 만들기 때문에 사용자 입력값은 직접 삽입하기 전에 이스케이프합니다.
const escapeHtml = (value) =>
  String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

const createPrintHtml = (detail, content, signMap) => {
  /*
   * 화면의 React DOM을 그대로 인쇄하면 사이드바/버튼/카드 스타일이 섞일 수 있습니다.
   * 그래서 결재 문서 출력에 필요한 데이터만 별도 HTML로 만들어 브라우저 인쇄 창에 전달합니다.
   */
  const approvalSignLines = (detail.lines || [])
    .filter((line) => Number(line.stepOrder) > 0)
    .sort((a, b) => Number(a.stepOrder) - Number(b.stepOrder));

  const signHeaderCells = approvalSignLines.map((line) => `
    <th>${escapeHtml(formatApprovalTarget(line))}</th>
  `).join('');

  const signImageCells = approvalSignLines.map((line) => {
    const signImg = signMap[line.approverNo];

    return `
      <td>
        ${signImg
          ? `<img src="${escapeHtml(buildResourceUrl(signImg))}" alt="인감" class="sign-img" />`
          : '<span class="muted">인감 미등록</span>'}
      </td>
    `;
  }).join('');

  const contentRows = content.invalid
    ? '<tr><td colspan="2">문서 본문 JSON을 해석하지 못했습니다.</td></tr>'
    : (content.fields || []).map((field) => `
        <tr>
          <th>${escapeHtml(field.label || '항목명 없음')}</th>
          <td>${escapeHtml(formatFieldValue(field)).replace(/\n/g, '<br />')}</td>
        </tr>
      `).join('');

  // 화면 상세와 동일하게 인쇄물에서도 실제 결재자와 참조자를 분리해서 렌더링합니다.
  const approvalLineRows = (detail.lines || [])
    .filter((line) => !line.reference)
    .map((line) => {
    return `
      <tr>
        <td>${escapeHtml(line.stepOrder)}단계</td>
        <td>${escapeHtml(formatApprovalTarget(line))}</td>
        <td>${escapeHtml(line.statusLabel || line.status || '-')}</td>
        <td>${escapeHtml(formatDateTime(line.processedAt))}</td>
      </tr>
    `;
  }).join('');

  const referenceLineRows = (detail.lines || [])
    .filter((line) => line.reference)
    .map((line) => `
      <tr>
        <td>${escapeHtml(formatApprovalTarget(line))}</td>
      </tr>
    `).join('');

  const fileRows = (detail.files || []).map((file) => {
    const fileUrl = buildResourceUrl(file.filePath);
    const previewHtml = isImagePath(file.filePath)
      ? `<img src="${escapeHtml(fileUrl)}" alt="${escapeHtml(file.fileName)}" class="file-img" />`
      : escapeHtml(file.fileName || '-');

    return `
      <div class="file-item">
        ${previewHtml}
        <div>${escapeHtml(file.fileName || '-')}</div>
        <div class="muted">${escapeHtml(formatFileSize(file.fileSize))}</div>
      </div>
    `;
  }).join('');

  /*
   * 브라우저 인쇄 대화상자에서 "PDF로 저장"을 선택할 수 있도록 별도 인쇄용 HTML을 구성합니다.
   * 화면 CSS와 분리하면 카드/버튼 같은 UI 요소가 섞이지 않아 결재 문서 출력물이 더 안정적입니다.
   */
  return `
    <!doctype html>
    <html lang="ko">
      <head>
        <meta charset="utf-8" />
        <title>${escapeHtml(detail.title || '결재 문서')}</title>
        <style>
          @page { size: A4; margin: 18mm; }
          body { font-family: "Malgun Gothic", Arial, sans-serif; color: #212529; }
          h1 { text-align: center; font-size: 24px; margin: 0 0 24px; }
          h2 { font-size: 16px; margin: 28px 0 10px; }
          table { width: 100%; border-collapse: collapse; margin-bottom: 14px; }
          th, td { border: 1px solid #ced4da; padding: 9px 10px; font-size: 13px; vertical-align: middle; }
          th { width: 170px; background: #f8f9fa; text-align: left; }
          .meta th { width: 130px; }
          .sign-img { width: 58px; height: 58px; object-fit: contain; }
          .sign-table { width: auto; margin-left: auto; margin-bottom: 18px; }
          .sign-table th, .sign-table td { min-width: 94px; text-align: center; }
          .sign-table .label { width: 56px; min-width: 56px; background: #f8f9fa; font-weight: 700; }
          .file-list { display: flex; gap: 12px; flex-wrap: wrap; }
          .file-item { width: 150px; font-size: 12px; border: 1px solid #dee2e6; padding: 8px; }
          .file-img { width: 100%; height: 95px; object-fit: cover; display: block; margin-bottom: 6px; }
          .muted { color: #6c757d; }
        </style>
      </head>
      <body>
        <h1>${escapeHtml(detail.title || '결재 문서')}</h1>

        <table class="sign-table">
          <tbody>
            <tr><th class="label">결재</th>${signHeaderCells || '<th>-</th>'}</tr>
            <tr><td class="label">인감</td>${signImageCells || '<td class="muted">결재자 없음</td>'}</tr>
          </tbody>
        </table>

        <h2>문서 정보</h2>
        <table class="meta">
          <tbody>
            <tr><th>서식</th><td>${escapeHtml(detail.formName || '-')}</td><th>상태</th><td>${escapeHtml(detail.statusLabel || detail.status || '-')}</td></tr>
            <tr><th>작성자</th><td>${escapeHtml(formatPerson(detail.writerName, detail.writerDeptName, detail.writerPositionName, detail.writerNo))}</td><th>현재 결재자</th><td>${escapeHtml(formatPerson(detail.currentApproverName, detail.currentApproverDeptName, detail.currentApproverPositionName, detail.currentApproverNo))}</td></tr>
            <tr><th>진행 단계</th><td>${Number(detail.maxStep) > 0 ? `${escapeHtml(detail.currentStep || 0)} / ${escapeHtml(detail.maxStep)}` : '-'}</td><th>작성일</th><td>${escapeHtml(formatDateTime(detail.createdAt))}</td></tr>
          </tbody>
        </table>

        <h2>문서 내용</h2>
        <table><tbody>${contentRows || '<tr><td>작성된 내용이 없습니다.</td></tr>'}</tbody></table>

        <h2>결재선</h2>
        <table>
          <thead>
            <tr><th>단계</th><th>대상자</th><th>상태</th><th>처리일시</th></tr>
          </thead>
          <tbody>${approvalLineRows || '<tr><td colspan="4">결재선 정보가 없습니다.</td></tr>'}</tbody>
        </table>

        <h2>참조</h2>
        <table>
          <thead>
            <tr><th>대상자</th></tr>
          </thead>
          <tbody>${referenceLineRows || '<tr><td>참조 대상자가 없습니다.</td></tr>'}</tbody>
        </table>

        <h2>첨부파일</h2>
        <div class="file-list">${fileRows || '<div class="muted">첨부파일이 없습니다.</div>'}</div>
      </body>
    </html>
  `;
};

// [전자결재] 결재 문서 상세 페이지
// 개인 문서함, 결재 예정 문서함처럼 같은 상세 데이터를 보여주는 화면에서 재사용할 수 있게
// 제목과 상신 취소 버튼 노출 여부를 props로 조절합니다.
const ApprovalsDetail = ({
  pageTitle = '개인 문서 상세',
  allowCancel = true,
  showPrint = true,
}) => {
  const navigate = useNavigate();
  const [userInfo] = useOutletContext();
  const [searchParams] = useSearchParams();
  const approvalId = searchParams.get('approvalId');

  const [detail, setDetail] = useState(null);
  const [signMap, setSignMap] = useState({});
  const [loading, setLoading] = useState(Boolean(approvalId));
  const [actionLoading, setActionLoading] = useState(false);
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

  // 결재선은 실제 승인/반려 처리를 하는 대상만, 참조는 열람 대상만 분리해 표시합니다.
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

  // 작성자 본인의 진행 중 문서에서만 상신 취소 버튼을 보여줍니다.
  // 최종 가능 여부는 백엔드가 다시 검증하므로 프론트 조건은 UX용 1차 필터입니다.
  const canCancel = allowCancel
    && detail
    && ['PENDING', 'IN_PROGRESS'].includes(detail.status)
    && String(detail.writerNo || '') === String(userInfo?.empNo || userInfo?.emp_no || '');

  useEffect(() => {
    if (!approvalId) {
      setErrorMessage('조회할 결재 문서 정보가 없습니다.');
      setLoading(false);
      return;
    }

    const fetchDetail = async () => {
      try {
        setLoading(true);
        setErrorMessage('');
        const response = await axiosInstance.get(PATH.API.APPROVAL.DETAIL(approvalId));
        setDetail(response.data);
      } catch (error) {
        console.error('결재 문서 상세 조회 실패:', error);
        setErrorMessage('결재 문서 상세 정보를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchDetail();
  }, [approvalId]);

  useEffect(() => {
    const approvalLines = (detail?.lines || []).filter((line) => Number(line.stepOrder) > 0);
    const approverNos = [...new Set(approvalLines.map((line) => line.approverNo).filter(Boolean))];

    if (approverNos.length === 0) {
      setSignMap({});
      return;
    }

    // 상세 응답에는 결재선 정보만 있으므로, 인감 이미지는 사원별 API를 모아 호출해 signMap으로 구성합니다.
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
  }, [detail?.lines]);

  const handleCancelApproval = async () => {
    if (!approvalId || !window.confirm('상신한 결재 문서를 취소하시겠습니까?')) {
      return;
    }

    try {
      setActionLoading(true);
      await axiosInstance.post(PATH.API.APPROVAL.CANCEL(approvalId));
      const response = await axiosInstance.get(PATH.API.APPROVAL.DETAIL(approvalId));
      setDetail(response.data);
      alert('상신이 취소되었습니다.');
    } catch (error) {
      console.error('상신 취소 실패:', error);
      const message = error.response?.data?.message
        || error.response?.data?.error
        || '상신 취소 중 오류가 발생했습니다. 완료 또는 반려된 문서인지 확인해 주세요.';
      alert(message);
    } finally {
      setActionLoading(false);
    }
  };

  const handlePrintDocument = () => {
    if (!detail) {
      return;
    }

    const printWindow = window.open('', '_blank', 'width=900,height=1000');
    if (!printWindow) {
      alert('팝업 차단을 해제한 뒤 다시 시도해 주세요.');
      return;
    }

    printWindow.document.open();
    printWindow.document.write(createPrintHtml(detail, content, signMap));
    printWindow.document.close();
    printWindow.focus();

    setTimeout(() => {
      printWindow.print();
    }, 300);
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
          <h2 className="mb-1">{pageTitle}</h2>
          <div className="text-body-secondary">
            {detail?.title || '결재 문서'}
          </div>
        </div>
        <CButton color="secondary" variant="outline" onClick={() => navigate(PATH.APPROVAL.PERSONAL)}>
          목록
        </CButton>
      </header>

      {errorMessage && <CAlert color="danger">{errorMessage}</CAlert>}

      {detail && (
        <>
          {/* 일반 결재 문서 양식처럼 결재자 이름과 인감 이미지를 문서 상단에 따로 배치합니다. */}
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
            {canCancel && (
              <CButton color="danger" onClick={handleCancelApproval} disabled={actionLoading}>
                상신 취소
              </CButton>
            )}
            {showPrint && (
              <CButton color="primary" variant="outline" onClick={handlePrintDocument}>
                인쇄/pdf로 저장
              </CButton>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default ApprovalsDetail;
