package com.cyrev.nitelestate.auth;

import jakarta.validation.constraints.NotBlank;

/** Step 2 of self-service account recovery — see UserService.verifyClaimAccount. */
public record ClaimAccountVerifyRequest(@NotBlank String phone, @NotBlank String code, @NotBlank String email) {
}
