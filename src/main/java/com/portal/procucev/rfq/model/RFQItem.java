package com.portal.procucev.rfq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RFQItem {
    private String itemDescription;
    private String partCode;
    private String partNumber;
    private String modelNumber;
    private String specification;
    private Double quantity;
    private String uom;
    private String brand;
    private String remarks;
    private String deliveryLocation;
    private String deliveryDate;

    private String category;
    private String division;
    private Double categoryConfidence;
    private String classificationStatus;

    @com.fasterxml.jackson.annotation.JsonProperty("description")
    public void setDescriptionAlias(String description) {
        if (this.itemDescription == null || this.itemDescription.isBlank()) {
            this.itemDescription = description;
        }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("delivery_date")
    public void setDeliveryDateAlias(String deliveryDate) {
        if (this.deliveryDate == null || this.deliveryDate.isBlank()) {
            this.deliveryDate = deliveryDate;
        }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("delivery_location")
    public void setDeliveryLocationAlias(String deliveryLocation) {
        if (this.deliveryLocation == null || this.deliveryLocation.isBlank()) {
            this.deliveryLocation = deliveryLocation;
        }
    }

    public String getEffectivePartNumber() {
        if (partCode != null && !partCode.isBlank()) return partCode;
        if (partNumber != null && !partNumber.isBlank()) return partNumber;
        if (modelNumber != null && !modelNumber.isBlank()) return modelNumber;
        return "";
    }
}
