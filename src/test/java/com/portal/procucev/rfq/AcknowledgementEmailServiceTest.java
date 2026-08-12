package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.dto.FailedRfqRequest;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.service.AcknowledgementEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    @DisplayName("Test sendSuccessAcknowledgement with valid entity and buyer")
    void testSendSuccessAcknowledgementSuccess() {
        RFQEntity entity = RFQEntity.builder()
                .rfqNumber("RFQ-100")
                .buyerEmail("buyer@test.com")
                .deliveryDate("2026-08-25")
                .deliveryLocation("Bangalore")
                .itemsJson("[{\"itemDescription\":\"Laptop\",\"quantity\":2.0,\"uom\":\"NOS\",\"category\":\"IT\"}]")
                .build();

        Buyer buyer = Buyer.builder().email("buyer@test.com").name("John").build();

        service.sendSuccessAcknowledgement(entity, buyer);
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendSuccessAcknowledgement invalid scenarios")
    void testSendSuccessAcknowledgementInvalid() {
        // Null entity
        assertDoesNotThrow(() -> service.sendSuccessAcknowledgement(null, null));

        // Invalid buyer email
        RFQEntity entity = RFQEntity.builder().rfqNumber("RFQ-101").buyerEmail("invalidemail").build();
        service.sendSuccessAcknowledgement(entity, null);
        Mockito.verify(mailSender, Mockito.never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendSuccessAcknowledgement mailSender exception handling")
    void testSendSuccessAcknowledgementMailSenderException() {
        RFQEntity entity = RFQEntity.builder().rfqNumber("RFQ-102").buyerEmail("buyer@test.com").build();
        Mockito.doThrow(new RuntimeException("SMTP Error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> service.sendSuccessAcknowledgement(entity, null));
    }

    @Test
    @DisplayName("Test sendFailureAcknowledgement with valid request and buyer")
    void testSendFailureAcknowledgementSuccess() {
        FailedRfqRequest req = FailedRfqRequest.builder()
                .buyerEmail("buyer@test.com")
                .rawSubject("Subject")
                .description("Desc")
                .quantity("10")
                .uom("NOS")
                .deliveryLocation("Location")
                .deliveryDate("2026-08-25")
                .reasonForFailure("Failure reason")
                .build();

        Buyer buyer = Buyer.builder().email("buyer@test.com").name("Jane").build();

        service.sendFailureAcknowledgement(req, buyer);
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendFailureAcknowledgement invalid scenarios")
    void testSendFailureAcknowledgementInvalid() {
        assertDoesNotThrow(() -> service.sendFailureAcknowledgement(null, null));

        FailedRfqRequest req = FailedRfqRequest.builder().buyerEmail("").build();
        service.sendFailureAcknowledgement(req, null);
        Mockito.verify(mailSender, Mockito.never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendMissingQuantityAcknowledgement")
    void testSendMissingQuantityAcknowledgement() {
        service.sendMissingQuantityAcknowledgement("buyer@test.com", "Buyer Name", List.of("Laptop", "Monitor"));
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));

        // Invalid email
        service.sendMissingQuantityAcknowledgement("invalidemail", null, null);
    }

    @Test
    @DisplayName("Test sendUnregisteredBuyerAcknowledgement")
    void testSendUnregisteredBuyerAcknowledgement() {
        service.sendUnregisteredBuyerAcknowledgement("unregistered@test.com");
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));

        // Invalid email
        service.sendUnregisteredBuyerAcknowledgement("");
    }

    @Test
    @DisplayName("Test sendSuccessAcknowledgement with invalid itemsJson and null item fields")
    void testSendSuccessAcknowledgementInvalidJson() {
        RFQEntity entity1 = RFQEntity.builder()
                .rfqNumber("RFQ-103")
                .buyerEmail("buyer@test.com")
                .itemsJson("INVALID_JSON")
                .build();
        service.sendSuccessAcknowledgement(entity1, null);

        RFQEntity entity2 = RFQEntity.builder()
                .rfqNumber("RFQ-104")
                .buyerEmail("buyer@test.com")
                .itemsJson("[{\"itemDescription\":null,\"quantity\":null,\"uom\":null,\"category\":null}]")
                .build();
        service.sendSuccessAcknowledgement(entity2, Buyer.builder().email("buyer@test.com").name("").build());
    }

    @Test
    @DisplayName("Test sendMissingQuantityAcknowledgement with null or empty missing list")
    void testSendMissingQuantityAcknowledgementNullList() {
        service.sendMissingQuantityAcknowledgement("buyer@test.com", null, null);
        service.sendMissingQuantityAcknowledgement("buyer@test.com", "Buyer", List.of());
    }

    @Test
    @DisplayName("Test sendFailureAcknowledgement mail exception handling")
    void testSendFailureAcknowledgementMailException() {
        Mockito.doThrow(new RuntimeException("SMTP Error")).when(mailSender).send(any(SimpleMailMessage.class));

        FailedRfqRequest req = FailedRfqRequest.builder()
                .buyerEmail("buyer@test.com")
                .description("Desc")
                .quantity("1")
                .uom("NOS")
                .deliveryLocation("L")
                .deliveryDate("D")
                .build();

        assertDoesNotThrow(() -> service.sendFailureAcknowledgement(req, null));
    }

    @Test
    @DisplayName("Test sendSuccessAcknowledgement with buyer email blank falls back to entity email")
    void testSendSuccessAcknowledgementBuyerEmailFallback() {
        RFQEntity entity = RFQEntity.builder()
                .rfqNumber("RFQ-200")
                .buyerEmail("entity@test.com")
                .itemsJson("[]")
                .build();

        Buyer buyer = Buyer.builder().email("").name("Test").build();

        service.sendSuccessAcknowledgement(entity, buyer);
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendSuccessAcknowledgement with null buyer falls back to entity email")
    void testSendSuccessAcknowledgementNullBuyer() {
        RFQEntity entity = RFQEntity.builder()
                .rfqNumber("RFQ-201")
                .buyerEmail("entity@test.com")
                .deliveryDate(null)
                .deliveryLocation(null)
                .itemsJson("[]")
                .build();

        service.sendSuccessAcknowledgement(entity, null);
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendFailureAcknowledgement with null rawSubject and null reasonForFailure")
    void testSendFailureAcknowledgementNullFields() {
        FailedRfqRequest req = FailedRfqRequest.builder()
                .buyerEmail("buyer@test.com")
                .rawSubject(null)
                .reasonForFailure(null)
                .description("Desc")
                .quantity("1")
                .uom("NOS")
                .deliveryLocation("L")
                .deliveryDate("D")
                .build();

        service.sendFailureAcknowledgement(req, Buyer.builder().email("").name("").build());
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendUnregisteredBuyerAcknowledgement mail exception handling")
    void testSendUnregisteredBuyerAcknowledgementException() {
        Mockito.doThrow(new RuntimeException("SMTP")).when(mailSender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> service.sendUnregisteredBuyerAcknowledgement("buyer@test.com"));
    }

    @Test
    @DisplayName("Test sendMissingQuantityAcknowledgement mail exception handling")
    void testSendMissingQuantityAcknowledgementException() {
        Mockito.doThrow(new RuntimeException("SMTP")).when(mailSender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> service.sendMissingQuantityAcknowledgement("buyer@test.com", "Name", List.of("Item1")));
    }

    @Test
    @DisplayName("Test sendSuccessAcknowledgement with null itemsJson")
    void testSendSuccessAcknowledgementNullItemsJson() {
        RFQEntity entity = RFQEntity.builder()
                .rfqNumber("RFQ-202")
                .buyerEmail("buyer@test.com")
                .itemsJson(null)
                .build();

        service.sendSuccessAcknowledgement(entity, null);
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendUnregisteredBuyerAcknowledgement with null email")
    void testSendUnregisteredBuyerAcknowledgementNull() {
        service.sendUnregisteredBuyerAcknowledgement(null);
        Mockito.verify(mailSender, Mockito.never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendFailureAcknowledgement buyer email from buyer object")
    void testSendFailureAcknowledgementBuyerEmailFromBuyer() {
        FailedRfqRequest req = FailedRfqRequest.builder()
                .buyerEmail("")
                .description("D").quantity("1").uom("U").deliveryLocation("L").deliveryDate("D")
                .build();

        Buyer buyer = Buyer.builder().email("from-buyer@test.com").name("BuyerName").build();
        service.sendFailureAcknowledgement(req, buyer);
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendSuccessAcknowledgement with multiple items having null fields")
    void testSendSuccessAcknowledgementMultipleItemsWithNulls() {
        RFQEntity entity = RFQEntity.builder()
                .rfqNumber("RFQ-203")
                .buyerEmail("buyer@test.com")
                .deliveryDate("2026-08-25")
                .deliveryLocation("Location")
                .itemsJson("[{\"itemDescription\":\"A\",\"quantity\":2.0,\"uom\":\"NOS\",\"category\":\"IT\"},{\"itemDescription\":null,\"quantity\":null,\"uom\":null,\"category\":null}]")
                .build();

        service.sendSuccessAcknowledgement(entity, Buyer.builder().email("buyer@test.com").name("Test").build());
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));
    }
}

