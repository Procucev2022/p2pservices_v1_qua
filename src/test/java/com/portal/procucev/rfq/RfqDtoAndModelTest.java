package com.portal.procucev.rfq;

import com.portal.procucev.rfq.dto.*;
import com.portal.procucev.rfq.entity.*;
import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RfqDtoAndModelTest {

    @Test
    @DisplayName("Test ApiResponse getters, setters, builder, equals, hashCode, toString")
    void testApiResponse() {
        ApiResponse<String> response1 = ApiResponse.<String>builder()
                .status("SUCCESS")
                .message("OK")
                .data("Payload")
                .errorCode("200")
                .build();

        assertEquals("SUCCESS", response1.getStatus());
        assertEquals("OK", response1.getMessage());
        assertEquals("Payload", response1.getData());
        assertEquals("200", response1.getErrorCode());

        ApiResponse<String> response2 = new ApiResponse<>();
        response2.setStatus("SUCCESS");
        response2.setMessage("OK");
        response2.setData("Payload");
        response2.setErrorCode("200");

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        assertNotNull(response1.toString());

        ApiResponse<String> successResp = ApiResponse.success("Done", "Data");
        assertEquals("SUCCESS", successResp.getStatus());
        assertEquals("Done", successResp.getMessage());

        ApiResponse<String> errorResp = ApiResponse.error("Err", "500");
        assertEquals("ERROR", errorResp.getStatus());
        assertEquals("Err", errorResp.getMessage());
        assertEquals("500", errorResp.getErrorCode());

        ApiResponse<String> allArgResp = new ApiResponse<>("SUCCESS", "OK", "Payload", "200");
        assertEquals("SUCCESS", allArgResp.getStatus());
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
                .message("Valid")
                .build();

        assertEquals("John", b1.getName());
        assertTrue(b1.isVerified());
        assertEquals("john@test.com", b1.getEmail());
        assertEquals("1", b1.getOrgId());
        assertEquals("2", b1.getUserId());
        assertEquals("Valid", b1.getMessage());

        BuyerResponse b2 = new BuyerResponse(true, "John", "john@test.com", "1", "2", "Valid");
        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
        assertNotNull(b1.toString());

        BuyerResponse b3 = new BuyerResponse();
        b3.setName("John");
        b3.setVerified(true);
        b3.setEmail("john@test.com");
        b3.setOrgId("1");
        b3.setUserId("2");
        b3.setMessage("Valid");
        assertEquals(b1, b3);
    }

    @Test
    @DisplayName("Test FailedRfqRequest getters, setters, builder, equals, hashCode, toString")
    void testFailedRfqRequest() {
        FailedRfqRequest f1 = FailedRfqRequest.builder()
                .transactionId(10L)
                .updatedDescription("Desc")
                .updatedQuantity("50")
                .updatedLocation("Bangalore")
                .updatedDeliveryDate("2026-09-01")
                .build();

        assertEquals(10L, f1.getTransactionId());
        assertEquals("Desc", f1.getUpdatedDescription());
        assertEquals("50", f1.getUpdatedQuantity());
        assertEquals("Bangalore", f1.getUpdatedLocation());
        assertEquals("2026-09-01", f1.getUpdatedDeliveryDate());

        FailedRfqRequest f2 = new FailedRfqRequest(10L, "Desc", "50", "Bangalore", "2026-09-01");
        assertEquals(f1, f2);
        assertEquals(f1.hashCode(), f2.hashCode());
        assertNotNull(f1.toString());

        FailedRfqRequest f3 = new FailedRfqRequest();
        f3.setTransactionId(10L);
        f3.setUpdatedDescription("Desc");
        f3.setUpdatedQuantity("50");
        f3.setUpdatedLocation("Bangalore");
        f3.setUpdatedDeliveryDate("2026-09-01");
        assertEquals(f1, f3);
    }

    @Test
    @DisplayName("Test ProcessingStats getters, setters, builder, equals, hashCode, toString")
    void testProcessingStats() {
        ProcessingStats s1 = ProcessingStats.builder()
                .totalProcessed(10)
                .successful(8)
                .failed(2)
                .unverifiedSender(1)
                .missingQuantity(1)
                .build();

        assertEquals(10, s1.getTotalProcessed());
        assertEquals(8, s1.getSuccessful());
        assertEquals(2, s1.getFailed());
        assertEquals(1, s1.getUnverifiedSender());
        assertEquals(1, s1.getMissingQuantity());

        ProcessingStats s2 = new ProcessingStats(10, 8, 2, 1, 1);
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotNull(s1.toString());

        ProcessingStats s3 = new ProcessingStats();
        s3.setTotalProcessed(10);
        s3.setSuccessful(8);
        s3.setFailed(2);
        s3.setUnverifiedSender(1);
        s3.setMissingQuantity(1);
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
        RFQRequest.OrgRef org3 = new RFQRequest.OrgRef();
        org3.setId("100");
        assertEquals(org, org3);

        RFQRequest.LocationDto loc = RFQRequest.LocationDto.builder()
                .address("123 Main St")
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560001")
                .build();
        assertEquals("123 Main St", loc.getAddress());
        assertEquals("Bangalore", loc.getCity());
        assertEquals("Karnataka", loc.getState());
        assertEquals("560001", loc.getPincode());
        RFQRequest.LocationDto loc2 = new RFQRequest.LocationDto("123 Main St", "Bangalore", "Karnataka", "560001");
        assertEquals(loc, loc2);
        assertEquals(loc.hashCode(), loc2.hashCode());
        assertNotNull(loc.toString());
        RFQRequest.LocationDto loc3 = new RFQRequest.LocationDto();
        loc3.setAddress("123 Main St");
        loc3.setCity("Bangalore");
        loc3.setState("Karnataka");
        loc3.setPincode("560001");
        assertEquals(loc, loc3);

        RFQRequest.RfqItemDto item = RFQRequest.RfqItemDto.builder()
                .brand("Dell")
                .unitofMeasures("NOS")
                .quantity(10.0)
                .description("Laptop")
                .category("IT")
                .createdBy("User")
                .createdTS("2026-08-11T12:00:00")
                .itemcode("PART123")
                .serialNo(1001)
                .remarks("None")
                .build();
        assertEquals("Dell", item.getBrand());
        assertEquals("NOS", item.getUnitofMeasures());
        assertEquals(10.0, item.getQuantity());
        assertEquals("Laptop", item.getDescription());
        assertEquals("IT", item.getCategory());
        assertEquals("User", item.getCreatedBy());
        assertEquals("2026-08-11T12:00:00", item.getCreatedTS());
        assertEquals("PART123", item.getItemcode());
        assertEquals(1001, item.getSerialNo());
        assertEquals("None", item.getRemarks());

        RFQRequest.RfqItemDto item2 = new RFQRequest.RfqItemDto("Dell", "NOS", 10.0, "Laptop", "IT", "User", "2026-08-11T12:00:00", "PART123", 1001, "None");
        assertEquals(item, item2);
        assertEquals(item.hashCode(), item2.hashCode());
        assertNotNull(item.toString());

        RFQRequest.RfqItemDto item3 = new RFQRequest.RfqItemDto();
        item3.setBrand("Dell");
        item3.setUnitofMeasures("NOS");
        item3.setQuantity(10.0);
        item3.setDescription("Laptop");
        item3.setCategory("IT");
        item3.setCreatedBy("User");
        item3.setCreatedTS("2026-08-11T12:00:00");
        item3.setItemcode("PART123");
        item3.setSerialNo(1001);
        item3.setRemarks("None");
        assertEquals(item, item3);

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
                .vendors(List.of())
                .rfqDocument(List.of())
                .rfqNumber("RFQ-123")
                .buyerEmail("test@test.com")
                .token("token123")
                .build();

        assertEquals("John", req.getCreatedBy());
        assertEquals("Project", req.getProjectDesc());
        assertEquals("2026-08-25", req.getDeliveryDate());
        assertTrue(req.isNoPrFlag());
        assertEquals(org, req.getOrg());
        assertEquals("1", req.getUser());
        assertEquals("T", req.getSourceType());
        assertEquals("Remarks", req.getRemarks());
        assertEquals(1, req.getClientdeliverylocationrfq().size());
        assertEquals(1, req.getRfqItem().size());
        assertEquals(0, req.getVendors().size());
        assertEquals(0, req.getRfqDocument().size());
        assertEquals("RFQ-123", req.getRfqNumber());
        assertEquals("test@test.com", req.getBuyerEmail());
        assertEquals("token123", req.getToken());

        RFQRequest req2 = new RFQRequest("John", "Project", "2026-08-25", true, org, "1", "T", "Remarks", List.of(loc), List.of(item), List.of(), List.of(), "RFQ-123", "test@test.com", "token123");
        assertEquals(req, req2);
        assertEquals(req.hashCode(), req2.hashCode());
        assertNotNull(req.toString());

        RFQRequest req3 = new RFQRequest();
        req3.setCreatedBy("John");
        req3.setProjectDesc("Project");
        req3.setDeliveryDate("2026-08-25");
        req3.setNoPrFlag(true);
        req3.setOrg(org);
        req3.setUser("1");
        req3.setSourceType("T");
        req3.setRemarks("Remarks");
        req3.setClientdeliverylocationrfq(List.of(loc));
        req3.setRfqItem(List.of(item));
        req3.setVendors(List.of());
        req3.setRfqDocument(List.of());
        req3.setRfqNumber("RFQ-123");
        req3.setBuyerEmail("test@test.com");
        req3.setToken("token123");
        assertEquals(req, req3);
    }

    @Test
    @DisplayName("Test RFQResponse getters, setters, builder, equals, hashCode, toString")
    void testRFQResponse() {
        RFQResponse r1 = RFQResponse.builder()
                .rfqNumber("RFQ-123")
                .status("CREATED")
                .message("Success")
                .build();

        assertEquals("RFQ-123", r1.getRfqNumber());
        assertEquals("CREATED", r1.getStatus());
        assertEquals("Success", r1.getMessage());

        RFQResponse r2 = new RFQResponse("RFQ-123", "CREATED", "Success");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotNull(r1.toString());

        RFQResponse r3 = new RFQResponse();
        r3.setRfqNumber("RFQ-123");
        r3.setStatus("CREATED");
        r3.setMessage("Success");
        assertEquals(r1, r3);
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
        assertEquals("1", b1.getOrgId());
        assertEquals("2", b1.getUserId());
        assertEquals("Street 1", b1.getAddress());
        assertEquals("City", b1.getCity());
        assertEquals("State", b1.getState());
        assertEquals("123456", b1.getPincode());
        assertEquals(now, b1.getCreatedAt());
        assertEquals(now, b1.getUpdatedAt());

        BuyerEntity b2 = new BuyerEntity(1L, "buyer@test.com", "Jane", "Acme", "9999999999", true, "1", "2", "Street 1", "City", "State", "123456", now, now);
        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
        assertNotNull(b1.toString());

        BuyerEntity b3 = new BuyerEntity();
        b3.setId(1L);
        b3.setEmail("buyer@test.com");
        b3.setContactPerson("Jane");
        b3.setCompanyName("Acme");
        b3.setPhone("9999999999");
        b3.setVerified(true);
        b3.setOrgId("1");
        b3.setUserId("2");
        b3.setAddress("Street 1");
        b3.setCity("City");
        b3.setState("State");
        b3.setPincode("123456");
        b3.setCreatedAt(now);
        b3.setUpdatedAt(now);
        assertEquals(b1, b3);

        BuyerEntity prePersistTest = new BuyerEntity();
        prePersistTest.onCreate();
        assertNotNull(prePersistTest.getCreatedAt());
        assertNotNull(prePersistTest.getUpdatedAt());

        BuyerEntity preUpdateTest = new BuyerEntity();
        preUpdateTest.onUpdate();
        assertNotNull(preUpdateTest.getUpdatedAt());
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
                .receivedTime(now)
                .status("SUCCESS")
                .rfqNumber("RFQ-999")
                .failureReason(null)
                .rawContent("Content")
                .attachmentPaths("path/1")
                .processedAt(now)
                .build();

        assertEquals(5L, t1.getId());
        assertEquals("MSG-1", t1.getMessageId());
        assertEquals("sender@test.com", t1.getSenderEmail());
        assertEquals("Subject", t1.getSubject());
        assertEquals(now, t1.getReceivedTime());
        assertEquals("SUCCESS", t1.getStatus());
        assertEquals("RFQ-999", t1.getRfqNumber());
        assertNull(t1.getFailureReason());
        assertEquals("Content", t1.getRawContent());
        assertEquals("path/1", t1.getAttachmentPaths());
        assertEquals(now, t1.getProcessedAt());

        EmailTransaction t2 = new EmailTransaction(5L, "MSG-1", "sender@test.com", "Subject", now, "SUCCESS", "RFQ-999", null, "Content", "path/1", now);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertNotNull(t1.toString());

        EmailTransaction t3 = new EmailTransaction();
        t3.setId(5L);
        t3.setMessageId("MSG-1");
        t3.setSenderEmail("sender@test.com");
        t3.setSubject("Subject");
        t3.setReceivedTime(now);
        t3.setStatus("SUCCESS");
        t3.setRfqNumber("RFQ-999");
        t3.setFailureReason(null);
        t3.setRawContent("Content");
        t3.setAttachmentPaths("path/1");
        t3.setProcessedAt(now);
        assertEquals(t1, t3);
    }

    @Test
    @DisplayName("Test RFQEntity getters, setters, builder, equals, hashCode, toString")
    void testRFQEntity() {
        LocalDateTime now = LocalDateTime.now();
        RFQEntity r1 = RFQEntity.builder()
                .id(1L)
                .rfqNumber("RFQ-100")
                .buyerEmail("buyer@test.com")
                .companyName("Acme")
                .category("IT")
                .deliveryLocation("Location")
                .deliveryDate("2026-08-25")
                .status("CREATED")
                .createdAt(now)
                .build();

        assertEquals(1L, r1.getId());
        assertEquals("RFQ-100", r1.getRfqNumber());
        assertEquals("buyer@test.com", r1.getBuyerEmail());
        assertEquals("Acme", r1.getCompanyName());
        assertEquals("IT", r1.getCategory());
        assertEquals("Location", r1.getDeliveryLocation());
        assertEquals("2026-08-25", r1.getDeliveryDate());
        assertEquals("CREATED", r1.getStatus());
        assertEquals(now, r1.getCreatedAt());

        RFQEntity r2 = new RFQEntity(1L, "RFQ-100", "buyer@test.com", "Acme", "IT", "Location", "2026-08-25", "CREATED", now);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotNull(r1.toString());

        RFQEntity r3 = new RFQEntity();
        r3.setId(1L);
        r3.setRfqNumber("RFQ-100");
        r3.setBuyerEmail("buyer@test.com");
        r3.setCompanyName("Acme");
        r3.setCategory("IT");
        r3.setDeliveryLocation("Location");
        r3.setDeliveryDate("2026-08-25");
        r3.setStatus("CREATED");
        r3.setCreatedAt(now);
        assertEquals(r1, r3);

        RFQEntity prePersistTest = new RFQEntity();
        prePersistTest.onCreate();
        assertNotNull(prePersistTest.getCreatedAt());
    }

    @Test
    @DisplayName("Test RfqItemRecord getters, setters, builder, equals, hashCode, toString")
    void testRfqItemRecord() {
        RfqItemRecord item1 = RfqItemRecord.builder()
                .id(10L)
                .rfqNumber("RFQ-100")
                .itemDescription("Laptop")
                .partCode("P123")
                .specification("16GB RAM")
                .quantity(5.0)
                .uom("NOS")
                .category("IT")
                .brand("Dell")
                .build();

        assertEquals(10L, item1.getId());
        assertEquals("RFQ-100", item1.getRfqNumber());
        assertEquals("Laptop", item1.getItemDescription());
        assertEquals("P123", item1.getPartCode());
        assertEquals("16GB RAM", item1.getSpecification());
        assertEquals(5.0, item1.getQuantity());
        assertEquals("NOS", item1.getUom());
        assertEquals("IT", item1.getCategory());
        assertEquals("Dell", item1.getBrand());

        RfqItemRecord item2 = new RfqItemRecord(10L, "RFQ-100", "Laptop", "P123", "16GB RAM", 5.0, "NOS", "IT", "Dell");
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
        assertNotNull(item1.toString());

        RfqItemRecord item3 = new RfqItemRecord();
        item3.setId(10L);
        item3.setRfqNumber("RFQ-100");
        item3.setItemDescription("Laptop");
        item3.setPartCode("P123");
        item3.setSpecification("16GB RAM");
        item3.setQuantity(5.0);
        item3.setUom("NOS");
        item3.setCategory("IT");
        item3.setBrand("Dell");
        assertEquals(item1, item3);
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
        assertEquals("Jane", b1.getContactPerson());
        assertEquals("999", b1.getPhone());
        assertEquals("City", b1.getCity());
        assertEquals("State", b1.getState());
        assertEquals("123", b1.getPincode());
        assertEquals("Address", b1.getAddress());
        assertTrue(b1.isVerified());
        assertEquals("1", b1.getOrgId());
        assertEquals("2", b1.getUserId());
        assertEquals("tok", b1.getToken());

        Buyer b2 = new Buyer(1L, "buyer@test.com", "Jane", "Acme", "Jane", "999", "City", "State", "123", "Address", true, "1", "2", "tok");
        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
        assertNotNull(b1.toString());

        Buyer b3 = new Buyer();
        b3.setId(1L);
        b3.setEmail("buyer@test.com");
        b3.setName("Jane");
        b3.setCompanyName("Acme");
        b3.setContactPerson("Jane");
        b3.setPhone("999");
        b3.setCity("City");
        b3.setState("State");
        b3.setPincode("123");
        b3.setAddress("Address");
        b3.setVerified(true);
        b3.setOrgId("1");
        b3.setUserId("2");
        b3.setToken("tok");
        assertEquals(b1, b3);
    }

    @Test
    @DisplayName("Test EmailData getters, setters, builder, equals, hashCode, toString")
    void testEmailData() {
        LocalDateTime now = LocalDateTime.now();
        EmailData e1 = EmailData.builder()
                .messageId("MSG-1")
                .fromEmail("sender@test.com")
                .subject("RFQ Subject")
                .body("RFQ Body")
                .receivedTime(now)
                .attachmentPaths(List.of("path/1"))
                .build();

        assertEquals("MSG-1", e1.getMessageId());
        assertEquals("sender@test.com", e1.getFromEmail());
        assertEquals("RFQ Subject", e1.getSubject());
        assertEquals("RFQ Body", e1.getBody());
        assertEquals(now, e1.getReceivedTime());
        assertEquals(1, e1.getAttachmentPaths().size());

        EmailData e2 = new EmailData("MSG-1", "sender@test.com", "RFQ Subject", "RFQ Body", now, List.of("path/1"));
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotNull(e1.toString());

        EmailData e3 = new EmailData();
        e3.setMessageId("MSG-1");
        e3.setFromEmail("sender@test.com");
        e3.setSubject("RFQ Subject");
        e3.setBody("RFQ Body");
        e3.setReceivedTime(now);
        e3.setAttachmentPaths(List.of("path/1"));
        assertEquals(e1, e3);
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
        assertEquals("City", r1.getDeliveryCity());
        assertEquals("State", r1.getDeliveryState());
        assertEquals("123456", r1.getDeliveryPincode());
        assertEquals("2026-08-25", r1.getDeliveryDate());
        assertEquals(0, r1.getItems().size());

        ExtractedRFQ r2 = new ExtractedRFQ("buyer@test.com", "IT", "Location", "City", "State", "123456", "2026-08-25", List.of());
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotNull(r1.toString());

        ExtractedRFQ r3 = new ExtractedRFQ();
        r3.setBuyerEmail("buyer@test.com");
        r3.setCategory("IT");
        r3.setDeliveryLocation("Location");
        r3.setDeliveryCity("City");
        r3.setDeliveryState("State");
        r3.setDeliveryPincode("123456");
        r3.setDeliveryDate("2026-08-25");
        r3.setItems(List.of());
        assertEquals(r1, r3);
    }

    @Test
    @DisplayName("Test RFQItem getters, setters, builder, equals, hashCode, toString")
    void testRFQItemModel() {
        RFQItem item1 = RFQItem.builder()
                .itemDescription("Laptop")
                .partCode("P123")
                .specification("16GB RAM")
                .quantity(10.0)
                .uom("NOS")
                .brand("Dell")
                .category("IT")
                .division("Hardware")
                .categoryConfidence(0.95)
                .classificationStatus("MATCHED")
                .remarks("Remarks")
                .build();

        assertEquals("Laptop", item1.getItemDescription());
        assertEquals("P123", item1.getPartCode());
        assertEquals("16GB RAM", item1.getSpecification());
        assertEquals(10.0, item1.getQuantity());
        assertEquals("NOS", item1.getUom());
        assertEquals("Dell", item1.getBrand());
        assertEquals("IT", item1.getCategory());
        assertEquals("Hardware", item1.getDivision());
        assertEquals(0.95, item1.getCategoryConfidence());
        assertEquals("MATCHED", item1.getClassificationStatus());
        assertEquals("Remarks", item1.getRemarks());
        assertEquals("P123", item1.getEffectivePartNumber());

        RFQItem itemNoPartCode = RFQItem.builder().itemDescription("Laptop").build();
        assertEquals("RFQ-ITEM-PART-NOS", itemNoPartCode.getEffectivePartNumber());

        RFQItem item2 = new RFQItem("Laptop", "P123", "16GB RAM", 10.0, "NOS", "Dell", "IT", "Hardware", 0.95, "MATCHED", "Remarks");
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
        assertNotNull(item1.toString());

        RFQItem item3 = new RFQItem();
        item3.setItemDescription("Laptop");
        item3.setPartCode("P123");
        item3.setSpecification("16GB RAM");
        item3.setQuantity(10.0);
        item3.setUom("NOS");
        item3.setBrand("Dell");
        item3.setCategory("IT");
        item3.setDivision("Hardware");
        item3.setCategoryConfidence(0.95);
        item3.setClassificationStatus("MATCHED");
        item3.setRemarks("Remarks");
        assertEquals(item1, item3);
    }
}
