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
import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;

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
            header.createCell(0).setCellValue("div");
            header.createCell(1).setCellValue("cat");
            header.createCell(2).setCellValue("item");
            header.createCell(3).setCellValue("product id");
            header.createCell(4).setCellValue("spec");

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
    @DisplayName("Test corrupt Excel file exception handling")
    void testLoadMasterDataCorruptedFile() throws Exception {
        File corruptFile = new File(tempDir.toFile(), "corrupt.xlsx");
        try (FileOutputStream fos = new FileOutputStream(corruptFile)) {
            fos.write("Not an excel file content".getBytes());
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", corruptFile.getAbsolutePath());
        assertDoesNotThrow(() -> loader.loadMasterData());
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
    @DisplayName("Test getCellValue numeric formatting and null cell handling")
    void testGetCellValueFormattingAndEdgeCases() throws Exception {
        File file = new File(tempDir.toFile(), "formatting_test.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Data");
            Row header = sheet.createRow(0);
            // Header with null cell at index 0, blank at index 1, valid at index 2
            header.createCell(1).setCellValue("   ");
            header.createCell(2).setCellValue("Item Code");
            header.createCell(3).setCellValue("Category");
            header.createCell(4).setCellValue("Description");

            Row row = sheet.createRow(1);
            row.createCell(2).setCellValue(12345.0); // Numeric code that becomes "12345"
            row.createCell(3).setCellValue("CatNum");
            row.createCell(4).setCellValue("DescNum");

            // Short row (less cells than header columns)
            Row shortRow = sheet.createRow(2);
            shortRow.createCell(0).setCellValue("Short");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }

        ReflectionTestUtils.setField(loader, "masterFilePath", file.getAbsolutePath());
        loader.loadMasterData();

        List<ExcelMasterDataLoader.MasterCategoryRecord> records = loader.getMasterRecords();
        assertFalse(records.isEmpty());
        assertEquals("12345", records.get(0).getItemCode());

        // Test private getCellValue with null
        String nullRes = ReflectionTestUtils.invokeMethod(loader, "getCellValue", (Cell) null);
        assertEquals("", nullRes);
    }

    @Test
    @DisplayName("Test resolveConfiguredFiles with directory, sibling checks, and getInputStream")
    void testResolveConfiguredFilesAndInputStream() throws Exception {
        // 1. Directory containing Set 1 and Set 2
        File dir = new File(tempDir.toFile(), "master_dir");
        dir.mkdirs();
        File set1 = new File(dir, "1st Set of Category Items Data.xlsx");
        set1.createNewFile();
        File set2 = new File(dir, "2nd Set of Category Items Data.xlsx");
        set2.createNewFile();

        ReflectionTestUtils.setField(loader, "masterFilePath", dir.getAbsolutePath());
        List<String> filesInDir = ReflectionTestUtils.invokeMethod(loader, "resolveConfiguredFiles");
        assertEquals(2, filesInDir.size());

        // 2. Directory without Set 1 / Set 2, but with other xlsx
        File otherDir = new File(tempDir.toFile(), "other_dir");
        otherDir.mkdirs();
        File otherXlsx = new File(otherDir, "custom.xlsx");
        otherXlsx.createNewFile();
        ReflectionTestUtils.setField(loader, "masterFilePath", otherDir.getAbsolutePath());
        List<String> otherFiles = ReflectionTestUtils.invokeMethod(loader, "resolveConfiguredFiles");
        assertEquals(1, otherFiles.size());

        // 3. 1st Set file with parent, but 2nd Set sibling doesn't exist
        File isolatedDir = new File(tempDir.toFile(), "isolated_dir");
        isolatedDir.mkdirs();
        File isoSet1 = new File(isolatedDir, "1st Set of Category Items Data.xlsx");
        isoSet1.createNewFile();
        ReflectionTestUtils.setField(loader, "masterFilePath", isoSet1.getAbsolutePath());
        List<String> isoFiles = ReflectionTestUtils.invokeMethod(loader, "resolveConfiguredFiles");
        assertEquals(1, isoFiles.size());

        // 4. 1st Set file without parent (relative filename)
        ReflectionTestUtils.setField(loader, "masterFilePath", "1st Set of Category Items Data.xlsx");
        List<String> relFiles = ReflectionTestUtils.invokeMethod(loader, "resolveConfiguredFiles");
        assertNotNull(relFiles);

        // 5. getInputStreamForFile null / not found
        assertNull(ReflectionTestUtils.invokeMethod(loader, "getInputStreamForFile", "non_existent_random_file_123.xlsx"));

        // 6. getCellValue edge cases: numeric .0 vs non-numeric .0 vs regular text
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Test");
            Row r = sheet.createRow(0);
            Cell c1 = r.createCell(0);
            c1.setCellValue(42.0);
            assertEquals("42", ReflectionTestUtils.invokeMethod(loader, "getCellValue", c1));

            Cell c2 = r.createCell(1);
            c2.setCellValue("ABC.0");
            assertEquals("ABC.0", ReflectionTestUtils.invokeMethod(loader, "getCellValue", c2));

            Cell c3 = r.createCell(2);
            c3.setCellValue("Regular Text");
            assertEquals("Regular Text", ReflectionTestUtils.invokeMethod(loader, "getCellValue", c3));
        }

        // 7. loadSingleExcelFile with specific header variants ("id", "item id", "spec", "code", "div")
        File variantFile = new File(tempDir.toFile(), "header_variants.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Variants");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Item ID");
            header.createCell(1).setCellValue("Category Name");
            header.createCell(2).setCellValue("Div");
            header.createCell(3).setCellValue("Item Description");
            header.createCell(4).setCellValue("Technical Spec");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("ID101");
            row.createCell(1).setCellValue("Electronics");
            row.createCell(2).setCellValue("Hardware");
            row.createCell(3).setCellValue("Sensor");
            row.createCell(4).setCellValue("12V DC");

            try (FileOutputStream fos = new FileOutputStream(variantFile)) {
                wb.write(fos);
            }
        }
        List<ExcelMasterDataLoader.MasterCategoryRecord> records = new ArrayList<>();
        ReflectionTestUtils.invokeMethod(loader, "loadSingleExcelFile", variantFile.getAbsolutePath(), records);
        assertEquals(1, records.size());
        assertEquals("Sensor", records.get(0).getItemDescription());

        // 8. getInputStreamForFile branches
        assertNull(ReflectionTestUtils.invokeMethod(loader, "getInputStreamForFile", "non_existent_file_xyz_123.xlsx"));
        assertNull(ReflectionTestUtils.invokeMethod(loader, "getInputStreamForFile", (String) null));

        // 9. getCellValue exception fallback
        Cell mockCell = Mockito.mock(Cell.class);
        Mockito.when(mockCell.toString()).thenReturn("Fallback Cell String");
        // Invoking getCellValue on mockCell where dataFormatter throws
        String fallbackResult = ReflectionTestUtils.invokeMethod(loader, "getCellValue", mockCell);
        assertNotNull(fallbackResult);

        // 10. loadSingleExcelFile with blank rows and skipped cells
        File blankRowsFile = new File(tempDir.toFile(), "blank_rows.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("BlankRows");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Category");
            header.createCell(1).setCellValue("Item Code");
            header.createCell(2).setCellValue(""); // blank header cell
            header.createCell(3).setCellValue("Description");

            Row emptyRow = sheet.createRow(1);
            emptyRow.createCell(0).setCellValue("");
            emptyRow.createCell(3).setCellValue("");

            Row validRow = sheet.createRow(2);
            validRow.createCell(0).setCellValue("Tools");
            validRow.createCell(1).setCellValue("CODE-1");
            validRow.createCell(3).setCellValue("Screwdriver");

            try (FileOutputStream fos = new FileOutputStream(blankRowsFile)) {
                wb.write(fos);
            }
        }
        List<ExcelMasterDataLoader.MasterCategoryRecord> blankRecords = new ArrayList<>();
        ReflectionTestUtils.invokeMethod(loader, "loadSingleExcelFile", blankRowsFile.getAbsolutePath(), blankRecords);
        assertEquals(1, blankRecords.size());
        assertEquals("Screwdriver", blankRecords.get(0).getItemDescription());

        // 11. Classpath resource loading in getInputStreamForFile
        java.io.InputStream cpIs = ReflectionTestUtils.invokeMethod(loader, "getInputStreamForFile", "junit-platform.properties");
        assertNotNull(cpIs);
        if (cpIs != null) cpIs.close();

        // 12. Header with no recognized headers -> fallback to default columns 0,1,2,3,4
        File noHeaderFile = new File(tempDir.toFile(), "no_header_match.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("NoHeaders");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Col0");
            header.createCell(1).setCellValue("Col1");
            header.createCell(2).setCellValue("Col2");
            header.createCell(3).setCellValue("Col3");
            header.createCell(4).setCellValue("Col4");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("DefaultCat");
            r1.createCell(1).setCellValue("DefaultDiv");
            r1.createCell(2).setCellValue("CODE001");
            r1.createCell(3).setCellValue("DefaultItemDesc");
            r1.createCell(4).setCellValue("DefaultSpec");

            Row rOnlyCat = sheet.createRow(2);
            rOnlyCat.createCell(0).setCellValue("CategoryOnly");

            try (FileOutputStream fos = new FileOutputStream(noHeaderFile)) {
                wb.write(fos);
            }
        }
        List<ExcelMasterDataLoader.MasterCategoryRecord> noHeaderRecords = new ArrayList<>();
        ReflectionTestUtils.invokeMethod(loader, "loadSingleExcelFile", noHeaderFile.getAbsolutePath(), noHeaderRecords);
        assertEquals(2, noHeaderRecords.size());
        assertEquals("DefaultItemDesc", noHeaderRecords.get(0).getItemDescription());
        assertEquals("CategoryOnly", noHeaderRecords.get(1).getCategory());

        // 13. Header with "cat", "div", "code", "desc"
        File altHeaderFile = new File(tempDir.toFile(), "alt_headers.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("AltHeaders");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("cat");
            header.createCell(1).setCellValue("div");
            header.createCell(2).setCellValue("code");
            header.createCell(3).setCellValue("desc");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("AltCat");
            r1.createCell(1).setCellValue("AltDiv");
            r1.createCell(2).setCellValue("CODE002");
            r1.createCell(3).setCellValue("AltDesc");

            try (FileOutputStream fos = new FileOutputStream(altHeaderFile)) {
                wb.write(fos);
            }
        }
        List<ExcelMasterDataLoader.MasterCategoryRecord> altRecords = new ArrayList<>();
        ReflectionTestUtils.invokeMethod(loader, "loadSingleExcelFile", altHeaderFile.getAbsolutePath(), altRecords);
        assertEquals(1, altRecords.size());
        assertEquals("AltDesc", altRecords.get(0).getItemDescription());

        // 13b. Header with "id", "item id", "item", "description"
        File altHeaderFile2 = new File(tempDir.toFile(), "alt_headers2.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("AltHeaders2");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("id");
            header.createCell(1).setCellValue("Category Name");
            header.createCell(2).setCellValue("Division Name");
            header.createCell(3).setCellValue("Item Description");
            header.createCell(4).setCellValue("Specification Details");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("ID-99");
            r1.createCell(1).setCellValue("Cat-99");
            r1.createCell(2).setCellValue("Div-99");
            r1.createCell(3).setCellValue("Desc-99");
            r1.createCell(4).setCellValue("Spec-99");

            // Row with blank desc and cat
            Row rBlank = sheet.createRow(2);
            rBlank.createCell(0).setCellValue("ID-Blank");

            // Short row
            Row rShort = sheet.createRow(3);
            rShort.createCell(0).setCellValue("ID-Short");
            rShort.createCell(1).setCellValue("Cat-Short");

            try (FileOutputStream fos = new FileOutputStream(altHeaderFile2)) {
                wb.write(fos);
            }
        }
        List<ExcelMasterDataLoader.MasterCategoryRecord> altRecords2 = new ArrayList<>();
        ReflectionTestUtils.invokeMethod(loader, "loadSingleExcelFile", altHeaderFile2.getAbsolutePath(), altRecords2);
        assertTrue(altRecords2.size() >= 2);

        // 14. getCellValue branches
        assertEquals("", ReflectionTestUtils.invokeMethod(loader, "getCellValue", (Cell) null));

        Cell mockCell1 = Mockito.mock(Cell.class);
        Cell mockCell2 = Mockito.mock(Cell.class);
        Cell mockCell3 = Mockito.mock(Cell.class);
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("T");
            Row r = s.createRow(0);
            Cell c1 = r.createCell(0);
            c1.setCellValue("123.0");
            assertEquals("123", ReflectionTestUtils.invokeMethod(loader, "getCellValue", c1));

            Cell c2 = r.createCell(1);
            c2.setCellValue("12.3.0");
            assertEquals("12.3.0", ReflectionTestUtils.invokeMethod(loader, "getCellValue", c2));

            Cell c3 = r.createCell(2);
            c3.setCellValue("Normal Text");
            assertEquals("Normal Text", ReflectionTestUtils.invokeMethod(loader, "getCellValue", c3));
        }

        // 15. getMatchingFileNames with sibling file and no-parent file
        File dirSibling = tempDir.toFile();
        File f1 = new File(dirSibling, "1st Set of Category Items Data.xlsx");
        File f2 = new File(dirSibling, "2nd Set of Category Items Data.xlsx");
        f1.createNewFile();
        f2.createNewFile();
        ReflectionTestUtils.setField(loader, "masterFilePath", f1.getAbsolutePath());
        List<String> namesWithSibling = ReflectionTestUtils.invokeMethod(loader, "resolveConfiguredFiles");
        assertTrue(namesWithSibling.size() >= 2);

        ReflectionTestUtils.setField(loader, "masterFilePath", "standalone_file.xlsx");
        List<String> namesStandalone = ReflectionTestUtils.invokeMethod(loader, "resolveConfiguredFiles");
        assertNotNull(namesStandalone);
    }
}
