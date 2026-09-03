package com.cyrev.nitelestate.payment.dto;

import com.cyrev.nitelestate.payment.Payment;
import com.cyrev.nitelestate.payment.PaymentMethod;
import com.cyrev.nitelestate.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long residentId,
        String residentName,
        Long invoiceId,
        BigDecimal amount,
        PaymentMethod method,
        String provider,
        String providerReference,
        PaymentStatus status,
        Instant paidAt
) {
    public static PaymentResponse from(Payment p, String residentName) {
        return new PaymentResponse(p.getId(), p.getResidentId(), residentName, p.getInvoiceId(), p.getAmount(),
                p.getMethod(), p.getProvider(), p.getProviderReference(), p.getStatus(), p.getPaidAt());
    }
}
