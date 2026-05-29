/**
 * @FileName : AiChatMessageRepository.java
 * @Description : AI 대화 메시지 Repository
 *                - AI 비서/챗봇 메시지 조회 및 저장
 *                - 세션별 메시지 목록 조회
 *                - USER/ASSISTANT 메시지 순서 및 버전 기록 복원에 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.28  송혜진       USER/ASSISTANT 메시지 기반 버전 기록 복원 기준 반영
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.MessageRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 세션(대화방) 하나를 열었을 때 메시지 목록을 순서대로 렌더링
@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessageEntity, Integer> {
    // 대화 세션 식별자를 통해, 채팅 세션 내 메시지 목록 조회
    List<AiChatMessageEntity> findBySessionSessionIdOrderBySeqNoAsc(Integer sessionId);

    // 세션 내 메시지 순번(seq_no) 중 가장 마지막 메시지 찾기
    Optional<AiChatMessageEntity> findTopBySessionSessionIdOrderBySeqNoDesc(Integer sessionId);

    Optional<AiChatMessageEntity> findTopBySessionSessionIdAndRoleOrderBySeqNoAsc(
            Integer sessionId,
            MessageRole role
    );

}
