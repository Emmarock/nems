package com.nitelestate.access.dto;

import com.nitelestate.access.AccessDirection;
import com.nitelestate.access.AccessEvent;
import com.nitelestate.access.AccessSubjectType;
import com.nitelestate.household.HouseholdMember;
import com.nitelestate.vehicle.Vehicle;
import com.nitelestate.visitor.Visitor;
import com.nitelestate.worker.Worker;

import java.time.Instant;

/**
 * subjectName/subjectPhone are populated for VISITOR and WORKER rows (expectedCheckoutAt for
 * VISITOR only - workers have a date range, not a same-day checkout time), the vehicle* fields
 * for VEHICLE rows - null otherwise. residentName is the resident tied to the row regardless of
 * subject type: the host for a VISITOR, the sponsor for a WORKER, the owner for a VEHICLE -
 * everywhere else in this app those are three separate relationships, but on this shared
 * recent-activity list they answer the same question ("whose resident account is this on"), so
 * one column covers all three rather than three near-duplicate fields. Denormalized onto this
 * shared list DTO (rather than a subtype per AccessSubjectType) so the security dashboard's
 * recent-activity table can show who or what a row actually refers to without a follow-up lookup
 * per row.
 */
public record AccessEventResponse(
        Long id,
        AccessSubjectType subjectType,
        Long subjectId,
        Long gateId,
        AccessDirection direction,
        Instant occurredAt,
        Long verifiedByUserId,
        String flagReason,
        String subjectName,
        String subjectPhone,
        Instant expectedCheckoutAt,
        String vehiclePlateNumber,
        String vehicleMake,
        String vehicleModel,
        String vehicleColour,
        String residentName
) {
    public static AccessEventResponse from(AccessEvent e) {
        return new AccessEventResponse(e.getId(), e.getSubjectType(), e.getSubjectId(), e.getGateId(),
                e.getDirection(), e.getOccurredAt(), e.getVerifiedByUserId(), e.getFlagReason(),
                null, null, null, null, null, null, null, null);
    }

    public static AccessEventResponse ofVisitor(AccessEvent e, Visitor v, String hostResidentName) {
        return new AccessEventResponse(e.getId(), e.getSubjectType(), e.getSubjectId(), e.getGateId(),
                e.getDirection(), e.getOccurredAt(), e.getVerifiedByUserId(), e.getFlagReason(),
                v.getName(), v.getPhone(), v.getValidUntil(), null, null, null, null, hostResidentName);
    }

    public static AccessEventResponse ofWorker(AccessEvent e, Worker w, String sponsorResidentName) {
        return new AccessEventResponse(e.getId(), e.getSubjectType(), e.getSubjectId(), e.getGateId(),
                e.getDirection(), e.getOccurredAt(), e.getVerifiedByUserId(), e.getFlagReason(),
                w.getFullName(), w.getPhone(), null, null, null, null, null, sponsorResidentName);
    }

    public static AccessEventResponse ofVehicle(AccessEvent e, Vehicle vehicle, String ownerResidentName) {
        return new AccessEventResponse(e.getId(), e.getSubjectType(), e.getSubjectId(), e.getGateId(),
                e.getDirection(), e.getOccurredAt(), e.getVerifiedByUserId(), e.getFlagReason(),
                null, null, null, vehicle.getPlateNumber(), vehicle.getMake(), vehicle.getModel(), vehicle.getColour(),
                ownerResidentName);
    }

    public static AccessEventResponse ofHouseholdMember(AccessEvent e, HouseholdMember m, String sponsorResidentName) {
        return new AccessEventResponse(e.getId(), e.getSubjectType(), e.getSubjectId(), e.getGateId(),
                e.getDirection(), e.getOccurredAt(), e.getVerifiedByUserId(), e.getFlagReason(),
                m.getFullName(), m.getPhone(), null, null, null, null, null, sponsorResidentName);
    }
}
