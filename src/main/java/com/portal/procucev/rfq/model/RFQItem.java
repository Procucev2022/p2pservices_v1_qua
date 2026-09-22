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

    // This class is annotated @JsonIgnoreProperties(ignoreUnknown = true), so any key the model
    // emits under a name we do not recognise is dropped with no error and no log line. The aliases
    // below cover the plausible name drift for the fields whose loss is not otherwise detectable.
    @com.fasterxml.jackson.annotation.JsonAlias({"item_description", "itemName", "item_name", "productName", "product_name", "material", "materialDescription"})
    private String itemDescription;

    @com.fasterxml.jackson.annotation.JsonAlias({"part_code", "partcode", "itemCode", "item_code", "materialCode", "material_code"})
    private String partCode;

    @com.fasterxml.jackson.annotation.JsonAlias({"part_number", "partNo", "part_no"})
    private String partNumber;

    @com.fasterxml.jackson.annotation.JsonAlias({"model_number", "modelNo", "model_no", "model"})
    private String modelNumber;

    @com.fasterxml.jackson.annotation.JsonAlias({"specifications", "specs", "spec", "technicalSpecification", "technical_specification", "specificationDetails", "size", "dimensions", "dimension"})
    private String specification;

    private Double quantity;

    @com.fasterxml.jackson.annotation.JsonAlias({"unitOfMeasure", "unit_of_measure", "unitofMeasures", "unit"})
    private String uom;

    @com.fasterxml.jackson.annotation.JsonAlias({"make", "Make", "MAKE", "manufacturer", "Manufacturer", "brandName", "brand_name", "preferredBrand", "preferred_brand"})
    private String brand;
    private String remarks;
    private String deliveryLocation;
    private String deliveryDate;

    private String category;
    private String division;
    private Double categoryConfidence;
    private String classificationStatus;

    @com.fasterxml.jackson.annotation.JsonSetter("quantity")
    @com.fasterxml.jackson.annotation.JsonAlias({"qty", "quantityRequired", "quantity_required", "requiredQuantity", "required_quantity", "orderQuantity", "order_quantity"})
    public void setQuantity(Object rawQty) {
        this.quantity = com.portal.procucev.rfq.util.QuantityNormalizer.normalize(rawQty);
        if ((this.uom == null || this.uom.isBlank()) && rawQty != null) {
            String extracted = com.portal.procucev.rfq.util.QuantityNormalizer.extractUom(rawQty.toString());
            if (extracted != null && !extracted.isBlank()) {
                this.uom = extracted;
            }
        }
    }

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
