package com.portal.procucev.customexception;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private String statusCode;
    private String message;
    private String status;
    private Date timestamp;
    private List<String> errorMsg;
    private T data;
	public ApiResponse() {
		super();
		// TODO Auto-generated constructor stub
	}
	public ApiResponse(String statusCode, String message, String status, Date timestamp, List<String> errorMsg,
			T data) {
		super();
		this.statusCode = statusCode;
		this.message = message;
		this.status = status;
		this.timestamp = timestamp;
		this.errorMsg = errorMsg;
		this.data = data;
	}

  
	

}
