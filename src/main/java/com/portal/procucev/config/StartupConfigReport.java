package com.portal.procucev.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Reports missing secrets once the application is up.
 *
 * <p>Every secret below is read from an environment variable. Those placeholders
 * previously had no default, so a single unset App Service setting aborted the
 * application context. Tomcat was then left with no deployed context and
 * answered 404 to every request, including {@code /actuator/health}, which made
 * the real cause invisible from the outside: browsers reported it as a CORS
 * failure because the 404 preflight carried no CORS headers.
 *
 * <p>The placeholders now default to empty so startup always completes. This
 * component makes the resulting gap obvious in the log instead, and each
 * subsystem raises a specific error when it actually needs the missing value.
 */
@Component
public class StartupConfigReport {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupConfigReport.class);

    /** Property name -> environment variable that supplies it. */
    private static final Map<String, String> REQUIRED_SECRETS = new LinkedHashMap<>();

    static {
        REQUIRED_SECRETS.put("jwt.secret", "JWT_SECRET");
        REQUIRED_SECRETS.put("spring.mail.password", "SMTP_PASSWORD");
        REQUIRED_SECRETS.put("mailPassword", "MAIL_PASSWORD");
        REQUIRED_SECRETS.put("quapassword", "QUA_EMAIL_PASSWORD");
        REQUIRED_SECRETS.put("app.oauth2.client-secret", "OAUTH2_CLIENT_SECRET");
        REQUIRED_SECRETS.put("sms.api.authkey", "SMS_API_AUTHKEY");
        REQUIRED_SECRETS.put("zoho.client.secret", "ZOHO_CLIENT_SECRET");
        REQUIRED_SECRETS.put("zoho.refresh.token", "ZOHO_REFRESH_TOKEN");
        REQUIRED_SECRETS.put("app.mail.password", "EMAIL_PASSWORD");
        REQUIRED_SECRETS.put("app.gemini.api-key", "GEMINI_API_KEY");
    }

    private final Environment environment;

    public StartupConfigReport(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reportMissingSecrets() {
        List<String> missing = new ArrayList<>();

        for (Map.Entry<String, String> entry : REQUIRED_SECRETS.entrySet()) {
            String value = environment.getProperty(entry.getKey());
            if (value == null || value.isBlank()) {
                missing.add(entry.getValue() + " (" + entry.getKey() + ")");
            }
        }

        if (missing.isEmpty()) {
            LOGGER.info("Startup configuration check: all {} required secrets are present.",
                    REQUIRED_SECRETS.size());
            return;
        }

        // Names only. Never log the values themselves.
        LOGGER.error("Startup configuration check: {} of {} required secrets are missing or empty. "
                        + "The features that depend on them will fail until these are set: {}",
                missing.size(), REQUIRED_SECRETS.size(), String.join(", ", missing));
    }
}
