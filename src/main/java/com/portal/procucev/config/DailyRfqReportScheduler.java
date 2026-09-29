package com.portal.procucev.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.portal.procucev.service.DailyRfqReportService;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class DailyRfqReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyRfqReportScheduler.class);

    @Autowired
    private DailyRfqReportService dailyRfqReportService;

    @Value("${jobs.enabled:true}")
    private boolean isEnabled;

    /**
     * Automatically triggers daily at 6:00 AM IST to report on all RFQs received across all channels
     * during the preceding calendar day (00:00:00 to 23:59:59 IST).
     */
    @Scheduled(cron = "${daily.rfq.report.cron:0 0 6 * * ?}", zone = "Asia/Kolkata")
    @SchedulerLock(name = "dailyRfqIntakeReportJob", lockAtMostFor = "15m", lockAtLeastFor = "30s")
    public void runDailyRfqReportJob() {
        if (!isEnabled) {
            log.info("Daily RFQ Report Scheduler is disabled via jobs.enabled=false");
            return;
        }

        log.info("Starting automated Daily RFQ Intake Report job at 6:00 AM IST...");
        try {
            // Target date defaults to yesterday in IST, force=false, overrideTo=null
            dailyRfqReportService.dispatchDailyReport(null, false, null);
            log.info("Completed automated Daily RFQ Intake Report job successfully.");
        } catch (Throwable t) {
            log.error("Unhandled error in automated Daily RFQ Intake Report job: {}", t.getMessage(), t);
        }
    }
}
