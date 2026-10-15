package com.nitelestate.notification.dto;

import com.nitelestate.notification.UserNotification;

import java.time.Instant;

public record UserNotificationResponse(
        Long id,
        String title,
        String message,
        boolean read,
        Instant createdAt
) {
    public static UserNotificationResponse from(UserNotification n) {
        return new UserNotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
