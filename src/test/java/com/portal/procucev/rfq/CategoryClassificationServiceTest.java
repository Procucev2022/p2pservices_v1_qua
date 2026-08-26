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
    @DisplayName("Test classifyItems with explicit non-generic item category (Step 0)")
    void testClassifyItemsExplicitNonGenericItemCategory() {
        RFQItem item = RFQItem.builder()
                .itemDescription("Precision Bearing")
                .category("Mechanical Components")
                .build();

        service.classifyItems(List.of(item));

        assertEquals("Mechanical Components", item.getCategory());
        assertEquals("Mechanical Components", item.getDivision());
        assertEquals(0.95, item.getCategoryConfidence());
        assertEquals("AI_EXTRACTED", item.getClassificationStatus());
    }

    @Test
    @DisplayName("Test classifyItems with generic item category falls through to domain keyword or master")
    void testClassifyItemsGenericItemCategoryFallthrough() {
        RFQItem item1 = RFQItem.builder()
                .itemDescription("Laptop Dell Latitude")
                .category("Multiple Categories")
                .build();

        RFQItem item2 = RFQItem.builder()
                .itemDescription("Laptop Dell Latitude")
                .category("3 categories")
                .build();

        service.classifyItems(List.of(item1, item2));

        assertEquals("IT Hardware & Electronics", item1.getCategory());
        assertEquals("IT Hardware & Electronics", item2.getCategory());
    }

    @Test
    @DisplayName("Test calculateMatchScore directly with null, empty, matching, and cross-domain records")
    void testCalculateMatchScoreDirectly() {
        ExcelMasterDataLoader.MasterCategoryRecord recBlank = new ExcelMasterDataLoader.MasterCategoryRecord();
        recBlank.setItemDescription("");
        double score0 = (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "text", recBlank);
        assertEquals(0.0, score0);

        ExcelMasterDataLoader.MasterCategoryRecord recMed = new ExcelMasterDataLoader.MasterCategoryRecord();
        recMed.setItemDescription("Monitor");
        recMed.setCategory("Surgical Equipment");
        double scoreMed = (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "computer pc monitor", recMed);
        assertEquals(0.0, scoreMed);

        ExcelMasterDataLoader.MasterCategoryRecord recMatch = new ExcelMasterDataLoader.MasterCategoryRecord();
        recMatch.setItemDescription("Industrial Valve");
        recMatch.setCategory("Valves & Pumps");
        double scoreMatch = (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "industrial valve valves & pumps", recMatch);
        assertEquals(1.0, scoreMatch);

        // masterDesc contains text
        ExcelMasterDataLoader.MasterCategoryRecord recContains = new ExcelMasterDataLoader.MasterCategoryRecord();
        recContains.setItemDescription("Heavy Duty Industrial Valve");
        double scoreContains = (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "heavy duty", recContains);
        assertEquals(0.7, scoreContains);

        // Pharma cross-domain
        ExcelMasterDataLoader.MasterCategoryRecord recPharma = new ExcelMasterDataLoader.MasterCategoryRecord();
        recPharma.setItemDescription("Monitor");
        recPharma.setCategory("Pharma Supplies");
        double scorePharma = (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "network server monitor", recPharma);
        assertEquals(0.0, scorePharma);
    }

    @Test
    @DisplayName("Test isGenericCategory and division retention in classifyItems")
    void testIsGenericCategoryAndDivisionRetention() {
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", (String) null));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "   "));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "Not Specified"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "mixed categories"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "Steel Pipes"));

        // Item with category and existing division
        RFQItem itemWithDiv = RFQItem.builder()
                .itemDescription("Custom Valve")
                .category("Flow Control")
                .division("Piping Division")
                .build();
        service.classifyItems(List.of(itemWithDiv));
        assertEquals("Flow Control", itemWithDiv.getCategory());
        assertEquals("Piping Division", itemWithDiv.getDivision());

        // Numeric category and numeric division in master record
        ExcelMasterDataLoader.MasterCategoryRecord recNumBoth = new ExcelMasterDataLoader.MasterCategoryRecord();
        recNumBoth.setItemDescription("Unique Num Item 999");
        recNumBoth.setCategory("123");
        recNumBoth.setDivision("456");
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(recNumBoth));

        RFQItem itemNumBoth = RFQItem.builder().itemDescription("Unique Num Item 999").build();
        service.classifyItems(List.of(itemNumBoth));
        assertEquals("General Industrial Goods", itemNumBoth.getCategory());

        // 4. isGenericCategory with '3 categories'
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "3 categories"));

        // 5. calculateMatchScore direct branches
        ExcelMasterDataLoader.MasterCategoryRecord rBlankDesc = new ExcelMasterDataLoader.MasterCategoryRecord();
        rBlankDesc.setItemDescription("");
        double s0 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "test", rBlankDesc);
        assertEquals(0.0, s0);

        ExcelMasterDataLoader.MasterCategoryRecord rWithCat = new ExcelMasterDataLoader.MasterCategoryRecord();
        rWithCat.setItemDescription("drill");
        rWithCat.setCategory("power tools");
        double s1 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "cordless drill with power tools kit", rWithCat);
        assertEquals(1.0, s1, 0.01);

        // 6. Master record with null division
        ExcelMasterDataLoader.MasterCategoryRecord rNullDiv = new ExcelMasterDataLoader.MasterCategoryRecord();
        rNullDiv.setItemDescription("Special Ceramic Tile");
        rNullDiv.setCategory("Tiles");
        rNullDiv.setDivision(null);
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(rNullDiv));

        RFQItem itemNullDiv = RFQItem.builder().itemDescription("Special Ceramic Tile in Tiles catalog").build();
        service.classifyItems(List.of(itemNullDiv));
        assertEquals("Tiles", itemNullDiv.getCategory());
        assertEquals("General Procurement", itemNullDiv.getDivision());

        // 7. Cross-domain rejection (computer + surgical)
        ExcelMasterDataLoader.MasterCategoryRecord rMed = new ExcelMasterDataLoader.MasterCategoryRecord();
        rMed.setItemDescription("computer monitor");
        rMed.setCategory("surgical devices");
        double sMed = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "computer monitor 24 inch", rMed);
        assertEquals(0.0, sMed);

        // 8. masterDesc.contains(text)
        ExcelMasterDataLoader.MasterCategoryRecord rLongDesc = new ExcelMasterDataLoader.MasterCategoryRecord();
        rLongDesc.setItemDescription("industrial stainless steel ball valve 2 inch");
        rLongDesc.setCategory("");
        double sLong = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "ball valve", rLongDesc);
        assertEquals(0.7, sLong, 0.01);

        // 9. Generic category keyword variants
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "various"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "several items"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "all products"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "null"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "N/A"));

        // 10. Numeric category with valid text division
        ExcelMasterDataLoader.MasterCategoryRecord rNumCat = new ExcelMasterDataLoader.MasterCategoryRecord();
        rNumCat.setItemDescription("Custom Hydraulic Cylinder 999");
        rNumCat.setCategory("789");
        rNumCat.setDivision("Fluid Power");
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(rNumCat));
        RFQItem itemNumCat = RFQItem.builder().itemDescription("Custom Hydraulic Cylinder 999 in 789 catalog").build();
        service.classifyItems(List.of(itemNumCat));
        assertEquals("Fluid Power", itemNumCat.getCategory());
        assertEquals("Fluid Power", itemNumCat.getDivision());

        // 11. Null category in bestMatch
        ExcelMasterDataLoader.MasterCategoryRecord rNullCat = new ExcelMasterDataLoader.MasterCategoryRecord();
        rNullCat.setItemDescription("Heavy Duty Spring");
        rNullCat.setCategory(null);
        rNullCat.setDivision("Mechanical Parts");
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(rNullCat));
        RFQItem itemNullCat = RFQItem.builder().itemDescription("Heavy Duty Spring").build();
        service.classifyItems(List.of(itemNullCat));
        assertEquals("General Industrial Goods", itemNullCat.getCategory());

        // 12. Numeric category with numeric division or blank division
        ExcelMasterDataLoader.MasterCategoryRecord rNumBoth = new ExcelMasterDataLoader.MasterCategoryRecord();
        rNumBoth.setItemDescription("Precision Bushing 888");
        rNumBoth.setCategory("123");
        rNumBoth.setDivision("456");
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(rNumBoth));
        RFQItem itemNumBoth2 = RFQItem.builder().itemDescription("Precision Bushing 888").build();
        service.classifyItems(List.of(itemNumBoth2));
        assertEquals("General Industrial Goods", itemNumBoth2.getCategory());

        ExcelMasterDataLoader.MasterCategoryRecord rNumBlankDiv = new ExcelMasterDataLoader.MasterCategoryRecord();
        rNumBlankDiv.setItemDescription("Precision Gear 777");
        rNumBlankDiv.setCategory("123");
        rNumBlankDiv.setDivision("   ");
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(rNumBlankDiv));
        RFQItem itemNumBlankDiv = RFQItem.builder().itemDescription("Precision Gear 777").build();
        service.classifyItems(List.of(itemNumBlankDiv));
        assertEquals("General Industrial Goods", itemNumBlankDiv.getCategory());

        // 13. isGenericCategory numeric pattern and negative cases
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "2 categories"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "5 category"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "isGenericCategory", "Industrial Fasteners"));

        // 14. calculateMatchScore all cross-domain branches and combined score
        ExcelMasterDataLoader.MasterCategoryRecord rPharma = new ExcelMasterDataLoader.MasterCategoryRecord();
        rPharma.setItemDescription("pc device");
        rPharma.setCategory("pharma supplies");
        assertEquals(0.0, (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "pc computer workstation", rPharma));

        ExcelMasterDataLoader.MasterCategoryRecord rNetworkSurg = new ExcelMasterDataLoader.MasterCategoryRecord();
        rNetworkSurg.setItemDescription("network switch");
        rNetworkSurg.setCategory("surgical instruments");
        assertEquals(0.0, (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "network router and switch", rNetworkSurg));

        ExcelMasterDataLoader.MasterCategoryRecord rItMed = new ExcelMasterDataLoader.MasterCategoryRecord();
        rItMed.setItemDescription("it server");
        rItMed.setCategory("medical devices");
        assertEquals(0.0, (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "it hardware server rack", rItMed));

        ExcelMasterDataLoader.MasterCategoryRecord rCombinedMatch = new ExcelMasterDataLoader.MasterCategoryRecord();
        rCombinedMatch.setItemDescription("ball valve");
        rCombinedMatch.setCategory("valves");
        assertEquals(1.0, (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "ball valve valves", rCombinedMatch), 0.01);

        // 15. Item with non-empty category and non-empty division
        RFQItem itemWithCatDiv = RFQItem.builder()
                .itemDescription("Gate Valve")
                .category("Industrial Valves")
                .division("Flow Control")
                .build();
        service.classifyItems(List.of(itemWithCatDiv));
        assertEquals("Industrial Valves", itemWithCatDiv.getCategory());
        assertEquals("Flow Control", itemWithCatDiv.getDivision());

        // 16. extractedCategory is "null" string and record has null category/division
        RFQItem itemNullCat3 = RFQItem.builder().itemDescription("Precision Bushing 999").build();
        ExcelMasterDataLoader.MasterCategoryRecord recNull = new ExcelMasterDataLoader.MasterCategoryRecord();
        recNull.setItemDescription("Precision Bushing 999");
        recNull.setCategory(null);
        recNull.setDivision(null);
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(recNull));
        service.classifyItems(List.of(itemNullCat3), "null");
        assertEquals("General Industrial Goods", itemNullCat3.getCategory());
        assertEquals("General Procurement", itemNullCat3.getDivision());

        // 17. record with blank category and null division
        RFQItem itemBlankCat = RFQItem.builder().itemDescription("Precision O-Ring 888").build();
        ExcelMasterDataLoader.MasterCategoryRecord recBlank = new ExcelMasterDataLoader.MasterCategoryRecord();
        recBlank.setItemDescription("Precision O-Ring 888");
        recBlank.setCategory("   ");
        recBlank.setDivision(null);
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(recBlank));
        service.classifyItems(List.of(itemBlankCat));
        assertEquals("General Industrial Goods", itemBlankCat.getCategory());
        assertEquals("General Procurement", itemBlankCat.getDivision());

        // 18. Low confidence match below minMatchedConfidence threshold
        RFQItem itemLowConf = RFQItem.builder().itemDescription("Custom Widget").build();
        ExcelMasterDataLoader.MasterCategoryRecord recLow = new ExcelMasterDataLoader.MasterCategoryRecord();
        recLow.setItemDescription("Unrelated Item");
        recLow.setCategory("Widget");
        recLow.setDivision("Custom Div");
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(recLow));
        service.classifyItems(List.of(itemLowConf));
        assertEquals("General Industrial Goods", itemLowConf.getCategory());
        assertEquals("General Procurement", itemLowConf.getDivision());

        // 19. masterDesc.contains(text) branch
        ExcelMasterDataLoader.MasterCategoryRecord recDescContains = new ExcelMasterDataLoader.MasterCategoryRecord();
        recDescContains.setItemDescription("heavy duty industrial gate valve");
        recDescContains.setCategory("");
        double scoreSub = (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "gate valve", recDescContains);
        assertEquals(0.7, scoreSub, 0.01);

        // 20. explicit category with pre-existing non-blank division
        RFQItem itemExplicitWithDiv = RFQItem.builder().itemDescription("Pump").category("Pumps").division("Fluid Machinery").build();
        service.classifyItems(List.of(itemExplicitWithDiv));
        assertEquals("Pumps", itemExplicitWithDiv.getCategory());
        assertEquals("Fluid Machinery", itemExplicitWithDiv.getDivision());

        // 21. cross-domain IT and medical combinations
        String[] itTerms = {"computer", "pc", "it", "network"};
        String[] medCats = {"surgical supplies", "medical devices", "pharma goods"};
        for (String t : itTerms) {
            for (String m : medCats) {
                ExcelMasterDataLoader.MasterCategoryRecord r = new ExcelMasterDataLoader.MasterCategoryRecord();
                r.setItemDescription(t + " monitor");
                r.setCategory(m);
                double s = (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        service, "calculateMatchScore", "need a " + t + " device", r);
                assertEquals(0.0, s, 0.001);
            }
        }

        // 22. numeric category and division
        RFQItem itemNumCatDiv = RFQItem.builder().itemDescription("Gizmo Part 456").build();
        ExcelMasterDataLoader.MasterCategoryRecord recNumCatDiv = new ExcelMasterDataLoader.MasterCategoryRecord();
        recNumCatDiv.setItemDescription("Gizmo Part 456");
        recNumCatDiv.setCategory("456");
        recNumCatDiv.setDivision("789");
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(recNumCatDiv));
        service.classifyItems(List.of(itemNumCatDiv));
        assertEquals("General Industrial Goods", itemNumCatDiv.getCategory());
        assertEquals("789", itemNumCatDiv.getDivision());

        // 23. isGenericCategory word variations
        String[] genericWords = {"categories", "multiple categories", "mixed", "different", "several", "all", "general", "not specified", "n/a", "null", "2 categories"};
        for (String gw : genericWords) {
            assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "isGenericCategory", gw));
        }
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "isGenericCategory", "Centrifugal Pumps"));

        // 24. brand and remarks non-null with valid division fallback
        RFQItem itemBrandRemarks = RFQItem.builder()
                .itemDescription("Gizmo Part 999 123")
                .specification("Type A")
                .brand("Acme")
                .remarks("Urgent Requirement")
                .build();
        ExcelMasterDataLoader.MasterCategoryRecord recNullCatDiv = new ExcelMasterDataLoader.MasterCategoryRecord();
        recNullCatDiv.setItemDescription("Gizmo Part 999");
        recNullCatDiv.setCategory("123");
        recNullCatDiv.setDivision("Electrical Goods");
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(recNullCatDiv));
        service.classifyItems(List.of(itemBrandRemarks));
        assertEquals("Electrical Goods", itemBrandRemarks.getCategory());
        assertEquals("Electrical Goods", itemBrandRemarks.getDivision());

        // 25. numeric category with blank division fallback
        RFQItem itemBlankDiv = RFQItem.builder().itemDescription("Gizmo Part 888").build();
        ExcelMasterDataLoader.MasterCategoryRecord recBlankDiv = new ExcelMasterDataLoader.MasterCategoryRecord();
        recBlankDiv.setItemDescription("Gizmo Part 888");
        recBlankDiv.setCategory("123");
        recBlankDiv.setDivision("   ");
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(recBlankDiv));
        service.classifyItems(List.of(itemBlankDiv));
        assertEquals("General Industrial Goods", itemBlankDiv.getCategory());
        assertEquals("General Procurement", itemBlankDiv.getDivision());

        // 26. calculateMatchScore with null/blank master itemDescription
        ExcelMasterDataLoader.MasterCategoryRecord recNullDesc = new ExcelMasterDataLoader.MasterCategoryRecord();
        recNullDesc.setItemDescription(null);
        assertEquals(0.0, (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "Some Text", recNullDesc), 0.001);

        ExcelMasterDataLoader.MasterCategoryRecord recBlankDesc = new ExcelMasterDataLoader.MasterCategoryRecord();
        recBlankDesc.setItemDescription("   ");
        assertEquals(0.0, (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "calculateMatchScore", "Some Text", recBlankDesc), 0.001);

        // 27. isGenericCategory numeric category regex
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "isGenericCategory", "3 category items"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "isGenericCategory", "15 categories requirement"));

        // 28. classifySingleItem with non-generic category and existing division
        RFQItem itemWithExistingDiv = RFQItem.builder().itemDescription("Item").category("Valves").division("Ball Valves").build();
        service.classifyItems(List.of(itemWithExistingDiv));
        assertEquals("Valves", itemWithExistingDiv.getCategory());
        assertEquals("Ball Valves", itemWithExistingDiv.getDivision());

        // 29. classifySingleItem with extractedCategory = "null" string
        RFQItem itemExtNull = RFQItem.builder().itemDescription("Item").build();
        service.classifyItems(List.of(itemExtNull), "null");
        assertNotNull(itemExtNull.getCategory());

        // 30. classifySingleItem matching record with null division
        RFQItem itemNullDivRec = RFQItem.builder().itemDescription("HypotheticalGizmoItemXYZ").specification("CustomSpecialty").build();
        ExcelMasterDataLoader.MasterCategoryRecord recNullDiv = new ExcelMasterDataLoader.MasterCategoryRecord();
        recNullDiv.setItemDescription("HypotheticalGizmoItemXYZ");
        recNullDiv.setCategory("CustomSpecialty");
        recNullDiv.setDivision(null);
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(recNullDiv));
        service.classifyItems(List.of(itemNullDivRec));
        assertEquals("CustomSpecialty", itemNullDivRec.getCategory());
        assertEquals("General Procurement", itemNullDivRec.getDivision());

        // 31. classifySingleItem matching record with null category and valid division
        org.springframework.test.util.ReflectionTestUtils.setField(service, "minMatchedConfidence", 0.65);
        RFQItem itemNullCatRec = RFQItem.builder().itemDescription("ItemAlphaBeta").specification("ItemAlphaBeta").build();
        ExcelMasterDataLoader.MasterCategoryRecord recNullCat = new ExcelMasterDataLoader.MasterCategoryRecord();
        recNullCat.setItemDescription("ItemAlphaBeta");
        recNullCat.setCategory(null);
        recNullCat.setDivision("ValidDivisionSpecial");
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(recNullCat));
        service.classifyItems(List.of(itemNullCatRec));
        assertEquals("ValidDivisionSpecial", itemNullCatRec.getCategory());
        assertEquals("ValidDivisionSpecial", itemNullCatRec.getDivision());

        // 32. classifySingleItem matching record with blank category and numeric division
        RFQItem itemBlankCatNumDiv = RFQItem.builder().itemDescription("ItemGammaDelta").specification("ItemGammaDelta").build();
        ExcelMasterDataLoader.MasterCategoryRecord recBlankCatNumDiv = new ExcelMasterDataLoader.MasterCategoryRecord();
        recBlankCatNumDiv.setItemDescription("ItemGammaDelta");
        recBlankCatNumDiv.setCategory("   ");
        recBlankCatNumDiv.setDivision("999.99");
        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(recBlankCatNumDiv));
        service.classifyItems(List.of(itemBlankCatNumDiv));
        assertEquals("General Industrial Goods", itemBlankCatNumDiv.getCategory());
    }
}

