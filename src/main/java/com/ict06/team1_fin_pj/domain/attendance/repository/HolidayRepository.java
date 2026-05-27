package com.ict06.team1_fin_pj.domain.attendance.repository;

import com.ict06.team1_fin_pj.domain.attendance.entity.HolidayEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

/*
 * 공휴일 Repository
 *
 * HOLIDAY 테이블을 조회하기 위한 Repository이다.
 * 결근 Scheduler, 연차 계산에서 공휴일 여부를 확인할 때 사용한다.
 */
public interface HolidayRepository extends JpaRepository<HolidayEntity, Integer> {

    /*
     * 특정 날짜가 사용 중인 공휴일인지 확인한다.
     *
     * 사용 예:
     * - 결근 Scheduler 실행 전 오늘이 공휴일인지 확인
     * - 연차 계산 시 기간 안의 공휴일 제외
     */
    boolean existsByHolidayDateAndIsActiveTrue(LocalDate holidayDate);
}