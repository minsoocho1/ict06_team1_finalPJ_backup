package com.ict06.team1_fin_pj.domain.calendar.repository;

import com.ict06.team1_fin_pj.domain.calendar.entity.ScheduleEntity;
import com.ict06.team1_fin_pj.domain.calendar.entity.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalendarRepository extends JpaRepository<ScheduleEntity, Integer> {

    // 로그인 사용자가 조회할 수 있는 일정만 가져온다.
    // 내 일정, 같은 부서 공개 개인일정, 부서일정, 전사일정을 캘린더에 표시한다
    @Query("""
            SELECT s
              FROM ScheduleEntity s
              LEFT JOIN FETCH s.creator c
              LEFT JOIN FETCH s.department d
              WHERE s.isDeleted = false
                AND (
                      c.empNo = :empNo
                      OR (
                           s.type = :personalType
                           AND s.isPublic = true
                           AND c.department.deptId = :deptId
                      )
                      OR (
                          s.type = :departmentType
                          AND d.deptId = :deptId
                      )
                      OR s.type = :companyType
                )
            ORDER BY s.startTime ASC
            """)
    List<ScheduleEntity> findVisibleSchedules(
            @Param("empNo") String empNo,
            @Param("deptId") Integer deptId,
            @Param("personalType") ScheduleType personalType,
            @Param("departmentType") ScheduleType departmentType,
            @Param("companyType") ScheduleType companyType
    );
}
