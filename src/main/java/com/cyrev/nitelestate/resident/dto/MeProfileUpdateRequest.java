package com.cyrev.nitelestate.resident.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Self-service profile edit (spec §6) - deliberately excludes propertyId/residentType/status,
 * which stay admin-managed (see ResidentRequest for the full admin-facing shape).
 */
public record MeProfileUpdateRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        String email,
        String emergencyContact
) {
}
