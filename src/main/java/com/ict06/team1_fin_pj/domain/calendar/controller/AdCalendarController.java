/**
 * @author : 송영은
 * description : 관리자용 일정 관리 컨트롤러
 * ========================================
 * DATE         AUTHOR      NOTE
 * 2026-04-24   송영은       최초 생성
 **/

package com.ict06.team1_fin_pj.domain.calendar.controller;

import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleListResponseDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleUpdateRequestDto;
import com.ict06.team1_fin_pj.domain.calendar.service.AdCalendarService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.io.IOException;
import java.util.List;

@RequestMapping("/admin/calendar")
@Controller
public class AdCalendarController {

    private final AdCalendarService service;

    public AdCalendarController(AdCalendarService service) {
        this.service = service;
    }

    // 관리자 일정 관리 메인 화면
    // 참석자 응답 처리에서 현재 로그인 관리자 사번을 JS가 사용할 수 있게 화면에 내려준다.
    @RequestMapping("/main")
    public String calendarMain(
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            @AuthenticationPrincipal PrincipalDetails principal
    ) throws ServletException, IOException {
        System.out.println("[AdCalendarController] - calendarMain()");

        if (principal != null && principal.getEmpNo() != null) {
            model.addAttribute("adminLoginEmpNo", principal.getEmpNo());
        }

        return "admin/calendar/calendarMain";
    }

    // 관리자 캘린더 일정 목록 API
    // 개인 비공개 일정을 제외한 관리자 관리 대상 일정만 내려준다.
    @GetMapping("/schedules")
    @ResponseBody
    public List<ScheduleListResponseDto> getAdminScheduleList(
            @RequestParam(required = false) List<String> selectedMemberNos,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        System.out.println("[AdCalendarController] - getAdminScheduleList()");

        if (principal == null || principal.getEmpNo() == null) {
            throw new IllegalArgumentException("로그인 관리자 정보가 없습니다.");
        }

        return service.getAdminScheduleList(selectedMemberNos, principal.getEmpNo());
    }

    // 관리자 일정 등록 API
    // 사용자 캘린더 등록 DTO를 그대로 재사용해서 관리자 전용 등록 화면과 연결한다.
    @PostMapping("/schedules")
    @ResponseBody
    public Integer createAdminSchedule(
            @RequestBody ScheduleCreateRequestDto dto,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        System.out.println("[AdCalendarController] - createAdminSchedule()");

        if (principal == null || principal.getEmpNo() == null) {
            throw new IllegalArgumentException("로그인 관리자 정보가 없습니다.");
        }

        return service.createAdminSchedule(dto, principal.getEmpNo());
    }

    // 관리자 일정 수정 API
    // 개인 비공개 일정은 서비스에서 한 번 더 차단한다.
    @PutMapping("/schedules/{scheduleId}")
    @ResponseBody
    public Integer updateAdminSchedule(
            @PathVariable Integer scheduleId,
            @RequestBody ScheduleUpdateRequestDto dto,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        System.out.println("[AdCalendarController] - updateAdminSchedule()");

        if (principal == null || principal.getEmpNo() == null) {
            throw new IllegalArgumentException("로그인 관리자 정보가 없습니다.");
        }

        return service.updateAdminSchedule(scheduleId, dto, principal.getEmpNo());
    }

    // 관리자 일정 삭제 API
    // 삭제 시 참석자 연결 정보도 서비스에서 함께 정리한다.
    @DeleteMapping("/schedules/{scheduleId}")
    @ResponseBody
    public void deleteAdminSchedule(
            @PathVariable Integer scheduleId,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        System.out.println("[AdCalendarController] - deleteAdminSchedule()");

        if (principal == null || principal.getEmpNo() == null) {
            throw new IllegalArgumentException("로그인 관리자 정보가 없습니다.");
        }

        service.deleteAdminSchedule(scheduleId, principal.getEmpNo());
    }
}
