package com.cyrev.nitelestate.auth;

public record LoginResponse(
        String token,
        String email,
        String fullName,
        String role,
        Long residentId,
        boolean mustChangePassword
) {
}
