package com.cyrev.nitelestate.user.dto;

import com.cyrev.nitelestate.user.Role;
import com.cyrev.nitelestate.user.User;
import com.cyrev.nitelestate.user.UserStatus;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        UserStatus status,
        Long residentId
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getStatus(), u.getResidentId());
    }
}
