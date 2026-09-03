package com.cyrev.nitelestate.billing.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Generates an invoice for a resident from an existing levy's current amount/name. */
public record InvoiceGenerateRequest(
        @NotNull Long residentId,
        @NotNull Long levyId,
        LocalDate dueDate
) {
}
