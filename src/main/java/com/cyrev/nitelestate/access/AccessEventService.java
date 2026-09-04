package com.cyrev.nitelestate.access;

import com.cyrev.nitelestate.access.dto.AccessEventResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.vehicle.Vehicle;
import com.cyrev.nitelestate.vehicle.VehicleRepository;
import com.cyrev.nitelestate.visitor.Visitor;
import com.cyrev.nitelestate.visitor.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccessEventService {

    private final AccessEventRepository accessEventRepository;
    private final VisitorRepository visitorRepository;
    private final VehicleRepository vehicleRepository;

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
        Map<Long, Vehicle> vehicles = vehicleRepository.findAllById(subjectIds(page, AccessSubjectType.VEHICLE))
                .stream().collect(Collectors.toMap(Vehicle::getId, Function.identity()));

        return PageResponse.of(page.map(e -> {
            if (e.getSubjectType() == AccessSubjectType.VISITOR && visitors.containsKey(e.getSubjectId())) {
                return AccessEventResponse.ofVisitor(e, visitors.get(e.getSubjectId()));
            }
            if (e.getSubjectType() == AccessSubjectType.VEHICLE && vehicles.containsKey(e.getSubjectId())) {
                return AccessEventResponse.ofVehicle(e, vehicles.get(e.getSubjectId()));
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
