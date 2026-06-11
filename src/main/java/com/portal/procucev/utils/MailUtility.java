package com.portal.procucev.utils;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.CollectionUtils;

import com.portal.procucev.customexception.AppException;
import com.portal.procucev.model.BFSUsers;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.RFQDocument;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.model.User;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;



public class MailUtility {
	

	@Value("${email.subject.prefix}")
	private String subjectPrefix;

	static final Logger LOGGER = LoggerFactory.getLogger(MailUtility.class);

	public static void emailVendorApprovedStatus(String type, String toAddress, JavaMailSender javaMailSender,
			InternetAddress add, String host, User user) {

		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n"
				+ "<b>Your Registration has been Approved Successfully. Please login to Procucev Portal.</b><br><br>\n"
				+ "\n" + "<p><a href=\"" + host + "/login?regId=" + user.getOrg().getId() + "\">Click Here!!</a></p>"
				+ "<b>to login into Procucev Portal </b>" + "\n"

				+ "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Solutions</b>\n" + "\n" + "\n" + "</body>\n"
				+ "</html>";
		sendEmailMessage(toAddress, add, javaMailSender, message, type);
	}

	public static boolean sendEmailMessage(String toAddress, InternetAddress add, JavaMailSender javaMailSender,
			String message, String type) {

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(toAddress);
			mimeMessageHelper.setFrom(add); // from Address
			mimeMessageHelper.setSubject("Procucev Portal Account Approved !!");
			mimeMessageHelper.setText(message, true);
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sent mail successfully to " + toAddress + "Message Called from " + type);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail - >" + toAddress + "Message Called for " + type);

		}

		return false;
	}

	public static boolean sendEmailMessageforReject(String toAddress, String fromAddress, JavaMailSender javaMailSender,
			String message, String type) {

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(toAddress);
			mimeMessageHelper.setFrom(fromAddress); // from Address
			mimeMessageHelper.setSubject("Procucev Portal Account Rejected !!");
			mimeMessageHelper.setText(message, true);
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sent mail successfully to " + toAddress + "Message Called from " + type);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail - >" + toAddress + "Message Called for " + type);

		}

		return false;
	}

	public static void emailVendorRejectedStatus(String type, String username, JavaMailSender javaMailSender,
			String mailFom, String host, User user) {
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n"
				+ "<b>Your Registration has been Rejected. Please contact Procucev for further details.</b><br><br>\n"
				+ "\n" + "<p><a href=\"" + host + "/login?regId=" + user.getOrg().getId() + "\">Click Here!!</a></p>"
				+ "<b>to login into Procucev Portal </b>" + "\n"

				+ "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Admin</b>\n" + "\n" + "\n" + "</body>\n" + "</html>";
		sendEmailMessageforReject(username, mailFom, javaMailSender, message, type);

	}

	public static boolean sendEmailMessageforNewPR(String toAddress, InternetAddress add, JavaMailSender javaMailSender,
			String message, String type) {

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(toAddress);
			mimeMessageHelper.setFrom(add); // from Address
			mimeMessageHelper.setSubject("New Procurement Request!!");
			mimeMessageHelper.setText(message, true);
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sent mail successfully to " + toAddress + "Message Called from " + type);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail - >" + toAddress + "Message Called for " + type);

		}

		return false;
	}

	public static boolean sendemailForNewRfq(String toAddress, InternetAddress add, JavaMailSender javaMailSender,
			String message, String type) {

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(toAddress);
			mimeMessageHelper.setFrom(add); // from Address
			mimeMessageHelper.setSubject("New RFQ!!");
			mimeMessageHelper.setText(message, true);
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sent mail successfully to " + toAddress + "Message Called from " + type);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail - >" + toAddress + "Message Called for " + type);

		}

		return false;
	}

	public static boolean rfqAccept(String toAddress, InternetAddress add, JavaMailSender javaMailSender,
			String message, String type) {

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(toAddress);
			mimeMessageHelper.setFrom(add); // from Address
			mimeMessageHelper.setSubject("RFQ Accepted!!");
			mimeMessageHelper.setText(message, true);
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sent mail successfully to " + toAddress + "Message Called from " + type);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail - >" + toAddress + "Message Called for " + type);

		}

		return false;
	}

	public static void emailrfqAccept(String type, String username, JavaMailSender javaMailSender, InternetAddress add,
			String rfqid, String host, User user) {
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "partner" + ", <br><br>\n"
				+ "\n" + "<b> Your RFQ With RfqID </b>" + rfqid + "<b> got Accepted </b>" + "<br><br>\n" + "\n"
				+ "<p><a href=\"" + host + "/login?regId=" + user.getOrg().getId() + "\">Click Here!!</a></p>\n"
				+ "<b>to login into Procucev Portal </b>" + "\n"

				+ "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Solutions</b>\n" + "\n" + "\n" + "</body>\n"
				+ "</html>";
		rfqAccept(username, add, javaMailSender, message, type);

	}

	public static boolean eamilfornewVendor(String toAddress, InternetAddress add, JavaMailSender javaMailSender,
			String message, String type) {
		LOGGER.info("Entered To send Email");
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(toAddress);
			mimeMessageHelper.setFrom(add); // from Address
			mimeMessageHelper.setSubject("New Vendor!!");
			mimeMessageHelper.setText(message, true);
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sent mail successfully to " + toAddress + "Message Called from " + type);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail - >" + toAddress + "Message Called for " + type);

		}

		return false;
	}

	public static void emailnewVendor(String type, String username, JavaMailSender javaMailSender, InternetAddress add,
			String vendorid, String host, User user) {
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b> You have received  Vendor with VendorId</b>" + vendorid + "<br><br>\n" + "\n"
				+ "<p><a href=\"" + host + "/login?regId=" + user.getOrg().getId() + "\">Click Here!!</a></p>\n"
				+ "<b>to login into Procucev Portal </b>" + "\n" + "<b>Thanks, <br></b>\n" + "\n"
				+ "<b>Procucev Solutions</b>\n" + "\n" + "\n" + "</body>\n" + "</html>";
		eamilfornewVendor(username, add, javaMailSender, message, type);

	}

	public static boolean emailforforgotpassword(String toAddress, InternetAddress add, JavaMailSender javaMailSender,
			String message, String type) {

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(toAddress);
			mimeMessageHelper.setFrom(add); // from Address
			mimeMessageHelper.setSubject("Procucev QUA AI – Your New Password");
			mimeMessageHelper.setText(message, true);
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sent mail successfully to " + toAddress + "Message Called from " + type);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail - >" + toAddress + "Message Called for " + type);

		}

		return false;
	}

	public static void emailforgotpassword(String type, String username, JavaMailSender javaMailSender,
	        InternetAddress add, String password, String host, User user) {

	    String message = "<!DOCTYPE html>"
	            + "<html>"
	            + "<body>"
	            + "<p>Dear Partner,</p>"

	            + "<p>Your login credentials have been updated. Use the following details to access your "
	            + "<b>Procucev QUA AI seller account</b>:</p>"

	            + "<p><b>Username:</b> " + username + "<br>"
	            + "<b>Mobile:</b> " + user.getPhone() + "<br>"
	            + "<b>One Time Password:</b> " + password + "</p>"

	            + "<p>(Use this password to log in for the first time and create your new password.)</p>"

	            + "<p><a href=\"" + host + "/login?regId=" + user.getOrg().getId() + "\">Click Here</a> "
	            + "to log in and update your password.</p>"

	            + "<p>All set for you. Just login now. "
	            + "If you still face any problem please drop a mail to "
	            + "<a href='mailto:support@procucev.com'>support@procucev.com</a>.</p>"

	            + "<br>"
	            + "<p>Thanks,<br>"
	            + "Team QUA AI</p>"

	            + "</body>"
	            + "</html>";

	    emailforforgotpassword(username, add, javaMailSender, message, type);
	}

	public static void mailingVerificationLinkWithUserLogin(JavaMailSender javaMailSender, String from,
			InternetAddress add, String pswd, String hostName, User user) {

		System.out.println("pswdd mail---" + user.getPassword());
		String verificationTemplate = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner"
				+ ", <br><br>\n" + "\n"
				+ "<b>Please click the below link to create your account at Procucev Portal.</b>\n" + "\n"
				+ "<p><a href=\"" + hostName + "/login" + "\">Create your account !!</a></p>\n"
				+ "<b>With login credentials below</b>\n" + ",<br><br>\n" + "<b>UserName : " + user.getUsername()
				+ ",<br><br></b>\n" + "\n" + "<b>Password :" + user.getPassword() + "<br><br></b>\n" + "\n"

				+ "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Admin</b>\n" + "\n" + "\n" + "</body>\n" + "</html>";

		System.out.println(verificationTemplate);
		// http://localhost:4201/vendorRegistration?regId=r123
		JavaMailSender javaMailSender2 = getJavaMailSender(from, pswd);
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(user.getUsername());
			mimeMessageHelper.setFrom(add); // from Address
			mimeMessageHelper.setSubject("Account Creation !!");
			mimeMessageHelper.setText(verificationTemplate, true);
			// javaMailSender.send(mimeMessage);
			javaMailSender2.send(mimeMessage);
		} catch (Exception e) {
			LOGGER.error("Error in sending creation mail -- " + e.getMessage());
			throw new AppException(StatusCodes.MAIL_SEND_ERROR, ApplicationConstants.MAIL_SENDING_FAILURE,
					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
		}
		LOGGER.info("Sent verifiation mail successfully");
	}

	public static void cancelAuctionInvitation(String type, String username, JavaMailSender javaMailSender,
			String mailFom, String auctionId, String host, User user) {
		// TODO Auto-generated method stub
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b> Your Auction with AuctionID</b>" + auctionId + "<b>got cancelled</b>" + "<br><br>\n"
				+ "\n" + "<p><a href=\"" + host + "/login?regId=" + user.getOrg().getId() + "\">Click Here!!</a></p>\n"
				+ "<b>to login into Procucev Portal </b>" + "\n" + "<b>Thanks, <br></b>\n" + "\n"
				+ "<b>Procucev Admin</b>\n" + "\n" + "\n" + "</body>\n" + "</html>";
		emailforcancelAuctionInvitation(username, mailFom, javaMailSender, message, type);

	}

	public static boolean emailforcancelAuctionInvitation(String toAddress, String fromAddress,
			JavaMailSender javaMailSender, String message, String type) {

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(toAddress);
			mimeMessageHelper.setFrom(fromAddress); // from Address
			mimeMessageHelper.setSubject("Cancel Auction!!");
			mimeMessageHelper.setText(message, true);
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sent mail successfully to " + toAddress + "Message Called from " + type);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail - >" + toAddress + "Message Called for " + type);

		}

		return false;
	}

	public static boolean sendemail(String toAddress, InternetAddress add, JavaMailSender javaMailSender,
			String message, String type, String subject) {

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(toAddress);
			mimeMessageHelper.setFrom(add); // from Address
			mimeMessageHelper.setSubject(subject);
			mimeMessageHelper.setText(message, true);
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sent mail successfully to " + toAddress + "Message Called from " + type);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Error in sending mail - >" + toAddress + "Message Called for " + type);

		}

		return false;
	}

	/**
	 * Generic method which accepts below params and sends email to all list of
	 * users
	 * 
	 * @param subject
	 * @param toAddress
	 * @param from
	 * @param javaMailSender
	 * @param message
	 * @param type
	 * @return
	 */
	public static boolean emailNotifierGenericBySenderList(String subject, List<String> toAddress, InternetAddress from,
			JavaMailSender javaMailSender, String message, String type) {

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(toAddress.toArray(new String[0]));
			mimeMessageHelper.setFrom(from); // from Address
			mimeMessageHelper.setSubject(subject);
			mimeMessageHelper.setText(message, true);
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sent mail successfully to " + toAddress + "Message Called from " + type);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail --> " + subject + "- >" + toAddress + "Message Called for " + type);

		}
		return false;
	}

	/**
	 * Generic method which sends email with given below params to provided user
	 * email address
	 * 
	 * @param subject
	 * @param toAddress
	 * @param from
	 * @param javaMailSender
	 * @param message
	 * @param type
	 * @return
	 */
	public static boolean emailNotifierGenericBySender(String subject, String toAddress, InternetAddress from,
			JavaMailSender javaMailSender, String message, String type) {

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(toAddress);
			mimeMessageHelper.setFrom(from); // from Address
			mimeMessageHelper.setSubject(subject);
			mimeMessageHelper.setText(message, true);
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sent mail successfully to " + toAddress + "Message Called from " + type);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail --> " + subject + "- >" + toAddress + "Message Called for " + type);

		}
		return false;
	}

	public static void notifyClientForNewComment(String type, List<String> userlist, JavaMailSender javaMailSender,
			InternetAddress fromAddress, String string2, String host, String deliveryId) {
		String subject = "New Delivery Comment from Vendor ";
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b>Your have a New Delivery Comment by Vendor for Delivery ID </b>" + deliveryId
				+ "<br><br>\n" + "\n" + "<p><a href=\"" + host + "/login\">Click Here!!</a></p>\n"
				+ "<b>to login into Procucev Portal </b>" + "<br><br>\n" + "\n" + "<b>Thanks, <br></b>\n" + "\n"
				+ "<b>Procucev Solutions</b>\n" + "\n" + "\n" + "</body>\n" + "</html>";
		emailNotifierGenericBySenderList(subject, userlist, fromAddress, javaMailSender, message, type);

	}

	public static void notifyVendorForNewComment(String type, List<String> userlist, JavaMailSender javaMailSender,
			InternetAddress fromAddress, String id, String host, String deliveryId) {

		String subject = "New Delivery Comment from Client ";
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b>Your have a  New Delivery Comment by Client for Delivery ID </b>" + deliveryId
				+ "<br><br>\n" + "\n" + "<p><a href=\"" + host + "/login\">Click Here!!</a></p>\n"
				+ "<b>to login into Procucev Portal </b>" + "<br><br>\n" + "\n" + "<b>Thanks, <br></b>\n" + "\n"
				+ "<b>Procucev Solutions</b>\n" + "\n" + "\n" + "</body>\n" + "</html>";
		emailNotifierGenericBySenderList(subject, userlist, fromAddress, javaMailSender, message, type);
	}

	public static void emailVendorDBDownloadByVendorExc(String type, String username, JavaMailSender javaMailSender,
			InternetAddress add, String host) {
		// TODO Auto-generated method stub
		String subject = "Vendor DataBase Notification ";
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b>Vendor Executive have downloaded Vendor DB Records </b>" + "<br><br>\n" + "\n"
				+ "<p><a href=\"" + host + "/login\">Click Here!!</a></p>\n" + "<b>to login into Procucev Portal </b>"
				+ "<br><br>\n" + "\n" + "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Solutions</b>\n" + "\n" + "\n"
				+ "</body>\n" + "</html>";
		emailNotifierGenericBySender(subject, username, add, javaMailSender, message, type);
	}

	public static void reminderMailWithUserCred(String type, String username, JavaMailSender javaMailSender,
			InternetAddress add, String host, User userData) {
		// TODO Auto-generated method stub
		String subject = "Vendor Registration Reminder ";
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "We welcome you to partner with Procucev!!!!" + "<br><br>\n" + "\n"
				+ "<b>By registering with us, you get to explore a world of possibilities, in terms of better customer reach, business continuity and potential demand visibility. Rest assured of healthy margins and watch your business volumes grow by leaps and bounds."
				+ "<br><br>\n" + "\n\"+" + "All of the above, without any commissions." + "<br><br>\n" + "\n\"+"
				+ "</b> Please find below login details to register your company. This link will be disable automatically after 5 days if you don't login.</b>\n"
				+ ", <br><br>\n" + "\n" + "<p><a href=\"" + host + "/login?regId=" + userData.getOrg().getId()
				+ "\">Create your account !!</a></p>\n" + "<b>Use the login details mentioned below to proceed:</b>\n"
				+ ",<br><br>\n" + "<b>UserName " + userData.getUsername() + ",<br><br></b>\n" + "\n" + "<b>Password "
				+ userData.getPassword() + "<br><br>\n" + "\n"
				+ "<b>In order to continue to receive RFQs and submit quotations for multiple enquiries, you have to register your firm within 90 days.</b>\n"
				+ ", <br><br>\n" + "\n" + "<b>Please connect with your Partner Associate for more details.</b>\n"
				+ "<br><br></b>\n"

				+ "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Solutions</b>\n" + "\n" + "\n" + "</body>\n"
				+ "</html>";

		emailNotifierGenericBySender(subject, username, add, javaMailSender, message, type);

	}

	public static void emailSelfRegisterVendor(String type, List<String> ids, JavaMailSender javaMailSender,
			InternetAddress add, String companyName, String host, Object object) {
		// TODO Auto-generated method stub
		String subject = "Self Register Vendor Notification ";
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b>You have received self registration by </b>" + companyName
				+ "<b>vendor for acceptance </b>" + "<br><br>\n" + "\n" + "<b>Thanks, <br></b>\n" + "\n"
				+ "<b>Procucev Solutions</b>\n" + "\n" + "\n" + "</body>\n" + "</html>";
		emailNotifierGenericBySenderList(subject, ids, add, javaMailSender, message, type);

	}

	public static void emailSelfRegisterVendorAcceptance(String type, List<String> ids, JavaMailSender javaMailSender,
			InternetAddress add, List<String> companyNames, String host, Object object) {
		// TODO Auto-generated method stub

		String subject = "Self Register Vendor Notification ";
		String message; // TODO Auto-generated method stub // TODO Auto-generated
		int i = 1;
		StringBuilder email = new StringBuilder();
		email.append("<html>");
		email.append("<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n" + "\n"
				+ "<b> You have reciceved self registered vendors for acceptance</b>" + "<br><br>\n"
				+ "<table style='border:2px solid black'>");

		email.append("<style>" + "table, th, td" + "border: 1px solid black;" + "  border-collapse: collapse;" + "}"
				+ "</style>");
		email.append("<tr>");
		email.append("<th>");
		email.append("S.NO");
		email.append("</th>");
		email.append("<th>");
		email.append("VendorName");
		email.append("</th>");
		email.append("</tr>");

		for (String name : companyNames) {

			email.append("<tr>");
			email.append("<td>");
			email.append(i++);
			email.append("</td>");
			email.append("<td> ");
			email.append(name);
			email.append("</td>");
			email.append("<td>");

			email.append("</tr>");
		}
		email.append("</table></body></html>");
		email.append("<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Admin</b>\n" + "\n" + "\n");

		message = email.toString();
		emailNotifierGenericBySenderList(subject, ids, add, javaMailSender, message, type);

	}

	public static void emaildailyRFQStatus(String type, List<String> ids, JavaMailSender javaMailSender,
			InternetAddress add, List<Rfq> rfqs, String host) {
		// TODO Auto-generated method stub

		String subject = "Pending RFQ's  Notification ";
		String message; // TODO Auto-generated method stub // TODO Auto-generated
		int i = 1;
		StringBuilder email = new StringBuilder();
		email.append("<html>");
		email.append("<head>");
		email.append("<style>" + "table, th, td" + "{" + "border: 1px solid black;" + " border-collapse: collapse;"
				+ "}" + "</style>");
		email.append("</head>");
		email.append("<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n" + "\n"
				+ "<b> Your RFQ'S with New and Accepted Status are below please accept and sumbit quotation </b>"
				+ "<br><br>\n" + "<table style='border:2px solid black'>");
		email.append("<tr>");
		email.append("<th>");
		email.append("S.NO");
		email.append("</th>");
		email.append("<th>");
		email.append("RFQID");
		email.append("</th>");
		email.append("<th>");
		email.append("Item Description");
		email.append("</th>");
		email.append("</tr>");

		if (!CollectionUtils.isEmpty(rfqs)) {
			for (Rfq rfq : rfqs) {
				email.append("<tr>");
				email.append("<td>");
				email.append(i++);
				email.append("</td>");
				email.append("<td> ");
				email.append(rfq.getRfqId());
				email.append("</td>");
				email.append("<td> ");
				if (!CollectionUtils.isEmpty(rfq.getRfqItem())) {
					email.append(rfq.getRfqItem().get(0).getDescription());
				}

				email.append("</td>");
				email.append("<td>");

				email.append("</tr>");
			}
		}
		email.append("</table></body></html>");
		email.append("<p><a href=\"" + host + "/login\">Click Here!!</a></p>\n"
				+ "<b>to login into Procucev Portal. </b>" + "\n" + "<b>For any queries please contact </b>" + "<br>\n"
				+ "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Solutions</b>\n" + "\n" + "\n");

		message = email.toString();
		emailNotifierGenericBySenderList(subject, ids, add, javaMailSender, message, type);

	}

	public static void emailNewRfq(String type, JavaMailSender javaMailSender, InternetAddress add, String rfqid,
			String host, User user, Rfq rfq, String username, String phone, String ccAddress) {
		String subject = "New RFQ!! ";
//		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
////				+ "\n" + "<b>You have received an RFQ with RFQID </b>" + rfqid + "<br><br>\n" + "\n" + "<p><a href=\""
//			+ host + "/login?regId=" + users.get(0).getOrg().getId() + "\">Click Here!!</a></p>\n"
//			+ "<b>to login into Procucev Portal </b>" + "\n" + "<b>Thanks, <br></b>\n" + "\n"
//			+ "<b>Procucev Solutions</b>\n" + "\n" + "\n" + "</body>\n" + "</html>";
//		
		String message; // TODO Auto-generated method stub // TODO Auto-generated
		int i = 1;
		StringBuilder email = new StringBuilder();
		email.append("<html>");
		email.append("<head>");
		email.append("<style>" + "table, th, td" + "{" + "border: 1px solid black;" + " border-collapse: collapse;"
				+ "}" + "</style>");
		email.append("</head>");
		email.append("<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n" + "\n"
				+ "<b> You have received an RFQ with RFQID </b>" + rfq.getRfqId() + "<br><br>\n"
				+ "<table style='border:2px solid black'>");
		email.append("<tr>");
		email.append("<th>");
		email.append("S.NO");
		email.append("</th>");
		email.append("<th>");
		email.append("Item Description");
		email.append("</th>");
		email.append("<th>");
		email.append("Specification");
		email.append("</th>");
		email.append("<th>");
		email.append("Qty");
		email.append("</th>");
		email.append("<th>");
		email.append("UOM");
		email.append("</th>");
		email.append("</tr>");

		if (!CollectionUtils.isEmpty(rfq.getRfqItem())) {
			for (RfqItem items : rfq.getRfqItem()) {

				email.append("<tr>");
				email.append("<td>");
				email.append(items.getSerialNo());
				email.append("</td>");
				email.append("<td> ");
				email.append(items.getDescription());
				email.append("</td>");
				email.append("<td> ");
				email.append(items.getBrand());
				email.append("</td>");
				email.append("<td> ");
				email.append(items.getQuantity());
				email.append("</td>");
				email.append("<td> ");
				email.append(items.getUnitofMeasures());
				email.append("</td>");
				email.append("<td>");

				email.append("</tr>");
			}
		}

		email.append("</table></body></html>");
		// email.append("<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Admin</b>\n" +
		// "\n" + "\n");
		email.append("<p><a href=\"" + host + "/login?regId=" + user.getOrg().getId() + "\">Click Here!!</a></p>\n"
				+ "<b>to login into Procucev Portal. </b>" + "\n" + "<b>For any queries please contact </b>" + phone
				+ "<br>\n" + "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Solutions</b>\n" + "\n" + "\n");

		message = email.toString();
		emailNotifierRFQ(subject, user, add, javaMailSender, message, type, username, ccAddress);

		// sendemailForNewRfq(username, add, javaMailSender, message, type);

	}

	private static boolean emailNotifierRFQ(String subject, User user, InternetAddress from,
			JavaMailSender javaMailSender, String message, String type, String username, String ccAddress) {
		// TODO Auto-generated method stub
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
		try {
			mimeMessageHelper.setTo(user.getUsername());
			mimeMessageHelper.setFrom(from); // from Address
			// mimeMessageHelper.setCc(username);
			mimeMessageHelper.setSubject(subject);
			mimeMessageHelper.setText(message, true);
			if (ccAddress != null && !ccAddress.isEmpty()) {
				mimeMessageHelper.setCc(ccAddress);
			}
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sent mail successfully to " + user + "Message Called from " + type);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail --> " + subject + "- >" + user + "Message Called for " + type);

		}
		return false;
	}

	public  static boolean emailNewRfqForNoPR(String subjectPrefix, String string, JavaMailSender javaMailSender, Rfq rfqData, String host,
			String mailId, String fromAddress, String ccAdd, String phonenumber, String rfqDueDate, String fullName,
			String mailId2, String password, String vendorId,String vendorMobileNumber) throws MessagingException {
		LOGGER.info("Entered To Send Email To Vendor Regarding RFQ");
		try {
			String subject =  subjectPrefix + " You have an Enquiry RFQ No " + rfqData.getRfqId() + " - " + vendorId;
			String message;
			String pincode = null;
			int i = 1;
			String address = null;
			StringBuilder email = new StringBuilder();
			if (!CollectionUtils.isEmpty(rfqData.getClientdeliverylocationrfq())) {
				address = rfqData.getClientdeliverylocationrfq().get(0).getCity();
				pincode = rfqData.getClientdeliverylocationrfq().get(0).getPincode();
			}
			email.append("<html>");
			email.append("<head>");
			email.append("<style>" + "table {" + "   border: 2px solid black;" + "   border-collapse: collapse;" + "}"
					+ "th, td {" + "   border: 1px solid black;" + "   padding: 8px;" + "}" + "</style>");
			email.append("</head>");
			email.append("<body>\n\n");
			email.append("Dear Partner,<br><br>\n\n");
			email.append(
				    "<b>We have received a new RFQ (Enquiry) through GMT. "
				    + "Please check the details below and send your quotation by replying to this email. "
				    + "Please do not change the subject line while replying.</b><br><br>\n\n"
				);
			
			
			email.append("Please login now to your QUA seller account at ");
			email.append("<a href=\"https://qua.procucev.com/login\">https://qua.procucev.com/login</a> ");
			email.append("to view full details and download the RFQ instantly. ");
			email.append("This is a live enquiry, do not miss it.<br><br>");
			
			email.append("<b>Your login details:</b><br>");
			email.append("Username: <b>" + mailId + "</b><br>");
			email.append("Mobile: <b>" + vendorMobileNumber + "</b><br>");
			email.append("Password: <b>" + "Welcome@123" + "</b><br>");
			email.append("(Use this password to log in for the first time to create your new password.)<br><br>");	
			
			email.append("Rfq Due Date: " + rfqDueDate + "</b><br><br>\n\n");
			email.append("<b>Project Description/Reference: " + rfqData.getProjectDesc() + "</b><br><br>\n\n");
			email.append("<b>Please find the below RFQ details: </b><br><br>\n\n");
			email.append("<table>");
			email.append("<tr>");
			email.append("<th>");
			email.append("S.NO");
			email.append("</th>");
			email.append("<th>");
			email.append("RFQID");
			email.append("</th>");
			email.append("<th>");
			email.append("Item Description");
			email.append("</th>");
			email.append("<th>");
			email.append("Specification");
			email.append("</th>");
			email.append("<th>");
			email.append("UOM");
			email.append("</th>");
			email.append("<th>");
			email.append("Quantity");
			email.append("</th>");
			email.append("<th>");
			email.append("Remarks");
			email.append("</th>");
			email.append("</tr>");

			// Iterate through each RfqItem in the list
			for (RfqItem item : rfqData.getRfqItem()) {
				email.append("<tr>");
				email.append("<td style='border: 1px solid black;'>");
				email.append(i++);
				email.append("</td>");
				email.append("<td style='border: 1px solid black;'> ");
				email.append(rfqData.getRfqId());
				email.append("</td>");
				email.append("<td style='border: 1px solid black;'> ");
				if (!CollectionUtils.isEmpty(rfqData.getRfqItem())) {
					email.append(item.getDescription());
				}
				email.append("</td>");

				// Include additional fields from RfqItem
				email.append("<td style='border: 1px solid black;'>");
				email.append(item.getBrand());
				email.append("</td>");
				email.append("<td style='border: 1px solid black;'>");
				email.append(item.getUnitofMeasures());
				email.append("</td>");
				email.append("<td style='border: 1px solid black;'>");
				email.append(item.getQuantity());
				email.append("</td>");
				email.append("<td style='border: 1px solid black;'>");
				email.append(item.getRemarks());
				email.append("</td>");
				email.append("</tr>");
			}
			email.append("</table>"); // Close the table
			email.append("<br><br>");
			email.append("<br><br>");
			// Assuming email is a StringBuilder or similar
			email.append("Delivery Address: " + address + "<br>");
			email.append("Pincode : " + pincode + "<br>");
			email.append("<b>Delivery Date:" + rfqData.getDeliveryDate() + "</b><br><br>\n\n");
			email.append("Please submit your offer on time to increase your chances of getting the order and connecting directly with the B2B client.<br><br>");

			email.append("To receive more RFQs, please update your relevant product categories in the QUA portal. "
			        + "Correct categories help you get more business opportunities.<br><br>");

			email.append("You can also check RFQs regularly on "
			        + "<a href=\"https://www.procucev.com\">www.procucev.com</a> "
			        + "- Request New RFQ, Check Status and more…<br><br>");

			email.append("<b>Thanks,</b><br>");
			email.append(fullName);
			email.append("<br><br>");
			email.append("Team GMT<br>");
			email.append("Procucev");
//			if (phonenumber != null) {
//				email.append("T: +91" + phonenumber);
//			}
//			email.append("<br>");
//			email.append("A: "
//					+ "302,1st Floor,Sharda,<br>Above Axis Bank,<br>ACES Layout,Kundalahalli,<br>Bengaluru,Karnataka");
			email.append("<br><br>");

			List<RFQDocument> documentList = rfqData.getRfqDocument();
			MimeMultipart multipart = new MimeMultipart();

			// Add the text content of the email to a MimeBodyPart
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(email.toString(), "text/html");
			multipart.addBodyPart(messageBodyPart);

			// Add attachments to the MimeMultipart
			if (!CollectionUtils.isEmpty(rfqData.getRfqDocument())) {
				email.append("<b>List of Documents:</b><br>");
				for (RFQDocument document : rfqData.getRfqDocument()) {
					byte[] file = document.getFile();
					ByteArrayDataSource dataSource = new ByteArrayDataSource(file, "application/octet-stream");
					MimeBodyPart attachmentPart = new MimeBodyPart();
					attachmentPart.setDataHandler(new DataHandler(dataSource));
					attachmentPart.setFileName(document.getFileName()); // Set the actual file name here
					multipart.addBodyPart(attachmentPart);
				}
			}

			JavaMailSender javaMailSender2 = getJavaMailSender(mailId2, password);
			// Custom from address
			LOGGER.info("Going to emailNotifierGenericNoPRBySenderList() to send email ");
			emailNotifierGenericNoPRBySenderList(subject, mailId, javaMailSender2, fromAddress, ccAdd, multipart,
					mailId2);

			return true;
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public static boolean emailInviteRfq(JavaMailSender javaMailSender, Rfq rfqData, String host, String mailId,
			String fromAddress, String ccAdd, String phonenumber, String fullName, String mailId2, String password,String vendorMobileNumber)
			throws MessagingException {
		LOGGER.info("Entered to send Invite RFQ Email to Vendor");
		try {
			String subject = "You have an enquiry- Please login to your QUA seller account!!";
			StringBuilder email = new StringBuilder();

			email.append("<html><body>");

			email.append("Dear Partner,<br><br>");

			email.append("Greetings from <b>Procucev!</b><br><br>");

			email.append("QUA by <b>Procucev</b> is a trusted AI B2B marketplace connecting genuine buyers and quality sellers across India.<br><br>");

			email.append("We have a new enquiry from a corporate buyer that matches your category. ");
			email.append("This is a verified business opportunity, please check the details below.<br><br>");

			email.append("Please login now to your QUA seller account at ");
			email.append("<a href=\"https://qua.procucev.com/login\">https://qua.procucev.com/login</a> ");
			email.append("to view full details and download the RFQ instantly. ");
			email.append("This is a live enquiry, do not miss it.<br><br>");
			
			email.append("<b>Your login details:</b><br>");
			email.append("Username: <b>" + mailId + "</b><br>");
			email.append("Mobile: <b>" + vendorMobileNumber + "</b><br>");
			email.append("Password: <b>" + "Welcome@123" + "</b><br>");
			email.append("(Use this password to log in for the first time to create your new password.)<br><br>");		
		//	email.append("<b>Enquiry Details:</b><br><br>"); 
// Build RFQ items table
			email.append("<b>Enquiry Details:</b><br><br>");

			// Simplified table with only Sl No, Item Description, Pin code
			email.append("<table style='border:1px solid black;border-collapse:collapse;'>");
			email.append(
			        "<tr>"
			        + "<th style='border:1px solid black;'>Sl No</th>"
			        + "<th style='border:1px solid black;'>Item Description</th>"
			        + "<th style='border:1px solid black;'>Pin code</th>"
			        + "</tr>");

			int i = 1;
			String pincode = "";

			if (!CollectionUtils.isEmpty(rfqData.getClientdeliverylocationrfq())) {
			    pincode = rfqData.getClientdeliverylocationrfq().get(0).getPincode();
			}

			if (!CollectionUtils.isEmpty(rfqData.getRfqItem())) {
			    for (RfqItem item : rfqData.getRfqItem()) {

			        email.append("<tr>");
			        email.append("<td style='border:1px solid black;'>")
			                .append(i++)
			                .append("</td>");

			        email.append("<td style='border:1px solid black;'>")
			                .append(item.getDescription() != null ? item.getDescription() : "")
			                .append("</td>");

			        email.append("<td style='border:1px solid black;'>")
			                .append(pincode != null ? pincode : "")
			                .append("</td>");

			        email.append("</tr>");
			    }
			}

			email.append("</table><br><br>");

			email.append("You can also get real-time enquiry alerts on WhatsApp. ");
			email.append("Just say Hi to <b>7090170801</b> now to receive new enquiries matching your categories as soon as they come in.<br><br>");

			email.append("Please submit your offer on time to increase your chances of getting the order and connecting directly with the B2B client.<br><br>");

			email.append("To receive more RFQs, please update your relevant product categories in the QUA portal. ");
			email.append("Correct categories help you get more business opportunities.<br><br>");

			email.append("You can also check RFQs regularly on ");
			email.append("<a href=\"https://www.procucev.com\">www.procucev.com</a> - Request New RFQ, Check Status and more…<br><br>");

			email.append("<i>This is system generated RFQ invitation and don’t reply to this email.</i><br><br>");

			email.append("<b>Best Regards,</b><br>");
			email.append("<b>Team Procucev</b>");

			email.append("</body></html>");
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(email.toString(), "text/html");

			MimeMultipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			JavaMailSender javaMailSender2 = getJavaMailSender(mailId2, password);
			emailNotifierGenericNoPRBySenderList(subject, mailId, javaMailSender2, fromAddress, ccAdd, multipart,
					mailId2);

			return true;
		} catch (MessagingException e) {
			e.printStackTrace();
			return false;
		}
	}

	private static boolean emailNotifierGenericNoPRBySenderList(String subject, String mailId,
			JavaMailSender javaMailSender, String fromAddress, String ccAdd, MimeMultipart multipart, String mailId2) {
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();

		try {
			MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
			// InternetAddress ia = new InternetAddress(fromAddress);
			String[] ccAddresses = (ccAdd != null) ? new String[] { ccAdd, fromAddress } : new String[] { fromAddress };
//			List<String> ccList = new ArrayList<>();
//	        // Add the fixed CC address
//	        ccList.add("notifications@procucev.com");
//	        // Conditionally add other addresses
//	        if (ccAdd != null) {
//	            ccList.add(ccAdd);
//	        }
//	        // Add the fromAddress
//	        ccList.add(fromAddress);
//	        String[] ccAddresses = ccList.toArray(new String[0]);
			mimeMessageHelper.setTo(mailId);
			mimeMessageHelper.setFrom(mailId2);
			// mimeMessageHelper.setCc(ccAddresses);
			mimeMessageHelper.setSubject(subject);
			mimeMessageHelper.setText("Please find the attachments below.");
			mimeMessageHelper.getMimeMessage().setContent(multipart); // Set the MimeMultipart as the content
			javaMailSender.send(mimeMessage);
			LOGGER.info("Sending mail successfully to " + mailId + " from " + mailId2);
			LOGGER.info("Sent mail successfully to " + mailId + " Message Called from " + fromAddress);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail - >" + mailId + " Message Called for " + fromAddress);
			return false;
		}

	}

	public static JavaMailSender getJavaMailSender(String username, String password) {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost("smtp.gmail.com"); // Replace with your SMTP host
		mailSender.setPort(587); // Replace with your SMTP port
		mailSender.setUsername(username);
		mailSender.setPassword(password);

		Properties props = mailSender.getJavaMailProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.debug", "true");

		return mailSender;
	}

	public  static void emailNewGMTRfqForNoPR(String string, String subjectPrefix, JavaMailSender javaMailSender, Rfq rfqData, String host,
			String vendorMail, String otherEmails, String mailFom, String emailPassword, String rfqDueDate,
			String vendorId) throws MessagingException {
		// TODO Auto-generated method stub
		// String subject = rfqData.getCategory() + " - You have an Enquiry RFQ No " +
		// rfqData.getRfqId();
		String subject = subjectPrefix + " You have an Enquiry RFQ No " + rfqData.getRfqId() + " - " + vendorId;
		String message;
		String pincode = null;
		int i = 1;
		String city = null;
		StringBuilder email = new StringBuilder();
		if (!CollectionUtils.isEmpty(rfqData.getClientdeliverylocationrfq())) {
			city = rfqData.getClientdeliverylocationrfq().get(0).getCity();
			pincode = rfqData.getClientdeliverylocationrfq().get(0).getPincode();
		}
		email.append("<html>");
		email.append("<head>");
		email.append("<style>" + "table {" + "   border: 2px solid black;" + "   border-collapse: collapse;" + "}"
				+ "th, td {" + "   border: 1px solid black;" + "   padding: 8px;" + "}" + "</style>");
		email.append("</head>");
		email.append("<body>\n\n");
		email.append("Dear Partner,<br><br>\n\n");
		email.append("<b>** Please find the below RFQ and Submit your Quotation in a reply mail **</b><br><br>\n\n");
		email.append("Rfq Due Date: " + rfqDueDate + "</b><br><br>\n\n");
		email.append("<b>Project Description/Reference: " + rfqData.getProjectDesc() + "</b><br><br>\n\n");
		email.append("<b>Please find the below RFQ details: </b><br><br>\n\n");
		email.append("<table>");
		email.append("<tr>");
		email.append("<th>");
		email.append("S.NO");
		email.append("</th>");
		email.append("<th>");
		email.append("RFQID");
		email.append("</th>");
		email.append("<th>");
		email.append("Item Description");
		email.append("</th>");
		email.append("<th>");
		email.append("Specification");
		email.append("</th>");
		email.append("<th>");
		email.append("UOM");
		email.append("</th>");
		email.append("<th>");
		email.append("Quantity");
		email.append("</th>");
		email.append("<th>");
		email.append("Remarks");
		email.append("</th>");
		email.append("</tr>");

		// Iterate through each RfqItem in the list
		for (RfqItem item : rfqData.getRfqItem()) {
			email.append("<tr>");
			email.append("<td style='border: 1px solid black;'>");
			email.append(i++);
			email.append("</td>");
			email.append("<td style='border: 1px solid black;'> ");
			email.append(rfqData.getRfqId());
			email.append("</td>");
			email.append("<td style='border: 1px solid black;'> ");
			if (!CollectionUtils.isEmpty(rfqData.getRfqItem())) {
				email.append(item.getDescription());
			}
			email.append("</td>");

			// Include additional fields from RfqItem
			email.append("<td style='border: 1px solid black;'>");
			email.append(item.getBrand());
			email.append("</td>");
			email.append("<td style='border: 1px solid black;'>");
			email.append(item.getUnitofMeasures());
			email.append("</td>");
			email.append("<td style='border: 1px solid black;'>");
			email.append(item.getQuantity());
			email.append("</td>");
			email.append("<td style='border: 1px solid black;'>");
			email.append(item.getRemarks());
			email.append("</td>");
			email.append("</tr>");
		}
		email.append("</table>"); // Close the table
		email.append("<br><br>");
		email.append("<br><br>");
		// Assuming email is a StringBuilder or similar
		email.append("<b> Delivery Location(City / Town): </b><br>");

		email.append("City : " + city + "<br>");
		email.append("Pincode : " + pincode + "<br>");
		email.append("<b>Delivery Date:" + rfqData.getDeliveryDate() + "</b><br><br>\n\n");
		email.append("<b>About Procucev:</b><br>");
		email.append(
				"We're a leading Enterprise Procurement company with a proven track record of connecting clients with vendors facilitating efficient and transparent procurement for all. Through our cutting-edge technology and expert services, we help companies streamline their procurement process, saving time and money. Boost your reach & visibility with Procucev. Join our network of trusted vendors and connect with established companies.<br>");
		email.append(
				"Visit our website at <a href=\"https://procucev.com\">procucev.com</a> to learn more about our services and the benefits of partnering with Procucev.<br><br>");
		email.append("<b>Thanks,</b><br>");
		email.append("Procucev Admin");
		email.append("<br><br>");

		List<RFQDocument> documentList = rfqData.getRfqDocument();
		MimeMultipart multipart = new MimeMultipart();

		// Add the text content of the email to a MimeBodyPart
		MimeBodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setContent(email.toString(), "text/html");
		multipart.addBodyPart(messageBodyPart);

		// Add attachments to the MimeMultipart
		if (!CollectionUtils.isEmpty(rfqData.getRfqDocument())) {
			email.append("<b>List of Documents:</b><br>");
			for (RFQDocument document : rfqData.getRfqDocument()) {
				byte[] file = document.getFile();
				ByteArrayDataSource dataSource = new ByteArrayDataSource(file, "application/octet-stream");
				MimeBodyPart attachmentPart = new MimeBodyPart();
				attachmentPart.setDataHandler(new DataHandler(dataSource));
				attachmentPart.setFileName(document.getFileName()); // Set the actual file name here
				multipart.addBodyPart(attachmentPart);
			}
		}
		JavaMailSender javaMailSender2 = getJavaMailSender(mailFom, emailPassword);
		// Custom from address
		emailNotifierGenericNoPRGmtBySenderList(subject, javaMailSender2, mailFom, otherEmails, multipart, vendorMail);

	}

	private static boolean emailNotifierGenericNoPRGmtBySenderList(String subject, JavaMailSender javaMailSender,
			String mailFom, String otherEmails, MimeMultipart multipart, String vendorMail) {
		// TODO Auto-generated method stub
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		try {
			LOGGER.info("Sending mail successfully to " + vendorMail + " from " + mailFom);
			MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
			// InternetAddress ia = new InternetAddress(fromAddress);
			mimeMessageHelper.setTo(vendorMail);
			mimeMessageHelper.setFrom(mailFom);
			if (otherEmails != null) {
				mimeMessageHelper.setCc(otherEmails);
			}
			mimeMessageHelper.setSubject(subject);
			mimeMessageHelper.setText("Please find the attachments below.");
			mimeMessageHelper.getMimeMessage().setContent(multipart); // Set the MimeMultipart as the content
			javaMailSender.send(mimeMessage);

			LOGGER.info("Sent mail successfully to " + vendorMail + " Message Called from " + mailFom);
			return true;
		} catch (Exception e) {
			LOGGER.error("Error in sending mail - >" + vendorMail + " Message Called for " + mailFom);
			return false;
		}

	}

	public static void sendOtpForEmail(String string, String email, JavaMailSender javaMailSender, InternetAddress add,
			String host, String otp) {
		String subject = "OTP For Validation ";
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b>You have received otp  </b>" + otp + "<b> for validation </b>" + "<br><br>\n" + "\n"
				+ "<p><a href=\"" + host + "/login\">Click Here!!</a></p>\n" + "<b>to login into Procucev Portal </b>"
				+ "<br><br>\n" + "\n" + "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Solutions</b>\n" + "\n" + "\n"
				+ "</body>\n" + "</html>";
		emailNotifierGenericBySender(subject, email, add, javaMailSender, message, string);
	}

	public static boolean forwardMessage(
	        String forwardAddress,
	        JavaMailSender javaMailSender,
	        String mailFrom,
	        Message originalMessage,
	        String emailPassword) {

	    try {
	        LOGGER.info("Forwarding message to {}", forwardAddress);

	        // Use provided JavaMailSender so we don't rebuild SMTP config every time
	        JavaMailSender sender = getJavaMailSender(mailFrom, emailPassword);

            MimeMessage forward = sender.createMimeMessage();

            // Basic headers
          
	        forward.setFrom(new InternetAddress(mailFrom));
            forward.setRecipient(Message.RecipientType.TO, new InternetAddress(forwardAddress));
            forward.setSubject("Fwd: " + originalMessage.getSubject());

	        Object content = originalMessage.getContent();

	        if (content instanceof Multipart) {
	            // ✅ Preserve original multipart mail exactly (your old code)
	            forward.setContent((Multipart) content);

	        } else {
	            // Rare fallback (attach original email)
	            ByteArrayOutputStream baos = new ByteArrayOutputStream();
	            originalMessage.writeTo(baos);

	            MimeMultipart multipart = new MimeMultipart();

	            MimeBodyPart intro = new MimeBodyPart();
	            intro.setText("Original message attached.", "UTF-8");
	            multipart.addBodyPart(intro);

	            MimeBodyPart attach = new MimeBodyPart();
	            DataSource ds = new ByteArrayDataSource(baos.toByteArray(), "message/rfc822");
	            attach.setDataHandler(new DataHandler(ds));
	            attach.setFileName("original-message.eml");
	            multipart.addBodyPart(attach);

	            forward.setContent(multipart);
	        }

	        forward.saveChanges();

	        sender.send(forward);
	        LOGGER.info("Forward email successfully sent to {}", forwardAddress);

	        return true;

	    } catch (Exception e) {
	        LOGGER.error("Forwarding failed", e);
	        return false;
	    }
	}


	public static void sendEmailForClient(String string, String email, JavaMailSender javaMailSender,
			InternetAddress add, String host) {
		// TODO Auto-generated method stub
		LOGGER.info("Entered To SendEmailForClient()");
		String subject = "Client Registration Successful";
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b>\n" + "Thank you for registering with Procucev!!</b>" + "<br><br>\n" + "\n" + "<b>\n"
				+ "You can now submit unlimited Free RFQs and competitive quotes directly to your inbox.GMT simplifies sourcing.\n</b>"
				+ "<br><br>\n" + "\n" + "<b>\n"
				+ "It's countdown time...You'll receive your user ID and password shortly.\n</b>" + "<br><br>\n" + "\n"
				+ "<p><a href=\"" + host + "/login\">Click Here!!</a></p>\n" + "<b>to login into Procucev Portal </b>"
				+ "<br><br>\n" + "\n" + "<b>Warm regards, <br></b>\n" + "\n" + "<b>Procucev Admin</b>\n" + "\n" + "\n"
				+ "</body>\n" + "</html>";
		emailNotifierGenericBySender(subject, email, add, javaMailSender, message, string);
	}

	public static void emailForVendor(String string, String email, JavaMailSender javaMailSender, InternetAddress add,
			String host) {
		// TODO Auto-generated method stub
		LOGGER.info("Entered To SendEmailForVendor()");
		String subject = "Vendor Registration Successful";
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b>\n" + "Thank you for registering with Procucev!!</b>" + "<br><br>\n" + "\n" + "<b>\n"
				+ "No more chasing leads, register with GMT and reach a wider audience of active buyers seeking your services.Take your business to the next level with GMT.\n</b>"
				+ "<br><br>\n" + "\n" + "<b>\n"
				+ "The launch date is on countdown…You will receive your user id and password soon..\n</b>"
				+ "<br><br>\n" + "\n" + "<p><a href=\"" + host + "/login\">Click Here!!</a></p>\n"
				+ "<b>to login into Procucev Portal </b>" + "<br><br>\n" + "\n" + "<b>Warm regards, <br></b>\n" + "\n"
				+ "<b>Procucev Admin,  <br></b>\n" + "<b>Procucev Enterprise Solutions, <br></b>\n"
				+ "<b>Bangalore <br></b>\n" + "\n" + "</body>\n" + "</html>";
		emailNotifierGenericBySender(subject, email, add, javaMailSender, message, string);
	}

	public static void mailingGMTClientRFQMailToinfoTeam(String string, String toEmail, JavaMailSender javaMailSender,
			InternetAddress add, String host, User user, String email, String phone, String fullName, String orgName) {
		// TODO Auto-generated method stub
		LOGGER.info("Entered To SendEmailForVendor()");
		String subject = user.getSubject();
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b>\n" + user.getMessage() + "<br><br>\n" + "\n" + "<b>\n" + "For RfqId:" + user.getRfqId()
				+ "<br><br>\n" + "\n" + "<b>\n" + "<b>Full Name:</b> " + fullName + "<br><br>\n"
				+ "<b>Organization Name:</b> " + orgName + "<br><br>\n" + "<b>Email:</b> " + email + "<br><br>\n"
				+ "<b>Phone:</b> " + phone + "<br><br>\n" + "\n" + "<b>Warm regards, <br></b>\n" + "\n"
				+ "<b>Procucev Admin,  <br></b>\n" + "<b>Procucev Enterprise Solutions, <br></b>\n"
				+ "<b>Bangalore <br></b>\n" + "\n" + "</body>\n" + "</html>";
		emailNotifierGenericBySender(subject, toEmail, add, javaMailSender, message, string);
	}

	public static void emailPPOForApproval(String string, String userName, JavaMailSender javaMailSender,
			InternetAddress add, String ppoId, String host, String type) {
		// TODO Auto-generated method stub
		String subject = "PPO For Approval ";
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b>You have received PPO with PPO ID </b>" + ppoId + "<b>for approval </b>" + "<br><br>\n"
				+ "\n" + "<p><a href=\"" + host + "/login\">Click Here!!</a></p>\n"
				+ "<b>to login into Procucev Portal </b>" + "<br><br>\n" + "\n" + "<b>Thanks, <br></b>\n" + "\n"
				+ "<b>Procucev Solutions</b>\n" + "\n" + "\n" + "</body>\n" + "</html>";
		emailNotifierGenericBySender(subject, userName, add, javaMailSender, message, type);
	}

	public static void buyerEmailBFSAccepted(String type, String userName, JavaMailSender javaMailSender,
	        InternetAddress add, BFSUsers bfsUser, String host, String uniqueId) {

	    String subject = "Your Bid Has Been Accepted – Next Steps on BFS";

	    StringBuilder email = new StringBuilder();

	    email.append("<html>");
	    email.append("<head>");
	    email.append("<style>");
	    email.append("table { border-collapse: collapse; width: 100%; }");
	    email.append("th, td { border: 1px solid #000; padding: 8px; text-align: left; }");
	    email.append("th { background-color: #f2f2f2; }");
	    email.append("</style>");
	    email.append("</head>");

	    email.append("<body style='font-family: Arial, sans-serif; color:#333;'>");

	    email.append("Dear Partner,<br><br>");

	    email.append("We are pleased to inform you that your bid for the below listed item(s) ");
	    email.append("has been accepted by the seller through BFS.<br><br>");

	    // Item Table
	    email.append("<table>");
	    email.append("<tr>");
	    email.append("<th>Item No</th>");
	    email.append("<th>Item Name</th>");
	    email.append("<th>Quantity</th>");
	    email.append("<th>Price</th>");
	    email.append("</tr>");

	    email.append("<tr>");
	    email.append("<td>");
	    if (bfsUser.getItems().getItemNumber() != null) {
	        email.append(bfsUser.getItems().getItemNumber());
	    }
	    email.append("</td>");

	    email.append("<td>");
	    email.append(bfsUser.getItems().getDescription());
	    email.append("</td>");

	    email.append("<td>");
	    email.append(bfsUser.getQuantity());
	    email.append("</td>");

	    email.append("<td>");
	    email.append(bfsUser.getAskPrice());
	    email.append("</td>");
	    email.append("</tr>");

	    email.append("</table><br>");

	    // Unique Passcode
	    email.append("<b>Transaction Unique Passcode: </b>");
	    email.append(uniqueId);
	    email.append("<br><br>");

	    email.append("Kindly share this code with your Procucev associate to proceed with the transaction. ");
	    email.append("Our team will support you in completing the deal smoothly and efficiently.<br><br>");

	    email.append("Looking to buy more? Simply search available stock on BFS and place your next bid.<br><br>");

	    email.append("Regards,<br>");
	    email.append("<b>Team QUA AI</b>");

	    email.append("</body>");
	    email.append("</html>");

	    String message = email.toString();

	    emailNotifierGenericBySender(subject, userName, add, javaMailSender, message, type);
	}
	
	public static void emailBFSAccepted(String type, String userName, JavaMailSender javaMailSender,
			InternetAddress add, BFSUsers bfsUser, String host, String uniqueId) {
		// TODO Auto-generated method stub
		String subject = "Item is Accepted from Buy From Stock(BFS) ! Next Steps for Transaction...";
		String message;
		String pincode = null;

		StringBuilder email = new StringBuilder();

		email.append("<html>");
		email.append("<head>");
		email.append("<style>" + "table {" + "   border: 2px solid black;" + "   border-collapse: collapse;" + "}"
				+ "th, td {" + "   border: 1px solid black;" + "   padding: 8px;" + "}" + "</style>");
		email.append("</head>");
		email.append("<body>\n\n");
		email.append("Dear Partner,<br><br>\n\n");
		email.append(
				"<b> The following items have been accepted for further transaction with Buy From Stock(BFS).</b><br><br>\n\n");
		email.append("<table>");
		email.append("<tr>");
		email.append("<th>");
		email.append("Item No");
		email.append("</th>");
		email.append("<th>");
		email.append("Item Name");
		email.append("</th>");
		email.append("<th>");
		email.append("Quantity");
		email.append("</th>");
		email.append("<th>");
		email.append("Price");
		email.append("</th>");
		email.append("</tr>");

		email.append("<tr>");
		email.append("<td style='border: 1px solid black;'>");
		if (bfsUser.getItems().getItemNumber() != null) {
			email.append(bfsUser.getItems().getItemNumber());
		}
		email.append("</td>");
		email.append("<td style='border: 1px solid black;'> ");
		email.append(bfsUser.getItems().getDescription());
		email.append("</td>");

		// Include additional fields from RfqItem
		email.append("<td style='border: 1px solid black;'>");
		email.append(bfsUser.getQuantity());
		email.append("</td>");
		email.append("<td style='border: 1px solid black;'>");
		email.append(bfsUser.getAskPrice());

		email.append("</table>"); // Close the table
		email.append("<br><br>");
		email.append("<br><br>");
		// Assuming email is a StringBuilder or similar
		email.append("<b> The following is the unique passcode: </b><br>");
		email.append(uniqueId + "<br>");
		email.append(
				"To proceed with the transaction, please contact your dedicated Procucev associate. They will guide you through the next steps and address any queries you may have.<br>");
		email.append("We look forward to facilitating a smooth transaction.");
		email.append("<br><br>");
		email.append("<b>About Procucev:</b><br>");
		email.append("<br><br>");
		email.append(
				"We're a leading Enterprise Procurement company with a proven track record of connecting clients with vendors facilitating efficient and transparent procurement for all. Through our cutting-edge technology and expert services, we help companies streamline their procurement process, saving time and money. Boost your reach & visibility with Procucev. Join our network of trusted vendors and connect with established companies.<br>");
		email.append(
				"Visit our website at <a href=\"https://procucev.com\">procucev.com</a> to learn more about our services and the benefits of partnering with Procucev.<br><br>");
		email.append("<b>Thanks,</b><br>");
		email.append("Procucev Admin");
		email.append("<br><br>");
		message = email.toString();
		emailNotifierGenericBySender(subject, userName, add, javaMailSender, message, type);
	}
	
	public static void sellerEmailBFSAccepted(String type, String userName, JavaMailSender javaMailSender,
	        InternetAddress add, BFSUsers bfsUser, String host, String uniqueId) {

	    String subject = "Bid Accepted Successfully – Proceed with BFS Transaction";

	    StringBuilder email = new StringBuilder();

	    email.append("<html>");
	    email.append("<head>");
	    email.append("<style>");
	    email.append("table { border-collapse: collapse; width: 100%; }");
	    email.append("th, td { border: 1px solid #000; padding: 8px; text-align: left; }");
	    email.append("th { background-color: #f2f2f2; }");
	    email.append("</style>");
	    email.append("</head>");

	    email.append("<body style='font-family: Arial, sans-serif; color:#333;'>");

	    email.append("Dear Partner,<br><br>");

	    email.append("You have successfully accepted the buyer’s bid for the below listed item(s) under BFS.<br><br>");

	    // Item & Bid Details Table
	    email.append("<table>");
	    email.append("<tr>");
	    email.append("<th>Item No</th>");
	    email.append("<th>Item Name</th>");
	    email.append("<th>Quantity</th>");
	    email.append("<th>Accepted Bid Price</th>");
	    email.append("</tr>");

	    email.append("<tr>");

	    email.append("<td>");
	    if (bfsUser.getItems().getItemNumber() != null) {
	        email.append(bfsUser.getItems().getItemNumber());
	    }
	    email.append("</td>");

	    email.append("<td>");
	    email.append(bfsUser.getItems().getDescription());
	    email.append("</td>");

	    email.append("<td>");
	    email.append(bfsUser.getQuantity());
	    email.append("</td>");

	    email.append("<td>");
	    email.append(bfsUser.getAskPrice());
	    email.append("</td>");

	    email.append("</tr>");
	    email.append("</table><br>");

	    email.append("The transaction is now initiated.<br><br>");

	    email.append("<b>Transaction Unique Passcode: </b>");
	    email.append(uniqueId);
	    email.append("<br><br>");

	    email.append("Kindly share this code while coordinating with your designated Procucev associate ");
	    email.append("to proceed with the next steps. Our team will assist you in completing the ");
	    email.append("transaction smoothly and efficiently.<br><br>");

	    email.append("We look forward to facilitating a successful deal closure.<br><br>");

	    email.append("Regards,<br>");
	    email.append("<b>Team QUA AI</b>");

	    email.append("</body>");
	    email.append("</html>");

	    String message = email.toString();

	    emailNotifierGenericBySender(subject, userName, add, javaMailSender, message, type);
	}

	public static void sendClientEmailForCM2(String type, String toAddress, Organization organization,
			JavaMailSender javaMailSender, InternetAddress add, String host) {
		// TODO Auto-generated method stub
		String subject = "New Client Registration!!";
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b>You have received new client registration with User Name </b>" + organization.getEmail()
				+ "<b>for Client </b>" + organization.getCompanyName() + "<br><br>\n" + "\n" + "<b>Address </b>"
				+ organization.getAddress1()
				// + "<br><br>\n" + "\n" + "<b>Pan</b>" + organization.getPan()
				+ "<br><br>\n" + "\n" + "<b>Phone</b>" + organization.getOrganizationPhonenumber() + "<br><br>\n" + "\n"
				+ "<p><a href=\"" + host + "/login\">Click Here!!</a></p>\n" + "<b>to login into Procucev Portal </b>"
				+ "<br><br>\n" + "\n" + "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Solutions</b>\n" + "\n" + "\n"
				+ "</body>\n" + "</html>";
		emailNotifierGenericBySender(subject, toAddress, add, javaMailSender, message, type);
	}

	public static void mailingVerificationLinkWithSelfUserLogin(JavaMailSender javaMailSender, String from,
	        InternetAddress add, String pswd, String hostName, User user) {

	    String verificationTemplate = "<!DOCTYPE html>"
	            + "<html>"
	            + "<body style='font-family: Arial, sans-serif; color: #333; line-height: 1.6;'>"

	            + "<p>Dear Partner,</p>"

	            + "<p>Greetings from Procucev!</p>"

	            + "<p>Welcome to the Procucev QUA AI portal! Your buyer account is ready. "
	            + "Click below to set up your profile and start circulating RFQs to trusted vendors:</p>"

	            + "<p><a href='" + hostName + "/login' "
	            + "style='background-color:#007bff; color:#fff; padding:10px 15px; "
	            + "text-decoration:none; border-radius:5px;'>"
	            + "Create Your Account</a></p>"

	            + "<p><b>Login Details:</b><br><br>"
	            + "Username: <b>" + user.getUsername() + "</b><br>"
	            + "Mobile: <b>" + user.getPhone() + "</b><br>"
	            + "Password: <b>" + user.getPassword() + "</b><br>"
	            + "(Use this password to log in for the first time and create your new password.)</p>"

	            + "<p>Procucev GMT & BFS powered by QUA AI simplifies your vendor sourcing — "
	            + "publish RFQs, get quotes directly to your inbox, and make immediate purchases through BFS. "
	            + "visit "
	            + "<a href='https://www.qua.procucev.com'>www.qua.procucev.com</a> anytime to get started.</p>"

	            + "<p>Facing difficulties? Let us connect with our dedicated support team - "
	            + "<a href='mailto:support@procucev.com'>support@procucev.com</a></p>"

	            + "<p>Thanks,<br><b>Team QUA AI</b></p>"

	            + "<hr>"
	            + "<p><b>About Procucev:</b><br>"
	            + "<i>Procucev leverages decades of expertise to deliver unified enterprise procurement solutions "
	            + "with AI-enabled vendor platforms, consulting, and digital technology, helping buyers streamline "
	            + "sourcing, circulate RFQs, and make faster purchases through QUA AI. Visit "
	            + "<a href='https://www.procucev.com'>www.procucev.com</a> to know more.</i></p>"

	            + "</body></html>";

	    try {
	        JavaMailSender mailSender = getJavaMailSender(from, pswd);
	        MimeMessage mimeMessage = mailSender.createMimeMessage();
	        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

	        helper.setTo(user.getUsername());
	        helper.setFrom(add);
	        helper.setSubject("Procucev QUA AI Portal – Your Buyer Account is Ready!");
	        helper.setText(verificationTemplate, true);

	        mailSender.send(mimeMessage);
	        LOGGER.info("Buyer account ready mail sent successfully to {}", user.getUsername());

	    } catch (Exception e) {
	        LOGGER.error("Error in sending buyer account creation mail --> {}", e.getMessage(), e);
	        throw new AppException(StatusCodes.MAIL_SEND_ERROR,
	                ApplicationConstants.MAIL_SENDING_FAILURE,
	                ApplicationConstants.BUSSINESS_EXCEPTION,
	                ApplicationConstants.FAILURE);
	    }
	}
	
	public static void emailForBidRequest(String type, String toAddress, JavaMailSender javaMailSender,
			InternetAddress add, String host, BFSUsers savedUser, User user, String desc) {
		// TODO Auto-generated method stub
		String subject = "New Bid Request from " + user.getCompanyName() +" for Item: " + savedUser.getItems().getDescription();
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear Partner,<br><br>\n" + "\n"
				+ "This is to inform you that <b>Buyer : </b>" + user.getCompanyName()
				+ " has requested a bid for the following item:<br><br>\n" + "\n" + "<b>Item Description: </b>"
				+ desc + "<br>\n" + "<b>Bid Price: </b>" + savedUser.getAskPrice()
				+ "<br><br>\n" + "\n" + "You can reach the buyer at <b>Email: </b>" + user.getUsername()
				+ " or <b>Phone: </b>" + user.getPhone() + "<br><br>\n" + "\n" + "<br><br>\n" + "Best regards,<br>\n"
				+ "<b>Procucev Solutions</b>\n" + "\n" + "</body>\n" + "</html>";
		emailNotifierGenericBySender(subject, toAddress, add, javaMailSender, message, type);
	}

	public static void sendVendorEmailForCM2(String type, String toAddress, Organization organization,
			JavaMailSender javaMailSender, InternetAddress add, String host) {
		// TODO Auto-generated method stub
		String subject = "New Self Vendor Registration!!";
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner" + ", <br><br>\n"
				+ "\n" + "<b>You have received new vendor registration with User Name </b>" + organization.getEmail()
				+ "<b>for Client </b>" + organization.getCompanyName() + "<br><br>\n" + "\n" + "<b>Address </b>"
				+ organization.getAddress1()
				// + "<br><br>\n" + "\n" + "<b>Pan</b>" + organization.getPan()
				+ "<br><br>\n" + "\n" + "<b>Phone</b>" + organization.getOrganizationPhonenumber() + "<br><br>\n" + "\n"
				+ "<p><a href=\"" + host + "/login\">Click Here!!</a></p>\n" + "<b>to login into Procucev Portal </b>"
				+ "<br><br>\n" + "\n" + "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Solutions</b>\n" + "\n" + "\n"
				+ "</body>\n" + "</html>";
		emailNotifierGenericBySender(subject, toAddress, add, javaMailSender, message, type);
	}

	public static void emailrfqReject(String string, String username, JavaMailSender javaMailSender,
			InternetAddress add, String rfqId, String host, User user) {
		// TODO Auto-generated method stub
		String subject = "RFQ Rejected By Vendor!!";
		String message = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "partner" + ", <br><br>\n"
				+ "\n" + "<b> Your RFQ With RfqID </b>" + rfqId + "<b> got Accepted </b>" + "<br><br>\n" + "\n"
				+ "<p><a href=\"" + host + "/login?regId=" + user.getOrg().getId() + "\">Click Here!!</a></p>\n"
				+ "<b>to login into Procucev Portal </b>" + "\n"

				+ "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Solutions</b>\n" + "\n" + "\n" + "</body>\n"
				+ "</html>";
		emailNotifierGenericBySender(subject, username, add, javaMailSender, message, string);
	}

//
//	public static void emailForVendor(String string, String email, JavaMailSender javaMailSender, InternetAddress add,
//			String host) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public static void sendVendorEmailForCM2(String string, String toAddress, Organization organization,
//			JavaMailSender javaMailSender, InternetAddress add, String host) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public static void sendOtpForEmail(String string, String email, JavaMailSender javaMailSender, InternetAddress add,
//			String host, String otp) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public static void sendClientEmailForCM2(String string, String toAddress, Organization organization,
//			JavaMailSender javaMailSender, InternetAddress add, String host) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public static void mailingGMTClientRFQMailToinfoTeam(String string, String toEmail, JavaMailSender javaMailSender,
//			InternetAddress add, String host, User user) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public static void emailNewGMTRfqForNoPR(String string, JavaMailSender javaMailSender, Rfq rfq, String host,
//			String email, String otherEmails, String mailFom, String emailPassword, String rfqDueDate) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public static void emailNewRfqForNoPR(String string, JavaMailSender javaMailSender, Rfq rfqData, String host,
//			String email, String username, String otherEmails, String phoneNumber, String rfqDueDate, String fullName,
//			String string2, String string3) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public static void emailforgotpassword(String string, String username, JavaMailSender javaMailSender,
//			InternetAddress add, String password, String host, User user) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public static void forwardMessage(String forwardAddress, JavaMailSender javaMailSender, String mailFom,
//			Message message) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public static void mailingVerificationLinkWithSelfUserLogin(JavaMailSender javaMailSender, String mail,
//			InternetAddress add, String pswd, String host, User user) {
//		// TODO Auto-generated method stub
//		
//	}

//	public static void mailingVerificationLinkWithUser(JavaMailSender javaMailSender, InternetAddress add,
//			String hostName, User user) {
//		System.out.println("pswdd mail---" + user.getPassword());
//		String verificationTemplate = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "Dear " + "Partner"
//				+ ", <br><br>\n" + "\n" + "Greetings from Procucev!!" + "<br><br>\n" + "\n"
//				+ "<b>Please click on the below link to start creating your Seller profile with Procucev Solutions</b>\n"
//				+ "\n" + "<p><a href=\"" + hostName + "/login?regId=" + user.getOrg().getId()
//				+ "\">Create your account !!</a></p>\n" + "<b>Use the login details mentioned below to proceed:</b>\n"
//				+ ",<br><br>\n" + "<b>UserName " + user.getUsername() + ",<br><br></b>\n" + "\n" + "<b>Password "
//				+ user.getPassword() + "<br><br></b>\n" + "\n"
//
//				+ "<b>Thanks, <br></b>\n" + "\n" + "<b>Procucev Solutions</b>\n" + "\n" + "\n" + "</body>\n"
//				+ "</html>";
//
//		System.out.println(verificationTemplate);
//		// http://localhost:4201/vendorRegistration?regId=r123
//
//		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
//		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
//		try {
//			mimeMessageHelper.setTo(user.getUsername());
//			mimeMessageHelper.setFrom(add); // from Address
//			mimeMessageHelper.setSubject("Procucev Portal Account Creation !!");
//			mimeMessageHelper.setText(verificationTemplate, true);
//			javaMailSender.send(mimeMessage);
//		} catch (Exception e) {
//			LOGGER.error("Error in sending creation mail -- " + e.getMessage());
//			throw new AppException(StatusCodes.MAIL_SEND_ERROR, ApplicationConstants.MAIL_SENDING_FAILURE,
//					ApplicationConstants.BUSSINESS_EXCEPTION, ApplicationConstants.FAILURE);
//		}
//		LOGGER.info("Sent verifiation mail successfully");
//	}


	public static void mailingVerificationLinkWithUser(JavaMailSender javaMailSender, InternetAddress add,
	        String hostName, User user) {

	    String verificationTemplate = "<!DOCTYPE html>"
	            + "<html>"
	            + "<body>"
	            + "Dear Partner,<br><br>"

	            + "Welcome to the Procucev QUA AI portal! Your account is ready. "
	            + "Click below to set up your profile and update your categories to unlock new business opportunities:<br><br>"

	            + "<p><a href=\"" + hostName + "/login?regId=" + user.getOrg().getId()
	            + "\" style='background-color:#007bff;color:#ffffff;padding:10px 20px;"
	            + "text-decoration:none;border-radius:5px;'>Click Here To Create Your Account</a></p><br>"

	            + "<b>Your login details:</b><br><br>"
	            + "Username: <b>" + user.getUsername() + "</b><br>"
	            + "Mobile: <b>" + user.getPhone() + "</b><br>"
	            + "Password: <b>" + user.getPassword() + "</b><br>"
	            + "(Use this password to log in for the first time to create your new password.)<br><br>"

	            + "Procucev GMT & BFS powered by QUA AI acts as your extended sales team, "
	            + "helping you get more business sales qualified leads and new business opportunities. "
	            + "visit "
	            + "<a href='https://www.qua.procucev.com'>www.qua.procucev.com</a> anytime to get started.<br><br>"

	            + "Facing difficulties? Let us connect with our dedicated support team - "
	            + "<a href='mailto:support@procucev.com'>support@procucev.com</a><br><br>"

	            + "Thanks,<br>"
	            + "<b>Team QUA AI</b><br><br>"

	            + "<hr>"
	            + "<b>About Procucev:</b><br>"
	            + "<i>Procucev leverages decades of expertise to deliver unified enterprise procurement solutions "
	            + "with AI-enabled vendor platforms, consulting, and digital technology, helping sellers expand "
	            + "their reach and connect with qualified buyers through QUA AI. Visit "
	            + "<a href='https://www.procucev.com'>www.procucev.com</a> to know more.</i>"

	            + "</body>"
	            + "</html>";

	    MimeMessage mimeMessage = javaMailSender.createMimeMessage();

	    try {
	        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

	        mimeMessageHelper.setTo(user.getUsername());
	        mimeMessageHelper.setFrom(add);
	        mimeMessageHelper.setSubject("Procucev QUA AI Portal – Your Seller Account is Ready!");
	        mimeMessageHelper.setText(verificationTemplate, true);

	        javaMailSender.send(mimeMessage);

	    } catch (Exception e) {
	        LOGGER.error("Error in sending account creation mail -- " + e.getMessage());
	        throw new AppException(StatusCodes.MAIL_SEND_ERROR,
	                ApplicationConstants.MAIL_SENDING_FAILURE,
	                ApplicationConstants.BUSSINESS_EXCEPTION,
	                ApplicationConstants.FAILURE);
	    }

	    LOGGER.info("Seller account ready mail sent successfully");
	}
	public static void emailForBuyerBidRequest(String type, String username, JavaMailSender javaMailSender,
			InternetAddress add, String host, BFSUsers savedUser, User user, String desc) {
		// TODO Auto-generated method stub
		String subject = " Bid Submitted Successfully – Awaiting Seller Confirmation " ;
		String message = "<!DOCTYPE html>"
		        + "<html>"
		        + "<body>"

		        + "Dear Partner,<br><br>"

		        + "Your bid for the below listed item(s) has been successfully submitted on <b>BFS</b>.<br><br>"

		        + "<b>Item & Bid Details</b><br>"
		        + "<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;'>"
		        + "<tr>"
		        + "<th>Item Description</th>"
		        + "<th>Bid Price</th>"
		        + "</tr>"
		        + "<tr>"
		        + "<td>" + desc + "</td>"
		        + "<td>" + savedUser.getAskPrice() + "</td>"
		        + "</tr>"
		        + "</table><br><br>"

		        + "The seller has been notified and your bid is currently awaiting acceptance. "
		        + "You will receive a confirmation once the seller approves the bid.<br><br>"

		        + "In the meantime, you may explore additional stock and place more bids via the "
		        + "<b>QUA AI portal</b> "
		        + "(<a href='https://qua.procucev.com'>www.qua.procucev.com</a>) <br><br>"
		        + "Regards,<br>"
		        + "<b>Team QUA AI</b>"
		        + "</body>"
		        + "</html>";
		emailNotifierGenericBySender(subject, username, add, javaMailSender, message, type);
		
	}

	public static void emailForsellerBidRequest(String type, String sellerEmail, JavaMailSender javaMailSender,
			InternetAddress add, String host, BFSUsers savedUser, User user, String desc) {
		// TODO Auto-generated method stub
		String subject = "New Bid Received on Your Listed Stock – Action Required";
		String message = "<!DOCTYPE html>"
		        + "<html>"
		        + "<body>"

		        + "Dear Partner,<br><br>"

		        + "You have received a new bid for the stock listed by you on <b>QUA AI</b>.<br><br>"

		        + "<b>Item & Bid Details</b><br>"
		        + "<table border='1' cellpadding='6' cellspacing='0' style='border-collapse:collapse;'>"
		        + "<tr>"
		        + "<th>Item Description</th>"
		        + "<th>Bid Price</th>"
		        + "</tr>"
		        + "<tr>"
		        + "<td>" + desc + "</td>"
		        + "<td>" + savedUser.getAskPrice() + "</td>"
		        + "</tr>"
		        + "</table><br><br>"

		        + "You may review and accept the bid instantly via:<br>"
		        + "&#8226; <b>QUA AI Portal:</b> "
		        + "<a href='https://qua.procucev.com'>www.qua.procucev.com</a><br><br>"

		        + "Accept the bid to connect directly with the buyer and proceed with the transaction.<br><br>"

		        + "<i>Sell hassle-free through BFS… List More Sell More.</i><br><br>"

		        + "Regards,<br>"
		        + "<b>Team QUA AI</b>"

		        + "</body>"
		        + "</html>";
		emailNotifierGenericBySender(subject, sellerEmail, add, javaMailSender, message, type);
	
		
	}

}
