/**
 * @FileName : AdOnboardingController.java
 * @Description : 관리자 온보딩 컨트롤러 - 문서 관리(RAG), 로드맵 및 일정 관리 설정 등을 처리
 * @Author : 김다솜
 * @Date : 2026. 04. 24
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -----------------------------------------------
 * @ 2026.04.24    김다솜        최초 생성 및 온보딩 관리 기본 기능 구현
 * @ 2026.05.10    김다솜        온보딩 로드맵 및 콘텐츠 CRUD 기능 추가
 * @ 2026.05.11    김다솜        문서 기반 RAG 처리 로직 및 로드맵 아이템 편집 기능 추가
 * @ 2026.05.12    김다솜        일정 관리 직원 목록/상세 화면 분리, 진행현황 통계 연동, 학습항목 알림 발송 및 문서 수정 시 RAG 재처리 흐름 분리
 * @ 2026.05.14    김다솜        문서 등록 비동기 처리 적용 및 캘린더 연동을 위한 아이템 수정 로직 위임
 */

package com.ict06.team1_fin_pj.domain.onboarding.controller;

import com.ict06.team1_fin_pj.common.dto.onboarding.AdminDocumentListDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AdDocumentRequestDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AiDocumentQuestionResponseDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AdminOnboardingScheduleEmployeeDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AdminOnboardingScheduleDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AdminRoadItemRequestDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AdminRoadmapRequestDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.DocumentProcessingResultDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.OnContentRequestDto;
import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.DocumentProcessLogRepository;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.DepartmentEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.PositionEntity;
import com.ict06.team1_fin_pj.domain.employee.repository.AdDepartmentRepository;
import com.ict06.team1_fin_pj.domain.employee.repository.AdPositionRepository;
import com.ict06.team1_fin_pj.domain.onboarding.entity.AccessLevel;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentStage;
import com.ict06.team1_fin_pj.domain.onboarding.entity.GeneratedType;
import com.ict06.team1_fin_pj.domain.onboarding.entity.OnContentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadItemEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadProgressEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadmapEntity;
import com.ict06.team1_fin_pj.domain.evaluation.repository.EvaluationQuestionRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.DocumentRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.OnContentRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadItemRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadProgressRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadmapRepository;
import com.ict06.team1_fin_pj.domain.onboarding.service.DocumentProcessingAsyncService;
import com.ict06.team1_fin_pj.domain.onboarding.service.DocumentProcessingService;
import com.ict06.team1_fin_pj.domain.onboarding.service.DocumentQuestionAnswerService;
import com.ict06.team1_fin_pj.domain.onboarding.service.OnboardingScheduleNotificationService;
import com.ict06.team1_fin_pj.domain.onboarding.service.RoadmapServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequestMapping("/admin/onboarding")
@Controller
@RequiredArgsConstructor
public class AdOnboardingController {

    private final DocumentRepository documentRepository;
    private final DocumentProcessLogRepository documentProcessLogRepository;
    private final OnContentRepository onContentRepository;
    private final EvaluationQuestionRepository evaluationQuestionRepository;
    private final RoadmapRepository roadmapRepository;
    private final RoadItemRepository roadItemRepository;
    private final RoadProgressRepository roadProgressRepository;
    private final EmpRepository empRepository;
    private final AdDepartmentRepository adDepartmentRepository;
    private final AdPositionRepository adPositionRepository;
    private final RoadmapServiceImpl roadmapService;
    private final DocumentProcessingService documentProcessingService;
    private final DocumentProcessingAsyncService documentProcessingAsyncService;
    private final DocumentQuestionAnswerService documentQuestionAnswerService;
    private final OnboardingScheduleNotificationService onboardingScheduleNotificationService;

    @RequestMapping("/main")
    public String onboardingMain(HttpServletRequest request, HttpServletResponse response, Model model)
            throws ServletException, IOException {
        System.out.println("[AdOnboardingController] - onboardingMain()");
        return "admin/onboarding/onboardingMain";
    }

    @GetMapping("/schedules")
    public String scheduleList(Model model) {
        System.out.println("[AdOnboardingController] - scheduleList()");

        // 모든 사원의 진행 정보를 한 번에 조회하되, 아이템이 연결된 유효한 데이터만 맵핑
        Map<Integer, RoadProgressEntity> progressByItemId = roadProgressRepository.findAll().stream()
                .filter(progress -> progress.getItem() != null)
                .collect(Collectors.toMap(
                        progress -> progress.getItem().getItemId(),
                        Function.identity(),
                        (left, right) -> left
                ));

        List<AdminOnboardingScheduleDto> schedules = roadItemRepository.findAllByOrderByRoadmap_Employee_EmpNoAscOrderNoAsc().stream()
                .map(item -> toScheduleDto(item, progressByItemId.get(item.getItemId())))
                .toList();

        model.addAttribute("employeeSchedules", toScheduleEmployeeDtos(schedules));
        return "admin/onboarding/scheduleList";
    }

    @GetMapping("/schedules/{empNo}")
    public String scheduleDetail(
            @PathVariable String empNo,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - scheduleDetail()");

        List<RoadItemEntity> items = roadItemRepository.findByRoadmap_Employee_EmpNoOrderByOrderNoAsc(empNo);
        if (items.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "해당 사원의 온보딩 일정 정보가 없습니다.");
            return "redirect:/admin/onboarding/schedules";
        }

        Map<Integer, RoadProgressEntity> progressByItemId = roadProgressRepository.findAll().stream()
                .collect(Collectors.toMap(
                        progress -> progress.getItem().getItemId(),
                        Function.identity(),
                        (left, right) -> left
                ));

        List<AdminOnboardingScheduleDto> schedules = items.stream()
                .map(item -> toScheduleDto(item, progressByItemId.get(item.getItemId())))
                .toList();

        AdminOnboardingScheduleDto firstSchedule = schedules.get(0);
        model.addAttribute("empNo", empNo);
        model.addAttribute("employeeName", firstSchedule.getEmployeeName());
        model.addAttribute("roadmapTitle", firstSchedule.getRoadmapTitle());
        model.addAttribute("schedules", schedules);
        model.addAttribute("items", schedules);
        return "admin/onboarding/scheduleDetail";
    }

    @PostMapping("/schedules/{empNo}/items/{itemId}/notify")
    public String notifyScheduleItem(
            @PathVariable String empNo,
            @PathVariable Integer itemId,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - notifyScheduleItem()");

        try {
            onboardingScheduleNotificationService.sendManualItemNotification(itemId);
            redirectAttributes.addFlashAttribute("successMessage", "수동 알림이 발송되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "알림 발송 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/admin/onboarding/schedules/" + empNo;
    }

    @GetMapping("/documents")
    public String documentList(Model model) {
        System.out.println("[AdOnboardingController] - documentList()");

        model.addAttribute("documents", documentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDocumentListDto)
                .toList());
        return "admin/onboarding/documentList";
    }

    @GetMapping("/documents/status")
    @ResponseBody
    public List<AdminDocumentListDto> documentStatus() {
        System.out.println("[AdOnboardingController] - documentStatus()");

        return documentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDocumentListDto)
                .toList();
    }

    @PostMapping("/documents/{docId}/answer")
    @ResponseBody
    public Map<String, Object> answerDocumentQuestion(
            @PathVariable Integer docId,
            @RequestParam String question
    ) {
        System.out.println("[AdOnboardingController] - answerDocumentQuestion()");

        Map<String, Object> response = new LinkedHashMap<>();
        try {
            AiDocumentQuestionResponseDto result = documentQuestionAnswerService.answerQuestion(docId, question);
            response.put("success", true);
            response.put("answer", result.getAnswer());
            response.put("usedChunkCount", result.getUsedChunkCount());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return response;
    }

    @GetMapping("/documents/new")
    public String documentForm(Model model) {
        System.out.println("[AdOnboardingController] - documentForm()");

        AdDocumentRequestDto document = new AdDocumentRequestDto();
        document.setCurrentStage(DocumentStage.UPLOADED);
        model.addAttribute("document", document);
        addDocumentFormOptions(model);
        model.addAttribute("isEdit", false);
        return "admin/onboarding/documentForm";
    }

    @PostMapping("/documents")
    public String createDocument(
            @ModelAttribute AdDocumentRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principal,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - createDocument()");

        DepartmentEntity department = getDepartment(requestDto.getDeptId());
        if (requestDto.getDeptId() != null && department == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected department was not found.");
            return "redirect:/admin/onboarding/documents";
        }

        DocumentEntity document = DocumentEntity.builder()
                .title(requestDto.getTitle())
                .filePath(requestDto.getFilePath())
                .department(department)
                .accessLevel(requestDto.getAccessLevel() != null ? requestDto.getAccessLevel() : AccessLevel.PUBLIC)
                .currentStage(DocumentStage.UPLOADED)
                .createdBy(principal != null ? principal.getEmp() : null)
                .build();

        documentRepository.saveAndFlush(document);
        documentProcessingAsyncService.processDocumentAsync(
                document.getDocId(),
                principal != null ? principal.getEmp() : null
        );
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Document saved successfully. Automatic processing started in the background. Refresh the page later to check updated chunks and vectors."
        );
        return "redirect:/admin/onboarding/documents";
    }

    @GetMapping("/documents/{docId}/edit")
    public String editDocumentForm(
            @PathVariable Integer docId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - editDocumentForm()");

        return documentRepository.findById(docId)
                .map(document -> {
                    model.addAttribute("document", toDocumentRequestDto(document));
                    model.addAttribute("documentId", document.getDocId());
                    model.addAttribute("createdByName", document.getCreatedBy() != null ? document.getCreatedBy().getName() : "-");
                    addDocumentFormOptions(model);
                    model.addAttribute("isEdit", true);
                    return "admin/onboarding/documentForm";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Document was not found.");
                    return "redirect:/admin/onboarding/documents";
                });
    }

    @PostMapping("/documents/{docId}/edit")
    public String updateDocument(
            @PathVariable Integer docId,
            @ModelAttribute AdDocumentRequestDto requestDto,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - updateDocument()");

        DepartmentEntity department = getDepartment(requestDto.getDeptId());
        if (requestDto.getDeptId() != null && department == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected department was not found.");
            return "redirect:/admin/onboarding/documents";
        }

        return documentRepository.findById(docId)
                .map(document -> {
                    document.updateDocument(
                            requestDto.getTitle(),
                            requestDto.getFilePath(),
                            department,
                            requestDto.getAccessLevel() != null ? requestDto.getAccessLevel() : AccessLevel.PUBLIC,
                            document.getCurrentStage()
                    );
                    documentRepository.save(document);
                    redirectAttributes.addFlashAttribute("successMessage", "Document updated successfully. Use reprocess when RAG data needs to be refreshed.");
                    return "redirect:/admin/onboarding/documents";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Document was not found.");
                    return "redirect:/admin/onboarding/documents";
                });
    }

    @PostMapping("/documents/{docId}/delete")
    public String deleteDocument(
            @PathVariable Integer docId,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - deleteDocument()");

        if (!documentRepository.existsById(docId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Document was not found.");
            return "redirect:/admin/onboarding/documents";
        }

        try {
            documentProcessingService.deleteDocument(docId);
            redirectAttributes.addFlashAttribute("successMessage", "Document deleted successfully.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Document is still referenced by other data and cannot be deleted.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/onboarding/documents";
    }

    @PostMapping("/documents/{docId}/process")
    public String processDocument(
            @PathVariable Integer docId,
            @AuthenticationPrincipal PrincipalDetails principal,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - processDocument()");

        if (!documentRepository.existsById(docId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Document was not found.");
            return "redirect:/admin/onboarding/documents";
        }

        documentProcessingAsyncService.processDocumentAsync(
                docId,
                principal != null ? principal.getEmp() : null
        );
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Document reprocessing started. Refresh the page later to check updated chunks and vectors."
        );
        return "redirect:/admin/onboarding/documents";
    }

    @GetMapping("/contents")
    public String contentList(HttpServletRequest request, HttpServletResponse response, Model model)
            throws ServletException, IOException {
        System.out.println("[AdOnboardingController] - onboardingContents()");

        model.addAttribute("contents", onContentRepository.findAll());
        return "admin/onboarding/contentList";
    }

    @GetMapping("/contents/new")
    public String contentForm(Model model) {
        System.out.println("[AdOnboardingController] - contentForm()");

        model.addAttribute("content", new OnContentEntity());
        addContentFormOptions(model, List.of(), List.of());
        model.addAttribute("isEdit", false);
        return "admin/onboarding/contentForm";
    }

    @PostMapping("/contents")
    public String createContent(
            @ModelAttribute OnContentRequestDto requestDto,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - createContent()");

        OnContentEntity content = OnContentEntity.builder()
                .title(requestDto.getTitle())
                .type(requestDto.getType())
                .category(requestDto.getCategory())
                .subCategory(requestDto.getSubCategory())
                .targetPosition(requestDto.getTargetPosition())
                .difficulty(requestDto.getDifficulty())
                .estimatedTime(requestDto.getEstimatedTime())
                .path(requestDto.getPath())
                .isMandatory(Boolean.TRUE.equals(requestDto.getIsMandatory()))
                .build();

        content.updateTargetPositions(getTargetPositions(requestDto));
        content.updateTargetDepartments(getTargetDepartments(requestDto));
        onContentRepository.save(content);
        redirectAttributes.addFlashAttribute("successMessage", "Content saved successfully.");

        return "redirect:/admin/onboarding/contents";
    }

    @GetMapping("/contents/{contentId}/edit")
    public String editContentForm(
            @PathVariable Integer contentId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - editContentForm()");

        return onContentRepository.findById(contentId)
                .map(content -> {
                    model.addAttribute("content", content);
                    addContentFormOptions(
                            model,
                            getTargetPositionIds(content),
                            getTargetDepartmentIds(content)
                    );
                    model.addAttribute("isEdit", true);
                    return "admin/onboarding/contentForm";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Content was not found.");
                    return "redirect:/admin/onboarding/contents";
                });
    }

    @PostMapping("/contents/{contentId}/edit")
    public String updateContent(
            @PathVariable Integer contentId,
            @ModelAttribute OnContentRequestDto requestDto,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - updateContent()");

        return onContentRepository.findById(contentId)
                .map(content -> {
                    content.updateContent(
                            requestDto.getTitle(),
                            requestDto.getType(),
                            requestDto.getCategory(),
                            requestDto.getSubCategory(),
                            requestDto.getTargetPosition(),
                            requestDto.getDifficulty(),
                            requestDto.getEstimatedTime(),
                            requestDto.getPath(),
                            requestDto.getIsMandatory()
                    );
                    content.updateTargetPositions(getTargetPositions(requestDto));
                    content.updateTargetDepartments(getTargetDepartments(requestDto));
                    onContentRepository.save(content);
                    redirectAttributes.addFlashAttribute("successMessage", "Content updated successfully.");
                    return "redirect:/admin/onboarding/contents";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Content was not found.");
                    return "redirect:/admin/onboarding/contents";
                });
    }

    @PostMapping("/contents/{contentId}/delete")
    public String deleteContent(
            @PathVariable Integer contentId,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - deleteContent()");

        if (!onContentRepository.existsById(contentId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Content was not found.");
            return "redirect:/admin/onboarding/contents";
        }

        try {
            onContentRepository.deleteById(contentId);
            redirectAttributes.addFlashAttribute("successMessage", "Content deleted successfully.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Content is still referenced by a roadmap and cannot be deleted.");
        }

        return "redirect:/admin/onboarding/contents";
    }

    @GetMapping("/roadmaps")
    public String roadmapList(Model model) {
        System.out.println("[AdOnboardingController] - roadmapList()");

        model.addAttribute("roadmaps", roadmapRepository.findAll());
        return "admin/onboarding/roadmapList";
    }

    @GetMapping("/roadmaps/new")
    public String roadmapForm(Model model) {
        System.out.println("[AdOnboardingController] - roadmapForm()");

        model.addAttribute("roadmap", new RoadmapEntity());
        model.addAttribute("employees", empRepository.findAll());
        model.addAttribute("generatedTypes", GeneratedType.values());
        model.addAttribute("isEdit", false);
        return "admin/onboarding/roadmapForm";
    }

    @PostMapping("/roadmaps")
    public String createRoadmap(
            @ModelAttribute AdminRoadmapRequestDto requestDto,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - createRoadmap()");

        EmpEntity employee = empRepository.findByEmpNo(requestDto.getEmpNo())
                .orElse(null);

        if (employee == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Employee was not found.");
            return "redirect:/admin/onboarding/roadmaps";
        }

        RoadmapEntity roadmap = RoadmapEntity.builder()
                .title(buildRoadmapTitle(employee))
                .employee(employee)
                .department(employee.getDepartment())
                .position(employee.getPosition())
                .generatedType(requestDto.getGeneratedType())
                .version(1)
                .isCompleted(false)
                .build();

        roadmapRepository.save(roadmap);
        redirectAttributes.addFlashAttribute("successMessage", "Roadmap saved successfully.");

        return "redirect:/admin/onboarding/roadmaps";
    }

    @GetMapping("/roadmaps/{roadmapId}/edit")
    public String editRoadmapForm(
            @PathVariable Integer roadmapId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - editRoadmapForm()");

        return roadmapRepository.findById(roadmapId)
                .map(roadmap -> {
                    model.addAttribute("roadmap", roadmap);
                    model.addAttribute("employees", empRepository.findAll());
                    model.addAttribute("generatedTypes", GeneratedType.values());
                    model.addAttribute("isEdit", true);
                    return "admin/onboarding/roadmapForm";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Roadmap was not found.");
                    return "redirect:/admin/onboarding/roadmaps";
                });
    }

    @PostMapping("/roadmaps/{roadmapId}/edit")
    public String updateRoadmap(
            @PathVariable Integer roadmapId,
            @ModelAttribute AdminRoadmapRequestDto requestDto,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - updateRoadmap()");

        return roadmapRepository.findById(roadmapId)
                .map(roadmap -> {
                    roadmap.updateRoadmap(buildRoadmapTitle(roadmap.getEmployee()), requestDto.getGeneratedType());
                    roadmapRepository.save(roadmap);
                    redirectAttributes.addFlashAttribute("successMessage", "Roadmap updated successfully.");
                    return "redirect:/admin/onboarding/roadmaps";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Roadmap was not found.");
                    return "redirect:/admin/onboarding/roadmaps";
                });
    }

    @PostMapping("/roadmaps/{roadmapId}/regenerate")
    public String regenerateRoadmap(
            @PathVariable Integer roadmapId,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - regenerateRoadmap()");

        return roadmapRepository.findById(roadmapId)
                .map(roadmap -> {
                    try {
                        RoadmapEntity regeneratedRoadmap = roadmapService.resetAndRegenerateRoadmap(roadmapId);
                        redirectAttributes.addFlashAttribute("successMessage", "Roadmap was reset and regenerated from the latest content list.");
                        return "redirect:/admin/onboarding/roadmaps/" + regeneratedRoadmap.getRoadmapId() + "/items";
                    } catch (IllegalStateException e) {
                        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                        return "redirect:/admin/onboarding/roadmaps";
                    }
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Roadmap was not found.");
                    return "redirect:/admin/onboarding/roadmaps";
                });
    }

    @GetMapping("/roadmaps/{roadmapId}/items")
    public String roadItemList(
            @PathVariable Integer roadmapId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - roadItemList()");

        return roadmapRepository.findById(roadmapId)
                .map(roadmap -> {
                    model.addAttribute("roadmap", roadmap);
                    model.addAttribute("items", roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmapId));
                    return "admin/onboarding/roadItemList";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Roadmap was not found.");
                    return "redirect:/admin/onboarding/roadmaps";
                });
    }

    @GetMapping("/roadmaps/{roadmapId}/items/new")
    public String roadItemForm(
            @PathVariable Integer roadmapId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - roadItemForm()");

        return roadmapRepository.findById(roadmapId)
                .map(roadmap -> {
                    model.addAttribute("roadmap", roadmap);
                    model.addAttribute("item", new RoadItemEntity());
                    model.addAttribute("contents", onContentRepository.findAll());
                    model.addAttribute("categories", getContentCategories());
                    model.addAttribute("defaultOrderNo", getNextRoadItemOrderNo(roadmapId));
                    model.addAttribute("isEdit", false);
                    return "admin/onboarding/roadItemForm";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Roadmap was not found.");
                    return "redirect:/admin/onboarding/roadmaps";
                });
    }

    @PostMapping("/roadmaps/{roadmapId}/items")
    public String createRoadItem(
            @PathVariable Integer roadmapId,
            @ModelAttribute AdminRoadItemRequestDto requestDto,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - createRoadItem()");

        RoadmapEntity roadmap = roadmapRepository.findById(roadmapId).orElse(null);
        OnContentEntity content = onContentRepository.findById(requestDto.getContentId()).orElse(null);

        if (roadmap == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Roadmap was not found.");
            return "redirect:/admin/onboarding/roadmaps";
        }

        if (content == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Content was not found.");
            return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items";
        }

        if (hasInvalidSchedule(requestDto)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Due date cannot be earlier than the start date.");
            return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items/new";
        }

        RoadItemEntity item = RoadItemEntity.builder()
                .roadmap(roadmap)
                .content(content)
                .itemTitle(requestDto.getItemTitle())
                .categoryName(requestDto.getCategoryName())
                .orderNo(requestDto.getOrderNo())
                .startDate(requestDto.getStartDate())
                .dueDate(requestDto.getDueDate())
                .build();

        roadItemRepository.save(item);
        redirectAttributes.addFlashAttribute("successMessage", "Roadmap item saved successfully.");

        return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items";
    }

    @GetMapping("/roadmaps/{roadmapId}/items/{itemId}/edit")
    public String editRoadItemForm(
            @PathVariable Integer roadmapId,
            @PathVariable Integer itemId,
            @RequestParam(required = false) String returnUrl,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - editRoadItemForm()");

        RoadmapEntity roadmap = roadmapRepository.findById(roadmapId).orElse(null);
        RoadItemEntity item = roadItemRepository.findById(itemId).orElse(null);

        if (roadmap == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Roadmap was not found.");
            return "redirect:/admin/onboarding/roadmaps";
        }

        if (item == null || !item.getRoadmap().getRoadmapId().equals(roadmapId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Roadmap item was not found.");
            return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items";
        }

        model.addAttribute("roadmap", roadmap);
        model.addAttribute("item", item);
        model.addAttribute("contents", onContentRepository.findAll());
        model.addAttribute("categories", getContentCategories());
        model.addAttribute("defaultOrderNo", item.getOrderNo());
        model.addAttribute("isEdit", true);
        model.addAttribute("returnUrl", normalizeReturnUrl(returnUrl));

        return "admin/onboarding/roadItemForm";
    }

    @PostMapping("/roadmaps/{roadmapId}/items/{itemId}/edit")
    public String updateRoadItem(
            @PathVariable Integer roadmapId,
            @PathVariable Integer itemId,
            @ModelAttribute AdminRoadItemRequestDto requestDto,
            @RequestParam(required = false) String returnUrl,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - updateRoadItem()");

        RoadItemEntity item = roadItemRepository.findById(itemId).orElse(null);
        OnContentEntity content = onContentRepository.findById(requestDto.getContentId()).orElse(null);

        if (item == null || !item.getRoadmap().getRoadmapId().equals(roadmapId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Roadmap item was not found.");
            return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items";
        }

        if (content == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Content was not found.");
            return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items";
        }

        if (hasInvalidSchedule(requestDto)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Due date cannot be earlier than the start date.");
            return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items/" + itemId + "/edit" + buildReturnUrlQuery(returnUrl);
        }

        roadmapService.updateRoadItem(itemId, requestDto);
        redirectAttributes.addFlashAttribute("successMessage", "Roadmap item updated successfully.");

        return "redirect:" + resolveRoadItemReturnUrl(returnUrl, roadmapId);
    }

    @PostMapping("/roadmaps/{roadmapId}/items/{itemId}/delete")
    public String deleteRoadItem(
            @PathVariable Integer roadmapId,
            @PathVariable Integer itemId,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - deleteRoadItem()");

        RoadItemEntity item = roadItemRepository.findById(itemId).orElse(null);

        if (item == null || !item.getRoadmap().getRoadmapId().equals(roadmapId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Roadmap item was not found.");
            return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items";
        }

        try {
            // 레포지토리 직접 삭제 대신 서비스를 통해 연관 캘린더 일정까지 삭제
            roadmapService.deleteRoadItem(itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Roadmap item deleted successfully.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Roadmap item is still referenced by progress data and cannot be deleted.");
        }

        return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items";
    }

    @PostMapping("/roadmaps/{roadmapId}/items/{itemId}/move-up")
    public String moveRoadItemUp(
            @PathVariable Integer roadmapId,
            @PathVariable Integer itemId,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - moveRoadItemUp()");

        moveRoadItemOrder(roadmapId, itemId, -1, redirectAttributes);
        return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items";
    }

    @PostMapping("/roadmaps/{roadmapId}/items/{itemId}/move-down")
    public String moveRoadItemDown(
            @PathVariable Integer roadmapId,
            @PathVariable Integer itemId,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - moveRoadItemDown()");

        moveRoadItemOrder(roadmapId, itemId, 1, redirectAttributes);
        return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items";
    }

    @PostMapping("/roadmaps/{roadmapId}/items/reorder")
    public String reorderRoadItems(
            @PathVariable Integer roadmapId,
            @RequestParam(name = "itemIds") List<Integer> itemIds,
            @RequestParam(name = "orderNos") List<Integer> orderNos,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("[AdOnboardingController] - reorderRoadItems()");

        if (itemIds == null || orderNos == null || itemIds.size() != orderNos.size()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Reorder request is invalid.");
            return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items";
        }

        Map<Integer, RoadItemEntity> itemMap = roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmapId).stream()
                .collect(Collectors.toMap(RoadItemEntity::getItemId, Function.identity()));

        for (int i = 0; i < itemIds.size(); i++) {
            RoadItemEntity item = itemMap.get(itemIds.get(i));

            if (item == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Roadmap item was not found.");
                return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items";
            }

            item.updateOrderNo(orderNos.get(i));
        }

        roadItemRepository.saveAll(itemMap.values());
        redirectAttributes.addFlashAttribute("successMessage", "Roadmap item order saved successfully.");
        return "redirect:/admin/onboarding/roadmaps/" + roadmapId + "/items";
    }

    private List<String> getContentCategories() {
        return onContentRepository.findAll().stream()
                .map(OnContentEntity::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private void addDocumentFormOptions(Model model) {
        model.addAttribute("departments", adDepartmentRepository.findAll());
        model.addAttribute("accessLevels", AccessLevel.values());
        model.addAttribute("documentStages", DocumentStage.values());
    }

    private void addContentFormOptions(
            Model model,
            List<Integer> selectedTargetPositionIds,
            List<Integer> selectedTargetDepartmentIds
    ) {
        model.addAttribute("positions", adPositionRepository.findAll());
        model.addAttribute("departments", adDepartmentRepository.findAll());
        model.addAttribute("selectedTargetPositionIds", selectedTargetPositionIds);
        model.addAttribute("selectedTargetDepartmentIds", selectedTargetDepartmentIds);
    }

    private List<PositionEntity> getTargetPositions(OnContentRequestDto requestDto) {
        if (requestDto.getTargetPositionIds() == null || requestDto.getTargetPositionIds().isEmpty()) {
            return List.of();
        }

        return adPositionRepository.findAllById(requestDto.getTargetPositionIds());
    }

    private List<DepartmentEntity> getTargetDepartments(OnContentRequestDto requestDto) {
        if (requestDto.getTargetDepartmentIds() == null || requestDto.getTargetDepartmentIds().isEmpty()) {
            return List.of();
        }

        return adDepartmentRepository.findAllById(requestDto.getTargetDepartmentIds());
    }

    private DepartmentEntity getDepartment(Integer deptId) {
        if (deptId == null) {
            return null;
        }

        return adDepartmentRepository.findById(deptId).orElse(null);
    }

    private List<Integer> getTargetPositionIds(OnContentEntity content) {
        return content.getTargetPositions().stream()
                .map(PositionEntity::getPositionId)
                .toList();
    }

    private List<Integer> getTargetDepartmentIds(OnContentEntity content) {
        return content.getTargetDepartments().stream()
                .map(DepartmentEntity::getDeptId)
                .toList();
    }

    private Integer getNextRoadItemOrderNo(Integer roadmapId) {
        return roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmapId).stream()
                .map(RoadItemEntity::getOrderNo)
                .filter(orderNo -> orderNo != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private void moveRoadItemOrder(
            Integer roadmapId,
            Integer itemId,
            int direction,
            RedirectAttributes redirectAttributes
    ) {
        List<RoadItemEntity> items = roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmapId);
        int currentIndex = -1;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItemId().equals(itemId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Roadmap item was not found.");
            return;
        }

        int targetIndex = currentIndex + direction;
        if (targetIndex < 0 || targetIndex >= items.size()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Items at the boundary cannot be moved further.");
            return;
        }

        RoadItemEntity currentItem = items.get(currentIndex);
        RoadItemEntity targetItem = items.get(targetIndex);
        Integer currentOrderNo = currentItem.getOrderNo();

        currentItem.updateOrderNo(targetItem.getOrderNo());
        targetItem.updateOrderNo(currentOrderNo);

        roadItemRepository.save(currentItem);
        roadItemRepository.save(targetItem);
        redirectAttributes.addFlashAttribute("successMessage", "Roadmap item order updated.");
    }

    private String buildRoadmapTitle(EmpEntity employee) {
        return employee.getName() + " Onboarding Roadmap";
    }

    private AdminDocumentListDto toDocumentListDto(DocumentEntity document) {
        int chunkCount = document.getChunks() != null ? document.getChunks().size() : 0;
        int vectorCount = document.getChunks() == null ? 0 : (int) document.getChunks().stream()
                .filter(chunk -> chunk.getVector() != null)
                .count();
        Optional<OnContentEntity> linkedContent = findLinkedContent(document);
        int generatedQuizCount = linkedContent
                .map(content -> (int) evaluationQuestionRepository.findByContent_ContentId(content.getContentId()).stream()
                        .filter(question -> question.getExplanation() != null)
                        .filter(question -> question.getExplanation().startsWith("[AI자동생성]"))
                        .count())
                .orElse(0);
        String lastErrorMessage = documentProcessLogRepository
                .findTopByDocument_DocIdAndErrorMessageIsNotNullOrderByJobIdDesc(document.getDocId())
                .map(log -> log.getErrorMessage())
                .orElse(null);

        return AdminDocumentListDto.builder()
                .docId(document.getDocId())
                .title(document.getTitle())
                .filePath(document.getFilePath())
                .summaryPreview(document.getSummaryPreview())
                .departmentName(document.getDepartment() != null ? document.getDepartment().getDeptName() : "공통")
                .accessLevel(document.getAccessLevel())
                .currentStage(document.getCurrentStage())
                .chunkCount(chunkCount)
                .vectorCount(vectorCount)
                .linkedContentId(linkedContent.map(OnContentEntity::getContentId).orElse(null))
                .generatedQuizCount(generatedQuizCount)
                .quizGenerated(generatedQuizCount > 0)
                .createdByName(document.getCreatedBy() != null ? document.getCreatedBy().getName() : "-")
                .lastErrorMessage(lastErrorMessage)
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private Optional<OnContentEntity> findLinkedContent(DocumentEntity document) {
        if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
            Optional<OnContentEntity> byPath = onContentRepository.findFirstByPath(document.getFilePath());
            if (byPath.isPresent()) {
                return byPath;
            }
        }

        if (document.getTitle() != null && !document.getTitle().isBlank()) {
            return onContentRepository.findFirstByTitleIgnoreCase(document.getTitle());
        }

        return Optional.empty();
    }

    private AdDocumentRequestDto toDocumentRequestDto(DocumentEntity document) {
        AdDocumentRequestDto dto = new AdDocumentRequestDto();
        dto.setTitle(document.getTitle());
        dto.setFilePath(document.getFilePath());
        dto.setDeptId(document.getDepartment() != null ? document.getDepartment().getDeptId() : null);
        dto.setAccessLevel(document.getAccessLevel());
        dto.setCurrentStage(document.getCurrentStage());
        return dto;
    }

    private void applyDocumentProcessFlashMessage(
            RedirectAttributes redirectAttributes,
            String baseMessage,
            DocumentProcessingResultDto processingResult
    ) {
        if (processingResult == null) {
            redirectAttributes.addFlashAttribute("successMessage", baseMessage);
            return;
        }

        if (processingResult.isSuccess()) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "%s %s (chunks %d, vectors %d)".formatted(
                            baseMessage,
                            processingResult.getMessage(),
                            processingResult.getChunkCount(),
                            processingResult.getVectorCount()
                    )
            );
            return;
        }

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "%s automatic processing failed. %s".formatted(baseMessage, processingResult.getMessage())
        );
    }

    private boolean hasInvalidSchedule(AdminRoadItemRequestDto requestDto) {
        return requestDto.getStartDate() != null
                && requestDto.getDueDate() != null
                && requestDto.getDueDate().isBefore(requestDto.getStartDate());
    }

    private String normalizeReturnUrl(String returnUrl) {
        if (returnUrl == null || returnUrl.isBlank()) {
            return null;
        }

        if (returnUrl.startsWith("/admin/onboarding/")) {
            return returnUrl;
        }

        return null;
    }

    private String resolveRoadItemReturnUrl(String returnUrl, Integer roadmapId) {
        String normalizedReturnUrl = normalizeReturnUrl(returnUrl);
        if (normalizedReturnUrl != null) {
            return normalizedReturnUrl;
        }

        return "/admin/onboarding/roadmaps/" + roadmapId + "/items";
    }

    private String buildReturnUrlQuery(String returnUrl) {
        String normalizedReturnUrl = normalizeReturnUrl(returnUrl);
        if (normalizedReturnUrl == null) {
            return "";
        }

        return "?returnUrl=" + normalizedReturnUrl;
    }

    private List<AdminOnboardingScheduleEmployeeDto> toScheduleEmployeeDtos(List<AdminOnboardingScheduleDto> schedules) {
        Map<String, List<AdminOnboardingScheduleDto>> schedulesByEmployee = schedules.stream()
                .collect(Collectors.groupingBy(
                        AdminOnboardingScheduleDto::getEmpNo,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return schedulesByEmployee.entrySet().stream()
                .map(entry -> {
                    List<AdminOnboardingScheduleDto> employeeSchedules = entry.getValue();
                    AdminOnboardingScheduleDto firstSchedule = employeeSchedules.get(0);
                    int totalCount = employeeSchedules.size();
                    int completedCount = (int) employeeSchedules.stream()
                            .filter(schedule -> "COMPLETED".equals(schedule.getStatus()))
                            .count();
                    int inProgressCount = (int) employeeSchedules.stream()
                            .filter(schedule -> "IN_PROGRESS".equals(schedule.getStatus()))
                            .count();
                    int notStartedCount = totalCount - completedCount - inProgressCount;
                    int progressRate = totalCount > 0 ? Math.round((completedCount * 100.0f) / totalCount) : 0;

                    return AdminOnboardingScheduleEmployeeDto.builder()
                            .empNo(firstSchedule.getEmpNo())
                            .employeeName(firstSchedule.getEmployeeName())
                            .roadmapTitle(firstSchedule.getRoadmapTitle())
                            .totalCount(totalCount)
                            .completedCount(completedCount)
                            .inProgressCount(inProgressCount)
                            .notStartedCount(notStartedCount)
                            .progressRate(progressRate)
                            .build();
                })
                .toList();
    }

    private AdminOnboardingScheduleDto toScheduleDto(RoadItemEntity item, RoadProgressEntity progress) {
        RoadmapEntity roadmap = item.getRoadmap();
        EmpEntity employee = roadmap != null ? roadmap.getEmployee() : null;

        return AdminOnboardingScheduleDto.builder()
                .roadmapId(roadmap != null ? roadmap.getRoadmapId() : null)
                .itemId(item.getItemId())
                .empNo(employee != null ? employee.getEmpNo() : "-")
                .employeeName(employee != null ? employee.getName() : "-")
                .roadmapTitle(roadmap != null ? roadmap.getTitle() : "-")
                .categoryName(item.getCategoryName())
                .itemTitle(item.getItemTitle())
                .contentTitle(item.getContent() != null ? item.getContent().getTitle() : "-")
                .startDate(item.getStartDate())
                .dueDate(item.getDueDate())
                .status(progress != null && progress.getStatus() != null ? progress.getStatus().name() : "NOT_STARTED")
                .build();
    }
}
