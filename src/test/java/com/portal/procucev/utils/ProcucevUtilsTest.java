package com.portal.procucev.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProcucevUtilsTest {

    @Test
    void testConstructor() {
        ProcucevUtils utils = new ProcucevUtils();
        assertNotNull(utils);
    }

    @Test
    void testGeneratePassword() {
        char[] password = ProcucevUtils.generatePassword(10);
        assertNotNull(password);
        assertEquals(10, password.length);

        // Check password characters rules
        String pwd = new String(password);
        assertTrue(pwd.length() == 10);
    }
}
