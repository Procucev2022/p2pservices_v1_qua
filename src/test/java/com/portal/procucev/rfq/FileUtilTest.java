package com.portal.procucev.rfq;

import com.portal.procucev.rfq.util.FileUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileUtilTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Test extractTextFromFile with null or non-existent file")
    void testExtractTextFromFileNullOrNonExistent() {
        assertEquals("", FileUtil.extractTextFromFile(null));
        File nonExistent = new File(tempDir.toFile(), "non_existent.txt");
        assertEquals("", FileUtil.extractTextFromFile(nonExistent));
    }

    @Test
    @DisplayName("Test extractTextFromFile with plain text and csv files")
    void testExtractTextFromPlainTextAndCsv() throws Exception {
        File txtFile = new File(tempDir.toFile(), "sample.txt");
        try (FileOutputStream fos = new FileOutputStream(txtFile)) {
            fos.write("Hello Text File".getBytes());
        }
        assertEquals("Hello Text File", FileUtil.extractTextFromFile(txtFile));

        File csvFile = new File(tempDir.toFile(), "sample.csv");
        try (FileOutputStream fos = new FileOutputStream(csvFile)) {
            fos.write("Col1,Col2\nVal1,Val2".getBytes());
        }
        assertEquals("Col1,Col2\nVal1,Val2", FileUtil.extractTextFromFile(csvFile));
    }

    @Test
    @DisplayName("Test extractTextFromFile with PDF file")
    void testExtractTextFromPdf() throws Exception {
        File pdfFile = new File(tempDir.toFile(), "sample.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
                contents.beginText();
                contents.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contents.newLineAtOffset(100, 700);
                contents.showText("Sample PDF Requirement");
                contents.endText();
            }
            doc.save(pdfFile);
        }
        String extracted = FileUtil.extractTextFromFile(pdfFile);
        assertTrue(extracted.contains("Sample PDF Requirement"));
    }

    @Test
    @DisplayName("Test extractTextFromFile with Excel file having multiple rows, formulas and blank cells")
    void testExtractTextFromExcel() throws Exception {
        File excelFile = new File(tempDir.toFile(), "sample.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row row0 = sheet.createRow(0);
            Cell cell1 = row0.createCell(0);
            cell1.setCellValue("Item Name");
            Cell cell2 = row0.createCell(1);
            cell2.setCellValue("Quantity");

            Row row1 = sheet.createRow(1);
            Cell cell11 = row1.createCell(0);
            cell11.setCellValue("Widget");
            Cell cell12 = row1.createCell(1);
            cell12.setCellFormula("2+3");

            // Empty row
            sheet.createRow(2);

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        String extracted = FileUtil.extractTextFromFile(excelFile);
        assertTrue(extracted.contains("Item Name"));
        assertTrue(extracted.contains("Quantity"));
        assertTrue(extracted.contains("Widget"));
    }

    @Test
    @DisplayName("Test extractTextFromFile with Word docx file")
    void testExtractTextFromWord() throws Exception {
        File docxFile = new File(tempDir.toFile(), "sample.docx");
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(docxFile)) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun r = p.createRun();
            r.setText("Sample Word document text");
            doc.write(fos);
        }
        String extracted = FileUtil.extractTextFromFile(docxFile);
        assertTrue(extracted.contains("Sample Word document text"));
    }

    @Test
    @DisplayName("Test extractTextFromFile with vision images")
    void testExtractTextFromVisionImages() throws Exception {
        File pngFile = new File(tempDir.toFile(), "sample.png");
        try (FileOutputStream fos = new FileOutputStream(pngFile)) {
            fos.write(new byte[]{1, 2, 3});
        }
        assertEquals("", FileUtil.extractTextFromFile(pngFile));
        assertTrue(FileUtil.isVisionImage(pngFile));
        assertEquals("image/png", FileUtil.visionImageMimeType(pngFile));

        File jpgFile = new File(tempDir.toFile(), "sample.jpg");
        try (FileOutputStream fos = new FileOutputStream(jpgFile)) {
            fos.write(new byte[]{1, 2, 3});
        }
        assertEquals("image/jpeg", FileUtil.visionImageMimeType(jpgFile));

        File webpFile = new File(tempDir.toFile(), "sample.webp");
        assertEquals("image/webp", FileUtil.visionImageMimeType(webpFile));

        File heicFile = new File(tempDir.toFile(), "sample.heic");
        assertEquals("image/heic", FileUtil.visionImageMimeType(heicFile));

        File heifFile = new File(tempDir.toFile(), "sample.heif");
        assertEquals("image/heif", FileUtil.visionImageMimeType(heifFile));
    }

    @Test
    @DisplayName("Test visionImageMimeType edge cases")
    void testVisionImageMimeTypeEdges() {
        assertNull(FileUtil.visionImageMimeType(null));
        assertNull(FileUtil.visionImageMimeType(new File("noextension")));
        assertNull(FileUtil.visionImageMimeType(new File("dotatend.")));
        assertNull(FileUtil.visionImageMimeType(new File("unknown.xyz")));
    }

    @Test
    @DisplayName("Test readAsBase64")
    void testReadAsBase64() throws Exception {
        assertNull(FileUtil.readAsBase64(null));
        assertNull(FileUtil.readAsBase64(new File(tempDir.toFile(), "notfound.bin")));

        File testFile = new File(tempDir.toFile(), "base64test.txt");
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write("Hello Base64".getBytes());
        }
        String encoded = FileUtil.readAsBase64(testFile);
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
    }

    @Test
    @DisplayName("Test extractTextFromFile with unsupported file format")
    void testExtractTextFromFileUnsupportedFormat() throws Exception {
        File binFile = new File(tempDir.toFile(), "sample.bin");
        try (FileOutputStream fos = new FileOutputStream(binFile)) {
            fos.write(new byte[]{0, 1, 2, 3});
        }
        assertEquals("", FileUtil.extractTextFromFile(binFile));
    }

    @Test
    @DisplayName("Test private constructor execution")
    void testPrivateConstructor() throws Exception {
        Constructor<FileUtil> constructor = FileUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        FileUtil instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("Test extractTextFromFile with corrupted files that cause exceptions")
    void testExtractTextFromFileCorruptedFiles() throws Exception {
        File badPdf = new File(tempDir.toFile(), "corrupt.pdf");
        try (FileOutputStream fos = new FileOutputStream(badPdf)) {
            fos.write("Not a pdf content".getBytes());
        }
        assertEquals("", FileUtil.extractTextFromFile(badPdf));

        File badXlsx = new File(tempDir.toFile(), "corrupt.xlsx");
        try (FileOutputStream fos = new FileOutputStream(badXlsx)) {
            fos.write("Not an excel content".getBytes());
        }
        assertEquals("", FileUtil.extractTextFromFile(badXlsx));

        File badDocx = new File(tempDir.toFile(), "corrupt.docx");
        try (FileOutputStream fos = new FileOutputStream(badDocx)) {
            fos.write("Not a docx content".getBytes());
        }
        assertEquals("", FileUtil.extractTextFromFile(badDocx));
    }

    @Test
    @DisplayName("Test extractExcelText with null rows, empty rows, missing cells, and broken formulas")
    void testExtractExcelAdvanced() throws Exception {
        File xlsmFile = new File(tempDir.toFile(), "advanced.xlsm");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("AdvancedSheet");
            
            // Row 0 with missing cell in middle
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("Header1");
            Cell c2 = r0.createCell(2); // cell 1 is intentionally null
            c2.setCellValue("Header3");

            // Row 1 with broken formula
            Row r1 = sheet.createRow(1);
            Cell c1_0 = r1.createCell(0);
            c1_0.setCellValue("Data1");
            Cell c1_1 = r1.createCell(1);
            c1_1.setCellFormula("NONEXISTENT_FORMULA()");

            // Row 2 is empty row with 0 cells
            sheet.createRow(2);

            try (FileOutputStream fos = new FileOutputStream(xlsmFile)) {
                wb.write(fos);
            }
        }
        String extracted = FileUtil.extractTextFromFile(xlsmFile);
        assertTrue(extracted.contains("Header1"));
        assertTrue(extracted.contains("Header3"));

        File xlsFile = new File(tempDir.toFile(), "test.xls");
        try (FileOutputStream fos = new FileOutputStream(xlsFile)) {
            fos.write(new byte[]{0, 1, 2});
        }
        assertEquals("", FileUtil.extractTextFromFile(xlsFile));

        // Test with row having only empty string cells
        File blankCellsFile = new File(tempDir.toFile(), "blank_cells.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("BlankSheet");
            Row r = sheet.createRow(0);
            r.createCell(0).setCellValue("");
            r.createCell(1).setCellValue("");
            try (FileOutputStream fos = new FileOutputStream(blankCellsFile)) {
                wb.write(fos);
            }
        }
        String blankExtracted = FileUtil.extractTextFromFile(blankCellsFile);
        assertNotNull(blankExtracted);
    }

    @Test
    @DisplayName("Test formatCell direct branches")
    void testFormatCellDirectBranches() {
        assertEquals("", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                FileUtil.class, "formatCell", null, null, null));

        org.apache.poi.ss.usermodel.DataFormatter mockFormatter = Mockito.mock(org.apache.poi.ss.usermodel.DataFormatter.class);
        org.apache.poi.ss.usermodel.FormulaEvaluator mockEvaluator = Mockito.mock(org.apache.poi.ss.usermodel.FormulaEvaluator.class);
        Cell mockCell = Mockito.mock(Cell.class);

        // Null evaluator branch
        DataFormatter fmt1 = Mockito.mock(DataFormatter.class);
        Cell cell1 = Mockito.mock(Cell.class);
        Mockito.when(fmt1.formatCellValue(cell1)).thenReturn("Cell Val");
        String v1 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                FileUtil.class, "formatCell", cell1, fmt1, null);
        assertEquals("Cell Val", v1);

        // Evaluator throws exception, raw formatter succeeds
        DataFormatter fmt2 = Mockito.mock(DataFormatter.class);
        FormulaEvaluator eval2 = Mockito.mock(FormulaEvaluator.class);
        Cell cell2 = Mockito.mock(Cell.class);
        Mockito.when(fmt2.formatCellValue(cell2, eval2)).thenThrow(new RuntimeException("evaluator fail"));
        Mockito.when(fmt2.formatCellValue(cell2)).thenReturn("Cell Val");
        String v2 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                FileUtil.class, "formatCell", cell2, fmt2, eval2);
        assertEquals("Cell Val", v2);

        // Evaluator throws and raw formatter also throws
        DataFormatter fmt3 = Mockito.mock(DataFormatter.class);
        FormulaEvaluator eval3 = Mockito.mock(FormulaEvaluator.class);
        Cell cell3 = Mockito.mock(Cell.class);
        Mockito.when(fmt3.formatCellValue(cell3, eval3)).thenThrow(new RuntimeException("evaluator fail"));
        Mockito.when(fmt3.formatCellValue(cell3)).thenThrow(new RuntimeException("raw fail"));
        String v3 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                FileUtil.class, "formatCell", cell3, fmt3, eval3);
        assertEquals("", v3);

        // formatCellValue returning null directly
        DataFormatter fmt4 = Mockito.mock(DataFormatter.class);
        FormulaEvaluator eval4 = Mockito.mock(FormulaEvaluator.class);
        Cell cell4 = Mockito.mock(Cell.class);
        Mockito.when(fmt4.formatCellValue(cell4, eval4)).thenReturn(null);
        String v4 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                FileUtil.class, "formatCell", cell4, fmt4, eval4);
        assertEquals("", v4);

        // formatCellValue returning null in fallback
        DataFormatter fmt5 = Mockito.mock(DataFormatter.class);
        FormulaEvaluator eval5 = Mockito.mock(FormulaEvaluator.class);
        Cell cell5 = Mockito.mock(Cell.class);
        Mockito.when(fmt5.formatCellValue(cell5, eval5)).thenThrow(new RuntimeException("eval fail"));
        Mockito.when(fmt5.formatCellValue(cell5)).thenReturn(null);
        String v5 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                FileUtil.class, "formatCell", cell5, fmt5, eval5);
        assertEquals("", v5);
    }
}
