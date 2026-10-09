package com.cyrev.nitelestate.household.dto;

import com.cyrev.nitelestate.household.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * No propertyId and no validity dates here on purpose: the property is always the sponsoring
 * resident's own (resolved server-side in HouseholdMemberService.request, same pattern as
 * WorkerRequest), and the validity window is set later by the landlord via
 * HouseholdMemberApprovalRequest — not something the requesting tenant controls.
 */
public record HouseholdMemberRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        @NotNull Relationship relationship,
        /** Optional base64 data URI, e.g. "data:image/jpeg;base64,...". Size-capped in HouseholdMemberService. */
        String photo
) {
}
