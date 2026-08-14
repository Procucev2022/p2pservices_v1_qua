package com.portal.procucev.rfq.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuantityNormalizer {

    private static final Map<String, Long> NUMBER_WORDS = new HashMap<>();

    static {
        NUMBER_WORDS.put("zero", 0L);
        NUMBER_WORDS.put("one", 1L);
        NUMBER_WORDS.put("two", 2L);
        NUMBER_WORDS.put("three", 3L);
        NUMBER_WORDS.put("four", 4L);
        NUMBER_WORDS.put("five", 5L);
        NUMBER_WORDS.put("six", 6L);
        NUMBER_WORDS.put("seven", 7L);
        NUMBER_WORDS.put("eight", 8L);
        NUMBER_WORDS.put("nine", 9L);
        NUMBER_WORDS.put("ten", 10L);
        NUMBER_WORDS.put("eleven", 11L);
        NUMBER_WORDS.put("twelve", 12L);
        NUMBER_WORDS.put("thirteen", 13L);
        NUMBER_WORDS.put("fourteen", 14L);
        NUMBER_WORDS.put("fifteen", 15L);
        NUMBER_WORDS.put("sixteen", 16L);
        NUMBER_WORDS.put("seventeen", 17L);
        NUMBER_WORDS.put("eighteen", 18L);
        NUMBER_WORDS.put("nineteen", 19L);

        NUMBER_WORDS.put("twenty", 20L);
        NUMBER_WORDS.put("thirty", 30L);
        NUMBER_WORDS.put("forty", 40L);
        NUMBER_WORDS.put("fifty", 50L);
        NUMBER_WORDS.put("sixty", 60L);
        NUMBER_WORDS.put("seventy", 70L);
        NUMBER_WORDS.put("eighty", 80L);
        NUMBER_WORDS.put("ninety", 90L);
    }

    private static final Pattern EXPLICIT_KEYWORD_PATTERN = Pattern.compile("(?i)(?:required\\s+)?(?:quantity|qty)\\s*[:=]\\s*([a-z0-9,\\-\\s\\.]+)");
    private static final Pattern DIGIT_UNIT_PATTERN = Pattern.compile("(?i)\\b([0-9,]+(?:\\.[0-9]+)?)\\s+(?:nos|units|pieces|pcs|bags|meters|mtr|kg|sheets|items|boxes|sets|rolls|liters|ltr|tons)\\b");
    private static final Pattern DIRECT_DIGIT_PATTERN = Pattern.compile("^\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*$");

    public static Double normalize(Object raw) {
        if (raw == null) {
            return null;
        }

        if (raw instanceof Number num) {
            double val = num.doubleValue();
            return val > 0 ? val : null;
        }

        String input = raw.toString().trim();
        if (input.isBlank()) {
            return null;
        }

        // 1. Direct number check (e.g. "1000", "1,000", "1,00,000", "5,000", "500.5")
        Matcher directMatcher = DIRECT_DIGIT_PATTERN.matcher(input);
        if (directMatcher.find()) {
            try {
                String cleanDigits = directMatcher.group(1).replace(",", "").trim();
                double val = Double.parseDouble(cleanDigits);
                if (val > 0) return val;
            } catch (Exception ignored) {}
        }

        // 2. Explicit Keyword Line Check: "Quantity: 1000", "Qty: 1000", "Quantity: five hundred bags"
        Matcher keywordMatcher = EXPLICIT_KEYWORD_PATTERN.matcher(input);
        if (keywordMatcher.find()) {
            String extractedValue = keywordMatcher.group(1).trim();
            Matcher directInValue = DIRECT_DIGIT_PATTERN.matcher(extractedValue);
            if (directInValue.matches()) {
                try {
                    double val = Double.parseDouble(directInValue.group(1).replace(",", ""));
                    if (val > 0) return val;
                } catch (Exception ignored) {}
            }
            Matcher digitInVal = DIGIT_UNIT_PATTERN.matcher(extractedValue);
            if (digitInVal.find()) {
                try {
                    String cleanDigits = digitInVal.group(1).replace(",", "").trim();
                    double val = Double.parseDouble(cleanDigits);
                    if (val > 0) return val;
                } catch (Exception ignored) {}
            }
            Double wordVal = parseWords(extractedValue);
            if (wordVal != null && wordVal > 0) return wordVal;
        }

        // 3. Digit with unit check (e.g. "1000 Nos", "1000 Units", "5,000 bags", "25 Nos")
        Matcher unitMatcher = DIGIT_UNIT_PATTERN.matcher(input);
        while (unitMatcher.find()) {
            try {
                String cleanDigits = unitMatcher.group(1).replace(",", "").trim();
                double val = Double.parseDouble(cleanDigits);
                if (val > 0) return val;
            } catch (Exception ignored) {}
        }

        // 4. Parse English number words from full text (e.g. "seven", "twenty-five", "one lakh", "ten lakh", "five hundred bags", "two thousand meters")
        Double wordResult = input.matches("(?i)^[a-z]+(?:[\\s-]+[a-z]+)*$") && startsWithNumberWord(input)
                ? parseWords(input) : null;
        if (wordResult != null && wordResult > 0) {
            return wordResult;
        }

        return null;
    }

    private static boolean startsWithNumberWord(String input) {
        String first = input.toLowerCase().split("[\\s-]+")[0];
        return NUMBER_WORDS.containsKey(first);
    }

    public static String extractUom(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String clean = text.trim();
        clean = clean.replaceAll("(?i)^(?:required\\s+)?(?:quantity|qty)\\s*[:=]\\s*", "").trim();

        // 1. Check digit followed by UOM (e.g. "25 Nos", "5,000 bags", "7 units")
        Pattern digitWithUom = Pattern.compile("(?i)^[0-9,\\.]+\\s*([a-z]+)$");
        Matcher m1 = digitWithUom.matcher(clean);
        if (m1.find()) {
            return capitalizeWord(m1.group(1));
        }

        // 2. Check number words followed by UOM (e.g. "seven units", "five hundred bags", "two thousand meters")
        String[] tokens = clean.split("\\s+");
        List<String> uomTokens = new ArrayList<>();
        for (String token : tokens) {
            String lower = token.toLowerCase().replaceAll("[^a-z]", "");
            if (lower.isBlank() || lower.equals("and") || lower.equals("of") || lower.equals("a")) {
                continue;
            }
            if (!NUMBER_WORDS.containsKey(lower)
                    && !lower.equals("hundred")
                    && !lower.equals("thousand") && !lower.equals("thousands")
                    && !lower.equals("lakh") && !lower.equals("lakhs") && !lower.equals("lac") && !lower.equals("lacs")
                    && !lower.equals("million") && !lower.equals("millions")
                    && !lower.equals("crore") && !lower.equals("crores")) {
                uomTokens.add(token.replaceAll("[^a-zA-Z]", ""));
            }
        }
        if (!uomTokens.isEmpty()) {
            String uomStr = String.join(" ", uomTokens).trim();
            if (!uomStr.isBlank()) {
                return capitalizeWord(uomStr);
            }
        }
        return null;
    }

    private static String capitalizeWord(String str) {
        if (str == null || str.isBlank()) return str;
        str = str.trim();
        if (str.length() == 1) return str.toUpperCase();
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    public static Double parseWords(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String clean = text.toLowerCase()
                .replaceAll("[-_]", " ")
                .replaceAll("[^a-z0-9\\s]", " ")
                .trim();

        String[] tokens = clean.split("\\s+");

        long total = 0L;
        long currentGroup = 0L;
        boolean foundNumberWord = false;

        for (String token : tokens) {
            if (token.isBlank() || token.equals("and") || token.equals("of") || token.equals("a")) {
                continue;
            }

            if (NUMBER_WORDS.containsKey(token)) {
                foundNumberWord = true;
                currentGroup += NUMBER_WORDS.get(token);
            } else if (token.equals("hundred")) {
                foundNumberWord = true;
                if (currentGroup == 0L) currentGroup = 1L;
                currentGroup *= 100L;
            } else if (token.equals("thousand") || token.equals("thousands")) {
                foundNumberWord = true;
                if (currentGroup == 0L) currentGroup = 1L;
                total += currentGroup * 1000L;
                currentGroup = 0L;
            } else if (token.equals("lakh") || token.equals("lakhs") || token.equals("lac") || token.equals("lacs")) {
                foundNumberWord = true;
                if (currentGroup == 0L) currentGroup = 1L;
                total += currentGroup * 100000L;
                currentGroup = 0L;
            } else if (token.equals("million") || token.equals("millions")) {
                foundNumberWord = true;
                if (currentGroup == 0L) currentGroup = 1L;
                total += currentGroup * 1000000L;
                currentGroup = 0L;
            } else if (token.equals("crore") || token.equals("crores")) {
                foundNumberWord = true;
                if (currentGroup == 0L) currentGroup = 1L;
                total += currentGroup * 10000000L;
                currentGroup = 0L;
            }
        }

        total += currentGroup;

        if (foundNumberWord && total > 0) {
            return (double) total;
        }

        return null;
    }
}
