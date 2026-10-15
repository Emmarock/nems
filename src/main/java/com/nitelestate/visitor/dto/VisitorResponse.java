package com.nitelestate.visitor.dto;

import com.nitelestate.visitor.Visitor;
import com.nitelestate.visitor.VisitorStatus;

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
        String qrTokenEntry,
        /** Null until the visitor's first check-in — see Visitor.qrTokenExit. */
        String qrTokenExit,
        VisitorStatus status,
        String photo
) {
    public static VisitorResponse from(Visitor v, String hostResidentName) {
        return new VisitorResponse(v.getId(), v.getName(), v.getPhone(), v.getVehiclePlate(), v.getHostResidentId(),
                hostResidentName, v.getValidFrom(), v.getValidUntil(), v.getQrTokenEntry(), v.getQrTokenExit(),
                v.getStatus(), v.getPhoto());
    }
}
