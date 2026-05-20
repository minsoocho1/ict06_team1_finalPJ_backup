package com.ict06.team1_fin_pj.domain.aiSecretary.controller;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeResponseDto;
import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import com.ict06.team1_fin_pj.domain.aiSecretary.service.AdAiSecretaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/AiSecretary")
@RequiredArgsConstructor
public class AdAiSecretaryController {

    private final AdAiSecretaryService adAiSecretaryService;

    /**
     * AI 비서 관리자 대시보드 화면 조회
     */
    @GetMapping("/dashboard")
    public String aiDashboard(
            @RequestParam(defaultValue = "7") int period,
            @RequestParam(defaultValue = "") String startDate,
            @RequestParam(defaultValue = "") String endDate,
            @RequestParam(defaultValue = "") String department,
            @RequestParam(defaultValue = "") String aiType,
            @RequestParam(defaultValue = "") String result,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        AdAiDashboardResponseDto dashboard = adAiSecretaryService.getDashboardData(
                period, startDate, endDate, department, aiType, result, page
        );

        model.addAttribute("selectedPeriod", period);
        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("selectedAiType", aiType);
        model.addAttribute("selectedResult", result);

        model.addAttribute("currentPage", dashboard.getCurrentPage());
        model.addAttribute("totalPages", dashboard.getTotalPages());
        model.addAttribute("totalLogCount", dashboard.getTotalLogCount());
        model.addAttribute("hasPrevious", dashboard.isHasPrevious());
        model.addAttribute("hasNext", dashboard.isHasNext());

        model.addAttribute("documentStatusTitle", dashboard.getDocumentStatusTitle());
        model.addAttribute("summaryCards", dashboard.getSummaryCards());
        model.addAttribute("featureUsageList", dashboard.getFeatureUsageList());
        model.addAttribute("usageTrendList", dashboard.getUsageTrendList());
        model.addAttribute("recentLogList", dashboard.getRecentLogList());
        model.addAttribute("documentStatusList", dashboard.getDocumentStatusList());

        model.addAttribute("activeDateFilterLabel", buildDateFilterLabel(period, startDate, endDate));
        model.addAttribute("dateFilterResetUrl", buildDateFilterResetUrl(period, department, aiType, result));

        return "admin/aiSecretary/adAiDashboard";
    }

    /**
     * 대시보드 최근 로그 CSV 다운로드
     */
    @GetMapping("/dashboard/download/csv")
    public ResponseEntity<byte[]> downloadDashboardCsv(
            @RequestParam(defaultValue = "7") int period,
            @RequestParam(defaultValue = "") String startDate,
            @RequestParam(defaultValue = "") String endDate,
            @RequestParam(defaultValue = "") String department,
            @RequestParam(defaultValue = "") String aiType,
            @RequestParam(defaultValue = "") String result
    ) {
        byte[] csvBytes = adAiSecretaryService.downloadRecentLogCsv(period, startDate, endDate, department, aiType, result);
        String fileName = "ai_dashboard_logs_" + LocalDate.now(ZoneId.of("Asia/Seoul")) + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes == null ? new byte[0] : csvBytes);
    }

    /**
     * AI RAG 지식베이스 관리 화면 조회
     */
    @GetMapping("/rag")
    public String aiRagManage(
            @RequestParam(defaultValue = "") String requestStartDate,
            @RequestParam(defaultValue = "") String requestEndDate,
            @RequestParam(defaultValue = "PENDING") String requestStatus,
            @RequestParam(defaultValue = "") String requestType,
            @RequestParam(defaultValue = "") String requestCategory,
            @RequestParam(defaultValue = "1") int requestPage,
            @RequestParam(defaultValue = "") String docStartDate,
            @RequestParam(defaultValue = "") String docEndDate,
            @RequestParam(defaultValue = "") String docStage,
            @RequestParam(defaultValue = "") String docCategory,
            @RequestParam(defaultValue = "") String docType,
            @RequestParam(defaultValue = "") String accessLevel,
            @RequestParam(defaultValue = "1") int docPage,
            Model model
    ) {
        // 1. 지식 등록 요청 데이터 조회 및 페이징 처리
        List<KnowledgeResponseDto> requestManagementRequests = adAiSecretaryService.getKnowledgeRequestsForAdmin(
                requestStartDate, requestEndDate, requestStatus, requestType, requestCategory
        );
        List<KnowledgeResponseDto> allKnowledgeRequests = adAiSecretaryService.getKnowledgeRequestsForAdmin(
                "", "", "", "", ""
        );
        List<Map<String, Object>> documentManagementRows = adAiSecretaryService.getDocumentManagementRows(allKnowledgeRequests);

        int requestPageSize = 10;
        int requestTotalCount = requestManagementRequests.size();
        int requestTotalPages = requestTotalCount == 0 ? 0 : (int) Math.ceil((double) requestTotalCount / requestPageSize);
        int currentRequestPage = requestTotalPages == 0 ? 0 : Math.min(Math.max(requestPage, 1), requestTotalPages);
        int fromIndex = requestTotalPages == 0 ? 0 : (currentRequestPage - 1) * requestPageSize;
        int toIndex = requestTotalPages == 0 ? 0 : Math.min(fromIndex + requestPageSize, requestTotalCount);

        model.addAttribute("requestStartDate", requestStartDate);
        model.addAttribute("requestEndDate", requestEndDate);
        model.addAttribute("selectedRequestStatus", requestStatus);
        model.addAttribute("selectedRequestType", requestType);
        model.addAttribute("selectedRequestCategory", requestCategory);
        model.addAttribute("requestPage", currentRequestPage);
        model.addAttribute("requestTotalPages", requestTotalPages);
        model.addAttribute("requestTotalCount", requestTotalCount);
        model.addAttribute("knowledgeRequests", requestTotalPages == 0
                ? List.<KnowledgeResponseDto>of()
                : requestManagementRequests.subList(fromIndex, toIndex));
        model.addAttribute("documentManagementCount", documentManagementRows.size());
        model.addAttribute("documentManagementRows", documentManagementRows);

        // 2. 문서 관리 검색 조건 복원
        model.addAttribute("docStartDate", docStartDate);
        model.addAttribute("docEndDate", docEndDate);
        model.addAttribute("selectedDocStage", docStage);
        model.addAttribute("selectedDocCategory", docCategory);
        model.addAttribute("selectedDocType", docType);
        model.addAttribute("selectedAccessLevel", accessLevel);

        // 3. 문서 관리 더미 페이징
        int docPageSize = 10;
        int docTotalCount = 128;
        int docTotalPages = (int) Math.ceil((double) docTotalCount / docPageSize);

        model.addAttribute("docPage", docPage);
        model.addAttribute("docPageSize", docPageSize);
        model.addAttribute("docTotalPages", docTotalPages);
        model.addAttribute("docTotalCount", docTotalCount);

        // 4. 공통 UI 옵션 및 더미 데이터 가인입
        model.addAttribute("embeddingSummary", List.of(
                Map.of("title", "전체 임베딩", "count", 27, "description", "전체 임베딩 작업이 진행 중입니다."),
                Map.of("title", "임베딩 진행", "count", 4, "description", "현재 문서를 임베딩하는 중입니다."),
                Map.of("title", "임베딩 대기", "count", 6, "description", "대기 중인 임베딩 작업입니다."),
                Map.of("title", "RAG 반영", "count", 18, "description", "RAG 반영 대기 현황입니다.")
        ));

        model.addAttribute("requestTypeOptions", List.of("사내 규정", "업무 매뉴얼", "FAQ", "서비스 이용 안내", "기타"));
        model.addAttribute("categoryOptions", List.of("근태", "인사", "전자결재", "교육", "복지", "시스템", "보안", "기타"));

        model.addAttribute("departmentOptions", List.of(
                Map.of("id", 1, "name", "경영지원팀"),
                Map.of("id", 2, "name", "인사팀"),
                Map.of("id", 3, "name", "개발1팀(BE)"),
                Map.of("id", 4, "name", "개발2팀(FE)"),
                Map.of("id", 5, "name", "디자인팀")
        ));

        model.addAttribute("roleOptions", List.of(
                Map.of("id", 1, "name", "ADMIN"),
                Map.of("id", 2, "name", "TEAM_LEADER"),
                Map.of("id", 3, "name", "USER")
        ));

        model.addAttribute("positionOptions", List.of(
                Map.of("id", 1, "name", "전원"),
                Map.of("id", 2, "name", "사원"),
                Map.of("id", 3, "name", "주임"),
                Map.of("id", 4, "name", "선임"),
                Map.of("id", 5, "name", "책임"),
                Map.of("id", 6, "name", "수석")
        ));

        model.addAttribute("gradeOptions", List.of(
                Map.of("id", "G1", "name", "G1"),
                Map.of("id", "G2", "name", "G2"),
                Map.of("id", "G3", "name", "G3"),
                Map.of("id", "G4", "name", "G4"),
                Map.of("id", "G5", "name", "G5")
        ));

        model.addAttribute("accessLevelOptions", List.of("전체 공개", "조건 조합", "관리자 전용"));

        model.addAttribute("accessBlockLogs", List.of(
                Map.of("user", "홍길동", "dept", "인사팀", "documentTitle", "근태 관리 문서", "reason", "권한 조건 불일치", "createdAt", "2026-05-11 14:22"),
                Map.of("user", "김철수", "dept", "개발1팀(BE)", "documentTitle", "보안 서약 문서", "reason", "관리자 검토 필요", "createdAt", "2026-05-11 13:40")
        ));

        return "admin/aiSecretary/adAiRagManage";
    }

    /**
     * 사내 지식 등록 요청 승인/반려 처리
     */
    @PostMapping("/rag/knowledge-requests/{requestId}/review")
    public String reviewKnowledgeRequest(
            @PathVariable Long requestId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String adminComment,
            @AuthenticationPrincipal PrincipalDetails principal,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("reviewErrorMessage", "로그인이 필요합니다.");
            return "redirect:/admin/AiSecretary/rag";
        }

        try {
            KnowledgeResponseDto reviewedRequest = adAiSecretaryService.reviewKnowledgeRequest(
                    requestId, status, adminComment, principal.getEmpNo()
            );
            redirectAttributes.addFlashAttribute("reviewSuccessMessage",
                    reviewedRequest == null
                            ? "자료 등록 요청이 처리되었습니다."
                            : "자료 등록 요청이 " + reviewedRequest.getStatusLabel() + " 처리되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("reviewErrorMessage", e.getMessage());
        } catch (Exception e) {
            String message = e.getMessage();
            if (e instanceof org.springframework.web.server.ResponseStatusException responseStatusException && responseStatusException.getReason() != null) {
                message = responseStatusException.getReason();
            }
            redirectAttributes.addFlashAttribute("reviewErrorMessage",
                    (message == null || message.isBlank()) ? "자료 등록 요청 처리에 실패했습니다." : message);
        }

        return "redirect:/admin/AiSecretary/rag";
    }

    private boolean isPendingKnowledgeRequest(KnowledgeResponseDto request) {
        return request != null
                && request.getStatus() != null
                && "PENDING".equalsIgnoreCase(request.getStatus().trim());
    }

    private boolean isRejectedKnowledgeRequest(KnowledgeResponseDto request) {
        return request != null
                && request.getStatus() != null
                && "REJECTED".equalsIgnoreCase(request.getStatus().trim());
    }

    /**
     * AI 보안 및 접근 권한 관리 화면 조회
     */
    @GetMapping("/security")
    public String aiSecurity(
            @RequestParam(defaultValue = "") String policyStartDate,
            @RequestParam(defaultValue = "") String policyEndDate,
            @RequestParam(defaultValue = "") String selectedPolicyDocType,
            @RequestParam(defaultValue = "") String selectedPolicyAccessLevel,
            @RequestParam(defaultValue = "") String selectedPolicyActiveStatus,
            @RequestParam(defaultValue = "1") int policyPage,
            @RequestParam(defaultValue = "") String blockStartDate,
            @RequestParam(defaultValue = "") String blockEndDate,
            @RequestParam(defaultValue = "") String selectedBlockDept,
            @RequestParam(defaultValue = "") String selectedBlockReason,
            @RequestParam(defaultValue = "1") int blockPage,
            Model model
    ) {
        model.addAttribute("policySummary", List.of(
                Map.of("label", "전체 공개 문서", "value", "18", "description", "모든 직원이 접근 가능한 문서입니다."),
                Map.of("label", "조건 조합 문서", "value", "12", "description", "선택한 조건에 따라 접근이 달라지는 문서입니다."),
                Map.of("label", "관리자 전용 문서", "value", "8", "description", "관리자만 접근 가능한 문서입니다."),
                Map.of("label", "접근 차단 로그", "value", "5", "description", "권한이 맞지 않아 차단된 기록입니다.")
        ));

        model.addAttribute("documentTypeOptions", List.of("사내 규정", "업무 매뉴얼", "FAQ", "서비스 이용 안내", "기타"));
        model.addAttribute("accessLevelOptions", List.of("전체 공개", "조건 조합", "관리자 전용"));
        model.addAttribute("activeStatusOptions", List.of("ACTIVE", "INACTIVE"));

        model.addAttribute("documentPolicies", List.of(
                Map.of("title", "전체 공개 문서", "documentType", "전체 공개", "accessLevel", "전체 공개", "target", "전체 직원", "activeStatus", "ACTIVE", "activeLabel", "활성", "updatedAt", "2026-05-09"),
                Map.of("title", "조건 조합 문서", "documentType", "조건 조합", "accessLevel", "조건 조합", "target", "TEAM_LEADER, ADMIN", "activeStatus", "ACTIVE", "activeLabel", "활성", "updatedAt", "2026-05-08"),
                Map.of("title", "관리자 전용 문서", "documentType", "관리자 전용", "accessLevel", "관리자 전용", "target", "ADMIN", "activeStatus", "INACTIVE", "activeLabel", "비활성", "updatedAt", "2026-05-07")
        ));

        model.addAttribute("policyPage", policyPage);
        model.addAttribute("policyPageSize", 10);
        model.addAttribute("policyTotalCount", 28);
        model.addAttribute("policyTotalPages", 3);

        model.addAttribute("selectedPolicyDocType", selectedPolicyDocType);
        model.addAttribute("selectedPolicyAccessLevel", selectedPolicyAccessLevel);
        model.addAttribute("selectedPolicyActiveStatus", selectedPolicyActiveStatus);
        model.addAttribute("policyStartDate", policyStartDate);
        model.addAttribute("policyEndDate", policyEndDate);

        model.addAttribute("blockReasonOptions", List.of("권한 조건 불일치", "비공개 문서 접근", "잘못된 부서 접근", "관리자 검토 필요"));

        model.addAttribute("accessBlockLogs", List.of(
                Map.of("user", "홍길동", "dept", "인사팀", "documentTitle", "근태 관리 문서", "reason", "권한 조건 불일치", "createdAt", "2026-05-11 14:22"),
                Map.of("user", "김철수", "dept", "개발1팀(BE)", "documentTitle", "보안 서약 문서", "reason", "관리자 검토 필요", "createdAt", "2026-05-11 13:40")
        ));

        model.addAttribute("blockPage", blockPage);
        model.addAttribute("blockPageSize", 10);
        model.addAttribute("blockTotalCount", 2);
        model.addAttribute("blockTotalPages", 1);
        model.addAttribute("blockStartDate", blockStartDate);
        model.addAttribute("blockEndDate", blockEndDate);
        model.addAttribute("selectedBlockDept", selectedBlockDept);
        model.addAttribute("selectedBlockReason", selectedBlockReason);

        model.addAttribute("departmentOptions", List.of(
                Map.of("id", 1, "name", "경영지원팀"),
                Map.of("id", 2, "name", "인사팀"),
                Map.of("id", 3, "name", "개발1팀(BE)"),
                Map.of("id", 4, "name", "개발2팀(FE)"),
                Map.of("id", 5, "name", "디자인팀")
        ));

        model.addAttribute("roleOptions", List.of(
                Map.of("id", 1, "name", "ADMIN"),
                Map.of("id", 2, "name", "TEAM_LEADER"),
                Map.of("id", 3, "name", "USER")
        ));

        model.addAttribute("positionOptions", List.of(
                Map.of("id", 1, "name", "전원"),
                Map.of("id", 2, "name", "사원"),
                Map.of("id", 3, "name", "주임"),
                Map.of("id", 4, "name", "선임"),
                Map.of("id", 5, "name", "책임"),
                Map.of("id", 6, "name", "수석")
        ));

        model.addAttribute("gradeOptions", List.of(
                Map.of("id", "G1", "name", "G1"),
                Map.of("id", "G2", "name", "G2"),
                Map.of("id", "G3", "name", "G3"),
                Map.of("id", "G4", "name", "G4"),
                Map.of("id", "G5", "name", "G5")
        ));

        return "admin/aiSecretary/adAiSecurity";
    }

    private String buildDateFilterLabel(int period, String startDate, String endDate) {
        if (hasText(startDate) && hasText(endDate)) {
            return "Period: " + startDate + " ~ " + endDate;
        }
        if (period == 30) {
            return "Period: Recent 30 days";
        }
        return "Period: Recent 7 days";
    }

    private String buildDateFilterResetUrl(int period, String department, String aiType, String result) {
        return UriComponentsBuilder.fromPath("/admin/AiSecretary/dashboard")
                .queryParam("period", 7)
                .queryParam("department", department)
                .queryParam("aiType", aiType)
                .queryParam("result", result)
                .queryParam("page", 1)
                .build().encode().toUriString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
