package com.portal.procucev.rfq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.repository.RFQRepository;
import com.portal.procucev.rfq.repository.RfqItemRecordRepository;
import com.portal.procucev.rfq.service.AIExtractionService;
import com.portal.procucev.rfq.service.AcknowledgementEmailService;
import com.portal.procucev.rfq.service.BuyerVerificationService;
import com.portal.procucev.rfq.service.CategoryClassificationService;
import com.portal.procucev.rfq.service.EmailProcessorService;
import com.portal.procucev.rfq.service.EmailReaderService;
import com.portal.procucev.rfq.service.RFQApiService;
import com.portal.procucev.rfq.service.RFQBuilderService;
import com.portal.procucev.rfq.service.ValidationService;
import com.portal.procucev.rfq.util.QuantityNormalizer;
import com.portal.procucev.rfq.util.QuantityNormalizer.QuantityMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression cover for the production incident of 18-Aug-2026 on rfq@procucev.com.
 *
 * <p>The email "Requirement of MS Hex Bolts M10" listed four items, each with its quantity written
 * after an en dash. The extraction model returned all four with uom "Nos" but quantity null, so
 * every row failed the mandatory-quantity rule, the whole payload was rejected, and the buyer was
 * asked to supply quantities they had already supplied.
 */
public class ItemQuantityRecoveryTest {

    /** Multiplication sign, which is what Gmail actually sends for a dimension separator. */
    private static final String MULT = "\u00D7";

    /** En dash, which is what Gmail actually sends between an item name and its quantity. */
    private static final String EN_DASH = "\u2013";

    /** No-break space, emitted by several mail clients in place of a plain space. */
    private static final String NBSP = "\u00A0";

    /** The body of the failing production email, with the characters the mail client really sent. */
    private static final String HEX_BOLTS_BODY =
            "We require MS Hex Bolts M10 " + MULT + " 50 mm " + EN_DASH + " 500 Nos, "
            + "Plain Washers M10 " + EN_DASH + " 1,000 Nos, "
            + "Spring Washers M10 " + EN_DASH + " 1,000 Nos and "
            + "Nuts M10 " + EN_DASH + " 500 Nos. "
            + "All materials should be zinc-plated and suitable for industrial applications. "
            + "Preferred Brands: TVS, Unbrako, LPS, Sundram Fasteners, or equivalent. "
            + "Material should be new, genuine, rust-free, and supplied in proper manufacturer "
            + "packaging. Delivery is required at Hyderabad";

    private EmailReaderService emailReaderService;
    private AIExtractionService aiExtractionService;
    private ValidationService validationService;
    private CategoryClassificationService categoryClassificationService;
    private BuyerVerificationService buyerVerificationService;
    private RFQBuilderService rfqBuilderService;
    private RFQApiService rfqApiService;
    private AcknowledgementEmailService acknowledgementEmailService;
    private EmailTransactionRepository emailTransactionRepository;
    private RFQRepository rfqRepository;
    private RfqItemRecordRepository rfqItemRecordRepository;
    private DateParser dateParser;
    private JavaMailSender mailSender;
    private ObjectMapper objectMapper;

    private EmailProcessorService emailProcessorService;

    @BeforeEach
    void setUp() {
        emailReaderService = mock(EmailReaderService.class);
        aiExtractionService = mock(AIExtractionService.class);
        validationService = mock(ValidationService.class);
        categoryClassificationService = mock(CategoryClassificationService.class);
        buyerVerificationService = mock(BuyerVerificationService.class);
        rfqBuilderService = mock(RFQBuilderService.class);
        rfqApiService = mock(RFQApiService.class);
        emailTransactionRepository = mock(EmailTransactionRepository.class);
        rfqRepository = mock(RFQRepository.class);
        rfqItemRecordRepository = mock(RfqItemRecordRepository.class);
        dateParser = new DateParser();
        mailSender = mock(JavaMailSender.class);
        objectMapper = new ObjectMapper();

        acknowledgementEmailService = new AcknowledgementEmailService(mailSender, objectMapper);

        emailProcessorService = new EmailProcessorService(
                emailReaderService,
                aiExtractionService,
                validationService,
                buyerVerificationService,
                rfqBuilderService,
                rfqApiService,
                categoryClassificationService,
                acknowledgementEmailService,
                rfqRepository,
                emailTransactionRepository,
                rfqItemRecordRepository,
                dateParser,
                objectMapper
        );

        Buyer buyer = Buyer.builder()
                .userId("d60408de-9c7a-43ee-9056-ace49db4794f")
                .email("govardhan.kilari@procucev.com")
                .name("Reshu Nair")
                .address("Bengaluru")
                .city("Bengaluru")
                .state("KARNATAKA")
                .pincode("560036")
                .verified(true)
                .build();

        when(buyerVerificationService.verifyAndGetBuyer(anyString())).thenReturn(buyer);
        when(rfqRepository.save(any(RFQEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rfqApiService.submitRFQ(any(RFQRequest.class))).thenAnswer(inv -> {
            RFQRequest req = inv.getArgument(0);
            return RFQResponse.builder()
                    .status("SUCCESS")
                    .rfqNumber(req.getRfqNumber())
                    .message("RFQ created successfully")
                    .build();
        });
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenAnswer(inv -> {
            ExtractedRFQ extracted = inv.getArgument(0);
            List<RFQRequest.RfqItemDto> dtos = new ArrayList<>();
            for (RFQItem item : extracted.getItems()) {
                dtos.add(RFQRequest.RfqItemDto.builder()
                        .description(item.getItemDescription())
                        .quantity(item.getQuantity())
                        .unitofMeasures(item.getUom())
                        .build());
            }
            return RFQRequest.builder()
                    .rfqNumber("RFQ-20260818041109-hexbolts")
                    .buyerEmail(extracted.getBuyerEmail())
                    .deliveryDate(extracted.getDeliveryDate())
                    .rfqItem(dtos)
                    .build();
        });
    }

    /** An item exactly as the model returned it in production: unit read, number dropped. */
    private static RFQItem modelItem(String description) {
        return RFQItem.builder()
                .itemDescription(description)
                .uom("Nos")
                .build();
    }

    // ---------------------------------------------------------------------
    // End to end: the exact production payload must now create the RFQ
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Production incident: 4 fastener items with en-dash quantities create an RFQ, not a clarification email")
    void hexBoltsEmailCreatesRfqWithAllFourQuantities() throws Exception {
        EmailData email = EmailData.builder()
                .messageId("<CADmWLxiajUSUBVrLTSse_WT2jGgG=qSx1F9Hcz8yQ6mh4q4fzA@mail.gmail.com>")
                .subject("Requirement of MS Hex Bolts M10")
                .senderEmail("govardhan.kilari@procucev.com")
                .body(HEX_BOLTS_BODY)
                .build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("govardhan.kilari@procucev.com")
                .items(new ArrayList<>(List.of(
                        modelItem("MS Hex Bolts M10 x 50 mm"),
                        modelItem("Plain Washers M10"),
                        modelItem("Spring Washers M10"),
                        modelItem("Nuts M10"))))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String status = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", status, "the email carried every quantity, so the RFQ must be created");

        ArgumentCaptor<RFQEntity> entityCaptor = ArgumentCaptor.forClass(RFQEntity.class);
        verify(rfqRepository, times(1)).save(entityCaptor.capture());

        List<RFQItem> saved = objectMapper.readValue(
                entityCaptor.getValue().getItemsJson(), new TypeReference<List<RFQItem>>() {});
        assertEquals(4, saved.size());
        assertEquals("MS Hex Bolts M10 x 50 mm", saved.get(0).getItemDescription());
        assertEquals(500.0, saved.get(0).getQuantity());
        assertEquals("Plain Washers M10", saved.get(1).getItemDescription());
        assertEquals(1000.0, saved.get(1).getQuantity());
        assertEquals("Spring Washers M10", saved.get(2).getItemDescription());
        assertEquals(1000.0, saved.get(2).getQuantity());
        assertEquals("Nuts M10", saved.get(3).getItemDescription());
        assertEquals(500.0, saved.get(3).getQuantity());
        assertEquals("Nos", saved.get(0).getUom(), "the unit the model supplied must be preserved");

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mailCaptor.capture());
        String sentBody = mailCaptor.getValue().getText();
        assertTrue(sentBody.contains("RFQ-20260818041109-hexbolts"), "buyer must get the success acknowledgement");
        assertFalse(sentBody.contains("Quantity is missing"), "buyer must not be asked for quantities they supplied");
    }

    @Test
    @DisplayName("Rows that genuinely state no quantity are still rejected and still queried")
    void itemsWithNoQuantityAnywhereAreStillRejected() {
        EmailData email = EmailData.builder()
                .messageId("MSG-NO-QTY")
                .subject("Requirement of MS Hex Bolts M10")
                .senderEmail("govardhan.kilari@procucev.com")
                .body("We require MS Hex Bolts M10 " + MULT + " 50 mm " + EN_DASH + " 500 Nos, "
                        + "Plain Washers M10 and Spring Washers M10. Please quote.")
                .build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("govardhan.kilari@procucev.com")
                .items(new ArrayList<>(List.of(
                        modelItem("MS Hex Bolts M10 x 50 mm"),
                        modelItem("Plain Washers M10"),
                        modelItem("Spring Washers M10"))))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        assertEquals("VALIDATION_FAILED", emailProcessorService.processSingleEmail(email));

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mailCaptor.capture());
        String body = mailCaptor.getValue().getText();
        assertTrue(body.contains("Quantity is missing for 2 items"), "only the two silent rows should be queried");
        assertTrue(body.contains("Plain Washers M10"));
        assertTrue(body.contains("Spring Washers M10"));
        assertFalse(body.contains("1. MS Hex Bolts"), "the row that stated 500 Nos must not be queried");
    }

    // ---------------------------------------------------------------------
    // Unit level: what findQuantityForItem must recover
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Every item in the failing body resolves to its own quantity and unit")
    void resolvesEachItemInRunOnRequirementSentence() {
        assertMatch(500.0, "Nos", HEX_BOLTS_BODY, "MS Hex Bolts M10 x 50 mm");
        assertMatch(1000.0, "Nos", HEX_BOLTS_BODY, "Plain Washers M10");
        assertMatch(1000.0, "Nos", HEX_BOLTS_BODY, "Spring Washers M10");
        assertMatch(500.0, "Nos", HEX_BOLTS_BODY, "Nuts M10");
    }

    @Test
    @DisplayName("A dimension in the item name is never mistaken for the quantity")
    void dimensionInNameIsNotReadAsQuantity() {
        // "50 mm" sits between the name and the real quantity: 500 must win, not 50.
        QuantityMatch match = QuantityNormalizer.findQuantityForItem(HEX_BOLTS_BODY, "MS Hex Bolts M10 x 50 mm");
        assertNotNull(match);
        assertEquals(500.0, match.quantity());
    }

    @Test
    @DisplayName("Separator, unit and digit-grouping variants all parse")
    void separatorAndGroupingVariants() {
        assertMatch(500.0, "Nos", "Nuts M10 - 500 Nos", "Nuts M10");
        assertMatch(500.0, "Nos", "Nuts M10: 500 nos", "Nuts M10");
        assertMatch(500.0, "Nos", "Nuts M10, 500 NOS", "Nuts M10");
        assertMatch(500.0, "Nos", "Nuts M10 = 500 Nos", "Nuts M10");
        assertMatch(1000.0, "Nos", "Nuts M10 " + EN_DASH + " 1,000 Nos", "Nuts M10");
        assertMatch(100000.0, "Pcs", "Nuts M10 - 1,00,000 Pcs", "Nuts M10");
        assertMatch(2500.0, "Units", "Nuts M10 - 2500 units", "Nuts M10");
        assertMatch(2.5, "Boxes", "Nuts M10 - 2.5 boxes", "Nuts M10");
        assertMatch(24.0, "Sets", "Nuts M10 " + NBSP + "-" + NBSP + "24 sets", "Nuts M10");
    }

    @Test
    @DisplayName("A flattened spreadsheet row is read as name, quantity, unit")
    void flattenedTableRowIsParsed() {
        String attachment = "| Item | Qty | UOM |\n| Plain Washers M10 | 1,000 | Nos |";
        assertMatch(1000.0, "Nos", attachment, "Plain Washers M10");
    }

    @Test
    @DisplayName("A quantity on the line below the item name is picked up")
    void quantityOnFollowingLineIsParsed() {
        assertMatch(750.0, "Nos", "Plain Washers M10\n750 Nos\n", "Plain Washers M10");
    }

    @Test
    @DisplayName("An explicit Qty keyword after the name is honoured even with no unit")
    void trailingKeywordFormIsParsed() {
        assertMatch(12.0, null, "Gear Box Seal 40x52x7, Qty: 12", "Gear Box Seal 40x52x7");
        assertMatch(30.0, null, "Gear Box Seal 40x52x7 - Quantity 30", "Gear Box Seal 40x52x7");
        assertMatch(45.0, null, "Gear Box Seal 40x52x7 required quantity of 45", "Gear Box Seal 40x52x7");
    }

    @Test
    @DisplayName("A quantity written before the item name is picked up")
    void leadingQuantityFormsAreParsed() {
        assertMatch(500.0, "Nos", "We need 500 Nos of Plain Washers M10", "Plain Washers M10");
        assertMatch(10.0, null, "We require 10 laptops for the Bangalore office", "Laptop");
        assertMatch(5.0, null, "Please quote for 5 Industrial Water Treatment Systems",
                "Industrial Water Treatment System");
    }

    @Test
    @DisplayName("Plural and typographic drift between the item name and the body still matches")
    void toleratesPluralAndTypographicDrift() {
        assertMatch(1000.0, "Nos", "Plain Washers M10 - 1,000 Nos", "Plain Washer M10");
        assertMatch(1000.0, "Nos", "Plain Washer M10 - 1,000 Nos", "Plain Washers M10");
        assertMatch(500.0, "Nos", "MS Hex Bolts M10x50mm - 500 Nos", "MS Hex Bolts M10 x 50 mm");
        assertMatch(15.0, "Nos", "Desktop Computers - 15 Nos", "Desktop Computer");
        // A bullet or dash carried into the item name must not defeat the match.
        assertMatch(500.0, "Nos", "Nuts M10 - 500 Nos", "- Nuts M10");
    }

    @Test
    @DisplayName("The first mention without a quantity does not stop a later mention that has one")
    void laterMentionWithQuantityWins() {
        String body = "Plain Washers M10 needed urgently for the plant.\nPlain Washers M10 - 1,000 Nos";
        assertMatch(1000.0, "Nos", body, "Plain Washers M10");
    }

    @Test
    @DisplayName("Regex metacharacters in an item name are treated as literal text")
    void regexMetacharactersInNameAreLiteral() {
        assertMatch(20.0, "Nos", "Cable (2.5 sq.mm) [XLPE] - 20 Nos", "Cable (2.5 sq.mm) [XLPE]");
    }

    // ---------------------------------------------------------------------
    // Unit level: what must NOT be read as a quantity
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Specification numbers beside the item name are not quantities")
    void specificationNumbersAreNotQuantities() {
        assertNull(QuantityNormalizer.findQuantityForItem(
                "HDPE Water Pipe 110 mm diameter, PN10 pressure rating", "HDPE Water Pipe"));
        assertNull(QuantityNormalizer.findQuantityForItem(
                "We need a 2 ton Air Conditioner for the server room", "Air Conditioner"));
        assertNull(QuantityNormalizer.findQuantityForItem(
                "Industrial Water Treatment System with 10,000 LPH capacity",
                "Industrial Water Treatment System"));
        assertNull(QuantityNormalizer.findQuantityForItem(
                "Desktop Computer with 16 GB DDR4 RAM, 512 GB SSD, 21.5-inch monitor", "Desktop Computer"));
    }

    @Test
    @DisplayName("A quantity belonging to a different item is not borrowed")
    void quantityOfAnotherItemIsNotBorrowed() {
        String body = "MS Hex Bolts M10 " + EN_DASH + " 500 Nos, Plain Washers M10 and Nuts M10.";
        assertNull(QuantityNormalizer.findQuantityForItem(body, "Plain Washers M10"));
        assertNull(QuantityNormalizer.findQuantityForItem(body, "Nuts M10"));
    }

    @Test
    @DisplayName("An item absent from the text yields nothing")
    void unknownItemYieldsNothing() {
        assertNull(QuantityNormalizer.findQuantityForItem(HEX_BOLTS_BODY, "Cement Bags OPC 53 Grade"));
    }

    @Test
    @DisplayName("Zero and unusable numbers are rejected in every position")
    void zeroAndUnusableNumbersAreRejected() {
        assertNull(QuantityNormalizer.findQuantityForItem("Nuts M10, Qty: 0", "Nuts M10"));
        assertNull(QuantityNormalizer.findQuantityForItem("Nuts M10 - 0 Nos", "Nuts M10"));
        assertNull(QuantityNormalizer.findQuantityForItem("0 Nos of Nuts M10", "Nuts M10"));
        assertNull(QuantityNormalizer.findQuantityForItem("We require 0 Nuts M10", "Nuts M10"));
        // A digit run long enough to overflow a double is matchable but unusable.
        assertNull(QuantityNormalizer.findQuantityForItem("Nuts M10 - " + "9".repeat(400) + " Nos", "Nuts M10"));
    }

    @Test
    @DisplayName("Blank, null and unmatchable arguments yield nothing")
    void blankAndUnmatchableArgumentsYieldNothing() {
        assertNull(QuantityNormalizer.findQuantityForItem(null, "Nuts M10"));
        assertNull(QuantityNormalizer.findQuantityForItem("   ", "Nuts M10"));
        assertNull(QuantityNormalizer.findQuantityForItem(HEX_BOLTS_BODY, null));
        assertNull(QuantityNormalizer.findQuantityForItem(HEX_BOLTS_BODY, "   "));
        // A name with no alphanumeric content cannot be turned into a pattern.
        assertNull(QuantityNormalizer.findQuantityForItem("Nuts M10 - 500 Nos", "--- ///"));
        // An implausibly long name is refused rather than compiled into a huge pattern.
        assertNull(QuantityNormalizer.findQuantityForItem("Nuts M10 - 500 Nos", "Nuts M10 " + "x".repeat(220)));
    }

    private static void assertMatch(double expectedQty, String expectedUom, String text, String description) {
        QuantityMatch match = QuantityNormalizer.findQuantityForItem(text, description);
        assertNotNull(match, () -> "expected a quantity for '" + description + "' in: " + text);
        assertEquals(expectedQty, match.quantity(), () -> "quantity for '" + description + "'");
        assertEquals(expectedUom, match.uom(), () -> "uom for '" + description + "'");
    }
}
