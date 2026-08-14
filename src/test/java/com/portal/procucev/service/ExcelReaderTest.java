package com.portal.procucev.service;

import com.portal.procucev.dao.ItemCategoryDao;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExcelReaderTest {

    @Mock
    private ItemCategoryDao itemCategoryDao;

    @InjectMocks
    private ExcelReader excelReader;

    @Test
    void testUploadExcelToDB_WithVariousCells() throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Sheet1");

        // Row 0: Header
        XSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue("Serial");

        // Row 1: Numeric serial, String division, Date category, Boolean item
        XSSFRow row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue(1.0);
        row1.createCell(1).setCellValue("Div1");

        XSSFCell dateCell = row1.createCell(2);
        dateCell.setCellValue(new Date());
        // CellStyle with date format
        org.apache.poi.ss.usermodel.CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
        dateCell.setCellStyle(dateStyle);

        row1.createCell(3).setCellValue(true);
        row1.createCell(4).setCellValue(999.50); // numeric non-date cell

        // Row 2: String serial, empty cells
        XSSFRow row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("200");
        row2.createCell(1).setCellValue(""); // empty string

        // Row 3: Null row skip
        // Row 4: Empty string serialNo, blank cell type
        XSSFRow row4 = sheet.createRow(4);
        row4.createCell(0).setCellValue("");
        row4.createCell(1); // BLANK cell type

        // Row 5: Formula serialNo
        XSSFRow row5 = sheet.createRow(5);
        XSSFCell fCell = row5.createCell(0);
        fCell.setCellFormula("1+2");

        // Row 6: Default cell type (Error) and null serial cell
        XSSFRow row6 = sheet.createRow(6);
        row6.createCell(1).setCellErrorValue((byte) 0);

        // Row 7: Whitespace string serial, boolean serial
        XSSFRow row7 = sheet.createRow(7);
        row7.createCell(0).setCellValue("   ");
        XSSFRow row8 = sheet.createRow(8);
        row8.createCell(0).setCellValue(true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertDoesNotThrow(() -> excelReader.uploadExcelToDB(bais));
        verify(itemCategoryDao, atLeastOnce()).save(any());
    }

    @Test
    void testUploadExcelToDB_InvalidStream_CatchException() {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[]{1, 2, 3});
        assertDoesNotThrow(() -> excelReader.uploadExcelToDB(bais));
    }
}
