package com.portal.procucev.rfq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenUsageTelemetry {
    private String messageId;
    private String modelName;
    private int promptTokens;
    private int candidateTokens;
    private int totalTokens;
    private int attemptsCount;
    private Double estimatedCostUsd;
    private LocalDateTime createdAt;
}
