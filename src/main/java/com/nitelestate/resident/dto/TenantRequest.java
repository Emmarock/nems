package com.nitelestate.resident.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * No propertyId/residentType here on purpose: a landlord adding a tenant can only add them to
 * their own property, resolved server-side in ResidentService.createTenant — residentType is
 * always TENANT.
 */
public record TenantRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        String email,
        String emergencyContact
) {
}
