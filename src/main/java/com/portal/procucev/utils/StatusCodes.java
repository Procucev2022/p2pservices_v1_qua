package com.portal.procucev.utils;

public class StatusCodes {

	public static final String NEW_VENDOR_CODE = "1001";
	public static final String CLIENT_PR_CLOSED_code = "1002";
	public static final String SEND_RFQ_CODE = "1003";
	public static final String MAIL_SEND_ERROR = "1004";
	public static final String OK_VENDOR_CODE = "200";
	// ❌ Server-side failures
    public static final String SERVER_ERROR = "500";

    // ⚠️ Business validation issues (e.g., insufficient credits, invalid input)
    public static final String VALIDATION_FAILED = "400";

}
