package com.nitelestate.notification;

import java.math.BigDecimal;
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
     * SMS verification code, step 1 of self-service account recovery (see
     * UserService.startClaimAccount) — proves the requester controls the resident's phone before
     * any reset link is emailed anywhere.
     */
    public static String claimOtp(String code) {
        return "Your " + BRAND + " verification code is " + code + ". It expires in 10 minutes. "
                + "Don't share this code with anyone.";
    }

    /**
     * Sent when someone who already has a login uses "Forgot password?" on the login page — see
     * UserService.startForgotPassword. Unlike accountClaimed (a brand-new login being set up for
     * the first time), this one explicitly reassures a reader who didn't request it that nothing
     * has changed yet, since an existing, working password is what's at stake here.
     */
    public static String passwordResetRequested(String resetUrl) {
        return "We received a request to reset your " + BRAND + " password.\n\n"
                + "Set a new password here: " + resetUrl
                + "\n\nThis link expires in 24 hours. If you didn't request this, you can ignore this message — "
                + "your password hasn't been changed.";
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

    /**
     * Sent to the paying resident the moment their payment is confirmed — either a treasurer/
     * financial secretary/admin approving a submitted receipt, or staff recording one manually
     * (which is itself an approval, made at the point of entry). See PaymentService.sendReceipt,
     * which attaches the full PDF receipt (payment.PaymentReceiptPdf) to this same email — the
     * body here is a short summary, not a duplicate of everything the PDF already carries.
     * levyDescription is allowed to be null (a general, unapplied payment) and is simply left
     * out of the summary when absent.
     */
    public static String paymentReceipt(String residentFullName, String receiptNumber, String levyDescription,
                                         BigDecimal amount, Instant paidAt) {
        StringBuilder message = new StringBuilder()
                .append("Hi ").append(residentFullName).append(", your payment of ").append(formatAmount(amount));
        if (levyDescription != null) {
            message.append(" for ").append(levyDescription);
        }
        message.append(" on ").append(paidAt.atZone(ESTATE_ZONE).format(DATE_TIME_FORMAT))
                .append(" has been confirmed on ").append(BRAND).append(".\n\n")
                .append("Receipt No: ").append(receiptNumber)
                .append("\n\nYour PDF receipt is attached to this email — keep it for your records. Thank you for your payment.");
        return message.toString();
    }

    private static String formatAmount(BigDecimal amount) {
        return "₦" + String.format("%,.2f", amount);
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

    /**
     * Non-domestic staff (contractors/artisans) are expected to check out every day by 6pm even
     * when their overall contract has days left — this fires once, shortly after 6pm, for anyone
     * whose last gate record today is still IN. Distinct from workerCheckoutDue, which is about
     * the contract's own end date, not the daily on-site curfew.
     */
    public static String workerPastDailyCurfew(String workerFullName, String contractorName) {
        return "Heads up: " + workerFullName + " (" + contractorName + ") on " + BRAND
                + " hasn't checked out today and it's past 6pm. Non-domestic staff are expected to check out "
                + "daily, even mid-contract — reach out to confirm they're not staying on site overnight.";
    }

    /**
     * Pre-departure countdown for a visitor pass, sent ~30/20/10 minutes before validUntil — see
     * CheckoutReminderJob.remindApproachingVisitorCheckouts. Distinct from visitorCheckoutDue,
     * which only fires once the pass has already reached its end date.
     */
    public static String visitorCheckoutApproaching(String visitorName, long minutesLeft) {
        return "Reminder: " + visitorName + "'s visitor pass on " + BRAND
                + " ends in about " + minutesLeft + " minutes. Please make sure they're on their way out.";
    }

    /**
     * Pre-curfew countdown for a non-domestic worker still on site, sent ~30/20/10 minutes before
     * the daily 6pm cut-off — see CheckoutReminderJob.remindApproachingWorkerCurfew. A friendly
     * nudge ahead of time, distinct from workerPastDailyCurfew's post-6pm "something's wrong" catch.
     */
    public static String workerCurfewApproaching(String workerFullName, String contractorName, long minutesLeft) {
        return "Reminder: " + workerFullName + " (" + contractorName + ") on " + BRAND
                + " needs to check out in about " + minutesLeft + " minutes (by 6pm, the daily cut-off for non-domestic staff).";
    }

    /** Sent to the host resident the instant their visitor checks in at the gate — see VisitorService.scan. */
    public static String visitorCheckedIn(String hostFullName, String visitorName) {
        return "Hi " + hostFullName + ", your visitor " + visitorName + " has just checked in at the gate on " + BRAND + ".";
    }

    /** Sent to the host resident the instant their visitor checks out at the gate — see VisitorService.scan. */
    public static String visitorCheckedOut(String hostFullName, String visitorName) {
        return "Hi " + hostFullName + ", your visitor " + visitorName + " has just checked out at the gate on " + BRAND + ".";
    }

    /** Sent to the sponsoring resident the instant their worker checks in at the gate — see WorkerService.checkIn. */
    public static String workerCheckedIn(String hostFullName, String workerName, String contractorName) {
        return "Hi " + hostFullName + ", your worker " + workerName + " (" + contractorName + ") has just checked in at the gate on " + BRAND + ".";
    }

    /** Sent to the sponsoring resident the instant their worker checks out at the gate — see WorkerService.checkOut. */
    public static String workerCheckedOut(String hostFullName, String workerName, String contractorName) {
        return "Hi " + hostFullName + ", your worker " + workerName + " (" + contractorName + ") has just checked out at the gate on " + BRAND + ".";
    }

    /**
     * Weekly nag sent straight to a resident whose account is in arrears — see
     * scheduling.ArrearsReminderJob (every Saturday 10am estate-local). Unlike workerCheckoutDue
     * etc., this goes to the debtor themselves, not a landlord/sponsor — it's their own balance.
     */
    public static String arrearsReminder(String residentFullName, BigDecimal outstanding) {
        return "Hi " + residentFullName + ", this is a reminder from " + BRAND + " that your account currently has an "
                + "outstanding balance of " + formatAmount(outstanding) + ". Please settle this at your earliest "
                + "convenience — see the Payments page in your resident portal for how to pay.";
    }
}
