package com.portal.procucev.rfq.scheduler;

import com.portal.procucev.rfq.service.DemoBuyerCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled cron job that periodically checks for unverified email-registered demo buyers
 * that did not log in within 1 hour, and automatically deletes their accounts.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DemoBuyerExpiryScheduler {

    private final DemoBuyerCleanupService cleanupService;

    @Value("${app.demo.expiry.enabled:true}")
    private boolean expiryJobEnabled;

    /**
     * Runs every 5 minutes by default to clean up expired demo buyer accounts.
     */
    @Scheduled(cron = "${app.demo.expiry.cron:0 */5 * * * ?}")
    @SchedulerLock(name = "demoBuyerExpiryJob", lockAtMostFor = "4m", lockAtLeastFor = "10s")
    public void runDemoBuyerExpiryJob() {
        if (!expiryJobEnabled) {
            log.debug("Demo buyer expiry job is disabled via configuration.");
            return;
        }
        try {
            log.debug("Running periodic demo buyer expiry check...");
            int cleanedCount = cleanupService.cleanupExpiredDemoBuyers();
            if (cleanedCount > 0) {
                log.info("Demo buyer expiry job cleaned up {} expired accounts.", cleanedCount);
            }
        } catch (Exception e) {
            log.error("Error occurred while running demo buyer expiry job: {}", e.getMessage(), e);
        }
    }
}
