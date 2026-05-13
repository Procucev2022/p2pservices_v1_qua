package com.portal.procucev.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.portal.procucev.Dto.BuyerCategoryReportDto;
import com.portal.procucev.Dto.SellerCategoryReportDto;
import com.portal.procucev.dao.GmtRfqVendorDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.OrgTypeDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.utils.ApplicationConstants;

@Service
public class ReportsServiceImpl  implements ReportsService {
	private static final Logger logger = LoggerFactory.getLogger(ReportsServiceImpl.class);
	
	@Autowired
	UserDao userDao;

	@Autowired
	OrgDao orgDao;
	
	@Autowired
	OrgTypeDao orgTypeDao;
	
	@Autowired
	GmtRfqVendorDao gmtRfqVendorDao;
	
	@Autowired
	RfqDao rfqDao;
	
	@Override
	public List<?> getSellerReports(String startDate, String endDate, String requestType) throws ParseException {
		   Date fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
	        Date toDate = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);

		    // Step 1: Vendor Org Type
		    OrgType orgTypeObject =
		            orgTypeDao.findByTypeName(ApplicationConstants.VENDOR);

	    if ("sellerReport".equalsIgnoreCase(requestType)) {
	        return gmtRfqVendorDao.getSellerReport(fromDate, toDate);
	    }

	    if ("sellerSummary".equalsIgnoreCase(requestType)) {
	       return orgDao.getSellerSummary(orgTypeObject,fromDate, toDate);
	    }

	    if ("sellerCategoryReport".equalsIgnoreCase(requestType)) {
	    	return getSellerCategoryReport(startDate, endDate);
	    }

	    throw new RuntimeException("Invalid requestType");
	}
	
	public List<SellerCategoryReportDto> getSellerCategoryReport(
	        String startDate,
	        String endDate
	) throws ParseException {

	    Date fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
	    Date toDate = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);

	    List<Object[]> rows =
	            userDao.getSellerCategoryRawData(fromDate, toDate);

	    Map<String, SellerCategoryReportDto> map = new LinkedHashMap<>();

	    for (Object[] row : rows) {

	        String orgId = (String) row[0];

	        SellerCategoryReportDto dto = map.get(orgId);

	        if (dto == null) {

	            dto = new SellerCategoryReportDto(
	                    (String) row[1], // companyName
	                    (String) row[2], // email
	                    (String) row[3], // mobile
	                    (String) row[4], // seller person
	                    (Date) row[5]    // last login
	            );

	            map.put(orgId, dto);
	        }

	        String category = (String) row[6];

	        if (category != null) {

	            if (dto.getCategory1() == null) {
	                dto.setCategory1(category);

	            } else if (dto.getCategory2() == null) {
	                dto.setCategory2(category);

	            } else if (dto.getCategory3() == null) {
	                dto.setCategory3(category);

	            } else if (dto.getCategory4() == null) {
	                dto.setCategory4(category);

	            } else if (dto.getCategory5() == null) {
	                dto.setCategory5(category);
	            }
	        }
	    }

	    return new ArrayList<>(map.values());
	}

	@Override
	public List<?> getBuyerReports(String startDate, String endDate, 
	                                String requestType) throws ParseException {
		logger.info("Entering into buyerReports Service...");
		logger.info("RequestType : {}",requestType);

	    Date fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    Date toDate = sdf.parse(endDate + " 23:59:59");
	    // Buyer OrgType
	    OrgType orgTypeObject = orgTypeDao.findByTypeName(ApplicationConstants.CLIENT);

	    if ("buyerReport".equalsIgnoreCase(requestType)) {
	    	logger.info("Inside BuyerReport...");
	        return rfqDao.getBuyerReport(fromDate, toDate);  
	    }

	    if ("buyerSummary".equalsIgnoreCase(requestType)) {
	    	logger.info("Inside Buyer Summary...");
	        return orgDao.getBuyerSummary(orgTypeObject, fromDate, toDate); 
	    }

	    if ("buyerCategoryReport".equalsIgnoreCase(requestType)) {
	    	logger.info("Inside Buyer Category...");
	        return getBuyerCategoryReport(startDate, endDate); 
	    }

	    throw new RuntimeException("Invalid requestType");
	}

	// ── Category Report logic (Buyer) ──
	public List<BuyerCategoryReportDto> getBuyerCategoryReport(
	        String startDate, String endDate) throws ParseException {

	    Date fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
	    Date toDate   = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);

	    List<Object[]> rows = userDao.getBuyerCategoryRawData(fromDate, toDate);

	    Map<String, BuyerCategoryReportDto> map = new LinkedHashMap<>();

	    for (Object[] row : rows) {

	        String orgId = (String) row[0];   // o.id

	        BuyerCategoryReportDto dto = map.get(orgId);

	        if (dto == null) {
	            dto = new BuyerCategoryReportDto(
	                (String) row[1],   // u.fullName      → buyerName
	                (String) row[2],   // u.username      → emailId
	                (String) row[3],   // u.phone         → mobileNo
	                (String) row[4],   // o.companyName   → companyName
	                (String) row[5],   // o.city          → companyLocation
	                (Date)   row[6]    // u.activityTs    → lastLoginDate
	            );
	            map.put(orgId, dto);
	        }

	        String category = (String) row[7];  // oc.category

	        if (category != null) {
	            if      (dto.getCategory1()  == null) dto.setCategory1(category);
	            else if (dto.getCategory2()  == null) dto.setCategory2(category);
	            else if (dto.getCategory3()  == null) dto.setCategory3(category);
	            else if (dto.getCategory4()  == null) dto.setCategory4(category);
	            else if (dto.getCategory5()  == null) dto.setCategory5(category);
	            else if (dto.getCategory6()  == null) dto.setCategory6(category);
	            else if (dto.getCategory7()  == null) dto.setCategory7(category);
	            else if (dto.getCategory8()  == null) dto.setCategory8(category);
	            else if (dto.getCategory9()  == null) dto.setCategory9(category);
	            else if (dto.getCategory10() == null) dto.setCategory10(category);
	        }
	    }

	    return new ArrayList<>(map.values());
	}
}