package com.cyrev.nitelestate.worker.dto;

import com.cyrev.nitelestate.worker.WorkerStatus;

import java.time.LocalDate;

/**
 * What a security officer sees on scanning a worker's QR pass: who they are (including their
 * photo, so it can be checked against the person standing at the gate), what they're doing,
 * and — critically — the destination (sponsoring resident + property) to confirm against, per
 * Nitel_Estate_Phase_2_Resident_Experience.md §4.
 */
public record WorkerLookupResponse(
        Long id,
        String fullName,
        String phone,
        String contractorName,
        String workType,
        LocalDate startDate,
        LocalDate expectedEndDate,
        WorkerStatus status,
        String flagReason,
        String photo,
        Long sponsorResidentId,
        String sponsorResidentName,
        String sponsorResidentPhone,
        Long propertyId,
        String propertyHouseNumber,
        String propertyAddress
) {
}
