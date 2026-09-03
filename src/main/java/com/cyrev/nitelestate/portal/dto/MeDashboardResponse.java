package com.cyrev.nitelestate.portal.dto;

import com.cyrev.nitelestate.billing.dto.AccountBalanceResponse;
import com.cyrev.nitelestate.property.dto.PropertyResponse;
import com.cyrev.nitelestate.resident.dto.ResidentResponse;
import com.cyrev.nitelestate.vehicle.dto.VehicleResponse;

import java.util.List;

/** Backs the resident portal dashboard mockup in spec §6. */
public record MeDashboardResponse(
        ResidentResponse resident,
        PropertyResponse property,
        AccountBalanceResponse account,
        List<VehicleResponse> vehicles
) {
}
