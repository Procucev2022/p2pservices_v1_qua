package com.portal.procucev.model;

import lombok.Data;

@Data
public class AuthRequest {

private String username;
	
private String password;

private String phone;

private boolean otp;

private String tempEmail;

private String tempPhone;
}
