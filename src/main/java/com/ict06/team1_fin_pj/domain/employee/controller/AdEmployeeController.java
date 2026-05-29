package com.ict06.team1_fin_pj.domain.employee.controller;

import com.ict06.team1_fin_pj.common.dto.employee.EmployeeCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.employee.EmployeeSearchConditionDto;
import com.ict06.team1_fin_pj.common.dto.employee.EmployeeUpdateRequestDto;
import com.ict06.team1_fin_pj.common.dto.employee.HrSelectOptionDto;
import com.ict06.team1_fin_pj.domain.employee.service.AdEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.ict06.team1_fin_pj.common.dto.employee.EmployeeListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

/*
 * 관리자 인사관리 컨트롤러
 *
 * /admin/employees 로 시작하는 요청을 처리한다.
 *
 * Controller의 역할:
 * - 사용자의 요청을 받음
 * - Service에게 실제 기능 처리를 맡김
 * - 처리 결과를 화면으로 전달함
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/employees")
public class AdEmployeeController {

    /*
     * 인사관리 서비스
     *
     * 실제 사원 조회, 등록, 수정 등의 기능은
     * AdEmployeeServiceImpl에서 처리한다.
     */
    private final AdEmployeeService adEmployeeService;

    /*
     * 사원 목록 화면
     *
     * GET /admin/employees
     *
     * 검색 조건을 받아서 사원 목록을 조회하고,
     * 부서/직급/권한 select 박스 데이터도 함께 화면에 전달한다.
     *
     * 페이징:
     * - page: 현재 페이지 번호, 0부터 시작
     * - size: 한 페이지에 보여줄 사원 수
     *
     * 예:
     * /admin/employees?page=0&size=10
     * /admin/employees?keyword=홍길동&page=1&size=10
     */
    @GetMapping
    public String employeeList(
            @ModelAttribute EmployeeSearchConditionDto conditionDto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        /*
         * 페이지 번호 보정
         *
         * page는 0부터 시작한다.
         * 혹시 음수 값이 들어오면 0으로 보정한다.
         */
        if (page < 0) {
            page = 0;
        }

        /*
         * 한 페이지 표시 개수 보정
         *
         * 너무 큰 값이 들어오면 화면이 무거워질 수 있으므로
         * 10, 20, 50 정도만 허용하는 방식도 가능하다.
         * 여기서는 기본 10명으로 사용한다.
         */
        if (size <= 0) {
            size = 10;
        }

        Pageable pageable = PageRequest.of(page, size);

        // 검색 조건과 페이지 정보에 맞는 사원 목록 조회
        Page<EmployeeListDto> employeePage =
                adEmployeeService.findEmployees(conditionDto, pageable);

        /*
         * 테이블에서 사용할 현재 페이지의 사원 목록
         *
         * 기존 list.html은 employees라는 이름으로 반복 출력하고 있으므로
         * 기존 화면 코드를 많이 바꾸지 않기 위해 employees도 유지한다.
         */
        model.addAttribute("employees", employeePage.getContent());

        /*
         * 페이징 UI에서 사용할 Page 객체
         *
         * 전체 페이지 수, 현재 페이지 번호, 전체 데이터 수 등을 가진다.
         */
        model.addAttribute("employeePage", employeePage);

        // 현재 페이지 번호
        model.addAttribute("page", page);

        // 현재 페이지 크기
        model.addAttribute("size", size);
        
        /*
         * 검색 필터용 본부 목록
         *
         * 사원 목록 검색 화면에서는
         * 부서를 본부 / 팀 2단계로 나누어 선택한다.
         */
        model.addAttribute("parentDepartments", adEmployeeService.findParentDepartments());

        /*
         * 사용자가 이미 본부를 선택한 상태라면
         * 해당 본부의 팀 목록도 함께 전달한다.
         *
         * 이렇게 해야 검색 후에도 팀 select 값이 유지된다.
         */
        if (conditionDto.getParentDeptId() != null) {
            model.addAttribute(
                    "teams",
                    adEmployeeService.findTeamsByParentDeptId(conditionDto.getParentDeptId())
            );
        } else {
            model.addAttribute("teams", List.of());
        }

        // 검색 필터 select 박스에 사용할 직급 목록
        model.addAttribute("positions", adEmployeeService.findPositions());

        // 검색 필터 select 박스에 사용할 권한 목록
        model.addAttribute("roles", adEmployeeService.findRoles());

        // 사용자가 선택한 검색 조건을 화면에 다시 유지하기 위해 전달
        model.addAttribute("condition", conditionDto);
        model.addAttribute(
                "statistics",
                adEmployeeService.getEmployeeStatistics()
        );

        // templates/admin/employee/list.html 화면으로 이동
        return "admin/employee/list";
    }

    /*
     * 사원 등록 화면
     *
     * GET /admin/employees/new
     *
     * 빈 등록 DTO와 select 박스 데이터를 화면에 전달한다.
     *
     * 변경 사항:
     * - 기존에는 departments 전체 목록을 전달했지만,
     * - 이제는 본부/팀 2단계 선택 구조이므로 parentDepartments만 먼저 전달한다.
     * - 팀 목록은 본부 선택 시 JS에서 Ajax로 불러온다.
     */
    @GetMapping("/new")
    public String createForm(Model model) {
        // 등록 화면에서 입력값을 담을 빈 DTO
        model.addAttribute("employeeCreateRequestDto", new EmployeeCreateRequestDto());

        /*
         * 등록 화면의 본부 select 박스 데이터
         *
         * parent_dept_id가 null인 부서만 가져온다.
         * 예: 경영본부, 개발본부
         */
        model.addAttribute("parentDepartments", adEmployeeService.findParentDepartments());

        // 등록 화면의 직급 select 박스 데이터
        model.addAttribute("positions", adEmployeeService.findPositions());

        // 등록 화면의 권한 select 박스 데이터
        model.addAttribute("roles", adEmployeeService.findRoles());

        // templates/admin/employee/create.html 화면으로 이동
        return "admin/employee/create";
    }

    /*
     * 본부 선택 시 하위 팀 목록 조회
     *
     * GET /admin/employees/departments/{parentDeptId}/teams
     *
     * 예:
     * /admin/employees/departments/1/teams
     *
     * parentDeptId가 1이면
     * parent_dept_id = 1인 팀 목록을 JSON으로 반환한다.
     *
     * @ResponseBody가 있으므로 HTML 화면이 아니라 JSON 데이터가 응답된다.
     */
    @GetMapping("/departments/{parentDeptId}/teams")
    @ResponseBody
    public List<HrSelectOptionDto> findTeamsByParentDeptId(
            @PathVariable Integer parentDeptId
    ) {
        return adEmployeeService.findTeamsByParentDeptId(parentDeptId);
    }

    /*
     * 사번 자동 생성
     *
     * GET /admin/employees/generate-emp-no?hireDate=2026-05-01
     *
     * 입사일을 기준으로 사번을 자동 생성한다.
     * @ResponseBody가 있으므로 화면 이름이 아니라 문자열 자체를 응답한다.
     */
    @GetMapping("/generate-emp-no")
    @ResponseBody
    public String generateEmpNo(@RequestParam String hireDate) {
        return adEmployeeService.generateEmpNo(hireDate);
    }

    /*
     * 로그인 아이디 자동 생성
     *
     * GET /admin/employees/generate-emp-id?name=홍길동
     *
     * 이름을 기준으로 아이디를 자동 생성한다.
     */
    @GetMapping("/generate-emp-id")
    @ResponseBody
    public String generateEmpId(@RequestParam String name) {
        return adEmployeeService.generateEmpId(name);
    }

    /*
     * 사원 등록 처리
     *
     * POST /admin/employees
     *
     * 등록 화면에서 입력한 값을 받아 사원을 저장한다.
     * 성공하면 목록 화면으로 이동한다.
     * 실패하면 다시 등록 화면으로 돌아간다.
     */
    @PostMapping
    public String createEmployee(
            @ModelAttribute EmployeeCreateRequestDto requestDto,
            Model model
    ) {
        try {
            // 사원 등록 처리
            adEmployeeService.createEmployee(requestDto);

            // 등록 성공 시 사원 목록으로 이동
            return "redirect:/admin/employees";

        } catch (IllegalArgumentException e) {
            /*
             * 등록 중 오류가 발생한 경우
             *
             * Service에서 "에러메시지|필드명" 형태로 예외를 던지고 있다.
             * 예: "이미 사용 중인 이메일입니다.|emailId"
             *
             * 이 값을 나눠서 화면에 전달하면,
             * 어떤 필드에서 오류가 났는지 표시할 수 있다.
             */
            String message = e.getMessage();
            String errorMessage = message;
            String errorField = "";

            if (message != null && message.contains("|")) {
                String[] parts = message.split("\\|");
                errorMessage = parts[0];
                errorField = parts[1];
            }

            // 사용자가 입력했던 값을 다시 화면에 유지
            model.addAttribute("employeeCreateRequestDto", requestDto);

            /*
             * 등록 실패 시에도 본부 select 박스가 다시 보여야 하므로
             * 본부 목록을 다시 전달한다.
             */
            model.addAttribute("parentDepartments", adEmployeeService.findParentDepartments());

            /*
             * 등록 실패 시 사용자가 이미 본부를 선택했다면,
             * 해당 본부의 하위 팀 목록도 다시 전달한다.
             *
             * 이 처리가 없으면 에러 발생 후 돌아왔을 때
             * 팀 select가 비어 보일 수 있다.
             */
            if (requestDto.getParentDeptId() != null) {
                model.addAttribute(
                        "teams",
                        adEmployeeService.findTeamsByParentDeptId(requestDto.getParentDeptId())
                );
            }

            // 등록 화면 select 박스 데이터 다시 전달
            model.addAttribute("positions", adEmployeeService.findPositions());
            model.addAttribute("roles", adEmployeeService.findRoles());

            // 에러 메시지와 에러가 발생한 필드명 전달
            model.addAttribute("errorMessage", errorMessage);
            model.addAttribute("errorField", errorField);

            // 다시 등록 화면으로 이동
            return "admin/employee/create";
        }
    }

    /*
     * 사원 상세 화면
     *
     * GET /admin/employees/{empNo}
     *
     * 특정 사번의 상세 정보를 조회해서 화면에 보여준다.
     */
    @GetMapping("/{empNo}")
    public String employeeDetail(
            @PathVariable String empNo,
            Model model
    ) {
        // 사번으로 사원 상세 정보 조회
        model.addAttribute("employee", adEmployeeService.findEmployeeDetail(empNo));

        // templates/admin/employee/detail.html 화면으로 이동
        return "admin/employee/detail";
    }

    /*
     * 사원 수정 화면
     *
     * GET /admin/employees/{empNo}/edit
     *
     * 기존 사원 정보를 조회해서 수정 화면에 보여준다.
     *
     * 변경 사항:
     * - 수정 화면에서도 본부/팀 2단계 select를 사용한다.
     * - 기존 사원의 팀이 속한 본부를 선택 상태로 보여줘야 한다.
     */
    @GetMapping("/{empNo}/edit")
    public String editForm(
            @PathVariable String empNo,
            Model model
    ) {
        // 수정 화면에 보여줄 기존 사원 정보
        EmployeeUpdateRequestDto updateDto = adEmployeeService.findEmployeeForUpdate(empNo);

        model.addAttribute("employeeUpdateRequestDto", updateDto);

        /*
         * 수정 화면의 본부 select 박스 데이터
         *
         * parent_dept_id가 null인 본부 목록을 전달한다.
         */
        model.addAttribute("parentDepartments", adEmployeeService.findParentDepartments());

        /*
         * 기존 사원이 속한 팀의 상위 본부가 있으면,
         * 그 본부에 속한 팀 목록을 미리 전달한다.
         *
         * 그래야 수정 화면 진입 시
         * 본부와 팀이 기존 값으로 선택되어 보인다.
         */
        if (updateDto.getParentDeptId() != null) {
            model.addAttribute(
                    "teams",
                    adEmployeeService.findTeamsByParentDeptId(updateDto.getParentDeptId())
            );
        }

        // 수정 화면 select 박스 데이터
        model.addAttribute("positions", adEmployeeService.findPositions());
        model.addAttribute("roles", adEmployeeService.findRoles());

        // templates/admin/employee/edit.html 화면으로 이동
        return "admin/employee/edit";
    }

    /*
     * 사원 수정 처리
     *
     * POST /admin/employees/{empNo}/edit
     *
     * 수정 화면에서 입력한 값으로 사원 정보를 수정한다.
     * 성공하면 상세 화면으로 이동한다.
     * 실패하면 다시 수정 화면으로 돌아간다.
     */
    @PostMapping("/{empNo}/edit")
    public String updateEmployee(
            @PathVariable String empNo,
            @ModelAttribute EmployeeUpdateRequestDto requestDto,
            Model model
    ) {
        try {
            // 사원 수정 처리
            adEmployeeService.updateEmployee(empNo, requestDto);

            // 수정 성공 시 해당 사원의 상세 화면으로 이동
            return "redirect:/admin/employees/" + empNo;

        } catch (IllegalArgumentException e) {
            /*
             * 수정 중 오류가 발생한 경우
             *
             * 등록과 마찬가지로
             * "에러메시지|필드명" 형태를 분리해서 화면에 전달한다.
             */
            String message = e.getMessage();
            String errorMessage = message;
            String errorField = "";

            if (message != null && message.contains("|")) {
                String[] parts = message.split("\\|");
                errorMessage = parts[0];
                errorField = parts[1];
            }

            // 사용자가 입력했던 수정 값을 다시 화면에 유지
            model.addAttribute("employeeUpdateRequestDto", requestDto);

            /*
             * 수정 실패 시에도 본부 select 박스가 다시 보여야 하므로
             * 본부 목록을 다시 전달한다.
             */
            model.addAttribute("parentDepartments", adEmployeeService.findParentDepartments());

            /*
             * 수정 실패 시 사용자가 선택했던 본부의 팀 목록도 다시 전달한다.
             *
             * 이 처리가 없으면 에러 발생 후 돌아왔을 때
             * 팀 select가 비어 보일 수 있다.
             */
            if (requestDto.getParentDeptId() != null) {
                model.addAttribute(
                        "teams",
                        adEmployeeService.findTeamsByParentDeptId(requestDto.getParentDeptId())
                );
            }

            // 수정 화면 select 박스 데이터 다시 전달
            model.addAttribute("positions", adEmployeeService.findPositions());
            model.addAttribute("roles", adEmployeeService.findRoles());

            // 에러 메시지와 에러 필드 전달
            model.addAttribute("errorMessage", errorMessage);
            model.addAttribute("errorField", errorField);

            // 다시 수정 화면으로 이동
            return "admin/employee/edit";
        }
    }
}