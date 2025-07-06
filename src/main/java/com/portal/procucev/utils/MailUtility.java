package com.portal.procucev.utils;

import org.springframework.mail.javamail.JavaMailSender;

import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.User;

import jakarta.mail.Message;
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

	public static void mailingGMTClientRFQMailToinfoTeam(String string, String toEmail, JavaMailSender javaMailSender,
			InternetAddress add, String host, User user) {
		// TODO Auto-generated method stub
		
	}

	public static void emailNewGMTRfqForNoPR(String string, JavaMailSender javaMailSender, Rfq rfq, String host,
			String email, String otherEmails, String mailFom, String emailPassword, String rfqDueDate) {
		// TODO Auto-generated method stub
		
	}

	public static void emailNewRfqForNoPR(String string, JavaMailSender javaMailSender, Rfq rfqData, String host,
			String email, String username, String otherEmails, String phoneNumber, String rfqDueDate, String fullName,
			String string2, String string3) {
		// TODO Auto-generated method stub
		
	}

	public static void emailforgotpassword(String string, String username, JavaMailSender javaMailSender,
			InternetAddress add, String password, String host, User user) {
		// TODO Auto-generated method stub
		
	}

	public static void forwardMessage(String forwardAddress, JavaMailSender javaMailSender, String mailFom,
			Message message) {
		// TODO Auto-generated method stub
		
	}

}
