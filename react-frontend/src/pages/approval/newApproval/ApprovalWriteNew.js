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
      ocr: parsed.ocr || {},
    };
  } catch (error) {
    return { title: '', fields: [], fileRequired: false, ocr: {}, invalid: true };
  }
};

// 입력값을 서버에 저장하기 전 필드 타입에 맞게 정리합니다.
const normalizeFieldValue = (field, value) => {
  if (field.type === 'amount') {
    return String(value || '').replace(/[^\d]/g, '');
  }

  return value;
};

// 금액 입력값을 화면에 표시할 때 1,000 단위 콤마 형식으로 변환합니다.
const formatAmount = (value) => {
  const digits = String(value || '').replace(/[^\d]/g, '');
  return digits ? Number(digits).toLocaleString('ko-KR') : '';
};

// 파일 업로드 정책은 서식별로 달라질 수 있으므로 파일 타입 판별 함수를 분리해 둡니다.
// 현재는 비용 정산 신청에서 증빙 파일로 이미지/PDF만 허용할 때 사용합니다.
const isImageFile = (file) => file?.type?.startsWith('image/');

// 선택한 파일이 PDF인지 확인하여 비용 정산 증빙 업로드 검증에 활용합니다.
const isPdfFile = (file) =>
  file?.type === 'application/pdf' || file?.name?.toLowerCase().endsWith('.pdf');

// 서버가 내려준 상대 파일 경로를 브라우저에서 접근 가능한 전체 URL로 변환합니다.
const buildResourceUrl = (path) => {
  if (!path) {
    return '';
  }

  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }

  return `${PATH.API.BASE.replace(/\/api$/, '')}${path}`;
};

// 임시저장 문서의 content JSON에서 필드별 기존 입력값을 복원합니다.
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

// 선택한 서식의 모든 입력 필드를 빈 값으로 초기화합니다.
const createEmptyValues = (fields) =>
  fields.reduce((acc, field) => {
    acc[field.id] = '';
    return acc;
  }, {});

// 날짜/시간 범위 검증에서 시작 필드와 종료 필드를 찾기 위해 id와 label을 같은 기준으로 정리합니다.
const normalizeRangeKey = (value) =>
  String(value || '') // 1. 값이 없으면 빈 문자열로 만들고 문자열로 변환합니다.
    .toLowerCase() // 2. 영문 대문자를 모두 소문자로 변경합니다.
    .replace(/[_\s]/g, '') // 3. 언더스코어(_)와 공백(띄어쓰기)을 모두 제거합니다.
    .replace(/start|end|from|to|시작|종료|끝|부터|까지/g, ''); // 4. 시작/종료와 관련된 단어들을 지워 순수 '본문 키'만 남깁니다. (예: "startDate" -> "date")

const hasStartText = (field) =>
  // 필드의 id와 label을 공백으로 합친 문자열에서 'start', 'from', '시작', '부터'가 포함되어 있는지 대소문자 구분 없이 검사합니다.
  /start|from|시작|부터/i.test(`${field.id || ''} ${field.label || ''}`);


const hasEndText = (field) =>
  // 필드의 id와 label을 공백으로 합친 문자열에서 'end', 'to', '종료', '끝', '까지'가 포함되어 있는지 대소문자 구분 없이 검사합니다.
  /end|to|종료|끝|까지/i.test(`${field.id || ''} ${field.label || ''}`);


const getComparableRangeKeys = (field) =>
  // 필드의 id와 label을 각각 정규화한 뒤, 빈 문자열을 제외(filter(Boolean))하고 유효한 값만 배열로 반환합니다.
  [normalizeRangeKey(field.id), normalizeRangeKey(field.label)].filter(Boolean);

// 시작/종료 의미가 있는 date 또는 time 필드를 자동으로 짝지어 반환합니다.
const findRangePairs = (fields, type) => {
  // 1. 전체 필드 중 매개변수로 받은 타입('date' 또는 'time')과 일치하는 필드만 골라냅니다.
  const rangeFields = fields.filter((field) => field.type === type);
  // 2. 그중에서 시작 의미를 가진 필드들을 필터링합니다.
  const startFields = rangeFields.filter(hasStartText);
  // 3. 그중에서 종료 의미를 가진 필드들을 필터링합니다.
  const endFields = rangeFields.filter(hasEndText);

  return endFields
    .map((endField) => {
      // 4. 종료 필드의 정규화된 비교 키 배열을 가져옵니다.
      const endKeys = getComparableRangeKeys(endField);
      // 5. 시작 필드 목록 중에서 종료 필드와 동일한 '본문 키'를 공유하는 필드를 찾습니다.
      const startField = startFields.find((candidate) => {
        const startKeys = getComparableRangeKeys(candidate);
        // 시작 필드의 키 중 하나라도 종료 필드의 키에 포함되어 있으면 매칭됩니다.
        return startKeys.some((key) => endKeys.includes(key));
      });

      // 6. 매칭되는 시작 필드가 있으면 객체로 묶어 반환하고, 없으면 null을 반환합니다.
      return startField ? { startField, endField } : null;
    })
    .filter(Boolean); // 7. null로 반환된 항목들을 배열에서 완전히 제거합니다.
};

// 결재 서식의 날짜/시간 범위에서 종료값이 시작값보다 빠른지 검증합니다.
const validateDateTimeRanges = (fields, values) => {
  // 1. 자동으로 날짜(date) 필드 쌍과 시간(time) 필드 쌍을 찾아 배열로 저장합니다.
  const datePairs = findRangePairs(fields, 'date');
  const timePairs = findRangePairs(fields, 'time');

  // 2. 날짜 필드 쌍을 순회하며 검증합니다.
  for (const { startField, endField } of datePairs) {
    const startValue = values[startField.id]; // 시작 날짜 값
    const endValue = values[endField.id]; // 종료 날짜 값

    // 시작값과 종료값이 모두 입력되었는데, 종료일이 시작일보다 빠르면 에러 메시지를 즉시 반환합니다.
    if (startValue && endValue && endValue < startValue) {
      return `${endField.label || '종료일'}은(는) ${startField.label || '시작일'}보다 빠를 수 없습니다.`;
    }
  }

  // 3. 사용자가 며칠 이상(Multi-Day)에 걸친 기간을 선택했는지 여부를 체크합니다.
  const hasMultiDayRange = datePairs.some(({ startField, endField }) => {
    const startValue = values[startField.id];
    const endValue = values[endField.id];
    // 종료일이 시작일보다 미래인 날짜 쌍이 하나라도 있으면 true가 됩니다.
    return startValue && endValue && endValue > startValue;
  });

  // 4. 시간 필드 쌍을 순회하며 검증합니다.
  for (const { startField, endField } of timePairs) {
    const startValue = values[startField.id]; // 시작 시간 값
    const endValue = values[endField.id]; // 종료 시간 값

    // 다른 날로 넘어가는 기간(hasMultiDayRange가 true)이 아닐 때만 시간 순서를 검사합니다.
    // (예: 5월 28일 23시 ~ 5월 29일 01시 처럼 날짜가 다르면 종료 시간이 시작 시간보다 빨라도 정상입니다.)
    if (!hasMultiDayRange && startValue && endValue && endValue < startValue) {
      return `${endField.label || '종료 시간'}은(는) ${startField.label || '시작 시간'}보다 빠를 수 없습니다.`;
    }
  }

  // 5. 모든 검증을 통과하면 빈 문자열을 반환하여 에러가 없음을 알립니다.
  return '';
};

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
  const [ocrLoading, setOcrLoading] = useState(false);

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
  const isReceiptOcrForm =
    template.ocr?.enabled === true && template.ocr?.documentType === 'receipt';
  const isExpenseSettlementForm =
    selectedForm?.formName === '비용 정산 신청' || documentTitle === '비용 정산 신청';
  const fileAccept = (isExpenseSettlementForm || isReceiptOcrForm)
    ? 'image/*,.pdf,application/pdf'
    : undefined;

  const filePreviews = useMemo(
    () =>
      files.map((file) => ({
        file,
        previewUrl: isImageFile(file) ? URL.createObjectURL(file) : '',
      })),
    [files]
  );

  // 임시저장함에서 들어온 경우에는 저장된 문서 상세와 최신 서식 정보를 함께 불러옵니다.
  // 서식 필드가 준비되면 화면 입력값 state를 초기화하고 기존 입력값은 유지합니다.
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
  // 이미지 미리보기에 사용한 Object URL을 해제하여 브라우저 메모리 누수를 방지합니다.
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

  // 서식 필드가 준비되면 화면 입력값 state를 초기화하고 기존 입력값은 유지합니다.
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

  // 이미지 미리보기에 사용한 Object URL을 해제하여 브라우저 메모리 누수를 방지합니다.
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

  // 비용 정산/OCR 대상 서식에서는 이미지 또는 PDF 파일만 업로드할 수 있게 검증합니다.
  const canUploadFile = (file) => {
    if (!isExpenseSettlementForm && !isReceiptOcrForm) {
      return true;
    }

    return isImageFile(file) || isPdfFile(file);
  };

  // [전자결재-OCR 연동용]: 영수증 파일을 백엔드 OCR API로 보내고, 인식 결과를 현재 서식 필드에 자동 반영합니다.
  const runReceiptOcr = async (targetFile = files[0]) => {
    if (!isReceiptOcrForm && !isExpenseSettlementForm) {
      return;
    }

    if (!targetFile) {
      setErrorMessage('OCR 인식할 영수증 파일을 먼저 첨부해주세요.');
      return;
    }

    if (!canUploadFile(targetFile)) {
      setErrorMessage('영수증 OCR은 이미지 또는 PDF 파일만 사용할 수 있습니다.');
      return;
    }

    try {
      setOcrLoading(true);
      const formData = new FormData();
      formData.append('file', targetFile);

      const response = await axiosInstance.post(PATH.API.APPROVAL.RECEIPT_OCR, formData);
      const nextValues = response.data?.fieldValues || {};

      if (Object.keys(nextValues).length === 0) {
        setErrorMessage(response.data?.message || '영수증에서 자동 입력할 항목을 찾지 못했습니다.');
        return;
      }

      setFieldValues((prev) => ({
        ...prev,
        ...nextValues,
      }));
      setErrorMessage('');
    } catch (error) {
      console.error('영수증 OCR 인식 실패:', error);
      setErrorMessage(
        error.response?.data?.message
        || error.response?.data?.error
        || '영수증 OCR 인식 중 오류가 발생했습니다.'
      );
    } finally {
      setOcrLoading(false);
    }
  };

  // 새 첨부파일 선택 시 파일 형식을 검증하고 필요하면 OCR 자동 입력을 실행합니다.
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

    // [전자결재-OCR 연동용]: template.ocr.autoFillOnUpload=true이면 파일 선택 직후 첫 번째 영수증을 자동 인식합니다.
    if (template.ocr?.autoFillOnUpload === true && availableFiles.length > 0) {
      runReceiptOcr(availableFiles[0]);
    }
  };

  // 아직 서버에 저장하지 않은 신규 첨부파일을 작성 화면에서 제거합니다.
  const removeFile = (index) => {
    setFiles((prev) => prev.filter((_, fileIndex) => fileIndex !== index));
    setFileInputKey((prev) => prev + 1);
  };

  // 임시저장 문서에 이미 저장되어 있던 첨부파일을 서버와 화면에서 함께 삭제합니다.
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

  // 현재 입력된 필드 값을 백엔드 Approval.content에 저장할 JSON 문자열로 변환합니다.
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

  // 임시저장/상신 API가 공통으로 사용하는 요청 본문을 생성합니다.
  const buildRequestPayload = (approvalLines = []) => ({
    formId: selectedForm.formId,
    title: documentTitle,
    content: buildContent(),
    approvalLines,
  });

  // 저장 또는 다음 단계 이동 전에 서식 정보와 첨부파일 필수 조건을 검증합니다.
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

    const rangeErrorMessage = validateDateTimeRanges(template.fields, fieldValues);
    if (rangeErrorMessage) {
      setErrorMessage(rangeErrorMessage);
      return false;
    }

    setErrorMessage('');
    return true;
  };

  // 첨부파일 유무에 따라 JSON 요청 또는 multipart 요청으로 백엔드 API를 호출합니다.
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

  // 작성 중인 문서를 DRAFT 상태로 저장하고 임시저장함으로 이동합니다.
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

  // 문서 내용을 검증한 뒤 결재선 설정 화면으로 작성 상태를 전달합니다.
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

  // 서식 필드 타입(text, select, amount 등)에 맞는 입력 컴포넌트를 렌더링합니다.
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

  // 새로 선택한 첨부파일 목록을 카드 형태로 보여주고 개별 삭제 버튼을 제공합니다.
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

  // 임시저장 문서에 이미 등록되어 있던 첨부파일 목록을 보여주고 삭제할 수 있게 합니다.
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
          <h2 className="user-page-title mb-1">{documentTitle || '결재 문서 작성'}</h2>
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
              {isReceiptOcrForm && (
                <div className="mt-2">
                  <CButton
                    color="info"
                    variant="outline"
                    size="sm"
                    onClick={() => runReceiptOcr()}
                    disabled={ocrLoading || files.length === 0}
                  >
                    {ocrLoading ? '영수증 OCR 인식 중...' : '영수증 OCR 자동입력'}
                  </CButton>
                  <div className="form-text">
                    OCR 결과는 결제일, 정산 금액, 구매 내역 요약 항목에 자동 반영됩니다.
                  </div>
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
