/**
 * @FileName : AdAiSecretaryController.java
 * @Description : 관리자 AI 사내포털 화면 및 RAG 관리 요청 처리 컨트롤러
 * @Author : 송혜진
 * @Date : 2026. 04. 17
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.17    송혜진        최초 생성 (관리자 AI 사내포털 화면 매핑 추가)
 * @ 2026.05.14    송혜진        AI 비서 운영 대시보드 조회 및 로그 다운로드 요청 처리 추가
 * @ 2026.05.20    송혜진        지식 베이스 및 RAG 관리 화면 매핑 추가
 * @ 2026.05.22    송혜진        자료 등록 요청 승인/반려 처리 및 관리자 최종 권한 조건 전달 반영
 * @ 2026.05.22    송혜진        관리자 새 문서 업로드 및 RAG 문서 활성화 요청 처리 흐름 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.controller;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.DocumentActivationResultDto;
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

    // service ?몄텧
    private final AdAiSecretaryService adAiSecretaryService;

    // AI 鍮꾩꽌 愿由ъ옄 ??쒕낫???붾㈃ 議고쉶
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

    // ??쒕낫??理쒓렐 濡쒓렇 CSV ?ㅼ슫濡쒕뱶
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

    // AI RAG 吏?앸쿋?댁뒪 愿由??붾㈃ 議고쉶
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
        // 1. 吏???깅줉 ?붿껌 ?곗씠??議고쉶 諛??섏씠吏?泥섎━
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

        // 2. 臾몄꽌 愿由?寃??議곌굔 蹂듭썝
        model.addAttribute("docStartDate", docStartDate);
        model.addAttribute("docEndDate", docEndDate);
        model.addAttribute("selectedDocStage", docStage);
        model.addAttribute("selectedDocCategory", docCategory);
        model.addAttribute("selectedDocType", docType);
        model.addAttribute("selectedAccessLevel", accessLevel);

        // 3. 臾몄꽌 愿由??붾? ?섏씠吏?
        int docPageSize = 10;
        int docTotalCount = 128;
        int docTotalPages = (int) Math.ceil((double) docTotalCount / docPageSize);

        model.addAttribute("docPage", docPage);
        model.addAttribute("docPageSize", docPageSize);
        model.addAttribute("docTotalPages", docTotalPages);
        model.addAttribute("docTotalCount", docTotalCount);

        // 4. 怨듯넻 UI ?듭뀡 諛??붾? ?곗씠??媛?몄엯
        model.addAttribute("embeddingSummary", List.of(
                Map.of("title", "전체 임베딩", "count", 27, "description", "전체 임베딩 작업의 현재 상태입니다."),
                Map.of("title", "임베딩 진행", "count", 4, "description", "현재 문서를 임베딩하는 중입니다."),
                Map.of("title", "임베딩 대기", "count", 6, "description", "대기 중인 임베딩 작업입니다."),
                Map.of("title", "RAG 반영", "count", 18, "description", "RAG 반영 대상 현황입니다.")
        ));

        model.addAttribute("requestTypeOptions", List.of("사내 규정", "업무 매뉴얼", "FAQ", "서비스 이용 안내", "기타"));
        model.addAttribute("categoryOptions", List.of("근태", "인사", "전자결재", "교육", "복지", "서비스", "보안", "기타"));

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
                Map.of("id", 1, "name", "임원"),
                Map.of("id", 2, "name", "사원"),
                Map.of("id", 3, "name", "주임"),
                Map.of("id", 4, "name", "대리"),
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

    // ?щ궡 吏???깅줉 ?붿껌 ?뱀씤/諛섎젮 泥섎━
    @PostMapping("/rag/knowledge-requests/{requestId}/review")
    public String reviewKnowledgeRequest(
            @PathVariable Long requestId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String adminComment,
            @RequestParam(defaultValue = "") String targetDept,
            @AuthenticationPrincipal PrincipalDetails principal,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("reviewErrorMessage", "로그인이 필요합니다.");
            return "redirect:/admin/AiSecretary/rag";
        }

        try {
            KnowledgeResponseDto reviewedRequest = adAiSecretaryService.reviewKnowledgeRequest(
                    requestId, status, adminComment, principal.getEmpNo(), targetDept
            );
            redirectAttributes.addFlashAttribute("reviewSuccessMessage",
                    reviewedRequest == null
                            ? "자료 등록 요청을 처리했습니다."
                            : "자료 등록 요청을 " + reviewedRequest.getStatusLabel() + " 처리했습니다.");
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

    // 愿由ъ옄 吏곸젒 RAG 臾몄꽌 ?깅줉 泥섎━
    @PostMapping("/rag/documents/direct")
    public String createDirectRagDocument(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String requestType,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String reason,
            @RequestParam(defaultValue = "") String sampleQuestion,
            @RequestParam(defaultValue = "") String referenceUrl,
            @RequestParam(defaultValue = "CUSTOM") String accessLevel,
            @RequestParam(defaultValue = "") String targetDept,
            @AuthenticationPrincipal PrincipalDetails principal,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("reviewErrorMessage", "로그인이 필요합니다.");
            return "redirect:/admin/AiSecretary/rag";
        }

        try {
            Integer docId = adAiSecretaryService.createDirectRagDocument(
                    title,
                    requestType,
                    category,
                    reason,
                    sampleQuestion,
                    referenceUrl,
                    accessLevel,
                    targetDept,
                    principal.getEmpNo()
            );

            redirectAttributes.addFlashAttribute("reviewSuccessMessage",
                    docId == null
                            ? "새 문서가 등록되었습니다."
                            : "새 문서가 등록되었습니다. 문서 ID: " + docId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("reviewErrorMessage", e.getMessage());
        } catch (Exception e) {
            String message = e.getMessage();
            if (e instanceof org.springframework.web.server.ResponseStatusException responseStatusException && responseStatusException.getReason() != null) {
                message = responseStatusException.getReason();
            }
            redirectAttributes.addFlashAttribute("reviewErrorMessage",
                    (message == null || message.isBlank()) ? "새 문서 등록에 실패했습니다." : message);
        }

        return "redirect:/admin/AiSecretary/rag";
    }

    @PostMapping("/rag/documents/{documentId}/activation")
    @ResponseBody
    public ResponseEntity<DocumentActivationResultDto> toggleDocumentActivation(
            @PathVariable Integer documentId,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(DocumentActivationResultDto.builder()
                    .success(false)
                    .message("로그인이 필요합니다.")
                    .stage("")
                    .stageLabel("")
                    .build());
        }

        try {
            DocumentActivationResultDto result = adAiSecretaryService.activateOrToggleDocument(
                    documentId,
                    principal.getEmpNo()
            );
            return ResponseEntity.ok(result);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(DocumentActivationResultDto.builder()
                    .success(false)
                    .message(e.getReason() == null || e.getReason().isBlank()
                            ? "문서 상태를 변경할 수 없습니다."
                            : e.getReason())
                    .stage("")
                    .stageLabel("")
                    .build());
        } catch (Exception e) {
            String message = e.getMessage();
            return ResponseEntity.internalServerError().body(DocumentActivationResultDto.builder()
                    .success(false)
                    .message(message == null || message.isBlank()
                            ? "문서 상태 변경 중 오류가 발생했습니다."
                            : message)
                    .stage("")
                    .stageLabel("")
                    .build());
        }
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

    // AI 蹂댁븞 諛??묎렐 沅뚰븳 愿由??붾㈃ 議고쉶
    @GetMapping("/security")
    public String aiSecurity(
            // ==========================================
            // [洹몃９ 1] 臾몄꽌 怨듦컻 ?뺤콉(Policy) 愿??寃??諛??섏씠吏??뚮씪誘명꽣
            // ==========================================
            @RequestParam(defaultValue = "") String policyStartDate,
            @RequestParam(defaultValue = "") String policyEndDate,
            @RequestParam(defaultValue = "") String selectedPolicyDocType,
            @RequestParam(defaultValue = "") String selectedPolicyAccessLevel,
            @RequestParam(defaultValue = "") String selectedPolicyActiveStatus,
            @RequestParam(defaultValue = "1") int policyPage,

            // ==========================================
            // [洹몃９ 2] 沅뚰븳 李⑤떒 濡쒓렇(Block Log) 愿??寃??諛??섏씠吏??뚮씪誘명꽣
            // ==========================================
            @RequestParam(defaultValue = "") String blockStartDate,
            @RequestParam(defaultValue = "") String blockEndDate,
            @RequestParam(defaultValue = "") String selectedBlockDept,
            @RequestParam(defaultValue = "") String selectedBlockReason,
            @RequestParam(defaultValue = "1") int blockPage,
            Model model
    ) {
        // 1. ?곷떒 ?듦퀎 ?꾪솴????쒕낫??移대뱶) ?곗씠??諛붿씤??
        model.addAttribute("policySummary", List.of(
                Map.of("label", "전체 공개 문서", "value", "18", "description", "모든 직원이 접근 가능한 문서입니다."),
                Map.of("label", "조건 조합 문서", "value", "12", "description", "선택한 조건에 따라 접근 대상이 달라지는 문서입니다."),
                Map.of("label", "관리자 전용 문서", "value", "8", "description", "관리자만 접근 가능한 문서입니다."),
                Map.of("label", "접근 차단 로그", "value", "5", "description", "권한이 맞지 않아 차단된 기록입니다.")
        ));

        // 2. 臾몄꽌 ?뺤콉 寃???꾪꽣???쒕∼?ㅼ슫(Select Box) ?듭뀡 由ъ뒪??
        model.addAttribute("documentTypeOptions", List.of("사내 규정", "업무 매뉴얼", "FAQ", "서비스 이용 안내", "기타"));
        model.addAttribute("accessLevelOptions", List.of("전체 공개", "조건 조합", "관리자 전용"));
        model.addAttribute("activeStatusOptions", List.of("ACTIVE", "INACTIVE"));

        // 3. 臾몄꽌 ?뺤콉 洹몃━??由ъ뒪?몄뿉 肉뚮젮以??곗씠???뚯씠釉?媛吏??곗씠??(?ν썑 DB 議고쉶 寃곌낵濡??泥대맆 ?곸뿭)
        model.addAttribute("documentPolicies", List.of(
                Map.of("title", "전체 공개 문서", "documentType", "전체 공개", "accessLevel", "전체 공개", "target", "전체 직원", "activeStatus", "ACTIVE", "activeLabel", "활성", "updatedAt", "2026-05-09"),
                Map.of("title", "조건 조합 문서", "documentType", "조건 조합", "accessLevel", "조건 조합", "target", "TEAM_LEADER, ADMIN", "activeStatus", "ACTIVE", "activeLabel", "활성", "updatedAt", "2026-05-08"),
                Map.of("title", "관리자 전용 문서", "documentType", "관리자 전용", "accessLevel", "관리자 전용", "target", "ADMIN", "activeStatus", "INACTIVE", "activeLabel", "비활성", "updatedAt", "2026-05-07")
        ));

        // 4. 臾몄꽌 ?뺤콉 紐⑸줉???섏씠吏??뺣낫 諛붿씤??
        model.addAttribute("policyPage", policyPage);
        model.addAttribute("policyPageSize", 10);
        model.addAttribute("policyTotalCount", 28);
        model.addAttribute("policyTotalPages", 3);

        // 5. ?ъ슜?먭? ?좏깮?덈뜕 寃??議곌굔???좎??섍린 ?꾪빐 媛??ㅼ떆 紐⑤뜽??二쇱엯 (?붾㈃ input/select ?쒓렇 value 留ㅽ븨??
        model.addAttribute("selectedPolicyDocType", selectedPolicyDocType);
        model.addAttribute("selectedPolicyAccessLevel", selectedPolicyAccessLevel);
        model.addAttribute("selectedPolicyActiveStatus", selectedPolicyActiveStatus);
        model.addAttribute("policyStartDate", policyStartDate);
        model.addAttribute("policyEndDate", policyEndDate);

        // 6. 沅뚰븳 李⑤떒 濡쒓렇 ?꾪꽣????됲듃 諛뺤뒪 ?듭뀡
        model.addAttribute("blockReasonOptions", List.of("권한 조건 불일치", "비공개 문서 접근", "잘못된 부서 접근", "관리자 검토 필요"));

        // 7. ?섎떒 ?곸뿭???몄텧???ㅼ젣 沅뚰븳 李⑤떒 濡쒓렇 由ъ뒪??媛吏??곗씠??
        model.addAttribute("accessBlockLogs", List.of(
                Map.of("user", "홍길동", "dept", "인사팀", "documentTitle", "근태 관리 문서", "reason", "권한 조건 불일치", "createdAt", "2026-05-11 14:22"),
                Map.of("user", "김철수", "dept", "개발1팀(BE)", "documentTitle", "보안 서약 문서", "reason", "관리자 검토 필요", "createdAt", "2026-05-11 13:40")
        ));

        // 8. 沅뚰븳 李⑤떒 濡쒓렇 紐⑸줉???섏씠吏?諛??좎????꾪꽣 ?뺣낫 諛붿씤??
        model.addAttribute("blockPage", blockPage);
        model.addAttribute("blockPageSize", 10);
        model.addAttribute("blockTotalCount", 2);
        model.addAttribute("blockTotalPages", 1);
        model.addAttribute("blockStartDate", blockStartDate);
        model.addAttribute("blockEndDate", blockEndDate);
        model.addAttribute("selectedBlockDept", selectedBlockDept);
        model.addAttribute("selectedBlockReason", selectedBlockReason);

        // 9. 愿由ъ옄 紐⑤떖李?議곌굔 ?ㅼ젙 ?앹뾽) ?깆뿉???ъ슜??怨듯넻 ?몄궗 湲곗? ?곗씠?곗뀑
        // 遺??Department) 由ъ뒪???듭뀡
        model.addAttribute("departmentOptions", List.of(
                Map.of("id", 1, "name", "경영지원팀"),
                Map.of("id", 2, "name", "인사팀"),
                Map.of("id", 3, "name", "개발1팀(BE)"),
                Map.of("id", 4, "name", "개발2팀(FE)"),
                Map.of("id", 5, "name", "디자인팀")
        ));

        // ??븷 沅뚰븳(Role) 由ъ뒪???듭뀡
        model.addAttribute("roleOptions", List.of(
                Map.of("id", 1, "name", "ADMIN"),
                Map.of("id", 2, "name", "TEAM_LEADER"),
                Map.of("id", 3, "name", "USER")
        ));

        // 吏곸쐞/吏곴툒(Position) 由ъ뒪???듭뀡
        model.addAttribute("positionOptions", List.of(
                Map.of("id", 1, "name", "임원"),
                Map.of("id", 2, "name", "사원"),
                Map.of("id", 3, "name", "주임"),
                Map.of("id", 4, "name", "대리"),
                Map.of("id", 5, "name", "책임"),
                Map.of("id", 6, "name", "수석")
        ));

        // ?몄궗 ?깃툒(Grade) 由ъ뒪???듭뀡
        model.addAttribute("gradeOptions", List.of(
                Map.of("id", "G1", "name", "G1"),
                Map.of("id", "G2", "name", "G2"),
                Map.of("id", "G3", "name", "G3"),
                Map.of("id", "G4", "name", "G4"),
                Map.of("id", "G5", "name", "G5")
        ));

        // 10. 理쒖쥌 ?붾㈃ 留ㅽ븨 ?뚯씪 諛섑솚 (src/main/resources/templates/admin/aiSecretary/adAiSecurity.html ?몄텧)
        return "admin/aiSecretary/adAiSecurity";
    }

    /* [helper ?⑥닔] ----------------------------------------------------- */
    // ?좏깮???좎쭨 ?꾪꽣 議곌굔???곕씪 ?붾㈃???쒖떆??湲곌컙 ?쇰꺼(Label) 臾몄옄?댁쓣 ?앹꽦
    private String buildDateFilterLabel(int period, String startDate, String endDate) {
        // [1] ?ъ슜?먭? ?쒖옉?쇨낵 醫낅즺?쇱쓣 ????吏곸젒 ?낅젰/ ?좏깮??寃쎌슦
        // hasText()瑜??듯빐 怨듬갚?대굹 null???꾨땶 ?좏슚??臾몄옄?댁씤吏 寃利?
        if (hasText(startDate) && hasText(endDate)) {
            return "Period: " + startDate + " ~ " + endDate;
        }

        // [2] 吏곸젒 ?낅젰???좎쭨 踰붿쐞媛 ?녾퀬, 怨좎젙 湲곌컙 ?좏깮 以?'理쒓렐 30?????좏깮??寃쎌슦
        if (period == 30) {
            return "Period: Recent 30 days";
        }

        // [3] ??紐⑤뱺 議곌굔???대떦?섏? ?딆? 寃쎌슦 (湲곕낯 媛?理쒓렐 7??
        return "Period: Recent 7 days";
    }

    // ?낅젰??臾몄옄?댁씠 null???꾨땲怨? 怨듬갚(Space)???쒖쇅???ㅼ젣 ?좏슚???띿뒪?몃? ?ы븿?섍퀬 ?덈뒗吏 寃??
    private boolean hasText(String value) {
        // '?ㅼ젣 湲?먭? 議댁옱?섎뒗 ?곹깭'濡??먮떒?섎㈃ true瑜?諛섑솚
        // 媛믪씠 null?닿굅??鍮?媛믪씠硫?fulse瑜?諛섑솚
        return value != null && !value.isBlank();
    }

    // ??쒕낫?쒖쓽 ?좎쭨 ?꾪꽣 珥덇린?????대룞??URL 二쇱냼瑜??앹꽦
    private String buildDateFilterResetUrl(int period, String department, String aiType, String result) {
        return UriComponentsBuilder.fromPath("/admin/AiSecretary/dashboard") // [1] 湲곕낯???섎뒗 踰좎씠??二쇱냼(Path) ?ㅼ젙
                .queryParam("period", 7)        // [2] 湲곌컙??湲곕낯 媛믪씤 7濡?媛뺤젣 ?명똿
                .queryParam("department", department)  // [3] 湲곗〈???좏깮?섏뼱 ?덈뜕 遺??媛믪? ?뚮씪誘명꽣濡??댁뼱諛쏆븘 二쇱냼??洹몃?濡??좎?
                .queryParam("aiType", aiType)          // [4] 湲곗〈 AI ?좏삎 ?좏깮 媛??좎?
                .queryParam("result", result)          // [5] 湲곗〈 泥섎━ 寃곌낵(?깃났/ ?ㅽ뙣 ?? 媛??좎?
                .queryParam("page", 1)          // [6] ?섏씠吏?珥덇린??(泥??섏씠吏濡?珥덇린??
                .build()          // [7] ?ㅼ젙???⑥뒪? ?뚮씪誘명꽣?ㅼ쓣 議고빀?섏뿬 ?섎굹??URI 媛앹껜濡?鍮뚮뱶
                .encode()         // [8] 二쇱냼李쎌쓽 ?쒓??대굹 ?뱀닔臾몄옄媛 ?ы븿 ??寃쎌슦 源⑥?吏 ?딅룄濡?UTF-8濡??덉쟾?섍쾶 ?몄퐫??
                .toUriString();   // [9] 理쒖쥌 ?꾩꽦 ??二쇱냼瑜??띿뒪??String) ?뺥깭濡?蹂?섑븯??諛섑솚
    }
}

