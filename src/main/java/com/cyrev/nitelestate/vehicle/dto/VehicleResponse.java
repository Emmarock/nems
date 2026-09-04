package com.cyrev.nitelestate.vehicle.dto;

import com.cyrev.nitelestate.vehicle.Vehicle;
import com.cyrev.nitelestate.vehicle.VehicleStatus;

public record VehicleResponse(
        Long id,
        String plateNumber,
        String vehicleType,
        String make,
        String model,
        String colour,
        Long residentId,
        String residentName,
        VehicleStatus status
) {
    public static VehicleResponse from(Vehicle v, String residentName) {
        return new VehicleResponse(v.getId(), v.getPlateNumber(), v.getVehicleType(), v.getMake(), v.getModel(),
                v.getColour(), v.getResidentId(), residentName, v.getStatus());
    }
}
