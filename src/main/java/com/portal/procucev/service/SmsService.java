package com.portal.procucev.service;

import org.springframework.http.ResponseEntity;

import com.portal.procucev.model.Organization;

public interface SmsService {

	ResponseEntity<String> sendOtpToMobile(Organization org);

	boolean validateMobileOtp(Organization org);

	boolean isMobileOtpValid(Organization org);

	void removeMobileOtp(Organization org);


}
