package com.nitelestate.complaint;

import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.search.Paging;
import com.nitelestate.complaint.dto.ComplaintAssignRequest;
import com.nitelestate.complaint.dto.ComplaintRequest;
import com.nitelestate.complaint.dto.ComplaintResponse;
import com.nitelestate.complaint.dto.ComplaintUpdateRequest;
import com.nitelestate.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RESIDENT')")
    public ComplaintResponse create(@Valid @RequestBody ComplaintRequest request) {
        return complaintService.create(currentUser.residentId(), request);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('RESIDENT')")
    public PageResponse<ComplaintResponse> mine(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return complaintService.search(currentUser.residentId(), null, false, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECRETARY', 'MAINTENANCE')")
    public ComplaintResponse updateStatus(@PathVariable Long id, @Valid @RequestBody ComplaintUpdateRequest request) {
        return complaintService.updateStatus(id, request);
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECRETARY', 'MAINTENANCE')")
    public ComplaintResponse assign(@PathVariable Long id, @RequestBody ComplaintAssignRequest request) {
        return complaintService.assign(id, request.assignedRole());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECRETARY', 'MAINTENANCE')")
    public ComplaintResponse findById(@PathVariable Long id) {
        return complaintService.findById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECRETARY', 'MAINTENANCE')")
    public PageResponse<ComplaintResponse> findAll(@RequestParam(required = false) ComplaintStatus status,
                                                    @RequestParam(required = false) Long residentId,
                                                    @RequestParam(defaultValue = "false") boolean openOnly,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return complaintService.search(residentId, status, openOnly, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
