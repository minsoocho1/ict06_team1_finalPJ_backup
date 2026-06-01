package com.ict06.team1_fin_pj.domain.calendar.controller;

import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleListResponseDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleUpdateRequestDto;
import com.ict06.team1_fin_pj.domain.calendar.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.ict06.team1_fin_pj.common.dto.calendar.CalendarHolidayDto;
import com.ict06.team1_fin_pj.common.dto.calendar.CalendarUnavailableEmployeeDto;
import com.ict06.team1_fin_pj.domain.calendar.service.CalendarAvailabilityService;
import com.ict06.team1_fin_pj.domain.calendar.service.CalendarHolidayService;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import java.util.List;

/**
 * 사용자용 캘린더 컨트롤러
 */
@RequestMapping("/calendar")
@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class CalendarController {

    @Autowired
    private CalendarService service;

    @Autowired
    private CalendarAvailabilityService calendarAvailabilityService;

    @Autowired
    private CalendarHolidayService calendarHolidayService;

    // 일정 등록
    @PostMapping("/create")
    public Integer createSchedule(@RequestBody ScheduleCreateRequestDto dto) {
        System.out.println("CalendarController - createSchedule()");

        return service.createSchedule(dto);
    }

    // 일정 목록 조회
    // 로그인 사용자 기준 기본 일정 + 조직도에서 선택한 구성원의 공개 개인일정을 내려준다.
    @GetMapping("/list")
    public List<ScheduleListResponseDto> getScheduleList(
            @RequestParam String empNo,
            @RequestParam(required = false) List<String> selectedMemberNos
    ) {
        System.out.println("CalendarController - getScheduleList()");

        return service.getScheduleList(empNo, selectedMemberNos);
    }

    // 일정 수정
    @PutMapping("/{scheduleId}")
    public Integer updateSchedule(
            // URL에서 수정할 일정 번호를 받음
            @PathVariable Integer scheduleId,
            // 수정 요청을 보낸 로그인 사용자 사번을 받음
            @RequestParam String requesterNo,
            // 프론트가 보낸 수정 데이터(JSON)를 DTO로 받음
            @RequestBody ScheduleUpdateRequestDto dto
    ) {
        System.out.println("CalendarController - updateSchedule()");

        return service.updateSchedule(scheduleId, dto, requesterNo);
    }

    // 일정 삭제
    @DeleteMapping("/{scheduleId}")
    public void deleteSchedule(
            @PathVariable Integer scheduleId,
            // 삭제 요청을 보낸 로그인 사용자 사번을 받음
            @RequestParam String requesterNo
    ) {
        System.out.println("CalendarController - deleteSchedule()");

        service.deleteSchedule(scheduleId, requesterNo);
    }

    // 참석자 응답 상태 변경
    @PatchMapping("/{scheduleId}/participants/status")
    public void updateParticipantStatus(
            @PathVariable Integer scheduleId,
            @RequestParam String empNo,
            @RequestParam String status
    ) {
        System.out.println("CalendarController - updateParticipantStatus()");

        service.updateParticipantStatus(scheduleId, empNo, status);
    }

    @GetMapping("/availability/unavailable-employees")
    public List<CalendarUnavailableEmployeeDto> getUnavailableEmployees(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) List<String> empNos
    ) {
        return calendarAvailabilityService.findUnavailableEmployees(start, end, empNos);
    }

    // 캘린더에 표시할 공휴일 라벨을 조회한다.
    @GetMapping("/holidays")
    public List<CalendarHolidayDto> getCalendarHolidays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return calendarHolidayService.findHolidays(start, end);
    }

    // 캘린더 등록/수정 검증 실패는 서버 오류가 아니라 사용자 입력 차단으로 응답한다.
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException e) {
        return e.getMessage();
    }
}
