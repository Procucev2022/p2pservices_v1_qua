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
    private String primaryModel = "gemini-3.7-flash";

    @Value("${app.gemini.backup-models:gemini-3.6-flash,gemini-3.5-flash,gemini-3.5-flash-lite,gemini-3.1-flash-lite}")
    private String backupModels = "gemini-3.6-flash,gemini-3.5-flash,gemini-3.5-flash-lite,gemini-3.1-flash-lite";

    @Value("${app.gemini.fallback-model:gemini-3.6-flash}")
    private String fallbackModel = "gemini-3.6-flash";

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

    public List<String> getAllConfiguredModels() {
        List<String> models = new ArrayList<>();
        if (primaryModel != null && !primaryModel.isBlank()) {
            models.add(primaryModel.trim());
        }
        if (backupModels != null && !backupModels.isBlank()) {
            for (String m : backupModels.split(",")) {
                String trimmed = m.trim();
                if (!trimmed.isEmpty() && !models.contains(trimmed)) {
                    models.add(trimmed);
                }
            }
        }
        if (fallbackModel != null && !fallbackModel.isBlank() && !models.contains(fallbackModel.trim())) {
            models.add(fallbackModel.trim());
        }
        if (models.isEmpty()) {
            models.addAll(List.of("gemini-3.7-flash", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite"));
        }
        return models;
    }

    public static Map<String, Object> getRfqExtractionSchema() {
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

        return Map.of(
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
    }

    public static Map<String, Object> getVendorCategorizationSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "industry", Map.of("type", "STRING"),
                        "category", Map.of("type", "STRING"),
                        "subCategories", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                        "capabilities", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                        "suitableProcurementCategories", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
                ),
                "required", List.of("industry", "category", "subCategories", "capabilities", "suitableProcurementCategories")
        );
    }

    public String generateContent(String promptText) {
        return generateContent(promptText, List.of(), null);
    }

    public String generateContentWithSpecificModel(String model, String promptText, List<InlineImage> images) throws Exception {
        return generateContentWithSpecificModel(model, promptText, images, null);
    }

    public String generateContentWithSpecificModel(String model, String promptText, List<InlineImage> images, Map<String, Object> responseSchema) throws Exception {
        String currentApiKey = getNextApiKey();
        List<InlineImage> inlineImages = images != null ? images : List.<InlineImage>of();
        log.info("Sending request to Gemini API (Model: {}, inline images: {})...", model, inlineImages.size());
        return callGeminiModel(model, promptText, inlineImages, responseSchema, currentApiKey);
    }

    /**
     * Sends the prompt through the configured model chain (Primary -> 1st Backup -> 2nd Backup -> ...).
     * Any downtime, 503, 429, or network failure automatically falls back to the next model.
     */
    public String generateContent(String promptText, List<InlineImage> images) {
        return generateContent(promptText, images, null);
    }

    public String generateContent(String promptText, List<InlineImage> images, Map<String, Object> responseSchema) {
        List<String> models = getAllConfiguredModels();
        List<InlineImage> inlineImages = images != null ? images : List.<InlineImage>of();
        String currentApiKey = getNextApiKey();

        Exception lastException = null;
        for (int i = 0; i < models.size(); i++) {
            String model = models.get(i);
            try {
                log.info("Attempting Gemini API call with model [{}/{}: {}]...", i + 1, models.size(), model);
                return callGeminiModel(model, promptText, inlineImages, responseSchema, currentApiKey);
            } catch (Exception e) {
                lastException = e;
                log.warn("Gemini model ({}) failed: {}. Proceeding to next backup model in chain...",
                        model, e.getMessage());
            }
        }
        log.error("All Gemini models in chain failed: {}", models);
        throw new ApplicationException("All Gemini AI API calls failed across models " + models + ": "
                + (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }

    private String callGeminiModel(String model, String promptText, List<InlineImage> images, Map<String, Object> responseSchema, String currentApiKey) throws Exception {
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

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("maxOutputTokens", maxOutputTokens);

        if (responseSchema != null && !responseSchema.isEmpty()) {
            generationConfig.put("responseSchema", responseSchema);
        }

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
