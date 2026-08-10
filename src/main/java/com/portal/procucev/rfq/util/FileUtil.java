package com.portal.procucev.rfq.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;

@Slf4j
public final class FileUtil {

    private FileUtil() {
    }

    public static String extractTextFromFile(File file) {
        if (file == null || !file.exists()) return "";
        String filename = file.getName().toLowerCase();
        try {
            if (filename.endsWith(".pdf")) {
                return extractPdfText(file);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return extractExcelText(file);
            } else if (filename.endsWith(".txt") || filename.endsWith(".csv")) {
                return extractPlainText(file);
            }
        } catch (Exception e) {
            log.error("Error extracting text from file {}: {}", file.getName(), e.getMessage());
        }
        return "";
    }

    private static String extractPdfText(File pdfFile) throws Exception {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static String extractExcelText(File excelFile) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        sb.append(cell.toString()).append(" ");
                    }
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    private static String extractPlainText(File textFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(textFile)) {
            return new String(fis.readAllBytes());
        }
    }
}
