package com.ict06.team1_fin_pj.domain.calendar.controller;

import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleListResponseDto;
import com.ict06.team1_fin_pj.common.dto.calendar.ScheduleUpdateRequestDto;
import com.ict06.team1_fin_pj.domain.calendar.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    // 일정 등록
    @PostMapping("/create")
    public Integer createSchedule(@RequestBody ScheduleCreateRequestDto dto) {
        System.out.println("CalendarController - createSchedule()");

        return service.createSchedule(dto);
    }

    // 일정 목록 조회
    // 로그인 사용자 기준으로 캘린더에 표시할 일정을 내려준다.
    @GetMapping("/list")
    public List<ScheduleListResponseDto> getScheduleList(@RequestParam String empNo) {
        System.out.println("CalendarController - getScheduleList()");

        return service.getScheduleList(empNo);
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
}