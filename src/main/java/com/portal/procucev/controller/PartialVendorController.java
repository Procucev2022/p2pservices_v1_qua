package com.portal.procucev.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.model.ApiResponse;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.PostOffice;
import com.portal.procucev.model.User;
import com.portal.procucev.service.SelfRegistrationService;
import com.portal.procucev.service.SmsService;
import com.portal.procucev.service.UserService;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.StatusCodes;

@CrossOrigin
@RestController
@RequestMapping("/partialvendor")
public class PartialVendorController {

	static Logger logger = LoggerFactory.getLogger(PartialVendorController.class);

	@Autowired
	SelfRegistrationService regService;

	@Autowired
	SmsService smsService;

	@Autowired
	UserService userServices;

	@PostMapping(value = "/SelfVendorRegistration")
	public ResponseEntity<?> vendorRegistration(@RequestBody Organization organization) throws IOException {

		logger.info("entered to save the vendor details");

		boolean status = regService.vendorRegistration(organization);

		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.CREATE_SELF_VENDOR, "")
				: String.format(ApplicationConstants.SELF_VENDOR_FAILED, "");
		MessageResponse response = new MessageResponse(StatusCodes.NEW_VENDOR_CODE, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@PostMapping(value = "/sendOtp")
	public ResponseEntity<?> sendOtp(@RequestBody Organization organization, HttpServletRequest request) {

		logger.info("Entered to send OTP");

		boolean status = regService.generateOtp(organization, request);

		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.OTP_GENERATE_SUCCESS, "")
				: String.format(ApplicationConstants.OTP_GENERATE_FAILED, "");
		MessageResponse response = new MessageResponse(StatusCodes.NEW_VENDOR_CODE, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@PostMapping(value = "/validateOtp")
	public ResponseEntity<?> validateOtp(@RequestBody Organization organization) {

		logger.info("Entered to send OTP");

		boolean status = regService.validateOtp(organization);

		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.OTP_VALID_SUCCESS, "")
				: String.format(ApplicationConstants.OTP_VALID_FAILED, "");
		MessageResponse response = new MessageResponse(StatusCodes.NEW_VENDOR_CODE, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@PostMapping("/validateClientDetails")
	public ResponseEntity<?> validateClient(@RequestBody Organization org) {
		boolean exists = regService.validateClient(org);
		Map<String, Object> responseBody = new HashMap<>();
		responseBody.put("exists", exists);
		return ResponseEntity.ok(responseBody);
	}

//	@PostMapping(value = "/SelfClientRegistration")
//	public ResponseEntity<?> clientRegistration(@RequestBody Organization organization) throws IOException {
//
//		logger.info("Entered to save the client details");
//
//		boolean status = regService.selfclientRegistrationData(organization);
//
//		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
//				: String.valueOf(ApplicationConstants.FAILURE);
//		String msg = status ? String.format(ApplicationConstants.CREATE_SELF_CLIENT, "")
//				: String.format(ApplicationConstants.SELF_CLIENT_FAILED, "");
//		String code = status ? String.valueOf(HttpStatus.OK.value())
//				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
//		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
//		return new ResponseEntity<>(response, HttpStatus.OK);
//
//	}
	@PostMapping(value = "/SelfClientRegistration")
	public ResponseEntity<?> clientRegistration(@RequestBody Organization organization) {
		logger.info("Entered to save the client details");
		try {
			boolean status = regService.selfclientRegistrationData(organization);
			String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
					: String.valueOf(ApplicationConstants.FAILURE);
			String msg = status ? String.format(ApplicationConstants.CREATE_SELF_CLIENT, "")
					: String.format(ApplicationConstants.SELF_CLIENT_FAILED, "");
			String code = status ? String.valueOf(HttpStatus.OK.value())
					: String.valueOf(HttpStatus.BAD_REQUEST.value()); // or leave this out if you handle via exception
			MessageResponse response = new MessageResponse(code, msg, null, statusCode);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (AppException e) {
			logger.error("Business validation error: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.CONFLICT) // or HttpStatus.BAD_REQUEST if it's a 400-type error
					.body(new MessageResponse("409", e.getMessage(), null, ApplicationConstants.FAILURE));
		} catch (Exception e) {
			logger.error("Unexpected error: ", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("500",
					"Something went wrong while processing the request", null, ApplicationConstants.FAILURE));
		}
	}

	@PostMapping("/getClientByPan")
	public ResponseEntity<?> getClientByPan(@RequestBody Organization org) throws AppException {
		logger.info("Enters to fetch the Client By PanNo::");
		Organization client = regService.getClientByPan(org);
		return new ResponseEntity<>(client, HttpStatus.OK);
	}

	@PostMapping("/validateClient")
	public ResponseEntity<Map<String, Object>> validateClientDetails(@RequestBody Organization org,
			HttpServletRequest request) {
		Map<String, Object> response = new HashMap<>();

		// Step 1: If user already exists with same email and phone, throw exception
		if (regService.userExistsByEmailAndPhone(org.getEmail(), org.getOrganizationPhonenumber())) {
			// throw new RuntimeException("User with provided email and phone number already
			// exists.");
			response.put("status", "error");
			response.put("message", "User with provided email and phone number already exists.");
			response.put("otpSentToEmail", false);
			response.put("otpSentToMobile", false);
			return ResponseEntity.ok(response);
		}

		// Step 2: Generate and send OTP to email only
		boolean otpGenerated = regService.generateOtp(org, request);

		// Step 3: Send OTP to mobile and capture SMS API response
		ResponseEntity<String> smsResponse = smsService.sendOtpToMobile(org);

		response.put("otpSentToEmail", otpGenerated);
		response.put("otpSentToMobile", smsResponse.getStatusCode().is2xxSuccessful());
		response.put("smsApiResponse", smsResponse.getBody());

		return ResponseEntity.ok(response);
	}

	@PostMapping("/validateAllOtps")
	public ResponseEntity<Map<String, Object>> validateAllOtps(@RequestBody Organization org) {
		Map<String, Object> response = new HashMap<>();

//	    boolean isEmailOtpValid = regService.validateOtp(org);
//	    boolean isMobileOtpValid = smsService.validateOtp(org);
//
//	    if (isEmailOtpValid && isMobileOtpValid) {
//	        response.put("status", "success");
//	        response.put("message", "Both OTPs are valid.");
//	        return ResponseEntity.ok(response);
//	    } else {
//	        response.put("status", "failure");
//	        response.put("message", "Invalid OTP(s)");
//	        if (!isEmailOtpValid) response.put("emailOtpValid", false);
//	        if (!isMobileOtpValid) response.put("mobileOtpValid", false);
//	        return ResponseEntity.ok(response); 
//	    }
		// Validate email OTP
		boolean isEmailOtpValid = regService.validateOtp(org);
		if (!isEmailOtpValid) {
			response.put("status", "failure");
			response.put("otpType", "email");
			response.put("message", "Invalid email OTP.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// Validate mobile OTP
		boolean isMobileOtpValid = smsService.validateOtp(org);
		if (!isMobileOtpValid) {
			response.put("status", "failure");
			response.put("otpType", "mobile");
			response.put("message", "Invalid mobile OTP.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// If both OTPs are valid
		response.put("status", "success");
		response.put("message", "Both OTPs are valid.");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{pincode}")
	public ResponseEntity<?> getCityAndState(@PathVariable("pincode") String pincode) {
		try {
			RestTemplate restTemplate = new RestTemplate();
			String url = "https://api.postalpincode.in/pincode/" + pincode;
			ResponseEntity<ApiResponse[]> response = restTemplate.getForEntity(url, ApiResponse[].class);
			ApiResponse[] body = response.getBody();

			if (body != null && body.length > 0 && body[0].getPostOffice() != null
					&& !body[0].getPostOffice().isEmpty()) {
				PostOffice po = body[0].getPostOffice().get(0);
				Map<String, String> result = new HashMap<>();
				result.put("city", po.getName());
				result.put("state", po.getState());
				return ResponseEntity.ok(result);
			} else {
				return ResponseEntity.status(404).body(Map.of("error", "No data found for the given pincode"));
			}
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("error", "Error fetching data", "details", e.getMessage()));
		}
	}

	@PostMapping("/upload")
	public ResponseEntity<String> uploadCsv(@RequestParam("file") MultipartFile file) {
		int inserted = regService.importFromCsv(file);
		return ResponseEntity.ok("Inserted " + inserted + " pincode records.");
	}

//	  @PostMapping("/getPincodeData")
//		public ResponseEntity<?> getPincodeData(@RequestBody PincodeData pincode) throws AppException {
//			logger.info("Enters to fetch the city and state By pincode::");
//			PincodeData client = regService.getCityByPincode(pincode);
//			return new ResponseEntity<>(client, HttpStatus.OK);
//		}

	@GetMapping("/getUsersByPhoneNumber/{phone}")
	public ResponseEntity<?> getUsersByPhone(@PathVariable("phone") String phone) {
		Map<String, Object> response = new HashMap<>();
		List<User> users = regService.getUsersByPhoneNumber(phone);

		if (users == null || users.isEmpty()) {
			response.put("status", "failure");
			response.put("message", "No user found with phone number: " + phone);
			return ResponseEntity.status(HttpStatus.OK).body(response); // 200 OK with message
		}

		return ResponseEntity.ok(users); // returns list of users
	}

//	@PostMapping(value = "/buyerRegistration")
//	public ResponseEntity<?> buyerRegistration(@RequestBody Organization organization) {
//		logger.info("Entered to save the client details");
//
//		try {
//			Map<String, Object> registrationResponse = regService.selfclientRegistrationDataByApp(organization);
//
//			boolean confirmationFlag = registrationResponse.get("confirmationFlag") != null
//					&& Boolean.TRUE.equals(registrationResponse.get("confirmationFlag"));
//
//			String statusCode = confirmationFlag ? String.valueOf(ApplicationConstants.SUCCESS)
//					: String.valueOf(ApplicationConstants.FAILURE);
//
//			String msg = confirmationFlag ? String.format(ApplicationConstants.CREATE_SELF_CLIENT, "")
//					: String.format(ApplicationConstants.SELF_CLIENT_FAILED, "");
//
//			String code = confirmationFlag ? String.valueOf(HttpStatus.OK.value())
//					: String.valueOf(HttpStatus.BAD_REQUEST.value());
//
//			MessageResponse response = new MessageResponse(code, msg, registrationResponse, statusCode, new Date());
//
//			HttpStatus httpStatus = confirmationFlag ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
//
//			return new ResponseEntity<>(response, httpStatus);
//
//		} catch (AppException e) {
//			logger.error("Business validation error: {}", e.getMessage());
//			return ResponseEntity.status(HttpStatus.CONFLICT)
//					.body(new MessageResponse("409", e.getMessage(), null, ApplicationConstants.FAILURE));
//		} catch (Exception e) {
//			logger.error("Unexpected error: ", e);
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("500",
//					"Something went wrong while processing the request", null, ApplicationConstants.FAILURE));
//		}
//	}
	
	@PostMapping(value = "/buyerRegistration")
	public ResponseEntity<?> buyerRegistration(@RequestBody Organization organization) {
	    logger.info("Entered to save the client details");

	    Map<String, Object> registrationResponse;
	    String statusCode;
	    String msg;

	    try {
	        registrationResponse = regService.selfclientRegistrationDataByApp(organization);

	        boolean confirmationFlag = Boolean.TRUE.equals(registrationResponse.get("confirmationFlag"));

	        if (confirmationFlag) {
	            statusCode = ApplicationConstants.SUCCESS;
	            msg = String.format(ApplicationConstants.CREATE_SELF_CLIENT, "");
	        } else {
	            statusCode = ApplicationConstants.FAILURE;
	            msg = registrationResponse.get("error") != null
	                    ? registrationResponse.get("error").toString()
	                    : String.format(ApplicationConstants.SELF_CLIENT_FAILED, "");
	        }

	        MessageResponse response = new MessageResponse(
	                String.valueOf(HttpStatus.OK.value()), // Always 200
	                msg,
	                registrationResponse,
	                statusCode,
	                new Date()
	        );

	        return ResponseEntity.ok(response); // Always return 200

	    } catch (AppException e) {
	        logger.error("Business validation error: {}", e.getMessage());
	        return ResponseEntity.ok(
	                new MessageResponse("409", e.getMessage(), null, ApplicationConstants.FAILURE, new Date())
	        );
	    } catch (Exception e) {
	        logger.error("Unexpected error: ", e);
	        return ResponseEntity.ok(
	                new MessageResponse("500", "Something went wrong", null, ApplicationConstants.FAILURE, new Date())
	        );
	    }
	}


	@PostMapping(value = "/forgotPassword")
	public ResponseEntity<?> forgotPassword(@RequestBody User user) {
		boolean status = userServices.forgotPassword(user);
		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.PASSWORD_RESET_MAIL_SENT_SUCCESS, "")
				: String.format(ApplicationConstants.MAIL_SENT_UNSUCCESS, "");
		AppException response = new AppException(statusCode, msg, null, null);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}


	@PostMapping(value = "/sellerRegistration")
	public ResponseEntity<?> sellerRegistration(@RequestBody Organization organization) {
	    logger.info("Entered to save the Seller details");

	    Map<String, Object> registrationResponse;
	    String statusCode;
	    String msg;

	    try {
	        registrationResponse = regService.sellerRegistration(organization);

	        boolean confirmationFlag = Boolean.TRUE.equals(registrationResponse.get("confirmationFlag"));

	        if (confirmationFlag) {
	            statusCode = ApplicationConstants.SUCCESS;
	            msg = String.format(ApplicationConstants.CREATE_SELLER_SUCCESS, "");
	        } else {
	            statusCode = ApplicationConstants.FAILURE;
	            msg = registrationResponse.get("error") != null
	                    ? registrationResponse.get("error").toString()
	                    : String.format(ApplicationConstants.CREATE_SELLER_FAILED, "");
	        }

	        MessageResponse response = new MessageResponse(
	                String.valueOf(HttpStatus.OK.value()), // Always 200
	                msg,
	                registrationResponse,
	                statusCode,
	                new Date()
	        );

	        return ResponseEntity.ok(response); // Always return 200

	    } catch (AppException e) {
	        logger.error("Business validation error: {}", e.getMessage());
	        return ResponseEntity.ok(
	                new MessageResponse("409", e.getMessage(), null, ApplicationConstants.FAILURE, new Date())
	        );
	    } catch (Exception e) {
	        logger.error("Unexpected error: ", e);
	        return ResponseEntity.ok(
	                new MessageResponse("500", "Something went wrong", null, ApplicationConstants.FAILURE, new Date())
	        );
	    }
	}

}
