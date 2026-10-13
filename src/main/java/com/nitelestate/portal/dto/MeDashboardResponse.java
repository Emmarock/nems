package com.nitelestate.portal.dto;

import com.nitelestate.billing.dto.AccountBalanceResponse;
import com.nitelestate.property.dto.PropertyResponse;
import com.nitelestate.resident.dto.ResidentResponse;
import com.nitelestate.vehicle.dto.VehicleResponse;

import java.util.List;

/** Backs the resident portal dashboard mockup in spec §6. */
public record MeDashboardResponse(
        ResidentResponse resident,
        PropertyResponse property,
        AccountBalanceResponse account,
        List<VehicleResponse> vehicles
) {
}
