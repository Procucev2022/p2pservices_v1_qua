package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.dto.FailedRfqRequest;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.service.AcknowledgementEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

public class AcknowledgementEmailServiceTest {

    private JavaMailSender mailSender;
    private ObjectMapper objectMapper;
    private AcknowledgementEmailService service;

    @BeforeEach
    void setUp() {
        mailSender = Mockito.mock(JavaMailSender.class);
        objectMapper = new ObjectMapper();
        service = new AcknowledgementEmailService(mailSender, objectMapper);
    }

    @Test
    @DisplayName("Case 1: Registered buyer + valid RFQ -> CASE 1 Template")
    void testCase1SuccessAcknowledgement() {
        RFQEntity entity = RFQEntity.builder()
                .rfqNumber("RFQ-100")
                .buyerEmail("buyer@test.com")
                .deliveryDate("2026-08-25")
                .deliveryLocation("Bangalore")
                .itemsJson("[{\"itemDescription\":\"Laptop\",\"quantity\":2.0,\"uom\":\"NOS\",\"category\":\"IT\"}]")
                .build();

        Buyer buyer = Buyer.builder().email("buyer@test.com").name("John").build();

        service.sendSuccessAcknowledgement(entity, buyer);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        SimpleMailMessage sentMsg = captor.getValue();

        assertEquals("rfq@procucev.com", sentMsg.getFrom());
        assertEquals("buyer@test.com", sentMsg.getTo()[0]);
        assertEquals("support@procucev.com", sentMsg.getCc()[0]);
        assertEquals("🚀 Your RFQ #RFQ-100 is Live — Suppliers Notified!", sentMsg.getSubject());
        assertTrue(sentMsg.getText().contains("Hi John,"));
    }

    @Test
    void testCase1MultipleRfqsSuccessAcknowledgement() {
        RFQEntity e1 = RFQEntity.builder().rfqNumber("RFQ-101").buyerEmail("buyer@test.com").build();
        RFQEntity e2 = RFQEntity.builder().rfqNumber("RFQ-102").buyerEmail("buyer@test.com").build();

        service.sendSuccessAcknowledgement(List.of(e1, e2), null);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        assertEquals("🚀 Your RFQs are Live — Suppliers Notified!", captor.getValue().getSubject());
    }

    @Test
    @DisplayName("Case 2: Unregistered buyer -> CASE 2 Template")
    void testCase2UnregisteredBuyerAcknowledgement() {
        service.sendUnregisteredBuyerAcknowledgement("unregistered@test.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        SimpleMailMessage sentMsg = captor.getValue();

        assertEquals("rfq@procucev.com", sentMsg.getFrom());
        assertEquals("unregistered@test.com", sentMsg.getTo()[0]);
        assertEquals("🚀 Almost There! Register to Get Your RFQ Live", sentMsg.getSubject());
    }

    @Test
    @DisplayName("Case 3: Registered buyer + missing details -> CASE 3 Template")
    void testCase3DetailsMissingAcknowledgement() {
        service.sendCase3DetailsMissingAcknowledgement("buyer@test.com", "Jane");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        SimpleMailMessage sentMsg = captor.getValue();

        assertEquals("rfq@procucev.com", sentMsg.getFrom());
        assertEquals("buyer@test.com", sentMsg.getTo()[0]);
        assertEquals("⚡ One Quick Detail Needed to Process Your RFQ", sentMsg.getSubject());
    }

    @Test
    void testCase3WithExtractedFailedItemsAndMaxCap() {
        List<String> failedItems = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            failedItems.add("Item: Item #" + i + " | Reason: Quantity missing");
        }
        failedItems.add("Delivery location is missing");

        service.sendCase3DetailsMissingAcknowledgement("buyer@test.com", "Jane", failedItems);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        String text = captor.getValue().getText();
        assertTrue(text.contains("Quantity is missing for 20 items:"));
        assertTrue(text.contains("and 5 more."));
    }

    @Test
    void testSendMissingQuantityAcknowledgement() {
        assertDoesNotThrow(() -> service.sendMissingQuantityAcknowledgement("buyer@test.com", "Jane", List.of("Item 1")));
    }

    @Test
    void testSendConsolidatedAcknowledgement() {
        RFQEntity entity = RFQEntity.builder().rfqNumber("RFQ-200").buyerEmail("buyer@test.com").build();
        Buyer buyer = Buyer.builder().email("buyer@test.com").name("Buyer").build();

        // 1. Success case
        service.sendConsolidatedAcknowledgement(List.of(entity), null, buyer, "Subject");
        // 2. Partial success case
        service.sendConsolidatedAcknowledgement(List.of(entity), List.of("Failed Item"), buyer, "Subject");
        // 3. Complete failure case
        service.sendConsolidatedAcknowledgement(null, List.of("Failed Item"), buyer, "Subject");
    }

    @Test
    void testSendFailureAcknowledgement() {
        FailedRfqRequest req1 = FailedRfqRequest.builder().buyerEmail("buyer@test.com").reasonForFailure("Reason").build();
        service.sendFailureAcknowledgement(req1, null);

        FailedRfqRequest req2 = FailedRfqRequest.builder().buyerEmail("buyer@test.com").build();
        service.sendFailureAcknowledgement(req2, null);
    }

    @Test
    void testInvalidEmailsAndExceptions() {
        assertDoesNotThrow(() -> service.sendSuccessAcknowledgement((List<RFQEntity>) null, null));
        assertDoesNotThrow(() -> service.sendSuccessAcknowledgement(List.of(RFQEntity.builder().buyerEmail("invalid").build()), null));
        assertDoesNotThrow(() -> service.sendUnregisteredBuyerAcknowledgement("invalid"));
        assertDoesNotThrow(() -> service.sendCase3DetailsMissingAcknowledgement("invalid", "Name"));

        doThrow(new RuntimeException("Mail error")).when(mailSender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> service.sendUnregisteredBuyerAcknowledgement("buyer@test.com"));
        assertDoesNotThrow(() -> service.sendCase3DetailsMissingAcknowledgement("buyer@test.com", "Jane"));
    }
}
