package com.cyrev.nitelestate.access;

import com.cyrev.nitelestate.access.dto.AccessEventCreateRequest;
import com.cyrev.nitelestate.access.dto.AccessEventResponse;
import com.cyrev.nitelestate.accesspolicy.AccessPolicyService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Security Officer App entry point (spec Phase 3 §4/§5) — logs resident/visitor/worker entry-exit. */
@RestController
@RequestMapping("/api/v1/access-events")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
public class AccessEventController {

    private final AccessEventService accessEventService;
    private final AccessPolicyService accessPolicyService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccessEventResponse record(@Valid @RequestBody AccessEventCreateRequest request) {
        String flag = switch (request.subjectType()) {
            case RESIDENT -> accessPolicyService.evaluateResident(request.subjectId());
            case VEHICLE -> accessPolicyService.evaluateVehicle(request.subjectId());
            case VISITOR, WORKER -> null; // evaluated by their own dedicated checkin endpoints
        };
        return accessEventService.record(request.subjectType(), request.subjectId(), request.direction(),
                request.gateId(), currentUser.userId(), flag);
    }

    @GetMapping
    public PageResponse<AccessEventResponse> findAll(@RequestParam(required = false) AccessSubjectType subjectType,
                                                       @RequestParam(required = false) Long subjectId,
                                                       @RequestParam(required = false) Long gateId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return accessEventService.search(subjectType, subjectId, gateId,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
    }
}
