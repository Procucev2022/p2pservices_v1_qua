package com.portal.procucev.rfq.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class DateParser {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH)
    );

    public String toIsoDateString(String inputDate) {
        if (inputDate == null || inputDate.trim().isEmpty()) {
            return null;
        }

        String cleaned = inputDate.trim();

        java.util.regex.Matcher relativeMatcher = java.util.regex.Pattern.compile("(?:within|in|before|after)?\\s*(\\d+)\\s+days?", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(cleaned);
        if (relativeMatcher.find()) {
            try {
                int days = Integer.parseInt(relativeMatcher.group(1));
                return LocalDate.now().plusDays(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception ignored) {}
        }

        String cleanedPrefix = cleaned.replaceAll("(?i)^(?:before|by|on|within|due|required)\\s+", "").trim();

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate parsed = LocalDate.parse(cleanedPrefix, formatter);
                return parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception ignored) {
            }
        }

        log.warn("Could not parse date string '{}' to ISO format.", inputDate);
        return cleaned;
    }

    public Date parseToDate(String inputDate) {
        if (inputDate == null || inputDate.trim().isEmpty()) {
            return Date.from(LocalDate.now().plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        String cleaned = inputDate.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate parsed = LocalDate.parse(cleaned, formatter);
                return Date.from(parsed.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (Exception ignored) {
            }
        }

        return Date.from(LocalDate.now().plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public String parseDateString(String inputDate) {
        String defaultFormattedDate = LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        if (inputDate == null || inputDate.trim().isEmpty()) {
            return defaultFormattedDate;
        }

        String iso = toIsoDateString(inputDate);
        return iso != null ? iso : defaultFormattedDate;
    }
}
