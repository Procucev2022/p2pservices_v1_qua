package com.portal.procucev.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.portal.procucev.customexception.RfqDocumentSizeExceededException;
import com.portal.procucev.dao.GmtItemsDao;
import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.ClientDeliveryLocationRfq;
import com.portal.procucev.model.GmtItems;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.RFQDocument;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.model.User;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.StatusConstants;

@Component
public class AutomaticRfqServiceImpl implements AutomaticRfqService{
	
private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AutomaticRfqServiceImpl.class);

@Autowired
MasterStatusDao masterStatusDao;

@Autowired
GmtItemsDao gmtItemsDao;

@Autowired
RfqDao rfqDao;

@Autowired
UserDao userDao;

@Autowired
OrgDao orgDao;

@Autowired
PincodeDao pincodeDao;

	/**
	 * Per-document size cap, shared with the web multipart limit and the email pipeline's attachment
	 * limit so a file accepted by one entry point is accepted by the other.
	 */
	@Value("${app.rfq.max-document-bytes:26214400}")
	private long maxDocumentBytes = 26214400L;

	@Override
	public long getMaxDocumentBytes() {
		return maxDocumentBytes;
	}

	/**
	 * Creates a client RFQ and its line items.
	 *
	 * <p>Transactional because it writes two tables: the RFQ with its cascaded {@code rfq_items},
	 * and the matching {@code gmt_items} rows. Without a transaction a failure on the second write
	 * left the first committed, so an RFQ could exist with no GMT items or vice versa.
	 */
	@Override
	@org.springframework.transaction.annotation.Transactional
	public boolean raiseRfq(Rfq rfq) {
		
		logger.info("Request received for RFQ creation with No PR by client: rfqId={}, projectDesc='{}', category='{}', itemCount={}",
				rfq.getRfqId(), rfq.getProjectDesc(), rfq.getCategory(),
				rfq.getRfqItem() != null ? rfq.getRfqItem().size() : 0);

		try {
			MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.pcprinprogress);
			MasterStatus newStatus = masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_NEW);

			rfq.setByClient(true);
			rfq.setStatus(resultStatus);
			rfq.setClientStatus(newStatus);
			String srcType = (rfq.getSourceType() != null && !rfq.getSourceType().isBlank())
					? rfq.getSourceType()
					: ApplicationConstants.TOOL;
			rfq.setSourceType(srcType);

			if (rfq.getRfqItem() != null) {
				for (RfqItem item : rfq.getRfqItem()) {
					if (item.getQuantity() <= 0) {
						item.setQuantity(1.0);
					}
					if (item.getUnitofMeasures() == null || item.getUnitofMeasures().isBlank()) {
						item.setUnitofMeasures("Nos");
					}
				}
			}

			// String rfqId = selfRegistrationService.generateId("RFQ");

			String rfqId = (rfq.getRfqId() != null && !rfq.getRfqId().isBlank())
					? rfq.getRfqId()
					: generateRfqId("RFQ");
			logger.info("Using RFQ Id: {}", rfqId);
			rfq.setRfqId(rfqId);

			// Save the RFQ first. Its line items are cascaded here, which is what assigns each
			// RfqItem its UUID. mapRfqItemToGmtItem reads that UUID for rfq_item_id, so building
			// the GMT rows before this point wrote every one of them with rfq_item_id = NULL,
			// leaving the per-item GMT records unlinked from the RFQ line they describe.
			rfqDao.saveAndFlush(rfq);
			logger.info("Completed Saving RFQ");

			List<RfqItem> rfqItems = rfq.getRfqItem();
			List<GmtItems> gmtItems = rfqItems.stream().map(this::mapRfqItemToGmtItem).collect(Collectors.toList());
			gmtItemsDao.saveAll(gmtItems);
			logger.info("Saved {} RFQ item(s) in GMT Items for rfqId={}", gmtItems.size(), rfq.getRfqId());
			return true;
		} catch (DataAccessException e) {
			logger.error("Error occurred while creating RFQ: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Fills in whatever part of the delivery address the caller did not supply.
	 *
	 * <p>Resolution order is: use the address as given when city, state and pincode are all present;
	 * otherwise complete it from the pincode master by pincode, then by city; and fall back to the
	 * buyer's organisation address when nothing usable was supplied or the master has no match.
	 */
	@Override
	public void resolveDeliveryLocation(Rfq rfq, User user) {
		if (rfq == null) {
			return;
		}

		if (rfq.getClientdeliverylocationrfq() == null || rfq.getClientdeliverylocationrfq().isEmpty()) {
			ClientDeliveryLocationRfq fresh = new ClientDeliveryLocationRfq();
			applyOrganizationAddress(fresh, user);
			rfq.setClientdeliverylocationrfq(new ArrayList<>(Collections.singletonList(fresh)));
			return;
		}

		ClientDeliveryLocationRfq delivery = rfq.getClientdeliverylocationrfq().get(0);
		if (delivery == null) {
			return;
		}

		String city = delivery.getCity();
		String state = delivery.getState();
		String pinCode = delivery.getPincode();

		logger.info("Client delivery location : {} + {} + {}", city, state, pinCode);

		if (isPresent(city) && isPresent(state) && isPresent(pinCode)) {
			logger.info("Delivery Location Details are perfect....");
			return;
		}

		if (isPresent(pinCode)) {
			logger.info("Inside pincode block");
			PincodeData pincodeData = pincodeDao.findByPincode(pinCode.trim());
			if (pincodeData != null) {
				logger.info("Location as per PinCode : {},{},{}", pincodeData.getCity(), pincodeData.getState(),
						pincodeData.getPincode());
				delivery.setCity(pincodeData.getCity());
				delivery.setState(pincodeData.getState());
			} else {
				applyOrganizationAddress(delivery, user);
			}
			return;
		}

		if (isPresent(city)) {
			logger.info("Inside city block");
			PincodeData pincodeData = pincodeDao.findByCityIgnoreCase(city.trim());
			if (pincodeData != null) {
				logger.info("Location as per City : {},{},{}", pincodeData.getCity(), pincodeData.getState(),
						pincodeData.getPincode());
				delivery.setPincode(pincodeData.getPincode());
				delivery.setState(pincodeData.getState());
			} else {
				applyOrganizationAddress(delivery, user);
			}
			return;
		}

		logger.info("No delivery location provided. Using organization address.");
		applyOrganizationAddress(delivery, user);
	}

	/**
	 * Builds the {@code rfq_documents} rows for an RFQ from Base64 payloads.
	 *
	 * <p>Only {@code file} receives the document bytes. {@code file_details} is deliberately left
	 * unset: that column is a 64KB {@code BLOB}, so writing the payload into it made MySQL reject
	 * the insert with "Data truncation: Data too long for column 'file_details'" for any attachment
	 * over 64KB, rolling back the whole RFQ. Nothing reads the column.
	 */
	@Override
	public void attachDocuments(Rfq rfq, List<Map<String, String>> documents) {
		if (rfq == null || documents == null || documents.isEmpty()) {
			return;
		}

		List<RFQDocument> mapped = new ArrayList<>();
		for (Map<String, String> documentMap : documents) {
			if (documentMap == null) {
				continue;
			}
			String encodedFile = documentMap.get("file");
			if (encodedFile == null || encodedFile.isBlank()) {
				continue;
			}

			String fileName = documentMap.get("fileName");
			byte[] bytes;
			try {
				bytes = Base64.getDecoder().decode(encodedFile);
			} catch (IllegalArgumentException e) {
				logger.warn("Skipping RFQ document '{}': payload is not valid Base64 ({})", fileName, e.getMessage());
				continue;
			}

			if (bytes.length > maxDocumentBytes) {
				throw new RfqDocumentSizeExceededException(fileName, bytes.length, maxDocumentBytes);
			}

			RFQDocument rfqDocument = new RFQDocument();
			rfqDocument.setFileName(fileName);
			rfqDocument.setFile(bytes);
			rfqDocument.setVersion(1);
			rfqDocument.setRfq(rfq);
			mapped.add(rfqDocument);

			logger.info("Attached RFQ document '{}' ({} bytes) to rfqId={}", fileName, bytes.length, rfq.getRfqId());
		}

		if (!mapped.isEmpty()) {
			rfq.setRfqDocument(mapped);
		}
	}

	private void applyOrganizationAddress(ClientDeliveryLocationRfq delivery, User user) {
		if (delivery == null || user == null || user.getOrg() == null) {
			return;
		}
		Organization org = user.getOrg();
		logger.info("Organization Address : {},{},{}", org.getCity(), org.getState(), org.getZipCode());
		delivery.setCity(org.getCity());
		delivery.setState(org.getState());
		delivery.setPincode(org.getZipCode());
	}

	private boolean isPresent(String value) {
		return value != null && !value.trim().isEmpty();
	}

	@Override
	public Map<String,String> validateEmail(String email) {
		
		Map<String, String> response = new HashMap<>();

	    User user = userDao.findByLatestUserName(email);

	    // User not found
	    if (user == null) {

	        response.put("status", "Failure");
	        response.put("name", null);
	        response.put("orgId", null);
	        response.put("userId", null);
	        response.put("code", "10");
	        response.put("description", "No Users Found");

	        return response;
	    }

	    // Populate user details
	    response.put("name", user.getFullName());
	    response.put("userId", user.getId());

	    // Org Id is null
	    if (user.getOrg().getId() == null) {

	        response.put("status", "Success");
	        response.put("orgId", null);
	        response.put("code", "00");
	        response.put("description", "User Found but Org Id is null");

	        return response;
	    }


	    response.put("status", "Success");
	    response.put("orgId",user.getOrg().getId());
	    response.put("code", "00");
	    response.put("description", "User Fetched Successfully");

	    return response;	
	}
	
	
	public String generateRfqId(String company) {
		String companyLetters = "";
		if (company != null && !company.isEmpty()) {
			companyLetters = company.length() >= 3 ? company.substring(0, 3).toUpperCase() : company.toUpperCase();
		}

		// current date in ddMM format
		String datePart = new SimpleDateFormat("yyddMM").format(new Date());

		// milliseconds part
		long millis = System.currentTimeMillis() % 1000000; // last 6 digits to shorten

		// optional random 3-digit suffix
		// int random = (int) (Math.random() * 1000);

		// return companyLetters + datePart + millis + String.format("%03d", random);
		return companyLetters + datePart + String.format("%06d", millis);
	}
	
	private GmtItems mapRfqItemToGmtItem(RfqItem rfqItem) {
		GmtItems gmtItem = new GmtItems();
		gmtItem.setBrand(rfqItem.getRemarks()+" "+rfqItem.getBrand());
		gmtItem.setDescription(rfqItem.getDescription());
		gmtItem.setQuantity(rfqItem.getQuantity());
		gmtItem.setRemarks(rfqItem.getRemarks());
		gmtItem.setUnitofMeasures(rfqItem.getUnitofMeasures());
		gmtItem.setRfqItemId(rfqItem.getId());
		return gmtItem;
	}

}
