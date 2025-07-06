package com.portal.procucev.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.portal.procucev.customexception.AppException;
import com.portal.procucev.dao.EmailUserRepo;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.EmailUser;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Permission;
import com.portal.procucev.model.ResetPassword;
import com.portal.procucev.model.Role;
import com.portal.procucev.model.User;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.MailUtility;
import com.portal.procucev.utils.ProcucevUtils;

@Service
public class ProcUserServiceImpl implements UserService {

	private static final Logger log = LoggerFactory.getLogger(ProcUserServiceImpl.class);

	@Autowired
	private UserDao userDao;
	
	@Autowired
	private OrgDao orgDao;

	@Autowired
	private EmailUserRepo emailUserRepo;

	@Autowired
	JavaMailSender javaMailSender;

	@Value("${spring.mail.username}")
	String mailFom;

	@Value("${host}")
	String host;

	@Override
	public User save(User user) {

		log.info("User Registration came with " + user.toString());
		// Check for user existence
		return userDao.save(user);

	}

	public User createUser(User user) {

		user.setActive(true);
		Role role = new Role();
		role.setId("5004");
		user.setRole(role);
		user.setResetPassword(true);
		// user.setPassword(generatePassword( 8));

		return userDao.save(user);

	}

	@Override
	public List<User> findAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(long id) {
		// TODO Auto-generated method stub

	}

	@Override
	public User getUserByEmail(User user) {

		User userObject = userDao.findByUsernameAndActive(user.getUsername(), true);
		if (userObject != null) {

			List<String> permissionDetails = new ArrayList<>();
			if (userObject.getRole() != null && !CollectionUtils.isEmpty(userObject.getRole().getPermission())) {

				userObject.getRole().getPermission().stream().forEach(permission -> {
					if (StringUtils.isNotEmpty(permission.getPermissionName())) {
						permissionDetails.add(permission.getPermissionName());
					}
				});
			}
			EmailUser res = emailUserRepo.findByEmail(user.getUsername());
			if (res != null) {
				userObject.setAuth(true);
			} else {
				userObject.setAuth(false);
			}
			userObject.setListofPermission(permissionDetails);

			//List<Permission> ownPermissionList = userObject.getOwnPermissionList();
			List<Permission> ownPermissionList = new ArrayList<>();
			if (!ownPermissionList.isEmpty() && ownPermissionList != null) {
				List<String> ownPermissionDetails = new ArrayList<>();
				ownPermissionList.stream().forEach(permission -> {
					if (StringUtils.isNotEmpty(permission.getPermissionName())) {
						ownPermissionDetails.add(permission.getPermissionName());
					}
				});
				userObject.setOwnPermissions(ownPermissionDetails);
			}

		} else {

			throw new AppException(HttpStatus.UNAUTHORIZED.value(), ApplicationConstants.USER_DETAILS_NOT_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		return userObject;
	}

	@Override
	public boolean changePassword(ResetPassword reset) {

		String pass = reset.getPassword();
		String newPass = reset.getNewpassword();
		User users = userDao.findByUsernameAndActive(reset.getUserName(), true);
		if (users != null) {
			String password = users.getPassword();
			if (pass.equals(password)) {
				users.setResetPassword(false);
				users.setPassword(newPass);
				userDao.save(users);
				return true;

			} else {
				throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
						ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
			}
		} else {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

	}

	/**
	 * Checks whether given email exist in database
	 */
	@Override
	public boolean checkUserexist(String useremail) {

		User userfound = userDao.findByUsernameAndActive(useremail, true);
		if (userfound != null) {
			return true;
		}
		return false;
	}

	@Override
	public List<User> updateUserByRoleByOrg(List<Organization> orglist, Role role) {

		try {
			// Get All Users associated to Organization and Active in status
			List<User> userList = userDao.findByOrgIn(orglist);
			for (User user : userList) {

				// Update role to each user
				user.setRole(role);

			}

			// Save all Users details into database with updated role
			userDao.saveAll(userList);
			return userList;
		} catch (Exception e) {
			log.error("Error While Updating User with roles" + e.getMessage());
		}
		return null;

	}

	@Override
	public boolean forgotPassword(User users) {
		boolean status = false;

		User user = userDao.findByUsernameAndActive(users.getUsername(), true);
		if (user == null) {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_USER_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
		user.setResetPassword(true);
		char[] pswd = ProcucevUtils.generatePassword(8);
		user.setPassword(pswd.toString());
		try {
			userDao.save(user);
			InternetAddress add = new InternetAddress(mailFom, "Procucev Notifications");
			MailUtility.emailforgotpassword("New Password", user.getUsername(), javaMailSender, add, user.getPassword(),
					host, user);
			status = true;
		} catch (Exception e) {
			log.error("Error Occured While Generating New Password " + e.getMessage());
		}
		return status;
	}

	@Override
	public boolean saveEmailuser(EmailUser user) {
		// TODO Auto-generated method stub
		boolean status = false;
		log.info("Entered to save Email Details For Authentication");
		if (user != null) {
			log.info("Saving Email Details:");
			emailUserRepo.save(user);
			status = true;
			log.info("Completed and returning response:");
		}
		return status;
	}

	@Override
	public boolean updateEmailUserPswd(EmailUser user) {
		// TODO Auto-generated method stub
		boolean status = false;
		log.info("Entered To Update Password");
		if (user != null) {
			log.info("Check whethere record exists with email::", user.getEmail());
			EmailUser res = emailUserRepo.findByEmail(user.getEmail());
			if (res != null) {

				emailUserRepo.updatePassword(user.getEmail(), user.getPassword());
				log.info("Updation Completed");
				status = true;
			}
		}
		return status;
	}

	@Override
	public boolean disableUser(User user) {
		// TODO Auto-generated method stub
		log.info("Entered To Disable User");

		Optional<User> userfound = userDao.findById(user.getId());
		if (userfound.isPresent()) {
			userDao.deactiveUser(user.getId());
			log.info("Deactivated User");
			String orgId = userDao.findOrgIdByUser(user.getId());
			List<String> emails = userDao.findByOrg(orgId);
			if(!emails.isEmpty() && emails!=null) {
				 orgDao.updateEmailByOrg(orgId,emails.get(0));
			}
			else {
				String email=null;
				orgDao.updateEmailByOrg(orgId,email);
			}
			return true;
		} else {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

	}

}
