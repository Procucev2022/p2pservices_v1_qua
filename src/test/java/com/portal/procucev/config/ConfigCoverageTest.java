package com.portal.procucev.config;

import com.portal.procucev.ProcucevApplication;
import com.portal.procucev.rfq.config.RfqSchemaInitializer;
import com.portal.procucev.service.GMTService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigCoverageTest {

    @Mock
    private Environment environment;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private GMTService gmtService;

    @Test
    void testStartupConfigReportCoverage() {
        when(environment.getProperty(anyString())).thenReturn(null);
        StartupConfigReport report = new StartupConfigReport(environment);
        assertDoesNotThrow(report::reportMissingSecrets);

        when(environment.getProperty(anyString())).thenReturn("secretValue");
        assertDoesNotThrow(report::reportMissingSecrets);
    }

    @Test
    void testRfqSchemaInitializerCoverage() {
        RfqSchemaInitializer initializer = new RfqSchemaInitializer(jdbcTemplate);
        assertDoesNotThrow(initializer::initializeSchema);
        verify(jdbcTemplate, atLeastOnce()).execute(anyString());

        doThrow(new RuntimeException("DB Exception")).when(jdbcTemplate).execute(contains("ALTER TABLE"));
        assertDoesNotThrow(initializer::initializeSchema);
    }

    @Test
    void testEmailConfigCoverage() {
        EmailConfig config = new EmailConfig();
        ReflectionTestUtils.setField(config, "gmtService", gmtService);
        ReflectionTestUtils.setField(config, "self", config);
        ReflectionTestUtils.setField(config, "isEnabled", true);

        assertDoesNotThrow(config::onApplicationReady);
        assertDoesNotThrow(config::scheduleTaskWithCronExpressionsforForwardEmailToClient);

        doThrow(new RuntimeException("Forward exception")).when(gmtService).emailForwarder();
        assertDoesNotThrow(config::scheduleTaskWithCronExpressionsforForwardEmailToClient);

        ReflectionTestUtils.setField(config, "isEnabled", false);
        assertDoesNotThrow(config::onApplicationReady);
        assertDoesNotThrow(config::scheduleTaskWithCronExpressionsforForwardEmailToClient);
    }

    @Test
    void testProcucevApplicationCoverage() throws Exception {
        assertDoesNotThrow(() -> {
            ProcucevApplication app = new ProcucevApplication();
        });

        File envFile = new File(".env");
        boolean created = false;
        if (!envFile.exists()) {
            try (PrintWriter out = new PrintWriter(new FileWriter(envFile))) {
                out.println("# Comment");
                out.println("");
                out.println("TEST_CONFIG_KEY_1=TEST_VALUE_1");
                out.println("TEST_CONFIG_KEY_2=\"TEST_VALUE_2\"");
                out.println("TEST_CONFIG_KEY_3='TEST_VALUE_3'");
            }
            created = true;
        }

        try {
            ReflectionTestUtils.invokeMethod(ProcucevApplication.class, "loadDotEnvIfPresent");
        } finally {
            if (created && envFile.exists()) {
                envFile.delete();
            }
        }
    }
}
