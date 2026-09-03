package com.cyrev.nitelestate.access;

import com.cyrev.nitelestate.access.dto.AccessEventResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Specs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessEventService {

    private final AccessEventRepository accessEventRepository;

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
        return PageResponse.of(accessEventRepository.findAll(spec, pageable), AccessEventResponse::from);
    }
}
