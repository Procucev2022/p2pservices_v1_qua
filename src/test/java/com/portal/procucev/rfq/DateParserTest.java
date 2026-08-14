package com.portal.procucev.rfq;

import com.portal.procucev.rfq.parser.DateParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class DateParserTest {

    private DateParser dateParser;

    @BeforeEach
    void setUp() {
        dateParser = new DateParser();
    }

    @Test
    @DisplayName("Test toIsoDateString with null or blank input")
    void testToIsoDateStringNullOrBlank() {
        assertNull(dateParser.toIsoDateString(null));
        assertNull(dateParser.toIsoDateString(""));
        assertNull(dateParser.toIsoDateString("   "));
    }

    @Test
    @DisplayName("Test toIsoDateString with relative days phrase")
    void testToIsoDateStringRelativeDays() {
        String res1 = dateParser.toIsoDateString("within 7 days");
        String expected1 = LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertEquals(expected1, res1);

        String res2 = dateParser.toIsoDateString("in 3 days");
        String expected2 = LocalDate.now().plusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertEquals(expected2, res2);

        String res3 = dateParser.toIsoDateString("before 10 days");
        String expected3 = LocalDate.now().plusDays(10).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertEquals(expected3, res3);
    }

    @Test
    @DisplayName("Test toIsoDateString with various date format patterns")
    void testToIsoDateStringPatterns() {
        assertEquals("2026-08-25", dateParser.toIsoDateString("2026-08-25"));
        assertEquals("2026-08-25", dateParser.toIsoDateString("25-August-2026"));
        assertEquals("2026-08-25", dateParser.toIsoDateString("25-Aug-2026"));
        assertEquals("2026-08-25", dateParser.toIsoDateString("25 August 2026"));
        assertEquals("2026-08-25", dateParser.toIsoDateString("25 Aug 2026"));
        assertEquals("2026-08-25", dateParser.toIsoDateString("August 25, 2026"));
        assertEquals("2026-08-25", dateParser.toIsoDateString("Aug 25, 2026"));
        assertEquals("2026-08-25", dateParser.toIsoDateString("25/08/2026"));
        assertEquals("2026-08-25", dateParser.toIsoDateString("25-08-2026"));
        assertEquals("2026-08-25", dateParser.toIsoDateString("08/25/2026"));
    }

    @Test
    @DisplayName("Test toIsoDateString with prefixes like required/due/by/on")
    void testToIsoDateStringWithPrefixes() {
        assertEquals("2026-08-25", dateParser.toIsoDateString("required 2026-08-25"));
        assertEquals("2026-08-25", dateParser.toIsoDateString("due by 25-08-2026"));
    }

    @Test
    @DisplayName("Test toIsoDateString with unparseable text returns raw cleaned text")
    void testToIsoDateStringUnparseable() {
        assertEquals("ASAP", dateParser.toIsoDateString("ASAP"));
    }

    @Test
    @DisplayName("Test parseToDate")
    void testParseToDate() {
        Date d1 = dateParser.parseToDate(null);
        assertNotNull(d1);

        Date d2 = dateParser.parseToDate("2026-08-25");
        assertNotNull(d2);

        Date d3 = dateParser.parseToDate("invalid date text");
        assertNotNull(d3);
    }

    @Test
    @DisplayName("Test parseDateString")
    void testParseDateString() {
        String defaultDate = LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertEquals(defaultDate, dateParser.parseDateString(null));
        assertEquals(defaultDate, dateParser.parseDateString("   "));
        assertEquals("2026-08-25", dateParser.parseDateString("2026-08-25"));
    }

    @Test
    @DisplayName("Test parseToDate with empty and blank inputs")
    void testParseToDateBlankInput() {
        Date d1 = dateParser.parseToDate("");
        assertNotNull(d1);

        Date d2 = dateParser.parseToDate("   ");
        assertNotNull(d2);
    }

    @Test
    @DisplayName("Test toIsoDateString with '1 day' singular phrasing")
    void testToIsoDateStringSingularDay() {
        String res = dateParser.toIsoDateString("after 1 day");
        String expected = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertEquals(expected, res);
    }

    @Test
    @DisplayName("Test parseDateString with unparseable returns raw text not null")
    void testParseDateStringUnparseable() {
        String result = dateParser.parseDateString("ASAP");
        assertEquals("ASAP", result);
    }

    @Test
    @DisplayName("Test toIsoDateString with multiple format patterns")
    void testToIsoDateStringAllFormats() {
        assertEquals("2026-01-15", dateParser.toIsoDateString("January 15, 2026"));
        assertEquals("2026-01-15", dateParser.toIsoDateString("Jan 15, 2026"));
        assertEquals("2026-01-15", dateParser.toIsoDateString("15 January 2026"));
        assertEquals("2026-01-15", dateParser.toIsoDateString("15 Jan 2026"));
        assertEquals("2026-01-15", dateParser.toIsoDateString("15-January-2026"));
        assertEquals("2026-01-15", dateParser.toIsoDateString("15-Jan-2026"));
        assertEquals("2026-01-15", dateParser.toIsoDateString("15/01/2026"));
        assertEquals("2026-01-15", dateParser.toIsoDateString("01/15/2026"));
        assertEquals("2026-01-15", dateParser.toIsoDateString("15-01-2026"));
    }

    @Test
    @DisplayName("Test parseToDate with various parseable formats")
    void testParseToDateMultipleFormats() {
        assertNotNull(dateParser.parseToDate("25-Aug-2026"));
        assertNotNull(dateParser.parseToDate("25/08/2026"));
        assertNotNull(dateParser.parseToDate("August 25, 2026"));
    }

    @Test
    @DisplayName("Test toIsoDateString prefix stripping variations")
    void testToIsoDateStringPrefixVariations() {
        assertEquals("2026-08-25", dateParser.toIsoDateString("on 2026-08-25"));
        assertEquals("2026-08-25", dateParser.toIsoDateString("before 2026-08-25"));
        assertEquals("2026-08-25", dateParser.toIsoDateString("within 2026-08-25"));
    }
}
