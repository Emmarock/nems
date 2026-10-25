package com.nitelestate.visitor;

import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.search.Paging;
import com.nitelestate.security.CurrentUser;
import com.nitelestate.visitor.dto.VisitorLookupResponse;
import com.nitelestate.visitor.dto.VisitorRequest;
import com.nitelestate.visitor.dto.VisitorResponse;
import com.nitelestate.visitor.dto.VisitorScanResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorService visitorService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RESIDENT')")
    public VisitorResponse create(@Valid @RequestBody VisitorRequest request) {
        return visitorService.create(currentUser.residentId(), request);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('RESIDENT')")
    public VisitorResponse cancel(@PathVariable Long id) {
        return visitorService.cancel(id, currentUser.residentId());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('RESIDENT')")
    public PageResponse<VisitorResponse> mine(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return visitorService.search(currentUser.residentId(), null, null, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "validFrom")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'SECURITY', 'PRO')")
    public VisitorResponse findById(@PathVariable Long id) {
        return visitorService.findById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'SECURITY', 'PRO')")
    public PageResponse<VisitorResponse> findAll(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) VisitorStatus status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return visitorService.search(null, q, status, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "validFrom")));
    }

    /** Gate-side QR scan: shows the destination (host resident + property) to confirm, plus
     * which direction this particular code is for (spec §9). */
    @GetMapping("/lookup/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECRETARY', 'CDA_ADMIN')")
    public VisitorLookupResponse lookup(@PathVariable String qrToken) {
        return visitorService.lookup(qrToken);
    }

    /** Checks the visitor in or out — the scanned token fixes which, the caller doesn't choose. */
    @PostMapping("/scan/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public VisitorScanResponse scan(@PathVariable String qrToken, @RequestParam(required = false) Long gateId) {
        return visitorService.scan(qrToken, gateId, currentUser.userId());
    }
}
