package com.portal.procucev.model;

import java.time.LocalDateTime;


public class OtpDetails {
	private String otp;
    private LocalDateTime expirationTime;

    public OtpDetails(String otp, LocalDateTime expirationTime) {
        this.otp = otp;
        this.expirationTime = expirationTime;
    }

    public String getOtp() {
        return otp;
    }

    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }
}