package com.portal.procucev.model;

import java.util.List;

import lombok.Data;

@Data
public class RfqStatusRequest {
	private String clientId;
	private List<String> rfqIds;
}
