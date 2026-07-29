package com.portal.procucev.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientRegistrationStatusTest {

    @Test
    void testEnumValues() {
        ClientRegistrationStatus[] values = ClientRegistrationStatus.values();
        assertEquals(2, values.length);
        assertEquals(ClientRegistrationStatus.NEW_CLIENT, ClientRegistrationStatus.valueOf("NEW_CLIENT"));
        assertEquals(ClientRegistrationStatus.EXISTING_CLIENT, ClientRegistrationStatus.valueOf("EXISTING_CLIENT"));
    }
}
