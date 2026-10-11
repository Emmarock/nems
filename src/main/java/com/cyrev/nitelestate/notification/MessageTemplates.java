package com.cyrev.nitelestate.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Canonical wording for every notification UserService sends — the copy lives here, once,
 * instead of being built inline wherever a message gets dispatched. The same plain text returned
 * from these methods carries unchanged to SMS and WhatsApp; for EMAIL, ResendEmailSender wraps it
 * in sender.EmailTemplate's branded HTML on top.
 */
public final class MessageTemplates {

    private static final String BRAND = "Nitel Estate Management System";
    private static final ZoneId ESTATE_ZONE = ZoneId.of("Africa/Lagos");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");

    private MessageTemplates() {
    }

    /**
     * Sent the moment a login is provisioned for someone — admin-created, bulk-created, or
     * landlord-added tenant alike. actionUrl is plain text here (SMS/WhatsApp show it as-is);
     * EmailTemplate auto-linkifies it into a clickable link for the HTML version. It's a one-time
     * "set your password" link (see UserService.issueResetToken) when mustChangePassword is true,
     * otherwise just the ordinary login page — see UserService.notifyNewAccount.
     */
    public static String accountProvisioned(String loginId, String password, boolean mustChangePassword, String actionUrl) {
        String passwordNote = mustChangePassword
                ? "Temporary password: " + password + ". You'll be asked to set your own password on first login."
                : "Your password: " + password;
        String actionLabel = mustChangePassword ? "Set your password here: " : "Log in here: ";
        return "Welcome to " + BRAND + ". Your login is " + loginId + ". " + passwordNote
                + "\n\n" + actionLabel + actionUrl;
    }

    /**
     * WhatsApp-only verification code, step 1 of self-service account recovery (see
     * UserService.startClaimAccount) — proves the requester controls the resident's phone before
     * any reset link is emailed anywhere.
     */
    public static String claimOtp(String code) {
        return "Your " + BRAND + " verification code is " + code + ". It expires in 10 minutes. "
                + "Don't share this code with anyone.";
    }

    /** Sent when an existing resident claims self-service access via phone+email verification (see UserService.claimAccount). */
    public static String accountClaimed(String fullName, String resetUrl) {
        return "Hi " + fullName + ", we found your account on " + BRAND + ".\n\n"
                + "Set your password here: " + resetUrl
                + "\n\nThis link expires in 24 hours. If you didn't request this, you can ignore this message.";
    }

    /** Sent to a landlord when a tenant shares a payment receipt (see PaymentService.shareReceiptWithLandlord). */
    public static String receiptShared(String tenantFullName) {
        return tenantFullName + " shared a payment receipt with you on " + BRAND
                + ". Review it and submit it from your Payments page if it checks out.";
    }

    /** Sent on every successful login — see UserService.notifyLogin for how device/location are resolved. */
    public static String loginAlert(String fullName, String device, String ipAddress, String location) {
        StringBuilder message = new StringBuilder()
                .append("You just signed in to your ").append(BRAND).append(" account as ").append(fullName).append(".\n\n")
                .append("Device: ").append(device).append("\n")
                .append("IP address: ").append(ipAddress != null ? ipAddress : "Unknown").append("\n");
        if (location != null) {
            message.append("Location: ").append(location).append("\n");
        }
        message.append("\nIf this wasn't you, contact an administrator immediately.");
        return message.toString();
    }

    /**
     * Daily nag to the property's landlord (not the sponsoring tenant, even if a tenant is the
     * one who actually requested the pass) — see scheduling.CheckoutReminderJob. Fires once the
     * day the pass is due and again every day it stays active past that date, so a non-domestic
     * worker who never gets checked out doesn't silently stop being flagged.
     */
    public static String workerCheckoutDue(String workerFullName, String contractorName, LocalDate expectedEndDate) {
        return "Reminder: " + workerFullName + " (" + contractorName + ") on " + BRAND
                + " was due to check out on " + expectedEndDate.format(DATE_FORMAT)
                + ". Reach out to have them wrap up and leave if they're still on site.";
    }

    /** Same as workerCheckoutDue, for a visitor pass instead. */
    public static String visitorCheckoutDue(String visitorName, Instant validUntil) {
        return "Reminder: " + visitorName + "'s visitor pass on " + BRAND
                + " was due to end on " + validUntil.atZone(ESTATE_ZONE).format(DATE_TIME_FORMAT)
                + ". Reach out to have them start leaving if they're still around.";
    }
}
