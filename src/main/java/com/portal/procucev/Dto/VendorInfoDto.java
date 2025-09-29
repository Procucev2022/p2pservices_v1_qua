package com.portal.procucev.Dto;

import java.util.List;

import lombok.Data;

@Data
public class VendorInfoDto {
	private String companyName;
    private String organizationPhonenumber;
    private String email;
    private String gstin;
    private String details;
    private boolean india;
    private String zipCode;
    private boolean isWebApp;
    private String sourceType;
    private List<String> categories;
}
