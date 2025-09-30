package com.portal.procucev.service;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.User;
import com.portal.procucev.utils.ClientRegistrationStatus;

import jakarta.servlet.http.HttpServletRequest;

public interface SelfRegistrationService {

	boolean selfclientRegistration(Organization organization);
	
	Organization getClientByPan(Organization org);

	ClientRegistrationStatus selfclientRegistrationData(Organization organization);
	
	boolean validateClient(Organization org);
	
	boolean generateOtp(Organization organization, HttpServletRequest request);

	boolean validateOtp(Organization organization);

	boolean submitUpgradeVendor(Organization organization);

	boolean upGradeVendorJob();

	boolean vendorRegistration(Organization organization) throws UnsupportedEncodingException;

	String generateId(String company) throws Exception;

	boolean checkOrgexist(String orgName);

	boolean validateUser(String username, String phoneNumber);

	String fetchPasswordByEmailAndPhone(String username, String phone);

	boolean generateEmailOtp(String email, HttpServletRequest request);

	boolean userExistsByEmailAndPhone(String email, String organizationPhonenumber);

	int importFromCsv(MultipartFile file);

	PincodeData getCityByPincode(PincodeData pincode);

	List<User> getUsersByPhoneNumber(String phone);

	Map<String, Object> selfclientRegistrationDataByApp(Organization organization);

	Map<String, Object> sellerRegistration(Organization organization);

	void registerFromExcel(MultipartFile file) throws Exception;

}