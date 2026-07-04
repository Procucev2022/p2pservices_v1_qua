package com.portal.procucev.controller;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.ClientDeliveryLocationRfq;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.User;
import com.portal.procucev.service.AutomaticRfqService;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.StatusCodes;

@RestController
@RequestMapping("/automate")
public class AutomaticRfqCreationController {

	public static final Logger logger = LoggerFactory.getLogger(AutomaticRfqCreationController.class);

	@Autowired
	AutomaticRfqService autoRfqService;

	@Autowired
	PincodeDao pincodeDao;
	
	@Autowired
	UserDao userDao;
	
	@Autowired
	OrgDao orgDao;

	@PostMapping("/raiseRfq")
	public ResponseEntity<?> raiseRfqAuto(@RequestBody Rfq rfq) {

	    Optional<User> userOpt = userDao.findById(rfq.getUser());

	    if (!userOpt.isPresent()) {
	        return ResponseEntity.badRequest().body("User not found");
	    }

	    User user = userOpt.get();

	    // Check if delivery location list exists
	    if (rfq.getClientdeliverylocationrfq() == null || rfq.getClientdeliverylocationrfq().isEmpty()) {

	        ClientDeliveryLocationRfq delivery = new ClientDeliveryLocationRfq();
	        delivery.setCity(user.getOrg().getCity());
	        delivery.setState(user.getOrg().getState());
	        delivery.setPincode(user.getOrg().getZipCode());
	    }

	    ClientDeliveryLocationRfq delivery = rfq.getClientdeliverylocationrfq().get(0);

	    String city = delivery.getCity();
	    String state = delivery.getState();
	    String pinCode = delivery.getPincode();

	    logger.info("Client delivery location : {} + {} + {}", city, state, pinCode);

	    // If all three are present, nothing to do
	    if (city != null && !city.trim().isEmpty()
	            && state != null && !state.trim().isEmpty()
	            && pinCode != null && !pinCode.trim().isEmpty()) {

	        logger.info("Delivery Location Details are perfect....");
	    }
	    // Only pincode is provided
	    else if (pinCode != null && !pinCode.trim().isEmpty()) {

	        logger.info("Inside pincode block");

	        PincodeData pincodeData = pincodeDao.findByPincode(pinCode);
	       

	        if (pincodeData != null) {
	        	 logger.info("Location as per PinCode : {},{},{}",pincodeData.getCity(),pincodeData.getState(),pincodeData.getPincode());
	            delivery.setCity(pincodeData.getCity());
	            delivery.setState(pincodeData.getState());
	        } else {
	            delivery.setCity(user.getOrg().getCity());
	            delivery.setState(user.getOrg().getState());
	            delivery.setPincode(user.getOrg().getZipCode());
	        }
	    }
	    // Only city is provided
	    else if (city != null && !city.trim().isEmpty()) {

	        logger.info("Inside city block");

	        PincodeData pincodeData = pincodeDao.findByCityIgnoreCase(city);
	        


	        if (pincodeData != null) {
	        	logger.info("Location as per City : {},{},{}",pincodeData.getCity(),pincodeData.getState(),pincodeData.getPincode());
	            delivery.setPincode(pincodeData.getPincode());
	            delivery.setState(pincodeData.getState());
	        } else {
	            delivery.setCity(user.getOrg().getCity());
	            delivery.setState(user.getOrg().getState());
	            delivery.setPincode(user.getOrg().getZipCode());
	        }
	    }
	    // Nothing is provided
	    else {

	        logger.info("No delivery location provided. Using organization address.");
	        logger.info("Organization Address : {},{},{}",user.getOrg().getCity(),user.getOrg().getState(),user.getOrg().getZipCode());

	        delivery.setCity(user.getOrg().getCity());
	        delivery.setState(user.getOrg().getState());
	        delivery.setPincode(user.getOrg().getZipCode());
	    }

	    boolean response = autoRfqService.raiseRfq(rfq);

	    String statusCode = response
	            ? String.valueOf(ApplicationConstants.SUCCESS)
	            : String.valueOf(ApplicationConstants.FAILURE);

	    String msg = response
	            ? String.format(ApplicationConstants.RFQ_CREATED_SUCCESS, "")
	            : String.format(ApplicationConstants.RFQ_CREATED_FAILURE, "");

	    MessageResponse responseObj = new MessageResponse(
	            StatusCodes.CLIENT_PR_CLOSED_code,
	            msg,
	            null,
	            statusCode);

	    return new ResponseEntity<>(responseObj, HttpStatus.OK);
	}

	@PostMapping("/validateEmail")
	public ResponseEntity<?> validateEmail(@RequestParam String email) {

		logger.info("Email Id : {}", email);
		Map<String, String> response = autoRfqService.validateEmail(email);

		return ResponseEntity.ok(response);
	}

}
