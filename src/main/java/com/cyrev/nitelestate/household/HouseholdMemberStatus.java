package com.cyrev.nitelestate.household;

/**
 * A household member's validity window is set by the property's landlord, not by the tenant who
 * requests the pass — so, unlike Worker/Visitor, a request genuinely sits PENDING_APPROVAL with
 * no QR issued until the landlord (or an admin) calls approve(). EXPIRED is implied, not stored —
 * same pattern as WorkerStatus: computed from validUntil vs today in HouseholdMemberService.evaluate.
 */
public enum HouseholdMemberStatus {
    PENDING_APPROVAL,
    ACTIVE,
    REVOKED
}
