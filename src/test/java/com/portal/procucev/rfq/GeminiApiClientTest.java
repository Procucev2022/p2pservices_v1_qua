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
        ReflectionTestUtils.setField(client, "primaryModel", "gemini-3.5-flash-lite");
        ReflectionTestUtils.setField(client, "fallbackModel", "gemini-3.1-flash-lite");
        ReflectionTestUtils.setField(client, "baseUrl", "https://generativelanguage.googleapis.com/v1beta/models");
        ReflectionTestUtils.setField(client, "apiKey", "test-key");

        ReflectionTestUtils.invokeMethod(client, "init");
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

        Mockito.when(restTemplate.postForEntity(contains("gemini-3.5-flash-lite"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponseBody, HttpStatus.OK));

        String result = client.generateContent("Test Prompt");

        assertNotNull(result);
        assertTrue(result.contains("buyer@test.com"));
    }

    @Test
    @DisplayName("Test generateContent primary fails, fallback model succeeds")
    void testGenerateContentPrimaryFailsFallbackSuccess() {
        Mockito.when(restTemplate.postForEntity(contains("gemini-3.5-flash-lite"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Primary Timeout"));

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

        Mockito.when(restTemplate.postForEntity(contains("gemini-3.1-flash-lite"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(fallbackResponseBody, HttpStatus.OK));

        String result = client.generateContent("Test Prompt");

        assertEquals("Fallback Success Text", result);
    }

    @Test
    @DisplayName("Test generateContent primary and fallback both fail")
    void testGenerateContentBothFail() {
        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API Outage"));

        assertThrows(ApplicationException.class, () -> client.generateContent("Test Prompt"));
    }
}
