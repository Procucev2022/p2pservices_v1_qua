package com.portal.procucev.rfq.service;

import com.portal.procucev.rfq.model.RFQItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryClassificationService {

    private final ExcelMasterDataLoader dataLoader;

    @Value("${app.category.classification.min-matched-confidence:0.85}")
    private double minMatchedConfidence = 0.85;

    public void classifyItems(List<RFQItem> items) {
        classifyItems(items, null);
    }

    public void classifyItems(List<RFQItem> items, String extractedCategory) {
        if (items == null || items.isEmpty()) return;

        List<ExcelMasterDataLoader.MasterCategoryRecord> masterRecords = dataLoader.getMasterRecords();

        for (RFQItem item : items) {
            classifySingleItem(item, masterRecords, extractedCategory);
        }
    }

    private void classifySingleItem(RFQItem item, List<ExcelMasterDataLoader.MasterCategoryRecord> masterRecords, String extractedCategory) {
        // Step 0: Check if item itself has an explicit non-generic category
        if (item.getCategory() != null && !item.getCategory().isBlank() && !isGenericCategory(item.getCategory())) {
            String cleanCat = item.getCategory().trim();
            item.setCategory(cleanCat);
            if (item.getDivision() == null || item.getDivision().isBlank()) {
                item.setDivision(cleanCat);
            }
            item.setCategoryConfidence(0.95);
            item.setClassificationStatus("AI_EXTRACTED");
            log.info("Classified item '{}' -> Category: '{}' (From item-level AI extraction)",
                    item.getItemDescription(), cleanCat);
            return;
        }

        if (extractedCategory != null && !extractedCategory.isBlank() && !extractedCategory.equalsIgnoreCase("null")) {
            String cleanCat = extractedCategory.trim();
            item.setCategory(cleanCat);
            item.setDivision(cleanCat);
            item.setCategoryConfidence(0.95);
            item.setClassificationStatus("AI_EXTRACTED");
            log.info("Classified item '{}' -> Category: '{}' (From explicit Email/AI extraction)",
                    item.getItemDescription(), cleanCat);
            return;
        }

        String desc = item.getItemDescription() != null ? item.getItemDescription().toLowerCase() : "";
        String spec = item.getSpecification() != null ? item.getSpecification().toLowerCase() : "";
        String brand = item.getBrand() != null ? item.getBrand().toLowerCase() : "";
        String remarks = item.getRemarks() != null ? item.getRemarks().toLowerCase() : "";
        String combined = desc + " " + spec + " " + brand + " " + remarks;

        // Step 1: Check Domain Keyword Map FIRST for high-accuracy category classification
        List<java.util.Map.Entry<String, String>> sortedKeywords = DOMAIN_KEYWORD_MAP.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getKey().length(), e1.getKey().length()))
                .toList();

        for (java.util.Map.Entry<String, String> entry : sortedKeywords) {
            if (containsKeyword(combined, entry.getKey())) {
                String catName = entry.getValue();
                item.setCategory(catName);
                item.setDivision(catName);
                item.setCategoryConfidence(0.90);
                item.setClassificationStatus("DOMAIN_KEYWORD_MATCHED");
                log.info("Classified item '{}' via domain keyword '{}' -> Category: '{}'",
                        item.getItemDescription(), entry.getKey(), catName);
                return;
            }
        }

        // Step 2: Excel Master Dataset Matching with Domain Protection
        ExcelMasterDataLoader.MasterCategoryRecord bestMatch = null;
        double highestScore = 0.0;

        for (ExcelMasterDataLoader.MasterCategoryRecord record : masterRecords) {
            double score = calculateMatchScore(combined, record);
            if (score > highestScore) {
                highestScore = score;
                bestMatch = record;
            }
        }

        if (bestMatch != null && highestScore >= minMatchedConfidence) {
            String category = bestMatch.getCategory();
            if (category != null && category.matches("^\\d+(\\.\\d+)?$")) {
                category = (bestMatch.getDivision() != null && !bestMatch.getDivision().isBlank() && !bestMatch.getDivision().matches("^\\d+(\\.\\d+)?$"))
                        ? bestMatch.getDivision() : "General Industrial Goods";
            }
            if (category == null || category.isBlank()) {
                category = "General Industrial Goods";
            }
            item.setCategory(category);
            item.setDivision(bestMatch.getDivision() != null ? bestMatch.getDivision() : "General Procurement");
            item.setCategoryConfidence(highestScore);
            item.setClassificationStatus("MATCHED");
            log.info("Classified item '{}' -> Category: '{}', Division: '{}' (Score: {})",
                    item.getItemDescription(), category, item.getDivision(), highestScore);
        } else {
            item.setCategory("General Industrial Goods");
            item.setDivision("General Procurement");
            item.setCategoryConfidence(0.5);
            item.setClassificationStatus("DEFAULT");
        }
    }

    private boolean isGenericCategory(String cat) {
        if (cat == null || cat.isBlank()) return true;
        String lower = cat.trim().toLowerCase();
        if (lower.matches(".*\\b(categories|category|multiple|various|mixed|different|several|all|general|not specified|n/a|null)\\b.*")) {
            return true;
        }
        if (lower.matches(".*\\d+\\s*(categories|category).*")) {
            return true;
        }
        return false;
    }

    private double calculateMatchScore(String text, ExcelMasterDataLoader.MasterCategoryRecord record) {
        String masterDesc = record.getItemDescription() != null ? record.getItemDescription().toLowerCase() : "";
        String masterCat = record.getCategory() != null ? record.getCategory().toLowerCase() : "";
        if (masterDesc.isBlank()) return 0.0;

        // Prevent cross-domain surgical/medical mismatch for IT monitors & electronics
        if ((containsKeyword(text, "computer") || containsKeyword(text, "pc") || containsKeyword(text, "it") || containsKeyword(text, "network")) &&
            (masterCat.contains("surgical") || masterCat.contains("medical") || masterCat.contains("pharma"))) {
            return 0.0;
        }

        double score = 0.0;
        if (text.contains(masterDesc) || masterDesc.contains(text)) {
            score += 0.7;
        }
        if (!masterCat.isBlank() && text.contains(masterCat)) {
            score += 0.3;
        }
        return score;
    }

    private static final java.util.Map<String, String> DOMAIN_KEYWORD_MAP = java.util.Map.ofEntries(
            // IT Hardware & Electronics
            java.util.Map.entry("laptop", "IT Hardware & Electronics"),
            java.util.Map.entry("laptops", "IT Hardware & Electronics"),
            java.util.Map.entry("computer", "IT Hardware & Electronics"),
            java.util.Map.entry("computers", "IT Hardware & Electronics"),
            java.util.Map.entry("desktop", "IT Hardware & Electronics"),
            java.util.Map.entry("desktops", "IT Hardware & Electronics"),
            java.util.Map.entry("notebooks", "Stationery & Office Supplies"),
            java.util.Map.entry("notebook", "IT Hardware & Electronics"),
            java.util.Map.entry("macbook", "IT Hardware & Electronics"),
            java.util.Map.entry("macbooks", "IT Hardware & Electronics"),
            java.util.Map.entry("pc", "IT Hardware & Electronics"),
            java.util.Map.entry("pcs", "IT Hardware & Electronics"),
            java.util.Map.entry("monitor", "IT Hardware & Electronics"),
            java.util.Map.entry("monitors", "IT Hardware & Electronics"),
            java.util.Map.entry("workstation", "IT Hardware & Electronics"),
            java.util.Map.entry("workstations", "IT Hardware & Electronics"),
            java.util.Map.entry("keyboard", "IT Hardware & Electronics"),
            java.util.Map.entry("keyboards", "IT Hardware & Electronics"),
            java.util.Map.entry("mouse", "IT Hardware & Electronics"),
            java.util.Map.entry("printer", "IT Hardware & Electronics"),
            java.util.Map.entry("printers", "IT Hardware & Electronics"),
            java.util.Map.entry("server", "IT Hardware & Electronics"),
            java.util.Map.entry("servers", "IT Hardware & Electronics"),
            java.util.Map.entry("cpu", "IT Hardware & Electronics"),
            java.util.Map.entry("cpus", "IT Hardware & Electronics"),
            java.util.Map.entry("ram", "IT Hardware & Electronics"),
            java.util.Map.entry("ssd", "IT Hardware & Electronics"),
            java.util.Map.entry("ssds", "IT Hardware & Electronics"),
            java.util.Map.entry("hard disk", "IT Hardware & Electronics"),

            // Networking Equipment
            java.util.Map.entry("switch", "Networking Equipment"),
            java.util.Map.entry("switches", "Networking Equipment"),
            java.util.Map.entry("router", "Networking Equipment"),
            java.util.Map.entry("routers", "Networking Equipment"),
            java.util.Map.entry("access point", "Networking Equipment"),
            java.util.Map.entry("access points", "Networking Equipment"),
            java.util.Map.entry("ethernet", "Networking Equipment"),
            java.util.Map.entry("firewall", "Networking Equipment"),
            java.util.Map.entry("firewalls", "Networking Equipment"),

            // Security & Surveillance
            java.util.Map.entry("cctv", "Security & Surveillance Equipment"),
            java.util.Map.entry("camera", "Security & Surveillance Equipment"),
            java.util.Map.entry("cameras", "Security & Surveillance Equipment"),
            java.util.Map.entry("surveillance", "Security & Surveillance Equipment"),

            // Stationery & Paper
            java.util.Map.entry("paper", "Stationery & Office Supplies"),
            java.util.Map.entry("pen", "Stationery & Office Supplies"),
            java.util.Map.entry("pens", "Stationery & Office Supplies"),
            java.util.Map.entry("stationery", "Stationery & Office Supplies"),

            // Appliances & Office Amenities
            java.util.Map.entry("dispenser", "Appliances & Office Amenities"),
            java.util.Map.entry("dispensers", "Appliances & Office Amenities"),
            java.util.Map.entry("cooler", "Appliances & Office Amenities"),
            java.util.Map.entry("coolers", "Appliances & Office Amenities"),

            // Medical & Healthcare Equipment
            java.util.Map.entry("medical", "Medical Equipment"),
            java.util.Map.entry("medical equipment", "Medical Equipment"),
            java.util.Map.entry("healthcare", "Medical Equipment"),
            java.util.Map.entry("patient monitor", "Medical Equipment"),

            // Construction
            java.util.Map.entry("cement", "Construction"),
            java.util.Map.entry("steel", "Construction"),
            java.util.Map.entry("pipe", "Construction"),
            java.util.Map.entry("pipes", "Construction"),
            java.util.Map.entry("cpvc", "Construction"),
            java.util.Map.entry("pvc", "Construction"),
            java.util.Map.entry("bricks", "Construction"),
            java.util.Map.entry("rebar", "Construction"),
            java.util.Map.entry("concrete", "Construction"),
            java.util.Map.entry("construction material", "Construction"),
            java.util.Map.entry("construction materials", "Construction"),

            // Industrial Machinery
            java.util.Map.entry("motor", "Industrial Machinery"),
            java.util.Map.entry("motors", "Industrial Machinery"),
            java.util.Map.entry("pump", "Industrial Machinery"),
            java.util.Map.entry("pumps", "Industrial Machinery"),
            java.util.Map.entry("valve", "Industrial Machinery"),
            java.util.Map.entry("valves", "Industrial Machinery"),
            java.util.Map.entry("bearing", "Industrial Machinery"),
            java.util.Map.entry("bearings", "Industrial Machinery"),
            java.util.Map.entry("compressor", "Industrial Machinery"),
            java.util.Map.entry("compressors", "Industrial Machinery"),
            java.util.Map.entry("generator", "Industrial Machinery"),
            java.util.Map.entry("generators", "Industrial Machinery"),

            // Safety Equipment
            java.util.Map.entry("helmet", "Safety Equipment"),
            java.util.Map.entry("helmets", "Safety Equipment"),
            java.util.Map.entry("safety", "Safety Equipment"),
            java.util.Map.entry("gloves", "Safety Equipment"),

            // Industrial Automation & Electrical
            java.util.Map.entry("electrical", "Industrial Automation & Electrical"),
            java.util.Map.entry("electrical equipment", "Industrial Automation & Electrical"),
            java.util.Map.entry("plc", "Industrial Automation & Electrical"),
            java.util.Map.entry("plc controller", "Industrial Automation & Electrical"),
            java.util.Map.entry("scada", "Industrial Automation & Electrical"),
            java.util.Map.entry("vfd", "Industrial Automation & Electrical"),
            java.util.Map.entry("inverter", "Industrial Automation & Electrical"),
            java.util.Map.entry("inverters", "Industrial Automation & Electrical"),
            java.util.Map.entry("contactor", "Industrial Automation & Electrical"),
            java.util.Map.entry("contactors", "Industrial Automation & Electrical"),
            java.util.Map.entry("relay", "Industrial Automation & Electrical"),
            java.util.Map.entry("relays", "Industrial Automation & Electrical"),
            java.util.Map.entry("circuit breaker", "Industrial Automation & Electrical"),
            java.util.Map.entry("circuit breakers", "Industrial Automation & Electrical"),

            // Instrumentation & Process Control
            java.util.Map.entry("pressure gauge", "Instrumentation & Process Control"),
            java.util.Map.entry("pressure gauges", "Instrumentation & Process Control"),
            java.util.Map.entry("gauge", "Instrumentation & Process Control"),
            java.util.Map.entry("gauges", "Instrumentation & Process Control"),
            java.util.Map.entry("transmitter", "Instrumentation & Process Control"),
            java.util.Map.entry("transmitters", "Instrumentation & Process Control"),
            java.util.Map.entry("sensor", "Instrumentation & Process Control"),
            java.util.Map.entry("sensors", "Instrumentation & Process Control"),
            java.util.Map.entry("flow meter", "Instrumentation & Process Control"),
            java.util.Map.entry("flow meters", "Instrumentation & Process Control"),
            java.util.Map.entry("thermocouple", "Instrumentation & Process Control"),
            java.util.Map.entry("thermocouples", "Instrumentation & Process Control"),

            // Office Furniture
            java.util.Map.entry("chair", "Office Furniture"),
            java.util.Map.entry("chairs", "Office Furniture"),
            java.util.Map.entry("desk", "Office Furniture"),
            java.util.Map.entry("desks", "Office Furniture"),
            java.util.Map.entry("table", "Office Furniture"),
            java.util.Map.entry("tables", "Office Furniture"),
            java.util.Map.entry("furniture", "Office Furniture"),

            // Batteries & Power Storage
            java.util.Map.entry("battery", "Batteries"),
            java.util.Map.entry("batteries", "Batteries"),
            java.util.Map.entry("12v", "Batteries"),
            java.util.Map.entry("amaron", "Batteries"),

            // Abrasives & Consumables
            java.util.Map.entry("abrasive", "Abrasives"),
            java.util.Map.entry("abrasives", "Abrasives"),
            java.util.Map.entry("fiber disc", "Abrasives"),
            java.util.Map.entry("60 grit", "Abrasives")
    );

    private boolean containsKeyword(String text, String keyword) {
        return Pattern.compile("(^|[^a-z0-9])" + Pattern.quote(keyword) + "([^a-z0-9]|$)").matcher(text).find();
    }

}
