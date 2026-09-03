package com.cyrev.nitelestate.reporting.dto;

import java.math.BigDecimal;

/** Backs the CDA dashboard mockup in spec §11, extended with worker/visitor activity (spec §8 Phase 2). */
public record DashboardResponse(
        long residents,
        long properties,
        long registeredVehicles,
        BigDecimal totalBilling,
        BigDecimal collected,
        BigDecimal outstanding,
        double collectionRatePercent,
        long activeWorkersOnSite,
        long activeVisitorPasses,
        long openComplaints
) {
}
