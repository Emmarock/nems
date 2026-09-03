package com.cyrev.nitelestate.payment;

import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.payment.dto.PaymentRecordRequest;
import com.cyrev.nitelestate.payment.dto.PaymentResponse;
import com.cyrev.nitelestate.payment.dto.PaymentWebhookPayload;
import com.cyrev.nitelestate.security.CurrentUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TREASURER')")
    public PaymentResponse recordManual(@Valid @RequestBody PaymentRecordRequest request) {
        return paymentService.recordManual(request, currentUser.userId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TREASURER', 'CDA_ADMIN')")
    public PaymentResponse findById(@PathVariable Long id) {
        return paymentService.findById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TREASURER', 'CDA_ADMIN')")
    public PageResponse<PaymentResponse> findAll(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) Long residentId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return paymentService.search(q, residentId, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "paidAt")));
    }

    /** Simulated gateway callback (spec §5: Payment Gateway -> Webhook -> NEMS -> Account Updated). */
    @PostMapping("/webhook")
    @SecurityRequirements
    public PaymentResponse webhook(@Valid @RequestBody PaymentWebhookPayload payload) {
        return paymentService.handleWebhook(payload);
    }
}
