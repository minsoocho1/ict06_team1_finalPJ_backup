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

    // service 주입
    private final AdAiSecretaryService adAiSecretaryService;

    // AI 비서 관리자 대시보드 화면 조회
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

    // 대시보드 최근 로그 CSV 다운로드
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

    // AI RAG 지식베이스 관리 화면 조회
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
            @RequestParam(defaultValue = "") String docKeyword,
            @RequestParam(defaultValue = "1") int docPage,
            Model model
    ) {
        // 1. 지식 등록 요청 데이터 조회 및 페이지 처리
        List<KnowledgeResponseDto> requestManagementRequests = adAiSecretaryService.getKnowledgeRequestsForAdmin(
                requestStartDate, requestEndDate, requestStatus, requestType, requestCategory
        );
        List<KnowledgeResponseDto> allKnowledgeRequests = adAiSecretaryService.getKnowledgeRequestsForAdmin(
                "", "", "", "", ""
        );
        List<Map<String, Object>> documentManagementRows = adAiSecretaryService.getDocumentManagementRows(
                allKnowledgeRequests,
                docStage,
                accessLevel,
                docKeyword
        );

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
        model.addAttribute("selectedDocKeyword", docKeyword);

        // 3. 문서 관리 하단 페이지
        int docPageSize = 10;
        int docTotalCount = 128;
        int docTotalPages = (int) Math.ceil((double) docTotalCount / docPageSize);

        model.addAttribute("docPage", docPage);
        model.addAttribute("docPageSize", docPageSize);
        model.addAttribute("docTotalPages", docTotalPages);
        model.addAttribute("docTotalCount", docTotalCount);

        // 4. 공통 UI 옵션 및 하단 데이터 주입
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

        model.addAttribute("accessBlockLogs", adAiSecretaryService.getAccessBlockLogs());
        return "admin/aiSecretary/adAiRagManage";
    }

    @GetMapping("/rag/documents/csv")
    public ResponseEntity<byte[]> downloadDocumentManagementCsv(
            @RequestParam(defaultValue = "") String docStage,
            @RequestParam(defaultValue = "") String accessLevel,
            @RequestParam(defaultValue = "") String docKeyword
    ) {
        byte[] csvBytes = adAiSecretaryService.downloadDocumentManagementCsv(docStage, accessLevel, docKeyword);
        String fileName = "ai_rag_documents_" + LocalDate.now(ZoneId.of("Asia/Seoul")) + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes == null ? new byte[0] : csvBytes);
    }

    @PostMapping("/rag/documents/{documentId}/detail")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateDocumentManagementDetail(
            @PathVariable Integer documentId,
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String requestType,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String targetDept,
            @RequestParam(defaultValue = "") String adminComment
    ) {
        return ResponseEntity.ok(
                adAiSecretaryService.updateDocumentManagementDetail(
                        documentId,
                        title,
                        requestType,
                        category,
                        targetDept,
                        adminComment
                )
        );
    }

    // 사내 지식 등록 요청 승인/반려 처리
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

    // 관리자 직접 RAG 문서 등록 처리
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

    // AI 보안 및 접근 권한 관리 화면 조회
    @GetMapping("/security")
    public String aiSecurity(
            // ==========================================
            // [그룹 1] 문서 공개 정책(Policy) 관련 검색 및 페이지 파라미터
            // ==========================================
            @RequestParam(defaultValue = "") String policyStartDate,
            @RequestParam(defaultValue = "") String policyEndDate,
            @RequestParam(defaultValue = "") String selectedPolicyDocType,
            @RequestParam(defaultValue = "") String selectedPolicyAccessLevel,
            @RequestParam(defaultValue = "") String selectedPolicyActiveStatus,
            @RequestParam(defaultValue = "1") int policyPage,

            // ==========================================
            // [그룹 2] 권한 차단 로그(Block Log) 관련 검색 및 페이지 파라미터
            // ==========================================
            @RequestParam(defaultValue = "") String blockStartDate,
            @RequestParam(defaultValue = "") String blockEndDate,
            @RequestParam(defaultValue = "") String selectedBlockDept,
            @RequestParam(defaultValue = "") String selectedBlockReason,
            @RequestParam(defaultValue = "1") int blockPage,
            Model model
    ) {
        // 1. 상단 통계 요약용 대시보드 카드 데이터 바인딩
        model.addAttribute("policySummary", List.of(
                Map.of("label", "전체 공개 문서", "value", "18", "description", "모든 직원이 접근 가능한 문서입니다."),
                Map.of("label", "조건 조합 문서", "value", "12", "description", "선택한 조건에 따라 접근 대상이 달라지는 문서입니다."),
                Map.of("label", "관리자 전용 문서", "value", "8", "description", "관리자만 접근 가능한 문서입니다."),
                Map.of("label", "접근 차단 로그", "value", "5", "description", "권한이 맞지 않아 차단된 기록입니다.")
        ));

        // 2. 문서 정책 검색 필터용 드롭다운(Select Box) 옵션 리스트
        model.addAttribute("documentTypeOptions", List.of("사내 규정", "업무 매뉴얼", "FAQ", "서비스 이용 안내", "기타"));
        model.addAttribute("accessLevelOptions", List.of("전체 공개", "조건 조합", "관리자 전용"));
        model.addAttribute("activeStatusOptions", List.of("ACTIVE", "INACTIVE"));

        // 3. 문서 정책 그리드 리스트에 뿌려줄 데이터 테이블 가짜 데이터 (추후 DB 조회 결과로 대체할 영역)
        model.addAttribute("documentPolicies", List.of(
                Map.of("title", "전체 공개 문서", "documentType", "전체 공개", "accessLevel", "전체 공개", "target", "전체 직원", "activeStatus", "ACTIVE", "activeLabel", "활성", "updatedAt", "2026-05-09"),
                Map.of("title", "조건 조합 문서", "documentType", "조건 조합", "accessLevel", "조건 조합", "target", "TEAM_LEADER, ADMIN", "activeStatus", "ACTIVE", "activeLabel", "활성", "updatedAt", "2026-05-08"),
                Map.of("title", "관리자 전용 문서", "documentType", "관리자 전용", "accessLevel", "관리자 전용", "target", "ADMIN", "activeStatus", "INACTIVE", "activeLabel", "비활성", "updatedAt", "2026-05-07")
        ));

        // 4. 문서 정책 목록의 페이지 정보 바인딩
        model.addAttribute("policyPage", policyPage);
        model.addAttribute("policyPageSize", 10);
        model.addAttribute("policyTotalCount", 28);
        model.addAttribute("policyTotalPages", 3);

        // 5. 사용자가 선택했던 검색 조건을 유지하기 위해 값을 다시 모델에 주입 (화면 input/select 태그 value 매핑용)
        model.addAttribute("selectedPolicyDocType", selectedPolicyDocType);
        model.addAttribute("selectedPolicyAccessLevel", selectedPolicyAccessLevel);
        model.addAttribute("selectedPolicyActiveStatus", selectedPolicyActiveStatus);
        model.addAttribute("policyStartDate", policyStartDate);
        model.addAttribute("policyEndDate", policyEndDate);

        // 6. 권한 차단 로그 필터 셀렉트 박스 옵션
        model.addAttribute("blockReasonOptions", List.of("권한 조건 불일치", "비공개 문서 접근", "잘못된 부서 접근", "관리자 검토 필요"));

        // 7. 하단 영역에 출력할 실제 권한 차단 로그 리스트 가짜 데이터
        model.addAttribute("accessBlockLogs", List.of(
                Map.of("user", "홍길동", "dept", "인사팀", "documentTitle", "근태 관리 문서", "reason", "권한 조건 불일치", "createdAt", "2026-05-11 14:22"),
                Map.of("user", "김철수", "dept", "개발1팀(BE)", "documentTitle", "보안 서약 문서", "reason", "관리자 검토 필요", "createdAt", "2026-05-11 13:40")
        ));

        // 8. 권한 차단 로그 목록의 페이지 및 선택한 필터 정보 바인딩
        model.addAttribute("blockPage", blockPage);
        model.addAttribute("blockPageSize", 10);
        model.addAttribute("blockTotalCount", 2);
        model.addAttribute("blockTotalPages", 1);
        model.addAttribute("blockStartDate", blockStartDate);
        model.addAttribute("blockEndDate", blockEndDate);
        model.addAttribute("selectedBlockDept", selectedBlockDept);
        model.addAttribute("selectedBlockReason", selectedBlockReason);

        // 9. 관리자 모달창 조건 설정 팝업에서 사용할 공통 인사 기준 데이터셋
        // 부서(Department) 리스트 옵션
        model.addAttribute("departmentOptions", List.of(
                Map.of("id", 1, "name", "경영지원팀"),
                Map.of("id", 2, "name", "인사팀"),
                Map.of("id", 3, "name", "개발1팀(BE)"),
                Map.of("id", 4, "name", "개발2팀(FE)"),
                Map.of("id", 5, "name", "디자인팀")
        ));

        // 역할 권한(Role) 리스트 옵션
        model.addAttribute("roleOptions", List.of(
                Map.of("id", 1, "name", "ADMIN"),
                Map.of("id", 2, "name", "TEAM_LEADER"),
                Map.of("id", 3, "name", "USER")
        ));

        // 직위/직급(Position) 리스트 옵션
        model.addAttribute("positionOptions", List.of(
                Map.of("id", 1, "name", "임원"),
                Map.of("id", 2, "name", "사원"),
                Map.of("id", 3, "name", "주임"),
                Map.of("id", 4, "name", "대리"),
                Map.of("id", 5, "name", "책임"),
                Map.of("id", 6, "name", "수석")
        ));

        // 인사 등급(Grade) 리스트 옵션
        model.addAttribute("gradeOptions", List.of(
                Map.of("id", "G1", "name", "G1"),
                Map.of("id", "G2", "name", "G2"),
                Map.of("id", "G3", "name", "G3"),
                Map.of("id", "G4", "name", "G4"),
                Map.of("id", "G5", "name", "G5")
        ));

        // 10. 최종 화면 매핑 파일 반환 (src/main/resources/templates/admin/aiSecretary/adAiSecurity.html 연결)
        return "admin/aiSecretary/adAiSecurity";
    }

    /* [helper 함수] ----------------------------------------------------- */
    // 선택한 날짜 필터 조건에 따라 화면에 표시할 기간 라벨 문자열을 생성
    private String buildDateFilterLabel(int period, String startDate, String endDate) {
        // [1] 사용자가 시작일과 종료일을 직접 입력/선택한 경우
        // hasText()를 통해 공백이나 null이 아닌 유효한 문자열인지 검증
        if (hasText(startDate) && hasText(endDate)) {
            return "Period: " + startDate + " ~ " + endDate;
        }

        // [2] 직접 입력한 날짜 범위가 없고, 고정 기간 선택 중 '최근 30일'을 선택한 경우
        if (period == 30) {
            return "Period: Recent 30 days";
        }

        // [3] 위 조건에 해당하지 않는 경우 (기본값: 최근 7일)
        return "Period: Recent 7 days";
    }

    // 입력된 문자열이 null이 아니고 공백을 제외한 실제 유효한 텍스트를 포함하고 있는지 검사
    private boolean hasText(String value) {
        // '실제 글자가 존재하는 상태'로 판단되면 true를 반환
        // 값이 null이거나 빈 값이면 false를 반환
        return value != null && !value.isBlank();
    }

    // 대시보드의 날짜 필터 초기화 후 이동할 URL 주소를 생성
    private String buildDateFilterResetUrl(int period, String department, String aiType, String result) {
        return UriComponentsBuilder.fromPath("/admin/AiSecretary/dashboard") // [1] 기본이 되는 베이스 주소(Path) 설정
                .queryParam("period", 7)        // [2] 기간은 기본값인 7로 강제 세팅
                .queryParam("department", department)  // [3] 기존에 선택되어 있던 부서 값을 파라미터로 이어받아 주소에 그대로 유지
                .queryParam("aiType", aiType)          // [4] 기존 AI 유형 선택 값 유지
                .queryParam("result", result)          // [5] 기존 처리 결과(성공/실패 등) 값 유지
                .queryParam("page", 1)          // [6] 페이지 초기화 (첫 페이지로 초기화)
                .build()          // [7] 설정한 패스와 파라미터들을 조합하여 하나의 URI 객체로 빌드
                .encode()         // [8] 주소값의 공백이나 특수문자가 포함될 경우 깨지지 않도록 UTF-8로 안전하게 인코딩
                .toUriString();   // [9] 최종 완성된 주소를 텍스트(String) 형태로 변환하여 반환
    }
}

