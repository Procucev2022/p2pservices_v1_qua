package com.portal.procucev.rfq;

import com.portal.procucev.rfq.parser.DateParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class DateParserDeliveryDateTest {

    private DateParser dateParser;
    private DateTimeFormatter formatter;

    @BeforeEach
    void setUp() {
        dateParser = new DateParser();
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    @Test
    @DisplayName("Case 1: Email has exact delivery date -> Use exact date")
    void testExactDeliveryDate() {
        String input = "Delivery required on 30-Aug-2026";
        String result = dateParser.parseDateString(input);
        assertEquals("2026-08-30", result);

        String isoInput = "2026-09-15";
        assertEquals("2026-09-15", dateParser.parseDateString(isoInput));
    }

    @Test
    @DisplayName("Case 2: Email has delivery period 'within 10 days' -> Current date + 10 days")
    void testDeliveryWithin10Days() {
        String input = "Delivery required within 10 days";
        String result = dateParser.parseDateString(input);
        String expected = LocalDate.now().plusDays(10).format(formatter);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Case 3: Email has delivery period 'within 7 days' -> Current date + 7 days")
    void testDeliveryWithin7Days() {
        String input = "Need delivery within 7 days";
        String result = dateParser.parseDateString(input);
        String expected = LocalDate.now().plusDays(7).format(formatter);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Case 4: Email has delivery period 'in 3 days' -> Current date + 3 days")
    void testDeliveryIn3Days() {
        String input = "Delivery in 3 days";
        String result = dateParser.parseDateString(input);
        String expected = LocalDate.now().plusDays(3).format(formatter);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Case 5: Email has no delivery information -> Default current date + 5 days")
    void testNoDeliveryInformation() {
        String expected = LocalDate.now().plusDays(5).format(formatter);

        assertEquals(expected, dateParser.parseDateString(null));
        assertEquals(expected, dateParser.parseDateString(""));
        assertEquals(expected, dateParser.parseDateString("   "));
        assertEquals(expected, dateParser.parseDateString("Not Specified"));
    }

    @Test
    @DisplayName("Exact date takes priority over relative period if both present")
    void testExactDateTakesPriorityOverPeriod() {
        String input = "Delivery required on 30-Aug-2026 (within 10 days)";
        String result = dateParser.parseDateString(input);
        assertEquals("2026-08-30", result);
    }
}
