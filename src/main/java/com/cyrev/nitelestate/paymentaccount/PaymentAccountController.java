package com.cyrev.nitelestate.paymentaccount;

import com.cyrev.nitelestate.paymentaccount.dto.PaymentAccountChangeResponse;
import com.cyrev.nitelestate.paymentaccount.dto.PaymentAccountDecisionRequest;
import com.cyrev.nitelestate.paymentaccount.dto.PaymentAccountRequest;
import com.cyrev.nitelestate.paymentaccount.dto.PaymentAccountResponse;
import com.cyrev.nitelestate.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * The one bank account residents pay any levy into. Viewable by any authenticated user; changing
 * it is multi-party: SUPER_ADMIN/CDA_ADMIN/TREASURER/FINANCIAL_SECRETARY can propose a change,
 * but it only takes effect once two OTHER of those roles approve it (see PaymentAccountService).
 */
@RestController
@RequestMapping("/api/v1/payment-account")
@RequiredArgsConstructor
public class PaymentAccountController {

    private static final String ELIGIBLE_ROLES = "hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY')";

    private final PaymentAccountService paymentAccountService;
    private final CurrentUser currentUser;

    @GetMapping
    public PaymentAccountResponse get() {
        return paymentAccountService.getSettings();
    }

    @GetMapping("/pending")
    @PreAuthorize(ELIGIBLE_ROLES)
    public PaymentAccountChangeResponse pending() {
        return paymentAccountService.getPendingChange();
    }

    @PostMapping("/changes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ELIGIBLE_ROLES)
    public PaymentAccountChangeResponse propose(@Valid @RequestBody PaymentAccountRequest request) {
        return paymentAccountService.proposeChange(currentUser.userId(), currentUser.role(), request);
    }

    @PutMapping("/changes/{id}/approve")
    @PreAuthorize(ELIGIBLE_ROLES)
    public PaymentAccountChangeResponse approve(@PathVariable Long id,
                                                 @RequestBody(required = false) PaymentAccountDecisionRequest request) {
        return paymentAccountService.decide(id, currentUser.userId(), currentUser.role(), ApprovalDecision.APPROVED,
                request != null ? request : new PaymentAccountDecisionRequest(null));
    }

    @PutMapping("/changes/{id}/reject")
    @PreAuthorize(ELIGIBLE_ROLES)
    public PaymentAccountChangeResponse reject(@PathVariable Long id,
                                                @RequestBody(required = false) PaymentAccountDecisionRequest request) {
        return paymentAccountService.decide(id, currentUser.userId(), currentUser.role(), ApprovalDecision.REJECTED,
                request != null ? request : new PaymentAccountDecisionRequest(null));
    }
}
