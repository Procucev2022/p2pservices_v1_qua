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

        if (extractedRFQ.getDeliveryCity() != null && !extractedRFQ.getDeliveryCity().isBlank() && !extractedRFQ.getDeliveryCity().equalsIgnoreCase("Not Specified")) {
            city = extractedRFQ.getDeliveryCity().trim();
        }
        if (extractedRFQ.getDeliveryState() != null && !extractedRFQ.getDeliveryState().isBlank() && !extractedRFQ.getDeliveryState().equalsIgnoreCase("Not Specified")) {
            state = extractedRFQ.getDeliveryState().trim();
        }
        if (extractedRFQ.getDeliveryPincode() != null && !extractedRFQ.getDeliveryPincode().isBlank() && !extractedRFQ.getDeliveryPincode().equalsIgnoreCase("Not Specified")) {
            pincode = extractedRFQ.getDeliveryPincode().trim();
        }

        String locStr = extractedRFQ.getDeliveryLocation() != null ? extractedRFQ.getDeliveryLocation().trim() : "";
        if (locStr.equalsIgnoreCase("Not Specified") || locStr.equalsIgnoreCase("NotSpecified") || locStr.equalsIgnoreCase("N/A")) {
            locStr = "";
        }

        boolean isRegisteredAddressFallback = locStr.isBlank()
                || locStr.equalsIgnoreCase("Registered Profile Address")
                || locStr.equalsIgnoreCase("Email Delivery Location")
                || (buyer != null && buyer.getAddress() != null && locStr.equalsIgnoreCase(buyer.getAddress().trim()));

        boolean hasEmailLocation = !isRegisteredAddressFallback && (
                !locStr.isBlank()
                || !city.isBlank()
                || !state.isBlank()
                || !pincode.isBlank()
        );

        String address = locStr;

        if (hasEmailLocation) {
            // Email contains location information: ONLY extract fields present in email text!
            // DO NOT fallback to buyer default profile!
            if (!locStr.isBlank()) {
                String cleanLoc = locStr.replaceAll("[^\\x00-\\x7F]", " ");
                
                // Extract 6-digit Indian pincode if explicitly present in email location text
                if (pincode.isBlank()) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d{6})\\b").matcher(cleanLoc);
                    if (m.find()) {
                        pincode = m.group(1);
                    }
                }

                // Scan known city names in locStr if city is blank
                if (city.isBlank()) {
                    String lowerLoc = cleanLoc.toLowerCase();
                    for (String knownCity : CITY_PIN_MAP.keySet()) {
                        if (lowerLoc.contains(knownCity)) {
                            city = knownCity.substring(0, 1).toUpperCase() + knownCity.substring(1);
                            break;
                        }
                    }
                }

                // Extract state if explicitly present in email location text
                if (state.isBlank()) {
                    String lowerLoc = cleanLoc.toLowerCase();
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
                    else if (lowerLoc.contains("kerala")) state = "Kerala";
                    else if (lowerLoc.contains("punjab")) state = "Punjab";
                    else if (lowerLoc.contains("haryana")) state = "Haryana";
                    else if (lowerLoc.contains("bihar")) state = "Bihar";
                    else if (lowerLoc.contains("odisha")) state = "Odisha";
                    else if (lowerLoc.contains("assam")) state = "Assam";
                    else if (lowerLoc.contains("jharkhand")) state = "Jharkhand";
                }

                // Extract city from location text if city not explicitly provided by AI or known city scan
                if (city.isBlank()) {
                    String[] tokens = locStr.split("[,\\-–—\\n]");
                    if (tokens.length > 0 && !tokens[0].trim().isBlank()) {
                        String firstToken = tokens[0].trim();
                        boolean isStateName = state != null && firstToken.equalsIgnoreCase(state);
                        if (!firstToken.matches("^\\d+$") && !isStateName) {
                            city = firstToken;
                        }
                    }
                }
            }
        } else {
            // CASE 1: ALL location fields missing from email -> Complete Buyer default location
            if (buyer != null) {
                if (city.isBlank() && buyer.getCity() != null && !buyer.getCity().isBlank()) {
                    city = buyer.getCity().trim();
                }
                if (state.isBlank() && buyer.getState() != null && !buyer.getState().isBlank()) {
                    state = buyer.getState().trim();
                }
                if (pincode.isBlank() && buyer.getPincode() != null && !buyer.getPincode().isBlank()) {
                    pincode = buyer.getPincode().trim();
                }
                if ((address.isBlank() || isRegisteredAddressFallback) && buyer.getAddress() != null && !buyer.getAddress().isBlank()) {
                    address = buyer.getAddress().trim();
                }
            }
            if (address.isBlank() || address.equalsIgnoreCase("Not Specified")) {
                address = "Registered Profile Address";
            }
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

                String partCodeVal = sanitizeText(item.getEffectivePartNumber());

                String cleanItemDesc = item.getItemDescription() != null && !item.getItemDescription().isBlank()
                        ? sanitizeText(item.getItemDescription())
                        : primaryDescription;

                // Technical detail only. The brand is deliberately NOT folded in here: this value
                // is surfaced to buyers and vendors as the "Specification" column, so a brand name
                // appearing in it is the defect this block exists to prevent.
                String specs;
                if (item.getSpecification() != null && !item.getSpecification().isBlank()
                        && !item.getSpecification().equalsIgnoreCase("Not Specified")
                        && !item.getSpecification().equalsIgnoreCase("null")) {
                    specs = sanitizeText(item.getSpecification());
                } else if (item.getRemarks() != null && !item.getRemarks().isBlank()) {
                    specs = sanitizeText(item.getRemarks());
                } else {
                    // The description carries the identifying size/dimension token, so it is the
                    // best available technical fallback. Never leave the specification blank.
                    specs = cleanItemDesc;
                }

                // Surface the part / model number in the specification too when it is not already
                // present. It stays in itemcode as well; buyers asked to see it as a spec detail.
                if (!partCodeVal.isBlank()
                        && !specs.toLowerCase().contains(partCodeVal.toLowerCase())) {
                    specs = specs.isBlank() ? "P/N: " + partCodeVal : "P/N: " + partCodeVal + ", " + specs;
                }

                if (specs.isBlank()) {
                    specs = primaryDescription;
                }

                if (specs.length() > 200) {
                    specs = specs.substring(0, 200).replaceAll("[,.-]+$", "").trim();
                }

                // Legacy column naming: the rfq_items table has no "specification" column. The
                // platform presents rfq_items.brand as "Specification" and rfq_items.remarks as
                // "Remarks" - see the RFQ tables in MailUtility and the Specification/Remarks
                // column mapping in GMTServiceImpl's manual Excel upload. The DTO field names
                // mirror the columns, not the labels, so the technical detail goes to brand() and
                // the brand name goes to remarks().
                rfqItemsList.add(RFQRequest.RfqItemDto.builder()
                        .brand(specs)
                        .unitofMeasures(item.getUom() != null && !item.getUom().isBlank() ? sanitizeText(item.getUom()) : "Nos")
                        .quantity(qty)
                        .description(cleanItemDesc)
                        .category(item.getCategory())
                        .createdBy(buyer.getName())
                        .createdTS(nowIso)
                        .itemcode(partCodeVal)
                        .serialNo(serialNo++)
                        .remarks(brandVal)
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

        // The line-item count is logged because nothing between extraction and persistence recorded
        // it, which made "the RFQ only has one line" impossible to attribute from the logs alone.
        log.info("Built RFQ Request Payload: RFQ Number={}, createdBy={}, projectDesc='{}', lineItems={}",
                request.getRfqNumber(), request.getCreatedBy(), request.getProjectDesc(), rfqItemsList.size());

        return request;
    }

    /**
     * Strips text down to ASCII for downstream storage.
     *
     * <p>Common typographic characters are transliterated first. Without this, the blanket
     * non-ASCII replacement turned a dimension typed as {@code 40×52×7} (U+00D7, which is what
     * Word, Excel and most mail clients autocorrect {@code x} into) into {@code 40-52-7}, and
     * curly quotes or dashes in a specification into a run of hyphens.
     */
    private String sanitizeText(String input) {
        if (input == null) return "";
        String transliterated = input
                .replace('\u00D7', 'x')   // × multiplication sign
                .replace('\u2715', 'x')   // ✕ multiplication x
                .replace('\u2716', 'x')   // ✖ heavy multiplication x
                .replace('\u00A0', ' ')   // non-breaking space
                .replace('\u2013', '-')   // – en dash
                .replace('\u2014', '-')   // — em dash
                .replace('\u2018', '\'')  // ' left single quote
                .replace('\u2019', '\'')  // ' right single quote
                .replace('\u201C', '"')   // " left double quote
                .replace('\u201D', '"')   // " right double quote
                .replace("\u00B5", "u")   // µ micro
                .replace("\u2032", "'")   // ′ prime
                .replace("\u2033", "\""); // ″ double prime
        return transliterated.replaceAll("[^\\x00-\\x7F]", "-").replaceAll("\\s+", " ").trim();
    }


}
