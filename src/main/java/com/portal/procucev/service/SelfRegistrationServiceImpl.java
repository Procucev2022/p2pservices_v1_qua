package com.portal.procucev.service;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import jakarta.mail.internet.InternetAddress;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.portal.procucev.customexception.AppException;
import com.portal.procucev.dao.ClientDao;
import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.OrgTypeDao;
import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.dao.RoleDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.ApiResponse;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.OtpDetails;
import com.portal.procucev.model.PostOffice;
import com.portal.procucev.model.Role;
import com.portal.procucev.model.User;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.MailUtility;
import com.portal.procucev.utils.ProcucevUtils;
import com.portal.procucev.utils.StatusConstants;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
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
	private UserDao userDao;
	
	@Autowired
	private RoleDao roleDao;
	
	@Autowired
	private ClientDao clientDao;
	
	@Autowired
	private PincodeDao pinCodeDao;
	
	private Map<String, OtpDetails> otpMap = new HashMap<>();
	
	@Override
	public boolean selfclientRegistration(Organization organization) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean selfclientRegistrationData(Organization organization) throws AppException {
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

	        return isNewClient; // true if new client, false if existing client
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
	    List<User> existingUsers = userDao.findByUsernameAndPhone(
	            organization.getEmail(),
	            organization.getOrganizationPhonenumber()
	    );

	    if (!existingUsers.isEmpty()) {
	        throw new AppException(HttpStatus.CONFLICT.value(), "User with the same email and phone number already exists", null, null,LocalDateTime.now());
	    }

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
	    user.setPhone(organization.getOrganizationPhonenumber());
	    user.setResetPassword(true);
	    user.setActive(true);
	    user.setSelfClient(true);
	    user.setClientStatus(status);
	    user.setRole(initiatorRole);
	    user.setUniqueId(generateUserId(organization.getOrganizationPhonenumber()));
	    user.setSourceType(organization.getSourceType());
	    user.setPassword(String.valueOf(ProcucevUtils.generatePassword(8)));

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

	public boolean generateOtp(Organization organization, HttpServletRequest request) {
		logger.info("Entered to generate OTP");
		String email=null;
		try {
			if (organization != null) {
				// Generate OTP
				String otp = generateOTPForEmail();
				LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(15);

				if(organization.getTempEmail()!=null && !organization.getTempEmail().isEmpty()) {
					email=organization.getTempEmail();
				}
				else {
					email=organization.getEmail();
				}
				// Store OTP and its expiration time in the map
				otpMap.put(email, new OtpDetails(otp, expirationTime));

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
	    if (checkUserexistWithPhone(organization.getEmail().trim(), organization.getOrganizationPhonenumber())) {
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
	public boolean validateOtp(Organization organization) {
		String email=null;
		if(organization.getTempEmail()!=null && !organization.getTempEmail().isEmpty())
		{
			 email=organization.getTempEmail();
		}
		else {
			 email=organization.getEmail();
		}
		OtpDetails otpDetails = otpMap.get(email);

		// Validate OTP
		if (otpDetails != null) {
			LocalDateTime expirationTime = otpDetails.getExpirationTime();
			LocalDateTime now = LocalDateTime.now();

			// Check if OTP is still valid (not expired)
			if (now.isBefore(expirationTime) && otpDetails.getOtp().equals(organization.getEmailOtp())) {
				// Remove OTP from the map after successful validation
				otpMap.remove(email);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean validateUser(String username, String phoneNumber) {
	  User user = userDao.findByUsernameAndPhoneAndActive(username, phoneNumber,true);
	  return user != null;
	}

	@Override
	public boolean generateEmailOtp(String email, HttpServletRequest request) {
		logger.info("Entered to generate OTP");
		try {
			if (email != null) {
				// Generate OTP
				String otp = generateOTPForEmail();
				LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(15);

				// Store OTP and its expiration time in the map
				otpMap.put(email, new OtpDetails(otp, expirationTime));

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

	
	 @Override
	    public List<User> getUsersByPhoneNumber(String phoneNumber) {
	        logger.info("Fetching users with phone number: {}", phoneNumber);

	        if (phoneNumber == null || phoneNumber.isBlank()) {
	            logger.warn("Phone number is null or blank");
	            return Collections.emptyList();
	        }

	        List<User> users = userDao.findByPhone(phoneNumber);

	        if (users == null || users.isEmpty()) {
	            logger.info("No users found with phone number: {}", phoneNumber);
	            return Collections.emptyList();
	        }

	        logger.info("Found {} user(s) with phone number: {}", users.size(), phoneNumber);
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
		return null;
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
			user.setPhone(organization.getOrganizationPhonenumber());
			user.setResetPassword(true);
			user.setActive(true);
			//user.setClientStatus(status);
			user.setRole(vendor);
			user.setSourceType(organization.getSourceType());
			user.setUniqueId(uniqueId);
			char[] pswd = ProcucevUtils.generatePassword(8);
			user.setPassword(pswd.toString());
			logger.info("saving User Details");
			User savedUser = userDao.save(user);
			return savedUser;
		}

}
