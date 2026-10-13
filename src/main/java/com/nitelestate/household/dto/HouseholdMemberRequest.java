package com.nitelestate.household.dto;

import com.nitelestate.household.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * No propertyId here on purpose: the property is always the sponsoring resident's own (resolved
 * server-side in HouseholdMemberService.request, same pattern as WorkerRequest). The pass still
 * needs the landlord (or an admin) to approve() it before a QR is issued — but there's no
 * validity window to set; once approved it's valid indefinitely, until revoked.
 */
public record HouseholdMemberRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        @NotNull Relationship relationship,
        /** Optional base64 data URI, e.g. "data:image/jpeg;base64,...". Size-capped in HouseholdMemberService. */
        String photo
) {
}
