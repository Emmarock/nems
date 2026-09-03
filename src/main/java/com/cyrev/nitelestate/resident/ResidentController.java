package com.cyrev.nitelestate.resident;

import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.resident.dto.ResidentArrearsResponse;
import com.cyrev.nitelestate.resident.dto.ResidentLookupResponse;
import com.cyrev.nitelestate.resident.dto.ResidentRequest;
import com.cyrev.nitelestate.resident.dto.ResidentResponse;
import com.cyrev.nitelestate.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY')")
    public ResidentResponse create(@Valid @RequestBody ResidentRequest request) {
        return residentService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY')")
    public ResidentResponse update(@PathVariable Long id, @Valid @RequestBody ResidentRequest request) {
        return residentService.update(id, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY')")
    public ResidentResponse findById(@PathVariable Long id) {
        return residentService.findById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY')")
    public PageResponse<ResidentResponse> findAll(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) Long propertyId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return residentService.search(q, propertyId, Paging.of(page, size, Sort.by("fullName")));
    }

    /** Drill-through for the security dashboard's "Accounts in arrears" stat card. */
    @GetMapping("/arrears")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY')")
    public PageResponse<ResidentArrearsResponse> arrears(@RequestParam(required = false) String q,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return residentService.searchInArrears(q, Paging.of(page, size, Sort.unsorted()));
    }

    /** Gate-side QR scan: the resident IS the destination (spec Phase 3 §4/§5). */
    @GetMapping("/lookup/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public ResidentLookupResponse lookup(@PathVariable String qrToken) {
        return residentService.lookup(qrToken);
    }

    @PostMapping("/checkin/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public ResidentResponse checkIn(@PathVariable String qrToken, @RequestParam(required = false) Long gateId) {
        return residentService.checkIn(qrToken, gateId, currentUser.userId());
    }

    @PostMapping("/checkout/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public ResidentResponse checkOut(@PathVariable String qrToken, @RequestParam(required = false) Long gateId) {
        return residentService.checkOut(qrToken, gateId, currentUser.userId());
    }
}
