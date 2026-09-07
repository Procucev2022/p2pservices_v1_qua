package com.portal.procucev.rfq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rfq_ai_token_usage")
public class RfqAiTokenUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfq_number", nullable = false)
    private String rfqNumber;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "prompt_tokens")
    private int promptTokens;

    @Column(name = "candidate_tokens")
    private int candidateTokens;

    @Column(name = "total_tokens")
    private int totalTokens;

    @Column(name = "attempts_count")
    private int attemptsCount;

    @Column(name = "estimated_cost_usd")
    private Double estimatedCostUsd;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
