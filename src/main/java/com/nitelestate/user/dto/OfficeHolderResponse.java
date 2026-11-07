package com.nitelestate.user.dto;

import com.nitelestate.user.Role;

/** One estate office and the resident assigned to it; the resident fields are null when unassigned. */
public record OfficeHolderResponse(
        Role role,
        Long residentId,
        String residentName,
        String residentEmail,
        String residentPhone,
        String propertyHouseNumber
) {
}
