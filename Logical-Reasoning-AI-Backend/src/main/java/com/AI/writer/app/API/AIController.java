package com.AI.writer.app.API;

import com.AI.writer.app.Entity.PromptHistory;
import com.AI.writer.app.Service.AIService;
import com.AI.writer.app.Util.AIResponseDTO;
import com.AI.writer.app.Util.PromptRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@AllArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/generate")
    public ResponseEntity<AIResponseDTO> generateResponse(@RequestBody PromptRequest request, @RequestAttribute("userId") Long userId) {
        AIResponseDTO response = aiService.processPrompt(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<PromptHistory>> getHistory(@RequestAttribute("userId") Long userId) {
        List<PromptHistory> history = aiService.getUserHistory(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String,Object>> getDashboard(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        int targetMonth = (month != null) ? month : LocalDateTime.now().getMonthValue();
        int targetYear = (year != null) ? year : LocalDateTime.now().getYear();

        Map<String, Object> stats = aiService.getDashboardStats(userId, targetMonth, targetYear);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/history/clear")
    public ResponseEntity<?> clearHistory(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "true") boolean preserveAnalytics) {
        aiService.clearUserHistory(userId, preserveAnalytics);
        return ResponseEntity.ok(Map.of("message", "Prompt history cleared successfully."));
    }
}