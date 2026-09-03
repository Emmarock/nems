package com.cyrev.nitelestate.billing.dto;

import java.math.BigDecimal;

/** TOTAL DUE - PAYMENTS + PENALTIES = OUTSTANDING (spec §4). */
public record AccountBalanceResponse(
        Long residentId,
        BigDecimal totalDue,
        BigDecimal totalPaid,
        BigDecimal penalties,
        BigDecimal outstanding
) {
}
