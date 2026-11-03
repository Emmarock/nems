package com.nitelestate.resident.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Landlord correcting a newly added tenant's email (see ResidentService.updateTenantEmail). */
public record TenantEmailUpdateRequest(@NotBlank @Email String email) {
}
