package com.portal.procucev.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.portal.procucev.model.Organization;
import com.portal.procucev.service.SmsService;

@RestController
@RequestMapping("/mobile")
public class MobileValidationController {

	@Autowired
	private SmsService smsService;

	@PostMapping("/sendOtp")
	public ResponseEntity<String> sendOtpMobile(@RequestBody Organization org) {
		return smsService.sendOtpToMobile(org);
	}

	@PostMapping("/validateOtp")
	public ResponseEntity<String> validateOtp(@RequestBody Organization org) {
		boolean isValid = smsService.validateMobileOtp(org);
		if (isValid) {
			return ResponseEntity.ok("OTP is valid");
		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired OTP");
		}
	}
}
