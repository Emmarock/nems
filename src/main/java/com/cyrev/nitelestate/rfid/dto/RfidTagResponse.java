package com.cyrev.nitelestate.rfid.dto;

import com.cyrev.nitelestate.rfid.RfidStatus;
import com.cyrev.nitelestate.rfid.RfidTag;

public record RfidTagResponse(
        Long id,
        String tagId,
        Long assignedResidentId,
        Long assignedWorkerId,
        Long vehicleId,
        RfidStatus status
) {
    public static RfidTagResponse from(RfidTag t) {
        return new RfidTagResponse(t.getId(), t.getTagId(), t.getAssignedResidentId(), t.getAssignedWorkerId(),
                t.getVehicleId(), t.getStatus());
    }
}
