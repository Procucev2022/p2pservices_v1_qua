package com.portal.procucev.rfq.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RFQItemCoverageTest {

    @Test
    void testRFQItemFullCoverage() {
        RFQItem item = RFQItem.builder()
                .itemDescription("Laptop")
                .quantity(10.0)
                .uom("Units")
                .partNumber("PN-123")
                .specification("16GB RAM")
                .brand("Dell")
                .category("IT Hardware")
                .division("IT")
                .deliveryLocation("Bengaluru")
                .deliveryDate("2026-09-30")
                .remarks("Urgent")
                .categoryConfidence(0.95)
                .classificationStatus("AI_EXTRACTED")
                .build();

        assertEquals("Laptop", item.getItemDescription());
        assertEquals(10.0, item.getQuantity());
        assertEquals("Units", item.getUom());
        assertEquals("PN-123", item.getEffectivePartNumber());
        assertEquals("16GB RAM", item.getSpecification());
        assertEquals("Dell", item.getBrand());
        assertEquals("IT Hardware", item.getCategory());
        assertEquals("IT", item.getDivision());
        assertEquals("Bengaluru", item.getDeliveryLocation());
        assertEquals("2026-09-30", item.getDeliveryDate());
        assertEquals("Urgent", item.getRemarks());
        assertEquals(0.95, item.getCategoryConfidence());
        assertEquals("AI_EXTRACTED", item.getClassificationStatus());

        RFQItem item2 = new RFQItem();
        item2.setItemDescription("Laptop");
        item2.setQuantity(10.0);
        item2.setUom("Units");
        item2.setPartNumber("PN-123");
        item2.setSpecification("16GB RAM");
        item2.setBrand("Dell");
        item2.setCategory("IT Hardware");
        item2.setDivision("IT");
        item2.setDeliveryLocation("Bengaluru");
        item2.setDeliveryDate("2026-09-30");
        item2.setRemarks("Urgent");
        item2.setCategoryConfidence(0.95);
        item2.setClassificationStatus("AI_EXTRACTED");

        assertEquals(item, item2);
        assertEquals(item.hashCode(), item2.hashCode());
        assertNotNull(item.toString());

        // Test string setter for quantity
        RFQItem stringQtyItem = new RFQItem();
        stringQtyItem.setQuantity("twenty units");
        assertEquals(20.0, stringQtyItem.getQuantity());
        assertEquals("Units", stringQtyItem.getUom());

        stringQtyItem.setQuantity("100");
        assertEquals(100.0, stringQtyItem.getQuantity());

        stringQtyItem.setQuantity((Object) 50);
        assertEquals(50.0, stringQtyItem.getQuantity());

        // Test alias setters when description/date/location are null vs non-null
        RFQItem aliasItem = new RFQItem();
        aliasItem.setDescriptionAlias("Description A");
        assertEquals("Description A", aliasItem.getItemDescription());
        aliasItem.setDescriptionAlias("Description B"); // should not overwrite
        assertEquals("Description A", aliasItem.getItemDescription());

        aliasItem.setDeliveryDateAlias("2026-10-01");
        assertEquals("2026-10-01", aliasItem.getDeliveryDate());
        aliasItem.setDeliveryDateAlias("2026-11-01"); // should not overwrite
        assertEquals("2026-10-01", aliasItem.getDeliveryDate());

        aliasItem.setDeliveryLocationAlias("Hyderabad");
        assertEquals("Hyderabad", aliasItem.getDeliveryLocation());
        aliasItem.setDeliveryLocationAlias("Chennai"); // should not overwrite
        assertEquals("Hyderabad", aliasItem.getDeliveryLocation());

        // Test getEffectivePartNumber branches
        RFQItem codeItem = new RFQItem();
        assertEquals("", codeItem.getEffectivePartNumber());

        codeItem.setModelNumber("MOD-99");
        assertEquals("MOD-99", codeItem.getEffectivePartNumber());

        codeItem.setPartNumber("PN-88");
        assertEquals("PN-88", codeItem.getEffectivePartNumber());

        codeItem.setPartCode("PC-77");
        assertEquals("PC-77", codeItem.getEffectivePartNumber());

        // Test other model records
        Buyer buyer = new Buyer("John", "john@example.com", "9876543210", "Acme Corp");
        assertEquals("John", buyer.name());
        assertEquals("john@example.com", buyer.email());
        assertEquals("9876543210", buyer.phone());
        assertEquals("Acme Corp", buyer.company());

        InlineImage img = new InlineImage("cid1", "image/png", new byte[]{1, 2, 3});
        assertEquals("cid1", img.contentId());
        assertEquals("image/png", img.contentType());
        assertArrayEquals(new byte[]{1, 2, 3}, img.data());

        EmailData emailData = new EmailData("MSG-1", "sender@test.com", "Test Subject", "Body text", "2026-08-20", java.util.List.of());
        assertEquals("MSG-1", emailData.messageId());
        assertEquals("sender@test.com", emailData.senderEmail());
        assertEquals("Test Subject", emailData.subject());
        assertEquals("Body text", emailData.bodyText());
        assertEquals("2026-08-20", emailData.receivedDate());
        assertTrue(emailData.attachments().isEmpty());

        ExtractedRFQ rfq = new ExtractedRFQ("Acme", "2026-09-01", "Bengaluru", java.util.List.of(item), "Urgent PR");
        assertEquals("Acme", rfq.companyName());
        assertEquals("2026-09-01", rfq.deliveryDate());
        assertEquals("Bengaluru", rfq.deliveryLocation());
        assertEquals(1, rfq.items().size());
        assertEquals("Urgent PR", rfq.specialInstructions());
    }
}
