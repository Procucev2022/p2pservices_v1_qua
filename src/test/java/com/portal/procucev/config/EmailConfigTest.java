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
    void testScheduleTask_Enabled() {
        ReflectionTestUtils.setField(config, "isEnabled", true);

        config.scheduleTaskWithCronExpressionsforForwardEmailToClient();
        verify(gmtService).emailForwarder();
    }

    @Test
    void testScheduleTask_Disabled() {
        ReflectionTestUtils.setField(config, "isEnabled", false);

        config.scheduleTaskWithCronExpressionsforForwardEmailToClient();
        verify(gmtService, never()).emailForwarder();
    }
}
