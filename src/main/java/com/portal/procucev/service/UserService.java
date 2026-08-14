package com.portal.procucev.service;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.springframework.http.ResponseEntity;

import com.portal.procucev.Dto.SimplePageResponse;
import com.portal.procucev.Dto.UserActivityDto;
import com.portal.procucev.Dto.VendorSummaryResponse;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.model.EmailUser;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.ResetPassword;
import com.portal.procucev.model.Role;
import com.portal.procucev.model.User;
import com.portal.procucev.model.UserActivity;

import jakarta.servlet.http.HttpServletRequest;


public interface UserService {

	User save(User user);

	List<User> findAll();

	void delete(long id);

	/**
	 * To get all logged user details by the email and active status
	 * @param user
	 * @return
	 */
	User getUserByEmail(User user);

	//boolean changePassword(User user, String pass, String newpswd, String confirmpswd);

	boolean changePassword(ResetPassword reset);
	
	boolean checkUserexist(String useremail);
	
	/**
	 * Method will update the Vendor Register User with Vendor Role on Approval of Vendor Registration
	 * @return
	 */
	List<User> updateUserByRoleByOrg(List<Organization> orglist, Role role);

	boolean forgotPassword(User users);

	boolean saveEmailuser(EmailUser user);

	boolean updateEmailUserPswd(EmailUser user);

	boolean disableUser(User user);

	boolean generateOtp(Organization organization, HttpServletRequest request);

	boolean validateEmailOtp(Organization organization) throws UnsupportedEncodingException;

	boolean validateOtp(Organization organization);

	boolean updateOrganization(Organization org);

	boolean updateBuyer(Organization org);

	boolean deactivateOrgUser(User user);

	Organization getSellerByEmail(User user);

	Organization getBuyerByEmail(User user);

	User getBuyerUserByEmail(User user);

	SimplePageResponse<VendorSummaryResponse> getVendorSummary(int page, int size, String search, String sourceType);

	List<VendorSummaryResponse> getVendorSummarySearchResults(String searchType, String searchValue);
	
	UserActivity saveUserActivity(UserActivityDto userActivity,String userName,String mobile);

	ResponseEntity<MessageResponse> getBuyerByEmail(String username);

}
