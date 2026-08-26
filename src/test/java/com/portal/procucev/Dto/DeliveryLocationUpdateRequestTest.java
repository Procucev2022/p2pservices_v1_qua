package com.portal.procucev.Dto;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryLocationUpdateRequestTest {

    @Test
    void testGettersSettersAndConstructors() {
        Date deliveryDate = new Date();
        DeliveryLocationUpdateRequest request = new DeliveryLocationUpdateRequest(
                "ID123", "RFQ123", "Raigarh", "Chhattisgarh", "496001", "Industrial Area", deliveryDate
        );

        assertEquals("ID123", request.getId());
        assertEquals("RFQ123", request.getRfqId());
        assertEquals("Raigarh", request.getCity());
        assertEquals("Chhattisgarh", request.getState());
        assertEquals("496001", request.getPincode());
        assertEquals("Industrial Area", request.getAddress());
        assertEquals(deliveryDate, request.getDeliveryDate());

        DeliveryLocationUpdateRequest empty = new DeliveryLocationUpdateRequest();
        empty.setId("ID456");
        empty.setRfqId("RFQ456");
        empty.setCity("Pune");
        empty.setState("Maharashtra");
        empty.setPincode("411001");
        empty.setAddress("Hinjawadi");
        empty.setDeliveryDate(deliveryDate);

        assertEquals("ID456", empty.getId());
        assertEquals("RFQ456", empty.getRfqId());
        assertEquals("Pune", empty.getCity());
        assertEquals("Maharashtra", empty.getState());
        assertEquals("411001", empty.getPincode());
        assertEquals("Hinjawadi", empty.getAddress());
        assertEquals(deliveryDate, empty.getDeliveryDate());

        assertNotNull(empty.toString());
        assertEquals(empty, empty);
        assertNotEquals(empty, request);
        assertNotEquals(0, empty.hashCode());
    }
}
