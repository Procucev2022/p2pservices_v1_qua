package com.portal.procucev.rfq;

import com.portal.procucev.rfq.client.GeminiApiClient;
import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.service.AIExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Cover for the targeted re-extraction retry.
 *
 * <p>The model is probabilistic, so one answer omitting a quantity is not evidence the buyer omitted
 * it: the same "Desktop Computer" email failed on 17-Aug and succeeded on 18-Aug with no code change
 * in between. Extraction is therefore re-asked, naming the exact gap, before a detail is declared
 * genuinely absent and the RFQ is rejected.
 */
public class ExtractionRetryTest {

    private GeminiApiClient geminiApiClient;
    private AIExtractionService service;

    @BeforeEach
    void setUp() {
        geminiApiClient = Mockito.mock(GeminiApiClient.class);
        service = new AIExtractionService(geminiApiClient);
    }

    private static EmailData email() {
        return EmailData.builder()
                .messageId("<retry-test@mail.gmail.com>")
                .subject("Requirement of MS Hex Bolts M10")
                .senderEmail("govardhan.kilari@procucev.com")
                .body("We require MS Hex Bolts M10 x 50 mm - 500 Nos, Plain Washers M10 - 1,000 Nos")
                .build();
    }

    /** Two items, quantities supplied per argument so a partial answer can be simulated. */
    private static String twoItems(String firstQty, String secondQty) {
        return "{\"buyerEmail\":\"govardhan.kilari@procucev.com\",\"items\":["
                + "{\"itemDescription\":\"MS Hex Bolts M10 x 50 mm\",\"uom\":\"Nos\",\"quantity\":" + firstQty + "},"
                + "{\"itemDescription\":\"Plain Washers M10\",\"uom\":\"Nos\",\"quantity\":" + secondQty + "}"
                + "]}";
    }

    private List<String> capturePrompts(int times) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(geminiApiClient, Mockito.times(times)).generateContent(captor.capture(), anyList());
        return captor.getAllValues();
    }

    @Test
    @DisplayName("A complete first answer is accepted without spending another call")
    void completeFirstAnswerMakesNoExtraCall() {
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn(twoItems("500", "1000"));

        ExtractedRFQ result = service.extractRFQFromEmail(email());

        assertEquals(500.0, result.getItems().get(0).getQuantity());
        assertEquals(1000.0, result.getItems().get(1).getQuantity());
        Mockito.verify(geminiApiClient, Mockito.times(1)).generateContent(anyString(), anyList());
    }

    @Test
    @DisplayName("A quantity the model dropped on the first answer is recovered on the second")
    void missingQuantityIsRecoveredOnRetry() {
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn(twoItems("null", "null"))
                .thenReturn(twoItems("500", "1000"));

        ExtractedRFQ result = service.extractRFQFromEmail(email());

        assertEquals(500.0, result.getItems().get(0).getQuantity());
        assertEquals(1000.0, result.getItems().get(1).getQuantity());
        Mockito.verify(geminiApiClient, Mockito.times(2)).generateContent(anyString(), anyList());
    }

    @Test
    @DisplayName("Values found across different attempts are combined rather than the last answer winning")
    void valuesFromSeparateAttemptsAreCombined() {
        // Attempt 1 reads only the first quantity, attempt 2 only the second. Neither answer alone
        // is usable; together they are complete.
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn(twoItems("500", "null"))
                .thenReturn(twoItems("null", "1000"));

        ExtractedRFQ result = service.extractRFQFromEmail(email());

        assertEquals(500.0, result.getItems().get(0).getQuantity(), "kept from attempt 1");
        assertEquals(1000.0, result.getItems().get(1).getQuantity(), "recovered on attempt 2");
    }

    @Test
    @DisplayName("The retry names the exact missing detail instead of re-asking blindly")
    void retryPromptNamesTheGap() {
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn(twoItems("500", "null"))
                .thenReturn(twoItems("500", "1000"));

        service.extractRFQFromEmail(email());

        List<String> prompts = capturePrompts(2);
        String retryPrompt = prompts.get(1);
        assertTrue(retryPrompt.contains("YOUR PREVIOUS ANSWER TO THIS EMAIL WAS INCOMPLETE"));
        assertTrue(retryPrompt.contains("Plain Washers M10"), "the unresolved item must be named");
        assertTrue(retryPrompt.contains("quantity"), "the unresolved field must be named");
        assertTrue(!retryPrompt.contains("item 1 (MS Hex Bolts M10 x 50 mm): quantity"),
                "an item that was already resolved must not be re-asked");
    }

    @Test
    @DisplayName("A detail genuinely absent from the email is still absent after every attempt")
    void genuinelyAbsentDetailStaysAbsent() {
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn(twoItems("500", "null"));

        ExtractedRFQ result = service.extractRFQFromEmail(email());

        assertEquals(500.0, result.getItems().get(0).getQuantity());
        assertNull(result.getItems().get(1).getQuantity(), "the buyer really did not state this one");
        // Exhausts the configured budget rather than giving up after one answer.
        Mockito.verify(geminiApiClient, Mockito.times(3)).generateContent(anyString(), anyList());
    }

    @Test
    @DisplayName("An answer with more line items replaces one with fewer")
    void answerWithMoreLineItemsWins() {
        // Attempt 1 sees only one of the two items, and without a quantity so a retry is triggered.
        // A dropped line item is silent data loss, so the fuller answer must win outright.
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn("{\"items\":[{\"itemDescription\":\"MS Hex Bolts M10 x 50 mm\",\"uom\":\"Nos\"}]}")
                .thenReturn(twoItems("500", "1000"));

        ExtractedRFQ result = service.extractRFQFromEmail(email());

        assertEquals(2, result.getItems().size(), "the answer that found both line items must win");
        assertEquals(500.0, result.getItems().get(0).getQuantity());
        assertEquals(1000.0, result.getItems().get(1).getQuantity());
    }

    @Test
    @DisplayName("Quantities are never merged between two different products")
    void quantitiesAreNotMergedAcrossDifferentItems() {
        // Attempt 2 returns a completely different product in slot 2. Its 1,000 must not be applied
        // to Plain Washers, which is the class of error that turned a Laptop order into bolts.
        String differentSecondItem = "{\"items\":["
                + "{\"itemDescription\":\"MS Hex Bolts M10 x 50 mm\",\"uom\":\"Nos\",\"quantity\":500},"
                + "{\"itemDescription\":\"Spring Washers M10\",\"uom\":\"Nos\",\"quantity\":1000}"
                + "]}";
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn(twoItems("500", "null"))
                .thenReturn(differentSecondItem);

        ExtractedRFQ result = service.extractRFQFromEmail(email());

        assertEquals("Plain Washers M10", result.getItems().get(1).getItemDescription());
        assertNull(result.getItems().get(1).getQuantity(),
                "a quantity belonging to Spring Washers must not land on Plain Washers");
    }

    @Test
    @DisplayName("A transient model failure is retried, and a persistent one is reported")
    void modelFailuresAreRetriedThenReported() {
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenThrow(new RuntimeException("503 upstream unavailable"))
                .thenReturn(twoItems("500", "1000"));

        ExtractedRFQ recovered = service.extractRFQFromEmail(email());
        assertNotNull(recovered);
        assertEquals(500.0, recovered.getItems().get(0).getQuantity());

        Mockito.reset(geminiApiClient);
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenThrow(new RuntimeException("503 upstream unavailable"));

        assertThrows(ApplicationException.class, () -> service.extractRFQFromEmail(email()));
        Mockito.verify(geminiApiClient, Mockito.atLeast(3)).generateContent(anyString(), anyList());
    }

    @Test
    @DisplayName("A malformed answer is re-asked with a strict-JSON instruction")
    void malformedAnswerTriggersStrictJsonInstruction() {
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn("I am unable to help with that request.")
                .thenReturn(twoItems("500", "1000"));

        ExtractedRFQ result = service.extractRFQFromEmail(email());
        assertEquals(2, result.getItems().size());

        String retryPrompt = capturePrompts(2).get(1);
        assertTrue(retryPrompt.contains("Return STRICT JSON ONLY"));
    }

    @Test
    @DisplayName("The attempt budget is configurable and honoured")
    void attemptBudgetIsConfigurable() {
        ReflectionTestUtils.setField(service, "minModelsForMissingQuantity", 1);
        ReflectionTestUtils.setField(service, "extractionMaxAttempts", 1);
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn(twoItems("500", "null"));

        service.extractRFQFromEmail(email());

        Mockito.verify(geminiApiClient, Mockito.times(1)).generateContent(anyString(), anyList());
    }

    @Test
    @DisplayName("An answer with no line items at all is re-asked")
    void emptyItemListIsReAsked() {
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn("{\"buyerEmail\":\"b@test.com\",\"items\":[]}")
                .thenReturn(twoItems("500", "1000"));

        ExtractedRFQ result = service.extractRFQFromEmail(email());

        assertEquals(2, result.getItems().size(), "an empty answer must not end the extraction");
    }

    @Test
    @DisplayName("Downtime and 503 failures are not counted as models reporting data missing")
    void downtimeDoesNotCountAsMissingDataModel() {
        // Attempt 1: Model 1 reports missing quantity (1 successful model)
        // Attempt 2: Model 2 throws 503 downtime (0 successful model contribution)
        // Attempt 3: Model 3 reports missing quantity (2 successful models)
        // Attempt 4: Model 4 recovers quantity -> success!
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn(twoItems("500", "null"))
                .thenThrow(new RuntimeException("503 Service Unavailable"))
                .thenReturn(twoItems("500", "null"))
                .thenReturn(twoItems("500", "1000"));

        ExtractedRFQ result = service.extractRFQFromEmail(email());
        assertNotNull(result);
        assertEquals(1000.0, result.getItems().get(1).getQuantity());
        Mockito.verify(geminiApiClient, Mockito.times(4)).generateContent(anyString(), anyList());
    }

    @Test
    @DisplayName("Fuzzy item descriptions with typos or spec suffix are matched and merged")
    void similarItemDescriptionsAreMerged() {
        String attempt1 = "{\"buyerEmail\":\"b@test.com\",\"items\":["
                + "{\"itemDescription\":\"Speed Btreaker 2 (DG Point)\",\"uom\":\"RFT\",\"quantity\":null},"
                + "{\"itemDescription\":\"Compound Wall Rework (50 Sq Ft) - Solid Blocks\",\"uom\":\"Sq Ft\",\"quantity\":null}"
                + "]}";
        String attempt2 = "{\"buyerEmail\":\"b@test.com\",\"items\":["
                + "{\"itemDescription\":\"Speed Breaker 2 (DG Point)\",\"uom\":\"RFT\",\"quantity\":22},"
                + "{\"itemDescription\":\"Compound Wall Rework - Solid Blocks\",\"uom\":\"Sq Ft\",\"quantity\":50}"
                + "]}";

        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn(attempt1)
                .thenReturn(attempt2);

        ExtractedRFQ result = service.extractRFQFromEmail(email());
        assertEquals(2, result.getItems().size());
        assertEquals(22.0, result.getItems().get(0).getQuantity(), "recovered despite 'Btreaker' vs 'Breaker' typo");
        assertEquals(50.0, result.getItems().get(1).getQuantity(), "recovered despite '(50 Sq Ft)' spec suffix");
    }
}
