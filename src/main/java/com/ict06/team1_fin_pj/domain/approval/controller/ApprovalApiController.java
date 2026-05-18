package com.ict06.team1_fin_pj.domain.approval.controller;

import com.ict06.team1_fin_pj.common.dto.approval.ApprovalCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalCreateResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalDetailResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalFormResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalListResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormDetailDto;
import com.ict06.team1_fin_pj.common.dto.employee.EmployeeListDto;
import com.ict06.team1_fin_pj.common.dto.employee.EmployeeSearchConditionDto;
import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import com.ict06.team1_fin_pj.domain.approval.service.ApprovalService;
import com.ict06.team1_fin_pj.domain.employee.service.AdEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/approval")
public class ApprovalApiController {

    private final ApprovalService approvalService;
    private final AdEmployeeService adEmployeeService;

    /**
     * 결재 서식 목록 조회 API
     *
     * - React 새 결재 문서 작성 화면에서 서식 선택 목록을 구성할 때 호출합니다.
     * - template JSON에는 text, number, date, time, amount, select 필드 정의가 들어갑니다.
     * - select 필드의 options도 template JSON 안에 포함되므로 프론트는 별도 옵션 API 없이 렌더링할 수 있습니다.
     * - lineTemplateId가 null이면 해당 서식에 기본 결재선이 연결되지 않은 상태입니다.
     */
    @GetMapping("/forms")
    public List<ApprovalFormResponseDto> getAvailableForms(
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.getAvailableForms(principal);
    }

    /**
     * 결재 서식 상세 조회 API
     *
     * - 사용자가 특정 서식을 선택했을 때 최신 template JSON과 기본 결재선 연결 정보를 조회합니다.
     * - 관리자 페이지에서 서식 또는 결재선 연결이 변경될 수 있으므로 작성 화면 진입 시 재조회하기 좋습니다.
     */
    @GetMapping("/forms/{formId}")
    public ApprovalFormResponseDto getFormDetail(
            @PathVariable Integer formId,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.getFormDetail(formId, principal);
    }

    /**
     * 기본 결재선 서식 상세 조회 API
     *
     * - 결재 서식에 lineTemplateId가 연결되어 있을 때 React 결재선 설정 화면에서 호출합니다.
     * - 관리자 전용 URL을 직원 화면에서 직접 사용하지 않도록 /api/approval 하위에 읽기 전용 API를 제공합니다.
     * - USER 타입 대상은 바로 결재선으로 사용할 수 있고, DEPT/POSITION 타입은 실제 결재자 확정이 추가로 필요합니다.
     */
    @GetMapping("/line-templates/{templateId}")
    public AppLineFormDetailDto getLineTemplateDetail(
            @PathVariable Integer templateId,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.getLineTemplateDetail(templateId, principal);
    }

    /**
     * 결재자/참조자 후보 사원 검색 API
     *
     * - React 결재선 설정 화면에서 단계별 후보자를 검색할 때 호출합니다.
     * - 사번, 이름, 부서명, 직급 검색은 기존 인사관리 검색 조건 DTO와 Repository 검색 로직을 재사용합니다.
     * - 관리자 URL과 섞이지 않도록 직원 전자결재 API인 /api/approval 하위에 별도 경로를 제공합니다.
     */
    @GetMapping("/employees")
    public Page<EmployeeListDto> searchApprovalEmployees(
            @ModelAttribute EmployeeSearchConditionDto conditionDto,
            @PageableDefault(size = 5, sort = "empNo", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        if (principal == null) {
            throw new IllegalArgumentException("로그인 정보가 필요합니다.");
        }

        return adEmployeeService.findEmployees(conditionDto, pageable);
    }

    /**
     * 개인 문서함 목록 조회 API
     *
     * - 로그인한 사용자가 작성한 결재 문서를 조회합니다.
     * - status 파라미터가 없으면 임시저장(DRAFT)을 제외한 전체 문서를 조회합니다.
     * - status 파라미터가 있으면 IN_PROGRESS, COMPLETED, REJECTED 등 특정 상태만 필터링합니다.
     * - React 화면에서 페이지네이션을 쉽게 처리할 수 있도록 Spring Data Page 형태로 반환합니다.
     */
    @GetMapping("/my-documents")
    public Page<ApprovalListResponseDto> getMyDocuments(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.getMyDocuments(status, principal, pageable);
    }

    /**
     * 임시저장 문서함 목록 조회 API
     *
     * - 로그인한 사용자가 작성한 문서 중 DRAFT 상태인 문서만 조회합니다.
     * - 일반 개인 문서함과 분리해, 화면에서도 임시저장함 메뉴를 독립적으로 구성할 수 있게 했습니다.
     */
    @GetMapping("/drafts")
    public Page<ApprovalListResponseDto> getMyDrafts(
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.getMyDrafts(principal, pageable);
    }

    /**
     * 참조 문서함 목록 조회 API
     *
     * - 로그인한 사용자가 참조자로 지정된 문서를 조회합니다.
     * - 참조자는 APP_LINE.stepOrder=0으로 저장된 대상입니다.
     * - 참조 문서는 결재자가 아니므로 승인/반려 권한은 없고 열람 권한만 가집니다.
     * - 임시저장(DRAFT) 문서는 아직 공유된 문서가 아니므로 참조 문서함에서 제외합니다.
     */
    @GetMapping("/referenced-documents")
    public Page<ApprovalListResponseDto> getMyReferencedDocuments(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.getMyReferencedDocuments(status, principal, pageable);
    }

    /**
     * 결재 대기 문서함 목록 조회 API
     *
     * - 로그인한 사용자가 지금 승인/반려해야 하는 문서를 조회합니다.
     * - 현재 결재자가 로그인 사용자이고, 문서 상태가 IN_PROGRESS인 문서만 반환합니다.
     */
    @GetMapping("/pending-documents")
    public Page<ApprovalListResponseDto> getPendingApprovals(
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.getPendingApprovals(principal, pageable);
    }

    /**
     * 결재 예정 문서함 목록 조회 API
     *
     * - 로그인한 사용자가 결재선에는 포함되어 있지만 아직 본인 차례가 아닌 문서를 조회합니다.
     * - stepOrder=0인 참조자는 결재자가 아니므로 예정 문서함에서 제외합니다.
     */
    @GetMapping("/upcoming-documents")
    public Page<ApprovalListResponseDto> getUpcomingApprovals(
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.getUpcomingApprovals(principal, pageable);
    }

    /**
     * 결재 승인 API
     *
     * - 결재 대기 문서 상세 화면에서 승인 버튼을 눌렀을 때 호출합니다.
     * - 현재 결재자가 로그인 사용자일 때만 승인할 수 있습니다.
     * - 다음 결재자가 있으면 다음 단계로 이동하고, 마지막 결재자라면 문서를 완료 처리합니다.
     */
    @PostMapping("/{approvalId}/approve")
    public ApprovalCreateResponseDto approveApproval(
            @PathVariable Integer approvalId,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.approveApproval(approvalId, principal);
    }

    /**
     * 결재 반려 API
     *
     * - 결재 대기 문서 상세 화면에서 반려 버튼을 눌렀을 때 호출합니다.
     * - 현재 결재자가 로그인 사용자일 때만 반려할 수 있습니다.
     * - 반려 시 문서 전체 결재 흐름은 REJECTED 상태로 종료됩니다.
     */
    @PostMapping("/{approvalId}/reject")
    public ApprovalCreateResponseDto rejectApproval(
            @PathVariable Integer approvalId,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.rejectApproval(approvalId, principal);
    }

    /**
     * 결재 문서 상신 취소 API
     *
     * - 작성자 본인만 상신 취소할 수 있습니다.
     * - 결재자가 아직 승인/반려하지 않은 진행중 문서만 취소할 수 있습니다.
     * - 취소된 문서는 CANCELED 상태로 남겨 개인 문서함에서 이력을 확인할 수 있게 합니다.
     */
    @PostMapping("/{approvalId}/cancel")
    public ApprovalCreateResponseDto cancelApproval(
            @PathVariable Integer approvalId,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.cancelApproval(approvalId, principal);
    }

    /**
     * 결재 문서 상세 조회 API
     *
     * - 목록에서 문서를 클릭했을 때 호출합니다.
     * - 작성자, 결재선에 포함된 결재자, 참조자만 열람할 수 있습니다.
     * - 임시저장(DRAFT) 문서는 아직 공유 전이므로 작성자만 열람할 수 있습니다.
     */
    /**
     * 결재 첨부파일 삭제 API
     *
     * - 임시저장 문서를 수정하는 화면에서 기존 첨부파일을 삭제할 때 사용합니다.
     * - 작성자 본인의 DRAFT 문서에 속한 첨부파일만 삭제할 수 있습니다.
     * - DB의 APP_FILE 행과 서버에 저장된 실제 파일을 함께 삭제합니다.
     */
    @DeleteMapping("/files/{fileId}")
    public void deleteApprovalFile(
            @PathVariable Integer fileId,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        approvalService.deleteApprovalFile(fileId, principal);
    }

    @GetMapping("/{approvalId}")
    public ApprovalDetailResponseDto getApprovalDetail(
            @PathVariable Integer approvalId,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.getApprovalDetail(approvalId, principal);
    }

    /**
     * 결재 문서 임시저장 API(JSON)
     *
     * - 첨부파일이 없는 경우 사용하는 API입니다.
     * - 문서 제목, 결재 양식, 본문 JSON, 결재선을 받아 DRAFT 상태로 저장합니다.
     */
    @PostMapping(
            value = "/drafts",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ApprovalCreateResponseDto saveDraft(
            @RequestBody ApprovalCreateRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.saveDraft(requestDto, principal, null);
    }

    /**
     * 결재 문서 임시저장 API(multipart)
     *
     * - 첨부파일이 있는 경우 사용하는 API입니다.
     * - request 파트에는 ApprovalCreateRequestDto JSON을, files 파트에는 첨부파일 목록을 전달합니다.
     * - JSON 전용 API와 같은 서비스 메서드를 사용해 저장 규칙이 달라지지 않도록 했습니다.
     */
    @PostMapping(
            value = "/drafts",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApprovalCreateResponseDto saveDraftWithFiles(
            @RequestPart("request") ApprovalCreateRequestDto requestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.saveDraft(requestDto, principal, files);
    }

    /**
     * 기존 임시저장 문서 수정 API(JSON)
     *
     * - 작성자 본인의 DRAFT 문서만 수정할 수 있습니다.
     * - 제목, 본문, 결재 양식, 결재선을 요청 값으로 갱신합니다.
     * - 첨부파일이 없는 화면 저장 흐름에서 사용합니다.
     */
    @PutMapping(
            value = "/drafts/{approvalId}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ApprovalCreateResponseDto updateDraft(
            @PathVariable Integer approvalId,
            @RequestBody ApprovalCreateRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.updateDraft(approvalId, requestDto, principal, null);
    }

    /**
     * 기존 임시저장 문서 수정 API(multipart)
     *
     * - 첨부파일을 추가하면서 임시저장 내용을 수정할 때 사용합니다.
     * - request 파트에는 ApprovalCreateRequestDto JSON을, files 파트에는 추가 첨부파일을 전달합니다.
     * - 현재 단계에서는 기존 첨부파일 삭제/교체가 아니라 새 파일 추가만 처리합니다.
     */
    @PutMapping(
            value = "/drafts/{approvalId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApprovalCreateResponseDto updateDraftWithFiles(
            @PathVariable Integer approvalId,
            @RequestPart("request") ApprovalCreateRequestDto requestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.updateDraft(approvalId, requestDto, principal, files);
    }

    /**
     * 기존 임시저장 문서 상신 API(JSON)
     *
     * - 임시저장 상세 화면에서 최종 내용을 함께 전달하고 바로 상신할 때 사용합니다.
     * - stepOrder=0인 참조자는 결재 순서 계산에서 제외되고, stepOrder가 1 이상인 첫 결재자가 현재 결재자가 됩니다.
     */
    @PostMapping(
            value = "/drafts/{approvalId}/submit",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ApprovalCreateResponseDto submitDraft(
            @PathVariable Integer approvalId,
            @RequestBody ApprovalCreateRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.submitDraft(approvalId, requestDto, principal, null);
    }

    /**
     * 기존 임시저장 문서 상신 API(multipart)
     *
     * - 임시저장 문서에 첨부파일을 추가한 뒤 바로 상신할 때 사용합니다.
     * - 신규 상신 API와 같은 multipart 구조를 사용해 React 쪽 구현 방식을 통일합니다.
     */
    @PostMapping(
            value = "/drafts/{approvalId}/submit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApprovalCreateResponseDto submitDraftWithFiles(
            @PathVariable Integer approvalId,
            @RequestPart("request") ApprovalCreateRequestDto requestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.submitDraft(approvalId, requestDto, principal, files);
    }

    /**
     * 결재 문서 상신 API(JSON)
     *
     * - 첨부파일 없이 바로 상신할 때 사용합니다.
     * - 결재선이 최소 1명 이상 있어야 하며, 첫 번째 결재자를 현재 결재자로 지정합니다.
     */
    @PostMapping(
            value = "/submit",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ApprovalCreateResponseDto submit(
            @RequestBody ApprovalCreateRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.submit(requestDto, principal, null);
    }

    /**
     * 결재 문서 상신 API(multipart)
     *
     * - 첨부파일과 함께 상신할 때 사용합니다.
     * - request 파트의 문서 정보와 files 파트의 파일을 하나의 트랜잭션 흐름에서 저장합니다.
     */
    @PostMapping(
            value = "/submit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApprovalCreateResponseDto submitWithFiles(
            @RequestPart("request") ApprovalCreateRequestDto requestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        return approvalService.submit(requestDto, principal, files);
    }
}
