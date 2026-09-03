package com.cyrev.nitelestate.access.dto;

import com.cyrev.nitelestate.access.AccessDirection;
import com.cyrev.nitelestate.access.AccessEvent;
import com.cyrev.nitelestate.access.AccessSubjectType;

import java.time.Instant;

public record AccessEventResponse(
        Long id,
        AccessSubjectType subjectType,
        Long subjectId,
        Long gateId,
        AccessDirection direction,
        Instant occurredAt,
        Long verifiedByUserId,
        String flagReason
) {
    public static AccessEventResponse from(AccessEvent e) {
        return new AccessEventResponse(e.getId(), e.getSubjectType(), e.getSubjectId(), e.getGateId(),
                e.getDirection(), e.getOccurredAt(), e.getVerifiedByUserId(), e.getFlagReason());
    }
}
