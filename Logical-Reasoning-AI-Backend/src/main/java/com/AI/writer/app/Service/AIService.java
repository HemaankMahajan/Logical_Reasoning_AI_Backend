package com.AI.writer.app.Service;

import com.AI.writer.app.Entity.PromptHistory;
import com.AI.writer.app.Entity.User;
import com.AI.writer.app.Repository.PromptHistoryRepository;
import com.AI.writer.app.Repository.UserRepository;
import com.AI.writer.app.Util.AIResponseDTO;
import com.AI.writer.app.Util.PromptRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AIService {

    @Value("${nvidia.api.url}")
    private String nvidiaApiUrl;

    @Value("${nvidia.api.key}")
    private String nvidiaApiKey;

    private final WebClient webClient;
    private final PromptHistoryRepository promptHistoryRepository;
    private final UserRepository userRepository; // Added UserRepository field

    // Updated constructor injection
    public AIService(WebClient.Builder webClientBuilder,
                     PromptHistoryRepository promptHistoryRepository,
                     UserRepository userRepository) {
        this.webClient = webClientBuilder.build();
        this.promptHistoryRepository = promptHistoryRepository;
        this.userRepository = userRepository;
    }

    public AIResponseDTO processPrompt(PromptRequest promptRequest, Long userId) {
        Map<String, Object> requestBody = Map.of(
                "model", "nvidia/nemotron-3.5-super-vl:free",
                "messages", new Object[]{
                        Map.of("role", "user", "content", promptRequest.getPromptText())
                },
                "reasoning", Map.of("enabled", true)
        );

        AIResponseDTO apiResponse = webClient.post()
                .uri(nvidiaApiUrl)
                .header("Authorization", "Bearer " + nvidiaApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(AIResponseDTO.class)
                .block();

        if (apiResponse != null && apiResponse.getChoices() != null && !apiResponse.getChoices().isEmpty()) {
            String content = apiResponse.getChoices().get(0).getMessage().getContent();
            int promptTokens = apiResponse.getUsage() != null ? apiResponse.getUsage().getPrompt_tokens() : 0;
            int completionTokens = apiResponse.getUsage() != null ? apiResponse.getUsage().getCompletion_tokens() : 0;
            int reasoningTokens = (apiResponse.getUsage() != null && apiResponse.getUsage().getCompletion_tokens_details() != null)
                    ? apiResponse.getUsage().getCompletion_tokens_details().getReasoning_tokens() : 0;
            int totalTokens = apiResponse.getUsage() != null ? apiResponse.getUsage().getTotal_tokens() : 0;

            // Save history for user
            PromptHistory history = PromptHistory.builder()
                    .userId(userId)
                    .promptText(promptRequest.getPromptText())
                    .aiResponse(content)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .reasoningTokens(reasoningTokens)
                    .totalTokens(totalTokens)
                    .createdAt(LocalDateTime.now())
                    .build();

            promptHistoryRepository.save(history);
        }

        return apiResponse;
    }

    public List<PromptHistory> getUserHistory(Long userId) {
        return promptHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Map<String, Object> getDashboardStats(Long userId, int month, int year) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        List<Object[]> dayStats = promptHistoryRepository.getTokenSumSince(userId, startOfDay);

        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);
        List<Object[]> monthStats = promptHistoryRepository.getTokenSumBetween(userId, startOfMonth, endOfMonth);

        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        List<Object[]> yearStats = promptHistoryRepository.getTokenSumBetween(userId, startOfYear, endOfYear);

        Map<String, Integer> yearlyParsed = parseStats(yearStats);

        // Include preserved archived tokens if viewing the current calendar year
        if (year == now.getYear()) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                int pArch = user.getArchivedPromptTokens() != null ? user.getArchivedPromptTokens().intValue() : 0;
                int cArch = user.getArchivedCompletionTokens() != null ? user.getArchivedCompletionTokens().intValue() : 0;
                int rArch = user.getArchivedReasoningTokens() != null ? user.getArchivedReasoningTokens().intValue() : 0;

                yearlyParsed = Map.of(
                        "promptTokens", yearlyParsed.get("promptTokens") + pArch,
                        "completionTokens", yearlyParsed.get("completionTokens") + cArch,
                        "reasoningTokens", yearlyParsed.get("reasoningTokens") + rArch
                );
            }
        }

        return Map.of(
                "today", parseStats(dayStats),
                "monthly", parseStats(monthStats),
                "yearly", yearlyParsed
        );
    }

    private Map<String, Integer> parseStats(List<Object[]> result) {
        if (result == null || result.isEmpty() || result.get(0) == null) {
            return Map.of("promptTokens", 0, "completionTokens", 0, "reasoningTokens", 0);
        }
        Object[] row = result.get(0);
        return Map.of(
                "promptTokens", row[0] != null ? ((Number) row[0]).intValue() : 0,
                "completionTokens", row[1] != null ? ((Number) row[1]).intValue() : 0,
                "reasoningTokens", row[2] != null ? ((Number) row[2]).intValue() : 0
        );
    }

    public void clearUserHistory(Long userId, boolean preserveAnalytics) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (preserveAnalytics) {
            List<Object[]> totals = promptHistoryRepository.getAllTimeTokenSum(userId);
            if (!totals.isEmpty()) {
                Object[] row = totals.get(0);
                long pTok = ((Number) row[0]).longValue();
                long cTok = ((Number) row[1]).longValue();
                long rTok = ((Number) row[2]).longValue();

                user.setArchivedPromptTokens((user.getArchivedPromptTokens() != null ? user.getArchivedPromptTokens() : 0L) + pTok);
                user.setArchivedCompletionTokens((user.getArchivedCompletionTokens() != null ? user.getArchivedCompletionTokens() : 0L) + cTok);
                user.setArchivedReasoningTokens((user.getArchivedReasoningTokens() != null ? user.getArchivedReasoningTokens() : 0L) + rTok);
            }
        } else {
            user.setArchivedPromptTokens(0L);
            user.setArchivedCompletionTokens(0L);
            user.setArchivedReasoningTokens(0L);
        }

        userRepository.save(user);
        promptHistoryRepository.deleteAllByUserId(userId);
    }
}