package com.cyrev.nitelestate.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleRequest(
        @NotBlank String plateNumber,
        String vehicleType,
        String make,
        String model,
        String colour,
        @NotNull Long residentId
) {
}
