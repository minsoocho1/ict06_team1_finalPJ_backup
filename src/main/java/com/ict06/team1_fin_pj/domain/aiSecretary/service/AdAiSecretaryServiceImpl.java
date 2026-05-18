package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogType;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiLogRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiTemplateDashboardRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiDocumentRepository;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardDocumentStatusDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardFeatureUsageDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardRecentLogDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardSummaryDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardUsageTrendDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdAiSecretaryServiceImpl implements AdAiSecretaryService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final int RECENT_LOG_PAGE_SIZE = 10;

    private final AiLogRepository aiLogRepository;
    private final AiDocumentRepository aiDocumentRepository;
    private final AiTemplateDashboardRepository aiTemplateDashboardRepository;

    @Override
    public AdAiDashboardResponseDto getDashboardData(
            int period,
            String startDate,
            String endDate,
            String department,
            String aiType,
            String result,
            int page
    ) {
        PeriodRange dashboardRange = resolveRange(period, startDate, endDate);
        List<AiLogEntity> currentLogs = loadLogs(dashboardRange);
        List<AiLogEntity> previousLogs = loadLogs(new PeriodRange(
                dashboardRange.compareStartAt(),
                dashboardRange.compareEndAt(),
                dashboardRange.compareStartAt().minusDays(dashboardRange.days()),
                dashboardRange.compareStartAt(),
                dashboardRange.compareLabel(),
                dashboardRange.days()
        ));

        Page<AiLogEntity> recentLogPage = loadRecentLogPage(
                dashboardRange,
                department,
                aiType,
                result,
                page,
                RECENT_LOG_PAGE_SIZE
        );

        return AdAiDashboardResponseDto.builder()
                .summaryCards(buildSummaryCards(currentLogs, previousLogs, dashboardRange.compareLabel()))
                .featureUsageList(buildFeatureUsage(currentLogs, dashboardRange))
                .usageTrendList(buildUsageTrend(dashboardRange, currentLogs))
                .recentLogList(buildRecentLogs(recentLogPage))
                .documentStatusList(buildDocumentStatusSummary())
                .documentStatusTitle(buildDocumentStatusTitle())
                .currentPage(recentLogPage.getTotalPages() == 0 ? 0 : recentLogPage.getNumber() + 1)
                .totalPages(recentLogPage.getTotalPages())
                .totalLogCount(recentLogPage.getTotalElements())
                .hasPrevious(recentLogPage.hasPrevious())
                .hasNext(recentLogPage.hasNext())
                .build();
    }

    @Override
    public byte[] downloadRecentLogCsv(
            int period,
            String startDate,
            String endDate,
            String department,
            String aiType,
            String result
    ) {
        PeriodRange range = resolveRange(period, startDate, endDate);
        List<AiLogEntity> logs = aiLogRepository.findDashboardLogsForExport(
                range.startAt(),
                range.endAt(),
                safe(department),
                safe(aiType),
                safe(result),
                AiLogType.CHATBOT,
                AiLogType.ASSISTANT
        );

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        appendCsvRow(csv,
                "사용 시각",
                "사용자명",
                "부서명",
                "AI 기능",
                "요청 요약",
                "처리 결과",
                "응답 소요 시간"
        );

        for (AiLogEntity log : logs) {
            if (log == null) {
                continue;
            }

            appendCsvRow(csv,
                    formatCsvCell(log.getCreatedAt() == null ? "-" : log.getCreatedAt().format(DATE_TIME_FORMATTER)),
                    formatCsvCell(resolveEmployeeName(log)),
                    formatCsvCell(resolveDepartmentName(log)),
                    formatCsvCell(resolveTypeLabel(log)),
                    formatCsvCell(buildRequestSummary(log)),
                    formatCsvCell(resolveResultLabel(log)),
                    formatCsvCell(resolveDurationText(log))
            );
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<AdAiDashboardSummaryDto> buildSummaryCards(
            List<AiLogEntity> currentLogs,
            List<AiLogEntity> previousLogs,
            String compareLabel
    ) {
        long currentTotal = currentLogs.size();
        long previousTotal = previousLogs.size();
        long currentChatbot = countByType(currentLogs, AiLogType.CHATBOT);
        long previousChatbot = countByType(previousLogs, AiLogType.CHATBOT);
        long currentAssistant = countAssistantOnly(currentLogs);
        long previousAssistant = countAssistantOnly(previousLogs);
        double currentAverageSeconds = averageDurationSeconds(currentLogs);
        double previousAverageSeconds = averageDurationSeconds(previousLogs);

        List<AdAiDashboardSummaryDto> summaryCards = new ArrayList<>();
        summaryCards.add(buildSummaryCard("전체 AI 요청 수", currentTotal, previousTotal, "건", compareLabel, false));
        summaryCards.add(buildSummaryCard("챗봇 질문 수", currentChatbot, previousChatbot, "건", compareLabel, false));
        summaryCards.add(buildSummaryCard("AI 비서 사용 수", currentAssistant, previousAssistant, "건", compareLabel, false));
        summaryCards.add(buildSummaryCard("평균 응답 속도", currentAverageSeconds, previousAverageSeconds, "초", compareLabel, true));
        return summaryCards;
    }

    private AdAiDashboardSummaryDto buildSummaryCard(
            String label,
            double currentValue,
            double previousValue,
            String unit,
            String compareLabel,
            boolean lowerIsBetter
    ) {
        String valueText = "초".equals(unit)
                ? String.format(Locale.KOREA, "%.1f", currentValue)
                : String.valueOf((long) currentValue);
        String changeRate = formatChangeRate(currentValue, previousValue);
        String trend = resolveTrend(currentValue, previousValue, lowerIsBetter);

        return AdAiDashboardSummaryDto.builder()
                .label(label)
                .value(valueText)
                .unit(unit)
                .changeRate(changeRate)
                .compareText(compareLabel)
                .trend(trend)
                .build();
    }

    private String formatChangeRate(double currentValue, double previousValue) {
        if (previousValue == 0.0d) {
            if (currentValue == 0.0d) {
                return "0.0%";
            }
            return "+100.0%";
        }

        double rate = ((currentValue - previousValue) / previousValue) * 100.0d;
        String sign = rate > 0 ? "+" : "";
        return sign + String.format(Locale.KOREA, "%.1f%%", rate);
    }

    private String resolveTrend(double currentValue, double previousValue, boolean lowerIsBetter) {
        if (currentValue == previousValue) {
            return "secondary";
        }

        if (lowerIsBetter) {
            return currentValue <= previousValue ? "up" : "down";
        }

        return currentValue >= previousValue ? "up" : "down";
    }

    private List<AdAiDashboardFeatureUsageDto> buildFeatureUsage(List<AiLogEntity> currentLogs, PeriodRange dashboardRange) {
        long chatbot = countByType(currentLogs, AiLogType.CHATBOT);
        long assistant = countAssistantOnly(currentLogs);
        long correction = countCorrectionLogs(currentLogs);
        long template = countTemplateUsage(dashboardRange);
        long total = chatbot + assistant + correction + template;

        return List.of(
                AdAiDashboardFeatureUsageDto.builder()
                        .label("챗봇")
                        .count(chatbot)
                        .percentage(calculatePercentage(chatbot, total))
                        .barClass("bg-primary")
                        .build(),
                AdAiDashboardFeatureUsageDto.builder()
                        .label("AI 비서")
                        .count(assistant)
                        .percentage(calculatePercentage(assistant, total))
                        .barClass("bg-success")
                        .build(),
                AdAiDashboardFeatureUsageDto.builder()
                        .label("문장 다듬기")
                        .count(correction)
                        .percentage(calculatePercentage(correction, total))
                        .barClass("bg-warning")
                        .build(),
                AdAiDashboardFeatureUsageDto.builder()
                        .label("템플릿 생성")
                        .count(template)
                        .percentage(calculatePercentage(template, total))
                        .barClass("bg-info")
                        .build()
        );
    }

    private int calculatePercentage(long count, long total) {
        if (total <= 0L) {
            return 0;
        }
        return (int) Math.round(count * 100.0d / total);
    }

    private List<AdAiDashboardUsageTrendDto> buildUsageTrend(PeriodRange range, List<AiLogEntity> currentLogs) {
        if (range == null) {
            return List.of();
        }

        Map<LocalDate, TrendCounter> grouped = new LinkedHashMap<>();
        for (AiLogEntity log : currentLogs) {
            if (log == null || log.getCreatedAt() == null) {
                continue;
            }

            LocalDate currentDate = log.getCreatedAt().atZone(SEOUL).toLocalDate();
            TrendCounter counter = grouped.computeIfAbsent(currentDate, key -> new TrendCounter());
            counter.totalCount++;

            if (log.getType() == AiLogType.CHATBOT) {
                counter.chatbotCount++;
            } else if (log.getType() == AiLogType.ASSISTANT && !isCorrectionLog(log)) {
                counter.assistantCount++;
            }
        }

        List<AdAiDashboardUsageTrendDto> usageTrends = new ArrayList<>();
        LocalDate startDate = range.startAt().toLocalDate();
        for (int i = 0; i < range.days(); i++) {
            LocalDate currentDate = startDate.plusDays(i);
            TrendCounter counter = grouped.getOrDefault(currentDate, new TrendCounter());
            usageTrends.add(AdAiDashboardUsageTrendDto.builder()
                    .label(currentDate.format(DATE_LABEL_FORMATTER))
                    .dateValue(currentDate.toString())
                    .count(counter.totalCount)
                    .totalCount(counter.totalCount)
                    .chatbotCount(counter.chatbotCount)
                    .assistantCount(counter.assistantCount)
                    .build());
        }
        return usageTrends;
    }

    private Page<AiLogEntity> loadRecentLogPage(
            PeriodRange range,
            String department,
            String aiType,
            String result,
            int page,
            int size
    ) {
        if (range == null) {
            return Page.empty(PageRequest.of(0, size));
        }

        String normalizedType = safe(aiType).toUpperCase(Locale.ROOT);
        if ("TEMPLATE".equals(normalizedType)) {
            return Page.empty(PageRequest.of(Math.max(page - 1, 0), size));
        }

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        return aiLogRepository.findDashboardLogs(
                range.startAt(),
                range.endAt(),
                safe(department),
                safe(aiType),
                safe(result),
                AiLogType.CHATBOT,
                AiLogType.ASSISTANT,
                pageable
        );
    }

    private List<AdAiDashboardRecentLogDto> buildRecentLogs(Page<AiLogEntity> recentLogPage) {
        if (recentLogPage == null || recentLogPage.isEmpty()) {
            return List.of();
        }

        return recentLogPage.getContent().stream()
                .filter(Objects::nonNull)
                .map(this::toRecentLogDto)
                .toList();
    }

    private boolean matchesDepartment(AiLogEntity log, String department) {
        if (department == null || department.isBlank()) {
            return true;
        }

        if (log.getEmployee() == null || log.getEmployee().getDepartment() == null) {
            return false;
        }

        String deptName = log.getEmployee().getDepartment().getDeptName();
        return deptName != null && deptName.equals(department.trim());
    }

    private boolean matchesAiType(AiLogEntity log, String aiType) {
        if (aiType == null || aiType.isBlank()) {
            return true;
        }

        String normalized = aiType.trim().toUpperCase(Locale.ROOT);
        if ("TEMPLATE".equals(normalized)) {
            return false;
        }

        if ("CORRECTION".equals(normalized)) {
            return log.getType() == AiLogType.ASSISTANT && isCorrectionLog(log);
        }

        return log.getType() != null && log.getType().name().equals(normalized);
    }

    private boolean matchesResult(AiLogEntity log, String result) {
        if (result == null || result.isBlank()) {
            return true;
        }

        String normalized = result.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SUCCESS" -> !isFallbackLog(log) && !isFailLog(log);
            case "FALLBACK" -> isFallbackLog(log);
            case "FAIL" -> isFailLog(log);
            default -> true;
        };
    }

    private boolean isFallbackLog(AiLogEntity log) {
        return containsIgnoreCase(log.getResponse(), "fallback=true");
    }

    private boolean isFailLog(AiLogEntity log) {
        return log.getErrorMessage() != null && !log.getErrorMessage().isBlank() && !isFallbackLog(log);
    }

    private boolean isCorrectionLog(AiLogEntity log) {
        return containsIgnoreCase(log.getQuery(), "feature=CORRECTION");
    }

    private AdAiDashboardRecentLogDto toRecentLogDto(AiLogEntity log) {
        String createdAt = log.getCreatedAt() == null ? "-" : log.getCreatedAt().format(DATE_TIME_FORMATTER);
        String durationText = log.getDurationMs() == null
                ? "0.0초"
                : String.format(Locale.KOREA, "%.1f초", log.getDurationMs() / 1000.0d);
        String responseSummary = safe(log.getResponse()).isBlank()
                ? "-"
                : truncate(log.getResponse(), 120);
        String errorMessage = safe(log.getErrorMessage());
        String messageContent = log.getMessage() == null || log.getMessage().getContent() == null
                ? "-"
                : safe(log.getMessage().getContent()).isBlank()
                ? "-"
                : log.getMessage().getContent().trim();

        return AdAiDashboardRecentLogDto.builder()
                .logId(log.getLogId() == null ? "-" : String.valueOf(log.getLogId()))
                .user(resolveEmployeeName(log))
                .department(resolveDepartmentName(log))
                .type(resolveTypeLabel(log))
                .requestSummary(buildRequestSummary(log))
                .responseSummary(responseSummary)
                .result(resolveResultCode(log))
                .resultLabel(resolveResultLabel(log))
                .durationText(durationText)
                .createdAt(createdAt)
                .errorMessage(errorMessage.isBlank() ? "-" : errorMessage)
                .messageContent(messageContent)
                .sessionId(log.getSession() == null || log.getSession().getSessionId() == null
                        ? "-"
                        : String.valueOf(log.getSession().getSessionId()))
                .messageId(log.getMessage() == null || log.getMessage().getMessageId() == null
                        ? "-"
                        : String.valueOf(log.getMessage().getMessageId()))
                .fallback(isFallbackLog(log))
                .build();
    }

    private String resolveEmployeeName(AiLogEntity log) {
        if (log.getEmployee() == null) {
            return "-";
        }

        String name = log.getEmployee().getName();
        return name == null || name.isBlank() ? "-" : name.trim();
    }

    private String resolveDepartmentName(AiLogEntity log) {
        if (log.getEmployee() == null || log.getEmployee().getDepartment() == null) {
            return "-";
        }

        String deptName = log.getEmployee().getDepartment().getDeptName();
        return deptName == null || deptName.isBlank() ? "-" : deptName.trim();
    }

    private String resolveTypeLabel(AiLogEntity log) {
        if (log.getType() == AiLogType.CHATBOT) {
            return "챗봇";
        }

        if (isCorrectionLog(log)) {
            return "문장 다듬기";
        }

        return "AI 비서";
    }

    private String buildRequestSummary(AiLogEntity log) {
        if (log.getType() == AiLogType.CHATBOT) {
            Integer questionLength = parseIntegerMeta(log.getQuery(), "questionLength");
            return questionLength == null ? "질문 요청" : "질문 " + questionLength + "자";
        }

        String feature = parseTextMeta(log.getQuery(), "feature");
        Integer inputLength = parseIntegerMeta(log.getQuery(), "inputLength");

        if ("CORRECTION".equalsIgnoreCase(feature)) {
            return inputLength == null ? "문장 다듬기 요청" : "문장 다듬기 " + inputLength + "자";
        }

        if ("ASSISTANT_REVISE".equalsIgnoreCase(feature)) {
            return inputLength == null ? "초안 수정 요청" : "초안 수정 " + inputLength + "자";
        }

        if ("ASSISTANT_DRAFT".equalsIgnoreCase(feature)) {
            return inputLength == null ? "초안 생성 요청" : "초안 생성 " + inputLength + "자";
        }

        String query = safe(log.getQuery());
        if (query.isBlank()) {
            return "-";
        }

        return truncate(query, 60);
    }

    private String resolveDurationText(AiLogEntity log) {
        return log.getDurationMs() == null
                ? "0.0초"
                : String.format(Locale.KOREA, "%.1f초", log.getDurationMs() / 1000.0d);
    }

    private String resolveResultCode(AiLogEntity log) {
        if (isFallbackLog(log)) {
            return "FALLBACK";
        }
        if (isFailLog(log)) {
            return "FAIL";
        }
        return "SUCCESS";
    }

    private String resolveResultLabel(AiLogEntity log) {
        return switch (resolveResultCode(log)) {
            case "FALLBACK" -> "대체 응답";
            case "FAIL" -> "실패";
            default -> "성공";
        };
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private LocalDate parseLocalDate(String value) {
        String safeValue = safe(value);
        if (safeValue.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(safeValue);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "-";
        }

        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength - 1) + "…";
    }

    private Integer parseIntegerMeta(String meta, String key) {
        String value = parseTextMeta(meta, key);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String parseTextMeta(String meta, String key) {
        if (meta == null || meta.isBlank() || key == null || key.isBlank()) {
            return null;
        }

        String[] parts = meta.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            String prefix = key + "=";
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || keyword == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private long countAssistantOnly(List<AiLogEntity> logs) {
        return logs.stream()
                .filter(Objects::nonNull)
                .filter(log -> log.getType() == AiLogType.ASSISTANT)
                .filter(log -> !isCorrectionLog(log))
                .count();
    }

    private long countCorrectionLogs(List<AiLogEntity> logs) {
        return logs.stream()
                .filter(Objects::nonNull)
                .filter(log -> log.getType() == AiLogType.ASSISTANT)
                .filter(this::isCorrectionLog)
                .count();
    }

    private long countTemplateUsage(PeriodRange range) {
        if (range == null) {
            return 0L;
        }

        return aiTemplateDashboardRepository.countByCreatedAtBetween(range.startAt(), range.endAt());
    }

    private void appendCsvRow(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(values[i] == null ? "" : values[i]);
        }
        csv.append("\r\n");
    }

    private String formatCsvCell(String value) {
        String safeValue = value == null ? "" : value;
        String normalized = safeValue.replace("\r\n", "\n").replace("\r", "\n");
        boolean needsQuoting = normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n");
        String escaped = normalized.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }

    private String buildDocumentStatusTitle() {
        LocalDate today = LocalDate.now(SEOUL);
        int weekOfMonth = ((today.getDayOfMonth() - 1) / 7) + 1;
        return today.getMonthValue() + "월 " + weekOfMonth + "째 주 문서 처리 상태 요약";
    }

    private List<AdAiDashboardDocumentStatusDto> buildDocumentStatusSummary() {
        long uploaded = aiDocumentRepository.countByCurrentStage("UPLOADED");
        long processing = aiDocumentRepository.countByCurrentStageIn(List.of(
                "CHUNKING",
                "EMBEDDING"
        ));
        long approvalPending = aiDocumentRepository.countByCurrentStage("APPROVAL_PENDING");
        long published = aiDocumentRepository.countByCurrentStage("PUBLISHED");
        long failed = aiDocumentRepository.countByCurrentStageIn(List.of(
                "CHUNK_FAILED",
                "EMBED_FAILED"
        ));

        return List.of(
                buildDocumentStatus("FAILED", "처리 실패", failed, "문서 처리 중 오류가 발생해 추가 확인이 필요한 상태입니다.", "text-bg-danger"),
                buildDocumentStatus("APPROVAL_PENDING", "승인 대기", approvalPending, "문서 반영이 완료되었지만 관리자 최종 승인을 기다리는 상태입니다.", "text-bg-warning"),
                buildDocumentStatus("PUBLISHED", "반영 완료", published, "최종 반영이 완료된 문서 상태입니다.", "text-bg-success"),
                buildDocumentStatus("PROCESSING", "임베딩 진행", processing, "문서 처리와 임베딩이 진행 중인 상태입니다.", "text-bg-primary"),
                buildDocumentStatus("UPLOADED", "업로드 완료", uploaded, "문서 업로드가 완료된 상태입니다.", "text-bg-secondary")
        );
    }

    private AdAiDashboardDocumentStatusDto buildDocumentStatus(
            String key,
            String title,
            long count,
            String description,
            String badgeClass
    ) {
        return AdAiDashboardDocumentStatusDto.builder()
                .key(key)
                .title(title)
                .count(count)
                .description(description)
                .badgeClass(badgeClass)
                .build();
    }

    private List<AiLogEntity> loadLogs(PeriodRange range) {
        if (range == null) {
            return List.of();
        }
        return Optional.ofNullable(
                        aiLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(range.startAt(), range.endAt())
                )
                .orElseGet(List::of);
    }

    private double averageDurationSeconds(List<AiLogEntity> logs) {
        if (logs.isEmpty()) {
            return 0.0d;
        }

        long count = 0L;
        long total = 0L;
        for (AiLogEntity log : logs) {
            if (log.getDurationMs() != null) {
                total += log.getDurationMs();
                count++;
            }
        }

        if (count == 0L) {
            return 0.0d;
        }

        return total / (double) count / 1000.0d;
    }

    private long countByType(List<AiLogEntity> logs, AiLogType type) {
        return logs.stream()
                .filter(Objects::nonNull)
                .filter(log -> log.getType() == type)
                .count();
    }

    private PeriodRange resolveRange(int periodDays, String startDateText, String endDateText) {
        LocalDate startDate = parseLocalDate(startDateText);
        LocalDate endDate = parseLocalDate(endDateText);

        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                LocalDate temp = startDate;
                startDate = endDate;
                endDate = temp;
            }

            long diff = ChronoUnit.DAYS.between(startDate, endDate) + 1L;
            int days = (int) Math.max(diff, 1L);
            LocalDateTime startAt = startDate.atStartOfDay();
            LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();
            LocalDateTime compareEndAt = startAt;
            LocalDateTime compareStartAt = startAt.minusDays(days);
            return new PeriodRange(startAt, endAt, compareStartAt, compareEndAt, "이전 기간 대비", days);
        }

        int days = periodDays == 30 ? 30 : 7;
        LocalDate today = LocalDate.now(SEOUL);
        String compareLabel = days == 30 ? "전월 대비" : "전주 대비";

        LocalDateTime endAt = today.plusDays(1).atStartOfDay();
        LocalDateTime startAt = today.minusDays(days - 1L).atStartOfDay();
        LocalDateTime compareEndAt = startAt;
        LocalDateTime compareStartAt = startAt.minusDays(days);

        return new PeriodRange(startAt, endAt, compareStartAt, compareEndAt, compareLabel, days);
    }

    private static final class PeriodRange {
        private final LocalDateTime startAt;
        private final LocalDateTime endAt;
        private final LocalDateTime compareStartAt;
        private final LocalDateTime compareEndAt;
        private final String compareLabel;
        private final int days;

        private PeriodRange(
                LocalDateTime startAt,
                LocalDateTime endAt,
                LocalDateTime compareStartAt,
                LocalDateTime compareEndAt,
                String compareLabel,
                int days
        ) {
            this.startAt = startAt;
            this.endAt = endAt;
            this.compareStartAt = compareStartAt;
            this.compareEndAt = compareEndAt;
            this.compareLabel = compareLabel;
            this.days = days;
        }

        private LocalDateTime startAt() {
            return startAt;
        }

        private LocalDateTime endAt() {
            return endAt;
        }

        private LocalDateTime compareStartAt() {
            return compareStartAt;
        }

        private LocalDateTime compareEndAt() {
            return compareEndAt;
        }

        private String compareLabel() {
            return compareLabel;
        }

        private int days() {
            return days;
        }
    }

    private static final class TrendCounter {
        private long totalCount;
        private long chatbotCount;
        private long assistantCount;
    }
}
