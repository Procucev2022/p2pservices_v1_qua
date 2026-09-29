package com.portal.procucev.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.portal.procucev.service.DailyRfqReportService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyRfqReportSchedulerTest {

    @Mock
    private DailyRfqReportService dailyRfqReportService;

    @InjectMocks
    private DailyRfqReportScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "isEnabled", true);
    }

    @Test
    void testRunDailyRfqReportJobWhenEnabled() {
        scheduler.runDailyRfqReportJob();
        verify(dailyRfqReportService).dispatchDailyReport(null, false, null);
    }

    @Test
    void testRunDailyRfqReportJobWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "isEnabled", false);
        scheduler.runDailyRfqReportJob();
        verify(dailyRfqReportService, never()).dispatchDailyReport(any(), anyBoolean(), any());
    }

    @Test
    void testRunDailyRfqReportJobCatchesThrowable() {
        when(dailyRfqReportService.dispatchDailyReport(null, false, null))
                .thenThrow(new RuntimeException("Simulated DB or SMTP failure"));

        assertDoesNotThrow(() -> scheduler.runDailyRfqReportJob());
    }
}
