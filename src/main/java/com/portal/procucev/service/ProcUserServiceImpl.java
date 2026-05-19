package com.portal.procucev.service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.mail.internet.InternetAddress;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import com.portal.procucev.Dto.SimplePageResponse;
import com.portal.procucev.Dto.VendorSummaryResponse;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.dao.EmailUserRepo;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.OrgTypeDao;
import com.portal.procucev.dao.OtpStoreDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.RoleDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.EmailUser;
import com.portal.procucev.model.OrgBranches;
import com.portal.procucev.model.OrgDivisionCategory;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.OtpDetails;
import com.portal.procucev.model.OtpStore;
import com.portal.procucev.model.Permission;
import com.portal.procucev.model.ResetPassword;
import com.portal.procucev.model.Role;
import com.portal.procucev.model.User;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.MailUtility;
import com.portal.procucev.utils.ProcucevUtils;
import com.portal.procucev.utils.StatusConstants;

@Service
public class ProcUserServiceImpl implements UserService {

	private static final Logger log = LoggerFactory.getLogger(ProcUserServiceImpl.class);

	@Autowired
	private UserDao userDao;
	
	@Autowired
	private RoleDao roleDao;

	@Autowired
	private OrgDao orgDao;
	
	@Autowired
	private RfqDao rfqDao;

	@Autowired
	private OrgTypeDao orgTypeDao;
	
	@Value("${mailid}")
	String mailid;
	
	@Autowired
	OtpStoreDao otpStoreDao;
	
	@Autowired
	private EmailUserRepo emailUserRepo;

	@Autowired
	JavaMailSender javaMailSender;

	@Value("${spring.mail.username}")
	String mailFom;

	@Value("${host}")
	String host;

	private Map<String, OtpDetails> otpsMap = new ConcurrentHashMap<>();

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
		String normalizedPhone = normalizePhone(user.getPhone());
		// User userObject = userDao.findByUsernameAndActive(user.getUsername(), true);
		User userObject = userDao.findByUsernameAndPhoneAndActive(user.getUsername(), normalizedPhone, true);
		if (userObject != null) {
			int rfqRaisedCount=rfqDao.findRfqCountByUser(userObject.getId());
			userObject.setRfqRaised(rfqRaisedCount);

			List<String> permissionDetails = new ArrayList<>();
			if (userObject.getRole() != null && !CollectionUtils.isEmpty(userObject.getRole().getPermission())) {

				userObject.getRole().getPermission().stream().forEach(permission -> {
					if (StringUtils.isNotEmpty(permission.getPermissionName())) {
						permissionDetails.add(permission.getPermissionName());
					}
				});
			}
			// To update last activity
			userDao.updateActivityTs(user.getUsername(), user.getPhone());
			EmailUser res = emailUserRepo.findByEmail(user.getUsername());
			if (res != null) {
				userObject.setAuth(true);
			} else {
				userObject.setAuth(false);
			}
			userObject.setListofPermission(permissionDetails);

			// List<Permission> ownPermissionList = userObject.getOwnPermissionList();
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

//	@Override
//	public boolean changePassword(ResetPassword reset) {
//
//		String pass = reset.getPassword();
//		String newPass = reset.getNewpassword();
//		 String normalizedPhone = normalizePhone(reset.getPhone());
//		//User users = userDao.findByUsernameAndActive(reset.getUserName(), true);
//		User users = userDao.findByUsernameAndPhoneAndActive(reset.getUserName(),normalizedPhone, true);
//		
//		if (users != null) {
//			String password = users.getPassword();
//			if (pass.equals(password)) {
//				users.setResetPassword(false);
//				users.setPassword(newPass);
//				userDao.save(users);
//				return true;
//
//			} else {
//				throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
//						ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
//			}
//		} else {
//			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
//					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
//		}
//
//	}

	@Override
	public boolean changePassword(ResetPassword reset) {

		String pass = reset.getPassword();
		String newPass = reset.getNewpassword();
		String normalizedPhone = normalizePhone(reset.getPhone());

		User users = userDao.findByUsernameAndPhoneAndActive(reset.getUserName(), normalizedPhone, true);

		if (users == null) {
			throw new AppException(HttpStatus.NOT_FOUND.value(),
					"User not found. Please check your username or phone number.",
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
		String password = users.getPassword();

		if (!pass.equals(password)) {
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "Current password is incorrect. Please try again.",
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

		users.setResetPassword(false);
		users.setPassword(newPass);
		userDao.save(users);

		return true;
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
		String normalizedPhone = normalizePhone(users.getPhone());
		// User user = userDao.findByUsernameAndActive(users.getUsername(), true);
		User user = userDao.findByUsernameAndPhoneAndActive(users.getUsername(), normalizedPhone, true);
		if (user == null) {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_USER_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
		user.setResetPassword(true);
		char[] pswd = ProcucevUtils.generatePassword(8);
		user.setPassword(new String(pswd));
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
			if (!emails.isEmpty() && emails != null) {
				orgDao.updateEmailByOrg(orgId, emails.get(0));
			} else {
				String email = null;
				orgDao.updateEmailByOrg(orgId, email);
			}
			return true;
		} else {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

	}

//	public boolean generateOtp(Organization organization, HttpServletRequest request) {
//		log.info("Entered to generate OTP");
//
//		try {
//			if (organization != null) {
//				String email = organization.getEmail();
//				String phone = organization.getOrganizationPhonenumber();
//
//				// Check if user exists
//				String normalizedPhone = normalizePhone(phone);
//				User user = userDao.findByUsernameAndPhoneAndActive(email, normalizedPhone, true);
//				if (user == null) {
//					log.error("User not found for email: {} and phone: {}", email, normalizedPhone);
//					return false;
//				}
//
//				// Generate OTP
//				String otp = generateOTPForEmail();
//				LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(15);
//
//				// Store OTP
//				// Store using unique key (email + phone)
//				String otpKey = email + "|" + normalizedPhone;
//				otpsMap.put(otpKey, new OtpDetails(otp, expirationTime));
//				// otpsMap.put(email, new OtpDetails(otp, expirationTime));
//
//				// Send OTP
//				InternetAddress fromAddress = new InternetAddress(mailFom, "Procucev Notifications");
//				MailUtility.sendOtpForEmail("OTP", email, javaMailSender, fromAddress, host, otp);
//
//				log.info("OTP sent successfully to {}", email);
//				return true;
//
//			} else {
//				log.error("Organization object is null");
//				throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
//						ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
//			}
//		} catch (UnsupportedEncodingException e) {
//			log.error("Encoding error while sending OTP", e);
//		} catch (Exception ex) {
//			log.error("Unexpected error while sending OTP", ex);
//		}
//
//		return false;
//	}

	@Override
	public boolean generateOtp(Organization organization, HttpServletRequest request) {
	    log.info("Entered to generate OTP");

	    try {
	        if (organization != null) {

	            // Resolve email (tempEmail takes priority)
	          
	            String email = organization.getEmail().trim().toLowerCase();
	            String phone = organization.getOrganizationPhonenumber();
	            String normalizedPhone = normalizePhone(phone);

	            // ✅ USER VALIDATION (added back)
	            User user = userDao.findByUsernameAndPhoneAndActive(email, normalizedPhone, true);
	            if (user == null) {
	                log.error("User not found for email: {} and phone: {}", email, normalizedPhone);
	                return false;
	            }

	            // Generate OTP
	            String otp = generateOTPForEmail();
	            LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(15);

	            String key = normalizedPhone + "_EMAIL_" + email;

	            // Save or update OTP in DB
	            otpStoreDao.findByOtpKey(key).ifPresentOrElse(existing -> {
	                existing.setOtp(otp);
	                existing.setExpirationTime(expirationTime);
	                otpStoreDao.save(existing);
	            }, () -> otpStoreDao.save(new OtpStore(key, otp, expirationTime)));

	            log.info("Stored OTP [{}] in DB for key [{}] expiring at [{}]", otp, key, expirationTime);

	            // Send OTP AFTER storing
	            InternetAddress add = new InternetAddress(mailFom, "Procucev Notifications");
	            MailUtility.sendOtpForEmail("OTP", email, javaMailSender, add, host, otp);

	            log.info("OTP sent successfully to {}", email);
	            return true;

	        } else {
	            log.error("Organization is null, cannot generate OTP");
	            throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
	                    ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
	        }

	    } catch (UnsupportedEncodingException e) {
	        log.error("Error generating OTP: {}", e.getMessage(), e);
	    } catch (Exception ex) {
	        log.error("Unexpected error while generating OTP", ex);
	    }

	    return false;
	}
	private static String generateOTPForEmail() {
		Random random = new Random();
		int otpLength = 6;
		StringBuilder otp = new StringBuilder();

		for (int i = 0; i < otpLength; i++) {
			otp.append(random.nextInt(10));
		}

		return otp.toString();
	}

	@Override
	public boolean validateOtp(Organization organization) {
		if (organization == null) {
			log.error("Organization object is null in validateOtp()");
			return false;
		}

		String email = organization.getEmail();
		String phone = organization.getOrganizationPhonenumber();

		if (email == null || phone == null || organization.getEmailOtp() == null) {
			log.warn("Missing email, phone, or OTP in request");
			return false;
		}

		String key = email + "|" + phone;
		OtpDetails otpDetails = otpsMap.get(key);

		// Validate OTP
		if (otpDetails != null) {
			LocalDateTime expirationTime = otpDetails.getExpirationTime();
			LocalDateTime now = LocalDateTime.now();

			// Check if OTP is still valid (not expired) and matches
			if (now.isBefore(expirationTime) && otpDetails.getOtp().equals(organization.getEmailOtp())) {
				// Remove OTP from the map after successful validation
				otpsMap.remove(key);
				log.info("OTP validated successfully for key: {}", key);
				return true;
			} else {
				log.warn("OTP expired or mismatch for key: {}", key);
			}
		} else {
			log.warn("No OTP entry found for key: {}", key);
		}

		return false;
	}

//	@Transactional
//	@Override
//	public boolean validateEmailOtp(Organization organization) throws UnsupportedEncodingException {
//		if (organization == null) {
//			log.error("Organization object is null in validateOtp()");
//			return false;
//		}
//
//		String email = organization.getEmail();
//		String phone = organization.getOrganizationPhonenumber();
//
//		if (email == null || phone == null || organization.getEmailOtp() == null) {
//			log.warn("Missing email, phone, or OTP in request");
//			return false;
//		}
//		String normalizedPhone = normalizePhone(phone);
//		String key = email + "|" + normalizedPhone;
//		OtpDetails otpDetails = otpsMap.get(key);
//
//		// Validate OTP
//		if (otpDetails != null) {
//			LocalDateTime expirationTime = otpDetails.getExpirationTime();
//			LocalDateTime now = LocalDateTime.now();
//
//			// Check if OTP is still valid (not expired) and matches
//			if (now.isBefore(expirationTime) && otpDetails.getOtp().equals(organization.getEmailOtp())) {
//				// Remove OTP from the map after successful validation
//			
//				User user = userDao.findByUsernameAndPhoneAndActive(email, normalizedPhone, true);
//				if (user != null 
//				        && !StatusConstants.EMAIL_VERIFIED.equals(user.getVerificationStatus()) 
//				        && !user.isSelfClient()) {
//					InternetAddress add = new InternetAddress(mailid, "Procucev Notifications");
//					MailUtility.mailingVerificationLinkWithUser(javaMailSender, add, host, user);
//				}
//				int updated = userDao.updateActivityTs(email, phone, StatusConstants.EMAIL_VERIFIED);
//				if (updated > 0) {
//					otpsMap.remove(key);
//					log.info("OTP validated and status updated successfully for key: {}", key);
//					return true;
//				}
//			} else {
//				log.warn("OTP expired or mismatch for key: {}", key);
//			}
//		} else {
//			log.warn("No OTP entry found for key: {}", key);
//			userDao.updateVerificationStatus(email, phone, StatusConstants.EMAIL_VERIFICATION_FAILED);
//		}
//
//		return false;
//	}

	@Transactional
	@Override
	public boolean validateEmailOtp(Organization organization) throws UnsupportedEncodingException {

	    if (organization == null) {
	        log.error("Organization object is null in validateEmailOtp()");
	        return false;
	    }
	    String email = organization.getEmail().trim().toLowerCase();
	    String phone = organization.getOrganizationPhonenumber();

	    if (email == null || phone == null || organization.getEmailOtp() == null) {
	        log.warn("Missing email, phone, or OTP in request");
	        return false;
	    }

	    String normalizedPhone = normalizePhone(phone);
	    String key = normalizedPhone + "_EMAIL_" + email;

	    log.info("Validating email OTP for key: {}", key);

	    Optional<OtpStore> recordOpt = otpStoreDao.findByOtpKey(key);

	    if (recordOpt.isEmpty()) {
	        log.warn("No OTP entry found for key: {}", key);
	        userDao.updateVerificationStatus(email, normalizedPhone, StatusConstants.EMAIL_VERIFICATION_FAILED);
	        return false;
	    }

	    OtpStore record = recordOpt.get();

	    // ✅ Expiry check + removal
	    if (LocalDateTime.now().isAfter(record.getExpirationTime())) {
	        log.warn("Email OTP expired for key: {}", key);
	        otpStoreDao.deleteByOtpKey(key); // removal logic
	        return false;
	    }

	    // ✅ OTP match check
	    if (!record.getOtp().equals(organization.getEmailOtp())) {
	        log.warn("Invalid OTP for key: {}", key);
	        return false;
	    }

	    // ✅ USER VALIDATION
	    User user = userDao.findByUsernameAndPhoneAndActive(email, normalizedPhone, true);
	    if (user == null) {
	        log.error("User not found for email: {} and phone: {}", email, normalizedPhone);
	        return false;
	    }

	    // ✅ Send verification mail if needed
	    if (!StatusConstants.EMAIL_VERIFIED.equals(user.getVerificationStatus()) && !user.isSelfClient()) {
	        InternetAddress add = new InternetAddress(mailid, "Procucev Notifications");
	        MailUtility.mailingVerificationLinkWithUser(javaMailSender, add, host, user);
	    }

	    // ✅ Update user status
	    int updated = userDao.updateActivityTs(email, normalizedPhone, StatusConstants.EMAIL_VERIFIED);

	    if (updated > 0) {
	        // ✅ REMOVE OTP AFTER SUCCESS
	        otpStoreDao.deleteByOtpKey(key);

	        log.info("OTP validated, removed, and status updated successfully for key: {}", key);
	        return true;
	    }

	    return false;
	}
//	    public String processBuyerExcel(MultipartFile file) throws Exception {
//	        Workbook workbook = WorkbookFactory.create(file.getInputStream());
//	        Sheet sheet = workbook.getSheetAt(0);
//	        OrgType clientType = orgTypeDao.findByTypeName("CLIENT");
//	        int success = 0, failed = 0;
//
//	        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//	            try {
//	                Row row = sheet.getRow(i);
//	                if (row == null) continue;
//
//	                String fullName = row.getCell(2).getStringCellValue(); // full_name
//	                String companyName = row.getCell(3).getStringCellValue();
//	                String companyId = row.getCell(4).getStringCellValue();
//	                String state = row.getCell(6).getStringCellValue();
//	                String address1 = row.getCell(7).getStringCellValue();
//	                String phone = getNumericCellAsString(row.getCell(8));
//	                String email = row.getCell(1).getStringCellValue(); // email (also username)
//	                String sector = row.getCell(9).getStringCellValue();
//
//	                // Skip if required values are missing
//	                if (email == null || phone == null) {
//	                    failed++;
//	                    continue;
//	                }
//
//	                List<Organization> existingOrgs = clientDao.findByCompanyNameAndOrgType(companyName, clientType);
//	                Organization org = existingOrgs.isEmpty() ? new Organization() : existingOrgs.get(0);
//	                org.setCompanyName(companyName);
//	                org.setCompanyId(companyId);
//	                org.setState(state);
//	                org.setAddress1(address1);
//	                org.setOrganizationPhonenumber(phone);
//	                org.setEmail(email);
//	                org.setClientSector(sector);
//	                org.setOrgType(clientType);
//	                org.setSelfClient(true);
//
//	                if (existingOrgs.isEmpty()) {
//	                    org = clientDao.save(org);
//	                }
//
//	                List<User> existingUsers = userDao.findByUsernameAndPhone(email, phone);
//	                if (existingUsers.isEmpty()) {
//	                    User user = new User();
//	                    user.setOrg(org);
//	                    user.setUsername(email);
//	                    user.setFullName(fullName);
//	                    user.setPhone(phone);
//	                    user.setResetPassword(true);
//	                    user.setActive(true);
//	                    user.setSelfClient(true);
//	                    user.setClientStatus(masterStatusDao.findByStatus("CLIENT_NEW"));
//	                    user.setRole(roleDao.findByRoleNameAndActive("ClientInitiator", true));
//	                    char[] password = ProcucevUtils.generatePassword(8);
//	                    user.setPassword(new String(password));
//	                    userDao.save(user);
//	                }
//
//	                success++;
//
//	            } catch (Exception e) {
//	                failed++;
//	                continue;
//	            }
//	        }
//
//	        return "Upload complete: Success = " + success + ", Failed = " + failed;
//	    }
//
//	    private String getNumericCellAsString(Cell cell) {
//	        if (cell == null) return null;
//	        if (cell.getCellType() == CellType.STRING) { 
//	            return BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
//	        } else {
//	            return cell.getStringCellValue();
//	        }
//	    }
//	}

	@Transactional
	public boolean updateOrganization(Organization updatedOrg) {
		if (updatedOrg == null) {
			log.error("updatedOrg object is null in updateOrganization() ");
			return false;
		} else {
			Organization existingOrg = orgDao.findById(updatedOrg.getId())
					.orElseThrow(() -> new RuntimeException("Organization not found"));

			// Overwrite only the fields you care about
			if (updatedOrg.getCompanyName() != null)
				existingOrg.setCompanyName(updatedOrg.getCompanyName());
			if (updatedOrg.getDetails() != null)
				existingOrg.setDetails(updatedOrg.getDetails());
			if (updatedOrg.getGstin() != null)
				existingOrg.setGstin(updatedOrg.getGstin());
			if (updatedOrg.getAddress1() != null)
				existingOrg.setAddress1(updatedOrg.getAddress1());
			if (updatedOrg.getState() != null)
				existingOrg.setState(updatedOrg.getState());
			if (updatedOrg.getCity() != null)
				existingOrg.setCity(updatedOrg.getCity());
			if (updatedOrg.getZipCode() != null)
				existingOrg.setZipCode(updatedOrg.getZipCode());
			if (updatedOrg.getContactPerson() != null)
				existingOrg.setContactPerson(updatedOrg.getContactPerson());
			if (updatedOrg.getEmail() != null)
				existingOrg.setEmail(updatedOrg.getEmail());
			if (updatedOrg.getOrganizationPhonenumber() != null)
				existingOrg.setOrganizationPhonenumber(updatedOrg.getOrganizationPhonenumber());

			if (updatedOrg.getSubscriptionPlan() != null) {
				existingOrg.setSubscriptionPlan(updatedOrg.getSubscriptionPlan());
			}

			// Optional: if nested collections are passed, handle them
			if (updatedOrg.getBranches() != null) {
				existingOrg.getBranches().clear();
				for (OrgBranches branch : updatedOrg.getBranches()) {
					branch.setOrganization(existingOrg); // Set back reference
					existingOrg.getBranches().add(branch);
				}
			}

			if (updatedOrg.getDivisionCategories() != null) {
				existingOrg.getDivisionCategories().clear();
				for (OrgDivisionCategory divCat : updatedOrg.getDivisionCategories()) {
					divCat.setOrganization(existingOrg); // Set back reference
					existingOrg.getDivisionCategories().add(divCat);
				}
			}
			orgDao.save(existingOrg);

			return true;
		}
	}

	@Override
	public boolean updateBuyer(Organization updatedOrg) {
		// TODO Auto-generated method stub
		if (updatedOrg == null) {
			log.error("updatedOrg object is null in updateBuyer() ");
			return false;
		} else {
			Organization existingOrg = orgDao.findById(updatedOrg.getId())
					.orElseThrow(() -> new RuntimeException("Organization not found"));

			// Overwrite only the fields you care about
			if (updatedOrg.getCompanyName() != null)
				existingOrg.setCompanyName(updatedOrg.getCompanyName());
			if (updatedOrg.getDetails() != null)
				existingOrg.setDetails(updatedOrg.getDetails());
			if (updatedOrg.getGstin() != null)
				existingOrg.setGstin(updatedOrg.getGstin());
			if (updatedOrg.getAddress1() != null)
				existingOrg.setAddress1(updatedOrg.getAddress1());
			if (updatedOrg.getState() != null)
				existingOrg.setState(updatedOrg.getState());
			if (updatedOrg.getCity() != null)
				existingOrg.setCity(updatedOrg.getCity());
			if (updatedOrg.getZipCode() != null)
				existingOrg.setZipCode(updatedOrg.getZipCode());
			if (updatedOrg.getContactPerson() != null)
				existingOrg.setContactPerson(updatedOrg.getContactPerson());
			if (updatedOrg.getEmail() != null)
				existingOrg.setEmail(updatedOrg.getEmail());
			if (updatedOrg.getOrganizationPhonenumber() != null)
				existingOrg.setOrganizationPhonenumber(updatedOrg.getOrganizationPhonenumber());

			// Update divisions
			if (updatedOrg.getDivisionCategories() != null && !updatedOrg.getDivisionCategories().isEmpty()) {
				List<OrgDivisionCategory> newDivs = new ArrayList<>();
				for (OrgDivisionCategory divCat : updatedOrg.getDivisionCategories()) {
					OrgDivisionCategory newCat = new OrgDivisionCategory();
					newCat.setDivision(divCat.getDivision()); // copy from payload
					newCat.setCategory(divCat.getCategory()); // copy from payload
					newCat.setOrganization(existingOrg); // back reference
					newCat.setUserId(updatedOrg.getUserId()); // user link
					newDivs.add(newCat);
				}
				existingOrg.getDivisionCategories().clear();
				existingOrg.getDivisionCategories().addAll(newDivs);
			}
			orgDao.save(existingOrg);

			return true;
		}
	}
	// =========================
	// Service Method
	// =========================

	@Override
	public SimplePageResponse<VendorSummaryResponse> getVendorSummary(
	        int page,
	        int size,
	        String search,
	        String sourceType
	) {

	    Logger logger = LoggerFactory.getLogger(getClass());
	    logger.info("Entered getVendorSummary API");

	    // Step 1: Vendor Org Type
	    OrgType orgTypeObject =
	            orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);

	    if (orgTypeObject == null) {
	        logger.warn("Vendor org type not found");
	        return new SimplePageResponse<>(0, Collections.emptyList());
	    }

	    // Step 2: Pagination
	    Pageable pageable = PageRequest.of(
	            page,
	            size,
	            Sort.by(Sort.Direction.DESC, "createdTS")
	    );

	    // Step 3: Fetch vendors
	    Page<Organization> vendorPage =
	            orgDao.findVendors(orgTypeObject, sourceType, search, pageable);

	    List<Organization> vendors = vendorPage.getContent();

	    if (vendors.isEmpty()) {
	        logger.warn("No vendors found");
	        return new SimplePageResponse<>(0, Collections.emptyList());
	    }

	    logger.info("Fetched {} vendors for page {}", vendors.size(), page);

	    // Step 4: Collect vendor IDs
	    List<String> vendorIds = vendors.stream()
	            .map(Organization::getId)
	            .toList();

	    // Step 5: Last login (single query)
	    List<Object[]> lastLoginData =
	            userDao.findLastLoginByOrgIds(vendorIds);

	    Map<String, Date> lastLoginMap = new HashMap<>();

	    for (Object[] row : lastLoginData) {
	        lastLoginMap.put((String) row[0], (Date) row[1]);
	    }

	    // Step 6: Build response
	    List<VendorSummaryResponse> response = new ArrayList<>();

	    for (Organization vendor : vendors) {

	        VendorSummaryResponse summary = new VendorSummaryResponse();

	        try {
	            summary.setId(vendor.getId());
	            summary.setCompanyId(vendor.getCompanyId());
	            summary.setCompanyName(vendor.getCompanyName());
	            summary.setName(vendor.getName());
	            summary.setEmail(vendor.getEmail());
	            summary.setGst(vendor.getGstin());
	            summary.setPincode(vendor.getZipCode());
	            summary.setPhoneNumber(vendor.getOrganizationPhonenumber());
	            summary.setDetails(vendor.getDetails());
	            summary.setSourceType(vendor.getSourceType());
	            summary.setCreatedTS(vendor.getCreatedTS());

	            summary.setSubscribed(
	                    vendor.getSubscriptionPlan() != null ? "Yes" : "No"
	            );

	            summary.setRfqsConsumed(
	                    vendor.getRfqUsedCount() != null ? vendor.getRfqUsedCount() : 0L
	            );

	            summary.setQuotesSubmitted(
	                    vendor.getQuoteSubmitted() != null ? vendor.getQuoteSubmitted() : 0L
	            );

	            summary.setVendorClass(vendor.getVendorClass());
	            summary.setSubscriptionExpiry(vendor.getSubscriptionExpiry());

	            // Last login
	            summary.setLastLogin(lastLoginMap.get(vendor.getId()));

	            summary.setError(null);

	        } catch (Exception ex) {

	            logger.error("Error processing vendor ID={}: {}", vendor.getId(), ex.getMessage(), ex);

	            summary.setId(vendor.getId());
	            summary.setCompanyId(vendor.getCompanyId());
	            summary.setCompanyName(vendor.getCompanyName());
	            summary.setName(vendor.getName());
	            summary.setSubscribed("Unknown");
	            summary.setError("Error fetching data: " + ex.getMessage());
	        }

	        response.add(summary);
	    }

	    logger.info("Vendor summary generated successfully for {} vendors", response.size());

	    // ✅ FINAL RETURN (IMPORTANT)
	    return new SimplePageResponse<>(
	            vendorPage.getTotalElements(),
	            response
	    );
	}

//	public List<VendorSummaryResponse> getVendorSummary() {
//		Logger logger = LoggerFactory.getLogger(getClass());
//
//		OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);
//		List<Organization> vendors = orgDao.findByOrgType(orgTypeObject, Sort.by(Sort.Direction.DESC, "createdTS"));
//
//		if (vendors == null || vendors.isEmpty()) {
//			logger.warn("No vendors found for orgType={}", ApplicationConstants.VENDOR);
//			return Collections.emptyList();
//		}
//
//		List<VendorSummaryResponse> response = new ArrayList<>();
//
//		for (Organization vendor : vendors) {
//			VendorSummaryResponse summary = new VendorSummaryResponse();
//			try {
//				logger.info("Processing vendor with ID={} and Name={}", vendor.getId(), vendor.getName());
//
//				// Set values into DTO
//				summary.setId(vendor.getId());
//				summary.setCompanyId(vendor.getCompanyId());
//				summary.setCompanyName(vendor.getCompanyName());
//				summary.setName(vendor.getName());
//				summary.setEmail(vendor.getEmail());
//				summary.setGst(vendor.getGstin());
//				summary.setPincode(vendor.getZipCode());
//				summary.setPhoneNumber(vendor.getOrganizationPhonenumber());
//				summary.setDetails(vendor.getDetails());
//				summary.setSourceType(vendor.getSourceType());
//				summary.setCreatedTS(vendor.getCreatedTS());
//				summary.setSubscribed(vendor.getSubscriptionPlan() != null ? "Yes" : "No");
//
//				// Rfqs created (safe null handling)
//				// summary.setRfqsCreated(vendor.getRfqsCreated() != null ?
//				// vendor.getRfqsCreated() : 0L);
//
//				// Rfqs consumed
//				summary.setRfqsConsumed(vendor.getRfqUsedCount() != null ? vendor.getRfqUsedCount() : 0L);
//
//				// Subscription expiry
//				if (vendor.getSubscriptionExpiry() != null) {
//					summary.setSubscriptionExpiry(vendor.getSubscriptionExpiry());
//				}
//
//				// Vendor class
//				summary.setVendorClass(vendor.getVendorClass());
//
//				// Last login from user activity
//				List<Date> activityTimestamps = userDao.findActivityTsByOrg(vendor.getId());
//				if (!CollectionUtils.isEmpty(activityTimestamps)) {
//					summary.setLastLogin(activityTimestamps.get(0));
//				}
//
//				// Quotes submitted
//				summary.setQuotesSubmitted(vendor.getQuoteSubmitted() != null ? vendor.getQuoteSubmitted() : 0L);
//
//				// No errors
//				summary.setError(null);
//
//			} catch (Exception ex) {
//				logger.error("Error processing vendor ID={}: {}", vendor.getId(), ex.getMessage(), ex);
//
//				// Populate minimal vendor details with error message
//				summary.setId(vendor.getId());
//				summary.setCompanyId(vendor.getCompanyId());
//				summary.setCompanyName(vendor.getCompanyName());
//				summary.setName(vendor.getName());
//				summary.setSubscribed("Unknown");
//				summary.setError("Error fetching data: " + ex.getMessage());
//			}
//
//			response.add(summary);
//		}
//
//		logger.info("Vendor summary generated for {} vendors", response.size());
//		return response;
//	}

	@Override
	public boolean deactivateOrgUser(User user) {
		// TODO Auto-generated method stub
		log.info("Entered To Disable User::");
		String normalizedPhone = normalizePhone(user.getPhone());
		User userfound = userDao.findByUsernameAndPhoneAndActive(user.getUsername(), normalizedPhone, true);
		if (userfound != null) {
			userDao.deactiveUser(userfound.getId());
			log.info("Deactivated User");
			String orgId = userDao.findOrgIdByUser(userfound.getId());
			List<String> emails = userDao.findByOrg(orgId);
			if (!emails.isEmpty() && emails != null) {
				orgDao.updateEmailByOrg(orgId, emails.get(0));
			} else {
				String email = null;
				orgDao.updateEmailByOrg(orgId, email);
			}
			return true;
		} else {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}

	}

	private String normalizePhone(String phone) {
		if (phone == null || phone.isBlank()) {
			return phone;
		}

		// Keep only digits
		String digits = phone.replaceAll("[^0-9]", "");

		// Remove leading zeros
		digits = digits.replaceFirst("^0+", "");

		// If it's a 10-digit number, assume Indian mobile and add +91
		if (digits.length() == 10) {
			return "+91" + digits;
		}

		// If it already starts with 91 and is 12 digits → enforce +91
		if (digits.length() == 12 && digits.startsWith("91")) {
			return "+91" + digits.substring(2);
		}

		// Otherwise, fallback with + (handles rare cases, but ensures valid format)
		return "+" + digits;
	}
	
	@Override
	public Organization getSellerByEmail(User user) {
	    log.info("Fetching seller details by email and phone");

	    if (user == null || user.getUsername() == null || user.getPhone() == null) {
	        log.warn("User, email, or phone is null");
	        return null;
	    }
	    Role role = roleDao.findByRoleNameAndActive(ApplicationConstants.Vendor, true);

	    String normalizedPhone = normalizePhone(user.getPhone());
	    // seller check needs to be added

	    User userData = userDao.findByUsernameAndPhoneAndActiveAndRole(
	            user.getUsername(),
	            normalizedPhone,
	            role
	    );

	    if (userData == null) {
	        log.warn("No active user found for email: {}", user.getEmail());
	        return null;
	    }

	    return userData.getOrg();
	}

	@Override
	public List<VendorSummaryResponse> getVendorSummarySearchResults(String searchType, String searchValue) {
		
		 // Step 1: Vendor Org Type
	    OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);
	    if (orgTypeObject == null) {
	        log.warn("Vendor org type not found");
	        return Collections.emptyList();
	    }
	    
	    // Step 3: Fetch vendors
	    List<Organization> vendors =
	            orgDao.findVendorsBySearchType("VENDOR",searchType,searchValue);

	    if (vendors.isEmpty()) {
	        log.warn("No vendors found");
	        return Collections.emptyList();
	    }

	    // Step 4: Collect vendor IDs
	    List<String> vendorIds = vendors.stream()
	            .map(Organization::getId)
	            .toList();

	    // Step 5: Last login (single query)
	    List<Object[]> lastLoginData =
	            userDao.findLastLoginByOrgIds(vendorIds);

	    Map<String, Date> lastLoginMap = new HashMap<>();

	    for (Object[] row : lastLoginData) {
	        lastLoginMap.put((String) row[0], (Date) row[1]);
	    }

	    // Step 6: Build response
	    List<VendorSummaryResponse> response = new ArrayList<>();

	    for (Organization vendor : vendors) {

	        VendorSummaryResponse summary = new VendorSummaryResponse();

	        try {
	            summary.setId(vendor.getId());
	            summary.setCompanyId(vendor.getCompanyId());
	            summary.setCompanyName(vendor.getCompanyName());
	            summary.setName(vendor.getName());
	            summary.setEmail(vendor.getEmail());
	            summary.setGst(vendor.getGstin());
	            summary.setPincode(vendor.getZipCode());
	            summary.setPhoneNumber(vendor.getOrganizationPhonenumber());
	            summary.setDetails(vendor.getDetails());
	            summary.setSourceType(vendor.getSourceType());
	            summary.setCreatedTS(vendor.getCreatedTS());

	            summary.setSubscribed(
	                    vendor.getSubscriptionPlan() != null ? "Yes" : "No"
	            );

	            summary.setRfqsConsumed(
	                    vendor.getRfqUsedCount() != null ? vendor.getRfqUsedCount() : 0L
	            );

	            summary.setQuotesSubmitted(
	                    vendor.getQuoteSubmitted() != null ? vendor.getQuoteSubmitted() : 0L
	            );

	            summary.setVendorClass(vendor.getVendorClass());
	            summary.setSubscriptionExpiry(vendor.getSubscriptionExpiry());

	            // Last login
	            summary.setLastLogin(lastLoginMap.get(vendor.getId()));

	            summary.setError(null);

	        } catch (Exception ex) {

	            log.error("Error processing vendor ID={}: {}", vendor.getId(), ex.getMessage(), ex);

	            summary.setId(vendor.getId());
	            summary.setCompanyId(vendor.getCompanyId());
	            summary.setCompanyName(vendor.getCompanyName());
	            summary.setName(vendor.getName());
	            summary.setSubscribed("Unknown");
	            summary.setError("Error fetching data: " + ex.getMessage());
	        }

	        response.add(summary);
	    }
	    
	    
		
		return response;
	}

}
