package com.ict06.team1_fin_pj.domain.calendar.service;

import com.ict06.team1_fin_pj.common.dto.calendar.CalendarHolidayDto;
import com.ict06.team1_fin_pj.domain.attendance.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 캘린더에서 공휴일 조회/일정 등록 차단을 담당한다.
@Service
@RequiredArgsConstructor
public class CalendarHolidayService {

    private final HolidayRepository holidayRepository;

    public List<CalendarHolidayDto> findHolidays(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return List.of();
        }

        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = toInclusiveEndDate(end);

        if (endDate.isBefore(startDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        return holidayRepository
                .findByHolidayDateBetweenAndIsActiveTrueOrderByHolidayDateAsc(startDate, endDate)
                .stream()
                .map(holiday -> CalendarHolidayDto.builder()
                        .holidayDate(holiday.getHolidayDate())
                        .holidayName(holiday.getHolidayName())
                        .build())
                .toList();
    }

    public void validateNotHoliday(LocalDateTime start, LocalDateTime end) {
        if (!findHolidays(start, end).isEmpty()) {
            throw new IllegalArgumentException("공휴일에는 일정을 등록하거나 수정할 수 없습니다.");
        }
    }

    // FullCalendar end는 exclusive로 들어올 수 있어서 표시 범위 마지막 날짜를 보정한다.
    private LocalDate toInclusiveEndDate(LocalDateTime end) {
        return end.minusNanos(1).toLocalDate();
    }
}