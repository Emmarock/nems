package com.cyrev.nitelestate.announcement;

import com.cyrev.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Tracks which user has read which announcement, so the frontend bell can show an unread count. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "announcement_read", uniqueConstraints = @UniqueConstraint(columnNames = {"announcement_id", "user_id"}))
public class AnnouncementRead extends BaseEntity {

    @Column(nullable = false)
    private Long announcementId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Instant readAt = Instant.now();
}
