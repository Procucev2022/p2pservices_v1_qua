package com.portal.procucev.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portal.procucev.Dto.ClientRFQDto;
import com.portal.procucev.Dto.ForwardRfqVendorRequest;
import com.portal.procucev.Dto.GMTRfqVendorDto;
import com.portal.procucev.Dto.RfqDTO;
import com.portal.procucev.Dto.VendorInfoDto;
import com.portal.procucev.Dto.VendorRFQDto;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.customexception.RfqStatusResponse;
import com.portal.procucev.model.CategoryDivision;
import com.portal.procucev.model.EmailRequest;
import com.portal.procucev.model.GmtItems;
import com.portal.procucev.model.GmtRfqVendors;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.model.RfqStatusRequest;
import com.portal.procucev.model.RfqVendor;
import com.portal.procucev.model.SubscriptionPlan;
import com.portal.procucev.model.User;
import com.portal.procucev.service.GMTService;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.EmailValidatorUtil;
import com.portal.procucev.utils.StatusCodes;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@CrossOrigin
@RestController
@RequestMapping("/rest/gmt")
public class GMTController {
	private static final Logger logger = LoggerFactory.getLogger(GMTController.class);
	@Autowired
	GMTService gmtService;

	@Autowired
	private JavaMailSender javaMailSender;

	@PostMapping(value = "/getClientRfqIds")
	public ResponseEntity<?> getClientRfqIds(@RequestBody User user) {
		List<String> rfqIdsList = gmtService.getClientRfqIds(user);
		return new ResponseEntity<>(rfqIdsList, HttpStatus.OK);
	}

	@PostMapping(value = "/getNoPrRfqByClient")
	public ResponseEntity<?> getNoPrRfqByClient(@RequestBody User user) {
		List<ClientRFQDto> status = gmtService.getNoPrRfqByClient(user);
		return new ResponseEntity<>(status, HttpStatus.OK);
	}

	@GetMapping(value = "/getAllDivisions")
	public ResponseEntity<?> getAllDivision() {
		List<String> status = gmtService.getAllDivision();
		return new ResponseEntity<>(status, HttpStatus.OK);
	}

	@PostMapping(value = "/createRFQForNoPrByClient")
	public ResponseEntity<?> createRFQForNoPrByClient(@RequestBody Rfq rfq) throws Exception {
		boolean response = gmtService.createRFQForNoPrByClient(rfq);
		String statusCode = response ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = response ? String.format(ApplicationConstants.RFQ_CREATED_SUCCESS, "")
				: String.format(ApplicationConstants.RFQ_CREATED_FAILURE, "");
		MessageResponse responseObj = new MessageResponse(StatusCodes.CLIENT_PR_CLOSED_code, msg, null, statusCode);
		return new ResponseEntity<>(responseObj, HttpStatus.OK);
	}

	@PostMapping(value = "/editRFQForNoPrByClient")
	public ResponseEntity<?> editRFQForNoPrByClient(@RequestBody Rfq rfq) throws Exception {
		boolean response = gmtService.editRFQForNoPrByClient(rfq);
		String statusCode = response ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = response ? String.format(ApplicationConstants.RFQ_EDIT_SUCCESS, "")
				: String.format(ApplicationConstants.RFQ_EDIT_FAILURE, "");
		MessageResponse responseObj = new MessageResponse(StatusCodes.CLIENT_PR_CLOSED_code, msg, null, statusCode);
		return new ResponseEntity<>(responseObj, HttpStatus.OK);
	}

	@PostMapping(value = "/convertRfqBoq")
	public ResponseEntity<?> convertRfqBoq(@RequestBody Rfq rfq) {
		List<RfqItem> itemList = gmtService.convertRFQBoq(rfq);
		return new ResponseEntity<>(itemList, HttpStatus.OK);
	}

	@PostMapping(value = "/sendingQueryMail")
	public ResponseEntity<?> queryMail(@RequestBody User user) throws AppException, UnsupportedEncodingException {
		boolean prList = gmtService.queryMail(user);
		String statusCode = prList ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = prList ? String.format(ApplicationConstants.MAIL_SENT_SUCCESS, "")
				: String.format(ApplicationConstants.MAIL_SENT_UNSUCCESS, "");
		String code = prList ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/fetchAllGMTRfqs")
	public ResponseEntity<?> getAllGMTRfq(@RequestBody Organization org) throws AppException {
		List<GMTRfqVendorDto> rfqList = gmtService.getAllGMTRfq(org);
		return new ResponseEntity<>(rfqList, HttpStatus.OK);
	}

	/* Need to check whether required or not */
	@PostMapping(value = "/requestRfqByVendors")
	public ResponseEntity<?> requestRfqByVendors(@RequestBody List<GmtRfqVendors> rfq) {
		logger.info("entered to send an request to multiple rfqs by Vendor");
		boolean response = gmtService.requestRfqByVendors(rfq);
		String statusCode = response ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = response ? String.format(ApplicationConstants.RFQ_REQUEST_SUCCESS, "")
				: String.format(ApplicationConstants.RFQ_REQUEST_FAILURE, "");
		MessageResponse responseObj = new MessageResponse(StatusCodes.OK_VENDOR_CODE, msg, null, statusCode);
		return new ResponseEntity<>(responseObj, HttpStatus.OK);

	}
	
	@PostMapping(value = "/requestRfqBySellers")
	public ResponseEntity<?> Sellers(@RequestBody List<GmtRfqVendors> rfq) {
		logger.info("entered to send an request to multiple rfqs by Vendor");
		boolean response = gmtService.requestRfqBySellers(rfq);
		String statusCode = response ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = response ? String.format(ApplicationConstants.RFQ_REQUEST_SUCCESS, "")
				: String.format(ApplicationConstants.RFQ_REQUEST_FAILURE, "");
		MessageResponse responseObj = new MessageResponse(StatusCodes.OK_VENDOR_CODE, msg, null, statusCode);
		return new ResponseEntity<>(responseObj, HttpStatus.OK);

	}

	@PostMapping(value = "/getVendorsByGmtRFQ")
	public ResponseEntity<?> getVendorsByGmtRFQ(@RequestBody Rfq rfq) {
		logger.info("entered to get an request to multiple Vendors by Rfq");
		List<GmtRfqVendors> response = gmtService.getVendorsByGmtRfq(rfq);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@PostMapping(value = "/approveVendor")
	public ResponseEntity<?> approveVendor(@RequestBody GmtRfqVendors rfq) throws MessagingException {
		logger.info("Entered to approver rfq for vendor");
		boolean response = gmtService.approveVendor(rfq);
		String statusCode = response ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = response ? String.format(ApplicationConstants.RFQ_REQUEST_SUCCESS, "")
				: String.format(ApplicationConstants.RFQ_REQUEST_FAILURE, "");
		MessageResponse responseObj = new MessageResponse(StatusCodes.SEND_RFQ_CODE, msg, null, statusCode);
		return new ResponseEntity<>(responseObj, HttpStatus.OK);

	}

	@PostMapping(value = "/ignoreRfqByVendor")
	public ResponseEntity<?> ignoreRfqByVendor(@RequestBody List<GmtRfqVendors> rfq) {
		logger.info("entered to ignore multiple rfqs by Vendor");
		boolean response = gmtService.ignoreRfqByVendor(rfq);
		String statusCode = response ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = response ? String.format(ApplicationConstants.RFQ_IGNORE_SUCCESS, "")
				: String.format(ApplicationConstants.RFQ_IGNORE_FAILURE, "");
		MessageResponse responseObj = new MessageResponse(StatusCodes.SEND_RFQ_CODE, msg, null, statusCode);
		return new ResponseEntity<>(responseObj, HttpStatus.OK);

	}

	@PostMapping(value = "/rejectRfqForVendorByCM2")
	public ResponseEntity<?> rejectRfqForVendor(@RequestBody GmtRfqVendors rfq) {
		logger.info("entered to reject rfqs for Vendor by CM2");
		boolean response = gmtService.rejectRfqForVendor(rfq);
		String statusCode = response ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = response ? String.format(ApplicationConstants.VENDOR_RFQ_REJECTED_SUCCESS, "")
				: String.format(ApplicationConstants.VENDOR_RFQ_REJECTION_FAILED, "");
		MessageResponse responseObj = new MessageResponse(StatusCodes.SEND_RFQ_CODE, msg, null, statusCode);
		return new ResponseEntity<>(responseObj, HttpStatus.OK);

	}

	@GetMapping("/fetchAllGMTRfqsForCM")
	public ResponseEntity<?> getAllRfqforCM() throws AppException {
		List<RfqDTO> rfqList = gmtService.getAllRfqForCM();
		return new ResponseEntity<>(rfqList, HttpStatus.OK);
	}

	/* Need to check whether required or not */
	@PostMapping(value = "/raiseQueryByVendor")
	public ResponseEntity<?> raiseQueryByVendor(@RequestBody GmtRfqVendors rfq) {
		logger.info("Entered to raise Query By Vendor for RFQ");
		boolean response = gmtService.raiseQueryByVendor(rfq);
		String statusCode = response ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = response ? String.format(ApplicationConstants.RAISED_QUERY_SUCCESS, "")
				: String.format(ApplicationConstants.RAISED_QUERY_FAILED, "");
		MessageResponse responseObj = new MessageResponse(StatusCodes.SEND_RFQ_CODE, msg, null, statusCode);
		return new ResponseEntity<>(responseObj, HttpStatus.OK);

	}

	@GetMapping("/fetchAllClientGMTRfqsForCM")
	public ResponseEntity<?> fetchAllClientGMTRfqsForCM() throws AppException {
		List<RfqDTO> rfqList = gmtService.fetchAllClientGMTRfqsForCM();
		return new ResponseEntity<>(rfqList, HttpStatus.OK);
	}

	@PostMapping(value = "/getCategoryByDivision")
	public ResponseEntity<?> getCategoryByDivision(@RequestBody CategoryDivision cat) {
		logger.info("Entered to get Category List By Division ");
		List<String> status = gmtService.getCategoryByDivision(cat);
		return new ResponseEntity<>(status, HttpStatus.OK);
	}

	@GetMapping(value = "/getAllGmtItems")
	public ResponseEntity<?> getAllGmtItems() {
		List<GmtItems> status = gmtService.getAllGmtItems();
		return new ResponseEntity<>(status, HttpStatus.OK);
	}

	/**
	 * Method to fetch an RFQ by an ID
	 * 
	 * @param rfq
	 * @return
	 * @throws Exception
	 */
	@PostMapping(value = "/fetchRfqById")
	public ResponseEntity<?> fetchRfqById(@RequestBody Rfq rfq) {
		ResponseEntity<?> status = gmtService.fetchRfqById(rfq);
		return status;
	}

	@GetMapping(value = "/getAllCategories")
	public ResponseEntity<?> getAllCategory() {
		List<String> status = gmtService.getAllCategory();
		return new ResponseEntity<>(status, HttpStatus.OK);
	}

	@GetMapping(value = "/getRFQsForNoPR")
	public ResponseEntity<?> getRFQsForNoPR() {
		List<RfqDTO> status = gmtService.getRFQsForNoPR();
		return new ResponseEntity<>(status, HttpStatus.OK);
	}

	@GetMapping("/getAllVendors")
	public ResponseEntity<?> getAllVendors() {
		List<VendorRFQDto> response = gmtService.getAllVendors();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/getAllVendorsByCategory")
	public ResponseEntity<?> getAllVendorsByCategory(@RequestBody Organization organization) {
		List<VendorRFQDto> response = gmtService.getAllVendorsByCategory(organization);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/createRFQForNoPrWithItems")
	public ResponseEntity<?> createRFQForNoPrWithItems(@RequestBody Rfq rfq) {
		boolean response = gmtService.createRFQWithNoPr(rfq);
		String statusCode = response ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = response ? String.format(ApplicationConstants.RFQ_CREATED_SUCCESS, "")
				: String.format(ApplicationConstants.RFQ_CREATED_FAILURE, "");
		MessageResponse responseObj = new MessageResponse(StatusCodes.CLIENT_PR_CLOSED_code, msg, null, statusCode);
		return new ResponseEntity<>(responseObj, HttpStatus.OK);
	}

//	@PostMapping(value = "/forwardRfq")
//	public ResponseEntity<?> forwardRfqForNoPr(@RequestBody Rfq rfq) {
//		boolean response = gmtService.forwardRfqForNoPr(rfq);
//		String statusCode = response ? String.valueOf(ApplicationConstants.SUCCESS)
//				: String.valueOf(ApplicationConstants.FAILURE);
//		String msg = response ? String.format(ApplicationConstants.RFQ_FORWARD_SUCCESS, "")
//				: String.format(ApplicationConstants.RFQ_FORWARD_FAILURE, "");
//		MessageResponse responseObj = new MessageResponse(StatusCodes.OK_VENDOR_CODE, msg, null, statusCode);
//		return new ResponseEntity<>(responseObj, HttpStatus.OK);
//	}
	@PostMapping(value = "/forwardRfq")
	public ResponseEntity<?> forwardRfqForNoPr(@RequestBody Rfq rfq) {
	    boolean response = gmtService.forwardRfqForNoPr(rfq);
	    String statusCode = response ? ApplicationConstants.SUCCESS : ApplicationConstants.FAILURE;
	    String msg;

	    if (response) {
	        if ("Forward".equalsIgnoreCase(rfq.getRequestType())) {
	            msg = "RFQ Forwarded Successfully";
	        } else {
	            msg = "RFQ Invited Successfully";
	        }
	    } else {
	        if ("Forward".equalsIgnoreCase(rfq.getRequestType())) {
	            msg = "RFQ Forward Failed";
	        } else {
	            msg = "RFQ Invitation Failed";
	        }
	    }

	    MessageResponse responseObj = new MessageResponse(StatusCodes.OK_VENDOR_CODE, msg, null, statusCode);
	    return new ResponseEntity<>(responseObj, HttpStatus.OK);
	}

	@PostMapping(value = "/getItemsbyrfqid")
	public ResponseEntity<?> getItemsbyrfqid(@RequestBody Rfq rfq) {
		List<RfqItem> status = gmtService.getItemsbyrfqrid(rfq);
		return new ResponseEntity<>(status, HttpStatus.OK);
	}

	@GetMapping("/getSelfRegisterClients")
	public ResponseEntity<?> getSelfRegisterClients() throws AppException {
		logger.info("Enters to fetch Self Register client list");
		List<Organization> clientList = gmtService.fetchSelfRegisterClients();
		return new ResponseEntity<>(clientList, HttpStatus.OK);
	}

	@PostMapping(value = "/editUser")
	public ResponseEntity<?> editUser(@RequestBody User user) throws AppException {
		boolean prList = gmtService.editUser(user);
		String statusCode = prList ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = prList ? String.format(ApplicationConstants.USER_EDIT_SUCCESS, "")
				: String.format(ApplicationConstants.USER_EDIT_FAILED, "");
		String code = prList ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/acceptSelfRegisterClient")
	public ResponseEntity<?> acceptClient(@RequestBody User user) throws AppException, UnsupportedEncodingException {
		boolean prList = gmtService.acceptSelfClient(user);
		String statusCode = prList ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = prList ? String.format(ApplicationConstants.USER_ACCEPT_SUCCESS, "")
				: String.format(ApplicationConstants.USER_ACCEPT_FAILED, "");
		String code = prList ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/ignoreSelfRegisterClient")
	public ResponseEntity<?> ignoreClient(@RequestBody User user) throws AppException {
		boolean prList = gmtService.ignoreClient(user);
		String statusCode = prList ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = prList ? String.format(ApplicationConstants.USER_IGNORED_SUCCESS, "")
				: String.format(ApplicationConstants.USER_IGNORED_FAILED, "");
		String code = prList ? String.valueOf(HttpStatus.OK.value())
				: String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
		MessageResponse response = new MessageResponse(code, msg, null, statusCode);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/disableUser")
	public ResponseEntity<?> disableUser(@RequestBody User user) {
		boolean status = gmtService.disableUser(user);
		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.USER_DELETE_SUCCESS, "")
				: String.format(ApplicationConstants.USER_DELETE_UNSUCCESS, "");
		AppException response = new AppException(statusCode, msg, null, null);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@PostMapping("/getclientusersByclient")
	public ResponseEntity<?> getclientusersByclient(@RequestBody Organization org) {
		Object clientList = gmtService.getclientusersByClientId(org);
		return new ResponseEntity<>(clientList, HttpStatus.OK);
	}

	@PostMapping(value = "/editAndResendRfqByCM")
	public ResponseEntity<?> editAndResendRfq(@RequestBody Rfq rfq) throws MessagingException {
		boolean response = gmtService.editAndResendRfq(rfq);
		String statusCode = response ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = response ? String.format(ApplicationConstants.RFQ_EDIT_SUCCESS, "")
				: String.format(ApplicationConstants.RFQ_EDIT_FAILURE, "");
		MessageResponse responseObj = new MessageResponse(StatusCodes.CLIENT_PR_CLOSED_code, msg, null, statusCode);
		return new ResponseEntity<>(responseObj, HttpStatus.OK);
	}

	@PostMapping(value = "/getVendorbyRFQ")
	public ResponseEntity<?> getVendorbyRFQ(@RequestBody Rfq rfq) {
		List<RfqVendor> status = gmtService.getVendorsbyRFQ(rfq);
		return new ResponseEntity<>(status, HttpStatus.OK);
	}

	@GetMapping(value = "/getGmtBuyers")
	public ResponseEntity<?> getGmtBuyers() {
		List<User> status = gmtService.getGmtBuyers();
		return new ResponseEntity<>(status, HttpStatus.OK);
	}

//	@PostMapping(value = "/createRFQByClient")
//	public ResponseEntity<?> createRFQByClient(@RequestBody Rfq rfq) throws Exception {
//		Map<String, Object> result = gmtService.createRFQByClient(rfq);
//
//		boolean success = result != null;
//
//		String statusCode = success ? String.valueOf(ApplicationConstants.SUCCESS)
//				: String.valueOf(ApplicationConstants.FAILURE);
//
//		String msg = success ? String.format(ApplicationConstants.RFQ_CREATED_SUCCESS, "")
//				: String.format(ApplicationConstants.RFQ_CREATED_FAILURE, "");
//
//		// directly pass result map with rfqId and rfquuid
//		Map<String, Object> data = success ? result : Map.of();
//
//		MessageResponse response = new MessageResponse(StatusCodes.OK_VENDOR_CODE, msg, data, statusCode, new Date());
//
//		return new ResponseEntity<>(response, HttpStatus.OK);
//	}
	
	@PostMapping(value = "/createRFQByClient")
	public ResponseEntity<?> createRFQByClient(@RequestBody Rfq rfq) throws Exception {
	    Map<String, Object> result = gmtService.createRFQByClient(rfq);
	    boolean success = (result != null && !result.isEmpty());

	    // business code (you can use your StatusCodes constants)
	    String businessCode = success ? "200" : "500";

	    String msg = success ? "RFQ created successfully" : "Failed to create RFQ";

	    // data payload (rfq details on success, empty on failure)
	    Map<String, Object> data = success ? result : Map.of();

	    // Use your existing 5-arg constructor:
	    // MessageResponse(String code, String message, Object data, String status, Date timestamp)
	    MessageResponse response = new MessageResponse(
	            businessCode,
	            msg,
	            data,
	            success ? "Success" : "Failure", // this becomes the "status" in JSON
	            new Date()
	    );

	    // set error messages (MessageResponse#setErrorMsg expects List<String>)
	    if (!success) {
	        List<String> errors = new ArrayList<>();
	        errors.add("Error occurred while creating RFQ");
	        response.setErrorMsg(errors);
	    }

	    return ResponseEntity.ok(response); // always HTTP 200 as you requested
	}



//	@PostMapping("/sendEmail")
//	public ResponseEntity<?> sendEmail(@RequestBody EmailRequest request) {
//		boolean status = gmtService.sendEmail(request);
//		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
//				: String.valueOf(ApplicationConstants.FAILURE);
//		String msg = status ? String.format(ApplicationConstants.EMAIL_SENT_SUCCESS, "")
//				: String.format(ApplicationConstants.EMAIL_SENT_UNSUCCESS, "");
//		MessageResponse response = new MessageResponse(StatusCodes.OK_VENDOR_CODE, msg, new Date(), statusCode, null);
//		return new ResponseEntity<>(response, HttpStatus.OK);
//	}
	@PostMapping("/sendEmail")
	public ResponseEntity<MessageResponse> sendEmail(@RequestBody EmailRequest emailRequest) {
		MessageResponse response = gmtService.sendEmail(emailRequest);
		HttpStatus httpStatus;
		switch (response.getStatusCode()) {
		case "200":
			httpStatus = HttpStatus.OK;
			break;
		case "206":
			httpStatus = HttpStatus.PARTIAL_CONTENT;
			break;
		case "400":
			httpStatus = HttpStatus.BAD_REQUEST;
			break;
		case "500":
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
			break;
		default:
			httpStatus = HttpStatus.OK;
		}
		return new ResponseEntity<>(response, httpStatus);
	}

	@GetMapping(value = "/getSubscriptionPlans")
	public ResponseEntity<?> getSubscriptionPlans() {
		try {
			// Call service to fetch all subscription plans
			List<SubscriptionPlan> plans = gmtService.getSubscriptionPlans();

			boolean success = (plans != null && !plans.isEmpty());

			String statusCode = success ? String.valueOf(ApplicationConstants.SUCCESS)
					: String.valueOf(ApplicationConstants.FAILURE);

			String msg = success ? String.format("Subscription plans fetched successfully")
					: String.format("No subscription plans found");

			// 👇 Wrap data in a Map like you did with rfqId
			Map<String, Object> data = success ? Map.of("plans", plans) : null;

			// Build standard response
			MessageResponse response = new MessageResponse(StatusCodes.OK_VENDOR_CODE, msg, data, statusCode,
					new Date());

			return new ResponseEntity<>(response, HttpStatus.OK);

		} catch (Exception e) {
			// In case of error, return failure response
			MessageResponse response = new MessageResponse(StatusCodes.OK_VENDOR_CODE,
					"Error fetching subscription plans: " + e.getMessage(), null,
					String.valueOf(ApplicationConstants.FAILURE), new Date());
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/rfqStatus")
	public ResponseEntity<Map<String, Object>> getRfqStatus(@RequestBody RfqStatusRequest request) {
		List<RfqStatusResponse> data = gmtService.getRfqStatuses(request);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", data);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/getSellerRfqCredits")
	public ResponseEntity<Map<String, Object>> getSellerRfqCredits(@RequestBody Organization org) {
		int credits = gmtService.getSellerRfqCredits(org);

		Map<String, Object> data = new HashMap<>();
		data.put("creditsAvailable", credits);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", data);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/getOrgById")
	public ResponseEntity<?> getOrgById(@RequestBody Organization organization) throws IOException {
		logger.info("enters to fetch the Org list");

		Organization vendorList = gmtService.getOrgById(organization);
		return new ResponseEntity<>(vendorList, HttpStatus.OK);
	}

	@PostMapping("/getOrgByUserId")
	public ResponseEntity<?> getOrgByUserId(@RequestBody User user) throws IOException {
		logger.info("enters to fetch the Org list");

		Organization vendorList = gmtService.getOrgByUserId(user);
		return new ResponseEntity<>(vendorList, HttpStatus.OK);
	}

	@PostMapping("/getRfqByCategory")
	public ResponseEntity<Map<String, Object>> getRfqByCategory(@RequestBody Organization org) {
		Map<String, Object> rfqs = gmtService.getRfqByItemCategory(org);
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", rfqs);

		return ResponseEntity.ok(response);
	}

//	@PostMapping("/getItemsByCategory")
//	public ResponseEntity<Map<String, Object>> getRfqByCategory(@RequestBody ItemCategory item) {
//		Map<String, Object> rfqs = gmtService.getRfqByItemCategory(org);
//		Map<String, Object> response = new HashMap<>();
//		response.put("success", true);
//		response.put("data", rfqs);
//
//		return ResponseEntity.ok(response);
//	}

	@PostMapping(value = "/forwardRfqsToVendor")
	public ResponseEntity<?> forwardRfqsToVendor(@RequestBody ForwardRfqVendorRequest request) {
		Map<String, Object> response = gmtService.forwardRfqsToVendor(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping(value = "/getOpenRfqs")
	public ResponseEntity<Map<String, Object>> getOpenRfqsForSeller(@RequestBody Organization org) {
		List<Map<String, Object>> openRfqs = gmtService.getLastOpenRfqsForVendor(org.getId());

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("success", true);
		response.put("open_rfqs", openRfqs);
		response.put("total_count", openRfqs.size());

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("seller_id", org.getId());
//	    metadata.put("query_timestamp", Instant.now());
		metadata.put("filter_criteria",
				Map.of("status", "open_for_bidding", "email_sent", true, "bid_submitted", false));
		response.put("metadata", metadata);

		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/getVendorInfo")
	public ResponseEntity<?> getVendorInfo(@RequestBody Organization orgRequest) {
	    if (orgRequest.getId() == null) {
	        return ResponseEntity.ok().body("Organization id and userId are required");
	    }

	    VendorInfoDto response = gmtService.getVendorInfo(orgRequest);

	    if (response == null) {
	        return ResponseEntity.status(HttpStatus.OK)
	                             .body("Vendor not found for orgId: " + orgRequest.getId());
	    }

	    return ResponseEntity.ok(response);
	}
	
	@GetMapping(value = "/runEmailForwarder")
	public ResponseEntity<?> emailForwarder() {
		gmtService.emailForwarder();
		return new ResponseEntity<>( HttpStatus.OK);
	}
	
	@PostMapping(value = "/getBuyerDataByRFQ")
	public ResponseEntity<?> getBuyerDataByRFQ(@RequestBody Rfq rfq) {
		logger.info("Entered to get buyer data by Rfq");
		User response = gmtService.getBuyerDataByRFQ(rfq);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

}
