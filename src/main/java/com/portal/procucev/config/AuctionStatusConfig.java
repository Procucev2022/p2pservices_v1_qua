package com.portal.procucev.config;
import jakarta.annotation.PostConstruct;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.portal.procucev.service.GMTService;


@Component
public class AuctionStatusConfig {

	private static final Logger log = LoggerFactory.getLogger(AuctionStatusConfig.class);

	@Autowired
	private GMTService gmtService;

	@Value("${jobs.enabled}")
	private boolean isEnabled;

	@PostConstruct
//	@Scheduled(cron = "0 30 13 * * ?", zone = "Asia/Kolkata") // 1:30 PM IST
//	@Scheduled(cron = "0 30 17 * * ?", zone = "Asia/Kolkata") // 5:30 PM IST
//	@Scheduled(cron = "0 0 23 * * ?", zone = "Asia/Kolkata")   // 11:00 PM
	@Scheduled(cron = "0 0 */2 * * ?", zone = "Asia/Kolkata")
	@SchedulerLock(name = "emailForwardJob")
	public void scheduleTaskWithCronExpressionsforForwardEmailToClient() {
		if (isEnabled) {
			log.info("Scheduled Service to Forward Vendor Quotation To Client Started  with ShedLock...");
			gmtService.emailForwarder();
			log.info("Scheduled Service to Send Reminder Email to vendors  Ended  with ShedLock...");
		}
	}

	@Scheduled(cron = "0 0 0 * * ?")  // every day at 00:00
	@SchedulerLock(name = "vendorClassUpdate")
	public void scheduledVendorClassUpdate() {
	    log.info("Starting scheduled vendor classification update  with ShedLock......");
	    gmtService.updateVendorClasses();
	    log.info("Completed scheduled vendor classification update ended with ShedLock...");
	}
	
}



