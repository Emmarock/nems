package com.nitelestate.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Spells out a naira amount for the receipt's "Amount" line, e.g. "Two hundred and forty thousand naira only". */
final class AmountInWords {

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };
    private static final String[] SCALES = {"", "Thousand", "Million", "Billion", "Trillion"};

    private AmountInWords() {
    }

    static String spell(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        long naira = rounded.longValue();
        int kobo = rounded.subtract(BigDecimal.valueOf(naira)).movePointRight(2).intValue();

        String words = naira == 0 ? "Zero" : spellInteger(naira);
        StringBuilder result = new StringBuilder(words).append(" Naira");
        if (kobo > 0) {
            result.append(" and ").append(spellInteger(kobo)).append(" Kobo");
        }
        return result.append(" Only").toString();
    }

    private static String spellInteger(long value) {
        if (value == 0) {
            return "Zero";
        }
        StringBuilder result = new StringBuilder();
        int scaleIndex = 0;
        while (value > 0) {
            int chunk = (int) (value % 1000);
            if (chunk != 0) {
                String chunkWords = spellUnderThousand(chunk);
                if (!SCALES[scaleIndex].isEmpty()) {
                    chunkWords += " " + SCALES[scaleIndex];
                }
                result.insert(0, chunkWords + " ");
            }
            value /= 1000;
            scaleIndex++;
        }
        return result.toString().trim();
    }

    private static String spellUnderThousand(int value) {
        StringBuilder result = new StringBuilder();
        if (value >= 100) {
            result.append(ONES[value / 100]).append(" Hundred");
            value %= 100;
            if (value > 0) {
                result.append(" and ");
            }
        }
        if (value >= 20) {
            result.append(TENS[value / 10]);
            if (value % 10 > 0) {
                result.append("-").append(ONES[value % 10]);
            }
        } else if (value > 0) {
            result.append(ONES[value]);
        }
        return result.toString();
    }
}
