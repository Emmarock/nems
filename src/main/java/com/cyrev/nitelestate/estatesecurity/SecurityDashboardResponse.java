package com.cyrev.nitelestate.estatesecurity;

/** Backs the Security Dashboard mockup in spec Phase 3 §1. */
public record SecurityDashboardResponse(
        long visitorsActive,
        long workersOnSite,
        long registeredVehicles,
        long accountsInArrears
) {
}
