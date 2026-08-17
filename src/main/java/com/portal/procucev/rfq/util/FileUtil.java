package com.portal.procucev.rfq.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

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
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls") || filename.endsWith(".xlsm")) {
                return extractExcelText(file);
            } else if (filename.endsWith(".docx")) {
                return extractWordText(file);
            } else if (filename.endsWith(".txt") || filename.endsWith(".csv")) {
                return extractPlainText(file);
            }
            // Unsupported types previously returned "" indistinguishably from an empty
            // attachment, which made attachment-driven extraction failures undiagnosable.
            log.warn("Unsupported attachment type '{}' - no text extracted. Supported: .pdf, .xlsx, .xls, .xlsm, .docx, .txt, .csv",
                    file.getName());
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

    /**
     * Flattens a spreadsheet to text for AI extraction.
     *
     * <p>Iterates by column index rather than with {@code for (Cell cell : row)}. POI's cell
     * iterator visits only physically present cells, so a row with an empty column silently
     * shifts its remaining values left and the quantity column stops lining up with the header.
     * On a multi-row requirement sheet that is enough to make quantities unreadable, which the
     * model then correctly reports as missing.
     *
     * <p>Values are rendered with {@link DataFormatter} plus a {@link FormulaEvaluator} so a
     * formula cell yields its computed value instead of the formula text, and are separated by
     * an explicit delimiter so column boundaries survive.
     */
    private static String extractExcelText(File excelFile) throws Exception {
        StringBuilder sb = new StringBuilder();
        DataFormatter formatter = new DataFormatter();
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = WorkbookFactory.create(fis)) {

            FormulaEvaluator evaluator = null;
            try {
                evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            } catch (Exception e) {
                log.warn("Could not create formula evaluator for {}: {}. Formula cells will render as formulas.",
                        excelFile.getName(), e.getMessage());
            }

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sb.append("--- Sheet: ").append(sheet.getSheetName()).append(" ---\n");

                for (Row row : sheet) {
                    if (row == null) {
                        sb.append("\n");
                        continue;
                    }
                    int lastCell = row.getLastCellNum();
                    if (lastCell <= 0) {
                        sb.append("\n");
                        continue;
                    }
                    StringBuilder rowSb = new StringBuilder();
                    boolean rowHasValue = false;
                    for (int c = 0; c < lastCell; c++) {
                        if (c > 0) {
                            rowSb.append(" | ");
                        }
                        Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        String value = formatCell(cell, formatter, evaluator);
                        if (!value.isEmpty()) {
                            rowHasValue = true;
                        }
                        rowSb.append(value);
                    }
                    if (rowHasValue) {
                        sb.append(rowSb).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String formatCell(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        try {
            String value = evaluator != null
                    ? formatter.formatCellValue(cell, evaluator)
                    : formatter.formatCellValue(cell);
            return value != null ? value.trim() : "";
        } catch (Exception e) {
            // A broken formula or unsupported function must not lose the rest of the sheet.
            try {
                String raw = formatter.formatCellValue(cell);
                return raw != null ? raw.trim() : "";
            } catch (Exception ignored) {
                return "";
            }
        }
    }

    private static String extractWordText(File wordFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(wordFile);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            return text != null ? text : "";
        }
    }

    private static String extractPlainText(File textFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(textFile)) {
            // Was platform-default charset, which mangles UTF-8 CSVs on a Linux/Windows mismatch.
            return new String(fis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
