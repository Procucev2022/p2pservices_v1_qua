package com.portal.procucev.rfq;

import com.portal.procucev.rfq.dto.ProcessingStats;
import com.portal.procucev.rfq.scheduler.EmailScheduler;
import com.portal.procucev.rfq.service.EmailProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class EmailSchedulerTest {

    private EmailProcessorService emailProcessorService;
    private EmailScheduler scheduler;

    @BeforeEach
    void setUp() {
        emailProcessorService = Mockito.mock(EmailProcessorService.class);
        scheduler = new EmailScheduler(emailProcessorService);
    }

    @Test
    @DisplayName("Test runEmailProcessingJob when enabled")
    void testRunEmailProcessingJobEnabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);

        ProcessingStats stats = ProcessingStats.builder().emailsProcessed(2).rfqsCreated(2).status("SUCCESS").build();
        Mockito.when(emailProcessorService.processUnreadEmails()).thenReturn(stats);

        scheduler.runEmailProcessingJob();

        verify(emailProcessorService).processUnreadEmails();
    }

    @Test
    @DisplayName("Test runEmailProcessingJob when disabled")
    void testRunEmailProcessingJobDisabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.runEmailProcessingJob();

        verify(emailProcessorService, never()).processUnreadEmails();
    }

    @Test
    @DisplayName("Test onApplicationReady when disabled")
    void testOnApplicationReadyDisabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.onApplicationReady();

        verify(emailProcessorService, never()).processUnreadEmails();
    }

    @Test
    @DisplayName("Test runEmailProcessingJob exception handling")
    void testRunEmailProcessingJobException() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
        Mockito.when(emailProcessorService.processUnreadEmails()).thenThrow(new RuntimeException("Scheduler Exception"));

        assertDoesNotThrow(() -> scheduler.runEmailProcessingJob());
    }

    @Test
    @DisplayName("Test onApplicationReady when enabled")
    void testOnApplicationReadyEnabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
        assertDoesNotThrow(() -> scheduler.onApplicationReady());
    }
}
