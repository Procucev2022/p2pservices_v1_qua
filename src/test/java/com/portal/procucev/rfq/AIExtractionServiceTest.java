package com.portal.procucev.rfq;

import com.portal.procucev.rfq.client.GeminiApiClient;
import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.service.AIExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

public class AIExtractionServiceTest {

    private GeminiApiClient geminiApiClient;
    private AIExtractionService aiExtractionService;

    @BeforeEach
    void setUp() {
        geminiApiClient = Mockito.mock(GeminiApiClient.class);
        aiExtractionService = new AIExtractionService(geminiApiClient);
    }

    @Test
    @DisplayName("Test extractRFQFromEmail with markdown JSON response")
    void testExtractRFQFromEmailSuccess() {
        String jsonResponse = "```json\n" +
                "{\n" +
                "  \"buyerEmail\": \"buyer@test.com\",\n" +
                "  \"deliveryLocation\": \"Bangalore\",\n" +
                "  \"deliveryDate\": \"2026-08-25\",\n" +
                "  \"items\": [\n" +
                "    {\"itemDescription\": \"Dell Laptop\", \"quantity\": 10.0, \"uom\": \"NOS\"}\n" +
                "  ]\n" +
                "}\n" +
                "```";

        Mockito.when(geminiApiClient.generateContent(anyString(), anyList())).thenReturn(jsonResponse);

        EmailData email = EmailData.builder()
                .subject("Need Laptops")
                .body("Please quote 10 laptops")
                .senderEmail("sender@test.com")
                .attachmentText("Attached doc text")
                .build();

        ExtractedRFQ rfq = aiExtractionService.extractRFQFromEmail(email);

        assertNotNull(rfq);
        assertEquals("buyer@test.com", rfq.getBuyerEmail());
        assertEquals("Bangalore", rfq.getDeliveryLocation());
        assertEquals(1, rfq.getItems().size());
        assertEquals("Dell Laptop", rfq.getItems().get(0).getItemDescription());
    }

    @Test
    @DisplayName("Test extractRFQFromEmail when JSON missing buyer email (fallback to sender)")
    void testExtractRFQFromEmailMissingBuyerEmail() {
        String jsonResponse = "```\n" +
                "{\n" +
                "  \"deliveryLocation\": \"Bangalore\",\n" +
                "  \"items\": [\n" +
                "    {\"itemDescription\": \"Monitor\", \"quantity\": 5.0, \"uom\": \"NOS\"}\n" +
                "  ]\n" +
                "}\n" +
                "```";

        Mockito.when(geminiApiClient.generateContent(anyString(), anyList())).thenReturn(jsonResponse);

        EmailData email = EmailData.builder()
                .subject("Monitor requirement")
                .body("Body text")
                .senderEmail("fallbacksender@test.com")
                .build();

        ExtractedRFQ rfq = aiExtractionService.extractRFQFromEmail(email);

        assertNotNull(rfq);
        assertEquals("fallbacksender@test.com", rfq.getBuyerEmail());
    }

    @Test
    @DisplayName("Test extractRFQFromEmail when Gemini API throws exception")
    void testExtractRFQFromEmailException() {
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList())).thenThrow(new RuntimeException("API error"));

        EmailData email = EmailData.builder()
                .subject("Error test")
                .senderEmail("sender@test.com")
                .build();

        assertThrows(ApplicationException.class, () -> aiExtractionService.extractRFQFromEmail(email));
    }

    @Test
    @DisplayName("Test extractRFQFromEmail with raw JSON response without code block markdown")
    void testExtractRFQFromEmailRawJson() {
        String jsonResponse = "{\"buyerEmail\":\"buyer@test.com\",\"items\":[]}";
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList())).thenReturn(jsonResponse);

        EmailData email = EmailData.builder()
                .senderEmail("sender@test.com")
                .build();

        ExtractedRFQ rfq = aiExtractionService.extractRFQFromEmail(email);
        assertNotNull(rfq);
        assertEquals("buyer@test.com", rfq.getBuyerEmail());
    }

    @Test
    @DisplayName("Test extractRFQFromEmail when Gemini returns null or empty response")
    void testExtractRFQFromEmailNullResponse() {
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList())).thenReturn(null);

        EmailData email = EmailData.builder().senderEmail("sender@test.com").build();
        assertThrows(ApplicationException.class, () -> aiExtractionService.extractRFQFromEmail(email));
    }
}
