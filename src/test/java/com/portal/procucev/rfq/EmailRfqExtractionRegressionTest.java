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
import com.portal.procucev.service.AutomaticRfqService;
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
        AutomaticRfqService automaticRfqService = Mockito.mock(AutomaticRfqService.class);
        Mockito.when(automaticRfqService.generateRfqId("RFQ")).thenReturn("RFQ260825000001");
        rfqBuilderService = new RFQBuilderService(new DateParser(), Mockito.mock(PincodeDao.class), automaticRfqService);
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
        // brand carries the Specification column (see RFQBuilderService for the legacy mapping).
        assertEquals("40x52x7", item.getBrand());
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
        assertEquals("40x52x7", item.getBrand());
        assertFalse(item.getDescription().contains("-"), "must not degrade into 40-52-7");
    }

    @Test
    void blankRemarksNoLongerYieldsEmptySpecification() {
        RFQRequest req = build(RFQItem.builder()
                .itemDescription("Gear Box Seal 40x52x7")
                .specification(null)
                .remarks("")
                .quantity(12.0)
                .build());

        String specification = req.getRfqItem().get(0).getBrand();
        assertNotNull(specification);
        assertFalse(specification.isBlank(), "specification must never be persisted blank");
        assertEquals("Gear Box Seal 40x52x7", specification);
    }

    // ---------- Field placement: Specification vs Remarks ----------

    @Test
    void brandGoesToRemarksAndTechnicalDetailGoesToSpecification() {
        RFQRequest req = build(RFQItem.builder()
                .itemDescription("Oil Seal 25x47x7")
                .specification("25x47x7, Nitrile rubber")
                .brand("SKF")
                .quantity(50.0)
                .build());

        RFQRequest.RfqItemDto item = req.getRfqItem().get(0);
        assertEquals("25x47x7, Nitrile rubber", item.getBrand(), "Specification column must hold technical detail");
        assertEquals("Brand: SKF", item.getRemarks(), "Remarks column must hold the brand");
        assertFalse(item.getBrand().contains("SKF"), "brand must not leak into the Specification column");
    }

    @Test
    void multiItemWithoutSpecificationDoesNotLeakBrandIntoSpecification() {
        // The old multi-item fallback built the spec as "<brand> <description> - <qty> Units".
        RFQRequest req = build(
                RFQItem.builder().itemDescription("Badminton Racket").brand("Yonex").quantity(10.0).build(),
                RFQItem.builder().itemDescription("Shuttlecock").brand("Li-Ning").quantity(24.0).build());

        RFQRequest.RfqItemDto racket = req.getRfqItem().get(0);
        assertFalse(racket.getBrand().contains("Yonex"), "Specification must not contain the brand");
        assertFalse(racket.getBrand().contains("Units"), "Specification must not contain a quantity blurb");
        assertEquals("Badminton Racket", racket.getBrand());
        assertEquals("Brand: Yonex", racket.getRemarks());

        RFQRequest.RfqItemDto shuttle = req.getRfqItem().get(1);
        assertEquals("Brand: Li-Ning", shuttle.getRemarks());
        assertFalse(shuttle.getBrand().contains("Li-Ning"));
    }

    @Test
    void partNumberIsSurfacedInSpecificationAndKeptInItemCode() {
        RFQRequest req = build(RFQItem.builder()
                .itemDescription("Bearing")
                .partCode("6205ZZ")
                .specification("Deep groove, 25x52x15")
                .brand("SKF")
                .quantity(24.0)
                .build());

        RFQRequest.RfqItemDto item = req.getRfqItem().get(0);
        assertEquals("6205ZZ", item.getItemcode());
        assertTrue(item.getBrand().contains("6205ZZ"), "part number should appear in Specification: " + item.getBrand());
        assertTrue(item.getBrand().contains("Deep groove"));
        assertEquals("Brand: SKF", item.getRemarks());
    }

    @Test
    void partNumberIsNotDuplicatedWhenAlreadyInSpecification() {
        RFQRequest req = build(RFQItem.builder()
                .itemDescription("Bearing")
                .partCode("6205ZZ")
                .specification("6205ZZ deep groove")
                .quantity(24.0)
                .build());

        String specification = req.getRfqItem().get(0).getBrand();
        assertEquals("6205ZZ deep groove", specification);
        assertEquals(1, countOccurrences(specification, "6205ZZ"));
    }

    @Test
    void missingBrandStillLabelsRemarksExplicitly() {
        RFQRequest req = build(RFQItem.builder()
                .itemDescription("Gear Box Seal 40x52x7")
                .specification("40x52x7")
                .quantity(12.0)
                .build());

        assertEquals("Brand: Not Specified", req.getRfqItem().get(0).getRemarks());
    }

    private int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = haystack.indexOf(needle);
        while (idx >= 0) {
            count++;
            idx = haystack.indexOf(needle, idx + needle.length());
        }
        return count;
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
    void excelDeliveryLocationRowSurvivesFlattening() throws Exception {
        File xlsx = tempDir.resolve("with-location.xlsx").toFile();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("RFQ");
            Row meta = sheet.createRow(0);
            meta.createCell(0).setCellValue("Delivery Location:");
            meta.createCell(1).setCellValue("Plot 14, Hinjewadi, Pune, Maharashtra - 411057");

            Row header = sheet.createRow(2);
            header.createCell(0).setCellValue("Item");
            header.createCell(1).setCellValue("Qty");
            Row r1 = sheet.createRow(3);
            r1.createCell(0).setCellValue("Gear Box Seal 40x52x7");
            r1.createCell(1).setCellValue(12);

            try (FileOutputStream out = new FileOutputStream(xlsx)) {
                wb.write(out);
            }
        }

        String text = FileUtil.extractTextFromFile(xlsx);
        assertTrue(text.contains("Delivery Location:"), "label must survive: " + text);
        assertTrue(text.contains("Plot 14, Hinjewadi, Pune, Maharashtra - 411057"),
                "location value must survive: " + text);

        // The label and its value must land on the same line so a line-tail scan can recover it.
        String locationLine = null;
        for (String line : text.split("\\R")) {
            if (line.contains("Delivery Location")) {
                locationLine = line;
            }
        }
        assertNotNull(locationLine);
        assertTrue(locationLine.contains("Hinjewadi"),
                "value must be on the same line as the label: " + locationLine);
    }

    @Test
    void unsupportedAttachmentTypeReturnsEmptyWithoutThrowing() throws Exception {
        File odt = tempDir.resolve("spec.odt").toFile();
        java.nio.file.Files.writeString(odt.toPath(), "irrelevant");
        assertEquals("", FileUtil.extractTextFromFile(odt));
    }
}
