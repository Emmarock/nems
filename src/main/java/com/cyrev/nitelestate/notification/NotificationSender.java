package com.cyrev.nitelestate.notification;

/**
 * One implementation per channel, active at a time. Real senders (sender.TwilioSmsSender,
 * sender.TwilioWhatsAppSender, sender.ResendEmailSender) are @Conditional on their provider's
 * credentials actually being set; the sender.Mock* implementations are @ConditionalOnMissingBean
 * of the matching real sender, so they take over automatically whenever a real one isn't
 * configured — local/dev runs on mocks with zero setup, prod switches to real senders the moment
 * env vars are set, no code changes either way. Swapping providers later is just implementing
 * this interface, adding a Condition, and registering a bean for the same channel() — no changes
 * needed in NotificationService.
 */
public interface NotificationSender {
    NotificationChannel channel();
    void send(String recipient, String message);

    /**
     * Same send, with two optional hints most senders don't need: subject (EMAIL only —
     * ResendEmailSender uses it in place of the generic default, e.g. an announcement's title)
     * and transactional (SMS only — sender.BulkSmsNigeriaSender routes OTPs/verification codes
     * through the provider's dedicated transactional gateway instead of its bulk one, since the
     * provider rejects OTP-shaped content on the bulk route). Default just ignores both and
     * falls back to the plain send - only the two senders above override this.
     */
    default void send(String recipient, String message, String subject, boolean transactional) {
        send(recipient, message);
    }

    /**
     * Same, with an optional EMAIL-only attachment (e.g. a generated payment receipt PDF — see
     * payment.PaymentService.sendReceipt). Default ignores it and falls back to the 4-arg send -
     * only sender.ResendEmailSender and sender.MockEmailSender override this; SMS/WhatsApp
     * senders have no use for it.
     */
    default void send(String recipient, String message, String subject, boolean transactional, NotificationAttachment attachment) {
        send(recipient, message, subject, transactional);
    }
}
