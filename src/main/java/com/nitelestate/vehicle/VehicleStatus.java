package com.nitelestate.vehicle;

public enum VehicleStatus {
    /** A tenant's self-registered vehicle lands here until the property's landlord approves it - see VehicleService.registerForResident. */
    PENDING_APPROVAL,
    ACTIVE,
    INACTIVE
}
