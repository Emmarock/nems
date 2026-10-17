package com.nitelestate.auth;

import jakarta.validation.constraints.NotBlank;

/** {@code identifier} is an email or phone number, same as LoginRequest. */
public record ForgotPasswordRequest(@NotBlank String identifier) {
}
