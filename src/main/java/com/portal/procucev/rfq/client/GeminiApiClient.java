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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;

    private RestTemplate restTemplate;

    @Value("${app.gemini.primary-model:gemini-3.7-flash}")
    private String primaryModel;

    /**
     * Availability fallback, used only when the primary model call throws. A response that parses
     * but reads the email wrongly never gets here, which is why the stronger model extracts first.
     */
    @Value("${app.gemini.fallback-model:gemini-3.5-flash-lite}")
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

    private String lastParsedApiKey = null;
    private List<String> apiKeys = List.of();
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    @PostConstruct
    void init() {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
        refreshApiKeys();
    }

    public synchronized void refreshApiKeys() {
        this.lastParsedApiKey = null;
        getApiKeys();
    }

    public synchronized List<String> getApiKeys() {
        if (apiKey == null || apiKey.isBlank()) {
            this.apiKeys = List.of();
            this.lastParsedApiKey = apiKey;
            return this.apiKeys;
        }
        if (!apiKey.equals(lastParsedApiKey)) {
            this.apiKeys = Arrays.stream(apiKey.split(","))
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .toList();
            this.lastParsedApiKey = apiKey;
            log.info("Initialized GeminiApiClient with {} API key(s) for round-robin rotation.", this.apiKeys.size());
        }
        return this.apiKeys;
    }

    private String getNextApiKey() {
        List<String> keys = getApiKeys();
        if (keys.isEmpty()) {
            throw new ApplicationException(
                    "Gemini API key is not configured. Set the GEMINI_API_KEY environment variable "
                            + "(or the app.gemini.api-key property) to enable AI extraction.");
        }
        int index = Math.floorMod(keyIndex.getAndIncrement(), keys.size());
        return keys.get(index);
    }

    public String generateContent(String promptText) {
        return generateContent(promptText, List.of());
    }

    /**
     * Sends the prompt, optionally with image attachments as inline data so a requirement sent as
     * a screenshot or photograph can be read. With an empty image list the request is identical to
     * the text-only form.
     */
    public String generateContent(String promptText, List<InlineImage> images) {
        String currentApiKey = getNextApiKey();
        List<InlineImage> inlineImages = images != null ? images : List.<InlineImage>of();
        
        String cleanPrimary = sanitizeModelName(primaryModel, "gemini-2.5-flash");
        String cleanFallback = sanitizeModelName(fallbackModel, "gemini-1.5-flash");

        log.info("Sending request to Gemini API (Primary Model: {}, inline images: {})...",
                cleanPrimary, inlineImages.size());
        try {
            return callGeminiModel(cleanPrimary, promptText, inlineImages, currentApiKey);
        } catch (Exception e) {
            log.warn("Primary Gemini model ({}) failed: {}. Retrying with Fallback Model ({})...",
                    cleanPrimary, e.getMessage(), cleanFallback);
            try {
                return callGeminiModel(cleanFallback, promptText, inlineImages, currentApiKey);
            } catch (Exception ex) {
                log.error("Fallback Gemini model ({}) call also failed: {}", cleanFallback, ex.getMessage());
                throw new ApplicationException("Gemini AI API calls failed on both primary (" + cleanPrimary + ") and fallback (" + cleanFallback + ") models: " + ex.getMessage(), ex);
            }
        }
    }

    private String sanitizeModelName(String rawModel, String defaultFallback) {
        if (rawModel == null || rawModel.isBlank()) {
            return defaultFallback;
        }
        return rawModel.trim().toLowerCase().replaceAll("\\s+", "-");
    }

    private String callGeminiModel(String model, String promptText, List<InlineImage> images, String currentApiKey) throws Exception {
        String url = String.format("%s/%s:generateContent", baseUrl, model);

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

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", responseSchema);
        // A requirement sheet with a hundred-plus line items needs far more output budget than the
        // model default. Without this the response is cut mid-array and the JSON fails to parse.
        generationConfig.put("maxOutputTokens", maxOutputTokens);

        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", currentApiKey);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode candidates = rootNode.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode candidate = candidates.get(0);

                // finishReason was previously ignored, so a truncated or filtered generation was
                // handed back as though it were complete. Surfacing it turns a silent partial
                // extraction into a diagnosable failure.
                String finishReason = candidate.path("finishReason").asText("");
                if (!finishReason.isEmpty() && !"STOP".equalsIgnoreCase(finishReason)) {
                    log.warn("Gemini model {} stopped with finishReason={} (maxOutputTokens={}). The extraction may be incomplete.",
                            model, finishReason, maxOutputTokens);
                    if ("MAX_TOKENS".equalsIgnoreCase(finishReason)) {
                        throw new ApplicationException("Gemini response was truncated (finishReason=MAX_TOKENS). "
                                + "The request likely exceeded the output budget of " + maxOutputTokens
                                + " tokens; raise app.gemini.max-output-tokens or split the attachment.");
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
