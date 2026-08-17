package com.portal.procucev.rfq;

import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.service.RFQBuilderService;
import com.portal.procucev.rfq.util.FileUtil;
import com.portal.procucev.rfq.util.QuantityNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression cover for two production Email-RFQ defects:
 *
 * <ol>
 *   <li>"Gear Box Seal 40x52x7, Qty: 12" created an RFQ with no description or specification.</li>
 *   <li>A sports-consumables request with two attachments (52 and 84 line items) was rejected
 *       with "quantity missing" even though the attachments listed quantities.</li>
 * </ol>
 */
class EmailRfqExtractionRegressionTest {

    @TempDir
    Path tempDir;

    private RFQBuilderService rfqBuilderService;

    @BeforeEach
    void setUp() {
        rfqBuilderService = new RFQBuilderService(new DateParser(), Mockito.mock(PincodeDao.class));
    }

    private Buyer buyer() {
        return Buyer.builder().name("Vikram").email("v@test.com").orgId("10").userId("20").build();
    }

    private RFQRequest build(RFQItem... items) {
        return rfqBuilderService.buildRFQRequest(
                ExtractedRFQ.builder()
                        .deliveryLocation("Nagpur, 440001, Maharashtra")
                        .deliveryDate("2026-08-25")
                        .items(List.of(items))
                        .build(),
                buyer(), "Requirement", null);
    }

    // ---------- Issue 1: description / specification loss ----------

    @Test
    void gearBoxSealKeepsDimensionsInDescriptionAndSpec() {
        RFQRequest req = build(RFQItem.builder()
                .itemDescription("Gear Box Seal 40x52x7")
                .specification("40x52x7")
                .quantity(12.0)
                .build());

        RFQRequest.RfqItemDto item = req.getRfqItem().get(0);
        assertEquals("Gear Box Seal 40x52x7", item.getDescription());
        assertEquals("40x52x7", item.getRemarks());
    }

    @Test
    void unicodeMultiplicationSignBecomesAsciiXNotHyphens() {
        // Word/Excel/Gmail autocorrect "40x52x7" into "40×52×7" (U+00D7).
        RFQRequest req = build(RFQItem.builder()
                .itemDescription("Gear Box Seal 40\u00D752\u00D77")
                .specification("40\u00D752\u00D77")
                .quantity(12.0)
                .build());

        RFQRequest.RfqItemDto item = req.getRfqItem().get(0);
        assertEquals("Gear Box Seal 40x52x7", item.getDescription(), "x separator must survive sanitisation");
        assertEquals("40x52x7", item.getRemarks());
        assertFalse(item.getDescription().contains("-"), "must not degrade into 40-52-7");
    }

    @Test
    void blankRemarksNoLongerYieldsEmptySpecification() {
        // Single-item branch previously only null-checked remarks, so "" produced an empty spec.
        RFQRequest req = build(RFQItem.builder()
                .itemDescription("Gear Box Seal 40x52x7")
                .specification(null)
                .remarks("")
                .quantity(12.0)
                .build());

        String remarks = req.getRfqItem().get(0).getRemarks();
        assertNotNull(remarks);
        assertFalse(remarks.isBlank(), "specification must never be persisted blank");
        assertEquals("Gear Box Seal 40x52x7", remarks);
    }

    @Test
    void specificationAliasesBindInsteadOfBeingSilentlyDropped() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (String key : List.of("specifications", "specs", "spec", "size", "dimensions", "technicalSpecification")) {
            RFQItem item = mapper.readValue(
                    "{\"itemDescription\":\"Gear Box Seal\",\"" + key + "\":\"40x52x7\",\"quantity\":12}",
                    RFQItem.class);
            assertEquals("40x52x7", item.getSpecification(), "key '" + key + "' should bind to specification");
        }
    }

    @Test
    void quantityAliasesBind() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (String key : List.of("qty", "quantityRequired", "required_quantity", "orderQuantity")) {
            RFQItem item = mapper.readValue(
                    "{\"itemDescription\":\"Shuttlecock\",\"" + key + "\":24}", RFQItem.class);
            assertEquals(24.0, item.getQuantity(), "key '" + key + "' should bind to quantity");
        }
    }

    // ---------- Issue 2: quantity wrongly reported missing ----------

    @Test
    void tradeUnitsUsedBySportsConsumablesNormaliseCorrectly() {
        assertEquals(10.0, QuantityNormalizer.normalize("10 Pairs"));
        assertEquals(1.0, QuantityNormalizer.normalize("1 Set"));
        assertEquals(2.0, QuantityNormalizer.normalize("2 Dozen"));
        assertEquals(24.0, QuantityNormalizer.normalize("24 Packets"));
        assertEquals(6.0, QuantityNormalizer.normalize("6 Box"));
        assertEquals(3.0, QuantityNormalizer.normalize("3 Piece"));
        assertEquals(12.0, QuantityNormalizer.normalize("12 Bundles"));
        assertEquals(5.0, QuantityNormalizer.normalize("5 Cartons"));
        // Regression guard: specification numbers must still be refused.
        assertNull(QuantityNormalizer.normalize("2 Ton"));
        assertNull(QuantityNormalizer.normalize("16GB"));
        assertNull(QuantityNormalizer.normalize("150W"));
    }

    @Test
    void excelWithBlankCellsKeepsColumnAlignment() throws Exception {
        File xlsx = tempDir.resolve("requirement.xlsx").toFile();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Consumables");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Sr");
            header.createCell(1).setCellValue("Item");
            header.createCell(2).setCellValue("Brand");
            header.createCell(3).setCellValue("Qty");

            // Brand column deliberately left empty: the old cell-iterator shifted Qty left into it.
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue(1);
            r1.createCell(1).setCellValue("Shuttlecock");
            r1.createCell(3).setCellValue(24);

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue(2);
            r2.createCell(1).setCellValue("Cricket Ball");
            r2.createCell(2).setCellValue("SG");
            r2.createCell(3).setCellValue(50);

            try (FileOutputStream out = new FileOutputStream(xlsx)) {
                wb.write(out);
            }
        }

        String text = FileUtil.extractTextFromFile(xlsx);
        assertTrue(text.contains("--- Sheet: Consumables ---"), "sheet name should be present");

        String[] lines = text.split("\\R");
        String shuttleLine = null;
        for (String line : lines) {
            if (line.contains("Shuttlecock")) {
                shuttleLine = line;
            }
        }
        assertNotNull(shuttleLine);
        // 4 columns => 3 delimiters, with the empty Brand cell preserved as an empty field.
        assertEquals(4, shuttleLine.split("\\|", -1).length,
                "blank Brand cell must be preserved so Qty stays in column 4: " + shuttleLine);
        assertTrue(shuttleLine.trim().endsWith("24"), "quantity must remain the last column: " + shuttleLine);
        assertTrue(text.contains("Cricket Ball"));
    }

    @Test
    void unsupportedAttachmentTypeReturnsEmptyWithoutThrowing() throws Exception {
        File odt = tempDir.resolve("spec.odt").toFile();
        java.nio.file.Files.writeString(odt.toPath(), "irrelevant");
        assertEquals("", FileUtil.extractTextFromFile(odt));
    }
}
