package com.cyrev.nitelestate.resident.dto;

import com.cyrev.nitelestate.resident.ResidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResidentRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        String email,
        Long propertyId,
        @NotNull ResidentType residentType,
        String emergencyContact
) {
}
