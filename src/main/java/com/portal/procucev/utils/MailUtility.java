package com.portal.procucev.utils;

import org.springframework.mail.javamail.JavaMailSender;

import com.portal.procucev.model.Organization;

import jakarta.mail.internet.InternetAddress;

public class MailUtility {

	public static void emailForVendor(String string, String email, JavaMailSender javaMailSender, InternetAddress add,
			String host) {
		// TODO Auto-generated method stub
		
	}

	public static void sendVendorEmailForCM2(String string, String toAddress, Organization organization,
			JavaMailSender javaMailSender, InternetAddress add, String host) {
		// TODO Auto-generated method stub
		
	}

	public static void sendOtpForEmail(String string, String email, JavaMailSender javaMailSender, InternetAddress add,
			String host, String otp) {
		// TODO Auto-generated method stub
		
	}

	public static void sendClientEmailForCM2(String string, String toAddress, Organization organization,
			JavaMailSender javaMailSender, InternetAddress add, String host) {
		// TODO Auto-generated method stub
		
	}

}
