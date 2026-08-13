package com.portal.procucev.rfq.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.exception.ApplicationException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;

    private RestTemplate restTemplate;

    @Value("${app.gemini.primary-model:gemini-2.5-flash}")
    private String primaryModel;

    @Value("${app.gemini.fallback-model:gemini-2.0-flash}")
    private String fallbackModel;

    @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.connect-timeout-ms:5000}")
    private int connectTimeoutMs = 5000;

    @Value("${app.gemini.read-timeout-ms:60000}")
    private int readTimeoutMs = 60000;

    @PostConstruct
    void init() {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    public String generateContent(String promptText) {
        log.info("Sending request to Gemini API (Primary Model: {})...", primaryModel);
        try {
            return callGeminiModel(primaryModel, promptText);
        } catch (Exception e) {
            log.warn("Primary Gemini model ({}) failed: {}. Retrying with Fallback Model ({})...",
                    primaryModel, e.getMessage(), fallbackModel);
            try {
                return callGeminiModel(fallbackModel, promptText);
            } catch (Exception ex) {
                log.error("Fallback Gemini model ({}) call also failed: {}", fallbackModel, ex.getMessage());
                throw new ApplicationException("Gemini AI API calls failed on both primary (" + primaryModel + ") and fallback (" + fallbackModel + ") models: " + ex.getMessage(), ex);
            }
        }
    }

    private String callGeminiModel(String model, String promptText) throws Exception {
        String url = String.format("%s/%s:generateContent", baseUrl, model);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", promptText);

        Map<String, Object> contentsObj = new HashMap<>();
        contentsObj.put("parts", List.of(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(contentsObj));

        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "buyerEmail", Map.of("type", "STRING"),
                        "category", Map.of("type", "STRING"),
                        "deliveryLocation", Map.of("type", "STRING"),
                        "deliveryCity", Map.of("type", "STRING"),
                        "deliveryState", Map.of("type", "STRING"),
                        "deliveryPincode", Map.of("type", "STRING"),
                        "deliveryDate", Map.of("type", "STRING"),
                        "items", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "itemDescription", Map.of("type", "STRING"),
                                                "partCode", Map.of("type", "STRING"),
                                                "specification", Map.of("type", "STRING"),
                                                "quantity", Map.of("type", "NUMBER", "nullable", true),
                                                "uom", Map.of("type", "STRING"),
                                                "brand", Map.of("type", "STRING")
                                        ),
                                        "required", List.of("itemDescription", "quantity", "uom")
                                )
                        )
                ),
                "required", List.of("items")
        );

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", responseSchema);

        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode candidates = rootNode.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode partsNode = candidates.get(0).path("content").path("parts");
                if (partsNode.isArray() && partsNode.size() > 0) {
                    return partsNode.get(0).path("text").asText();
                }
            }
        }
        throw new ApplicationException("Gemini API response did not contain expected text content.");
    }
}
