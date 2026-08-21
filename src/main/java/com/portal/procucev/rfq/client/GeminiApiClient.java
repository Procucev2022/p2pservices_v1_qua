package com.portal.procucev.rfq.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.InlineImage;
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
import java.util.ArrayList;
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

    @Value("${app.gemini.primary-model:gemini-1.5-flash}")
    private String primaryModel;

    @Value("${app.gemini.fallback-model:gemini-2.0-flash}")
    private String fallbackModel;

    @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.connect-timeout-ms:5000}")
    private int connectTimeoutMs = 5000;

    @Value("${app.gemini.read-timeout-ms:60000}")
    private int readTimeoutMs = 60000;

    @Value("${app.gemini.max-output-tokens:65536}")
    private int maxOutputTokens = 65536;

    @PostConstruct
    void init() {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    public String generateContent(String promptText) {
        return generateContent(promptText, List.of());
    }

    /**
     * Sends the prompt, optionally with image attachments as inline data.
     */
    public String generateContent(String promptText, List<InlineImage> images) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApplicationException(
                    "Gemini API key is not configured. Set the GEMINI_API_KEY environment variable "
                            + "(or the app.gemini.api-key property) to enable AI extraction.");
        }
        List<InlineImage> inlineImages = images != null ? images : List.<InlineImage>of();
        log.info("Sending request to Gemini API (Primary Model: {}, inline images: {})...",
                primaryModel, inlineImages.size());
        try {
            return callGeminiModel(primaryModel, promptText, inlineImages);
        } catch (Exception e) {
            log.warn("Primary Gemini model ({}) failed: {}. Retrying with Fallback Model ({})...",
                    primaryModel, e.getMessage(), fallbackModel);
            try {
                return callGeminiModel(fallbackModel, promptText, inlineImages);
            } catch (Exception ex) {
                log.error("Fallback Gemini model ({}) call also failed: {}", fallbackModel, ex.getMessage());
                throw new ApplicationException("Gemini AI API calls failed on both primary (" + primaryModel + ") and fallback (" + fallbackModel + ") models: " + ex.getMessage(), ex);
            }
        }
    }

    private String callGeminiModel(String model, String promptText, List<InlineImage> images) throws Exception {
        String url = String.format("%s/%s:generateContent?key=%s", baseUrl, model, apiKey.trim());

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", promptText);

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(textPart);
        for (InlineImage image : images) {
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", image.mimeType());
            inlineData.put("data", image.base64Data());
            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("inlineData", inlineData);
            parts.add(imagePart);
        }

        Map<String, Object> contentsObj = new HashMap<>();
        contentsObj.put("parts", parts);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(contentsObj));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("maxOutputTokens", maxOutputTokens);

        // Attach strict RFQ extraction responseSchema only when images/RFQ extraction are present
        if (!images.isEmpty()) {
            Map<String, Object> itemSchema = new HashMap<>();
            itemSchema.put("type", "OBJECT");
            itemSchema.put("properties", Map.of(
                    "itemDescription", Map.of("type", "STRING"),
                    "partCode", Map.of("type", "STRING"),
                    "specification", Map.of("type", "STRING"),
                    "quantity", Map.of("type", "NUMBER"),
                    "uom", Map.of("type", "STRING"),
                    "brand", Map.of("type", "STRING"),
                    "category", Map.of("type", "STRING"),
                    "deliveryDate", Map.of("type", "STRING"),
                    "deliveryLocation", Map.of("type", "STRING")
            ));

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
                                    "items", itemSchema
                            )
                    ),
                    "required", List.of("items")
            );
            generationConfig.put("responseSchema", responseSchema);
        }

        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey.trim());

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode candidates = rootNode.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode candidate = candidates.get(0);

                String finishReason = candidate.path("finishReason").asText("");
                if (!finishReason.isEmpty() && !"STOP".equalsIgnoreCase(finishReason)) {
                    log.warn("Gemini model {} stopped with finishReason={} (maxOutputTokens={}).",
                            model, finishReason, maxOutputTokens);
                    if ("MAX_TOKENS".equalsIgnoreCase(finishReason)) {
                        throw new ApplicationException("Gemini response was truncated (finishReason=MAX_TOKENS).");
                    }
                }

                JsonNode partsNode = candidate.path("content").path("parts");
                if (partsNode.isArray() && partsNode.size() > 0) {
                    return partsNode.get(0).path("text").asText();
                }
            }
        }
        throw new ApplicationException("Gemini API response did not contain expected text content.");
    }
}
