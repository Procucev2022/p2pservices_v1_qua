package com.portal.procucev.utils;

import java.util.Hashtable;
import java.util.List;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import jakarta.mail.internet.InternetAddress;

public class EmailValidatorUtil {
	 // --- Validate email format + MX record ---
    public static void validateEmails(List<String> emails, List<String> invalidEmails) {
        if (emails != null) {
            for (String email : emails) {
                if (email != null && !email.isBlank()) {
                    try {
                        // Validate format
                        new InternetAddress(email, true);

                        // Validate domain MX record
                        if (!isDomainValid(email)) {
                            invalidEmails.add(email);
                        }
                    } catch (Exception e) {
                        invalidEmails.add(email);
                    }
                }
            }
        }
    }

    private static boolean isDomainValid(String email) {
        try {
            String domain = email.substring(email.indexOf("@") + 1);
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});
            return attrs != null && attrs.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static void validateEmail(String email, List<String> invalidEmails) {
        if (email != null && !email.isBlank()) {
            try {
                // Validate format
                new InternetAddress(email, true);

                // Validate domain MX record
                if (!isDomainValid(email)) {
                    invalidEmails.add(email);
                }
            } catch (Exception e) {
                invalidEmails.add(email);
            }
        }
    }

}