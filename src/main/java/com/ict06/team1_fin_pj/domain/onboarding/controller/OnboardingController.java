/**
 * @FileName : OnboardingController.java
 * @Description : 사용자 온보딩 대시보드 및 학습 콘텐츠 API 컨트롤러
 * @Author : 김다솜
 * @Date : 2026. 05. 06
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.06    김다솜        최초 생성 및 온보딩 대시보드 요약 API 구현
 * @ 2026.05.15    김다솜        학습 콘텐츠 AI 요약/재설명/직접 질문 API 추가
 * @ 2026.05.18    김다솜        학습 이해도 자기 평가 API 추가, 깨진 주석 복구
 */
package com.ict06.team1_fin_pj.domain.onboarding.controller;

import com.ict06.team1_fin_pj.common.dto.onboarding.AiContentQuestionRequestDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.LearningSelfCheckRequestDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.OnContentDetailResponseDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.OnboardingDashboardResponse;
import com.ict06.team1_fin_pj.domain.onboarding.repository.OnContentRepository;
import com.ict06.team1_fin_pj.domain.onboarding.service.ContentLearningAssistService;
import com.ict06.team1_fin_pj.domain.onboarding.service.LearningSelfCheckService;
import com.ict06.team1_fin_pj.domain.onboarding.service.OnboardingDashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class OnboardingController {

    private final OnboardingDashboardServiceImpl dashboardService;
    private final OnContentRepository onContentRepository;
    private final ContentLearningAssistService contentLearningAssistService;
    private final LearningSelfCheckService learningSelfCheckService;

    @GetMapping("/{empNo}")
    public OnboardingDashboardResponse getDashboard(@PathVariable String empNo) {
        return dashboardService.getDashboard(empNo);
    }

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

    @GetMapping("/content/{contentId}/self-check")
    public ResponseEntity<?> getLearningSelfCheck(
            @PathVariable Integer contentId,
            @RequestParam String empNo
    ) {
        try {
            return ResponseEntity.ok(learningSelfCheckService.getSelfCheck(contentId, empNo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/content/{contentId}/self-check")
    public ResponseEntity<?> saveLearningSelfCheck(
            @PathVariable Integer contentId,
            @RequestBody LearningSelfCheckRequestDto request
    ) {
        try {
            return ResponseEntity.ok(learningSelfCheckService.saveSelfCheck(contentId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
