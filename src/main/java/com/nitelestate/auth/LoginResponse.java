package com.nitelestate.auth;

import com.nitelestate.resident.ResidentType;

public record LoginResponse(
        String token,
        String email,
        String fullName,
        String role,
        Long residentId,
        /** Null for non-resident roles — lets the frontend tell a tenant from an owner/landlord without a second round-trip. */
        ResidentType residentType,
        boolean mustChangePassword
) {
}
