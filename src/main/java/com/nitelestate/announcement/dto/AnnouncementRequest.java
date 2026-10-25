package com.nitelestate.announcement.dto;

import com.nitelestate.notification.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AnnouncementRequest(
        @NotBlank String title,
        @NotBlank String message,
        @NotEmpty Set<NotificationChannel> channels,
        /** Optional base64 data URI - image/* or application/pdf (e.g. meeting minutes), e.g.
         * "data:image/jpeg;base64,..." or "data:application/pdf;base64,...". Size- and type-capped,
         * and mutually exclusive with the SMS channel, in AnnouncementService. */
        String attachment,
        /** Optional original filename for {@code attachment} (e.g. "October CDA Minutes.pdf"). */
        String attachmentFilename,
        /** Null defaults to true (everyone) - see Announcement.includeTenants. */
        Boolean includeTenants
) {
}
