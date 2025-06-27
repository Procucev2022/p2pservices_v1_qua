package com.portal.procucev.service;

import java.io.UnsupportedEncodingException;

import com.portal.procucev.model.Organization;

import jakarta.servlet.http.HttpServletRequest;

public interface SelfRegistrationService {

	boolean selfclientRegistration(Organization organization);
	
	Organization getClientByPan(Organization org);

	boolean selfclientRegistrationData(Organization organization);
	
	boolean validateClient(Organization org);
	
	boolean generateOtp(Organization organization, HttpServletRequest request);

	boolean validateOtp(Organization organization);

	boolean submitUpgradeVendor(Organization organization);

	boolean upGradeVendorJob();

	boolean vendorRegistration(Organization organization) throws UnsupportedEncodingException;

}