package com.cyrev.nitelestate.property.dto;

import com.cyrev.nitelestate.property.OccupancyStatus;
import com.cyrev.nitelestate.property.Property;
import com.cyrev.nitelestate.property.PropertyType;

public record PropertyResponse(
        Long id,
        String block,
        String plot,
        String houseNumber,
        String address,
        PropertyType propertyType,
        Long ownerId,
        String ownerName,
        OccupancyStatus occupancyStatus
) {
    public static PropertyResponse from(Property p, String ownerName) {
        return new PropertyResponse(p.getId(), p.getBlock(), p.getPlot(), p.getHouseNumber(), p.getAddress(),
                p.getPropertyType(), p.getOwnerId(), ownerName, p.getOccupancyStatus());
    }
}
