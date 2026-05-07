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

import com.portal.procucev.Dto.SellerCategoryReportDto;
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
}