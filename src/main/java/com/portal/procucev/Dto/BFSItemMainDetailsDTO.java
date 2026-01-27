package com.portal.procucev.Dto;
import lombok.Data;
@Data
public class BFSItemMainDetailsDTO {

    private String id;
    private String description;
    private String specification;
    private Double totalQuantity;
    private Double availableQuantity;
    private String category;
    private String itemNumber;
    private String location;
    private String ageOfAsset;
    private String unitofMeasures;
    private Double sellPrice;
    private Double discount;
    private Double askPrice;
    private String bfsGroup;
    private Boolean buyPriceDisclosure;
    private String remarks;
    private Boolean imagesFlag;

    // 🔴 MUST MATCH Hibernate types & order EXACTLY
    public BFSItemMainDetailsDTO(
            String id,
            String description,
            String specification,
            Double totalQuantity,
            Double availableQuantity,
            String category,
            String itemNumber,
            String location,
            String ageOfAsset,
            String unitofMeasures,
            Double sellPrice,
            Double discount,
            Double askPrice,
            String bfsGroup,
            Boolean buyPriceDisclosure,
            String remarks,
            Boolean imagesFlag
    ) {
        this.id = id;
        this.description = description;
        this.specification = specification;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
        this.category = category;
        this.itemNumber = itemNumber;
        this.location = location;
        this.ageOfAsset = ageOfAsset;
        this.unitofMeasures = unitofMeasures;
        this.sellPrice = sellPrice;
        this.discount = discount;
        this.askPrice = askPrice;
        this.bfsGroup = bfsGroup;
        this.buyPriceDisclosure = buyPriceDisclosure;
        this.remarks = remarks;
        this.imagesFlag = imagesFlag;
    }
}
