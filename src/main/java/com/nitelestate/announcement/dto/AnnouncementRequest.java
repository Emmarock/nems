package com.nitelestate.announcement.dto;

import com.nitelestate.notification.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AnnouncementRequest(
        @NotBlank String title,
        @NotBlank String message,
        @NotEmpty Set<NotificationChannel> channels,
        /** Optional base64 data URI, e.g. "data:image/jpeg;base64,...". Size-capped, and mutually
         * exclusive with the SMS channel, in AnnouncementService. */
        String photo
) {
}
