package com.ict06.team1_fin_pj.domain.attendance.repository;

import com.ict06.team1_fin_pj.domain.attendance.entity.LeaveOccurrenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 연차 발생/사용/잔여 내역 Repository
// interface로 만든다.
// JpaRepository<엔티티명, PK 타입>
public interface LeaveOccurrenceRepository
        extends JpaRepository<LeaveOccurrenceEntity, Integer>,
                LeaveOccurrenceRepositoryCustom  {

    // 특정 사원의 특정 연도 연차 발생 내역 조회
    // LeaveOccurrenceEntity에는 empNo가 직접 없고 employee 안에 있으므로
    // employee.empNo 조건은 findByEmployee_EmpNo 형태로 작성한다.
    List<LeaveOccurrenceEntity> findByEmployee_EmpNoAndTargetYear(
            String empNo,
            Integer targetYear
    );
}