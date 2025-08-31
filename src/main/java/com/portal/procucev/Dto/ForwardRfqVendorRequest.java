package com.portal.procucev.Dto;

import java.util.List;

import lombok.Data;

@Data
public class ForwardRfqVendorRequest {
	private List<String> rfqIds;
	private String sellerId;
	private String email;
}
