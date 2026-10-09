package com.cyrev.nitelestate.household.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** The landlord (or an admin) sets the validity window when approving a pending request. */
public record HouseholdMemberApprovalRequest(
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validUntil
) {
}
