package com.nitelestate.user;

import com.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_user")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    /** Normalized (last-10-digit) phone number - an alternative login identifier. See PhoneNumbers. */
    @Column(unique = true, length = 20)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status = UserStatus.ACTIVE;

    /** Set only when role == RESIDENT; links this login to their Resident profile. */
    @Column
    private Long residentId;

    /**
     * True for a bulk-created or admin-reset account whose current password wasn't chosen by
     * its holder. While true, {@link com.nitelestate.security.CustomUserDetails} grants
     * only enough authority to call PUT /api/v1/auth/password - every other endpoint 403s until
     * they set their own password, which clears this flag (see UserService.changePassword).
     */
    @Column(nullable = false)
    private boolean mustChangePassword = false;

    /**
     * One-time "set your password" link token (see UserService.issueResetToken /
     * resetPasswordWithToken) - only the SHA-256 hash is stored, same reasoning as passwordHash:
     * a DB leak must not hand out usable tokens. Cleared the moment it's used or superseded by a
     * newer one; resetTokenExpiresAt null or in the past means no link is currently valid.
     */
    @Column(length = 64)
    private String resetTokenHash;

    private Instant resetTokenExpiresAt;
}
