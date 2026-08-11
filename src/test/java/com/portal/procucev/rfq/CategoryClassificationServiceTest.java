package com.portal.procucev.rfq;

import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.service.CategoryClassificationService;
import com.portal.procucev.rfq.service.ExcelMasterDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CategoryClassificationServiceTest {

    private ExcelMasterDataLoader dataLoader;
    private CategoryClassificationService service;

    @BeforeEach
    void setUp() {
        dataLoader = Mockito.mock(ExcelMasterDataLoader.class);
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(new ArrayList<>());
        service = new CategoryClassificationService(dataLoader);
    }

    @Test
    @DisplayName("Test classifyItems with null or empty list")
    void testClassifyItemsNullOrEmpty() {
        assertDoesNotThrow(() -> service.classifyItems(null));
        assertDoesNotThrow(() -> service.classifyItems(List.of()));
    }

    @Test
    @DisplayName("Test classifyItems with explicit extracted category")
    void testClassifyItemsExplicitCategory() {
        RFQItem item = RFQItem.builder().itemDescription("Random Item").build();
        service.classifyItems(List.of(item), "Electrical Equipment");

        assertEquals("Electrical Equipment", item.getCategory());
        assertEquals("Electrical Equipment", item.getDivision());
        assertEquals(0.95, item.getCategoryConfidence());
        assertEquals("AI_EXTRACTED", item.getClassificationStatus());
    }

    @Test
    @DisplayName("Test classifyItems with domain keywords (laptops, monitors, valves)")
    void testClassifyItemsDomainKeywords() {
        RFQItem laptop = RFQItem.builder().itemDescription("Dell Laptops").build();
        RFQItem monitor = RFQItem.builder().itemDescription("Dell Monitor").build();
        RFQItem valve = RFQItem.builder().itemDescription("Industrial Valve").build();

        service.classifyItems(List.of(laptop, monitor, valve));

        assertEquals("IT Hardware & Electronics", laptop.getCategory());
        assertEquals("DOMAIN_KEYWORD_MATCHED", laptop.getClassificationStatus());

        assertEquals("IT Hardware & Electronics", monitor.getCategory());
        assertEquals("DOMAIN_KEYWORD_MATCHED", monitor.getClassificationStatus());

        assertEquals("Industrial Machinery", valve.getCategory());
        assertEquals("DOMAIN_KEYWORD_MATCHED", valve.getClassificationStatus());
    }

    @Test
    @DisplayName("Test classifyItems Excel master dataset match (High Score)")
    void testClassifyItemsExcelMasterMatch() {
        ExcelMasterDataLoader.MasterCategoryRecord record1 = new ExcelMasterDataLoader.MasterCategoryRecord();
        record1.setItemDescription("Specialized Heavy Crane Unit 99");
        record1.setCategory("Heavy Machinery");
        record1.setDivision("Mechanical Engineering");

        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(record1));

        RFQItem item = RFQItem.builder().itemDescription("Specialized Heavy Crane Unit 99").specification("Heavy Machinery").build();
        service.classifyItems(List.of(item));

        assertEquals("Heavy Machinery", item.getCategory());
        assertEquals("Mechanical Engineering", item.getDivision());
        assertEquals("MATCHED", item.getClassificationStatus());
        assertTrue(item.getCategoryConfidence() >= 0.85);
    }


    @Test
    @DisplayName("Test classifyItems Excel master numeric category fallback")
    void testClassifyItemsNumericCategoryFallback() {
        ExcelMasterDataLoader.MasterCategoryRecord record1 = new ExcelMasterDataLoader.MasterCategoryRecord();
        record1.setItemDescription("Unmatched Unique Equipment XYZ");
        record1.setCategory("123.45"); // Numeric
        record1.setDivision("Custom Division");

        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(record1));

        RFQItem item = RFQItem.builder().itemDescription("Unmatched Unique Equipment XYZ").specification("123.45").build();
        service.classifyItems(List.of(item));

        assertEquals("Custom Division", item.getCategory());
        assertEquals("MATCHED", item.getClassificationStatus());
    }


    @Test
    @DisplayName("Test classifyItems cross-domain medical IT protection and default fallback")
    void testClassifyItemsCrossDomainProtectionAndDefaultFallback() {
        ExcelMasterDataLoader.MasterCategoryRecord medicalRecord = new ExcelMasterDataLoader.MasterCategoryRecord();
        medicalRecord.setItemDescription("Computer Surgical Monitor");
        medicalRecord.setCategory("Surgical Equipment");

        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(medicalRecord));

        RFQItem item = RFQItem.builder().itemDescription("Custom IT Computer Monitor").build();
        service.classifyItems(List.of(item));

        // Keyword domain map catches monitor first
        assertEquals("IT Hardware & Electronics", item.getCategory());
    }

    @Test
    @DisplayName("Test classifyItems default fallback when no keywords or master records match")
    void testClassifyItemsDefaultFallback() {
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of());

        RFQItem item = RFQItem.builder().itemDescription("Unrecognized Widget XYZ123").build();
        service.classifyItems(List.of(item));

        assertEquals("General Industrial Goods", item.getCategory());
        assertEquals("General Procurement", item.getDivision());
        assertEquals(0.5, item.getCategoryConfidence());
        assertEquals("DEFAULT", item.getClassificationStatus());
    }

    @Test
    @DisplayName("Test classifyItems with null extractedCategory string (should not be AI_EXTRACTED)")
    void testClassifyItemsNullExtractedCategory() {
        RFQItem item = RFQItem.builder().itemDescription("Unrecognized Widget XYZ123").build();
        service.classifyItems(List.of(item), null);
        assertEquals("DEFAULT", item.getClassificationStatus());
    }

    @Test
    @DisplayName("Test classifyItems with blank extractedCategory string")
    void testClassifyItemsBlankExtractedCategory() {
        RFQItem item = RFQItem.builder().itemDescription("Unrecognized Widget XYZ123").build();
        service.classifyItems(List.of(item), "   ");
        assertEquals("DEFAULT", item.getClassificationStatus());
    }

    @Test
    @DisplayName("Test classifyItems with 'null' string extractedCategory")
    void testClassifyItemsNullStringExtractedCategory() {
        RFQItem item = RFQItem.builder().itemDescription("Unrecognized Widget XYZ123").build();
        service.classifyItems(List.of(item), "null");
        assertEquals("DEFAULT", item.getClassificationStatus());
    }

    @Test
    @DisplayName("Test classifyItems excel match with null category falls back to General Industrial Goods")
    void testClassifyItemsNullCategoryExcelMatch() {
        ExcelMasterDataLoader.MasterCategoryRecord record = new ExcelMasterDataLoader.MasterCategoryRecord();
        record.setItemDescription("Unmatched Special Device ABC 1");
        record.setCategory(null);
        record.setDivision("Custom Division");

        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(record));

        RFQItem item = RFQItem.builder().itemDescription("Unmatched Special Device ABC 1").specification("Unmatched Special Device ABC 1").build();
        service.classifyItems(List.of(item));

        assertEquals("General Industrial Goods", item.getCategory());
        assertEquals("DEFAULT", item.getClassificationStatus());
    }

    @Test
    @DisplayName("Test classifyItems excel match with blank category falls back to General Industrial Goods")
    void testClassifyItemsBlankCategoryExcelMatch() {
        ExcelMasterDataLoader.MasterCategoryRecord record = new ExcelMasterDataLoader.MasterCategoryRecord();
        record.setItemDescription("Unmatched Special Device ABC 2");
        record.setCategory("");
        record.setDivision("Custom Division");

        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(record));

        RFQItem item = RFQItem.builder().itemDescription("Unmatched Special Device ABC 2").specification("Unmatched Special Device ABC 2").build();
        service.classifyItems(List.of(item));

        assertEquals("General Industrial Goods", item.getCategory());
        assertEquals("DEFAULT", item.getClassificationStatus());
    }


    @Test
    @DisplayName("Test classifyItems excel numeric category with numeric division falls back to General Industrial Goods")
    void testClassifyItemsNumericCategoryAndDivision() {
        ExcelMasterDataLoader.MasterCategoryRecord record = new ExcelMasterDataLoader.MasterCategoryRecord();
        record.setItemDescription("Unmatched Special Device ABC 3");
        record.setCategory("99");
        record.setDivision("123");

        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(record));

        RFQItem item = RFQItem.builder().itemDescription("Unmatched Special Device ABC 3").specification("99").build();
        service.classifyItems(List.of(item));

        assertEquals("General Industrial Goods", item.getCategory());
        assertEquals("MATCHED", item.getClassificationStatus());
    }

    @Test
    @DisplayName("Test classifyItems excel match with null division sets General Procurement")
    void testClassifyItemsNullDivisionExcelMatch() {
        ExcelMasterDataLoader.MasterCategoryRecord record = new ExcelMasterDataLoader.MasterCategoryRecord();
        record.setItemDescription("Unmatched Special Device ABC 4");
        record.setCategory("Heavy Machinery");
        record.setDivision(null);

        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(record));

        RFQItem item = RFQItem.builder().itemDescription("Unmatched Special Device ABC 4").specification("Heavy Machinery").build();
        service.classifyItems(List.of(item));

        assertEquals("Heavy Machinery", item.getCategory());
        assertEquals("General Procurement", item.getDivision());
    }


    @Test
    @DisplayName("Test classifyItems single-arg overload routes to two-arg")
    void testClassifyItemsSingleArgOverload() {
        RFQItem item = RFQItem.builder().itemDescription("Dell Laptop").build();
        service.classifyItems(List.of(item));
        assertEquals("IT Hardware & Electronics", item.getCategory());
        assertEquals("DOMAIN_KEYWORD_MATCHED", item.getClassificationStatus());
    }

    @Test
    @DisplayName("Test classifyItems with null item fields (nulls in description & spec)")
    void testClassifyItemsNullItemFields() {
        RFQItem item = RFQItem.builder().build();
        service.classifyItems(List.of(item));
        assertEquals("DEFAULT", item.getClassificationStatus());
    }

    @Test
    @DisplayName("Test classifyItems with various domain keywords")
    void testClassifyItemsVariousDomainKeywords() {
        RFQItem cement = RFQItem.builder().itemDescription("Portland Cement bags").build();
        RFQItem chair = RFQItem.builder().itemDescription("Office Chair ergonomic").build();
        RFQItem cctv = RFQItem.builder().itemDescription("CCTV Camera System").build();
        RFQItem paper = RFQItem.builder().itemDescription("A4 Paper ream").build();
        RFQItem dispenser = RFQItem.builder().itemDescription("Water Dispenser unit").build();
        RFQItem helmet = RFQItem.builder().itemDescription("Safety Helmet hard hat").build();
        RFQItem plc = RFQItem.builder().itemDescription("PLC Controller Siemens").build();
        RFQItem gauge = RFQItem.builder().itemDescription("Pressure Gauge industrial").build();

        service.classifyItems(List.of(cement, chair, cctv, paper, dispenser, helmet, plc, gauge));

        assertEquals("Construction", cement.getCategory());
        assertEquals("Office Furniture", chair.getCategory());
        assertEquals("Security & Surveillance Equipment", cctv.getCategory());
        assertEquals("Stationery & Office Supplies", paper.getCategory());
        assertEquals("Appliances & Office Amenities", dispenser.getCategory());
        assertEquals("Safety Equipment", helmet.getCategory());
        assertEquals("Industrial Automation & Electrical", plc.getCategory());
        assertEquals("Instrumentation & Process Control", gauge.getCategory());
    }

    @Test
    @DisplayName("Test classifyItems cross-domain protection prevents IT items matching medical records")
    void testClassifyItemsCrossDomainProtectionNoKeyword() {
        ExcelMasterDataLoader.MasterCategoryRecord medRecord = new ExcelMasterDataLoader.MasterCategoryRecord();
        medRecord.setItemDescription("network cable");
        medRecord.setCategory("Medical Pharma Equipment");
        medRecord.setDivision("Medical");

        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(medRecord));

        RFQItem item = RFQItem.builder().itemDescription("network cable for IT PC setup").build();
        service.classifyItems(List.of(item));

        // Should not match medical record due to cross-domain protection;
        // Falls through to domain keyword or default
        assertNotEquals("Medical Pharma Equipment", item.getCategory());
    }

    @Test
    @DisplayName("Test classifyItems with low score match below threshold")
    void testClassifyItemsLowScoreMatch() {
        ExcelMasterDataLoader.MasterCategoryRecord record = new ExcelMasterDataLoader.MasterCategoryRecord();
        record.setItemDescription("Very Specific Rare Part XYZ");
        record.setCategory("Specialty Parts");
        record.setDivision("Special");

        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(record));

        RFQItem item = RFQItem.builder().itemDescription("Completely Different Random Thing").build();
        service.classifyItems(List.of(item));

        assertEquals("DEFAULT", item.getClassificationStatus());
    }

    @Test
    @DisplayName("Test classifyItems excel match with blank masterDesc returns 0 score")
    void testClassifyItemsBlankMasterDesc() {
        ExcelMasterDataLoader.MasterCategoryRecord record = new ExcelMasterDataLoader.MasterCategoryRecord();
        record.setItemDescription("");
        record.setCategory("Some Category");
        record.setDivision("Some Division");

        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(record));

        RFQItem item = RFQItem.builder().itemDescription("Unrecognized Widget XYZ123").build();
        service.classifyItems(List.of(item));

        assertEquals("DEFAULT", item.getClassificationStatus());
    }
}

