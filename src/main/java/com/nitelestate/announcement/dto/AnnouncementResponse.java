package com.nitelestate.announcement.dto;

import com.nitelestate.announcement.Announcement;
import com.nitelestate.notification.NotificationChannel;

import java.time.Instant;
import java.util.Set;

public record AnnouncementResponse(
        Long id,
        String title,
        String message,
        Long createdByUserId,
        Set<NotificationChannel> channels,
        String photo,
        Instant createdAt,
        boolean read
) {
    public static AnnouncementResponse from(Announcement a, boolean read) {
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getMessage(), a.getCreatedByUserId(),
                a.getChannels(), a.getPhoto(), a.getCreatedAt(), read);
    }
}
