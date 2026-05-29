/**
 * @FileName : AiLogRepository.java
 * @Description : AI 사용 로그 Repository
 *                - AI 비서/챗봇 사용 이력 저장 및 조회
 *                - 성공 여부, 응답 시간, 기능 유형별 사용 통계 조회
 *                - 관리자 AI 운영 대시보드 및 권한 차단 로그 조회에 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.27  송혜진       권한 차단 로그 및 AI 사용 통계 조회 기준 반영
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiLogRepository extends JpaRepository<AiLogEntity, Integer> {

    @EntityGraph(attributePaths = {"message"})
    List<AiLogEntity> findByMessage_MessageIdInOrderByLogIdAsc(List<Integer> messageIds);

    @EntityGraph(attributePaths = {"employee", "employee.department", "session", "message"})
    List<AiLogEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    @EntityGraph(attributePaths = {"employee", "employee.department", "session", "message"})
    @Query("""
            select log
            from AiLogEntity log
            join log.employee employee
            left join employee.department department
            where log.createdAt >= :startAt
              and log.createdAt < :endAt
              and (:department = '' or :department is null or department.deptName = :department)
              and (
                    :aiType = '' or :aiType is null
                    or (:aiType = 'CHATBOT' and log.type = :chatbotType)
                    or (:aiType = 'ASSISTANT' and log.type = :assistantType and lower(coalesce(log.query, '')) not like '%feature=correction%')
                    or (:aiType = 'CORRECTION' and log.type = :assistantType and lower(coalesce(log.query, '')) like '%feature=correction%')
              )
              and (
                    :result = '' or :result is null
                    or (:result = 'SUCCESS' and (log.errorMessage is null or log.errorMessage = '') and lower(coalesce(log.response, '')) not like '%fallback=true%')
                    or (:result = 'FALLBACK' and lower(coalesce(log.response, '')) like '%fallback=true%')
                    or (:result = 'FAIL' and log.errorMessage is not null and log.errorMessage <> '' and lower(coalesce(log.response, '')) not like '%fallback=true%')
              )
            order by log.createdAt desc
            """)
    Page<AiLogEntity> findDashboardLogs(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("department") String department,
            @Param("aiType") String aiType,
            @Param("result") String result,
            @Param("chatbotType") AiLogType chatbotType,
            @Param("assistantType") AiLogType assistantType,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"employee", "employee.department"})
    @Query("""
            select log
            from AiLogEntity log
            join log.employee employee
            left join employee.department department
            where log.createdAt >= :startAt
              and log.createdAt < :endAt
              and (:department = '' or :department is null or department.deptName = :department)
              and (
                    :aiType = '' or :aiType is null
                    or (:aiType = 'CHATBOT' and log.type = :chatbotType)
                    or (:aiType = 'ASSISTANT' and log.type = :assistantType and lower(coalesce(log.query, '')) not like '%feature=correction%')
                    or (:aiType = 'CORRECTION' and log.type = :assistantType and lower(coalesce(log.query, '')) like '%feature=correction%')
              )
              and (
                    :result = '' or :result is null
                    or (:result = 'SUCCESS' and (log.errorMessage is null or log.errorMessage = '') and lower(coalesce(log.response, '')) not like '%fallback=true%')
                    or (:result = 'FALLBACK' and lower(coalesce(log.response, '')) like '%fallback=true%')
                    or (:result = 'FAIL' and log.errorMessage is not null and log.errorMessage <> '' and lower(coalesce(log.response, '')) not like '%fallback=true%')
              )
            order by log.createdAt desc
            """)
    List<AiLogEntity> findDashboardLogsForExport(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("department") String department,
            @Param("aiType") String aiType,
            @Param("result") String result,
            @Param("chatbotType") AiLogType chatbotType,
            @Param("assistantType") AiLogType assistantType
    );
}
