package com.ict06.team1_fin_pj.domain.attendance.scheduler;

import com.ict06.team1_fin_pj.domain.attendance.service.LeaveService;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 연차 자동 부여 Scheduler
 *
 * 역할:
 * - 연차 자동 부여 예약 작업 처리
 * - 매년 1월 1일 연차 자동 지급
 * - 매월 신입사원 월차 자동 지급
 */

// Lombok 로그 객체(log) 자동 생성
// log.info(), log.error() 등을 사용가능
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveScheduler {

    /**
     * 연차 관련 Service
     */
    private final LeaveService leaveService;

    /**
     * 매년 1월 1일 00시에 전체 사원 연차 자동 부여
     *
     * cron 형식:
     * 초 분 시 일 월 요일
     *
     * 0 0 0 1 1 *
     * → 매년 1월 1일 00:00 실행
     */
    @Scheduled(cron = "0 0 0 1 1 *")
    public void grantAnnualLeaveEveryYear() {

        log.info("정기 연차 자동 부여 Scheduler 시작");

        // 전체 사원 연차 자동 부여 실행
        int grantedCount =
                leaveService.grantAnnualLeaveForAllEmployees();

        log.info("정기 연차 자동 부여 완료 인원 수 = {}", grantedCount);
        log.info("정기 연차 자동 부여 Scheduler 종료");
    }

    /**
     * 매월 1일 신입사원 월차 자동 부여
     *
     * 대상:
     * - 입사 1년 미만 재직자
     *
     * 실행 시점:
     * - 매월 1일 00시
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void grantMonthlyLeaveForNewEmployees() {

        log.info("신입사원 월차 자동 부여 Scheduler 시작");

        // 1년 미만 신입사원 월차 자동 부여 실행
        int grantedCount =
                leaveService.grantMonthlyLeaveForNewEmployees();

        // 월차 자동 부여 결과 로그 출력
        log.info("신입사원 월차 자동 부여 완료 인원 수 = {}", grantedCount);
        log.info("신입사원 월차 자동 부여 Scheduler 종료");

    }
}