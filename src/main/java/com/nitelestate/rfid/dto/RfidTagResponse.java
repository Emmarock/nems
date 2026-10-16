package com.nitelestate.rfid.dto;

import com.nitelestate.rfid.RfidStatus;
import com.nitelestate.rfid.RfidTag;

public record RfidTagResponse(
        Long id,
        String tagId,
        Long assignedResidentId,
        String assignedResidentName,
        Long assignedWorkerId,
        String assignedWorkerName,
        Long assignedVisitorId,
        String assignedVisitorName,
        Long vehicleId,
        String vehiclePlateNumber,
        RfidStatus status
) {
    public static RfidTagResponse from(RfidTag t, String assignedResidentName, String assignedWorkerName,
                                        String assignedVisitorName, String vehiclePlateNumber) {
        return new RfidTagResponse(t.getId(), t.getTagId(), t.getAssignedResidentId(), assignedResidentName,
                t.getAssignedWorkerId(), assignedWorkerName, t.getAssignedVisitorId(), assignedVisitorName,
                t.getVehicleId(), vehiclePlateNumber, t.getStatus());
    }
}
