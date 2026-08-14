package com.portal.procucev.utils;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.InitialDirContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class EmailValidatorUtilTest {

    @Test
    void testConstructor() {
        EmailValidatorUtil util = new EmailValidatorUtil();
        assertNotNull(util);
    }

    @Test
    void testValidateEmail_Valid_InvalidDomain_Malformed_Null_Blank() {
        List<String> invalidEmails = new ArrayList<>();

        EmailValidatorUtil.validateEmail("user@gmail.com", invalidEmails);

        EmailValidatorUtil.validateEmail("user@invalid-domain-99999.com", invalidEmails);

        EmailValidatorUtil.validateEmail("@malformed", invalidEmails);

        EmailValidatorUtil.validateEmail(null, invalidEmails);

        EmailValidatorUtil.validateEmail("   ", invalidEmails);

        assertFalse(invalidEmails.isEmpty());
    }

    @Test
    void testValidateEmails_List() {
        List<String> emails = Arrays.asList("user@gmail.com", "user@invalid-domain-99999.com", "@malformed", null, "   ");
        List<String> invalidEmails = new ArrayList<>();

        EmailValidatorUtil.validateEmails(emails, invalidEmails);
        assertFalse(invalidEmails.isEmpty());

        invalidEmails.clear();
        EmailValidatorUtil.validateEmails(null, invalidEmails);
        assertTrue(invalidEmails.isEmpty());
    }

    /**
     * The MX lookup is stubbed at construction time so the outcome does not depend on DNS being
     * reachable from the build machine.
     */
    @Test
    void testDomainValidationForEmptyNullAndPopulatedMxRecords() {
        List<String> invalidEmails = new ArrayList<>();

        // no MX records for the domain
        try (MockedConstruction<InitialDirContext> ignored = mockConstruction(InitialDirContext.class,
                (mock, context) -> when(mock.getAttributes(anyString(), any(String[].class)))
                        .thenReturn(new BasicAttributes()))) {
            EmailValidatorUtil.validateEmail("user@example.com", invalidEmails);
            assertEquals(List.of("user@example.com"), invalidEmails);
        }

        // the directory returns nothing at all
        invalidEmails.clear();
        try (MockedConstruction<InitialDirContext> ignored = mockConstruction(InitialDirContext.class,
                (mock, context) -> when(mock.getAttributes(anyString(), any(String[].class)))
                        .thenReturn(null))) {
            EmailValidatorUtil.validateEmails(List.of("user@example.com"), invalidEmails);
            assertEquals(List.of("user@example.com"), invalidEmails);
        }

        // a real MX record makes the address acceptable
        invalidEmails.clear();
        BasicAttributes mxRecords = new BasicAttributes();
        mxRecords.put(new BasicAttribute("MX", "10 mx.example.com"));
        try (MockedConstruction<InitialDirContext> ignored = mockConstruction(InitialDirContext.class,
                (mock, context) -> when(mock.getAttributes(anyString(), any(String[].class)))
                        .thenReturn(mxRecords))) {
            EmailValidatorUtil.validateEmail("user@example.com", invalidEmails);
            assertTrue(invalidEmails.isEmpty());
        }
    }
}
