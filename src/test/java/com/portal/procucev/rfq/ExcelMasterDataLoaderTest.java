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
        File excelFile = new File(tempDir.toFile(), "1st Set of Category Items Data.xlsx");
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

        ReflectionTestUtils.setField(loader, "masterFilePath", tempDir.toFile().getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("IT Hardware", records.get(0).getCategory());
        assertEquals("Electronics", records.get(0).getDivision());
        assertEquals("ITEM123", records.get(0).getItemCode());
        assertEquals("Dell Latitude Laptop", records.get(0).getItemDescription());
        assertEquals("16GB RAM, 512GB SSD", records.get(0).getSpecifications());
    }

    @Test
    @DisplayName("Test loadMasterData with non-standard header column order")
    void testLoadMasterDataDynamicHeaderOrder() throws Exception {
        File excelFile = new File(tempDir.toFile(), "custom_headers.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Categories");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Division");
            header.createCell(1).setCellValue("Category");
            header.createCell(2).setCellValue("Item Description");
            header.createCell(3).setCellValue("Item Code");
            header.createCell(4).setCellValue("Specifications");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Electronics");
            row1.createCell(1).setCellValue("IT Hardware");
            row1.createCell(2).setCellValue("Dell Latitude Laptop");
            row1.createCell(3).setCellValue("ITEM123");
            row1.createCell(4).setCellValue("16GB RAM, 512GB SSD");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", excelFile.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertNotNull(records);
        assertEquals(1, records.size());
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

    @Test
    @DisplayName("Test loadMasterData with null masterFilePath uses default file names")
    void testLoadMasterDataNullPath() {
        ReflectionTestUtils.setField(loader, "masterFilePath", null);
        assertDoesNotThrow(() -> loader.loadMasterData());
    }

    @Test
    @DisplayName("Test loadMasterData with blank masterFilePath uses default file names")
    void testLoadMasterDataBlankPath() {
        ReflectionTestUtils.setField(loader, "masterFilePath", "   ");
        assertDoesNotThrow(() -> loader.loadMasterData());
    }

    @Test
    @DisplayName("Test loadMasterData directory with no standard files falls back to listing xlsx")
    void testLoadMasterDataDirectoryListFallback() throws Exception {
        File dir = tempDir.toFile();
        File customFile = new File(dir, "custom_data.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Category");
            header.createCell(1).setCellValue("Description");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Tools");
            row.createCell(1).setCellValue("Hammer");
            try (FileOutputStream fos = new FileOutputStream(customFile)) {
                wb.write(fos);
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", dir.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertFalse(records.isEmpty());
    }

    @Test
    @DisplayName("Test loadMasterData with file named '1st Set...' resolves sibling 2nd set")
    void testLoadMasterDataSiblingResolution() throws Exception {
        File set1 = new File(tempDir.toFile(), "1st Set of Category Items Data.xlsx");
        File set2 = new File(tempDir.toFile(), "2nd Set of Category Items Data.xlsx");

        for (File f : List.of(set1, set2)) {
            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet("Data");
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Category");
                header.createCell(1).setCellValue("Description");
                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue("Cat-" + f.getName().charAt(0));
                row.createCell(1).setCellValue("Desc");
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    wb.write(fos);
                }
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", set1.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertEquals(2, records.size());
    }

    @Test
    @DisplayName("Test loadMasterData with no recognizable headers uses default column order")
    void testLoadMasterDataNoRecognizableHeaders() throws Exception {
        File file = new File(tempDir.toFile(), "noheaders.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Col A");
            header.createCell(1).setCellValue("Col B");
            header.createCell(2).setCellValue("Col C");
            header.createCell(3).setCellValue("Col D");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("CatVal");
            row.createCell(1).setCellValue("DivVal");
            row.createCell(2).setCellValue("CodeVal");
            row.createCell(3).setCellValue("DescVal");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", file.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertFalse(records.isEmpty());
        assertEquals("CatVal", records.get(0).getCategory());
    }

    @Test
    @DisplayName("Test loadMasterData with blank data rows are skipped")
    void testLoadMasterDataBlankRowsSkipped() throws Exception {
        File file = new File(tempDir.toFile(), "blank_rows.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Category");
            header.createCell(1).setCellValue("Description");
            // blank row
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("");
            row.createCell(1).setCellValue("");
            // valid row
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("ValidCat");
            row2.createCell(1).setCellValue("ValidDesc");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", file.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertEquals(1, records.size());
        assertEquals("ValidCat", records.get(0).getCategory());
    }

    @Test
    @DisplayName("Test loadMasterData with numeric cell values ending in .0 are cleaned")
    void testLoadMasterDataNumericCellCleaning() throws Exception {
        File file = new File(tempDir.toFile(), "numeric.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Category");
            header.createCell(1).setCellValue("Code");
            header.createCell(2).setCellValue("Description");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Cat");
            row.createCell(1).setCellValue(12345.0); // numeric
            row.createCell(2).setCellValue("Item");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", file.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertFalse(records.isEmpty());
    }

    @Test
    @DisplayName("Test loadMasterData with header containing 'id' keyword")
    void testLoadMasterDataIdHeader() throws Exception {
        File file = new File(tempDir.toFile(), "id_header.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Category");
            header.createCell(1).setCellValue("ID");
            header.createCell(2).setCellValue("Description");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Cat");
            row.createCell(1).setCellValue("ID-001");
            row.createCell(2).setCellValue("Item");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", file.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertFalse(records.isEmpty());
        assertEquals("ID-001", records.get(0).getItemCode());
    }

    @Test
    @DisplayName("Test loadMasterData with header containing 'item' keyword maps to description")
    void testLoadMasterDataItemHeader() throws Exception {
        File file = new File(tempDir.toFile(), "item_header.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Category");
            header.createCell(1).setCellValue("Item");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Cat");
            row.createCell(1).setCellValue("Widget");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", file.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertFalse(records.isEmpty());
        assertEquals("Widget", records.get(0).getItemDescription());
    }

    @Test
    @DisplayName("Test loadMasterData directory containing other .xlsx files")
    void testLoadMasterDataDirectoryFallbackXlsx() throws Exception {
        File dir = new File(tempDir.toFile(), "empty_sets_dir");
        dir.mkdirs();
        File customXlsx = new File(dir, "custom_data.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Category");
            h.createCell(1).setCellValue("Description");
            Row r = sheet.createRow(1);
            r.createCell(0).setCellValue("Hardware");
            r.createCell(1).setCellValue("Bolt");
            try (FileOutputStream fos = new FileOutputStream(customXlsx)) {
                wb.write(fos);
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", dir.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertFalse(records.isEmpty());
        assertEquals("Hardware", records.get(0).getCategory());
    }

    @Test
    @DisplayName("Test loadMasterData directory with set1 and set2 present")
    void testLoadMasterDataDirectoryWithBothSets() throws Exception {
        File dir = new File(tempDir.toFile(), "both_sets_dir");
        dir.mkdirs();
        File set1 = new File(dir, "1st Set of Category Items Data.xlsx");
        File set2 = new File(dir, "2nd Set of Category Items Data.xlsx");

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet s1 = wb.createSheet("S1");
            Row h1 = s1.createRow(0);
            h1.createCell(0).setCellValue("Category");
            h1.createCell(1).setCellValue("Description");
            Row r1 = s1.createRow(1);
            r1.createCell(0).setCellValue("Set1Cat");
            r1.createCell(1).setCellValue("Set1Desc");
            try (FileOutputStream fos = new FileOutputStream(set1)) {
                wb.write(fos);
            }
        }
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet s2 = wb.createSheet("S2");
            Row h2 = s2.createRow(0);
            h2.createCell(0).setCellValue("Category");
            h2.createCell(1).setCellValue("Description");
            Row r2 = s2.createRow(1);
            r2.createCell(0).setCellValue("Set2Cat");
            r2.createCell(1).setCellValue("Set2Desc");
            try (FileOutputStream fos = new FileOutputStream(set2)) {
                wb.write(fos);
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", dir.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertEquals(2, records.size());
    }
}

