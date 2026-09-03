package com.cyrev.nitelestate.property.dto;

import com.cyrev.nitelestate.property.OccupancyStatus;
import com.cyrev.nitelestate.property.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PropertyRequest(
        @NotBlank String block,
        @NotBlank String plot,
        @NotBlank String houseNumber,
        @NotBlank String address,
        @NotNull PropertyType propertyType,
        Long ownerId,
        OccupancyStatus occupancyStatus
) {
}
