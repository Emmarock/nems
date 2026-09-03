package com.cyrev.nitelestate.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record VisitorRequest(
        @NotBlank String name,
        @NotBlank String phone,
        String vehiclePlate,
        @NotNull Instant validFrom,
        @NotNull Instant validUntil
) {
}
