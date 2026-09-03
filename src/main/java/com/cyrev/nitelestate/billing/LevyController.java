package com.cyrev.nitelestate.billing;

import com.cyrev.nitelestate.billing.dto.LevyRequest;
import com.cyrev.nitelestate.billing.dto.LevyResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN')")
    public LevyResponse create(@Valid @RequestBody LevyRequest request) {
        return levyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN')")
    public LevyResponse update(@PathVariable Long id, @Valid @RequestBody LevyRequest request) {
        return levyService.update(id, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'RESIDENT')")
    public PageResponse<LevyResponse> findAll(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return levyService.search(Paging.of(page, size, Sort.by("name")));
    }
}
