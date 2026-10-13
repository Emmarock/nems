package com.nitelestate.payment.dto;

import com.nitelestate.payment.PaymentMethod;
import com.nitelestate.payment.SharedReceipt;
import com.nitelestate.payment.SharedReceiptStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record SharedReceiptResponse(
        Long id,
        Long tenantResidentId,
        String tenantName,
        Long landlordResidentId,
        Long levyId,
        String levyName,
        BigDecimal amount,
        PaymentMethod method,
        String receiptImage,
        SharedReceiptStatus status,
        Instant createdAt
) {
    public static SharedReceiptResponse from(SharedReceipt r, String tenantName, String levyName) {
        return new SharedReceiptResponse(r.getId(), r.getTenantResidentId(), tenantName, r.getLandlordResidentId(),
                r.getLevyId(), levyName, r.getAmount(), r.getMethod(), r.getReceiptImage(), r.getStatus(), r.getCreatedAt());
    }
}
