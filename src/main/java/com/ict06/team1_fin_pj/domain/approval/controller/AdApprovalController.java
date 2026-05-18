/**
 * @author : 송영은
 * description : 관리자용 전자결재 컨트롤러
 * ========================================
 * DATE         AUTHOR      NOTE
 * 2026-04-29   송영은       최초 생성
 * 2026-05-01   송영은       결재 서식 생성, 조회 추가
 **/

package com.ict06.team1_fin_pj.domain.approval.controller;

import com.ict06.team1_fin_pj.common.dto.approval.*;
import com.ict06.team1_fin_pj.common.dto.employee.EmployeeListDto;
import com.ict06.team1_fin_pj.common.dto.employee.EmployeeSearchConditionDto;
import com.ict06.team1_fin_pj.common.dto.employee.HrSelectOptionDto;
import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import com.ict06.team1_fin_pj.domain.approval.entity.AppFormEntity;
import com.ict06.team1_fin_pj.domain.approval.service.AdApprovalServiceImpl;
import com.ict06.team1_fin_pj.domain.employee.service.AdEmployeeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// 관리자용 전자결재 컨트롤러
@RequestMapping("/admin/approval")
@RequiredArgsConstructor
@Controller
public class AdApprovalController {

    @Autowired
    private AdApprovalServiceImpl service;
    private final AdEmployeeService adEmployeeService;

    // [[ 전자 결재 서식 관리 ]]
    // [새 전자 결재 서식 만들기] ----------------------------------------------------------------------------
    // 새 서식 추가 페이지
    @RequestMapping("/createAppForm")
    public String createAppForm(HttpServletRequest request, HttpServletResponse response, Model model)
            throws ServletException, IOException {
        System.out.println("[AdApprovalController] - createAppForm()");
        model.addAttribute("activeTab", "appForm"); // 서브 헤더의 어떤 탭(appForm, appLineForm) 활성화 시킬 지 전달
        return "admin/approval/createAppForm";
    }

    // 서식 저장하기 버튼 클릭 후 DB에 저장. Ajax
    @PostMapping("/createAppForm/action")
    @ResponseBody
    public String createAppFormAction(@RequestBody Map<String, Object> body) {
        System.out.println("[AdApprovalController] - createAppFormAction()");

//        String templateHtml = (String) body.get("templateHtml");
        String template = (String) body.get("template");
        System.out.println("formName:" + (String) body.get("formName"));
        System.out.println("template: " + template);

        // AppFormEntity 형태로 준비
        AppFormEntity entity = AppFormEntity.builder()
                .formName((String) body.get("formName"))
                .template(template)
                .build();

        // 서비스에 전달
        service.saveAppForm(entity);
        return "ok";
    }

    // [전자 결재 서식 목록 조회] ----------------------------------------------------------------------------
    // 서식 목록 조회 화면
    @RequestMapping("/appFormList")
    public String appFormList(HttpServletRequest request, HttpServletResponse response, Model model)
            throws ServletException, IOException {
        System.out.println("[AdApprovalController] - appFormList()");
        model.addAttribute("activeTab", "appForm"); // 서브 헤더의 어떤 탭(appForm, appLineForm) 활성화 시킬 지 전달
        return "admin/approval/appFormList";
    }

    // 페이징 처리된 서식 목록 조회 (Ajax)
    @GetMapping("/getAppForms")
    @ResponseBody
    public Page<AppFormListDto> getAppForms(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword) {
        System.out.println("[AdApprovalController] - getAppForms()");
        return service.getAppFormsWithPaging(page, size, keyword);
    }

    // 서식 1건 상세 보기
    @GetMapping("/viewAppForm/{formId}")
    public String viewAppForm(@PathVariable int formId, Model model) {
        System.out.println("[AdApprovalController] - viewAppForm()");
        model.addAttribute("activeTab", "appForm"); // 서브 헤더의 어떤 탭(appForm, appLineForm) 활성화 시킬 지 전달

        AppFormEntity form = service.selectAppForm(formId);
        model.addAttribute("form", form);

        return "admin/approval/viewAppForm"; // thymeleaf 파일
    }

    // 모든 결재선 서식 목록 조회 (Ajax)
    @GetMapping("/appLineTemplates")
    @ResponseBody
    public List<AppLineFormListDto> appLineTemplates(){
        System.out.println("[AdApprovalController] - appLineTemplates()");
        return service.listAllAppLineTemplates();
    }

    // 결재선 서식과 결재 서식 연결 저장 (Ajax)
    @PutMapping("/appForms/{formId}/lineTemplate")
    @ResponseBody
    public ResponseEntity<?> applyLineTemplate(
            @PathVariable Integer formId,
            @RequestParam(required = false) String templateId
    ) {
        System.out.println("[AdApprovalController] - applyLineTemplate()");

        // templateId가 비어 있으면 결재 서식과 결재선 서식의 연결을 해제합니다.
        // 관리자가 모달에서 "선택 안 함"을 고른 경우 이 경로를 사용합니다.
        Integer parsedTemplateId =
                templateId == null || templateId.isBlank()
                        ? null
                        : Integer.valueOf(templateId);

        service.applyLineTemplate(formId, parsedTemplateId);
        return ResponseEntity.ok().build();
    }

    // 여러 결재 서식에 같은 결재선 서식을 일괄 적용하거나 연결을 해제합니다.
    @PutMapping("/appForms/lineTemplate")
    @ResponseBody
    public ResponseEntity<?> applyLineTemplateToForms(
            @RequestBody Map<String, Object> body
    ) {
        System.out.println("[AdApprovalController] - applyLineTemplateToForms()");

        List<Integer> formIds = ((List<?>) body.get("formIds"))
                .stream()
                .map(value -> Integer.valueOf(value.toString()))
                .toList();

        Object templateIdValue = body.get("templateId");
        Integer templateId =
                templateIdValue == null || templateIdValue.toString().isBlank()
                        ? null
                        : Integer.valueOf(templateIdValue.toString());

        service.applyLineTemplateToForms(formIds, templateId);
        return ResponseEntity.ok().build();
    }

    // [전자 결재 서식 삭제] ----------------------------------------------------------------------------
    @DeleteMapping("/deleteAppForm/{formId}")
    @ResponseBody
    public ResponseEntity<?> deleteAppForm(@PathVariable int formId) {
        System.out.println("[AdApprovalController] - deleteAppForm()");
        service.deleteAppForm(formId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/deleteAppForms")
    @ResponseBody
    public ResponseEntity<?> deleteAppForms(@RequestBody Map<String, Object> body) {
        System.out.println("[AdApprovalController] - deleteAppForms()");

        List<Integer> formIds = ((List<?>) body.get("formIds"))
                .stream()
                .map(value -> Integer.valueOf(value.toString()))
                .toList();

        service.deleteAppForms(formIds);
        return ResponseEntity.ok().build();
    }

    // [전자 결재 서식 수정] ----------------------------------------------------------------------------
    // 서식 수정 페이지
    @GetMapping("/editAppForm/{formId}")
    public String editAppForm(@PathVariable int formId, Model model) {
        System.out.println("[AdApprovalController] - editAppForm()");
        model.addAttribute("activeTab", "appForm"); // 서브 헤더의 어떤 탭(appForm, appLineForm) 활성화 시킬 지 전달

        AppFormEntity form = service.selectAppForm(formId); //
        model.addAttribute("form", form);
        return "admin/approval/editAppForm"; // thymeleaf 파일
    }

    // 서식 수정 처리. Ajax
    @PutMapping("/updateAppForm/{formId}")
    @ResponseBody
    public ResponseEntity<?> updateAppForm(@PathVariable int formId,
                                           @RequestBody AppFormDto dto){
        System.out.println("[AdApprovalController] - updateAppForm()");
        service.updateAppForm(formId, dto);
        return ResponseEntity.ok().build();
    }
    
    // ---------------------------------------------------------------
    // [[ 전자 결재 결재선 서식 관리 ]]
    // [새 전자 결재선 서식 추가] -------------------------------------
    // 새 전자 결재선 서식 추가 화면
    @RequestMapping("/createAppLineForm")
    public String createAppLineForm(HttpServletRequest request, HttpServletResponse response, Model model)
            throws ServletException, IOException {
        System.out.println("[AdApprovalController] - createAppLineForm()");
        model.addAttribute("activeTab", "appLineForm"); // 서브 헤더의 어떤 탭(appForm, appLineForm) 활성화 시킬 지 전달

        return "admin/approval/createAppLineForm";
    }

    // 사원 조회 (검색 Ajax)
    @GetMapping("/targets/employees")
    @ResponseBody
    public Page<EmployeeListDto> searchEmployees(
            @ModelAttribute EmployeeSearchConditionDto conditionDto,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return adEmployeeService.findEmployees(conditionDto, pageable);
    }

    // 부서 조회 (검색 Ajax)
    @GetMapping("/targets/departments")
    @ResponseBody
    public List<HrSelectOptionDto> getDepartments() {
        return adEmployeeService.findDepartments();
    }

    // 직급 조회 (검색 Ajax)
    @GetMapping("/targets/positions")
    @ResponseBody
    public List<HrSelectOptionDto> getPositions() {
        return adEmployeeService.findPositions();
    }

    // 새 전자 결재선 서식 추가 처리 (Ajax)
    @PostMapping("/createAppLineFormAction")
    @ResponseBody
    public String createAppLineFormAction(
            @RequestBody AppLineFormRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        System.out.println("[AdApprovalController] - createAppLineFormAction()");
        service.saveAppLineForm(requestDto, principal);
        return "ok";
    }

    // [전자 결재선 서식 조회] ---------------------------------------
    // [전자 결재선 서식 목록 화면 (메인)]
    @RequestMapping("/appLineFormList")
    public String appLineFormList(HttpServletRequest request, HttpServletResponse response, Model model,
                              @PageableDefault(size = 10,
                                      sort = "templateId",
                                      direction = Sort.Direction.DESC)
                              Pageable pageable)
            throws ServletException, IOException {
        System.out.println("[AdApprovalController] - appLineFormList()");
        model.addAttribute("activeTab", "appLineForm"); // 서브 헤더의 어떤 탭(appForm, appLineForm) 활성화 시킬 지 전달

//        model.addAttribute(
//                "page",
//                service.listAppLineForm(pageable)
//        );
        return "admin/approval/appLineFormList";
    }

    // 페이징 처리된 결재선 서식 목록 조회 (Ajax)
    @GetMapping("/getAppLineForms")
    @ResponseBody
    public Page<AppLineFormListDto> getAppLineForms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        System.out.println("[AdApprovalController] - getAppLineForms()");

        return service.getAppLineFormsWithPaging(page, size, keyword);
    }

    // 전자 결재선 서식 1건 상세 조회
    @GetMapping("/appLineFormDetail/{id}")
    public String appLineFormDetail(
            @PathVariable Integer id,
            Model model
    ) {
        System.out.println("[AdApprovalController] - appLineFormDetail()");
        model.addAttribute("activeTab", "appLineForm"); // 서브 헤더의 어떤 탭(appForm, appLineForm) 활성화 시킬 지 전달
        model.addAttribute(
                "detail",
                service.selectAppLineForm(id)
        );

        return "admin/approval/appLineFormDetail";
    }

    // [전자 결재선 서식 삭제] ----------------------------------------
    @DeleteMapping("/deleteAppLineForm/{templateId}")
    @ResponseBody
    public ResponseEntity<?> deleteAppLineForm(
            @PathVariable int templateId
    ) {

        System.out.println("[AdApprovalController] - deleteAppLineForm()");

        service.deleteAppLineTemplate(templateId);

        return ResponseEntity.ok().build();
    }

    // [전자 결재선 서식 수정] ----------------------------------------
    // 전자 결재선 서식 수정 페이지 get
    @GetMapping("/editAppLineForm/{templateId}")
    public String editAppLineForm(
            @PathVariable Integer templateId,
            Model model
    ) {
        System.out.println("[AdApprovalController] - editAppLineForm()");
        model.addAttribute("activeTab", "appLineForm"); // 서브 헤더의 어떤 탭(appForm, appLineForm) 활성화 시킬 지 전달

        model.addAttribute(
                "templateId",
                templateId
        );

        return "admin/approval/editAppLineForm";
    }

    // 전자 결재선 서식 수정 preload 전용 JSON API
    @GetMapping("/appLineFormDetailData/{templateId}")
    @ResponseBody
    public AppLineFormDetailDto appLineFormDetailData(
            @PathVariable Integer templateId
    ) {
        System.out.println("[AdApprovalController] - appLineFormDetailData()");
        return service.selectAppLineForm(templateId);
    }

    // 전자 결재선 서식 수정 처리 (Ajax)
    @PutMapping("/appLineForm/{templateId}")
    @ResponseBody
    public ResponseEntity<?> updateAppLineForm(
            @PathVariable Integer templateId,
            @RequestBody AppLineFormRequestDto dto,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        System.out.println("[AdApprovalController] - updateAppLineForm()");
        service.updateAppLineForm(
                templateId,
                dto,
                principal
        );

        return ResponseEntity.ok().build();
    }


}
