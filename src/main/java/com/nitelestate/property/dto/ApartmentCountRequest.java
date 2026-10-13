package com.nitelestate.property.dto;

/** Landlord self-service — see PropertyService.updateApartmentCount. Null clears the cap. */
public record ApartmentCountRequest(Integer apartmentCount) {
}
