package com.nitelestate.notification;

import com.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A personal, in-app alert for one user — distinct from Announcement, which is estate-wide and
 * every resident sees the same set. Used for events specific to one person, e.g. "your visitor
 * just checked in" — see UserNotificationService.notify.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_notification")
public class UserNotification extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;
}
