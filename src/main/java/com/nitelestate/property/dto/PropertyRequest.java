package com.nitelestate.property.dto;

import com.nitelestate.property.OccupancyStatus;
import com.nitelestate.property.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PropertyRequest(
        @NotBlank String road,
        @NotBlank String houseNumber,
        @NotBlank String address,
        /** Optional — Nigeria's federal postcode rollout is recent, most properties won't have one yet. */
        String postcode,
        @NotNull PropertyType propertyType,
        Long ownerId,
        OccupancyStatus occupancyStatus,
        Integer apartmentCount
) {
}
