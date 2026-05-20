package com.ict06.team1_fin_pj.domain.approval.service;

import com.ict06.team1_fin_pj.common.dto.approval.ApprovalCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalCreateResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalDetailResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalFormResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalListResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.ApprovalEmployeeSignResponseDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormDetailDto;
import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * 일반 직원용 전자결재 서비스 인터페이스입니다.
 *
 * 관리자용 결재 양식/결재선 템플릿 관리는 AdApprovalService에서 처리하고,
 * 이 서비스는 React 사용자 화면에서 사용하는 문서 작성, 임시저장, 상신, 개인 문서함 조회를 담당합니다.
 */
public interface ApprovalService {

    /**
     * React 문서 작성 화면에서 선택할 수 있는 결재 서식 목록을 조회합니다.
     *
     * template JSON까지 함께 내려주므로 프론트에서는 목록 선택 후 즉시 동적 입력 폼을 구성할 수 있습니다.
     * 필요하면 추후 목록용 DTO와 상세용 DTO를 분리해 응답 크기를 줄일 수 있습니다.
     */
    List<ApprovalFormResponseDto> getAvailableForms(PrincipalDetails principal);

    /**
     * 특정 결재 서식의 상세 정보를 조회합니다.
     *
     * 사용자가 목록에서 서식을 선택했을 때 최신 template JSON과 기본 결재선 연결 정보를 확인하는 용도입니다.
     */
    ApprovalFormResponseDto getFormDetail(Integer formId, PrincipalDetails principal);

    /**
     * 결재 서식에 연결된 기본 결재선 서식 상세 정보를 조회합니다.
     *
     * 직원 작성 화면에서는 관리자 URL을 직접 호출하지 않고 이 API를 통해 기본 결재선 미리보기만 사용합니다.
     */
    AppLineFormDetailDto getLineTemplateDetail(Integer templateId, PrincipalDetails principal);

    /**
     * 결재자로 선택한 사원의 인감 이미지 경로를 조회합니다.
     *
     * 결재선 설정 화면에서는 이 응답으로 인감 미리보기를 보여주고,
     * signImg가 비어 있으면 사용자에게 관리자 등록 요청 안내를 띄웁니다.
     */
    ApprovalEmployeeSignResponseDto getEmployeeSign(String empNo, PrincipalDetails principal);

    /**
     * 새 결재 문서를 임시저장합니다.
     * 첨부파일이 없는 JSON 요청과 첨부파일이 있는 multipart 요청 모두 이 메서드를 사용합니다.
     */
    ApprovalCreateResponseDto saveDraft(
            ApprovalCreateRequestDto requestDto,
            PrincipalDetails principal,
            List<MultipartFile> files
    );

    /**
     * 기존 임시저장 문서를 수정합니다.
     * DRAFT 상태이고 작성자 본인인 경우에만 수정할 수 있습니다.
     */
    ApprovalCreateResponseDto updateDraft(
            Integer approvalId,
            ApprovalCreateRequestDto requestDto,
            PrincipalDetails principal,
            List<MultipartFile> files
    );

    /**
     * 새 결재 문서를 상신합니다.
     * 상신 시에는 결재선이 필수이며, 첫 번째 결재자가 현재 결재자로 지정됩니다.
     */
    ApprovalCreateResponseDto submit(
            ApprovalCreateRequestDto requestDto,
            PrincipalDetails principal,
            List<MultipartFile> files
    );

    /**
     * 기존 임시저장 문서를 수정 내용으로 갱신한 뒤 상신합니다.
     * DRAFT 상태이고 작성자 본인인 경우에만 상신할 수 있습니다.
     */
    ApprovalCreateResponseDto submitDraft(
            Integer approvalId,
            ApprovalCreateRequestDto requestDto,
            PrincipalDetails principal,
            List<MultipartFile> files
    );

    /**
     * 작성자 본인의 임시저장 문서를 삭제합니다.
     *
     * 임시저장 문서는 아직 결재자에게 공유되지 않은 개인 작업물이므로,
     * 작성자 본인이고 DRAFT 상태일 때만 실제 삭제를 허용합니다.
     */
    void deleteDraft(
            Integer approvalId,
            PrincipalDetails principal
    );

    /**
     * 로그인한 사용자가 작성한 결재 문서 목록을 조회합니다.
     * status가 null이면 임시저장을 제외한 전체 문서를 조회합니다.
     */
    Page<ApprovalListResponseDto> getMyDocuments(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            PrincipalDetails principal,
            Pageable pageable
    );

    /**
     * 로그인한 사용자의 임시저장 문서 목록을 조회합니다.
     */
    Page<ApprovalListResponseDto> getMyDrafts(
            LocalDate startDate,
            LocalDate endDate,
            PrincipalDetails principal,
            Pageable pageable
    );

    /**
     * 로그인한 사용자가 참조자로 지정된 결재 문서 목록을 조회합니다.
     *
     * 참조자는 APP_LINE.stepOrder=0으로 저장된 대상이며,
     * 결재 승인/반려 권한은 없고 열람 권한만 가집니다.
     */
    Page<ApprovalListResponseDto> getMyReferencedDocuments(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            PrincipalDetails principal,
            Pageable pageable
    );

    /**
     * 현재 로그인 사용자가 지금 결재해야 하는 문서 목록을 조회합니다.
     */
    Page<ApprovalListResponseDto> getPendingApprovals(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            PrincipalDetails principal,
            Pageable pageable
    );

    /**
     * 로그인 사용자가 과거에 승인/반려 처리한 문서 목록을 조회합니다.
     */
    Page<ApprovalListResponseDto> getProcessedApprovals(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            PrincipalDetails principal,
            Pageable pageable
    );

    /**
     * 로그인 사용자가 결재선에 포함되어 있지만 아직 차례가 오지 않은 문서 목록을 조회합니다.
     */
    Page<ApprovalListResponseDto> getUpcomingApprovals(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            PrincipalDetails principal,
            Pageable pageable
    );

    /**
     * 결재 대기 문서를 승인 처리합니다.
     * 현재 결재자가 로그인 사용자일 때만 처리할 수 있습니다.
     */
    ApprovalCreateResponseDto approveApproval(
            Integer approvalId,
            PrincipalDetails principal
    );

    /**
     * 결재 대기 문서를 반려 처리합니다.
     * 현재 결재자가 로그인 사용자일 때만 처리할 수 있습니다.
     */
    ApprovalCreateResponseDto rejectApproval(
            Integer approvalId,
            PrincipalDetails principal
    );

    /**
     * 작성자가 상신한 결재 문서를 취소 처리합니다.
     *
     * 결재가 완료/반려되기 전 진행 중 문서라면 작성자가 취소할 수 있습니다.
     * 취소 문서는 삭제하지 않고 CANCELED 상태로 남겨 개인 문서함에서 이력을 확인할 수 있게 합니다.
     */
    ApprovalCreateResponseDto cancelApproval(
            Integer approvalId,
            PrincipalDetails principal
    );

    /**
     * 결재 문서 상세 정보를 조회합니다.
     *
     * 작성자, 결재자, 참조자만 조회할 수 있으며,
     * 임시저장 문서는 작성자만 열람할 수 있습니다.
     */
    ApprovalDetailResponseDto getApprovalDetail(
            Integer approvalId,
            PrincipalDetails principal
    );

    /**
     * 임시저장 문서에 첨부된 파일을 삭제합니다.
     * 작성자 본인의 DRAFT 문서에 속한 파일만 삭제할 수 있습니다.
     */
    void deleteApprovalFile(
            Integer fileId,
            PrincipalDetails principal
    );
}
