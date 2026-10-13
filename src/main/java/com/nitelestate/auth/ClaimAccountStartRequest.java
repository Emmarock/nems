package com.nitelestate.auth;

import jakarta.validation.constraints.NotBlank;

/** Step 1 of self-service account recovery — see UserService.startClaimAccount. */
public record ClaimAccountStartRequest(@NotBlank String phone) {
}
