package com.nitelestate.rfid;

import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.search.Paging;
import com.nitelestate.rfid.dto.RfidRequestCreateRequest;
import com.nitelestate.rfid.dto.RfidRequestIssueRequest;
import com.nitelestate.rfid.dto.RfidRequestResponse;
import com.nitelestate.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Landlord/tenant-requested physical RFID tags for visitors/workers without a smartphone to
 * show a QR pass, and the security-side queue to fulfil them at the gate. */
@RestController
@RequestMapping("/api/v1/rfid-requests")
@RequiredArgsConstructor
public class RfidRequestController {

    private final RfidRequestService rfidRequestService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RESIDENT')")
    public RfidRequestResponse create(@Valid @RequestBody RfidRequestCreateRequest request) {
        return rfidRequestService.create(currentUser.residentId(), request);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('RESIDENT')")
    public PageResponse<RfidRequestResponse> mine(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return rfidRequestService.mine(currentUser.residentId(), Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('RESIDENT')")
    public RfidRequestResponse cancel(@PathVariable Long id) {
        return rfidRequestService.cancel(id, currentUser.residentId());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public PageResponse<RfidRequestResponse> pending(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return rfidRequestService.pending(Paging.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public RfidRequestResponse issue(@PathVariable Long id, @Valid @RequestBody RfidRequestIssueRequest request) {
        return rfidRequestService.issue(id, request.tagId());
    }
}
