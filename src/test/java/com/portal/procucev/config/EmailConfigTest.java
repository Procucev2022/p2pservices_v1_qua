package com.portal.procucev.config;

import com.portal.procucev.service.GMTService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConfigTest {

    @Mock
    private GMTService gmtService;

    @InjectMocks
    private EmailConfig config;

    @Test
    void testScheduleTask_Enabled_Success() {
        ReflectionTestUtils.setField(config, "isEnabled", true);

        config.scheduleTaskWithCronExpressionsforForwardEmailToClient();
        verify(gmtService).emailForwarder();
    }

    @Test
    void testScheduleTask_Enabled_Exception() {
        ReflectionTestUtils.setField(config, "isEnabled", true);
        doThrow(new RuntimeException("mail forward error")).when(gmtService).emailForwarder();

        config.scheduleTaskWithCronExpressionsforForwardEmailToClient();
        verify(gmtService).emailForwarder();
    }

    @Test
    void testScheduleTask_Disabled() {
        ReflectionTestUtils.setField(config, "isEnabled", false);

        config.scheduleTaskWithCronExpressionsforForwardEmailToClient();
        verify(gmtService, never()).emailForwarder();
    }

    @Test
    void testOnApplicationReady_Enabled() {
        EmailConfig selfMock = mock(EmailConfig.class);
        ReflectionTestUtils.setField(config, "isEnabled", true);
        ReflectionTestUtils.setField(config, "self", selfMock);

        config.onApplicationReady();
    }

    @Test
    void testOnApplicationReady_Disabled() {
        EmailConfig selfMock = mock(EmailConfig.class);
        ReflectionTestUtils.setField(config, "isEnabled", false);
        ReflectionTestUtils.setField(config, "self", selfMock);

        config.onApplicationReady();
        verifyNoInteractions(selfMock);
    }
}
