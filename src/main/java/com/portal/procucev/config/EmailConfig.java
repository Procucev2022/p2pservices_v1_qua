package com.portal.procucev.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.portal.procucev.utils.ContextClassLoaderRunnable.preservingContextClassLoader;

import com.portal.procucev.service.GMTService;

import java.util.concurrent.CompletableFuture;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;

@Component
public class EmailConfig {

	private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

	@Autowired
	private GMTService gmtService;

	/**
	 * Self reference so the startup trigger below goes through the Spring proxy and
	 * therefore through {@link SchedulerLock}. Invoking the method on {@code this}
	 * bypasses the proxy, which would let every instance run the forwarder unlocked
	 * on deploy.
	 */
	@Autowired
	@Lazy
	private EmailConfig self;

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
		// Same reason as the email RFQ startup trigger: runAsync uses ForkJoinPool.commonPool(),
		// whose workers do not inherit the web application's class loader in a WAR deployment. This
		// job parses mail attachments through Jakarta Mail and Apache POI, both of which discover
		// their handlers via the thread context class loader.
		CompletableFuture.runAsync(preservingContextClassLoader(self::scheduleTaskWithCronExpressionsforForwardEmailToClient));
	}

	/**
	 * Forwards vendor quotations from the GMT mailbox to the buyer who raised the RFQ.
	 *
	 * <p>Locked because the job opens the shared GMT INBOX read-write, flags the messages it
	 * consumes, and then increments {@code Rfq.quoteCount} and
	 * {@code Organization.quoteSubmitted}. Without a lock, two instances running the startup
	 * trigger or the same 22:00 tick can forward one quotation to the buyer twice and count it
	 * twice.
	 */
	@Scheduled(cron = "0 0 22 * * ?")
	@SchedulerLock(name = "gmtQuotationForwardJob", lockAtMostFor = "20m", lockAtLeastFor = "30s")
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
