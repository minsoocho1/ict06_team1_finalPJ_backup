import React, { useEffect, useMemo, useState } from 'react';
import {
  CAlert,
  CBadge,
  CButton,
  CCard,
  CCardBody,
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
import { useNavigate, useOutletContext } from 'react-router-dom';

import axiosInstance from 'src/api/axiosInstance';
import { PATH } from 'src/constants/path';
import { containerStyle } from 'src/styles/js/demoPageStyle';
import { canViewApproverMenus } from './components/approvalAuth';

const SUMMARY_SIZE = 3;

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

const readPageContent = (response) => response.data?.content || [];

const getSummaryTime = (document) => new Date(document.updatedAt || document.createdAt || 0).getTime();

// [전자결재] 메인 화면
const Approval = () => {
  const outletContext = useOutletContext();
  const [contextUserInfo] = Array.isArray(outletContext) ? outletContext : [];
  const navigate = useNavigate();

  const [summary, setSummary] = useState({
    mine: [],
    referenced: [],
    drafts: [],
    pending: [],
    upcoming: [],
  });
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const showApproverMenus = canViewApproverMenus(contextUserInfo);

  useEffect(() => {
    const fetchSummary = async () => {
      try {
        setLoading(true);
        setErrorMessage('');

        const commonParams = {
          page: 0,
          size: SUMMARY_SIZE,
        };

        /*
         * 백엔드의 기존 목록 API를 그대로 활용해 각 문서함의 앞 3건만 조회합니다.
         * 목록 API는 updatedAt 내림차순으로 정렬하므로 사용자는 최근 작업 문서를 먼저 볼 수 있습니다.
         */
        const requests = [
          axiosInstance.get(PATH.API.APPROVAL.MY_DOCUMENTS, { params: commonParams }),
          axiosInstance.get(PATH.API.APPROVAL.REFERENCED_DOCUMENTS, { params: commonParams }),
          axiosInstance.get(PATH.API.APPROVAL.DRAFTS, { params: commonParams }),
        ];

        if (showApproverMenus) {
          requests.push(
            axiosInstance.get(PATH.API.APPROVAL.PENDING_DOCUMENTS, { params: commonParams }),
            axiosInstance.get(PATH.API.APPROVAL.UPCOMING_DOCUMENTS, { params: commonParams })
          );
        }

        const responses = await Promise.all(requests);

        setSummary({
          mine: readPageContent(responses[0]),
          referenced: readPageContent(responses[1]),
          drafts: readPageContent(responses[2]),
          pending: showApproverMenus ? readPageContent(responses[3]) : [],
          upcoming: showApproverMenus ? readPageContent(responses[4]) : [],
        });
      } catch (error) {
        console.error('전자결재 요약 문서함 조회 실패:', error);
        setErrorMessage('전자결재 요약 문서함을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchSummary();
  }, [showApproverMenus]);

  const cards = useMemo(() => {
    /*
     * 개인문서함은 "내가 상신한 문서"와 "참조 문서"를 같은 카드에서 보여줍니다.
     * 두 API 응답을 합친 뒤 최신순으로 다시 정렬해, 사용자가 개인문서함의 최근 흐름을 한눈에 볼 수 있게 합니다.
     */
    const personalDocuments = [
      ...summary.mine.map((document) => ({
        ...document,
        summaryType: 'submitted',
        summaryTypeLabel: '상신',
        summaryTypeBadgeColor: 'primary',
      })),
      ...summary.referenced.map((document) => ({
        ...document,
        summaryType: 'referenced',
        summaryTypeLabel: '참조',
        summaryTypeBadgeColor: 'info',
      })),
    ]
      .sort((left, right) => getSummaryTime(right) - getSummaryTime(left))
      .slice(0, SUMMARY_SIZE);

    const baseCards = [
      {
        key: 'personal',
        title: '개인문서함',
        documents: personalDocuments,
        morePath: PATH.APPROVAL.PERSONAL,
        openDocument: (document) => navigate(PATH.APPROVAL.PERSONAL_DETAIL_WITH_ID(document.approvalId), {
          state: { from: document.summaryType === 'referenced' ? 'referenced' : 'mine' },
        }),
      },
      {
        key: 'drafts',
        title: '임시저장 문서',
        documents: summary.drafts,
        morePath: PATH.APPROVAL.TMP,
        openDocument: (document) => navigate(PATH.APPROVAL.TMP_DETAIL_WITH_ID(document.approvalId)),
      },
    ];

    if (!showApproverMenus) {
      return baseCards;
    }

    return [
      ...baseCards,
      {
        key: 'pending',
        title: '결재 대기 문서',
        documents: summary.pending,
        morePath: PATH.APPROVAL.PENDING,
        openDocument: (document) => navigate(PATH.APPROVAL.PENDING_DETAIL_WITH_ID(document.approvalId)),
      },
      {
        key: 'upcoming',
        title: '결재 예정 문서',
        documents: summary.upcoming,
        morePath: PATH.APPROVAL.UPCOMING,
        openDocument: (document) => navigate(PATH.APPROVAL.UPCOMING_DETAIL_WITH_ID(document.approvalId)),
      },
    ];
  }, [navigate, showApproverMenus, summary]);

  return (
    <div style={containerStyle}>
      <header className="mb-4">
        <h2 className="mb-1">전자결재 메인</h2>
        <div className="text-body-secondary">
          {contextUserInfo?.name
            ? `${contextUserInfo.name}님의 최근 결재 문서를 문서함별로 확인합니다.`
            : '최근 결재 문서를 문서함별로 확인합니다.'}
        </div>
      </header>

      {errorMessage && <CAlert color="danger">{errorMessage}</CAlert>}

      {loading ? (
        <div className="py-5 text-center">
          <CSpinner size="sm" className="me-2" />
          요약 문서함을 불러오는 중입니다.
        </div>
      ) : (
        <CRow className="g-4">
          {cards.map((card) => (
            <CCol xs={12} lg={6} key={card.key}>
              <SummaryCard card={card} />
            </CCol>
          ))}
        </CRow>
      )}
    </div>
  );
};

const SummaryCard = ({ card }) => {
  const navigate = useNavigate();

  return (
    <CCard className="h-100 border-0 shadow-sm" style={{ borderRadius: 8 }}>
      <CCardBody>
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h5 className="mb-0 fw-bold">{card.title}</h5>
          <CButton
            type="button"
            color="primary"
            variant="outline"
            size="sm"
            onClick={() => navigate(card.morePath)}
            title={`${card.title} 더보기`}
            aria-label={`${card.title} 더보기`}
          >
            더보기
          </CButton>
        </div>

        <CTable hover responsive align="middle" className="mb-0">
          <CTableHead>
            <CTableRow>
              <CTableHeaderCell>문서 제목</CTableHeaderCell>
              {card.documents.some((document) => document.summaryTypeLabel) && (
                <CTableHeaderCell style={{ width: 90 }}>분류</CTableHeaderCell>
              )}
              <CTableHeaderCell style={{ width: 160 }}>작성일</CTableHeaderCell>
            </CTableRow>
          </CTableHead>
          <CTableBody>
            {card.documents.length === 0 ? (
              <CTableRow>
                <CTableDataCell
                  colSpan={card.documents.some((document) => document.summaryTypeLabel) ? 3 : 2}
                  className="text-center text-body-secondary py-4"
                >
                  표시할 문서가 없습니다.
                </CTableDataCell>
              </CTableRow>
            ) : (
              card.documents.map((document) => (
                <CTableRow
                  key={`${document.summaryType || card.key}-${document.approvalId}`}
                  role="button"
                  onClick={() => card.openDocument(document)}
                >
                  <CTableDataCell>
                    <strong>{document.title || '-'}</strong>
                  </CTableDataCell>
                  {card.documents.some((item) => item.summaryTypeLabel) && (
                    <CTableDataCell>
                      <CBadge color={document.summaryTypeBadgeColor || 'secondary'}>
                        {document.summaryTypeLabel || '-'}
                      </CBadge>
                    </CTableDataCell>
                  )}
                  <CTableDataCell>{formatDateTime(document.createdAt || document.updatedAt)}</CTableDataCell>
                </CTableRow>
              ))
            )}
          </CTableBody>
        </CTable>
      </CCardBody>
    </CCard>
  );
};

export default Approval;
