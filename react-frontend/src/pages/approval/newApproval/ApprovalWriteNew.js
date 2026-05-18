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

const isImageFile = (file) => file?.type?.startsWith('image/');

const isPdfFile = (file) =>
  file?.type === 'application/pdf' || file?.name?.toLowerCase().endsWith('.pdf');

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

  const [selectedForm, setSelectedForm] = useState(initialForm);
  const [loading, setLoading] = useState(Boolean(initialForm?.formId));
  const [errorMessage, setErrorMessage] = useState('');
  const [fieldValues, setFieldValues] = useState({});
  const [files, setFiles] = useState([]);
  const [fileInputKey, setFileInputKey] = useState(0);
  const [saving, setSaving] = useState(false);

  const template = useMemo(
    () => parseTemplate(selectedForm?.template),
    [selectedForm]
  );

  const documentTitle = template.title || selectedForm?.formName || '';
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

  useEffect(() => {
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
  }, [initialForm?.formId]);

  useEffect(() => {
    setFieldValues(createEmptyValues(template.fields));
  }, [template.fields]);

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

    if (canAttachFile && files.length === 0) {
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

  const requestApprovalApi = async (apiPath, payload) => {
    if (!canAttachFile || files.length === 0) {
      return axiosInstance.post(apiPath, payload);
    }

    const formData = new FormData();
    formData.append(
      'request',
      new Blob([JSON.stringify(payload)], { type: 'application/json' })
    );
    files.forEach((file) => formData.append('files', file));

    return axiosInstance.post(apiPath, formData);
  };

  const saveDraft = async () => {
    if (!validateWriteForm()) {
      return;
    }

    try {
      setSaving(true);
      await requestApprovalApi(PATH.API.APPROVAL.DRAFTS, buildRequestPayload([]));
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
        documentTitle,
        content: buildContent(),
        files: canAttachFile ? files : [],
      },
    });
  };

  const renderField = (field) => {
    const commonProps = {
      id: field.id,
      value: fieldValues[field.id] || '',
      onChange: (event) => updateFieldValue(field, event.target.value),
    };

    if (field.type === 'select') {
      const options = Array.isArray(field.options) ? field.options : [];
      return (
        <CFormSelect {...commonProps}>
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
      return <CFormTextarea {...commonProps} rows={3} placeholder={field.placeholder || ''} />;
    }

    if (field.type === 'amount') {
      return (
        <CInputGroup>
          <CFormInput
            {...commonProps}
            type="text"
            inputMode="numeric"
            value={formatAmount(fieldValues[field.id])}
            placeholder={field.placeholder || '0'}
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
