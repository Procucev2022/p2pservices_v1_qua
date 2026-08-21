package com.portal.procucev.rfq;

import com.portal.procucev.rfq.dto.FailedRfqRequest;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.service.AcknowledgementEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AcknowledgementEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private AcknowledgementEmailService service;

    @BeforeEach
    void setUp() {
        service = new AcknowledgementEmailService(mailSender, new com.fasterxml.jackson.databind.ObjectMapper());
        ReflectionTestUtils.setField(service, "mailFrom", "rfq@procucev.com");
        ReflectionTestUtils.setField(service, "mailCc", "support@procucev.com");
        ReflectionTestUtils.setField(service, "failureTo", "govardhan.kilari@procucev.com");
    }

    @Test
    @DisplayName("Case 1: RFQ live -> Success email sent to buyer with CC support@procucev.com")
    void testCase1SuccessAcknowledgement() {
        RFQEntity rfqEntity = RFQEntity.builder()
                .rfqNumber("RFQ-2026-001")
                .buyerEmail("buyer@test.com")
                .build();
        Buyer buyer = Buyer.builder()
                .email("buyer@test.com")
                .name("John Doe")
                .build();

        service.sendSuccessAcknowledgement(rfqEntity, buyer);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        SimpleMailMessage sentMsg = captor.getValue();

        assertEquals("rfq@procucev.com", sentMsg.getFrom(), "Sender must be rfq@procucev.com");
        assertEquals("buyer@test.com", sentMsg.getTo()[0]);
        assertNotNull(sentMsg.getCc());
        assertEquals("support@procucev.com", sentMsg.getCc()[0]);
        assertNotEquals("notification@procucev.com", sentMsg.getFrom());
        assertTrue(sentMsg.getSubject().contains("RFQ #RFQ-2026-001 is Live"));
        assertTrue(sentMsg.getText().contains("John Doe"));
    }

    @Test
    @DisplayName("Case 2: Unregistered buyer -> Failure email sent to govardhan.kilari@procucev.com")
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
    }

    @Test
    @DisplayName("Case 3: Registered buyer + missing details -> CASE 3 Template sent to govardhan.kilari@procucev.com")
    void testCase3DetailsMissingAcknowledgement() {
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
    @DisplayName("Invalid buyer email -> No email sent for success acknowledgement")
    void testSendSuccessAcknowledgementInvalidBuyerEmail() {
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
    @DisplayName("Test sendDuplicateEmailAcknowledgement sends email to govardhan.kilari@procucev.com with CC to support")
    void testSendDuplicateEmailAcknowledgement() {
        service.sendDuplicateEmailAcknowledgement("buyer@test.com", "Duplicate Subject");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        SimpleMailMessage sentMsg = captor.getValue();

        assertEquals("rfq@procucev.com", sentMsg.getFrom());
        assertEquals("govardhan.kilari@procucev.com", sentMsg.getTo()[0]);
        assertEquals("support@procucev.com", sentMsg.getCc()[0]);
        assertTrue(sentMsg.getSubject().contains("Duplicate Request Received"));
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
}
