package com.portal.procucev.service;

import java.text.ParseException;
import java.util.List;

public interface ReportsService {

	public List<?> getSellerReports(String startDate, String endDate, String requestType) throws ParseException;

	public List<?> getBuyerReports(String startDate, String endDate, String requestType)  throws ParseException;

}
