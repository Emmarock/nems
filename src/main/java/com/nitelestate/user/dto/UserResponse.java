package com.nitelestate.user.dto;

import com.nitelestate.user.Role;
import com.nitelestate.user.User;
import com.nitelestate.user.UserStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String phone,
        String fullName,
        Role role,
        UserStatus status,
        Long residentId,
        boolean mustChangePassword,
        /** True once mustChangePassword's "set your password" link has passed its 24h validity
         * (or none was ever issued) - this is what an admin's "Resend invite" action is for. */
        boolean inviteLinkExpired
) {
    public static UserResponse from(User u) {
        boolean expired = u.isMustChangePassword()
                && (u.getResetTokenExpiresAt() == null || u.getResetTokenExpiresAt().isBefore(Instant.now()));
        return new UserResponse(u.getId(), u.getEmail(), u.getPhone(), u.getFullName(), u.getRole(), u.getStatus(),
                u.getResidentId(), u.isMustChangePassword(), expired);
    }
}
