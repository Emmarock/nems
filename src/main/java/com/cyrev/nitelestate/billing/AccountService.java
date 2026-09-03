package com.cyrev.nitelestate.billing;

import com.cyrev.nitelestate.billing.dto.AccountBalanceResponse;
import com.cyrev.nitelestate.billing.dto.InvoiceResponse;
import com.cyrev.nitelestate.billing.dto.LevyBalanceResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.payment.PaymentRepository;
import com.cyrev.nitelestate.payment.dto.PaymentResponse;
import com.cyrev.nitelestate.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Computes a resident's account per spec §4: TOTAL DUE - PAYMENTS + PENALTIES = OUTSTANDING. */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    public AccountBalanceResponse getBalance(Long residentId) {
        BigDecimal totalDue = invoiceRepository.sumIssuedAmountByResident(residentId);
        BigDecimal totalPaid = paymentRepository.sumSuccessfulAmountByResident(residentId);
        BigDecimal penalties = BigDecimal.ZERO;
        BigDecimal outstanding = totalDue.subtract(totalPaid).add(penalties);
        return new AccountBalanceResponse(residentId, totalDue, totalPaid, penalties, outstanding);
    }

    /** Per-levy due/paid/outstanding breakdown for a resident, biggest outstanding balance first. */
    public List<LevyBalanceResponse> getBalanceBreakdown(Long residentId) {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, BigDecimal> due = new LinkedHashMap<>();
        for (Object[] row : invoiceRepository.sumIssuedAmountByResidentGroupedByLevy(residentId)) {
            Long levyId = ((Number) row[0]).longValue();
            names.put(levyId, (String) row[1]);
            due.put(levyId, (BigDecimal) row[2]);
        }
        Map<Long, BigDecimal> paid = new LinkedHashMap<>();
        for (Object[] row : paymentRepository.sumSuccessfulAmountByResidentGroupedByLevy(residentId)) {
            paid.put(((Number) row[0]).longValue(), (BigDecimal) row[1]);
        }

        List<LevyBalanceResponse> breakdown = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : due.entrySet()) {
            BigDecimal totalDue = entry.getValue();
            BigDecimal totalPaid = paid.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            breakdown.add(new LevyBalanceResponse(entry.getKey(), names.get(entry.getKey()), totalDue, totalPaid,
                    totalDue.subtract(totalPaid)));
        }
        breakdown.sort(Comparator.comparing(LevyBalanceResponse::outstanding).reversed());
        return breakdown;
    }

    public PageResponse<InvoiceResponse> getInvoices(Long residentId, Pageable pageable) {
        return invoiceService.search(null, residentId, pageable);
    }

    public PageResponse<PaymentResponse> getPayments(Long residentId, Pageable pageable) {
        return paymentService.search(null, residentId, pageable);
    }
}
