package com.portal.procucev.rfq.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RfqAiTokenUsageTest {

    @Test
    void testBuilderAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        RfqAiTokenUsage usage = RfqAiTokenUsage.builder()
                .id(10L)
                .rfqNumber("RFQ260109648263")
                .messageId("msg-abc-123")
                .modelName("gemini-2.5-flash")
                .promptTokens(1250)
                .candidateTokens(380)
                .totalTokens(1630)
                .attemptsCount(2)
                .estimatedCostUsd(0.000207)
                .createdAt(now)
                .build();

        assertEquals(10L, usage.getId());
        assertEquals("RFQ260109648263", usage.getRfqNumber());
        assertEquals("msg-abc-123", usage.getMessageId());
        assertEquals("gemini-2.5-flash", usage.getModelName());
        assertEquals(1250, usage.getPromptTokens());
        assertEquals(380, usage.getCandidateTokens());
        assertEquals(1630, usage.getTotalTokens());
        assertEquals(2, usage.getAttemptsCount());
        assertEquals(0.000207, usage.getEstimatedCostUsd());
        assertEquals(now, usage.getCreatedAt());
        assertNotNull(usage.toString());
    }

    @Test
    void testSettersAndNoArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        RfqAiTokenUsage usage = new RfqAiTokenUsage();
        usage.setId(1L);
        usage.setRfqNumber("RFQ-100");
        usage.setMessageId("msg-1");
        usage.setModelName("gemini-1.5-pro");
        usage.setPromptTokens(500);
        usage.setCandidateTokens(200);
        usage.setTotalTokens(700);
        usage.setAttemptsCount(1);
        usage.setEstimatedCostUsd(0.0001);
        usage.setCreatedAt(now);

        assertEquals(1L, usage.getId());
        assertEquals("RFQ-100", usage.getRfqNumber());
        assertEquals("msg-1", usage.getMessageId());
        assertEquals("gemini-1.5-pro", usage.getModelName());
        assertEquals(500, usage.getPromptTokens());
        assertEquals(200, usage.getCandidateTokens());
        assertEquals(700, usage.getTotalTokens());
        assertEquals(1, usage.getAttemptsCount());
        assertEquals(0.0001, usage.getEstimatedCostUsd());
        assertEquals(now, usage.getCreatedAt());
    }

    @Test
    void testAllArgsConstructorAndEqualsHashCode() {
        LocalDateTime now = LocalDateTime.now();
        RfqAiTokenUsage usage1 = new RfqAiTokenUsage(1L, "RFQ-1", "msg-1", "gemini-flash", 100, 50, 150, 1, 0.00005, now);
        RfqAiTokenUsage usage2 = new RfqAiTokenUsage(1L, "RFQ-1", "msg-1", "gemini-flash", 100, 50, 150, 1, 0.00005, now);
        RfqAiTokenUsage usage3 = new RfqAiTokenUsage(2L, "RFQ-2", "msg-2", "gemini-pro", 200, 100, 300, 2, 0.00010, now);

        assertEquals(usage1, usage2);
        assertEquals(usage1.hashCode(), usage2.hashCode());
        assertNotEquals(usage1, usage3);
        assertNotEquals(usage1, null);
        assertNotEquals(usage1, "string");
        assertTrue(usage1.canEqual(usage2));
    }

    @Test
    void testPrePersist_WhenCreatedAtIsNull_SetsCreatedAt() {
        RfqAiTokenUsage usage = new RfqAiTokenUsage();
        assertNull(usage.getCreatedAt());

        usage.prePersist();

        assertNotNull(usage.getCreatedAt());
    }

    @Test
    void testPrePersist_WhenCreatedAtIsNotNull_PreservesCreatedAt() {
        LocalDateTime past = LocalDateTime.of(2025, 1, 1, 10, 0);
        RfqAiTokenUsage usage = new RfqAiTokenUsage();
        usage.setCreatedAt(past);

        usage.prePersist();

        assertEquals(past, usage.getCreatedAt());
    }
}
