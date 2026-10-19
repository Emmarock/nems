package com.nitelestate.user.dto;

/**
 * resetUrl is returned here once, directly to the admin who triggered the resend, the same
 * convention as BulkCreateResidentUsersResponse's temporary passwords - necessary because
 * Redaction strips the token out of every durable record of the dispatched message (console
 * logs, NotificationLog), by design, so there is otherwise no way to retrieve a working link if
 * no real email/SMS provider is configured and the message never actually reaches the resident.
 */
public record ResendInviteResponse(UserResponse user, String resetUrl) {
}
