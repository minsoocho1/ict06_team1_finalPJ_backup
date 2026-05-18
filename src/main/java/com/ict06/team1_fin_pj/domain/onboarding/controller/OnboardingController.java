/**
 * @FileName : OnboardingController.java
 * @Description : 사용자 온보딩 대시보드 및 학습 콘텐츠 API Controller
 *                - 대시보드 요약, 학습 콘텐츠 상세, AI 학습 도우미 요청 처리
 * @Author : 김다솜
 * @Date : 2026. 05. 06
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.06    김다솜        최초 생성 및 온보딩 대시보드 요약 API 구현
 * @ 2026.05.15    김다솜        학습 콘텐츠 AI 요약/재설명/직접 질문 API 추가
 */
package com.ict06.team1_fin_pj.domain.onboarding.controller;

import com.ict06.team1_fin_pj.common.dto.onboarding.AiContentQuestionRequestDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.OnContentDetailResponseDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.OnboardingDashboardResponse;
import com.ict06.team1_fin_pj.domain.onboarding.repository.OnContentRepository;
import com.ict06.team1_fin_pj.domain.onboarding.service.ContentLearningAssistService;
import com.ict06.team1_fin_pj.domain.onboarding.service.OnboardingDashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class OnboardingController {

    private final OnboardingDashboardServiceImpl dashboardService;
    private final OnContentRepository onContentRepository;
    private final ContentLearningAssistService contentLearningAssistService;

    /**
     * @MethodName : getDashboard
     * @Description : 사번 기준 온보딩 대시보드 요약 정보 조회
     *
     * @param empNo 사번
     * @return 온보딩 대시보드 요약 응답 DTO
     */
    @GetMapping("/{empNo}")
    public OnboardingDashboardResponse getDashboard(@PathVariable String empNo) {
        return dashboardService.getDashboard(empNo);
    }

    /**
     * @MethodName : getContentDetail
     * @Description : 학습 상세 화면용 콘텐츠 정보 조회
     *
     * @param contentId 콘텐츠 식별자
     * @return 콘텐츠 상세 응답 DTO
     */
    @GetMapping("/content/{contentId}")
    public ResponseEntity<?> getContentDetail(@PathVariable Integer contentId) {
        return onContentRepository.findById(contentId)
                .<ResponseEntity<?>>map(content -> ResponseEntity.ok(
                        OnContentDetailResponseDto.builder()
                                .contentId(content.getContentId())
                                .title(content.getTitle())
                                .type(content.getType() != null ? content.getType().name() : null)
                                .category(content.getCategory())
                                .subCategory(content.getSubCategory())
                                .targetPosition(content.getTargetPosition())
                                .difficulty(content.getDifficulty() != null ? content.getDifficulty().name() : null)
                                .estimatedTime(content.getEstimatedTime())
                                .tags(content.getTags())
                                .isMandatory(content.getIsMandatory())
                                .path(content.getPath())
                                .build()
                ))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * @MethodName : explainContent
     * @Description : 문서 기반 핵심 요약 또는 쉬운 설명 생성
     *
     * @param contentId 콘텐츠 식별자
     * @param mode      summary 또는 simple
     * @return AI 학습 도우미 응답 DTO
     */
    @GetMapping("/content/{contentId}/explain")
    public ResponseEntity<?> explainContent(
            @PathVariable Integer contentId,
            @RequestParam(defaultValue = "summary") String mode
    ) {
        try {
            return ResponseEntity.ok(contentLearningAssistService.explainContent(contentId, mode));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * @MethodName : askContentQuestion
     * @Description : 학습 콘텐츠 직접 질문 응답 생성
     *
     * @param contentId 콘텐츠 식별자
     * @param request   질문 요청 DTO
     * @return AI 학습 도우미 응답 DTO
     */
    @PostMapping("/content/{contentId}/question")
    public ResponseEntity<?> askContentQuestion(
            @PathVariable Integer contentId,
            @RequestBody AiContentQuestionRequestDto request
    ) {
        try {
            return ResponseEntity.ok(contentLearningAssistService.askQuestion(contentId, request.getQuestion()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
