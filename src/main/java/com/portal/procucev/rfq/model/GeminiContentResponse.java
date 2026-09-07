package com.portal.procucev.rfq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiContentResponse {
    private String text;
    private String model;
    private int promptTokens;
    private int candidateTokens;
    private int totalTokens;

    public String getModelName() {
        return model != null ? model : "gemini-flash";
    }
}
