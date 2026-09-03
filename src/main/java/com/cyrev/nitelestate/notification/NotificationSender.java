package com.cyrev.nitelestate.notification;

/**
 * One implementation per channel. Real Termii/Twilio/SES-backed senders can replace the
 * Mock* implementations later by implementing this interface and registering a bean for
 * the same channel() — no changes needed in NotificationService.
 */
public interface NotificationSender {
    NotificationChannel channel();
    void send(String recipient, String message);
}
