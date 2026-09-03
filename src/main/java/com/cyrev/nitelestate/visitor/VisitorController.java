package com.cyrev.nitelestate.visitor;

import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.security.CurrentUser;
import com.cyrev.nitelestate.visitor.dto.VisitorLookupResponse;
import com.cyrev.nitelestate.visitor.dto.VisitorRequest;
import com.cyrev.nitelestate.visitor.dto.VisitorResponse;
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
        return visitorService.search(currentUser.residentId(), null, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "validFrom")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'SECURITY')")
    public VisitorResponse findById(@PathVariable Long id) {
        return visitorService.findById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'SECURITY')")
    public PageResponse<VisitorResponse> findAll(@RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return visitorService.search(null, q, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "validFrom")));
    }

    /** Gate-side QR scan: shows the destination (host resident + property) to confirm (spec §9). */
    @GetMapping("/lookup/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public VisitorLookupResponse lookup(@PathVariable String qrToken) {
        return visitorService.lookup(qrToken);
    }

    @PostMapping("/checkin/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public VisitorResponse checkIn(@PathVariable String qrToken, @RequestParam(required = false) Long gateId) {
        return visitorService.checkIn(qrToken, gateId, currentUser.userId());
    }

    @PostMapping("/checkout/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public VisitorResponse checkOut(@PathVariable String qrToken, @RequestParam(required = false) Long gateId) {
        return visitorService.checkOut(qrToken, gateId, currentUser.userId());
    }
}
