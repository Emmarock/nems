package com.cyrev.nitelestate.announcement;

import com.cyrev.nitelestate.common.BaseEntity;
import com.cyrev.nitelestate.notification.NotificationChannel;
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
}
