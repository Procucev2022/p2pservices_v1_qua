package com.portal.procucev.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.portal.procucev.dao.GmtItemsDao;
import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.GmtItems;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.Organization;
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

	@Override
	public boolean raiseRfq(Rfq rfq) {
		
		logger.info("Request received for RFQ creation with No PR by client {}", rfq);

		try {
			MasterStatus resultStatus = masterStatusDao.findByStatus(StatusConstants.pcprinprogress);
			MasterStatus newStatus = masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_NEW);

			rfq.setByClient(true);
			rfq.setStatus(resultStatus);
			rfq.setClientStatus(newStatus);
			rfq.setSourceType(ApplicationConstants.TOOL); //need to check

			// String rfqId = selfRegistrationService.generateId("RFQ");

			String rfqId = generateRfqId("RFQ");
			logger.info("Generated RFQ Id: {}", rfqId);
			rfq.setRfqId(rfqId);

			List<RfqItem> rfqItems = rfq.getRfqItem();
			List<GmtItems> gmtItems = rfqItems.stream().map(this::mapRfqItemToGmtItem).collect(Collectors.toList());
			gmtItemsDao.saveAll(gmtItems);
			logger.info("Saved RFQ items in GMT Items"); 
			rfqDao.save(rfq);
			logger.info("Completed Saving RFQ");
			return true;
		} catch (DataAccessException e) {
			logger.error("Error occurred while creating RFQ: {}", e.getMessage());
			return false;
		}
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
		return companyLetters + datePart + millis;
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
