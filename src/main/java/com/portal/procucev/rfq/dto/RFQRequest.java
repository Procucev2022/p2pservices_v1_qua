package com.portal.procucev.rfq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RFQRequest {

    private String createdBy;
    private String projectDesc;
    private String deliveryDate;
    private boolean noPrFlag;
    private OrgRef org;
    private String user;
    private String sourceType;
    private String remarks;
    private List<LocationDto> clientdeliverylocationrfq;
    private List<RfqItemDto> rfqItem;
    private List<Object> vendors;
    private List<Map<String, String>> rfqDocument;

    private transient String rfqNumber;
    private transient String buyerEmail;
    private transient String token;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrgRef {
        private String id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDto {
        private String address;
        private String city;
        private String state;
        private String pincode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RfqItemDto {
        private String brand;
        private String unitofMeasures;
        private int quantity;
        private String description;
        private String category;
        private String createdBy;
        private String createdTS;
        private String itemcode;
        private int serialNo;
        private String remarks;
    }
}
