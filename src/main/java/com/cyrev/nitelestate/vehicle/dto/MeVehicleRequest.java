package com.cyrev.nitelestate.vehicle.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Self-service vehicle registration (spec §6) - deliberately excludes residentId, which
 * MeController resolves server-side from the caller. Using the admin-facing VehicleRequest
 * here directly was a bug: its @NotNull residentId failed @Valid on every self-service call,
 * since the client never sends one (it's injected afterward).
 */
public record MeVehicleRequest(
        @NotBlank String plateNumber,
        String vehicleType,
        String make,
        String model,
        String colour
) {
}
