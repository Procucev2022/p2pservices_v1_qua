package com.portal.procucev.rfq.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class DateParser {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            ISO_FORMATTER,
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

    private static final List<Pattern> EXACT_DATE_REGEXES = List.of(
            Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b"),
            Pattern.compile("\\b(\\d{1,2}-[A-Za-z]{3,9}-\\d{4})\\b"),
            Pattern.compile("\\b(\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{4})\\b"),
            Pattern.compile("\\b([A-Za-z]{3,9}\\s+\\d{1,2},\\s+\\d{4})\\b"),
            Pattern.compile("\\b(\\d{1,2}/\\d{1,2}/\\d{4})\\b"),
            Pattern.compile("\\b(\\d{1,2}-\\d{1,2}-\\d{4})\\b")
    );

    private static final Pattern RELATIVE_DAYS_PATTERN = Pattern.compile(
            "(?:within|in|before|after|required in|required within|delivery in|delivery required in|delivery required within|need delivery within)?\\s*(\\d+)\\s*days?(?:\\s+from\\s+now)?",
            Pattern.CASE_INSENSITIVE
    );

    public String toIsoDateString(String inputDate) {
        if (inputDate == null || inputDate.trim().isEmpty()) {
            return null;
        }

        String cleaned = inputDate.trim();

        // Priority 1: Exact Delivery Date substring match
        for (Pattern regex : EXACT_DATE_REGEXES) {
            Matcher matcher = regex.matcher(cleaned);
            if (matcher.find()) {
                String candidate = matcher.group(1);
                for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                    try {
                        LocalDate parsed = LocalDate.parse(candidate, formatter);
                        return parsed.format(ISO_FORMATTER);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // Direct parse of prefix-stripped string
        String cleanedPrefix = cleaned.replaceAll("(?i)^(?:delivery|required|delivery required|on|by|before|due|within|in|\\s)+", "").trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate parsed = LocalDate.parse(cleanedPrefix, formatter);
                return parsed.format(ISO_FORMATTER);
            } catch (Exception ignored) {
            }
        }

        // Priority 2: Delivery Period (relative days e.g., "within 10 days", "in 3 days", "10 days from now")
        Matcher relativeMatcher = RELATIVE_DAYS_PATTERN.matcher(cleaned);
        if (relativeMatcher.find()) {
            try {
                int days = Integer.parseInt(relativeMatcher.group(1));
                return LocalDate.now().plusDays(days).format(ISO_FORMATTER);
            } catch (Exception ignored) {
            }
        }

        log.warn("Could not parse date string '{}' to exact date or relative period.", inputDate);
        return cleaned;
    }

    public Date parseToDate(String inputDate) {
        if (inputDate == null || inputDate.trim().isEmpty()) {
            return Date.from(LocalDate.now().plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        String iso = toIsoDateString(inputDate);
        if (iso != null) {
            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    LocalDate parsed = LocalDate.parse(iso, formatter);
                    return Date.from(parsed.atStartOfDay(ZoneId.systemDefault()).toInstant());
                } catch (Exception ignored) {
                }
            }
        }

        return Date.from(LocalDate.now().plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public String parseDateString(String inputDate) {
        String defaultFormattedDate = LocalDate.now().plusDays(5).format(ISO_FORMATTER);

        if (inputDate == null || inputDate.trim().isEmpty() || inputDate.equalsIgnoreCase("Not Specified") || inputDate.equalsIgnoreCase("null")) {
            return defaultFormattedDate;
        }

        String iso = toIsoDateString(inputDate);
        return (iso != null && !iso.isBlank()) ? iso : defaultFormattedDate;
    }
}
