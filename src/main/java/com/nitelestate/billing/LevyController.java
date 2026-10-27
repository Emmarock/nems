package com.nitelestate.billing;

import com.nitelestate.billing.dto.LevyApplyResponse;
import com.nitelestate.billing.dto.LevyRequest;
import com.nitelestate.billing.dto.LevyResponse;
import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.search.Paging;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/levies")
@RequiredArgsConstructor
public class LevyController {

    private final LevyService levyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY')")
    public LevyResponse create(@Valid @RequestBody LevyRequest request) {
        return levyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY')")
    public LevyResponse update(@PathVariable Long id, @Valid @RequestBody LevyRequest request) {
        return levyService.update(id, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY', 'RESIDENT')")
    public PageResponse<LevyResponse> findAll(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return levyService.search(Paging.of(page, size, Sort.by("name")));
    }

    /** One-time catch-up so a levy meant for everyone isn't only billed to residents created from now on. */
    @PostMapping("/{id}/apply-to-all-residents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY')")
    public LevyApplyResponse applyToAllResidents(@PathVariable Long id) {
        return levyService.applyToAllResidents(id);
    }
}
