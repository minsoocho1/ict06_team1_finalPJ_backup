/**
 * @FileName : AdAiSecretaryController.java
 * @Description : 사내 AI 포털 관리자 페이지 위한 관리자 컨트롤러
 * @Author : 송혜진
 * @Date : 2026. 05. 12
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -----------------------------------------------
 * @ 2026.05.12    송혜진        최초 생성 및 AI 데이터 운영 대시보드 기본 구조 설계

 */


package com.ict06.team1_fin_pj.domain.aiSecretary.controller;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardResponseDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.service.AdAiSecretaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RequestMapping("/admin/AiSecretary")
@Controller
@RequiredArgsConstructor
public class AdAiSecretaryController {

    private final AdAiSecretaryService adAiSecretaryService;

    // 관리자 AI 데이터 운영 대시보드
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
                period,
                startDate,
                endDate,
                department,
                aiType,
                result,
                page
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
        String activeDateFilterLabel = buildDateFilterLabel(period, startDate, endDate);
        String dateFilterResetUrl = buildDateFilterResetUrl(period, department, aiType, result);
        model.addAttribute("activeDateFilterLabel", activeDateFilterLabel);
        model.addAttribute("dateFilterResetUrl", dateFilterResetUrl);

        return "admin/aiSecretary/adAiDashboard";
    }

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

    @GetMapping("/rag")
    public String aiRagManage(
            /*
             * [자료 등록 요청 목록 필터]
             *
             * requestStartDate / requestEndDate:
             * - 사용자가 직접 날짜 범위를 선택하는 방식
             *
             * requestStatus:
             * - AI_KNOWLEDGE_REQUEST.status 기준
             * - PENDING / APPROVED / REJECTED / REFLECTED
             */
            @RequestParam(defaultValue = "") String requestStartDate,
            @RequestParam(defaultValue = "") String requestEndDate,
            @RequestParam(defaultValue = "") String requestStatus,
            @RequestParam(defaultValue = "") String requestType,
            @RequestParam(defaultValue = "") String requestCategory,
            @RequestParam(defaultValue = "1") int requestPage,

            /*
             * [전체 등록 문서 리스트 필터]
             *
             * docStartDate / docEndDate:
             * - 등록일 또는 최근 상태 변경일 기준 날짜 범위
             *
             * docStage:
             * - DOCUMENT.current_stage 기준
             */
            @RequestParam(defaultValue = "") String docStartDate,
            @RequestParam(defaultValue = "") String docEndDate,
            @RequestParam(defaultValue = "") String docStage,
            @RequestParam(defaultValue = "") String docCategory,
            @RequestParam(defaultValue = "") String docType,
            @RequestParam(defaultValue = "") String accessLevel,
            @RequestParam(defaultValue = "1") int docPage,

            Model model
    ) {
        /*
         * [자료 등록 요청 목록 필터 선택값]
         */
        model.addAttribute("requestStartDate", requestStartDate);
        model.addAttribute("requestEndDate", requestEndDate);
        model.addAttribute("selectedRequestStatus", requestStatus);
        model.addAttribute("selectedRequestType", requestType);
        model.addAttribute("selectedRequestCategory", requestCategory);

        /*
         * [자료 등록 요청 목록 페이징]
         *
         * 현재는 더미값.
         * 추후 AI_KNOWLEDGE_REQUEST 조회 결과 기반으로 교체한다.
         */
        model.addAttribute("requestPage", requestPage);
        model.addAttribute("requestTotalPages", 3);
        model.addAttribute("requestTotalCount", 21);

        /*
         * [전체 등록 문서 리스트 필터 선택값]
         */
        model.addAttribute("docStartDate", docStartDate);
        model.addAttribute("docEndDate", docEndDate);
        model.addAttribute("selectedDocStage", docStage);
        model.addAttribute("selectedDocCategory", docCategory);
        model.addAttribute("selectedDocType", docType);
        model.addAttribute("selectedAccessLevel", accessLevel);

        /*
         * [전체 등록 문서 리스트 페이징]
         *
         * 현재는 더미값.
         * 추후 DOCUMENT 조회 결과 기반으로 교체한다.
         *
         * 정책:
         * - 전체 등록 문서 리스트는 한 페이지에 10개씩 노출한다.
         */
        int docPageSize = 10;
        int docTotalCount = 128;
        int docTotalPages = (int) Math.ceil((double) docTotalCount / docPageSize);

        model.addAttribute("docPage", docPage);
        model.addAttribute("docPageSize", docPageSize);
        model.addAttribute("docTotalPages", docTotalPages);
        model.addAttribute("docTotalCount", docTotalCount);

        /*
         * [자료 등록 요청 필터 옵션]
         */
        model.addAttribute("requestTypeOptions", List.of(
                "업무 매뉴얼",
                "FAQ",
                "사내 규정",
                "서비스 이용 안내",
                "기타"
        ));

        /*
         * [조직/사원 관리 기반 권한 선택 옵션]
         *
         * 새 문서 업로드 시 기본 열람 권한의 기본 적용 대상을 선택하기 위해 사용한다.
         *
         * 실제 연동 시:
         * - department
         * - role
         * - position
         * - grade_code
         * 테이블에서 조회한다.
         */
        model.addAttribute("departmentOptions", List.of(
                Map.of("id", 1, "name", "경영본부"),
                Map.of("id", 2, "name", "개발본부"),
                Map.of("id", 3, "name", "경영지원팀"),
                Map.of("id", 4, "name", "인사팀"),
                Map.of("id", 5, "name", "개발1팀(BE)"),
                Map.of("id", 6, "name", "개발2팀(FE)"),
                Map.of("id", 7, "name", "디자인팀")
        ));

        model.addAttribute("roleOptions", List.of(
                Map.of("id", 1, "name", "ADMIN"),
                Map.of("id", 2, "name", "TEAM_LEADER"),
                Map.of("id", 3, "name", "USER")
        ));

        model.addAttribute("positionOptions", List.of(
                Map.of("id", 1, "name", "사원"),
                Map.of("id", 2, "name", "주임"),
                Map.of("id", 3, "name", "선임"),
                Map.of("id", 4, "name", "책임"),
                Map.of("id", 5, "name", "수석")
        ));

        model.addAttribute("gradeOptions", List.of(
                Map.of("id", "G1", "name", "일반 등급"),
                Map.of("id", "G2", "name", "중간 관리 등급"),
                Map.of("id", "G3", "name", "고급 관리 등급"),
                Map.of("id", "G4", "name", "시니어 등급"),
                Map.of("id", "G5", "name", "임원/본부 등급")
        ));

        model.addAttribute("categoryOptions", List.of(
                "근태",
                "전자결재",
                "인사",
                "교육",
                "복지",
                "시스템",
                "기타"
        ));

        /*
         * [전체 등록 문서 리스트 필터 옵션]
         */
        model.addAttribute("docTypeOptions", List.of(
                "사내 규정",
                "업무 매뉴얼",
                "FAQ",
                "서비스 이용 안내",
                "기타"
        ));

        model.addAttribute("docCategoryOptions", List.of(
                "근태",
                "전자결재",
                "인사",
                "교육",
                "복지",
                "시스템",
                "기타"
        ));

        model.addAttribute("accessLevelOptions", List.of(
                "전체 공개",
                "특정 부서",
                "팀장 이상",
                "관리자 전용"
        ));

        /*
         * [지식 베이스/RAG 상태 요약 카드]
         *
         * 화면 표시 기준:
         * - 상태 라벨은 직접 노출하지 않는다.
         * - title / count / description만 보여준다.
         * - description은 ? 아이콘 hover tooltip에서 표시한다.
         *
         * 실제 DB 조건:
         * - 처리 실패   : current_stage IN ('CHUNK_FAILED', 'EMBED_FAILED')
         * - 승인 대기   : current_stage = 'APPROVAL_PENDING'
         * - 임베딩 완료 : current_stage = 'REFLECTED'
         * - 임베딩 진행 : current_stage IN ('CHUNKING', 'EMBEDDING')
         * - 업로드 완료 : current_stage = 'UPLOADED'
         */
        model.addAttribute("embeddingSummary", List.of(
                Map.of(
                        "key", "FAILED",
                        "title", "처리 실패",
                        "count", "2",
                        "description", "청크 분리 또는 임베딩 중 오류가 발생하여 재처리가 필요한 문서입니다.",
                        "trend", "danger"
                ),
                Map.of(
                        "key", "APPROVAL_PENDING",
                        "title", "승인 대기",
                        "count", "7",
                        "description", "임베딩은 완료되었지만 관리자의 최종 반영 승인을 기다리는 문서입니다.",
                        "trend", "warning"
                ),
                Map.of(
                        "key", "REFLECTED",
                        "title", "임베딩 완료",
                        "count", "24",
                        "description", "문서가 최종 반영되어 챗봇/RAG 검색에서 실제 사용할 수 있는 상태입니다.",
                        "trend", "success"
                ),
                Map.of(
                        "key", "PROCESSING",
                        "title", "임베딩 진행",
                        "count", "5",
                        "description", "문서 청크 분리 또는 임베딩 생성 작업이 진행 중인 문서입니다.",
                        "trend", "primary"
                ),
                Map.of(
                        "key", "UPLOADED",
                        "title", "업로드 완료",
                        "count", "3",
                        "description", "문서는 업로드되었지만 아직 청크 분리나 임베딩 작업이 시작되지 않은 상태입니다.",
                        "trend", "secondary"
                )
        ));

        /*
         * [관리자 처리 기준 안내 모달 데이터]
         *
         * 목적:
         * - 각 문서 상태에서 관리자가 어떤 조치를 해야 하는지 안내한다.
         * - 실제 기능 연결 전에는 UI 가이드 역할을 한다.
         */
        model.addAttribute("adminActionGuides", List.of(
                Map.of(
                        "statusName", "처리 실패",
                        "stageCondition", "CHUNK_FAILED / EMBED_FAILED",
                        "priority", "높음",
                        "adminDecision", "오류 확인 및 재처리 여부 판단",
                        "mainAction", "실패 사유 확인 후 재시도 또는 보류",
                        "buttons", "오류 확인 / 재시도"
                ),
                Map.of(
                        "statusName", "승인 대기",
                        "stageCondition", "APPROVAL_PENDING",
                        "priority", "높음",
                        "adminDecision", "문서 반영 승인 여부 판단",
                        "mainAction", "문서 내용과 권한 확인 후 승인 또는 반려",
                        "buttons", "상세 / 승인 / 반려"
                ),
                Map.of(
                        "statusName", "업로드 완료",
                        "stageCondition", "UPLOADED",
                        "priority", "보통",
                        "adminDecision", "처리 시작 여부 확인",
                        "mainAction", "자동 처리 대기 또는 수동 처리 시작",
                        "buttons", "상세 / 처리 시작"
                ),
                Map.of(
                        "statusName", "임베딩 진행",
                        "stageCondition", "CHUNKING / EMBEDDING",
                        "priority", "낮음",
                        "adminDecision", "처리 진행 상태 모니터링",
                        "mainAction", "진행 로그 확인",
                        "buttons", "진행 로그"
                ),
                Map.of(
                        "statusName", "임베딩 완료",
                        "stageCondition", "REFLECTED",
                        "priority", "낮음",
                        "adminDecision", "반영 결과 및 권한 확인",
                        "mainAction", "문서 상세 확인 또는 권한 관리",
                        "buttons", "상세 / 권한 관리"
                )
        ));

        /*
         * [자료 등록 요청 목록]
         *
         * AI_KNOWLEDGE_REQUEST 테이블 확정 전 더미 데이터.
         *
         * 반려 상태는 이 목록에서 확인한다.
         */
        model.addAttribute("knowledgeRequests", List.of(
                Map.of(
                        "requester", "송혜진",
                        "requestType", "업무 매뉴얼",
                        "category", "근태",
                        "title", "조퇴 신청 기준 챗봇 반영 요청",
                        "status", "PENDING",
                        "statusLabel", "검토 대기",
                        "createdAt", "2026-05-11"
                ),
                Map.of(
                        "requester", "김민수",
                        "requestType", "FAQ",
                        "category", "전자결재",
                        "title", "결재 반려 후 재상신 절차 추가 요청",
                        "status", "APPROVED",
                        "statusLabel", "승인",
                        "createdAt", "2026-05-10"
                ),
                Map.of(
                        "requester", "박지은",
                        "requestType", "사내 규정",
                        "category", "인사",
                        "title", "인사평가 기준 챗봇 반영 요청",
                        "status", "REJECTED",
                        "statusLabel", "반려",
                        "createdAt", "2026-05-09"
                )
        ));


        /*
         * [전체 등록 문서 리스트]
         *
         * DOCUMENT 테이블 연동 전 더미 데이터.
         *
         * 주의:
         * - 실제 DB 연동 시에는 docPage / docPageSize 기준으로
         *   현재 페이지에 해당하는 10개 데이터만 조회한다.
         * - 이 목록의 상태는 DOCUMENT.current_stage 기준이다.
         * - 반려 상태는 이 목록이 아니라 자료 등록 요청 목록에서 확인한다.
         */
        model.addAttribute("documents", List.of(
                Map.of(
                        "createdAt", "2026-05-09",
                        "stage", "REFLECTED",
                        "stageLabel", "임베딩 완료",
                        "title", "근태관리 사용자 매뉴얼",
                        "category", "근태",
                        "documentType", "업무 매뉴얼",
                        "owner", "인사팀",
                        "accessLevel", "전체 공개",
                        "requestReason", "챗봇이 조퇴 신청 기준을 정확히 답변하지 못해 근태관리 사용자 매뉴얼을 반영했습니다.",
                        "questionExample", "조퇴 신청은 어디에서 하나요?"
                ),
                Map.of(
                        "createdAt", "2026-05-08",
                        "stage", "EMBEDDING",
                        "stageLabel", "임베딩 진행",
                        "title", "전자결재 승인 절차 FAQ",
                        "category", "전자결재",
                        "documentType", "FAQ",
                        "owner", "총무팀",
                        "accessLevel", "전체 공개",
                        "requestReason", "챗봇이 조퇴 신청 기준을 정확히 답변하지 못해 근태관리 사용자 매뉴얼을 반영했습니다.",
                        "questionExample", "조퇴 신청은 어디에서 하나요?"
                ),
                Map.of(
                        "createdAt", "2026-05-07",
                        "stage", "CHUNK_FAILED",
                        "stageLabel", "처리 실패",
                        "title", "인사평가 운영 가이드",
                        "category", "인사",
                        "documentType", "사내 규정",
                        "owner", "인사팀",
                        "accessLevel", "팀장 이상",
                        "requestReason", "챗봇이 조퇴 신청 기준을 정확히 답변하지 못해 근태관리 사용자 매뉴얼을 반영했습니다.",
                        "questionExample", "조퇴 신청은 어디에서 하나요?"
                )
        ));

        return "admin/aiSecretary/adAiRagManage";
    }

    // 관리자 보안 및 권한 제어
    @GetMapping("/security")
    public String aiSecurity(
            /*
             * [문서 권한 관리 필터]
             *
             * policyStartDate / policyEndDate:
             * - 권한 정책 최종 변경일 기준 조회 기간
             *
             * policyDocType:
             * - 사내 규정 / 업무 매뉴얼 / FAQ / 서비스 이용 안내 / 기타
             *
             * policyAccessLevel:
             * - 전체 공개 / 특정 부서 / 특정 역할 / 특정 직책 / 특정 등급 / 관리자 전용
             *
             * policyActiveStatus:
             * - ACTIVE / INACTIVE
             */
            @RequestParam(defaultValue = "") String policyStartDate,
            @RequestParam(defaultValue = "") String policyEndDate,
            @RequestParam(defaultValue = "") String policyDocType,
            @RequestParam(defaultValue = "") String policyAccessLevel,
            @RequestParam(defaultValue = "") String policyActiveStatus,
            @RequestParam(defaultValue = "1") int policyPage,

            /*
             * [비인가 접근 차단 로그 필터]
             *
             * blockStartDate / blockEndDate:
             * - 비인가 접근 시도 일시 기준 조회 기간
             *
             * blockDept:
             * - 사용자 소속 부서
             *
             * blockReason:
             * - 차단 사유
             */
            @RequestParam(defaultValue = "") String blockStartDate,
            @RequestParam(defaultValue = "") String blockEndDate,
            @RequestParam(defaultValue = "") String blockDept,
            @RequestParam(defaultValue = "") String blockReason,
            @RequestParam(defaultValue = "1") int blockPage,

            Model model
    ) {
        /*
         * [권한 정책 요약 카드]
         *
         * 추후 실제 연동 시:
         * - DOCUMENT 또는 DOCUMENT_PERMISSION_POLICY 기준으로 집계한다.
         */
        model.addAttribute("policySummary", List.of(
                Map.of(
                        "label", "전체 공개 문서",
                        "value", "18",
                        "description", "모든 직원이 챗봇/RAG 검색 결과로 접근할 수 있는 문서입니다.",
                        "status", "success"
                ),
                Map.of(
                        "label", "부서 제한 문서",
                        "value", "9",
                        "description", "특정 부서 직원만 접근 가능한 문서입니다.",
                        "status", "primary"
                ),
                Map.of(
                        "label", "직급 제한 문서",
                        "value", "6",
                        "description", "팀장 이상 또는 특정 직책 이상만 접근 가능한 문서입니다.",
                        "status", "warning"
                ),
                Map.of(
                        "label", "관리자 전용 문서",
                        "value", "4",
                        "description", "일반 사용자에게 노출되지 않고 관리자만 접근 가능한 문서입니다.",
                        "status", "danger"
                )
        ));

        /*
         * [문서 권한 관리 필터 선택값]
         */
        model.addAttribute("policyStartDate", policyStartDate);
        model.addAttribute("policyEndDate", policyEndDate);
        model.addAttribute("selectedPolicyDocType", policyDocType);
        model.addAttribute("selectedPolicyAccessLevel", policyAccessLevel);
        model.addAttribute("selectedPolicyActiveStatus", policyActiveStatus);

        /*
         * [문서 권한 관리 페이징]
         *
         * 정책:
         * - 한 페이지에 10개씩 노출
         */
        int policyPageSize = 10;
        int policyTotalCount = 36;
        int policyTotalPages = (int) Math.ceil((double) policyTotalCount / policyPageSize);

        model.addAttribute("policyPage", policyPage);
        model.addAttribute("policyPageSize", policyPageSize);
        model.addAttribute("policyTotalCount", policyTotalCount);
        model.addAttribute("policyTotalPages", policyTotalPages);

        /*
         * [비인가 접근 차단 로그 필터 선택값]
         */
        model.addAttribute("blockStartDate", blockStartDate);
        model.addAttribute("blockEndDate", blockEndDate);
        model.addAttribute("selectedBlockDept", blockDept);
        model.addAttribute("selectedBlockReason", blockReason);

        /*
         * [비인가 접근 차단 로그 페이징]
         *
         * 정책:
         * - 한 페이지에 10개씩 노출
         */
        int blockPageSize = 10;
        int blockTotalCount = 18;
        int blockTotalPages = (int) Math.ceil((double) blockTotalCount / blockPageSize);

        model.addAttribute("blockPage", blockPage);
        model.addAttribute("blockPageSize", blockPageSize);
        model.addAttribute("blockTotalCount", blockTotalCount);
        model.addAttribute("blockTotalPages", blockTotalPages);

        /*
         * [문서/권한 필터 옵션]
         */
        model.addAttribute("documentTypeOptions", List.of(
                "사내 규정",
                "업무 매뉴얼",
                "FAQ",
                "서비스 이용 안내",
                "기타"
        ));

        model.addAttribute("accessLevelOptions", List.of(
                "전체 공개",
                "특정 부서",
                "특정 역할",
                "특정 직책",
                "특정 등급",
                "관리자 전용"
        ));

        model.addAttribute("activeStatusOptions", List.of(
                "활성",
                "비활성"
        ));

        /*
         * [조직/사원 관리 기반 선택 옵션]
         *
         * 실제 연동 시:
         * - department
         * - role
         * - position
         * - grade_code
         * 테이블에서 조회한다.
         */
        model.addAttribute("departmentOptions", List.of(
                Map.of("id", 1, "name", "경영본부"),
                Map.of("id", 2, "name", "개발본부"),
                Map.of("id", 3, "name", "경영지원팀"),
                Map.of("id", 4, "name", "인사팀"),
                Map.of("id", 5, "name", "개발1팀(BE)"),
                Map.of("id", 6, "name", "개발2팀(FE)"),
                Map.of("id", 7, "name", "디자인팀")
        ));

        model.addAttribute("roleOptions", List.of(
                Map.of("id", 1, "name", "ADMIN"),
                Map.of("id", 2, "name", "TEAM_LEADER"),
                Map.of("id", 3, "name", "USER")
        ));

        model.addAttribute("positionOptions", List.of(
                Map.of("id", 1, "name", "사원"),
                Map.of("id", 2, "name", "주임"),
                Map.of("id", 3, "name", "선임"),
                Map.of("id", 4, "name", "책임"),
                Map.of("id", 5, "name", "수석")
        ));

        model.addAttribute("gradeOptions", List.of(
                Map.of("id", "G1", "name", "일반 등급"),
                Map.of("id", "G2", "name", "중간 관리 등급"),
                Map.of("id", "G3", "name", "고급 관리 등급"),
                Map.of("id", "G4", "name", "시니어 등급"),
                Map.of("id", "G5", "name", "임원/본부 등급")
        ));

        /*
         * [비인가 접근 차단 사유 옵션]
         */
        model.addAttribute("blockReasonOptions", List.of(
                "관리자 전용 문서 접근",
                "부서 제한 문서 접근",
                "직급 제한 문서 접근",
                "역할 제한 문서 접근",
                "등급 제한 문서 접근"
        ));

        /*
         * [문서 권한 관리 목록 더미 데이터]
         */
        model.addAttribute("documentPolicies", List.of(
                Map.of(
                        "title", "근태관리 사용자 매뉴얼",
                        "documentType", "업무 매뉴얼",
                        "accessLevel", "전체 공개",
                        "target", "전 직원",
                        "activeStatus", "ACTIVE",
                        "activeLabel", "활성",
                        "updatedAt", "2026-05-09",
                        "requestReason", "챗봇이 조퇴 신청 기준을 정확히 답변하지 못해 근태관리 사용자 매뉴얼을 반영했습니다.",
                        "questionExample", "조퇴 신청은 어디에서 하나요?"
                ),
                Map.of(
                        "title", "인사평가 운영 가이드",
                        "documentType", "사내 규정",
                        "accessLevel", "특정 역할",
                        "target", "TEAM_LEADER, ADMIN",
                        "activeStatus", "ACTIVE",
                        "activeLabel", "활성",
                        "updatedAt", "2026-05-08",
                        "requestReason", "팀장이 인사평가 면담 절차를 챗봇에서 확인할 수 있도록 반영했습니다.",
                        "questionExample", "팀원 평가 면담은 어떤 절차로 진행하나요?"
                ),
                Map.of(
                        "title", "관리자용 급여 처리 매뉴얼",
                        "documentType", "업무 매뉴얼",
                        "accessLevel", "관리자 전용",
                        "target", "ADMIN",
                        "activeStatus", "INACTIVE",
                        "activeLabel", "비활성",
                        "updatedAt", "2026-05-07",
                        "requestReason", "급여 처리 기준 문의 대응을 위해 관리자 전용 문서로 등록했습니다.",
                        "questionExample", "급여 처리 기준은 어떻게 확인하나요?"
                )
        ));

        /*
         * [비인가 접근 차단 로그 더미 데이터]
         */
        model.addAttribute("accessBlockLogs", List.of(
                Map.of(
                        "user", "김민수",
                        "dept", "개발팀",
                        "documentTitle", "관리자용 급여 처리 매뉴얼",
                        "reason", "관리자 전용 문서 접근",
                        "createdAt", "2026-05-11 14:22"
                ),
                Map.of(
                        "user", "박지은",
                        "dept", "영업팀",
                        "documentTitle", "인사평가 운영 가이드",
                        "reason", "역할 제한 문서 접근",
                        "createdAt", "2026-05-11 13:40"
                )
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

    private String buildDateFilterResetUrl(
            int period,
            String department,
            String aiType,
            String result
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/AiSecretary/dashboard")
                .queryParam("period", 7)
                .queryParam("department", department)
                .queryParam("aiType", aiType)
                .queryParam("result", result)
                .queryParam("page", 1);

        return builder.build().encode().toUriString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

