package com.nitelestate.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The token is the one-time credential here — no current password, no prior login needed. */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8) String newPassword
) {
}
