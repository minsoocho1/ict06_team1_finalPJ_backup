package com.ict06.team1_fin_pj.domain.attendance.repository;

import com.ict06.team1_fin_pj.domain.attendance.entity.AttendanceStatus;
import com.ict06.team1_fin_pj.domain.attendance.entity.AttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// 근태 DB 접근용 Repository
// JpaRepository<엔티티, PK타입>
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Integer>,
        AttendanceRepositoryCustom {

    // 1. 출근 중복 체크 (하루 1번)
    // empNo + workDate 기준으로 조회, 있으면 이미 출근한 상태
    Optional<AttendanceEntity> findByEmployee_EmpNoAndWorkDate(String empNo, LocalDate workDate);

    // 2. 내 근태 목록 조회
    // 사번(empNo) 기준 전체 근태 조회
    List<AttendanceEntity> findByEmployee_EmpNo(String empNo);

    // 3. 퇴근 처리용 조회
    // 오늘 근태 1건 조회 (퇴근 찍을 때 필요)
    Optional<AttendanceEntity> findByEmployee_EmpNoAndWorkDateOrderByCheckInAtDesc(
            String empNo,
            LocalDate workDate
    );

    // 상태별 근태 건수 조회
    // 예: ON_TIME 몇 건인지, LATE 몇 건인지 조회할 때 사용
    long countByStatus(AttendanceStatus status);

    // 특정 근무일 기준 근태 목록 조회
    // 오늘 출근 현황 계산에서 사용
    List<AttendanceEntity> findByWorkDate(LocalDate workDate);

}