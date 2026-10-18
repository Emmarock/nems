package com.nitelestate.resident;

import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.search.Paging;
import com.nitelestate.resident.dto.ResidentArrearsResponse;
import com.nitelestate.resident.dto.ResidentLookupResponse;
import com.nitelestate.resident.dto.ResidentRequest;
import com.nitelestate.resident.dto.ResidentResponse;
import com.nitelestate.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'FINANCIAL_SECRETARY')")
    public ResidentResponse update(@PathVariable Long id, @Valid @RequestBody ResidentRequest request) {
        return residentService.update(id, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECURITY')")
    public ResidentResponse findById(@PathVariable Long id) {
        return residentService.findById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECURITY')")
    public PageResponse<ResidentResponse> findAll(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) Long propertyId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return residentService.search(q, propertyId, Paging.of(page, size, Sort.by("fullName")));
    }

    /** Drill-through for the security dashboard's "Accounts in arrears" stat card. */
    @GetMapping("/arrears")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECURITY')")
    public PageResponse<ResidentArrearsResponse> arrears(@RequestParam(required = false) String q,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return residentService.searchInArrears(q, Paging.of(page, size, Sort.unsorted()));
    }

    /** Downloadable PDF snapshot of every account currently in arrears — same list as {@link #arrears}, unpaginated. */
    @GetMapping("/arrears/report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECURITY')")
    public ResponseEntity<byte[]> arrearsReport() {
        byte[] pdf = residentService.generateArrearsReportPdf();
        String filename = "arrears-report-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }

    /** Suspends any resident (owner or tenant) - disables their login and revokes any household passes they sponsor. */
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public ResidentResponse suspend(@PathVariable Long id) {
        return residentService.suspend(id);
    }

    /** Lifts a suspension - re-enables login; sponsored household passes stay revoked until re-approved. */
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public ResidentResponse reactivate(@PathVariable Long id) {
        return residentService.reactivate(id);
    }

    /** Gate-side QR scan: the resident IS the destination (spec Phase 3 §4/§5). */
    @GetMapping("/lookup/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECRETARY', 'CDA_ADMIN')")
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
