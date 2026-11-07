package com.nitelestate.user.dto;

import com.nitelestate.user.Role;

/** A staff member's own office profile: the login email and the personal email notifications go to. */
public record OfficeProfileResponse(String fullName, String email, Role role, String personalEmail) {
}
