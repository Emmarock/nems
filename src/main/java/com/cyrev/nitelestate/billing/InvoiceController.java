package com.cyrev.nitelestate.billing;

import com.cyrev.nitelestate.billing.dto.InvoiceGenerateRequest;
import com.cyrev.nitelestate.billing.dto.InvoiceResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER')")
    public InvoiceResponse generate(@Valid @RequestBody InvoiceGenerateRequest request) {
        return invoiceService.generate(request);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TREASURER')")
    public InvoiceResponse cancel(@PathVariable Long id) {
        return invoiceService.cancel(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER')")
    public PageResponse<InvoiceResponse> findAll(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) Long residentId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return invoiceService.search(q, residentId, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "issueDate")));
    }
}
