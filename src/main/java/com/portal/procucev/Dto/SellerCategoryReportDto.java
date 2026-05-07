package com.portal.procucev.Dto;


import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SellerCategoryReportDto {

    @JsonProperty("CompanyName")
    private String companyName;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("MobileNo")
    private String mobileNo;

    @JsonProperty("SellerPersonName")
    private String sellerPersonName;

    @JsonProperty("LastLoginDate")
    private Date lastLoginDate;

    @JsonProperty("Category1")
    private String category1;

    @JsonProperty("Category2")
    private String category2;

    @JsonProperty("Category3")
    private String category3;

    @JsonProperty("Category4")
    private String category4;

    @JsonProperty("Category5")
    private String category5;


    public SellerCategoryReportDto(String companyName,
                                   String email,
                                   String mobileNo,
                                   String sellerPersonName,
                                   Date lastLoginDate) {

        this.companyName = companyName;
        this.email = email;
        this.mobileNo = mobileNo;
        this.sellerPersonName = sellerPersonName;
        this.lastLoginDate = lastLoginDate;
    }
}