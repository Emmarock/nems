package com.cyrev.nitelestate.property.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Self-service correction of the resident's own house number/address/road (spec §6) - many
 * were synthesized during the 2026 data import since the source spreadsheet rarely had real ones,
 * so residents need a way to fix their own. postcode is optional — Nigeria's federal postcode
 * rollout is recent, most properties won't have one yet. Deliberately excludes propertyType/
 * occupancyStatus/ownerId, which stay admin-managed (see PropertyRequest).
 */
public record MePropertyUpdateRequest(
        @NotBlank String houseNumber,
        String address,
        @NotBlank String road,
        String postcode
) {
}
