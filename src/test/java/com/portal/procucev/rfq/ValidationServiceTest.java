package com.portal.procucev.rfq;

import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    @Test
    @DisplayName("Test validateWithDetails when extractedRFQ is null")
    void testValidateNullExtractedRfq() {
        ValidationService.ValidationResult res = validationService.validateWithDetails(null);
        assertFalse(res.isValid());
        assertEquals("AI Extraction returned null result.", res.getFailureReason());
    }

    @Test
    @DisplayName("Test validateWithDetails when buyer email is null or blank")
    void testValidateNullOrBlankBuyerEmail() {
        ExtractedRFQ rfq1 = ExtractedRFQ.builder().buyerEmail(null).build();
        ValidationService.ValidationResult res1 = validationService.validateWithDetails(rfq1);
        assertFalse(res1.isValid());
        assertEquals("Buyer email is missing.", res1.getFailureReason());

        ExtractedRFQ rfq2 = ExtractedRFQ.builder().buyerEmail("  ").build();
        ValidationService.ValidationResult res2 = validationService.validateWithDetails(rfq2);
        assertFalse(res2.isValid());
        assertEquals("Buyer email is missing.", res2.getFailureReason());
    }

    @Test
    @DisplayName("Test validateWithDetails when items list is null or empty")
    void testValidateNullOrEmptyItems() {
        ExtractedRFQ rfq1 = ExtractedRFQ.builder().buyerEmail("buyer@test.com").items(null).build();
        ValidationService.ValidationResult res1 = validationService.validateWithDetails(rfq1);
        assertFalse(res1.isValid());
        assertEquals("No line items found in RFQ.", res1.getFailureReason());

        ExtractedRFQ rfq2 = ExtractedRFQ.builder().buyerEmail("buyer@test.com").items(List.of()).build();
        ValidationService.ValidationResult res2 = validationService.validateWithDetails(rfq2);
        assertFalse(res2.isValid());
        assertEquals("No line items found in RFQ.", res2.getFailureReason());
    }

    @Test
    @DisplayName("Test validateWithDetails when item is null or description is null/blank")
    void testValidateNullItemOrDescription() {
        List<RFQItem> list1 = new ArrayList<>();
        list1.add(null);
        ExtractedRFQ rfq1 = ExtractedRFQ.builder().buyerEmail("buyer@test.com").items(list1).build();
        ValidationService.ValidationResult res1 = validationService.validateWithDetails(rfq1);
        assertFalse(res1.isValid());
        assertEquals("Item #1 is null.", res1.getFailureReason());

        ExtractedRFQ rfq2 = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .items(List.of(RFQItem.builder().itemDescription(null).build()))
                .build();
        ValidationService.ValidationResult res2 = validationService.validateWithDetails(rfq2);
        assertFalse(res2.isValid());
        assertEquals("Item #1 has no description.", res2.getFailureReason());

        ExtractedRFQ rfq3 = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .items(List.of(RFQItem.builder().itemDescription("  ").build()))
                .build();
        ValidationService.ValidationResult res3 = validationService.validateWithDetails(rfq3);
        assertFalse(res3.isValid());
        assertEquals("Item #1 has no description.", res3.getFailureReason());
    }

    @Test
    @DisplayName("Test validateWithDetails with valid items and default quantity and UOM")
    void testValidateValidItemsDefaulting() {
        RFQItem item1 = RFQItem.builder().itemDescription("Item 1").quantity(null).uom(null).build();
        RFQItem item2 = RFQItem.builder().itemDescription("Item 2").quantity(0.0).uom("").build();
        RFQItem item3 = RFQItem.builder().itemDescription("Item 3").quantity(5.0).uom("PCS").build();

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .items(List.of(item1, item2, item3))
                .build();

        ValidationService.ValidationResult res = validationService.validateWithDetails(rfq);
        assertTrue(res.isValid());
        assertNull(res.getFailureReason());
        assertEquals(1.0, item1.getQuantity());
        assertEquals("Nos", item1.getUom());
        assertEquals(1.0, item2.getQuantity());
        assertEquals("Nos", item2.getUom());
        assertEquals(5.0, item3.getQuantity());
        assertEquals("PCS", item3.getUom());
    }
}
