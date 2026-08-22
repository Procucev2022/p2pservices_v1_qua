package com.portal.procucev.rfq;

import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.util.QuantityNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QuantityNormalizerTest {

    @Test
    @DisplayName("Test 1-20 number words")
    void test1To20() {
        assertEquals(1.0, QuantityNormalizer.normalize("one"));
        assertEquals(2.0, QuantityNormalizer.normalize("two"));
        assertEquals(3.0, QuantityNormalizer.normalize("three"));
        assertEquals(4.0, QuantityNormalizer.normalize("four"));
        assertEquals(5.0, QuantityNormalizer.normalize("five"));
        assertEquals(6.0, QuantityNormalizer.normalize("six"));
        assertEquals(7.0, QuantityNormalizer.normalize("seven"));
        assertEquals(8.0, QuantityNormalizer.normalize("eight"));
        assertEquals(9.0, QuantityNormalizer.normalize("nine"));
        assertEquals(10.0, QuantityNormalizer.normalize("ten"));
        assertEquals(11.0, QuantityNormalizer.normalize("eleven"));
        assertEquals(12.0, QuantityNormalizer.normalize("twelve"));
        assertEquals(13.0, QuantityNormalizer.normalize("thirteen"));
        assertEquals(14.0, QuantityNormalizer.normalize("fourteen"));
        assertEquals(15.0, QuantityNormalizer.normalize("fifteen"));
        assertEquals(16.0, QuantityNormalizer.normalize("sixteen"));
        assertEquals(17.0, QuantityNormalizer.normalize("seventeen"));
        assertEquals(18.0, QuantityNormalizer.normalize("eighteen"));
        assertEquals(19.0, QuantityNormalizer.normalize("nineteen"));
        assertEquals(20.0, QuantityNormalizer.normalize("twenty"));
    }

    @Test
    @DisplayName("Test 21-99 and hyphenated numbers")
    void test21To99() {
        assertEquals(21.0, QuantityNormalizer.normalize("twenty-one"));
        assertEquals(25.0, QuantityNormalizer.normalize("twenty-five"));
        assertEquals(25.0, QuantityNormalizer.normalize("twenty five"));
        assertEquals(30.0, QuantityNormalizer.normalize("thirty"));
        assertEquals(40.0, QuantityNormalizer.normalize("forty"));
        assertEquals(50.0, QuantityNormalizer.normalize("fifty"));
        assertEquals(60.0, QuantityNormalizer.normalize("sixty"));
        assertEquals(70.0, QuantityNormalizer.normalize("seventy"));
        assertEquals(80.0, QuantityNormalizer.normalize("eighty"));
        assertEquals(90.0, QuantityNormalizer.normalize("ninety"));
        assertEquals(99.0, QuantityNormalizer.normalize("ninety-nine"));
    }

    @Test
    @DisplayName("Test hundreds and numbers with 'and'")
    void testHundreds() {
        assertEquals(100.0, QuantityNormalizer.normalize("one hundred"));
        assertEquals(105.0, QuantityNormalizer.normalize("one hundred and five"));
        assertEquals(150.0, QuantityNormalizer.normalize("one hundred and fifty"));
        assertEquals(200.0, QuantityNormalizer.normalize("two hundred"));
        assertEquals(500.0, QuantityNormalizer.normalize("five hundred"));
        assertEquals(999.0, QuantityNormalizer.normalize("nine hundred and ninety-nine"));
    }

    @Test
    @DisplayName("Test thousands and tens of thousands")
    void testThousands() {
        assertEquals(1000.0, QuantityNormalizer.normalize("one thousand"));
        assertEquals(2000.0, QuantityNormalizer.normalize("two thousand"));
        assertEquals(10000.0, QuantityNormalizer.normalize("ten thousand"));
        assertEquals(25000.0, QuantityNormalizer.normalize("twenty-five thousand"));
        assertEquals(25000.0, QuantityNormalizer.normalize("twenty five thousand"));
        assertEquals(100000.0, QuantityNormalizer.normalize("one hundred thousand"));
    }

    @Test
    @DisplayName("Test boundary lakhs: 1 lakh to 10 lakh")
    void testLakhsBoundary() {
        assertEquals(100000.0, QuantityNormalizer.normalize("one lakh"));
        assertEquals(200000.0, QuantityNormalizer.normalize("two lakh"));
        assertEquals(500000.0, QuantityNormalizer.normalize("five lakh"));
        assertEquals(900000.0, QuantityNormalizer.normalize("nine lakh"));
        assertEquals(1000000.0, QuantityNormalizer.normalize("ten lakh"));
    }

    @Test
    @DisplayName("Test combinations of lakhs + thousands + hundreds")
    void testCombinations() {
        assertEquals(120000.0, QuantityNormalizer.normalize("one lakh twenty thousand"));
        assertEquals(250000.0, QuantityNormalizer.normalize("two lakh fifty thousand"));
        assertEquals(525000.0, QuantityNormalizer.normalize("five lakh twenty-five thousand"));
        assertEquals(999000.0, QuantityNormalizer.normalize("nine lakh ninety-nine thousand"));
        assertEquals(1000000.0, QuantityNormalizer.normalize("ten lakh"));
    }

    @Test
    @DisplayName("Test numeric quantities with Indian and international commas")
    void testNumericCommas() {
        assertEquals(1000.0, QuantityNormalizer.normalize("1,000"));
        assertEquals(10000.0, QuantityNormalizer.normalize("10,000"));
        assertEquals(100000.0, QuantityNormalizer.normalize("1,00,000"));
        assertEquals(1000000.0, QuantityNormalizer.normalize("10,00,000"));
    }

    @Test
    @DisplayName("Test word and numeric quantity with units attached")
    void testQuantityWithUnits() {
        assertEquals(7.0, QuantityNormalizer.normalize("seven laptops"));
        assertEquals(10.0, QuantityNormalizer.normalize("ten units"));
        assertEquals(25.0, QuantityNormalizer.normalize("twenty-five Nos"));
        assertEquals(500.0, QuantityNormalizer.normalize("five hundred bags"));
        assertEquals(1000.0, QuantityNormalizer.normalize("one thousand pieces"));
        assertEquals(200000.0, QuantityNormalizer.normalize("two lakh units"));
        assertEquals(1000000.0, QuantityNormalizer.normalize("ten lakh pieces"));
        assertEquals(25.0, QuantityNormalizer.normalize("25 nos"));
    }

    @Test
    @DisplayName("Test multiple items with different word-based quantities in RFQItem")
    void testMultipleItemsIndependentQuantities() {
        RFQItem item1 = new RFQItem();
        item1.setQuantity("seven Nos");

        RFQItem item2 = new RFQItem();
        item2.setQuantity("ten Nos");

        RFQItem item3 = new RFQItem();
        item3.setQuantity("five hundred Bags");

        RFQItem item4 = new RFQItem();
        item4.setQuantity("two thousand meters");

        assertEquals(7.0, item1.getQuantity());
        assertEquals(10.0, item2.getQuantity());
        assertEquals(500.0, item3.getQuantity());
        assertEquals(2000.0, item4.getQuantity());
    }

    @Test
    @DisplayName("Test UOM extraction for mixed formats")
    void testExtractUom() {
        assertEquals("Units", QuantityNormalizer.extractUom("seven units"));
        assertEquals("Bags", QuantityNormalizer.extractUom("five hundred bags"));
        assertEquals("Meters", QuantityNormalizer.extractUom("two thousand meters"));
        assertEquals("Bags", QuantityNormalizer.extractUom("5,000 bags"));
        assertEquals("Nos", QuantityNormalizer.extractUom("25 Nos"));
        assertEquals("Pieces", QuantityNormalizer.extractUom("twenty five pieces"));
        assertEquals("M", QuantityNormalizer.extractUom("10 m"));
    }

    @Test
    @DisplayName("Test mandatory explicit quantity examples from requirement")
    void testExplicitRequirementExamples() {
        assertEquals(7.0, QuantityNormalizer.normalize("Quantity: 7"));
        assertEquals(7.0, QuantityNormalizer.normalize("Quantity: seven"));
        assertEquals(10.0, QuantityNormalizer.normalize("Quantity: ten units"));
        assertEquals(1000.0, QuantityNormalizer.normalize("Quantity: 1000 units"));
        assertEquals(1000.0, QuantityNormalizer.normalize("Quantity: one thousand"));
        assertEquals(25.0, QuantityNormalizer.normalize("Quantity: 25 UOM: Nos Location: Bengaluru"));
        assertNull(QuantityNormalizer.normalize("Quantity: 16 GB RAM"));
    }

    @Test
    @DisplayName("Test null and non-quantity string inputs")
    void testNullAndInvalidInputs() {
        assertNull(QuantityNormalizer.normalize(null));
        assertNull(QuantityNormalizer.normalize(""));
        assertNull(QuantityNormalizer.normalize("   "));
        assertNull(QuantityNormalizer.normalize("please provide quotation"));
    }

    @Test
    @DisplayName("Test Specification Numbers Exclusion (capacity, power, size, memory, etc. MUST NOT be treated as quantity)")
    void testSpecificationNumbersExclusion() {
        assertNull(QuantityNormalizer.normalize("10,000 liters per hour capacity"));
        assertNull(QuantityNormalizer.normalize("2 Ton air conditioner"));
        assertNull(QuantityNormalizer.normalize("500 kg capacity"));
        assertNull(QuantityNormalizer.normalize("110 mm diameter pipe"));
        assertNull(QuantityNormalizer.normalize("PN10 pressure rating"));
        assertNull(QuantityNormalizer.normalize("150W LED light"));
        assertNull(QuantityNormalizer.normalize("24-inch monitor"));
        assertNull(QuantityNormalizer.normalize("15 HP motor"));
        assertNull(QuantityNormalizer.normalize("20:1 reduction ratio"));
        assertNull(QuantityNormalizer.normalize("100 LPH capacity"));
        assertNull(QuantityNormalizer.normalize("50 kg per bag"));
        assertNull(QuantityNormalizer.normalize("12-inch display"));
        assertNull(QuantityNormalizer.normalize("16GB RAM"));
        assertNull(QuantityNormalizer.normalize("512GB SSD"));
        assertNull(QuantityNormalizer.normalize("5-star rating"));

        // Explicit purchase quantity combined with spec capacity should still extract purchase quantity (5.0)
        assertEquals(5.0, QuantityNormalizer.normalize("Quantity: 5 Units, 10,000 LPH capacity"));
    }

    @Test
    @DisplayName("Test Number object inputs, negative/zero numbers, Millions, Crores, and UOM edge cases")
    void testNumberInputsAndLargeWords() {
        assertEquals(50.0, QuantityNormalizer.normalize(50));
        assertEquals(50.5, QuantityNormalizer.normalize(50.5));
        assertNull(QuantityNormalizer.normalize(0));
        assertNull(QuantityNormalizer.normalize(-10));

        assertEquals(1000000.0, QuantityNormalizer.normalize("one million"));
        assertEquals(2000000.0, QuantityNormalizer.normalize("two millions"));
        assertEquals(10000000.0, QuantityNormalizer.normalize("one crore"));
        assertEquals(20000000.0, QuantityNormalizer.normalize("two crores"));

        assertEquals(1000.0, QuantityNormalizer.normalize("Quantity: 1,000"));
        assertEquals(500.0, QuantityNormalizer.normalize("Quantity: 500.0"));
        assertEquals(500.0, QuantityNormalizer.normalize("Quantity: five hundred"));

        assertNull(QuantityNormalizer.extractUom(null));
        assertNull(QuantityNormalizer.extractUom(""));
        assertEquals("Bags", QuantityNormalizer.extractUom("Quantity: 500 bags"));
        assertEquals("X", QuantityNormalizer.extractUom("10 x"));
    }

    @Test
    @DisplayName("Test findQuantityForItem variations")
    void testFindQuantityForItem() {
        assertNull(QuantityNormalizer.findQuantityForItem(null, "Hex Bolts"));
        assertNull(QuantityNormalizer.findQuantityForItem("", "Hex Bolts"));
        assertNull(QuantityNormalizer.findQuantityForItem("Some text", null));
        assertNull(QuantityNormalizer.findQuantityForItem("Some text", ""));
        assertNull(QuantityNormalizer.findQuantityForItem("Some text", "a".repeat(250)));
        assertNull(QuantityNormalizer.findQuantityForItem("Some text", "???!!!"));

        // Trailing keyword: Hex Bolts - Qty: 50
        QuantityNormalizer.QuantityMatch match1 = QuantityNormalizer.findQuantityForItem("Item: Hex Bolts M10, Qty: 50", "Hex Bolts M10");
        assertNotNull(match1);
        assertEquals(50.0, match1.quantity());
        assertNull(match1.uom());

        // Trailing unit: Plain Washers M10 - 1,000 Nos
        QuantityNormalizer.QuantityMatch match2 = QuantityNormalizer.findQuantityForItem("Plain Washers M10 - 1,000 Nos", "Plain Washers M10");
        assertNotNull(match2);
        assertEquals(1000.0, match2.quantity());
        assertEquals("Nos", match2.uom());

        // Leading unit: 500 Nos of Plain Washers M10
        QuantityNormalizer.QuantityMatch match3 = QuantityNormalizer.findQuantityForItem("500 Nos of Plain Washers M10", "Plain Washers M10");
        assertNotNull(match3);
        assertEquals(500.0, match3.quantity());
        assertEquals("Nos", match3.uom());

        // Leading purchase: we require 10 laptops
        QuantityNormalizer.QuantityMatch match4 = QuantityNormalizer.findQuantityForItem("we require 10 laptops", "laptops");
        assertNotNull(match4);
        assertEquals(10.0, match4.quantity());

        // Typographic variants
        QuantityNormalizer.QuantityMatch match5 = QuantityNormalizer.findQuantityForItem("M10 \u00D7 50 mm \u2013 100 Nos", "M10 x 50 mm");
        assertNotNull(match5);
        assertEquals(100.0, match5.quantity());

        // Leading purchase phrases with approximation
        QuantityNormalizer.QuantityMatch match6 = QuantityNormalizer.findQuantityForItem("Please quote for a total of approx. 200 monitors", "monitors");
        assertNotNull(match6);
        assertEquals(200.0, match6.quantity());

        // Trade unit variations
        assertEquals(5.0, QuantityNormalizer.normalize("5 sets"));
        assertEquals(12.0, QuantityNormalizer.normalize("12 dozens"));
        assertEquals(10.0, QuantityNormalizer.normalize("10 boxes"));
        assertEquals(20.0, QuantityNormalizer.normalize("20 rolls"));
        assertEquals(15.0, QuantityNormalizer.normalize("15 packets"));
        assertEquals(30.0, QuantityNormalizer.normalize("30 cartons"));
        assertEquals(50.0, QuantityNormalizer.normalize("50 reams"));
        assertEquals(100.0, QuantityNormalizer.normalize("100 sheets"));
        assertEquals(8.0, QuantityNormalizer.normalize("8 tubes"));
        assertEquals(4.0, QuantityNormalizer.normalize("4 drums"));
        assertEquals(6.0, QuantityNormalizer.normalize("6 coils"));
        assertEquals(2.0, QuantityNormalizer.normalize("2 lengths"));

        // UOM extraction with lacs, crores
        assertEquals("Kg", QuantityNormalizer.extractUom("two lac kg"));
        assertEquals("Bags", QuantityNormalizer.extractUom("ten crore bags"));
        assertEquals("Meters", QuantityNormalizer.extractUom("ten thousand meters"));
        assertNull(QuantityNormalizer.extractUom("100"));

        // Specification indicator vs leading digit in Quantity:
        assertNull(QuantityNormalizer.normalize("Quantity: 16 GB RAM"));
        assertNull(QuantityNormalizer.normalize("2 ton air conditioner"));
        assertEquals(25.0, QuantityNormalizer.normalize("Quantity: 25 UOM: Nos Location: Bengaluru"));
        // Instantiation
        assertNotNull(new QuantityNormalizer());
    }

    @Test
    @DisplayName("Test parseWords multipliers without preceding count and word variants")
    void testParseWordsStandaloneMultipliers() {
        assertEquals(100.0, QuantityNormalizer.parseWords("hundred"));
        assertEquals(1000.0, QuantityNormalizer.parseWords("thousand"));
        assertEquals(1000.0, QuantityNormalizer.parseWords("thousands"));
        assertEquals(100000.0, QuantityNormalizer.parseWords("lakh"));
        assertEquals(100000.0, QuantityNormalizer.parseWords("lakhs"));
        assertEquals(100000.0, QuantityNormalizer.parseWords("lac"));
        assertEquals(100000.0, QuantityNormalizer.parseWords("lacs"));
        assertEquals(1000000.0, QuantityNormalizer.parseWords("million"));
        assertEquals(1000000.0, QuantityNormalizer.parseWords("millions"));
        assertEquals(10000000.0, QuantityNormalizer.parseWords("crore"));
        assertEquals(10000000.0, QuantityNormalizer.parseWords("crores"));

        assertNull(QuantityNormalizer.parseWords(null));
        assertNull(QuantityNormalizer.parseWords("   "));
        assertNull(QuantityNormalizer.parseWords("and of a"));
        assertNull(QuantityNormalizer.parseWords("completely unrelated words without numbers"));
    }

    @Test
    @DisplayName("Test capitalizeWord and extractUom edge cases")
    void testCapitalizeWordAndExtractUom() {
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                QuantityNormalizer.class, "capitalizeWord", (String) null));
        assertEquals("   ", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                QuantityNormalizer.class, "capitalizeWord", "   "));
        assertEquals("A", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                QuantityNormalizer.class, "capitalizeWord", "a"));
        assertEquals("Box", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                QuantityNormalizer.class, "capitalizeWord", "box"));

        // extractUom with millions/crores
        assertEquals("Pieces", QuantityNormalizer.extractUom("two million pieces"));
        assertEquals("Boxes", QuantityNormalizer.extractUom("two crore boxes"));
        assertEquals("Units", QuantityNormalizer.extractUom("ten lacs units"));

        // Number object types in normalize
        assertEquals(42.0, QuantityNormalizer.normalize(42));
        assertEquals(42.0, QuantityNormalizer.normalize(42L));
        assertEquals(42.5, QuantityNormalizer.normalize(42.5f));
        assertEquals(42.5, QuantityNormalizer.normalize(42.5d));
        assertEquals(42.0, QuantityNormalizer.normalize(java.math.BigInteger.valueOf(42)));
        assertEquals(42.75, QuantityNormalizer.normalize(new java.math.BigDecimal("42.75")));

        // Zero and negative numbers
        assertNull(QuantityNormalizer.normalize(0));
        assertNull(QuantityNormalizer.normalize(-10));
        assertNull(QuantityNormalizer.normalize("0"));
        assertNull(QuantityNormalizer.normalize("-5"));
        assertNull(QuantityNormalizer.normalize("0 Nos"));
        assertNull(QuantityNormalizer.normalize("Quantity: 0"));
        assertNull(QuantityNormalizer.normalize("Quantity: 0 Nos"));
        assertNull(QuantityNormalizer.normalize("Quantity: 0 items to ship"));
        assertNull(QuantityNormalizer.normalize("hello world today"));

        // extractUom null / empty / number only
        assertNull(QuantityNormalizer.extractUom((String) null));
        assertNull(QuantityNormalizer.extractUom("   "));
        assertNull(QuantityNormalizer.extractUom("five hundred"));
        assertEquals("Bags", QuantityNormalizer.extractUom("two thousand and ten bags"));
        assertEquals("Meters", QuantityNormalizer.extractUom("ten thousand thousands lakh lakhs lac lacs million millions crore crores meters"));
        assertEquals("Nos", QuantityNormalizer.extractUom("Required Qty: 25 Nos"));
        assertEquals("Units", QuantityNormalizer.extractUom("Quantity = 10 units"));
        assertEquals("Pcs", QuantityNormalizer.extractUom("Qty : 50 pcs"));
    }
}
