package com.cyrev.nitelestate.billing.dto;

import com.cyrev.nitelestate.billing.Invoice;
import com.cyrev.nitelestate.billing.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceResponse(
        Long id,
        Long residentId,
        String residentName,
        Long levyId,
        String description,
        BigDecimal amount,
        LocalDate issueDate,
        LocalDate dueDate,
        InvoiceStatus status
) {
    public static InvoiceResponse from(Invoice i, String residentName) {
        return new InvoiceResponse(i.getId(), i.getResidentId(), residentName, i.getLevyId(), i.getDescription(),
                i.getAmount(), i.getIssueDate(), i.getDueDate(), i.getStatus());
    }
}
