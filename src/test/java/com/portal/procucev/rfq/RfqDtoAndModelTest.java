package com.portal.procucev.rfq;

import com.portal.procucev.rfq.dto.*;
import com.portal.procucev.rfq.entity.*;
import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;


import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RfqDtoAndModelTest {

    @Test
    @DisplayName("Test ApiResponse getters, setters, builder, equals, hashCode, toString")
    void testApiResponse() {
        LocalDateTime now = LocalDateTime.now();
        ApiResponse<String> response1 = ApiResponse.<String>builder()
                .success(true)
                .message("OK")
                .data("Payload")
                .timestamp(now)
                .build();

        assertTrue(response1.isSuccess());
        assertEquals("OK", response1.getMessage());
        assertEquals("Payload", response1.getData());
        assertEquals(now, response1.getTimestamp());

        ApiResponse<String> response2 = new ApiResponse<>();
        response2.setSuccess(true);
        response2.setMessage("OK");
        response2.setData("Payload");
        response2.setTimestamp(now);

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        assertNotNull(response1.toString());

        ApiResponse<String> successResp = ApiResponse.success("Done", "Data");
        assertTrue(successResp.isSuccess());
        assertEquals("Done", successResp.getMessage());

        ApiResponse<String> errorResp = ApiResponse.error("Err");
        assertFalse(errorResp.isSuccess());
        assertEquals("Err", errorResp.getMessage());

        ApiResponse<String> allArgResp = new ApiResponse<>(true, "OK", "Payload", now);
        assertTrue(allArgResp.isSuccess());
    }

    @Test
    @DisplayName("Test BuyerResponse getters, setters, builder, equals, hashCode, toString")
    void testBuyerResponse() {
        BuyerResponse b1 = BuyerResponse.builder()
                .verified(true)
                .name("John")
                .email("john@test.com")
                .orgId("1")
                .userId("2")
                .status("VALID")
                .build();

        assertEquals("John", b1.getName());
        assertTrue(b1.isVerified());
        assertEquals("john@test.com", b1.getEmail());
        assertEquals("1", b1.getOrgId());
        assertEquals("2", b1.getUserId());
        assertEquals("VALID", b1.getStatus());

        BuyerResponse b2 = new BuyerResponse("john@test.com", "John", "1", "2", "Acme", "John", "999", "City", "State", "123", "tok", true, "VALID");
        assertEquals("john@test.com", b2.getEmail());
        assertNotNull(b1.toString());

        BuyerResponse b3 = new BuyerResponse();
        b3.setName("John");
        b3.setVerified(true);
        b3.setEmail("john@test.com");
        b3.setOrgId("1");
        b3.setUserId("2");
        b3.setStatus("VALID");
        assertEquals(b1.getName(), b3.getName());
    }

    @Test
    @DisplayName("Test FailedRfqRequest getters, setters, builder, equals, hashCode, toString")
    void testFailedRfqRequest() {
        FailedRfqRequest f1 = FailedRfqRequest.builder()
                .buyerEmail("buyer@test.com")
                .buyerName("Buyer")
                .rawSubject("Subject")
                .description("Desc")
                .quantity("50")
                .deliveryLocation("Bangalore")
                .deliveryDate("2026-09-01")
                .reasonForFailure("Failed")
                .build();

        assertEquals("buyer@test.com", f1.getBuyerEmail());
        assertEquals("Desc", f1.getDescription());
        assertEquals("50", f1.getQuantity());
        assertEquals("Bangalore", f1.getDeliveryLocation());
        assertEquals("2026-09-01", f1.getDeliveryDate());

        FailedRfqRequest f2 = new FailedRfqRequest("buyer@test.com", "Buyer", "Subject", "Desc", "P1", "Spec", "Brand", "50", "NOS", "Bangalore", "2026-09-01", "Failed", "Ref");
        assertEquals(f1.getBuyerEmail(), f2.getBuyerEmail());
        assertNotNull(f1.toString());

        FailedRfqRequest f3 = new FailedRfqRequest();
        f3.setBuyerEmail("buyer@test.com");
        f3.setDescription("Desc");
        f3.setQuantity("50");
        f3.setDeliveryLocation("Bangalore");
        f3.setDeliveryDate("2026-09-01");
        assertEquals(f1.getBuyerEmail(), f3.getBuyerEmail());
    }

    @Test
    @DisplayName("Test ProcessingStats getters, setters, builder, equals, hashCode, toString")
    void testProcessingStats() {
        ProcessingStats s1 = ProcessingStats.builder()
                .status("SUCCESS")
                .emailsProcessed(10)
                .rfqsCreated(8)
                .errors(2)
                .executionTime("5 sec")
                .build();

        assertEquals("SUCCESS", s1.getStatus());
        assertEquals(10, s1.getEmailsProcessed());
        assertEquals(8, s1.getRfqsCreated());
        assertEquals(2, s1.getErrors());
        assertEquals("5 sec", s1.getExecutionTime());

        ProcessingStats s2 = new ProcessingStats("SUCCESS", 10, 8, 2, "5 sec");
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotNull(s1.toString());

        ProcessingStats s3 = new ProcessingStats();
        s3.setStatus("SUCCESS");
        s3.setEmailsProcessed(10);
        s3.setRfqsCreated(8);
        s3.setErrors(2);
        s3.setExecutionTime("5 sec");
        assertEquals(s1, s3);
    }

    @Test
    @DisplayName("Test RFQRequest and inner DTOs")
    void testRFQRequest() {
        RFQRequest.OrgRef org = RFQRequest.OrgRef.builder().id("100").build();
        assertEquals("100", org.getId());
        RFQRequest.OrgRef org2 = new RFQRequest.OrgRef("100");
        assertEquals(org, org2);
        assertEquals(org.hashCode(), org2.hashCode());
        assertNotNull(org.toString());

        RFQRequest.LocationDto loc = RFQRequest.LocationDto.builder()
                .address("123 Main St")
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560001")
                .build();
        assertEquals("123 Main St", loc.getAddress());
        assertEquals("Bangalore", loc.getCity());

        RFQRequest.RfqItemDto item = RFQRequest.RfqItemDto.builder()
                .brand("Dell")
                .unitofMeasures("NOS")
                .quantity(10.0)
                .description("Laptop")
                .category("IT")
                .build();
        assertEquals("Dell", item.getBrand());
        assertEquals(10.0, item.getQuantity());

        RFQRequest req = RFQRequest.builder()
                .createdBy("John")
                .projectDesc("Project")
                .deliveryDate("2026-08-25")
                .noPrFlag(true)
                .org(org)
                .user("1")
                .sourceType("T")
                .remarks("Remarks")
                .clientdeliverylocationrfq(List.of(loc))
                .rfqItem(List.of(item))
                .rfqNumber("RFQ-123")
                .buyerEmail("test@test.com")
                .token("token123")
                .build();

        assertEquals("John", req.getCreatedBy());
        assertEquals("RFQ-123", req.getRfqNumber());
        assertEquals("test@test.com", req.getBuyerEmail());
    }

    @Test
    @DisplayName("Test RFQResponse getters, setters, builder, equals, hashCode, toString")
    void testRFQResponse() {
        LocalDateTime now = LocalDateTime.now();
        RFQResponse r1 = RFQResponse.builder()
                .rfqNumber("RFQ-123")
                .status("CREATED")
                .buyerEmail("buyer@test.com")
                .message("Success")
                .createdAt(now)
                .build();

        assertEquals("RFQ-123", r1.getRfqNumber());
        assertEquals("CREATED", r1.getStatus());
        assertEquals("Success", r1.getMessage());

        RFQResponse r2 = new RFQResponse("RFQ-123", "CREATED", "buyer@test.com", "Success", now);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotNull(r1.toString());

        RFQResponse r3 = new RFQResponse();
        r3.setRfqNumber("RFQ-123");
        r3.setStatus("CREATED");
        r3.setMessage("Success");
        assertEquals(r1.getRfqNumber(), r3.getRfqNumber());
    }

    @Test
    @DisplayName("Test BuyerEntity getters, setters, builder, equals, hashCode, toString")
    void testBuyerEntity() {
        LocalDateTime now = LocalDateTime.now();
        BuyerEntity b1 = BuyerEntity.builder()
                .id(1L)
                .email("buyer@test.com")
                .contactPerson("Jane")
                .companyName("Acme")
                .phone("9999999999")
                .verified(true)
                .orgId("1")
                .userId("2")
                .address("Street 1")
                .city("City")
                .state("State")
                .pincode("123456")
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(1L, b1.getId());
        assertEquals("buyer@test.com", b1.getEmail());
        assertEquals("Jane", b1.getContactPerson());
        assertEquals("Acme", b1.getCompanyName());
        assertEquals("9999999999", b1.getPhone());
        assertTrue(b1.isVerified());

        BuyerEntity b2 = new BuyerEntity(1L, "buyer@test.com", "Acme", "Jane", "9999999999", true, "1", "2", "City", "State", "123456", "Street 1", now, now);
        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
        assertNotNull(b1.toString());
    }

    @Test
    @DisplayName("Test EmailTransaction getters, setters, builder, equals, hashCode, toString")
    void testEmailTransaction() {
        LocalDateTime now = LocalDateTime.now();
        EmailTransaction t1 = EmailTransaction.builder()
                .id(5L)
                .messageId("MSG-1")
                .senderEmail("sender@test.com")
                .subject("Subject")
                .status("SUCCESS")
                .errorMessage(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(5L, t1.getId());
        assertEquals("MSG-1", t1.getMessageId());
        assertEquals("sender@test.com", t1.getSenderEmail());
        assertEquals("Subject", t1.getSubject());
        assertEquals("SUCCESS", t1.getStatus());
        assertNull(t1.getErrorMessage());

        EmailTransaction t2 = new EmailTransaction(5L, "MSG-1", "Subject", "sender@test.com", "SUCCESS", null, null, now, now);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertNotNull(t1.toString());
    }

    @Test
    @DisplayName("Test RFQEntity getters, setters, builder, equals, hashCode, toString")
    void testRFQEntity() {
        LocalDateTime now = LocalDateTime.now();
        RFQEntity r1 = RFQEntity.builder()
                .id(1L)
                .rfqNumber("RFQ-100")
                .buyerEmail("buyer@test.com")
                .rawSubject("Subject")
                .itemsJson("[]")
                .deliveryLocation("Location")
                .deliveryDate("2026-08-25")
                .status("CREATED")
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(1L, r1.getId());
        assertEquals("RFQ-100", r1.getRfqNumber());
        assertEquals("buyer@test.com", r1.getBuyerEmail());
        assertEquals("Subject", r1.getRawSubject());
        assertEquals("CREATED", r1.getStatus());

        RFQEntity r2 = new RFQEntity(1L, "RFQ-100", "buyer@test.com", "CREATED", "Subject", "[]", "Location", "2026-08-25", null, now, now);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotNull(r1.toString());
    }

    @Test
    @DisplayName("Test RfqItemRecord getters, setters, builder, equals, hashCode, toString")
    void testRfqItemRecord() {
        LocalDateTime now = LocalDateTime.now();
        RfqItemRecord item1 = RfqItemRecord.builder()
                .id(10L)
                .buyerEmail("buyer@test.com")
                .itemDescription("Laptop")
                .deliveryDate("2026-08-25")
                .rfqNumber("RFQ-100")
                .category("IT")
                .division("Hardware")
                .categoryConfidence(0.95)
                .classificationStatus("MATCHED")
                .createdAt(now)
                .build();

        assertEquals(10L, item1.getId());
        assertEquals("RFQ-100", item1.getRfqNumber());
        assertEquals("Laptop", item1.getItemDescription());
        assertEquals("IT", item1.getCategory());

        RfqItemRecord item2 = new RfqItemRecord(10L, "buyer@test.com", "Laptop", "2026-08-25", "RFQ-100", "IT", "Hardware", 0.95, "MATCHED", now);
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
        assertNotNull(item1.toString());
    }

    @Test
    @DisplayName("Test ApplicationException constructors")
    void testApplicationException() {
        ApplicationException e1 = new ApplicationException("Error occurred");
        assertEquals("Error occurred", e1.getMessage());

        Throwable cause = new RuntimeException("Cause");
        ApplicationException e2 = new ApplicationException("Error with cause", cause);
        assertEquals("Error with cause", e2.getMessage());
        assertEquals(cause, e2.getCause());
    }

    @Test
    @DisplayName("Test Buyer getters, setters, builder, equals, hashCode, toString")
    void testBuyerModel() {
        Buyer b1 = Buyer.builder()
                .id(1L)
                .email("buyer@test.com")
                .name("Jane")
                .companyName("Acme")
                .contactPerson("Jane")
                .phone("999")
                .city("City")
                .state("State")
                .pincode("123")
                .address("Address")
                .verified(true)
                .orgId("1")
                .userId("2")
                .token("tok")
                .build();

        assertEquals(1L, b1.getId());
        assertEquals("buyer@test.com", b1.getEmail());
        assertEquals("Jane", b1.getName());
        assertEquals("Acme", b1.getCompanyName());
    }

    @Test
    @DisplayName("Test EmailData getters, setters, builder, equals, hashCode, toString")
    void testEmailData() {
        java.util.Date now = new java.util.Date();
        EmailData e1 = EmailData.builder()
                .messageId("MSG-1")
                .senderEmail("sender@test.com")
                .senderName("Sender")
                .subject("RFQ Subject")
                .body("RFQ Body")
                .receivedDate(now)
                .attachments(List.of())
                .attachmentText("Text")
                .build();

        assertEquals("MSG-1", e1.getMessageId());
        assertEquals("sender@test.com", e1.getSenderEmail());
        assertEquals("Sender", e1.getSenderName());
        assertEquals("RFQ Subject", e1.getSubject());
        assertEquals("RFQ Body", e1.getBody());
        assertEquals(now, e1.getReceivedDate());
    }

    @Test
    @DisplayName("Test ExtractedRFQ getters, setters, builder, equals, hashCode, toString")
    void testExtractedRFQ() {
        ExtractedRFQ r1 = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .category("IT")
                .deliveryLocation("Location")
                .deliveryCity("City")
                .deliveryState("State")
                .deliveryPincode("123456")
                .deliveryDate("2026-08-25")
                .items(List.of())
                .build();

        assertEquals("buyer@test.com", r1.getBuyerEmail());
        assertEquals("IT", r1.getCategory());
        assertEquals("Location", r1.getDeliveryLocation());
    }

    @Test
    @DisplayName("Test RFQItem getters, setters, builder, equals, hashCode, toString")
    void testRFQItemModel() {
        RFQItem item1 = RFQItem.builder()
                .itemDescription("Laptop")
                .partCode("P123")
                .partNumber("PN456")
                .modelNumber("M789")
                .specification("16GB RAM")
                .quantity(10.0)
                .uom("NOS")
                .brand("Dell")
                .deliveryLocation("BLR")
                .deliveryDate("2026-08-25")
                .category("IT")
                .division("Hardware")
                .categoryConfidence(0.95)
                .classificationStatus("MATCHED")
                .remarks("Remarks")
                .build();

        assertEquals("Laptop", item1.getItemDescription());
        assertEquals("P123", item1.getPartCode());
        assertEquals("PN456", item1.getPartNumber());
        assertEquals("M789", item1.getModelNumber());
        assertEquals("16GB RAM", item1.getSpecification());
        assertEquals(10.0, item1.getQuantity());
        assertEquals("NOS", item1.getUom());
        assertEquals("Dell", item1.getBrand());
        assertEquals("BLR", item1.getDeliveryLocation());
        assertEquals("2026-08-25", item1.getDeliveryDate());
        assertEquals("IT", item1.getCategory());
        assertEquals("Hardware", item1.getDivision());
        assertEquals(0.95, item1.getCategoryConfidence());
        assertEquals("MATCHED", item1.getClassificationStatus());
        assertEquals("Remarks", item1.getRemarks());

        // getEffectivePartNumber: partCode set
        assertEquals("P123", item1.getEffectivePartNumber());

        // getEffectivePartNumber: partCode null, partNumber set
        RFQItem itemPN = RFQItem.builder().partNumber("PN456").build();
        assertEquals("PN456", itemPN.getEffectivePartNumber());

        // getEffectivePartNumber: partCode blank, partNumber set
        RFQItem itemPNBlank = RFQItem.builder().partCode("").partNumber("PN456").build();
        assertEquals("PN456", itemPNBlank.getEffectivePartNumber());

        // getEffectivePartNumber: partCode null, partNumber null, modelNumber set
        RFQItem itemMN = RFQItem.builder().modelNumber("M789").build();
        assertEquals("M789", itemMN.getEffectivePartNumber());

        // getEffectivePartNumber: partCode blank, partNumber blank, modelNumber set
        RFQItem itemMNBlank = RFQItem.builder().partCode("").partNumber("").modelNumber("M789").build();
        assertEquals("M789", itemMNBlank.getEffectivePartNumber());

        // getEffectivePartNumber: all null
        RFQItem itemNone = RFQItem.builder().itemDescription("Laptop").build();
        assertEquals("", itemNone.getEffectivePartNumber());

        // getEffectivePartNumber: all blank
        RFQItem itemAllBlank = RFQItem.builder().partCode("  ").partNumber("  ").modelNumber("  ").build();
        assertEquals("", itemAllBlank.getEffectivePartNumber());

        // equals / hashCode / toString
        RFQItem item2 = new RFQItem("Laptop", "P123", "PN456", "M789", "16GB RAM", 10.0, "NOS", "Dell", "Remarks", "BLR", "2026-08-25", "IT", "Hardware", 0.95, "MATCHED");
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
        assertNotNull(item1.toString());

        // no-arg constructor + setters
        RFQItem item3 = new RFQItem();
        item3.setItemDescription("Laptop");
        item3.setPartCode("P123");
        item3.setPartNumber("PN456");
        item3.setModelNumber("M789");
        item3.setSpecification("16GB RAM");
        item3.setQuantity(10.0);
        item3.setUom("NOS");
        item3.setBrand("Dell");
        item3.setDeliveryLocation("BLR");
        item3.setDeliveryDate("2026-08-25");
        item3.setCategory("IT");
        item3.setDivision("Hardware");
        item3.setCategoryConfidence(0.95);
        item3.setClassificationStatus("MATCHED");
        item3.setRemarks("Remarks");
        assertEquals(item1, item3);
    }

    // --- Entity lifecycle method tests ---

    @Test
    @DisplayName("Test BuyerEntity lifecycle methods: onCreate and onUpdate")
    void testBuyerEntityLifecycle() {
        BuyerEntity b = new BuyerEntity();
        b.setEmail("test@test.com");
        assertNull(b.getCreatedAt());
        assertNull(b.getUpdatedAt());

        ReflectionTestUtils.invokeMethod(b, "onCreate");
        assertNotNull(b.getCreatedAt());
        assertNotNull(b.getUpdatedAt());

        LocalDateTime firstUpdate = b.getUpdatedAt();
        ReflectionTestUtils.invokeMethod(b, "onUpdate");
        assertNotNull(b.getUpdatedAt());
    }

    @Test
    @DisplayName("Test EmailTransaction lifecycle methods: onCreate and onUpdate")
    void testEmailTransactionLifecycle() {
        EmailTransaction t = new EmailTransaction();
        t.setMessageId("MSG-X");
        assertNull(t.getCreatedAt());
        assertNull(t.getUpdatedAt());

        ReflectionTestUtils.invokeMethod(t, "onCreate");
        assertNotNull(t.getCreatedAt());
        assertNotNull(t.getUpdatedAt());

        ReflectionTestUtils.invokeMethod(t, "onUpdate");
        assertNotNull(t.getUpdatedAt());
    }

    @Test
    @DisplayName("Test RFQEntity lifecycle methods: onCreate and onUpdate")
    void testRFQEntityLifecycle() {
        RFQEntity r = new RFQEntity();
        r.setRfqNumber("RFQ-X");
        assertNull(r.getCreatedAt());
        assertNull(r.getUpdatedAt());

        ReflectionTestUtils.invokeMethod(r, "onCreate");
        assertNotNull(r.getCreatedAt());
        assertNotNull(r.getUpdatedAt());

        ReflectionTestUtils.invokeMethod(r, "onUpdate");
        assertNotNull(r.getUpdatedAt());
    }

    @Test
    @DisplayName("Test RfqItemRecord lifecycle method: onCreate")
    void testRfqItemRecordLifecycle() {
        RfqItemRecord item = new RfqItemRecord();
        item.setBuyerEmail("test@test.com");
        item.setItemDescription("Widget");
        assertNull(item.getCreatedAt());

        ReflectionTestUtils.invokeMethod(item, "onCreate");
        assertNotNull(item.getCreatedAt());
    }


    @Test
    @DisplayName("Test RfqItemRecord full field coverage via setters")
    void testRfqItemRecordSetters() {
        LocalDateTime now = LocalDateTime.now();
        RfqItemRecord item = new RfqItemRecord();
        item.setId(1L);
        item.setBuyerEmail("buyer@test.com");
        item.setItemDescription("Desc");
        item.setDeliveryDate("2026-01-01");
        item.setRfqNumber("RFQ-1");
        item.setCategory("Cat");
        item.setDivision("Div");
        item.setCategoryConfidence(0.9);
        item.setClassificationStatus("MATCHED");
        item.setCreatedAt(now);

        assertEquals(1L, item.getId());
        assertEquals("buyer@test.com", item.getBuyerEmail());
        assertEquals("Desc", item.getItemDescription());
        assertEquals("2026-01-01", item.getDeliveryDate());
        assertEquals("RFQ-1", item.getRfqNumber());
        assertEquals("Cat", item.getCategory());
        assertEquals("Div", item.getDivision());
        assertEquals(0.9, item.getCategoryConfidence());
        assertEquals("MATCHED", item.getClassificationStatus());
        assertEquals(now, item.getCreatedAt());
    }

    @Test
    @DisplayName("Test BuyerEntity full field coverage via setters")
    void testBuyerEntitySetters() {
        LocalDateTime now = LocalDateTime.now();
        BuyerEntity b = new BuyerEntity();
        b.setId(1L);
        b.setEmail("a@b.com");
        b.setCompanyName("Co");
        b.setContactPerson("CP");
        b.setPhone("123");
        b.setVerified(false);
        b.setOrgId("O1");
        b.setUserId("U1");
        b.setCity("C");
        b.setState("S");
        b.setPincode("P");
        b.setAddress("A");
        b.setCreatedAt(now);
        b.setUpdatedAt(now);

        assertEquals(1L, b.getId());
        assertEquals("a@b.com", b.getEmail());
        assertEquals("Co", b.getCompanyName());
        assertEquals("CP", b.getContactPerson());
        assertEquals("123", b.getPhone());
        assertFalse(b.isVerified());
        assertEquals("O1", b.getOrgId());
        assertEquals("U1", b.getUserId());
        assertEquals("C", b.getCity());
        assertEquals("S", b.getState());
        assertEquals("P", b.getPincode());
        assertEquals("A", b.getAddress());
        assertEquals(now, b.getCreatedAt());
        assertEquals(now, b.getUpdatedAt());
    }

    @Test
    @DisplayName("Test EmailTransaction full field coverage via setters")
    void testEmailTransactionSetters() {
        LocalDateTime now = LocalDateTime.now();
        EmailTransaction t = new EmailTransaction();
        t.setId(1L);
        t.setMessageId("M1");
        t.setSubject("S");
        t.setSenderEmail("se");
        t.setStatus("OK");
        t.setErrorMessage("err");
        t.setCreatedAt(now);
        t.setUpdatedAt(now);

        assertEquals(1L, t.getId());
        assertEquals("M1", t.getMessageId());
        assertEquals("S", t.getSubject());
        assertEquals("se", t.getSenderEmail());
        assertEquals("OK", t.getStatus());
        assertEquals("err", t.getErrorMessage());
    }

    @Test
    @DisplayName("Test RFQEntity full field coverage via setters")
    void testRFQEntitySetters() {
        LocalDateTime now = LocalDateTime.now();
        RFQEntity r = new RFQEntity();
        r.setId(1L);
        r.setRfqNumber("RFQ-1");
        r.setBuyerEmail("b@t.com");
        r.setStatus("CREATED");
        r.setRawSubject("Sub");
        r.setItemsJson("[]");
        r.setDeliveryLocation("L");
        r.setDeliveryDate("D");
        r.setCreatedAt(now);
        r.setUpdatedAt(now);

        assertEquals(1L, r.getId());
        assertEquals("RFQ-1", r.getRfqNumber());
        assertEquals("b@t.com", r.getBuyerEmail());
        assertEquals("CREATED", r.getStatus());
        assertEquals("Sub", r.getRawSubject());
        assertEquals("[]", r.getItemsJson());
        assertEquals("L", r.getDeliveryLocation());
        assertEquals("D", r.getDeliveryDate());
    }
}

