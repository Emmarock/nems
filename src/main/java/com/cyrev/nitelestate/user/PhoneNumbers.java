package com.cyrev.nitelestate.user;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * Normalizes any phone number - Nigerian or international - to a real, dispatchable E.164
 * number (e.g. "+2348023797036", "+447911123456"). The same value is used both as the
 * login-matching key (see CustomUserDetailsService) and as the actual SMS/WhatsApp send target,
 * so there's no separate "reconstruct a dispatchable number" step and nothing to get wrong there.
 *
 * "NG" is only a fallback default region, used for numbers typed without a country code
 * (08023797036, 2348023797036) - a number that already carries one (+447911123456) is parsed as
 * that country regardless. This can't be Nigeria-only: several residents have foreign numbers,
 * and blindly prepending +234 to a bare local-format foreign number would silently construct a
 * real but wrong Nigerian number and message a stranger.
 */
public final class PhoneNumbers {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    private static final String DEFAULT_REGION = "NG";

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            PhoneNumber parsed = PHONE_UTIL.parse(raw, DEFAULT_REGION);
            if (!PHONE_UTIL.isValidNumber(parsed)) {
                return null;
            }
            return PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException ex) {
            return null;
        }
    }
}
