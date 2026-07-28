package com.portal.procucev.Dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserActivityDto {
	
	@JsonProperty("loginTime")
	private LocalDateTime loginTime;
	@JsonProperty("gmtorbfs")
	private String gmtOrBfs;
	@JsonProperty("type")
	private String operationType;
	@JsonProperty("subType")
	private String operationSubType;
	
	
	public LocalDateTime getLoginTime() {
		return loginTime;
	}
	public void setLoginTime(LocalDateTime loginTime) {
		this.loginTime = loginTime;
	}
	
	public String getGmtOrBfs() {
		return gmtOrBfs;
	}
	public void setGmtOrBfs(String gmtOrBfs) {
		this.gmtOrBfs = gmtOrBfs;
	}
	public String getOperationType() {
		return operationType;
	}
	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}
	public String getOperationSubType() {
		return operationSubType;
	}
	public void setOperationSubType(String operationSubType) {
		this.operationSubType = operationSubType;
	}
	@Override
	public String toString() {
		return "UserActivityDto [loginTime=" + loginTime + ", gmtOrBfs=" + gmtOrBfs + ", operationType=" + operationType
				+ ", operationSubType=" + operationSubType +"]";
	}
	
	
	
}
