package com.portal.procucev.Dto;


import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class OrganizationUpdateRequest {
    private String companyName;
    private String pan;
    private String gstin;
    private String address1;
    private String address2;
    private String country;
    private String state;
    private String city;
    private String zipCode;
    private String contactPerson;
	private String details;
    private String email;
    private String organizationPhonenumber;
    private String website;

    private List<DivisionCategoryDTO> divisionCategories;
    private List<BranchDTO> branches;
    private List<SubscriptionPlanDTO> subscriptionPlans;

    @Data
    public static class DivisionCategoryDTO {
        private String division;
        private String category;
    }

    @Data
    public static class BranchDTO {
        private String branchName;
        private String contactPerson;
        private String email;
        private String address;
    }

    @Data
    public static class SubscriptionPlanDTO {
        private String planType;
        private String planName;
        private String features;
        private double price;
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean active;
    }
}
