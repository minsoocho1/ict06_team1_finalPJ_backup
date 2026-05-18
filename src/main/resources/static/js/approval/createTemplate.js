/**
 * @FileName : createTemplate.js
 * @Description : 관리자 전자결재 서식 생성/수정 화면에서 동적 입력 필드를 관리합니다.
 *                저장되는 template JSON은 직원용 React 화면과 승인 후 후처리 로직에서 함께 사용됩니다.
 * @Author : 송영은
 * @Date : 2026. 04. 29
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -----------------------------------------------
 * @ 2026.04.29    송영은       최초 생성
 * @ 2026.05.04    송영은       서식 수정 오류 해결
 * @ 2026.05.14    송영은       셀렉트 박스 필드 및 옵션 입력 기능 추가
 */

let fields = [];

/**
 * 관리자가 입력한 항목명/설명/옵션이 HTML로 렌더링될 때 태그로 해석되지 않도록 이스케이프합니다.
 */
function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

/**
 * 금액 필드는 화면에서는 원화 표기, JSON에는 숫자만 저장하기 위해 포맷을 분리합니다.
 */
function formatKrwAmount(value) {
    const digits = String(value ?? '').replace(/[^\d]/g, '');
    if (!digits) return '';
    return Number(digits).toLocaleString('ko-KR') + ' 원';
}

function unformatKrwAmount(value) {
    return String(value ?? '').replace(/[^\d]/g, '');
}

/**
 * 셀렉트 박스 옵션은 관리자가 줄바꿈 또는 쉼표로 입력할 수 있게 합니다.
 */
function parseSelectOptions(value) {
    return String(value ?? '')
        .split(/\r?\n|,/)
        .map(option => option.trim())
        .filter(option => option.length > 0);
}

function getDefaultOptions(type, options) {
    if (Array.isArray(options) && options.length > 0) {
        return options;
    }

    return type === 'select' ? ['옵션1', '옵션2'] : [];
}

/**
 * 필드 타입에 따라 오른쪽 입력 영역에 표시할 미리보기 입력 요소를 생성합니다.
 */
function renderInputHtml(type, placeholder, options) {
    const now = new Date().toTimeString().slice(0, 5);
    const today = new Date().toISOString().slice(0, 10);

    if (type === 'text') {
        return `<input type="text" class="form-control field-placeholder" placeholder="${escapeHtml(placeholder)}">`;
    }
    if (type === 'number') {
        return `<input type="number" class="form-control field-placeholder" placeholder="${escapeHtml(placeholder || '0')}">`;
    }
    if (type === 'date') {
        return `<input type="date" class="form-control field-placeholder" value="${today}">`;
    }
    if (type === 'time') {
        return `<input type="time" class="form-control field-placeholder" value="${now}">`;
    }
    if (type === 'amount') {
        return `<input type="text" class="form-control field-placeholder amount-input" inputmode="numeric" placeholder="0 원" value="${formatKrwAmount(placeholder)}">`;
    }
    if (type === 'select') {
        const safeOptions = getDefaultOptions(type, options);
        const optionHtml = safeOptions
            .map(option => `<option>${escapeHtml(option)}</option>`)
            .join('');

        return `
            <select class="form-select mb-2 field-select-preview">
                ${optionHtml}
            </select>
            <textarea class="form-control form-control-sm field-options"
                rows="3"
                placeholder="옵션을 줄바꿈 또는 쉼표로 입력하세요.">${escapeHtml(safeOptions.join('\n'))}</textarea>
            <div class="form-text">예: 정상근무, 재택근무, 외근</div>
        `;
    }

    return '';
}

/**
 * 새 필드를 화면에 추가합니다.
 * savedId는 기존 서식 수정 시 DB에 저장된 필드 id를 유지하기 위한 값입니다.
 */
function addField(type, label = '', placeholder = '', description = '', options = [], savedId = null) {
    const container = document.getElementById('dynamicFields');
    const fieldId = savedId || 'field_' + Date.now() + '_' + Math.floor(Math.random() * 1000);
    const fieldNameId = fieldId + '_name';
    const safeOptions = getDefaultOptions(type, options);

    const fieldHtml = `
        <div class="row align-items-start py-3 border-bottom field-item"
            id="${fieldId}"
            data-id="${fieldId}"
            data-type="${type}">
            <div class="col-md-4">
                <input type="text"
                    class="form-control border-0 shadow-none field-label"
                    placeholder="항목명을 입력하세요"
                    id="${fieldNameId}"
                    required
                    value="${escapeHtml(label)}">
                <input type="text"
                    class="form-control form-control-sm border-0 shadow-none text-muted field-description"
                    placeholder="입력 안내 문구"
                    value="${escapeHtml(description)}">
            </div>
            <div class="col-md-7">
                ${renderInputHtml(type, placeholder, safeOptions)}
            </div>
            <div class="col-md-1 text-end">
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="removeField('${fieldId}')">X</button>
            </div>
        </div>
    `;

    container.insertAdjacentHTML('beforeend', fieldHtml);

    fields.push({
        id: fieldId,
        nameId: fieldNameId,
        type: type,
        placeholder: placeholder,
        description: description,
        options: safeOptions,
        required: false
    });
}

function removeField(fieldId) {
    document.getElementById(fieldId)?.remove();
    fields = fields.filter(field => field.id !== fieldId);
}

/**
 * 화면에 배치된 필드 DOM을 읽어 DB에 저장할 template JSON 구조로 변환합니다.
 */
function collectTemplateData(titleSelector = '#formTitle', fileRequiredSelector = '#isFileRequired') {
    const formData = {
        title: $(titleSelector).val(),
        fields: [],
        fileRequired: $(fileRequiredSelector).val() === 'yes'
    };

    $('#dynamicFields .field-item').each(function () {
        const $field = $(this);
        const type = $field.data('type');
        const input = $field.find('.field-placeholder');
        const options = type === 'select'
            ? parseSelectOptions($field.find('.field-options').val())
            : [];

        formData.fields.push({
            id: $field.data('id'),
            type: type,
            label: $field.find('.field-label').val(),
            placeholder: type === 'amount'
                ? unformatKrwAmount(input.val())
                : input.attr('placeholder') || '',
            description: $field.find('.field-description').val(),
            options: options
        });
    });

    return formData;
}

/**
 * 수정 화면에서 기존 template JSON을 다시 동적 필드 UI로 복원합니다.
 */
function loadTemplate(template) {
    $('#formTitle').val(template.title);
    fields = [];
    $('#dynamicFields').empty();

    (template.fields || []).forEach(field => {
        addField(
            field.type,
            field.label,
            field.placeholder,
            field.description,
            field.options,
            field.id
        );
    });

    $('#isFileRequired').val(template.fileRequired ? 'yes' : 'no');
}

$(document).on('input', '.amount-input', function () {
    this.value = formatKrwAmount(this.value);
});

$(document).on('input', '.field-options', function () {
    const $field = $(this).closest('.field-item');
    const options = parseSelectOptions($(this).val());
    const optionHtml = options
        .map(option => `<option>${escapeHtml(option)}</option>`)
        .join('');

    $field.find('.field-select-preview').html(optionHtml);
});

$('#formEditor').on('submit', function (e) {
    e.preventDefault();

    const formData = collectTemplateData();

    $.ajax({
        url: '/admin/approval/createAppForm/action',
        type: 'POST',
        contentType: 'application/json; charset=UTF-8',
        data: JSON.stringify({
            formName: formData.title,
            template: JSON.stringify(formData)
        }),
        success: function () {
            alert('저장되었습니다.');
            window.location.href = '/admin/approval/appFormList';
        },
        error: function (err) {
            console.error('서식 저장 실패:', err);
            alert('오류가 발생했습니다.');
        }
    });
});
