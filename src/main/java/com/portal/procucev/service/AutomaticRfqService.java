package com.portal.procucev.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.portal.procucev.model.Rfq;

@Service
public interface AutomaticRfqService {
	
	
	boolean raiseRfq(Rfq rfq);
	Map<String,String> validateEmail(String email);

}
