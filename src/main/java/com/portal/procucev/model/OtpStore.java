package com.portal.procucev.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "otp_store")
public class OtpStore extends Procucev {


	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	@Column(name = "otp_key", nullable = false, unique = true)
	private String otpKey;

	@Column(name = "otp", nullable = false, length = 6)
	private String otp;

	@Column(name = "expiration_time", nullable = false)
	private LocalDateTime expirationTime;

	public OtpStore(String otpKey, String otp, LocalDateTime expirationTime) {
		super();
		this.otpKey = otpKey;
		this.otp = otp;
		this.expirationTime = expirationTime;
	}

	public OtpStore() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	

}
