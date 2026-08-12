package com.portal.procucev.rfq.service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ExcelMasterDataLoader {

    @Value("${app.category.master.file.path:1st Set of Category Items Data.xlsx}")
    private String masterFilePath;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MasterCategoryRecord {
        private String category;
        private String division;
        private String itemCode;
        private String itemDescription;
        private String specifications;
    }

    private List<MasterCategoryRecord> masterRecords = new ArrayList<>();

    @PostConstruct
    public void init() {
        loadMasterData();
    }

    public synchronized void loadMasterData() {
        log.info("Loading Category Master Data Excel files using configured path: {}", masterFilePath);
        List<MasterCategoryRecord> records = new ArrayList<>();

        List<String> fileNames = resolveConfiguredFiles();

        for (String fileName : fileNames) {
            loadSingleExcelFile(fileName, records);
        }

        this.masterRecords = records;
        log.info("Successfully loaded TOTAL {} category master records across both 1st and 2nd Excel datasets.", masterRecords.size());
    }

    private void loadSingleExcelFile(String fileName, List<MasterCategoryRecord> targetList) {
        try (InputStream is = getInputStreamForFile(fileName)) {
            if (is == null) {
                log.warn("Category dataset file '{}' not found. Skipping this set.", fileName);
                return;
            }

            try (Workbook workbook = new XSSFWorkbook(is)) {
                Sheet sheet = workbook.getSheetAt(0);
                boolean isHeader = true;

                int catCol = -1, divCol = -1, codeCol = -1, descCol = -1, specCol = -1;
                int loadedCount = 0;

                for (Row row : sheet) {
                    if (isHeader) {
                        isHeader = false;
                        boolean foundAnyHeader = false;
                        for (int i = 0; i < row.getLastCellNum(); i++) {
                            Cell cell = row.getCell(i);
                            if (cell == null) continue;
                            String headerText = getCellValue(cell).toLowerCase().trim();
                            if (headerText.isBlank()) continue;

                            if (headerText.contains("spec")) {
                                specCol = i;
                                foundAnyHeader = true;
                            } else if (headerText.contains("code") || headerText.equalsIgnoreCase("id") || headerText.endsWith(" id")) {
                                codeCol = i;
                                foundAnyHeader = true;
                            } else if (headerText.contains("category") || headerText.contains("cat")) {
                                catCol = i;
                                foundAnyHeader = true;
                            } else if (headerText.contains("division") || headerText.contains("div")) {
                                divCol = i;
                                foundAnyHeader = true;
                            } else if (headerText.contains("desc") || headerText.contains("description") || headerText.contains("item")) {
                                descCol = i;
                                foundAnyHeader = true;
                            }
                        }

                        if (!foundAnyHeader) {
                            catCol = 0;
                            divCol = 1;
                            codeCol = 2;
                            descCol = 3;
                            specCol = 4;
                        }
                        continue;
                    }

                    String cat = catCol >= 0 && catCol < row.getLastCellNum() ? getCellValue(row.getCell(catCol)) : "";
                    String div = divCol >= 0 && divCol < row.getLastCellNum() ? getCellValue(row.getCell(divCol)) : "";
                    String code = codeCol >= 0 && codeCol < row.getLastCellNum() ? getCellValue(row.getCell(codeCol)) : "";
                    String desc = descCol >= 0 && descCol < row.getLastCellNum() ? getCellValue(row.getCell(descCol)) : "";
                    String spec = specCol >= 0 && specCol < row.getLastCellNum() ? getCellValue(row.getCell(specCol)) : "";

                    if (!desc.isBlank() || !cat.isBlank()) {
                        targetList.add(MasterCategoryRecord.builder()
                                .category(cat)
                                .division(div)
                                .itemCode(code)
                                .itemDescription(desc)
                                .specifications(spec)
                                .build());
                        loadedCount++;
                    }
                }

                log.info("Loaded {} records from Excel file: {}", loadedCount, fileName);
            }
        } catch (Exception e) {
            log.error("Failed to parse Category Master Excel file '{}': {}", fileName, e.getMessage());
        }
    }

    private List<String> resolveConfiguredFiles() {
        if (masterFilePath == null || masterFilePath.isBlank()) {
            return List.of("1st Set of Category Items Data.xlsx", "2nd Set of Category Items Data.xlsx");
        }

        File configuredFile = new File(masterFilePath);
        if (configuredFile.isDirectory()) {
            List<String> files = new ArrayList<>();
            File set1 = new File(configuredFile, "1st Set of Category Items Data.xlsx");
            File set2 = new File(configuredFile, "2nd Set of Category Items Data.xlsx");
            if (set1.exists()) files.add(set1.getPath());
            if (set2.exists()) files.add(set2.getPath());
            if (files.isEmpty()) {
                File[] list = configuredFile.listFiles((dir, name) -> name.toLowerCase().endsWith(".xlsx"));
                if (list != null) {
                    for (File f : list) files.add(f.getPath());
                }
            }
            return files;
        }

        List<String> fileNames = new ArrayList<>();
        fileNames.add(masterFilePath);

        if ("1st Set of Category Items Data.xlsx".equalsIgnoreCase(configuredFile.getName())) {
            File siblingSecondSet = configuredFile.getParentFile() != null
                    ? new File(configuredFile.getParentFile(), "2nd Set of Category Items Data.xlsx")
                    : new File("2nd Set of Category Items Data.xlsx");
            if (siblingSecondSet.exists()) {
                fileNames.add(siblingSecondSet.getPath());
            }
        }

        return fileNames;
    }

    private InputStream getInputStreamForFile(String fileName) {
        try {
            File file = new File(fileName);
            if (file.exists()) return new FileInputStream(file);

            InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
            if (is != null) return is;
        } catch (Exception e) {
            log.warn("Could not open file input stream for {}: {}", fileName, e.getMessage());
        }
        return null;
    }

    private final DataFormatter dataFormatter = new DataFormatter();

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        try {
            String str = dataFormatter.formatCellValue(cell).trim();
            if (str.endsWith(".0") && str.matches("^\\d+\\.0$")) {
                str = str.substring(0, str.length() - 2);
            }
            return str;
        } catch (Exception e) {
            return cell.toString().trim();
        }
    }

    public List<MasterCategoryRecord> getMasterRecords() {
        return masterRecords;
    }
}
