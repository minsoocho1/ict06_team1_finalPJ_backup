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
