package com.portal.procucev.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
import com.portal.procucev.model.Organization;

@Service
public class SmsServiceImpl implements SmsService {

	@Value("${sms.api.url}")
	private String smsApiUrl;

	@Value("${sms.api.authkey}")
	private String authKey;

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
			String key = org.getEmail() + "_" + phoneNumber;
			otpCache.put(key, otp);

			return ResponseEntity.ok("OTP sent to " + phoneNumber + ". SMS API Response: " + response.getBody());

		} catch (HttpClientErrorException | HttpServerErrorException ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("SMS API Error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + e.getMessage());
		}
	}

	@Override
	public boolean validateOtp(Organization org) {
		String phoneNumber = null;
		String key = null;
		if (org.getTempPhone() != null && !org.getTempPhone().isEmpty()) {
			phoneNumber = org.getTempPhone();
			key = org.getEmail() + "_" + phoneNumber;
		} else {
			phoneNumber = org.getOrganizationPhonenumber();
			key = org.getEmail() + "_" + phoneNumber;
		}
		String storedOtp = otpCache.get(key);

		if (storedOtp != null && storedOtp.equals(org.getMobileOtp())) {
			otpCache.remove(phoneNumber); // Remove OTP after successful validation
			return true;
		}
		return false;
	}
}
