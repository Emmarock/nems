package com.cyrev.nitelestate.property.dto;

import com.cyrev.nitelestate.property.OccupancyStatus;
import com.cyrev.nitelestate.property.Property;
import com.cyrev.nitelestate.property.PropertyType;

public record PropertyResponse(
        Long id,
        String road,
        String houseNumber,
        String address,
        String postcode,
        PropertyType propertyType,
        Long ownerId,
        String ownerName,
        OccupancyStatus occupancyStatus,
        Integer apartmentCount
) {
    public static PropertyResponse from(Property p, String ownerName) {
        return new PropertyResponse(p.getId(), p.getRoad(), p.getHouseNumber(), p.getAddress(), p.getPostcode(),
                p.getPropertyType(), p.getOwnerId(), ownerName, p.getOccupancyStatus(), p.getApartmentCount());
    }
}
