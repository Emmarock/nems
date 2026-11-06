package com.nitelestate.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record VisitorRequest(
        @NotBlank String name,
        @NotBlank String phone,
        String vehiclePlate,
        @NotNull Instant validFrom,
        @NotNull Instant validUntil,
        /** Required base64 data URI, e.g. "data:image/jpeg;base64,..." - every visitor must have a
         * photo so security can check them at the gate. Format and size are checked in VisitorService. */
        @NotBlank(message = "A photo of the visitor is required") String photo
) {
}
