/**
 * @author : 송영은
 * description : 관리자용 근태 관리 컨트롤러
 * ========================================
 * DATE         AUTHOR      NOTE
 * 2026-04-24   송영은       최초 생성
 * 2026-05-06   조민수       기능개발
 **/

package com.ict06.team1_fin_pj.domain.attendance.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceSummaryDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceSearchDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdDepartmentWorkHourDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceStatisticsDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdMonthlyAttendanceTrendDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdDepartmentLateRateDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdTodayAttendanceStatusDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceUpdateRequestDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceChangeLogDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdLeaveStatusDTO;

import com.ict06.team1_fin_pj.domain.attendance.service.LeaveService;
import com.ict06.team1_fin_pj.domain.attendance.service.AdAttendanceService;
import com.ict06.team1_fin_pj.domain.employee.entity.DepartmentEntity;
import com.ict06.team1_fin_pj.domain.employee.repository.DepartmentRepository;

import java.security.Principal;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;

@RequestMapping("/admin/attendance")
@Controller
public class AdAttendanceController {

    // 관리자 근태 Service
    private final AdAttendanceService adAttendanceService;

    // 부서 Repository
    // 관리자 근태 검색의 부서 select box에 사용
    private final DepartmentRepository departmentRepository;

    // 연차/휴가 현황 조회 Service
    private final LeaveService leaveService;

    // 생성자 주입
    public AdAttendanceController(
            AdAttendanceService adAttendanceService,
            DepartmentRepository departmentRepository,
            LeaveService leaveService
    ) {
        this.adAttendanceService = adAttendanceService;
        this.departmentRepository = departmentRepository;
        this.leaveService = leaveService;
    }

    // [근태 관리 메인]
    @RequestMapping("/main")
    public String attendanceMain(
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,

            // 검색 조건: 근무일
            @RequestParam(required = false) String workDate,

            // 검색 조건: 부서 ID
            @RequestParam(required = false) String deptId,

            // 검색 조건: 근태 상태
            @RequestParam(required = false) String status,

            // 검색 조건: 사원명 검색어
            @RequestParam(required = false) String keyword,

            // 검색 조건: 정렬 기준
            // latest : 최신 근무일순
            // oldest : 오래된 근무일순
            // empNo  : 사번순
            // name   : 이름순
            @RequestParam(required = false, defaultValue = "latest") String sortType,

            // 현재 페이지 번호
            // Spring Data JPA Page는 0부터 시작
            // 화면에서는 1페이지처럼 보이지만 URL/내부 값은 0부터 사용
            @RequestParam(required = false, defaultValue = "0") int page,

            // 한 페이지당 데이터 개수
            @RequestParam(required = false, defaultValue = "10") int size

    )
            throws ServletException, IOException {
        System.out.println("<<< AdAttendanceController - attendanceMain() >>>");

        // 관리자 근태 상단 요약 카드 조회
        AdAttendanceSummaryDTO summary = adAttendanceService.getAttendanceSummary();

        model.addAttribute("totalCount", summary.getTotalCount());
        model.addAttribute("onTimeCount", summary.getOnTimeCount());
        model.addAttribute("lateCount", summary.getLateCount());
        model.addAttribute("earlyCount", summary.getEarlyCount());


        // 전체 부서 목록 조회
        // 관리자 근태 검색 화면의 부서 select box 데이터로 사용
        List<DepartmentEntity> departmentList =
                departmentRepository.findAllByOrderByDeptIdAsc();

        // Thymeleaf로 전달
        // attendanceMain.html 에서 departmentList 사용 가능
        model.addAttribute("departmentList", departmentList);

        // 검색 조건 DTO 생성
        AdAttendanceSearchDTO searchDTO = new AdAttendanceSearchDTO();
        searchDTO.setWorkDate(workDate);
        searchDTO.setDeptId(deptId);
        searchDTO.setStatus(status);
        searchDTO.setKeyword(keyword);
        // 정렬 조건 저장
        searchDTO.setSortType(sortType);

        // 관리자 근태 목록 조회
        // Service에서 PageImpl<AdAttendanceDTO> 형태로 반환
        Page<AdAttendanceDTO> attendancePage =
                adAttendanceService.getAttendanceList(searchDTO, page, size);

        // 실제 테이블에 출력할 목록
        List<AdAttendanceDTO> attendanceList = attendancePage.getContent();

        // Thymeleaf 로 전달
        model.addAttribute("attendanceList", attendanceList);

        // Thymeleaf 페이징 UI에서 사용할 Page 객체 전달
        model.addAttribute("attendancePage", attendancePage);

        // 검색 버튼을 눌렀을 때 입력한 값이 화면에 그대로 남도록 전달
        // 검색 조건 유지용 DTO 전달
        // 화면에서 searchDTO.workDate, searchDTO.deptId 처럼 사용 가능
        model.addAttribute("searchDTO", searchDTO);

        // 사이드바 현재 메뉴 표시용
        model.addAttribute("activeAttendanceMenu", "main");

        return "admin/attendance/attendanceMain";
    }

    /**
     * 관리자 근태 현황 Excel 다운로드
     *
     * 요청 경로:
     * GET /admin/attendance/excel
     *
     * 역할:
     * - 관리자 근태 현황 화면의 검색 조건을 그대로 받아온다.
     * - 조건에 맞는 근태 데이터를 Excel(.xlsx) 파일로 생성한다.
     * - 생성된 파일을 브라우저에서 다운로드하도록 응답한다.
     */
    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadAttendanceExcel(

            // 검색 조건: 근무일
            @RequestParam(required = false) String workDate,

            // 검색 조건: 부서 ID
            @RequestParam(required = false) String deptId,

            // 검색 조건: 근태 상태
            @RequestParam(required = false) String status,

            // 검색 조건: 사원명 검색어
            @RequestParam(required = false) String keyword,

            // 정렬 조건
            @RequestParam(required = false, defaultValue = "latest") String sortType
    ) {
        // 1. 검색 조건 DTO 생성
        // 화면에서 검색한 조건과 동일한 조건으로 Excel을 내려받기 위함
        AdAttendanceSearchDTO searchDTO = new AdAttendanceSearchDTO();
        searchDTO.setWorkDate(workDate);
        searchDTO.setDeptId(deptId);
        searchDTO.setStatus(status);
        searchDTO.setKeyword(keyword);
        searchDTO.setSortType(sortType);

        // 2. Service에서 Excel byte 배열 생성
        byte[] excelBytes =
                adAttendanceService.downloadAttendanceExcel(searchDTO);

        // 3. 다운로드 파일명 생성
        String today =
                LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String fileName =
                "attendance_list_" + today + ".xlsx";

        // 4. 한글/특수문자 파일명 깨짐 방지를 위한 인코딩
        String encodedFileName =
                URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                        .replaceAll("\\+", "%20");

        // 5. Excel 파일 다운로드 응답 반환
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excelBytes);
    }

    /**
     * 관리자 연차/휴가 현황 Excel 다운로드
     *
     * 요청 경로:
     * GET /admin/attendance/leave/excel
     *
     * 역할:
     * - 관리자 연차/휴가 현황 화면의 검색 조건을 그대로 받아온다.
     * - 조건에 맞는 연차/휴가 데이터를 Excel(.xlsx) 파일로 생성한다.
     * - 생성된 파일을 브라우저에서 다운로드하도록 응답한다.
     */
    @GetMapping("/leave/excel")
    public ResponseEntity<byte[]> downloadLeaveExcel(

            // 사원명 검색어
            @RequestParam(required = false) String keyword,

            // 부서 검색 조건
            @RequestParam(required = false) Integer deptId,

            // 정렬 조건
            @RequestParam(required = false, defaultValue = "empNo") String sortType
    ) {
        // 1. Service에서 Excel byte 배열 생성
        byte[] excelBytes =
                leaveService.downloadLeaveExcel(
                        keyword,
                        deptId,
                        sortType
                );

        // 2. 다운로드 파일명 생성
        // 예: leave_status_20260520.xlsx
        String today =
                LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String fileName =
                "leave_status_" + today + ".xlsx";

        // 3. 한글/특수문자 파일명 깨짐 방지를 위한 인코딩
        String encodedFileName =
                URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                        .replaceAll("\\+", "%20");

        // 4. Excel 파일 다운로드 응답 반환
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excelBytes);
    }

    // [관리자 연차/휴가 현황 화면]
    @GetMapping("/leave")
    public String attendanceLeave(

            // 사원명 검색어
            @RequestParam(required = false) String keyword,

            // 부서 검색 조건
            @RequestParam(required = false) Integer deptId,

            // 검색시 정렬조건
            @RequestParam(required = false, defaultValue = "empNo") String sortType,

            // 현재 페이지 번호
            // Spring Data Page는 0부터 시작한다.
            @RequestParam(required = false, defaultValue = "0") int page,

            // 한 페이지당 데이터 개수
            @RequestParam(required = false, defaultValue = "10") int size,

            Model model
    ) {

        // 사원별 총 연차 / 사용 연차 / 잔여 연차 목록 조회
        // 검색 조건이 없으면 전체 조회된다.
        // 관리자 연차 현황 Page 조회
        Page<AdLeaveStatusDTO> leaveStatusPage =
                leaveService.findAdminLeaveStatusList(
                        keyword,
                        deptId,
                        sortType,
                        page,
                        size
                );

        // 현재 페이지에 출력할 목록
        List<AdLeaveStatusDTO> leaveStatusList =
                leaveStatusPage.getContent();

        // 전체 부서 목록 조회
        // 검색 영역의 부서 select box에 사용한다.
        List<DepartmentEntity> departmentList =
                departmentRepository.findAllByOrderByDeptIdAsc();

        // 연차 현황 목록 전달
        model.addAttribute("leaveStatusList", leaveStatusList);

        // 페이징 UI에서 사용할 Page 객체
        model.addAttribute("leaveStatusPage", leaveStatusPage);

        // 현재 선택한 목록 개수 유지용
        model.addAttribute("size", size);

        // 부서 select box 데이터 전달
        model.addAttribute("departmentList", departmentList);

        // 검색 후 입력값이 화면에 유지되도록 전달
        model.addAttribute("keyword", keyword);
        model.addAttribute("deptId", deptId);
        model.addAttribute("sortType", sortType);

        // 사이드바 현재 메뉴 표시용
        model.addAttribute("activeAttendanceMenu", "leave");

        // 관리자 연차/휴가 현황 화면 반환
        return "admin/attendance/attendanceLeave";
    }

    /**
     * 관리자용 전체 사원 연차 자동 부여 테스트
     *
     * 요청 경로:
     * POST /admin/attendance/leave/grant-all
     *
     * 역할:
     * - 재직 중인 전체 사원을 대상으로 연차를 자동 부여한다.
     * - 이미 올해 연차가 부여된 사원은 중복 생성하지 않는다.
     */
    @PostMapping("/leave/grant-all")
    public String grantAnnualLeaveForAllEmployees(
            RedirectAttributes redirectAttributes
    ) {

        // 전체 사원 연차 자동 부여 실행
        int grantedCount =
                leaveService.grantAnnualLeaveForAllEmployees();

        // 성공 메시지 생성
        String message =
                grantedCount + "명의 사원에게 연차가 자동 부여되었습니다.";

        // redirect 후 1회성 메시지 전달
        redirectAttributes.addFlashAttribute(
                "successMessage",
                message
        );

        // 처리 후 관리자 연차 현황 화면으로 다시 이동
        return "redirect:/admin/attendance/leave";
    }

    // [관리자 근태 통계 화면]
    @RequestMapping("/statistics")
    public String attendanceStatistics(Model model) {

        // 관리자 근태 통계 페이지 진입 확인용 로그
        System.out.println("<<< AdAttendanceController - attendanceStatistics() >>>");

        // 관리자 근태 통계 조회
        AdAttendanceStatisticsDTO statistics =
                adAttendanceService.getAttendanceStatistics();

        // 오늘 출근 현황 조회
        AdTodayAttendanceStatusDTO todayStatus =
                adAttendanceService.getTodayAttendanceStatus();

        // Thymeleaf로 전달
        model.addAttribute("statistics", statistics);
        model.addAttribute("todayStatus", todayStatus);

        // 상태별 비율 차트용 데이터 생성
        Map<String, Object> statusChartData = Map.of(
                "onTimeRate", statistics.getOnTimeRate(),
                "lateRate", statistics.getLateRate(),
                "earlyRate", statistics.getEarlyRate()
        );

        // ObjectMapper : Java 객체(Map)를 JSON 문자열로 바꿔주는 객체
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String statusChartJson = objectMapper.writeValueAsString(statusChartData);

            // Thymeleaf로 전달
            model.addAttribute("statusChartJson", statusChartJson);

        } catch (JsonProcessingException e) {

            // JSON 변환 실패 시에도 화면이 죽지 않도록
            // 빈 JSON 형태로 전달한다.
            model.addAttribute("statusChartJson", "{}");
        }


        // 부서별 평균 근무시간 차트용 데이터 조회
        List<AdDepartmentWorkHourDTO> departmentWorkHourList =
                adAttendanceService.getDepartmentAverageWorkHours();

        // Java 객체(List)를 JSON 문자열로 변환
        try {
            String departmentWorkHourJson =
                    objectMapper.writeValueAsString(departmentWorkHourList);


            // Thymeleaf로 전달
            model.addAttribute("departmentWorkHourJson", departmentWorkHourJson);

        } catch (JsonProcessingException e) {

            // JSON 변환 실패 시 화면이 죽지 않도록 빈 배열 전달
            model.addAttribute("departmentWorkHourJson", "[]");
        }

        // 월별 근태 추이 차트용 데이터 조회
        List<AdMonthlyAttendanceTrendDTO> monthlyTrendList =
                adAttendanceService.getMonthlyAttendanceTrend();

        // 월별 근태 추이 데이터를 JSON 문자열로 변환한다.
        try {
            String monthlyTrendJson =
                    objectMapper.writeValueAsString(monthlyTrendList);

            // Thymeleaf로 전달
            model.addAttribute("monthlyTrendJson", monthlyTrendJson);

        } catch (JsonProcessingException e) {

            // JSON 변환 실패 시 화면이 죽지 않도록 빈 배열 전달
            model.addAttribute("monthlyTrendJson", "[]");
        }

        // 부서별 지각률 차트용 데이터 조회
        List<AdDepartmentLateRateDTO> departmentLateRateList =
                adAttendanceService.getDepartmentLateRates();

        // 부서별 지각률 데이터를 JSON 문자열로 변환한다.
        try {
            String departmentLateRateJson =
                    objectMapper.writeValueAsString(departmentLateRateList);

            // Thymeleaf로 전달
            model.addAttribute("departmentLateRateJson", departmentLateRateJson);

        } catch (JsonProcessingException e) {

            // JSON 변환 실패 시 화면이 죽지 않도록 빈 배열 전달
            model.addAttribute("departmentLateRateJson", "[]");
        }

        // 사이드바 현재 메뉴 표시용
        model.addAttribute("activeAttendanceMenu", "statistics");

        // 관리자 근태 통계 화면 반환
        return "admin/attendance/attendanceStatistics";
    }

    /**
     * 관리자 근태 수정 처리
     *
     * 화면에서 수정 Modal 저장 버튼을 누르면
     * 이 주소로 수정 데이터가 들어온다.
     *
     * 요청 주소:
     * POST /admin/attendance/update
     */
    @PostMapping("/update")
    public ResponseEntity<String> updateAttendance(
            @RequestBody AdAttendanceUpdateRequestDTO request,
            Principal principal
    ) {
        /*
         * principal.getName()
         * 현재 로그인한 관리자 사번을 가져온다.
         *
         * 예:
         * 로그인한 관리자 사번이 20260001이면
         * adminEmpNo = "20260001"
         */
        String adminEmpNo = principal.getName();

        /*
         * Service로 수정 요청을 넘긴다.
         *
         * 여기서 실제로:
         * - ATTENDANCE 수정
         * - ATTENDANCE_CHANGE_LOG 저장
         * 이 처리된다.
         */
        adAttendanceService.updateAttendanceByAdmin(
                request,
                adminEmpNo
        );

        /*
         * 화면/JS에 성공 메시지 반환
         */
        return ResponseEntity.ok("근태 수정 완료");
    }

    /**
     * 관리자 근태 수정 이력 조회 화면
     *
     * ATTENDANCE_CHANGE_LOG에 저장된
     * 관리자 수정 이력을 최신순으로 페이징 조회한다.
     *
     * 요청 예:
     * /admin/attendance/log?page=0&size=10
     */
    @GetMapping("/log")
    public String attendanceChangeLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        // 1. 변경 이력 Page 조회
        Page<AdAttendanceChangeLogDTO> changeLogPage =
                adAttendanceService.getAttendanceChangeLogs(page, size);

        // 2. 화면 반복 출력용 리스트
        model.addAttribute("changeLogList", changeLogPage.getContent());

        // 3. 페이징 정보 전체
        model.addAttribute("changeLogPage", changeLogPage);

        // 4. 현재 size 유지용
        model.addAttribute("size", size);

        return "admin/attendance/attendanceLog";
    }
}
