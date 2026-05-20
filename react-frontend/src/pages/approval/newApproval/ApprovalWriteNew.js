import React, { useEffect, useMemo, useState } from 'react';
import {
  CAlert,
  CButton,
  CCard,
  CCardBody,
  CCardHeader,
  CFormInput,
  CFormLabel,
  CFormSelect,
  CFormTextarea,
  CInputGroup,
  CInputGroupText,
  CSpinner,
} from '@coreui/react';
import { useLocation, useNavigate, useOutletContext } from 'react-router-dom';

import axiosInstance from 'src/api/axiosInstance';
import { PATH } from 'src/constants/path';
import { containerStyle } from 'src/styles/js/demoPageStyle';

// template 문자열을 작성 화면에서 쓰기 쉬운 객체로 변환합니다.
const parseTemplate = (template) => {
  if (!template) {
    return { title: '', fields: [], fileRequired: false };
  }

  try {
    const parsed = JSON.parse(template);
    return {
      title: parsed.title || '',
      fields: Array.isArray(parsed.fields) ? parsed.fields : [],
      fileRequired: Boolean(parsed.fileRequired),
    };
  } catch (error) {
    return { title: '', fields: [], fileRequired: false, invalid: true };
  }
};

const normalizeFieldValue = (field, value) => {
  if (field.type === 'amount') {
    return String(value || '').replace(/[^\d]/g, '');
  }

  return value;
};

const formatAmount = (value) => {
  const digits = String(value || '').replace(/[^\d]/g, '');
  return digits ? Number(digits).toLocaleString('ko-KR') : '';
};

// 파일 업로드 정책은 서식별로 달라질 수 있으므로 파일 타입 판별 함수를 분리해 둡니다.
// 현재는 비용 정산 신청에서 증빙 파일로 이미지/PDF만 허용할 때 사용합니다.
const isImageFile = (file) => file?.type?.startsWith('image/');

const isPdfFile = (file) =>
  file?.type === 'application/pdf' || file?.name?.toLowerCase().endsWith('.pdf');

const buildResourceUrl = (path) => {
  if (!path) {
    return '';
  }

  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }

  return `${PATH.API.BASE.replace(/\/api$/, '')}${path}`;
};

const parseContentFields = (content) => {
  if (!content) {
    return {};
  }

  try {
    const parsed = JSON.parse(content);
    return (Array.isArray(parsed.fields) ? parsed.fields : []).reduce((acc, field) => {
      acc[field.id] = field.value || '';
      return acc;
    }, {});
  } catch (error) {
    return {};
  }
};

const createEmptyValues = (fields) =>
  fields.reduce((acc, field) => {
    acc[field.id] = '';
    return acc;
  }, {});

// [전자결재] 새 결재 진행 - 결재 내용 작성 페이지
const ApprovalWriteNew = () => {
  const [userInfo] = useOutletContext();
  const navigate = useNavigate();
  const location = useLocation();

  const initialForm = location.state?.selectedForm || null;
  const draftId = location.state?.draftId || null;

  // [결재-근태 연동용]: 근태 화면에서 넘어온 서식 기본값과 사용자가 수정하면 안 되는 필드 목록입니다.
  const presetFieldValues = useMemo(
    () => location.state?.presetFieldValues || {},
    [location.state?.presetFieldValues]
  );
  const lockedFieldIds = useMemo(
    () => location.state?.lockedFieldIds || [],
    [location.state?.lockedFieldIds]
  );
  const lockedFieldIdSet = useMemo(
    () => new Set(lockedFieldIds),
    [lockedFieldIds]
  );

  const [selectedForm, setSelectedForm] = useState(initialForm);
  const [loading, setLoading] = useState(Boolean(initialForm?.formId || draftId));
  const [errorMessage, setErrorMessage] = useState('');
  const [fieldValues, setFieldValues] = useState({});
  const [files, setFiles] = useState([]);
  const [existingFiles, setExistingFiles] = useState([]);
  const [draftLines, setDraftLines] = useState([]);
  const [fileInputKey, setFileInputKey] = useState(0);
  const [saving, setSaving] = useState(false);

  const template = useMemo(
    () => parseTemplate(selectedForm?.template),
    [selectedForm]
  );

  const documentTitle = template.title || selectedForm?.formName || '';

  /*
   * template.fileRequired=false는 "첨부파일이 선택 사항"이 아니라
   * "이 서식에는 첨부파일 입력 UI를 아예 보여주지 않는다"는 의미로 사용합니다.
   */
  const canAttachFile = template.fileRequired === true;
  const isExpenseSettlementForm =
    selectedForm?.formName === '비용 정산 신청' || documentTitle === '비용 정산 신청';
  const fileAccept = isExpenseSettlementForm ? 'image/*,.pdf,application/pdf' : undefined;

  const filePreviews = useMemo(
    () =>
      files.map((file) => ({
        file,
        previewUrl: isImageFile(file) ? URL.createObjectURL(file) : '',
      })),
    [files]
  );

  // 임시저장함에서 들어온 경우에는 저장된 문서 상세와 최신 서식 정보를 함께 불러옵니다.
  useEffect(() => {
    if (!draftId) {
      return;
    }

    const fetchDraftDetail = async () => {
      try {
        setLoading(true);
        setErrorMessage('');

        const detailResponse = await axiosInstance.get(PATH.API.APPROVAL.DETAIL(draftId));
        const detail = detailResponse.data;

        if (detail?.status !== 'DRAFT') {
          setErrorMessage('임시저장 상태의 문서만 수정할 수 있습니다.');
          return;
        }

        const formResponse = await axiosInstance.get(PATH.API.APPROVAL.FORM_DETAIL(detail.formId));
        setSelectedForm(formResponse.data);
        setFieldValues(parseContentFields(detail.content));
        setExistingFiles(detail.files || []);
        setDraftLines(detail.lines || []);
      } catch (error) {
        console.error('임시저장 문서 상세 조회 실패:', error);
        setErrorMessage('임시저장 문서를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchDraftDetail();
  }, [draftId]);

  // 서식 목록에서 받은 데이터가 오래되었을 수 있어 작성 화면 진입 시 상세 API로 최신 template을 다시 조회합니다.
  useEffect(() => {
    if (draftId) {
      return;
    }

    if (!initialForm?.formId) {
      setErrorMessage('먼저 결재 서식을 선택해 주세요.');
      setLoading(false);
      return;
    }

    const fetchLatestForm = async () => {
      try {
        setLoading(true);
        const response = await axiosInstance.get(
          PATH.API.APPROVAL.FORM_DETAIL(initialForm.formId)
        );
        setSelectedForm(response.data);
      } catch (error) {
        console.error('결재 서식 상세 조회 실패:', error);
        setErrorMessage('결재 서식 정보를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchLatestForm();
  }, [draftId, initialForm?.formId]);

  useEffect(() => {
    setFieldValues((prev) => ({
      ...createEmptyValues(template.fields),
      ...prev,
      // [결재-근태 연동용]: 근태 화면에서 계산한 조퇴/외근 기본값이 기존 빈 값보다 우선 적용되게 합니다.
      ...presetFieldValues,
    }));
  }, [presetFieldValues, template.fields]);

  // 첨부파일이 필요 없는 서식으로 바뀌면 이전 선택 파일이 함께 제출되지 않도록 즉시 비웁니다.
  useEffect(() => {
    if (!canAttachFile) {
      setFiles([]);
    }
  }, [canAttachFile]);

  useEffect(() => {
    return () => {
      filePreviews.forEach((item) => {
        if (item.previewUrl) {
          URL.revokeObjectURL(item.previewUrl);
        }
      });
    };
  }, [filePreviews]);

  // 금액 필드는 화면에는 콤마가 보이지만 서버 저장값은 숫자만 남도록 정규화합니다.
  const updateFieldValue = (field, value) => {
    setFieldValues((prev) => ({
      ...prev,
      [field.id]: normalizeFieldValue(field, value),
    }));
  };

  const canUploadFile = (file) => {
    if (!isExpenseSettlementForm) {
      return true;
    }

    return isImageFile(file) || isPdfFile(file);
  };

  const handleFileChange = (event) => {
    const selectedFiles = Array.from(event.target.files || []);
    const availableFiles = selectedFiles.filter(canUploadFile);

    if (availableFiles.length !== selectedFiles.length) {
      setErrorMessage('비용 정산 신청은 이미지 또는 PDF 파일만 첨부할 수 있습니다.');
      setFileInputKey((prev) => prev + 1);
    } else {
      setErrorMessage('');
    }

    setFiles(availableFiles);
  };

  const removeFile = (index) => {
    setFiles((prev) => prev.filter((_, fileIndex) => fileIndex !== index));
    setFileInputKey((prev) => prev + 1);
  };

  const removeExistingFile = async (fileId) => {
    if (!window.confirm('기존 첨부파일을 삭제하시겠습니까?')) {
      return;
    }

    try {
      await axiosInstance.delete(PATH.API.APPROVAL.DELETE_FILE(fileId));
      setExistingFiles((prev) => prev.filter((file) => file.fileId !== fileId));
    } catch (error) {
      console.error('임시저장 첨부파일 삭제 실패:', error);
      setErrorMessage('첨부파일 삭제 중 오류가 발생했습니다.');
    }
  };

  const buildContent = () => {
    const fields = template.fields.map((field) => ({
      id: field.id,
      type: field.type,
      label: field.label,
      value: fieldValues[field.id] || '',
    }));

    return JSON.stringify({
      formId: selectedForm.formId,
      formName: selectedForm.formName,
      title: documentTitle,
      fields,
    });
  };

  const buildRequestPayload = (approvalLines = []) => ({
    formId: selectedForm.formId,
    title: documentTitle,
    content: buildContent(),
    approvalLines,
  });

  const validateWriteForm = () => {
    if (!selectedForm?.formId) {
      setErrorMessage('결재 서식 정보가 없습니다.');
      return false;
    }

    if (!documentTitle) {
      setErrorMessage('결재 서식 제목을 확인해 주세요.');
      return false;
    }

    if (canAttachFile && files.length === 0 && existingFiles.length === 0) {
      setErrorMessage('이 서식은 첨부파일이 필수입니다.');
      return false;
    }

    if (isExpenseSettlementForm && files.some((file) => !canUploadFile(file))) {
      setErrorMessage('비용 정산 신청은 이미지 또는 PDF 파일만 첨부할 수 있습니다.');
      return false;
    }

    setErrorMessage('');
    return true;
  };

  const requestApprovalApi = async (apiPath, payload, method = 'post') => {
    if (!canAttachFile || files.length === 0) {
      return axiosInstance[method](apiPath, payload);
    }

    /*
     * 첨부파일이 있는 경우 request(JSON)와 files(binary)를 multipart/form-data로 함께 전송합니다.
     * 백엔드는 @RequestPart("request")와 @RequestPart("files")로 같은 API에서 처리합니다.
     */
    const formData = new FormData();
    formData.append(
      'request',
      new Blob([JSON.stringify(payload)], { type: 'application/json' })
    );
    files.forEach((file) => formData.append('files', file));

    return axiosInstance[method](apiPath, formData);
  };

  const saveDraft = async () => {
    if (!validateWriteForm()) {
      return;
    }

    try {
      setSaving(true);
      const apiPath = draftId ? PATH.API.APPROVAL.UPDATE_DRAFT(draftId) : PATH.API.APPROVAL.DRAFTS;
      const method = draftId ? 'put' : 'post';
      await requestApprovalApi(apiPath, buildRequestPayload(draftLines), method);
      alert('임시저장되었습니다.');
      navigate(PATH.APPROVAL.TMP);
    } catch (error) {
      console.error('임시저장 실패:', error);
      setErrorMessage('임시저장 중 오류가 발생했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const moveToLineStep = () => {
    if (!validateWriteForm()) {
      return;
    }

    navigate(PATH.APPROVAL.NEW_SETLINE, {
      state: {
        selectedForm,
        draftId,
        draftLines,
        existingFiles,
        documentTitle,
        content: buildContent(),
        files: canAttachFile ? files : [],
        // [결재-근태 연동용]: 결재선 설정 화면을 거쳐도 자동 입력값과 잠금 필드 정보를 유지합니다.
        presetFieldValues,
        lockedFieldIds,
      },
    });
  };

  const renderField = (field) => {
    // [결재-근태 연동용]: 조퇴 시작 시각, 외근 퇴근 시각처럼 근태 기준값으로 고정해야 하는 필드를 잠급니다.
    const isLocked = lockedFieldIdSet.has(field.id);
    const commonProps = {
      id: field.id,
      value: fieldValues[field.id] || '',
      onChange: (event) => updateFieldValue(field, event.target.value),
    };

    if (field.type === 'select') {
      const options = Array.isArray(field.options) ? field.options : [];
      return (
        <CFormSelect {...commonProps} disabled={isLocked}>
          <option value="">선택</option>
          {options.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </CFormSelect>
      );
    }

    if (field.type === 'text') {
      return <CFormTextarea {...commonProps} rows={3} placeholder={field.placeholder || ''} readOnly={isLocked} />;
    }

    // amount는 사용자가 입력하는 동안에도 1,000원 형식으로 읽히도록 inputGroup으로 렌더링합니다.
    if (field.type === 'amount') {
      return (
        <CInputGroup>
          <CFormInput
            {...commonProps}
            type="text"
            inputMode="numeric"
            value={formatAmount(fieldValues[field.id])}
            placeholder={field.placeholder || '0'}
            readOnly={isLocked}
          />
          <CInputGroupText>원</CInputGroupText>
        </CInputGroup>
      );
    }

    return (
      <CFormInput
        {...commonProps}
        type={field.type === 'amount' ? 'text' : field.type || 'text'}
        placeholder={field.placeholder || ''}
        readOnly={isLocked}
      />
    );
  };

  const renderFilePreview = () => {
    if (files.length === 0) {
      return null;
    }

    return (
      <div className="d-flex flex-wrap gap-3 mt-3">
        {filePreviews.map((item, index) => (
          <div
            className="border rounded position-relative bg-light"
            style={{ width: '150px', minHeight: '150px', overflow: 'hidden' }}
            key={`${item.file.name}-${item.file.lastModified}-${index}`}
          >
            <CButton
              color="danger"
              size="sm"
              className="position-absolute top-0 end-0 m-1"
              style={{ zIndex: 1, lineHeight: 1 }}
              onClick={() => removeFile(index)}
            >
              x
            </CButton>

            {item.previewUrl ? (
              <img
                src={item.previewUrl}
                alt={item.file.name}
                style={{ width: '100%', height: '110px', objectFit: 'cover' }}
              />
            ) : (
              <div className="d-flex align-items-center justify-content-center" style={{ height: '110px' }}>
                <strong>{isPdfFile(item.file) ? 'PDF' : 'FILE'}</strong>
              </div>
            )}

            <div className="small px-2 py-2 text-truncate" title={item.file.name}>
              {item.file.name}
            </div>
          </div>
        ))}
      </div>
    );
  };

  const renderExistingFiles = () => {
    if (existingFiles.length === 0) {
      return null;
    }

    return (
      <div className="d-flex flex-wrap gap-3 mt-3">
        {existingFiles.map((file) => (
          <div
            className="border rounded position-relative bg-light"
            style={{ width: '150px', minHeight: '150px', overflow: 'hidden' }}
            key={file.fileId}
          >
            <CButton
              color="danger"
              size="sm"
              className="position-absolute top-0 end-0 m-1"
              style={{ zIndex: 1, lineHeight: 1 }}
              onClick={() => removeExistingFile(file.fileId)}
            >
              x
            </CButton>

            {file.filePath?.match(/\.(png|jpe?g|gif|webp)$/i) ? (
              <img
                src={buildResourceUrl(file.filePath)}
                alt={file.fileName}
                style={{ width: '100%', height: '110px', objectFit: 'cover' }}
              />
            ) : (
              <div className="d-flex align-items-center justify-content-center" style={{ height: '110px' }}>
                <strong>FILE</strong>
              </div>
            )}

            <div className="small px-2 py-2 text-truncate" title={file.fileName}>
              {file.fileName}
            </div>
          </div>
        ))}
      </div>
    );
  };

  if (loading) {
    return (
      <div style={containerStyle} className="py-5 text-center">
        <CSpinner size="sm" className="me-2" />
        결재 서식을 불러오는 중입니다.
      </div>
    );
  }

  return (
    <div style={containerStyle}>
      <header className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-1">{documentTitle || '결재 문서 작성'}</h2>
          <div className="text-body-secondary">
            {selectedForm?.formName || '서식 미선택'}
            {userInfo?.name ? ` · 작성자 ${userInfo.name}` : ''}
          </div>
        </div>
        <CButton color="secondary" variant="outline" onClick={() => navigate(PATH.APPROVAL.NEW_SELECT)}>
          서식 다시 선택
        </CButton>
      </header>

      {errorMessage && <CAlert color="danger">{errorMessage}</CAlert>}

      <CCard className="mb-4">
        <CCardHeader>
          <strong>문서 내용</strong>
        </CCardHeader>
        <CCardBody>
          {template.invalid ? (
            <CAlert color="danger">서식 JSON 형식이 올바르지 않습니다. 관리자에게 문의해 주세요.</CAlert>
          ) : (
            template.fields.map((field) => (
              <div className="mb-4" key={field.id}>
                <CFormLabel htmlFor={field.id}>{field.label || '항목명 없음'}</CFormLabel>
                {renderField(field)}
                {field.description && (
                  <div className="form-text">{field.description}</div>
                )}
              </div>
            ))
          )}

          {canAttachFile && (
            <div className="mb-4">
              <CFormLabel htmlFor="approval-files">첨부파일 (필수)</CFormLabel>
              {renderExistingFiles()}
              <CFormInput
                key={fileInputKey}
                id="approval-files"
                type="file"
                multiple
                accept={fileAccept}
                onChange={handleFileChange}
              />
              {isExpenseSettlementForm && (
                <div className="form-text">
                  비용 정산 증빙은 이미지 또는 PDF 파일만 첨부할 수 있습니다.
                </div>
              )}
              {renderFilePreview()}
            </div>
          )}

          <div className="d-flex justify-content-end gap-2">
            <CButton color="secondary" variant="outline" onClick={saveDraft} disabled={saving}>
              임시저장
            </CButton>
            <CButton color="primary" onClick={moveToLineStep} disabled={template.invalid || saving}>
              결재선 설정
            </CButton>
          </div>
        </CCardBody>
      </CCard>
    </div>
  );
};

export default ApprovalWriteNew;
