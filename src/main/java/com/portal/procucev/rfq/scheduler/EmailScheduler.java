package com.portal.procucev.rfq.scheduler;

import com.portal.procucev.rfq.dto.ProcessingStats;
import com.portal.procucev.rfq.service.EmailProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailScheduler {

    private final EmailProcessorService emailProcessorService;

    @Value("${app.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!schedulerEnabled) {
            return;
        }
        log.info("Application started: Triggering immediate initial Email RFQ processing run...");
        CompletableFuture.runAsync(this::runEmailProcessingJob);
    }

    @Scheduled(cron = "${app.scheduler.cron:0 */5 * * * ?}")
    public void runEmailProcessingJob() {
        if (!schedulerEnabled) {
            log.info("Email RFQ Scheduler is currently disabled via configuration.");
            return;
        }

        log.info("Cron trigger fired: Starting scheduled Email RFQ processing job...");
        try {
            ProcessingStats stats = emailProcessorService.processUnreadEmails();
            log.info("Scheduled Email RFQ Processing complete: Status={}, EmailsProcessed={}, RFQsCreated={}, Errors={}, Duration={}",
                    stats.getStatus(), stats.getEmailsProcessed(), stats.getRfqsCreated(), stats.getErrors(), stats.getExecutionTime());
        } catch (Exception e) {
            log.error("Scheduled Email RFQ Job failed: {}", e.getMessage(), e);
        }
    }
}
