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
                .rfqNumber("RFQ-20260818041109-hexbolts")
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
        assertEquals("RFQ-20260818041109-hexbolts", entity.getRfqNumber());
        assertEquals("buyer@example.com", entity.getBuyerEmail());
        assertEquals("SUCCESS", entity.getStatus());
        assertEquals("Subject", entity.getRawSubject());
        assertEquals("[{}]", entity.getItemsJson());
        assertEquals("Bengaluru", entity.getDeliveryLocation());
        assertEquals("2026-09-30", entity.getDeliveryDate());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());

        assertNotNull(entity.getDisplayRfqNumber());
        assertNotNull(entity.getShortRfqNumber());

        entity.onCreate();
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());

        entity.onUpdate();
        assertNotNull(entity.getUpdatedAt());

        RFQEntity entity2 = new RFQEntity();
        entity2.setId(1L);
        entity2.setRfqNumber("RFQ-20260818041109-hexbolts");
        entity2.setBuyerEmail("buyer@example.com");
        entity2.setStatus("SUCCESS");
        entity2.setRawSubject("Subject");
        entity2.setItemsJson("[{}]");
        entity2.setDeliveryLocation("Bengaluru");
        entity2.setDeliveryDate("2026-09-30");
        entity2.setCreatedAt(entity.getCreatedAt());
        entity2.setUpdatedAt(entity.getUpdatedAt());

        assertEquals(entity, entity2);
        assertEquals(entity.hashCode(), entity2.hashCode());
        assertNotNull(entity.toString());
    }
}
