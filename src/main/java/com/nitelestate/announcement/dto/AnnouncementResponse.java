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
        String attachment,
        String attachmentFilename,
        boolean includeTenants,
        Instant createdAt,
        boolean read
) {
    public static AnnouncementResponse from(Announcement a, boolean read) {
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getMessage(), a.getCreatedByUserId(),
                a.getChannels(), a.getAttachment(), a.getAttachmentFilename(), a.isIncludeTenants(), a.getCreatedAt(), read);
    }
}
