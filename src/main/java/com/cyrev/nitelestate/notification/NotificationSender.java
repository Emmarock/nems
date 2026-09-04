package com.cyrev.nitelestate.notification;

/**
 * One implementation per channel, active at a time. Real senders (sender.TwilioSmsSender,
 * sender.TwilioWhatsAppSender, sender.SendGridEmailSender) are @Conditional on their provider's
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
}
