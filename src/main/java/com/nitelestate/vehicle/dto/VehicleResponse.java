package com.nitelestate.vehicle.dto;

import com.nitelestate.resident.Resident;
import com.nitelestate.vehicle.Vehicle;
import com.nitelestate.vehicle.VehicleStatus;

public record VehicleResponse(
        Long id,
        String plateNumber,
        String vehicleType,
        String make,
        String model,
        String colour,
        Long residentId,
        String residentName,
        String residentPhone,
        VehicleStatus status
) {
    public static VehicleResponse from(Vehicle v, String residentName) {
        return new VehicleResponse(v.getId(), v.getPlateNumber(), v.getVehicleType(), v.getMake(), v.getModel(),
                v.getColour(), v.getResidentId(), residentName, null, v.getStatus());
    }

    /** Includes the owner's phone, so the landlord's approval list can show who to contact. */
    public static VehicleResponse from(Vehicle v, Resident owner) {
        return new VehicleResponse(v.getId(), v.getPlateNumber(), v.getVehicleType(), v.getMake(), v.getModel(),
                v.getColour(), v.getResidentId(), owner != null ? owner.getFullName() : null,
                owner != null ? owner.getPhone() : null, v.getStatus());
    }
}
