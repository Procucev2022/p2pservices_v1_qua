package com.portal.procucev.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.portal.procucev.dao.GmtRfqVendorDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.OrgTypeDao;
import com.portal.procucev.dao.RfqVendorDao;
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
	       // return userDao.getSellerCategoryReport(startDate, endDate);
	    }

	    throw new RuntimeException("Invalid requestType");
	}
}
