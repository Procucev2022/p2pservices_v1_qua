package com.portal.procucev.customexception;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Class to handle all response messages for Errors
 */
public class MessageResponse implements Serializable {

	private static final long serialVersionUID = 1L;
	private String statusCode;
	private String message;
	private List<String> errorMsg;
	private Date timestamp = new Date();
	private String status;
	private String type;
	 private Map<String, Object> data;

	

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	public MessageResponse(String statusCode, String message, List<String> errorMsg, String status) {
		super();
		this.statusCode = statusCode;
		this.message = message;
		this.errorMsg = errorMsg;
		this.status = status;
	}

	public MessageResponse(String statusCode, String message, Date timestamp, String status, String type) {
		super();
		this.statusCode = statusCode;
		this.message = message;
		this.timestamp = timestamp;
		this.status = status;
		this.type = type;
	}
	
	public MessageResponse( String message, String status) {
		super();
		this.message = message;
		this.status = status;
	}

	public MessageResponse(String statusCode, String message, List<String> errorMsg, Date timestamp, String status,
			String type) {
		super();
		this.statusCode = statusCode;
		this.message = message;
		this.errorMsg = errorMsg;
		this.timestamp = timestamp;
		this.status = status;
		this.type = type;
	}

	public MessageResponse(String statusCode, String message, Map<String, Object> data, String status,Date timestamp) {
	    super();
	    this.statusCode = statusCode;
	    this.message = message;
	    this.status = status;
	    this.data = data;
	    this.timestamp = timestamp;
	}
	/**
	 * @return the statusCode
	 */
	public String getStatusCode() {
		return statusCode;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the errorMsg
	 */
	public List<String> getErrorMsg() {
		return errorMsg;
	}

	/**
	 * @param errorMsg the errorMsg to set
	 */
	public void setErrorMsg(List<String> errorMsg) {
		this.errorMsg = errorMsg;
	}

	/**
	 * @return the timestamp
	 */
	public Date getTimestamp() {
		return timestamp;
	}

	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

}
