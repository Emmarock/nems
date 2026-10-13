package com.nitelestate.household;

/**
 * A household member pass is approved by the property's landlord, not self-issued by the tenant
 * who requests it — so, unlike Worker/Visitor, a request genuinely sits PENDING_APPROVAL with no
 * QR issued until the landlord (or an admin) calls approve(). There's no expiry: once ACTIVE, a
 * pass stays valid indefinitely, same as the home owner's own standing, until the sponsor, the
 * landlord, or an admin revokes it.
 */
public enum HouseholdMemberStatus {
    PENDING_APPROVAL,
    ACTIVE,
    REVOKED
}
