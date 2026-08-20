package com.portal.procucev.rfq.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class RFQEntityCoverageTest {

    @Test
    void testRFQEntityFullCoverage() {
        LocalDateTime now = LocalDateTime.now();
        RFQEntity entity = RFQEntity.builder()
                .id(1L)
                .rfqNumber("RFQ-100")
                .buyerEmail("buyer@example.com")
                .status("SUCCESS")
                .rawSubject("Subject")
                .itemsJson("[{}]")
                .deliveryLocation("Bengaluru")
                .deliveryDate("2026-09-30")
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(1L, entity.getId());
        assertEquals("RFQ-100", entity.getRfqNumber());
        assertEquals("buyer@example.com", entity.getBuyerEmail());
        assertEquals("SUCCESS", entity.getStatus());
        assertEquals("Subject", entity.getRawSubject());
        assertEquals("[{}]", entity.getItemsJson());
        assertEquals("Bengaluru", entity.getDeliveryLocation());
        assertEquals("2026-09-30", entity.getDeliveryDate());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());

        RFQEntity entity2 = new RFQEntity();
        entity2.setId(1L);
        entity2.setRfqNumber("RFQ-100");
        entity2.setBuyerEmail("buyer@example.com");
        entity2.setStatus("SUCCESS");
        entity2.setRawSubject("Subject");
        entity2.setItemsJson("[{}]");
        entity2.setDeliveryLocation("Bengaluru");
        entity2.setDeliveryDate("2026-09-30");
        entity2.setCreatedAt(now);
        entity2.setUpdatedAt(now);

        assertEquals(entity, entity2);
        assertEquals(entity.hashCode(), entity2.hashCode());
        assertNotNull(entity.toString());

        BuyerEntity buyer = BuyerEntity.builder()
                .id(10L)
                .email("buyer@test.com")
                .name("John")
                .companyName("Acme")
                .phone("1234567890")
                .verified(true)
                .registered(true)
                .build();
        assertEquals(10L, buyer.getId());
        assertEquals("buyer@test.com", buyer.getEmail());
        assertEquals("John", buyer.getName());
        assertEquals("Acme", buyer.getCompanyName());
        assertEquals("1234567890", buyer.getPhone());
        assertTrue(buyer.isVerified());
        assertTrue(buyer.isRegistered());

        BuyerEntity buyer2 = new BuyerEntity();
        buyer2.setId(10L);
        buyer2.setEmail("buyer@test.com");
        buyer2.setName("John");
        buyer2.setCompanyName("Acme");
        buyer2.setPhone("1234567890");
        buyer2.setVerified(true);
        buyer2.setRegistered(true);
        assertEquals(buyer, buyer2);
        assertNotNull(buyer.toString());

        CategoryMasterEntity cat = CategoryMasterEntity.builder().id(1L).categoryName("Pumps").division("Mechanical").build();
        assertEquals("Pumps", cat.getCategoryName());
        assertEquals("Mechanical", cat.getDivision());
        CategoryMasterEntity cat2 = new CategoryMasterEntity(1L, "Pumps", "Mechanical");
        assertEquals(cat, cat2);

        CategoryItemMappingEntity map = CategoryItemMappingEntity.builder().id(2L).itemName("Pump").categoryName("Pumps").build();
        assertEquals("Pump", map.getItemName());
        assertEquals("Pumps", map.getCategoryName());
        CategoryItemMappingEntity map2 = new CategoryItemMappingEntity(2L, "Pump", "Pumps");
        assertEquals(map, map2);

        EmailTransaction tx = EmailTransaction.builder().id(3L).messageId("M-1").senderEmail("s@test.com").status("SUCCESS").build();
        assertEquals("M-1", tx.getMessageId());
        assertEquals("s@test.com", tx.getSenderEmail());
        assertEquals("SUCCESS", tx.getStatus());

        RfqItemRecord itemRec = RfqItemRecord.builder().id(4L).rfqNumber("RFQ-1").itemDescription("Pump").quantity(5.0).uom("Nos").build();
        assertEquals("RFQ-1", itemRec.getRfqNumber());
        assertEquals("Pump", itemRec.getItemDescription());
        assertEquals(5.0, itemRec.getQuantity());
        assertEquals("Nos", itemRec.getUom());
    }
}
