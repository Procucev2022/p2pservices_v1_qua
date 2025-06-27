package com.portal.procucev.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.model.Organization;
import com.portal.procucev.service.SelfRegistrationService;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.StatusCodes;

@CrossOrigin
@RestController
@RequestMapping("/partialvendor")
public class PartialVendorController {

	static Logger logger = LoggerFactory.getLogger(PartialVendorController.class);

	@Autowired
	SelfRegistrationService regService;
	

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

		boolean status = regService.generateOtp(organization,request);

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
	
	@PostMapping(value = "/SelfClientRegistration")
	public ResponseEntity<?> clientRegistration(@RequestBody Organization organization) throws IOException {

		logger.info("Entered to save the client details");

		boolean status = regService.selfclientRegistrationData(organization);

		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.CREATE_SELF_CLIENT, "")
				: String.format(ApplicationConstants.SELF_CLIENT_FAILED, "");
		String code = status ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@PostMapping("/getClientByPan")
	public ResponseEntity<?> getClientByPan(@RequestBody Organization org) throws AppException {
		logger.info("Enters to fetch the Client By PanNo::");
		Organization client = regService.getClientByPan(org);
		return new ResponseEntity<>(client, HttpStatus.OK);
	}

}
