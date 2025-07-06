package com.portal.procucev.model;

public class ResetPassword {
	private static final long serialVersionUID = 1L;

	private String userName;
	
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

	@Override
	public String toString() {
		return "ResetPassword [userName=" + userName + ", password=" + password + ", newpassword=" + newpassword
				+ ", confirmpassword=" + confirmpassword + "]";
	}

	public ResetPassword() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	

}
