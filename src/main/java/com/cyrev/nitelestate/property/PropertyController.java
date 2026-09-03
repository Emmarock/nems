package com.cyrev.nitelestate.property;

import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.property.dto.PropertyLookupResponse;
import com.cyrev.nitelestate.property.dto.PropertyRequest;
import com.cyrev.nitelestate.property.dto.PropertyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN')")
    public PropertyResponse create(@Valid @RequestBody PropertyRequest request) {
        return propertyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN')")
    public PropertyResponse update(@PathVariable Long id, @Valid @RequestBody PropertyRequest request) {
        return propertyService.update(id, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY')")
    public PropertyResponse findById(@PathVariable Long id) {
        return propertyService.findById(id);
    }

    /** Admin-generated enforcement QR pass for this building (lazily created on first request). */
    @GetMapping("/{id}/access-pass")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN')")
    public Map<String, String> accessPass(@PathVariable Long id) {
        return Map.of("qrToken", propertyService.getOrCreateQrToken(id));
    }

    /** Gate/enforcement-side QR scan: shows the building's owner and full payment history. */
    @GetMapping("/lookup/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY', 'TREASURER', 'CDA_ADMIN')")
    public PropertyLookupResponse lookup(@PathVariable String qrToken) {
        return propertyService.lookup(qrToken);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY')")
    public PageResponse<PropertyResponse> findAll(@RequestParam(required = false) String q,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return propertyService.search(q, Paging.of(page, size, Sort.by("houseNumber")));
    }
}
