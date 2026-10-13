package com.nitelestate.payment.dto;

import com.nitelestate.payment.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Admin-recorded, back-office payment (spec §5 Phase 1 — bank transfer/cash/cheque confirmations). */
public record PaymentRecordRequest(
        @NotNull Long residentId,
        Long invoiceId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull PaymentMethod method
) {
}
