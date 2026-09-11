package com.portal.procucev.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.Dto.SmsRequest;
import com.portal.procucev.dao.OtpStoreDao;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.OtpStore;

import jakarta.transaction.Transactional;

@Service
public class SmsServiceImpl implements SmsService {
	private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);
	@Value("${sms.api.url}")
	private String smsApiUrl;

	@Value("${sms.api.authkey}")
	private String authKey;
	
	@Autowired
	private OtpStoreDao otpStoreDao;

	private final Map<String, String> otpCache = new ConcurrentHashMap<>();

	@Override
	public ResponseEntity<String> sendOtpToMobile(Organization org) {
		String phoneNumber = null;
		if (org.getTempPhone() != null && !org.getTempPhone().isEmpty()) {
			phoneNumber = org.getTempPhone();
		} else {
			phoneNumber = org.getOrganizationPhonenumber();
		}
		
		String url = "https://sms.sendmsg.in/datasend";
		String otp = String.valueOf(new Random().nextInt(900000) + 100000);
		String message = "OTP for registering your access to Get My quoTe (GMT): " + otp
				+ ". Valid for 5 mins. Do not share. - Team Procucev.";
		String key = org.getEmail().trim().toLowerCase()+"_MOBILE_"+phoneNumber.trim();
		//otpCache.put(key, otp);
		LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(15);
		otpStoreDao.findByOtpKey(key).ifPresentOrElse(
	            existing -> {
	                existing.setOtp(otp);
	                existing.setExpirationTime(expirationTime);
	                otpStoreDao.save(existing);
	            },
	            () -> otpStoreDao.save(new OtpStore(key, otp, expirationTime))
	    );

		   logger.info("Generated Mobile OTP [{}] stored in DB for key [{}]", otp, key);

		Map<String, Object> body = new HashMap<>();
		body.put("user", "Procucev_OTP");
		body.put("pass", "TzlzyMcFEZRF");

		String formattedNumber = phoneNumber.startsWith("91") ? phoneNumber : "91" + phoneNumber;

		Map<String, String> sms = new HashMap<>();
		sms.put("to", formattedNumber);
		sms.put("from", "PROCUC");
		sms.put("smstext", message);
		sms.put("smsgid", "TEST");

		List<Map<String, String>> smstosendList = new ArrayList<>();
		smstosendList.add(sms);

		body.put("smstosend", smstosendList);

		try {
			ObjectMapper mapper = new ObjectMapper();
			String jsonPayload = mapper.writeValueAsString(body);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);
			RestTemplate restTemplate = new RestTemplate();

			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
			
			return ResponseEntity.ok("OTP sent to " + phoneNumber + ". SMS API Response: " + response.getBody());

		} catch (HttpClientErrorException | HttpServerErrorException ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("SMS API Error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + e.getMessage());
		}
	}

	@Override
	@Transactional
	public boolean validateMobileOtp(Organization org) {
		boolean valid = isMobileOtpValid(org);
		if (valid) {
			removeMobileOtp(org);
		}
		return valid;
	}
	
	@Override
	public boolean isMobileOtpValid(Organization org) {
	    String phoneNumber = (org.getTempPhone() != null && !org.getTempPhone().isEmpty())
	            ? org.getTempPhone().trim()
	            : org.getOrganizationPhonenumber().trim();


	    String key = org.getEmail().trim().toLowerCase() + "_MOBILE_" + phoneNumber.trim();
	    logger.info("Validating Mobile OTP for key: {}", key);
//	    int attempts = 0;
//	    String storedOtp = otpCache.get(key);
//	    while (storedOtp == null && attempts < 3) { // retry 3 times
//	        attempts++;
//	        logger.info("OTP not found for key {}. Retry attempt {}/3", key, attempts);
//	        try {
//	            Thread.sleep(100); // wait 100ms before checking again
//	        } catch (InterruptedException e) {
//	            Thread.currentThread().interrupt();
//	            logger.warn("Thread interrupted while waiting for OTP for key {}", key);
//	        }   storedOtp = otpCache.get(key);
//	    }

	    Optional<OtpStore> recordOpt = otpStoreDao.findByOtpKey(key);
	    if (recordOpt.isEmpty()) {
	        logger.warn("No Mobile OTP found for key: {}", key);
	        return false;
	    }
        
	    OtpStore record = recordOpt.get();
	    if (LocalDateTime.now().isAfter(record.getExpirationTime())) {
	        logger.warn("Mobile OTP expired for key: {}", key);
	        otpStoreDao.deleteByOtpKey(key);
	        return false;
	    }
        
	    boolean valid = record.getOtp().equals(org.getMobileOtp());
	    logger.info(valid ? "Mobile OTP valid for key: {}" : "Invalid Mobile OTP for key: {}", key);


	    return valid;
	}

	@Override
	@Transactional
	public void removeMobileOtp(Organization org) {
	    String phoneNumber = (org.getTempPhone() != null && !org.getTempPhone().isEmpty())
	            ? org.getTempPhone().trim()
	            : org.getOrganizationPhonenumber().trim();

	    String key = org.getEmail().trim().toLowerCase() + "_MOBILE_" + phoneNumber.trim();
	    //otpCache.remove(key);
	    otpStoreDao.deleteByOtpKey(key);
	    logger.info("Removed mobile OTP from DB for key: {}", key);
	}

}
