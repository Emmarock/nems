package com.cyrev.nitelestate.gate;

import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.gate.dto.GateRequest;
import com.cyrev.nitelestate.gate.dto.GateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gates")
@RequiredArgsConstructor
public class GateController {

    private final GateService gateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN')")
    public GateResponse create(@Valid @RequestBody GateRequest request) {
        return gateService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN')")
    public GateResponse update(@PathVariable Long id, @Valid @RequestBody GateRequest request) {
        return gateService.update(id, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public GateResponse findById(@PathVariable Long id) {
        return gateService.findById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public PageResponse<GateResponse> findAll(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return gateService.search(Paging.of(page, size, Sort.by("name")));
    }
}
