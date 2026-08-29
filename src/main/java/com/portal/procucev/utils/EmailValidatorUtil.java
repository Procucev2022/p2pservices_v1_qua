package com.portal.procucev.utils;

import java.util.Hashtable;
import java.util.List;

import javax.naming.NameNotFoundException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import jakarta.mail.internet.InternetAddress;

public class EmailValidatorUtil {

    /** Keeps a slow or unreachable resolver from stalling the request thread. */
    private static final String DNS_TIMEOUT_MS = "3000";
    private static final String DNS_RETRIES = "1";

    // --- Validate email format + deliverable domain ---
    public static void validateEmails(List<String> emails, List<String> invalidEmails) {
        if (emails != null) {
            for (String email : emails) {
                validateEmail(email, invalidEmails);
            }
        }
    }

    public static void validateEmail(String email, List<String> invalidEmails) {
        if (email == null || email.isBlank()) {
            return;
        }

        // 1. Format check
        if (!isFormatValid(email)) {
            invalidEmails.add(email);
            return;
        }

        // 2. Domain check
        if (!isDomainValid(email)) {
            invalidEmails.add(email);
        }
    }

    /**
     * Syntax-only check, with no DNS lookup.
     *
     * Use this where a malformed address should be rejected but an
     * undeliverable one should not block the operation, such as taking a
     * payment where the user is redirected to the gateway in their browser
     * and the receipt email is only a convenience.
     */
    public static boolean isFormatValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        try {
            new InternetAddress(email, true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** True when the address is syntactically valid but its domain cannot receive mail. */
    public static boolean isDeliverable(String email) {
        return isFormatValid(email) && isDomainValid(email);
    }

    /**
     * Confirms the domain can receive mail.
     *
     * Per RFC 5321 section 5.1 a domain with no MX record is still deliverable if
     * it resolves to an address record, so both are checked. A transient lookup
     * failure is treated as valid so a DNS outage cannot lock users out; only a
     * domain that definitively does not exist is rejected.
     */
    private static boolean isDomainValid(String email) {
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return false;
        }
        String domain = email.substring(at + 1);

        DirContext ctx = null;
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", DNS_TIMEOUT_MS);
            env.put("com.sun.jndi.dns.timeout.retries", DNS_RETRIES);
            ctx = new InitialDirContext(env);

            Attributes attrs = ctx.getAttributes(domain, new String[] { "MX", "A", "AAAA" });
            if (attrs == null) {
                return false;
            }
            return attrs.get("MX") != null
                    || attrs.get("A") != null
                    || attrs.get("AAAA") != null;

        } catch (NameNotFoundException e) {
            // Domain does not exist, so mail can never be delivered.
            return false;
        } catch (Exception e) {
            // Resolver unreachable or timed out: do not block the user on infrastructure.
            return true;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception ignored) {
                    // Nothing actionable if the context fails to close.
                }
            }
        }
    }
}
