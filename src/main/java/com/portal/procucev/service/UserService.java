package com.portal.procucev.service;
import java.util.List;

import com.portal.procucev.Dto.VendorSummaryResponse;
import com.portal.procucev.model.EmailUser;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.ResetPassword;
import com.portal.procucev.model.Role;
import com.portal.procucev.model.User;

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

	boolean validateEmailOtp(Organization organization);

	boolean validateOtp(Organization organization);

	boolean updateOrganization(Organization org);

	boolean updateBuyer(Organization org);

	List<VendorSummaryResponse> getVendorSummary();

	boolean deactivateOrgUser(User user);

}
