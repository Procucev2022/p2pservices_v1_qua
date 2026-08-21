package com.portal.procucev.rfq;

import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.service.RFQBuilderService;
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

public class EmailRfqDefaultingAndRemarksTest {

    private DateParser dateParser;
    private PincodeDao pincodeDao;
    private RFQBuilderService rfqBuilderService;
    private ValidationService validationService;
    private Buyer defaultBuyer;

    @BeforeEach
    void setUp() {
        dateParser = new DateParser();
        pincodeDao = Mockito.mock(PincodeDao.class);
        rfqBuilderService = new RFQBuilderService(dateParser, pincodeDao);
        validationService = new ValidationService();

        defaultBuyer = Buyer.builder()
                .name("Registered Buyer")
                .email("buyer@registeredcompany.com")
                .companyName("Registered Buyer Corp")
                .address("100 Registered St")
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560001")
                .orgId("10")
                .userId("20")
                .verified(true)
                .build();
    }

    @Test
    @DisplayName("Test Case 1: Email contains quantity, delivery date and location")
    void testCase1_AllInformationProvided() {
        String expectedDate = "2026-08-30";
        String expectedLocation = "Hyderabad, Telangana";
        double expectedQty = 50.0;

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@registeredcompany.com")
                .deliveryLocation(expectedLocation)
                .deliveryDate(expectedDate)
                .items(new ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription("Dell Latitude Laptops")
                                .quantity(expectedQty)
                                .specification("Core i5, 16GB RAM, 512GB SSD")
                                .brand("Dell")
                                .deliveryLocation(expectedLocation)
                                .deliveryDate(expectedDate)
                                .build()
                )))
                .build();

        // Validation check
        ValidationService.ValidationResult valResult = validationService.validateWithDetails(rfq);
        assertTrue(valResult.isValid(), "Validation should pass when all fields are provided");

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, defaultBuyer, "Need Laptops", null);

        assertNotNull(req);
        assertEquals(1, req.getRfqItem().size());
        assertEquals(expectedQty, req.getRfqItem().get(0).getQuantity(), "Quantity should equal email quantity");
        assertEquals(expectedDate, req.getDeliveryDate(), "Delivery date should equal email delivery date");
        assertEquals("Hyderabad", req.getClientdeliverylocationrfq().get(0).getCity(), "Location should match email location city");
    }

    @Test
    @DisplayName("Test Case 2: Email does NOT contain quantity -> Defaults to 1 & RFQ creation succeeds")
    void testCase2_MissingQuantity_DefaultsToOne() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@registeredcompany.com")
                .deliveryLocation("Chennai, Tamil Nadu")
                .deliveryDate("2026-08-28")
                .items(new ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription("Dell Laptops")
                                .quantity(null) // Missing quantity
                                .specification("Core i5")
                                .build()
                )))
                .build();

        // Validation should succeed even when quantity is missing
        ValidationService.ValidationResult valResult = validationService.validateWithDetails(rfq);
        assertTrue(valResult.isValid(), "Validation should succeed when quantity is missing");

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, defaultBuyer, "Need Laptops", null);

        assertNotNull(req, "RFQ creation should succeed");
        assertEquals(1, req.getRfqItem().size());
        assertEquals(1.0, req.getRfqItem().get(0).getQuantity(), "Quantity should default to 1.0 when missing");
    }

    @Test
    @DisplayName("Test Case 3: Email does NOT contain delivery date -> Defaults to current date + 5 days")
    void testCase3_MissingDeliveryDate_DefaultsToCurrentPlusFiveDays() {
        String expectedDefaultDate = LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryLocation("Mumbai, Maharashtra")
                .deliveryDate(null) // Missing delivery date
                .items(new ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription("Office Chairs")
                                .quantity(10.0)
                                .build()
                )))
                .build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, defaultBuyer, "Need Office Chairs", null);

        assertNotNull(req);
        assertEquals(expectedDefaultDate, req.getDeliveryDate(), "Delivery date should dynamically default to current date + 5 days");
    }

    @Test
    @DisplayName("Test Case 4: Email does NOT contain delivery location -> Defaults to buyer's registered location")
    void testCase4_MissingDeliveryLocation_DefaultsToBuyerRegisteredLocation() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryLocation(null) // Missing location
                .deliveryDate("2026-08-25")
                .items(new ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription("Monitors")
                                .quantity(5.0)
                                .build()
                )))
                .build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, defaultBuyer, "Need Monitors", null);

        assertNotNull(req);
        assertEquals(1, req.getClientdeliverylocationrfq().size());
        assertEquals("Bangalore", req.getClientdeliverylocationrfq().get(0).getCity(), "City should default to buyer's registered city");
        assertEquals("Karnataka", req.getClientdeliverylocationrfq().get(0).getState(), "State should default to buyer's registered state");
        assertEquals("560001", req.getClientdeliverylocationrfq().get(0).getPincode(), "Pincode should default to buyer's registered pincode");
        assertEquals("100 Registered St", req.getClientdeliverylocationrfq().get(0).getAddress(), "Address should default to buyer's registered address");
    }

    @Test
    @DisplayName("Test Case 5: Email contains neither quantity, delivery date nor location -> All defaults applied & RFQ creation succeeds")
    void testCase5_MissingQuantityDateAndLocation_AllDefaultsApplied() {
        String expectedDefaultDate = LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@registeredcompany.com")
                .deliveryLocation(null)
                .deliveryDate(null)
                .items(new ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription("Ergonomic Keyboards")
                                .quantity(null)
                                .build()
                )))
                .build();

        ValidationService.ValidationResult valResult = validationService.validateWithDetails(rfq);
        assertTrue(valResult.isValid(), "Validation should succeed when quantity, date and location are all missing");

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, defaultBuyer, "Need Keyboards", null);

        assertNotNull(req, "RFQ creation should succeed");
        assertEquals(1.0, req.getRfqItem().get(0).getQuantity(), "Quantity should default to 1");
        assertEquals(expectedDefaultDate, req.getDeliveryDate(), "Delivery date should default to current date + 5 days");
        assertEquals("Bangalore", req.getClientdeliverylocationrfq().get(0).getCity(), "Location should default to buyer's registered location");
    }

    @Test
    @DisplayName("Test Case 6: Email contains RFQ info plus sender/company info -> RFQ remarks contains only RFQ request content")
    void testCase6_RemarksExcludesSenderAndCompanyInfo() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryLocation("Bangalore")
                .deliveryDate("2026-08-30")
                .items(new ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription("Dell Latitude laptops")
                                .specification("Core i5, 16GB RAM, 512GB SSD")
                                .quantity(50.0)
                                .brand("Dell")
                                .deliveryDate("2026-08-30")
                                .deliveryLocation("Bangalore")
                                .build()
                )))
                .build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, defaultBuyer, "RFQ Request", null);

        assertNotNull(req);
        String remarks = req.getRemarks();
        assertNotNull(remarks, "Remarks should not be null");

        // Verify structure
        assertTrue(remarks.contains("Description:\nDell Latitude laptops"), "Remarks should contain Description");
        assertTrue(remarks.contains("Specifications:\nCore i5, 16GB RAM, 512GB SSD"), "Remarks should contain Specifications");
        assertTrue(remarks.contains("Quantity:\n50"), "Remarks should contain Quantity");
        assertTrue(remarks.contains("Brand:\nDell"), "Remarks should contain Brand");
        assertTrue(remarks.contains("Delivery Date:\n2026-08-30"), "Remarks should contain Delivery Date");
        assertTrue(remarks.contains("Delivery Location:\nBangalore"), "Remarks should contain Delivery Location");

        // Exclusions: Email ID, sender details, company details must NOT be included in remarks
        assertFalse(remarks.contains("buyer@registeredcompany.com"), "Remarks must NOT contain buyer email ID");
        assertFalse(remarks.contains("Registered Buyer Corp"), "Remarks must NOT contain buyer company name");
        assertFalse(remarks.contains("John"), "Remarks must NOT contain sender name");
        assertFalse(remarks.contains("Regards"), "Remarks must NOT contain email metadata/sign-off");
    }
}
