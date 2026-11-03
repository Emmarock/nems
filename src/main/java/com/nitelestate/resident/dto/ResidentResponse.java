package com.nitelestate.resident.dto;

import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentStatus;
import com.nitelestate.resident.ResidentType;

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
        LocalDate registrationDate,
        /** Only set on the landlord's tenant list: true while the tenant hasn't logged in and set a password yet. */
        Boolean invitePending
) {
    public static ResidentResponse from(Resident r, String propertyHouseNumber) {
        return from(r, propertyHouseNumber, null);
    }

    public static ResidentResponse from(Resident r, String propertyHouseNumber, Boolean invitePending) {
        return new ResidentResponse(r.getId(), r.getFullName(), r.getPhone(), r.getEmail(), r.getPropertyId(),
                propertyHouseNumber, r.getResidentType(), r.getEmergencyContact(), r.getStatus(), r.getRegistrationDate(),
                invitePending);
    }
}
