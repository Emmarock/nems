package com.nitelestate.resident.dto;

import com.nitelestate.resident.ResidentStatus;
import com.nitelestate.resident.ResidentType;

/**
 * What a security officer sees on scanning a resident's own QR pass — the resident IS the
 * destination here (contrast VisitorLookupResponse/WorkerLookupResponse, where the pass
 * holder is heading to someone else's property).
 */
public record ResidentLookupResponse(
        Long id,
        String fullName,
        String phone,
        ResidentType residentType,
        ResidentStatus status,
        String flagReason,
        Long propertyId,
        String propertyHouseNumber,
        String propertyAddress
) {
}
