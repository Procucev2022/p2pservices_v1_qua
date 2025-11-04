
package com.portal.procucev.controller;

import java.util.List;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.util.*;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.portal.procucev.Dto.VendorSummaryResponse;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.dao.EmailUserRepo;
import com.portal.procucev.model.EmailUser;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.ResetPassword;
import com.portal.procucev.model.User;
import com.portal.procucev.service.UserService;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.StatusCodes;
import com.portal.procucev.utils.StatusConstants;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/rest/users")
@CrossOrigin
public class UserController {
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	@Autowired
	private UserService userServices;

	@Autowired
	EmailUserRepo emailUserRepo;

	@RequestMapping(value = "/user", method = RequestMethod.GET)
	public List<User> listUser() {
		return userServices.findAll();
	}

	@RequestMapping(value = "/user", method = RequestMethod.POST)
	public User create(@RequestBody User user) {
		return userServices.save(user);
	}

	@RequestMapping(value = "/user/{id}", method = RequestMethod.DELETE)
	public String delete(@PathVariable(value = "id") Long id) {
		userServices.delete(id);
		return "success";
	}

	@PostMapping(value = "/user/loggedUser")
	public User getUserDetails(@RequestBody User user) {

		return userServices.getUserByEmail(user);

	}

//	@PostMapping(value = "/changePswd")
//	public ResponseEntity<?> changePswd(@RequestBody ResetPassword reset) {
//		boolean status = userServices.changePassword(reset);
//		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
//				: String.valueOf(ApplicationConstants.FAILURE);
//		String msg = status ? String.format(ApplicationConstants.PASSWORD_CHANGED_SUCCESS, "")
//				: String.format(ApplicationConstants.PASSWORD_CHANGED_UNSUCCESS, "");
//		//AppException response = new AppException(statusCode, msg, null, null);
//		return ResponseEntity.ok(new MessageResponse(StatusCodes.OK_VENDOR_CODE,msg,null, statusCode));
//
//	}
	
	@PostMapping(value = "/changePswd")
	public ResponseEntity<?> changePswd(@RequestBody ResetPassword reset) {
	    try {
	        boolean status = userServices.changePassword(reset);
	        String msg = status 
	            ? "Password changed successfully."
	            : "Password change failed.";

	        return ResponseEntity.ok(
	            new MessageResponse(StatusCodes.OK_VENDOR_CODE, msg, null, "Success")
	        );

	    } catch (AppException ae) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	            .body(new MessageResponse(
	                StatusCodes.OK_VENDOR_CODE,
	                ae.getErrorMessage(),
	                null,
	                "Failure"
	            ));
	    }
	}


	@PostMapping(value = "/saveAuth")
	public ResponseEntity<?> saveEmailUser(@RequestBody EmailUser user) {
		try {
			EmailUser emailResponse = emailUserRepo.findByEmail(user.getEmail());
			String msg;
			if (emailResponse != null) {
				String statusCode = String.valueOf(ApplicationConstants.FAILURE);
				msg = "Already Authenticated With Email";
				throw new AppException(statusCode, msg, null, null);
			} else {
				boolean status = userServices.saveEmailuser(user);
				String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
						: String.valueOf(ApplicationConstants.FAILURE);
				msg = status ? String.format(ApplicationConstants.AUTHENTICATE_SUCCESS, "")
						: String.format(ApplicationConstants.AUTHENTICATE_UNSUCCESS, "");
				return ResponseEntity.ok(new MessageResponse(statusCode, msg));
			}
		} catch (AppException e) {
			AppException response = new AppException("200", "Already Authenticated With Email", null, null);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new AppException("Internal Server Error"));
		}
	}

	@PostMapping(value = "/updateAuth")
	public ResponseEntity<?> updateEmailUserPswd(@RequestBody EmailUser user) {
		boolean status = userServices.updateEmailUserPswd(user);
		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.PASSWORD_CHANGED_SUCCESS, "")
				: String.format(ApplicationConstants.PASSWORD_CHANGED_UNSUCCESS, "");
		AppException response = new AppException(statusCode, msg, null, null);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	@PostMapping(value = "/disableUser")
	public ResponseEntity<?> disableUser(@RequestBody User user) {
		boolean status = userServices.disableUser(user);
		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.USER_DELETE_SUCCESS, "")
				: String.format(ApplicationConstants.USER_DELETE_UNSUCCESS, "");
		//AppException response = new AppException(statusCode, msg, null, null);
		return ResponseEntity.ok(new MessageResponse(StatusCodes.OK_VENDOR_CODE,msg,null, statusCode));


	}
	
	@PostMapping(value = "/sendOtp")
	public ResponseEntity<?> sendOtp(@RequestBody Organization organization, HttpServletRequest request) {

	    logger.info("Entered to send OTP");

	    boolean status = userServices.generateOtp(organization, request);

	    if (status) {
	        // Success case: 200 OK
	        MessageResponse response = new MessageResponse(
	                StatusCodes.OK_VENDOR_CODE,
	                String.format(ApplicationConstants.OTP_GENERATE_SUCCESS, ""),
	                null,
	                String.valueOf(ApplicationConstants.SUCCESS)
	        );
	        return ResponseEntity.ok(response);
	    } else {
	        // Failure case: 404 Not Found (user not registered) or 400 Bad Request
	        MessageResponse response = new MessageResponse(
	                "404",
	                "User not registered or OTP could not be sent.",
	                null,
	                String.valueOf(ApplicationConstants.FAILURE)
	        );
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	    }
	}
 //  Always return 200 OK
	    
//
//	    return status 
//	        ? ResponseEntity.status(HttpStatus.OK).body(response)       // 200
//	        : ResponseEntity.status(HttpStatus.CREATED).body(response); // 201
	
	@PostMapping(value = "/validateOtp")
	public ResponseEntity<?> validateOtp(@RequestBody Organization organization) throws UnsupportedEncodingException {

	    logger.info("Entered to validate OTP");

	    boolean status = userServices.validateEmailOtp(organization);

	    if (status) {
	        // Success case: 200 OK
	        MessageResponse response = new MessageResponse(
	                StatusCodes.OK_VENDOR_CODE,
	                String.format(ApplicationConstants.OTP_VALID_SUCCESS, ""),
	                null,
	                String.valueOf(ApplicationConstants.SUCCESS)
	        );
	        return ResponseEntity.ok(response);
	    } else {
	        // Failure case: 400 Bad Request (invalid OTP)
	        MessageResponse response = new MessageResponse(
	                "400",
	                String.format(ApplicationConstants.OTP_VALID_FAILED, ""),
	                null,
	                String.valueOf(ApplicationConstants.FAILURE)
	        );
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	    }
	}


	@PostMapping(value = "/updateSeller")
	    public ResponseEntity<?> updateOrganization(
	     
	            @RequestBody Organization org
	    ) {
	        boolean status = userServices.updateOrganization(org);
	        String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
					: String.valueOf(ApplicationConstants.FAILURE);
			String msg = status ? String.format(ApplicationConstants.SELLER_UPDATE_SUCCESS, "")
					: String.format(ApplicationConstants.SELLER_UPDATE_FAILED, "");
			MessageResponse response = new MessageResponse(StatusCodes.OK_VENDOR_CODE, msg, null, statusCode);
			return new ResponseEntity<>(response, HttpStatus.OK);

		}
	
	@PostMapping(value = "/updateBuyer")
    public ResponseEntity<?> updateBuyer(
     
            @RequestBody Organization org
    ) {
        boolean status = userServices.updateBuyer(org);
        String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.BUYER_UPDATE_SUCCESS, "")
				: String.format(ApplicationConstants.BUYER_UPDATE_FAILED, "");
		MessageResponse response = new MessageResponse(StatusCodes.OK_VENDOR_CODE, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}


	    @GetMapping("/vendorSummary")
	    public ResponseEntity<Map<String, Object>> getVendorSummary() {
	        Map<String, Object> response = new HashMap<>();
	        try {
	            List<VendorSummaryResponse> vendors = userServices.getVendorSummary();
	            if (vendors.isEmpty()) {
	            	response.put("statusCode", StatusCodes.OK_VENDOR_CODE);
	                response.put("satus", "Success");
	                response.put("message", "No vendors found with role 'vendor'");
	                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	            }
                response.put("statusCode", StatusCodes.OK_VENDOR_CODE);
	            response.put("satus", "Success");
	            response.put("data", vendors);
	            return ResponseEntity.ok(response);

	        } catch (Exception e) {
	        	response.put("statusCode", StatusCodes.OK_VENDOR_CODE);
	            response.put("satus", "Failure");
	            response.put("message", "Failed to fetch vendor summary");
	            response.put("error", e.getMessage());
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	        }
	    }
	    
	    @PostMapping(value = "/deactivateOrgUser")
		public ResponseEntity<?> deactivateOrgUser(@RequestBody User user) {
			boolean status = userServices.deactivateOrgUser(user);
			String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
					: String.valueOf(ApplicationConstants.FAILURE);
			String msg = status ? String.format(ApplicationConstants.USER_DELETE_SUCCESS, "")
					: String.format(ApplicationConstants.USER_DELETE_UNSUCCESS, "");
			//AppException response = new AppException(statusCode, msg, null, null);
			return ResponseEntity.ok(new MessageResponse(StatusCodes.OK_VENDOR_CODE,msg,null, statusCode));


		}
}
