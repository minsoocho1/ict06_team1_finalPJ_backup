package com.ict06.team1_fin_pj.domain.attendance.controller;

import com.ict06.team1_fin_pj.common.dto.attendance.AttendanceDTO;
import com.ict06.team1_fin_pj.domain.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
//@RequestMapping("/api/attendance") // 공통 url이 api 로 시작하면 jwt 설정에 의해 차단됩니다.
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // 출근 API
    @PostMapping("/check-in")
    public String checkIn(@RequestBody Map<String, Object> body) {
        String empNo = (String) body.get("empNo");
        Double lat = Double.valueOf(body.get("lat").toString());
        Double lng = Double.valueOf(body.get("lng").toString());

        attendanceService.checkIn(empNo, lat, lng);
        return "출근 완료";
    }
//    @PostMapping("/check-in")
//    public String checkIn(
//            @RequestParam String empNo,
//            @RequestParam Double lat,
//            @RequestParam Double lng
//    ) {
//        attendanceService.checkIn(empNo, lat, lng);
//        return "출근 완료";
//    }

    // 퇴근 API
    @PostMapping("/check-out")
    public String checkOut(@RequestBody Map<String, Object> body) {
        String empNo = (String) body.get("empNo");
        Double lat = Double.valueOf(body.get("lat").toString());
        Double lng = Double.valueOf(body.get("lng").toString());
        attendanceService.checkOut(empNo, lat, lng);
        return "퇴근 완료";
    }
//    @PostMapping("/check-out")
//    public String checkOut(
//            @RequestParam String empNo,
//            @RequestParam Double lat,
//            @RequestParam Double lng
//    ) {
//        attendanceService.checkOut(empNo, lat, lng);
//        return "퇴근 완료";
//    }

    // 내 근태 목록 조회 API
    @GetMapping("/my")
    public List<AttendanceDTO> getMyAttendance(@RequestParam String empNo) {

        // empNo(사번)를 기준으로 근태 목록 조회
        return attendanceService.getMyAttendance(empNo);
    }

}