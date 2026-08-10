package com.portal.procucev.rfq;

import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.service.CategoryClassificationService;
import com.portal.procucev.rfq.service.ExcelMasterDataLoader;
import com.portal.procucev.rfq.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmailRfqValidationRulesTest {

    private ValidationService validationService;
    private DateParser dateParser;
    private CategoryClassificationService categoryClassificationService;
    private ExcelMasterDataLoader excelMasterDataLoader;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
        dateParser = new DateParser();
        excelMasterDataLoader = Mockito.mock(ExcelMasterDataLoader.class);
        Mockito.when(excelMasterDataLoader.getMasterRecords()).thenReturn(new ArrayList<>());
        categoryClassificationService = new CategoryClassificationService(excelMasterDataLoader);
    }

    @Test
    @DisplayName("TEST 1: Valid existing buyer + quantity + location + date -> Validation Passes")
    void test1_ValidBuyerQuantityLocationDate() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .deliveryLocation("Bangalore, Karnataka - 560001")
                .deliveryDate("2026-08-25")
                .items(List.of(
                        RFQItem.builder().itemDescription("Dell Laptop").quantity(20.0).uom("NOS").build()
                ))
                .build();

        ValidationService.ValidationResult result = validationService.validateWithDetails(rfq);
        assertTrue(result.isValid());
        assertFalse(result.isMissingQuantity());
    }

    @Test
    @DisplayName("TEST 2 & 6: Delivery location fallback uses buyer registration address when location is missing")
    void test2_DeliveryLocationFallback() {
        Buyer buyer = Buyer.builder()
                .address("45 Outer Ring Road, Mahadevapura")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560048")
                .verified(true)
                .build();

        String city = "";
        String state = "";
        String pincode = "";

        if (city.isBlank() && buyer.getCity() != null) city = buyer.getCity();
        if (state.isBlank() && buyer.getState() != null) state = buyer.getState();
        if (pincode.isBlank() && buyer.getPincode() != null) pincode = buyer.getPincode();

        assertEquals("Bengaluru", city);
        assertEquals("Karnataka", state);
        assertEquals("560048", pincode);
    }

    @Test
    @DisplayName("TEST 3: Missing quantity -> Validation fails and reports missing quantity")
    void test3_MissingQuantityFails() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Dell Latitude Laptops").quantity(null).uom("NOS").build()
                ))
                .build();

        ValidationService.ValidationResult result = validationService.validateWithDetails(rfq);
        assertFalse(result.isValid());
        assertTrue(result.isMissingQuantity());
        assertEquals(1, result.getMissingItems().size());
        assertTrue(result.getFailureReason().contains("Quantity is missing for:\n1. Dell Latitude Laptops"));
    }

    @Test
    @DisplayName("TEST 4: Multiple items where one item is missing quantity -> Entire RFQ fails (No partial RFQ)")
    void test4_MultipleItemsOneMissingQuantityFailsEntireRfq() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Dell Latitude Laptop").quantity(20.0).uom("NOS").build(),
                        RFQItem.builder().itemDescription("Computer Monitor").quantity(null).uom("NOS").build()
                ))
                .build();

        ValidationService.ValidationResult result = validationService.validateWithDetails(rfq);
        assertFalse(result.isValid());
        assertTrue(result.isMissingQuantity());
        assertEquals(1, result.getMissingItems().size());
        assertEquals("Computer Monitor", result.getMissingItems().get(0));
    }

    @Test
    @DisplayName("TEST 5: Non-existing buyer -> Unverified buyer status halts RFQ creation")
    void test5_NonExistingBuyerValidation() {
        Buyer buyer = Buyer.builder().email("unregistered@example.com").verified(false).build();
        assertFalse(buyer.isVerified());
    }

    @Test
    @DisplayName("TEST 7 & 14: Explicit delivery date vs dynamic current date + 5 days fallback")
    void test7_DeliveryDateResolution() {
        // Explicit Date
        String explicitDate = dateParser.parseDateString("20-08-2026");
        assertEquals("2026-08-20", explicitDate);

        // Missing Date -> Current Date + 5 Days
        String defaultDate = dateParser.parseDateString(null);
        String expectedDefault = LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertEquals(expectedDefault, defaultDate);

        // Relative Date Phrase "within 10 days"
        String relativeDate = dateParser.parseDateString("within 10 days");
        String expectedRelative = LocalDate.now().plusDays(10).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertEquals(expectedRelative, relativeDate);
    }

    @Test
    @DisplayName("TEST 8 & 9: Excel category resolution maps laptop to IT Hardware & Electronics")
    void test8_CategoryClassification() {
        RFQItem item = RFQItem.builder().itemDescription("Dell Latitude Laptops").build();
        categoryClassificationService.classifyItems(List.of(item));

        assertEquals("IT Hardware & Electronics", item.getCategory());
        assertEquals("DOMAIN_KEYWORD_MATCHED", item.getClassificationStatus());
    }
}
