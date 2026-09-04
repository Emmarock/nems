package com.cyrev.nitelestate.user;

/**
 * Normalizes a phone number to its last 10 digits, so a resident can log in whether they type
 * 08023797036, +2348023797036, or 2348023797036 - all three collapse to the same key. Used both
 * when storing a login phone and when matching one at login time (see CustomUserDetailsService).
 */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() < 10) {
            return null;
        }
        return digits.substring(digits.length() - 10);
    }
}
