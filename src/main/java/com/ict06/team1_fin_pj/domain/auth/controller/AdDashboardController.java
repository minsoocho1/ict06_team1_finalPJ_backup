/**
 * @FileName : AdDashboardController.java
 * @Description : 관리자 대시보드 화면 컨트롤러
 * @Author : 김다솜
 * @Date : 2026. 05. 11
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -----------------------------------------------
 * @ 2026.05.11    김다솜        최초 생성
 * @ 2026.05.12    김다솜        관리자 대시보드 실제 데이터 연동 및 최근 활동 바인딩
 * @ 2026.05.18    김다솜        AI 통계 서버 장애 시 DB 기반 통계 대체 조회 추가
 */
package com.ict06.team1_fin_pj.domain.auth.controller;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdDashboardController {

    private static final String AI_SERVER_URL = "http://localhost:8000/api/stats/dashboard";

    private final RestTemplate restTemplate;
    private final EntityManager entityManager;

    @GetMapping("/home")
    @SuppressWarnings("unchecked")
    public String adminHome(Model model) {
        Map<String, Object> dashboardStats = buildDbDashboardStats();

        try {
            Map<String, Object> aiStats = restTemplate.getForObject(AI_SERVER_URL, Map.class);
            if (aiStats != null) {
                dashboardStats = mergeDashboardStats(dashboardStats, aiStats);
            }
        } catch (Exception e) {
            model.addAttribute("error", "AI 통계 서버 연결에 실패하여 DB 기준 통계를 표시합니다.");
        }

        Map<String, Object> kpis = dashboardStats.get("kpis") instanceof Map<?, ?> rawKpis
                ? (Map<String, Object>) rawKpis
                : Collections.emptyMap();
        List<Map<String, Object>> recentActivities =
                normalizeRecentActivities((List<Map<String, Object>>) dashboardStats.getOrDefault("recentActivities", Collections.emptyList()));

        model.addAttribute("stats", dashboardStats);
        model.addAttribute("totalOnboarding", kpis.getOrDefault("totalOnboarding", 0));
        model.addAttribute("avgProgress", kpis.getOrDefault("avgProgress", 0.0));
        model.addAttribute("evaluationSubmissions", kpis.getOrDefault("evaluationSubmissions", 0));
        model.addAttribute("aiActivities", kpis.getOrDefault("aiActivities", 0));
        model.addAttribute("recentActivities", recentActivities);

        return "admin/auth/home";
    }

    private Map<String, Object> buildDbDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<Map<String, Object>> onboardingTeam = fetchOnboardingProgress(false);
        List<Map<String, Object>> onboardingDivision = fetchOnboardingProgress(true);
        List<Map<String, Object>> quizTeam = fetchQuizScores(false);
        List<Map<String, Object>> quizDivision = fetchQuizScores(true);

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalOnboarding", fetchLong("select count(distinct emp_no) from roadmap"));
        kpis.put("avgProgress", averageRate(onboardingTeam, "completion_rate"));
        kpis.put("evaluationSubmissions", fetchLong("""
                select count(*)
                from (
                    select qr.emp_no, q.category_name
                    from quiz_result qr
                    join quiz_question q on q.question_id = qr.question_id
                    group by qr.emp_no, q.category_name
                ) submissions
                """));
        kpis.put("aiActivities", fetchLong("select count(*) from ai_log"));

        stats.put("kpis", kpis);
        stats.put("onboardingTeam", onboardingTeam);
        stats.put("onboardingDivision", onboardingDivision);
        stats.put("rag", fetchRagStatus());
        stats.put("ai_usage", fetchAiUsage());
        stats.put("quizTeam", quizTeam);
        stats.put("quizDivision", quizDivision);
        stats.put("evaluationStatus", fetchEvaluationStatus());
        stats.put("recentActivities", fetchRecentActivities());
        return stats;
    }

    private Map<String, Object> mergeDashboardStats(Map<String, Object> fallback, Map<String, Object> aiStats) {
        Map<String, Object> merged = new LinkedHashMap<>(fallback);
        aiStats.forEach((key, value) -> {
            if (hasData(value)) {
                merged.put(key, value);
            }
        });
        return merged;
    }

    private boolean hasData(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        return true;
    }

    private List<Map<String, Object>> fetchOnboardingProgress(boolean division) {
        String deptExpr = division ? "coalesce(parent.dept_name, d.dept_name)" : "d.dept_name";
        return fetchRows("""
                select %s as dept_name,
                       round(
                           coalesce(
                               sum(case when rp.status = 'COMPLETED' then 1 else 0 end) * 100.0
                               / nullif(count(ri.item_id), 0),
                               0
                           ),
                           1
                       ) as completion_rate
                from roadmap r
                join employee e on e.emp_no = r.emp_no
                join department d on d.dept_id = e.dept_id
                left join department parent on parent.dept_id = d.parent_dept_id
                join road_item ri on ri.roadmap_id = r.roadmap_id
                left join road_progress rp on rp.item_id = ri.item_id and rp.emp_no = r.emp_no
                group by %s
                order by %s
                """.formatted(deptExpr, deptExpr, deptExpr), "dept_name", "completion_rate");
    }

    private List<Map<String, Object>> fetchQuizScores(boolean division) {
        String deptExpr = division ? "coalesce(parent.dept_name, d.dept_name)" : "d.dept_name";
        return fetchRows("""
                select %s as dept_name,
                       round(avg(coalesce(qr.score, 0) * 100.0 / nullif(q.score, 0)), 1) as avg_score
                from quiz_result qr
                join quiz_question q on q.question_id = qr.question_id
                join employee e on e.emp_no = qr.emp_no
                join department d on d.dept_id = e.dept_id
                left join department parent on parent.dept_id = d.parent_dept_id
                group by %s
                order by %s
                """.formatted(deptExpr, deptExpr, deptExpr), "dept_name", "avg_score");
    }

    private List<Map<String, Object>> fetchRagStatus() {
        return fetchRows("""
                select coalesce(current_stage, 'UNKNOWN') as stage, count(*) as count
                from document
                group by coalesce(current_stage, 'UNKNOWN')
                order by stage
                """, "stage", "count");
    }

    private Map<String, Object> fetchAiUsage() {
        Map<String, Object> usage = new LinkedHashMap<>();
        for (Map<String, Object> row : fetchRows("""
                select type, count(*) as count
                from ai_log
                group by type
                order by type
                """, "type", "count")) {
            usage.put(String.valueOf(row.get("type")), row.get("count"));
        }
        return usage;
    }

    private List<Map<String, Object>> fetchEvaluationStatus() {
        return fetchRows("""
                select case when score_rate >= 80 then '합격' else '보완필요' end as status,
                       count(*) as count
                from (
                    select qr.emp_no,
                           q.category_name,
                           sum(coalesce(qr.score, 0)) * 100.0 / nullif(sum(coalesce(q.score, 0)), 0) as score_rate
                    from quiz_result qr
                    join quiz_question q on q.question_id = qr.question_id
                    group by qr.emp_no, q.category_name
                ) result_summary
                group by case when score_rate >= 80 then '합격' else '보완필요' end
                order by status
                """, "status", "count");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchRecentActivities() {
        List<Map<String, Object>> activities = new ArrayList<>();
        List<Object[]> rows = entityManager.createNativeQuery("""
                select title, message, activity_time, border_class, text_class
                from (
                    select '평가 제출 완료' as title,
                           e.name || ' · ' || q.category_name as message,
                           max(qr.submitted_at) as activity_time,
                           'border-success' as border_class,
                           'text-success' as text_class
                    from quiz_result qr
                    join quiz_question q on q.question_id = qr.question_id
                    join employee e on e.emp_no = qr.emp_no
                    group by e.name, q.category_name

                    union all

                    select '온보딩 학습 완료' as title,
                           e.name || ' · ' || ri.item_title as message,
                           now() as activity_time,
                           'border-primary' as border_class,
                           'text-primary' as text_class
                    from road_progress rp
                    join road_item ri on ri.item_id = rp.item_id
                    join employee e on e.emp_no = rp.emp_no
                    where rp.status = 'COMPLETED'

                    union all

                    select 'AI 활동 기록' as title,
                           coalesce(e.name, '알 수 없음') || ' · ' || al.type as message,
                           al.created_at as activity_time,
                           'border-info' as border_class,
                           'text-info' as text_class
                    from ai_log al
                    left join employee e on e.emp_no = al.emp_no
                ) activities
                where activity_time is not null
                order by activity_time desc
                limit 6
                """).getResultList();

        for (Object[] row : rows) {
            Map<String, Object> activity = new LinkedHashMap<>();
            activity.put("title", row[0]);
            activity.put("message", row[1]);
            activity.put("time_label", toTimeLabel(row[2]));
            activity.put("borderClass", row[3]);
            activity.put("textClass", row[4]);
            activities.add(activity);
        }
        return activities;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchRows(String sql, String firstKey, String secondKey) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();
        for (Object[] row : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put(firstKey, row[0]);
            item.put(secondKey, toNumber(row[1]));
            rows.add(item);
        }
        return rows;
    }

    private long fetchLong(String sql) {
        Object value = entityManager.createNativeQuery(sql).getSingleResult();
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Object toNumber(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(1, RoundingMode.HALF_UP).doubleValue();
        }
        if (value instanceof Number number) {
            return number;
        }
        return 0;
    }

    private double averageRate(List<Map<String, Object>> rows, String key) {
        return rows.stream()
                .map(row -> row.get(key))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToDouble(Number::doubleValue)
                .average()
                .orElse(0.0);
    }

    private String toTimeLabel(Object value) {
        LocalDateTime activityTime = null;
        if (value instanceof Timestamp timestamp) {
            activityTime = timestamp.toLocalDateTime();
        } else if (value instanceof LocalDateTime localDateTime) {
            activityTime = localDateTime;
        }
        if (activityTime == null) {
            return "-";
        }

        long minutes = Math.max(0, Duration.between(activityTime, LocalDateTime.now()).toMinutes());
        if (minutes < 1) {
            return "방금 전";
        }
        if (minutes < 60) {
            return minutes + "분 전";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "시간 전";
        }
        return (hours / 24) + "일 전";
    }

    /**
     * Normalize recent activity links from either AI-server stats or DB fallback stats.
     */
    private List<Map<String, Object>> normalizeRecentActivities(List<Map<String, Object>> activities) {
        return activities.stream()
                .peek(activity -> activity.put("url", resolveActivityUrl(String.valueOf(activity.getOrDefault("title", "")))))
                .collect(Collectors.toList());
    }

    private String resolveActivityUrl(String title) {
        if (title.contains("문서")) {
            return "/admin/onboarding/documents";
        }
        if (title.contains("평가")) {
            return "/admin/evaluation/main";
        }
        if (title.contains("온보딩")) {
            return "/admin/onboarding/schedules";
        }
        if (title.contains("AI")) {
            return "/admin/AiSecretary/dashboard";
        }
        return "/admin/home";
    }
}
