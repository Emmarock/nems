package com.cyrev.nitelestate.visitor.dto;

import com.cyrev.nitelestate.visitor.Visitor;
import com.cyrev.nitelestate.visitor.VisitorStatus;

import java.time.Instant;

public record VisitorResponse(
        Long id,
        String name,
        String phone,
        String vehiclePlate,
        Long hostResidentId,
        String hostResidentName,
        Instant validFrom,
        Instant validUntil,
        String qrToken,
        VisitorStatus status,
        String photo
) {
    public static VisitorResponse from(Visitor v, String hostResidentName) {
        return new VisitorResponse(v.getId(), v.getName(), v.getPhone(), v.getVehiclePlate(), v.getHostResidentId(),
                hostResidentName, v.getValidFrom(), v.getValidUntil(), v.getQrToken(), v.getStatus(), v.getPhoto());
    }
}
