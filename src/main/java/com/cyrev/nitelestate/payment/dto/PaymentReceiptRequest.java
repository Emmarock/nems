package com.cyrev.nitelestate.payment.dto;

import com.cyrev.nitelestate.payment.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Resident self-service: "I paid this levy offline, here's my receipt" - creates a Payment in
 * PENDING_APPROVAL for a treasurer/financial secretary to confirm against the bank alert they
 * received (see PaymentService.submitReceipt / approve / reject).
 */
public record PaymentReceiptRequest(
        @NotNull Long levyId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull PaymentMethod method,
        /** Optional base64 data URI, e.g. "data:image/jpeg;base64,...". Size-capped in PaymentService. */
        String receiptImage
) {
}
