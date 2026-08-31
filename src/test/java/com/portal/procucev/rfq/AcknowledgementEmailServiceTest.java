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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
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
        RFQEntity entity1 = RFQEntity.builder()
                .rfqNumber("RFQ-100")
                .buyerEmail("buyer@test.com")
                .deliveryDate("2026-08-25")
                .deliveryLocation("Bangalore")
                .itemsJson("[{\"itemDescription\":\"Laptop\",\"quantity\":2.0,\"uom\":\"NOS\",\"category\":\"IT\"}]")
                .build();

        RFQEntity entity2 = RFQEntity.builder()
                .rfqNumber("RFQ-101")
                .buyerEmail("buyer@test.com")
                .build();

        Buyer buyer = Buyer.builder().email("buyer@test.com").name("John").build();

        // Single entity overload
        service.sendSuccessAcknowledgement(entity1, buyer);

        // Multi-entity
        service.sendSuccessAcknowledgement(List.of(entity1, entity2), buyer);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender, Mockito.times(2)).send(captor.capture());
        SimpleMailMessage sentMsg = captor.getAllValues().get(0);

        assertEquals("rfq@procucev.com", sentMsg.getFrom());
        assertEquals("buyer@test.com", sentMsg.getTo()[0]);
        assertEquals("support@procucev.com", sentMsg.getCc()[0]);
        assertEquals("🚀 Your RFQ #RFQ-100 is Live — Suppliers Notified!", sentMsg.getSubject());
        assertTrue(sentMsg.getText().contains("Hi John,"));

        SimpleMailMessage multiMsg = captor.getAllValues().get(1);
        assertEquals("🚀 Your RFQs are Live — Suppliers Notified!", multiMsg.getSubject());
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

        assertEquals("🚀 Almost There! Register to Get Your RFQ Live", sentMsg.getSubject());
        assertTrue(sentMsg.getText().contains("Hi there,"));
        assertTrue(sentMsg.getText().contains("procucev.com/get-my-quote/"));

        assertNotNull(service.getCase2Body());
    }

    @Test
    @DisplayName("Case 3: Registered buyer + missing details -> CASE 3 Template sent to govardhan.kilari@procucev.com")
    void testCase3DetailsMissingAcknowledgement() {
        Buyer buyer = Buyer.builder().email("buyer@test.com").name("Jane").build();

        service.sendCase3DetailsMissingAcknowledgement("buyer@test.com", "Jane");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        SimpleMailMessage sentMsg = captor.getValue();

        assertEquals("rfq@procucev.com", sentMsg.getFrom());
        assertEquals("govardhan.kilari@procucev.com", sentMsg.getTo()[0]);
        assertNotNull(sentMsg.getCc());
        assertEquals("support@procucev.com", sentMsg.getCc()[0]);

        assertEquals("⚡ One Quick Detail Needed to Process Your RFQ", sentMsg.getSubject());
        assertTrue(sentMsg.getText().contains("Hi Jane,"));
        assertTrue(sentMsg.getText().contains("• Quantity required"));

        assertNotNull(service.getCase3Body("Jane"));

        // Test with many failed items (> 15)
        List<String> failedItems = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            failedItems.add("Item: Part " + i + " | Reason: Missing quantity");
        }
        failedItems.add("Group 1 | Reason: Missing location");
        String body = service.getCase3Body("Jane", failedItems);
        assertTrue(body.contains("...and 5 more"));
        assertTrue(body.contains("Delivery location"));
    }

    @Test
    @DisplayName("Test sendMissingQuantityAcknowledgement")
    void testSendMissingQuantityAcknowledgement() {
        service.sendMissingQuantityAcknowledgement("buyer@test.com", "John", List.of("Item 1"));
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendSuccessAcknowledgement invalid scenarios")
    void testSendSuccessAcknowledgementInvalid() {
        assertDoesNotThrow(() -> service.sendSuccessAcknowledgement((RFQEntity) null, null));
        assertDoesNotThrow(() -> service.sendSuccessAcknowledgement(Collections.emptyList(), null));

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
    @DisplayName("Test sendConsolidatedAcknowledgement with empty created list")
    void testSendConsolidatedAcknowledgementEmptyCreated() {
        Buyer buyer = Buyer.builder().email("buyer@test.com").name("Alice").build();
        List<String> failedItems = List.of("Item: Laptop | Reason: Missing quantity");

        service.sendConsolidatedAcknowledgement(Collections.emptyList(), failedItems, buyer, "Subject");
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));

        // System error failure items
        List<String> systemFailures = List.of("Group error: DB timeout");
        service.sendConsolidatedAcknowledgement(null, systemFailures, buyer, "Subject");
        Mockito.verify(mailSender, Mockito.times(2)).send(any(SimpleMailMessage.class));
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
        assertNotNull(sentMsg.getCc());
        assertEquals("support@procucev.com", sentMsg.getCc()[0]);
        assertTrue(sentMsg.getSubject().contains("Duplicate Request Received"));
    }

    @Test
    @DisplayName("Test sendFileSizeExceededAcknowledgement sends failure alert to govardhan.kilari@procucev.com")
    void testFileSizeExceededAcknowledgement() {
        service.sendFileSizeExceededAcknowledgement("buyer@test.com", "John Doe", "drawing_huge.pdf", 26214400L);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        SimpleMailMessage sentMsg = captor.getValue();

        assertEquals("rfq@procucev.com", sentMsg.getFrom());
        assertEquals("govardhan.kilari@procucev.com", sentMsg.getTo()[0]);
        assertNotNull(sentMsg.getCc());
        assertEquals("support@procucev.com", sentMsg.getCc()[0]);
        assertTrue(sentMsg.getSubject().contains("File Size Exceeded"));
        assertTrue(sentMsg.getText().contains("drawing_huge.pdf"));
        assertTrue(sentMsg.getText().contains("25MB"));
    }

    @Test
    @DisplayName("Test exception handling when mailSender throws")
    void testMailSenderExceptionHandling() {
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        RFQEntity entity = RFQEntity.builder().rfqNumber("RFQ-1").buyerEmail("buyer@test.com").build();
        Buyer buyer = Buyer.builder().email("buyer@test.com").name("Buyer").build();

        assertDoesNotThrow(() -> service.sendSuccessAcknowledgement(entity, buyer));
        assertDoesNotThrow(() -> service.sendUnregisteredBuyerAcknowledgement("buyer@test.com"));
        assertDoesNotThrow(() -> service.sendCase3DetailsMissingAcknowledgement("buyer@test.com", "Buyer"));
        assertDoesNotThrow(() -> service.sendProcessingFailureAcknowledgement("buyer@test.com", "Buyer", "Error"));
        assertDoesNotThrow(() -> service.sendFileSizeExceededAcknowledgement("buyer@test.com", "Buyer", "file.pdf", 1000L));
        assertDoesNotThrow(() -> service.sendDuplicateEmailAcknowledgement("buyer@test.com", "Subject"));
    }

    @Test
    @DisplayName("Test resolveFailureRecipient and mailCc edge cases")
    void testRecipientAndConfigEdgeCases() {
        assertEquals("govardhan.kilari@procucev.com", service.resolveFailureRecipient());

        ReflectionTestUtils.setField(service, "failureTo", null);
        assertEquals("govardhan.kilari@procucev.com", service.resolveFailureRecipient());

        ReflectionTestUtils.setField(service, "failureTo", "invalid_no_at");
        assertEquals("govardhan.kilari@procucev.com", service.resolveFailureRecipient());

        ReflectionTestUtils.setField(service, "failureTo", "custom@procucev.com");
        assertEquals("custom@procucev.com", service.resolveFailureRecipient());

        ReflectionTestUtils.setField(service, "mailCc", null);
        assertDoesNotThrow(() -> service.sendUnregisteredBuyerAcknowledgement("unreg@test.com"));

        // sendFileSizeExceededAcknowledgement with 0 maxBytes
        service.sendFileSizeExceededAcknowledgement(null, null, null, 0L);
        Mockito.verify(mailSender, Mockito.atLeastOnce()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test sendFailureAcknowledgement and helper methods all branches")
    void testFailureAcknowledgementAndHelpers() {
        // sendPartialSuccessAcknowledgement with null email and empty failures
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "sendPartialSuccessAcknowledgement", (String) null, "Buyer", List.of(), List.of()));

        // sendFailureAcknowledgement with null buyer and null buyerEmail in request
        FailedRfqRequest req1 = FailedRfqRequest.builder().reasonForFailure("Missing quantity for: Valves").build();
        service.sendFailureAcknowledgement(req1, null);

        // sendFailureAcknowledgement with buyer
        Buyer buyer = Buyer.builder().email("buyer@corp.com").name("Corp Buyer").build();
        FailedRfqRequest req2 = FailedRfqRequest.builder().buyerEmail("buyer@corp.com").reasonForFailure("Missing location").build();
        service.sendFailureAcknowledgement(req2, buyer);

        // extractItemName
        assertNull(ReflectionTestUtils.invokeMethod(service, "extractItemName", (String) null));
        assertNull(ReflectionTestUtils.invokeMethod(service, "extractItemName", "   "));
        assertNull(ReflectionTestUtils.invokeMethod(service, "extractItemName", "Raw Item Name"));
        assertNull(ReflectionTestUtils.invokeMethod(service, "extractItemName", "Item:   "));
        assertEquals("Valves", ReflectionTestUtils.invokeMethod(service, "extractItemName", "Item: Valves | Reason: Missing quantity"));
        assertEquals("Ball Valve", ReflectionTestUtils.invokeMethod(service, "extractItemName", "Item: Ball Valve"));

        // describesMissingDetail
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(service, "describesMissingDetail", (List<?>) null));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(service, "describesMissingDetail", List.of()));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(service, "describesMissingDetail", List.of("Missing quantity for: Part A")));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(service, "describesMissingDetail", List.of("Missing location for: Part A")));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(service, "describesMissingDetail", List.of("Database Connection Error")));

        // resolveBuyerName
        assertEquals("Valued Customer", ReflectionTestUtils.invokeMethod(service, "resolveBuyerName", (Buyer) null));
        Buyer bName = Buyer.builder().name("Contact Person").build();
        assertEquals("Contact Person", ReflectionTestUtils.invokeMethod(service, "resolveBuyerName", bName));
        Buyer bNoName = Buyer.builder().email("john.doe@test.com").build();
        assertEquals("Valued Customer", ReflectionTestUtils.invokeMethod(service, "resolveBuyerName", bNoName));

        // summariseFailures
        assertEquals("We could not create the RFQ for this requirement.", ReflectionTestUtils.invokeMethod(service, "summariseFailures", (List<?>) null));
        assertEquals("We could not create the RFQ for this requirement.", ReflectionTestUtils.invokeMethod(service, "summariseFailures", List.of()));
        assertEquals("Missing quantity for item", ReflectionTestUtils.invokeMethod(service, "summariseFailures", List.of("Missing quantity for item")));
        assertEquals("F1\nF2", ReflectionTestUtils.invokeMethod(service, "summariseFailures", List.of("F1", "F2")));

        // sendConsolidatedAcknowledgement
        RFQEntity entity = RFQEntity.builder().rfqNumber("RFQ-999").buyerEmail("buyer@test.com").build();
        service.sendConsolidatedAcknowledgement(List.of(entity), List.of("Failed item 1"), buyer, "Subject");
        service.sendConsolidatedAcknowledgement(List.of(), List.of("Missing quantity for: Item X"), buyer, "Subject");
        service.sendConsolidatedAcknowledgement(List.of(), List.of("Database Timeout"), buyer, "Subject");

        // sendDuplicateEmailAcknowledgement
        service.sendDuplicateEmailAcknowledgement("buyer@test.com", "RFQ Subject");
        service.sendDuplicateEmailAcknowledgement(null, null);

        // Invalid buyer email in sendSuccessAcknowledgement
        RFQEntity invalidEntity = RFQEntity.builder().rfqNumber("RFQ-000").buyerEmail("invalid-email-address").build();
        service.sendSuccessAcknowledgement(List.of(invalidEntity), null);

        // MailSender throwing exception
        Mockito.doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> service.sendSuccessAcknowledgement(List.of(entity), buyer));
        assertDoesNotThrow(() -> service.sendDuplicateEmailAcknowledgement("buyer@test.com", "RFQ Subject"));
        assertDoesNotThrow(() -> service.sendFileSizeExceededAcknowledgement("buyer@test.com", "Name", "file.pdf", 1000L));

        // Remaining branch coverage: null rfqEntities, partial success edge cases, getCase3Body truncation
        service.sendSuccessAcknowledgement((List<RFQEntity>) null, buyer);
        service.sendSuccessAcknowledgement(List.of(), buyer);
        service.sendSuccessAcknowledgement((RFQEntity) null, buyer);
        service.sendFailureAcknowledgement(null, null);
        service.sendFailureAcknowledgement(FailedRfqRequest.builder().reasonForFailure("").build(), null);

        ReflectionTestUtils.invokeMethod(service, "sendPartialSuccessAcknowledgement", "buyer@test.com", "Buyer", null, List.of("Failed"));
        ReflectionTestUtils.invokeMethod(service, "sendPartialSuccessAcknowledgement", "invalid-email", "Buyer", List.of(entity), List.of("Failed"));
        ReflectionTestUtils.invokeMethod(service, "sendPartialSuccessAcknowledgement", (String) null, "Buyer", List.of(entity), List.of("Failed"));

        // resolveFailureRecipient fallback
        ReflectionTestUtils.setField(service, "failureTo", null);
        assertEquals("govardhan.kilari@procucev.com", service.resolveFailureRecipient());
        ReflectionTestUtils.setField(service, "failureTo", "   ");
        assertEquals("govardhan.kilari@procucev.com", service.resolveFailureRecipient());
        ReflectionTestUtils.setField(service, "failureTo", "invalid-email");
        assertEquals("govardhan.kilari@procucev.com", service.resolveFailureRecipient());

        // createBaseMailMessage with null / blank mailCc
        ReflectionTestUtils.setField(service, "mailCc", null);
        assertNotNull(ReflectionTestUtils.invokeMethod(service, "createBaseMailMessage", "test@test.com"));
        ReflectionTestUtils.setField(service, "mailCc", "  ");
        assertNotNull(ReflectionTestUtils.invokeMethod(service, "createBaseMailMessage", "test@test.com"));

        // getCase3Body item truncation with > 15 items
        List<String> manyItems = java.util.stream.IntStream.rangeClosed(1, 20)
                .mapToObj(i -> "Item: Item " + i + " | Reason: Missing quantity")
                .toList();
        String case3Body = service.getCase3Body("Buyer", manyItems);
        assertTrue(case3Body.contains("...and 5 more."));

        // Single quantity item and location bullet
        List<String> locAndQtyItems = new java.util.ArrayList<>();
        locAndQtyItems.add(null);
        locAndQtyItems.add("Item: Valve | Reason: Missing quantity");
        locAndQtyItems.add("Item: Pipe | Reason: Missing location");
        String case3Body2 = service.getCase3Body("Buyer", locAndQtyItems);
        assertTrue(case3Body2.contains("Quantity required"));
        assertTrue(case3Body2.contains("Delivery location"));

        // sendConsolidatedAcknowledgement branches
        service.sendConsolidatedAcknowledgement(List.of(entity), null, buyer, "Sub");
        service.sendConsolidatedAcknowledgement(List.of(entity), List.of("Failed item"), buyer, "Sub");
        service.sendConsolidatedAcknowledgement(null, List.of("Missing quantity on Item 1"), buyer, "Sub");
        service.sendConsolidatedAcknowledgement(null, List.of("Internal server error"), buyer, "Sub");

        // getCase1Body with multiple RFQ numbers
        String case1Multi = ReflectionTestUtils.invokeMethod(service, "getCase1Body", "Buyer", "RFQ-001, RFQ-002, RFQ-003");
        assertNotNull(case1Multi);
        assertTrue(case1Multi.contains("✉️ RFQ-001"));

        // extractItemName branches
        assertEquals("Gate Valve", ReflectionTestUtils.invokeMethod(service, "extractItemName", "Item: Gate Valve"));
        assertNull(ReflectionTestUtils.invokeMethod(service, "extractItemName", "Error without item prefix"));

        // 19. resolveBuyerName branches
        assertEquals("Valued Customer", ReflectionTestUtils.invokeMethod(service, "resolveBuyerName", (Buyer) null));
        assertEquals("Valued Customer", ReflectionTestUtils.invokeMethod(service, "resolveBuyerName", Buyer.builder().name(null).build()));
        assertEquals("Valued Customer", ReflectionTestUtils.invokeMethod(service, "resolveBuyerName", Buyer.builder().name("   ").build()));
        assertEquals("Alice", ReflectionTestUtils.invokeMethod(service, "resolveBuyerName", Buyer.builder().name(" Alice ").build()));

        // 20. describesMissingDetail branches
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(service, "describesMissingDetail", (List<String>) null));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(service, "describesMissingDetail", List.of()));
        List<String> listWithNull = new java.util.ArrayList<>();
        listWithNull.add(null);
        listWithNull.add("System timeout");
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(service, "describesMissingDetail", listWithNull));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(service, "describesMissingDetail", List.of("Missing item description")));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(service, "describesMissingDetail", List.of("Quantity not provided")));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(service, "describesMissingDetail", List.of("Delivery location missing")));

        // 21. sendSuccessAcknowledgement single and multi
        service.sendSuccessAcknowledgement(List.of(entity), null);
        RFQEntity entity2 = RFQEntity.builder().rfqNumber("RFQ-999").buyerEmail("buyer@corp.com").build();
        service.sendSuccessAcknowledgement(List.of(entity, entity2), Buyer.builder().email("buyer@corp.com").name(null).build());

        // 22. sendPartialSuccessAcknowledgement single and multi with null/empty buyerName
        ReflectionTestUtils.invokeMethod(service, "sendPartialSuccessAcknowledgement", "buyer@corp.com", null, List.of(entity), List.of("Fail 1"));
        ReflectionTestUtils.invokeMethod(service, "sendPartialSuccessAcknowledgement", "buyer@corp.com", "  ", List.of(entity, entity2), List.of("Fail 1", "Fail 2"));

        // 23. sendFileSizeExceededAcknowledgement variants
        service.sendFileSizeExceededAcknowledgement("buyer@corp.com", null, null, 10485760L);
        service.sendFileSizeExceededAcknowledgement("buyer@corp.com", "Bob", "spec.pdf", 10485760L);

        // 24. sendProcessingFailureAcknowledgement variants
        service.sendProcessingFailureAcknowledgement("buyer@corp.com", null, "Error details");
        service.sendProcessingFailureAcknowledgement("buyer@corp.com", "Bob", "Error details");

        // 25. sendDuplicateEmailAcknowledgement variants
        service.sendDuplicateEmailAcknowledgement("buyer@corp.com", null);
        service.sendDuplicateEmailAcknowledgement("buyer@corp.com", "Bob");

        // 26. sendCase3DetailsMissingAcknowledgement & sendMissingQuantityAcknowledgement
        service.sendCase3DetailsMissingAcknowledgement("buyer@corp.com", "Bob");
        service.sendCase3DetailsMissingAcknowledgement("buyer@corp.com", null, List.of("Missing spec"));
        service.sendMissingQuantityAcknowledgement("buyer@corp.com", "Bob", List.of("Item A"));

        // 27. getCase1Subject, getCase1Body, getCase2Body, getCase3Body helpers
        assertNotNull(service.getCase1Subject(null));
        assertNotNull(service.getCase1Subject("RFQ-100"));
        assertNotNull(service.getCase1Subject("RFQ-100, RFQ-200"));

        assertNotNull(service.getCase1Body(null, null));
        assertNotNull(service.getCase1Body("Bob", "RFQ-100"));

        assertNotNull(service.getCase2Body(null));
        assertNotNull(service.getCase2Body("  "));
        assertNotNull(service.getCase3Body("Bob"));

        // 28. getCase3Body with single item, multiple items, and > 15 items
        List<String> items1 = List.of("Item: Valve A | Missing quantity");
        String body1 = service.getCase3Body("Bob", items1);
        assertTrue(body1.contains("1 item:"));

        List<String> items18 = new java.util.ArrayList<>();
        for (int i = 1; i <= 18; i++) {
            items18.add("Item: Item " + i + " | Missing quantity");
        }
        items18.add("Item: Item 1 | Missing location");
        String body18 = service.getCase3Body("Bob", items18);
        assertTrue(body18.contains("...and 3 more"));

        // 29. sendFileSizeExceededAcknowledgement with maxBytes <= 0
        service.sendFileSizeExceededAcknowledgement("buyer@corp.com", "Bob", "doc.pdf", 0L);

        // 30. sendConsolidatedAcknowledgement with blank email and null buyer variants
        service.sendConsolidatedAcknowledgement(List.of(entity), null, Buyer.builder().email("").build(), "RFQ Request");
        service.sendConsolidatedAcknowledgement(null, List.of("Quantity missing"), null, "RFQ Request");
        service.sendConsolidatedAcknowledgement(Collections.emptyList(), List.of("Database error"), null, "RFQ Request");

        // 31. sendSuccessAcknowledgement invalid email
        service.sendSuccessAcknowledgement(List.of(entity), Buyer.builder().email("invalid-email-format").build());

        // 32. mailSender exception handling across all sender methods
        Mockito.doThrow(new RuntimeException("Mail server down")).when(mailSender).send(Mockito.any(SimpleMailMessage.class));
        service.sendSuccessAcknowledgement(List.of(entity), Buyer.builder().email("buyer@corp.com").build());
        service.sendProcessingFailureAcknowledgement("buyer@corp.com", "Bob", "Err");
        service.sendFileSizeExceededAcknowledgement("buyer@corp.com", "Bob", "att.pdf", 1000L);
        service.sendDuplicateEmailAcknowledgement("buyer@corp.com", "Subject");
        ReflectionTestUtils.invokeMethod(service, "sendPartialSuccessAcknowledgement", "buyer@corp.com", "Bob", List.of(entity), List.of("Fail"));

        // 33. sendFailureAcknowledgement and helpers with null / blank fields
        service.sendFailureAcknowledgement(null, null);
        service.sendFailureAcknowledgement(FailedRfqRequest.builder().buyerEmail("buyer@req.com").build(), Buyer.builder().email("").build());
        service.sendFileSizeExceededAcknowledgement(null, "Bob", "", 1000L);
        service.sendDuplicateEmailAcknowledgement("", "");
        ReflectionTestUtils.invokeMethod(service, "sendPartialSuccessAcknowledgement", "", "Bob", List.of(entity), (List<String>) null);
    }
}
