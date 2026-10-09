package com.cyrev.nitelestate.notification;

/**
 * Canonical wording for every notification UserService sends — the copy lives here, once,
 * instead of being built inline wherever a message gets dispatched. The same plain text returned
 * from these methods carries unchanged to SMS and WhatsApp; for EMAIL, ResendEmailSender wraps it
 * in sender.EmailTemplate's branded HTML on top.
 */
public final class MessageTemplates {

    private static final String BRAND = "Nitel Estate Management System";

    private MessageTemplates() {
    }

    /**
     * Sent the moment a login is provisioned for someone — admin-created, bulk-created, or
     * landlord-added tenant alike. loginUrl is plain text here (SMS/WhatsApp show it as-is);
     * EmailTemplate auto-linkifies it into a clickable button for the HTML version.
     */
    public static String accountProvisioned(String loginId, String password, boolean mustChangePassword, String loginUrl) {
        String passwordNote = mustChangePassword
                ? "Temporary password: " + password + ". You'll be asked to set your own password on first login."
                : "Your password: " + password;
        return "Welcome to " + BRAND + ". Your login is " + loginId + ". " + passwordNote
                + "\n\nLog in here: " + loginUrl;
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
}
