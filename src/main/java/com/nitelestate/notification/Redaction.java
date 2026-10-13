package com.nitelestate.notification;

import java.util.regex.Pattern;

/**
 * Strips credential material (temporary passwords, password-reset tokens) out of a message
 * before it's logged or persisted (NotificationLog, the Mock*Sender console logs, and every real
 * sender's "not configured, would send" fallback log). The actual outbound call to a real
 * provider (Resend/Twilio) still gets the untouched message — delivering it to its one intended
 * recipient is the whole point and isn't "logging" — this only guards what ends up durable in
 * application logs or the database.
 */
public final class Redaction {

    private static final String MASK = "[REDACTED]";
    private static final Pattern PASSWORD = Pattern.compile("(?i)(password:\\s*)\\S+");
    private static final Pattern TOKEN_PARAM = Pattern.compile("(?i)([?&]token=)[^\\s&]+");
    /** Covers MessageTemplates.claimOtp's "verification code is 123456" - a code is just as much a credential as a password for the few minutes it's valid. */
    private static final Pattern VERIFICATION_CODE = Pattern.compile("(?i)(verification code is )\\d+");

    private Redaction() {
    }

    public static String redact(String message) {
        if (message == null) {
            return null;
        }
        String redacted = PASSWORD.matcher(message).replaceAll("$1" + MASK);
        redacted = TOKEN_PARAM.matcher(redacted).replaceAll("$1" + MASK);
        return VERIFICATION_CODE.matcher(redacted).replaceAll("$1" + MASK);
    }
}
