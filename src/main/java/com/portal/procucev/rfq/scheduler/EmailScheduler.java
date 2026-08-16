package com.portal.procucev.rfq.scheduler;

import java.util.concurrent.CompletableFuture;

import com.portal.procucev.rfq.dto.ProcessingStats;
import com.portal.procucev.rfq.service.EmailProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailScheduler {

    private final EmailProcessorService emailProcessorService;

    /**
     * Self reference so the startup trigger below goes through the Spring proxy and therefore
     * through {@link SchedulerLock}. Calling the method on {@code this} would bypass the proxy
     * and let every instance run the job unlocked on deploy.
     */
    @Autowired
    @Lazy
    private EmailScheduler self;

    @Value("${app.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!schedulerEnabled) {
            return;
        }
        log.info("Application started: Triggering immediate initial Email RFQ processing run...");
        CompletableFuture.runAsync(self::runEmailProcessingJob);
    }

    /**
     * Polls the mailbox for new RFQ emails.
     *
     * <p>Locked because the job opens the shared INBOX in read-write mode and flags/moves the
     * messages it consumes. Without a lock, every application instance polls the same mailbox on
     * the same schedule and can process one email more than once. {@code lockAtLeastFor} holds the
     * lock briefly after a fast run so a second instance cannot pick up the same tick.
     */
    @Scheduled(cron = "${app.scheduler.cron:0 */1 * * * ?}")
    @SchedulerLock(name = "emailRfqProcessingJob", lockAtMostFor = "9m", lockAtLeastFor = "30s")
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
