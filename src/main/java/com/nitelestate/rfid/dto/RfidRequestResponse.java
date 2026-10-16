package com.nitelestate.rfid.dto;

import com.nitelestate.access.AccessSubjectType;
import com.nitelestate.rfid.RfidRequest;
import com.nitelestate.rfid.RfidRequestStatus;

import java.time.Instant;

public record RfidRequestResponse(
        Long id,
        Long requestedByResidentId,
        String requestedByResidentName,
        AccessSubjectType subjectType,
        Long subjectId,
        String subjectName,
        String subjectPhone,
        RfidRequestStatus status,
        Long issuedTagId,
        String issuedTagTagId,
        Instant createdAt
) {
    public static RfidRequestResponse from(RfidRequest r, String requestedByResidentName, String subjectName,
                                            String subjectPhone, String issuedTagTagId) {
        return new RfidRequestResponse(r.getId(), r.getRequestedByResidentId(), requestedByResidentName,
                r.getSubjectType(), r.getSubjectId(), subjectName, subjectPhone, r.getStatus(), r.getIssuedTagId(),
                issuedTagTagId, r.getCreatedAt());
    }
}
