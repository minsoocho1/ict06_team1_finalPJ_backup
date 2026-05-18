/**
 * @FileName : CalendarRepository.java
 * @Description : 캘린더 일정 관리 Repository
 *                - ScheduleEntity 테이블 데이터 접근 담당
 *                - 일정 CRUD 및 사번/카테고리 기준 조회 처리
 * @Author : 정준하
 * @Date : 2026. 05. 01
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.01    정준하        최초 생성
 * @ 2026.05.14    김다솜        온보딩 일정 연동 조회 메서드 정리
 */

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

    // 특정 사원의 특정 카테고리 일정 조회
    List<ScheduleEntity> findByCreator_EmpNoAndCategory(String empNo, String category);
}
