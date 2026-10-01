package com.portal.procucev.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.multipart.MultipartFile;

import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.User;
import com.portal.procucev.utils.ClientRegistrationStatus;

import jakarta.servlet.http.HttpServletRequest;

public interface SelfRegistrationService {

	@CacheEvict(value = "gmtBuyers", allEntries = true)
	boolean selfclientRegistration(Organization organization);
	
	Organization getClientByPan(Organization org);

	@CacheEvict(value = "gmtBuyers", allEntries = true)
	ClientRegistrationStatus selfclientRegistrationData(Organization organization);
	
	boolean validateClient(Organization org);
	
	boolean generateOtp(Organization organization, HttpServletRequest request);

	boolean validateEmailOtp(Organization organization);

	boolean submitUpgradeVendor(Organization organization);

	boolean upGradeVendorJob();

	@CacheEvict(value = { "allVendors", "gmtBuyers" }, allEntries = true)
	boolean vendorRegistration(Organization organization) throws UnsupportedEncodingException;

	String generateId(String company) throws Exception;

	boolean checkOrgexist(String orgName);

	boolean validateUser(String username, String phoneNumber);

	String fetchPasswordByEmailAndPhone(String username, String phone);

	boolean generateEmailOtp(String email, HttpServletRequest request, String phone);

	boolean userExistsByEmailAndPhone(String email, String organizationPhonenumber);

	int importFromCsv(MultipartFile file);

	PincodeData getCityByPincode(PincodeData pincode);

	List<User> getUsersByPhoneNumber(String phone);

	@CacheEvict(value = "gmtBuyers", allEntries = true)
	Map<String, Object> selfclientRegistrationDataByApp(Organization organization);

	@CacheEvict(value = { "allVendors", "gmtBuyers" }, allEntries = true)
	Map<String, Object> sellerRegistration(Organization organization);

	@CacheEvict(value = { "allVendors", "gmtBuyers" }, allEntries = true)
	void registerFromExcel(MultipartFile file) throws Exception;

	void importCategoriesFromExcel(MultipartFile file) throws IOException;

	void removeEmailOtp(Organization organization);

	boolean isEmailOtpValid(Organization organization);

	Map<String, Object> SellerregisterFromExcel(MultipartFile file) throws EncryptedDocumentException, InvalidFormatException, IOException;

	boolean validateUserApproval(String username, String phone);

	List<Map<String, Object>> getPincodeDetails(String pincode);

}