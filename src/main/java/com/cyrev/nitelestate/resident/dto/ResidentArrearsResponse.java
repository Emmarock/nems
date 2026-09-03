package com.cyrev.nitelestate.resident.dto;

import java.math.BigDecimal;

/** One row of the "Accounts in arrears" drill-through from the security dashboard. */
public record ResidentArrearsResponse(
        Long id,
        String fullName,
        String phone,
        Long propertyId,
        String propertyHouseNumber,
        BigDecimal outstanding
) {
}
