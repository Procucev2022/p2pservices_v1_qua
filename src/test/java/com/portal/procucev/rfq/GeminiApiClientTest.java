package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.client.GeminiApiClient;
import com.portal.procucev.rfq.exception.ApplicationException;
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
    }

    @Test
    @DisplayName("Test generateContent primary model success")
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
}
