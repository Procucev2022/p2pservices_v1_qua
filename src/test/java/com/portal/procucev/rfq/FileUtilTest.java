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
}
