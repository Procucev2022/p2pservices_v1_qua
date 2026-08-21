package com.portal.procucev.model;

import com.portal.procucev.Dto.ClientRFQDto;
import com.portal.procucev.Dto.GMTRfqVendorDto;
import com.portal.procucev.Dto.RfqDTO;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.RFQItem;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RfqCoverageTest {

    @Test
    void testRfqModelFullCoverage() {
        Rfq rfq = new Rfq();
        rfq.setSpecialInstruction("Special");
        Date now = new Date();
        rfq.setRfqClosingDate(now);
        Organization org = new Organization();
        rfq.setOrg(org);
        rfq.setByClient(true);
        rfq.setRfqApprovalFlag(true);
        rfq.setDivision("DIV");
        rfq.setQuotationReceived(true);
        rfq.setSourceType("W");
        rfq.setUser("user1");
        rfq.setCount(5);
        rfq.setQuoteCount(3);
        rfq.setQuoteSubmittedDate(now);
        rfq.setFromClient(true);

        RfqItem item = new RfqItem();
        rfq.setRfqItem(List.of(item));

        RfqVendor vendor = new RfqVendor();
        rfq.setRfqVendor(List.of(vendor));

        RFQDocument doc = new RFQDocument();
        rfq.setRfqDocument(List.of(doc));

        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        rfq.setClientdeliverylocationrfq(List.of(loc));

        rfq.setProjectDesc("Desc");
        rfq.setCategory("CAT");
        rfq.setDeliveryDate(now);
        rfq.setNoPrFlag(true);

        MasterStatus status = new MasterStatus();
        rfq.setStatus(status);
        rfq.setRfqStatus(status);
        rfq.setClientStatus(status);

        rfq.setBoqFileName("boq.xlsx");
        rfq.setBoqfile(new byte[]{1, 2, 3});
        rfq.setVendors(List.of(org));
        rfq.setNumberOfItems(10);
        rfq.setPrId("PR1");
        rfq.setNewCommentAvailableProcucev(true);
        rfq.setNewCommentAvailableVendor(true);
        rfq.setRfqId("RFQ-2026-100");
        rfq.setDescription("RFQ Description");
        rfq.setRequestType("TYPE");

        assertEquals("Special", rfq.getSpecialInstruction());
        assertEquals(now, rfq.getRfqClosingDate());
        assertEquals(org, rfq.getOrg());
        assertTrue(rfq.isByClient());
        assertTrue(rfq.isRfqApprovalFlag());
        assertEquals("DIV", rfq.getDivision());
        assertTrue(rfq.isQuotationReceived());
        assertEquals("W", rfq.getSourceType());
        assertEquals("user1", rfq.getUser());
        assertEquals(5, rfq.getCount());
        assertEquals(3, rfq.getQuoteCount());
        assertEquals(now, rfq.getQuoteSubmittedDate());
        assertTrue(rfq.isFromClient());
        assertEquals(1, rfq.getRfqItem().size());
        assertEquals(1, rfq.getRfqVendor().size());
        assertEquals(1, rfq.getRfqDocument().size());
        assertEquals(1, rfq.getClientdeliverylocationrfq().size());
        assertEquals("Desc", rfq.getProjectDesc());
        assertEquals("CAT", rfq.getCategory());
        assertEquals(now, rfq.getDeliveryDate());
        assertTrue(rfq.isNoPrFlag());
        assertEquals(status, rfq.getStatus());
        assertEquals(status, rfq.getRfqStatus());
        assertEquals(status, rfq.getClientStatus());
        assertEquals("boq.xlsx", rfq.getBoqFileName());
        assertArrayEquals(new byte[]{1, 2, 3}, rfq.getBoqfile());
        assertEquals(1, rfq.getVendors().size());
        assertEquals(10, rfq.getNumberOfItems());
        assertEquals("PR1", rfq.getPrId());
        assertTrue(rfq.isNewCommentAvailableProcucev());
        assertTrue(rfq.isNewCommentAvailableVendor());
        assertEquals("RFQ-2026-100", rfq.getRfqId());
        assertEquals("RFQ Description", rfq.getDescription());
        assertEquals("TYPE", rfq.getRequestType());

        assertNotNull(rfq.getDisplayRfqId());
        assertNotNull(rfq.getDisplayRfqNumber());
        assertNotNull(rfq.getRfqIcon());
        assertNotNull(rfq.toString());
    }

    @Test
    void testRfqDtoFullCoverage() {
        RfqDTO dto = new RfqDTO();
        Date now = new Date();
        dto.setId("ID123");
        dto.setRfqId("RFQ100");
        dto.setProjectDesc("Desc");
        dto.setCategory("CAT");
        dto.setCreatedBy("User");
        dto.setCompanyName("ORG");
        dto.setCompanyId("COMP1");
        dto.setPhoneNumber("9876543210");
        dto.setQuotationReceived(true);
        dto.setCount(2);
        MasterStatus status = new MasterStatus();
        dto.setClientStatus(status);
        dto.setClientStatusId("S1");
        dto.setClientStatusName("OPEN");
        dto.setDivision("DIV");
        dto.setCreatedTs(now);
        dto.setByClient(true);
        dto.setDeliveryDate(now);
        dto.setNoOfQuotes(3);
        dto.setNoOfVendors(5L);
        dto.setQuoteSubmittedDate(now);
        dto.setNewCommentAvailableVendor(true);
        dto.setSourceType("W");

        assertEquals("ID123", dto.getId());
        assertEquals("RFQ100", dto.getRfqId());
        assertEquals("Desc", dto.getProjectDesc());
        assertEquals("CAT", dto.getCategory());
        assertEquals("User", dto.getCreatedBy());
        assertEquals("ORG", dto.getCompanyName());
        assertEquals("COMP1", dto.getCompanyId());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertTrue(dto.isQuotationReceived());
        assertEquals(2, dto.getCount());
        assertEquals(status, dto.getClientStatus());
        assertEquals("S1", dto.getClientStatusId());
        assertEquals("OPEN", dto.getClientStatusName());
        assertEquals("DIV", dto.getDivision());
        assertEquals(now, dto.getCreatedTs());
        assertTrue(dto.isByClient());
        assertEquals(now, dto.getDeliveryDate());
        assertEquals(3, dto.getNoOfQuotes());
        assertEquals(5L, dto.getNoOfVendors());
        assertEquals(now, dto.getQuoteSubmittedDate());
        assertTrue(dto.isNewCommentAvailableVendor());
        assertEquals("W", dto.getSourceType());
        assertNotNull(dto.toString());

        RfqDTO dto2 = new RfqDTO();
        dto2.setId("ID123");
        assertEquals(dto, dto2);
        assertEquals(dto.hashCode(), dto2.hashCode());
    }

    @Test
    void testGMTRfqVendorDtoFullCoverage() {
        GMTRfqVendorDto dto = new GMTRfqVendorDto();
        dto.setId("ID");
        dto.setRfqId("RFQ1");
        dto.setDesc("Desc");
        dto.setDivision("Div");
        dto.setCategory("Cat");
        dto.setDeliveryLocation("Loc");

        assertEquals("ID", dto.getId());
        assertEquals("RFQ1", dto.getRfqId());
        assertEquals("Desc", dto.getDesc());
        assertEquals("Div", dto.getDivision());
        assertEquals("Cat", dto.getCategory());
        assertEquals("Loc", dto.getDeliveryLocation());
        assertNotNull(dto.toString());

        GMTRfqVendorDto dto2 = new GMTRfqVendorDto();
        dto2.setId("ID");
        assertEquals(dto, dto2);
        assertEquals(dto.hashCode(), dto2.hashCode());
    }

    @Test
    void testClientRFQDtoFullCoverage() {
        ClientRFQDto dto = new ClientRFQDto();
        dto.setId("ID1");
        dto.setProjectDesc("Desc");
        dto.setRfqId("RFQ1");
        dto.setDivision("DIV");
        dto.setUser("User");
        dto.setCreatedBy("Creator");

        assertEquals("ID1", dto.getId());
        assertEquals("Desc", dto.getProjectDesc());
        assertEquals("RFQ1", dto.getRfqId());
        assertEquals("DIV", dto.getDivision());
        assertEquals("User", dto.getUser());
        assertEquals("Creator", dto.getCreatedBy());
        assertNotNull(dto.toString());

        ClientRFQDto dto2 = new ClientRFQDto();
        dto2.setId("ID1");
        assertEquals(dto, dto2);
        assertEquals(dto.hashCode(), dto2.hashCode());
    }

    @Test
    void testRFQEntityCoverage() {
        RFQEntity entity = new RFQEntity();
        entity.setId(1L);
        entity.setRfqNumber("RFQ1");
        entity.setBuyerEmail("buyer@test.com");
        entity.setStatus("OPEN");
        entity.setRawSubject("Subject");
        entity.setItemsJson("[]");
        entity.setDeliveryLocation("Location");
        entity.setDeliveryDate("2026-12-31");
        entity.setRemarks("Remarks");

        assertEquals(1L, entity.getId());
        assertEquals("RFQ1", entity.getRfqNumber());
        assertEquals("buyer@test.com", entity.getBuyerEmail());
        assertEquals("OPEN", entity.getStatus());
        assertEquals("Subject", entity.getRawSubject());
        assertEquals("[]", entity.getItemsJson());
        assertEquals("Location", entity.getDeliveryLocation());
        assertEquals("2026-12-31", entity.getDeliveryDate());
        assertEquals("Remarks", entity.getRemarks());

        assertNotNull(entity.toString());
        RFQEntity entity2 = new RFQEntity(1L, "RFQ1", "buyer@test.com", "OPEN", "Subject", "[]", "Location", "2026-12-31", "Remarks", null, null);
        assertEquals(entity, entity2);
        assertEquals(entity.hashCode(), entity2.hashCode());
    }

    @Test
    void testRFQItemModelCoverage() {
        RFQItem item = new RFQItem();
        item.setItemDescription("Desc");
        item.setSpecification("Spec");
        item.setUom("NOS");
        item.setQuantity(10.0);
        item.setRemarks("Rem");
        item.setBrand("Brand");

        assertEquals("Desc", item.getItemDescription());
        assertEquals("Spec", item.getSpecification());
        assertEquals("NOS", item.getUom());
        assertEquals(10.0, item.getQuantity());
        assertEquals("Rem", item.getRemarks());
        assertEquals("Brand", item.getBrand());

        RFQItem item2 = RFQItem.builder()
                .itemDescription("Desc")
                .specification("Spec")
                .uom("NOS")
                .quantity(10.0)
                .remarks("Rem")
                .brand("Brand")
                .build();
        assertEquals(item, item2);
        assertEquals(item.hashCode(), item2.hashCode());
        assertNotNull(item.toString());
    }
}
