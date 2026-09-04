package com.cyrev.nitelestate.rfid.dto;

import com.cyrev.nitelestate.rfid.RfidStatus;
import com.cyrev.nitelestate.rfid.RfidTag;

public record RfidTagResponse(
        Long id,
        String tagId,
        Long assignedResidentId,
        String assignedResidentName,
        Long assignedWorkerId,
        String assignedWorkerName,
        Long vehicleId,
        String vehiclePlateNumber,
        RfidStatus status
) {
    public static RfidTagResponse from(RfidTag t, String assignedResidentName, String assignedWorkerName,
                                        String vehiclePlateNumber) {
        return new RfidTagResponse(t.getId(), t.getTagId(), t.getAssignedResidentId(), assignedResidentName,
                t.getAssignedWorkerId(), assignedWorkerName, t.getVehicleId(), vehiclePlateNumber, t.getStatus());
    }
}
