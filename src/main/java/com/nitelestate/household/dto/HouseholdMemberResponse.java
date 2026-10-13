package com.nitelestate.household.dto;

import com.nitelestate.household.HouseholdMember;
import com.nitelestate.household.HouseholdMemberStatus;
import com.nitelestate.household.Relationship;

public record HouseholdMemberResponse(
        Long id,
        String fullName,
        String phone,
        Relationship relationship,
        Long propertyId,
        String propertyHouseNumber,
        Long sponsorResidentId,
        HouseholdMemberStatus status,
        String qrToken,
        String photo
) {
    public static HouseholdMemberResponse from(HouseholdMember m, String propertyHouseNumber) {
        return new HouseholdMemberResponse(m.getId(), m.getFullName(), m.getPhone(), m.getRelationship(),
                m.getPropertyId(), propertyHouseNumber, m.getSponsorResidentId(), m.getStatus(),
                m.getQrToken(), m.getPhoto());
    }
}
