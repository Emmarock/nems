package com.cyrev.nitelestate.resident.dto;

import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentStatus;
import com.cyrev.nitelestate.resident.ResidentType;

import java.time.LocalDate;

public record ResidentResponse(
        Long id,
        String fullName,
        String phone,
        String email,
        Long propertyId,
        String propertyHouseNumber,
        ResidentType residentType,
        String emergencyContact,
        ResidentStatus status,
        LocalDate registrationDate
) {
    public static ResidentResponse from(Resident r, String propertyHouseNumber) {
        return new ResidentResponse(r.getId(), r.getFullName(), r.getPhone(), r.getEmail(), r.getPropertyId(),
                propertyHouseNumber, r.getResidentType(), r.getEmergencyContact(), r.getStatus(), r.getRegistrationDate());
    }
}
