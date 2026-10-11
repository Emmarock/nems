package com.cyrev.nitelestate.resident;

public enum ResidentStatus {
    ACTIVE,
    INACTIVE,
    /** Admin/security-initiated, distinct from INACTIVE (moved out) - a temporary restriction, e.g. pending a review, reversible via ResidentService.reactivate. */
    SUSPENDED
}
