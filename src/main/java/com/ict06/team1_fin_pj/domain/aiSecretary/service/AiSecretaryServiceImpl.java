/**
 * @FileName : AiSecretaryServiceImpl.java
 * @Description : AI 鍮꾩꽌/梨쀫큸 ?몄뀡 諛?硫붿떆吏 愿由??쒕퉬??援ы쁽泥?
 * @Author : ?≫삙吏?
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ ?섏젙??        ?섏젙??       ?섏젙?댁슜
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    ?≫삙吏?       理쒖큹 ?앹꽦 (?몄뀡 ?앹꽦 諛?硫붿떆吏 ??? 紐⑸줉 議고쉶 硫붿꽌??異붽?)
 * @ 2026.05.05    ?≫삙吏?       CHATBOT 理쒓렐 48?쒓컙 ???⑥씪 ?몄뀡 議고쉶 ?먮뒗 ?앹꽦 硫붿꽌??異붽?
 * @ 2026.05.12    ?≫삙吏?       ASSISTANT ?몄뀡 ?κ린 ?좎? 諛?理쒓렐 ?묒꽦 紐⑸줉 議고쉶 湲곗? 諛섏쁺
 * @ 2026.05.22    ?≫삙吏?       梨쀫큸 ?몄뀡 ?뚯쑀??empNo 湲곕컲 RAG 寃???곕룞 ?먮쫫 ?뺣━
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.ChatbotReferenceDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatSessionEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiRetrievalTraceEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.MessageRole;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.SessionType;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiChatMessageRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiChatSessionRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiLogRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiRetrievalTraceRepository;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiSecretaryServiceImpl implements AiSecretaryService {

    private static final long CHATBOT_RETENTION_HOURS = 48L;

    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final AiLogRepository aiLogRepository;
    private final AiRetrievalTraceRepository aiRetrievalTraceRepository;
    private final EmpRepository empRepository;

    // 怨듯넻 ?몄뀡 ?앹꽦 吏꾩엯??
    @Override
    @Transactional
    public AiChatSessionEntity createSession(String empNo, SessionType sessionType, String title) {

        if (sessionType == SessionType.CHATBOT) {
            return getOrCreateChatbotSession(empNo);
        }

        return createAssistantSession(empNo, title);
    }

    // ASSISTANT ?몄뀡 ?앹꽦
    @Override
    @Transactional
    public AiChatSessionEntity createAssistantSession(String empNo, String title) {

        EmpEntity employee = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new IllegalArgumentException("議댁옱?섏? ?딅뒗 ?ъ썝?낅땲?? empNo=" + empNo));

        LocalDateTime now = LocalDateTime.now();

        AiChatSessionEntity session = AiChatSessionEntity.builder()
                .employee(employee)
                .sessionType(SessionType.ASSISTANT)
                .title(title)
                .lastMessageAt(now)
                .build();

        return aiChatSessionRepository.save(session);
    }

    // CHATBOT 理쒓렐 48?쒓컙 ???⑥씪 ?몄뀡 議고쉶 ?먮뒗 ?앹꽦
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
                .orElseGet(() -> createNewChatbotSession(empNo)); // orElseGet() 媛믪씠 鍮꾩뼱 ?덉쓣 ?留??泥댄븷 媛??앹꽦
    }

    // CHATBOT ?좉퇋 ?몄뀡 ?앹꽦
    private AiChatSessionEntity createNewChatbotSession(String empNo) {

        EmpEntity employee = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new IllegalArgumentException("議댁옱?섏? ?딅뒗 ?ъ썝?낅땲?? empNo=" + empNo));

        LocalDateTime now = LocalDateTime.now();

        AiChatSessionEntity session = AiChatSessionEntity.builder()
                .employee(employee)
                .sessionType(SessionType.CHATBOT)
                .title("챗봇 대화")
                .lastMessageAt(now)
                .build();

        return aiChatSessionRepository.save(session);
    }

    // ?몄뀡 紐⑸줉 議고쉶
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

    // 硫붿떆吏 紐⑸줉 議고쉶
    @Override
    public List<AiChatMessageEntity> getMessageList(Integer sessionId) {

        return aiChatMessageRepository.findBySessionSessionIdOrderBySeqNoAsc(sessionId);
    }

    @Override
    public Map<Integer, List<ChatbotReferenceDto>> getMessageReferences(List<Integer> assistantMessageIds) {
        if (assistantMessageIds == null || assistantMessageIds.isEmpty()) {
            return Map.of();
        }

        List<AiLogEntity> logs = aiLogRepository.findByMessage_MessageIdInOrderByLogIdAsc(assistantMessageIds)
                .stream()
                .filter(log -> log.getMessage() != null && log.getMessage().getMessageId() != null)
                .toList();

        if (logs.isEmpty()) {
            return Map.of();
        }

        List<Integer> logIds = logs.stream()
                .map(AiLogEntity::getLogId)
                .filter(id -> id != null)
                .toList();

        if (logIds.isEmpty()) {
            return Map.of();
        }

        Map<Integer, Integer> logIdToMessageId = new LinkedHashMap<>();
        for (AiLogEntity log : logs) {
            if (log.getLogId() == null || log.getMessage() == null || log.getMessage().getMessageId() == null) {
                continue;
            }
            logIdToMessageId.put(log.getLogId(), log.getMessage().getMessageId());
        }

        List<AiRetrievalTraceEntity> traces =
                aiRetrievalTraceRepository.findByLog_LogIdInAndUsedInAnswerTrueOrderByTraceIdAsc(logIds);

        if (traces.isEmpty()) {
            return Map.of();
        }

        Map<Integer, LinkedHashMap<Integer, ChatbotReferenceDto>> messageReferenceMap = new LinkedHashMap<>();

        for (AiRetrievalTraceEntity trace : traces) {
            if (trace == null || trace.getLog() == null || trace.getDocument() == null) {
                continue;
            }

            Integer logId = trace.getLog().getLogId();
            Integer messageId = logIdToMessageId.get(logId);
            Integer docId = trace.getDocument().getDocId();

            if (messageId == null || docId == null) {
                continue;
            }

            LinkedHashMap<Integer, ChatbotReferenceDto> referencesByDocId =
                    messageReferenceMap.computeIfAbsent(messageId, ignored -> new LinkedHashMap<>());

            referencesByDocId.computeIfAbsent(docId, ignored ->
                    ChatbotReferenceDto.builder()
                            .docId(docId)
                            .title(safe(trace.getDocument().getTitle(), "李멸퀬 臾몄꽌 " + docId))
                            .url(normalizeReferenceUrl(trace.getDocument().getFilePath()))
                            .build()
            );
        }

        Map<Integer, List<ChatbotReferenceDto>> result = new LinkedHashMap<>();
        messageReferenceMap.forEach((messageId, referencesByDocId) ->
                result.put(messageId, List.copyOf(referencesByDocId.values()))
        );
        return result;
    }

    // 硫붿떆吏 ???
    @Override
    @Transactional
    public AiChatMessageEntity saveMessage(Integer sessionId, AiChatMessageEntity message) {

        AiChatSessionEntity session = aiChatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("議댁옱?섏? ?딅뒗 ?몄뀡?낅땲?? sessionId=" + sessionId));

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

    private String normalizeReferenceUrl(String filePath) {
        String normalized = safe(filePath);
        return normalized.isBlank() ? null : normalized;
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
