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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

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

        assertEquals("rfq@procucev.com", sentMsg.getFrom(), "Sender must be rfq@procucev.com");
        assertNotNull(sentMsg.getTo());
        assertEquals("buyer@test.com", sentMsg.getTo()[0]);
        assertNotNull(sentMsg.getCc());
        assertEquals("support@procucev.com", sentMsg.getCc()[0]);
        assertNotEquals("notification@procucev.com", sentMsg.getFrom());

        assertEquals("🚀 Your RFQ #RFQ-100 is Live — Suppliers Notified!", sentMsg.getSubject());
        assertTrue(sentMsg.getText().contains("Hi John,"));
        assertTrue(sentMsg.getText().contains("converted into RFQ #RFQ-100"));
        assertTrue(sentMsg.getText().contains("Team Procucev"));
    }

    @Test
    @DisplayName("Case 2: Unregistered buyer -> CASE 2 Template sent to govardhan.kilari@procucev.com")
    void testCase2UnregisteredBuyerAcknowledgement() {
        service.sendUnregisteredBuyerAcknowledgement("unregistered@test.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        SimpleMailMessage sentMsg = captor.getValue();

        assertEquals("rfq@procucev.com", sentMsg.getFrom(), "Sender must be rfq@procucev.com");
        assertEquals("govardhan.kilari@procucev.com", sentMsg.getTo()[0]);
        assertNotNull(sentMsg.getCc());
        assertEquals("support@procucev.com", sentMsg.getCc()[0]);
        assertNotEquals("notification@procucev.com", sentMsg.getFrom());

        assertEquals("🚀 Almost There! Register to Get Your RFQ Live", sentMsg.getSubject());
        assertTrue(sentMsg.getText().contains("Hi there,"));
        assertTrue(sentMsg.getText().contains("procucev.com/get-my-quote/"));
        assertTrue(sentMsg.getText().contains("Team Procucev"));
    }

    @Test
    @DisplayName("Case 3: Registered buyer + missing details -> CASE 3 Template sent to govardhan.kilari@procucev.com")
    void testCase3DetailsMissingAcknowledgement() {
        Buyer buyer = Buyer.builder().email("buyer@test.com").name("Jane").build();

        service.sendCase3DetailsMissingAcknowledgement("buyer@test.com", "Jane");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        SimpleMailMessage sentMsg = captor.getValue();

        assertEquals("rfq@procucev.com", sentMsg.getFrom(), "Sender must be rfq@procucev.com");
        assertEquals("govardhan.kilari@procucev.com", sentMsg.getTo()[0]);
        assertNotNull(sentMsg.getCc());
        assertEquals("support@procucev.com", sentMsg.getCc()[0]);
        assertNotEquals("notification@procucev.com", sentMsg.getFrom());

        assertEquals("⚡ One Quick Detail Needed to Process Your RFQ", sentMsg.getSubject());
        assertTrue(sentMsg.getText().contains("Hi Jane,"));
        assertTrue(sentMsg.getText().contains("• Quantity required"));
        assertTrue(sentMsg.getText().contains("Team Procucev"));
    }

    @Test
    @DisplayName("Test sendSuccessAcknowledgement invalid scenarios")
    void testSendSuccessAcknowledgementInvalid() {
        assertDoesNotThrow(() -> service.sendSuccessAcknowledgement((RFQEntity) null, null));

        RFQEntity entity = RFQEntity.builder().rfqNumber("RFQ-101").buyerEmail("invalidemail").build();
        service.sendSuccessAcknowledgement(entity, null);
        Mockito.verify(mailSender, Mockito.never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendFailureAcknowledgement sends failure template to govardhan.kilari@procucev.com")
    void testSendFailureAcknowledgementSuccess() {
        FailedRfqRequest req = FailedRfqRequest.builder()
                .buyerEmail("buyer@test.com")
                .description("Desc")
                .build();

        Buyer buyer = Buyer.builder().email("buyer@test.com").name("Jane").build();

        service.sendFailureAcknowledgement(req, buyer);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        SimpleMailMessage sentMsg = captor.getValue();

        assertEquals("rfq@procucev.com", sentMsg.getFrom());
        assertEquals("govardhan.kilari@procucev.com", sentMsg.getTo()[0]);
        assertEquals("support@procucev.com", sentMsg.getCc()[0]);
        assertEquals("⚠️ We Could Not Process Your RFQ", sentMsg.getSubject());
    }

    @Test
    @DisplayName("Test sendConsolidatedAcknowledgement partial success: success to buyer, failure to govardhan")
    void testSendConsolidatedAcknowledgementPartialSuccess() {
        RFQEntity entity = RFQEntity.builder()
                .rfqNumber("RFQ-201")
                .buyerEmail("buyer@test.com")
                .build();
        Buyer buyer = Buyer.builder().email("buyer@test.com").name("Alice").build();
        List<String> failedItems = List.of("Group (Hardware) | Reason: RFQ Creation error: Server error");

        service.sendConsolidatedAcknowledgement(List.of(entity), failedItems, buyer, "Subject");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender, Mockito.times(2)).send(captor.capture());
        List<SimpleMailMessage> sentMessages = captor.getAllValues();

        // Message 1: Success to buyer
        assertEquals("buyer@test.com", sentMessages.get(0).getTo()[0]);
        assertTrue(sentMessages.get(0).getSubject().contains("RFQ #RFQ-201"));

        // Message 2: Failure alert to govardhan.kilari@procucev.com
        assertEquals("govardhan.kilari@procucev.com", sentMessages.get(1).getTo()[0]);
        assertEquals("⚠️ Some RFQs Were Created, Some Need Attention", sentMessages.get(1).getSubject());
    }

    @Test
    @DisplayName("Test sendDuplicateEmailAcknowledgement sends email to buyer with CC to support")
    void testSendDuplicateEmailAcknowledgement() {
        service.sendDuplicateEmailAcknowledgement("buyer@test.com", "Duplicate Subject");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        SimpleMailMessage sentMsg = captor.getValue();

        assertEquals("rfq@procucev.com", sentMsg.getFrom());
        assertEquals("buyer@test.com", sentMsg.getTo()[0]);
        assertNotNull(sentMsg.getCc());
        assertEquals("support@procucev.com", sentMsg.getCc()[0]);
        assertTrue(sentMsg.getSubject().contains("Duplicate Request Received"));
    }
}
