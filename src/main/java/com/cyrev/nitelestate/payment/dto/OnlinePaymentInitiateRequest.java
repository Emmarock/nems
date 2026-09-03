package com.cyrev.nitelestate.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Resident-initiated online payment (spec §5 Phase 2 — "Pay Outstanding" from the portal). */
public record OnlinePaymentInitiateRequest(
        Long invoiceId,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
