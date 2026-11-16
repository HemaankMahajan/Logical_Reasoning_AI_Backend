package com.AI.writer.app.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "prompt_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String promptText;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String aiResponse;

    private int promptTokens;
    private int completionTokens;
    private int reasoningTokens;
    private int totalTokens;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}