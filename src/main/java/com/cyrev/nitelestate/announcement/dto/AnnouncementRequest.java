package com.cyrev.nitelestate.announcement.dto;

import com.cyrev.nitelestate.notification.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AnnouncementRequest(
        @NotBlank String title,
        @NotBlank String message,
        @NotEmpty Set<NotificationChannel> channels
) {
}
