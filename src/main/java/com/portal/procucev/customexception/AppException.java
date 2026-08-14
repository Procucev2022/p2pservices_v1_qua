package com.portal.procucev.customexception;

import java.time.LocalDateTime;

public class AppException extends RuntimeException {
	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}

	private static final long serialVersionUID = 1L;

	private int errorCode;
	private String errorMessage;
	private String exceptiontype;
	private String status;
	private String statusCode;
	private LocalDateTime timestamp; 

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getExceptiontype() {
		return exceptiontype;
	}

	public void setExceptiontype(String exceptiontype) {
		this.exceptiontype = exceptiontype;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public AppException(int errorCode, String errorMessage, String exceptiontype, String status) {
		super(errorMessage);
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.exceptiontype = exceptiontype;
		this.status = status;
	}

	public AppException(int errorCode, String errorMessage, String exceptiontype, String status, LocalDateTime timeStamp) {
		super(errorMessage);
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.exceptiontype = exceptiontype;
		this.status = status;
		this.timestamp = LocalDateTime.now();
	}

	public AppException(String statusCode, String msg, String exceptiontype, String status) {
		super(msg);
		this.statusCode = statusCode;
		this.errorMessage = msg;
		this.exceptiontype = exceptiontype;
		this.status = status;
	}

	public AppException(int value, String message, String bussinessexception) {
		super(message);
		this.errorCode = value;
		this.errorMessage = message;
		this.exceptiontype = bussinessexception;
	}

	public AppException(String message) {
		super(message);
		this.errorMessage = message;
	}

	public AppException(int value, String message) {
		super(message);
		this.errorCode = value;
		this.errorMessage = message;
	}

	@Override
	public String toString() {
		return "AppException [errorCode=" + errorCode + ", errorMessage=" + errorMessage + ", exceptiontype="
				+ exceptiontype + ", status=" + status + "]";
	}
}
