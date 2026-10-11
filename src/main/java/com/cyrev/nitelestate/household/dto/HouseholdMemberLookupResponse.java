package com.cyrev.nitelestate.household.dto;

import com.cyrev.nitelestate.household.HouseholdMemberStatus;
import com.cyrev.nitelestate.household.Relationship;

/**
 * What a security officer sees on scanning a household member's QR pass: who they are (including
 * their photo), their relationship to the sponsoring resident, and the destination (sponsor +
 * property) to confirm against — same shape as WorkerLookupResponse.
 */
public record HouseholdMemberLookupResponse(
        Long id,
        String fullName,
        String phone,
        Relationship relationship,
        HouseholdMemberStatus status,
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
