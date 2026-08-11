package com.portal.procucev.rfq;

import com.portal.procucev.rfq.service.ExcelMasterDataLoader;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelMasterDataLoaderTest {

    @TempDir
    Path tempDir;

    private ExcelMasterDataLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ExcelMasterDataLoader();
    }

    @Test
    @DisplayName("Test init and loadMasterData with existing workspace Excel file or fallback")
    void testInitAndLoadMasterData() {
        loader.init();
        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertNotNull(records);
    }

    @Test
    @DisplayName("Test loadMasterData with custom temporary excel directory")
    void testLoadMasterDataWithCustomTempFile() throws Exception {
        File excelFile = new File(tempDir.toFile(), "test_master.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Categories");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Category");
            header.createCell(1).setCellValue("Division");
            header.createCell(2).setCellValue("Item Code");
            header.createCell(3).setCellValue("Item Description");
            header.createCell(4).setCellValue("Specifications");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("IT Hardware");
            row1.createCell(1).setCellValue("Electronics");
            row1.createCell(2).setCellValue("ITEM123");
            row1.createCell(3).setCellValue("Dell Latitude Laptop");
            row1.createCell(4).setCellValue("16GB RAM, 512GB SSD");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", excelFile.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertNotNull(records);
        assertFalse(records.isEmpty());
        assertEquals("IT Hardware", records.get(0).getCategory());
        assertEquals("Electronics", records.get(0).getDivision());
        assertEquals("ITEM123", records.get(0).getItemCode());
        assertEquals("Dell Latitude Laptop", records.get(0).getItemDescription());
        assertEquals("16GB RAM, 512GB SSD", records.get(0).getSpecifications());
    }

    @Test
    @DisplayName("Test loadMasterData when masterFilePath is directory")
    void testLoadMasterDataDirectory() throws Exception {
        File dir = tempDir.toFile();
        File file1 = new File(dir, "1st Set of Category Items Data.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Cat");
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("cat");
            row0.createCell(1).setCellValue("div");
            row0.createCell(2).setCellValue("code");
            row0.createCell(3).setCellValue("desc");
            row0.createCell(4).setCellValue("spec");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Tools");
            row1.createCell(3).setCellValue("Hammer");

            try (FileOutputStream fos = new FileOutputStream(file1)) {
                wb.write(fos);
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", dir.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertNotNull(records);
        assertFalse(records.isEmpty());
        assertEquals("Tools", records.get(0).getCategory());
    }

    @Test
    @DisplayName("Test MasterCategoryRecord model getters, setters, builder, equals, hashCode, toString")
    void testMasterCategoryRecordModel() {
        ExcelMasterDataLoader.MasterCategoryRecord r1 = ExcelMasterDataLoader.MasterCategoryRecord.builder()
                .category("Cat")
                .division("Div")
                .itemCode("Code")
                .itemDescription("Desc")
                .specifications("Spec")
                .build();

        assertEquals("Cat", r1.getCategory());
        assertEquals("Div", r1.getDivision());
        assertEquals("Code", r1.getItemCode());
        assertEquals("Desc", r1.getItemDescription());
        assertEquals("Spec", r1.getSpecifications());

        ExcelMasterDataLoader.MasterCategoryRecord r2 = new ExcelMasterDataLoader.MasterCategoryRecord("Cat", "Div", "Code", "Desc", "Spec");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotNull(r1.toString());

        ExcelMasterDataLoader.MasterCategoryRecord r3 = new ExcelMasterDataLoader.MasterCategoryRecord();
        r3.setCategory("Cat");
        r3.setDivision("Div");
        r3.setItemCode("Code");
        r3.setItemDescription("Desc");
        r3.setSpecifications("Spec");
        assertEquals(r1, r3);
    }

    @Test
    @DisplayName("Test loadMasterData with non-existent file path")
    void testLoadMasterDataNonExistentPath() {
        ReflectionTestUtils.setField(loader, "masterFilePath", "non_existent_file.xlsx");
        assertDoesNotThrow(() -> loader.loadMasterData());
    }
}
