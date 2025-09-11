package com.portal.procucev.service;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import com.portal.procucev.dao.ItemCategoryDao;
import com.portal.procucev.model.ItemCategory;
@Service
public class ExcelReader {
	
	@Autowired
	private ItemCategoryDao itemCategoryDao;
//    public static void main(String[] args) throws IOException, EncryptedDocumentException, InvalidFormatException {
//        FileInputStream fis = new FileInputStream("C:\\Users\\nagen\\Downloads\\Division.xlsx");
//        Workbook workbook = WorkbookFactory.create(fis);
//        Sheet sheet = workbook.getSheetAt(1);
//        System.out.println("First cell: " + sheet.getRow(0).getCell(0));
//        workbook.close();
//   
//    }}

//    public static void main(String[] args) throws IOException, EncryptedDocumentException, InvalidFormatException {
//        String excelPath = "C:\\Users\\nagen\\Downloads\\Division.xlsx";
//
//        ExcelReader reader = new ExcelReader();
//        reader.uploadExcelToDB(excelPath);  // Call the method to insert into DB
//    }

//    public void uploadExcelToDB(String excelPath) {
//    	try (FileInputStream fis = new FileInputStream(excelPath);
//    	         Workbook workbook = new XSSFWorkbook(fis)) {
//    		 FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
//    	        Sheet sheet = workbook.getSheetAt(1);
//
//    	        for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Skip header row
//    	            Row row = sheet.getRow(i);
//
//    	            if (row != null) {
//    	                ItemCategory item = new ItemCategory();
//    	                item.setId(UUID.randomUUID().toString());
//
//    	                // For serialNo: get numeric value directly if cell is numeric
//    	                Cell serialNoCell = row.getCell(0);
//    	                int serialNo = 0;
//    	                if (serialNoCell != null) {
//    	                	int cellType = serialNoCell.getCellType();
//    	                	if (cellType == Cell.CELL_TYPE_FORMULA) {
//    	                	    int evaluated = evaluator.evaluateFormulaCell(serialNoCell); // returns int
//    	                	    cellType = evaluated;
//    	                	}
//
//    	                	if (cellType == Cell.CELL_TYPE_NUMERIC) {
//    	                	    serialNo = (int) serialNoCell.getNumericCellValue();
//    	                	} else if (cellType == Cell.CELL_TYPE_STRING) {
//    	                	    String val = serialNoCell.getStringCellValue().trim();
//    	                	    if (!val.isEmpty()) {
//    	                	        serialNo = Integer.parseInt(val);
//    	                	    }
//    	                	}
//    	                }
//    	                System.out.println("serial no---" + serialNo);
//    	                item.setSerialNo(serialNo);
//
//    	                item.setDivision(getCellValue(row.getCell(1)));
//    	                item.setCategory(getCellValue(row.getCell(2)));
//    	                item.setItem(getCellValue(row.getCell(3)));
//    	                item.setCreatedTS(new Date());
//
//    	                // Save to DB
//    	                itemCategoryDao.save(item);
//    	            }
//    	        }
//
//    	    } catch (Exception e) {
//    	        e.printStackTrace();
//    	    }
//    }
	private String getCellValue(Cell cell) {
	    if (cell == null) {
	        return "";
	    }

	    switch (cell.getCellType()) {
	        case Cell.CELL_TYPE_STRING:
	            return cell.getStringCellValue().trim();
	        case Cell.CELL_TYPE_NUMERIC:
	            if (DateUtil.isCellDateFormatted(cell)) {
	                return cell.getDateCellValue().toString();
	            } else {
	                return String.valueOf(cell.getNumericCellValue());
	            }
	        case Cell.
	        CELL_TYPE_BOOLEAN:
	            return String.valueOf(cell.getBooleanCellValue());
	       
	        default:
	            return "";
	    }

	}
	

    public void uploadExcelToDB(InputStream inputStream) {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = workbook.getSheetAt(1);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Skip header row
                Row row = sheet.getRow(i);
                if (row != null) {
                    ItemCategory item = new ItemCategory();
                    item.setId(UUID.randomUUID().toString());

                    // Serial No
                    Cell serialNoCell = row.getCell(0);
                    int serialNo = 0;
                    if (serialNoCell != null) {
	                	int cellType = serialNoCell.getCellType();
	                	if (cellType == Cell.CELL_TYPE_FORMULA) {
	                	    int evaluated = evaluator.evaluateFormulaCell(serialNoCell); // returns int
	                	    cellType = evaluated;
	                	}

	                	if (cellType == Cell.CELL_TYPE_NUMERIC) {
	                	    serialNo = (int) serialNoCell.getNumericCellValue();
	                	} else if (cellType == Cell.CELL_TYPE_STRING) {
	                	    String val = serialNoCell.getStringCellValue().trim();
	                	    if (!val.isEmpty()) {
	                	        serialNo = Integer.parseInt(val);
	                	    }
	                	}
	                }
	                System.out.println("serial no---" + serialNo);
	                item.setSerialNo(serialNo);

                    item.setDivision(getCellValue(row.getCell(1)));
                    item.setCategory(getCellValue(row.getCell(2)));
                    item.setItem(getCellValue(row.getCell(3)));
                    item.setCreatedTS(new Date());

                    // Save to DB
                    itemCategoryDao.save(item);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}