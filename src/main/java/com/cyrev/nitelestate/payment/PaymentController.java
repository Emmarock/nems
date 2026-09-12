package com.cyrev.nitelestate.payment;

import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.payment.dto.PaymentRecordRequest;
import com.cyrev.nitelestate.payment.dto.PaymentResponse;
import com.cyrev.nitelestate.payment.dto.PaymentReviewRequest;
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY')")
    public PaymentResponse recordManual(@Valid @RequestBody PaymentRecordRequest request) {
        return paymentService.recordManual(request, currentUser.userId());
    }

    /** Treasurer/financial secretary confirms a resident-submitted receipt against the bank alert
     * they received (spec: payments must be validated before counting as paid). */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY')")
    public PaymentResponse approve(@PathVariable Long id, @RequestBody(required = false) PaymentReviewRequest request) {
        return paymentService.approve(id, currentUser.userId(), request != null ? request : new PaymentReviewRequest(null));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY')")
    public PaymentResponse reject(@PathVariable Long id, @RequestBody(required = false) PaymentReviewRequest request) {
        return paymentService.reject(id, currentUser.userId(), request != null ? request : new PaymentReviewRequest(null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY', 'CDA_ADMIN')")
    public PaymentResponse findById(@PathVariable Long id) {
        return paymentService.findById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY', 'CDA_ADMIN')")
    public PageResponse<PaymentResponse> findAll(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) Long residentId,
                                                  @RequestParam(required = false) PaymentStatus status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return paymentService.search(q, residentId, status, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "paidAt")));
    }

    /** Simulated gateway callback (spec §5: Payment Gateway -> Webhook -> NEMS -> Account Updated). */
    @PostMapping("/webhook")
    @SecurityRequirements
    public PaymentResponse webhook(@Valid @RequestBody PaymentWebhookPayload payload) {
        return paymentService.handleWebhook(payload);
    }
}
