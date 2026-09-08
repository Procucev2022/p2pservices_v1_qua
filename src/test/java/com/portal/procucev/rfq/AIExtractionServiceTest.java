package com.portal.procucev.rfq;

import com.portal.procucev.rfq.client.GeminiApiClient;
import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.GeminiContentResponse;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.service.AIExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

public class AIExtractionServiceTest {

    private GeminiApiClient geminiApiClient;
    private AIExtractionService aiExtractionService;

    @TempDir
    Path tempDir;

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

        Mockito.when(geminiApiClient.getAllConfiguredModels()).thenReturn(List.of("gemini-3.7-flash"));
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
    @DisplayName("Test multi-attempt gap recovery and merging when first model misses quantity")
    void testMultiAttemptGapRecovery() {
        String respModel1 = "{\n" +
                "  \"buyerEmail\": \"buyer@test.com\",\n" +
                "  \"items\": [\n" +
                "    {\"itemDescription\": \"Plain Washers M10\", \"quantity\": null, \"uom\": \"Nos\"}\n" +
                "  ]\n" +
                "}";

        String respModel2 = "{\n" +
                "  \"buyerEmail\": \"buyer@test.com\",\n" +
                "  \"deliveryLocation\": \"Hyderabad\",\n" +
                "  \"items\": [\n" +
                "    {\"itemDescription\": \"Plain Washers M10\", \"quantity\": 1000.0, \"uom\": \"Nos\"}\n" +
                "  ]\n" +
                "}";

        Mockito.when(geminiApiClient.getAllConfiguredModels()).thenReturn(List.of("gemini-3.7-flash", "gemini-3.6-flash"));
        Mockito.when(geminiApiClient.generateContent(contains("attempt"), anyList())).thenReturn(respModel2);
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList())).thenReturn(respModel1, respModel2);

        EmailData email = EmailData.builder()
                .subject("Requirement for Washers")
                .body("Plain Washers M10 - 1000 Nos")
                .senderEmail("buyer@test.com")
                .build();

        ExtractedRFQ rfq = aiExtractionService.extractRFQFromEmail(email);
        assertNotNull(rfq);
        assertEquals(1, rfq.getItems().size());
        assertEquals(1000.0, rfq.getItems().get(0).getQuantity());
        assertEquals("Hyderabad", rfq.getDeliveryLocation());
    }

    @Test
    @DisplayName("Test merging when candidate has more or fewer items")
    void testMergingItemCounts() {
        String resp1 = "{\n" +
                "  \"buyerEmail\": \"buyer@test.com\",\n" +
                "  \"items\": [\n" +
                "    {\"itemDescription\": \"Item 1\", \"quantity\": 10.0},\n" +
                "    {\"itemDescription\": \"Item 2\", \"quantity\": null}\n" +
                "  ]\n" +
                "}";

        String resp2More = "{\n" +
                "  \"buyerEmail\": \"buyer@test.com\",\n" +
                "  \"deliveryLocation\": \"Pune\",\n" +
                "  \"items\": [\n" +
                "    {\"itemDescription\": \"Item 1\", \"quantity\": 10.0},\n" +
                "    {\"itemDescription\": \"Item 2\", \"quantity\": 20.0},\n" +
                "    {\"itemDescription\": \"Item 3\", \"quantity\": 30.0}\n" +
                "  ]\n" +
                "}";

        Mockito.when(geminiApiClient.getAllConfiguredModels()).thenReturn(List.of("model-a", "model-b"));
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList())).thenReturn(resp1, resp2More);

        EmailData email = EmailData.builder().senderEmail("buyer@test.com").build();
        ExtractedRFQ rfq = aiExtractionService.extractRFQFromEmail(email);

        assertEquals(3, rfq.getItems().size());
        assertEquals("Pune", rfq.getDeliveryLocation());
    }

    @Test
    @DisplayName("Test fuzzy item comparison methods")
    void testFuzzyComparison() {
        assertTrue(aiExtractionService.isSameOrSimilarItem("Speed Breaker", "Speed Breaker"));
        assertTrue(aiExtractionService.isSameOrSimilarItem("Speed Breaker 500mm", "Speed Breaker"));
        assertTrue(aiExtractionService.isSameOrSimilarItem("Speed Breaker", "Speed Btreaker")); // typo
        assertTrue(aiExtractionService.isSameOrSimilarItem("Dell Latitude 5420 Laptop", "Dell Latitude 5420 Laptop i7"));
        assertFalse(aiExtractionService.isSameOrSimilarItem("Speed Breaker", "Hex Bolt"));
        assertFalse(aiExtractionService.isSameOrSimilarItem("", "Hex Bolt"));
        assertFalse(aiExtractionService.isSameOrSimilarItem(null, "Hex Bolt"));
    }

    @Test
    @DisplayName("Test image attachments collection with limits")
    void testImageAttachmentsCollection() throws Exception {
        File imgFile = new File(tempDir.toFile(), "spec.png");
        try (FileOutputStream fos = new FileOutputStream(imgFile)) {
            fos.write(new byte[]{1, 2, 3, 4});
        }

        File hugeImg = new File(tempDir.toFile(), "huge.jpg");
        try (FileOutputStream fos = new FileOutputStream(hugeImg)) {
            fos.write(new byte[1000]);
        }

        EmailData email = EmailData.builder()
                .subject("Need image items")
                .attachments(List.of(imgFile, hugeImg))
                .senderEmail("buyer@test.com")
                .build();

        String jsonResponse = "{\"buyerEmail\":\"buyer@test.com\",\"items\":[{\"itemDescription\":\"Part A\",\"quantity\":5.0}]}";
        Mockito.when(geminiApiClient.getAllConfiguredModels()).thenReturn(List.of("gemini-3.7-flash"));
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList())).thenReturn(jsonResponse);

        ExtractedRFQ rfq = aiExtractionService.extractRFQFromEmail(email);
        assertNotNull(rfq);
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
    @DisplayName("Test sanitizeJsonOutput")
    void testSanitizeJsonOutput() {
        assertEquals("{}", aiExtractionService.sanitizeJsonOutput(null));
        assertEquals("{\"key\":\"value\"}", aiExtractionService.sanitizeJsonOutput("Here is json: {\"key\":\"value\"} Thanks"));
        assertEquals("{\"key\":\"value\"}", aiExtractionService.sanitizeJsonOutput("```json\n{\"key\":\"value\"}\n```"));
        assertEquals("{\"key\":\"value\"}", aiExtractionService.sanitizeJsonOutput("```\n{\"key\":\"value\"}\n```"));
    }

    @Test
    @DisplayName("Test buildCorrectiveInstruction with various gaps")
    void testBuildCorrectiveInstruction() {
        List<String> gaps = List.of(
                "Missing quantities for 2 items",
                "Delivery location was not found",
                "Delivery date was not found",
                "No line items extracted"
        );

        String instruction = ReflectionTestUtils.invokeMethod(aiExtractionService, "buildCorrectiveInstruction", gaps);
        assertNotNull(instruction);
        assertTrue(instruction.contains("Missing quantities for 2 items"));
        assertTrue(instruction.contains("Delivery location was not found"));
    }

    @Test
    @DisplayName("Test collectInlineImages when non-image files and limit caps are reached")
    void testCollectInlineImagesLimits() throws Exception {
        ReflectionTestUtils.setField(aiExtractionService, "maxInlineImages", 1);
        ReflectionTestUtils.setField(aiExtractionService, "maxInlineImageBytes", 100L);
        ReflectionTestUtils.setField(aiExtractionService, "maxInlineImageTotalBytes", 150L);

        File nonImage = new File(tempDir.toFile(), "document.pdf");
        try (FileOutputStream fos = new FileOutputStream(nonImage)) {
            fos.write("PDF bytes".getBytes());
        }

        File img1 = new File(tempDir.toFile(), "valid.png");
        try (FileOutputStream fos = new FileOutputStream(img1)) {
            fos.write(new byte[]{1, 2, 3, 4});
        }

        File img2 = new File(tempDir.toFile(), "second.png");
        try (FileOutputStream fos = new FileOutputStream(img2)) {
            fos.write(new byte[]{5, 6, 7, 8});
        }

        EmailData email = EmailData.builder()
                .attachments(List.of(nonImage, img1, img2))
                .build();

        List<?> images = ReflectionTestUtils.invokeMethod(aiExtractionService, "collectInlineImages", email);
        assertNotNull(images);
        assertEquals(1, images.size());
    }

    @Test
    @DisplayName("Test mergeMissingItemFields and fillMissingTopLevel all fields")
    void testMergeMissingItemFieldsAndTopLevel() {
        RFQItem target = RFQItem.builder()
                .itemDescription("Hex Bolt")
                .quantity(null)
                .uom(null)
                .specification(null)
                .brand(null)
                .partCode(null)
                .deliveryLocation(null)
                .deliveryDate(null)
                .build();

        RFQItem source = RFQItem.builder()
                .itemDescription("Hex Bolt M10")
                .quantity(100.0)
                .uom("Nos")
                .specification("Grade 8.8")
                .brand("Unbrako")
                .partCode("HB-10")
                .deliveryLocation("Pune")
                .deliveryDate("2026-09-01")
                .build();

        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", target, source, 2);

        assertEquals(100.0, target.getQuantity());
        assertEquals("Nos", target.getUom());
        assertEquals("Grade 8.8", target.getSpecification());
        assertEquals("Unbrako", target.getBrand());
        assertEquals("HB-10", target.getPartCode());
        assertEquals("Pune", target.getDeliveryLocation());
        assertEquals("2026-09-01", target.getDeliveryDate());

        // Target with blank item description receives source item description
        RFQItem blankDescTarget = new RFQItem();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", blankDescTarget, source, 2);
        assertEquals("Hex Bolt M10", blankDescTarget.getItemDescription());

        // Target with non-matching item description does not merge
        RFQItem nonMatchingTarget = RFQItem.builder().itemDescription("Completely Unrelated Item XYZ").build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", nonMatchingTarget, source, 2);
        assertNull(nonMatchingTarget.getQuantity());

        // fillMissingTopLevel
        ExtractedRFQ topTarget = new ExtractedRFQ();
        ExtractedRFQ topSource = ExtractedRFQ.builder()
                .deliveryLocation("Bengaluru")
                .deliveryCity("Bangalore")
                .deliveryState("Karnataka")
                .deliveryPincode("560001")
                .deliveryDate("2026-09-15")
                .category("Hardware")
                .buyerEmail("buyer@corp.com")
                .build();

        ReflectionTestUtils.invokeMethod(aiExtractionService, "fillMissingTopLevel", topTarget, topSource);
        assertEquals("Bengaluru", topTarget.getDeliveryLocation());
        assertEquals("Bangalore", topTarget.getDeliveryCity());
        assertEquals("Karnataka", topTarget.getDeliveryState());
        assertEquals("560001", topTarget.getDeliveryPincode());
        assertEquals("2026-09-15", topTarget.getDeliveryDate());
        assertEquals("Hardware", topTarget.getCategory());
        assertEquals("buyer@corp.com", topTarget.getBuyerEmail());
    }

    @Test
    @DisplayName("Test mergeBetterResult when candidate has fewer items or equal items with rearranged positions")
    void testMergeBetterResultEqualAndFewer() {
        RFQItem item1 = RFQItem.builder().itemDescription("Pump").quantity(null).build();
        RFQItem item2 = RFQItem.builder().itemDescription("Motor").quantity(null).build();

        ExtractedRFQ best = ExtractedRFQ.builder().items(List.of(item1, item2)).build();

        // 1. Candidate with fewer items
        RFQItem candItem1 = RFQItem.builder().itemDescription("Pump").quantity(5.0).build();
        ExtractedRFQ candFewer = ExtractedRFQ.builder().items(List.of(candItem1)).build();
        ExtractedRFQ merged1 = ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeBetterResult", best, candFewer, 2);
        assertEquals(5.0, merged1.getItems().get(0).getQuantity());

        // 2. Candidate with equal items rearranged (Motor at 0, Pump at 1)
        RFQItem itemA = RFQItem.builder().itemDescription("Pump").quantity(null).build();
        RFQItem itemB = RFQItem.builder().itemDescription("Motor").quantity(null).build();
        ExtractedRFQ best2 = ExtractedRFQ.builder().items(List.of(itemA, itemB)).build();

        RFQItem candRearranged1 = RFQItem.builder().itemDescription("Motor").quantity(20.0).build();
        RFQItem candRearranged2 = RFQItem.builder().itemDescription("Pump").quantity(15.0).build();
        ExtractedRFQ candRearranged = ExtractedRFQ.builder().items(List.of(candRearranged1, candRearranged2)).build();
        ExtractedRFQ merged2 = ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeBetterResult", best2, candRearranged, 2);
        assertEquals(15.0, merged2.getItems().get(0).getQuantity());
        assertEquals(20.0, merged2.getItems().get(1).getQuantity());
    }

    @Test
    @DisplayName("Test describeGaps, logRawModelResponse, abbreviate")
    void testLoggingAndGaps() throws Exception {
        ExtractedRFQ rfqGaps = ExtractedRFQ.builder()
                .items(List.of(
                        RFQItem.builder().itemDescription("").quantity(null).build(),
                        RFQItem.builder().itemDescription("Item 2").quantity(0.0).build()
                ))
                .build();

        List<String> gaps = ReflectionTestUtils.invokeMethod(aiExtractionService, "describeGaps", rfqGaps);
        assertNotNull(gaps);
        assertFalse(gaps.isEmpty());

        // abbreviate
        assertEquals("", ReflectionTestUtils.invokeMethod(aiExtractionService, "abbreviate", (String) null));
        assertEquals("short", ReflectionTestUtils.invokeMethod(aiExtractionService, "abbreviate", "short"));
        String longText = "a".repeat(4500);
        String abbr = ReflectionTestUtils.invokeMethod(aiExtractionService, "abbreviate", longText);
        assertTrue(abbr.endsWith("...[truncated]"));

        // logRawModelResponse
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(aiExtractionService, "logRawModelResponse", 1, (String) null));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(aiExtractionService, "logRawModelResponse", 1, "short text"));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(aiExtractionService, "logRawModelResponse", 1, "x".repeat(4500)));

        // parseAndValidateJson
        assertThrows(Exception.class, () -> ReflectionTestUtils.invokeMethod(aiExtractionService, "parseAndValidateJson", (String) null, "fallback@test.com"));
        assertThrows(Exception.class, () -> ReflectionTestUtils.invokeMethod(aiExtractionService, "parseAndValidateJson", "   ", "fallback@test.com"));

        String jsonWithEmail = "{\"buyerEmail\": \"buyer@test.com\", \"items\": []}";
        ExtractedRFQ parsed1 = ReflectionTestUtils.invokeMethod(aiExtractionService, "parseAndValidateJson", jsonWithEmail, "fallback@test.com");
        assertEquals("buyer@test.com", parsed1.getBuyerEmail());

        String jsonWithoutEmail = "{\"buyerEmail\": \"\", \"items\": []}";
        ExtractedRFQ parsed2 = ReflectionTestUtils.invokeMethod(aiExtractionService, "parseAndValidateJson", jsonWithoutEmail, "fallback@test.com");
        assertEquals("fallback@test.com", parsed2.getBuyerEmail());

        // computeLevenshteinSimilarity & computeTokenOverlap
        double lev0 = ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", (String) null, "test");
        assertEquals(0.0, lev0);
        double lev1 = ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", "same", "same");
        assertEquals(1.0, lev1);
        double levDiff = ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", "kitten", "sitting");
        assertTrue(levDiff > 0.0 && levDiff < 1.0);

        double tok0 = ReflectionTestUtils.invokeMethod(aiExtractionService, "computeTokenOverlap", (String) null, "test");
        assertEquals(0.0, tok0);
        double tok1 = ReflectionTestUtils.invokeMethod(aiExtractionService, "computeTokenOverlap", "item valve", "valve item");
        assertEquals(1.0, tok1);

        // mergeMissingItemFields branches
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", (RFQItem) null, new RFQItem(), 1);
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", new RFQItem(), (RFQItem) null, 1);

        RFQItem targetBlankDesc = RFQItem.builder().itemDescription("").build();
        RFQItem sourceDesc = RFQItem.builder().itemDescription("Recovered Desc").build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", targetBlankDesc, sourceDesc, 1);
        assertEquals("Recovered Desc", targetBlankDesc.getItemDescription());

        RFQItem targetDiff = RFQItem.builder().itemDescription("Laptop").build();
        RFQItem sourceDiff = RFQItem.builder().itemDescription("Hex Bolt").quantity(10.0).build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", targetDiff, sourceDiff, 1);
        assertNull(targetDiff.getQuantity());

        RFQItem targetEmptyFields = RFQItem.builder()
                .itemDescription("Gate Valve")
                .quantity(0.0)
                .uom("")
                .specification("")
                .brand("")
                .partCode("")
                .deliveryLocation("")
                .deliveryDate("")
                .build();
        RFQItem sourceFullFields = RFQItem.builder()
                .itemDescription("Gate Valve")
                .quantity(25.0)
                .uom("Nos")
                .specification("Class 150")
                .brand("L&T")
                .partCode("GV-150")
                .deliveryLocation("Mumbai")
                .deliveryDate("2026-11-01")
                .build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", targetEmptyFields, sourceFullFields, 1);
        assertEquals(25.0, targetEmptyFields.getQuantity());
        assertEquals("Nos", targetEmptyFields.getUom());
        assertEquals("Class 150", targetEmptyFields.getSpecification());
        assertEquals("L&T", targetEmptyFields.getBrand());
        assertEquals("GV-150", targetEmptyFields.getPartCode());
        assertEquals("Mumbai", targetEmptyFields.getDeliveryLocation());
        assertEquals("2026-11-01", targetEmptyFields.getDeliveryDate());

        // fillMissingTopLevel branches
        ExtractedRFQ targetTop = ExtractedRFQ.builder()
                .deliveryLocation("")
                .deliveryCity("")
                .deliveryState("")
                .deliveryPincode("")
                .deliveryDate("")
                .category("")
                .buyerEmail("")
                .build();
        ExtractedRFQ sourceTop = ExtractedRFQ.builder()
                .deliveryLocation("123 Industrial Area")
                .deliveryCity("Pune")
                .deliveryState("Maharashtra")
                .deliveryPincode("411001")
                .deliveryDate("2026-12-01")
                .category("Valves")
                .buyerEmail("buyer@example.com")
                .build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "fillMissingTopLevel", targetTop, sourceTop);
        assertEquals("123 Industrial Area", targetTop.getDeliveryLocation());
        assertEquals("Pune", targetTop.getDeliveryCity());
        assertEquals("Maharashtra", targetTop.getDeliveryState());
        assertEquals("411001", targetTop.getDeliveryPincode());
        assertEquals("2026-12-01", targetTop.getDeliveryDate());
        assertEquals("Valves", targetTop.getCategory());
        assertEquals("buyer@example.com", targetTop.getBuyerEmail());

        // isSameOrSimilarItem and similarity calculations
        assertFalse(aiExtractionService.isSameOrSimilarItem(null, "Laptop"));
        assertFalse(aiExtractionService.isSameOrSimilarItem("Laptop", ""));
        assertTrue(aiExtractionService.isSameOrSimilarItem("Speed Btreaker 50mm", "Speed Breaker 50mm"));
        assertTrue(aiExtractionService.isSameOrSimilarItem("Industrial Gate Valve DN50", "Gate Valve DN50"));
        assertFalse(aiExtractionService.isSameOrSimilarItem("Laptop Dell", "Office Chair"));

        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", (String) null, "test"));
        assertEquals(1.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", "test", "test"));
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", "", "test"));

        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeTokenOverlap", (String) null, "test"));
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeTokenOverlap", "   ", "test"));

        // Logging & abbreviate edge cases
        ReflectionTestUtils.invokeMethod(aiExtractionService, "logRawModelResponse", 1, (String) null);
        ReflectionTestUtils.invokeMethod(aiExtractionService, "logRawModelResponse", 1, "Short response");
        ReflectionTestUtils.invokeMethod(aiExtractionService, "logRawModelResponse", 1, "A".repeat(5000));

        assertEquals("", ReflectionTestUtils.invokeMethod(aiExtractionService, "abbreviate", (String) null));
        assertTrue(((String) ReflectionTestUtils.invokeMethod(aiExtractionService, "abbreviate", "B".repeat(5000))).contains("[truncated]"));

        // logExtractionSummary with null item in list
        ExtractedRFQ rfqWithNullItem = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .items(java.util.Collections.singletonList(null))
                .build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "logExtractionSummary", rfqWithNullItem);

        // collectInlineImages edge cases
        EmailData emailNoAtts = EmailData.builder().attachments(null).build();
        List<?> noImgs = ReflectionTestUtils.invokeMethod(aiExtractionService, "collectInlineImages", emailNoAtts);
        assertTrue(noImgs.isEmpty());

        File textAtt = File.createTempFile("sample_", ".txt");
        EmailData emailTextAtt = EmailData.builder().attachments(List.of(textAtt)).build();
        List<?> txtImgs = ReflectionTestUtils.invokeMethod(aiExtractionService, "collectInlineImages", emailTextAtt);
        assertTrue(txtImgs.isEmpty());
        textAtt.delete();

        // isBlank helper branches
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "isBlank", (String) null));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "isBlank", "   "));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "isBlank", "not specified"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "isBlank", "null"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "isBlank", "Valid Value"));

        // hasMissingQuantity branches
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", (ExtractedRFQ) null));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", ExtractedRFQ.builder().items(null).build()));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", ExtractedRFQ.builder().items(List.of()).build()));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", ExtractedRFQ.builder().items(List.of(RFQItem.builder().quantity(null).build())).build()));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", ExtractedRFQ.builder().items(List.of(RFQItem.builder().quantity(0.0).build())).build()));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", ExtractedRFQ.builder().items(List.of(RFQItem.builder().quantity(10.0).build())).build()));

        // logAttemptResult branches
        ReflectionTestUtils.invokeMethod(aiExtractionService, "logAttemptResult", 1, ExtractedRFQ.builder().build(), "Raw error");
        ReflectionTestUtils.invokeMethod(aiExtractionService, "logAttemptResult", 2, ExtractedRFQ.builder().items(List.of(RFQItem.builder().build())).build(), "Raw ok");

        // describeGaps branches
        List<String> gapsNull = ReflectionTestUtils.invokeMethod(aiExtractionService, "describeGaps", (ExtractedRFQ) null);
        assertFalse(gapsNull.isEmpty());

        ExtractedRFQ rfqWithNullAndBlankItems = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(java.util.Arrays.asList(
                        null,
                        RFQItem.builder().itemDescription("").quantity(null).build(),
                        RFQItem.builder().itemDescription("Item 2").quantity(0.0).build(),
                        RFQItem.builder().itemDescription("Item 3").quantity(10.0).build()
                )))
                .build();
        List<String> gapsMixed = ReflectionTestUtils.invokeMethod(aiExtractionService, "describeGaps", rfqWithNullAndBlankItems);
        assertEquals(4, gapsMixed.size());

        // mergeMissingItemFields branches
        RFQItem targetItem = RFQItem.builder().itemDescription("").quantity(0.0).build();
        RFQItem sourceItem = RFQItem.builder()
                .itemDescription("Laptop")
                .quantity(5.0)
                .uom("Nos")
                .specification("Core i7")
                .brand("Dell")
                .partCode("DL-123")
                .deliveryLocation("Mumbai")
                .deliveryDate("2026-12-01")
                .build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", targetItem, sourceItem, 2);
        assertEquals("Laptop", targetItem.getItemDescription());
        assertEquals(5.0, targetItem.getQuantity());
        assertEquals("Nos", targetItem.getUom());
        assertEquals("Core i7", targetItem.getSpecification());
        assertEquals("Dell", targetItem.getBrand());
        assertEquals("DL-123", targetItem.getPartCode());
        assertEquals("Mumbai", targetItem.getDeliveryLocation());
        assertEquals("2026-12-01", targetItem.getDeliveryDate());

        // Disagreeing items
        RFQItem targetDiff2 = RFQItem.builder().itemDescription("Office Chair").build();
        RFQItem sourceDiff2 = RFQItem.builder().itemDescription("Centrifugal Pump").build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", targetDiff2, sourceDiff2, 2);
        assertEquals("Office Chair", targetDiff2.getItemDescription());

        // Null arguments
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", (RFQItem) null, sourceDiff2, 2);

        // Multi-attempt extraction where items have blank description but valid quantity
        String jsonBlankDesc = """
        {
            "deliveryLocation": "Pune",
            "items": [
                {
                    "itemDescription": "",
                    "quantity": 10.0
                }
            ]
        }
        """;
        Mockito.when(geminiApiClient.getAllConfiguredModels()).thenReturn(List.of("m1", "m2", "m3", "m4"));
        Mockito.when(geminiApiClient.generateContent(Mockito.anyString(), Mockito.anyList())).thenReturn(jsonBlankDesc);
        ReflectionTestUtils.setField(aiExtractionService, "extractionMaxAttempts", 2);
        ExtractedRFQ resAttempts = aiExtractionService.extractRFQFromEmail(EmailData.builder().subject("RFQ").body("Need items").senderEmail("buyer@corp.com").build());
        assertNotNull(resAttempts);
        ReflectionTestUtils.setField(aiExtractionService, "extractionMaxAttempts", 3);

        // fillMissingTopLevel
        ExtractedRFQ targetTop2 = ExtractedRFQ.builder().build();
        ExtractedRFQ sourceTop2 = ExtractedRFQ.builder()
                .deliveryLocation("Pune")
                .deliveryCity("Pune")
                .deliveryState("Maharashtra")
                .deliveryPincode("411001")
                .deliveryDate("2026-12-01")
                .category("Pumps")
                .buyerEmail("buyer@corp.com")
                .build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "fillMissingTopLevel", targetTop2, sourceTop2);
        assertEquals("Pune", targetTop2.getDeliveryLocation());
        assertEquals("Pune", targetTop2.getDeliveryCity());
        assertEquals("Maharashtra", targetTop2.getDeliveryState());
        assertEquals("411001", targetTop2.getDeliveryPincode());
        assertEquals("2026-12-01", targetTop2.getDeliveryDate());
        assertEquals("Pumps", targetTop2.getCategory());
        assertEquals("buyer@corp.com", targetTop2.getBuyerEmail());

        // executeModelCall returning non-null specific model
        Mockito.when(geminiApiClient.generateContentWithSpecificModel(Mockito.anyString(), Mockito.anyString(), Mockito.anyList()))
                .thenReturn(jsonBlankDesc);
        ExtractedRFQ resSpec = aiExtractionService.extractRFQFromEmail(EmailData.builder().subject("RFQ").body("Need 10 pumps").senderEmail("b@c.com").build());
        assertNotNull(resSpec);

        // fillMissingTopLevel with both target and source blank
        ExtractedRFQ targetTopBlank = ExtractedRFQ.builder().build();
        ExtractedRFQ sourceTopBlank = ExtractedRFQ.builder().build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "fillMissingTopLevel", targetTopBlank, sourceTopBlank);
        assertNull(targetTopBlank.getDeliveryLocation());
        assertNull(targetTopBlank.getDeliveryCity());

        // mergeMissingItemFields with both target and source blank fields
        RFQItem targetItemBlank = RFQItem.builder().itemDescription("Item 1").quantity(5.0).build();
        RFQItem sourceItemBlank = RFQItem.builder().itemDescription("Item 1").quantity(0.0).build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", targetItemBlank, sourceItemBlank, 2);
        assertNull(targetItemBlank.getUom());
        assertNull(targetItemBlank.getSpecification());
        assertNull(targetItemBlank.getBrand());
        assertNull(targetItemBlank.getPartCode());
        assertNull(targetItemBlank.getDeliveryLocation());
        assertNull(targetItemBlank.getDeliveryDate());
        assertEquals(5.0, targetItemBlank.getQuantity());

        // computeTokenOverlap and computeLevenshteinSimilarity edge cases
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeTokenOverlap", (String) null, "test"), 0.001);
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeTokenOverlap", "test", (String) null), 0.001);
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeTokenOverlap", "---", "test"), 0.001);
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeTokenOverlap", "test", "---"), 0.001);

        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", (String) null, "test"), 0.001);
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", "test", (String) null), 0.001);
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", "", "test"), 0.001);
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", "test", ""), 0.001);

        // fillMissingTopLevel filling all missing fields
        ExtractedRFQ targetAllBlank = ExtractedRFQ.builder().build();
        ExtractedRFQ sourceAllFilled = ExtractedRFQ.builder()
                .deliveryLocation("Pune")
                .deliveryCity("Pune")
                .deliveryState("Maharashtra")
                .deliveryPincode("411001")
                .deliveryDate("2026-10-10")
                .category("Industrial")
                .buyerEmail("buyer@corp.com")
                .build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "fillMissingTopLevel", targetAllBlank, sourceAllFilled);
        assertEquals("Pune", targetAllBlank.getDeliveryLocation());
        assertEquals("Pune", targetAllBlank.getDeliveryCity());
        assertEquals("Maharashtra", targetAllBlank.getDeliveryState());
        assertEquals("411001", targetAllBlank.getDeliveryPincode());
        assertEquals("2026-10-10", targetAllBlank.getDeliveryDate());
        assertEquals("Industrial", targetAllBlank.getCategory());
        assertEquals("buyer@corp.com", targetAllBlank.getBuyerEmail());

        // mergeMissingItemFields with null target/source
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", (RFQItem) null, sourceItemBlank, 2);
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", targetItemBlank, (RFQItem) null, 2);

        // mergeMissingItemFields filling blank description and differing item
        RFQItem targetNoDesc = RFQItem.builder().itemDescription("").build();
        RFQItem sourceWithDesc = RFQItem.builder().itemDescription("Recovered Desc").build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", targetNoDesc, sourceWithDesc, 2);
        assertEquals("Recovered Desc", targetNoDesc.getItemDescription());

        RFQItem targetDiffItem = RFQItem.builder().itemDescription("Completely Different Apple").build();
        RFQItem sourceDiffItem = RFQItem.builder().itemDescription("Completely Different Zebra").build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", targetDiffItem, sourceDiffItem, 2);

        // mergeMissingItemFields filling all fields
        RFQItem targetFillAll = RFQItem.builder().itemDescription("Matching Item").quantity(0.0).build();
        RFQItem sourceFillAll = RFQItem.builder()
                .itemDescription("Matching Item")
                .quantity(15.0)
                .uom("Kgs")
                .specification("High Grade")
                .brand("BrandX")
                .partCode("PX-100")
                .deliveryLocation("Factory 1")
                .deliveryDate("2026-11-11")
                .build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", targetFillAll, sourceFillAll, 2);
        assertEquals(15.0, targetFillAll.getQuantity());
        assertEquals("Kgs", targetFillAll.getUom());
        assertEquals("High Grade", targetFillAll.getSpecification());
        assertEquals("BrandX", targetFillAll.getBrand());
        assertEquals("PX-100", targetFillAll.getPartCode());
        assertEquals("Factory 1", targetFillAll.getDeliveryLocation());
        assertEquals("2026-11-11", targetFillAll.getDeliveryDate());

        // mergeMissingItemFields when target already has values and source has blanks
        RFQItem targetHasAll = RFQItem.builder()
                .itemDescription("Matching Item")
                .quantity(10.0)
                .uom("Nos")
                .specification("Spec A")
                .brand("Brand A")
                .partCode("PA-1")
                .deliveryLocation("Loc A")
                .deliveryDate("2026-10-10")
                .build();
        RFQItem sourceBlankAll = RFQItem.builder().itemDescription("Matching Item").build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "mergeMissingItemFields", targetHasAll, sourceBlankAll, 2);
        assertEquals(10.0, targetHasAll.getQuantity());
        assertEquals("Nos", targetHasAll.getUom());

        // fillMissingTopLevel when target has values and source has blanks
        ExtractedRFQ targetTopHasAll = ExtractedRFQ.builder()
                .deliveryLocation("Loc")
                .deliveryCity("City")
                .deliveryState("State")
                .deliveryPincode("123456")
                .deliveryDate("2026-10-10")
                .category("Cat")
                .buyerEmail("b@c.com")
                .build();
        ExtractedRFQ sourceTopBlankAll = ExtractedRFQ.builder().build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "fillMissingTopLevel", targetTopHasAll, sourceTopBlankAll);
        assertEquals("Loc", targetTopHasAll.getDeliveryLocation());

        // mergeBetterResult with same count but permuted / non-matching items
        ExtractedRFQ best2Items = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("Item A").build(),
                        RFQItem.builder().itemDescription("Item B").build()
                )))
                .build();
        ExtractedRFQ candPermuted = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("Item B").quantity(20.0).build(),
                        RFQItem.builder().itemDescription("Item Z Unmatched").build()
                )))
                .build();
        ExtractedRFQ resultMerged = ReflectionTestUtils.invokeMethod(
                aiExtractionService, "mergeBetterResult", best2Items, candPermuted, 2);
        assertNotNull(resultMerged);
        assertEquals(20.0, best2Items.getItems().get(1).getQuantity());
    }

    @Test
    @DisplayName("Test extractRFQ with generateContentWithSpecificModelDetailed returning tokens")
    void testExtractRFQWithSpecificModelDetailedAndTokens() throws Exception {
        String json = "{\n" +
                "  \"buyerEmail\": \"buyer@test.com\",\n" +
                "  \"deliveryLocation\": \"Pune\",\n" +
                "  \"deliveryDate\": \"2026-09-20\",\n" +
                "  \"items\": [{\"itemDescription\": \"Widget A\", \"quantity\": 15.0, \"uom\": \"Nos\"}]\n" +
                "}";

        GeminiContentResponse resp = GeminiContentResponse.builder()
                .text(json)
                .model("gemini-3.7-flash")
                .promptTokens(300)
                .candidateTokens(80)
                .totalTokens(380)
                .build();

        Mockito.when(geminiApiClient.getAllConfiguredModels()).thenReturn(List.of("gemini-3.7-flash"));
        Mockito.when(geminiApiClient.generateContentWithSpecificModelDetailed(eq("gemini-3.7-flash"), anyString(), anyList()))
                .thenReturn(resp);

        EmailData email = EmailData.builder()
                .messageId("MSG-TOKENS-1")
                .subject("Need Widgets")
                .body("Please send 15 Widget A")
                .senderEmail("buyer@test.com")
                .build();

        ExtractedRFQ result = aiExtractionService.extractRFQFromEmail(email);
        assertNotNull(result);
        assertNotNull(result.getTokenUsage());
        assertEquals("gemini-3.7-flash", result.getTokenUsage().getModelName());
        assertEquals(300, result.getTokenUsage().getPromptTokens());
        assertEquals(80, result.getTokenUsage().getCandidateTokens());
        assertEquals(380, result.getTokenUsage().getTotalTokens());
        assertEquals("MSG-TOKENS-1", result.getTokenUsage().getMessageId());
        assertTrue(result.getTokenUsage().getEstimatedCostUsd() > 0);
    }

    @Test
    @DisplayName("Test executeModelCallDetailed fallback to generateContentDetailed and token estimation math")
    void testExecuteModelCallDetailedFallbackAndTokenMath() throws Exception {
        // Long response to exceed 400 length for Math.max(100, length / 4)
        String jsonLong = "{\n" +
                "  \"buyerEmail\": \"buyer@test.com\",\n" +
                "  \"deliveryLocation\": \"Pune Industrial Area Phase 2, Near Railway Crossing, Pune, Maharashtra\",\n" +
                "  \"deliveryDate\": \"2026-09-20\",\n" +
                "  \"items\": [\n" +
                "    {\"itemDescription\": \"Heavy Duty Industrial Hydraulic High Pressure Double Acting Cylinder Type 1\", \"quantity\": 10.0, \"uom\": \"Nos\", \"specification\": \"Standard Industrial Hydraulic Specification Grade A High Strength Alloy Steel\", \"brand\": \"Rexroth Precision Engineering\"},\n" +
                "    {\"itemDescription\": \"Heavy Duty Industrial Hydraulic High Pressure Double Acting Cylinder Type 2\", \"quantity\": 20.0, \"uom\": \"Nos\", \"specification\": \"Standard Industrial Hydraulic Specification Grade B High Strength Alloy Steel\", \"brand\": \"Rexroth Precision Engineering\"}\n" +
                "  ]\n" +
                "}";
        assertTrue(jsonLong.length() > 400);

        GeminiContentResponse respDetailed = GeminiContentResponse.builder()
                .text(jsonLong)
                .model(null) // test model == null fallback to currentModel
                .totalTokens(0) // test tTokens == 0 fallback math
                .build();

        Mockito.when(geminiApiClient.getAllConfiguredModels()).thenReturn(List.of("gemini-3.7-flash"));
        Mockito.when(geminiApiClient.generateContentWithSpecificModelDetailed(anyString(), anyString(), anyList()))
                .thenThrow(new UnsupportedOperationException("Specific model detailed unsupported"));
        Mockito.when(geminiApiClient.generateContentDetailed(anyString(), anyList()))
                .thenReturn(respDetailed);

        // Long prompt to exceed 1600 length for Math.max(400, length / 4)
        String longBody = "Please provide quotation for the following heavy duty industrial equipment: " + "DETAILS ".repeat(300);
        assertTrue(longBody.length() > 1600);

        EmailData email = EmailData.builder()
                .messageId("MSG-TOKENS-FALLBACK")
                .subject("Long Request Body")
                .body(longBody)
                .senderEmail("buyer@test.com")
                .build();

        ExtractedRFQ result = aiExtractionService.extractRFQFromEmail(email);
        assertNotNull(result);
        assertNotNull(result.getTokenUsage());
        assertEquals("gemini-3.7-flash", result.getTokenUsage().getModelName());
        assertTrue(result.getTokenUsage().getPromptTokens() > 400);
        assertTrue(result.getTokenUsage().getCandidateTokens() > 100);
        assertEquals(result.getTokenUsage().getPromptTokens() + result.getTokenUsage().getCandidateTokens(),
                result.getTokenUsage().getTotalTokens());
    }

    @Test
    @DisplayName("Test executeModelCallDetailed double NoSuchMethodError fallback to legacy generateContent")
    void testExecuteModelCallDetailedNoSuchMethodFallback() throws Exception {
        String json = "{\n" +
                "  \"buyerEmail\": \"buyer@test.com\",\n" +
                "  \"deliveryLocation\": \"Mumbai\",\n" +
                "  \"deliveryDate\": \"2026-09-25\",\n" +
                "  \"items\": [{\"itemDescription\": \"Item Legacy\", \"quantity\": 5.0, \"uom\": \"Nos\"}]\n" +
                "}";

        Mockito.when(geminiApiClient.getAllConfiguredModels()).thenReturn(List.of("gemini-3.7-flash"));
        Mockito.when(geminiApiClient.generateContentWithSpecificModelDetailed(anyString(), anyString(), anyList()))
                .thenThrow(new NoSuchMethodError("No such method"));
        Mockito.when(geminiApiClient.generateContentDetailed(anyString(), anyList()))
                .thenThrow(new NoSuchMethodError("No such method"));
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn(json);

        EmailData email = EmailData.builder()
                .messageId("MSG-LEGACY")
                .subject("Need Item Legacy")
                .body("Need 5 Item Legacy")
                .senderEmail("buyer@test.com")
                .build();

        ExtractedRFQ result = aiExtractionService.extractRFQFromEmail(email);
        assertNotNull(result);
        assertEquals("gemini-3.7-flash", result.getTokenUsage().getModelName());
    }

    @Test
    @DisplayName("Test collectInlineImages edge cases and limits additional")
    void testCollectInlineImagesLimitsAdditional() throws Exception {
        EmailData emailNoAttach = EmailData.builder().attachments(null).build();
        List<?> imagesNull = ReflectionTestUtils.invokeMethod(aiExtractionService, "collectInlineImages", emailNoAttach);
        assertNotNull(imagesNull);
        assertTrue(imagesNull.isEmpty());

        File nonImageFile = new File(tempDir.toFile(), "notes.txt");
        try (FileOutputStream fos = new FileOutputStream(nonImageFile)) {
            fos.write("some text".getBytes());
        }

        File validImageFile = new File(tempDir.toFile(), "sample.png");
        try (FileOutputStream fos = new FileOutputStream(validImageFile)) {
            fos.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
            fos.write(new byte[100]);
        }

        EmailData emailWithFiles = EmailData.builder()
                .attachments(List.of(nonImageFile, validImageFile))
                .build();
        List<?> images = ReflectionTestUtils.invokeMethod(aiExtractionService, "collectInlineImages", emailWithFiles);
        assertNotNull(images);
        assertEquals(1, images.size());

        // Oversized single image limit
        ReflectionTestUtils.setField(aiExtractionService, "maxInlineImageBytes", 10L);
        List<?> imagesOverSingle = ReflectionTestUtils.invokeMethod(aiExtractionService, "collectInlineImages", emailWithFiles);
        assertTrue(imagesOverSingle.isEmpty());

        // Oversized total image limit
        ReflectionTestUtils.setField(aiExtractionService, "maxInlineImageBytes", 1000000L);
        ReflectionTestUtils.setField(aiExtractionService, "maxInlineImageTotalBytes", 50L);
        List<?> imagesOverTotal = ReflectionTestUtils.invokeMethod(aiExtractionService, "collectInlineImages", emailWithFiles);
        assertTrue(imagesOverTotal.isEmpty());

        // Max inline images count
        ReflectionTestUtils.setField(aiExtractionService, "maxInlineImages", 0);
        List<?> imagesMaxZero = ReflectionTestUtils.invokeMethod(aiExtractionService, "collectInlineImages", emailWithFiles);
        assertTrue(imagesMaxZero.isEmpty());
    }

    @Test
    @DisplayName("Test loadPromptTemplate exception when loading template fails")
    void testLoadPromptTemplateException() throws Exception {
        AIExtractionService spyService = Mockito.spy(aiExtractionService);
        Mockito.doThrow(new java.io.IOException("Template read failure")).when(spyService).loadPromptTemplate();

        EmailData email = EmailData.builder()
                .subject("Test Subject")
                .body("Test Body")
                .senderEmail("buyer@test.com")
                .build();

        assertThrows(ApplicationException.class, () -> spyService.extractRFQFromEmail(email));
    }

    @Test
    @DisplayName("Test describeGaps and hasMissingQuantity branches")
    void testDescribeGapsAndHasMissingQuantity() {
        // hasMissingQuantity branches
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", (ExtractedRFQ) null));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", ExtractedRFQ.builder().items(null).build()));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", ExtractedRFQ.builder().items(List.of()).build()));

        ExtractedRFQ withNullItem = ExtractedRFQ.builder().items(Collections.singletonList(null)).build();
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", withNullItem));

        ExtractedRFQ withNullQty = ExtractedRFQ.builder().items(List.of(RFQItem.builder().quantity(null).build())).build();
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", withNullQty));

        ExtractedRFQ withZeroQty = ExtractedRFQ.builder().items(List.of(RFQItem.builder().quantity(0.0).build())).build();
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", withZeroQty));

        ExtractedRFQ withValidQty = ExtractedRFQ.builder().items(List.of(RFQItem.builder().quantity(10.0).build())).build();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "hasMissingQuantity", withValidQty));

        // describeGaps branches
        List<String> gapsNull = ReflectionTestUtils.invokeMethod(aiExtractionService, "describeGaps", (ExtractedRFQ) null);
        assertEquals(1, gapsNull.size());

        List<String> gapsNullItem = ReflectionTestUtils.invokeMethod(aiExtractionService, "describeGaps", withNullItem);
        assertEquals(1, gapsNullItem.size());
        assertTrue(gapsNullItem.get(0).contains("the whole item is null"));

        ExtractedRFQ withBlankDescAndZeroQty = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("   ").quantity(-1.0).build()))
                .build();
        List<String> gapsBlank = ReflectionTestUtils.invokeMethod(aiExtractionService, "describeGaps", withBlankDescAndZeroQty);
        assertTrue(gapsBlank.size() >= 2);
    }

    @Test
    @DisplayName("Test isSameOrSimilarItem and Levenshtein and Token overlap branches")
    void testSimilarityAndOverlapBranches() {
        assertFalse(aiExtractionService.isSameOrSimilarItem(null, "bolt"));
        assertFalse(aiExtractionService.isSameOrSimilarItem("bolt", ""));
        assertTrue(aiExtractionService.isSameOrSimilarItem("Hex Bolt M10", "hex bolt m10"));
        assertTrue(aiExtractionService.isSameOrSimilarItem("M10 Hex Bolt Grade 8.8", "Hex Bolt"));
        assertTrue(aiExtractionService.isSameOrSimilarItem("Speed Btreaker", "Speed Breaker"));

        // Levenshtein direct
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", null, "abc"));
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", "abc", null));
        assertEquals(1.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", "same", "same"));
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", "", "abc"));
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeLevenshteinSimilarity", "abc", ""));

        // Token overlap direct
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeTokenOverlap", null, "abc"));
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeTokenOverlap", "abc", null));
        assertEquals(0.0, (Double) ReflectionTestUtils.invokeMethod(aiExtractionService, "computeTokenOverlap", "---", "###"));

        // isBlank direct
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "isBlank", "null"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "isBlank", "Not Specified"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(aiExtractionService, "isBlank", "valid text"));

        // abbreviate direct
        assertEquals("", ReflectionTestUtils.invokeMethod(aiExtractionService, "abbreviate", (String) null));
        assertEquals("short", ReflectionTestUtils.invokeMethod(aiExtractionService, "abbreviate", "short"));
        ReflectionTestUtils.setField(aiExtractionService, "maxLoggedResponseChars", 5);
        assertEquals("12345...[truncated]", ReflectionTestUtils.invokeMethod(aiExtractionService, "abbreviate", "123456789"));

        // logRawModelResponse direct
        ReflectionTestUtils.invokeMethod(aiExtractionService, "logRawModelResponse", 1, (String) null);
        ReflectionTestUtils.invokeMethod(aiExtractionService, "logRawModelResponse", 1, "short");
        ReflectionTestUtils.invokeMethod(aiExtractionService, "logRawModelResponse", 1, "longer-than-five-chars");

        // logExtractionSummary with null item
        ExtractedRFQ rfqWithNullItem = ExtractedRFQ.builder().buyerEmail("b@c.com").items(Collections.singletonList(null)).build();
        ReflectionTestUtils.invokeMethod(aiExtractionService, "logExtractionSummary", rfqWithNullItem);
    }

    @Test
    @DisplayName("Test missing quantity confirmed across multiple successful models breaks early")
    void testMissingQuantityConfirmedAcrossMultipleModels() throws Exception {
        ReflectionTestUtils.setField(aiExtractionService, "minModelsForMissingQuantity", 2);

        String jsonNoQty = "{\n" +
                "  \"buyerEmail\": \"buyer@test.com\",\n" +
                "  \"deliveryLocation\": \"Pune\",\n" +
                "  \"items\": [{\"itemDescription\": \"Item Without Qty\", \"quantity\": null, \"uom\": \"Nos\"}]\n" +
                "}";

        Mockito.when(geminiApiClient.getAllConfiguredModels()).thenReturn(List.of("model-a", "model-b", "model-c"));
        Mockito.when(geminiApiClient.generateContentWithSpecificModelDetailed(anyString(), anyString(), anyList()))
                .thenReturn(GeminiContentResponse.builder().text(jsonNoQty).totalTokens(100).build());

        EmailData email = EmailData.builder()
                .messageId("MSG-MULTI-BREAK")
                .subject("RFQ Subject")
                .body("RFQ Body")
                .senderEmail("buyer@test.com")
                .build();

        ExtractedRFQ result = aiExtractionService.extractRFQFromEmail(email);
        assertNotNull(result);
        assertNull(result.getItems().get(0).getQuantity());
    }
}
