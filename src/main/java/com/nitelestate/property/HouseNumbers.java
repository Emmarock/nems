package com.nitelestate.property;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Estate house numbers follow "<ROAD IN CAPITALS>-<3-digit number>", e.g. "ROAD A-007" or
 * "1ST AVENUE-033". Used when a new property is created without a house number: it gets the
 * next number on its road.
 */
public final class HouseNumbers {

    private HouseNumbers() {
    }

    /**
     * The next unused house number on {@code road}: one more than the highest "<ROAD>-NNN" already
     * taken (gaps are not reused, so a number never silently moves to a different house). Other
     * formats on the same road, such as the imported "ROAD A-NEW2-001" series, are ignored.
     */
    public static String next(PropertyRepository propertyRepository, String road) {
        String prefix = road.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        Pattern ours = Pattern.compile("^" + Pattern.quote(prefix) + "-(\\d+)$");
        int max = 0;
        for (String taken : propertyRepository.findHouseNumbersLike(escapeLike(prefix) + "-%")) {
            Matcher m = ours.matcher(taken.toUpperCase(Locale.ROOT));
            if (m.matches() && m.group(1).length() <= 9) {
                max = Math.max(max, Integer.parseInt(m.group(1)));
            }
        }
        String candidate;
        int n = max;
        do {
            candidate = prefix + "-" + String.format("%03d", ++n);
        } while (propertyRepository.existsByHouseNumberIgnoreCase(candidate));
        return candidate;
    }

    /** Roads are typed by admins, so % and _ in them must not act as LIKE wildcards. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
