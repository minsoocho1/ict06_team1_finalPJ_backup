/**
 * @FileName : AiSecretaryServiceImpl.java
 * @Description : AI 비서/챗봇 세션 및 메시지 관리 서비스 구현체
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진        최초 생성 (세션 생성 및 메시지 저장, 목록 조회 메서드 추가)
 * @ 2026.05.05    송혜진        CHATBOT 최근 48시간 내 단일 세션 조회 또는 생성 메서드 추가
 * @ 2026.05.12    송혜진        ASSISTANT 세션 장기 유지 및 최근 작성 목록 조회 기준 반영
 * @ 2026.05.22    송혜진        챗봇 세션 소유자 empNo 기반 RAG 검색 연동 흐름 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatSessionEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.SessionType;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiChatMessageRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiChatSessionRepository;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiSecretaryServiceImpl implements AiSecretaryService {

    private static final long CHATBOT_RETENTION_HOURS = 48L;

    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final EmpRepository empRepository;

    // 공통 세션 생성 진입점
    @Override
    @Transactional
    public AiChatSessionEntity createSession(String empNo, SessionType sessionType, String title) {

        if (sessionType == SessionType.CHATBOT) {
            return getOrCreateChatbotSession(empNo);
        }

        return createAssistantSession(empNo, title);
    }

    // ASSISTANT 세션 생성
    @Override
    @Transactional
    public AiChatSessionEntity createAssistantSession(String empNo, String title) {

        EmpEntity employee = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사원입니다. empNo=" + empNo));

        LocalDateTime now = LocalDateTime.now();

        AiChatSessionEntity session = AiChatSessionEntity.builder()
                .employee(employee)
                .sessionType(SessionType.ASSISTANT)
                .title(title)
                .lastMessageAt(now)
                .build();

        return aiChatSessionRepository.save(session);
    }

    // CHATBOT 최근 48시간 내 단일 세션 조회 또는 생성
    @Override
    @Transactional
    public AiChatSessionEntity getOrCreateChatbotSession(String empNo) {

        LocalDateTime cutoff = LocalDateTime.now().minusHours(CHATBOT_RETENTION_HOURS);

        return aiChatSessionRepository
                .findTopByEmployee_EmpNoAndSessionTypeAndLastMessageAtAfterOrderByLastMessageAtDesc(
                        empNo,
                        SessionType.CHATBOT,
                        cutoff
                )
                .orElseGet(() -> createNewChatbotSession(empNo)); // orElseGet() 값이 비어 있을 대만 대체할 값 생성
    }

    // CHATBOT 신규 세션 생성
    private AiChatSessionEntity createNewChatbotSession(String empNo) {

        EmpEntity employee = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사원입니다. empNo=" + empNo));

        LocalDateTime now = LocalDateTime.now();

        AiChatSessionEntity session = AiChatSessionEntity.builder()
                .employee(employee)
                .sessionType(SessionType.CHATBOT)
                .title("챗봇 대화")
                .lastMessageAt(now)
                .build();

        return aiChatSessionRepository.save(session);
    }

    // 세션 목록 조회
    @Override
    public List<AiChatSessionEntity> getSessionList(String empNo, SessionType sessionType) {

        if (sessionType == SessionType.CHATBOT) {
            return List.of();
        }

        return aiChatSessionRepository
                .findByEmployee_EmpNoAndSessionTypeOrderByLastMessageAtDesc(
                        empNo,
                        SessionType.ASSISTANT
                );
    }

    // 메시지 목록 조회
    @Override
    public List<AiChatMessageEntity> getMessageList(Integer sessionId) {

        return aiChatMessageRepository.findBySessionSessionIdOrderBySeqNoAsc(sessionId);
    }

    // 메시지 저장
    @Override
    @Transactional
    public AiChatMessageEntity saveMessage(Integer sessionId, AiChatMessageEntity message) {

        AiChatSessionEntity session = aiChatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세션입니다. sessionId=" + sessionId));

        Integer lastSeqNo = aiChatMessageRepository
                .findTopBySessionSessionIdOrderBySeqNoDesc(sessionId)
                .map(AiChatMessageEntity::getSeqNo)
                .orElse(0);

        int nextSeqNo = lastSeqNo + 1;

        message.setSession(session);
        message.setSeqNo(nextSeqNo);

        AiChatMessageEntity savedMessage = aiChatMessageRepository.save(message);

        session.updateLastMessageAt(LocalDateTime.now());

        return savedMessage;
    }
}