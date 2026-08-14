package com.portal.procucev.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.portal.procucev.service.GMTService;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class EmailConfig {

	private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

	@Autowired
	private GMTService gmtService;

	@Value("${jobs.enabled:true}")
	private boolean isEnabled;

	/**
	 * Runs the forwarder once after the application is fully up.
	 *
	 * <p>This used to be a {@code @PostConstruct}, which meant a blocking IMAP
	 * connection to Gmail happened inside bean initialisation. No IMAPS connect
	 * timeout is configured, so a slow or unreachable mail host would stall
	 * context startup indefinitely, leaving Tomcat with no deployed context and
	 * every request answering 404. Running it after startup, off the main thread,
	 * removes that failure mode.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		if (!isEnabled) {
			return;
		}
		CompletableFuture.runAsync(this::scheduleTaskWithCronExpressionsforForwardEmailToClient);
	}

	@Scheduled(cron = "0 0 22 * * ?")
	public void scheduleTaskWithCronExpressionsforForwardEmailToClient() {
		if (isEnabled) {
			log.info("Scheduled Service to Forward Vendor Quotation To Client Started");
			try {
				gmtService.emailForwarder();
			} catch (Exception e) {
				log.error("Forward Vendor Quotation To Client failed", e);
			}
			log.info("Scheduled Service to Send Reminder Email to vendors  Ended");
		}
	}
	
    // Runs every day at 3:00 AM
//	    @Scheduled(cron = "0 0 3 * * *")
//	    public void sendDailyReports() {
//	    	gmtService.dailyReportEmailForwarder();
//	    	
//	    }
	    
	    

}
