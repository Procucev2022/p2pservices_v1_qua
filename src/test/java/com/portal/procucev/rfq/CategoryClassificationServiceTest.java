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
        record1.setItemDescription("Heavy Duty Hydraulic Pump");
        record1.setCategory("Heavy Machinery");
        record1.setDivision("Mechanical Engineering");

        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(record1));

        RFQItem item = RFQItem.builder().itemDescription("Heavy Duty Hydraulic Pump").build();
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
        record1.setItemDescription("Custom Industrial Machine");
        record1.setCategory("123.45"); // Numeric
        record1.setDivision("Custom Division");

        Mockito.when(dataLoader.getMasterRecords()).thenReturn(List.of(record1));

        RFQItem item = RFQItem.builder().itemDescription("Custom Industrial Machine").build();
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
}
