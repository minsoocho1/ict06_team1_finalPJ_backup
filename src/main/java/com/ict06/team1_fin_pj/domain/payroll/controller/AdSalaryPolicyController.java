/**
 * @author : 송영은
 * description : 관리자용 급여 관리 컨트롤러
 * ========================================
 * DATE         AUTHOR      NOTE
 * 2026-04-24   송영은       최초 생성
 **/

package com.ict06.team1_fin_pj.domain.payroll.controller;

import com.ict06.team1_fin_pj.common.dto.payroll.*;
import com.ict06.team1_fin_pj.domain.payroll.service.AdSalaryPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor // final 필드 생성자 자동 생성
@RequestMapping("/admin/payroll")
public class AdSalaryPolicyController {

    private final AdSalaryPolicyService adSalaryPolicyService;

    // 기본급 정책 목록 조회
    @GetMapping("/salary-policy")
    public String salaryPolicyPage(SalaryPolicySearchDTO searchDTO, Model model) {

        // 검색조건 + 페이징 기반으로 기본급 정책 조회
        SalaryPolicyPageResponseDTO pageResponse =
                adSalaryPolicyService.getSalaryPolicyList(searchDTO);

        // 화면에 뿌릴 데이터
        model.addAttribute("salaryPolicyList", pageResponse.getContent());
        model.addAttribute("pageResponse", pageResponse);

        // select box 데이터 (부서 / 직급 / 급여등급)
        model.addAttribute("headDepartmentList", adSalaryPolicyService.getHeadDepartmentList());
        model.addAttribute("departmentList", adSalaryPolicyService.getDepartmentList());
        model.addAttribute("positionList", adSalaryPolicyService.getPositionList());
        model.addAttribute("gradeCodeList", adSalaryPolicyService.getGradeCodeList());

        // 검색 조건 유지 (화면에 다시 뿌리기 위함)
        model.addAttribute("searchDTO", searchDTO);

        return "admin/payroll/salary-policy";
    }

    // 기본급 등록 페이지 (등록화면)
    @GetMapping("/salary-policy/register")
    public String showSalaryPolicyRegistrationPage(Model model) {

        // 부서, 직급 리스트를 모달에 넣기
        model.addAttribute("departmentList", adSalaryPolicyService.getDepartmentList());
        model.addAttribute("positionList", adSalaryPolicyService.getPositionList());

        return "admin/payroll/salary-policy-register";
    }

    // 등록 가능 여부 체크 (AJAX)
    @GetMapping("/salary-policy/register-check")
    @ResponseBody // JSON으로 반환
    public SalaryPolicyRegisterCheckResponseDTO checkSalaryPolicyRegisterAvailable(
            @RequestParam String deptId,
            @RequestParam String positionId
    ) {
        // 해당 부서+직급에 등록 가능한 급여등급 + 중복 여부 반환
        return adSalaryPolicyService.checkSalaryPolicyRegisterAvailable(deptId, positionId);
    }

    // 등록 시 서열 검증 (AJAX)
    @GetMapping("/salary-policy/check-grade-order")
    @ResponseBody // JSON으로 반환
    public boolean checkGradeOrder(SalaryPolicyRequestDTO requestDTO) {

        // G1 < G2 < G3 < G4 < G5 서열 검증
        // DB 조회 → Java에서 비교
        return adSalaryPolicyService.isValidGradeOrder(requestDTO);
    }

    // 기본급 정책 최종 등록 - AJAX 검증을 통과했더라도 Service에서 중복/서열 다시 검증
    @PostMapping("/salary-policy/register")
    public String registerSalaryPolicy(@ModelAttribute SalaryPolicyRequestDTO requestDTO,
                                       @RequestParam(defaultValue = "1") int searchPage,
                                       @RequestParam(defaultValue = "10") int searchSize,
                                       @RequestParam(required = false) String searchHeadDeptId,
                                       @RequestParam(required = false) String searchDeptId,
                                       @RequestParam(required = false) String searchPositionId,
                                       @RequestParam(required = false) String searchGradeId,
                                       @RequestParam(required = false) String searchKeyword,
                                       RedirectAttributes redirectAttributes) {

        // 최종 등록 (서비스에서 중복 + 서열 다시 검증)
        adSalaryPolicyService.registerSalaryPolicy(requestDTO);

        // 사용자에게 메시지 전달 (1회성)
        redirectAttributes.addFlashAttribute("successMessage", "기본급 정책이 등록되었습니다.");

        // 기존 검색 조건 유지
        addSearchCondition(redirectAttributes, searchPage, searchSize, searchHeadDeptId,
                searchDeptId, searchPositionId, searchGradeId, searchKeyword);

        return "redirect:/admin/payroll/salary-policy";
    }

    // 수정 모달용 단건 조회 - 수정 버튼 클릭 시 실행되며, 수정 모달에 기존 정책 정보를 채우기 위한 용도
    @GetMapping("/salary-policy/{policyId}")
    @ResponseBody
    public SalaryPolicyResponseDTO getSalaryPolicyDetail(@PathVariable Long policyId) {

        // 특정 정책 상세 조회 (모달에 채우기)
        return adSalaryPolicyService.getSalaryPolicyDetail(policyId);
    }

    // 수정 시 서열 검증 (AJAX)
    @GetMapping("/salary-policy/check-grade-order-update")
    @ResponseBody
    public boolean checkGradeOrderForUpdate(SalaryPolicyRequestDTO requestDTO) {

        // 수정 시에는 "자기 자신 제외"하고 서열 검증
        return adSalaryPolicyService.isValidGradeOrderForUpdateCheck(requestDTO);
    }

    // 수정 처리
    @PostMapping("/salary-policy/update")
    public String updateSalaryPolicy(@ModelAttribute SalaryPolicyRequestDTO requestDTO,
                                     @RequestParam(defaultValue = "1") int searchPage,
                                     @RequestParam(defaultValue = "10") int searchSize,
                                     @RequestParam(required = false) String searchHeadDeptId,
                                     @RequestParam(required = false) String searchDeptId,
                                     @RequestParam(required = false) String searchPositionId,
                                     @RequestParam(required = false) String searchGradeId,
                                     @RequestParam(required = false) String searchKeyword,
                                     RedirectAttributes redirectAttributes) {

        // 수정 로직 실행
        // 기존 데이터 비활성화 + 새 정책 등록 방식
        adSalaryPolicyService.updateSalaryPolicy(requestDTO);

        redirectAttributes.addFlashAttribute("successMessage", "기본급 정책이 수정되었습니다.");

        // 검색 조건 유지
        addSearchCondition(redirectAttributes, searchPage, searchSize,  searchHeadDeptId,
                searchDeptId, searchPositionId, searchGradeId, searchKeyword);

        return "redirect:/admin/payroll/salary-policy";
    }

    // 삭제 처리
    @PostMapping("/salary-policy/delete")
    public String deleteSalaryPolicy(@RequestParam Long policyId,
                                     @RequestParam(defaultValue = "1") int searchPage,
                                     @RequestParam(defaultValue = "10") int searchSize,
                                     @RequestParam(required = false) String searchHeadDeptId,
                                     @RequestParam(required = false) String searchDeptId,
                                     @RequestParam(required = false) String searchPositionId,
                                     @RequestParam(required = false) String searchGradeId,
                                     @RequestParam(required = false) String searchKeyword,
                                     RedirectAttributes redirectAttributes) {

        // 실제 삭제
        adSalaryPolicyService.deleteSalaryPolicy(policyId);

        redirectAttributes.addFlashAttribute("successMessage", "기본급 정책이 삭제되었습니다.");

        addSearchCondition(redirectAttributes, searchPage, searchSize, searchHeadDeptId,
                searchDeptId, searchPositionId, searchGradeId, searchKeyword);

        return "redirect:/admin/payroll/salary-policy";
    }

    // 검색 조건 유지 공통 메서드
    private void addSearchCondition(RedirectAttributes redirectAttributes,
                                    int page,
                                    int size,
                                    String headDeptId,
                                    String deptId,
                                    String positionId,
                                    String gradeId,
                                    String keyword) {

        // 페이지 유지
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);

        // 검색 조건 유지 (null/빈값 제외)
        if (headDeptId != null && !headDeptId.isBlank()) {
            redirectAttributes.addAttribute("headDeptId", headDeptId);
        }

        if (deptId != null && !deptId.isBlank()) {
            redirectAttributes.addAttribute("deptId", deptId);
        }

        if (positionId != null && !positionId.isBlank()) {
            redirectAttributes.addAttribute("positionId", positionId);
        }

        if (gradeId != null && !gradeId.isBlank()) {
            redirectAttributes.addAttribute("gradeId", gradeId);
        }

        if (keyword != null && !keyword.isBlank()) {
            redirectAttributes.addAttribute("keyword", keyword);
        }
    }
}
