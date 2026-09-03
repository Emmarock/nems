package com.cyrev.nitelestate.property.dto;

import com.cyrev.nitelestate.billing.dto.AccountBalanceResponse;
import com.cyrev.nitelestate.billing.dto.LevyBalanceResponse;
import com.cyrev.nitelestate.payment.dto.PaymentResponse;
import com.cyrev.nitelestate.property.OccupancyStatus;
import com.cyrev.nitelestate.property.PropertyType;

import java.util.List;

/**
 * What an enforcement officer sees scanning a building's QR pass — the property itself plus its
 * owning resident's full payment picture, so compliance can be checked on the spot without
 * looking the resident up separately.
 */
public record PropertyLookupResponse(
        Long id,
        String houseNumber,
        String block,
        String plot,
        String address,
        PropertyType propertyType,
        OccupancyStatus occupancyStatus,
        Long ownerId,
        String ownerName,
        String ownerPhone,
        AccountBalanceResponse balance,
        List<LevyBalanceResponse> levyBreakdown,
        List<PaymentResponse> recentPayments
) {
}
