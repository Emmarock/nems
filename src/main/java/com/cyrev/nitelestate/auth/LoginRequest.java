package com.cyrev.nitelestate.auth;

import jakarta.validation.constraints.NotBlank;

/** {@code identifier} is either the account's email or its registered phone number. */
public record LoginRequest(
        @NotBlank String identifier,
        @NotBlank String password
) {
}
