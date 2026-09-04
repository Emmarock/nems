package com.cyrev.nitelestate.access.dto;

import com.cyrev.nitelestate.access.AccessDirection;
import com.cyrev.nitelestate.access.AccessEvent;
import com.cyrev.nitelestate.access.AccessSubjectType;
import com.cyrev.nitelestate.vehicle.Vehicle;
import com.cyrev.nitelestate.visitor.Visitor;
import com.cyrev.nitelestate.worker.Worker;

import java.time.Instant;

/**
 * subjectName/subjectPhone are populated for VISITOR and WORKER rows (expectedCheckoutAt for
 * VISITOR only - workers have a date range, not a same-day checkout time), the vehicle* fields
 * for VEHICLE rows - null otherwise. Denormalized onto this shared list DTO (rather than a
 * subtype per AccessSubjectType) so the security dashboard's recent-activity table can show who
 * or what a row actually refers to without a follow-up lookup per row.
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
        String vehicleResidentName
) {
    public static AccessEventResponse from(AccessEvent e) {
        return new AccessEventResponse(e.getId(), e.getSubjectType(), e.getSubjectId(), e.getGateId(),
                e.getDirection(), e.getOccurredAt(), e.getVerifiedByUserId(), e.getFlagReason(),
                null, null, null, null, null, null, null, null);
    }

    public static AccessEventResponse ofVisitor(AccessEvent e, Visitor v) {
        return new AccessEventResponse(e.getId(), e.getSubjectType(), e.getSubjectId(), e.getGateId(),
                e.getDirection(), e.getOccurredAt(), e.getVerifiedByUserId(), e.getFlagReason(),
                v.getName(), v.getPhone(), v.getValidUntil(), null, null, null, null, null);
    }

    public static AccessEventResponse ofWorker(AccessEvent e, Worker w) {
        return new AccessEventResponse(e.getId(), e.getSubjectType(), e.getSubjectId(), e.getGateId(),
                e.getDirection(), e.getOccurredAt(), e.getVerifiedByUserId(), e.getFlagReason(),
                w.getFullName(), w.getPhone(), null, null, null, null, null, null);
    }

    public static AccessEventResponse ofVehicle(AccessEvent e, Vehicle vehicle, String residentName) {
        return new AccessEventResponse(e.getId(), e.getSubjectType(), e.getSubjectId(), e.getGateId(),
                e.getDirection(), e.getOccurredAt(), e.getVerifiedByUserId(), e.getFlagReason(),
                null, null, null, vehicle.getPlateNumber(), vehicle.getMake(), vehicle.getModel(), vehicle.getColour(),
                residentName);
    }
}
