package com.nitelestate.household.dto;

import com.nitelestate.household.HouseholdMember;
import com.nitelestate.household.HouseholdMemberStatus;
import com.nitelestate.household.Relationship;
import com.nitelestate.resident.Resident;

public record HouseholdMemberResponse(
        Long id,
        String fullName,
        String phone,
        Relationship relationship,
        Long propertyId,
        String propertyHouseNumber,
        Long sponsorResidentId,
        String sponsorResidentName,
        String sponsorResidentPhone,
        HouseholdMemberStatus status,
        String qrToken,
        String photo
) {
    public static HouseholdMemberResponse from(HouseholdMember m, String propertyHouseNumber) {
        return from(m, propertyHouseNumber, null);
    }

    /** The sponsor (the tenant who requested the pass) lets the landlord see whose household it belongs to. */
    public static HouseholdMemberResponse from(HouseholdMember m, String propertyHouseNumber, Resident sponsor) {
        return new HouseholdMemberResponse(m.getId(), m.getFullName(), m.getPhone(), m.getRelationship(),
                m.getPropertyId(), propertyHouseNumber, m.getSponsorResidentId(),
                sponsor != null ? sponsor.getFullName() : null, sponsor != null ? sponsor.getPhone() : null,
                m.getStatus(), m.getQrToken(), m.getPhoto());
    }
}
