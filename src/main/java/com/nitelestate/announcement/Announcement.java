package com.nitelestate.announcement;

import com.nitelestate.common.BaseEntity;
import com.nitelestate.notification.NotificationChannel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/** Estate-wide notice (spec §8). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "announcement")
public class Announcement extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(nullable = false)
    private Long createdByUserId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "announcement_channel", joinColumns = @JoinColumn(name = "announcement_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private Set<NotificationChannel> channels = new HashSet<>();

    /**
     * Optional image or PDF (e.g. the secretary sharing meeting minutes) - shown/downloadable in
     * the portal list/detail and emailed as an attachment. Never sent via SMS
     * (AnnouncementService.create rejects that combination), so it's not deliverable everywhere a
     * plain-text announcement is. Base64 data URI, same convention as Worker/Visitor/
     * HouseholdMember photos.
     */
    @Column(columnDefinition = "text")
    private String attachment;

    /** Original filename for {@link #attachment} (e.g. "October CDA Minutes.pdf") - purely
     * cosmetic, used for the download link/email attachment name instead of a generic one. */
    private String attachmentFilename;

    /**
     * Whether this announcement's audience includes tenants - defaults to false (an announcement
     * has to opt tenants in, not opt them out - see AnnouncementService.create). Controls both who
     * it's broadcast to (AnnouncementService.broadcast) and who sees it at all in their portal list
     * (AnnouncementService.search) - a tenant excluded from the audience doesn't just skip the
     * SMS/email, the announcement doesn't appear for them.
     */
    @Column(nullable = false)
    private boolean includeTenants = false;
}
