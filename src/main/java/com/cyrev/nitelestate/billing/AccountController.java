package com.cyrev.nitelestate.billing;

import com.cyrev.nitelestate.billing.dto.AccountBalanceResponse;
import com.cyrev.nitelestate.billing.dto.InvoiceResponse;
import com.cyrev.nitelestate.billing.dto.LevyBalanceResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'SECURITY')")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{residentId}/balance")
    public AccountBalanceResponse balance(@PathVariable Long residentId) {
        return accountService.getBalance(residentId);
    }

    @GetMapping("/{residentId}/balance-breakdown")
    public List<LevyBalanceResponse> balanceBreakdown(@PathVariable Long residentId) {
        return accountService.getBalanceBreakdown(residentId);
    }

    @GetMapping("/{residentId}/invoices")
    public PageResponse<InvoiceResponse> invoices(@PathVariable Long residentId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return accountService.getInvoices(residentId, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "issueDate")));
    }

    @GetMapping("/{residentId}/payments")
    public PageResponse<PaymentResponse> payments(@PathVariable Long residentId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return accountService.getPayments(residentId, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "paidAt")));
    }
}
