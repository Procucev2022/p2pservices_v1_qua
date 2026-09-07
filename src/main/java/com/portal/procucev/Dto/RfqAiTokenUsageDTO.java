package com.portal.procucev.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RfqAiTokenUsageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String rfqNumber;
    private String messageId;
    private String sourceType;
    private String modelName;
    private int promptTokens;
    private int candidateTokens;
    private int totalTokens;
    private int attemptsCount;
    private Double estimatedCostUsd;
    private String formattedCost;
    private LocalDateTime createdAt;
}
