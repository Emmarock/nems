package com.cyrev.nitelestate.visitor.dto;

import com.cyrev.nitelestate.visitor.VisitorStatus;

import java.time.Instant;

/**
 * What a security officer sees on scanning a visitor's QR pass: who they are, and —
 * critically — the destination (host resident + property) to confirm against, per
 * Nitel_Estate_Management_System_NEMS.md §9.
 */
public record VisitorLookupResponse(
        Long id,
        String name,
        String phone,
        String vehiclePlate,
        Instant validFrom,
        Instant validUntil,
        VisitorStatus status,
        String flagReason,
        Long hostResidentId,
        String hostResidentName,
        String hostResidentPhone,
        Long propertyId,
        String propertyHouseNumber,
        String propertyAddress
) {
}
