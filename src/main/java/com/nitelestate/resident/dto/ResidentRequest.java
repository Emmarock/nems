package com.nitelestate.resident.dto;

import com.nitelestate.resident.ResidentType;
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
