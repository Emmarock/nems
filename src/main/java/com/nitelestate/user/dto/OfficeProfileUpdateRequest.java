package com.nitelestate.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Blank or null clears the personal email, so notifications fall back to the login email. */
public record OfficeProfileUpdateRequest(@Email @Size(max = 255) String personalEmail) {
}
