package com.cyrev.nitelestate.household;

import com.cyrev.nitelestate.access.dto.AccessEventResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.household.dto.HouseholdMemberLookupResponse;
import com.cyrev.nitelestate.household.dto.HouseholdMemberRequest;
import com.cyrev.nitelestate.household.dto.HouseholdMemberResponse;
import com.cyrev.nitelestate.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** The Household Member module — spouse/family passes approved by the landlord, valid indefinitely until revoked. */
@RestController
@RequestMapping("/api/v1/household-members")
@RequiredArgsConstructor
public class HouseholdMemberController {

    private final HouseholdMemberService householdMemberService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RESIDENT')")
    public HouseholdMemberResponse request(@Valid @RequestBody HouseholdMemberRequest request) {
        return householdMemberService.request(currentUser.residentId(), request);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('RESIDENT', 'SUPER_ADMIN', 'CDA_ADMIN')")
    public HouseholdMemberResponse approve(@PathVariable Long id) {
        return householdMemberService.approve(id, residentIdOrNull(), currentUser.role());
    }

    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('RESIDENT', 'SUPER_ADMIN', 'CDA_ADMIN')")
    public HouseholdMemberResponse revoke(@PathVariable Long id) {
        return householdMemberService.revoke(id, residentIdOrNull(), currentUser.role());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('RESIDENT')")
    public PageResponse<HouseholdMemberResponse> mine(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return householdMemberService.mine(currentUser.residentId(), Paging.of(page, size, Sort.by("fullName")));
    }

    /** Household members awaiting approval on properties the caller owns — empty if they own none. */
    @GetMapping("/pending-approval")
    @PreAuthorize("hasRole('RESIDENT')")
    public PageResponse<HouseholdMemberResponse> pendingApproval(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return householdMemberService.pendingForLandlord(currentUser.residentId(), Paging.of(page, size, Sort.by("fullName")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public HouseholdMemberResponse findById(@PathVariable Long id) {
        return householdMemberService.findById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public PageResponse<HouseholdMemberResponse> findAll(@RequestParam(required = false) String q,
                                                           @RequestParam(required = false) Long propertyId,
                                                           @RequestParam(required = false) Long sponsorResidentId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return householdMemberService.search(q, propertyId, sponsorResidentId, Paging.of(page, size, Sort.by("fullName")));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public PageResponse<AccessEventResponse> logs(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return householdMemberService.logs(id, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
    }

    /** Gate-side QR scan: shows the destination (sponsor resident + property) to confirm. */
    @GetMapping("/lookup/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECRETARY', 'CDA_ADMIN')")
    public HouseholdMemberLookupResponse lookup(@PathVariable String qrToken) {
        return householdMemberService.lookup(qrToken);
    }

    @PostMapping("/checkin/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public HouseholdMemberResponse checkIn(@PathVariable String qrToken, @RequestParam(required = false) Long gateId) {
        return householdMemberService.checkIn(qrToken, gateId, currentUser.userId());
    }

    @PostMapping("/checkout/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public HouseholdMemberResponse checkOut(@PathVariable String qrToken, @RequestParam(required = false) Long gateId) {
        return householdMemberService.checkOut(qrToken, gateId, currentUser.userId());
    }

    /** SUPER_ADMIN/CDA_ADMIN callers aren't linked to a resident id — residentId() would throw. */
    private Long residentIdOrNull() {
        return currentUser.role() == com.cyrev.nitelestate.user.Role.RESIDENT ? currentUser.residentId() : null;
    }
}
