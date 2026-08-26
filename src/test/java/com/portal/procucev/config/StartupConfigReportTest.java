package com.portal.procucev.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartupConfigReportTest {

    @Mock
    private Environment environment;

    @Test
    void testReportMissingSecrets_AllPresent() {
        when(environment.getProperty(anyString())).thenReturn("some-secret-value");

        StartupConfigReport report = new StartupConfigReport(environment);
        report.reportMissingSecrets();

        verify(environment, atLeast(1)).getProperty("jwt.secret");
    }

    @Test
    void testReportMissingSecrets_SomeMissingOrBlank() {
        when(environment.getProperty("jwt.secret")).thenReturn(null);
        when(environment.getProperty("spring.mail.password")).thenReturn("");
        when(environment.getProperty("mailPassword")).thenReturn("   ");
        when(environment.getProperty("quapassword")).thenReturn("valid-pass");

        StartupConfigReport report = new StartupConfigReport(environment);
        report.reportMissingSecrets();

        verify(environment, atLeast(1)).getProperty("jwt.secret");
    }
}
