package com.nitelestate.notification;

/**
 * Published by NotificationService.dispatch() and picked up asynchronously by
 * NotificationService.handle() - see that class for why (decouples the caller, e.g. UserService
 * creating an account, from the latency/failure of an outbound Twilio/Resend call).
 *
 * subject and transactional are per-channel hints (EMAIL and SMS respectively) that most
 * dispatches leave at their defaults — see NotificationSender's 4-arg send for what they do.
 * attachment is EMAIL-only too (e.g. a generated payment receipt PDF) and null for everything else.
 */
public record NotificationRequestedEvent(
        NotificationChannel channel, String recipient, String message, String subject, boolean transactional,
        NotificationAttachment attachment) {
}
