package com.portal.procucev.rfq;

import com.portal.procucev.rfq.util.FileUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    @DisplayName("Test extractTextFromFile with Excel file")
    void testExtractTextFromExcel() throws Exception {
        File excelFile = new File(tempDir.toFile(), "sample.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Test");
            Row row = sheet.createRow(0);
            Cell cell1 = row.createCell(0);
            cell1.setCellValue("Item Name");
            Cell cell2 = row.createCell(1);
            cell2.setCellValue("Quantity");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        String extracted = FileUtil.extractTextFromFile(excelFile);
        assertTrue(extracted.contains("Item Name"));
        assertTrue(extracted.contains("Quantity"));
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
    }

    @Test
    @DisplayName("Test vision image MIME types and checks")
    void testVisionImageMethods() throws Exception {
        assertNull(FileUtil.visionImageMimeType(null));
        assertNull(FileUtil.visionImageMimeType(new File("nodotfile")));
        assertNull(FileUtil.visionImageMimeType(new File("dotatend.")));
        assertNull(FileUtil.visionImageMimeType(new File("test.bmp")));

        assertEquals("image/png", FileUtil.visionImageMimeType(new File("photo.png")));
        assertEquals("image/jpeg", FileUtil.visionImageMimeType(new File("photo.jpg")));
        assertEquals("image/jpeg", FileUtil.visionImageMimeType(new File("photo.jpeg")));
        assertEquals("image/webp", FileUtil.visionImageMimeType(new File("photo.webp")));
        assertEquals("image/heic", FileUtil.visionImageMimeType(new File("photo.heic")));
        assertEquals("image/heif", FileUtil.visionImageMimeType(new File("photo.heif")));

        assertTrue(FileUtil.isVisionImage(new File("photo.PNG")));
        assertFalse(FileUtil.isVisionImage(new File("document.pdf")));

        File imageFile = new File(tempDir.toFile(), "test.png");
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            fos.write(new byte[]{1, 2, 3});
        }
        // Image files return empty string in extractTextFromFile because they are handled by vision
        assertEquals("", FileUtil.extractTextFromFile(imageFile));
    }

    @Test
    @DisplayName("Test readAsBase64 method")
    void testReadAsBase64() throws Exception {
        assertNull(FileUtil.readAsBase64(null));
        assertNull(FileUtil.readAsBase64(new File(tempDir.toFile(), "missing.png")));

        File sampleFile = new File(tempDir.toFile(), "sample_data.txt");
        try (FileOutputStream fos = new FileOutputStream(sampleFile)) {
            fos.write("Hello Base64".getBytes());
        }
        String base64 = FileUtil.readAsBase64(sampleFile);
        assertNotNull(base64);
        assertFalse(base64.isBlank());

        // Test exception branch in readAsBase64 (passing a directory causes Files.readAllBytes to fail)
        assertNull(FileUtil.readAsBase64(tempDir.toFile()));
    }

    @Test
    @DisplayName("Test extractTextFromFile with Word docx file")
    void testExtractTextFromDocx() throws Exception {
        File docxFile = new File(tempDir.toFile(), "sample.docx");
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph p = doc.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun r = p.createRun();
            r.setText("Sample Word Content for RFQ");
            try (FileOutputStream fos = new FileOutputStream(docxFile)) {
                doc.write(fos);
            }
        }
        String extracted = FileUtil.extractTextFromFile(docxFile);
        assertTrue(extracted.contains("Sample Word Content for RFQ"));
    }

    @Test
    @DisplayName("Test extractTextFromFile with Excel formulas, empty rows, and multiple extensions")
    void testExtractExcelAdvanced() throws Exception {
        File xlsxFile = new File(tempDir.toFile(), "formula.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Calculations");
            Row r0 = sheet.createRow(0);
            r0.createCell(0).setCellValue(10);
            r0.createCell(1).setCellValue(20);
            Cell sumCell = r0.createCell(2);
            sumCell.setCellFormula("A1+B1");

            // Empty row
            sheet.createRow(1);

            // Row with blank cells
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("Total");
            r2.createCell(2).setCellValue(30);

            try (FileOutputStream fos = new FileOutputStream(xlsxFile)) {
                wb.write(fos);
            }
        }
        String extracted = FileUtil.extractTextFromFile(xlsxFile);
        assertTrue(extracted.contains("Calculations"));
        assertTrue(extracted.contains("Total"));

        // Test with .xls and .xlsm filenames
        File xlsFile = new File(tempDir.toFile(), "sample.xls");
        try (FileOutputStream fos = new FileOutputStream(xlsFile)) {
            java.nio.file.Files.copy(xlsxFile.toPath(), fos);
        }
        FileUtil.extractTextFromFile(xlsFile);

        File xlsmFile = new File(tempDir.toFile(), "sample.xlsm");
        try (FileOutputStream fos = new FileOutputStream(xlsmFile)) {
            java.nio.file.Files.copy(xlsxFile.toPath(), fos);
        }
        FileUtil.extractTextFromFile(xlsmFile);
    }
}
