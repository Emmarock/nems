package com.cyrev.nitelestate.worker;

import com.cyrev.nitelestate.access.dto.AccessEventResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.security.CurrentUser;
import com.cyrev.nitelestate.worker.dto.WorkerLookupResponse;
import com.cyrev.nitelestate.worker.dto.WorkerRequest;
import com.cyrev.nitelestate.worker.dto.WorkerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** The Worker Module (spec Phase 2 §4): /api/v1/workers + access-pass/checkin/checkout/logs. */
@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RESIDENT')")
    public WorkerResponse request(@Valid @RequestBody WorkerRequest request) {
        return workerService.request(currentUser.residentId(), request);
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public WorkerResponse suspend(@PathVariable Long id) {
        return workerService.suspend(id);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN')")
    public WorkerResponse complete(@PathVariable Long id) {
        return workerService.complete(id);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('RESIDENT')")
    public PageResponse<WorkerResponse> mine(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return workerService.search(null, currentUser.residentId(), false, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "startDate")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public WorkerResponse findById(@PathVariable Long id) {
        return workerService.findById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public PageResponse<WorkerResponse> findAll(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) boolean activeOnly,
                                                 @RequestParam(required = false) Long sponsorResidentId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return workerService.search(q, sponsorResidentId, activeOnly, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "startDate")));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public PageResponse<AccessEventResponse> logs(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return workerService.logs(id, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
    }

    /** Gate-side QR scan: shows the destination (sponsor resident + property) to confirm (spec Phase 2 §4). */
    @GetMapping("/lookup/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public WorkerLookupResponse lookup(@PathVariable String qrToken) {
        return workerService.lookup(qrToken);
    }

    @PostMapping("/checkin/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public WorkerResponse checkIn(@PathVariable String qrToken, @RequestParam(required = false) Long gateId) {
        return workerService.checkIn(qrToken, gateId, currentUser.userId());
    }

    @PostMapping("/checkout/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public WorkerResponse checkOut(@PathVariable String qrToken, @RequestParam(required = false) Long gateId) {
        return workerService.checkOut(qrToken, gateId, currentUser.userId());
    }
}
