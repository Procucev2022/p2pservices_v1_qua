package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.entity.EmailTransaction;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.service.EmailProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Cover for the production incident of 18-Aug-2026 10:53 UTC.
 *
 * <p>A buyer sent a fully specified requirement for a Laptop, quantity 25. The log recorded
 * {@code Item='MS Hex Bolt M10 x 50 mm'} with {@code Specification='M10 x 50 mm, Zinc Plated'} -
 * the product from an unrelated earlier RFQ in the same Gmail thread. The extraction had returned a
 * blank description, and the thread merge then filled it from the previous message, retargeting the
 * RFQ at the wrong product entirely. Had a quantity been recovered, the buyer would have received
 * an RFQ for bolts instead of a laptop.
 */
public class ThreadContextIsolationTest {

    private static final String PRIOR_MESSAGE_ID = "<prior-hex-bolts@mail.gmail.com>";

    private EmailTransactionRepository emailTransactionRepository;
    private ObjectMapper objectMapper;
    private EmailProcessorService service;

    @BeforeEach
    void setUp() throws Exception {
        emailTransactionRepository = Mockito.mock(EmailTransactionRepository.class);
        objectMapper = new ObjectMapper();

        service = new EmailProcessorService(
                Mockito.mock(com.portal.procucev.rfq.service.EmailReaderService.class),
                Mockito.mock(com.portal.procucev.rfq.service.AIExtractionService.class),
                Mockito.mock(com.portal.procucev.rfq.service.ValidationService.class),
                Mockito.mock(com.portal.procucev.rfq.service.BuyerVerificationService.class),
                Mockito.mock(com.portal.procucev.rfq.service.RFQBuilderService.class),
                Mockito.mock(com.portal.procucev.rfq.service.RFQApiService.class),
                Mockito.mock(com.portal.procucev.rfq.service.CategoryClassificationService.class),
                Mockito.mock(com.portal.procucev.rfq.service.AcknowledgementEmailService.class),
                Mockito.mock(com.portal.procucev.rfq.repository.RFQRepository.class),
                emailTransactionRepository,
                Mockito.mock(com.portal.procucev.rfq.repository.RfqItemRecordRepository.class),
                new com.portal.procucev.rfq.parser.DateParser(),
                objectMapper);

        // The earlier RFQ in the same thread: MS Hex Bolts.
        ExtractedRFQ historical = ExtractedRFQ.builder()
                .buyerEmail("govardhan.kilari@procucev.com")
                .deliveryLocation("Hyderabad")
                .items(List.of(RFQItem.builder()
                        .itemDescription("MS Hex Bolt M10 x 50 mm")
                        .specification("M10 x 50 mm, Zinc Plated, New, genuine and rust-free")
                        .brand("TVS")
                        .category("Fasteners")
                        .build()))
                .build();

        EmailTransaction prior = EmailTransaction.builder()
                .messageId(PRIOR_MESSAGE_ID)
                .extractionJson(objectMapper.writeValueAsString(historical))
                .build();
        Mockito.when(emailTransactionRepository.findByMessageId(anyString())).thenReturn(Optional.of(prior));
    }

    private ExtractedRFQ merge(EmailData email, ExtractedRFQ extracted) {
        return ReflectionTestUtils.invokeMethod(service, "mergeThreadContext", email, extracted);
    }

    /** What the model returned for the Laptop email: uom read, everything else dropped. */
    private static ExtractedRFQ blankExtraction() {
        RFQItem item = new RFQItem();
        item.setUom("Nos");
        return ExtractedRFQ.builder()
                .buyerEmail("govardhan.kilari@procucev.com")
                .items(new ArrayList<>(List.of(item)))
                .build();
    }

    @Test
    @DisplayName("A new requirement sent inside an old thread never inherits the old product")
    void newRequirementInOldThreadKeepsItsOwnProduct() {
        EmailData email = EmailData.builder()
                .messageId("<CADmWLxipjSd4HYrxKdyp3QayB_ydniXVG=WnurmWgV2XN1R8TQ@mail.gmail.com>")
                .subject("(No Subject)")
                .senderEmail("govardhan.kilari@procucev.com")
                .references(PRIOR_MESSAGE_ID)
                .body("Remarks: Original branded material, warranty certificate required.\n"
                        + "Description: Laptop\nQuantity: 25\nUOM: Nos\nLocation: Bengaluru\n"
                        + "Specification: Intel Core i5, 16 GB RAM, 512 GB SSD, 15.6-inch FHD, Windows 11\n"
                        + "State: Karnataka\nPincode: 560001\nCity: Bengaluru\n")
                .build();

        ExtractedRFQ merged = merge(email, blankExtraction());

        assertNotNull(merged);
        RFQItem item = merged.getItems().get(0);
        assertEquals("Laptop", item.getItemDescription(),
                "the product named in THIS email must win over the thread's previous product");
        assertNull(item.getSpecification(), "a different product's specification must not be carried over");
        assertNull(item.getBrand(), "a different product's brand must not be carried over");
        assertNull(item.getCategory(), "a different product's category must not be carried over");
    }

    @Test
    @DisplayName("A reply that only supplies the missing quantity still inherits the item under discussion")
    void clarificationReplyStillInheritsTheItem() {
        EmailData reply = EmailData.builder()
                .messageId("<reply@mail.gmail.com>")
                .subject("RE: One Quick Detail Needed to Process Your RFQ")
                .senderEmail("govardhan.kilari@procucev.com")
                .inReplyTo(PRIOR_MESSAGE_ID)
                .body("Sorry for the miss. The quantity is 500 Nos. Please proceed.")
                .build();

        ExtractedRFQ merged = merge(reply, blankExtraction());

        RFQItem item = merged.getItems().get(0);
        assertEquals("MS Hex Bolt M10 x 50 mm", item.getItemDescription(),
                "a reply naming no product must still resolve to the item being discussed");
        assertEquals("M10 x 50 mm, Zinc Plated, New, genuine and rust-free", item.getSpecification());
        assertEquals("TVS", item.getBrand());
        assertEquals("Fasteners", item.getCategory());
        assertEquals("Hyderabad", merged.getDeliveryLocation());
    }

    @Test
    @DisplayName("A description the model already read is never replaced by thread history")
    void modelSuppliedDescriptionIsNeverReplaced() {
        EmailData email = EmailData.builder()
                .messageId("<new@mail.gmail.com>")
                .senderEmail("govardhan.kilari@procucev.com")
                .references(PRIOR_MESSAGE_ID)
                .body("Description: Laptop\nQuantity: 25\nUOM: Nos\n")
                .build();

        RFQItem item = RFQItem.builder().itemDescription("Laptop").uom("Nos").build();
        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .items(new ArrayList<>(List.of(item)))
                .build();

        ExtractedRFQ merged = merge(email, extracted);
        assertEquals("Laptop", merged.getItems().get(0).getItemDescription());
    }

    @Test
    @DisplayName("An email with no thread headers is returned untouched")
    void emailWithNoThreadHeadersIsUntouched() {
        EmailData standalone = EmailData.builder()
                .messageId("<standalone@mail.gmail.com>")
                .senderEmail("govardhan.kilari@procucev.com")
                .body("Description: Laptop\nQuantity: 25\n")
                .build();

        ExtractedRFQ merged = merge(standalone, blankExtraction());
        assertNull(merged.getItems().get(0).getItemDescription());
        Mockito.verify(emailTransactionRepository, Mockito.never()).findByMessageId(anyString());
    }
}
