package com.nitelestate.resident.dto;

import com.nitelestate.property.OccupancyStatus;
import com.nitelestate.property.PropertyType;
import com.nitelestate.resident.ResidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Creates a resident and a brand-new property together, linked both ways in one transaction -
 * saves the two-step dance of creating one, then searching for it to link the other (see
 * ResidentService.createWithProperty). The new resident's propertyId always points at the new
 * property; the property's ownerId is only set when residentType is OWNER or LANDLORD - a TENANT
 * occupies it without owning it, same as the regular two-step flow would produce.
 */
public record ResidentWithPropertyRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        String email,
        @NotNull ResidentType residentType,
        String emergencyContact,

        @NotBlank String road,
        @NotBlank String houseNumber,
        @NotBlank String address,
        /** Optional — Nigeria's federal postcode rollout is recent, most properties won't have one yet. */
        String postcode,
        @NotNull PropertyType propertyType,
        OccupancyStatus occupancyStatus,
        Integer apartmentCount
) {
}
