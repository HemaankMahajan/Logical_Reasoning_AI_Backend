package com.AI.writer.app.Entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Builder.Default
    private Long archivedPromptTokens = 0L;

    @Builder.Default
    private Long archivedCompletionTokens = 0L;

    @Builder.Default
    private Long archivedReasoningTokens = 0L;
}
