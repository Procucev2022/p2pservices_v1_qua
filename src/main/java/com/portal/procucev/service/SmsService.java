package com.portal.procucev.service;

import org.springframework.http.ResponseEntity;

import com.portal.procucev.model.Organization;

public interface SmsService {

	ResponseEntity<String> sendOtpToMobile(Organization org);

	boolean validateOtp(String phoneNumber, String enteredOtp);

}
