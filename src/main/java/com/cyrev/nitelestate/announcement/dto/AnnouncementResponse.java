package com.cyrev.nitelestate.announcement.dto;

import com.cyrev.nitelestate.announcement.Announcement;
import com.cyrev.nitelestate.notification.NotificationChannel;

import java.time.Instant;
import java.util.Set;

public record AnnouncementResponse(
        Long id,
        String title,
        String message,
        Long createdByUserId,
        Set<NotificationChannel> channels,
        Instant createdAt,
        boolean read
) {
    public static AnnouncementResponse from(Announcement a, boolean read) {
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getMessage(), a.getCreatedByUserId(),
                a.getChannels(), a.getCreatedAt(), read);
    }
}
