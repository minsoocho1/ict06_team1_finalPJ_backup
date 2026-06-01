/**
 * @FileName : AiSecretaryController.java
 * @Description : 사용자 AI 비서 및 챗봇 API 컨트롤러
 *                - AI 비서 문서 초안 생성, 수정, 템플릿 생성 요청 처리
 *                - 문장 다듬기 및 참고 자료 본문 추출 API 제공
 *                - 사내 AI 챗봇 세션 생성, 질문/답변 처리
 *                - 사용자 템플릿 요청 및 자료 등록 요청 처리
 *                - AI 대화 세션 및 메시지 조회 기능 제공
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.12  송혜진       AI 비서/챗봇 세션 및 메시지 API 정리
 * @ 2026.05.27  송혜진       챗봇 참고 문서 references 응답 구조 반영
 * @ 2026.05.28  송혜진       참고 자료 본문 추출 및 문서 작성 API 보강
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.controller;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.AiChatMessageCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AiChatMessageResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AiChatSessionCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AiChatSessionResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AssistantDraftRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AssistantDraftResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AssistantReviseRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AssistantReviseResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AssistantTemplateRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AssistantTemplateResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.ChatbotAskRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.ChatbotAskResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.ChatbotReferenceDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.CorrectionRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.CorrectionResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeRequestCreateDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeRequestSuggestionsDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.ReferenceExtractResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.TemplateRequestCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.TemplateRequestResponseDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatSessionEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.MessageRole;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.SessionType;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiChatMessageRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.response.ApiResponse;
import com.ict06.team1_fin_pj.domain.aiSecretary.service.AiAssistantDraftService;
import com.ict06.team1_fin_pj.domain.aiSecretary.service.AiChatbotService;
import com.ict06.team1_fin_pj.domain.aiSecretary.service.AiCorrectionService;
import com.ict06.team1_fin_pj.domain.aiSecretary.service.AiKnowledgeRequestService;
import com.ict06.team1_fin_pj.domain.aiSecretary.service.AiSecretaryService;
import com.ict06.team1_fin_pj.domain.aiSecretary.service.AiTemplateRequestService;
import com.ict06.team1_fin_pj.domain.aiSecretary.service.ReferenceFileExtractService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-secretary")
public class AiSecretaryController {

    private final AiSecretaryService aiSecretaryService;
    private final AiChatbotService aiChatbotService;
    private final AiCorrectionService aiCorrectionService;
    private final AiAssistantDraftService aiAssistantDraftService;
    private final AiTemplateRequestService aiTemplateRequestService;
    private final AiKnowledgeRequestService aiKnowledgeRequestService;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final ReferenceFileExtractService referenceFileExtractService;

    @Autowired
    public AiSecretaryController(
            AiSecretaryService aiSecretaryService,
            AiChatbotService aiChatbotService,
            AiCorrectionService aiCorrectionService,
            AiAssistantDraftService aiAssistantDraftService,
            AiTemplateRequestService aiTemplateRequestService,
            AiKnowledgeRequestService aiKnowledgeRequestService,
            AiChatMessageRepository aiChatMessageRepository,
            ReferenceFileExtractService referenceFileExtractService
    ) {
        this.aiSecretaryService = aiSecretaryService;
        this.aiChatbotService = aiChatbotService;
        this.aiCorrectionService = aiCorrectionService;
        this.aiAssistantDraftService = aiAssistantDraftService;
        this.aiTemplateRequestService = aiTemplateRequestService;
        this.aiKnowledgeRequestService = aiKnowledgeRequestService;
        this.aiChatMessageRepository = aiChatMessageRepository;
        this.referenceFileExtractService = referenceFileExtractService;
    }

    @PostMapping("/sessions")
    public ApiResponse<AiChatSessionResponseDto> createSession(
            @Valid @RequestBody AiChatSessionCreateRequestDto requestDto
    ) {
        AiChatSessionEntity session = aiSecretaryService.createSession(
                requestDto.getEmpNo(),
                requestDto.getSessionType(),
                requestDto.getTitle()
        );

        return ApiResponse.ok("세션 생성 성공", toSessionResponse(session));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<AiChatSessionResponseDto>> getSessions(
            @RequestParam String empNo,
            @RequestParam SessionType sessionType
    ) {
        List<AiChatSessionResponseDto> responseDto = aiSecretaryService.getSessionList(empNo, sessionType)
                .stream()
                .map(this::toSessionResponse)
                .toList();

        return ApiResponse.ok("세션 목록 조회 성공", responseDto);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<AiChatMessageResponseDto>> getMessages(
            @PathVariable Integer sessionId
    ) {
        List<AiChatMessageEntity> messages = aiSecretaryService.getMessageList(sessionId);
        List<Integer> assistantMessageIds = messages.stream()
                .filter(message -> message.getRole() == MessageRole.ASSISTANT)
                .map(AiChatMessageEntity::getMessageId)
                .filter(messageId -> messageId != null)
                .toList();

        Map<Integer, List<ChatbotReferenceDto>> referencesByMessageId =
                aiSecretaryService.getMessageReferences(assistantMessageIds);

        List<AiChatMessageResponseDto> responseDto = messages.stream()
                .map(message -> AiChatMessageResponseDto.from(
                        message,
                        referencesByMessageId.getOrDefault(message.getMessageId(), List.of())
                ))
                .toList();

        return ApiResponse.ok("메시지 목록 조회 성공", responseDto);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<AiChatMessageResponseDto> saveMessage(
            @PathVariable Integer sessionId,
            @Valid @RequestBody AiChatMessageCreateRequestDto requestDto
    ) {
        AiChatMessageEntity message = AiChatMessageEntity.builder()
                .role(requestDto.getRole())
                .content(requestDto.getContent())
                .modelName(requestDto.getModelName())
                .build();

        AiChatMessageEntity savedMessage = aiSecretaryService.saveMessage(sessionId, message);

        return ApiResponse.ok("메시지 저장 성공", AiChatMessageResponseDto.from(savedMessage));
    }

    @PostMapping("/chatbot/session")
    public ApiResponse<AiChatSessionResponseDto> getOrCreateChatbotSession(
            @RequestParam String empNo
    ) {
        AiChatSessionEntity session = aiSecretaryService.getOrCreateChatbotSession(empNo);
        return ApiResponse.ok("챗봇 세션 조회 성공", toSessionResponse(session));
    }

    @PostMapping("/chatbot/ask")
    public ApiResponse<ChatbotAskResponseDto> askChatbot(
            @Valid @RequestBody ChatbotAskRequestDto requestDto
    ) {
        ChatbotAskResponseDto responseDto =
                aiChatbotService.ask(requestDto.getSessionId(), requestDto.getContent());

        return ApiResponse.ok("챗봇 응답 생성 성공", responseDto);
    }

    @PostMapping("/correction")
    public ApiResponse<CorrectionResponseDto> correctText(
            @Valid @RequestBody CorrectionRequestDto requestDto
    ) {
        CorrectionResponseDto responseDto = aiCorrectionService.correct(
                requestDto.getEmpNo(),
                requestDto.getText(),
                requestDto.getMode()
        );

        return ApiResponse.ok("문장 다듬기 성공", responseDto);
    }

    @PostMapping("/assistant/draft")
    public ApiResponse<AssistantDraftResponseDto> createAssistantDraft(
            @Valid @RequestBody AssistantDraftRequestDto requestDto
    ) {
        AssistantDraftResponseDto responseDto =
                aiAssistantDraftService.createDraft(requestDto);

        return ApiResponse.ok("AI 문서 초안 생성 성공", responseDto);
    }

    // 첨부 파일 본문을 1회성 참고 텍스트로 추출하는 API다.
    // RAG 저장이나 영구 문서 등록과는 연결하지 않는다.
    @PostMapping(
            value = "/assistant/reference/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<?>> extractReferenceText(
            @RequestPart("file") MultipartFile file
    ) {
        try {
            ReferenceExtractResponseDto responseDto = referenceFileExtractService.extract(file);
            return ResponseEntity.ok(ApiResponse.ok("참고 자료 본문 추출 성공", responseDto));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail(exception.getMessage()));
        }
    }

    @PostMapping("/assistant/revise")
    public ApiResponse<AssistantReviseResponseDto> reviseAssistantDraft(
            @Valid @RequestBody AssistantReviseRequestDto requestDto
    ) {
        AssistantReviseResponseDto responseDto =
                aiAssistantDraftService.reviseDraft(requestDto);

        return ApiResponse.ok("AI 문서 수정 성공", responseDto);
    }

    @PostMapping("/assistant/template")
    public ApiResponse<AssistantTemplateResponseDto> createAssistantTemplate(
            @Valid @RequestBody AssistantTemplateRequestDto requestDto
    ) {
        AssistantTemplateResponseDto responseDto =
                aiAssistantDraftService.createTemplate(requestDto);

        return ApiResponse.ok("AI 템플릿 생성 성공", responseDto);
    }

    @PostMapping("/template-request")
    public ApiResponse<TemplateRequestResponseDto> createTemplateRequest(
            @Valid @RequestBody TemplateRequestCreateRequestDto requestDto
    ) {
        TemplateRequestResponseDto responseDto =
                aiTemplateRequestService.createRequest(requestDto);

        return ApiResponse.ok("추천 템플릿 추가 요청이 접수되었습니다.", responseDto);
    }

    @GetMapping("/template-request/my")
    public ApiResponse<List<TemplateRequestResponseDto>> getMyTemplateRequests(
            @RequestParam String empNo
    ) {
        List<TemplateRequestResponseDto> responseDto =
                aiTemplateRequestService.getMyRequests(empNo);

        return ApiResponse.ok("추천 템플릿 추가 요청 목록 조회 성공", responseDto);
    }

    @PostMapping("/knowledge-request")
    public ApiResponse<KnowledgeResponseDto> createKnowledgeRequest(
            @Valid @RequestBody KnowledgeRequestCreateDto requestDto
    ) {
        KnowledgeResponseDto responseDto =
                aiKnowledgeRequestService.createRequest(requestDto);

        return ApiResponse.ok("자료 등록 요청이 접수되었습니다.", responseDto);
    }

    @GetMapping("/knowledge-request/my")
    public ApiResponse<List<KnowledgeResponseDto>> getMyKnowledgeRequests(
            @RequestParam String empNo
    ) {
        List<KnowledgeResponseDto> responseDto =
                aiKnowledgeRequestService.getMyRequests(empNo);

        return ApiResponse.ok("자료 등록 요청 목록 조회 성공", responseDto);
    }

    @GetMapping("/knowledge-request/suggestions")
    public ApiResponse<KnowledgeRequestSuggestionsDto> getKnowledgeRequestSuggestions() {
        KnowledgeRequestSuggestionsDto responseDto = aiKnowledgeRequestService.getSuggestions();
        return ApiResponse.ok("자동완성 후보 조회 성공", responseDto);
    }

    private AiChatSessionResponseDto toSessionResponse(AiChatSessionEntity session) {
        return AiChatSessionResponseDto.builder()
                .sessionId(session.getSessionId())
                .empNo(session.getEmployee() != null ? session.getEmployee().getEmpNo() : null)
                .sessionType(session.getSessionType())
                .documentType(resolveDocumentType(session.getSessionId()))
                .title(session.getTitle())
                .status(session.getStatus())
                .lastMessageAt(session.getLastMessageAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private String resolveDocumentType(Integer sessionId) {
        return aiChatMessageRepository
                .findTopBySessionSessionIdAndRoleOrderBySeqNoAsc(sessionId, MessageRole.USER)
                .map(AiChatMessageEntity::getContent)
                .map(this::parseDocumentTypeFromUserMessage)
                .orElse("REPORT");
    }

    private String parseDocumentTypeFromUserMessage(String content) {
        if (content == null) {
            return "REPORT";
        }

        String upper = content.toUpperCase();

        if (upper.contains("TEMPLATE")) {
            return "TEMPLATE";
        }
        if (upper.contains("MINUTES")) {
            return "MINUTES";
        }
        if (upper.contains("APPROVAL")) {
            return "APPROVAL";
        }
        return "REPORT";
    }
}
