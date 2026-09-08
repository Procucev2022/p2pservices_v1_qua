package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.client.GeminiApiClient;
import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.InlineImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

public class GeminiApiClientTest {

    private RestTemplateBuilder restTemplateBuilder;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private GeminiApiClient client;

    @BeforeEach
    void setUp() {
        restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
        restTemplate = Mockito.mock(RestTemplate.class);
        objectMapper = new ObjectMapper();

        Mockito.when(restTemplateBuilder.setConnectTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        Mockito.when(restTemplateBuilder.setReadTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);

        client = new GeminiApiClient(restTemplateBuilder, objectMapper);
        ReflectionTestUtils.setField(client, "primaryModel", "gemini-3.7-flash");
        ReflectionTestUtils.setField(client, "backupModels", "gemini-3.6-flash,gemini-3.5-flash,gemini-3.5-flash-lite,gemini-3.1-flash-lite");
        ReflectionTestUtils.setField(client, "fallbackModel", "gemini-3.6-flash");
        ReflectionTestUtils.setField(client, "baseUrl", "https://generativelanguage.googleapis.com/v1beta/models");
        ReflectionTestUtils.setField(client, "apiKey", "test-key");

        ReflectionTestUtils.invokeMethod(client, "init");
    }

    @Test
    @DisplayName("Test getAllConfiguredModels returns ordered hierarchy")
    void testGetAllConfiguredModels() {
        List<String> models = client.getAllConfiguredModels();
        assertEquals(5, models.size());
        assertEquals("gemini-3.7-flash", models.get(0));
        assertEquals("gemini-3.6-flash", models.get(1));
        assertEquals("gemini-3.5-flash", models.get(2));
        assertEquals("gemini-3.5-flash-lite", models.get(3));
        assertEquals("gemini-3.1-flash-lite", models.get(4));

        // When models are null or empty
        ReflectionTestUtils.setField(client, "primaryModel", "");
        ReflectionTestUtils.setField(client, "backupModels", "gemini-3.6-flash, gemini-3.7-flash");
        ReflectionTestUtils.setField(client, "fallbackModel", "gemini-fallback");
        List<String> modelsWithFallback = client.getAllConfiguredModels();
        assertTrue(modelsWithFallback.contains("gemini-fallback"));

        ReflectionTestUtils.setField(client, "primaryModel", null);
        ReflectionTestUtils.setField(client, "backupModels", null);
        ReflectionTestUtils.setField(client, "fallbackModel", null);
        List<String> defaultModels = client.getAllConfiguredModels();
        assertFalse(defaultModels.isEmpty());

        ReflectionTestUtils.setField(client, "apiKey", null);
        assertTrue(client.getApiKeys().isEmpty());
    }

    @Test
    @DisplayName("Test generateContent primary model success and with inline images")
    void testGenerateContentPrimarySuccess() {
        String jsonResponseBody = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\"text\": \"{\\\"buyerEmail\\\":\\\"buyer@test.com\\\"}\"}\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Mockito.when(restTemplate.postForEntity(contains("gemini-3.7-flash"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponseBody, HttpStatus.OK));

        String result = client.generateContent("Test Prompt");

        assertNotNull(result);
        assertTrue(result.contains("buyer@test.com"));

        // With inline images
        List<InlineImage> images = List.of(new InlineImage("spec.png", "image/png", "base64bytes"));
        String imageResult = client.generateContent("Test Prompt with Image", images);
        assertNotNull(imageResult);
    }

    @Test
    @DisplayName("Test generateContentWithSpecificModel")
    void testGenerateContentWithSpecificModel() throws Exception {
        String jsonResponseBody = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\"text\": \"Specific model text\"}\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Mockito.when(restTemplate.postForEntity(contains("custom-model"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponseBody, HttpStatus.OK));

        String res = client.generateContentWithSpecificModel("custom-model", "prompt", null);
        assertEquals("Specific model text", res);

        List<InlineImage> images = List.of(new InlineImage("doc.jpg", "image/jpeg", "imgbytes"));
        String res2 = client.generateContentWithSpecificModel("custom-model", "prompt", images);
        assertEquals("Specific model text", res2);
    }

    @Test
    @DisplayName("Test finishReason handling")
    void testFinishReasonHandling() {
        String maxTokensResponse = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"finishReason\": \"MAX_TOKENS\",\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\"text\": \"Truncated...\"}\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(maxTokensResponse, HttpStatus.OK));

        assertThrows(ApplicationException.class, () -> client.generateContent("Test Prompt"));

        String otherFinishReason = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"finishReason\": \"SAFETY\",\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\"text\": \"Safety filtered\"}\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(otherFinishReason, HttpStatus.OK));

        String safetyRes = client.generateContent("Test Prompt");
        assertEquals("Safety filtered", safetyRes);
    }

    @Test
    @DisplayName("Test generateContent primary fails, 1st backup model succeeds")
    void testGenerateContentPrimaryFailsFallbackSuccess() {
        Mockito.when(restTemplate.postForEntity(contains("gemini-3.7-flash"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Primary 503 Unavailable"));

        String fallbackResponseBody = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\"text\": \"Fallback Success Text\"}\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Mockito.when(restTemplate.postForEntity(contains("gemini-3.6-flash"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(fallbackResponseBody, HttpStatus.OK));

        String result = client.generateContent("Test Prompt");

        assertEquals("Fallback Success Text", result);
    }

    @Test
    @DisplayName("Test generateContent cascades through 3 models until 4th succeeds")
    void testGenerateContentCascadesThroughModels() {
        Mockito.when(restTemplate.postForEntity(contains("gemini-3.7-flash"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("503 Service Unavailable"));
        Mockito.when(restTemplate.postForEntity(contains("gemini-3.6-flash"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("429 Rate Limit"));
        Mockito.when(restTemplate.postForEntity(contains("gemini-3.5-flash"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("500 Internal Server Error"));

        String successBody = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\"text\": \"Recovered on 3.5-flash-lite\"}\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Mockito.when(restTemplate.postForEntity(contains("gemini-3.5-flash-lite"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(successBody, HttpStatus.OK));

        String result = client.generateContent("Test Prompt");
        assertEquals("Recovered on 3.5-flash-lite", result);
    }

    @Test
    @DisplayName("Test generateContent primary and all backup models fail")
    void testGenerateContentBothFail() {
        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API Outage"));

        assertThrows(ApplicationException.class, () -> client.generateContent("Test Prompt"));
    }

    @Test
    @DisplayName("Test generateContent with empty candidates array throws")
    void testGenerateContentEmptyCandidates() {
        String emptyResponse = "{\"candidates\": []}";

        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(emptyResponse, HttpStatus.OK));

        assertThrows(ApplicationException.class, () -> client.generateContent("Test Prompt"));
    }

    @Test
    @DisplayName("Test generateContent with empty parts array throws")
    void testGenerateContentEmptyParts() {
        String emptyPartsResponse = "{\"candidates\": [{\"content\": {\"parts\": []}}]}";

        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(emptyPartsResponse, HttpStatus.OK));

        assertThrows(ApplicationException.class, () -> client.generateContent("Test Prompt"));
    }

    @Test
    @DisplayName("Test generateContent with null body throws")
    void testGenerateContentNullBody() {
        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThrows(ApplicationException.class, () -> client.generateContent("Test Prompt"));
    }

    @Test
    @DisplayName("Test generateContent with non-2xx response throws")
    void testGenerateContentNon2xxResponse() {
        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{}", HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ApplicationException.class, () -> client.generateContent("Test Prompt"));
    }

    @Test
    @DisplayName("Test generateContent with missing content path throws")
    void testGenerateContentMissingContentPath() {
        String missingContent = "{\"candidates\": [{\"no_content\": {}}]}";

        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(missingContent, HttpStatus.OK));

        assertThrows(ApplicationException.class, () -> client.generateContent("Test Prompt"));
    }

    @Test
    @DisplayName("Test multiple API keys round-robin rotation in sequential calls")
    void testMultipleApiKeysRoundRobinRotation() {
        ReflectionTestUtils.setField(client, "apiKey", "key-one, key-two, key-three");
        ReflectionTestUtils.invokeMethod(client, "refreshApiKeys");

        assertEquals(3, client.getApiKeys().size());
        assertEquals(List.of("key-one", "key-two", "key-three"), client.getApiKeys());

        String okResponse = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"OK\"}]}}]}";
        org.mockito.ArgumentCaptor<org.springframework.http.HttpEntity<String>> entityCaptor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.http.HttpEntity.class);

        Mockito.when(restTemplate.postForEntity(anyString(), entityCaptor.capture(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(okResponse, HttpStatus.OK));

        // Call 1 -> key-one
        client.generateContent("Prompt 1");
        assertEquals("key-one", entityCaptor.getValue().getHeaders().getFirst("x-goog-api-key"));

        // Call 2 -> key-two
        client.generateContent("Prompt 2");
        assertEquals("key-two", entityCaptor.getValue().getHeaders().getFirst("x-goog-api-key"));

        // Call 3 -> key-three
        client.generateContent("Prompt 3");
        assertEquals("key-three", entityCaptor.getValue().getHeaders().getFirst("x-goog-api-key"));

        // Call 4 -> wraps around to key-one
        client.generateContent("Prompt 4");
        assertEquals("key-one", entityCaptor.getValue().getHeaders().getFirst("x-goog-api-key"));
    }

    @Test
    @DisplayName("Test whitespace and empty entries sanitization in comma-separated API keys")
    void testApiKeySanitization() {
        ReflectionTestUtils.setField(client, "apiKey", "  key-a , ,   key-b ,   , key-c  ");
        ReflectionTestUtils.invokeMethod(client, "refreshApiKeys");

        assertEquals(List.of("key-a", "key-b", "key-c"), client.getApiKeys());
    }

    @Test
    @DisplayName("Test empty or blank apiKey throws ApplicationException")
    void testEmptyApiKeyThrows() {
        ReflectionTestUtils.setField(client, "apiKey", "   ,  ,  ");
        ReflectionTestUtils.invokeMethod(client, "refreshApiKeys");

        assertThrows(ApplicationException.class, () -> client.generateContent("Test Prompt"));
    }

    @Test
    @DisplayName("Test concurrent multithreaded requests round-robin distribution")
    void testConcurrentRoundRobin() throws InterruptedException {
        ReflectionTestUtils.setField(client, "apiKey", "key-1, key-2, key-3");
        ReflectionTestUtils.invokeMethod(client, "refreshApiKeys");

        String okResponse = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"OK\"}]}}]}";
        java.util.List<String> usedKeys = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        Mockito.when(restTemplate.postForEntity(anyString(), any(org.springframework.http.HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    org.springframework.http.HttpEntity<?> entity = invocation.getArgument(1);
                    usedKeys.add(entity.getHeaders().getFirst("x-goog-api-key"));
                    return new ResponseEntity<>(okResponse, HttpStatus.OK);
                });

        int totalThreads = 12;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(totalThreads);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(totalThreads);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    client.generateContent("Test Prompt");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(totalThreads, usedKeys.size());
        long countKey1 = usedKeys.stream().filter("key-1"::equals).count();
        long countKey2 = usedKeys.stream().filter("key-2"::equals).count();
        long countKey3 = usedKeys.stream().filter("key-3"::equals).count();

        assertEquals(4, countKey1);
        assertEquals(4, countKey2);
        assertEquals(4, countKey3);
    }

    @Test
    @DisplayName("Test generateContent when getAllConfiguredModels returns empty")
    void testGenerateContentEmptyConfiguredModels() {
        GeminiApiClient spyClient = Mockito.spy(client);
        Mockito.doReturn(List.of()).when(spyClient).getAllConfiguredModels();

        assertThrows(ApplicationException.class, () -> spyClient.generateContent("prompt"));
    }

    @Test
    @DisplayName("Test getAllConfiguredModels deduplication, blank tokens, and fallback model duplicates")
    void testGetAllConfiguredModelsDeduplication() {
        ReflectionTestUtils.setField(client, "primaryModel", "  ");
        ReflectionTestUtils.setField(client, "backupModels", "modelA,  ,,modelA,modelB");
        ReflectionTestUtils.setField(client, "fallbackModel", "modelB");

        List<String> models = client.getAllConfiguredModels();
        assertEquals(2, models.size());
        assertEquals("modelA", models.get(0));
        assertEquals("modelB", models.get(1));

        // fallbackModel blank
        ReflectionTestUtils.setField(client, "fallbackModel", "   ");
        List<String> models2 = client.getAllConfiguredModels();
        assertEquals(2, models2.size());
    }

    @Test
    @DisplayName("Test callGeminiModel with SAFETY finishReason and empty or non-array candidates")
    void testCallGeminiModelCandidatesEdgeCases() {
        // 1. SAFETY finish reason (does not throw, returns text)
        String safetyResponse = "{\"candidates\": [{\"finishReason\": \"SAFETY\", \"content\": {\"parts\": [{\"text\": \"Safe output\"}]}}]}";
        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(safetyResponse, HttpStatus.OK));
        assertEquals("Safe output", client.generateContent("prompt"));

        // 2. Empty candidates array
        String emptyCandidates = "{\"candidates\": []}";
        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(emptyCandidates, HttpStatus.OK));
        assertThrows(ApplicationException.class, () -> client.generateContent("prompt"));

        // 3. Non-array candidates
        String nonArrayCandidates = "{\"candidates\": \"invalid\"}";
        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(nonArrayCandidates, HttpStatus.OK));
        assertThrows(ApplicationException.class, () -> client.generateContent("prompt"));
    }

    @Test
    @DisplayName("Test generateContentDetailed and generateContentWithSpecificModelDetailed with token usage branches")
    void testDetailedMethodsAndTokenUsage() throws Exception {
        // 1. generateContentDetailed with full usageMetadata
        String responseWithUsage = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\"parts\": [{\"text\": \"Response with tokens\"}]}\n" +
                "    }\n" +
                "  ],\n" +
                "  \"usageMetadata\": {\n" +
                "    \"promptTokenCount\": 120,\n" +
                "    \"candidatesTokenCount\": 45,\n" +
                "    \"totalTokenCount\": 165\n" +
                "  }\n" +
                "}";

        Mockito.when(restTemplate.postForEntity(contains("gemini-3.7-flash"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(responseWithUsage, HttpStatus.OK));

        com.portal.procucev.rfq.model.GeminiContentResponse resp = client.generateContentDetailed("Detailed prompt", null);
        assertNotNull(resp);
        assertEquals("Response with tokens", resp.getText());
        assertEquals("gemini-3.7-flash", resp.getModel());
        assertEquals(120, resp.getPromptTokens());
        assertEquals(45, resp.getCandidateTokens());
        assertEquals(165, resp.getTotalTokens());

        // 2. totalTokenCount is 0, but promptTokenCount > 0 -> totalTokens calculated as sum
        String responseWithZeroTotalPromptOnly = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\"parts\": [{\"text\": \"Response sum tokens prompt\"}]}\n" +
                "    }\n" +
                "  ],\n" +
                "  \"usageMetadata\": {\n" +
                "    \"promptTokenCount\": 80,\n" +
                "    \"candidatesTokenCount\": 0,\n" +
                "    \"totalTokenCount\": 0\n" +
                "  }\n" +
                "}";

        Mockito.when(restTemplate.postForEntity(contains("gemini-3.7-flash"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(responseWithZeroTotalPromptOnly, HttpStatus.OK));

        com.portal.procucev.rfq.model.GeminiContentResponse respSumPrompt = client.generateContentDetailed("Detailed prompt 2", List.of());
        assertEquals(80, respSumPrompt.getPromptTokens());
        assertEquals(0, respSumPrompt.getCandidateTokens());
        assertEquals(80, respSumPrompt.getTotalTokens());

        // 3. totalTokenCount is 0, candidateTokenCount > 0 -> totalTokens calculated as sum
        String responseWithZeroTotalCandidateOnly = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\"parts\": [{\"text\": \"Response sum tokens candidate\"}]}\n" +
                "    }\n" +
                "  ],\n" +
                "  \"usageMetadata\": {\n" +
                "    \"promptTokenCount\": 0,\n" +
                "    \"candidatesTokenCount\": 60,\n" +
                "    \"totalTokenCount\": 0\n" +
                "  }\n" +
                "}";

        Mockito.when(restTemplate.postForEntity(contains("custom-model"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(responseWithZeroTotalCandidateOnly, HttpStatus.OK));

        com.portal.procucev.rfq.model.GeminiContentResponse respSumCandidate =
                client.generateContentWithSpecificModelDetailed("custom-model", "Specific prompt", List.of(new InlineImage("a.png", "image/png", "base64")));
        assertEquals(0, respSumCandidate.getPromptTokens());
        assertEquals(60, respSumCandidate.getCandidateTokens());
        assertEquals(60, respSumCandidate.getTotalTokens());

        // 4. parts is not an array
        String nonArrayParts = "{\"candidates\": [{\"content\": {\"parts\": \"not-array\"}}]}";
        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(nonArrayParts, HttpStatus.OK));
        assertThrows(ApplicationException.class, () -> client.generateContentDetailed("prompt", null));
    }
}
