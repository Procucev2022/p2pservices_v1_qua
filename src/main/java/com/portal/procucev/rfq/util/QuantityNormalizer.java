package com.portal.procucev.rfq.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    // Singular forms and common trade units were previously absent, so legitimate quantities such
    // as "1 Set", "10 Pairs", "2 Dozen" or "24 Packets" normalised to null and the item was
    // rejected as quantity-less.
    private static final String TRADE_UNITS =
            "nos?|no\\.|units?|pieces?|pcs?|bags?|items?|boxes|box|sets?|rolls?|pairs?|dozens?|dzn?|"
            + "packets?|pkts?|pkt|packs?|bundles?|cartons?|ctns?|reams?|sheets?|tubes?|cans?|drums?|"
            + "coils?|lengths?|laptops?|systems?|machines?|numbers?";
    private static final Pattern DIGIT_UNIT_PATTERN =
            Pattern.compile("(?i)\\b([0-9,]+(?:\\.[0-9]+)?)\\s*(?:" + TRADE_UNITS + ")\\b");
    private static final Pattern DIRECT_DIGIT_PATTERN = Pattern.compile("^\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*$");
    private static final Pattern SPECIFICATION_INDICATOR_PATTERN = Pattern.compile("(?i)\\b(?:lph|liters?\\s*(?:per|/)\\s*hour|liters?\\s+capacity|capacity|ton|tons|w|watt|watts|hp|gb|tb|mb|ram|ssd|inch|inches|mm|cm|diameter|pn\\d+|bar|psi|v|kv|kva|rpm|hz|star|rating|display|screen|reduction\\s+ratio|per\\s+bag|ratio)\\b");

    /** A quantity literal, allowing grouped thousands in western (1,000) or Indian (1,00,000) style. */
    private static final String QTY_NUMBER = "[0-9]{1,3}(?:,[0-9]{2,3})+|[0-9]+(?:\\.[0-9]+)?";

    /** A number at the very start of a labelled value, e.g. the "25" in "Quantity: 25 UOM: Nos". */
    private static final Pattern LEADING_DIGIT_PATTERN = Pattern.compile("^\\s*(" + QTY_NUMBER + ")\\b");

    /** Punctuation that mail clients and flattened spreadsheets put between an item name and its quantity. */
    private static final String NAME_QTY_SEPARATOR = "[\\s\\-:=@*)\\],|]*";

    /**
     * A quantity written immediately after the item name and qualified by a trade unit, as in
     * "Plain Washers M10 - 1,000 Nos" or the flattened table row "Plain Washers M10 | 1,000 | Nos".
     * Requiring the unit is what keeps a trailing specification number out of the match.
     */
    private static final Pattern TRAILING_UNIT_QTY_PATTERN = Pattern.compile(
            "(?i)^" + NAME_QTY_SEPARATOR + "(" + QTY_NUMBER + ")[\\s|]*(" + TRADE_UNITS + ")\\b");

    /** A quantity written after the item name behind an explicit keyword, as in "Gear Box Seal 40x52x7, Qty: 12". */
    private static final Pattern TRAILING_KEYWORD_QTY_PATTERN = Pattern.compile(
            "(?i)^" + NAME_QTY_SEPARATOR + "(?:required\\s+)?(?:qty|quantity)\\b[\\s:=|]*(?:of\\s+)?(" + QTY_NUMBER + ")\\b");

    /** A quantity written before the item name, as in "500 Nos of Plain Washers M10". */
    private static final Pattern LEADING_UNIT_QTY_PATTERN = Pattern.compile(
            "(?i)(" + QTY_NUMBER + ")\\s*(" + TRADE_UNITS + ")\\b\\s*(?:of\\s+)?[\\s\\-:=|]*$");

    /**
     * A quantity written before the item name with the item itself acting as the unit, as in
     * "we require 10 laptops". The purchasing verb is required, and the number must sit immediately
     * before the name, which is what stops a capacity such as "2 ton air conditioner" from matching.
     */
    private static final Pattern LEADING_PURCHASE_QTY_PATTERN = Pattern.compile(
            "(?i)\\b(?:require|requires|required|need|needs|needed|quote\\s+for|purchase|order|supply|procure)\\s+"
            + "(?:a\\s+total\\s+of\\s+)?(?:about\\s+|approx\\.?\\s+|approximately\\s+)?(" + QTY_NUMBER + ")\\s*$");

    /** Invisible or exotic spacing that varies between mail clients and breaks a literal name match. */
    private static final Pattern MATCH_NOISE_PATTERN = Pattern.compile("[\u00A0\u2000-\u200B]"); // nbsp and exotic spaces

    /** Upper bound on an item name we are willing to turn into a matching pattern. */
    private static final int MAX_MATCHABLE_NAME_LENGTH = 200;

    /** Shortest token stem length that keeps an optional plural suffix from swallowing a whole word. */
    private static final int MIN_STEM_LENGTH = 3;

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

        // 0. Specification Number Guard: If input contains capacity/spec indicators without explicit quantity keywords, return null.
        boolean hasExplicitKeyword = EXPLICIT_KEYWORD_PATTERN.matcher(input).find();
        if (!hasExplicitKeyword && SPECIFICATION_INDICATOR_PATTERN.matcher(input).find()) {
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
            // A labelled value frequently runs straight into the next label, either because the
            // buyer wrote one line ("Quantity: 25 UOM: Nos Location: Bengaluru") or because the
            // HTML-to-text conversion collapsed a label block. Requiring the WHOLE value to be
            // numeric rejected those outright, so an RFQ that stated every detail was still
            // reported as quantity-less. Accept the leading number instead, unless what follows it
            // is a specification unit - "Quantity: 16 GB RAM" must still normalise to null.
            Matcher leadingInValue = LEADING_DIGIT_PATTERN.matcher(extractedValue);
            if (leadingInValue.find()) {
                String remainder = extractedValue.substring(leadingInValue.end()).trim();
                if (!SPECIFICATION_INDICATOR_PATTERN.matcher(remainder).find()) {
                    double val = Double.parseDouble(leadingInValue.group(1).replace(",", ""));
                    if (val > 0 && Double.isFinite(val)) {
                        return val;
                    }
                }
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

    /**
     * Locates the purchase quantity that the buyer wrote next to one specific item name.
     *
     * <p>This exists because the extraction model reliably reads the unit but sometimes drops the
     * number when a requirement is written as a run-on list such as "MS Hex Bolts M10 x 50 mm -
     * 500 Nos, Plain Washers M10 - 1,000 Nos". Every item then arrives with a uom and a null
     * quantity, and the whole RFQ is rejected as quantity-less.
     *
     * <p>The search is anchored on the item's own name, so unlike a whole-email scan it stays
     * correct for multi-item requirements: a quantity is only accepted when it sits directly beside
     * that item. Returns null when no quantity is written next to the name, which leaves the
     * mandatory-quantity rule free to ask the buyer for it.
     *
     * @param text            the email body, attachment text or subject to search
     * @param itemDescription the item name to anchor on
     * @return the quantity and the unit that qualified it, or null when the name carries no quantity
     */
    public static QuantityMatch findQuantityForItem(String text, String itemDescription) {
        if (text == null || text.isBlank() || itemDescription == null || itemDescription.isBlank()) {
            return null;
        }
        Pattern namePattern = buildItemNamePattern(itemDescription);
        if (namePattern == null) {
            return null;
        }
        String haystack = normalizeForMatching(text);
        Matcher nameMatcher = namePattern.matcher(haystack);
        while (nameMatcher.find()) {
            QuantityMatch match = matchQuantityAroundName(haystack, nameMatcher.start(), nameMatcher.end());
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    /** Looks for a quantity directly after the matched name, then directly before it. */
    private static QuantityMatch matchQuantityAroundName(String haystack, int nameStart, int nameEnd) {
        String tail = haystack.substring(nameEnd);

        Matcher keyword = TRAILING_KEYWORD_QTY_PATTERN.matcher(tail);
        if (keyword.find()) {
            Double qty = parseGroupedNumber(keyword.group(1));
            if (qty != null) {
                return new QuantityMatch(qty, null);
            }
        }

        Matcher trailing = TRAILING_UNIT_QTY_PATTERN.matcher(tail);
        if (trailing.find()) {
            Double qty = parseGroupedNumber(trailing.group(1));
            if (qty != null) {
                return new QuantityMatch(qty, capitalizeWord(trailing.group(2)));
            }
        }

        String head = haystack.substring(0, nameStart);

        Matcher leadingUnit = LEADING_UNIT_QTY_PATTERN.matcher(head);
        if (leadingUnit.find()) {
            Double qty = parseGroupedNumber(leadingUnit.group(1));
            if (qty != null) {
                return new QuantityMatch(qty, capitalizeWord(leadingUnit.group(2)));
            }
        }

        Matcher leadingPurchase = LEADING_PURCHASE_QTY_PATTERN.matcher(head);
        if (leadingPurchase.find()) {
            Double qty = parseGroupedNumber(leadingPurchase.group(1));
            if (qty != null) {
                return new QuantityMatch(qty, null);
            }
        }
        return null;
    }

    /**
     * Parses a digit run that may carry grouping commas. The callers pass a regex-captured number,
     * so this rejects only the values that are matchable but unusable as a quantity: zero, and a
     * digit run long enough to overflow to infinity.
     */
    private static Double parseGroupedNumber(String digits) {
        double val = Double.parseDouble(digits.replace(",", ""));
        return val > 0 && Double.isFinite(val) ? val : null;
    }

    /**
     * Builds a tolerant pattern for an item name.
     *
     * <p>The name is compared against text the buyer typed, so the two rarely agree character for
     * character. Tokens are joined by "any non-alphanumeric run" so "M10x50mm" matches "M10 x 50 mm",
     * and each token accepts an optional plural so a "Desktop Computer" line item still matches
     * "desktop computers".
     */
    private static Pattern buildItemNamePattern(String description) {
        String normalized = normalizeForMatching(description);
        if (normalized.length() > MAX_MATCHABLE_NAME_LENGTH) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int used = 0;
        for (String token : normalized.split("[^a-z0-9]+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (used > 0) {
                sb.append("[^a-z0-9]*");
            }
            String stem = token.endsWith("s") && token.length() > MIN_STEM_LENGTH
                    ? token.substring(0, token.length() - 1)
                    : token;
            sb.append(Pattern.quote(stem)).append("s?");
            used++;
        }
        return used == 0 ? null : Pattern.compile(sb.toString());
    }

    /**
     * Folds the typographic variants a mail client emits into the plain ASCII forms the patterns
     * expect, so a dimension written "M10 x 50 mm" still matches a body that used the
     * multiplication sign, and a quantity behind an en dash still matches one behind a hyphen.
     */
    private static String normalizeForMatching(String text) {
        String out = text
                .replace('\u00D7', 'x')  // multiplication sign
                .replace('\u2715', 'x')  // multiplication x
                .replace('\u2716', 'x')  // heavy multiplication x
                .replace('\u2010', '-')  // hyphen
                .replace('\u2011', '-')  // non-breaking hyphen
                .replace('\u2012', '-')  // figure dash
                .replace('\u2013', '-')  // en dash
                .replace('\u2014', '-')  // em dash
                .replace('\u2212', '-'); // minus sign
        out = MATCH_NOISE_PATTERN.matcher(out).replaceAll(" ");
        return out.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT).trim();
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

    /**
     * A purchase quantity found beside an item name, together with the unit that qualified it.
     *
     * @param quantity the numeric purchase quantity, always greater than zero
     * @param uom      the unit token that qualified the number, or null when the quantity was found
     *                 behind an explicit "Qty:" keyword with no unit written next to it
     */
    public record QuantityMatch(double quantity, String uom) {
    }
}
