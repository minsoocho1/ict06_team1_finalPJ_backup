package com.ict06.team1_fin_pj.domain.approval.service;

import com.ict06.team1_fin_pj.common.dto.approval.ApprovalCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalCreateResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalDetailResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalFileResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalFormResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalLineRequestDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalLineResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalListResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalEmployeeSignResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormDetailDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormStepDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormTargetDto;
import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import com.ict06.team1_fin_pj.domain.approval.entity.AppFileEntity;
import com.ict06.team1_fin_pj.domain.approval.entity.AppFormEntity;
import com.ict06.team1_fin_pj.domain.approval.entity.AppLineEntity;
import com.ict06.team1_fin_pj.domain.approval.entity.AppLineTemplateDetailEntity;
import com.ict06.team1_fin_pj.domain.approval.entity.AppLineTemplateEntity;
import com.ict06.team1_fin_pj.domain.approval.entity.ApprovalEntity;
import com.ict06.team1_fin_pj.domain.approval.entity.ApprovalLineStatus;
import com.ict06.team1_fin_pj.domain.approval.entity.ApprovalStatus;
import com.ict06.team1_fin_pj.domain.approval.repository.AppFileRepository;
import com.ict06.team1_fin_pj.domain.approval.repository.AppFormRepository;
import com.ict06.team1_fin_pj.domain.approval.repository.AppLineTemplateRepository;
import com.ict06.team1_fin_pj.domain.approval.repository.ApprovalRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 일반 직원용 전자결재 서비스 구현체입니다.
 *
 * 관리자용 결재 양식/결재선 템플릿 관리는 AdApprovalService가 담당하고,
 * 이 클래스는 React 사용자 화면에서 호출하는 결재 문서 작성, 임시저장, 상신, 목록/상세 조회를 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    /*
     * 결재 첨부파일 저장 위치입니다.
     * 인사관리(employee) 업로드 영역과 섞이지 않도록 프로젝트 루트의 approval 전용 폴더를 사용합니다.
     */
    private static final String UPLOAD_BASE_DIR =
            System.getProperty("user.dir") + "/ict_06_uploads/approval";

    private final ApprovalRepository approvalRepository;
    private final AppFileRepository appFileRepository;
    private final AppFormRepository appFormRepository;
    private final AppLineTemplateRepository appLineTemplateRepository;
    private final EmployeeRepository employeeRepository;
    private final ApprovalAttendanceSyncService approvalAttendanceSyncService;

    /**
     * 직원이 새 결재 문서를 작성할 때 선택할 수 있는 결재 서식 목록을 조회합니다.
     *
     * 관리자 화면에서 관리하는 APP_FORM 전체를 대상으로 하며,
     * React 화면은 응답의 template JSON을 파싱해 text/date/time/select 등의 입력 필드를 렌더링합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ApprovalFormResponseDto> getAvailableForms(PrincipalDetails principal) {
        validatePrincipal(principal);

        return appFormRepository.findAll(Sort.by("formName").ascending().and(Sort.by("formId").ascending()))
                .stream()
                .map(this::toFormResponse)
                .toList();
    }

    /**
     * 특정 결재 서식의 상세 정보를 조회합니다.
     *
     * 목록 응답 이후에도 관리자가 서식을 수정했을 수 있으므로,
     * 실제 작성 화면 진입 시에는 이 API로 최신 template을 한 번 더 확인할 수 있습니다.
     */
    @Override
    @Transactional(readOnly = true)
    public ApprovalFormResponseDto getFormDetail(Integer formId, PrincipalDetails principal) {
        validatePrincipal(principal);

        if (formId == null) {
            throw new IllegalArgumentException("결재 서식 ID가 필요합니다.");
        }

        AppFormEntity form = appFormRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결재 서식입니다."));

        return toFormResponse(form);
    }

    /**
     * 결재 서식에 연결된 기본 결재선 서식의 상세 정보를 조회합니다.
     *
     * 관리자 페이지와 같은 DTO 구조를 사용하지만, 직원 화면에서는 읽기 전용 미리보기와 USER 타입 결재자 자동 채우기에만 사용합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public AppLineFormDetailDto getLineTemplateDetail(Integer templateId, PrincipalDetails principal) {
        validatePrincipal(principal);

        if (templateId == null) {
            throw new IllegalArgumentException("결재선 서식 ID가 필요합니다.");
        }

        AppLineTemplateEntity template = appLineTemplateRepository.findDetailById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결재선 서식입니다."));

        Map<Integer, List<AppLineTemplateDetailEntity>> groupedDetails = template.getDetails()
                .stream()
                .collect(Collectors.groupingBy(AppLineTemplateDetailEntity::getStepOrder));

        List<AppLineFormStepDto> steps = groupedDetails.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> AppLineFormStepDto.builder()
                        .stepOrder(entry.getKey())
                        .targets(entry.getValue().stream()
                                .map(this::toLineTemplateTargetResponse)
                                .toList())
                        .build())
                .toList();

        return AppLineFormDetailDto.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName())
                .isDefault(template.getIsDefault())
                .steps(steps)
                .build();
    }

    /**
     * 결재자로 선택한 사원의 인감 이미지 경로를 조회합니다.
     *
     * APP_LINE에는 결재자 사번만 저장하지만, 작성 화면의 인감 미리보기와 향후 PDF 출력에서는
     * EMPLOYEE.sign_img 경로가 필요하므로 전자결재 전용 읽기 API로 제공합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public ApprovalEmployeeSignResponseDto getEmployeeSign(String empNo, PrincipalDetails principal) {
        validatePrincipal(principal);

        if (empNo == null || empNo.isBlank()) {
            throw new IllegalArgumentException("사원 번호가 필요합니다.");
        }

        EmpEntity employee = employeeRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new IllegalArgumentException("사원 정보를 찾을 수 없습니다."));

        return ApprovalEmployeeSignResponseDto.builder()
                .empNo(employee.getEmpNo())
                .name(employee.getName())
                .signImg(employee.getSignImg())
                .build();
    }

    /**
     * 새 결재 문서를 임시저장합니다.
     *
     * 임시저장은 아직 결재 진행이 시작되지 않은 상태이므로 currentStep은 0,
     * currentApprover는 null로 저장합니다.
     */
    @Override
    @Transactional
    public ApprovalCreateResponseDto saveDraft(
            ApprovalCreateRequestDto requestDto,
            PrincipalDetails principal,
            List<MultipartFile> files
    ) {
        ApprovalEntity approval = createApproval(requestDto, principal);
        approval.saveAsDraft();
        addFiles(approval, files);

        ApprovalEntity saved = approvalRepository.save(approval);
        return toCreateResponse(saved);
    }

    /**
     * 기존 임시저장 문서를 수정합니다.
     *
     * 작성자 본인의 DRAFT 문서만 수정할 수 있으며,
     * 제목/본문/양식/결재선은 요청 내용으로 교체하고 첨부파일은 새 파일만 추가합니다.
     */
    @Override
    @Transactional
    public ApprovalCreateResponseDto updateDraft(
            Integer approvalId,
            ApprovalCreateRequestDto requestDto,
            PrincipalDetails principal,
            List<MultipartFile> files
    ) {
        ApprovalEntity approval = getEditableDraft(approvalId, principal);
        updateDraftFields(approval, requestDto);
        addFiles(approval, files);

        return toCreateResponse(approval);
    }

    /**
     * 새 결재 문서를 상신합니다.
     *
     * stepOrder=0은 참조자이므로 실제 결재 단계 계산에서는 제외하고,
     * stepOrder가 1 이상인 대상 중 가장 앞 순서의 사람을 현재 결재자로 지정합니다.
     */
    @Override
    @Transactional
    public ApprovalCreateResponseDto submit(
            ApprovalCreateRequestDto requestDto,
            PrincipalDetails principal,
            List<MultipartFile> files
    ) {
        validateSubmitRequest(requestDto);

        ApprovalEntity approval = createApproval(requestDto, principal);
        EmpEntity firstApprover = findFirstApprover(requestDto.getApprovalLines());

        int maxStep = requestDto.getApprovalLines().stream()
                .filter(line -> line.getStepOrder() != null && line.getStepOrder() > 0)
                .map(ApprovalLineRequestDto::getStepOrder)
                .max(Integer::compareTo)
                .orElse(1);

        approval.submit(firstApprover, maxStep);
        addFiles(approval, files);

        ApprovalEntity saved = approvalRepository.save(approval);
        return toCreateResponse(saved);
    }

    /**
     * 기존 임시저장 문서를 수정한 뒤 상신합니다.
     *
     * 화면에서는 임시저장 상세에서 내용을 고친 뒤 바로 "상신" 버튼을 누르는 흐름에 사용합니다.
     */
    @Override
    @Transactional
    public ApprovalCreateResponseDto submitDraft(
            Integer approvalId,
            ApprovalCreateRequestDto requestDto,
            PrincipalDetails principal,
            List<MultipartFile> files
    ) {
        validateSubmitRequest(requestDto);

        ApprovalEntity approval = getEditableDraft(approvalId, principal);
        updateDraftFields(approval, requestDto);
        addFiles(approval, files);

        EmpEntity firstApprover = findFirstApprover(requestDto.getApprovalLines());
        int maxStep = requestDto.getApprovalLines().stream()
                .filter(line -> line.getStepOrder() != null && line.getStepOrder() > 0)
                .map(ApprovalLineRequestDto::getStepOrder)
                .max(Integer::compareTo)
                .orElse(1);

        approval.submit(firstApprover, maxStep);

        return toCreateResponse(approval);
    }

    /**
     * 작성자 본인의 임시저장 문서를 삭제합니다.
     *
     * 임시저장 문서는 아직 결재 흐름이 시작되지 않은 개인 작업물이므로 이력으로 남기지 않고 삭제합니다.
     * 첨부파일이 있으면 DB 행 삭제 전에 실제 업로드 파일을 먼저 제거합니다.
     */
    @Override
    @Transactional
    public void deleteDraft(
            Integer approvalId,
            PrincipalDetails principal
    ) {
        ApprovalEntity approval = getEditableDraft(approvalId, principal);

        approval.getFiles().forEach(this::deletePhysicalFile);
        approvalRepository.delete(approval);
    }

    /**
     * 로그인 사용자가 작성한 결재 문서 목록을 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalListResponseDto> getMyDocuments(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            PrincipalDetails principal,
            Pageable pageable
    ) {
        validatePrincipal(principal);

        ApprovalStatus approvalStatus = parseStatus(status);
        return approvalRepository.findMyDocuments(
                principal.getEmpNo(),
                approvalStatus,
                startDate,
                endDate,
                pageable
        );
    }

    /**
     * 로그인 사용자의 임시저장 문서 목록을 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalListResponseDto> getMyDrafts(
            LocalDate startDate,
            LocalDate endDate,
            PrincipalDetails principal,
            Pageable pageable
    ) {
        validatePrincipal(principal);

        return approvalRepository.findMyDrafts(
                principal.getEmpNo(),
                startDate,
                endDate,
                pageable
        );
    }

    /**
     * 로그인 사용자가 참조자로 지정된 결재 문서 목록을 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalListResponseDto> getMyReferencedDocuments(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            PrincipalDetails principal,
            Pageable pageable
    ) {
        validatePrincipal(principal);

        ApprovalStatus approvalStatus = parseStatus(status);
        return approvalRepository.findMyReferencedDocuments(
                principal.getEmpNo(),
                approvalStatus,
                startDate,
                endDate,
                pageable
        );
    }

    /**
     * 현재 로그인 사용자가 지금 결재해야 하는 문서 목록을 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalListResponseDto> getPendingApprovals(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            PrincipalDetails principal,
            Pageable pageable
    ) {
        validatePrincipal(principal);

        ApprovalStatus approvalStatus = parseStatus(status);
        return approvalRepository.findPendingApprovals(
                principal.getEmpNo(),
                approvalStatus,
                startDate,
                endDate,
                pageable
        );
    }

    /**
     * 로그인 사용자가 과거에 결재 처리한 문서 목록을 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalListResponseDto> getProcessedApprovals(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            PrincipalDetails principal,
            Pageable pageable
    ) {
        validatePrincipal(principal);

        ApprovalStatus approvalStatus = parseStatus(status);
        return approvalRepository.findProcessedApprovals(
                principal.getEmpNo(),
                approvalStatus,
                startDate,
                endDate,
                pageable
        );
    }

    /**
     * 로그인 사용자가 결재선에는 포함되어 있지만 아직 차례가 오지 않은 문서 목록을 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalListResponseDto> getUpcomingApprovals(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            PrincipalDetails principal,
            Pageable pageable
    ) {
        validatePrincipal(principal);

        ApprovalStatus approvalStatus = parseStatus(status);
        return approvalRepository.findUpcomingApprovals(
                principal.getEmpNo(),
                approvalStatus,
                startDate,
                endDate,
                pageable
        );
    }

    /**
     * 현재 결재 대기 중인 문서를 승인 처리합니다.
     *
     * 현재 결재선은 APPROVED로 바꾸고, 다음 결재자가 있으면 currentApprover/currentStep을 이동합니다.
     * 더 이상 남은 결재자가 없으면 문서 상태를 COMPLETED로 종료합니다.
     */
    @Override
    @Transactional
    public ApprovalCreateResponseDto approveApproval(
            Integer approvalId,
            PrincipalDetails principal
    ) {
        ApprovalEntity approval = getProcessableApproval(approvalId, principal);
        AppLineEntity currentLine = findCurrentApprovalLine(approval, principal.getEmpNo());

        currentLine.approve();

        Optional<AppLineEntity> nextLine = findNextApprovalLine(approval);
        if (nextLine.isPresent()) {
            AppLineEntity next = nextLine.get();
            approval.moveToNextApprover(next.getApprover(), next.getStepOrder());
        } else {
            approval.complete();
            // [결재-근태 연동용]: 마지막 결재자가 승인해 문서가 완료된 시점에만 근태 데이터 반영을 실행합니다.
            approvalAttendanceSyncService.syncIfAttendanceForm(approval);
        }

        return toCreateResponse(approval);
    }

    /**
     * 현재 결재 대기 중인 문서를 반려 처리합니다.
     *
     * 현재 결재선은 REJECTED로 바꾸고, 문서 전체 상태도 REJECTED로 종료합니다.
     */
    @Override
    @Transactional
    public ApprovalCreateResponseDto rejectApproval(
            Integer approvalId,
            PrincipalDetails principal
    ) {
        ApprovalEntity approval = getProcessableApproval(approvalId, principal);
        AppLineEntity currentLine = findCurrentApprovalLine(approval, principal.getEmpNo());

        currentLine.reject();
        approval.reject();

        return toCreateResponse(approval);
    }

    /**
     * 작성자가 상신한 결재 문서를 취소 처리합니다.
     *
     * 결재가 모두 완료되기 전이라면 작성자가 직접 상신 취소할 수 있습니다.
     * 이미 처리된 결재선 이력은 그대로 남겨 두고, 문서 전체 상태만 CANCELED로 전환합니다.
     */
    @Override
    @Transactional
    public ApprovalCreateResponseDto cancelApproval(
            Integer approvalId,
            PrincipalDetails principal
    ) {
        ApprovalEntity approval = getCancelableApproval(approvalId, principal);
        approval.cancel();

        return toCreateResponse(approval);
    }

    /**
     * 결재 문서 상세 정보를 조회합니다.
     *
     * 작성자는 자신의 문서를 볼 수 있고, 결재자와 참조자는 상신 이후 문서만 볼 수 있습니다.
     */
    /**
     * 임시저장 문서의 첨부파일을 삭제합니다.
     *
     * 작성자 본인의 DRAFT 문서에 속한 파일만 삭제할 수 있습니다.
     * DB의 APP_FILE 행과 실제 업로드 디렉터리의 파일을 함께 삭제합니다.
     */
    @Override
    @Transactional
    public void deleteApprovalFile(
            Integer fileId,
            PrincipalDetails principal
    ) {
        AppFileEntity file = getDeletableFile(fileId, principal);
        deletePhysicalFile(file);
        appFileRepository.delete(file);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalDetailResponseDto getApprovalDetail(
            Integer approvalId,
            PrincipalDetails principal
    ) {
        validatePrincipal(principal);

        if (approvalId == null) {
            throw new IllegalArgumentException("결재 문서 ID가 필요합니다.");
        }

        ApprovalEntity approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결재 문서입니다."));

        validateDetailReadable(approval, principal.getEmpNo());

        return toDetailResponse(approval);
    }

    /**
     * 요청 DTO를 ApprovalEntity로 변환합니다.
     *
     * 작성자(writer)는 요청 body에서 받지 않고 JWT 인증 정보에서 가져옵니다.
     * 이렇게 해야 사용자가 다른 사원 번호를 임의로 보내 대리 상신하는 문제를 막을 수 있습니다.
     */
    private ApprovalEntity createApproval(
            ApprovalCreateRequestDto requestDto,
            PrincipalDetails principal
    ) {
        validateCommonRequest(requestDto, principal);

        AppFormEntity form = appFormRepository.findById(requestDto.getFormId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결재 양식입니다."));

        EmpEntity writer = employeeRepository.findByEmpNo(principal.getEmpNo())
                .orElseThrow(() -> new IllegalArgumentException("작성자 정보를 찾을 수 없습니다."));

        ApprovalEntity approval = ApprovalEntity.builder()
                .form(form)
                .title(requestDto.getTitle().trim())
                .content(requestDto.getContent())
                .writer(writer)
                .currentStep(0)
                .maxStep(calculateMaxStep(requestDto.getApprovalLines()))
                .status(ApprovalStatus.DRAFT)
                .isDeleted(false)
                .build();

        if (requestDto.getApprovalLines() != null) {
            requestDto.getApprovalLines().stream()
                    .map(this::createLine)
                    .sorted(Comparator.comparing(AppLineEntity::getStepOrder))
                    .forEach(approval::addLine);
        }

        return approval;
    }

    /**
     * 수정 가능한 임시저장 문서를 조회하고 권한을 검증합니다.
     */
    private ApprovalEntity getEditableDraft(Integer approvalId, PrincipalDetails principal) {
        validatePrincipal(principal);

        if (approvalId == null) {
            throw new IllegalArgumentException("결재 문서 ID가 필요합니다.");
        }

        ApprovalEntity approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결재 문서입니다."));

        validateDraftEditable(approval, principal.getEmpNo());

        return approval;
    }

    /**
     * 승인/반려 처리가 가능한 문서를 조회하고 현재 결재자 권한을 검증합니다.
     */
    private ApprovalEntity getProcessableApproval(Integer approvalId, PrincipalDetails principal) {
        validatePrincipal(principal);

        if (approvalId == null) {
            throw new IllegalArgumentException("결재 문서 ID가 필요합니다.");
        }

        ApprovalEntity approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결재 문서입니다."));

        if (approval.getStatus() != ApprovalStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("진행 중인 결재 문서만 승인 또는 반려할 수 있습니다.");
        }

        if (approval.getCurrentApprover() == null
                || !principal.getEmpNo().equals(approval.getCurrentApprover().getEmpNo())) {
            throw new IllegalArgumentException("현재 결재자만 승인 또는 반려할 수 있습니다.");
        }

        return approval;
    }

    /**
     * 상신 취소가 가능한 문서를 조회하고 작성자 권한과 문서 진행 상태를 검증합니다.
     *
     * COMPLETED/REJECTED/CANCELED처럼 결재 흐름이 이미 종료된 문서는 취소할 수 없고,
     * IN_PROGRESS 상태의 문서만 작성자가 취소할 수 있게 제한합니다.
     */
    private ApprovalEntity getCancelableApproval(Integer approvalId, PrincipalDetails principal) {
        validatePrincipal(principal);

        if (approvalId == null) {
            throw new IllegalArgumentException("결재 문서 ID가 필요합니다.");
        }

        ApprovalEntity approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결재 문서입니다."));

        boolean isWriter = approval.getWriter() != null
                && principal.getEmpNo().equals(approval.getWriter().getEmpNo());

        if (!isWriter) {
            throw new IllegalArgumentException("상신 취소는 작성자만 할 수 있습니다.");
        }

        if (approval.getStatus() != ApprovalStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("진행 중인 결재 문서만 상신 취소할 수 있습니다.");
        }

        return approval;
    }

    /**
     * 현재 로그인 사용자가 처리해야 하는 결재선 한 줄을 찾습니다.
     */
    /**
     * 삭제 가능한 첨부파일을 조회하고 작성자/문서 상태 권한을 검증합니다.
     */
    private AppFileEntity getDeletableFile(Integer fileId, PrincipalDetails principal) {
        validatePrincipal(principal);

        if (fileId == null) {
            throw new IllegalArgumentException("첨부파일 ID가 필요합니다.");
        }

        AppFileEntity file = appFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 첨부파일입니다."));

        ApprovalEntity approval = file.getApproval();
        if (approval == null) {
            throw new IllegalArgumentException("첨부파일이 연결된 결재 문서를 찾을 수 없습니다.");
        }

        boolean isWriter = approval.getWriter() != null
                && principal.getEmpNo().equals(approval.getWriter().getEmpNo());

        if (!isWriter) {
            throw new IllegalArgumentException("첨부파일은 문서 작성자만 삭제할 수 있습니다.");
        }

        if (approval.getStatus() != ApprovalStatus.DRAFT) {
            throw new IllegalArgumentException("임시저장 문서의 첨부파일만 삭제할 수 있습니다.");
        }

        return file;
    }

    /**
     * DB에 저장된 웹 경로를 실제 파일 경로로 변환해 업로드 파일을 삭제합니다.
     * 저장 폴더 밖의 경로가 만들어지지 않도록 normalize 후 base directory 하위 여부를 검증합니다.
     */
    private void deletePhysicalFile(AppFileEntity file) {
        String filePath = file.getFilePath();
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        String savedFileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        Path basePath = Paths.get(UPLOAD_BASE_DIR).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(savedFileName).normalize();

        if (!targetPath.startsWith(basePath)) {
            throw new IllegalArgumentException("올바르지 않은 첨부파일 경로입니다.");
        }

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("결재 첨부파일 삭제 중 오류가 발생했습니다.", e);
        }
    }

    private AppLineEntity findCurrentApprovalLine(ApprovalEntity approval, String empNo) {
        AppLineEntity currentLine = approval.getLines().stream()
                .filter(line -> line.getStepOrder() != null
                        && line.getStepOrder().equals(approval.getCurrentStep()))
                .filter(line -> line.getApprover() != null
                        && empNo.equals(line.getApprover().getEmpNo()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("현재 처리할 결재선을 찾을 수 없습니다."));

        if (currentLine.getStatus() != ApprovalLineStatus.WAITING) {
            throw new IllegalArgumentException("이미 처리된 결재선입니다.");
        }

        return currentLine;
    }

    /**
     * 현재 단계 또는 이후 단계의 다음 실제 결재자를 찾습니다.
     *
     * 같은 stepOrder에 결재자가 여러 명 있을 수 있으므로,
     * 현재 단계에 WAITING 상태인 결재선이 남아 있으면 그 결재자를 먼저 다음 결재자로 지정합니다.
     * stepOrder=0인 참조자는 결재 단계가 아니므로 제외합니다.
     */
    private Optional<AppLineEntity> findNextApprovalLine(ApprovalEntity approval) {
        return approval.getLines().stream()
                .filter(line -> line.getStepOrder() != null
                        && line.getStepOrder() >= approval.getCurrentStep())
                .filter(line -> line.getStepOrder() > 0)
                .filter(line -> line.getStatus() == ApprovalLineStatus.WAITING)
                .min(Comparator.comparing(AppLineEntity::getStepOrder));
    }

    /**
     * 임시저장 문서의 기본 정보와 결재선을 요청 내용으로 교체합니다.
     */
    private void updateDraftFields(
            ApprovalEntity approval,
            ApprovalCreateRequestDto requestDto
    ) {
        validateDraftRequestFields(requestDto);

        AppFormEntity form = appFormRepository.findById(requestDto.getFormId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결재 양식입니다."));

        approval.updateDraftContent(
                form,
                requestDto.getTitle().trim(),
                requestDto.getContent()
        );

        if (requestDto.getApprovalLines() != null) {
            List<AppLineEntity> newLines = requestDto.getApprovalLines().stream()
                    .map(this::createLine)
                    .sorted(Comparator.comparing(AppLineEntity::getStepOrder))
                    .toList();

            approval.replaceLines(newLines);
        }
    }

    /**
     * 결재선 요청 한 줄을 AppLineEntity로 변환합니다.
     *
     * stepOrder=0은 참조자, stepOrder>=1은 실제 결재자입니다.
     */
    private AppLineEntity createLine(ApprovalLineRequestDto lineDto) {
        if (lineDto == null) {
            throw new IllegalArgumentException("결재선 정보가 비어 있습니다.");
        }

        if (lineDto.getApproverNo() == null || lineDto.getApproverNo().isBlank()) {
            throw new IllegalArgumentException("결재자 사번은 필수입니다.");
        }

        if (lineDto.getStepOrder() == null || lineDto.getStepOrder() < 0) {
            throw new IllegalArgumentException("결재 순서는 0 이상이어야 합니다. 0은 참조자를 의미합니다.");
        }

        EmpEntity approver = employeeRepository.findByEmpNo(lineDto.getApproverNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결재자입니다. 사번: " + lineDto.getApproverNo()));

        return AppLineEntity.builder()
                .approver(approver)
                .stepOrder(lineDto.getStepOrder())
                .status(ApprovalLineStatus.WAITING)
                .build();
    }

    /**
     * 상신 직후 현재 결재자로 지정할 첫 번째 결재자를 찾습니다.
     * 참조자(stepOrder=0)는 결재 순서에 참여하지 않으므로 제외합니다.
     */
    private EmpEntity findFirstApprover(List<ApprovalLineRequestDto> approvalLines) {
        String firstApproverNo = approvalLines.stream()
                .filter(line -> line.getStepOrder() != null && line.getStepOrder() > 0)
                .min(Comparator.comparing(ApprovalLineRequestDto::getStepOrder))
                .map(ApprovalLineRequestDto::getApproverNo)
                .orElseThrow(() -> new IllegalArgumentException("결재선에 최소 1명 이상의 실제 결재자가 필요합니다."));

        return employeeRepository.findByEmpNo(firstApproverNo)
                .orElseThrow(() -> new IllegalArgumentException("첫 번째 결재자를 찾을 수 없습니다."));
    }

    /**
     * 업로드된 첨부파일 목록을 결재 문서에 연결합니다.
     */
    private void addFiles(ApprovalEntity approval, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(this::saveApprovalFile)
                .forEach(approval::addFile);
    }

    /**
     * 첨부파일을 서버 디렉터리에 저장하고 APP_FILE 엔티티를 생성합니다.
     */
    private AppFileEntity saveApprovalFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("파일명이 없는 첨부파일은 저장할 수 없습니다.");
        }

        String extension = "";
        int extensionIndex = originalFilename.lastIndexOf(".");
        if (extensionIndex >= 0) {
            extension = originalFilename.substring(extensionIndex);
        }

        String savedFileName = UUID.randomUUID() + extension;
        File folder = new File(UPLOAD_BASE_DIR);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File saveFile = new File(folder, savedFileName);
        try {
            file.transferTo(saveFile);
        } catch (IOException e) {
            throw new RuntimeException("결재 첨부파일 저장 중 오류가 발생했습니다.", e);
        }

        return AppFileEntity.builder()
                .fileName(originalFilename)
                .filePath("/approval/uploads/" + savedFileName)
                .fileSize(file.getSize())
                .build();
    }

    /**
     * 임시저장과 상신에 공통으로 필요한 필수값을 검증합니다.
     */
    private void validateCommonRequest(
            ApprovalCreateRequestDto requestDto,
            PrincipalDetails principal
    ) {
        validatePrincipal(principal);
        validateDraftRequestFields(requestDto);
    }

    /**
     * 임시저장/상신 요청 DTO 자체의 필수값을 검증합니다.
     * 새 문서 생성과 기존 임시저장 수정에서 함께 사용합니다.
     */
    private void validateDraftRequestFields(ApprovalCreateRequestDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("결재 문서 정보가 필요합니다.");
        }

        if (requestDto.getFormId() == null) {
            throw new IllegalArgumentException("결재 양식은 필수입니다.");
        }

        if (requestDto.getTitle() == null || requestDto.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("결재 문서 제목은 필수입니다.");
        }
    }

    /**
     * 임시저장 문서를 수정하거나 상신할 수 있는 상태인지 확인합니다.
     *
     * 임시저장 문서는 아직 다른 결재자에게 공유되지 않은 작성자 개인 작업물이므로,
     * 작성자 본인만 수정할 수 있고 DRAFT 상태일 때만 이 API 흐름을 허용합니다.
     */
    private void validateDraftEditable(ApprovalEntity approval, String empNo) {
        boolean isWriter = approval.getWriter() != null
                && empNo.equals(approval.getWriter().getEmpNo());

        if (!isWriter) {
            throw new IllegalArgumentException("임시저장 문서는 작성자만 수정할 수 있습니다.");
        }

        if (approval.getStatus() != ApprovalStatus.DRAFT) {
            throw new IllegalArgumentException("이미 상신된 문서는 임시저장 수정 API에서 변경할 수 없습니다.");
        }
    }

    /**
     * 상신 전용 검증입니다.
     *
     * 참조자만 있는 결재선은 실제 승인 흐름을 만들 수 없으므로 허용하지 않습니다.
     */
    private void validateSubmitRequest(ApprovalCreateRequestDto requestDto) {
        if (requestDto.getApprovalLines() == null || requestDto.getApprovalLines().isEmpty()) {
            throw new IllegalArgumentException("상신하려면 결재선을 1명 이상 지정해야 합니다.");
        }

        boolean hasApprover = requestDto.getApprovalLines().stream()
                .anyMatch(line -> line != null
                        && line.getStepOrder() != null
                        && line.getStepOrder() > 0);

        if (!hasApprover) {
            throw new IllegalArgumentException("상신하려면 참조자 외에 실제 결재자가 1명 이상 필요합니다.");
        }
    }

    /**
     * JWT 인증 사용자 정보가 존재하는지 확인합니다.
     */
    private void validatePrincipal(PrincipalDetails principal) {
        if (principal == null) {
            throw new IllegalArgumentException("로그인 정보가 필요합니다.");
        }
    }

    /**
     * 목록 조회 status 파라미터를 ApprovalStatus enum으로 변환합니다.
     */
    private ApprovalStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return ApprovalStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 결재 상태입니다. status=" + status);
        }
    }

    /**
     * 결재선의 전체 단계 수를 계산합니다.
     * 참조자(stepOrder=0)는 단계 수에 영향을 주지 않습니다.
     */
    private Integer calculateMaxStep(List<ApprovalLineRequestDto> approvalLines) {
        if (approvalLines == null || approvalLines.isEmpty()) {
            return 0;
        }

        return approvalLines.stream()
                .map(line -> {
                    if (line == null || line.getStepOrder() == null) {
                        throw new IllegalArgumentException("결재 순서는 필수입니다.");
                    }
                    return line.getStepOrder();
                })
                .max(Integer::compareTo)
                .orElse(0);
    }

    /**
     * 상세 조회 권한을 검증합니다.
     */
    private void validateDetailReadable(ApprovalEntity approval, String empNo) {
        boolean isWriter = approval.getWriter() != null
                && empNo.equals(approval.getWriter().getEmpNo());

        if (isWriter) {
            return;
        }

        if (approval.getStatus() == ApprovalStatus.DRAFT) {
            throw new IllegalArgumentException("임시저장 문서는 작성자만 조회할 수 있습니다.");
        }

        boolean isLineParticipant = approval.getLines().stream()
                .anyMatch(line -> line.getApprover() != null
                        && empNo.equals(line.getApprover().getEmpNo()));

        if (!isLineParticipant) {
            throw new IllegalArgumentException("결재 문서 조회 권한이 없습니다.");
        }
    }

    /**
     * 저장/상신 직후 프론트가 사용할 최소 응답 DTO로 변환합니다.
     */
    private ApprovalCreateResponseDto toCreateResponse(ApprovalEntity approval) {
        return ApprovalCreateResponseDto.builder()
                .approvalId(approval.getApprovalId())
                .status(approval.getStatus().name())
                .currentStep(approval.getCurrentStep())
                .maxStep(approval.getMaxStep())
                .currentApproverNo(
                        approval.getCurrentApprover() != null
                                ? approval.getCurrentApprover().getEmpNo()
                                : null
                )
                .currentApproverName(
                        approval.getCurrentApprover() != null
                                ? approval.getCurrentApprover().getName()
                                : null
                )
                .build();
    }

    /**
     * ApprovalEntity를 상세 조회 응답 DTO로 변환합니다.
     */
    private ApprovalDetailResponseDto toDetailResponse(ApprovalEntity approval) {
        return ApprovalDetailResponseDto.builder()
                .approvalId(approval.getApprovalId())
                .formId(approval.getForm() != null ? approval.getForm().getFormId() : null)
                .formName(approval.getForm() != null ? approval.getForm().getFormName() : null)
                .title(approval.getTitle())
                .content(approval.getContent())
                .status(approval.getStatus())
                .writerNo(approval.getWriter() != null ? approval.getWriter().getEmpNo() : null)
                .writerName(approval.getWriter() != null ? approval.getWriter().getName() : null)
                .writerDeptName(getDepartmentName(approval.getWriter()))
                .writerPositionName(getPositionName(approval.getWriter()))
                .currentStep(approval.getCurrentStep())
                .maxStep(approval.getMaxStep())
                .currentApproverNo(
                        approval.getCurrentApprover() != null
                                ? approval.getCurrentApprover().getEmpNo()
                                : null
                )
                .currentApproverName(
                        approval.getCurrentApprover() != null
                                ? approval.getCurrentApprover().getName()
                                : null
                )
                .currentApproverDeptName(getDepartmentName(approval.getCurrentApprover()))
                .currentApproverPositionName(getPositionName(approval.getCurrentApprover()))
                .createdAt(approval.getCreatedAt())
                .updatedAt(approval.getUpdatedAt())
                .lines(toLineResponses(approval))
                .files(toFileResponses(approval))
                .build();
    }

    /**
     * 결재선 Entity 목록을 상세 화면용 DTO 목록으로 변환합니다.
     * 참조자(stepOrder=0)가 먼저 보이고, 이후 실제 결재 순서대로 정렬합니다.
     */
    private List<ApprovalLineResponseDto> toLineResponses(ApprovalEntity approval) {
        return approval.getLines().stream()
                .sorted(Comparator
                        .comparing(AppLineEntity::getStepOrder)
                        .thenComparing(AppLineEntity::getLineId))
                .map(line -> {
                    return ApprovalLineResponseDto.builder()
                            .lineId(line.getLineId())
                            .approverNo(line.getApprover() != null ? line.getApprover().getEmpNo() : null)
                            .approverName(line.getApprover() != null ? line.getApprover().getName() : null)
                            .approverDeptName(getDepartmentName(line.getApprover()))
                            .approverPositionName(getPositionName(line.getApprover()))
                            .stepOrder(line.getStepOrder())
                            .status(line.getStatus())
                            .processedAt(line.getProcessedAt())
                            .build();
                })
                .toList();
    }

    /**
     * 첨부파일 Entity 목록을 상세 화면용 DTO 목록으로 변환합니다.
     */
    /**
     * 상세 화면에서 사람을 "이름(소속, 직급, 사번)" 형태로 보여주기 위한 부서명 추출 메서드입니다.
     * 사원이 없거나 부서가 비어 있는 과거 데이터도 상세 조회가 깨지지 않도록 null을 반환합니다.
     */
    private String getDepartmentName(EmpEntity employee) {
        return employee != null && employee.getDepartment() != null
                ? employee.getDepartment().getDeptName()
                : null;
    }

    /**
     * 상세 화면 표시용 직급명 추출 메서드입니다.
     */
    private String getPositionName(EmpEntity employee) {
        return employee != null && employee.getPosition() != null
                ? employee.getPosition().getPositionName()
                : null;
    }

    private List<ApprovalFileResponseDto> toFileResponses(ApprovalEntity approval) {
        return approval.getFiles().stream()
                .sorted(Comparator.comparing(AppFileEntity::getFileId))
                .map(file -> ApprovalFileResponseDto.builder()
                        .fileId(file.getFileId())
                        .fileName(file.getFileName())
                        .filePath(file.getFilePath())
                        .fileSize(file.getFileSize())
                        .build())
                .toList();
    }

    /**
     * APP_FORM Entity를 직원 화면용 응답 DTO로 변환합니다.
     *
     * 결재선 서식 연결은 선택 사항이므로 null 검사를 거쳐 내려줍니다.
     * 이렇게 해두면 관리자가 결재선이 연결되지 않은 서식으로 설정해도 React 화면에서 오류 없이 처리할 수 있습니다.
     */
    private ApprovalFormResponseDto toFormResponse(AppFormEntity form) {
        return ApprovalFormResponseDto.builder()
                .formId(form.getFormId())
                .formName(form.getFormName())
                .template(form.getTemplate())
                .isDefault(form.getIsDefault())
                .lineTemplateId(
                        form.getLineTemplate() != null
                                ? form.getLineTemplate().getTemplateId()
                                : null
                )
                .lineTemplateName(
                        form.getLineTemplate() != null
                                ? form.getLineTemplate().getTemplateName()
                                : null
                )
                .build();
    }

    /**
     * 결재선 서식 상세 Entity를 React 화면에서 표시하기 좋은 대상 DTO로 변환합니다.
     *
     * 실제 상신 API는 현재 사원 번호 기반 결재선만 받기 때문에 USER 타입은 approverNo로 사용할 수 있는 id를 내려주고,
     * DEPT/POSITION 타입은 화면에서 참고 정보로 보여주되 사용자가 실제 결재자를 확정하도록 안내합니다.
     */
    private AppLineFormTargetDto toLineTemplateTargetResponse(AppLineTemplateDetailEntity detail) {
        String id = "";
        String name = "-";
        String dept = "";
        String position = "";
        Integer positionId = 0;

        switch (detail.getApproverType()) {
            case USER -> {
                if (detail.getApprover() != null) {
                    id = detail.getApprover().getEmpNo();
                    name = detail.getApprover().getName();
                    if (detail.getApprover().getDepartment() != null) {
                        dept = detail.getApprover().getDepartment().getDeptName();
                    }
                    if (detail.getApprover().getPosition() != null) {
                        position = detail.getApprover().getPosition().getPositionName();
                        positionId = detail.getApprover().getPosition().getPositionId();
                    }
                }
            }
            case DEPT -> {
                if (detail.getDepartment() != null) {
                    id = String.valueOf(detail.getDepartment().getDeptId());
                    name = detail.getDepartment().getDeptName();
                    dept = detail.getDepartment().getDeptName();
                }
            }
            case POSITION -> {
                if (detail.getMinPosition() != null) {
                    id = String.valueOf(detail.getMinPosition().getPositionId());
                    name = detail.getMinPosition().getPositionName();
                    position = detail.getMinPosition().getPositionName();
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
}
