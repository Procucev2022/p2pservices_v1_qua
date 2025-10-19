package com.portal.procucev.service;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import com.portal.procucev.customexception.AppException;
import com.portal.procucev.dao.CategoryDivisionDao;
import com.portal.procucev.dao.ClientDao;
import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.OrgTypeDao;
import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.dao.RoleDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.OtpDetails;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.CategoryDivision;
import com.portal.procucev.model.Role;
import com.portal.procucev.model.User;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.ClientRegistrationStatus;
import com.portal.procucev.utils.EmailValidatorUtil;
import com.portal.procucev.utils.MailUtility;
import com.portal.procucev.utils.PhoneNumberUtils;
import com.portal.procucev.utils.ProcucevUtils;
import com.portal.procucev.utils.StatusConstants;

import jakarta.mail.internet.InternetAddress;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class SelfRegistrationServiceImpl implements SelfRegistrationService {
	private static final Logger logger = LoggerFactory.getLogger(SelfRegistrationServiceImpl.class);
	
	@Value("${toAddress}")
	String toAddress;

	@Autowired
	JavaMailSender javaMailSender;

	@Autowired
	private OrgTypeDao orgTypeDao;

	@Autowired
	private MasterStatusDao masterStatusDao;

	@Value("${spring.mail.username}")
	String mailFom;

	@Value("${mailid}")
	String mailid; 
	
	@Value("${host}")
	String host;

	@Autowired
	private OrgDao orgDao;
	
	@Autowired
	private CategoryDivisionDao categoryDivisionDao;
	
	@Autowired
	private UserDao userDao;
	
	@Autowired
	private RoleDao roleDao;
	
	@Autowired
	private ClientDao clientDao;
	
	@Autowired
	private PincodeDao pinCodeDao;
	
	private final Map<String, OtpDetails> otpMap = new ConcurrentHashMap<>();
	
	@Override
	public boolean selfclientRegistration(Organization organization) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ClientRegistrationStatus selfclientRegistrationData(Organization organization) throws AppException {
	    logger.info("Entered self client registration");

	    // Validate required fields
	    if (organization.getCompanyName() == null || organization.getCompanyName().isEmpty()) {
	        throw new AppException( HttpStatus.BAD_REQUEST.value(), "Company name is required for registration", null, null,LocalDateTime.now());
	    }
	    if (organization.getEmail() == null || organization.getEmail().isEmpty()) {
	        throw new AppException( HttpStatus.BAD_REQUEST.value(), "Email is required for registration", null, null,LocalDateTime.now());
	    }
	    if (organization.getOrganizationPhonenumber() == null || organization.getOrganizationPhonenumber().isEmpty()) {
	        throw new AppException( HttpStatus.BAD_REQUEST.value(), "Phone number is required for registration", null, null,LocalDateTime.now());
	    }

	    try {
	       
	        OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.CLIENT);
	        if (orgTypeObject == null) {
	            throw new AppException( HttpStatus.INTERNAL_SERVER_ERROR.value(), "Client organization type not configured. Contact admin.", null, null,LocalDateTime.now());
	        }

	        // Check if organization already exists
	        List<Organization> orgList = orgDao.findByCompanyNameAndOrgType(organization.getCompanyName(), orgTypeObject);
	        String normalizedPhone = PhoneNumberUtils.normalize(organization.getOrganizationPhonenumber());
	        User existingUsers = userDao.findByUsernameAndPhoneAndActive(
		            organization.getEmail(),
		           normalizedPhone,true
		    );

		    if (existingUsers!=null) {
		        throw new AppException(HttpStatus.CONFLICT.value(), "User with the same email and phone number already exists", null, null,LocalDateTime.now());
		    }
	        boolean isNewClient = orgList.isEmpty();
	        Organization targetOrg;

	        if (isNewClient) {
	            // New client registration
	            organization.setOrgType(orgTypeObject);
	            organization.setSelfClient(true);
	            organization.setGmtName("GMT Basic");
	            organization.setBfsName(StatusConstants.BFS_PRO);
	            organization.setSourceType(ApplicationConstants.TOOL);
	            organization.setCompanyId(generateId(organization.getCompanyName()));

	            PincodeData pincodeData = getCityByPincode(organization.getZipCode());
	            if (pincodeData != null) {
	                organization.setCity(pincodeData.getCity());
	                organization.setState(pincodeData.getState());
	            }
	           
		        organization.setOrganizationPhonenumber(normalizedPhone);
	            targetOrg = clientDao.save(organization);
	            logger.info("New client saved with ID: {}", targetOrg.getId());
	        } else {
	            // Existing client, use first organization
	            targetOrg = orgList.get(0);
	            logger.info("Existing client found with ID: {}", targetOrg.getId());
	        }

	        // Create user for the organization
	        User user = new User();
	        user.setOrg(targetOrg);
	        setUserDetails(organization, user); // throws AppException if duplicate or failure

	        // Send email notification
	        InternetAddress add = new InternetAddress(mailid, "Procucev Notifications");
	        MailUtility.sendClientEmailForCM2(
	                "New Client Registration", toAddress, organization, javaMailSender, add, host
	        );

	        return isNewClient ? ClientRegistrationStatus.NEW_CLIENT : ClientRegistrationStatus.EXISTING_CLIENT; // true if new client, false if existing client
	    } catch (AppException ae) {
	        logger.error("Business validation error: {}", ae.getMessage());
	        throw ae; // propagate meaningful messages
	    } catch (Exception e) {
	        logger.error("Unexpected error during self client registration", e);
	        throw new AppException( HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong while processing the registration", null, null,LocalDateTime.now());
	    }
	}

	
//	private void enrichWithLocation(Organization org) {
//        String pincode = org.getZipCode();
//        if (pincode == null || pincode.isBlank()) return;
//
//        try {
//            RestTemplate restTemplate = new RestTemplate();
//            String url = "https://api.postalpincode.in/pincode/" + pincode;
//            ResponseEntity<ApiResponse[]> response = restTemplate.getForEntity(url, ApiResponse[].class);
//            ApiResponse[] body = response.getBody();
//
//            if (body != null && body.length > 0 && body[0].getPostOffice() != null && !body[0].getPostOffice().isEmpty()) {
//                PostOffice po = body[0].getPostOffice().get(0);
//                org.setCity(po.getName());
//                org.setState(po.getState());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            // Log failure to fetch city/state
//        }
//    }
	
	@Override
	public String generateId(String company) throws Exception {

		try {
			String companyLetters = "";
			if (StringUtils.isNotEmpty(company)) {
				if (company.length() >= 3) {
					companyLetters = company.substring(0, 3);
				} else {
					companyLetters = company;
				}

			}
			String companyId = companyLetters.toUpperCase().trim()
					+ new SimpleDateFormat("yyMMddHHmmss").format(new Date());
			return companyId;

		} catch (NullPointerException nullpointerException) {
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
	}
	
	public void setUserDetails(Organization organization, User user) throws AppException {
	    logger.info("Setting user details for email: {} and phone: {}", organization.getEmail(), organization.getOrganizationPhonenumber());

	    // Check for duplicate user
//	    User existingUsers = userDao.findByUsernameAndPhoneAndActive(
//	            organization.getEmail(),
//	            organization.getOrganizationPhonenumber(),true
//	    );
//
//	    if (existingUsers!=null) {
//	        throw new AppException(HttpStatus.CONFLICT.value(), "User with the same email and phone number already exists", null, null,LocalDateTime.now());
//	    }

	    // Fetch client status
	    MasterStatus status = masterStatusDao.findByStatus(StatusConstants.CLIENT_NEW);
	    if (status == null) {
	        throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Default client status not configured. Contact admin.", null, null,LocalDateTime.now());
	    }

	    // Fetch role
	    Role initiatorRole = roleDao.findByRoleNameAndActive(StatusConstants.ClientInitiator, true);
	    if (initiatorRole == null) {
	        throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Client initiator role not configured. Contact admin.", null, null,LocalDateTime.now());
	    }

	    // Populate user
	    user.setUsername(organization.getEmail());
	    user.setFullName(organization.getName());
	    user.setPhone(PhoneNumberUtils.normalize(organization.getOrganizationPhonenumber()));
	    user.setResetPassword(true);
	    user.setActive(true);
	    user.setSelfClient(true);
	    user.setClientStatus(status);
	    user.setRole(initiatorRole);
	    user.setUniqueId(generateUserId(organization.getOrganizationPhonenumber()));
	    user.setSourceType(organization.getSourceType());
	    user.setPassword(String.valueOf(ProcucevUtils.generatePassword(8)));
	    user.setVerificationStatus(StatusConstants.PENDING_EMAIL_VERIFICATION);

	    // Save user
	    try {
	        userDao.save(user);
	        logger.info("User saved successfully with ID: {}", user.getId());
	    } catch (Exception e) {
	        logger.error("Failed to save user", e);
	        throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to save user. Please try again.", null, null,LocalDateTime.now());
	    }
	}


	@Override
	public boolean validateClient(Organization org) {
		logger.info("Entered to validate client");

		if (org.getCompanyName() != null) {
			if (checkOrgexist(org.getCompanyName())) {
				return true; // Organization with the same name already exists
			}
		}

		if (org.getPan() != null) {
			OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.CLIENT);
			List<Organization> orgList = orgDao.findByPanAndOrgType(org.getPan(), orgTypeObject);
			if (!orgList.isEmpty()) {
				return true; // Organization with the same PAN already exists
			}
		}

		if (org.getCrn() != null) {
			OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.CLIENT);
			List<Organization> orgList = orgDao.findByCrnAndOrgType(org.getCrn(), orgTypeObject);
			if (!orgList.isEmpty()) {
				return true; // Organization with the same PAN already exists
			}
		}

		if (org.getEmail() != null) {
			if (checkUserexist(org.getEmail())) {
				return true; // User associated with the email already exists
			}
		}

		return false; // Organization does not exist
	}

	@Override
	public Organization getClientByPan(Organization org) {
		// TODO Auto-generated method stub
		logger.info("Entered to get client by pan");
		OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.CLIENT);
		if (org != null && org.getPan() != null) {
			logger.info("Entered To Get Client Details By PAN::");
			List<Organization> orgList = orgDao.findByPanAndOrgType(org.getPan(), orgTypeObject);
			if (!CollectionUtils.isEmpty(orgList)) {
				Organization organization = orgList.get(0);
				return organization;
			}
		} else if (org != null && org.getCrn() != null) {
			logger.info("Entered To Get Client Details By CRN::");
			List<Organization> orgDataList = orgDao.findByCrnAndOrgType(org.getPan(), orgTypeObject);
			if (!CollectionUtils.isEmpty(orgDataList)) {
				Organization organization = orgDataList.get(0);
				return organization;
			}
		} else {
			logger.error("No clients available in the database");
			throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
		return org;

	}

	@Override
	public boolean generateOtp(Organization organization, HttpServletRequest request) {
	    logger.info("Entered to generate OTP");

	    String email = null;
	    String key = null;

	    try {
	        if (organization != null) {
	            String otp = generateOTPForEmail();
	            LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(15);

	            if (organization.getTempEmail() != null && !organization.getTempEmail().isEmpty()) {
	                email = organization.getTempEmail().trim().toLowerCase();
	            } else {
	                email = organization.getEmail().trim().toLowerCase();
	            }

	            key = organization.getOrganizationPhonenumber().trim()+"_EMAIL_"+email;

	            // Store OTP and its expiration time in the map BEFORE sending
	            otpMap.put(key, new OtpDetails(otp, expirationTime));
	            logger.info("Stored OTP [{}] for key [{}] expiring at [{}]", otp, key, expirationTime);

	            // Send OTP via email AFTER storing
	            InternetAddress add = new InternetAddress(mailFom, "Procucev Notifications");
	            MailUtility.sendOtpForEmail("OTP", email, javaMailSender, add, host, otp);
	            
	            return true;
	        } else {
	            logger.error("Organization is null, cannot generate OTP");
	            throw new AppException(HttpStatus.NO_CONTENT.value(),
	                    ApplicationConstants.NO_DATA_FOUND,
	                    ApplicationConstants.BUSINESS_EXCEPTION,
	                    ApplicationConstants.FAILURE);
	        }
	    } catch (UnsupportedEncodingException e) {
	        logger.error("Error generating OTP: {}", e.getMessage(), e);
	    }

	    return false;
	}




	@Override
	public boolean submitUpgradeVendor(Organization organization) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean upGradeVendorJob() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean vendorRegistration(Organization organization) throws UnsupportedEncodingException {
	    logger.info("Starting vendor registration for email: {}", 
	                 organization != null ? organization.getEmail() : "null");

	    if (organization == null) {
	        throw new AppException(
	            HttpStatus.BAD_REQUEST.value(),
	            "Organization payload cannot be null",
	            ApplicationConstants.VALIDATION_EXCEPTION,
	            ApplicationConstants.FAILURE,LocalDateTime.now()
	        );
	    }

	    // 1. Validate email & phone
	    if (organization.getEmail() == null || organization.getEmail().trim().isEmpty()) {
	        throw new AppException(
	            HttpStatus.BAD_REQUEST.value(),
	            "Email cannot be empty",
	            ApplicationConstants.VALIDATION_EXCEPTION,
	            ApplicationConstants.FAILURE,LocalDateTime.now()
	        );
	    }

	    if (organization.getOrganizationPhonenumber() == null || organization.getOrganizationPhonenumber().trim().isEmpty()) {
	        throw new AppException(
	            HttpStatus.BAD_REQUEST.value(),
	            "Phone number cannot be empty",
	            ApplicationConstants.VALIDATION_EXCEPTION,
	            ApplicationConstants.FAILURE,LocalDateTime.now()
	        );
	    }

	    // 2. Check if organization already exists
//	    if (checkOrgexist(organization.getCompanyName().trim())) {
//	        throw new AppException(
//	            HttpStatus.CONFLICT.value(),
//	            ApplicationConstants.VENDOR_ALREADY_EXISTS,
//	            ApplicationConstants.BUSSINESS_EXCEPTION,
//	            ApplicationConstants.FAILURE
//	        );
//	    }

	    // 3. Check if user already associated
	    String normalizedPhone = PhoneNumberUtils.normalize(organization.getOrganizationPhonenumber());
	    if (checkUserexistWithPhone(organization.getEmail().trim(), normalizedPhone)) {
	        throw new AppException(
	            HttpStatus.CONFLICT.value(),
	            ApplicationConstants.USER_ALREADY_ASSOCIATED_TO_ACCOUNT,
	            ApplicationConstants.BUSSINESS_EXCEPTION,
	            ApplicationConstants.FAILURE,LocalDateTime.now()
	        );
	    }

	    try {
	        // Fetch OrgType and Statuses
	        OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);
	        MasterStatus vendorStatus = masterStatusDao.findByStatus(StatusConstants.SELF_REGISTER_VC_ACCEPTED);
	        MasterStatus evaluationStatus = masterStatusDao.findByStatus(StatusConstants.EVALUATION_NOT_STARTED);

	        if (orgTypeObject == null || vendorStatus == null || evaluationStatus == null) {
	            throw new AppException(
	                HttpStatus.INTERNAL_SERVER_ERROR.value(),
	                "Master data missing: Unable to fetch required OrgType/Statuses",
	                ApplicationConstants.SYSTEM_EXCEPTION,
	                ApplicationConstants.FAILURE,LocalDateTime.now()
	            );
	        }

	        // Assign values
	        organization.setOrgType(orgTypeObject);
	        organization.setVendorStatus(vendorStatus);
	        organization.setStatus(evaluationStatus);
	        organization.setProcucevStatus(vendorStatus);
	        organization.setGmtName(StatusConstants.GMT_Basic);
	        organization.setBfsName(StatusConstants.BFS_PRO);

	        // Pincode enrichment
	        if (organization.getZipCode() != null) {
	            PincodeData pincodeData = getCityByPincode(organization.getZipCode());
	            if (pincodeData != null) {
	                organization.setCity(pincodeData.getCity());
	                organization.setState(pincodeData.getState());
	            }
	        }

	        organization.setRfqCredits(1);
	        organization.setSubCategory(organization.getDetails());
	        organization.setSourceType(ApplicationConstants.TOOL);
	        
	        organization.setOrganizationPhonenumber(normalizedPhone);
	        // Save Organization
	        Organization savedOrg = orgDao.save(organization);
	        logger.info("Vendor Organization saved with ID: {}", savedOrg.getId());

	        // Create User
	        User user = new User();
	        user.setOrg(savedOrg);
	        User savedUser = setSellerUserDetails(organization, user);

	        // Send mail
	        InternetAddress fromAddress = new InternetAddress(mailid, "Procucev Notifications");
	        MailUtility.mailingVerificationLinkWithUser(javaMailSender, fromAddress, host, savedUser);

	        return true;

	    } catch (AppException ex) {
	        logger.error("Business validation failed during vendor registration: {}", ex.getMessage(), ex);
	        throw ex;
	    } catch (Exception ex) {
	        logger.error("Unexpected error during vendor registration", ex);
	        throw new AppException(
	            HttpStatus.INTERNAL_SERVER_ERROR.value(),
	            "Unexpected error during vendor registration: " + ex.getMessage(),
	            ApplicationConstants.SYSTEM_EXCEPTION,
	            ApplicationConstants.FAILURE,LocalDateTime.now()
	        );
	    }
	}
	
	
	@Override
	public boolean checkOrgexist(String orgName) {

		boolean orgexist = false;

		Organization orgfound = orgDao.findByCompanyName(orgName);
		if (orgfound != null) {
			orgexist = true;
		}

		return orgexist;
	}
	
	public boolean checkUserexistWithPhone(String useremail, String phone) {

		User userfound = userDao.findByUsernameAndPhoneAndActive(useremail,phone,
				
				true);
		if (userfound != null) {
			return true;
		}
		return false;
	}
	public boolean checkUserexist(String useremail) {

		User userfound = userDao.findByUsernameAndActive(useremail, true);
		if (userfound != null) {
			return true;
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
	public boolean validateEmailOtp(Organization organization) {
	    String email = null;
	    String key = null;

	    // Determine which email to use
	    if (organization.getTempEmail() != null && !organization.getTempEmail().isEmpty()) {
	        email = organization.getTempEmail().trim().toLowerCase();
	        key = organization.getOrganizationPhonenumber().trim()+"_EMAIL_"+email;
	    } else {
	        email = organization.getEmail().trim().toLowerCase();
	        key = organization.getOrganizationPhonenumber().trim()+"_EMAIL_"+email;
	    }

	    logger.info("Validating OTP for key: {}", key);

	    // Retrieve OTP details
	    OtpDetails otpDetails = otpMap.get(key);
	    logger.info("Fetched OTP details from otpMap for key {}: {}", key, otpDetails);
	    if (otpDetails == null) {
	        logger.warn("OTP not found for key {}. Retrying after short delay...", key);
	        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
	        otpDetails = otpMap.get(key);
	    }
	    if (otpDetails == null) {
	        logger.warn("No OTP found for key: {}. Current otpMap keys: {}", key, otpMap.keySet());
	        return false;
	    }

	    LocalDateTime expirationTime = otpDetails.getExpirationTime();
	    LocalDateTime now = LocalDateTime.now();
	    logger.info("Now: {}, ExpirationTime: {}", now, expirationTime);
	    logger.info("User entered OTP: {}, Expected OTP: {}", organization.getEmailOtp(), otpDetails.getOtp());

	    // Validate OTP
	    if (now.isBefore(expirationTime) && otpDetails.getOtp().equals(organization.getEmailOtp())) {
	        logger.info("OTP matched and is valid. Removing OTP entry from otpMap for key: {}", key);
	        otpMap.remove(key); // ✅ fix: previously removing using 'email' instead of 'key'
	        logger.info("otpMap after removal: {}", otpMap.keySet());
	        return true;
	    } else if (!otpDetails.getOtp().equals(organization.getEmailOtp())) {
	        logger.warn("Invalid OTP entered for key: {}", key);
	    } else {
	        logger.warn("OTP expired for key: {}", key);
	    }

	    return false;
	}

	@Override
	public boolean isEmailOtpValid(Organization organization) {
	    String email = (organization.getTempEmail() != null && !organization.getTempEmail().isEmpty())
	            ? organization.getTempEmail().trim().toLowerCase()
	            : organization.getEmail().trim().toLowerCase();

	    String key = organization.getOrganizationPhonenumber().trim() + "_EMAIL_" + email;
	    logger.info("Validating email OTP for key: {}", key);

	    OtpDetails otpDetails = otpMap.get(key);
	    int attempts = 0;
	    while (otpDetails == null && attempts < 3) { // retry 3 times
	        attempts++;
	        logger.info("OTP not found for key {}. Retry attempt {}/3", key, attempts);
	        try {
	            Thread.sleep(100); // wait 100ms before checking again
	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	            logger.warn("Thread interrupted while waiting for OTP for key {}", key);
	        }
	    if (otpDetails == null) {
	        logger.warn("OTP not found for key {}. Current otpMap keys: {}", key, otpMap.keySet());
	        return false;
	    }

	    LocalDateTime now = LocalDateTime.now();
	    logger.info("Current time: {}, OTP expiration time: {}", now, otpDetails.getExpirationTime());
	    logger.info("User entered OTP: {}, Expected OTP: {}", organization.getEmailOtp(), otpDetails.getOtp());

	    boolean valid = now.isBefore(otpDetails.getExpirationTime()) &&
	                    otpDetails.getOtp().equals(organization.getEmailOtp());

	    if (valid) {
	        logger.info("Email OTP is valid for key: {}", key);
	    } else if (!otpDetails.getOtp().equals(organization.getEmailOtp())) {
	        logger.warn("Invalid email OTP entered for key: {}", key);
	    } else {
	        logger.warn("Email OTP expired for key: {}", key);
	    }

	    return valid;
	}

	@Override
	public void removeEmailOtp(Organization organization) {
	    String email = (organization.getTempEmail() != null && !organization.getTempEmail().isEmpty())
	            ? organization.getTempEmail().trim().toLowerCase()
	            : organization.getEmail().trim().toLowerCase();

	    String key = organization.getOrganizationPhonenumber().trim() + "_EMAIL_" + email;
	    otpMap.remove(key);
	    logger.info("Removed email OTP from cache for key: {}", key);
	}

	@Override
	public boolean validateUser(String username, String phoneNumber) {
	  User user = userDao.findByUsernameAndPhoneAndActive(username, phoneNumber,true);
	  return user != null;
	}

	@Override
	public boolean generateEmailOtp(String email, HttpServletRequest request,String phone) {
		logger.info("Entered to generate OTP");
		try {
			if (email != null) {
				// Generate OTP
				String otp = generateOTPForEmail();
				LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(15);
				  String key = phone.trim()+"_EMAIL_"+email.toLowerCase().trim();
				// Store OTP and its expiration time in the map
				otpMap.put(key, new OtpDetails(otp, expirationTime));

				// Send OTP via email
				InternetAddress add = new InternetAddress(mailFom, "Procucev Notifications");
				MailUtility.sendOtpForEmail("OTP", email, javaMailSender, add, host, otp);
				return true;
			} else {
				logger.error("No Vendors available in the Database");
				throw new AppException(HttpStatus.NO_CONTENT.value(), ApplicationConstants.NO_DATA_FOUND,
						ApplicationConstants.BUSINESS_EXCEPTION, ApplicationConstants.FAILURE);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public String fetchPasswordByEmailAndPhone(String email, String phone) {
	    if (email == null || phone == null) {
	        throw new IllegalArgumentException("Email and phone must not be null");
	    }

	   User userOpt = userDao.findByUsernameAndPhoneAndActive(email, phone, true);
	    if (userOpt!=null) {
	        return userOpt.getPassword(); // Assuming getPassword() returns encoded password
	    } else {
	        throw new UsernameNotFoundException("No active user found with provided email and phone");
	    }
	}

	@Override
	public boolean userExistsByEmailAndPhone(String email, String organizationPhonenumber) {
	    if (email == null || organizationPhonenumber == null) {
	        return false;
	    }

	    User user = userDao.findByUsernameAndPhoneAndActive(email, organizationPhonenumber, true);
	    return user != null;
	}


	
	@Override
	 public int importFromCsv(MultipartFile file) {
	        int inserted = 0;
	        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
	            Iterable<CSVRecord> records = CSVFormat.DEFAULT
	                    .withFirstRecordAsHeader()
	                    .parse(reader);

	            for (CSVRecord record : records) {
	            	 String city = record.get("City/Town/Village").trim();
	                String pincode = record.get("Pincode").trim();
	                // Office Name
	                String state = record.get("StateName").trim();

	                if (!pinCodeDao.existsByPincode(pincode)) {
	                    PincodeData entry = new PincodeData(pincode, city, state);
	                    pinCodeDao.save(entry);
	                    inserted++;
	                }
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return inserted;
	    }


	public PincodeData getCityByPincode(String pincode) {
		// TODO Auto-generated method stub
		if(pincode!=null) {
			PincodeData data = pinCodeDao.findByPincode(pincode);
		
			return data;}
		return null;
		
	}

	
//	 @Override
//	    public List<User> getUsersByPhoneNumber(String phoneNumber) {
//	        logger.info("Fetching users with phone number: {}", phoneNumber);
//
//	        if (phoneNumber == null || phoneNumber.isBlank()) {
//	            logger.warn("Phone number is null or blank");
//	            return Collections.emptyList();
//	        }
//
//	        List<User> users = userDao.findByPhone(phoneNumber);
//
//	        if (users == null || users.isEmpty()) {
//	            logger.info("No users found with phone number: {}", phoneNumber);
//	            return Collections.emptyList();
//	        }
//
//	        logger.info("Found {} user(s) with phone number: {}", users.size(), phoneNumber);
//	        return users;
//	    }
	@Override
	public List<User> getUsersByPhoneNumber(String phoneNumber) {
	    logger.info("Fetching users with phone number: {}", phoneNumber);

	    if (phoneNumber == null || phoneNumber.isBlank()) {
	        logger.warn("Phone number is null or blank");
	        return Collections.emptyList();
	    }

	    // Normalize input (remove spaces, dashes, etc.)
	    String normalizedPhone = phoneNumber.replaceAll("[^0-9+]", "");

	    // Always ensure we generate 3 variants:
	    // +91XXXXXXXXXX, 91XXXXXXXXXX, XXXXXXXXXX
	    String withPrefix = normalizedPhone.startsWith("+91")
	            ? normalizedPhone
	            : "+91" + normalizedPhone.replaceFirst("^91", "");

	    String with91 = withPrefix.replaceFirst("^\\+91", "91");
	    String withoutPrefix = withPrefix.replaceFirst("^\\+91", "");

	    // Collect all variants
	    List<String> variants = Arrays.asList(withPrefix, with91, withoutPrefix);

	    // Query for all formats at once
	    List<User> users = userDao.findByPhoneIn(variants);

	    if (users == null || users.isEmpty()) {
	        logger.info("No users found with phone number variants: {}", variants);
	        return Collections.emptyList();
	    }

	    logger.info("Found {} user(s) with phone number variants: {}", users.size(), variants);
	    return users;
	}

	
	 public String generateUserId(String mobileNumber) {
	        // Validate mobile number length
	        if (mobileNumber == null || mobileNumber.length() < 4) {
	            throw new IllegalArgumentException("Invalid mobile number");
	        }

	        // Get last 4 digits
	        String last4Digits = mobileNumber.substring(mobileNumber.length() - 4);

	        // Get current timestamp
	        String timestamp = String.valueOf(System.currentTimeMillis());

	        // Generate 3-digit random number
	        int randomSuffix = new Random().nextInt(900) + 100;  // Range: 100–999

	        // Concatenate to form user ID
	        String userId = "USR" + timestamp + last4Digits + randomSuffix;

	        return userId;
	    }

	@Override
	public PincodeData getCityByPincode(PincodeData pincode) {
		// TODO Auto-generated method stub
		return  pinCodeDao.findByPincode(pincode.getPincode());
	}
	
	@Override
	public Map<String, Object> selfclientRegistrationDataByApp(Organization organization) {
	    logger.info("Entered self client registration");

	    Map<String, Object> response = new HashMap<>();
	    User userData = userDao.findByUsernameAndPhoneAndActive(organization.getEmail(), organization.getOrganizationPhonenumber(), true);

        if (userData != null) {
            logger.warn("User already exists with given email and phone");
            response.put("confirmationFlag", false);
            response.put("error", "User already exists with this email and phone number.");
            return response;
        }
        List<String> invalidEmails = new ArrayList<>();
        EmailValidatorUtil.validateEmail(organization.getEmail(), invalidEmails);

        if (!invalidEmails.isEmpty()) {
            logger.warn("Invalid email found: {}", invalidEmails);
            response.put("confirmationFlag", false);
            response.put("error", "Invalid email format: " + invalidEmails);
            return response;
        }
	    try {
	        List<Organization> orgList = new ArrayList<>();
	        OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.CLIENT);

	        if (organization.getCompanyName() != null) {
	            logger.info("Validating Company Name");
	            orgList = orgDao.findByCompanyNameAndOrgType(organization.getCompanyName(), orgTypeObject);
	        }

	        User user = new User();

	        if (orgList.isEmpty()) {
	            logger.info("Saving Client For First Time");

	            organization.setOrgType(orgTypeObject);
	            organization.setSelfClient(true);
	            organization.setGmtName("GMT Basic");
	            organization.setBfsName(StatusConstants.BFS_PRO);

	            String companyId = generateId(organization.getCompanyName());
	            organization.setCompanyId(companyId);
	            logger.info("Company Id: {}", companyId);

	            PincodeData pincodeData = getCityByPincode(organization.getZipCode());
	            if (pincodeData != null) {
	                organization.setCity(pincodeData.getCity());
	                organization.setState(pincodeData.getState());
	            }
	            String normalizedPhone = PhoneNumberUtils.normalize(organization.getOrganizationPhonenumber());
		        organization.setOrganizationPhonenumber(normalizedPhone);

	            Organization savedOrg = clientDao.save(organization);
	            logger.info("Saved Org ID: {}", savedOrg.getId());

	            user.setOrg(savedOrg);
	            setUserDetails(organization, user);

	            response.put("orgId", savedOrg.getId());
	            response.put("companyId", savedOrg.getCompanyId());
	        } else {
	            logger.info("Saving User For Existing Client");
	            user.setOrg(orgList.get(0));
	            setUserDetails(organization, user);
	            response.put("orgId", orgList.get(0).getId());
	            response.put("companyId", orgList.get(0).getCompanyId());
	            response.put("companyName", orgList.get(0).getCompanyName());
	        }
	        response.put("id", user.getId());
	        response.put("email", user.getUsername());
	        response.put("uniqueId", user.getUniqueId());  
	        response.put("confirmationFlag", true);
            response.put("selfClient", user.isSelfClient()); 
            response.put("fullName", user.getFullName());
            response.put("verificationStatus", user.getVerificationStatus());

	        InternetAddress add = new InternetAddress(mailid, "Procucev Notifications");
	        MailUtility.sendClientEmailForCM2(
	                "New Client Registration", toAddress, organization, javaMailSender, add, host
	        );

	        return response;
	    } catch (Exception e) {
	        logger.error("Error during self client registration", e);
	        response.put("confirmationFlag", false);
	        response.put("error", e.getMessage());
	        return response;
	    }
	}

	@Override
	public Map<String, Object> sellerRegistration(Organization organization) {
		// TODO Auto-generated method stub
		logger.info("Entered Buyer registration");
		MasterStatus vendorstatus = masterStatusDao.findByStatus(StatusConstants.SELF_REGISTER_VC_ACCEPTED);
	    Map<String, Object> response = new HashMap<>();
	    User userData = userDao.findByUsernameAndPhoneAndActive(organization.getEmail(), organization.getOrganizationPhonenumber(), true);

        if (userData != null) {
            logger.warn("User already exists with given email and phone");
            response.put("confirmationFlag", false);
            response.put("error", "User already exists with this email and phone number.");
            return response;
        }
        List<String> invalidEmails = new ArrayList<>();
        EmailValidatorUtil.validateEmail(organization.getEmail(), invalidEmails);

        if (!invalidEmails.isEmpty()) {
            logger.warn("Invalid email found: {}", invalidEmails);
            response.put("confirmationFlag", false);
            response.put("error", "Invalid email format: " + invalidEmails);
            return response;
        }
	    try {
	        List<Organization> orgList = new ArrayList<>();
	        OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);
	        //SELF_REGISTER_VC_ACCEPTED
	       
	        User user = new User();

	            logger.info("Saving Client For First Time");

	            organization.setOrgType(orgTypeObject);
	            organization.setGmtName("GMT Basic");
	            organization.setBfsName(StatusConstants.BFS_PRO);
	            organization.setProcucevStatus(vendorstatus);
	            organization.setVendorStatus(vendorstatus);
	            String companyId = generateId(organization.getCompanyName());
	            organization.setCompanyId(companyId);
	            organization.setRfqCredits(1);
	        
	            //organization.setSourceType(ApplicationConstants.TOOL);
	            logger.info("Company Id: {}", companyId);

	            PincodeData pincodeData = getCityByPincode(organization.getZipCode());
	            if (pincodeData != null) {
	                organization.setCity(pincodeData.getCity());
	                organization.setState(pincodeData.getState());
	            }
	            String normalizedPhone = PhoneNumberUtils.normalize(organization.getOrganizationPhonenumber());
		        organization.setOrganizationPhonenumber(normalizedPhone);
	            Organization savedOrg = clientDao.save(organization);
	            logger.info("Saved Org ID: {}", savedOrg.getId());

	            user.setOrg(savedOrg);
	            User savedUser = setSellerUserDetails(organization, user);

	            response.put("orgId", savedOrg.getId());
	            response.put("companyId", savedOrg.getCompanyId());
	            response.put("id", user.getId());
	            response.put("email", user.getUsername());
	            response.put("uniqueId", user.getUniqueId());  
	            response.put("confirmationFlag", true);
	            response.put("companyName", savedOrg.getCompanyName());
	            response.put("selfClient", user.isSelfClient()); 
	            response.put("fullName", user.getFullName()); 
	            response.put("verificationStatus", user.getVerificationStatus());

	        InternetAddress add = new InternetAddress(mailid, "Procucev Notifications");
	        MailUtility.mailingVerificationLinkWithUser( javaMailSender, add, host, savedUser);


	        return response;
	    } catch (Exception e) {
	        logger.error("Error during seller registration", e);
	        response.put("confirmationFlag", false);
	        response.put("error", e.getMessage());
	        return response;
	    }
}
	    
	    public User setSellerUserDetails(Organization organization, User user) {
			logger.info("Setting User Details::");
			User existingUsers = userDao.findByUsernameAndPhoneAndActive(
		            organization.getEmail(),  
		            organization.getOrganizationPhonenumber(), true);

		    if (existingUsers!=null) {
		        logger.warn("User already exists with the given email and phone");
		        throw new AppException("409", "User with the same email and phone number already exists", null, null);
		    }
			//MasterStatus status = masterStatusDao.findByStatus(StatusConstants.CLIENT_NEW);
			Role vendor = roleDao.findByRoleNameAndActive(StatusConstants.VENDOR, true);
			String uniqueId = generateUserId(organization.getOrganizationPhonenumber());
			user.setUsername(organization.getEmail());
			user.setFullName(organization.getName());
			user.setPhone(PhoneNumberUtils.normalize(organization.getOrganizationPhonenumber()));
			user.setResetPassword(true);
			user.setActive(true);
			//user.setClientStatus(status);
			user.setRole(vendor);
			user.setSourceType(organization.getSourceType());
			user.setUniqueId(uniqueId);
			user.setVerificationStatus(StatusConstants.PENDING_EMAIL_VERIFICATION);
			char[] pswd = ProcucevUtils.generatePassword(8);
			user.setPassword(pswd.toString());
			logger.info("saving User Details");
			User savedUser = userDao.save(user);
			return savedUser;
		}

	    
		@Override
		public void registerFromExcel(MultipartFile file) throws Exception {
		    try (InputStream inputStream = file.getInputStream()) {
		        Workbook workbook = WorkbookFactory.create(inputStream);
		        Sheet sheet = workbook.getSheetAt(0);

		        for (int i = 1; i <= sheet.getLastRowNum(); i++) { // skip header
		            Row row = sheet.getRow(i);
		            if (row == null) continue;

		            String username = getCellValue(row.getCell(3));
		            String fullName = getCellValue(row.getCell(4));
		            String companyName = getCellValue(row.getCell(5));
		            String address1 = getCellValue(row.getCell(6));
		            String state = getCellValue(row.getCell(7));
		            String pincode = getCellValue(row.getCell(8));
		            String phone = getCellValue(row.getCell(9));
		            String clientstatus = getCellValue(row.getCell(12)); // assuming email in column 8

		            // Basic validation
		            if (companyName == null || companyName.isEmpty()) {
		                logger.warn("Row {} skipped: company name missing", i);
		                continue;
		            }
		           
		            if (phone == null || phone.isEmpty()) {
		                logger.warn("Row {} skipped: phone missing", i);
		                continue;
		            }

		            // Normalize phone
		            phone = PhoneNumberUtils.normalize(phone);

		            try {
		                // Check if organization exists
		                OrgType orgType = orgTypeDao.findByTypeName("CLIENT");
		                List<Organization> orgList = orgDao.findByCompanyNameAndOrgType(companyName, orgType);
		                Organization org;
		                boolean isNewClient = orgList.isEmpty();

		                if (isNewClient) {
		                    // Create new organization
		                    org = new Organization();
		                    org.setCompanyName(companyName);
		                    org.setAddress1(address1);
		                    org.setCity(address1);
		                    org.setState(state);
		                    org.setZipCode(pincode);
		                    org.setOrganizationPhonenumber(phone);
		                    org.setEmail(username);
		                    org.setSelfClient(true);
		                    org.setOrgType(orgType);
		                    org.setGmtName("GMT Basic");
		                    org.setBfsName(StatusConstants.BFS_PRO);
		                    org.setSourceType(ApplicationConstants.TOOL);
		                    org.setCompanyId(generateId(companyName));

		                    PincodeData pincodeData = getCityByPincode(pincode);
		                    if (pincodeData != null) {
		                        org.setCity(pincodeData.getCity());
		                        org.setState(pincodeData.getState());
		                    }

		                    org = clientDao.save(org);
		                    logger.info("New organization created with ID: {}", org.getId());
		                } else {
		                    org = orgList.get(0);
		                    logger.info("Existing organization found with ID: {}", org.getId());
		                }

		                // Check for duplicate user
		                User existingUser = userDao.findByUsernameAndPhoneAndActive(username, phone, true);
		                if (existingUser != null) {
		                    logger.warn("Row {} skipped: user already exists with email {} and phone {}", i, username, phone);
		                    continue;
		                }

		                // Create new user
		                User user = new User();
		                user.setOrg(org);
		                user.setUsername(username);
		                user.setFullName(fullName != null ? fullName : companyName);
		                user.setPhone(phone);
		                user.setResetPassword(true);
		                user.setActive(true);
		                user.setSelfClient(true);
		                MasterStatus status = masterStatusDao.findById(clientstatus).get();
		                user.setClientStatus(status);
		                Role role = roleDao.findByRoleNameAndActive(StatusConstants.ClientInitiator, true);
		                user.setRole(role);
		                user.setUniqueId(generateUserId(phone));
		                user.setSourceType(ApplicationConstants.TOOL);
		                String pswd=String.valueOf(ProcucevUtils.generatePassword(8));
		                user.setPassword(String.valueOf(pswd));
		                user.setVerificationStatus(StatusConstants.PENDING_EMAIL_VERIFICATION);

		                User savedUser = userDao.save(user);
		                logger.info("User created successfully for row {}: {}", i, username);

		                // Optional: send email notification
		                InternetAddress add = new InternetAddress(mailid, "Procucev Notifications");
//		                MailUtility.sendClientEmailForCM2(
//		                        "New Client Registration", toAddress, org, javaMailSender, add, host
//		                );
		                MailUtility.mailingVerificationLinkWithSelfUserLogin(javaMailSender, toAddress, add, pswd, host,
								savedUser);
		            } catch (Exception e) {
		                logger.error("Failed processing row {}: {}", i, e.getMessage(), e);
		            }
		        }
		        workbook.close();
		    }
		}

			private String getCellValue(Cell cell) {
			    if (cell == null) return "";

			    switch (cell.getCellType()) {
			        case Cell.CELL_TYPE_STRING:
			            return cell.getStringCellValue().trim();
			        case Cell.CELL_TYPE_NUMERIC:
			            if (DateUtil.isCellDateFormatted(cell)) {
			                return cell.getDateCellValue().toString();
			            } else {
			                double value = cell.getNumericCellValue();
			                long longVal = (long) value;
			                return (value == longVal) ? String.valueOf(longVal) : String.valueOf(value);
			            }
			        case Cell.CELL_TYPE_BOOLEAN:
			            return String.valueOf(cell.getBooleanCellValue());
			        case Cell.CELL_TYPE_FORMULA:
			            FormulaEvaluator evaluator = cell.getSheet().getWorkbook()
			                    .getCreationHelper().createFormulaEvaluator();
			            return getCellValue(evaluator.evaluateInCell(cell));
			        case Cell.CELL_TYPE_BLANK:
			            return "";
			        default:
			            return "";
			    }
			}
			public void importCategoriesFromExcel(MultipartFile file) throws IOException {
			    try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
			        Sheet sheet = workbook.getSheetAt(0);

			        // Row 1 (second row in Excel) contains division headers
			        Row headerRow = sheet.getRow(1); // <-- changed to 1

			        if (headerRow == null) {
			            throw new IllegalStateException("Header row (row index 1) is missing in the Excel sheet.");
			        }

			        // Iterate columns C (index 2) to M (index 12)
			        for (int col = 2; col <= 12; col++) {
			            Cell headerCell = headerRow.getCell(col);
			            if (headerCell == null) continue;

			            String division = headerCell.getStringCellValue().trim();
			            if (division.isEmpty()) continue;

			            // Now read categories under this division (start from row 3, i.e. index 2)
			            for (int row = 2; row <= sheet.getLastRowNum(); row++) {
			                Row currentRow = sheet.getRow(row);
			                if (currentRow == null) continue;

			                Cell categoryCell = currentRow.getCell(col);
			                if (categoryCell == null) continue;

			                String category = categoryCell.getStringCellValue().trim();
			                if (category.isEmpty()) continue;

			                CategoryDivision cd = new CategoryDivision();
			                cd.setDivision(division);
			                cd.setCategory(category);

			                categoryDivisionDao.save(cd);
			            }
			        }
			    }
			}

}
