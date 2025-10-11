package com.portal.procucev.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
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
import com.portal.procucev.service.ExcelReader;
import com.portal.procucev.service.SelfRegistrationService;
import com.portal.procucev.service.SmsService;
import com.portal.procucev.service.UserService;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.ClientRegistrationStatus;
import com.portal.procucev.utils.PhoneNumberUtils;
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
	
	@Autowired
    private ExcelReader excelReader;
	
	@PostMapping(value = "/SelfVendorRegistration")
	public ResponseEntity<?> vendorRegistration(@RequestBody Organization organization) throws IOException {
	    logger.info("Entered to save the vendor details");

	    try {
	        boolean status = regService.vendorRegistration(organization);

	        String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
	                                   : String.valueOf(ApplicationConstants.FAILURE);
	        String msg = status ? String.format(ApplicationConstants.CREATE_SELF_VENDOR, "")
	                            : String.format(ApplicationConstants.SELF_VENDOR_FAILED, "");

	        MessageResponse response = new MessageResponse(StatusCodes.NEW_VENDOR_CODE, msg, null, statusCode);
	        return new ResponseEntity<>(response, HttpStatus.OK);

	    } catch (AppException ex) {
	        logger.error("Vendor registration failed: {}", ex.getMessage(), ex);

	        MessageResponse response = new MessageResponse(
	                StatusCodes.NEW_VENDOR_CODE,
	                ex.getMessage(),   // message from your service exception
	                null,
	                ApplicationConstants.FAILURE
	        );
	        return new ResponseEntity<>(response, HttpStatus.OK);

	    } catch (Exception ex) {
	        logger.error("Unexpected error in vendor registration: {}", ex.getMessage(), ex);

	        MessageResponse response = new MessageResponse(
	                StatusCodes.NEW_VENDOR_CODE,
	                ApplicationConstants.SELF_VENDOR_FAILED,
	                null,
	                ApplicationConstants.FAILURE
	        );
	        return new ResponseEntity<>(response, HttpStatus.OK);
	    }
	}

	@PostMapping(value = "/sendOtp")
	public ResponseEntity<?> sendOtp(@RequestBody Organization organization, HttpServletRequest request) {

	    logger.info("Entered to send OTP");

	    boolean status = regService.generateOtp(organization, request);

	    if (status) {
	        MessageResponse response = new MessageResponse(
	                "200",
	                ApplicationConstants.OTP_GENERATE_SUCCESS,
	                null,
	                "Success"
	        );
	        return ResponseEntity.status(HttpStatus.OK).body(response); // ✅ 200
	    } else {
	        MessageResponse response = new MessageResponse(
	                "400",
	                ApplicationConstants.OTP_GENERATE_FAILED,
	                null,
	                "Failure"
	        );
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // ✅ 400
	    }
	}


	@PostMapping(value = "/validateOtp")
	public ResponseEntity<?> validateOtp(@RequestBody Organization organization) {

	    logger.info("Entered to validate OTP");

	    boolean status = regService.validateOtp(organization);

	    if (status) {
	        MessageResponse response = new MessageResponse(
	                "200",
	                ApplicationConstants.OTP_VALID_SUCCESS,
	                null,
	                "Success"
	        );
	        return ResponseEntity.status(HttpStatus.OK).body(response); // ✅ 200
	    } else {
	        MessageResponse response = new MessageResponse(
	                "401", // or "400" depending on meaning
	                ApplicationConstants.OTP_VALID_FAILED,
	                null,
	                "Failure"
	        );
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response); // ✅ 401 Unauthorized
	    }
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
	public ResponseEntity<MessageResponse> clientRegistration(@RequestBody Organization organization) {
	    logger.info("Entered to save the client details");

	    try {
	        ClientRegistrationStatus status = regService.selfclientRegistrationData(organization);

	        String msg = (status == ClientRegistrationStatus.NEW_CLIENT)
	                ? "New client created successfully and user added."
	                : "Existing client detected. User added successfully.";

	        MessageResponse response = new MessageResponse(
	                StatusCodes.OK_VENDOR_CODE, msg, null, String.valueOf(ApplicationConstants.SUCCESS));

	        return new ResponseEntity<>(response, HttpStatus.OK);

	    } catch (AppException ex) {
	        logger.error("Client registration failed: {}", ex.getMessage(), ex);
	        MessageResponse response = new MessageResponse(
	                StatusCodes.OK_VENDOR_CODE, ex.getMessage(), null, String.valueOf(ApplicationConstants.FAILURE));
	        return new ResponseEntity<>(response, HttpStatus.OK);

	    } catch (Exception ex) {
	        logger.error("Unexpected error in client registration: {}", ex.getMessage(), ex);
	        MessageResponse response = new MessageResponse(
	                StatusCodes.OK_VENDOR_CODE, "Something went wrong while processing the registration", null,
	                String.valueOf(ApplicationConstants.FAILURE));
	        return new ResponseEntity<>(response, HttpStatus.OK);
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
		String normalizedPhone = PhoneNumberUtils.normalize(org.getOrganizationPhonenumber());
		if (regService.userExistsByEmailAndPhone(org.getEmail(), normalizedPhone)) {
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
			return ResponseEntity.status(HttpStatus.OK).body(response);
		}

		// Validate mobile OTP
		boolean isMobileOtpValid = smsService.validateOtp(org);
		if (!isMobileOtpValid) {
			response.put("status", "failure");
			response.put("otpType", "mobile");
			response.put("message", "Invalid mobile OTP.");
			return ResponseEntity.status(HttpStatus.OK).body(response);
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

	@PostMapping("/getCityPincode")
	public ResponseEntity<?> getPincodeData(@RequestBody PincodeData pincode) throws AppException {
	    logger.info("Entered getPincodeData API to fetch city and state by pincode");
	    
	    PincodeData result = regService.getCityByPincode(pincode);
	    
	    if (result == null) {
	        throw new AppException(HttpStatus.OK.value(), 
	                               "No city/state found for given pincode", 
	                               null, null, LocalDateTime.now());
	    }
	    
	    return ResponseEntity.ok(result);
	}

	@GetMapping("/getUsersByPhoneNumber/{phone}")
	public ResponseEntity<?> getUsersByPhone(@PathVariable("phone") String phone) {
	    List<User> users = regService.getUsersByPhoneNumber(phone);

	    if (users == null || users.isEmpty()) {
	        // uses: MessageResponse(String statusCode, String message, List<String> errorMsg, Date timestamp, String status, String type)
	        MessageResponse response = new MessageResponse(
	                "204",
	                "No user found with phone number: " + phone,
	                null,          // errorMsg
	                new Date(),    // timestamp
	                "Failure",     // status
	                null           // type
	        );
	        return ResponseEntity.status(HttpStatus.OK).body(response);
	    }

	    // uses: MessageResponse(String statusCode, String message, Map<String,Object> data, String status, Date timestamp)
	    MessageResponse response = new MessageResponse(
	            "200",
	            "Users fetched successfully",
	            Map.of("users", users), // wrap list into a Map to match constructor
	            "Success",
	            new Date()
	    );

	    return ResponseEntity.ok(response);
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
	public ResponseEntity<MessageResponse> buyerRegistration(@RequestBody Organization organization) {
	    logger.info("Entered to save the client details");

	    try {
	        Map<String, Object> registrationResponse = regService.selfclientRegistrationDataByApp(organization);

	        boolean confirmationFlag = Boolean.TRUE.equals(registrationResponse.get("confirmationFlag"));

	        if (confirmationFlag) {
	            // Success: Buyer created
	            MessageResponse response = new MessageResponse(
	                    String.valueOf(HttpStatus.OK.value()), // 201 Created
	                    String.format(ApplicationConstants.CREATE_SELF_CLIENT, ""),
	                    registrationResponse,
	                    ApplicationConstants.SUCCESS,
	                    new Date()
	            );
	            return ResponseEntity.status(HttpStatus.OK).body(response);
	        } else {
	            // Failure due to business logic
	            String errorMsg = registrationResponse.get("error") != null
	                    ? registrationResponse.get("error").toString()
	                    : String.format(ApplicationConstants.SELF_CLIENT_FAILED, "");

	            MessageResponse response = new MessageResponse(
	                    String.valueOf(HttpStatus.CONFLICT.value()), // 409 Conflict
	                    errorMsg,
	                    registrationResponse,
	                    ApplicationConstants.FAILURE,
	                    new Date()
	            );
	            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	        }

	    } catch (AppException e) {
	        logger.error("Business validation error: {}", e.getMessage());
	        MessageResponse response = new MessageResponse(
	                String.valueOf(HttpStatus.CONFLICT.value()), // 409 Conflict
	                e.getMessage(),
	                null,
	                ApplicationConstants.FAILURE,
	                new Date()
	        );
	        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

	    } catch (Exception e) {
	        logger.error("Unexpected error: ", e);
	        MessageResponse response = new MessageResponse(
	                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), // 500
	                "Something went wrong",
	                null,
	                ApplicationConstants.FAILURE,
	                new Date()
	        );
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
	public ResponseEntity<MessageResponse> sellerRegistration(@RequestBody Organization organization) {
	    logger.info("Entered to save the Seller details");

	    try {
	        Map<String, Object> registrationResponse = regService.sellerRegistration(organization);

	        boolean confirmationFlag = Boolean.TRUE.equals(registrationResponse.get("confirmationFlag"));

	        if (confirmationFlag) {
	            // Success: Seller created
	            MessageResponse response = new MessageResponse(
	                    String.valueOf(HttpStatus.OK.value()), // 201 Created
	                    String.format(ApplicationConstants.CREATE_SELLER_SUCCESS, ""),
	                    registrationResponse,
	                    ApplicationConstants.SUCCESS,
	                    new Date()
	            );
	            return ResponseEntity.status(HttpStatus.OK).body(response);
	        } else {
	            // Failure due to business logic
	            String errorMsg = registrationResponse.get("error") != null
	                    ? registrationResponse.get("error").toString()
	                    : String.format(ApplicationConstants.CREATE_SELLER_FAILED, "");

	            MessageResponse response = new MessageResponse(
	                    String.valueOf(HttpStatus.CONFLICT.value()), // 409 Conflict
	                    errorMsg,
	                    registrationResponse,
	                    ApplicationConstants.FAILURE,
	                    new Date()
	            );
	            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	        }

	    } catch (AppException e) {
	        logger.error("Business validation error: {}", e.getMessage());
	        MessageResponse response = new MessageResponse(
	                String.valueOf(HttpStatus.CONFLICT.value()), // 409 Conflict
	                e.getMessage(),
	                null,
	                ApplicationConstants.FAILURE,
	                new Date()
	        );
	        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

	    } catch (Exception e) {
	        logger.error("Unexpected error: ", e);
	        MessageResponse response = new MessageResponse(
	                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), // 500
	                "Something went wrong",
	                null,
	                ApplicationConstants.FAILURE,
	                new Date()
	        );
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	    }
	}

	
//	@PostMapping("/uploadexcel")
//    public String uploadExcel() {
//        //String excelPath = "C:\\Users\\nagen\\Downloads\\Divisions.xlsx";
//		 ClassPathResource resource = new ClassPathResource("Divisions.xlsx");
//		    File file = resource.getFile(); // get the File object
//		    excelReader.uploadExcelToDB(file); // pass the File
//        //excelReader.uploadExcelToDB(excelPath);
//        return "Excel data inserted into DB successfully.";
//    }
	 @PostMapping("/uploadexcel")
	    public String uploadExcel() {
	        try {
	            ClassPathResource resource = new ClassPathResource("Divisionlist.xlsx");
	            try (InputStream inputStream = resource.getInputStream()) {
	                excelReader.uploadExcelToDB(inputStream);
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            return "Error inserting Excel data into DB.";
	        }

	        return "Excel data inserted into DB successfully.";
	    }
	  @PostMapping("/uploadBuyerDetails")
	    public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile file) {
	        try {
	        	regService.registerFromExcel(file);
	            return ResponseEntity.ok("Excel uploaded and registration completed successfully!");
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                                 .body("Error processing Excel: " + e.getMessage());
	        }
	    }
	  

	    @PostMapping("/uploadCategories")
	    public ResponseEntity<String> uploadCategoriesExcel(@RequestParam("file") MultipartFile file) {
	        try {
	        	regService.importCategoriesFromExcel(file);
	            return ResponseEntity.ok("Data imported successfully!");
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body("Error: " + e.getMessage());
	        }
	    }

}
