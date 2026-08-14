package com.portal.procucev.rfq.service;

import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Files;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RFQBuilderService {

    private final DateParser dateParser;
    private final PincodeDao pincodeDao;

    private static final Map<String, String> CITY_PIN_MAP = Map.ofEntries(
            Map.entry("raipur", "492001"),
            Map.entry("hyderabad", "500001"),
            Map.entry("secunderabad", "500003"),
            Map.entry("bangalore", "560001"),
            Map.entry("bengaluru", "560001"),
            Map.entry("chennai", "600001"),
            Map.entry("mumbai", "400001"),
            Map.entry("pune", "411001"),
            Map.entry("delhi", "110001"),
            Map.entry("new delhi", "110001"),
            Map.entry("gurugram", "122001"),
            Map.entry("noida", "201301"),
            Map.entry("kolkata", "700001"),
            Map.entry("ahmedabad", "380001"),
            Map.entry("surat", "395001"),
            Map.entry("jaipur", "302001"),
            Map.entry("bhopal", "462001"),
            Map.entry("indore", "452001"),
            Map.entry("visakhapatnam", "530001"),
            Map.entry("vijayawada", "520001"),
            Map.entry("kakinada", "533431")
    );

    public RFQRequest buildRFQRequest(ExtractedRFQ extractedRFQ, Buyer buyer, String rawSubject, List<File> attachmentFiles) {
        String rfqNumber = CommonUtil.generateUniqueRfqNumber();
        log.info("Generating unique RFQ Number ONCE: {}", rfqNumber);

        boolean isMultipleItems = extractedRFQ.getItems() != null && extractedRFQ.getItems().size() > 1;

        String primaryDescription = "RFQ Requirement";
        if (extractedRFQ.getItems() != null && !extractedRFQ.getItems().isEmpty()) {
            if (isMultipleItems) {
                String firstDesc = extractedRFQ.getItems().get(0).getItemDescription();
                if (firstDesc != null && !firstDesc.isBlank()) {
                    String clean = sanitizeText(firstDesc);
                    primaryDescription = clean.toLowerCase().endsWith("s") ? clean : clean + "s";
                } else {
                    primaryDescription = "Procurement Items";
                }
            } else {
                String desc = extractedRFQ.getItems().get(0).getItemDescription();
                if (desc != null && !desc.isBlank()) {
                    primaryDescription = sanitizeText(desc);
                }
            }
        }
        if (primaryDescription.length() > 100) {
            primaryDescription = primaryDescription.substring(0, 100).trim();
        }

        String deliveryDate = dateParser.parseDateString(extractedRFQ.getDeliveryDate());

        String city = "";
        String state = "";
        String pincode = "";

        if (extractedRFQ.getDeliveryCity() != null && !extractedRFQ.getDeliveryCity().isBlank()) {
            city = extractedRFQ.getDeliveryCity().trim();
        }
        if (extractedRFQ.getDeliveryState() != null && !extractedRFQ.getDeliveryState().isBlank()) {
            state = extractedRFQ.getDeliveryState().trim();
        }
        if (extractedRFQ.getDeliveryPincode() != null && !extractedRFQ.getDeliveryPincode().isBlank()) {
            pincode = extractedRFQ.getDeliveryPincode().trim();
        }

        String locStr = extractedRFQ.getDeliveryLocation() != null ? extractedRFQ.getDeliveryLocation().trim() : "";
        if (locStr.equalsIgnoreCase("Not Specified") || locStr.equalsIgnoreCase("NotSpecified") || locStr.equalsIgnoreCase("N/A")) {
            locStr = "";
        }
        if (!locStr.isBlank()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d{6})\\b").matcher(locStr);
            if (m.find()) {
                pincode = m.group(1);
            }
        }

        if (locStr.contains("560037") || pincode.startsWith("560")) {
            city = "Bangalore";
            state = "Karnataka";
            if (pincode.isBlank()) pincode = "560037";
        } else if (!locStr.isBlank()) {
            String lowerLoc = locStr.toLowerCase();
            for (String knownCity : CITY_PIN_MAP.keySet()) {
                if (lowerLoc.contains(knownCity)) {
                    city = knownCity.substring(0, 1).toUpperCase() + knownCity.substring(1);
                    if (pincode.isBlank()) {
                        pincode = CITY_PIN_MAP.get(knownCity);
                    }
                    break;
                }
            }

            if (lowerLoc.contains("chhattisgarh")) state = "Chhattisgarh";
            else if (lowerLoc.contains("karnataka")) state = "Karnataka";
            else if (lowerLoc.contains("telangana")) state = "Telangana";
            else if (lowerLoc.contains("andhra")) state = "Andhra Pradesh";
            else if (lowerLoc.contains("maharashtra")) state = "Maharashtra";
            else if (lowerLoc.contains("tamil nadu") || lowerLoc.contains("tamilnadu")) state = "Tamil Nadu";
            else if (lowerLoc.contains("delhi")) state = "Delhi";
            else if (lowerLoc.contains("gujarat")) state = "Gujarat";
            else if (lowerLoc.contains("west bengal") || lowerLoc.contains("bengal")) state = "West Bengal";
            else if (lowerLoc.contains("rajasthan")) state = "Rajasthan";
            else if (lowerLoc.contains("madhya pradesh")) state = "Madhya Pradesh";
            else if (lowerLoc.contains("uttar pradesh")) state = "Uttar Pradesh";
        }

        if (pincode.isBlank() && !city.isBlank()) {
            pincode = CITY_PIN_MAP.getOrDefault(city.toLowerCase(), "");
        }

        if (!city.isBlank() && (state.isBlank() || pincode.isBlank())) {
            try {
                PincodeData pinData = pincodeDao.findByCityIgnoreCase(city);
                if (pinData != null) {
                    if (state.isBlank() && pinData.getState() != null && !pinData.getState().isBlank()) {
                        state = pinData.getState().trim();
                    }
                    if (pincode.isBlank() && pinData.getPincode() != null && !pinData.getPincode().isBlank()) {
                        pincode = pinData.getPincode().trim();
                    }
                }
            } catch (Exception e) {
                log.warn("Could not query pincodeDao for city {}: {}", city, e.getMessage());
            }
        }

        if (state.isBlank() && !pincode.isBlank()) {
            try {
                PincodeData pinData = pincodeDao.findByPincode(pincode);
                if (pinData != null && pinData.getState() != null && !pinData.getState().isBlank()) {
                    state = pinData.getState().trim();
                    if (city.isBlank() && pinData.getCity() != null) {
                        city = pinData.getCity().trim();
                    }
                }
            } catch (Exception e) {
                log.warn("Could not query pincodeDao for pincode {}: {}", pincode, e.getMessage());
            }
        }

        String address = locStr;
        if ((address.isBlank() || address.equalsIgnoreCase("Not Specified")) && buyer != null && buyer.getAddress() != null && !buyer.getAddress().isBlank()) {
            address = buyer.getAddress().trim();
        }

        // Fallback to Buyer's Registered Profile Address if still missing
        if ((city.isBlank() || city.equalsIgnoreCase("Not Specified")) && buyer != null && buyer.getCity() != null && !buyer.getCity().isBlank()) {
            city = buyer.getCity().trim();
        }
        if ((state.isBlank() || state.equalsIgnoreCase("Not Specified")) && buyer != null && buyer.getState() != null && !buyer.getState().isBlank()) {
            state = buyer.getState().trim();
        }
        if ((pincode.isBlank() || pincode.equalsIgnoreCase("Not Specified")) && buyer != null && buyer.getPincode() != null && !buyer.getPincode().isBlank()) {
            pincode = buyer.getPincode().trim();
        }
        if (address.isBlank() || address.equalsIgnoreCase("Not Specified")) {
            address = "Registered Profile Address";
        }

        List<RFQRequest.LocationDto> locations = List.of(
                RFQRequest.LocationDto.builder()
                        .address(address)
                        .city(city)
                        .state(state)
                        .pincode(pincode)
                        .build()
        );

        List<Map<String, String>> rfqDocuments = new ArrayList<>();
        if (attachmentFiles != null) {
            for (File file : attachmentFiles) {
                if (file != null && file.exists() && file.length() > 0) {
                    try {
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        String b64 = Base64.getEncoder().encodeToString(bytes);
                        Map<String, String> docMap = new HashMap<>();
                        docMap.put("fileName", file.getName());
                        docMap.put("file", b64);
                        rfqDocuments.add(docMap);
                    } catch (Exception e) {
                        log.error("Error encoding attachment {}: {}", file.getName(), e.getMessage());
                    }
                }
            }
        }

        List<RFQRequest.RfqItemDto> rfqItemsList = new ArrayList<>();
        int serialNo = 1001;
        String nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        if (extractedRFQ.getItems() != null && !extractedRFQ.getItems().isEmpty()) {
            for (RFQItem item : extractedRFQ.getItems()) {
                String rawBrand = item.getBrand();
                String brandVal = rawBrand != null ? sanitizeText(rawBrand) : "";
                if (brandVal.isBlank() || brandVal.equalsIgnoreCase("null") || brandVal.equalsIgnoreCase("Not Specified")) {
                    brandVal = "Brand: Not Specified";
                } else if (!brandVal.startsWith("Brand:")) {
                    brandVal = "Brand: " + brandVal.trim();
                }
                if (brandVal.length() > 50) {
                    brandVal = brandVal.substring(0, 50).trim();
                }

                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Quantity is mandatory for item: " + (item.getItemDescription() != null ? item.getItemDescription() : "RFQ Item"));
                }
                double qty = item.getQuantity();
                String qtyDisplay = formatQuantity(qty);

                String partCodeVal = sanitizeText(item.getEffectivePartNumber());

                String specs;
                if (isMultipleItems) {
                    if (item.getSpecification() != null && !item.getSpecification().isBlank()) {
                        specs = sanitizeText(item.getSpecification());
                    } else if (item.getRemarks() != null && !item.getRemarks().isBlank()) {
                        specs = sanitizeText(item.getRemarks());
                    } else {
                        String bStr = item.getBrand() != null && !item.getBrand().equalsIgnoreCase("null") ? item.getBrand().trim() : "";
                        String dStr = item.getItemDescription() != null ? item.getItemDescription().trim() : primaryDescription;
                        specs = sanitizeText(bStr + " " + dStr + " - " + qtyDisplay + " Units");
                    }
                } else {
                    specs = item.getSpecification() != null && !item.getSpecification().isBlank()
                            ? sanitizeText(item.getSpecification())
                            : (item.getRemarks() != null ? sanitizeText(item.getRemarks()) : primaryDescription);
                }

                if (specs.length() > 200) {
                    specs = specs.substring(0, 200).replaceAll("[,.-]+$", "").trim();
                }

                String cleanDesc = item.getItemDescription() != null && !item.getItemDescription().isBlank()
                        ? sanitizeText(item.getItemDescription())
                        : primaryDescription;

                rfqItemsList.add(RFQRequest.RfqItemDto.builder()
                        .brand(brandVal)
                        .unitofMeasures(item.getUom() != null && !item.getUom().isBlank() ? sanitizeText(item.getUom()) : "Nos")
                        .quantity(qty)
                        .description(cleanDesc)
                        .category(item.getCategory())
                        .createdBy(buyer.getName())
                        .createdTS(nowIso)
                        .itemcode(partCodeVal)
                        .serialNo(serialNo++)
                        .remarks(specs)
                        .build());
            }
        }

        String orgIdVal = (buyer != null && buyer.getOrgId() != null && !buyer.getOrgId().isBlank()) ? buyer.getOrgId() : "1";
        String userIdVal = (buyer != null && buyer.getUserId() != null && !buyer.getUserId().isBlank()) ? buyer.getUserId() : "1";

        RFQRequest request = RFQRequest.builder()
                .createdBy(buyer != null && buyer.getName() != null ? buyer.getName() : "User")
                .projectDesc(primaryDescription)
                .deliveryDate(deliveryDate)
                .noPrFlag(true)
                .org(RFQRequest.OrgRef.builder().id(orgIdVal).build())
                .user(userIdVal)
                .sourceType("T")
                .remarks("")
                .clientdeliverylocationrfq(locations)
                .rfqItem(rfqItemsList)
                .vendors(new ArrayList<>())
                .rfqDocument(rfqDocuments)
                .rfqNumber(rfqNumber)
                .buyerEmail(buyer != null ? buyer.getEmail() : null)
                .token(buyer != null ? buyer.getToken() : null)
                .build();

        log.info("Built RFQ Request Payload: RFQ Number={}, createdBy={}, projectDesc='{}'",
                request.getRfqNumber(), request.getCreatedBy(), request.getProjectDesc());

        return request;
    }

    private String sanitizeText(String input) {
        if (input == null) return "";
        return input.replaceAll("[^\\x00-\\x7F]", "-").replaceAll("\\s+", " ").trim();
    }

    private String formatQuantity(double quantity) {
        return quantity == Math.rint(quantity)
                ? String.valueOf((long) quantity)
                : String.valueOf(quantity);
    }
}
