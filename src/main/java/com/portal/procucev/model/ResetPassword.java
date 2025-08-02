package com.portal.procucev.model;

public class ResetPassword {
	private static final long serialVersionUID = 1L;

	private String userName;
	
	private String phone;
	
	private String password;
	
	private String newpassword;
	
	private String confirmpassword;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getNewpassword() {
		return newpassword;
	}

	public void setNewpassword(String newpassword) {
		this.newpassword = newpassword;
	}

	public String getConfirmpassword() {
		return confirmpassword;
	}

	public void setConfirmpassword(String confirmpassword) {
		this.confirmpassword = confirmpassword;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	@Override
	public String toString() {
		return "ResetPassword [userName=" + userName + ", phone=" + phone + ", password=" + password + ", newpassword="
				+ newpassword + ", confirmpassword=" + confirmpassword + "]";
	}

	public ResetPassword() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	

}
