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
                .partCode("PC-999")
                .partNumber("PN-123")
                .modelNumber("MD-456")
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
        assertEquals("PC-999", item.getEffectivePartNumber());
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
        item2.setPartCode("PC-999");
        item2.setPartNumber("PN-123");
        item2.setModelNumber("MD-456");
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

        // Test quantity & UOM behavior
        RFQItem stringQtyItem = new RFQItem();
        stringQtyItem.setUom("Pieces");
        stringQtyItem.setQuantity("twenty units");
        // UOM should not be overwritten if already set
        assertEquals("Pieces", stringQtyItem.getUom());
        assertEquals(20.0, stringQtyItem.getQuantity());

        stringQtyItem.setUom("");
        stringQtyItem.setQuantity("500 bags");
        assertEquals("Bags", stringQtyItem.getUom());
        assertEquals(500.0, stringQtyItem.getQuantity());

        stringQtyItem.setUom(null);
        stringQtyItem.setQuantity("100");
        assertEquals(100.0, stringQtyItem.getQuantity());
        assertNull(stringQtyItem.getUom());

        stringQtyItem.setQuantity((Object) 50);
        assertEquals(50.0, stringQtyItem.getQuantity());

        stringQtyItem.setQuantity(null);
        assertNull(stringQtyItem.getQuantity());

        // Test aliases
        RFQItem aliasItem = new RFQItem();
        aliasItem.setDescriptionAlias("Desc 1");
        assertEquals("Desc 1", aliasItem.getItemDescription());
        aliasItem.setDescriptionAlias("Desc 2");
        assertEquals("Desc 1", aliasItem.getItemDescription());

        aliasItem.setItemDescription("  ");
        aliasItem.setDescriptionAlias("New Desc");
        assertEquals("New Desc", aliasItem.getItemDescription());

        aliasItem.setDeliveryDateAlias("2026-10-01");
        assertEquals("2026-10-01", aliasItem.getDeliveryDate());
        aliasItem.setDeliveryDateAlias("2026-10-02");
        assertEquals("2026-10-01", aliasItem.getDeliveryDate());

        aliasItem.setDeliveryDate("   ");
        aliasItem.setDeliveryDateAlias("2026-11-01");
        assertEquals("2026-11-01", aliasItem.getDeliveryDate());

        aliasItem.setDeliveryLocationAlias("Hyderabad");
        assertEquals("Hyderabad", aliasItem.getDeliveryLocation());
        aliasItem.setDeliveryLocationAlias("Chennai");
        assertEquals("Hyderabad", aliasItem.getDeliveryLocation());

        aliasItem.setDeliveryLocation("");
        aliasItem.setDeliveryLocationAlias("Kolkata");
        assertEquals("Kolkata", aliasItem.getDeliveryLocation());

        // Test getEffectivePartNumber fallback branches
        RFQItem partItem = new RFQItem();
        assertEquals("", partItem.getEffectivePartNumber());

        partItem.setPartCode("   ");
        partItem.setPartNumber("   ");
        partItem.setModelNumber("   ");
        assertEquals("", partItem.getEffectivePartNumber());

        partItem.setModelNumber("MD-1");
        assertEquals("MD-1", partItem.getEffectivePartNumber());

        partItem.setPartNumber("PN-1");
        assertEquals("PN-1", partItem.getEffectivePartNumber());

        partItem.setPartCode("PC-1");
        assertEquals("PC-1", partItem.getEffectivePartNumber());
    }
}
