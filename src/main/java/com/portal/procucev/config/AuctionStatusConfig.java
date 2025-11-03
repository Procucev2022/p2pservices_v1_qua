package com.portal.procucev.config;
import jakarta.annotation.PostConstruct;

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

	@Scheduled(cron = "0 0 22 * * ?")
	@PostConstruct
	public void scheduleTaskWithCronExpressionsforForwardEmailToClient() {
		if (isEnabled) {
			log.info("Scheduled Service to Forward Vendor Quotation To Client Started");
			gmtService.emailForwarder();
			log.info("Scheduled Service to Send Reminder Email to vendors  Ended");
		}
	}

}



