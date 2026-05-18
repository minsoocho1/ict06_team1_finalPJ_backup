/**
 * @FileName : AdDashboardController.java
 * @Description : 관리자 메인 대시보드 컨트롤러
 *                - FastAPI AI 서버에서 대시보드 통계 데이터를 조회
 *                - KPI, 차트, 최근 시스템 활동 데이터를 관리자 홈 화면에 바인딩
 * @Author : 김다솜
 * @Date : 2026. 05. 11
 * @Modification_History
 * @
 * @ 수정일          수정자         수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.11    김다솜         최초 생성
 * @ 2026.05.12    김다솜         대시보드 실제 데이터 연동 및 최근 시스템 활동 바인딩 추가
 */
package com.ict06.team1_fin_pj.domain.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdDashboardController {

    private final RestTemplate restTemplate;

    private static final String AI_SERVER_URL = "http://localhost:8000/api/stats/dashboard";

    /**
     * 관리자 홈 대시보드 화면에 KPI, 차트, 최근 활동 데이터를 바인딩한다.
     */
    @GetMapping("/home")
    public String adminHome(Model model) {
        model.addAttribute("stats", Map.of(
                "kpis", Map.of(
                        "totalOnboarding", 0,
                        "avgProgress", 0.0,
                        "evaluationSubmissions", 0,
                        "aiActivities", 0
                ),
                "onboardingTeam", Collections.emptyList(),
                "onboardingDivision", Collections.emptyList(),
                "rag", Collections.emptyList(),
                "ai_usage", Collections.emptyMap(),
                "quizTeam", Collections.emptyList(),
                "quizDivision", Collections.emptyList(),
                "recentActivities", Collections.emptyList()
        ));
        model.addAttribute("totalOnboarding", 0);
        model.addAttribute("avgProgress", 0.0);
        model.addAttribute("evaluationSubmissions", 0);
        model.addAttribute("aiActivities", 0);
        model.addAttribute("recentActivities", Collections.emptyList());

        try {
            Map<String, Object> stats = restTemplate.getForObject(AI_SERVER_URL, Map.class);
            if (stats != null) {
                model.addAttribute("stats", stats);
            }

            if (stats != null && stats.get("kpis") instanceof Map<?, ?>) {
                Map<String, Object> kpis = (Map<String, Object>) stats.get("kpis");
                model.addAttribute("totalOnboarding", kpis.getOrDefault("totalOnboarding", 0));
                model.addAttribute("avgProgress", kpis.getOrDefault("avgProgress", 0.0));
                model.addAttribute("evaluationSubmissions", kpis.getOrDefault("evaluationSubmissions", 0));
                model.addAttribute("aiActivities", kpis.getOrDefault("aiActivities", 0));
            }

            List<Map<String, Object>> recentActivities =
                    stats != null
                            ? normalizeRecentActivities((List<Map<String, Object>>) stats.getOrDefault("recentActivities", Collections.emptyList()))
                            : Collections.emptyList();
            model.addAttribute("recentActivities", recentActivities);
        } catch (Exception e) {
            model.addAttribute("error", "AI 서버와 통신할 수 없습니다.");
            model.addAttribute("recentActivities", Collections.emptyList());
        }

        return "admin/auth/home";
    }

    /**
     * 최근 시스템 활동 링크 정규화
     */
    private List<Map<String, Object>> normalizeRecentActivities(List<Map<String, Object>> activities) {
        return activities.stream()
                .peek(activity -> activity.put("url", resolveActivityUrl(String.valueOf(activity.getOrDefault("title", "")))))
                .collect(Collectors.toList());
    }

    /**
     * 활동 제목 기준 관리자 이동 경로 결정
     */
    private String resolveActivityUrl(String title) {
        if (title.contains("문서")) {
            return "/admin/onboarding/documents";
        }
        if (title.contains("평가")) {
            return "/admin/evaluation/main";
        }
        if (title.contains("AI")) {
            return "/admin/home";
        }
        return "/admin/home";
    }
}
