package com.cyrev.nitelestate.access;

import com.cyrev.nitelestate.access.dto.AccessEventResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.vehicle.Vehicle;
import com.cyrev.nitelestate.vehicle.VehicleRepository;
import com.cyrev.nitelestate.visitor.Visitor;
import com.cyrev.nitelestate.visitor.VisitorRepository;
import com.cyrev.nitelestate.worker.Worker;
import com.cyrev.nitelestate.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AccessEventService {

    private final AccessEventRepository accessEventRepository;
    private final VisitorRepository visitorRepository;
    private final WorkerRepository workerRepository;
    private final VehicleRepository vehicleRepository;
    private final ResidentRepository residentRepository;

    /**
     * The single choke point every checkIn/checkOut across Resident, Visitor, Worker, and
     * Vehicle goes through (plus the security-initiated /access-events endpoint), so the
     * IN/OUT-must-alternate rule below applies uniformly to every kind of access into the estate
     * without needing to be duplicated in each module's service.
     */
    @Transactional
    public AccessEventResponse record(AccessSubjectType subjectType, Long subjectId, AccessDirection direction,
                                       Long gateId, Long verifiedByUserId, String flagReason) {
        requireAlternatingDirection(subjectType, subjectId, direction);

        AccessEvent event = new AccessEvent();
        event.setSubjectType(subjectType);
        event.setSubjectId(subjectId);
        event.setDirection(direction);
        event.setGateId(gateId);
        event.setVerifiedByUserId(verifiedByUserId);
        event.setFlagReason(flagReason);
        return AccessEventResponse.from(accessEventRepository.save(event));
    }

    /**
     * A subject is either inside the estate or outside it - never both, never neither after
     * their first entry - so a new event's direction must be the opposite of whatever was last
     * recorded for that same subject (regardless of gate: someone can enter at one gate and
     * leave at another, but can't enter twice with no exit between). This is a hard block, not a
     * flag like the arrears/expired-pass checks elsewhere: those are policy calls for security to
     * weigh, this is a logically impossible state (already inside, or never having entered).
     */
    private void requireAlternatingDirection(AccessSubjectType subjectType, Long subjectId, AccessDirection direction) {
        AccessDirection lastDirection = accessEventRepository
                .findTopBySubjectTypeAndSubjectIdOrderByIdDesc(subjectType, subjectId)
                .map(AccessEvent::getDirection)
                .orElse(null);

        if (direction == AccessDirection.IN && lastDirection == AccessDirection.IN) {
            throw new BadRequestException(
                    subjectType + " #" + subjectId + " is already checked in - check out before checking in again");
        }
        if (direction == AccessDirection.OUT && lastDirection != AccessDirection.IN) {
            throw new BadRequestException(
                    subjectType + " #" + subjectId + " is not currently checked in - cannot check out");
        }
    }

    public PageResponse<AccessEventResponse> search(AccessSubjectType subjectType, Long subjectId, Long gateId,
                                                      Pageable pageable) {
        Specification<AccessEvent> spec = Specification.<AccessEvent>where(Specs.eq(subjectType, "subjectType"))
                .and(Specs.eq(subjectId, "subjectId"))
                .and(Specs.eq(gateId, "gateId"));
        Page<AccessEvent> page = accessEventRepository.findAll(spec, pageable);

        Map<Long, Visitor> visitors = visitorRepository.findAllById(subjectIds(page, AccessSubjectType.VISITOR))
                .stream().collect(Collectors.toMap(Visitor::getId, Function.identity()));
        Map<Long, Worker> workers = workerRepository.findAllById(subjectIds(page, AccessSubjectType.WORKER))
                .stream().collect(Collectors.toMap(Worker::getId, Function.identity()));
        Map<Long, Vehicle> vehicles = vehicleRepository.findAllById(subjectIds(page, AccessSubjectType.VEHICLE))
                .stream().collect(Collectors.toMap(Vehicle::getId, Function.identity()));

        // The one resident relationship each subject type carries - host/sponsor/owner are
        // different relationships elsewhere in the app, but all resolve through this one lookup.
        List<Long> residentIds = Stream.of(
                        visitors.values().stream().map(Visitor::getHostResidentId),
                        workers.values().stream().map(Worker::getSponsorResidentId),
                        vehicles.values().stream().map(Vehicle::getResidentId))
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> residentNames = residentRepository.findAllById(residentIds).stream()
                .collect(Collectors.toMap(Resident::getId, Resident::getFullName));

        return PageResponse.of(page.map(e -> {
            if (e.getSubjectType() == AccessSubjectType.VISITOR && visitors.containsKey(e.getSubjectId())) {
                Visitor visitor = visitors.get(e.getSubjectId());
                return AccessEventResponse.ofVisitor(e, visitor, residentNames.get(visitor.getHostResidentId()));
            }
            if (e.getSubjectType() == AccessSubjectType.WORKER && workers.containsKey(e.getSubjectId())) {
                Worker worker = workers.get(e.getSubjectId());
                return AccessEventResponse.ofWorker(e, worker, residentNames.get(worker.getSponsorResidentId()));
            }
            if (e.getSubjectType() == AccessSubjectType.VEHICLE && vehicles.containsKey(e.getSubjectId())) {
                Vehicle vehicle = vehicles.get(e.getSubjectId());
                return AccessEventResponse.ofVehicle(e, vehicle, residentNames.get(vehicle.getResidentId()));
            }
            return AccessEventResponse.from(e);
        }));
    }

    private List<Long> subjectIds(Page<AccessEvent> page, AccessSubjectType type) {
        return page.getContent().stream()
                .filter(e -> e.getSubjectType() == type)
                .map(AccessEvent::getSubjectId)
                .distinct()
                .toList();
    }
}
