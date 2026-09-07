package com.portal.procucev.rfq.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeminiContentResponseTest {

    @Test
    void testBuilderAndGetters() {
        GeminiContentResponse response = GeminiContentResponse.builder()
                .text("Extracted RFQ JSON")
                .model("gemini-2.5-flash")
                .promptTokens(1500)
                .candidateTokens(400)
                .totalTokens(1900)
                .build();

        assertEquals("Extracted RFQ JSON", response.getText());
        assertEquals("gemini-2.5-flash", response.getModel());
        assertEquals(1500, response.getPromptTokens());
        assertEquals(400, response.getCandidateTokens());
        assertEquals(1900, response.getTotalTokens());
        assertEquals("gemini-2.5-flash", response.getModelName());
        assertNotNull(response.toString());
    }

    @Test
    void testSettersAndNoArgsConstructor() {
        GeminiContentResponse response = new GeminiContentResponse();
        response.setText("Sample response");
        response.setModel("gemini-1.5-pro");
        response.setPromptTokens(300);
        response.setCandidateTokens(100);
        response.setTotalTokens(400);

        assertEquals("Sample response", response.getText());
        assertEquals("gemini-1.5-pro", response.getModel());
        assertEquals(300, response.getPromptTokens());
        assertEquals(100, response.getCandidateTokens());
        assertEquals(400, response.getTotalTokens());
        assertEquals("gemini-1.5-pro", response.getModelName());
    }

    @Test
    void testGetModelName_WhenModelIsNull_ReturnsDefaultGeminiFlash() {
        GeminiContentResponse response = new GeminiContentResponse();
        response.setModel(null);

        assertEquals("gemini-flash", response.getModelName());
    }

    @Test
    void testAllArgsConstructorAndEqualsHashCode() {
        GeminiContentResponse resp1 = new GeminiContentResponse("text", "model-a", 10, 20, 30);
        GeminiContentResponse resp2 = new GeminiContentResponse("text", "model-a", 10, 20, 30);
        GeminiContentResponse resp3 = new GeminiContentResponse("other", "model-b", 5, 10, 15);

        assertEquals(resp1, resp2);
        assertEquals(resp1.hashCode(), resp2.hashCode());
        assertNotEquals(resp1, resp3);
        assertNotEquals(resp1, null);
        assertNotEquals(resp1, "some-string");
        assertTrue(resp1.canEqual(resp2));
    }
}
