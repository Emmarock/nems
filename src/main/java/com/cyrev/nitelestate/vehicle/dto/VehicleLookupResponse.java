package com.cyrev.nitelestate.vehicle.dto;

import com.cyrev.nitelestate.vehicle.VehicleStatus;

/** What security sees scanning a vehicle's QR pass — identifies it and its owner before granting gate access. */
public record VehicleLookupResponse(
        Long id,
        String plateNumber,
        String vehicleType,
        String make,
        String model,
        String colour,
        VehicleStatus status,
        String flagReason,
        Long residentId,
        String residentName,
        String residentPhone,
        Long propertyId,
        String propertyHouseNumber,
        String propertyAddress
) {
}
