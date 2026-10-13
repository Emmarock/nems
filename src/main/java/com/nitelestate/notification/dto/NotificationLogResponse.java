package com.nitelestate.notification.dto;

import com.nitelestate.notification.NotificationChannel;
import com.nitelestate.notification.NotificationLog;
import com.nitelestate.notification.NotificationStatus;

import java.time.Instant;

/** message is already redacted — see NotificationService.handle, which redacts before persisting NotificationLog.message in the first place. */
public record NotificationLogResponse(
        Long id, NotificationChannel channel, String recipient, String message, NotificationStatus status, Instant createdAt) {

    public static NotificationLogResponse from(NotificationLog log) {
        return new NotificationLogResponse(
                log.getId(), log.getChannel(), log.getRecipient(), log.getMessage(), log.getStatus(), log.getCreatedAt());
    }
}
