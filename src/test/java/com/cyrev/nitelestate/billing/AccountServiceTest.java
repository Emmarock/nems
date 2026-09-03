package com.cyrev.nitelestate.billing;

import com.cyrev.nitelestate.billing.dto.AccountBalanceResponse;
import com.cyrev.nitelestate.payment.PaymentRepository;
import com.cyrev.nitelestate.payment.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Verifies spec §4: TOTAL DUE - PAYMENTS + PENALTIES = OUTSTANDING. */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void outstandingBalance_isDueMinusPaidPlusPenalties() {
        Long residentId = 1L;
        when(invoiceRepository.sumIssuedAmountByResident(residentId)).thenReturn(new BigDecimal("150000.00"));
        when(paymentRepository.sumSuccessfulAmountByResident(residentId)).thenReturn(new BigDecimal("100000.00"));

        AccountBalanceResponse balance = accountService.getBalance(residentId);

        assertThat(balance.totalDue()).isEqualByComparingTo("150000.00");
        assertThat(balance.totalPaid()).isEqualByComparingTo("100000.00");
        assertThat(balance.penalties()).isEqualByComparingTo("0");
        assertThat(balance.outstanding()).isEqualByComparingTo("50000.00");
    }

    @Test
    void outstandingBalance_isZero_whenFullyPaid() {
        Long residentId = 2L;
        when(invoiceRepository.sumIssuedAmountByResident(residentId)).thenReturn(new BigDecimal("50000.00"));
        when(paymentRepository.sumSuccessfulAmountByResident(residentId)).thenReturn(new BigDecimal("50000.00"));

        AccountBalanceResponse balance = accountService.getBalance(residentId);

        assertThat(balance.outstanding()).isEqualByComparingTo("0.00");
    }
}
