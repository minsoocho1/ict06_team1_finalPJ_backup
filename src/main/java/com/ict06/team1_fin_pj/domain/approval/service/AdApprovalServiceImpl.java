package com.ict06.team1_fin_pj.domain.approval.service;

import com.ict06.team1_fin_pj.common.dto.approval.AppFormDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppFormListDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppFormTargetDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormDetailDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormListDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormRequestDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormStepDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormTargetDto;
import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import com.ict06.team1_fin_pj.domain.approval.entity.AppFormEntity;
import com.ict06.team1_fin_pj.domain.approval.entity.AppLineTemplateDetailEntity;
import com.ict06.team1_fin_pj.domain.approval.entity.AppLineTemplateEntity;
import com.ict06.team1_fin_pj.domain.approval.entity.ApproverType;
import com.ict06.team1_fin_pj.domain.approval.repository.AppFormRepository;
import com.ict06.team1_fin_pj.domain.approval.repository.AppLineTemplateRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.DepartmentEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.PositionEntity;
import com.ict06.team1_fin_pj.domain.employee.repository.AdDepartmentRepository;
import com.ict06.team1_fin_pj.domain.employee.repository.AdEmployeeRepository;
import com.ict06.team1_fin_pj.domain.employee.repository.AdPositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 관리자용 전자결재 설정 서비스입니다.
 * 결재 서식(APP_FORM)과 결재선 서식(APP_LINE_TEMPLATE)의 등록, 조회, 수정, 삭제를 담당합니다.
 */
@Service
public class AdApprovalServiceImpl implements AdApprovalService {

    @Autowired
    private AppFormRepository appFormRepository;

    @Autowired
    private AppLineTemplateRepository appLineTemplateRepository;

    @Autowired
    private AdEmployeeRepository adEmployeeRepository;

    @Autowired
    private AdDepartmentRepository adDepartmentRepository;

    @Autowired
    private AdPositionRepository adPositionRepository;

    // [결재 서식 관리] ---------------------------------------------------------

    @Override
    public void saveAppForm(AppFormEntity entity) {
        appFormRepository.save(entity);
    }

    @Override
    public List<AppFormEntity> listAllAppForms() {
        return appFormRepository.findAll(Sort.by("formId"));
    }

    /**
     * 관리자 결재 서식 목록을 검색어와 페이지 조건에 맞춰 조회합니다.
     * keyword는 DB 컬럼 전체 검색이 아니라 관리자 화면용 간단 검색이므로 메모리 필터링 후 Page로 변환합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AppFormListDto> getAppFormsWithPaging(
            int page,
            int size,
            String keyword
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("formId").descending() // 번호 순 정렬이 더 직관적인 것 같아 수정
        );

        List<AppFormListDto> filteredForms =
                appFormRepository.findAll(Sort.by("formId").descending()) // 번호 순 정렬이 더 직관적인 것 같아 수정
                        .stream()
                        .filter(form -> matchesAppFormKeyword(form, keyword))
                        .map(this::toAppFormListDto)
                        .toList();

        return toPage(filteredForms, pageable);
    }

    /**
     * APP_FORM이 결재선 서식 FK를 가지는 새 구조에 맞춰 목록 DTO를 만듭니다.
     */
    private AppFormListDto toAppFormListDto(AppFormEntity form) {
        AppLineTemplateEntity lineTemplate = form.getLineTemplate();

        return AppFormListDto.builder()
                .formId(form.getFormId())
                .formName(form.getFormName())
                .isDefault(form.getIsDefault())
                .updatedAt(form.getUpdatedAt())
                .lineTemplateId(
                        lineTemplate != null
                                ? lineTemplate.getTemplateId()
                                : null
                )
                .lineTemplateName(
                        lineTemplate != null
                                ? lineTemplate.getTemplateName()
                                : null
                )
                .build();
    }

    @Override
    public AppFormEntity selectAppForm(int id) {
        return appFormRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 결재 서식입니다. ID: " + id));
    }

    @Override
    @Transactional
    public void deleteAppForm(int id) {
        AppFormEntity form = appFormRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("결재 서식 없음"));

        validateEditableAppForm(form);
        appFormRepository.delete(form);
    }

    @Override
    @Transactional
    public void deleteAppForms(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<AppFormEntity> forms = appFormRepository.findAllById(ids);
        forms.forEach(this::validateEditableAppForm);
        appFormRepository.deleteAll(forms);
    }

    /**
     * JPA 변경 감지를 사용해 결재 서식의 기본 정보만 수정합니다.
     * 결재선 서식 연결은 applyLineTemplate()에서 별도로 관리합니다.
     */
    @Transactional
    @Override
    public void updateAppForm(int formId, AppFormDto dto) {
        AppFormEntity prevEntity = appFormRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("결재 서식 없음"));

        validateEditableAppForm(prevEntity);
        prevEntity.updateForm(dto.getFormName(), dto.getTemplate());
    }

    /**
     * 기본 결재 서식은 샘플 데이터나 시스템 기준값으로 사용할 수 있어 수정/삭제를 막습니다.
     */
    private void validateEditableAppForm(AppFormEntity form) {
        if (form.isDefaultForm()) {
            throw new IllegalStateException("기본 결재 서식은 수정하거나 삭제할 수 없습니다.");
        }
    }

    // [결재선 서식 관리] -------------------------------------------------------

    @Transactional
    @Override
    public void saveAppLineForm(AppLineFormRequestDto dto, PrincipalDetails principal) {
        EmpEntity loginEmp = principal.getEmpEntity();

        AppLineTemplateEntity template =
                AppLineTemplateEntity.builder()
                        .templateName(dto.getTemplateName())
                        .isDefault(
                                dto.getIsDefault() != null
                                        ? dto.getIsDefault()
                                        : false
                        )
                        .createdBy(loginEmp)
                        .build();

        appLineTemplateRepository.save(template);

        // 참조 대상은 결재 순서가 아니므로 stepOrder를 0으로 저장합니다.
        if (dto.getRefTargets() != null) {
            dto.getRefTargets().forEach(t -> {
                AppLineTemplateDetailEntity detail =
                        createDetailEntity(template, 0, t);

                template.addDetail(detail);
            });
        }

        // 실제 승인자는 화면에서 전달한 step 값에 따라 1단계, 2단계처럼 순서를 가집니다.
        dto.getApprovalSteps().forEach(stepDto -> {
            int step = stepDto.getStep();

            stepDto.getTargets().forEach(t -> {
                AppLineTemplateDetailEntity detail =
                        createDetailEntity(template, step, t);

                template.addDetail(detail);
            });
        });
    }

    /**
     * 화면에서 넘어온 대상 타입(USER/DEPT/POSITION)에 따라 결재선 상세 엔티티를 생성합니다.
     */
    private AppLineTemplateDetailEntity createDetailEntity(
            AppLineTemplateEntity template,
            int step,
            AppFormTargetDto t
    ) {
        ApproverType type = ApproverType.valueOf(t.getType());

        AppLineTemplateDetailEntity.AppLineTemplateDetailEntityBuilder builder =
                AppLineTemplateDetailEntity.builder()
                        .template(template)
                        .stepOrder(step)
                        .approverType(type);

        switch (type) {
            case USER -> {
                EmpEntity emp = adEmployeeRepository.findById(t.getId())
                        .orElseThrow(() -> new IllegalArgumentException("사원 없음"));
                builder.approver(emp);
            }
            case DEPT -> {
                DepartmentEntity dept = adDepartmentRepository.findById(Integer.valueOf(t.getId()))
                        .orElseThrow(() -> new IllegalArgumentException("부서 없음"));
                builder.department(dept);
            }
            case POSITION -> {
                PositionEntity pos = adPositionRepository.findById(Integer.valueOf(t.getId()))
                        .orElseThrow(() -> new IllegalArgumentException("직급 없음"));
                builder.minPosition(pos);
            }
        }

        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppLineFormListDto> listAppLineForm(Pageable pageable) {
        return appLineTemplateRepository.findAll(pageable)
                .map(t -> AppLineFormListDto.builder()
                        .templateId(t.getTemplateId())
                        .templateName(t.getTemplateName())
                        .formName(getConnectedFormNames(t.getTemplateId()))
                        .createdBy(
                                t.getCreatedBy() != null
                                        ? t.getCreatedBy().getName()
                                        + "("
                                        + t.getCreatedBy().getEmpNo()
                                        + ")"
                                        : ""
                        )
                        .isDefault(t.getIsDefault())
                        .createdAt(t.getCreatedAt())
                        .build()
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppLineFormListDto> listAllAppLineTemplates() {
        return appLineTemplateRepository.findAll()
                .stream()
                .map(entity -> AppLineFormListDto.builder()
                        .templateId(entity.getTemplateId())
                        .templateName(entity.getTemplateName())
                        .isDefault(entity.getIsDefault())
                        .build()
                )
                .toList();
    }

    /**
     * 결재 서식과 결재선 서식을 연결합니다.
     * 새 DB 구조에서는 APP_FORM.line_template_id가 FK이므로 form 쪽 연관관계를 변경합니다.
     */
    @Transactional
    @Override
    public void applyLineTemplate(Integer formId, Integer templateId) {
        AppFormEntity form =
                appFormRepository.findById(formId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("결재 서식 없음"));

        // templateId가 null이면 결재선 서식 연결을 해제합니다.
        // APP_FORM.line_template_id가 nullable FK이므로 JPA 변경 감지로 null 업데이트됩니다.
        if (templateId == null) {
            form.updateLineTemplate(null);
            return;
        }

        AppLineTemplateEntity template =
                appLineTemplateRepository.findById(templateId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("결재선 서식 없음"));

        form.updateLineTemplate(template);
    }

    /**
     * 여러 결재 서식에 같은 결재선 서식을 한 번에 적용합니다.
     * templateId가 null이면 선택된 결재 서식들의 결재선 연결을 일괄 해제합니다.
     */
    @Transactional
    @Override
    public void applyLineTemplateToForms(List<Integer> formIds, Integer templateId) {
        if (formIds == null || formIds.isEmpty()) {
            throw new IllegalArgumentException("결재선 서식을 적용할 결재 서식을 선택해주세요.");
        }

        List<AppFormEntity> forms = appFormRepository.findAllById(formIds);

        if (forms.size() != formIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 결재 서식이 포함되어 있습니다.");
        }

        AppLineTemplateEntity template = null;

        if (templateId != null) {
            template = appLineTemplateRepository.findById(templateId)
                    .orElseThrow(() ->
                            new IllegalArgumentException("결재선 서식 없음"));
        }

        for (AppFormEntity form : forms) {
            form.updateLineTemplate(template);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppLineFormListDto> getAppLineFormsWithPaging(
            int page,
            int size,
            String keyword
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "templateId")
        );

        List<AppLineFormListDto> filteredTemplates =
                appLineTemplateRepository.findAll(Sort.by(Sort.Direction.DESC, "templateId"))
                        .stream()
                        .filter(template -> matchesAppLineTemplateKeyword(template, keyword))
                        .map(this::toAppLineFormListDto)
                        .toList();

        return toPage(filteredTemplates, pageable);
    }

    private AppLineFormListDto toAppLineFormListDto(AppLineTemplateEntity entity) {
        return AppLineFormListDto.builder()
                .templateId(entity.getTemplateId())
                .templateName(entity.getTemplateName())
                .formName(getConnectedFormNames(entity.getTemplateId()))
                .isDefault(entity.getIsDefault())
                .createdAt(entity.getCreatedAt())
                .createdBy(
                        entity.getCreatedBy() != null
                                ? entity.getCreatedBy().getName()
                                + "("
                                + entity.getCreatedBy().getEmpNo()
                                + ")"
                                : "-"
                )
                .build();
    }

    private boolean matchesAppFormKeyword(AppFormEntity form, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);

        if (normalizedKeyword.isBlank()) {
            return true;
        }

        return String.valueOf(form.getFormId()).contains(normalizedKeyword)
                || containsIgnoreCase(form.getFormName(), normalizedKeyword);
    }

    private boolean matchesAppLineTemplateKeyword(
            AppLineTemplateEntity template,
            String keyword
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);

        if (normalizedKeyword.isBlank()) {
            return true;
        }

        return String.valueOf(template.getTemplateId()).contains(normalizedKeyword)
                || containsIgnoreCase(template.getTemplateName(), normalizedKeyword);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private boolean containsIgnoreCase(String target, String keyword) {
        return target != null
                && target.toLowerCase().contains(keyword.toLowerCase());
    }

    private <T> Page<T> toPage(List<T> items, Pageable pageable) {
        int start = Math.min((int) pageable.getOffset(), items.size());
        int end = Math.min(start + pageable.getPageSize(), items.size());

        return new PageImpl<>(
                items.subList(start, end),
                pageable,
                items.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AppLineFormDetailDto selectAppLineForm(Integer id) {
        AppLineTemplateEntity template =
                appLineTemplateRepository.findDetailById(id)
                        .orElseThrow(() -> new IllegalArgumentException("결재선 서식 없음"));

        Map<Integer, List<AppLineTemplateDetailEntity>> grouped =
                template.getDetails().stream()
                        .collect(Collectors.groupingBy(
                                AppLineTemplateDetailEntity::getStepOrder
                        ));

        List<AppLineFormStepDto> steps = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<AppLineFormTargetDto> targets =
                            entry.getValue().stream()
                                    .map(this::convertTargetDto)
                                    .toList();

                    return AppLineFormStepDto.builder()
                            .stepOrder(entry.getKey())
                            .targets(targets)
                            .build();
                })
                .toList();

        return AppLineFormDetailDto.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName())
                .formName(getConnectedFormNames(template.getTemplateId()))
                .isDefault(template.getIsDefault())
                .steps(steps)
                .build();
    }

    /**
     * 결재선 상세 엔티티를 관리자 화면에서 표시하기 쉬운 대상 DTO로 변환합니다.
     */
    private AppLineFormTargetDto convertTargetDto(
            AppLineTemplateDetailEntity detail
    ) {
        String id = "";
        String name = "-";
        String dept = "";
        String position = "";
        Integer positionId = 0;

        switch (detail.getApproverType()) {
            case USER -> {
                if (detail.getApprover() != null) {
                    EmpEntity emp = detail.getApprover();

                    id = emp.getEmpNo();
                    name = emp.getName();

                    if (emp.getDepartment() != null) {
                        dept = emp.getDepartment().getDeptName();
                    }

                    if (emp.getPosition() != null) {
                        position = emp.getPosition().getPositionName();
                        positionId = emp.getPosition().getPositionId();
                    }
                }
            }
            case DEPT -> {
                if (detail.getDepartment() != null) {
                    id = String.valueOf(detail.getDepartment().getDeptId());
                    name = detail.getDepartment().getDeptName();
                }
            }
            case POSITION -> {
                if (detail.getMinPosition() != null) {
                    id = String.valueOf(detail.getMinPosition().getPositionId());
                    name = detail.getMinPosition().getPositionName();
                    positionId = detail.getMinPosition().getPositionId();
                }
            }
        }

        return AppLineFormTargetDto.builder()
                .id(id)
                .name(name)
                .dept(dept)
                .position(position)
                .positionId(positionId)
                .type(detail.getApproverType().name())
                .build();
    }

    /**
     * 특정 결재선 서식을 사용 중인 결재 서식명을 쉼표로 묶어 표시합니다.
     * 하나의 결재선 서식을 여러 결재 서식이 공유할 수 있으므로 목록 조회가 필요합니다.
     */
    private String getConnectedFormNames(Integer templateId) {
        List<AppFormEntity> forms =
                appFormRepository.findByLineTemplate_TemplateId(templateId);

        if (forms.isEmpty()) {
            return "-";
        }

        return forms.stream()
                .map(AppFormEntity::getFormName)
                .collect(Collectors.joining(", "));
    }

    @Override
    @Transactional
    public void deleteAppLineTemplate(int id) {
        AppLineTemplateEntity template =
                appLineTemplateRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("결재선 서식 없음"));

        if (!appFormRepository.findByLineTemplate_TemplateId(id).isEmpty()) {
            throw new IllegalStateException("결재 서식에서 사용 중인 결재선 서식은 삭제할 수 없습니다.");
        }

        appLineTemplateRepository.delete(template);
    }

    @Transactional
    @Override
    public void updateAppLineForm(
            Integer templateId,
            AppLineFormRequestDto dto,
            PrincipalDetails principal
    ) {
        AppLineTemplateEntity template =
                appLineTemplateRepository.findDetailById(templateId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("결재선 서식 없음"));

        template.updateNameIsDefault(dto.getTemplateName(), dto.getIsDefault());
        template.getDetails().clear();

        if (dto.getRefTargets() != null) {
            dto.getRefTargets().forEach(t -> {
                AppLineTemplateDetailEntity detail =
                        createDetailEntity(
                                template,
                                0,
                                t
                        );

                template.addDetail(detail);
            });
        }

        dto.getApprovalSteps().forEach(stepDto -> {
            int step = stepDto.getStep();

            stepDto.getTargets().forEach(t -> {
                AppLineTemplateDetailEntity detail =
                        createDetailEntity(
                                template,
                                step,
                                t
                        );

                template.addDetail(detail);
            });
        });
    }
}
