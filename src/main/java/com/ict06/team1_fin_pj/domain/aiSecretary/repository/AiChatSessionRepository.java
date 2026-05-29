/**
 * @FileName : AiChatSessionRepository.java
 * @Description : AI 대화 세션 Repository
 *                - AI 비서 및 사내 챗봇 세션 조회/저장
 *                - 사용자별 세션 목록 조회
 *                - 세션 타입별 ASSISTANT / CHATBOT 세션 관리
 *                - AI 비서 최근 작성 목록 및 챗봇 세션 복원에 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.12  송혜진       AI 비서/챗봇 세션 조회 기준 정리
 * @ 2026.05.28  송혜진       사용자 AI 비서 세션 및 최근 작성 목록 복원 기준 반영
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatSessionEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.SessionType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AiChatSessionRepository extends JpaRepository<AiChatSessionEntity, Integer> {

    // ASSISTANT 목록 조회용
    @EntityGraph(attributePaths = "employee")
    List<AiChatSessionEntity> findByEmployee_EmpNoAndSessionTypeOrderByLastMessageAtDesc(
            String empNo,
            SessionType sessionType
    );

    // CHATBOT 최근 48시간 내 단일 최근 세션 조회용
    @EntityGraph(attributePaths = "employee")
    Optional<AiChatSessionEntity> findTopByEmployee_EmpNoAndSessionTypeAndLastMessageAtAfterOrderByLastMessageAtDesc(
            String empNo,
            SessionType sessionType,
            LocalDateTime cutoff
    );

    // 48시간 지난 CHATBOT 세션 삭제 대상 조회용
    List<AiChatSessionEntity> findBySessionTypeAndLastMessageAtBefore(
            SessionType sessionType,
            LocalDateTime cutoff
    );
}