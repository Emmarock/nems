package com.cyrev.nitelestate.access;

import com.cyrev.nitelestate.access.dto.AccessEventResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
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

@Service
@RequiredArgsConstructor
public class AccessEventService {

    private final AccessEventRepository accessEventRepository;
    private final VisitorRepository visitorRepository;
    private final WorkerRepository workerRepository;
    private final VehicleRepository vehicleRepository;
    private final ResidentRepository residentRepository;

    @Transactional
    public AccessEventResponse record(AccessSubjectType subjectType, Long subjectId, AccessDirection direction,
                                       Long gateId, Long verifiedByUserId, String flagReason) {
        AccessEvent event = new AccessEvent();
        event.setSubjectType(subjectType);
        event.setSubjectId(subjectId);
        event.setDirection(direction);
        event.setGateId(gateId);
        event.setVerifiedByUserId(verifiedByUserId);
        event.setFlagReason(flagReason);
        return AccessEventResponse.from(accessEventRepository.save(event));
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

        List<Long> vehicleResidentIds = vehicles.values().stream()
                .map(Vehicle::getResidentId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> vehicleResidentNames = residentRepository.findAllById(vehicleResidentIds).stream()
                .collect(Collectors.toMap(Resident::getId, Resident::getFullName));

        return PageResponse.of(page.map(e -> {
            if (e.getSubjectType() == AccessSubjectType.VISITOR && visitors.containsKey(e.getSubjectId())) {
                return AccessEventResponse.ofVisitor(e, visitors.get(e.getSubjectId()));
            }
            if (e.getSubjectType() == AccessSubjectType.WORKER && workers.containsKey(e.getSubjectId())) {
                return AccessEventResponse.ofWorker(e, workers.get(e.getSubjectId()));
            }
            if (e.getSubjectType() == AccessSubjectType.VEHICLE && vehicles.containsKey(e.getSubjectId())) {
                Vehicle vehicle = vehicles.get(e.getSubjectId());
                return AccessEventResponse.ofVehicle(e, vehicle, vehicleResidentNames.get(vehicle.getResidentId()));
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
