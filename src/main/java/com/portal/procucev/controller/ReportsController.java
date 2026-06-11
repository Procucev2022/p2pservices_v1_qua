package com.portal.procucev.controller;

import java.text.ParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.portal.procucev.service.ReportsService;

@CrossOrigin
@RestController
@RequestMapping("/rest/reports")
public class ReportsController {
	
	@Autowired
	ReportsService reportsService;
	
	@GetMapping("/seller-report")
	public ResponseEntity<?> getSellerReports(
	        @RequestParam String startDate,
	        @RequestParam String endDate,
	        @RequestParam String requestType) throws ParseException {

	    return ResponseEntity.ok(
	            reportsService.getSellerReports(startDate, endDate, requestType)
	    );
	}
	
	@GetMapping("/buyer-report")
	public ResponseEntity<?> getBuyerReports(
	        @RequestParam String startDate,
	        @RequestParam String endDate,
	        @RequestParam String requestType) throws ParseException {

	    return ResponseEntity.ok(
	            reportsService.getBuyerReports(startDate, endDate, requestType)
	    );
	}
	
	@GetMapping("/rfq-report")
	public ResponseEntity<?> getRfqReports(
	        @RequestParam String startDate,
	        @RequestParam String endDate,
	        @RequestParam String requestType) throws ParseException {

	    return ResponseEntity.ok(
	            reportsService.getRfqReports(startDate, endDate, requestType)
	    );
	}
	
	

}

