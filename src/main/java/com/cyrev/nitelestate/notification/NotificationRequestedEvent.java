package com.cyrev.nitelestate.notification;

/**
 * Published by NotificationService.dispatch() and picked up asynchronously by
 * NotificationService.handle() - see that class for why (decouples the caller, e.g. UserService
 * creating an account, from the latency/failure of an outbound Twilio/SendGrid call).
 */
public record NotificationRequestedEvent(NotificationChannel channel, String recipient, String message) {
}
