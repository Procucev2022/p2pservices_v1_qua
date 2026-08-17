package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.repository.RFQRepository;
import com.portal.procucev.rfq.repository.RfqItemRecordRepository;
import com.portal.procucev.rfq.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Reproduces RFQ-20260817134508-bd06ef75, where the model filed the whole requirement sentence
 * under "brand" and left "specification" empty. The RFQ was created showing
 * Specification = "Laptop" and Remarks = "Brand: -new laptops witl...".
 */
class EmailRfqSpecificationSalvageTest {

    private static final String LAPTOP_SPEC =
            "Intel Core i5 / equivalent processor, 16 GB RAM, 512 GB SSD, 15.6-inch Full HD display, "
            + "Wi-Fi, Bluetooth, integrated webcam, USB ports, HDMI port, and Windows 11 operating system";

    private EmailReaderService emailReaderService;
    private AIExtractionService aiExtractionService;
    private BuyerVerificationService buyerVerificationService;
    private RFQBuilderService rfqBuilderService;
    private RFQApiService rfqApiService;
    private EmailProcessorService emailProcessorService;

    @BeforeEach
    void setUp() {
        emailReaderService = Mockito.mock(EmailReaderService.class);
        aiExtractionService = Mockito.mock(AIExtractionService.class);
        buyerVerificationService = Mockito.mock(BuyerVerificationService.class);
        rfqBuilderService = Mockito.mock(RFQBuilderService.class);
        rfqApiService = Mockito.mock(RFQApiService.class);

        emailProcessorService = new EmailProcessorService(
                emailReaderService,
                aiExtractionService,
                Mockito.mock(ValidationService.class),
                buyerVerificationService,
                rfqBuilderService,
                rfqApiService,
                Mockito.mock(CategoryClassificationService.class),
                Mockito.mock(AcknowledgementEmailService.class),
                Mockito.mock(RFQRepository.class),
                Mockito.mock(EmailTransactionRepository.class),
                Mockito.mock(RfqItemRecordRepository.class),
                new DateParser(),
                new ObjectMapper());
    }

    /** Drives one email through the pipeline and returns the ExtractedRFQ handed to the builder. */
    private ExtractedRFQ runAndCaptureBuilderInput(EmailData email, ExtractedRFQ aiResult) {
        Mockito.when(buyerVerificationService.verifyAndGetBuyer(email.getSenderEmail()))
                .thenReturn(Buyer.builder().email(email.getSenderEmail()).name("Buyer").verified(true).build());
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(aiResult);

        RFQRequest request = RFQRequest.builder().rfqNumber("RFQ-TEST").deliveryDate("2026-08-25").build();
        Mockito.when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(request);
        Mockito.when(rfqApiService.submitRFQ(any()))
                .thenReturn(RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-TEST").build());
        Mockito.when(Mockito.mock(RFQRepository.class).save(any()))
                .thenReturn(RFQEntity.builder().rfqNumber("RFQ-TEST").build());

        emailProcessorService.processSingleEmail(email);

        ArgumentCaptor<ExtractedRFQ> captor = ArgumentCaptor.forClass(ExtractedRFQ.class);
        Mockito.verify(rfqBuilderService).buildRFQRequest(captor.capture(), any(), any(), any());
        return captor.getValue();
    }

    @Test
    @DisplayName("Requirement prose mis-filed under brand is moved into specification")
    void requirementProseInBrandIsMovedToSpecification() {
        EmailData email = EmailData.builder()
                .messageId("MSG-LAPTOP-SALVAGE")
                .senderEmail("buyer@test.com")
                .subject("Laptop")
                .body("We require 10 new laptops with the following specification. Delivery Location: Bangalore")
                .build();

        // Exactly what production returned: prose in brand, specification empty.
        ExtractedRFQ aiResult = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .deliveryLocation("Bangalore")
                .deliveryDate("2026-08-25")
                .items(List.of(RFQItem.builder()
                        .itemDescription("Laptop")
                        .brand("\u2013new laptops with " + LAPTOP_SPEC)
                        .specification(null)
                        .quantity(10.0)
                        .uom("Nos")
                        .category("IT Hardware & Electronics")
                        .build()))
                .build();

        RFQItem item = runAndCaptureBuilderInput(email, aiResult).getItems().get(0);

        assertNotNull(item.getSpecification(), "specification must be populated from the salvaged text");
        assertTrue(item.getSpecification().contains("16 GB RAM"), item.getSpecification());
        assertTrue(item.getSpecification().contains("Windows 11 operating system"), item.getSpecification());
        assertTrue(item.getBrand() == null || item.getBrand().isBlank() || item.getBrand().equalsIgnoreCase("Not Specified"),
                "prose must not remain in the brand field, was: " + item.getBrand());
    }

    @Test
    @DisplayName("A genuine brand name is preserved and not treated as prose")
    void genuineBrandNamesAreKept() {
        for (String brand : List.of("Dell", "SKF", "Dell / HP / Lenovo", "Larsen & Toubro", "Tata Steel")) {
            EmailData email = EmailData.builder()
                    .messageId("MSG-BRAND-" + brand.hashCode())
                    .senderEmail("buyer@test.com")
                    .subject("Laptop")
                    .body("Quantity: 10 units. Delivery Location: Bangalore")
                    .build();

            ExtractedRFQ aiResult = ExtractedRFQ.builder()
                    .buyerEmail("buyer@test.com")
                    .deliveryLocation("Bangalore")
                    .deliveryDate("2026-08-25")
                    .items(List.of(RFQItem.builder()
                            .itemDescription("Laptop")
                            .brand(brand)
                            .specification("16 GB RAM, 512 GB SSD")
                            .quantity(10.0)
                            .build()))
                    .build();

            Mockito.reset(rfqBuilderService);
            RFQItem item = runAndCaptureBuilderInput(email, aiResult).getItems().get(0);
            assertEquals(brand, item.getBrand(), "genuine brand '" + brand + "' must be preserved");
            assertEquals("16 GB RAM, 512 GB SSD", item.getSpecification());
        }
    }

    @Test
    @DisplayName("Non-brand text is discarded when a specification already exists")
    void proseInBrandIsDiscardedWhenSpecificationAlreadyPresent() {
        EmailData email = EmailData.builder()
                .messageId("MSG-BOTH")
                .senderEmail("buyer@test.com")
                .subject("Laptop")
                .body("Quantity: 10 units. Delivery Location: Bangalore")
                .build();

        ExtractedRFQ aiResult = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .deliveryLocation("Bangalore")
                .deliveryDate("2026-08-25")
                .items(List.of(RFQItem.builder()
                        .itemDescription("Laptop")
                        .brand("new laptops with 16 GB RAM and 512 GB SSD")
                        .specification("Intel Core i5, 15.6-inch Full HD display")
                        .quantity(10.0)
                        .build()))
                .build();

        RFQItem item = runAndCaptureBuilderInput(email, aiResult).getItems().get(0);
        assertEquals("Intel Core i5, 15.6-inch Full HD display", item.getSpecification(),
                "an existing specification must not be overwritten");
        assertTrue(item.getBrand() == null || item.getBrand().isBlank() || item.getBrand().equalsIgnoreCase("Not Specified"),
                "prose must be dropped from brand, was: " + item.getBrand());
    }
}
