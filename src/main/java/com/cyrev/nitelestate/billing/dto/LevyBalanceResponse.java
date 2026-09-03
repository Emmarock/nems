package com.cyrev.nitelestate.billing.dto;

import java.math.BigDecimal;

/** One resident's due/paid/outstanding amounts for a single levy category. */
public record LevyBalanceResponse(
        Long levyId,
        String levyName,
        BigDecimal totalDue,
        BigDecimal totalPaid,
        BigDecimal outstanding
) {
}
