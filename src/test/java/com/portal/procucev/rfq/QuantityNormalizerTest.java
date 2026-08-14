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
    }

    @Test
    @DisplayName("Test mandatory explicit quantity examples from requirement")
    void testExplicitRequirementExamples() {
        assertEquals(7.0, QuantityNormalizer.normalize("Quantity: 7"));
        assertEquals(7.0, QuantityNormalizer.normalize("Quantity: seven"));
        assertEquals(10.0, QuantityNormalizer.normalize("Quantity: ten units"));
        assertEquals(1000.0, QuantityNormalizer.normalize("Quantity: 1000 units"));
        assertEquals(1000.0, QuantityNormalizer.normalize("Quantity: one thousand"));
    }

    @Test
    @DisplayName("Test null and non-quantity string inputs")
    void testNullAndInvalidInputs() {
        assertNull(QuantityNormalizer.normalize(null));
        assertNull(QuantityNormalizer.normalize(""));
        assertNull(QuantityNormalizer.normalize("   "));
        assertNull(QuantityNormalizer.normalize("please provide quotation"));
    }
}
