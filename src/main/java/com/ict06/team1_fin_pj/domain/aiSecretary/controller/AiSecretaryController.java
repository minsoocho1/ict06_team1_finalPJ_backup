package com.ict06.team1_fin_pj.domain.aiSecretary.controller;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.*;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatSessionEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.MessageRole;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.SessionType;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiChatMessageRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.response.ApiResponse;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AssistantDraftRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AssistantDraftResponseDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-secretary")
public class AiSecretaryController {

    // SERVICE 호출
    @Autowired
    private AiSecretaryService aiSecretaryService;

    @Autowired
    private  AiChatbotService aiChatbotService;

    @Autowired
    private AiCorrectionService aiCorrectionService;

    @Autowired
    private AiAssistantDraftService aiAssistantDraftService;

    @Autowired
    private AiTemplateRequestService aiTemplateRequestService;

    @Autowired
    private AiChatMessageRepository aiChatMessageRepository;

    // 챗봇 최근 세션 조회 또는 생성
    // POST /api/ai-secretary/chatbot/session?empNo=20209999
    @PostMapping("/chatbot/session")
    public ApiResponse<AiChatSessionResponseDto> getOrCreateChatbotSession(
            @RequestParam String empNo
    ) {
        AiChatSessionEntity session = aiSecretaryService.getOrCreateChatbotSession(empNo);

        AiChatSessionResponseDto response = AiChatSessionResponseDto.builder()
                .sessionId(session.getSessionId())
                .empNo(session.getEmployee().getEmpNo())
                .sessionType(session.getSessionType())
                .title(session.getTitle())
                .status(session.getStatus())
                .lastMessageAt(session.getLastMessageAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();

        return ApiResponse.ok("챗봇 세션 조회 성공", response);
    }

    // AI 비서 채팅 세션 목록 조회
    @GetMapping("/sessions")
    public ApiResponse<List<AiChatSessionResponseDto>> getSessionList(
            @RequestParam String empNo,
            @RequestParam SessionType sessionType
    ) {
        // 세션 응답 DTO 포함 List 생성
        List<AiChatSessionResponseDto> response  = aiSecretaryService.getSessionList(empNo, sessionType)
                .stream() // 데이터를 하나씩 꺼내서 .map으로 반복해서 DTO를 만들어라
                .map(session -> AiChatSessionResponseDto.builder()
                        .sessionId(session.getSessionId())
                        .empNo(session.getEmployee().getEmpNo())
                        .sessionType(session.getSessionType())
                        .documentType(resolveDocumentType(session.getSessionId()))
                        .title(session.getTitle())
                        .status(session.getStatus())
                        .lastMessageAt(session.getLastMessageAt())
                        .createdAt(session.getCreatedAt())
                        .updatedAt(session.getUpdatedAt())
                        .build()
                )
                .toList(); // 많은 DTO 들을 최종적으로 하나의 List에 담음

        return ApiResponse.ok("세션 목록 조회 성공", response);
    }

    // 채팅 세션 내 메시지 목록 조회
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<AiChatMessageResponseDto>> getMessageList(
        @PathVariable Integer sessionId // URL 경로(path)에 있는 상수 값을 변수(Variable)로 가져오기
    ) {
        // 메시지 응답 DTO 포함 List 생성
        List<AiChatMessageResponseDto> response = aiSecretaryService.getMessageList(sessionId)
                .stream()
                .map(message -> AiChatMessageResponseDto.builder()
                        .messageId(message.getMessageId())
                        .sessionId(message.getSession().getSessionId())
                        .role(message.getRole())
                        .content(message.getContent())
                        .seqNo(message.getSeqNo())
                        .modelName(message.getModelName())
                        .promptTokens(message.getPromptTokens())
                        .completionTokens(message.getCompletionTokens())
                        .createdAt(message.getCreatedAt())
                        .build())
                .toList();

        return ApiResponse.ok("메시지 목록 조회 성공", response);
    }

    // 채팅 세션 내 메시지 저장
    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<AiChatMessageResponseDto> saveMessage(
        @PathVariable Integer sessionId,
        @Valid @RequestBody AiChatMessageCreateRequestDto request
    ){
        // [1] PATH에서 받아온 값 DB에 넣기
        AiChatMessageEntity message = AiChatMessageEntity.builder()
                .role(request.getRole())
                .content(request.getContent())
                .modelName(request.getModelName())
                .build();

        // [2] 메시지 저장하기
        AiChatMessageEntity savedMessage = aiSecretaryService.saveMessage(sessionId, message);

        // [3] 메시지 응답 DTO 생성
        AiChatMessageResponseDto response = AiChatMessageResponseDto.builder()
                .messageId(savedMessage.getMessageId())
                .sessionId(savedMessage.getSession().getSessionId())
                .role(savedMessage.getRole())
                .content(savedMessage.getContent())
                .seqNo(savedMessage.getSeqNo())
                .modelName(savedMessage.getModelName())
                .promptTokens(savedMessage.getPromptTokens())
                .completionTokens(savedMessage.getCompletionTokens())
                .createdAt(savedMessage.getCreatedAt())
                .build();

        return ApiResponse.ok("메시지 저장 성공", response);
    }

    // 챗봇 응답 생성
    @PostMapping("/chatbot/ask")
    public ApiResponse<ChatbotAskResponseDto> askChatbot(
            @Valid @RequestBody ChatbotAskRequestDto requestDto
    ) {
        ChatbotAskResponseDto response =
                aiChatbotService.ask(requestDto.getSessionId(), requestDto.getContent());

        return ApiResponse.ok("챗봇 응답 생성 성공", response);
    }

    // 문장 다듬기
    @PostMapping("/correction")
    public ApiResponse<CorrectionResponseDto> correctText(
            @Valid @RequestBody CorrectionRequestDto requestDto
    ) {
        CorrectionResponseDto response = aiCorrectionService.correct(
                requestDto.getEmpNo(),
                requestDto.getText(),
                requestDto.getMode()
        );

        return ApiResponse.ok("문장 다듬기 성공", response);
    }

    // AI 문서 초안 생성
    @PostMapping("/assistant/draft")
    public ApiResponse<AssistantDraftResponseDto> createAssistantDraft(
            @Valid @RequestBody AssistantDraftRequestDto requestDto
    ) {
        System.out.println("<<< POST /api/ai-secretary/assistant/draft 진입 >>>");
        System.out.println("type = " + requestDto.getType());
        System.out.println("title = " + requestDto.getTitle());
        System.out.println("empNo = " + requestDto.getEmpNo());

        AssistantDraftResponseDto response =
                aiAssistantDraftService.createDraft(requestDto);

        return ApiResponse.ok("AI 초안 생성 성공", response);
    }

    // AI 문서 추가 수정
    // POST) /api/ai-secretary/assistant/revise
    @PostMapping("/assistant/revise")
    public ApiResponse<AssistantReviseResponseDto> reviseAssistantDraft(
            @Valid @RequestBody AssistantReviseRequestDto requestDto
    ) {
        AssistantReviseResponseDto response =
                aiAssistantDraftService.reviseDraft(requestDto);

        return ApiResponse.ok("AI 문서 수정 성공", response);
    }

    // AI 템플릿 생성
    @PostMapping("/assistant/template")
    public ApiResponse<AssistantTemplateResponseDto> createAssistantTemplate(
            @Valid @RequestBody AssistantTemplateRequestDto requestDto
    ) {
        AssistantTemplateResponseDto response =
                aiAssistantDraftService.createTemplate(requestDto);

        return ApiResponse.ok("AI 템플릿 생성 성공", response);
    }

    // AI 생성 템플릿을 추천 템플릿 목록 추가 요청
    @PostMapping("/template-request")
    public ApiResponse<TemplateRequestResponseDto> createTemplateRequest(
            @Valid @RequestBody TemplateRequestCreateRequestDto requestDto
    ) {
        TemplateRequestResponseDto responseDto =
                aiTemplateRequestService.createRequest(requestDto);

        return ApiResponse.ok("추천 템플릿 추가 요청이 접수되었습니다.", responseDto);
    }

    // 추천 템플릿 추가 요청 목록 조회
    @GetMapping("/template-request/my")
    public ApiResponse<List<TemplateRequestResponseDto>> getMyTemplateRequests(
            @RequestParam String empNo
    ){
        List<TemplateRequestResponseDto> responseDto =
                aiTemplateRequestService.getMyRequests(empNo);

        return ApiResponse.ok("추천 템플릿 추가 요청 목록 조회 성공", responseDto);
    }

    private String resolveDocumentType(Integer sessionId) {
        return aiChatMessageRepository
                .findTopBySessionSessionIdAndRoleOrderBySeqNoAsc(sessionId, MessageRole.USER)
                .map(AiChatMessageEntity::getContent)
                .map(this::parseDocumentTypeFromUserMessage)
                .orElse("REPORT");
    }

    private String parseDocumentTypeFromUserMessage(String content) {
        if (content == null) return "REPORT";

        String upper = content.toUpperCase();

        if (upper.contains("TEMPLATE") || upper.contains("템플릿")) return "TEMPLATE";
        if (upper.contains("MINUTES")) return "MINUTES";
        if (upper.contains("APPROVAL")) return "APPROVAL";
        return "REPORT";
    }

}
